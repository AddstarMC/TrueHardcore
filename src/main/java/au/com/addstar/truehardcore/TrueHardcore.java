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

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.truehardcore.commands.CommandTH;
import au.com.addstar.truehardcore.config.ConfigManager;
import au.com.addstar.truehardcore.config.ThConfig;
import au.com.addstar.truehardcore.database.Database;
import au.com.addstar.truehardcore.functions.CombatTracker;
import au.com.addstar.truehardcore.functions.Util;
import au.com.addstar.truehardcore.functions.WorldRollback;
import au.com.addstar.truehardcore.listeners.ChunkListener;
import au.com.addstar.truehardcore.listeners.PacketListener;
import au.com.addstar.truehardcore.listeners.PlayerListener;
import au.com.addstar.truehardcore.objects.AccessState;
import au.com.addstar.truehardcore.objects.ChunkStorage;
import au.com.addstar.truehardcore.objects.HardcorePlayers;
import au.com.addstar.truehardcore.objects.HardcorePlayers.HardcorePlayer;
import au.com.addstar.truehardcore.objects.HardcorePlayers.PlayerState;
import au.com.addstar.truehardcore.objects.HardcoreWorlds;
import au.com.addstar.truehardcore.objects.HardcoreWorlds.HardcoreWorld;
import au.com.addstar.truehardcore.placeholder.PlaceholderAPI;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLib;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.destroystokyo.paper.MaterialTags;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.WorldBorder;
import de.myzelyam.api.vanish.VanishAPI;
import dev.esophose.playerparticles.api.PlayerParticlesAPI;
import dev.esophose.playerparticles.particles.FixedParticleEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.title.Title;
import org.prism_mc.prism.paper.api.PrismPaperApi;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TrueHardcore extends JavaPlugin {
    private static final Logger logger = Logger.getLogger("Minecraft");
    private static final Logger debuglog = Logger.getLogger("DebugLog");
    public static TrueHardcore instance;
    private static Economy econ = null;
    private static ThConfig cfg;
    private static PluginDescriptionFile pdfFile = null;
    // Hardcore worlds
    public final HardcoreWorlds hardcoreWorlds = new HardcoreWorlds();
    public final ChunkStorage chunkStorage = new ChunkStorage();
    // Data for ALL hardcore players
    public final HardcorePlayers hcPlayers = new HardcorePlayers();
    private final Set<UUID> allowedTeleport = new HashSet<>();
    private final Set<UUID> playersJoining = new HashSet<>();
    private final HashMap<HardcoreWorld, Integer> playersJoiningWorld = new HashMap<HardcoreWorld, Integer>();

    public final String header = ChatColor.DARK_RED + "[" + ChatColor.RED + "TrueHardcore"
          + ChatColor.DARK_RED + "] " + ChatColor.YELLOW;

    // Number of columns to probe within a single loaded chunk before giving up on it.
    // Loading the chunk is the expensive part, so sampling several columns per load makes
    // a first-attempt success far more likely.
    private static final int SPAWN_SAMPLE_COLUMNS = 10;

    // Common natural surface blocks that no Tag/MaterialTag below captures, so they must be
    // listed explicitly. Notably GRASS_BLOCK is NOT part of Tag.DIRT, so without this it (and
    // therefore most overworld surface) would be rejected as a spawn spot.
    private final List<Material> spawnBlocks = new ArrayList<>(Arrays.asList(
            Material.GRASS_BLOCK,
            Material.STONE,
            Material.DIORITE,
            Material.GRANITE,
            Material.ANDESITE,
            Material.TUFF,
            Material.BEDROCK,
            Material.CLAY
    ));

    public CombatTracker combatTracker;
    public WorldRollback rollbackHandler;
    public Boolean prismHooked = false;
    public PrismPaperApi prism;
    private boolean vaultEnabled = false;
    private FileHandler debugFileHandler;
    private PluginManager pm = null;
    private Database dbConnection = null;
    private Boolean lwcHooked = false;
    private Boolean wbHooked = false;
    private Boolean pvHooked = false;
    private Boolean ppHooked = false;
    private LWC lwc;
    private WorldBorder wb;
    private VanishAPI pv;
    private PlayerParticlesAPI ppapi;

    public TrueHardcore() {
        super();
    }

    public static ThConfig getCfg() {
        return cfg;
    }

    public static void log(String data) {
        logger.info("[" + pdfFile.getName() + "] " + data);
        debuglog.info(data);
    }

    public static void warn(String data) {
        logger.warning("[" + pdfFile.getName() + "] " + data);
        debuglog.warning(data);
    }

    /**
     * record debug messag.
     *
     * @param data message
     */
    public static void debug(String data) {
        if (cfg != null && cfg.debugEnabled) {
            logger.info("[" + pdfFile.getName() + "] " + data);
        }
        debuglog.info(data);
    }

    // Write data to debug log
    public static void debugLog(String data) {
        debuglog.info(data);
    }

    @Override
    public void onEnable() {

        instance = this;
        pdfFile = this.getDescription();
        loadConfig();
        // This block configure the logger with handler and formatter
        chunkStorage.enable();
        try {
            debuglog.setUseParentHandlers(false);
            debugFileHandler = new FileHandler("plugins/TrueHardcore/debug.log", true);
            Util.LogFormatter formatter = new Util.LogFormatter();
            debugFileHandler.setFormatter(formatter);
            debuglog.addHandler(debugFileHandler);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        pm = this.getServer().getPluginManager();
        // Check if vault is loaded (required for economy)
        vaultEnabled = setupEconomy();
        if (vaultEnabled) {
            log("Found Vault! Hooking for economy!");
        } else {
            log("Vault was not detected! Economy rewards are not available.");
        }

        Plugin p = pm.getPlugin("LWC");
        if (p instanceof LWCPlugin) {
            lwcHooked = true;
            lwc = ((LWCPlugin) p).getLWC();
            log("LWC Found, hooking into LWC.");
        } else {
            lwcHooked = false;
            log("LWC not Found");
        }
        // Prism v4 exposes its API through the Bukkit ServicesManager rather than the plugin
        // instance. It registers the service on enable, so as long as Prism is in our softdepend
        // (it is) it will be available by the time we run.
        RegisteredServiceProvider<PrismPaperApi> prismProvider =
                getServer().getServicesManager().getRegistration(PrismPaperApi.class);
        if (prismProvider != null) {
            prismHooked = true;
            prism = prismProvider.getProvider();
            log("Prism found, hooking it.");
            rollbackHandler = new WorldRollback(prism);
        } else {
            prismHooked = false;
            log("Prism not found! This won't work very well...");
        }

        p = pm.getPlugin("WorldBorder");
        if (p instanceof WorldBorder) {
            wbHooked = true;
            wb = WorldBorder.plugin;
            log("WorldBorder found, hooking it.");
        } else {
            wbHooked = false;
            log("WorldBorder not found! Spawning will not be limited...");
        }

        p = pm.getPlugin("PremiumVanish");
        if (p != null && p.isEnabled()) {
            pvHooked = true;
            log("PremiumVanish found, will use it.");
        } else {
            pvHooked = false;
            log("PremiumVanish not found! Vanished players will not be unvanished.");
        }

        p = pm.getPlugin("PlayerParticles");
        if (p != null && p.isEnabled()) {
            ppHooked = true;
            ppapi = PlayerParticlesAPI.getInstance();
            log("PlayerParticles found, will use it.");
        } else {
            ppHooked = false;
            log("PlayerParticles not found! Particle effects will not be removed on death.");
        }

        p = pm.getPlugin("ProtocolLib");
        if (p instanceof ProtocolLib) {
            ProtocolLibrary.getProtocolManager().addPacketListener(
                    new PacketListener(
                            this,
                            ListenerPriority.NORMAL,
                            PacketType.Play.Server.RESPAWN,
                            PacketType.Play.Server.LOGIN));
            log("ProtocolLib found! Hardcore hearts will be enabled.");
        } else {
            log("ProtocolLib not found! Hardcore hearts will not work.");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI found! Registering placeholders...");
            new PlaceholderAPI(this).register();
            //Bukkit.getPluginManager().registerEvents(this, this);
            getLogger().info("Placeholders loaded!");
        } else {
            getLogger().warning("PlaceholderAPI not found, skipping...");
        }

        // Open/initialise the database
        dbConnection = new Database();
        if (dbConnection.isConnected()) {
            log("Successfully connected to the database.");
            // Lightweight schema migration: ensure newer columns exist before loading.
            dbConnection.ensureColumn("players", "rollbackpending",
                    "tinyint(1) NOT NULL DEFAULT 0");
            dbConnection.ensureColumn("players", "historypurged",
                    "tinyint(1) NOT NULL DEFAULT 0");
            log("Loading players from database...");
            loadAllPlayers();
        } else {
            log(pdfFile.getName() + " " + pdfFile.getVersion() + " could not be enabled!");
            this.setEnabled(false);
            return;
        }

        log("Registering commands and events...");
        try {
            CommandExecutor exec = new CommandTH(this);
            Objects.requireNonNull(getCommand("truehardcore")).setExecutor(exec);
            Objects.requireNonNull(getCommand("th")).setExecutor(exec);
        } catch (NullPointerException e) {
            log("Please check plugin yml commands are missing");
        }
        pm.registerEvents(new PlayerListener(this), this);
        long baseChunkTime = cfg.baseChunkTime * 60 * 60 * 20;
        if (baseChunkTime > 0) {
            pm.registerEvents(new ChunkListener(baseChunkTime), this);
        }

        // Dynamically load the spawnBlocks list from Tags/MaterialTags
        spawnBlocks.addAll(Tag.LEAVES.getValues());
        spawnBlocks.addAll(Tag.DIRT.getValues());
        spawnBlocks.addAll(Tag.ICE.getValues());
        spawnBlocks.addAll(Tag.TERRACOTTA.getValues());
        spawnBlocks.addAll(Tag.SAND.getValues());
        spawnBlocks.addAll(Tag.LOGS.getValues());
        spawnBlocks.addAll(Tag.CONCRETE_POWDER.getValues());
        spawnBlocks.addAll(MaterialTags.SANDSTONES.getValues());
        spawnBlocks.addAll(MaterialTags.COBBLESTONES.getValues());
        spawnBlocks.addAll(MaterialTags.CONCRETES.getValues());
        spawnBlocks.addAll(MaterialTags.MUSHROOM_BLOCKS.getValues());
        spawnBlocks.addAll(MaterialTags.ORES.getValues());

        // Dump out all the spawnBlocks to log for debugging
        debug("SpawnBlocks: " + spawnBlocks.toString());

        // Enable combat log tracker
        combatTracker = new CombatTracker(this);
        pm.registerEvents(combatTracker, this);

        if (cfg.autoSaveEnabled) {
            log("Launching auto-save timer (every 5 minutes)...");
            getServer().getScheduler().runTaskTimer(this, this::saveInGamePlayers,
                  300 * 20L, 300 * 20L);
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, chunkStorage::expireOldChunks, 300 * 20L, 5 * 20L);

        if (cfg.historyPurgeEnabled) {
            // Real-time interval from config, converted to ticks only for scheduling.
            long intervalTicks = Math.max(1L, (long) cfg.historyPurgeIntervalMinutes) * 60L * 20L;
            log("Launching Prism history-purge sweep (every " + cfg.historyPurgeIntervalMinutes
                  + " minutes, retention " + cfg.historyPurgeRetentionDays + " days)...");
            getServer().getScheduler().runTaskTimerAsynchronously(this,
                  this::sweepHistoryPurges, intervalTicks, intervalTicks);
        }
        log(pdfFile.getName() + " " + pdfFile.getVersion() + " has been enabled");
    }

    @Override
    public void onDisable() {
        chunkStorage.disable();
        if (combatTracker != null) {
            combatTracker.onDisable();
        }
        if (rollbackHandler != null) {
            rollbackHandler.onDisable();
        }
        getServer().getScheduler().cancelTasks(this);
        saveAllPlayers();
        if (dbConnection != null) {
            dbConnection.closeDatabase();
        }
        log(pdfFile.getName() + " has been disabled!");
        if (debugFileHandler != null) {
            debugFileHandler.close();
        }
    }

    /**
     * Detect and configure  economy.
     *
     * @return boolean true on success
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
              .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    public FileConfiguration config() {
        return getConfig();
    }

    /**
     * Give money to a player.
     *
     * @param player player
     * @param money  amount
     * @return true on success
     */
    @SuppressWarnings("unused")
    public boolean giveMoney(OfflinePlayer player, int money) {
        if (vaultEnabled) {
            EconomyResponse resp = econ.depositPlayer(player, money);
            if (resp.type == ResponseType.SUCCESS) {
                log(player + " has been given $" + resp.amount
                      + " (new balance $" + resp.balance + ")");
                return true;
            } else {
                warn("Vault payment failed! Error: " + resp.errorMessage);
            }
        }
        return false;
    }

    /**
     * Function that runs a combat log situation.
     *
     * @param player   the logging player
     * @param location the players location.
     */
    @SuppressWarnings("unused")
    public void handlePlayerCombatLogging(final Player player, Location location) {
        PlayerInventory inv = player.getInventory();
        if (location.getBlock().isEmpty()) {
            BlockData data = Bukkit.createBlockData(Material.CHEST);
            location.getBlock().setBlockData(data);
            Block block = location.getBlock();
            if (block instanceof Chest) {
                ((Chest) block).getInventory().addItem(inv.getContents());
                inv.clear();
                broadCastToHardcore(player.getDisplayName()
                      + " has logged out in combat and left behind goodies."
                      + " Find them if you can!");
            }
        }
    }

    /**
     * Execute a configured command with placeholder replacement, dispatched on the next tick.
     *
     * @param command the raw command string with placeholders.
     * @param player  the relevant player.
     * @param hcp     the hardcore player record.
     * @param hcw     the hardcore world (used for the world placeholder).
     * @param cause   the cause string (death message, entity name, etc).
     */
    public void executeConfigCommand(String command, Player player, HardcorePlayer hcp,
                                     HardcoreWorld hcw, String cause) {
        String placeholderPattern = "%([^%]+)%";
        String cmd = command;

        Pattern pattern = Pattern.compile(placeholderPattern);
        Matcher matcher = pattern.matcher(cmd);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String resolvedValue = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player,
                    "%" + placeholder + "%");
            cmd = cmd.replace("%" + placeholder + "%", resolvedValue);
        }
        cmd = cmd.replaceAll("<player>", ChatColor.stripColor(player.getName()))
                .replaceAll("<displayname>", ChatColor.stripColor(player.getDisplayName()))
                .replaceAll("<world>", hcw.getWorld().getName())
                .replaceAll("<cause>", ChatColor.stripColor(cause))
                .replaceAll("<score>", Integer.toString(hcp.getScore()));
        debug("Executing (next tick): " + cmd);
        final String finalCmd = cmd;
        Bukkit.getScheduler().runTask(this, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
    }

    /**
     * Handle player death.
     *
     * @param player player.
     * @param event  death event.
     */
    public void doPlayerDeath(final Player player, PlayerDeathEvent event) {
        final World realWorld = player.getWorld();

        final HardcorePlayer hcp = hcPlayers.get(realWorld, player);
        if (hcp == null) {
            TrueHardcore.log("Player was null - this should not happen");
            throw new NullPointerException("Player was null");
        }
        final World world = getServer().getWorld(hcp.getWorld());
        if (world == null) {
            TrueHardcore.log("World was null - this should not happen");
            throw new NullPointerException("World was null");
        }
        final HardcoreWorld hcw = hardcoreWorlds.get(world.getName());
        chunkStorage.addChunk(player.getChunk(), System.currentTimeMillis() + (hcw.getChunkHoldOnDeathDelay() * 1000));
        TrueHardcore.debug("Chunk setForceLoaded(true): " + player.getChunk().getX() + " " + player.getChunk().getZ() + " "
                + player.getChunk().getWorld().getName() + " (" + hcw.getChunkHoldOnDeathDelay() + "s)");
        player.getChunk().setForceLoaded(true);
        hcp.setState(PlayerState.DEAD);
        hcp.setInCombat(false);
        hcp.setCombatExpiry(0);
        hcp.setDeathMsg(event.getDeathMessage());
        hcp.setDeathPos(player.getLocation());
        hcp.setDeaths(hcp.getDeaths() + 1);
        hcp.updatePlayer(player);
        hcp.calcGameTime();

        String deathMessage = event.getDeathMessage();
        if (deathMessage != null) {
            deathMessage = deathMessage.replaceFirst(player.getName(),
                  ChatColor.AQUA + player.getName() + ChatColor.YELLOW);
            broadcastToAllServers(header + deathMessage + "!");
        }
        broadcastToAllServers(header + "Final Score: " + ChatColor.GREEN
              + player.getTotalExperience() + " " + ChatColor.AQUA
              + "(" + hcp.getWorld() + ")");

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
            for (Map.Entry<String, HardcorePlayer> entry : hcPlayers.allRecords().entrySet()) {
                HardcorePlayer other = entry.getValue();
                // Only compare other player's scores in the same world
                if ((other != null) && (other.getWorld().equals(hcp.getWorld()))) {
                    // Don't compare score to yourself
                    if (!other.getPlayerName().equals(hcp.getPlayerName())) {
                        if (hcp.getScore() <= other.getTopScore()) {
                            highscore = false;
                            debug(hcp.getPlayerName() + "'s score (" + hcp.getScore()
                                  + ") did not beat " + other.getPlayerName() + " (" + other.getTopScore() + ")");
                            break;
                        }
                    }
                } else {
                    warn("Record for key \"" + entry.getKey() + "\" not found! This should not happen!");
                }
            }

            if (highscore) {
                broadcastToAllServers(header + ChatColor.AQUA + player.getName()
                      + ChatColor.GREEN + " has beaten the all time high score!");
            } else if (personalbest) {
                player.sendMessage(ChatColor.GREEN
                      + "Congratulations! You just beat your personal high score!");
            }
        }

        // Execute death command if one is configured
        if (!hcw.getDeathCommand().isEmpty()) {
            executeConfigCommand(hcw.getDeathCommand(), player, hcp, hcw, deathMessage);
        }

        // Mark a rollback as outstanding so the player can't re-enter (even after a paid revive)
        // until it completes. Cleared by WorldRollback once every dimension's rollback finishes.
        // Only set when Prism is hooked - otherwise no rollback runs and nothing would clear it.
        if (prismHooked) {
            hcp.setRollbackPending(true);
            // Re-arm the death-anchored history purge for this new death (clears any true left
            // over from a previous life). The periodic sweeper purges it ~retention days later.
            hcp.setHistoryPurged(false);
        }

        savePlayer(hcp);

        // Dont drop XP or items
        if (!hcw.getDeathDrops()) {
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

        if ((hcw.getRollbackDelay() > 0) && (!hcw.getRollbackBroadcast().isEmpty())) {
            String wh = ChatColor.DARK_RED + "[" + ChatColor.RED + hcp.getWorld()
                  + ChatColor.DARK_RED + "] " + ChatColor.YELLOW;
            broadcastToWorld(hcp.getWorld(), wh + " "
                    + ChatColor.translateAlternateColorCodes('&', hcw.getRollbackBroadcast()
                            .replace("%time%", Util.long2Time(hcw.getRollbackDelay()))
                            .replace("%player%", player.getDisplayName())
                    )
            );
        }

        instance.getServer().getScheduler().runTaskLater(instance, () -> {
            try {
                if (instance.lwcHooked) {
                    // Always remove the locks straight away!
                    debug("Removing LWC locks...");
                    int count = 0;
                    if (lwc.getPhysicalDatabase() != null) {
                        List<Protection> prots = lwc.getPhysicalDatabase()
                              .loadProtectionsByPlayer(player.getUniqueId().toString());
                        String w = world.getName();
                        for (Protection prot : prots) {
                            if (prot.getWorld().equals(w)
                                  || prot.getWorld().equals(w + "_nether")
                                  || prot.getWorld().equals(w + "_the_end")) {
                                count++;
                                // Remove LWC protection
                                prot.remove();
                                prot.removeCache();
                            }
                        }
                    } else {
                        log("WARNING: LWC.getPhysicalDatabase() failed!");
                    }
                    debug("Removed " + count + " LWC protections.");
                }

                if (prismHooked) {
                    // Queue rollback for the Overworld
                    rollbackHandler.queueRollback("ROLLBACK", player, world, hcw.getRollbackDelay());

                    // Queue rollback for The Nether
                    World netherworld = instance.getServer().getWorld(world.getName() + "_nether");
                    if (netherworld != null) {
                        rollbackHandler.queueRollback("ROLLBACK", player, netherworld, hcw.getRollbackDelay());
                    }

                    // Queue rollback for The End
                    World endworld = instance.getServer().getWorld(world.getName() + "_the_end");
                    if (endworld != null) {
                        rollbackHandler.queueRollback("ROLLBACK", player, endworld, hcw.getRollbackDelay());
                    }
                }

                if (ppHooked) {
                    int ppremoved = 0;
                    debug("Removing all fixed PlayerParticles effects for player " + player.getName());
                    // Remove all fixed particle effects that the player has placed in this HC world
                    for (FixedParticleEffect effect : ppapi.getFixedParticleEffects(player)) {
                        // Ensure it is for the correct hardcore world
                        if (hcw == hardcoreWorlds.get(effect.getLocation().getWorld().getName())) {
                            ppapi.removeFixedEffect(player, effect.getId());
                            ppremoved++;
                        }
                    }
                    debug("Removed " + ppremoved + " PlayerParticles effects for player " + player.getName()
                            + " in " + hcw.getWorld().getName());
                }
            } catch (Exception e) {
                // Do nothing or throw an error if you want
                e.printStackTrace();
            }
        }, 20L);
    }

    private void loadConfig() {
        // Read (or initialise) plugin config file
        ConfigManager configManager = new ConfigManager(this);
        configManager.loadConfig();
        cfg = configManager.getConfig();
    }

    /**
     * Play the game in a world.
     *
     * @param world  the world name
     * @param player bukkit player
     * @return true on success
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean playGame(String world, Player player) {
        // Only check whitelist if world is whitelisted
        HardcoreWorld hcw = hardcoreWorlds.get(world);
        if (hcw.isWhitelisted()) {
            if (!isOnWhiteList(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED
                      + "Sorry, you are not allowed to play this world.");
                return false;
            }
        }
        if (!cfg.gameEnabled && Util.noPermission(player, "truehardcore.admin")) {
            player.sendMessage(ChatColor.RED + "TrueHardcore is currently disabled.");
            return false;
        }

        if (isPlayerJoining(player)) {
            player.sendMessage(ChatColor.RED
                + "Still searching for a location, please wait...");
            return false;
        }

        if (playerJoiningCount(hcw) > 0) {
            player.sendMessage(ChatColor.RED + "Sorry, other players are currently joining, please wait...");
            debug("Join cancelled, already " + playerJoiningCount(hcw) + " players joining " + hcw.getWorld().getName());
            return false;
        }

        HardcorePlayer hcp = hcPlayers.get(world, player.getUniqueId());
        if (hcp != null) {
            // Never let a player into a world that is still being restored from their last death.
            // This gates revived (ALIVE) players too, since rollbackPending is independent of state.
            if (hcp.isRollbackPending()) {
                player.sendMessage(ChatColor.RED + "Rollback is still in progress from your "
                      + "previous death - please try again shortly.");
                return false;
            }
            if ((hcp.getState() == PlayerState.DEAD) && (hcp.getGameEnd() != null)) {
                // Check last death time
                Date now = new Date();
                long diff = (now.getTime() - hcp.getGameEnd().getTime()) / 1000;
                long wait = (hcw.getBantime() - diff);

                if (wait > 0) {
                    player.sendMessage(ChatColor.RED + "Sorry, you must wait "
                          + Util.long2Time(wait) + " to play " + hcw.getWorld().getName()
                          + " again.");
                    return false;
                }
            }
        }

        // Always update player/ip tracking
        updateTracking(player);

        // Check if this account is already marked as an alt or primary
        String accType = getAccountType(player);
        if (accType != null) {
            if (accType.equals("alt")) {
                // Account already marked as alt - refuse entry
                player.sendMessage(ChatColor.RED + "Sorry, your account has been detected as an Alt of another.");
                player.sendMessage(ChatColor.RED + "If you believe this is an error or you have a legitimate use,");
                player.sendMessage(ChatColor.RED + "please make a /ticket requesting an exclusion and why.");
                setAccountType(player, "alt");
                return false;
            }
        } else {
            if (isAltAccount(player)) {
                // Alt detected - refuse entry and flag account
                player.sendMessage(ChatColor.RED + "Sorry, your account has been detected as an Alt of another.");
                player.sendMessage(ChatColor.RED + "If you believe this is an error or you have a legitimate use,");
                player.sendMessage(ChatColor.RED + "please make a /ticket requesting an exclusion and why.");
                setAccountType(player, "alt");
                return false;
            } else {
                // Not a known alt or detected as an alt, so mark as a Primary account
                setAccountType(player, "primary");
            }
        }

        if ((hcp == null) || (hcp.getState() == PlayerState.DEAD)) {
            log("Player " + player.getName() + " is attempting to start a new hardcore life in world " + world);
            findNewSpawn(player, world, hcp);
        } else if (hcp.getState() == PlayerState.IN_GAME) {
            player.sendMessage(ChatColor.RED + "You are already playing hardcore!");
            player.sendMessage(ChatColor.RED + "This should not happen, please report it to an admin!");
            return false;
        } else {
            // Resume existing game
            log("Player " + player.getName() + " is resuming hardcore in world " + world);
            player.sendMessage(ChatColor.GREEN
                  + "Returning to your last hardcore location... good luck!");
            debug(player.getName() + " is returning to " + hcw.getWorld().getName());
            hcp.setState(PlayerState.IN_GAME);
            joinGame(world, player);
            TrueHardcore.debug("AFTER: Player has " + hcp.getScore() + " score - "
                    + "Level:" + player.getLevel() + ", "
                    + "XP:" + player.getExp() + ", "
                    + "TotalXP:" + player.getTotalExperience());
            player.setTotalExperience(hcp.getScore());
            player.setExp(hcp.getExp());
            player.setLevel(hcp.getLevel());
            TrueHardcore.debug("AFTER: Player has " + hcp.getScore() + " score - "
                    + "Level:" + player.getLevel() + ", "
                    + "XP:" + player.getExp() + ", "
                    + "TotalXP:" + player.getTotalExperience());
            savePlayer(hcp);
            cleanAndGreet(player, world);
        }
        return true;
    }

    /**
     * Compute a player's current eligibility to play a hardcore world.
     *
     * <p>This is a read-only mirror of the gate checks performed by
     * {@link #playGame(String, Player)}, in the same order, but performs no writes
     * (no account-type or tracking updates). Safe to call repeatedly, e.g. from the
     * lobby hologram placeholders.
     *
     * <p>The remaining cooldown time (when {@link AccessState#COOLDOWN}) is not
     * returned here; callers that need it should recompute it from
     * {@code hcp.getGameEnd()} and {@code hcw.getBantime()} as the placeholder does.
     *
     * @param world  the world name
     * @param player the player (may be offline)
     * @return the computed access state
     */
    public AccessState getAccessState(String world, OfflinePlayer player) {
        return getAccessState(world, player, false);
    }

    /**
     * As {@link #getAccessState(String, OfflinePlayer)}, but able to suppress the debug
     * logging of the queries it runs.
     *
     * <p>Polled callers such as the lobby hologram placeholders re-evaluate this on an
     * interval for every viewer, which would otherwise flood the debug log. Live callers
     * (join/play gating) should leave {@code silent} false so the queries stay visible.
     *
     * @param world  the world name
     * @param player the player (may be offline)
     * @param silent suppress debug logging of the queries run
     * @return the computed access state
     */
    public AccessState getAccessState(String world, OfflinePlayer player, boolean silent) {
        HardcoreWorld hcw = hardcoreWorlds.get(world);
        if (hcw == null) {
            // No such world configured - nothing to play
            return AccessState.DISABLED;
        }

        // Whitelist (only enforced on whitelisted worlds)
        if (hcw.isWhitelisted() && !isOnWhiteList(player.getUniqueId(), silent)) {
            return AccessState.NOT_WHITELISTED;
        }

        // Game globally disabled. playGame exempts truehardcore.admin, but we can't
        // check live permissions for an OfflinePlayer, so report DISABLED regardless.
        if (!cfg.gameEnabled) {
            return AccessState.DISABLED;
        }

        HardcorePlayer hcp = hcPlayers.get(world, player.getUniqueId());
        if (hcp != null) {
            // Rollback still outstanding from a previous death - gates even revived
            // (ALIVE) players, matching playGame.
            if (hcp.isRollbackPending()) {
                return AccessState.ROLLBACK;
            }

            // Death cooldown
            if (hcp.getState() == PlayerState.DEAD && hcp.getGameEnd() != null) {
                long diff = (new Date().getTime() - hcp.getGameEnd().getTime()) / 1000;
                long wait = hcw.getBantime() - diff;
                if (wait > 0) {
                    return AccessState.COOLDOWN;
                }
            }
        }

        // Alt account. Read-only: never writes account type or tracking here.
        String accType = getAccountType(player.getUniqueId(), silent);
        if ("alt".equals(accType)) {
            return AccessState.ALT;
        }
        if (accType == null) {
            // Live IP-based alt detection needs the online player; if offline we
            // simply fall back to the stored (absent) account type.
            Player online = Bukkit.getPlayer(player.getUniqueId());
            if (online != null && isAltAccount(online, silent)) {
                return AccessState.ALT;
            }
        }

        // Already in a game
        if (hcp != null && hcp.getState() == PlayerState.IN_GAME) {
            return AccessState.IN_GAME;
        }

        return AccessState.ALLOWED;
    }

    private void cleanAndGreet(Player player, String world) {
        unVanishPlayer(player);
        String greeting = hardcoreWorlds.get(world).getGreeting();
        if ((greeting != null) && (!greeting.isEmpty())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', greeting));
        }
    }

    private void findNewSpawn(Player player, String world, HardcorePlayer hcp) {
        final HardcoreWorld hcw = hardcoreWorlds.get(world);
        final int oldX;
        final int oldZ;
        final int hcDist = hcw.getSpawnDistance();

        if (hcp == null) {
            // Never played before
            oldX = 0;
            oldZ = 0;
        } else {
            // New life for an existing player
            oldX = hcp.getDeathPos().getBlockX();
            oldZ = hcp.getDeathPos().getBlockZ();
        }

        // Mark the player as currently joining a world
        // This is to prevent a player from joining again while waiting for a spawn location
        setPlayerJoining(player, true);

        // Increment the joining count for this world
        setPlayerJoiningCount(hcw, playerJoiningCount(hcw) + 1);

        player.sendMessage(ChatColor.YELLOW + "Finding a new spawn location.. please wait..");

        final BossBar bossbar = Bukkit.getServer().createBossBar(
                ChatColor.GREEN + "Finding suitable spawn location...",
                BarColor.GREEN,
                BarStyle.SEGMENTED_20);
        bossbar.setProgress(0);
        bossbar.addPlayer(player);
        bossbar.setVisible(true);

        final Location l = new Location(hcw.getWorld(), oldX, 255, oldZ);
        debug("Selecting spawn point " + hcDist + " blocks from: "
              + l.getBlockX() + " / " + l.getBlockY() + " / " + l.getBlockZ());

        // Attempt to find a new location once per tick so we don't cause too much lag
        new BukkitRunnable() {
            int maxattempts = 20;
            int attempt = 0;

            @Override
            public void run() {
                attempt++;

                // Set progress on bossbar
                float progress = ((float) attempt / (float) maxattempts);
                bossbar.setProgress(progress);

                //TrueHardcore.debug("Attempt #" + attempt + " to find location...");
                boolean goodSpawn = false;
                String reason = "Unable to find valid block (unknown)";

                // Lets do some trig!!
                int dist = hcDist - (int) (Math.random() * 500); // Reduce distance by random amount for variation
                int deg = (int) (Math.random() * 360); // Random degrees
                double x = (dist * Math.cos(Math.toRadians(deg))) + l.getBlockX();
                double z = (dist * Math.sin(Math.toRadians(deg))) + l.getBlockZ();

                // Loading a far-out chunk is the expensive part; a single column probe throws
                // most of that work away. Instead, snap to the chunk we just paid to load and
                // sample several columns within it, greatly raising the odds of a first-try hit.
                int chunkOriginX = (int) Math.floor(x / 16.0) * 16;
                int chunkOriginZ = (int) Math.floor(z / 16.0) * 16;

                // 'b' holds the last-scanned ground block so the give-up/debug output below still
                // has a sensible location to report even when no column in the chunk was valid.
                Block b = new Location(hcw.getWorld(), x, hcw.getWorld().getMaxHeight(), z).getBlock();
                for (int i = 0; i < SPAWN_SAMPLE_COLUMNS && !goodSpawn; i++) {
                    // Random column within the 16x16 chunk (keeps the "natural" spread of spawns)
                    int cx = chunkOriginX + (int) (Math.random() * 16);
                    int cz = chunkOriginZ + (int) (Math.random() * 16);

                    ColumnResult result = scanColumn(hcw.getWorld(), cx, cz);
                    b = result.ground;
                    reason = result.reason;
                    if (result.valid) {
                        goodSpawn = true;
                    }
                }

                Location spawn = b.getLocation().add(0, 1, 0);
                if (goodSpawn) {
                    this.cancel();

                    // Center player on block for safety
                    spawn.add(0.5, 0, 0);
                    spawn.add(0, 0, 0.5);

                    debug("[Attempt #" + attempt + "] GOOD: "
                          + Util.padLeft(String.valueOf(spawn.getX()), 9)
                          + Util.padLeft(String.valueOf(spawn.getY()), 7)
                          + Util.padLeft(String.valueOf(spawn.getZ()), 9)
                          + "   (" + dist + " blocks away)"
                          + "  => " + reason);

                    // Return the good location
                    spawn.setPitch(0F);
                    spawn.setYaw(0F);
                    bossbar.setVisible(false);
                    bossbar.removeAll();
                    if (player.isOnline()) {
                        newSpawn(player, spawn);
                    } else {
                        debug("WARNING: Player " + player.getName() + " is no longer online!");
                    }
                    Bukkit.getScheduler().runTaskLater(TrueHardcore.instance, () -> {
                        // Decrement the joining count for this world
                        setPlayerJoiningCount(hcw, playerJoiningCount(hcw) - 1);
                        setPlayerJoining(player, false);
                    }, 50L);
                } else {
                    debug("[Attempt #" + attempt + "] BAD : "
                          + Util.padLeft(String.valueOf(spawn.getX()), 9)
                          + Util.padLeft(String.valueOf(spawn.getY()), 7)
                          + Util.padLeft(String.valueOf(spawn.getZ()), 9)
                          + "   (" + dist + " blocks away)"
                          + "  => " + reason);
                }

                // Abort if we tried too many times
                if (attempt >= maxattempts) {
                    this.cancel();
                    log("Unable to find a good spawn point for " + player.getName()
                            + " in " + world + " after " + attempt + " attempts!");
                    setPlayerJoining(player, false);
                    if ((player != null) && (player.isOnline())) {
                        player.sendMessage(ChatColor.RED + "Unable to find a suitable starting location. Try again soon.");
                    }
                    bossbar.setVisible(false);
                    bossbar.removeAll();
                    Bukkit.getScheduler().runTaskLater(TrueHardcore.instance, () -> {
                        // Decrement the joining count for this world
                        setPlayerJoiningCount(hcw, playerJoiningCount(hcw) - 1);
                    }, 20L);
                }
            }
        }.runTaskTimer(instance, 2L, 25L);
    }

    /**
     * Outcome of scanning a single (x,z) column for a valid spawn ground block.
     */
    private static final class ColumnResult {
        final boolean valid;
        final Block ground;
        final String reason;

        ColumnResult(boolean valid, Block ground, String reason) {
            this.valid = valid;
            this.ground = ground;
            this.reason = reason;
        }
    }

    /**
     * Scan a single column from the top down and decide whether it is a valid spawn spot.
     * Mirrors the original per-column validation: the first solid block found must be an
     * allowed {@code spawnBlocks} type, inside the world border, and not liquid.
     *
     * @param world the world to scan in
     * @param x     block x coordinate of the column
     * @param z     block z coordinate of the column
     * @return the scan result (valid flag, the ground block reached, and a debug reason)
     */
    private ColumnResult scanColumn(World world, int x, int z) {
        int startY = world.getMaxHeight();
        Block b = new Location(world, x, startY, z).getBlock();
        String reason = "Unable to find valid block (unknown)";

        for (int y = startY; y > 10; y--) {
            b = b.getRelative(BlockFace.DOWN);
            reason = "Unable to find valid block (" + b.getType() + ")";

            if (b.isEmpty()) {
                // Move down to the next block if this one is empty
                continue;
            }
            else if (b.isLiquid()) {
                // We never want lava or water, so just skip this column
                return new ColumnResult(false, b, "BAD: Found liquid (" + b.getType() + ")");
            }
            else if (b.isSolid() && b.getType().isBlock()) {
                // We've found a real block so lets check it
                if (spawnBlocks.contains(b.getType())) {
                    // Make sure it's inside the world border (if one exists)
                    if (insideWorldBorder(b.getLocation())) {
                        return new ColumnResult(true, b, "Allowed block type (" + b.getType() + ")!");
                    }
                    return new ColumnResult(false, b, "Outside world border");
                }
                return new ColumnResult(false, b, "Wrong block type (" + b.getType() + ")");
            }
        }
        return new ColumnResult(false, b, reason);
    }

    /**
     * Allocate a new spawn point for a player.
     *
     * @param player the player
     * @param spawn  the location
     * @return true on success
     */
    private void newSpawn(Player player, Location spawn) {
        String world = spawn.getWorld().getName();
        HardcoreWorld hcw = hardcoreWorlds.get(world);
        final HardcorePlayer hcp = hcPlayers.get(spawn.getWorld(), player);
        Util.loadChunk(spawn);
        Util.teleport(player,spawn).thenAccept(result -> {
            if (result) {

                if (hcp == null) {
                    // New player so lets make a new record for them
                    HardcorePlayer p  = hcPlayers.newPlayer(spawn.getWorld().getName(), player.getUniqueId(), player.getName());
                    welcomePlayer(p,spawn,hcw,player);
                    return;
                }
                welcomePlayer(hcp,spawn,hcw,player);
            } else {
                warn("Teleport failed!");
                player.sendMessage(ChatColor.RED + "Sorry, teleport into hardcore has failed!");
                player.sendMessage(ChatColor.RED + "Please make a ticket for this to let us know.");
            }
        });
    }

    private void welcomePlayer(HardcorePlayer hcp, Location spawn, HardcoreWorld hcw, Player player){
        hcp.setState(PlayerState.IN_GAME);
        hcp.setSpawnPos(spawn);
        setProtected(hcp, hcw.getSpawnProtection());
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
        savePlayer(hcp);
        Bukkit.getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
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
                Objects.requireNonNull(player.getEquipment()).clear();
                player.getInventory().clear();
                player.eject();

                // Reset all player stats
                for (Statistic stat : Statistic.values()) {
                    try {
                        if (stat.getType() == Statistic.Type.UNTYPED) {
                            // This only resets basic/untyped stats
                            // We will not reset BLOCK/ITEM/ENTITY stats for now, maybe in the future
                            player.setStatistic(stat, 0);
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("Unable to reset " + stat.name() + " for player " + player.getName() + "(Cause: " + e.getMessage() + ")");
                    }
                }
            }
        });
        cleanAndGreet(player, spawn.getWorld().getName());
        broadCastToHardcore(header + ChatColor.GREEN + player.getDisplayName()
                + " has " + ChatColor.AQUA + "started " + ChatColor.GREEN
                + hcp.getWorld(), player.getName());
        player.sendMessage(ChatColor.RED + "!!!! WARNING !!!! WARNING !!!!");
        player.sendMessage(ChatColor.RED
                + "This plugin is highly experimental! Use at own risk!");
        player.sendMessage(ChatColor.RED + "Please report ALL problems in detail.");
        player.sendMessage(ChatColor.GREEN
                + "Welcome to TrueHardcore. Good luck on your adventure!");
        player.sendMessage(ChatColor.GREEN + "Type " + ChatColor.AQUA
                + "/th leave" + ChatColor.GREEN + " to exit (progress will be saved)");

        if (!hcw.getNewLifeCommand().isEmpty()) {
            executeConfigCommand(hcw.getNewLifeCommand(), player, hcp, hcw, "New Life");
        }
    }

    /**
     * Leave a game.
     *
     * @param player the player
     */
    public void leaveGame(Player player) {
        HardcorePlayer hcp = hcPlayers.get(player);
        if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
            if (!player.isInsideVehicle()) {
                // We have to change the game state to allow the teleport out of the world
                hcp.setState(PlayerState.ALIVE);
                hcp.updatePlayer(player);
                Util.teleport(player,getLobbyLocation(player,hcp.getWorld())).thenAccept(result -> {
                    if (result) {
                        broadCastToHardcore(header + ChatColor.YELLOW
                                + player.getDisplayName() + " has left "
                                + hcp.getWorld(), player.getName());
                        hcp.calcGameTime();
                        savePlayer(hcp);
                    } else {
                        // Teleport failed so set the game state back
                        hcp.setState(PlayerState.IN_GAME);
                        player.sendMessage(ChatColor.RED + "Teleportation failed.");
                    }
                });
            } else {
                player.sendMessage(ChatColor.RED + "You cannot leave while you are a passenger.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "You are not currently in a hardcore game.");
            // If player is in a hardcore world, send them to the lobby
            World world = player.getWorld();
            if (isHardcoreWorld(world)) {
                warn("Player " + player.getName()
                        + " was in hardcore world " + world.getName()
                        + " but not marked as IN_GAME, returning player to lobby.");
                player.sendMessage(ChatColor.RED + "This should not happen, please report it to an admin!");
                Util.teleport(player, getLobbyLocation(player, world.getName())).thenAccept(result -> {
                    if (result) {
                        player.sendMessage(ChatColor.GREEN + "You have been returned to the lobby.");
                    } else {
                        player.sendMessage(ChatColor.RED + "Teleportation to lobby failed. Please re-log.");
                    }
                });
            }
        }
    }

    /**
     * Called by {@link au.com.addstar.truehardcore.functions.WorldRollback} once every dimension
     * of a player's death has been rolled back successfully. Clears the pending flag so the
     * player is allowed back into the world. Must be called on the main thread.
     *
     * @param playerId  the player whose rollback completed
     * @param worldName any dimension of the world (suffix is stripped to find the record)
     */
    public void onRollbackComplete(UUID playerId, String worldName) {
        HardcorePlayer hcp = hcPlayers.get(worldName, playerId);
        if (hcp == null) {
            warn("Rollback completed for an unknown player record: " + worldName + "/" + playerId);
            return;
        }
        hcp.setRollbackPending(false);
        savePlayer(hcp);
        debug("Rollback fully complete for " + hcp.getWorld() + "/" + hcp.getPlayerName()
                + "; cleared rollback-pending.");
    }

    /**
     * Save a player.
     *
     * @param hcp the player
     * @return Save Task
     */
    @SuppressWarnings("UnusedReturnValue")
    public BukkitTask savePlayer(HardcorePlayer hcp) {
        return savePlayer(hcp, true,
              false);
    }

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    private BukkitTask savePlayer(HardcorePlayer hcp, boolean async) {
        return savePlayer(hcp, async, false);
    }

    private BukkitTask savePlayer(HardcorePlayer hcp, boolean async, final boolean autoSave) {
        if (hcp == null) {
            warn("SavePlayer called with null record!");
            return null;
        }

        if (autoSave) {
            debugLog("Auto-saving data for " + hcp.getWorld() + "/" + hcp.getPlayerName());
        } else {
            debug("Saving data for " + hcp.getWorld() + "/" + hcp.getPlayerName());
        }

        // CowKills, PigKills, SheepKills, ChickenKills;
        // CreeperKills, ZombieKills, SkeletonKills, SpiderKills, EnderKills, SlimeKills;
        // OtherKills, PlayerKills;

        //noinspection SpellCheckingInspection
        final String query = "INSERT INTO `players` (`id`, `player`, `world`, `spawnpos`,"
              + "`lastpos`, `lastjoin`, `lastquit`, `gamestart`, `gameend`, `gametime`,"
              + "`level`, `exp`, `score`, `topscore`, `state`, `deathmsg`, `deathpos`, "
              + "`deaths`,`cowkills`, `pigkills`, `sheepkills`, `chickenkills`, "
              + "`creeperkills`, `zombiekills`, `skeletonkills`,`spiderkills`, `enderkills`,"
              + " `slimekills`, `mooshkills`, `otherkills`, `playerkills`, `rollbackpending`,"
              + " `historypurged`)"
              + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
              + "ON DUPLICATE KEY UPDATE "
              + "`spawnpos`=?, `lastpos`=?, `lastjoin`=?, `lastquit`=?, `gamestart`=?, `gameend`=?, `gametime`=?,"
              + "`level`=?, `exp`=?, `score`=?, `topscore`=?, `state`=?, `deathmsg`=?, "
              + "`deathpos`=?, `deaths`=?, `cowkills`=?, `pigkills`=?, `sheepkills`=?, "
              + "`chickenkills`=?, `creeperkills`=?, `zombiekills`=?, `skeletonkills`=?, "
              + "`spiderkills`=?, `enderkills`=?, `slimekills`=?, `mooshkills`=?, `otherkills`=?,"
              + "`playerkills`=?, `rollbackpending`=?, `historypurged`=?\n";

        final String[] values = {
              hcp.getUniqueId().toString(),
              hcp.getPlayerName(),
              hcp.getWorld(),
              Util.loc2Str(hcp.getSpawnPos()),
              Util.loc2Str(hcp.getLastPos()),
              Util.date2Mysql(hcp.getLastJoin()),
              Util.date2Mysql(hcp.getLastQuit()),
              Util.date2Mysql(hcp.getGameStart()),
              Util.date2Mysql(hcp.getGameEnd()),
              String.valueOf(hcp.getGameTime()),
              String.valueOf(hcp.getLevel()),
              String.valueOf(hcp.getExp()),
              String.valueOf(hcp.getScore()),
              String.valueOf(hcp.getTopScore()),
              hcp.getState().toString(),
              hcp.getDeathMsg(),
              Util.loc2Str(hcp.getDeathPos()),
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
              hcp.isRollbackPending() ? "1" : "0",
              hcp.isHistoryPurged() ? "1" : "0",

              // REPEATED FOR UPDATE!
              Util.loc2Str(hcp.getSpawnPos()),
              Util.loc2Str(hcp.getLastPos()),
              Util.date2Mysql(hcp.getLastJoin()),
              Util.date2Mysql(hcp.getLastQuit()),
              Util.date2Mysql(hcp.getGameStart()),
              Util.date2Mysql(hcp.getGameEnd()),
              String.valueOf(hcp.getGameTime()),
              String.valueOf(hcp.getLevel()),
              String.valueOf(hcp.getExp()),
              String.valueOf(hcp.getScore()),
              String.valueOf(hcp.getTopScore()),
              hcp.getState().toString(),
              hcp.getDeathMsg(),
              Util.loc2Str(hcp.getDeathPos()),
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
              hcp.isRollbackPending() ? "1" : "0",
              hcp.isHistoryPurged() ? "1" : "0"
        };
        hcp.setModified(false);

        Runnable saveTask = () -> {
            try {
                int result = dbConnection.preparedUpdate(query, values, autoSave);
                if (result < 0) {
                    debug("Player record save failed!");
                    debug("Query: " + query);
                    debug("Values: " + Arrays.toString(values));
                }
            } catch (Exception e) {
                debug("Unable to save player record to database!");
                debug("Query: " + query);
                debug("Values: " + Arrays.toString(values));
                e.printStackTrace();
            }
        };

        BukkitTask task = null;
        if (async) {
            if (!autoSave) {
                debug("Launching async save task...");
            }
            task = getServer().getScheduler().runTaskAsynchronously(this, saveTask);
        } else {
            if (!autoSave) {
                debug("Saving synchronously...");
            }
            saveTask.run();
        }

        return task;
    }

    private void joinGame(String world, Player player) {
        debug("Joining game for " + player.getName());
        HardcorePlayer hcp = hcPlayers.get(world, player.getUniqueId());
        if (hcp != null) {
            if (hcp.getLastPos() != null) {
                debugLog("Returning player to: " + hcp.getLastPos());
                Util.loadChunk(hcp.getLastPos());
                Util.teleport(player,hcp.getLastPos()).thenAccept(result -> {
                    if (result) {
                        Bukkit.getScheduler().runTask(this, () -> {
                            player.setWalkSpeed(0.2F);
                            player.setFlySpeed(0.2F);
                            player.setGameMode(GameMode.SURVIVAL);
                            player.setOp(false);
                            player.setAllowFlight(false);
                            player.setFlying(false);
                            player.setFallDistance(0);
                            player.setNoDamageTicks(60);
                        });
                        broadCastToHardcore(header + ChatColor.GREEN + player.getDisplayName()
                                + " has entered " + hcp.getWorld(), player.getName());
                    } else {
                        warn("Teleport failed!");

                    }
                });
            }
        } else {
            warn("Player record NOT found!");
        }
    }

    /**
     * load players.
     *
     * @return true on success
     */
    @SuppressWarnings("UnusedReturnValue")
    private boolean loadAllPlayers() {
        String query = "SELECT * FROM `players` WHERE `world`=? ORDER BY id";
        try {
            hcPlayers.clear();
            ResultSet res = dbConnection.preparedQuery(query, new String[]{cfg.world});
            if (res != null) {
                while (res.next()) {
                    UUID id = UUID.fromString(res.getString("id"));
                    String name = res.getString("player");
                    String world = res.getString("world");
                    debugLog("Loading: " + world + "/" + name);
                    HardcorePlayer hcp = hcPlayers.newPlayer(world, id, name);
                    loadPlayerFromData(hcp, res);
                }
            }
        } catch (Exception e) {
            debug("Unable to load player records from database!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Load a player.
     *
     * @param world  world
     * @param player player
     * @return true on success
     */
    public boolean loadPlayer(String world, UUID player) {
        String query = "SELECT * FROM `players` WHERE `id`=? and `world`=?";
        try {
            debugLog("Reload player record from DB: " + world + "/" + player);
            ResultSet res = dbConnection.preparedQuery(query, new String[]{player.toString(),
                  world});
            HardcorePlayer hcp = hcPlayers.get(world, player);
            if ((res != null) && (hcp != null) && (res.next())) {
                debugLog("Loading: " + world + "/" + player);
                loadPlayerFromData(hcp, res);
            } else {
                return false;
            }
        } catch (Exception e) {
            debug("Unable to load player record to database!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private void loadPlayerFromData(HardcorePlayer hcp, ResultSet res) {
        try {
            hcp.setLoadDataOnly(true);
            hcp.setSpawnPos(Util.str2Loc(res.getString("spawnpos")));
            hcp.setLastPos(Util.str2Loc(res.getString("lastpos")));
            hcp.setLastJoin(Util.mysql2Date(res.getString("lastjoin")));
            hcp.setLastQuit(Util.mysql2Date(res.getString("lastquit")));
            hcp.setGameStart(Util.mysql2Date(res.getString("gamestart")));
            hcp.setGameEnd(Util.mysql2Date(res.getString("gameend")));
            hcp.setGameTime(res.getInt("gametime"));
            hcp.setLevel(res.getInt("level"));
            hcp.setExp(res.getInt("exp"));
            hcp.setScore(res.getInt("score"));
            hcp.setTopScore(res.getInt("topscore"));
            hcp.setState(PlayerState.valueOf(res.getString("state")));
            hcp.setDeathMsg(res.getString("deathmsg"));
            hcp.setDeathPos(Util.str2Loc(res.getString("deathpos")));
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
            hcp.setRollbackPending(res.getBoolean("rollbackpending"));
            hcp.setHistoryPurged(res.getBoolean("historypurged"));
            hcp.setModified(false);
            hcp.setLoadDataOnly(false);
        } catch (Exception e) {
            debug("Unable to load player record to database!");
            e.printStackTrace();
        }
    }

    /**
     * Save all players.
     */
    public void saveAllPlayers() {
        for (Map.Entry<String, HardcorePlayer> entry : hcPlayers.allRecords().entrySet()) {
            HardcorePlayer hcp = entry.getValue();
            if ((hcp != null) && (hcp.isModified())) {
                savePlayer(hcp, false);
            }
        }
    }

    private void saveInGamePlayers() {
        //Saving in game players synchronously
        for (Map.Entry<String, HardcorePlayer> entry : hcPlayers.allRecords().entrySet()) {
            HardcorePlayer hcp = entry.getValue();
            if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
                Player p = Bukkit.getPlayer(hcp.getPlayerName());
                if (p != null) {
                    hcp.updatePlayer(p);
                    savePlayer(hcp, true, true);
                }
            }
        }
    }

    /**
     * Resolve this instance's dimension worlds (overworld + nether + end) for a base world name.
     * Nulls (dimension not loaded) are included; callers/purgeHistory skip them.
     *
     * @param baseWorld the primary world name (dimension suffixes stripped)
     * @return the three dimension worlds, some possibly null
     */
    private List<World> worldsForBase(String baseWorld) {
        List<World> worlds = new ArrayList<>();
        worlds.add(getServer().getWorld(baseWorld));
        worlds.add(getServer().getWorld(baseWorld + "_nether"));
        worlds.add(getServer().getWorld(baseWorld + "_the_end"));
        return worlds;
    }

    /**
     * Periodic sweep (runs off the main thread) that purges the Prism history of players whose
     * most recent death is older than the configured retention. Each death is purged exactly once
     * thanks to the persisted historyPurged flag, so in steady state this does almost no work.
     * Eligible players are spaced out by the configured delay to avoid a burst of DB deletes.
     */
    private void sweepHistoryPurges() {
        if (!prismHooked) {
            return;
        }
        final long retentionMillis =
                (long) getCfg().historyPurgeRetentionDays * 24L * 60L * 60L * 1000L;
        final long now = System.currentTimeMillis();

        // Snapshot eligible records so we don't hold/iterate the live map across scheduling.
        final List<HardcorePlayer> eligible = new ArrayList<>();
        for (HardcorePlayer hcp : hcPlayers.allRecords().values()) {
            if (hcp == null || hcp.isHistoryPurged()
                    || hcp.getState() != PlayerState.DEAD || hcp.getGameEnd() == null) {
                continue;
            }
            if ((now - hcp.getGameEnd().getTime()) > retentionMillis) {
                eligible.add(hcp);
            }
        }
        if (eligible.isEmpty()) {
            return;
        }

        debug("History purge sweep: " + eligible.size() + " death(s) eligible.");
        final long delayTicks = Math.max(0L, (long) getCfg().historyPurgeDelaySeconds) * 20L;
        for (int i = 0; i < eligible.size(); i++) {
            final HardcorePlayer hcp = eligible.get(i);
            getServer().getScheduler().runTaskLaterAsynchronously(this,
                    () -> purgeHistoryForPlayer(hcp), i * delayTicks);
        }
    }

    /**
     * Purge one player's death history and, on success, mark it purged. Runs the Prism delete off
     * the main thread; the flag save hops back to the main thread. Safe to call for an already
     * purged record (re-checks the flag). Reused by the admin force-purge command.
     *
     * @param hcp the dead player's record
     */
    public void purgeHistoryForPlayer(HardcorePlayer hcp) {
        purgeHistoryForPlayer(hcp, false);
    }

    /**
     * Purge one player's death history and, on success, mark it purged.
     *
     * @param hcp   the dead player's record
     * @param force if true, purge even if already marked purged (admin/manual use)
     */
    public void purgeHistoryForPlayer(HardcorePlayer hcp, boolean force) {
        if (hcp == null || hcp.getGameEnd() == null) {
            return;
        }
        if (!force && hcp.isHistoryPurged()) {
            return;
        }
        if (rollbackHandler == null) {
            return;
        }
        List<World> worlds = worldsForBase(hcp.getWorld());
        boolean ok = rollbackHandler.purgeHistory(hcp.getPlayerName(), worlds, hcp.getGameEnd());
        if (ok) {
            // Touch player state + save on the main thread.
            getServer().getScheduler().runTask(this, () -> {
                hcp.setHistoryPurged(true);
                savePlayer(hcp);
                debug("Marked history purged for " + hcp.getWorld() + "/" + hcp.getPlayerName());
            });
        }
    }

    /**
     * true if a hardcore world.
     *
     * @param world the world.
     * @return boolean.
     */
    public boolean isHardcoreWorld(World world) {
        if (world != null) {
            return hardcoreWorlds.contains(world.getName());
        }
        return false;
    }

    /**
     * Get the lobby location.
     *
     * @param player player
     * @param world  world
     * @return the location.
     */
    public Location getLobbyLocation(Player player, String world) {
        Location loc = null;
        if (world != null) {
            HardcoreWorld hcw = hardcoreWorlds.get(world);
            loc = hcw.getExitPos();
        }

        if (loc == null) {
            warn("Sending " + player.getName() + " to world spawn!");
            String worldName = cfg.lobbyWorld;
            loc = Objects.requireNonNull(getServer().getWorld(worldName)).getSpawnLocation();
        }

        return loc;
    }

    private boolean isOnWhiteList(UUID player) {
        return isOnWhiteList(player, false);
    }

    private boolean isOnWhiteList(UUID player, boolean silent) {
        String query = "SELECT 1 FROM `whitelist` WHERE id=?";
        try {
            ResultSet res = dbConnection.preparedQuery(query, new String[]{player.toString()}, silent);
            if (res != null && res.next()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Add to the white list.
     *
     * @param player the player
     * @return true on success
     */
    public boolean addToWhitelist(UUID player) {
        String query = "INSERT IGNORE INTO `whitelist` (id) VALUES (?)";
        try {
            debugLog("Add player to whitelist: " + player);
            int result = dbConnection.preparedUpdate(query, new String[]{player.toString()});
            if (result < 0) {
                debug("Whitelist update failed!");
                return false;
            }
        } catch (Exception e) {
            debug("Unable to update whitelist!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getAccountType(Player player) {
        return getAccountType(player.getUniqueId());
    }

    public String getAccountType(UUID uuid) {
        return getAccountType(uuid, false);
    }

    public String getAccountType(UUID uuid, boolean silent) {
        String query = "SELECT type FROM `accounts` WHERE id=?";
        try {
            ResultSet res = dbConnection.preparedQuery(query, new String[]{uuid.toString()}, silent);
            if ((res != null) && (res.next())) {
                return res.getString("type");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean setAccountType(Player player, String type) {
        return setAccountType(player.getUniqueId(), player.getName(), type);
    }

    public boolean setAccountType(UUID uuid, String name, String type) {
        String query = "INSERT INTO `accounts` (id, playername, type) VALUES (?, ?, ?) "
              + "ON DUPLICATE KEY UPDATE playername=?, type=?";
        try {
            String[] params = {uuid.toString(), name, type, name, type};
            int result = dbConnection.preparedUpdate(query, params);
            if (result > 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isAltAccount(Player player) {
        return isAltAccount(player, false);
    }

    public boolean isAltAccount(Player player, boolean silent) {
        String query = "SELECT * FROM `tracking` WHERE ip=? AND id!=? "
              + "AND lastseen > DATE_SUB(NOW(), INTERVAL 7 DAY) LIMIT 1";
        try {
            String ip = player.getAddress().getAddress().getHostAddress();
            UUID uuid = player.getUniqueId();
            ResultSet res = dbConnection.preparedQuery(query, new String[]{ip.toString(), uuid.toString()}, silent);
            if ((res != null) && (res.next())) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean updateTracking(Player player) {
        String query = "INSERT INTO `tracking` SET id=?, ip=?, playername=?, firstseen=NOW(), lastseen=NOW() "
              + "ON DUPLICATE KEY UPDATE lastseen=NOW(), playername=?";
        try {
            String ip = player.getAddress().getAddress().getHostAddress();
            UUID uuid = player.getUniqueId();
            String[] params = {uuid.toString(), ip, player.getName(), player.getName()};
            int result = dbConnection.preparedUpdate(query, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Un vanish a player.
     *
     * @param player the player
     */
    public void unVanishPlayer(Player player) {
        if (isPlayerVanished(player)) {
            debug("UnVanishing: " + player.getName());
            VanishAPI.showPlayer(player);
        }
    }

    public boolean isPlayerVanished(Player player) {
        return (pvHooked) && (VanishAPI.isInvisible(player));
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean setProtected(HardcorePlayer hcp, long seconds) {
        if (hcp != null) {
            if (hcp.isGodMode()) {
                debug(hcp.getPlayerName() + " already in god mode!");
                return false;
            }

            final String world = hcp.getWorld();
            final UUID id = hcp.getUniqueId();
            final Player player = getServer().getPlayer(id);

            hcp.setGodMode(true);
            Objects.requireNonNull(player).sendMessage(ChatColor.YELLOW
                  + "You are now invincible for " + seconds + " seconds...");
            player.showTitle(getTitle(
                    "New life invincibility!",
                    "You are now invincible for " + seconds + " seconds...",
                    NamedTextColor.GREEN,
                    NamedTextColor.GRAY
            ));

            getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                HardcorePlayer hcp1 = hcPlayers.get(world, id);
                if (hcp1 != null) {
                    hcp1.setGodMode(false);
                    if (hcp1.getState() == PlayerState.IN_GAME) {
                        player.sendMessage(ChatColor.RED + "Your invincibility has now worn off... Good luck!");
                        player.showTitle(getTitle(
                            "Your invincibility ended!",
                            "You are no longer protected from all damage",
                            NamedTextColor.RED,
                            NamedTextColor.GRAY
                        ));
                    } else {
                        debug("Disable protection: Player " + id + " is no longer in game");
                    }
                } else {
                    debug("Disable protection: Player " + id + " does not exist!");
                }
            }, (seconds * 20));

        }
        return false;
    }

    private static @NotNull Title getTitle(String titletxt, String subtitletxt, NamedTextColor titleColor, NamedTextColor subtitleColor) {
        final Component mainTitle = Component.text(titletxt, titleColor);
        final Component subtitle = Component.text(subtitletxt, subtitleColor);
        final Title.Times times = Title.Times.times(
                Duration.ofMillis(500),   // fade in
                Duration.ofMillis(6000),  // duration
                Duration.ofMillis(1000)   // fade out
        );
        final Title title = Title.title(mainTitle, subtitle, times);
        return title;
    }

    /**
     * True if the player is save from monsters.
     *
     * @param player player
     * @param x      location x
     * @param y      location y
     * @param z      location z
     * @return boolean
     */
    public boolean isPlayerSafe(Player player, double x, double y, double z) {
        List<Entity> entities = player.getNearbyEntities(x, y, z);
        for (Entity e : entities) {
            if (e instanceof Monster) {
                return false;
            }
            if (e instanceof Slime) {
                return false;
            }
            if (e instanceof Ghast) {
                return false;
            }
        }
        return true;
    }

    private void broadcastToWorld(String world, String rawMsg) {
        String msg = ChatColor.translateAlternateColorCodes('&', rawMsg);
        debug(msg);
        for (final Player p : getServer().getOnlinePlayers()) {
            HardcorePlayer hcp = hcPlayers.get(world, p.getUniqueId());
            if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
                p.sendMessage(msg);
            }
        }
    }

    public void broadCastToHardcore(String rawmsg) {
        broadCastToHardcore(rawmsg, null);
    }

    /**
     * Broadcast to the Hardcore server excluding certain players.
     *
     * @param rawMsg        the message
     * @param excludePlayer the excluded players
     */
    public void broadCastToHardcore(String rawMsg, String excludePlayer) {
        String msg = ChatColor.translateAlternateColorCodes('&', rawMsg);
        debug("HardcoreBroadcast: " + msg);
        for (final Player p : getServer().getOnlinePlayers()) {
            // Skip the excluded player (if specified)
            if ((excludePlayer != null) && (excludePlayer.equals(p.getName()))) {
                continue;
            }
            if (isHardcoreWorld(p.getWorld())) {
                HardcorePlayer hcp = hcPlayers.get(p);
                if ((hcp != null) && (hcp.getState() == PlayerState.IN_GAME)) {
                    p.sendMessage(msg);
                }
            }
        }
    }

    /**
     * True if a location is inside the world border.
     *
     * @param loc the location to test
     * @return boolean.
     */
    public boolean insideWorldBorder(Location loc) {
        if (!wbHooked) {
            return true;
        }
        //noinspection ConstantConditions
        BorderData bd = wb.getWorldBorder(loc.getWorld().getName());
        boolean result = (bd != null) && (bd.insideBorder(loc));
        TrueHardcore.debug("  WorldBorder radius for " + loc.getWorld().getName() + ": " + bd.getRadius()
                + " => Location " + (int)loc.getX() + " " + (int)loc.getZ() + " inside border = " + result);
        return result;
    }

    /**
     * Broadcast to all servers.
     *
     * @param msg the message
     */
    public void broadcastToAllServers(String msg) {
        // Send ChatControl broadcast via Pandora
        MasterPlugin.getInstance().sendChatControlMessage(Bukkit.getConsoleSender(), cfg.broadcastChannel, msg);
    }

    public boolean isTeleportAllowed(UUID uuid) {
        return allowedTeleport.contains(uuid);
    }

    public void addAllowedTeleport(UUID uuid) {
        allowedTeleport.add(uuid);
        debug("Allowing player " + uuid + " to teleport temporarily");
    }

    public void removeAllowedTeleport(UUID uuid) {
        if (isTeleportAllowed(uuid))
            debug("Removing player " + uuid + " from allowed teleport list");
        allowedTeleport.remove(uuid);
    }

    public boolean isPlayerJoining(Player player) {
        synchronized (playersJoining) {
            return playersJoining.contains(player.getUniqueId());
        }
    }

    public int playerJoiningCount(HardcoreWorld world) {
        synchronized (playersJoiningWorld) {
            return playersJoiningWorld.getOrDefault(world, 0);
        }
    }

    public void setPlayerJoiningCount(HardcoreWorld world, Integer count) {
        // Ensure the count is never negative
        if (count < 0) count = 0;

        synchronized (playersJoiningWorld) {
            playersJoiningWorld.put(world, count);
        }
    }

    public void setPlayerJoining(Player player, boolean joining) {
        synchronized (playersJoining) {
            if (joining) {
                // Mark the player as currently joining a world
                // This is to prevent a player from joining again while waiting for a spawn location
                playersJoining.add(player.getUniqueId());
                TrueHardcore.debug("Player " + player.getName() + " marked as joining a world");
            } else {
                // No longer mark player as joining world
                playersJoining.remove(player.getUniqueId());
                TrueHardcore.debug("Player " + player.getName() + " no longer joining a world");
            }
        }
    }
}