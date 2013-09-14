package au.com.addstar.truehardcore;
/*
* TrueHardcore
* Copyright (C) 2013 add5tar <copyright at addstar dot com dot au>
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
*/

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandTH implements CommandExecutor {
	private TrueHardcore plugin;
	
	public CommandTH(TrueHardcore instance) {
		plugin = instance;
	}
	
	/*
	 * Handle the /truehardcore command
	 */
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		String action = "";
		if (args.length > 0) {
			action = args[0].toUpperCase();
		}

		if (action.equals("PLAY")) {
			if (Util.RequirePermission((Player) sender, "truehardcore.use")) {
				if (args.length > 1) {
					World world = plugin.getServer().getWorld(args[1]);
					if (world == null) {
						sender.sendMessage(ChatColor.RED + "Error: Unknown world!");
						return true;
					}
					
					if (plugin.IsHardcoreWorld(world)) {
						plugin.PlayGame(world.getName(), (Player) sender);
					} else {
						sender.sendMessage(ChatColor.RED + "Error: That is not a hardcore world!");
					}
				} else {
					sender.sendMessage(ChatColor.YELLOW + "Usage: /th play <world>");
				}
			}
		}
		else if (action.equals("LEAVE")) {
			plugin.LeaveGame((Player) sender);
		}
		else if (action.equals("INFO")) {
			// Nothing yet
		}
		else if (action.equals("LIST")) {
			for (String key : plugin.HCPlayers.AllRecords().keySet()) {
				sender.sendMessage("Record: " + key);
			}
		}
		else {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "TrueHardcore Commands:");
			sender.sendMessage(ChatColor.AQUA + "/th play   " + ChatColor.YELLOW + ": Start or resume your hardcore game");
			sender.sendMessage(ChatColor.AQUA + "/th leave  " + ChatColor.YELLOW + ": Leave the hardcore game (progress is saved)");
		}
		
		return true;
	}
}
