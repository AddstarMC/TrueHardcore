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

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.truehardcore.HardcorePlayers.HardcorePlayer;
import au.com.addstar.truehardcore.HardcorePlayers.PlayerState;
import au.com.addstar.truehardcore.HardcoreWorlds.HardcoreWorld;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.lishid.openinv.IOpenInv;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.WorldBorder;
import me.botsko.prism.Prism;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.kitteh.vanish.VanishManager;
import org.kitteh.vanish.VanishPlugin;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public final class TrueHardcore extends JavaPlugin {
    public static TrueHardcore instance;

    private final Object lock = new Object();
    
    private static Economy econ = null;
    public static Permission perms = null;
    public static Chat chat = null;
    private boolean VaultEnabled = false;
    public static boolean DebugEnabled = false;
    public List<String> RollbackCmds = null;
    public boolean GameEnabled = true;
    public String BroadcastChannel = null;
    public boolean AutoSaveEnabled = false;
    public long combatTime = (30000);//Combat time in millseconds;
    public long baseChunkTime = 0; //chunkInhabited in ticks
    public CombatTracker cTracker;
    public boolean antiCombatLog = false;
    public WorldRollback RollbackHandler;

    private static final Logger logger = Logger.getLogger("Minecraft");
    private static final Logger debuglog = Logger.getLogger("DebugLog");
    private FileHandler debugfh;
    
    private final ConfigManager cfg = new ConfigManager(this);
    private static PluginDescriptionFile pdfFile = null;
    private PluginManager pm = null;

    private Database dbcon = null;
    String DBHost;
    String DBPort;
    String DBName;
    String DBUser;
    String DBPass;

    private Boolean LWCHooked = false;
    Boolean PrismHooked = false;
    Boolean OIHooked = false;
    IOpenInv openInv;
    private Boolean WBHooked = false;
    private Boolean VNPHooked = false;
    private Boolean BCHooked = false;
    
    private LWC lwc;
    Prism prism;
    private WorldBorder wb;
    private VanishManager vnp;

    // Hardcore worlds
    public final HardcoreWorlds HardcoreWorlds = new HardcoreWorlds();
    
    // Data for ALL hardcore players
    public final HardcorePlayers HCPlayers = new HardcorePlayers();

    public final String Header = ChatColor.DARK_RED + "[" + ChatColor.RED + "TrueHardcore" + ChatColor.DARK_RED + "] " + ChatColor.YELLOW;
    
    private final List<Material> SpawnBlocks = Arrays.asList(
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.PODZOL,
            Material.GRASS,
            Material.GRASS_BLOCK,
            Material.GRASS_PATH,
            Material.SAND,
            Material.SANDSTONE,
            Material.SMOOTH_SANDSTONE,
            Material.SMOOTH_RED_SANDSTONE,
            Material.SMOOTH_STONE,
            Material.STONE,
            Material.DIORITE,
            Material.COBBLESTONE,
            Material.SMOOTH_STONE,
            Material.BEDROCK,
            Material.SNOW,
            Material.SNOW_BLOCK,
            Material.CLAY,
            Material.TERRACOTTA,
            Material.ICE,
            Material.PACKED_ICE,
            Material.BLUE_ICE,
            Material.RED_SAND,
            Material.RED_SANDSTONE,
            Material.CUT_SANDSTONE
        
            );
    
    @Override
    public void onEnable(){
        instance = this;

        // This block configure the logger with handler and formatter
        try {
            debuglog.setUseParentHandlers(false);
            debugfh = new FileHandler("plugins/TrueHardcore/debug.log", true);
            Util.LogFormatter formatter = new Util.LogFormatter();
            debugfh.setFormatter(formatter);
            debuglog.addHandler(debugfh);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        // Grab that plugin manager!
        pdfFile = this.getDescription();
        pm = this.getServer().getPluginManager();

        // Check if vault is loaded (required for economy)
        VaultEnabled = setupEconomy();
        if (VaultEnabled) {
            Log("Found Vault! Hooking for economy!");
        } else {
            Log("Vault was not detected! Economy rewards are not available.");
        }
        
        Plugin p = pm.getPlugin("LWC");
        if (p instanceof LWCPlugin) {
            LWCHooked = true;
            lwc = ((LWCPlugin)p).getLWC();
            Log("LWC Found, hooking into LWC.");
        } else {
            LWCHooked = false;
            Log("LWC not Found");
        }
        p = pm.getPlugin("Prism");
        if (p instanceof Prism) {
            PrismHooked = true;
            prism = (Prism)p;
            Log("Prism found, hooking it.");
            RollbackHandler = new WorldRollback(prism);
        } else {
            PrismHooked = false;
            Log("Prism not found! This won't work very well...");
        }

        p = pm.getPlugin("WorldBorder");
        if (p instanceof WorldBorder) {
            WBHooked = true;
            wb = WorldBorder.plugin;
            Log("WorldBorder found, hooking it.");
        } else {
            WBHooked = false;
            Log("WorldBorder not found! Spawning will not be limited...");
        }

        p = pm.getPlugin("BungeeChatBukkit");
        if (p instanceof BungeeChat) {
            BCHooked = true;
            Log("BungeeChat found, hooking it.");
        } else {
            BCHooked = false;
            Log("BungeeChat not found! No cross server messages");
        }
        
        p = pm.getPlugin("VanishNoPacket");
        if (p instanceof VanishPlugin) {
            VNPHooked = true;
            vnp = ((VanishPlugin)p).getManager();
            Log("VanishNoPacket found, hooking it.");
        } else {
            WBHooked = false;
            Log("VanishNoPacket not found! Vanished players will not be unvanished...");
        }
        OIHooked = checkOpenInventory();
        // Read (or initialise) plugin config file
        cfg.LoadConfig(getConfig());

        // Save the default config (if one doesn't exist)
        saveDefaultConfig();

        // Open/initialise the database
        dbcon = new Database(this);
        if (dbcon.IsConnected) {
            Log("Successfully connected to the database.");
            Log("Loading players from database...");
            LoadAllPlayers();
        } else {
            Log(pdfFile.getName() + " " + pdfFile.getVersion() + " could not be enabled!");
            this.setEnabled(false);
            return;
        }
        
        Log("Registering commands and events...");
        getCommand("truehardcore").setExecutor(new CommandTH(this));
        getCommand("th").setExecutor(new CommandTH(this));
        pm.registerEvents(new PlayerListener(this), this);
        if(baseChunkTime>0){
            pm.registerEvents(new ChunkListener(baseChunkTime), p);
        }

        //enable combatlog if true
        enableCombatLog(antiCombatLog);
        // Set auto save timer
        if (AutoSaveEnabled) {
            Log("Launching auto-save timer (every 5 minutes)...");
            getServer().getScheduler().runTaskTimer(this, this::SaveIngamePlayers, 300 * 20L, 300 * 20L);
        }

        Log(pdfFile.getName() + " " + pdfFile.getVersion() + " has been enabled");
    }

    protected void enableCombatLog(boolean enable) {
        if (enable) {
            if (cTracker == null) {
                cTracker = new CombatTracker(this);
                pm.registerEvents(cTracker, this);
            }
        } else {
            if (cTracker != null) {
                cTracker.onDisable();
            }
            cTracker = null;
        }
        Log("Combat Logging is " + antiCombatLog);

    }
    
    @Override
    public void onDisable(){
        // cancel all tasks we created
        if(cTracker!= null)cTracker.onDisable();
        getServer().getScheduler().cancelTasks(this);
        SaveAllPlayers();
        Log(pdfFile.getName() + " has been disabled!");
        debugfh.close();
        
    }
    
    /*
     * Detect/configure Vault
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean checkOpenInventory() {
        Plugin p = getServer().getPluginManager().getPlugin("OpenInv");
        if(p == null){
            Log("Open Inventory support disabled");
            return false;
        }else{
            if(p instanceof IOpenInv){
                openInv = (IOpenInv) p;
                Log("Open Inventory support enabled");
                return true;
            }
        }
        return false;
    }
    
    public static void Log(String data) {
        logger.info("[" + pdfFile.getName() + "] " + data);
        debuglog.info(data);
    }

    public static void Warn(String data) {
        logger.warning("[" + pdfFile.getName() + "] " + data);
        debuglog.warning(data);
    }
    
    public static void Debug(String data) {
        if (DebugEnabled) {
            logger.info("[" + pdfFile.getName() + "] " + data);
        }
        debuglog.info(data);
    }

    // Write data to debug log
    public static void DebugLog(String data) {
        debuglog.info(data);
    }
    
    public FileConfiguration Config() {
        return getConfig();
    }
    
    public boolean GiveMoney(OfflinePlayer player, int money) {
        if (VaultEnabled) {
            EconomyResponse resp = econ.depositPlayer(player, money);
            if (resp.type == ResponseType.SUCCESS) {
                Log(player + " has been given $" + resp.amount + " (new balance $" + resp.balance + ")");
                return true;
            } else {
                Warn("Vault payment failed! Error: " + resp.errorMessage);
            }
        }
        return false;
    }
    
    public void handlePlayerCombatLogging(final Player player,Location location){
        PlayerInventory inv = player.getInventory();
        if(location.getBlock().isEmpty()){
            BlockData data = Bukkit.createBlockData(Material.CHEST);
            location.getBlock().setBlockData(data);
            Block block = location.getBlock();
            if(block instanceof Chest){
                ((Chest) block).getInventory().addItem(inv.getContents());
               inv.clear();
                BroadcastToHardcore(player.getDisplayName() + " has logged out in combat and left behind goodies. Find them if you can!");
            }
        }
    }
    
    
    public void DoPlayerDeath(final Player player, PlayerDeathEvent event) {
        final World realworld = player.getWorld();
        
        final HardcorePlayer hcp = HCPlayers.Get(realworld, player);
        final World world = getServer().getWorld(hcp.getWorld());
        final HardcoreWorld hcw = HardcoreWorlds.Get(world.getName());

        hcp.setState(PlayerState.DEAD);
        hcp.setCombat(false);
        hcp.setCombatTime(0);
        hcp.setDeathMsg(event.getDeathMessage());
        hcp.setDeathPos(player.getLocation());
        hcp.setDeaths(hcp.getDeaths()+1);
        hcp.updatePlayer(player);
        hcp.calcGameTime();
        
        String DeathMsg = event.getDeathMessage();
        DeathMsg = DeathMsg.replaceFirst(player.getName(), ChatColor.AQUA + player.getName() + ChatColor.YELLOW);
        BroadcastToAllServers(Header + DeathMsg + "!");
        BroadcastToAllServers(
                Header +
                "Final Score: " + ChatColor.GREEN + player.getTotalExperience() + " " +
                ChatColor.AQUA + "(" + hcp.getWorld() + ")"
        );
        event.setDeathMessage(null);
        if (hcp.getScore() > 0) {
            // Check if this is the player's personal best
            boolean personalbest = false;
            if (hcp.getScore() > hcp.getTopScore()) {
                hcp.setTopScore(hcp.getScore());
                personalbest = true;
            }

            // Check if this is a high score
            boolean highscore = true;
            for (Map.Entry<String, HardcorePlayer> entry: HCPlayers.AllRecords().entrySet()) {
                HardcorePlayer h = entry.getValue();
                if (h != null) {
                    // Only compare other player's scores in the same world
                    if ((h.getWorld().equals(hcp.getWorld())) && (!Objects.equals(h.getPlayerName(), hcp.getPlayerName()))) {
                        if (h.getTopScore() >= hcp.getScore()) {
                            highscore = false;
                            Debug(hcp.getPlayerName() + "'s score (" + hcp.getScore() + ") did not beat " + h.getPlayerName() + " (" + h.getTopScore() + ")");
                            break;
                        }
                    }
                } else {
                    Warn("Record for key \"" + entry.getKey() + "\" not found! This should not happen!");
                }
            }
            
            if (highscore) {
                BroadcastToAllServers(Header + ChatColor.AQUA + player.getName() + ChatColor.GREEN + " has beaten the all time high score!");
            }
            else if (personalbest) {
                player.sendMessage(ChatColor.GREEN + "Congratulations! You just beat your personal high score!");
            }
        }
        
        SavePlayer(hcp);

        // Dont drop XP or items
        if (!hcw.  getDeathDrops()) {
            event.setDroppedExp(0);
            event.getDrops().clear();
        }
        
        // Reset XP levels
        event.setNewExp(0);
        event.setNewLevel(0);
        event.setNewTotalExp(0);
        event.setKeepLevel(false);
        player.setLevel(0);
        player.setExp(0);
        
        if (hcw.getRollbackDelay() > 0) {
            String wh = ChatColor.DARK_RED + "[" + ChatColor.RED + hcp.getWorld() + ChatColor.DARK_RED + "] " + ChatColor.YELLOW;
            BroadcastToWorld(hcp.getWorld(), wh + " " +
                            ChatColor.YELLOW + "You now have " + Util.Long2Time(hcw.getRollbackDelay()) +
                            " to raid " + ChatColor.AQUA + player.getName() + "'s " + ChatColor.YELLOW + "stuff before it all disappears!");
        }
        
        instance.getServer().getScheduler().runTaskLater(instance, () -> {
            try {
                if (instance.LWCHooked) {
                    // Always remove the locks straight away!
                    Debug("Removing LWC locks...");
                    int count = 0;
                    if (lwc.getPhysicalDatabase() != null) {
                        List<Protection> prots = lwc.getPhysicalDatabase().loadProtectionsByPlayer(player.getUniqueId().toString());
                        String w = world.getName();
                        for(Protection prot : prots) {
                            if (prot.getWorld().equals(w) || prot.getWorld().equals(w + "_nether")) {
                                count++;

                                // Remove LWC protection
                                prot.remove();
                                prot.removeCache();
                            }
                        }
                    } else {
                        Log("WARNING: LWC.getPhysicalDatabase() failed!");
                    }
                    Debug("Removed " + count + " LWC protections.");
                }

                if (PrismHooked) {
                    // Queue rollback for the Overworld
                    RollbackHandler.QueueRollback("ROLLBACK", player, world, hcw.getRollbackDelay());

                    // Queue rollback for The Nether
                    World netherworld = instance.getServer().getWorld(world.getName() + "_nether");
                    if (netherworld != null) {
                        RollbackHandler.QueueRollback("ROLLBACK", player, netherworld, hcw.getRollbackDelay());
                    }
                }
            } catch (Exception e) {
                // Do nothing or throw an error if you want
                e.printStackTrace();
            }
        }, 20L);
    }
    
    public boolean PlayGame(String world, Player player) {
        // Only check whitelist if world is whitelisted
        HardcoreWorld hcw = HardcoreWorlds.Get(world);
        if (hcw.isWhitelisted()) {
            if (!IsOnWhiteList(world, player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Sorry, you are not allowed to play this world.");
                return false;
            }
        }
        if (!GameEnabled && !Util.HasPermission(player, "truehardcore.admin")) {
            player.sendMessage(ChatColor.RED + "TrueHardcore is currently disabled.");
            return false;
        }
        
        HardcorePlayer hcp = HCPlayers.Get(world, player.getUniqueId());
        if (hcp != null) {
            if ((hcp.getState() == PlayerState.DEAD) && (hcp.getGameEnd() != null)) {
                // Check last death time
                Date now = new Date();
                long diff = (now.getTime() - hcp.getGameEnd().getTime()) / 1000;
                long wait = (hcw.getBantime() - diff);
                
                if (wait > 0) {
                    player.sendMessage(ChatColor.RED + "Sorry, you must wait " + Util.Long2Time(wait) + " to play " + hcw.getWorld().getName() + " again.");
                    return false;
                }
            }
        }
        
        if ((hcp == null) || (hcp.getState() == PlayerState.DEAD)) {
            player.sendMessage(ChatColor.YELLOW + "Finding a new spawn location.. please wait..");
            Location spawn = null;
            World w = getServer().getWorld(world);

            // Never played before... create them!
            if (hcp == null) {
                Debug("New hardcore player: " + player.getName() + " (" + world + ")");
                hcp = HCPlayers.NewPlayer(world, player.getUniqueId(), player.getName());
                spawn = GetNewLocation(w, 0, 0, hcw.getSpawnDistance());
            }
            else if (hcp.getDeathPos() == null) {
                Warn("No previous position found for known " + player.getName());
                spawn = GetNewLocation(w, 0, 0, hcw.getSpawnDistance());
            } else {
                Debug(player.getName() + " is restarting game (" + world + ")");
                spawn = GetNewLocation(w, hcp.getDeathPos().getBlockX(), hcp.getDeathPos().getBlockZ(), hcw.getSpawnDistance());
            }

            if (spawn != null) {
                hcp.setState(PlayerState.IN_GAME);
                if (NewSpawn(player, spawn)) {
                    SetProtected(hcp, hcw.getSpawnProtection());
                    hcp.setGameTime(0);
                    hcp.setChickenKills(0);
                    hcp.setCowKills(0);
                    hcp.setPigKills(0);
                    hcp.setSheepKills(0);
                    hcp.setChickenKills(0);
                    hcp.setCreeperKills(0);
                    hcp.setZombieKills(0);
                    hcp.setSkeletonKills(0);
                    hcp.setSpiderKills(0);
                    hcp.setEnderKills(0);
                    hcp.setSlimeKills(0);
                    hcp.setMooshKills(0);
                    hcp.setOtherKills(0);
                    hcp.setPlayerKills(0);
                    hcp.updatePlayer(player);
                    SavePlayer(hcp);
                    cleanAndGreet(player,world);
                    BroadcastToHardcore(Header + ChatColor.GREEN + player.getDisplayName() + " has " + ChatColor.AQUA + "started " + ChatColor.GREEN + hcp.getWorld(), player.getName());
                    return true;
                } else {
                    return false;
                }
            } else {
                player.sendMessage(ChatColor.RED + "Unable to find suitable spawn location. Please try again.");
                Warn("Unable to find suitable spawn location for " + player.getName() + " (" + world + ")");
                return false;
            }
        }
        else if (hcp.getState() == PlayerState.IN_GAME) {
            player.sendMessage(ChatColor.RED + "You are already playing hardcore!");
            return false;
        } else {
            // Resume existing game
            player.sendMessage(ChatColor.GREEN + "Returning to your last hardcore location... good luck!");
            Debug(player.getName() + " is returning to " + hcw.getWorld().getName());
            hcp.setState(PlayerState.IN_GAME);
            JoinGame(world, player);
            SavePlayer(hcp);
            cleanAndGreet(player,world);
            return true;
        }
    }

    private void cleanAndGreet(Player player,String world){
        UnvanishPlayer(player);
        String greeting = HardcoreWorlds.Get(world).getGreeting();
        if ((greeting != null) && (!greeting.isEmpty())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', greeting));
        }
    }
    
    private boolean NewSpawn(Player player, Location spawn) {
        HardcorePlayer hcp = HCPlayers.Get(spawn.getWorld(), player);
        
        if (Util.Teleport(player, spawn)) {
            hcp.setState(PlayerState.IN_GAME);
            hcp.setSpawnPos(spawn);
            player.setFallDistance(0);
            player.setHealth(20.0D);
            player.setFoodLevel(20);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setExp(0);
            player.setLevel(0);
            player.setTotalExperience(0);
            player.setWalkSpeed(0.2F);
            player.setFlySpeed(0.2F);
            player.setGameMode(GameMode.SURVIVAL);
            player.setOp(false);
            player.getEnderChest().clear();
            player.getEquipment().clear();
            player.getInventory().clear();
            player.eject();
            player.sendMessage(ChatColor.RED + "!!!! WARNING !!!! WARNING !!!!");
            player.sendMessage(ChatColor.RED + "This plugin is highly experimental! Use at own risk!");
            player.sendMessage(ChatColor.RED + "Please report ALL problems in detail.");
            player.sendMessage(ChatColor.GREEN + "Welcome to TrueHardcore. Good luck on your adventure!");
            player.sendMessage(ChatColor.GREEN + "Type " + ChatColor.AQUA + "/th leave" + ChatColor.GREEN + " to exit (progress will be saved)");
            return true;
        } else {
            Warn("Teleport failed!");
            return false;
        }
    }
    
    private Location GetNewLocation(World world, int oldX, int oldZ, int dist) {
        Location l = new Location(world, oldX, 255, oldZ);
        Debug("Selecting spawn point " + dist + " blocks from: " + l.getBlockX() + " / " + l.getBlockY() + " / " + l.getBlockZ());

        double x;
        double z;
        int deg;
        Location nl = null;

        // Only try to find a good place 30 times
        for (int count = 0; count < 30; count++) {
            boolean GoodSpawn = false;
            Location spawn = null;
            String reason = "";

            // Lets do some trig!!
            dist = dist + (int) (Math.random() * 100);									// Random radius padding
            deg = (int) (Math.random() * 360);											// Random degrees
            x = (dist * Math.cos(Math.toRadians(deg))) + l.getBlockX();
            z = (dist * Math.sin(Math.toRadians(deg))) + l.getBlockZ();
            nl = new Location(world, x, 255, z);

            // Get the highest block at the selected location
            Block b = nl.getBlock();
            while((b.getType() == Material.AIR) && (b.getY() > 1)) {
                b = b.getRelative(BlockFace.DOWN);
            }

            spawn = new Location(b.getWorld(), b.getX(), b.getY()+2, b.getZ());
            if (SpawnBlocks.contains(b.getType())) {
                if (spawn.getBlockX() >= 0) { spawn.setX(spawn.getBlockX() + 0.5); }
                if (spawn.getBlockX() < 0)  { spawn.setX(spawn.getBlockX() - 0.5); }

                if (spawn.getBlockZ() >= 0) { spawn.setZ(spawn.getBlockZ() + 0.5); }
                if (spawn.getBlockZ() < 0)  { spawn.setZ(spawn.getBlockZ() - 0.5); }

                // Make sure it's inside the world border (if one exists)
                if (InsideWorldBorder(spawn)) {
                    GoodSpawn = true;
                    reason = "Allowed block type (" + b.getType() + ")!";
                } else {
                    reason = "Outside world border";
                }
            } else {
                reason = "Wrong block type (" + b.getType() + ")";
            }
            
            if (GoodSpawn) {
                Debug("GOOD: "
                        + Util.padLeft(String.valueOf(spawn.getX()), 9)
                        + Util.padLeft(String.valueOf(spawn.getY()), 7)
                        + Util.padLeft(String.valueOf(spawn.getZ()), 9)
                        + "   (" + dist + " blocks away)"
                        + "  => " + reason);

                // Return the good location
                spawn.setPitch(0F);
                spawn.setYaw(0F);
                return spawn;
            } else {
                Debug("BAD : "
                        + Util.padLeft(String.valueOf(spawn.getX()), 9)
                        + Util.padLeft(String.valueOf(spawn.getY()), 7)
                        + Util.padLeft(String.valueOf(spawn.getZ()), 9)
                        + "   (" + dist + " blocks away)"
                        + "  => " + reason);
            }
        }
        
        return null;
    }
    
    public void LeaveGame(Player player) {
        HardcorePlayer hcp = HCPlayers.Get(player);
        if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
            if (!player.isInsideVehicle()) {
                // We have to change the game state to allow the teleport out of the world
                hcp.setState(PlayerState.ALIVE);
                hcp.updatePlayer(player);
                if (Util.Teleport(player, GetLobbyLocation(player, hcp.getWorld()))) {
                    BroadcastToHardcore(Header + ChatColor.YELLOW + player.getDisplayName() + " has left " + hcp.getWorld(), player.getName());
                    hcp.calcGameTime();
                    SavePlayer(hcp);
                } else {
                    // Teleport failed so set the game state back
                    hcp.setState(PlayerState.IN_GAME);
                    player.sendMessage(ChatColor.RED + "Teleportation failed.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "You cannot leave while you are a passenger.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "You are not currently in a hardcore game.");
        }
    }
    
    public BukkitTask SavePlayer(HardcorePlayer hcp) {
        return SavePlayer(hcp, true, false);
    }
    
    private BukkitTask SavePlayer(HardcorePlayer hcp, boolean Async) {
        return SavePlayer(hcp, Async, false);
    }
    
    private BukkitTask SavePlayer(HardcorePlayer hcp, boolean Async, final boolean AutoSave) {
        if (hcp == null) {
            Warn("SavePlayer called with null record!");
            return null;
        }

        if (AutoSave) {
            DebugLog("Auto-saving data for " + hcp.getWorld() + "/" + hcp.getPlayerName());
        } else {
            Debug("Saving data for " + hcp.getWorld() + "/" + hcp.getPlayerName());
        }

        // CowKills, PigKills, SheepKills, ChickenKills;
        // CreeperKills, ZombieKills, SkeletonKills, SpiderKills, EnderKills, SlimeKills;
        // OtherKills, PlayerKills;
        
        final String query = "INSERT INTO `players` \n" +
                "(`id`, `player`, `world`, `spawnpos`, `lastpos`, `lastjoin`, `lastquit`, `gamestart`, `gameend`, `gametime`,\n" +
                "`level`, `exp`, `score`, `topscore`, `state`, `deathmsg`, `deathpos`, `deaths`,\n" +
                "`cowkills`, `pigkills`, `sheepkills`, `chickenkills`, `creeperkills`, `zombiekills`, `skeletonkills`,\n" +
                "`spiderkills`, `enderkills`, `slimekills`, `mooshkills`, `otherkills`, `playerkills`)\n\n" +
                
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE \n\n" +
                
                "`spawnpos`=?, `lastpos`=?, `lastjoin`=?, `lastquit`=?, `gamestart`=?, `gameend`=?, `gametime`=?,\n" +
                "`level`=?, `exp`=?, `score`=?, `topscore`=?, `state`=?, `deathmsg`=?, `deathpos`=?, `deaths`=?,\n" +
                "`cowkills`=?, `pigkills`=?, `sheepkills`=?, `chickenkills`=?, `creeperkills`=?, `zombiekills`=?, `skeletonkills`=?,\n" +
                "`spiderkills`=?, `enderkills`=?, `slimekills`=?, `mooshkills`=?, `otherkills`=?, `playerkills`=?\n";
                
        final String[] values = {
                hcp.getUniqueId().toString(),
                hcp.getPlayerName(),
                hcp.getWorld(),
                Util.Loc2Str(hcp.getSpawnPos()),
                Util.Loc2Str(hcp.getLastPos()),
                Util.Date2Mysql(hcp.getLastJoin()),
                Util.Date2Mysql(hcp.getLastQuit()),
                Util.Date2Mysql(hcp.getGameStart()),
                Util.Date2Mysql(hcp.getGameEnd()),
                String.valueOf(hcp.getGameTime()),
                String.valueOf(hcp.getLevel()),
                String.valueOf(hcp.getExp()),
                String.valueOf(hcp.getScore()),
                String.valueOf(hcp.getTopScore()),
                hcp.getState().toString(),
                hcp.getDeathMsg(),
                Util.Loc2Str(hcp.getDeathPos()),
                String.valueOf(hcp.getDeaths()),

                String.valueOf(hcp.getCowKills()),
                String.valueOf(hcp.getPigKills()),
                String.valueOf(hcp.getSheepKills()),
                String.valueOf(hcp.getChickenKills()),
                String.valueOf(hcp.getCreeperKills()),
                String.valueOf(hcp.getZombieKills()),
                String.valueOf(hcp.getSkeletonKills()),
                String.valueOf(hcp.getSpiderKills()),
                String.valueOf(hcp.getEnderKills()),
                String.valueOf(hcp.getSlimeKills()),
                String.valueOf(hcp.getMooshKills()),
                String.valueOf(hcp.getOtherKills()),
                String.valueOf(hcp.getPlayerKills()),

                // REPEATED FOR UPDATE!
                Util.Loc2Str(hcp.getSpawnPos()),
                Util.Loc2Str(hcp.getLastPos()),
                Util.Date2Mysql(hcp.getLastJoin()),
                Util.Date2Mysql(hcp.getLastQuit()),
                Util.Date2Mysql(hcp.getGameStart()),
                Util.Date2Mysql(hcp.getGameEnd()),
                String.valueOf(hcp.getGameTime()),
                String.valueOf(hcp.getLevel()),
                String.valueOf(hcp.getExp()),
                String.valueOf(hcp.getScore()),
                String.valueOf(hcp.getTopScore()),
                hcp.getState().toString(),
                hcp.getDeathMsg(),
                Util.Loc2Str(hcp.getDeathPos()),
                String.valueOf(hcp.getDeaths()),
                
                String.valueOf(hcp.getCowKills()),
                String.valueOf(hcp.getPigKills()),
                String.valueOf(hcp.getSheepKills()),
                String.valueOf(hcp.getChickenKills()),
                String.valueOf(hcp.getCreeperKills()),
                String.valueOf(hcp.getZombieKills()),
                String.valueOf(hcp.getSkeletonKills()),
                String.valueOf(hcp.getSpiderKills()),
                String.valueOf(hcp.getEnderKills()),
                String.valueOf(hcp.getSlimeKills()),
                String.valueOf(hcp.getMooshKills()),
                String.valueOf(hcp.getOtherKills()),
                String.valueOf(hcp.getPlayerKills())
        };
        hcp.setModified(false);

        Runnable savetask = () -> {
            try {
                int result = dbcon.PreparedUpdate(query, values, AutoSave);
                if (result < 0) {
                    Debug("Player record save failed!");
                    Debug("Query: " + query);
                    Debug("Values: " + Arrays.toString(values));
                }
            }
            catch (Exception e) {
                Debug("Unable to save player record to database!");
                Debug("Query: " + query);
                Debug("Values: " + Arrays.toString(values));
                e.printStackTrace();
            }
        };
        
        BukkitTask task = null;
        if (Async) {
            if (!AutoSave) Debug("Launching async save task...");
            task = getServer().getScheduler().runTaskAsynchronously(this, savetask);
        } else {
            if (!AutoSave) Debug("Saving synchronously...");
            savetask.run();
        }
        
        return task;
    }

    private void JoinGame(String world, Player player) {
        Debug("Joining game for " + player.getName());
        HardcorePlayer hcp = HCPlayers.Get(world, player.getUniqueId());
        if (hcp != null) {
            if (hcp.getLastPos() != null) {
                DebugLog("Returning player to: " + hcp.getLastPos());
                if (Util.Teleport(player, hcp.getLastPos())) {
                    player.setWalkSpeed(0.2F);
                    player.setFlySpeed(0.2F);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setOp(false);
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.setFallDistance(0);
                    player.setNoDamageTicks(60);
                    BroadcastToHardcore(Header + ChatColor.GREEN + player.getDisplayName() + " has entered " + hcp.getWorld(), player.getName());
                } else {
                    Warn("Teleport failed!");
                }
            }
        } else {
            Warn("Player record NOT found!");
        }
    }
    
    private Boolean LoadAllPlayers() {
        String query = "SELECT * FROM `players` ORDER BY world,id";
        try {
            HCPlayers.Clear();
            ResultSet res = dbcon.PreparedQuery(query, null);
            if (res != null) {
                while (res.next()) {
                    UUID id = UUID.fromString(res.getString("id"));
                    String name = res.getString("player");
                    String world = res.getString("world");
                    DebugLog("Loading: " + world + "/" + name);
                    HardcorePlayer hcp = HCPlayers.NewPlayer(world, id, name);
                    LoadPlayerFromData(hcp, res);
                }
            }
        }
        catch (Exception e) {
            Debug("Unable to load player record to database!");
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    public Boolean LoadPlayer(String world, UUID player) {
        String query = "SELECT * FROM `players` WHERE `id`=? and `world`=?";
        try {
            DebugLog("Reload player record from DB: " + world + "/" + player);
            ResultSet res = dbcon.PreparedQuery(query, new String[]{player.toString(), world});
            HardcorePlayer hcp = HCPlayers.Get(world, player);
            if ((res != null) && (hcp != null) && (res.next())) {
                DebugLog("Loading: " + world + "/" + player);
                LoadPlayerFromData(hcp, res);
            } else {
                return false;
            }
        }
        catch (Exception e) {
            Debug("Unable to load player record to database!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void LoadPlayerFromData(HardcorePlayer hcp, ResultSet res) {
        try {
            hcp.setLoadDataOnly(true);
            hcp.setLastPos(Util.Str2Loc(res.getString("lastpos")));
            hcp.setLastJoin(Util.Mysql2Date(res.getString("lastjoin")));
            hcp.setLastQuit(Util.Mysql2Date(res.getString("lastquit")));
            hcp.setGameStart(Util.Mysql2Date(res.getString("gamestart")));
            hcp.setGameEnd(Util.Mysql2Date(res.getString("gameend")));
            hcp.setGameTime(res.getInt("gametime"));
            hcp.setLevel(res.getInt("level"));
            hcp.setExp(res.getInt("exp"));
            hcp.setScore(res.getInt("score"));
            hcp.setTopScore(res.getInt("topscore"));
            hcp.setState(PlayerState.valueOf(res.getString("state")));
            hcp.setDeathMsg(res.getString("deathmsg"));
            hcp.setDeathPos(Util.Str2Loc(res.getString("deathpos")));
            hcp.setDeaths(res.getInt("deaths"));
            hcp.setCowKills(res.getInt("cowkills"));
            hcp.setPigKills(res.getInt("pigkills"));
            hcp.setSheepKills(res.getInt("sheepkills"));
            hcp.setChickenKills(res.getInt("chickenkills"));
            hcp.setCreeperKills(res.getInt("creeperkills"));
            hcp.setZombieKills(res.getInt("zombiekills"));
            hcp.setSkeletonKills(res.getInt("skeletonkills"));
            hcp.setSpiderKills(res.getInt("spiderkills"));
            hcp.setEnderKills(res.getInt("enderkills"));
            hcp.setSlimeKills(res.getInt("slimekills"));
            hcp.setMooshKills(res.getInt("mooshkills"));
            hcp.setOtherKills(res.getInt("otherkills"));
            hcp.setPlayerKills(res.getInt("playerkills"));
            hcp.setModified(false);
            hcp.setLoadDataOnly(false);
        }
        catch (Exception e) {
            Debug("Unable to load player record to database!");
            e.printStackTrace();
        }
    }
    
    public void SaveAllPlayers() {
        for (Map.Entry<String, HardcorePlayer> entry: HCPlayers.AllRecords().entrySet()) {
            HardcorePlayer hcp = entry.getValue();
            if ((hcp != null) && (hcp.isModified())) {
                SavePlayer(hcp, false);
            }
        }
    }
    
    private void SaveIngamePlayers() {
        for (Map.Entry<String, HardcorePlayer> entry: HCPlayers.AllRecords().entrySet()) {
            HardcorePlayer hcp = entry.getValue();
            if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
                Player p = Bukkit.getPlayer(hcp.getPlayerName());
                if (p != null) {
                    hcp.updatePlayer(p);
                    SavePlayer(hcp, true, true);
                }
            }
        }
    }
    
    public boolean IsHardcoreWorld(World world) {
        if (world != null) {
            return HardcoreWorlds.Contains(world.getName());
        }
        return false;
    }
    
    public Location GetLobbyLocation(Player player, String world) {
        Location loc = null;
        if (world != null) {
            HardcoreWorld hcw = HardcoreWorlds.Get(world);
            loc = hcw.getExitPos();
        }
        
        if (loc == null) {
            Warn("Sending " + player.getName() + " to world spawn!");
            loc = getServer().getWorld(getConfig().getString("lobbyWorld", "games")).getSpawnLocation();
        }
        
        return loc;
    }
    
    private boolean IsOnWhiteList(String world, UUID player) {
        String query = "SELECT worlds FROM `whitelist` WHERE id=?";
        try {
            ResultSet res = dbcon.PreparedQuery(query, new String[] {player.toString()});
            if (res != null) {
                if (res.next()) {
                    String[] worlds = StringUtils.split(res.getString("worlds"), ",");
                    for (String w : worlds) {
                        if ((w.equals(world)) || (w.equals("*"))) {
                            return true;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Boolean AddToWhitelist(UUID player) {
        String query = "INSERT INTO `whitelist` (id, worlds) VALUES (?, ?)";
        String worlds = HardcoreWorlds.GetNames();
        try {
            DebugLog("Add player to whitelist: " + player);
            int result = dbcon.PreparedUpdate(query, new String[]{player.toString(), worlds});
            if (result < 0) {
                Debug("Whitelist update failed!");
                return false;
            }
        }
        catch (Exception e) {
            Debug("Unable to load player record to database!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void UnvanishPlayer(Player player) {
        if (IsPlayerVanished(player)) {
            Debug("Unvanishing " + player.getName());
            vnp.toggleVanish(player);
        }
    }
    
    public boolean IsPlayerVanished(Player player) {
        return (VNPHooked) && (vnp.isVanished(player));
    }
    
    private boolean SetProtected(HardcorePlayer hcp, long seconds) {
        if (hcp != null) {
            if (hcp.isGodMode()) {
                Debug(hcp.getPlayerName() + " already in god mode!");
                return false;
            }

            final String world = hcp.getWorld();
            final UUID id = hcp.getUniqueId();
            final Player player = getServer().getPlayer(id);

            hcp.setGodMode(true);
            player.sendMessage(ChatColor.YELLOW + "You are now invincible for " + seconds + " seconds...");
            
            getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                HardcorePlayer hcp1 = HCPlayers.Get(world, id);
                if (hcp1 != null) {
                    hcp1.setGodMode(false);
                    if (hcp1.getState() == PlayerState.IN_GAME) {
                        player.sendMessage(ChatColor.RED + "Your invincibility has now worn off... Good luck!");
                    } else {
                        //Debug("Disable protection: Player " + pname + " is no longer in game");
                    }
                } else {
                    //Debug("Disable protection: Player " + pname + " does not exist!");
                }
            }, (seconds * 20));

        }
        return false;
    }
    
    boolean IsPlayerSafe(Player player, double x, double y, double z) {
        List<Entity> ents = player.getNearbyEntities(x, y, z);
        if (ents != null) {
            for (Entity e : ents) {
                if (e instanceof Monster) return false;
                if (e instanceof Slime) return false;
                if (e instanceof Ghast) return false;
            }
        }
        return true;
    }
    
    private void BroadcastToWorld(String world, String rawmsg) {
        String msg = ChatColor.translateAlternateColorCodes('&', rawmsg);
        Debug(msg);
        for (final Player p : getServer().getOnlinePlayers()) {
            HardcorePlayer hcp = HCPlayers.Get(world, p.getUniqueId());
            if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
                p.sendMessage(msg);
            }
        }
    }

    public void BroadcastToHardcore(String rawmsg) {
        BroadcastToHardcore(rawmsg, null);
    }
    
    public void BroadcastToHardcore(String rawmsg, String excludePlayer) {
        String msg = ChatColor.translateAlternateColorCodes('&', rawmsg);
        Debug("HardcoreBroadcast: " + msg);
        for (final Player p : getServer().getOnlinePlayers()) {
            // Skip the excluded player (if specified)
            if ((excludePlayer != null) && (excludePlayer.equals(p.getName())))
                continue;

            if (IsHardcoreWorld(p.getWorld())) {
                HardcorePlayer hcp = HCPlayers.Get(p);
                if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
                    p.sendMessage(msg);
                }
            }
        }
    }
    
    public boolean InsideWorldBorder(Location loc) {
        BorderData bd = null;
        if (!WBHooked) return true;
        bd = wb.getWorldBorder(loc.getWorld().getName());
        return (bd != null) && (bd.insideBorder(loc));
    }
    
    public void BroadcastToAllServers(String msg) {
        Bukkit.getServer().broadcastMessage(msg);
        if (BCHooked) {
            BungeeChat.mirrorChat(msg, BroadcastChannel);
        }
    }
}