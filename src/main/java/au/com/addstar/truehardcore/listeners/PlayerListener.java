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

package au.com.addstar.truehardcore.listeners;

import au.com.addstar.truehardcore.TrueHardcore;
import au.com.addstar.truehardcore.functions.Util;
import au.com.addstar.truehardcore.objects.HardcorePlayers;
import au.com.addstar.truehardcore.objects.HardcorePlayers.HardcorePlayer;
import au.com.addstar.truehardcore.objects.HardcorePlayers.PlayerState;
import au.com.addstar.truehardcore.objects.HardcoreWorlds.HardcoreWorld;
import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import network.darkhelmet.prism.actionlibs.ActionsQuery;
import network.darkhelmet.prism.api.actions.MatchRule;
import network.darkhelmet.prism.actionlibs.QueryParameters;
import network.darkhelmet.prism.actionlibs.QueryResult;
import network.darkhelmet.prism.api.actions.Handler;
import network.darkhelmet.prism.api.actions.PrismProcessType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final TrueHardcore plugin;
    private final HardcorePlayers hardcorePlayers;

    public PlayerListener(TrueHardcore instance) {
        plugin = instance;
        hardcorePlayers = plugin.hcPlayers;
    }

    private static String eventToString(PlayerEvent event) {
        StringBuilder out = new StringBuilder(event.getClass().getSimpleName() + " player:"
                + event.getPlayer() + "(" + event.getPlayer().getUniqueId() + ")");

        // For teleport events, log from/to locations
        if (event instanceof PlayerTeleportEvent tpEvent) {
            out.append(", From: ").append(tpEvent.getFrom());
            out.append(", To: ").append(tpEvent.getTo());
        }

        // For PlayerPortalEvent, log cause and from/to locations
        if (event instanceof PlayerPortalEvent ppEvent) {
            out.append(", Cause: ").append(ppEvent.getCause());
            out.append(", From: ").append(ppEvent.getFrom());
            out.append(", To: ").append(ppEvent.getTo());
        }

        // For PlayerTeleportEndGatewayEvent log from/to locations
        if (event instanceof PlayerTeleportEndGatewayEvent ptegEvent) {
            out.append(", From: ").append(ptegEvent.getFrom());
            out.append(", To: ").append(ptegEvent.getTo());
        }
        try {
            Field[] fields = event.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    if (field.getName().equalsIgnoreCase("HANDLER_LIST")) {
                        // Skip this field
                        continue;
                    }
                    field.setAccessible(true);
                    out.append(", ").append(field.getName()).append(": ");
                    Object ob = field.get(event);
                    out.append(ob != null ? ob.toString() : "NULL");
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    //suppress
                }
            }
        } catch (Exception e) {
          out.append("ERROR DECODING:").append(e.getMessage());
        }
        return out.toString();
    }

    /**
     * Handle player deaths in the hardcore world.
     * Perform death management if they were "in-game"
     **/
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();

        if (plugin.isHardcoreWorld(player.getWorld())) {
            HardcorePlayer hcp = hardcorePlayers.get(player);
            if (hcp == null) {
                return;
            }
            if (hcp.getState() != PlayerState.IN_GAME) {
                return;
            }

            TrueHardcore.debug("Handling " + player.getName() + " death...");
            plugin.doPlayerDeath(player, event);
        }
    }

    /*
     * When a player is saved from death by using a totem of undying
     * announce the near-death event to the server
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerUseTotem(EntityResurrectEvent event) {
        // We only care about players
        if (!(event.getEntity() instanceof Player)) return;

        // Ignore if the player isn't holding a totem of undying
        if (event.getHand() == null)  return;

        // Check if the player is in hardcore and then announce the event
        Player player = (Player) event.getEntity();
        if (plugin.isHardcoreWorld(player.getWorld())) {
            HardcorePlayer hcp = hardcorePlayers.get(player);
            if (hcp == null) {
                return;
            }
            if (hcp.getState() == PlayerState.IN_GAME) {
                // Get the type of entity that last hurt the player
                String damagecause = "UNKNOWN";

                // Check if the damage was by entity or block
                EntityDamageEvent lastDamage = player.getLastDamageCause();
                if (lastDamage instanceof EntityDamageByEntityEvent edbee) {
                    TrueHardcore.debug("Player " + player.getName()
                        + " was saved by Totem when damaged by entity event: " + edbee.getDamager().getType());
                    Entity damager = edbee.getDamager();
                    damagecause = damager.getType().toString();
                } else if (lastDamage instanceof EntityDamageByBlockEvent edbbe) {
                    TrueHardcore.debug("Player " + player.getName()
                        + " was saved by Totem when damaged by block event: " + edbbe.getCause());
                    if (edbbe.getDamager() != null) {
                        damagecause = edbbe.getCause().toString();
                    }
                } else {
                    if (lastDamage != null) {
                        TrueHardcore.debug("Player " + player.getName()
                            + " was saved by Totem when damaged by event: " + lastDamage.getCause());
                        damagecause = lastDamage.getCause().toString();
                    } else {
                        TrueHardcore.debug("Player " + player.getName()
                            + " was saved by Totem but last damage cause is NULL!");
                    }
                }

                plugin.broadcastToAllServers(plugin.header + ChatColor.AQUA + player.getDisplayName()
                    + ChatColor.RED + " just escaped a death from "
                    + ChatColor.YELLOW + damagecause
                    + ChatColor.RED + " by using a Totem of Undying in " + hcp.getWorld());
            }
        }
    }

    /*
     * Handle player is kicked inside the hardcore world
     * Change their player state if they were "in-game"
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        handlePlayerExit(event);
    }

    private void handlePlayerExit(PlayerEvent event) {
        final Player player = event.getPlayer();
        TrueHardcore.instance.removeAllowedTeleport(player.getUniqueId());
        if (!plugin.isHardcoreWorld(player.getWorld())) {
            return;
        }

        TrueHardcore.debug("EVENT: " + event.getEventName() + " / " + "LOCATION: " + player.getLocation());
        // We only care about existing hardcore players
        HardcorePlayer hcp = hardcorePlayers.get(player);
        if (hcp == null) {
            return;
        }
        if (hcp.getState() == PlayerState.IN_GAME) {
            // Mark the player at no longer in game
            hcp.setState(PlayerState.ALIVE);
            hcp.updatePlayer(player);
            hcp.calcGameTime();
            plugin.savePlayer(hcp);
            plugin.broadCastToHardcore(plugin.header + ChatColor.YELLOW
                    + player.getDisplayName() + " has left " + hcp.getWorld(), player.getName());
        }
    }

    /**
     * Handle player quits inside the hardcore world.
     * Change their player state if they were "in-game"
     **/
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerExit(event);
    }

    /**
     * Handle players joining the server in the hardcore world.
     * Change their hardcore player state, or kick them out!
     **/
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        TrueHardcore.instance.removeAllowedTeleport(player.getUniqueId());
        if (!plugin.isHardcoreWorld(player.getWorld())) {
            return;
        }
        TrueHardcore.debug("EVENT: " + event.getEventName() + " / " + "LOCATION: " + player.getLocation());

        if (player.isDead()) {
            TrueHardcore.debug(player.getName() + " joined " + player.getWorld()
                    + " while dead! Ignoring event...");
            return;
        }
        // Check if player is resuming a game or somehow stuck in the world but not playing
        Location loc;
        HardcorePlayer hcp = hardcorePlayers.get(player);
        if (hcp == null) {
            TrueHardcore.warn(player.getName()
                    + " joined in hardcore world with no player record!");
            loc = plugin.getLobbyLocation(player, player.getWorld().getName());
        } else if ((hcp.getState() == PlayerState.ALIVE)
                || (hcp.getState() == PlayerState.IN_GAME)) {
            // Send player to game lobby
            TrueHardcore.debug(player.getName() + " joined in " + player.getWorld().getName()
                    + "! Returning player to lobby...");

            // Ensure their actual location is saved properly
            hcp.setLastPos(event.getPlayer().getLocation());
            loc = plugin.getLobbyLocation(player, hcp.getWorld());
            hcp.setState(PlayerState.ALIVE);
        } else {
            TrueHardcore.warn(player.getName()
                    + " joined in hardcore world with no game in progress (State="
                    + hcp.getState() + ")!");
            loc = plugin.getLobbyLocation(player, hcp.getWorld());
        }

        // We need to send them away!
        // Riders must be ejected before teleport
        if (player.isInsideVehicle()) {
            TrueHardcore.debug(player.getName() + " exiting vehicle before joining...");
            player.leaveVehicle();
        }

        // Save record if needed
        if (hcp != null) {
            plugin.savePlayer(hcp);
        }

        // Send the player to the lobby
        // We delay this a little to allow other plugins to finish teleporting (like geSuitTeleports "/back")
        // Our teleport back to lobby is the last one applied
        Bukkit.getScheduler().runTaskLater(TrueHardcore.instance, () -> {
            TrueHardcore.debug("Executing delayed teleport of " + player.getName() + " to lobby at " + loc);
            Util.teleport(player,loc).thenAccept(result -> {
                if (result) {
                    TrueHardcore.debug("Player " + player.getName() + " was successfully teleported to lobby at " + loc);
                } else {
                    if (hcp != null) {
                        // Mark the player as in game (don't do this by default! causes teleport problems
                        // and interop issues with NCP)
                        TrueHardcore.warn("Unable to send " + player.getName()
                                + " to lobby! Resuming game play...");
                        hcp.setState(PlayerState.IN_GAME);
                        plugin.savePlayer(hcp);
                        if (plugin.isPlayerVanished(player)) {
                            plugin.unVanishPlayer(player);
                        }
                        plugin.broadCastToHardcore(plugin.header + ChatColor.GREEN
                                + player.getDisplayName() + " has entered "
                                + hcp.getWorld(), player.getName());
                    } else {
                        TrueHardcore.warn("Unable to send " + player.getName()
                                + " to lobby and no player record!! THAT IS BAD!");
                    }
                }
            });
        }, 10L);
    }

    /**
     * Handle the player respawn after death.
     * Return the player to the lobby location
     **/
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        TrueHardcore.debug("PlayerRespawn event for " + player.getName() + ": " + player.getLocation());
        TrueHardcore.debug(eventToString(event));
        HardcorePlayer hcp = hardcorePlayers.get(player.getWorld(), event.getPlayer());
        if (!plugin.isHardcoreWorld(player.getWorld()) || hcp == null) {
            return;
        }

        if (hcp.getState() != PlayerState.DEAD) {
            // Handle players using the exit portal in the end
            if (player.getWorld().getName().contains("the_end")) {
                TrueHardcore.debug("Respawning " + player.getName() + " at " + "(" + hcp.getSpawnPos() + ")");
                player.sendMessage(ChatColor.GREEN + "You have been sent back to your initial spawn location.");
                TrueHardcore.debug("PlayerSpawnPos: " + hcp.getSpawnPos());
                event.setRespawnLocation(hcp.getSpawnPos());

                // Temporarily allow this player to teleport for a short time
                // This is required sometimes when the portal teleport cause is set to UNKNOWN (we don't know why)
                TrueHardcore.instance.addAllowedTeleport(player.getUniqueId());

                // Give the player 10s invincibility after teleport to protect them
                player.setNoDamageTicks(200);
            }
            // Don't want to do anything else if the player isn't dead
            return;
        }

        TrueHardcore.debug("PLAYER STATE : " + hcp.getState().toString());
        Location loc = plugin.getLobbyLocation(player, player.getWorld().getName());
        event.setRespawnLocation(loc);
        HardcoreWorld hcw = plugin.hardcoreWorlds.get(player.getWorld().getName());
        player.sendMessage(ChatColor.RED + "You are now banned from " + hcw.getWorld().getName()
                + " for " + Util.long2Time(hcw.getBantime()) + "!");
    }

    /**
     * Handle teleports into or out of hardcore worlds.
     * Prevent "in-game" players from teleporting out of the world
     **/
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        TrueHardcore.debug(eventToString(event));
        final Player player = event.getPlayer();
        final Location from = event.getFrom();
        final Location to = event.getTo();
        if (to == null) {
            plugin.getLogger().warning("Teleport Event from or to a null world:");
            return;
        }
        final World worldFrom = from.getWorld();
        final World worldTo = to.getWorld();
        if (worldFrom == null || worldTo == null) {
            plugin.getLogger().warning("Teleport Event from or to a null world:");
            if (worldFrom != null) {
                plugin.getLogger().warning("Teleport TO was NULL");
            } else {
                plugin.getLogger().warning("Teleport FROM was NULL");
            }
            plugin.getLogger().warning(" Teleport Event: " + event.toString());
        } else {
            // Ignore if neither from/to are related to hardcore
            if (!plugin.isHardcoreWorld(worldTo) && !plugin.isHardcoreWorld(worldFrom)) {
                return;
            }
            TeleportCause cause = event.getCause();

            // Some teleport methods are fine.. let them go
            if ((cause == TeleportCause.ENDER_PEARL) || (cause == TeleportCause.END_PORTAL)
                  || (cause == TeleportCause.NETHER_PORTAL) || (cause == TeleportCause.END_GATEWAY)) {
                TrueHardcore.debug("Player teleport cause " + cause + " for " + player.getName() + " is automatically allowed.");
                return;
            }

            if (TrueHardcore.instance.isTeleportAllowed(player.getUniqueId())) {
                // Remove the temporary teleport allowance so it can't be reused
                // This avoids issues with other plugins that teleport like /back (geSuitTeleports)
                TrueHardcore.instance.removeAllowedTeleport(player.getUniqueId());
                TrueHardcore.debug("Player teleport for " + player.getName() + " is temporarily allowed.");
                return;
            }

            // Ignore block/chunk loading teleport glitches within the same world (or anti cheat corrections)
            if (worldFrom.equals(worldTo) && (from.distance(to) <= 30)) {
                return;
            }

            if (plugin.isHardcoreWorld(worldFrom)) {
                // Prevent unauthorised teleport while in hardcore worlds
                HardcorePlayer hcp = hardcorePlayers.get(worldFrom, player);
                if (hcp == null) {
                    return;
                }
                if (hcp.getState() == PlayerState.IN_GAME) {
                    if (worldFrom.equals(worldTo)) {
                        // Prevent unauthorised teleport within hardcore worlds
                        if (player.isOp() || player.hasPermission(
                                "truehardcore.bypass.teleport")) {
                            TrueHardcore.debug("Teleport override (within world) allowed for "
                                    + player.getName());
                            return;
                        } else {
                            TrueHardcore.debug(player.getName()
                                    + " teleport within hardcore cancelled!");
                            player.sendMessage(ChatColor.RED
                                    + "You are not allowed to teleport while in hardcore!");
                        }
                    } else {
                        // Prevent unauthorised exit from hardcore
                        if (player.isOp()
                                || player.hasPermission("truehardcore.bypass.teleportout")) {
                            TrueHardcore.debug("Teleport override (out of world) allowed for "
                                    + player.getName());
                            return;
                        } else {
                            TrueHardcore.debug(player.getName()
                                    + " teleport out of hardcore cancelled!");
                            player.sendMessage(ChatColor.RED
                                    + "You are not allowed to teleport out of hardcore!");
                        }
                    }
                    player.sendMessage(ChatColor.GREEN + "Type " + ChatColor.AQUA
                            + "/th leave" + ChatColor.GREEN + " to exit (progress will be saved)");
                    TrueHardcore.debug("From: " + from);
                    TrueHardcore.debug("To  : " + to);
                    event.setCancelled(true);
                }
            } else if (plugin.isHardcoreWorld(worldTo)) {
                // Prevent unauthorised entry into hardcore worlds
                HardcorePlayer hcp = hardcorePlayers.get(worldTo, player);
                if ((hcp == null) || (hcp.getState() != PlayerState.IN_GAME)) {
                    if (player.isOp() || player.hasPermission(
                            "truehardcore.bypass.teleportin")) {
                        TrueHardcore.debug("Teleport override (into world) allowed for "
                                + player.getName());
                    } else {
                        event.setCancelled(true);
                        TrueHardcore.debug(player.getName()
                                + "teleport into hardcore was cancelled!");
                        player.sendMessage(ChatColor.RED
                                + "You are not allowed to teleport to a hardcore world.");
                    }
                }
            }
        }
    }

    /*
     * Handle any damage to players
     * Prevent player taking damage while in the "spawn protection" period
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        checkDamagedPlayer(event);
    }

    private void checkDamagedPlayer(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!plugin.isHardcoreWorld(event.getEntity().getWorld())) {
            return;
        }

        Player player = (Player) event.getEntity();
        HardcorePlayer hcp = hardcorePlayers.get(player);

        if ((hcp != null) && (hcp.isGodMode())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerExhaustion(EntityExhaustionEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!plugin.isHardcoreWorld(event.getEntity().getWorld())) {
            return;
        }

        Player player = (Player) event.getEntity();
        HardcorePlayer hcp = hardcorePlayers.get(player);

        if ((hcp != null) && (hcp.isGodMode())) {
            event.setCancelled(true);
            event.setExhaustion(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!plugin.isHardcoreWorld(event.getEntity().getWorld())) {
            return;
        }

        Player player = (Player) event.getEntity();
        HardcorePlayer hcp = hardcorePlayers.get(player);

        if ((hcp != null) && (hcp.isGodMode())) {
            if (event.getNewEffect().getType().getEffectCategory() == PotionEffectType.Category.HARMFUL) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Tag a player causing damage to another or being damaged by another.
     **/
    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity ent = event.getEntity();
        if (!plugin.isHardcoreWorld(ent.getWorld())) {
            return;
        }

        if (ent.getLastDamageCause() instanceof EntityDamageByBlockEvent) {
            if (!(ent instanceof Player)) {
                return;
            }
            TrueHardcore.debug("EntityDeathByBlock: " + ent.getName() + " killed by "
                    + ent.getLastDamageCause());

            // We only care about TNT events
            EntityDamageByBlockEvent causeB = (EntityDamageByBlockEvent) ent.getLastDamageCause();
            if (
                    (causeB == null) || (causeB.getDamager() == null)
                            || ((causeB.getDamager().getType() != Material.TNT)
                            && (causeB.getDamager().getType() != Material.TNT_MINECART))) {
                return;
            }
            Location blockLoc = new Location(causeB.getDamager().getWorld(),
                    causeB.getDamager().getX(),
                    causeB.getDamager().getY(),
                    causeB.getDamager().getZ());
            OfflinePlayer killed = ((Player) ent).getPlayer();
            String killedDisplayName = ((Player) ent).getDisplayName();
            if (blockLoc.getWorld() == null) {
                TrueHardcore.debug("Error - null world - EntityDeathByBlock: "
                        + ent.getName() + " killed by " + ent.getLastDamageCause());
                return;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> findPlacer(blockLoc, blockLoc.getWorld().getName(), killed,
                            killedDisplayName));
            return;
        }
        if (!(ent.getLastDamageCause() instanceof EntityDamageByEntityEvent)) {
            return;
        }

        // Find out who did the last damage
        EntityDamageByEntityEvent cause = (EntityDamageByEntityEvent) ent.getLastDamageCause();
        if (cause.isCancelled()) {
            return;
        }
        Entity damager = cause.getDamager();
        if (damager instanceof Player) {
            Player killer = (Player) damager;
            HardcorePlayer hcp = hardcorePlayers.get(killer);
            if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
                if (ent instanceof Player) {
                    Player killed = (Player) ent;
                    TrueHardcore.debug("EntityDeath: " + killer.getName() + " killed "
                            + killed.getName());

                    hcp.setPlayerKills(hcp.getPlayerKills() + 1);
                    giveSkullOnline(killed, killed.getDisplayName(), killer);
                    plugin.broadcastToAllServers(ChatColor.RED + killer.getDisplayName()
                            + " has taken the life of " + killed.getDisplayName()
                            + " and gained a trophy!");
                } else {
                    TrueHardcore.debug("EntityDeath: " + killer.getName() + " killed "
                            + ent.getType());
                    switch (ent.getType()) {
                        case COW:
                            hcp.setCowKills(hcp.getCowKills() + 1);
                            break;
                        case PIG:
                            hcp.setPigKills(hcp.getPigKills() + 1);
                            break;
                        case SHEEP:
                            hcp.setSheepKills(hcp.getSheepKills() + 1);
                            break;
                        case CHICKEN:
                            hcp.setChickenKills(hcp.getChickenKills() + 1);
                            break;
                        case CREEPER:
                            hcp.setCreeperKills(hcp.getCreeperKills() + 1);
                            break;
                        case ZOMBIE:
                            hcp.setZombieKills(hcp.getZombieKills() + 1);
                            break;
                        case SKELETON:
                            hcp.setSkeletonKills(hcp.getSkeletonKills() + 1);
                            break;
                        case SPIDER:
                        case CAVE_SPIDER:
                            hcp.setSpiderKills(hcp.getSpiderKills() + 1);
                            break;
                        case ENDERMAN:
                            hcp.setEnderKills(hcp.getEnderKills() + 1);
                            break;
                        case SLIME:
                            hcp.setSlimeKills(hcp.getSlimeKills() + 1);
                            break;
                        case MOOSHROOM:
                            hcp.setMooshKills(hcp.getMooshKills() + 1);
                            break;
                        case PLAYER:
                            hcp.setPlayerKills(hcp.getPlayerKills() + 1);
                            break;
                        default:
                            hcp.setOtherKills(hcp.getOtherKills() + 1);
                            break;
                    }
                }
            } else {
                TrueHardcore.debug("Ignoring hardcore death: " + killer.getName()
                        + " killed " + ent.getType());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void playerExitPortal(PlayerPortalEvent event) {
        TrueHardcore.debug(eventToString(event));
        Location to = event.getTo();
        if ((to != null) && (plugin.isHardcoreWorld(to.getWorld()))) {
            if (!plugin.insideWorldBorder(to)) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                player.sendMessage(ChatColor.RED + "Sorry, this portal destination is not inside "
                        + "the borders of a Hardcore world. Please move it to another location.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void playerEnterPortal(PlayerPortalEvent event) {
        TrueHardcore.debug(eventToString(event));
        Player player = event.getPlayer();
        if (player.hasPermission("truehardcore.endteleport")) {
            return;
        }
        if (event.getCause() == TeleportCause.END_PORTAL) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Sorry, this portal destination is unavailable. "
                    + "Best get out of that fire quick you have temporary resistance!!");
            PotionEffect effect = new PotionEffect(PotionEffectType.FIRE_RESISTANCE,
                    400, 2, false, true);
            player.addPotionEffect(effect);
            player.playNote(event.getPlayer().getLocation(), Instrument.PIANO,
                    Note.natural(1, Note.Tone.B));
        }
    }

    private void findPlacer(Location location, String worldName, OfflinePlayer killed,
                            String displayName) {
        if (!plugin.prismHooked) {
            return;
            //if Prism isn't loaded ...this wont work.
        }
        QueryParameters parameters = new QueryParameters();
        parameters.setSpecificBlockLocation(location);
        parameters.setProcessType(PrismProcessType.LOOKUP);
        parameters.setLimit(1);
        parameters.addActionType("block-place", MatchRule.INCLUDE);
        ActionsQuery aq = new ActionsQuery(plugin.prism);
        QueryResult lookupResult = aq.lookup(parameters);
        if (lookupResult.getActionResults().size() > 0) {
            Handler handle = lookupResult.getActionResults().get(0);
            UUID killerUuid = handle.getUuid();
            OfflinePlayer killer = Bukkit.getOfflinePlayer(killerUuid);
            Bukkit.getScheduler().runTask(plugin, () -> updateGame(worldName, killed, killer,
                    displayName));

        }
    }

    private void updateGame(String worldName, OfflinePlayer killed, OfflinePlayer killer,
                            String killedDN) {
        HardcorePlayer hcp = hardcorePlayers.get(worldName, killer.getUniqueId());
        if (hcp != null) {
            TrueHardcore.debug("EntityDeath: " + killer.getName() + " killed " + killedDN);
            hcp.setPlayerKills(hcp.getPlayerKills() + 1);
            if (killer.isOnline()) {
                Player killerOnline = Bukkit.getPlayer(killer.getUniqueId());
                if (killerOnline != null) {
                    giveSkullOnline(killed, killedDN, killerOnline);
                }
            } else {
                TrueHardcore.debug(killer.getName() + " is offline and killed " + killedDN);
            }
        }
    }

    private void giveSkullOnline(OfflinePlayer killedName, String killedDN, Player killer) {
        killer.getWorld().strikeLightningEffect(killer.getLocation());
        PotionEffect effect = new PotionEffect(PotionEffectType.NAUSEA, 5 * 20, 0, false, true);
        killer.addPotionEffect(effect);
        giveSkull(killedName, killedDN, killer, true);
    }

    private void giveSkull(OfflinePlayer killed, String killedDN, Player killer, boolean isOnline) {
        if (killer == null) {
            return;
        }
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(killed);
            skullMeta.setDisplayName(killedDN);
            skullMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            skullMeta.addEnchant(Enchantment.LOOTING, 1, true);
            List<String> loreList = new ArrayList<>();
            loreList.add("The Head of " + killedDN);
            Date date = new Date(System.currentTimeMillis());
            DateFormat df = DateFormat.getDateTimeInstance(2, 2);
            df.format(date);
            loreList.add("Killed on " + df.format(date) + " by " + killer.getDisplayName());
            skullMeta.setLore(loreList);
        }
        skull.setItemMeta(skullMeta);
        killer.getWorld().dropItem(killer.getLocation(), skull);
    }
}