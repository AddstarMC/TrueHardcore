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

class WorldRollback implements Runnable {
	private final Player player;
	private final World world;
	private final Prism prism;
	private final TrueHardcore plugin;
	private final int clearTime;
	private final Object lock;

	public WorldRollback(Prism prism, Player p, World w, Integer ct, Object lock) {
		plugin = TrueHardcore.instance;
		this.prism = prism;
		world = w;
		player = p;
		clearTime = ct;
		this.lock = lock;
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
				plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Rollback rollback = new Rollback(prism, Bukkit.getConsoleSender(), result.getActionResults(), params, null);
                    rollback.apply();
                });
			}
			
			plugin.Debug("Scheduling activity purge for " + player.getName() + " (" + world.getName() + ")...");
			plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                synchronized (lock) {
                    try {
                        plugin.Debug("Purging changes for " + player.getName() + " (" + world.getName() + ")...");
                        params.setProcessType(PrismProcessType.DELETE);
                        aq.delete(params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 600L);
			
		} catch (Exception e) {
		    // Do nothing or throw an error if you want
			e.printStackTrace();
		}
	}
}
