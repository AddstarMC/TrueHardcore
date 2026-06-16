/*
 * TrueHardcore
 * Copyright (C) 2013 - 2020  AddstarMC <copyright at addstar dot com dot au>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package au.com.addstar.truehardcore.functions;

import au.com.addstar.truehardcore.TrueHardcore;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.paper.api.PrismPaperApi;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static au.com.addstar.truehardcore.TrueHardcore.warn;
import static au.com.addstar.truehardcore.TrueHardcore.debug;
import static au.com.addstar.truehardcore.TrueHardcore.log;

/**
 * Rolls back a player's world changes when they die, using the Prism v4 API.
 *
 * <p>True hardcore means death is permanent: everything a player built or broke is undone.
 * On death the plugin queues a rollback (per dimension) here; after a short delay we ask
 * Prism to reverse every reversible change that player caused in that world.</p>
 *
 * <p>The delay exists so the world is settled before we touch it (death animation, item
 * drops, the player being moved to the lobby, etc.). Prism's {@code rollback(...)} helper is
 * itself asynchronous - it runs the storage query off-thread and applies block changes back
 * on the region/main thread for us - so this class only needs to hold pending requests and
 * fire them once their delay has elapsed.</p>
 *
 * <p>We do not purge the activity rows afterwards. Prism v4 marks rolled-back activities as
 * {@code reversed}, and our rollback query excludes reversed activities, so a player's death
 * is never rolled back twice. This keeps the audit trail intact; trimming old rows is left to
 * Prism's own purge configuration.</p>
 *
 * <p>A single death fans out into up to three dimension rollbacks (overworld/nether/end), whose
 * Prism futures complete independently and possibly out of order. We track them in memory per
 * death and, only once <em>every</em> dimension has completed successfully, clear the player's
 * {@code rollbackPending} flag (via {@link TrueHardcore#onRollbackComplete}). Until then the
 * player is held out of the world. Any failure leaves the flag set so the world is never entered
 * half-restored - failing safe. This in-memory tracking does not survive a restart: a crash
 * mid-rollback leaves the persisted flag set, requiring an admin to clear it.</p>
 */
public class WorldRollback {
    /**
     * Reversible actions we want undone on death. This deliberately mirrors the historical
     * TrueHardcore rollback scope rather than rolling back everything Prism can reverse.
     * Item drops/pickups are intentionally not listed - they aren't reversible block changes
     * and {@link ActivityQuery.ActivityQueryBuilder#rollback()} already restricts the query to
     * reversible actions, so they're never touched.
     *
     * <p>v4 key changes from the old v3 list: {@code water-bucket} -> {@code bucket-empty},
     * {@code lighter} -> {@code block-ignite}, and {@code portal-create} no longer exists in v4
     * (portal blocks are recorded as {@code block-place}, which is already covered).</p>
     */
    private static final List<String> ROLLBACK_ACTION_KEYS = List.of(
            "block-break",
            "block-place",
            "item-insert",
            "entity-shear",
            "entity-leash",
            "entity-dye",
            "bucket-empty",
            "block-ignite"
    );

    private final PrismPaperApi prism;
    private final TrueHardcore plugin;
    private final ArrayList<RollbackRequest> rollbackQueue;
    private final BukkitTask queueTask;

    /**
     * Identifies this plugin as the owner of the Prism modification queues we create.
     * Prism keys queues and completion results by owner; using a dedicated marker keeps our
     * death rollbacks separate from any operator-issued {@code /pr} commands.
     */
    private final Object queueOwner = new Object();

    /**
     * Outstanding deaths being rolled back, keyed by {@link #deathKey(UUID, String)}. Each entry
     * counts the dimensions still pending for that death so we only clear the player's pending
     * flag once all of them have finished. Guarded by its own monitor.
     */
    private final Map<String, PendingDeath> pendingDeaths = new HashMap<>();

    /**
     * Tracks the rollback progress of a single death across its dimension worlds.
     */
    private static class PendingDeath {
        final UUID playerId;
        final String worldBase;
        int remaining;
        boolean failed;

        PendingDeath(UUID playerId, String worldBase) {
            this.playerId = playerId;
            this.worldBase = worldBase;
        }
    }

    /**
     * Key a death by player and base world name. The {@code _nether}/{@code _the_end} dimension
     * suffixes are stripped so all three dimensions of one death share a single tracker, matching
     * how player records are keyed.
     */
    private static String deathKey(UUID playerId, String worldName) {
        return worldName.replaceAll("_nether|_the_end", "") + "/" + playerId;
    }

    /**
     * Rollback handler backed by the Prism v4 API.
     *
     * @param prism the Prism API service obtained from the Bukkit ServicesManager
     */
    public WorldRollback(PrismPaperApi prism) {
        this.plugin = TrueHardcore.instance;
        this.prism = prism;
        this.rollbackQueue = new ArrayList<>();

        // Poll the queue on the main thread; due requests are dispatched to Prism, which then
        // handles its own threading. Delayed start so we don't fire during server warm-up.
        queueTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                new ProcessNextRequest(), 600L, 100L);
    }

    /**
     * Call on shutdown to stop the queue processor.
     */
    public void onDisable() {
        queueTask.cancel();
    }

    /**
     * A pending rollback for one player in one world (dimension), due at {@link #taskTime}.
     */
    public static class RollbackRequest {
        public final Player player;
        public final World world;
        public final Instant taskTime;
        /** Player UUID captured at queue time; the player may be offline by completion. */
        public final UUID playerId;
        /** Tracker key for the death this dimension belongs to. */
        public final String deathKey;

        /**
         * Create a rollback request.
         *
         * @param player the dead player whose changes are being undone
         * @param world  the world (dimension) to roll back
         * @param time   the earliest time this request should be executed
         */
        public RollbackRequest(Player player, World world, Instant time) {
            this.player = player;
            this.world = world;
            this.taskTime = time;
            this.playerId = player.getUniqueId();
            this.deathKey = deathKey(this.playerId, world.getName());
        }
    }

    public ArrayList<RollbackRequest> getQueue() {
        return rollbackQueue;
    }

    /**
     * Queue a rollback to run after the given delay.
     *
     * <p>Called once per dimension when a player dies. The {@code type} argument is retained for
     * call-site compatibility; only rollbacks are queued now (the old v3 PURGE step is gone, as
     * Prism v4 tracks reversed activities itself).</p>
     *
     * @param type  legacy request type, expected to be {@code "ROLLBACK"}
     * @param p     the dead player
     * @param w     the world (dimension) to roll back
     * @param delay seconds to wait before executing
     */
    public void queueRollback(String type, Player p, World w, int delay) {
        if (!"ROLLBACK".equalsIgnoreCase(type)) {
            warn("Ignoring unsupported rollback request type \"" + type + "\" for "
                    + w.getName() + "/" + p.getName());
            return;
        }
        Instant time = Instant.now().plusSeconds(delay);
        RollbackRequest req = new RollbackRequest(p, w, time);
        debug("Queuing rollback task: " + w.getName() + "/" + p.getName() + " @ " + time);

        // Register this dimension against the death so we know how many completions to wait for.
        synchronized (pendingDeaths) {
            PendingDeath pd = pendingDeaths.computeIfAbsent(req.deathKey,
                    k -> new PendingDeath(req.playerId,
                            w.getName().replaceAll("_nether|_the_end", "")));
            pd.remaining++;
        }

        synchronized (rollbackQueue) {
            rollbackQueue.add(req);
        }
    }

    /**
     * Record one dimension's rollback outcome and, when the whole death has finished, clear the
     * player's pending flag. Called from the Prism completion callback (off the main thread).
     *
     * @param req     the dimension request that just completed
     * @param success true if the rollback applied without error
     */
    private void dimensionComplete(RollbackRequest req, boolean success) {
        boolean clearFlag = false;
        synchronized (pendingDeaths) {
            PendingDeath pd = pendingDeaths.get(req.deathKey);
            if (pd == null) {
                // Restart or admin clear removed the tracker; nothing to do.
                return;
            }
            if (!success) {
                pd.failed = true;
            }
            pd.remaining--;
            if (pd.remaining <= 0) {
                pendingDeaths.remove(req.deathKey);
                if (!pd.failed) {
                    clearFlag = true;
                } else {
                    warn("Rollback for " + pd.worldBase + "/" + req.player.getName()
                            + " had a failed dimension; leaving rollback pending (manual review).");
                }
            }
        }

        if (clearFlag) {
            // Touch player state on the main thread.
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.onRollbackComplete(req.playerId, req.world.getName()));
        }
    }

    /**
     * Remove and return the first request whose delay has elapsed, or null if none are due.
     *
     * @return the next due RollbackRequest, or null
     */
    public RollbackRequest getNextRequest() {
        synchronized (rollbackQueue) {
            for (RollbackRequest req : rollbackQueue) {
                if (Instant.now().isAfter(req.taskTime)) {
                    rollbackQueue.remove(req);
                    debug("Found rollback task to run: " + req.world.getName()
                            + "/" + req.player.getName());
                    return req;
                }
            }
            return null;
        }
    }

    /**
     * Build the Prism query for a single death rollback: every reversible change the player
     * caused in the given world, excluding activities Prism has already reversed.
     *
     * @param req the request to build a query for
     * @return a Prism activity query in rollback mode
     */
    private ActivityQuery buildQuery(RollbackRequest req) {
        return PaperActivityQuery.builder()
                .causePlayerName(req.player.getName())
                .worldUuid(req.world.getUID())
                .actionTypeKeys(ROLLBACK_ACTION_KEYS)
                .reversed(false)
                .rollback()
                .build();
    }

    /**
     * Polls the queue and dispatches due rollbacks to Prism.
     *
     * <p>Runs on the main thread. {@link PrismPaperApi#rollback(Object, ActivityQuery)} returns
     * immediately with a future and handles querying/applying off the main thread, so there's
     * no need to wrap this in an async task or guard it with a manual lock as the v3 version did.</p>
     */
    public class ProcessNextRequest implements Runnable {
        @Override
        public void run() {
            final RollbackRequest req = getNextRequest();
            if (req == null) {
                return;
            }

            if (req.player == null || req.world == null) {
                warn("WARNING: Rollback task has a null player or world! Ignoring..");
                return;
            }

            final String who = req.world.getName() + "/" + req.player.getName();
            debug("Handling rollback task for: " + who);

            try {
                prism.rollback(queueOwner, buildQuery(req))
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                warn("Rollback failed for " + who + "!");
                                error.printStackTrace();
                                dimensionComplete(req, false);
                                return;
                            }
                            if (result.applied() == 0) {
                                debug("Nothing to rollback for " + who);
                            } else {
                                log("Rolled back " + result.applied() + " changes for " + who);
                            }
                            dimensionComplete(req, true);
                        });
            } catch (Exception e) {
                warn("Rollback dispatch failed for " + who + "!");
                e.printStackTrace();
                // Count the failed dispatch so the death's tracker doesn't hang forever.
                dimensionComplete(req, false);
            }
        }
    }
}
