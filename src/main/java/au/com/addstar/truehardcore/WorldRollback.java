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
	private ArrayList<RollbackRequest> RollbackQueue;
	private BukkitTask QueueTask;

	public WorldRollback(Prism prism) {
		plugin = TrueHardcore.instance;
		this.prism = prism;
		RollbackQueue = new ArrayList<RollbackRequest>();

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
				RollbackRequest req = GetNextRequest();
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

				// Lookup changes for specified world+player
				Debug("Querying changes for " + req.player.getName() + " (" + req.world.getName() + ")...");
				final ActionsQuery aq = new ActionsQuery(prism);
				final QueryResult result = aq.lookup(params);

				switch (req.type) {
					case "ROLLBACK":
						// Rollback found changes
						if (!result.getActionResults().isEmpty()) {
							Debug("Rolling back " + result.getActionResults().size() + " changes for " + req.player.getName() + " (" + req.world.getName() + ")...");
							Rollback rollback = new Rollback(prism, Bukkit.getConsoleSender(), result.getActionResults(), params, null);
							rollback.apply();
						}

						// Always add a purge query for this death to the end of the queue
						QueueRollback("PURGE", req.player, req.world, 20);
						break;

					case "PURGE":
						synchronized (RollbackQueue) {
							try {
								Debug("Purging changes for " + req.player.getName() + " (" + req.world.getName() + ")...");
								params.setProcessType(PrismProcessType.DELETE);
								aq.delete(params);
								Debug("Purge completed for " + req.player.getName() + " (" + req.world.getName() + ")...");
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
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
