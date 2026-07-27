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
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationResultStatus;
import org.prism_mc.prism.api.services.modifications.ModificationSkipReason;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.api.util.Pair;
import org.prism_mc.prism.paper.api.PrismPaperApi;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
     *
     * <p><strong>{@code bucket-empty} and {@code item-insert} are intentionally omitted:</strong>
     * Prism v4.4 has no rollback handler for these action types - it returns them as
     * {@code SKIPPED:NOT_IMPLEMENTED}, so querying them only produces hundreds of noise "problem"
     * rows (spamming the log and triggering pointless retries) while restoring nothing. If a future
     * Prism build implements rollback for placed fluids (bucket-empty) or container inserts
     * (item-insert), re-add them here. Until then they cannot be undone regardless of the query.</p>
     */
    private static final List<String> ROLLBACK_ACTION_KEYS = List.of(
            "block-break",
            "block-place",
            "entity-shear",
            "entity-leash",
            "entity-dye",
            "block-ignite"
    );

    private final PrismPaperApi prism;
    private final TrueHardcore plugin;
    private final ArrayList<RollbackRequest> rollbackQueue;
    private final BukkitTask queueTask;

    /**
     * The rollback currently handed to Prism, or {@code null} when Prism's queue is idle.
     *
     * <p>Prism exposes a <em>single global</em> modification queue: only one rollback/restore can
     * run at a time, and dispatching a second while one is in flight throws
     * {@code IllegalStateException: A modification queue is already running}. We therefore serialise
     * every dispatch through this reference - across all deaths, all players, and all dimensions,
     * not just the dimensions of one death. It is set right before we hand a request to Prism and
     * cleared in the completion callback (success or failure). Touched only on the main thread (the
     * queue poller and the retry task both run there), so no synchronisation is needed. While it is
     * non-null the poller leaves requests on the queue and retries requeue themselves, so nothing is
     * dropped; work just waits its turn. Exposed via {@link #getInFlight()} for {@code /th queue}.</p>
     */
    private RollbackRequest inFlight = null;

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
     * Total dispatches for a single dimension's rollback: the initial attempt plus one retry. Prism
     * v4 can leave part of an operation un-applied (skipped/partial/planned); because those
     * activities stay {@code reversed=false}, re-running the same query re-matches only the
     * leftovers, clearing the common transient case. One retry is enough - a persistent shortfall
     * usually means blocks Prism genuinely can't restore (e.g. other players have since edited the
     * area), so we accept the result after the retry rather than holding the player out.
     */
    private static final int MAX_ROLLBACK_ATTEMPTS = 2;

    /** Seconds to wait before the retry, letting the world settle before re-applying. */
    private static final int RETRY_DELAY_SECONDS = 3;

    /**
     * A pending rollback for one player in one world (dimension), due at {@link #taskTime}.
     */
    public static class RollbackRequest {
        public final Player player;
        public final World world;
        /** Earliest time this request may run; pushed forward when a retry is requeued. */
        public Instant taskTime;
        /** Player UUID captured at queue time; the player may be offline by completion. */
        public final UUID playerId;
        /** Tracker key for the death this dimension belongs to. */
        public final String deathKey;
        /** 1-based dispatch attempt for this dimension; incremented on the retry. */
        public int attempt = 1;

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
     * The rollback currently running in Prism's single global queue, or {@code null} if idle. Note
     * an in-flight request is <em>not</em> in {@link #getQueue()} - the poller removes it on dispatch
     * - so callers wanting the full picture should show this alongside the waiting queue.
     *
     * @return the in-flight request, or null
     */
    public RollbackRequest getInFlight() {
        return inFlight;
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
     * Permanently delete a player's Prism activity history from before their most recent death,
     * across the given worlds. This is the death-anchored history purge: everything the player
     * caused with a timestamp earlier than {@code gameEnd} (their last death) is removed -
     * rolled-back rows and non-reversible clutter (pickups, item-use) alike. Current-life
     * activity is always timestamped after {@code gameEnd} and is never touched.
     *
     * <p><strong>Must be called off the main thread.</strong> Unlike {@code prism.rollback(...)},
     * the storage-adapter delete/count calls are synchronous and hit the database directly.</p>
     *
     * @param playerName the cause player name Prism recorded activities under
     * @param worlds     the worlds (dimensions) to purge; nulls are skipped
     * @param gameEnd    the player's most recent death time; the purge upper bound
     * @return true if all worlds purged without error (safe to mark the death purged), false if
     *         any world failed (leave the flag unset so it retries)
     */
    public boolean purgeHistory(String playerName, List<World> worlds, Date gameEnd) {
        // Prism stores timestamps as epoch SECONDS, and the query before() bound is in seconds.
        final long beforeSeconds = gameEnd.getTime() / 1000L;
        boolean allOk = true;
        int totalDeleted = 0;

        for (World world : worlds) {
            if (world == null) {
                continue;
            }
            try {
                ActivityQuery query = PaperActivityQuery.builder()
                        .causePlayerName(playerName)
                        .worldUuid(world.getUID())
                        .before(beforeSeconds)
                        .build();

                Pair<Integer, Integer> bounds =
                        prism.storageAdapter().getActivitiesPkBounds(query);
                if (bounds == null || bounds.key() == null || bounds.value() == null) {
                    // No matching rows in this world - nothing to delete.
                    continue;
                }
                int deleted = prism.storageAdapter()
                        .deleteActivities(query, bounds.key(), bounds.value());
                totalDeleted += deleted;
                debug("Purged " + deleted + " history rows for " + playerName
                        + " in " + world.getName());
            } catch (Exception e) {
                warn("History purge failed for " + playerName + " in " + world.getName() + "!");
                e.printStackTrace();
                allOk = false;
            }
        }

        if (totalDeleted > 0) {
            log("Purged " + totalDeleted + " total history rows for " + playerName);
        }
        return allOk;
    }

    /**
     * Maximum number of individual non-applied changes to log coordinates for, per dimension.
     * A large rollback can skip hundreds of changes; we log the first few so an operator can walk
     * to them in-world without flooding the log.
     */
    private static final int MAX_LOGGED_DETAILS = 15;

    /**
     * Log the outcome of a single dimension's rollback. Prism v4's {@link ModificationQueueResult}
     * reports far more than the applied count: how many changes were <em>planned</em> versus
     * actually applied, plus per-change {@code SKIPPED}/{@code PARTIAL} statuses with reasons. A
     * partial rollback (permadeath world left half-restored) is high-severity here, so whenever
     * {@code applied} falls short of {@code planned} we surface the shortfall, a breakdown of why
     * changes were skipped/partial, and the coordinates of the first several offenders so they can
     * be found in-world.
     *
     * @param who      world/player label for log lines
     * @param result   the Prism rollback result
     * @param elapsed  wall-clock time from dispatch to completion
     * @return true if the rollback hit a <em>retry-worthy problem</em> - a genuine failure to apply
     *         a change (an {@code ERRORED} skip, or changes left {@code PLANNED} because the queue
     *         didn't complete). Returns false when everything either applied or was
     *         <em>validly</em> skipped (e.g. {@code ALREADY_SET}: the block was changed by someone
     *         else since, so Prism must not overwrite it). Valid skips are expected in normal play
     *         and a retry cannot clear them - it would re-skip identically - so they don't count.
     */
    private boolean logResult(String who, ModificationQueueResult result, Duration elapsed) {
        final int applied = result.applied();
        final int skipped = result.skipped();
        final int partial = result.partial();
        // NB: result.planned() is unreliable in COMPLETING mode (observed as 0 even when thousands
        // applied), so we don't use it. "Processed" is the trustworthy total of what Prism acted on.
        final int processed = applied + skipped + partial;

        if (processed == 0) {
            debug("Nothing to rollback for " + who);
            return false;
        }

        // Classify the non-applied result rows into "valid skips" (expected, permanent - a retry
        // can't help) vs "problems" (ERRORED / left PLANNED - worth one retry). See classifyRows.
        Shortfall sf = classifyRows(result.results());

        // Headline: applied out of total processed, plus the queue mode. mode != COMPLETING means
        // the rollback previewed/planned but never executed - changes left outstanding, which looks
        // in-world exactly like a partial rollback.
        String headline = "Rolled back " + applied + "/" + processed + " processed for " + who
                + " (skipped=" + skipped + ", partial=" + partial
                + ", validSkips=" + sf.validSkips + ", problems=" + sf.problems
                + ", mode=" + result.mode() + ", " + elapsed.toMillis() + "ms)";

        // Side-effect counters - handy for spotting unexpected lava drains / entity moves / drop
        // removals that could look like "missing" world state to a reporting player.
        debug("Rollback side effects for " + who + ": removedBlocks=" + result.removedBlocks()
                + ", drainedLava=" + result.drainedLava()
                + ", movedEntities=" + result.movedEntities()
                + ", removedDrops=" + result.removedDrops()
                + ", mode=" + result.mode());

        boolean modeIncomplete = result.mode() != ModificationQueueMode.COMPLETING;
        if (sf.problems == 0 && !modeIncomplete) {
            // Fully restored, modulo blocks Prism validly refused. This is the healthy steady state.
            log(headline);
            logReasonHistogram(who, sf, false); // valid-skip breakdown at debug only
            return false;
        }

        // A genuine problem or an incomplete queue. Warn and explain, and ask for a retry.
        warn(headline + " - rollback did NOT fully complete; see breakdown below");
        logReasonHistogram(who, sf, true);
        return true;
    }

    /**
     * A classified breakdown of a rollback's non-applied result rows.
     *
     * <p>Prism v4 skip reasons split cleanly into two groups (verified against Prism's
     * {@code PaperBlockAction}/queue source):</p>
     * <ul>
     *   <li><b>Valid skips</b> - {@code ALREADY_SET} (the block was modified by someone/something
     *   since, so Prism must not overwrite it - the common collaborative-area case),
     *   {@code BLACKLISTED} (config says don't touch), {@code FULL_INVENTORY} (can't return an item
     *   to a full container). These are expected, permanent, and a retry re-skips them identically.</li>
     *   <li><b>Problems</b> - {@code ERRORED} (a block-set threw), and any row left {@code PLANNED}
     *   (the queue didn't reach COMPLETING for it - interrupted/aborted). {@code NOT_IMPLEMENTED} is
     *   the builder default and also counts as a problem. These are worth one retry, since the
     *   activities stay {@code reversed=false} and a fresh pass can pick them up.</li>
     * </ul>
     */
    private static class Shortfall {
        int validSkips;
        int problems;
        int plannedRows;
        /** Reason histogram keyed "STATUS:reason", first-seen order. */
        final Map<String, Integer> reasons = new LinkedHashMap<>();
        /** Human-readable "action @ x,y,z -> reason" for the first few problem rows. */
        final List<String> problemDetails = new ArrayList<>();
        /** True if per-change detail was unavailable, so counts came from headline totals only. */
        boolean detailMissing;
    }

    /**
     * Classify the per-change result rows into valid-skips vs problems. A change is a "problem" if
     * it errored or was left planned; every other non-applied row is a valid skip.
     */
    private Shortfall classifyRows(List<ModificationResult> results) {
        Shortfall sf = new Shortfall();
        if (results == null || results.isEmpty()) {
            sf.detailMissing = true;
            return sf;
        }

        for (ModificationResult r : results) {
            ModificationResultStatus status = r.status();
            if (status == ModificationResultStatus.APPLIED) {
                continue;
            }

            String reason;
            boolean problem;
            if (status == ModificationResultStatus.SKIPPED) {
                ModificationSkipReason sr = r.skipReason();
                reason = "SKIPPED:" + sr;
                // ERRORED (and the NOT_IMPLEMENTED default) are real failures; other skips are valid.
                problem = sr == ModificationSkipReason.ERRORED
                        || sr == ModificationSkipReason.NOT_IMPLEMENTED;
            } else if (status == ModificationResultStatus.PARTIAL) {
                // PARTIAL means the change applied but with blacklisted content stripped / a full
                // inventory - the block itself was set, so treat as a valid (expected) outcome.
                reason = "PARTIAL:" + r.partialReason();
                problem = false;
            } else {
                // PLANNED (or anything unexpected) means it never completed - a problem.
                reason = String.valueOf(status);
                problem = true;
                sf.plannedRows++;
            }

            sf.reasons.merge(reason, 1, Integer::sum);
            if (problem) {
                sf.problems++;
                if (sf.problemDetails.size() < MAX_LOGGED_DETAILS) {
                    sf.problemDetails.add(describe(r) + " -> " + reason);
                }
            } else {
                sf.validSkips++;
            }
        }
        return sf;
    }

    /**
     * Log the reason histogram and, when problems are present, the coordinates of the first several
     * problem blocks so an operator can find them in-world.
     *
     * @param who      world/player label
     * @param sf       the classified breakdown
     * @param asWarning true to log at warning level (real problem), false for debug (valid-skip noise)
     */
    private void logReasonHistogram(String who, Shortfall sf, boolean asWarning) {
        if (sf.detailMissing) {
            if (asWarning) {
                warn("  (no per-change detail available from Prism for " + who + ")");
            }
            return;
        }
        if (sf.reasons.isEmpty()) {
            return; // everything applied
        }

        StringBuilder sb = new StringBuilder("  rollback reasons for ").append(who).append(": ");
        boolean first = true;
        for (Map.Entry<String, Integer> e : sf.reasons.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append(" x").append(e.getValue());
            first = false;
        }
        if (asWarning) {
            warn(sb.toString());
        } else {
            debug(sb.toString());
        }

        if (!asWarning) {
            return; // valid-skip breakdown: histogram at debug is enough, skip per-block detail
        }

        // Interpret a PLANNED tail so an operator needn't know Prism's internals: these changes were
        // matched and queued but the queue never completed them (e.g. a batch fetch failed
        // mid-stream). They remain reversible in Prism, hence restorable by a preview.
        if (sf.plannedRows > 0) {
            warn("  " + sf.plannedRows + " change(s) for " + who + " were left PLANNED (queue did"
                    + " not complete - likely interrupted mid-stream); still reversible in Prism.");
        }

        for (String d : sf.problemDetails) {
            warn("    " + d);
        }
        int shown = sf.problemDetails.size();
        if (sf.problems > shown) {
            warn("    ...and " + (sf.problems - shown) + " more problem block(s) not shown");
        }
    }

    /**
     * Render one non-applied change as "action @ x,y,z" for the detail log, so an operator can go
     * straight to the block that wasn't restored. Defensive against nulls in the result row.
     */
    private String describe(ModificationResult r) {
        Activity a = r.activity();
        if (a == null) {
            return (r.target() != null ? r.target() : "?") + " @ ?";
        }
        String action = (a.action() != null && a.action().type() != null)
                ? a.action().type().key() : "?";
        Coordinate c = a.coordinate();
        String loc = (c != null) ? (c.intX() + "," + c.intY() + "," + c.intZ()) : "?";
        return action + " @ " + loc;
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
            // Prism runs one rollback at a time; if one is in flight, leave requests queued and try
            // again next tick. This is what prevents the "A modification queue is already running"
            // crash when multiple deaths (or dimensions) come due close together.
            if (inFlight != null) {
                return;
            }

            final RollbackRequest req = getNextRequest();
            if (req == null) {
                return;
            }

            if (req.player == null || req.world == null) {
                warn("WARNING: Rollback task has a null player or world! Ignoring..");
                return;
            }

            dispatch(req);
        }
    }

    /**
     * Dispatch a rollback for a dimension to Prism and handle its completion.
     *
     * <p>If the result is a shortfall (some changes skipped/partial/left planned) we retry the same
     * query exactly once - the un-applied activities stay {@code reversed=false}, so re-running
     * re-matches only the leftovers and clears the common transient case. A shortfall is expected in
     * normal play (multiple players editing the same area means Prism legitimately can't restore
     * some blocks), so after that single retry we always treat the dimension as
     * <strong>complete</strong> regardless of whether it fully applied: the leftover details are
     * logged (and remain reversible in Prism for manual cleanup), but the player is never held out
     * of the world for a partial rollback. We only mark the dimension failed on a hard error from
     * Prism (the future completing exceptionally), where nothing meaningful happened.</p>
     *
     * <p>Called on the main thread (from the queue processor or the retry task). Prism's
     * {@code rollback(...)} runs off-thread and completes the returned future off-thread; we hop
     * back to the main thread in the callback to clear {@link #rollbackInFlight} and continue.</p>
     *
     * @param req the dimension request to (re)dispatch
     */
    private void dispatch(RollbackRequest req) {
        final String who = req.world.getName() + "/" + req.player.getName();
        final Instant dispatched = Instant.now();
        debug("Handling rollback task for: " + who
                + (req.attempt > 1 ? " (retry " + req.attempt + "/" + MAX_ROLLBACK_ATTEMPTS + ")" : ""));

        // Claim the single global Prism queue. Cleared on every completion/failure path below and
        // in onRollbackReturned().
        inFlight = req;
        try {
            prism.rollback(queueOwner, buildQuery(req))
                    .whenComplete((result, error) ->
                            // Completion fires off-thread; resume on the main thread.
                            plugin.getServer().getScheduler().runTask(plugin,
                                    () -> onRollbackReturned(req, who, dispatched, result, error)));
        } catch (IllegalStateException e) {
            // Prism's queue was busy despite our guard (e.g. an operator /pr rollback is running).
            // Don't fail the dimension - release the flag and requeue so the poller retries later.
            warn("Prism queue busy for " + who + "; will requeue and retry (" + e.getMessage() + ")");
            inFlight = null;
            requeue(req);
        } catch (Exception e) {
            warn("Rollback dispatch failed for " + who + "!");
            e.printStackTrace();
            inFlight = null;
            // Count the failed dispatch so the death's tracker doesn't hang forever.
            dimensionComplete(req, false);
        }
    }

    /**
     * Handle a completed Prism rollback on the main thread: clear the in-flight flag, log/classify
     * the result, and either retry (one problem-driven retry), accept-and-release, or fail on a hard
     * Prism error. Releasing {@link #rollbackInFlight} here lets the poller dispatch the next queued
     * rollback (another dimension, another death, another player) on the following tick.
     */
    private void onRollbackReturned(RollbackRequest req, String who, Instant dispatched,
                                    ModificationQueueResult result, Throwable error) {
        inFlight = null;

        if (error != null) {
            warn("Rollback failed for " + who + "!");
            error.printStackTrace();
            dimensionComplete(req, false);
            return;
        }

        // problemFound is true only for retry-worthy issues (errored / left planned) - NOT for
        // valid skips like ALREADY_SET, which a retry can't fix.
        boolean problemFound = logResult(who, result, Duration.between(dispatched, Instant.now()));

        if (problemFound && req.attempt < MAX_ROLLBACK_ATTEMPTS) {
            // Genuine shortfall - one retry may sweep up changes the queue didn't complete (they
            // stay reversed=false, so a fresh pass re-matches them).
            req.attempt++;
            warn("Rollback for " + who + " had unresolved problems; scheduling one retry in "
                    + RETRY_DELAY_SECONDS + "s.");
            scheduleRetry(req);
            return;
        }

        if (problemFound) {
            // Retry still left problems. Accept anyway so the player is not held out; leftovers are
            // logged above and stay reversible in Prism for cleanup.
            warn("Rollback for " + who + " still had problems after retry; accepting and letting the"
                    + " player continue (problem blocks logged above; still reversible via /pr for"
                    + " manual cleanup).");
        }
        // Valid skips or a cleared retry both land here: dimension is done, player is released. We
        // never hold a player out for a partial rollback.
        dimensionComplete(req, true);
    }

    /**
     * Put a request back on the queue so the poller re-dispatches it once the Prism queue frees up.
     * Used when a dispatch was refused because Prism was already running a modification.
     */
    private void requeue(RollbackRequest req) {
        synchronized (rollbackQueue) {
            rollbackQueue.add(req);
        }
    }

    /**
     * Schedule a retry of a partial rollback. Rather than dispatch directly (which could collide
     * with another death's rollback that started in the meantime), we push the request's due-time
     * out by {@link #RETRY_DELAY_SECONDS} and put it back on the queue. The poller then re-dispatches
     * it once it's due <em>and</em> the single Prism queue is free - so retries serialise with all
     * other rollbacks through the same {@link #rollbackInFlight} guard.
     *
     * @param req the request to retry
     */
    private void scheduleRetry(RollbackRequest req) {
        req.taskTime = Instant.now().plusSeconds(RETRY_DELAY_SECONDS);
        requeue(req);
    }
}
