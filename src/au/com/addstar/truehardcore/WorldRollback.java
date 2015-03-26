package au.com.addstar.truehardcore;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.appliers.Rollback;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldRollback implements Runnable {
	final Player player;
	final World world;
	final Prism prism;
	final TrueHardcore plugin;
	final Integer cleartime;
	
	public WorldRollback(Prism prism, Player p, World w, Integer ct) {
		plugin = TrueHardcore.instance;
		this.prism = prism;
		world = w;
		player = p;
		cleartime = ct;
	}
	
	@Override
	public void run() {
		try {
			final QueryParameters params = new QueryParameters();
			params.addPlayerName(player.getName());
			params.setWorld(world.getName());
			params.setProcessType(PrismProcessType.ROLLBACK);
//			params.silent = false;
//			params.before = 0;
//			params.excludeVictimsMode = true;
//			params.excludeKillersMode = true;
			
			// Rollback specified world
			plugin.Debug("Rollback changes for " + player.getName() + " (" + world.getName() + ")...");
			
			final ActionsQuery aq = new ActionsQuery(prism);
			final QueryResult result = aq.lookup(params);
			
			if (!result.getActionResults().isEmpty()) {
				Rollback rollback = new Rollback(prism, Bukkit.getConsoleSender(), result.getActionResults(), params, null);
				rollback.apply();
			}
			
			plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
				@Override
				public void run() {
					try {
						plugin.Debug("Clearing changes for " + player.getName() + " (" + world.getName() + ")...");
						params.setProcessType(PrismProcessType.DELETE);
						aq.delete(params);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, cleartime * 20L);
			
		} catch (Exception e) {
		    // Do nothing or throw an error if you want
			e.printStackTrace();
		}
	}
}
