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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import au.com.addstar.truehardcore.HardcoreWorlds.*;
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

			plugin.Debug("Handling " + player.getName() + " death...");
			plugin.DoPlayerDeath(player, event);
		}
	}

	/*
	 * Handle player is kicked inside the hardcore world
	 * Change their player state if they were "in-game" 
	 */
	@EventHandler(ignoreCancelled=true)
	public void onPlayerKick(PlayerKickEvent event) {
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
			hcp.calcGameTime();
			plugin.SavePlayer(hcp);
			plugin.BroadcastToHardcore(plugin.Header + ChatColor.YELLOW + player.getDisplayName() + " has left " + hcp.getWorld(), player.getName());
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
			hcp.calcGameTime();
			plugin.SavePlayer(hcp);
			plugin.BroadcastToHardcore(plugin.Header + ChatColor.YELLOW + player.getDisplayName() + " has left " + hcp.getWorld(), player.getName());
		}
	}

	/*
	 * Handle players joining the server in the hardcore world
	 * Change their hardcore player state, or kick them out!
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (!plugin.IsHardcoreWorld(player.getWorld())) { return; }

		plugin.DebugLog("EVENT: " + event.getEventName());
		plugin.DebugLog("LOCATION: " + player.getLocation().toString());
		
		if (player.isDead()) {
			plugin.Debug(player.getName() + " joined " + player.getWorld() + " while dead! Ignoring event...");
			return;
		}

		// Check if player is resuming a game or somehow stuck in the world but not playing
		Location loc = null;
		HardcorePlayer hcp = HCPlayers.Get(player);
		if (hcp == null) {
			plugin.Warn(player.getName() + " joined in hardcore world with no player record!");
			loc = plugin.GetLobbyLocation(player, player.getWorld().getName());
		}
		else if ((hcp.getState() == PlayerState.ALIVE) || (hcp.getState() == PlayerState.IN_GAME)) {
			// Send player to game lobby
			plugin.Debug(player.getName() + " joined in " + player.getWorld().getName() + "! Returning player to lobby...");
			loc = plugin.GetLobbyLocation(player, hcp.getWorld());
			hcp.setState(PlayerState.ALIVE);
		} else {
			plugin.Warn(player.getName() + " joined in hardcore world with no game in progess (State=" + hcp.getState() + ")!");
			loc = plugin.GetLobbyLocation(player, hcp.getWorld());
		}
		
		// We need to send them away!
		if (loc != null) {
			// Riders must be ejected before teleport
			if (player.isInsideVehicle()) {
				plugin.Debug(player.getName() + " exiting vehicle...");
				player.leaveVehicle();
			}

			// Save record if needed
			if (hcp != null) {
				plugin.SavePlayer(hcp);					
			}

			// Send the player to the lobby
			if (!Util.Teleport(player, loc)) {
				if (hcp != null) {
					// Mark the player as in game (don't do this by default! causes teleport problems + interop issues with NCP)
					plugin.Warn("Unable to send " + player.getName() + " to lobby! Resuming game play...");
					hcp.setState(PlayerState.IN_GAME);
					plugin.SavePlayer(hcp);
					if (plugin.IsPlayerVanished(player)) {
						plugin.UnvanishPlayer(player);
					}
					plugin.BroadcastToHardcore(plugin.Header + ChatColor.GREEN + player.getDisplayName() + " has entered " + hcp.getWorld(), player.getName());
				} else {
					plugin.Warn("Unable to send " + player.getName() + " to lobby and no player record!! THAT IS BAD!");
				}
			}
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

		// We only care about players who have played and are dead 
		HardcorePlayer hcp = HCPlayers.Get(player.getWorld(), event.getPlayer());
		if ((hcp == null) || (hcp.getState() != PlayerState.DEAD)) { return; }
		
		HardcoreWorld hcw = plugin.HardcoreWorlds.Get(player.getWorld().getName());

		plugin.DebugLog("EVENT: " + event.getEventName());
		plugin.DebugLog("LOCATION: " + player.getLocation().toString());
		
		Location loc = plugin.GetLobbyLocation(player, player.getWorld().getName());
		event.setRespawnLocation(loc);

		player.sendMessage(ChatColor.RED + "You are now banned from " + hcw.getWorld().getName() + " for " + Util.Long2Time(hcw.getBantime()) + "!");
	}

	/*
	 * Handle teleports into or out of hardcore worlds
	 * Prevent "in-game" players from teleporting out of the world
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		final Player player = event.getPlayer();
		final Location from = event.getFrom();
		final Location to   = event.getTo();

		// Ignore if neither from/to are related to hardcore
		if (!plugin.IsHardcoreWorld(to.getWorld()) && !plugin.IsHardcoreWorld(from.getWorld())) { return; }

		TeleportCause cause = event.getCause();
		plugin.DebugLog(
				"PlayerTeleportEvent (" + player.getName() + "): " + 
				from.getWorld().getName() + " " + from.getX() + " " + from.getY() + " " + from.getZ() +
				" [TO] " +
				to.getWorld().getName() + " " + to.getX() + " " + to.getY() + " " + to.getZ() +
				" (" + cause + ")"
		);

		// Some teleport methods are fine.. let them go
		if ((cause == TeleportCause.ENDER_PEARL) || (cause == TeleportCause.END_PORTAL) || (cause == TeleportCause.NETHER_PORTAL)) {
			return;
		}
		
		// Ignore block/chunk loading teleport glitches within the same world (or NoCheatPlus)
		if (from.getWorld().equals(to.getWorld()) && (from.distance(to) <= 30)) { return; }

		//plugin.DebugLog("EVENT: " + event.getEventName());
		//plugin.DebugLog("FROM : " + from);
		//plugin.DebugLog("TO   : " + to);

		if (plugin.IsHardcoreWorld(from.getWorld())) {
			// Prevent unauthorised exit from hardcore
			HardcorePlayer hcp = HCPlayers.Get(from.getWorld(), player);
			if (hcp == null) { return; }
			if (hcp.getState() == PlayerState.IN_GAME) {
				if (player.isOp()) {
					plugin.Debug("OP override! Teleport allowed.");
				} else {
					event.setCancelled(true);
					if (from.getWorld().equals(to.getWorld())) {
						plugin.Debug(player.getName() + " teleport within hardcore cancelled!");
						player.sendMessage(ChatColor.RED + "You are not allowed to teleport while in hardcore!");
					} else {
						plugin.Debug(player.getName() + " teleport out of hardcore cancelled!");
						player.sendMessage(ChatColor.RED + "You are not allowed to teleport out of hardcore!");
					}
					player.sendMessage(ChatColor.GREEN + "Type " + ChatColor.AQUA + "/th leave" + ChatColor.GREEN + " to exit (progress will be saved)");
					plugin.Debug("From: " + from);
					plugin.Debug("To  : " + to);
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
					plugin.Debug(player.getName() + "teleport into hardcore was cancelled!");
					player.sendMessage(ChatColor.RED + "You are not allowed to teleport to a hardcore world.");
				}
			}
		}
	}
	
	/*
	 * Handle any damage to players
	 * Prevent player taking damage while in the "spawn protection" period
	 */
	@EventHandler(ignoreCancelled=true)
	public void onPlayerDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) { return; }
		if (!plugin.IsHardcoreWorld(event.getEntity().getWorld())) { return; }

		Player player = (Player) event.getEntity();
		HardcorePlayer hcp = HCPlayers.Get(player);
		
		if ((hcp != null) && (hcp.isGodMode())) {
			event.setCancelled(true);
	    }
	}

	@EventHandler(ignoreCancelled=true)
	public void onEntityDeath(EntityDeathEvent event) {
		Entity ent = event.getEntity();
		if (!plugin.IsHardcoreWorld(ent.getWorld())) { return; }
		if (!(ent.getLastDamageCause() instanceof EntityDamageByEntityEvent)) { return; }

		// Find out who did the last damage
		EntityDamageByEntityEvent cause = (EntityDamageByEntityEvent) ent.getLastDamageCause();
		Entity damager = cause.getDamager();

		if (damager instanceof Player) {
			Player killer = (Player) damager;
			HardcorePlayer hcp = HCPlayers.Get(killer);
			if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
				if (ent instanceof Player) {
					Player killed = (Player) ent;
					plugin.DebugLog("EntityDeath: " + killer.getName() + " killed " + killed.getName());
					hcp.setPlayerKills(hcp.getPlayerKills()+1);
				} else {
					plugin.DebugLog("EntityDeath: " + killer.getName() + " killed " + ent.getType());
					switch (ent.getType()) {
					case COW:
						hcp.setCowKills(hcp.getCowKills()+1);
						break;
					case PIG:
						hcp.setPigKills(hcp.getPigKills()+1);
						break;
					case SHEEP:
						hcp.setSheepKills(hcp.getSheepKills()+1);
						break;
					case CHICKEN:
						hcp.setChickenKills(hcp.getChickenKills()+1);
						break;
					case CREEPER:
						hcp.setCreeperKills(hcp.getCreeperKills()+1);
						break;
					case ZOMBIE:
						hcp.setZombieKills(hcp.getZombieKills()+1);
						break;
					case SKELETON:
						hcp.setSkeletonKills(hcp.getSkeletonKills()+1);
						break;
					case SPIDER:
					case CAVE_SPIDER:
						hcp.setSpiderKills(hcp.getSpiderKills()+1);
						break;
					case ENDERMAN:
						hcp.setEnderKills(hcp.getEnderKills()+1);
						break;
					case SLIME:
						hcp.setSlimeKills(hcp.getSlimeKills()+1);
						break;
					case MUSHROOM_COW:
						hcp.setMooshKills(hcp.getMooshKills()+1);
						break;
					case PLAYER:
						hcp.setPlayerKills(hcp.getPlayerKills()+1);
						break;
					default:
						hcp.setOtherKills(hcp.getOtherKills()+1);
						break;
					}
				}
			} else {
				plugin.DebugLog("Ignoring hardcore death: " + killer.getName() + " killed " + ent.getType());
			}
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	private void playerExitPortal(PlayerPortalEvent event) {
		Location to = event.getTo();
		if ((to != null) && (plugin.IsHardcoreWorld(to.getWorld()))) {
			if (!plugin.InsideWorldBorder(to)) {
				event.setCancelled(true);
				Player player = event.getPlayer();
				player.sendMessage(ChatColor.RED + "Sorry, this portal destination is not inside the borders of a Hardcore world. Please move it to another location.");
			}
		}
	}
}