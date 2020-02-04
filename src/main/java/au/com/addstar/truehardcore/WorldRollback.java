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

package au.com.addstar.truehardcore;

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

import static au.com.addstar.truehardcore.TrueHardcore.Debug;
import static au.com.addstar.truehardcore.TrueHardcore.Warn;

class WorldRollback {
    private Prism prism;
    private TrueHardcore plugin;
    private final ArrayList<RollbackRequest> RollbackQueue;
    private BukkitTask QueueTask;

    public WorldRollback(Prism prism) {
        plugin = TrueHardcore.instance;
        this.prism = prism;
        RollbackQueue = new ArrayList<>();

        // Launch the queue processor thread (with delayed start)
        QueueTask = plugin.getServer().getScheduler().runTaskTimer(plugin, new ProcessNextRequest(), 600L, 100L);
    }

    class RollbackRequest {
        public Player player;
        public World world;
        public Instant taskTime;
        public String type;

        public RollbackRequest(String t, Player p, World w, Instant time) {
            this.type = t;
            this.player = p;
            this.world = w;
            this.taskTime = time;
        }
    }

    public ArrayList<RollbackRequest> GetQueue() {
        return RollbackQueue;
    }

    // Create the rollback request for a scheduled time and add it to the queue
    public void QueueRollback(String t, Player p, World w, int delay) {
        Instant time = Instant.now().plusSeconds(delay);
        RollbackRequest req = new RollbackRequest(t, p, w, time);
        Debug("Queuing " + req.type.toLowerCase() + " task: " + req.world.getName() + "/" + req.player.getName() + " @ " + time);
        synchronized (RollbackQueue) {
            RollbackQueue.add(req);
        }
    }

    public RollbackRequest GetNextRequest() {
        synchronized (RollbackQueue) {
            if (RollbackQueue.size() == 0) return null;		// Nothing to do

            // Find the first task in the queue that is due to be executed
            for (RollbackRequest req : RollbackQueue) {
                if (Instant.now().isAfter(req.taskTime)) {
                    // This task can be executed now so lets do it!
                    RollbackQueue.remove(req);
                    Debug("Found " + req.type.toLowerCase() + " task to run: " + req.world.getName() + "/" + req.player.getName());
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
                final RollbackRequest req = GetNextRequest();
                if (req == null) return;

                Debug("Handling " + req.type.toLowerCase() + " task for: " + req.world.getName() + "/" + req.player.getName());
                if (req.player == null) {
                    Warn("WARNING: Rollback task contains an invalid player! Ignoring..");
                    return;
                }
                if (req.world == null) {
                    Warn("WARNING: Rollback task contains an invalid world! Ignoring..");
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
                            Debug("Querying changes for " + req.player.getName() + " (" + req.world.getName() + ")...");
                            QueryResult result = aq.lookup(params);

                            if (result.getActionResults().size() > 0) {
                                // Always add a purge query for this death to the end of the queue
                                QueueRollback("PURGE", req.player, req.world, 20);

                                Debug("Rolling back " + result.getActionResults().size() + " changes for " + req.player.getName() + " (" + req.world.getName() + ")...");
                                Rollback rollback = new Rollback(prism, Bukkit.getConsoleSender(), result.getActionResults(), params, null);
                                rollback.apply();
                            } else {
                                Debug("Nothing to rollback for " + req.player.getName() + " (" + req.world.getName() + ")");
                            }
                        } catch (Exception e) {
                            Warn("Rollback failed for " + req.player.getName() + "/" + req.world.getName() + "!");
                            e.printStackTrace();
                        }
                        break;
                    case "PURGE":
                        // We can't do this on the main thread or the server will lock up too long
                        // This will cause Prism connection locking issues sometimes - eventually we'll figure out
                        // a better way to do this without causing server lag or DB connection issues
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                Debug("Purging changes for " + req.player.getName() + " (" + req.world.getName() + ")...");
                                ActionsQuery aq = new ActionsQuery(prism);
                                params.setProcessType(PrismProcessType.DELETE);
                                // Temporarily removed until new Prism is working
                                //aq.setShouldPauseDB(true);
                                aq.delete(params);
                                Debug("Purge completed for " + req.player.getName() + " (" + req.world.getName() + ")...");
							} catch (Exception e) {
								Warn("Activity purge failed for " + req.player.getName() + "/" + req.world.getName() + "!");
								e.printStackTrace();
							}
						});
                        break;
                    default:
                        Warn("WARNING: Unknown rollback task \"" + req.type + "\"!");
                        break;
                }
            } catch (Exception e) {
                // Do nothing or throw an error if you want
                e.printStackTrace();
            }
        }
    }
}
