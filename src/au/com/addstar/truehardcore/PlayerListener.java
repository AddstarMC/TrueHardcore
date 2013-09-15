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
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import au.com.addstar.truehardcore.HardcorePlayers.*;

public class PlayerListener implements Listener {
	
	private TrueHardcore plugin;
	private HardcorePlayers HCPlayers;
	public PlayerListener(TrueHardcore instance) {
		plugin = instance;
		HCPlayers = plugin.HCPlayers;
	}

	/*
	 * Handle player deaths in the hardcore world
	 * Perform death management if they were "in-game"    
	 */
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		final Player player = event.getEntity();
		
		if (plugin.IsHardcoreWorld(player.getWorld())) {
			HardcorePlayer hcp = HCPlayers.Get(player);
			if (hcp == null) { return; }
			if (hcp.getState() != PlayerState.IN_GAME) { return; }

			plugin.Debug("Handling player death...");
			plugin.DoPlayerDeath(player, event);
		}
	}

	/*
	 * Handle player is kicked inside the hardcore world
	 * Change their player state if they were "in-game" 
	 */
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		if (event.isCancelled()) { return; }

		final Player player = event.getPlayer();
		if (!plugin.IsHardcoreWorld(player.getWorld())) { return; }

		plugin.DebugLog("EVENT: " + event.getEventName());
		plugin.DebugLog("LOCATION: " + player.getLocation().toString());

		// We only care about existing hardcore players
		HardcorePlayer hcp = HCPlayers.Get(player);
		if (hcp == null) { return; }
		if (hcp.getState() == PlayerState.IN_GAME) {
			// Mark the player at no longer in game
			hcp.setState(PlayerState.ALIVE);
			hcp.updatePlayer(player);
			plugin.SavePlayer(hcp);
		}
	}

	/*
	 * Handle player quits inside the hardcore world
	 * Change their player state if they were "in-game" 
	 */
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		if (!plugin.IsHardcoreWorld(player.getWorld())) { return; }

		plugin.DebugLog("EVENT: " + event.getEventName());
		plugin.DebugLog("LOCATION: " + player.getLocation().toString());

		// We only care about existing hardcore players
		HardcorePlayer hcp = HCPlayers.Get(player);
		if (hcp == null) { return; }
		if (hcp.getState() == PlayerState.IN_GAME) {
			// Mark the player at no longer in game
			hcp.setState(PlayerState.ALIVE);
			hcp.updatePlayer(player);
			plugin.SavePlayer(hcp);
		}
	}

	/*
	 * Handle players joining the server in the hardcore world
	 * Change their hardcore player state, or kick them out!
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (!plugin.IsHardcoreWorld(player.getWorld())) { return; }

		plugin.DebugLog("EVENT: " + event.getEventName());
		plugin.DebugLog("LOCATION: " + player.getLocation().toString());

		// Check if player is resuming a game or somehow stuck in the world but not playing
		HardcorePlayer hcp = HCPlayers.Get(player);
		if (hcp == null) { return; }
		if (hcp.getState() == PlayerState.ALIVE) {
			// Mark the player as in game
			hcp.setState(PlayerState.IN_GAME);
			plugin.SavePlayer(hcp);
		} else {
			plugin.Warn("Player joined in hardcore world with no game in progess!");
			plugin.SendToLobby(player);
		}
	}

	/*
	 * Handle player changing worlds
	 * TODO: work out if this is needed (currently does nothing)
	 */
	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		final Player player = event.getPlayer();
		//plugin.Debug("EVENT: " + event.getEventName());
		//plugin.Debug("FROM: " + event.getFrom().getName());
		//plugin.Debug("LOCATION: " + player.getLocation().toString());
		
		if (plugin.IsHardcoreWorld(event.getFrom())) {
			// World change
			plugin.Debug("Player exit from hardcore world");
		}
		else if (plugin.IsHardcoreWorld(player.getWorld())) {
			// Player changing to the hardcore world
			//plugin.LoadPlayer(player);
			plugin.Debug("Player entering hardcore world");
		}
	}
	
	/*
	 * Handle the player respawning after death
	 * Return the player to the lobby location
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		final Player player = event.getPlayer();
		if (!plugin.IsHardcoreWorld(player.getWorld())) { return; }

		plugin.DebugLog("EVENT: " + event.getEventName());
		plugin.DebugLog("LOCATION: " + player.getLocation().toString());
		plugin.DebugLog("BED RESPAWN: " + event.isBedSpawn());
		
		plugin.Debug("Sending player to lobby");
		Location loc = plugin.getServer().getWorld("games").getSpawnLocation();
		event.setRespawnLocation(loc);
		player.sendMessage(ChatColor.RED + "You are now banned from hardcore for " + (plugin.DeathBan / 60) + " minutes!");
	}

	/*
	 * Handle teleports into or out of hardcore worlds
	 * Prevent "in-game" players from teleporting out of the world
	 * TODO: Prevent anyone from teleporting in
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.isCancelled()) { return; }

		final Player player = event.getPlayer();
		final Location from = event.getFrom();
		final Location to   = event.getTo();

		// Ignore if neither from/to are related to hardcore
		if (!plugin.IsHardcoreWorld(to.getWorld()) && !plugin.IsHardcoreWorld(from.getWorld())) { return; }

		// Ignore block/chunk loading teleport glitches within the same world (or NoCheatPlus)
		if (from.getWorld().equals(to.getWorld()) && (from.distance(to) <= 30)) { return; }

		plugin.DebugLog("EVENT: " + event.getEventName());
		plugin.DebugLog("FROM : " + from);
		plugin.DebugLog("TO   : " + to);

		if (plugin.IsHardcoreWorld(from.getWorld())) {
			// Prevent unauthorised exit from hardcore
			HardcorePlayer hcp = HCPlayers.Get(from.getWorld(), player);
			if (hcp == null) { return; }
			if (hcp.getState() == PlayerState.IN_GAME) {
				if (player.isOp()) {
					plugin.Debug("OP override! Teleport allowed.");
				} else { 
					event.setCancelled(true);
					plugin.Debug("Player teleport out of hardcore cancelled!");
					player.sendMessage(ChatColor.RED + "You are not allowed to teleport while in hardcore!");
					player.sendMessage(ChatColor.GREEN + "Type " + ChatColor.AQUA + "/th leave" + ChatColor.GREEN + " to exit (progress will be saved)");
				}
			}
		}
		else if (plugin.IsHardcoreWorld(to.getWorld())) {
			// Prevent unauthorised entry into hardcore worlds
			HardcorePlayer hcp = HCPlayers.Get(to.getWorld(), player);
			if ((hcp == null) || (hcp.getState() != PlayerState.IN_GAME)) {
				if (player.isOp()) {
					plugin.Debug("OP override! Teleport allowed.");
				} else { 
					event.setCancelled(true);
					plugin.Debug("Player teleport into hardcore was cancelled!");
					player.sendMessage(ChatColor.RED + "You are not allowed to teleport to a hardcore world.");
				}
			}
		}
	}
}
