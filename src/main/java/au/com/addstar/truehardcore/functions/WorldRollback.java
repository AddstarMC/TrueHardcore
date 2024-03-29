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
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionsQuery;
import network.darkhelmet.prism.api.actions.MatchRule;
import network.darkhelmet.prism.actionlibs.QueryParameters;
import network.darkhelmet.prism.actionlibs.QueryResult;
import network.darkhelmet.prism.api.actions.PrismProcessType;
import network.darkhelmet.prism.appliers.Rollback;
import network.darkhelmet.prism.purge.PurgeTask;
import network.darkhelmet.prism.purge.SenderPurgeCallback;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import static au.com.addstar.truehardcore.TrueHardcore.warn;
import static au.com.addstar.truehardcore.TrueHardcore.debug;
import static au.com.addstar.truehardcore.TrueHardcore.log;

public class WorldRollback {
    private Prism prism;
    private TrueHardcore plugin;
    private final ArrayList<RollbackRequest> rollbackQueue;
    private BukkitTask queueTask;
    private Boolean queueLocked = false;

    /**
     * Rollback handler using Prism.
     * @param prism plugin
     */
    public WorldRollback(Prism prism) {
        plugin = TrueHardcore.instance;
        this.prism = prism;
        rollbackQueue = new ArrayList<>();

        // Launch the queue processor thread (with delayed start)
        queueTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
              new ProcessNextRequest(),600L, 100L);
    }

    /**
     * Call on shutdown to ensure thread is taken care off.
     */
    public void onDisable() {
        queueTask.cancel();
    }

    public static class RollbackRequest {
        public Player player;
        public World world;
        public Instant taskTime;
        public String type;

        /**
         * Create a rollback Request.
         * @param t type
         * @param p player
         * @param w world
         * @param time time
         */
        public RollbackRequest(String t, Player p, World w, Instant time) {
            this.type = t;
            this.player = p;
            this.world = w;
            this.taskTime = time;
        }
    }

    public ArrayList<RollbackRequest> getQueue() {
        return rollbackQueue;
    }

    /**
     *Create the rollback request for a scheduled time and add it to the queue.
     * @param t type
     * @param p player
     * @param w world
     * @param delay delay
     */
    public void queueRollback(String t, Player p, World w, int delay) {
        Instant time = Instant.now().plusSeconds(delay);
        RollbackRequest req = new RollbackRequest(t, p, w, time);
        debug("Queuing " + req.type.toLowerCase() + " task: "
              + req.world.getName() + "/" + req.player.getName() + " @ " + time);
        synchronized (rollbackQueue) {
            rollbackQueue.add(req);
        }
    }

    /**
     * Get the next item in the Queue.
     * @return RollbackRequest
     */
    public RollbackRequest getNextRequest() {
        synchronized (rollbackQueue) {
            if (rollbackQueue.size() == 0) {
                return null;
            }
            // Find the first task in the queue that is due to be executed
            for (RollbackRequest req : rollbackQueue) {
                if (Instant.now().isAfter(req.taskTime)) {
                    // This task can be executed now so lets do it!
                    rollbackQueue.remove(req);
                    debug("Found " + req.type.toLowerCase()
                          + " task to run: " + req.world.getName() + "/"
                          + req.player.getName());
                    return req;
                }
            }

            return null;
        }
    }
    
    public class ProcessNextRequest implements Runnable {
        @Override
        public void run() {
            // Attempt to lock the queue. If already locked, it will fail so we skip this cycle.
            if (!lockQueue()) {
                return;
            }

            try {
                final RollbackRequest req = getNextRequest();
                if (req == null) {
                    unlockQueue();
                    return;
                }
                debug("Handling " + req.type.toLowerCase() + " task for: "
                      + req.world.getName() + "/" + req.player.getName());
                if (req.player == null) {
                    warn("WARNING: Rollback task contains an invalid player! Ignoring..");
                    unlockQueue();
                    return;
                }
                if (req.world == null) {
                    warn("WARNING: Rollback task contains an invalid world! Ignoring..");
                    unlockQueue();
                    return;
                }

                final QueryParameters params = new QueryParameters();
                params.addPlayerName(req.player.getName());
                params.setWorld(req.world.getName());
                params.setProcessType(PrismProcessType.ROLLBACK);
                params.addActionType("item-drop", MatchRule.EXCLUDE);
                params.addActionType("item-pickup", MatchRule.EXCLUDE);
                params.addActionType("block-break");
                params.addActionType("block-place");
                params.addActionType("item-insert");
                params.addActionType("entity-shear");
                params.addActionType("entity-leash");
                params.addActionType("entity-dye");
                params.addActionType("water-bucket");
                params.addActionType("lighter");
                params.addActionType("portal-create");

                switch (req.type) {
                    case "ROLLBACK":
                        // Rollback found changes
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                // Lookup changes for specified world+player
                                ActionsQuery aq = new ActionsQuery(prism);
                                debug("Querying changes for " + req.player.getName() + " ("
                                        + req.world.getName() + ")...");
                                QueryResult result = aq.lookup(params);

                                if (result.getActionResults().size() > 0) {
                                    // Always add a purge query for this death to the end of the queue
                                    queueRollback("PURGE", req.player, req.world, 20);

                                    log("Rolling back " + result.getActionResults().size()
                                            + " changes for " + req.player.getName() + " ("
                                            + req.world.getName() + ")...");
                                    Rollback rollback = new Rollback(prism, Bukkit.getConsoleSender(),
                                            result.getActionResults(), params, null);
                                    rollback.apply();
                                } else {
                                    debug("Nothing to rollback for " + req.player.getName()
                                            + " (" + req.world.getName() + ")");
                                }
                            } catch (Exception e) {
                                warn("Rollback failed for " + req.player.getName() + "/"
                                        + req.world.getName() + "!");
                                e.printStackTrace();
                            }
                            unlockQueue();
                        });
                        break;
                    case "PURGE":
                        // We can't do this on the main thread or the server will lock up too long
                        // This will cause Prism connection locking issues sometimes - eventually
                        // we'll figure out a better way to do this without causing server lag or
                        // DB connection issues
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                debug("Purging changes for " + req.player.getName() + " ("
                                      + req.world.getName() + ")...");

                                // build callback
                                final SenderPurgeCallback callback = new SenderPurgeCallback();
                                callback.setSender(Bukkit.getConsoleSender());

                                // add to an arraylist so we're consistent
                                QueryParameters parameters = new QueryParameters();
                                parameters.addPlayerName(req.player.getName());
                                parameters.setWorld(req.world.getName());
                                final CopyOnWriteArrayList<QueryParameters> paramList = new CopyOnWriteArrayList<>();
                                paramList.add(parameters);

                                final ActionsQuery aq = new ActionsQuery(prism);
                                final long minId = parameters.getMinPrimaryKey();
                                final long maxId = parameters.getMaxPrimaryKey();

                                plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                                        new PurgeTask(prism, paramList, 20, minId, maxId, callback));

                                log("Purge queued for " + req.player.getName() + " ("
                                      + req.world.getName() + ")...");
                            } catch (Exception e) {
                                warn("Activity purge failed for " + req.player.getName()
                                      + "/" + req.world.getName() + "!");
                                e.printStackTrace();
                            }
                            unlockQueue();
                        });
                        break;
                    default:
                        warn("WARNING: Unknown rollback task \"" + req.type + "\"!");
                        unlockQueue();
                        break;
                }
            } catch (Exception e) {
                // Do nothing or throw an error if you want
                e.printStackTrace();
                unlockQueue();
            }
        }
    }

    // Lock rollback queue so no further entries can be processed
    private boolean lockQueue() {
        synchronized (queueLocked) {
            if (queueLocked) {
                // queue already locked
                return false;
            }
            queueLocked = true;
            return true;
        }
    }

    // Unlock queue to allow further processing.
    private void unlockQueue() {
        synchronized (queueLocked) {
            queueLocked = false;
        }
    }

    // Check if the queue is locked or not
    public boolean isQueueLocked() {
        synchronized (queueLocked) {
            return queueLocked;
        }
    }
}
