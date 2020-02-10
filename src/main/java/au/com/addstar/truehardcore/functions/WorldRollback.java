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
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.appliers.Rollback;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.ArrayList;

import static au.com.addstar.truehardcore.TrueHardcore.debug;
import static au.com.addstar.truehardcore.TrueHardcore.warn;

public class WorldRollback {
    private Prism prism;
    private TrueHardcore plugin;
    private final ArrayList<RollbackRequest> rollbackQueue;
    private BukkitTask queueTask;

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
            try {
                final RollbackRequest req = getNextRequest();
                if (req == null) {
                    return;
                }
                debug("Handling " + req.type.toLowerCase() + " task for: "
                      + req.world.getName() + "/" + req.player.getName());
                if (req.player == null) {
                    warn("WARNING: Rollback task contains an invalid player! "
                          + "Ignoring..");
                    return;
                }
                if (req.world == null) {
                    warn("WARNING: Rollback task contains an invalid world! Ignoring..");
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


                switch (req.type) {
                    case "ROLLBACK":
                        // Rollback found changes
                        try {
                            // Lookup changes for specified world+player
                            ActionsQuery aq = new ActionsQuery(prism);
                            debug("Querying changes for " + req.player.getName() + " ("
                                  + req.world.getName() + ")...");
                            QueryResult result = aq.lookup(params);

                            if (result.getActionResults().size() > 0) {
                                // Always add a purge query for this death to the end of the queue
                                queueRollback("PURGE", req.player, req.world, 20);

                                debug("Rolling back " + result.getActionResults().size()
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
                                ActionsQuery aq = new ActionsQuery(prism);
                                params.setProcessType(PrismProcessType.DELETE);
                                // Temporarily removed until new Prism is working
                                //aq.setShouldPauseDB(true);
                                aq.delete(params);
                                debug("Purge completed for " + req.player.getName() + " ("
                                      + req.world.getName() + ")...");
                            } catch (Exception e) {
                                warn("Activity purge failed for " + req.player.getName()
                                      + "/" + req.world.getName() + "!");
                                e.printStackTrace();
                            }
                        });
                        break;
                    default:
                        warn("WARNING: Unknown rollback task \"" + req.type + "\"!");
                        break;
                }
            } catch (Exception e) {
                // Do nothing or throw an error if you want
                e.printStackTrace();
            }
        }
    }
}
