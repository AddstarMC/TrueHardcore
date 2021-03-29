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
import au.com.addstar.truehardcore.objects.HardcorePlayers;
import au.com.addstar.truehardcore.objects.HardcoreWorlds;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ExecutionException;

/**
 * This class will track players in combat and remove the status when adequate time has passed...
 * Created for the AddstarMC Project. Created by Narimm on 15/10/2018.
 */
public class CombatTracker implements Listener {

    private TrueHardcore plugin;
    private BukkitTask task;

    /**
     * A Extension that tracks combat and drops a players inventory if they log out in combat.
     * @param plugin the plugin.
     */
    public CombatTracker(TrueHardcore plugin) {
        this.plugin = plugin;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, combatMonitor(), 40,
              100);
    }

    /**
     * Runs as the plugin is disabled.
     */
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            HardcorePlayers.HardcorePlayer hcPlayer = plugin.hcPlayers.get(player);
            hcPlayer.setInCombat(false);
            hcPlayer.setCombatTime(0);
        }
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        task.cancel();
    }

    /**
     * Runs async.
     *
     * @return Runnable combat monitor
     */
    public Runnable combatMonitor() {
        return () -> {
            long currTime = System.currentTimeMillis();
            try {
                for (Player player : Bukkit.getScheduler().callSyncMethod(TrueHardcore.instance,
                      Bukkit::getOnlinePlayers).get()) {
                    if (!plugin.isHardcoreWorld(player.getWorld())) {
                        return;
                    }
                    HardcorePlayers.HardcorePlayer hcp = plugin.hcPlayers.get(player);
                    if (hcp != null && hcp.isInCombat()) {
                        if (hcp.getCombatTime() <= currTime) {
                            hcp.setCombatTime(0);
                            hcp.setInCombat(false);
                            plugin.getLogger().info("Combat mode has ended for " + player.getName());
                            if (player.isOnline()) {
                                Bukkit.getScheduler().runTask(TrueHardcore.instance, () -> {
                                    if (player.isOnline())
                                        player.sendMessage(ChatColor.GREEN + "You have exited combat mode.");
                                });
                            }
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        };
    }

    /**
     * Handle players logging out while in combat mode
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        HardcorePlayers.HardcorePlayer hcp = plugin.hcPlayers.get(event.getPlayer());
        if (hcp != null) {
            hcp.setCombatTime(0);
            hcp.setInCombat(false);
            plugin.getLogger().info(ChatColor.YELLOW + "Player " + event.getPlayer().getName()
                    + " left " + hcp.getWorld() + " while in combat mode!");
        }
    }

    /**
     * Handles Player damage.
     * @param event the event
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamagebyEntity(EntityDamageByEntityEvent event) {
        if (!plugin.isHardcoreWorld(event.getDamager().getWorld())) {
            return;
        }
        if (event.getDamager() instanceof Player) {
            if (event.getEntity() instanceof Player) {
                HardcoreWorlds.HardcoreWorld hcw = plugin.hardcoreWorlds.get(event.getEntity().getWorld().getName());
                if (hcw != null && hcw.getAntiCombatLog()) {
                    // Mark and notify players about combat, if necessary
                    playersInCombat((Player) event.getDamager(), (Player) event.getEntity());
                }
            }
        }
    }

    /**
     *
     */
    private boolean playersInCombat(Player player1, Player player2) {
        HardcorePlayers.HardcorePlayer attacker = plugin.hcPlayers.get(player1);
        HardcorePlayers.HardcorePlayer defender = plugin.hcPlayers.get(player2);
        if (attacker == null || defender == null) return false;

        long combatTime = System.currentTimeMillis()
                + (TrueHardcore.getCfg().getWorldConfig(defender.getWorld()).combatTime * 1000L);

        // If either player is not already in combat, log to console about it
        if (!attacker.isInCombat() || !defender.isInCombat()) {
            plugin.getLogger().info(String.format("[Combat Log] %s attacked %s",
                attacker.getPlayerName(),
                defender.getPlayerName()));
        }

        // If a player is not already in combat, mark them and notify them
        if (!attacker.isInCombat()) {
            attacker.setInCombat(true);
            giveWarning(player1);
        }
        if (!defender.isInCombat()) {
            defender.setInCombat(true);
            giveWarning(player2);
        }

        // Always reset the combat expiry time for the most recent attack
        attacker.setCombatTime(combatTime);
        defender.setCombatTime(combatTime);
        return true;
    }

    /**
     *
     */
    private void giveWarning(Player p) {
        p.sendMessage(ChatColor.RED + "Warning: You have entered combat mode!");
        p.sendMessage(ChatColor.RED + "Logging out during combat mode could incur a penalty.");
    }
}