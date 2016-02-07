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
			params.addActionType("item-drop", MatchRule.EXCLUDE);
			params.addActionType("item-pickup", MatchRule.EXCLUDE);
			
			// Rollback specified world
			plugin.Debug("Rollback changes for " + player.getName() + " (" + world.getName() + ")...");
			
			final ActionsQuery aq = new ActionsQuery(prism);
			final QueryResult result = aq.lookup(params);
			
			if (!result.getActionResults().isEmpty()) {
				plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
					@Override
					public void run() {
						Rollback rollback = new Rollback(prism, Bukkit.getConsoleSender(), result.getActionResults(), params, null);
						rollback.apply();
					}
				});
			}
			
			plugin.Debug("Scheduling activity purge for " + player.getName() + " (" + world.getName() + ")...");
			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
				@Override
				public void run() {
					try {
						plugin.Debug("Purging changes for " + player.getName() + " (" + world.getName() + ")...");
						params.setProcessType(PrismProcessType.DELETE);
						aq.delete(params);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, cleartime * 300L);
			
		} catch (Exception e) {
		    // Do nothing or throw an error if you want
			e.printStackTrace();
		}
	}
}
