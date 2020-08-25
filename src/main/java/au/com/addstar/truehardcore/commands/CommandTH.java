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

package au.com.addstar.truehardcore.commands;

import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.truehardcore.TrueHardcore;
import au.com.addstar.truehardcore.functions.Util;
import au.com.addstar.truehardcore.functions.WorldRollback;
import au.com.addstar.truehardcore.objects.ChunkStorage;
import au.com.addstar.truehardcore.objects.HardcorePlayers.HardcorePlayer;
import au.com.addstar.truehardcore.objects.HardcorePlayers.PlayerState;
import au.com.addstar.truehardcore.objects.HardcoreWorlds.HardcoreWorld;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CommandTH implements CommandExecutor {
    private final TrueHardcore plugin;

    public CommandTH(TrueHardcore instance) {
        plugin = instance;
    }

    /**
     * Handle the TH command.
     *
     * @param sender       the command sender
     * @param cmd          the commands
     * @param commandLabel labels
     * @param args         arguments
     * @return true on success
     */
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull Command cmd,
                             @NotNull String commandLabel, String[] args) {
        String action = "";
        if (args.length > 0) {
            action = args[0].toUpperCase();
        }
        Player player;
        switch (action) {
            case "PLAY":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.use")) {
                        return true;
                    }
                }
                if (args.length > 1) {
                    World world = plugin.getServer().getWorld(args[1]);
                    if (world == null) {
                        sender.sendMessage(ChatColor.RED + "Error: Unknown world!");
                        return true;
                    }

                    if (plugin.isHardcoreWorld(world)) {
                        if (sender instanceof Player) {
                            player = (Player) sender;
                            if (!plugin.isHardcoreWorld(player.getWorld())) {
                                plugin.playGame(world.getName(), player);
                            } else {
                                sender.sendMessage(ChatColor.RED
                                      + "Error: You are already in a hardcore world!");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Error: Must be an in game player.");

                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Error: That is not a hardcore world!");
                    }
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /th play <world>");
                }
                break;
            case "LEAVE":

                player = (Player) sender;
                final Location oldloc = player.getLocation();

                if (!plugin.isPlayerSafe(player, 5, 5, 5)) {
                    player.sendMessage(ChatColor.RED
                          + "It's not safe to leave.. there are monsters around..");
                    return true;
                }
                HardcorePlayer hcPlayer = plugin.hcPlayers.get(player);
                if (hcPlayer.isCombat() && hcPlayer.getCombatTime() > System.currentTimeMillis()) {
                    player.sendMessage(ChatColor.RED + "It's not safe to leave.. you are in combat "
                          + "until " + Util.long2Time(hcPlayer.getCombatTime()
                          - System.currentTimeMillis()));
                    return true;
                } else if (hcPlayer.isCombat() && (hcPlayer.getCombatTime()
                      < System.currentTimeMillis())) {
                    hcPlayer.setCombatTime(0);
                    hcPlayer.setCombat(false);
                }
                player.sendMessage(ChatColor.GOLD + "Teleportation will commence in "
                      + ChatColor.RED + "5 seconds" + ChatColor.GOLD + ". Don't move.");
                final Player player2 = player;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    Location newloc = player2.getLocation();
                    if (newloc.distance(oldloc) <= 1) {
                        plugin.leaveGame(player2);
                    } else {
                        player2.sendMessage(ChatColor.DARK_RED
                              + "Pending teleportation request cancelled.");
                    }
                }, 5 * 20L);
                break;
            case "INFO":

                HardcorePlayer hcp = null;
                if (args.length == 1) {
                    if (sender instanceof Player) {
                        if (!Util.requirePermission((Player) sender, "truehardcore.info")) {
                            return true;
                        }

                        player = (Player) sender;
                        hcp = plugin.hcPlayers.get(player);
                        if (hcp != null) {
                            hcp.updatePlayer(player);
                        } else {
                            sender.sendMessage(ChatColor.RED
                                  + "You must be in the hardcore world to use this command");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /th info <player> [world]");
                    }
                } else if (args.length == 2) {
                    if (sender instanceof Player) {
                        if (!Util.requirePermission((Player) sender, "truehardcore.info.other")) {
                            return true;
                        }
                    }
                    player = plugin.getServer().getPlayer(args[1]);
                    if (player != null) {
                        hcp = plugin.hcPlayers.get(player);
                        if (plugin.isHardcoreWorld(player.getWorld())) {
                            hcp.updatePlayer(player);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Error: Unknown player!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Unknown player");
                    }
                } else if (args.length == 3) {
                    if (sender instanceof Player) {
                        if (!Util.requirePermission((Player) sender, "truehardcore.info.other")) {
                            return true;
                        }
                    }
                    //noinspection deprecation
                    OfflinePlayer lookup = Bukkit.getOfflinePlayer(args[2]);
                    if (!lookup.hasPlayedBefore()) {
                        sender.sendMessage(ChatColor.RED + "Error: Unknown player!");
                    } else {
                        hcp = plugin.hcPlayers.get(args[1], lookup.getUniqueId());
                        if (hcp != null) {
                            player = plugin.getServer().getPlayer(args[2]);
                            if (player != null) {
                                if (plugin.isHardcoreWorld(player.getWorld())) {
                                    if (Objects.equals(args[1], player.getWorld().getName())) {
                                        hcp.updatePlayer(player);
                                    }
                                }
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Error: Unknown player!");
                        }
                    }
                }

                if (hcp != null) {
                    Integer gt;
                    if (hcp.getState() == PlayerState.IN_GAME) {
                        gt = (hcp.getGameTime() + hcp.timeDiff(hcp.getLastJoin(), new Date()));
                    } else {
                        gt = hcp.getGameTime();
                    }
                    String gametime = Util.long2Time(gt);

                    sender.sendMessage(ChatColor.GREEN + "Hardcore player information:");
                    sender.sendMessage(ChatColor.YELLOW + "Player: " + ChatColor.AQUA
                          + hcp.getPlayerName());
                    sender.sendMessage(ChatColor.YELLOW + "World: " + ChatColor.AQUA
                          + hcp.getWorld());
                    sender.sendMessage(ChatColor.YELLOW + "State: " + ChatColor.AQUA
                          + hcp.getState());
                    sender.sendMessage(ChatColor.YELLOW + "Game Time: " + ChatColor.AQUA
                          + gametime);
                    sender.sendMessage(ChatColor.YELLOW + "Current Level: " + ChatColor.AQUA
                          + hcp.getLevel());
                    sender.sendMessage(ChatColor.YELLOW + "Total Score: " + ChatColor.AQUA
                          + hcp.getScore());
                    sender.sendMessage(ChatColor.YELLOW + "Total Deaths: " + ChatColor.AQUA
                          + hcp.getDeaths());
                    sender.sendMessage(ChatColor.YELLOW + "Top Score: " + ChatColor.AQUA
                          + hcp.getTopScore());
                }
                break;
            case "DUMP":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                if (args.length == 1) {
                    for (Map.Entry<String, HardcorePlayer> entry : plugin.hcPlayers
                          .allRecords().entrySet()) {
                        hcp = entry.getValue();
                        if (hcp != null) {
                            sender.sendMessage(
                                  Util.padRight(entry.getKey(), 30)
                                        + Util.padLeft(hcp.getScore() + "", 8)
                                        + Util.padLeft(hcp.getTopScore() + "", 8)
                                        + "   " + hcp.getState());
                        } else {
                            TrueHardcore.warn("Record for key \"" + entry.getKey()
                                  + "\" not found! This should not happen!");
                        }
                    }
                } else if (args.length == 3) {
                    //noinspection deprecation
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);

                    hcp = plugin.hcPlayers.get(args[2], offlinePlayer.getUniqueId());
                    if (hcp != null) {
                        String sincedeath = "";
                        if (hcp.getGameEnd() != null) {
                            Date now = new Date();
                            long diff = (now.getTime() - hcp.getGameEnd().getTime()) / 1000;
                            sincedeath = Util.long2Time(diff);
                        }

                        sender.sendMessage(ChatColor.YELLOW + "Player Name    : " + ChatColor.AQUA
                              + hcp.getPlayerName());
                        sender.sendMessage(ChatColor.YELLOW + "World          : " + ChatColor.AQUA
                              + hcp.getWorld());
                        sender.sendMessage(ChatColor.YELLOW + "SpawnPos       : " + ChatColor.AQUA
                              + hcp.getSpawnPos());
                        sender.sendMessage(ChatColor.YELLOW + "LastPos        : " + ChatColor.AQUA
                              + hcp.getLastPos());
                        sender.sendMessage(ChatColor.YELLOW + "LastJoin       : " + ChatColor.AQUA
                              + hcp.getLastJoin());
                        sender.sendMessage(ChatColor.YELLOW + "LastQuit       : " + ChatColor.AQUA
                              + hcp.getLastQuit());
                        sender.sendMessage(ChatColor.YELLOW + "GameStart      : " + ChatColor.AQUA
                              + hcp.getGameStart());
                        sender.sendMessage(ChatColor.YELLOW + "GameEnd        : " + ChatColor.AQUA
                              + hcp.getGameEnd());
                        sender.sendMessage(ChatColor.YELLOW + "Since Death    : " + ChatColor.AQUA
                              + sincedeath);
                        sender.sendMessage(ChatColor.YELLOW + "GameTime       : " + ChatColor.AQUA
                              + hcp.getGameTime());
                        sender.sendMessage(ChatColor.YELLOW + "Level          : " + ChatColor.AQUA
                              + hcp.getLevel());
                        sender.sendMessage(ChatColor.YELLOW + "Exp            : " + ChatColor.AQUA
                              + hcp.getExp());
                        sender.sendMessage(ChatColor.YELLOW + "Score          : " + ChatColor.AQUA
                              + hcp.getScore());
                        sender.sendMessage(ChatColor.YELLOW + "TopScore       : " + ChatColor.AQUA
                              + hcp.getTopScore());
                        sender.sendMessage(ChatColor.YELLOW + "State          : " + ChatColor.AQUA
                              + hcp.getState());
                        sender.sendMessage(ChatColor.YELLOW + "GodMode        : " + ChatColor.AQUA
                              + hcp.isGodMode());
                        sender.sendMessage(ChatColor.YELLOW + "DeathMsg       : " + ChatColor.AQUA
                              + hcp.getDeathMsg());
                        sender.sendMessage(ChatColor.YELLOW + "DeathPos       : " + ChatColor.AQUA
                              + hcp.getDeathPos());
                        sender.sendMessage(ChatColor.YELLOW + "Deaths         : " + ChatColor.AQUA
                              + hcp.getDeaths());
                        sender.sendMessage(ChatColor.YELLOW + "Modified       : " + ChatColor.AQUA
                              + hcp.isModified());
                        outputKillScores(sender, hcp);
                        sender.sendMessage(ChatColor.YELLOW + "Player in Combat?   : "
                              + ChatColor.AQUA + hcp.isCombat() + " : "
                              + Util.long2Time(hcp.getCombatTime()));

                    }
                }
                break;
            case "DUMPWORLDS":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                if (args.length == 1) {
                    Map<UUID, List<ChunkStorage.TrueHardCoreChunk>> stored
                          = TrueHardcore.instance.chunkStorage.getSnapShot();
                    for (Map.Entry<String, HardcoreWorld> entry :
                          plugin.hardcoreWorlds.allRecords().entrySet()) {
                        HardcoreWorld hcw = entry.getValue();
                        sender.sendMessage(ChatColor.RED + "World Name     : " + ChatColor.AQUA
                              + hcw.getWorld().getName());
                        sender.sendMessage(ChatColor.YELLOW + " Greeting      : " + ChatColor.AQUA
                              + ChatColor.translateAlternateColorCodes('&',
                              hcw.getGreeting()));
                        sender.sendMessage(ChatColor.YELLOW + " Ban Time      : " + ChatColor.AQUA
                              + hcw.getBantime());
                        sender.sendMessage(ChatColor.YELLOW + " Distance      : " + ChatColor.AQUA
                              + hcw.getSpawnDistance());
                        sender.sendMessage(ChatColor.YELLOW + " Protection    : " + ChatColor.AQUA
                              + hcw.getSpawnProtection());
                        sender.sendMessage(ChatColor.YELLOW + " ExitPos       : " + ChatColor.AQUA
                              + hcw.getExitPos());
                        sender.sendMessage(ChatColor.YELLOW + " Rollback delay: " + ChatColor.AQUA
                              + hcw.getRollbackDelay());
                        sender.sendMessage(ChatColor.YELLOW + " DeathDrops    : " + ChatColor.AQUA
                              + hcw.getDeathDrops());
                        sender.sendMessage(ChatColor.YELLOW + " Whitelisted   : " + ChatColor.AQUA
                              + hcw.isWhitelisted());
                        List<ChunkStorage.TrueHardCoreChunk> chunks = stored.get(hcw.getWorld().getUID());
                        sender.sendMessage(ChatColor.YELLOW + " Chunks Held:" + chunks.size());
                        for (ChunkStorage.TrueHardCoreChunk chunk : chunks) {
                            sender.sendMessage(ChatColor.GRAY + "      Chunk: X:" + chunk.getX() + " Z:" + chunk.getZ()
                                  + " Expiry:" + chunk.getExpiry());
                        }
                    }
                }
                break;
            case "SET":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                if (args.length == 3) {
                    switch (args[1].toUpperCase()) {
                        case "EXIT":
                            if (sender instanceof Player) {

                                World world = plugin.getServer().getWorld(args[2]);
                                if ((plugin.isHardcoreWorld(world))) {
                                    HardcoreWorld hcw = plugin.hardcoreWorlds.get(world.getName());
                                    TrueHardcore.debug("Setting ExitPos for "
                                          + hcw.getWorld().getName());
                                    player = (Player) sender;
                                    hcw.setExitPos(player.getLocation());
                                    plugin.config().set("worlds." + world.getName()
                                          + ".exitpos", Util.loc2Str(player.getLocation()));
                                    plugin.saveConfig();
                                } else {
                                    sender.sendMessage(ChatColor.RED
                                          + "Not a valid hardcore world");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED
                                      + "Error: Must be an in game player.");
                            }
                            break;
                        case "COMBATLOG":
                            switch (args[2].toUpperCase()) {
                                case "ENABLE":
                                    TrueHardcore.getCfg().antiCombatLog = true;
                                    plugin.enableCombatLog(true);
                                    break;
                                case "DISABLE":
                                    TrueHardcore.getCfg().antiCombatLog = false;
                                    plugin.enableCombatLog(false);
                                    break;
                                default:
                                    throw new IllegalArgumentException(
                                          " Please use: Enable or Disable");
                            }
                            sender.sendMessage("AntiCombatLogging is now: "
                                  + TrueHardcore.getCfg().antiCombatLog);
                            break;
                        default:
                            sender.sendMessage(ChatColor.RED + "Invalid option \""
                                  + args[1] + "\"");
                            break;
                    }
                }
                break;
            case "LIST":
            case "WHO":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.list")) {
                        return true;
                    }
                }

                sender.sendMessage(ChatColor.GREEN + "Players currently in hardcore worlds:");

                // TODO: Fix this really ugly code!

                boolean playing = false;
                for (Map.Entry<String, HardcoreWorld> entry : plugin.hardcoreWorlds
                      .allRecords().entrySet()) {
                    // Check hardcore world
                    World world = plugin.getServer().getWorld(entry.getKey());
                    playing = outputPlayingForWorld(sender, playing, world);

                    // Corresponding nether world
                    world = plugin.getServer().getWorld(entry.getKey() + "_nether");
                    if (world != null) {
                        playing = outputPlayingForWorld(sender, playing, world);
                    }

                    // Corresponding end world
                    world = plugin.getServer().getWorld(entry.getKey() + "_the_end");
                    if (world != null) {
                        playing = outputPlayingForWorld(sender, playing, world);
                    }
                }
                if (!playing) {
                    sender.sendMessage(ChatColor.RED + "None");
                }
                break;
            case "STATS":
            case "KILLS":
                hcp = null;
                if (args.length == 1) {
                    if (sender instanceof Player) {
                        if (!Util.requirePermission((Player) sender, "truehardcore.stats")) {
                            return true;
                        }

                        player = (Player) sender;
                        hcp = plugin.hcPlayers.get(player);
                        if (hcp == null) {
                            sender.sendMessage(ChatColor.RED
                                  + "You must be in the hardcore world to use this command");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /th info <player> [world]");
                    }
                } else if (args.length == 2) {
                    if (sender instanceof Player) {
                        if (!Util.requirePermission((Player) sender,
                              "truehardcore.stats.other")) {
                            return true;
                        }
                    }
                    player = plugin.getServer().getPlayer(args[1]);
                    if (player != null) {
                        hcp = plugin.hcPlayers.get(player);
                        if (!plugin.isHardcoreWorld(player.getWorld())) {
                            sender.sendMessage(ChatColor.RED + "Error: Unknown player!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Unknown player");
                    }
                } else if (args.length == 3) {
                    if (sender instanceof Player) {
                        if (!Util.requirePermission((Player) sender,
                              "truehardcore.stats.other")) {
                            return true;
                        }
                    }
                    //noinspection deprecation
                    hcp = plugin.hcPlayers.get(args[2],
                          Bukkit.getOfflinePlayer(args[1]).getUniqueId());
                    if (hcp == null) {
                        sender.sendMessage(ChatColor.RED + "Error: Unknown player!");
                    }
                }

                if (hcp != null) {
                    sender.sendMessage(ChatColor.GREEN + "Hardcore player statistics:");
                    outputKillScores(sender, hcp);
                }
                break;
            case "WHITELIST":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED
                          + "Usage: /th whitelist <add|del|list> [player]");
                } else {
                    if (sender instanceof Player) {
                        if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                            return true;
                        }
                    }
                    String type = args[1].toUpperCase();
                    switch (type) {
                        case "ADD":
                            //noinspection deprecation
                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
                            if (offlinePlayer.getUniqueId().version() == 4) {
                                if (plugin.addToWhitelist(offlinePlayer.getUniqueId())) {
                                    sender.sendMessage(ChatColor.GREEN + "Player " + offlinePlayer
                                          .getName() + " added to TrueHardcore whitelist.");
                                } else {
                                    sender.sendMessage(ChatColor.RED
                                          + "ERROR: Failed to add player to whitelist!");
                                }
                            } else {
                                // We have to resolve this player

                                // We cannot do this lookup without there
                                // being at least one player on this server
                                boolean hasPlayers = false;
                                if (Bukkit.getOnlinePlayers().size() > 0) {
                                    hasPlayers = true;
                                }

                                if (!hasPlayers) {
                                    sender.sendMessage(ChatColor.RED + "ERROR: At least one player"
                                          + " is needed to be on this server to do a name lookup");
                                } else {
                                    final String name = args[2];

                                    Lookup.lookupPlayerName(name, (success, def, error) -> {
                                        if (success) {
                                            if (plugin.addToWhitelist(def.getUniqueId())) {
                                                sender.sendMessage(ChatColor.GREEN + "Player "
                                                      + def.getName()
                                                      + " added to TrueHardcore whitelist.");
                                            } else {
                                                sender.sendMessage(ChatColor.RED
                                                      + "ERROR: Failed to add player to whitelist!"
                                                );
                                            }
                                        } else {
                                            sender.sendMessage(ChatColor.RED
                                                  + "ERROR: Failed to lookup the UUID "
                                                  + "for that player: " + error.getMessage());
                                        }
                                    });
                                }
                            }
                            break;
                        case "LIST":
                        case "DEL":
                            sender.sendMessage(ChatColor.RED + "Not implemented yet!");
                            break;
                        default:
                            sender.sendMessage(ChatColor.RED
                                  + "Usage: /th whitelist <add|del|list> [player]");
                            break;
                    }
                }
                break;
            case "SAVE":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                TrueHardcore.debug("Saving buffered data...");
                plugin.saveAllPlayers();
                break;
            case "LOAD":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /th load <player> <world>");
                    return true;
                } else if (args.length == 3) {
                    //noinspection deprecation
                    if (plugin.loadPlayer(args[2],
                          Bukkit.getOfflinePlayer(args[1]).getUniqueId())) {
                        sender.sendMessage(ChatColor.GREEN + "Player record " + args[2] + "/"
                              + args[1] + " has been reloaded.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Player record failed to load!");
                    }
                }
                break;
            case "DISABLE":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                TrueHardcore.getCfg().gameEnabled = false;
                TrueHardcore.debugLog("TrueHardcore has been disabled.");
                sender.sendMessage(ChatColor.RED + "TrueHardcore has been disabled.");
                break;
            case "ENABLE":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                TrueHardcore.getCfg().gameEnabled = true;
                TrueHardcore.debugLog("TrueHardcore has been enabled.");
                sender.sendMessage(ChatColor.GREEN + "TrueHardcore has been enabled.");
                break;
            case "BCAST":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }

                if (args.length > 1) {
                    List<String> msg = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
                    plugin.broadCastToHardcore(plugin.header + StringUtils.join(msg, " "));
                } else {
                    sender.sendMessage(ChatColor.RED + "You must provide a message to broadcast");
                }
                break;
            case "DEBUG":

                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                TrueHardcore.getCfg().debugEnabled = !TrueHardcore.getCfg().debugEnabled;
                sender.sendMessage(ChatColor.RED + "Debug Status:" + TrueHardcore.getCfg().debugEnabled);
                break;
            case "QUEUE":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "Queue locked: "
                      + ChatColor.YELLOW + plugin.rollbackHandler.isQueueLocked());
                sender.sendMessage(ChatColor.GREEN + "Hardcore rollback queue:");
                for (int x = 0; x < plugin.rollbackHandler.getQueue().size(); x++) {
                    WorldRollback.RollbackRequest req = plugin.rollbackHandler.getQueue().get(x);
                    sender.sendMessage(ChatColor.RED + Integer.toString(x) + ": "
                          + ChatColor.AQUA + req.type + " "
                          + ChatColor.YELLOW + req.world.getName() + " "
                          + ChatColor.GREEN + req.player.getName() + " "
                          + ChatColor.WHITE + req.taskTime);
                }
                break;
            case "FORCEALIVE":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /th forcealive <player> <world>");
                    return true;
                } else if (args.length == 3) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("Player must be online to peform the force alive..."
                              + args[1] + " could not be found.");
                        return true;
                    }
                    if (TrueHardcore.instance.isHardcoreWorld(target.getWorld())) {
                        sender.sendMessage(target.getDisplayName() + " is in " + target.getWorld()
                              + " ask them to return to lobby before updating.");
                        return true;
                    }
                    if (plugin.loadPlayer(args[2], target.getUniqueId())) {
                        sender.sendMessage(ChatColor.GREEN + "Player record " + args[2] + "/"
                              + args[1] + " has been reloaded.");
                        HardcorePlayer hcplayer = TrueHardcore.instance.hcPlayers.get(args[2],
                              target.getUniqueId());
                        if (hcplayer == null) {
                            sender.sendMessage(ChatColor.RED + "Player record failed to load!");
                            return false;
                        }
                        if (hcplayer.getState() == PlayerState.IN_GAME) {
                            hcplayer.setLoadDataOnly(true);
                            hcplayer.setState(PlayerState.ALIVE);
                            hcplayer.setLoadDataOnly(false);
                        }
                        TrueHardcore.instance.savePlayer(hcplayer);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Player record failed to load!");
                    }
                }
                break;
            case "TP":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /th tp <player> <player>");
                    return true;
                }
                Player from = Bukkit.getPlayer(args[1]);
                Player to = Bukkit.getPlayer(args[2]);
                if (to != null && Util.teleport(from, to.getLocation())) {
                    sender.sendMessage(ChatColor.GREEN + "Teleported "
                          + from.getDisplayName() + " to " + to.getDisplayName());
                } else {
                    sender.sendMessage(ChatColor.RED + "Unable to teleport "
                          + from.getDisplayName() + " to " + to.getDisplayName());
                }
                break;
            case "ACCOUNT":
                if (sender instanceof Player) {
                    if (!Util.requirePermission((Player) sender, "truehardcore.admin")) {
                        return true;
                    }
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /th account <player> [primary|alt]");
                    return true;
                }

                //noinspection deprecation
                OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);
                if (!p.hasPlayedBefore()) {
                    sender.sendMessage(ChatColor.RED + "Player " + args[1] + " does not exist!");
                    return true;
                }

                if (args.length == 2) {
                    String type = TrueHardcore.instance.getAccountType(p.getUniqueId());
                    if (type != null) {
                        sender.sendMessage(ChatColor.GREEN + "Account " + p.getName()
                              + " has account type: " + ChatColor.AQUA + type);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Account " + p.getName()
                              + " has no existing account.");
                    }
                } else {
                    String type = args[2].toLowerCase();
                    if (type.equals("alt") || type.equals("primary")) {
                        TrueHardcore.instance.setAccountType(p.getUniqueId(), p.getName(), type);
                        sender.sendMessage(ChatColor.GREEN + "Account type for " + p.getName()
                              + " has been set to: " + ChatColor.AQUA + type);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /th account <player> [primary|alt]");
                    }
                }
                break;
            default:
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "TrueHardcore Commands:");
                sender.sendMessage(ChatColor.AQUA + "/th play       " + ChatColor.YELLOW
                      + ": Start or resume your game");
                sender.sendMessage(ChatColor.AQUA + "/th leave      " + ChatColor.YELLOW
                      + ": Exit the hardcore game");
                sender.sendMessage(ChatColor.AQUA + "/th list       " + ChatColor.YELLOW
                      + ": List the current hardcore players");
                sender.sendMessage(ChatColor.AQUA + "/th info       " + ChatColor.YELLOW
                      + ": Display your game information");
                sender.sendMessage(ChatColor.AQUA + "/th stats      " + ChatColor.YELLOW
                      + ": Display kill statistics");
                if (!(sender instanceof Player) || (Util.requirePermission((Player) sender,
                      "truehardcore.admin"))) {
                    sender.sendMessage(ChatColor.AQUA + "/th bcast      " + ChatColor.YELLOW
                          + ": Message all TH players");
                    sender.sendMessage(ChatColor.AQUA + "/th enable     " + ChatColor.YELLOW
                          + ": Enable TrueHardocre");
                    sender.sendMessage(ChatColor.AQUA + "/th disable    " + ChatColor.YELLOW
                          + ": Disable TrueHardcore");
                    sender.sendMessage(ChatColor.AQUA + "/th dump       " + ChatColor.YELLOW
                          + ": Dump player record");
                    sender.sendMessage(ChatColor.AQUA + "/th dumpworlds " + ChatColor.YELLOW
                          + ": Dump world records");
                    sender.sendMessage(ChatColor.AQUA + "/th save       " + ChatColor.YELLOW
                          + ": Save all in-memory changes");
                    sender.sendMessage(ChatColor.AQUA + "/th load       " + ChatColor.YELLOW
                          + ": Load player data from DB");
                    sender.sendMessage(ChatColor.AQUA + "/th whitelist  " + ChatColor.YELLOW
                          + ": Add/remove player to whitelist");
                    sender.sendMessage(ChatColor.AQUA + "/th account  " + ChatColor.YELLOW
                          + ": Manage account types");
                    sender.sendMessage(ChatColor.AQUA + "/th debug  " + ChatColor.YELLOW
                          + ": Toggle debug until restart");
                    sender.sendMessage(ChatColor.AQUA + "/th queue  " + ChatColor.YELLOW
                          + ": Show the hardcore rollback queue.");
                    sender.sendMessage(ChatColor.AQUA + "/th forcealive <player> <world>  "
                          + ChatColor.YELLOW
                          + ": Forces an update the the player - removing them from game and "
                          + "setting state to alive.");
                }
                break;
        }
        return true;
    }

    private boolean outputPlayingForWorld(CommandSender sender, boolean playing, World world) {
        if ((world != null) && (world.getPlayers().size() > 0)) {
            ArrayList<String> players = new ArrayList<>();
            for (Player p : world.getPlayers()) {
                if (plugin.isPlayerVanished(p)) {
                    continue;
                }
                playing = true;
                players.add(p.getDisplayName());
            }
            if (players.size() > 0) {
                sender.sendMessage(ChatColor.YELLOW + world.getName() + ": " + ChatColor.AQUA
                      + StringUtils.join(players, ChatColor.AQUA + ", "));
            }
        }
        return playing;
    }

    private void outputKillScores(CommandSender sender, HardcorePlayer hcp) {
        sender.sendMessage(ChatColor.YELLOW + "Cow Kills      : " + ChatColor.AQUA
              + hcp.getCowKills());
        sender.sendMessage(ChatColor.YELLOW + "Pig Kills      : " + ChatColor.AQUA
              + hcp.getPigKills());
        sender.sendMessage(ChatColor.YELLOW + "Sheep Kills    : " + ChatColor.AQUA
              + hcp.getSheepKills());
        sender.sendMessage(ChatColor.YELLOW + "Chicken Kills  : " + ChatColor.AQUA
              + hcp.getChickenKills());
        sender.sendMessage(ChatColor.YELLOW + "Creeper Kills  : " + ChatColor.AQUA
              + hcp.getCreeperKills());
        sender.sendMessage(ChatColor.YELLOW + "Zombie Kills   : " + ChatColor.AQUA
              + hcp.getZombieKills());
        sender.sendMessage(ChatColor.YELLOW + "Skeleton Kills : " + ChatColor.AQUA
              + hcp.getSkeletonKills());
        sender.sendMessage(ChatColor.YELLOW + "Spider Kills   : " + ChatColor.AQUA
              + hcp.getSpiderKills());
        sender.sendMessage(ChatColor.YELLOW + "Ender Kills    : " + ChatColor.AQUA
              + hcp.getEnderKills());
        sender.sendMessage(ChatColor.YELLOW + "Slime Kills    : " + ChatColor.AQUA
              + hcp.getSlimeKills());
        sender.sendMessage(ChatColor.YELLOW + "Moosh Kills    : " + ChatColor.AQUA
              + hcp.getMooshKills());
        sender.sendMessage(ChatColor.YELLOW + "Other Kills    : " + ChatColor.AQUA
              + hcp.getOtherKills());
        sender.sendMessage(ChatColor.YELLOW + "Player Kills   : " + ChatColor.AQUA
              + hcp.getPlayerKills());
    }
}
