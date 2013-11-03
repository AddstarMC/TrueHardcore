package au.com.addstar.truehardcore;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.QueryParams;
import de.diddiz.LogBlock.CommandsHandler.CommandClearLog;
import de.diddiz.LogBlock.CommandsHandler.CommandRollback;

public class WorldRollback implements Runnable {
	final Player player;
	final World world;
	final LogBlock logblock;
	final TrueHardcore plugin;
	final Integer cleartime;
	
	public WorldRollback(LogBlock lb, Player p, World w, Integer ct) {
		plugin = TrueHardcore.instance;
		logblock = lb;
		world = w;
		player = p;
		cleartime = ct;
	}
	
	@Override
	public void run() {
		try {
			final QueryParams params = new QueryParams(logblock);
			params.setPlayer(player.getName());
			params.world = world;
			params.silent = false;
			params.before = 0;
			params.excludeVictimsMode = true;
			params.excludeKillersMode = true;

			final CommandSender cs = plugin.getServer().getConsoleSender();

			if (logblock == null) {
				plugin.Debug("CRITICAL! logblock handle is null");
			}

			// Rollback specified world
			plugin.Debug("Rollback changes for " + player.getName() + " (" + params.world.getName() + ")...");
			CommandRollback cr = logblock.getCommandsHandler().new CommandRollback(cs, params, true);
			cr.close();

			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
				@Override
				public void run() {
					try {
						// Clear player changes in the world
						params.world = world;
						plugin.Debug("Clearing changes for " + player.getName() + " (" + params.world.getName() + ")...");
						CommandClearLog ccl = logblock.getCommandsHandler().new CommandClearLog(cs, params, true);
						ccl.close();
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
