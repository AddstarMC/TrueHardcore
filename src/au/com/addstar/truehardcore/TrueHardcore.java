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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.WorldBorder;

import de.diddiz.LogBlock.CommandsHandler.CommandClearLog;
import de.diddiz.LogBlock.CommandsHandler.CommandRollback;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.QueryParams;

public final class TrueHardcore extends JavaPlugin {
	public static TrueHardcore instance;
	
	public static Economy econ = null;
	public static Permission perms = null;
	public static Chat chat = null;
	public boolean VaultEnabled = false;
	public boolean DebugEnabled = false;
	public String HardcoreWorld = null;
	public List<String> RollbackCmds = null;
	public int DeathBan;
	public int SpawnProtection;
	
	private static final Logger logger = Logger.getLogger("Minecraft");
	public ConfigManager cfg = new ConfigManager(this);
	public PluginDescriptionFile pdfFile = null;
	public PluginManager pm = null;

	public Database dbcon = null;
	public String DBHost;
	public String DBPort;
	public String DBName;
	public String DBUser;
	public String DBPass;
	
	private Boolean LWCHooked = false;
	private Boolean LBHooked = false;
	private Boolean WBHooked = false;
	private LWC lwc;
	private LogBlock logblock;
	private WorldBorder wb;
	
	private final List<Material> SpawnBlocks = Arrays.asList(
			Material.DIRT, 
			Material.GRASS,
			Material.SAND,
			Material.STONE,
			Material.COBBLESTONE,
			Material.BEDROCK,
			Material.SNOW,
			Material.SNOW_BLOCK,
			Material.CLAY,
			Material.OBSIDIAN,
			Material.SANDSTONE
	);
	
	public String Header = ChatColor.DARK_RED + "[" + ChatColor.RED + "TrueHardcore" + ChatColor.DARK_RED + "] " + ChatColor.YELLOW;
			
	@Override
	public void onEnable(){
		// Register necessary events
		pdfFile = this.getDescription();
		pm = this.getServer().getPluginManager();
		pm.registerEvents(new PlayerListener(this), this);

		// Check if vault is loaded (required for economy)
		VaultEnabled = setupEconomy();
		if (VaultEnabled) {
			Log("Found Vault! Hooking for economy!");
		} else {
			Log("Vault was not detected! Economy rewards are not available.");
		}
		
		Plugin p = pm.getPlugin("LWC");
    	if (p != null && p instanceof LWCPlugin) {
    		LWCHooked = true;
    		lwc = ((LWCPlugin)p).getLWC();
    		Log("LWC Found, hooking into LWC.");
    	} else {
    		LWCHooked = false;
    		Log("LWC not Found");
    	}
    	
    	p = pm.getPlugin("LogBlock");
    	if (p != null && p instanceof LogBlock) {
    		LBHooked = true;
    		logblock = LogBlock.getInstance();
    		Log("LogBlock found, hooking it.");
    	} else {
    		LBHooked = false;
    		Log("LogBlock not found! This won't work very well...");
    	}

    	p = pm.getPlugin("WorldBorder");
    	if (p != null && p instanceof WorldBorder) {
    		WBHooked = true;
    		wb = WorldBorder.plugin;
    		Log("WorldBorder found, hooking it.");
    	} else {
    		WBHooked = false;
    		Log("WorldBorder not found! Spawning will not be limited...");
    	}

    	// Read (or initialise) plugin config file
		cfg.LoadConfig(getConfig());

		// Save the default config (if one doesn't exist)
		saveDefaultConfig();

		// Open/initialise the database
		dbcon = new Database(this);
		if (dbcon.IsConnected) {
			Log(pdfFile.getName() + " " + pdfFile.getVersion() + " has been enabled");
		} else {
			Log(pdfFile.getName() + " " + pdfFile.getVersion() + " could not be enabled!");
			this.setEnabled(false);
			return;
		}

		getCommand("truehardcore").setExecutor(new CommandTH(this));
		getCommand("th").setExecutor(new CommandTH(this));
	}
	
	@Override
	public void onDisable(){
		// cancel all tasks we created
        getServer().getScheduler().cancelTasks(this);
		
		Log(pdfFile.getName() + " has been disabled!");
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
	
	public void Log(String data) {
		logger.info(pdfFile.getName() + " " + data);
	}

	public void Warn(String data) {
		logger.warning(pdfFile.getName() + " " + data);
	}
	
	public void Debug(String data) {
		if (DebugEnabled) {
			logger.info(pdfFile.getName() + " " + data);
		}
	}

	public FileConfiguration Config() {
		return getConfig();
	}
	
	public Material GetMaterial(String name) {
		Material mat = Material.matchMaterial(name);
		if (mat != null) {
			return mat;
		}
		return null;
	}
	
	public boolean GiveMoney(String player, int money) {
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
	
	public boolean GiveItemStack(Player player, ItemStack itemstack) {
		PlayerInventory inventory = player.getInventory();
		HashMap result = inventory.addItem(itemstack);
		//TODO: Check "result" to ensure all items were given
		return true;
	}

	public ItemStack CreateStack(Material item, int datavalue, int amount) {
		ItemStack itemstack = new ItemStack(item, amount, (short)datavalue);
		return itemstack;
	}

	/*
	 * Check if the player has the specified permission
	 */
	public boolean HasPermission(Player player, String perm) {
		if (player instanceof Player) {
			// Real player
			if (player.hasPermission(perm)) {
				return true;
			}
		} else {
			// Console has permissions for everything
			return true;
		}
		return false;
	}
	
	/*
	 * Check required permission and send error response to player if not allowed
	 */
	public boolean RequirePermission(Player player, String perm) {
		if (!HasPermission(player, perm)) {
			if (player instanceof Player) {
				player.sendMessage(ChatColor.RED + "Sorry, you do not have permission for this command.");
				return false;
			}
		}
		return true;
	}

	/*
	 * Check if player is online
	 */
	public boolean IsPlayerOnline(String player) {
		if (player == null) { return false; }
		if (player == "") { return false; }
		if (this.getServer().getPlayer(player) != null) {
			// Found player.. they must be online!
			return true;
		}
		return false;
	}

	public void DoPlayerDeath(final Player player, PlayerDeathEvent event) {
		final TrueHardcore plugin = this;

		// Let's handle the death messages ourself  
		String DeathMsg = event.getDeathMessage();
		DeathMsg = DeathMsg.replaceFirst(player.getName(), ChatColor.AQUA + player.getName() + ChatColor.YELLOW);
		plugin.getServer().broadcastMessage(Header + DeathMsg + "!");
		plugin.getServer().broadcastMessage(Header + "Final Score: " + ChatColor.GREEN + player.getTotalExperience());
		event.setDeathMessage(null);

		// Dont drop XP or items
		event.setDroppedExp(0);
		event.getDrops().clear();
		
		// Reset XP levels
		event.setNewExp(0);
		event.setNewLevel(0);
		event.setNewTotalExp(0);
		event.setKeepLevel(false);
		player.setLevel(0);
		player.setExp(0);
		
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				try {
					if (plugin.LWCHooked) {
						plugin.Debug("Removing LWC locks...");
				        int count = 0;
						if (lwc.getPhysicalDatabase() != null) {
					        List<Protection> prots = lwc.getPhysicalDatabase().loadProtectionsByPlayer(player.getName());
					        for(Protection prot : prots) {
					        	if (prot.getWorld().equals(HardcoreWorld)) {
					        		count++;
		
					        		// Remove LWC protection
					        		prot.remove();
					        		prot.removeCache();
					        	}
					        }
						} else {
							plugin.Log("WARNING: LWC.getPhysicalDatabase() failed!");
						}
				        plugin.Debug("Removed " + count + " LWC protections.");
					}

					if (LBHooked) {
						try {
							final QueryParams params = new QueryParams(logblock);
							params.setPlayer(player.getName());
							params.world = plugin.getServer().getWorld(HardcoreWorld);
							params.silent = false;
							params.before = 0;
							params.excludeVictimsMode = true;
							params.excludeKillersMode = true;

							final CommandSender cs = plugin.getServer().getConsoleSender();

							if (logblock == null) {
								plugin.Debug("CRITICAL! logblock is null");
							} else {
								plugin.Debug("logblock is NOT null");
							}

							plugin.Debug("Rollback changes for " + player.getName() + "...");
							CommandRollback cr = plugin.logblock.getCommandsHandler().new CommandRollback(cs, params, true);
							cr.close();

							plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
								@Override
								public void run() {
									try {
										plugin.Debug("Clearing changes for " + player.getName() + "...");
										CommandClearLog ccl = plugin.logblock.getCommandsHandler().new CommandClearLog(cs, params, true);
										ccl.close();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}, 60L);
							
						} catch (Exception e) {
						    // Do nothing or throw an error if you want
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
				    // Do nothing or throw an error if you want
					e.printStackTrace();
				}
			}
		}, 40L);
	}
	
	public boolean StartGame(Player player) {
		int x = player.getLocation().getBlockX();
		int z = player.getLocation().getBlockZ();
		Location spawn = GetNewLocation(x, z, 3000);
		player.setNoDamageTicks(SpawnProtection * 20);
		player.teleport(spawn);
		player.setHealth(20);
		player.setFoodLevel(20);
		player.setAllowFlight(false);
		player.setFlying(false);
		player.setExp(0);
		player.setLevel(0);
		player.setTotalExperience(0);
		player.setWalkSpeed(0.2F);
		player.setFlySpeed(0.2F);
		player.setGameMode(GameMode.SURVIVAL);
		player.sendMessage(ChatColor.GREEN + "Welcome to TrueHardcore. Good luck on your adventure!");
		player.sendMessage(ChatColor.YELLOW + "You are invincible for 60 seconds...");
		return true;
	}
	
	public Location GetNewLocation(int oldX, int oldZ, int dist) {
		Location l = new Location(getServer().getWorld(HardcoreWorld), oldX, 255, oldZ);
		Debug(l.getBlockX() + " / " + l.getBlockY() + " / " + l.getBlockZ());

		double x;
		double z;
		int deg;
		Location nl = null;
		
		for (int count = 0; count < 30; count++) {
			dist = dist + (int) (Math.random() * 100);									// Random radius padding
			deg = (int) (Math.random() * 360);											// Random degrees
			Debug("Deg : " + deg);
			x = (dist * Math.cos(Math.toRadians(deg))) + l.getBlockX();  
			z = (dist * Math.sin(Math.toRadians(deg))) + l.getBlockZ(); 
			nl = new Location(getServer().getWorld(HardcoreWorld), x, 255, z);
			//Debug(nl.getBlockX() + " / " + nl.getBlockY() + " / " + nl.getBlockZ());
			Debug("Distance: " + l.distance(nl));

			// Get the highest block at the selected location
			Block b = nl.getBlock();
			while((b.getType() == Material.AIR) && (b.getY() > 1)) {
				b = b.getRelative(BlockFace.DOWN);
			}

			Debug(b.getLocation().getBlockX() + " / " + b.getLocation().getBlockY() + " / " + b.getLocation().getBlockZ());
			Debug(b.getType().toString());

			// Get worldborder
			BorderData bd = wb.GetWorldBorder(HardcoreWorld);
			
			if (SpawnBlocks.contains(b.getType())) {
				Location spawn = new Location(b.getWorld(), b.getX(), b.getY()+2, b.getZ());
				if (spawn.getBlockX() >= 0) { spawn.setX(spawn.getBlockX() + 0.5); }
				if (spawn.getBlockX() < 0)  { spawn.setX(spawn.getBlockX() - 0.5); }

				if (spawn.getBlockZ() >= 0) { spawn.setZ(spawn.getBlockZ() + 0.5); }
				if (spawn.getBlockZ() < 0)  { spawn.setZ(spawn.getBlockZ() - 0.5); }
				
				if (bd.insideBorder(spawn)) {
					Debug(spawn.getX() + " / " + spawn.getY() + " / " + spawn.getZ());
					Debug("GOOD SPAWN LOCATION!");
					spawn.setPitch(0F);
					spawn.setYaw(0F);
					return spawn;
				} else {
					Debug("OUTSIDE WORLD!!");
				}
			} else {
				Debug("WRONG BLOCK!!");
			}
			Debug("======================================");
		}
		
		return null;
	}
}
