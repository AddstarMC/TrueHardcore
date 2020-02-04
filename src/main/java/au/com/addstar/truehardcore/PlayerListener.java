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

package au.com.addstar.truehardcore;

import com.lishid.openinv.IOpenInv;
import com.lishid.openinv.internal.ISpecialEnderChest;
import com.lishid.openinv.internal.ISpecialPlayerInventory;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.appliers.PrismProcessType;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import au.com.addstar.truehardcore.HardcoreWorlds.*;
import au.com.addstar.truehardcore.HardcorePlayers.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.text.DateFormat;
import java.util.*;

class PlayerListener implements Listener {

    private final TrueHardcore plugin;
    private final HardcorePlayers HCPlayers;
    PlayerListener(TrueHardcore instance) {
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

            TrueHardcore.Debug("Handling " + player.getName() + " death...");
            plugin.DoPlayerDeath(player, event);
        }
    }

    /*
     * Handle player is kicked inside the hardcore world
     * Change their player state if they were "in-game"
     */
    @EventHandler(ignoreCancelled=true)
    public void onPlayerKick(PlayerKickEvent event) {
        handlePlayerExit(event);
    }
    private void handlePlayerExit(PlayerEvent event){
        final Player player = event.getPlayer();
        if (!plugin.IsHardcoreWorld(player.getWorld())) { return; }

        TrueHardcore.DebugLog("EVENT: " + event.getEventName());
        TrueHardcore.DebugLog("LOCATION: " + player.getLocation().toString());
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
        handlePlayerExit(event);
    }
    

    /*
     * Handle players joining the server in the hardcore world
     * Change their hardcore player state, or kick them out!
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (!plugin.IsHardcoreWorld(player.getWorld())) { return; }
    
        TrueHardcore.DebugLog("EVENT: " + event.getEventName());
        TrueHardcore.DebugLog("LOCATION: " + player.getLocation().toString());
        
        if (player.isDead()) {
            TrueHardcore.Debug(player.getName() + " joined " + player.getWorld() + " while dead! Ignoring event...");
            return;
        }
        // Check if player is resuming a game or somehow stuck in the world but not playing
        Location loc;
        HardcorePlayer hcp = HCPlayers.Get(player);
        if (hcp == null) {
            TrueHardcore.Warn(player.getName() + " joined in hardcore world with no player record!");
            loc = plugin.GetLobbyLocation(player, player.getWorld().getName());
        }
        else if ((hcp.getState() == PlayerState.ALIVE) || (hcp.getState() == PlayerState.IN_GAME)) {
            // Send player to game lobby
            TrueHardcore.Debug(player.getName() + " joined in " + player.getWorld().getName() + "! Returning player to lobby...");
            loc = plugin.GetLobbyLocation(player, hcp.getWorld());
            hcp.setState(PlayerState.ALIVE);
        } else {
            TrueHardcore.Warn(player.getName() + " joined in hardcore world with no game in progess (State=" + hcp.getState() + ")!");
            loc = plugin.GetLobbyLocation(player, hcp.getWorld());
        }
        
        // We need to send them away!
        if (loc != null) {
            // Riders must be ejected before teleport
            if (player.isInsideVehicle()) {
                TrueHardcore.Debug(player.getName() + " exiting vehicle...");
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
                    TrueHardcore.Warn("Unable to send " + player.getName() + " to lobby! Resuming game play...");
                    hcp.setState(PlayerState.IN_GAME);
                    plugin.SavePlayer(hcp);
                    if (plugin.IsPlayerVanished(player)) {
                        plugin.UnvanishPlayer(player);
                    }
                    plugin.BroadcastToHardcore(plugin.Header + ChatColor.GREEN + player.getDisplayName() + " has entered " + hcp.getWorld(), player.getName());
                } else {
                    TrueHardcore.Warn("Unable to send " + player.getName() + " to lobby and no player record!! THAT IS BAD!");
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
    
        TrueHardcore.DebugLog("EVENT: " + event.getEventName());
        TrueHardcore.DebugLog("LOCATION: " + player.getLocation().toString());
        
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
        final World worldFrom = from.getWorld();
        final World worldTo = to.getWorld();
        if(worldFrom == null || worldTo == null) {
            plugin.getLogger().warning("Teleport Event from or to a null world:");
            if (worldFrom != null) {
                plugin.getLogger().warning("Teleport TO was NULL");
            } else {
                plugin.getLogger().warning("Teleport FROM was NULL");
            }
            plugin.getLogger().warning(" Teleport Event: "+event.toString());
        } else {
            // Ignore if neither from/to are related to hardcore
            if (!plugin.IsHardcoreWorld(worldTo) && !plugin.IsHardcoreWorld(worldFrom)) {
                return;
            }
            TeleportCause cause = event.getCause();
            TrueHardcore.DebugLog(
                  "PlayerTeleportEvent (" + player.getName() + "): " +
                        worldFrom.getName() + " " + from.getX() + " " + from.getY() + " " + from.getZ() +
                        " [TO] " +
                        worldTo.getName() + " " + to.getX() + " " + to.getY() + " " + to.getZ() +
                        " (" + cause + ")"
            );

            // Some teleport methods are fine.. let them go
            if ((cause == TeleportCause.ENDER_PEARL) || (cause == TeleportCause.END_PORTAL) || (cause == TeleportCause.NETHER_PORTAL)) {
                return;
            }

            // Ignore block/chunk loading teleport glitches within the same world (or NoCheatPlus)
            if (worldFrom.equals(worldTo) && (from.distance(to) <= 30)) {
                return;
            }

            if (plugin.IsHardcoreWorld(worldFrom)) {
                // Prevent unauthorised teleports while in hardcore worlds
                HardcorePlayer hcp = HCPlayers.Get(worldFrom, player);
                if (hcp == null) {
                    return;
                }
                if (hcp.getState() == PlayerState.IN_GAME) {
                    if (worldFrom.equals(worldTo)) {
                        // Prevent unauthorised teleports within hardcore worlds
                        if (player.isOp() || player.hasPermission("truehardcore.bypass.teleport")) {
                            TrueHardcore.Debug("Teleport override (within world) allowed for " + player.getName());
                            return;
                        } else {
                            TrueHardcore.Debug(player.getName() + " teleport within hardcore cancelled!");
                            player.sendMessage(ChatColor.RED + "You are not allowed to teleport while in hardcore!");
                        }
                    } else {
                        // Prevent unauthorised exit from hardcore
                        if (player.isOp() || player.hasPermission("truehardcore.bypass.teleportout")) {
                            TrueHardcore.Debug("Teleport override (out of world) allowed for " + player.getName());
                            return;
                        } else {
                            TrueHardcore.Debug(player.getName() + " teleport out of hardcore cancelled!");
                            player.sendMessage(ChatColor.RED + "You are not allowed to teleport out of hardcore!");
                        }
                    }
                    player.sendMessage(ChatColor.GREEN + "Type " + ChatColor.AQUA + "/th leave" + ChatColor.GREEN + " to exit (progress will be saved)");
                    TrueHardcore.Debug("From: " + from);
                    TrueHardcore.Debug("To  : " + to);
                    event.setCancelled(true);
                }
            } else if (plugin.IsHardcoreWorld(worldTo)) {
                // Prevent unauthorised entry into hardcore worlds
                HardcorePlayer hcp = HCPlayers.Get(worldTo, player);
                if ((hcp == null) || (hcp.getState() != PlayerState.IN_GAME)) {
                    if (player.isOp() || player.hasPermission("truehardcore.bypass.teleportin")) {
                        TrueHardcore.Debug("Teleport override (into world) allowed for " + player.getName());
                    } else {
                        event.setCancelled(true);
                        TrueHardcore.Debug(player.getName() + "teleport into hardcore was cancelled!");
                        player.sendMessage(ChatColor.RED + "You are not allowed to teleport to a hardcore world.");
                    }
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
        checkDamagedPlayer(event);
    }
    
    private void checkDamagedPlayer(EntityDamageEvent event){
        if (!(event.getEntity() instanceof Player)) { return; }
        if (!plugin.IsHardcoreWorld(event.getEntity().getWorld())) { return; }
        
        Player player = (Player) event.getEntity();
        HardcorePlayer hcp = HCPlayers.Get(player);
        
        if ((hcp != null) && (hcp.isGodMode())) {
            event.setCancelled(true);
        }
    }
    
    /*
     * Tag a player causing damage to another or being damaged by another
     */


    @EventHandler(ignoreCancelled=true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity ent = event.getEntity();
        if (!plugin.IsHardcoreWorld(ent.getWorld())) { return; }

        if(ent.getLastDamageCause() instanceof EntityDamageByBlockEvent){
            if (!(ent instanceof Player)) return;
            TrueHardcore.DebugLog("EntityDeathByBlock: " + ent.getName() + " killed by " + ent.getLastDamageCause());

            // We only care about TNT events
            EntityDamageByBlockEvent causeB = (EntityDamageByBlockEvent) ent.getLastDamageCause();
            if (
                    (causeB == null) ||
                    (causeB.getDamager() == null) ||
                    ((causeB.getDamager().getType() != Material.TNT) && (causeB.getDamager().getType() != Material.TNT_MINECART))
                ) return;

            Location blockLoc = new Location(causeB.getDamager().getWorld(),causeB.getDamager().getX(),causeB.getDamager().getY(),causeB.getDamager().getZ());
            OfflinePlayer killed = ((Player) ent).getPlayer();
            String killedDisplayName = ((Player)ent).getDisplayName();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> findPlacer(blockLoc, blockLoc.getWorld().getName(), killed, killedDisplayName));
            return;
        }
        if (!(ent.getLastDamageCause() instanceof EntityDamageByEntityEvent)) { return; }

        // Find out who did the last damage
        EntityDamageByEntityEvent cause = (EntityDamageByEntityEvent) ent.getLastDamageCause();
        if (cause.isCancelled())return;
        Entity damager = cause.getDamager();
        if (damager instanceof Player) {
            Player killer = (Player) damager;
            HardcorePlayer hcp = HCPlayers.Get(killer);
            if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
                if (ent instanceof Player) {
                    Player killed = (Player) ent;
                    TrueHardcore.DebugLog("EntityDeath: " + killer.getName() + " killed " + killed.getName());
                    hcp.setPlayerKills(hcp.getPlayerKills()+1);
                    giveSkullOnline(killed,killed.getDisplayName(),killer);
                    plugin.BroadcastToAllServers(ChatColor.RED + killer.getDisplayName() + " has taken the life of " + killed.getDisplayName() + " and gained a trophy!");
                } else {
                    TrueHardcore.DebugLog("EntityDeath: " + killer.getName() + " killed " + ent.getType());
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
                TrueHardcore.DebugLog("Ignoring hardcore death: " + killer.getName() + " killed " + ent.getType());
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
    @EventHandler(ignoreCancelled = true)
    private void playerEnterPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("truehardcore.endteleport")){
            return;
        }
        if(event.getCause() == TeleportCause.END_PORTAL){
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Sorry, this portal destination is unavailable. Best get out of that fire quick you have temporary resistance!!");
            PotionEffect effect = new PotionEffect(PotionEffectType.FIRE_RESISTANCE,400,2,false,true);
            player.addPotionEffect(effect);
            player.playNote(event.getPlayer().getLocation(), Instrument.PIANO, Note.natural(1, Note.Tone.B));
        }
    }

    private void findPlacer(Location location,String worldName, OfflinePlayer killed,String displayName){
        if(!plugin.PrismHooked)return;//if Prism isnt loaded ...this wont work.
        QueryParameters parameters = new QueryParameters();
        parameters.setSpecificBlockLocation(location);
        parameters.setProcessType(PrismProcessType.LOOKUP);
        parameters.setLimit(1);
        parameters.addActionType("block-place", MatchRule.INCLUDE);
        ActionsQuery aq = new ActionsQuery(plugin.prism);
        QueryResult lookupResult = aq.lookup( parameters);
        if(lookupResult.getActionResults().size() > 0){
            Handler handle = lookupResult.getActionResults().get(0);
            UUID killerUUID = handle.getUUID();
            OfflinePlayer killer = Bukkit.getOfflinePlayer(killerUUID);
            Bukkit.getScheduler().runTask(plugin, () -> updateGame(worldName, killed, killer, displayName));

        }
    }

    private void updateGame(String worldName, OfflinePlayer killed, OfflinePlayer killer, String killedDN){
        HardcorePlayer hcp = HCPlayers.Get(worldName,killer.getUniqueId());
        if(hcp != null) {
            TrueHardcore.DebugLog("EntityDeath: " + killer.getName() + " killed " + killedDN);
            hcp.setPlayerKills(hcp.getPlayerKills()+1);
            if (killer.isOnline()) {
                Player killerOnline = Bukkit.getPlayer(killer.getUniqueId());
                if (killerOnline != null) giveSkullOnline(killed,killedDN, killerOnline);
            } else {
                giveSkullOffline(killed,killedDN,killer);
            }
        }
    }


    private void giveSkullOffline(OfflinePlayer killedName, String killedDN, OfflinePlayer killer){
        IOpenInv openInv = plugin.openInv;
        Player loadedKiller = openInv.loadPlayer(killer);
        openInv.retainPlayer(loadedKiller,plugin);
        giveSkull(killedName,killedDN,loadedKiller, false);
        openInv.releasePlayer(loadedKiller,plugin);
    }
    private void giveSkullOnline(OfflinePlayer killedName, String killedDN, Player killer){
        killer.getWorld().strikeLightningEffect(killer.getLocation());
        PotionEffect effect =  new PotionEffect(PotionEffectType.CONFUSION,5*20,0,false,true);
        killer.addPotionEffect(effect);
        giveSkull(killedName,killedDN,killer, true);
    }

    private void giveSkull(OfflinePlayer killed, String killedDN, Player killer, boolean isOnline){
        if (killer==null)return;
        ItemStack skull =new ItemStack(Material.PLAYER_HEAD,1);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if(skullMeta != null) {
            skullMeta.setOwningPlayer(killed);
            skullMeta.setDisplayName(killedDN);
            skullMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            skullMeta.addEnchant(Enchantment.LUCK, 1, true);
            List<String> loreList = new ArrayList<>();
            loreList.add("The Head of " + killedDN);
            Date date = new Date(System.currentTimeMillis());
            DateFormat df = DateFormat.getDateTimeInstance(2, 2);
            df.format(date);
            loreList.add("Killed on " + df.format(date) + " by " + killer.getDisplayName());
            skullMeta.setLore(loreList);
        }
        skull.setItemMeta(skullMeta);
        killer.getWorld().dropItem(killer.getLocation(),skull);
        try {
            ISpecialPlayerInventory inv = plugin.openInv.getSpecialInventory(killer, isOnline);
            Inventory binv = inv.getBukkitInventory();
            HashMap<Integer, ItemStack> left = binv.addItem(skull);
            if(!left.isEmpty()){
                TrueHardcore.DebugLog("Unable to add item to normal inventory: " + skull.toString());
                for (Map.Entry<Integer,ItemStack> e: left.entrySet()){
                    if(e.getKey()>0){
                            ISpecialEnderChest sec = plugin.openInv.getSpecialEnderChest(killer, isOnline);
                            Inventory enderInv = sec.getBukkitInventory();
                            ItemStack item = e.getValue();
                            item.setAmount(e.getKey());
                            HashMap<Integer, ItemStack> leftnew = enderInv.addItem(item);
                            if(!leftnew.isEmpty()){
                                TrueHardcore.Log("Unable to add item to enderchest: " + item.toString());
                            }
                        TrueHardcore.Log("Unable to add item to enderchest: " + item.toString());
                    }
                }
            }
        }catch (InstantiationException e){
            TrueHardcore.Log(e.getMessage());
        }

    }


}