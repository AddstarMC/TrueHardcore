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

import java.util.Set;

import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import au.com.addstar.truehardcore.HardcoreWorlds.*;

class ConfigManager {

    private final TrueHardcore plugin;
    public ConfigManager(TrueHardcore instance) {
        plugin = instance;
    }
    
    private FileConfiguration Config() {
        return plugin.getConfig();
    }
    
    public void LoadConfig(FileConfiguration config) {
        config.options().copyDefaults(true);
        
        TrueHardcore.DebugEnabled = Config().getBoolean("debug");

        // Get the list of worlds
        Set<String> worlds = Config().getConfigurationSection("worlds").getKeys(false);

        // Load each world's settings
        TrueHardcore.Debug("Loading worlds...");
        if (worlds != null) {
            for (String w : worlds) {
                TrueHardcore.Debug("Found World: " + w);
                World world = plugin.getServer().getWorld(w);
                if (world != null) {
                    HardcoreWorld hcw = plugin.HardcoreWorlds.NewWorld(world.getName());
                    hcw.setWorld(world);
                    hcw.setGreeting(Config().getString("worlds." + w + ".greeting"));
                    hcw.setBantime(Config().getInt("worlds." + w + ".ban-time", 43200));				// Default = 12h
                    hcw.setSpawnDistance(Config().getInt("worlds." + w + ".spawn-distance", 5000));		// Default = 5000
                    hcw.setSpawnProtection(Config().getInt("worlds." + w + ".protection-time", 60));	// Default = 5000
                    hcw.setExitPos(Util.Str2Loc(Config().getString("worlds." + w + ".exitpos")));	    // Default = null
                    hcw.setRollbackDelay(Config().getInt("worlds." + w + ".rollback-delay", 0));		// Default = 0
                    hcw.setDeathDrops(Config().getBoolean("worlds." + w + ".death-drops", false));		// Default = false
                    hcw.setWhitelisted(Config().getBoolean("worlds." + w + ".whitelisted", true));		// Default = true
                    Difficulty difficulty;
                    if(Config().getString("worlds." + w +".difficulty",null) != null) {
                        try {
                            difficulty = Difficulty.valueOf(Config().getString("worlds." + w + ".difficulty", "HARD"));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Config for " + w + " world chunk difficulty was not formatted correctly");
                            difficulty = Difficulty.HARD;
                        }
                        hcw.setDifficulty(difficulty);
                        world.setDifficulty(difficulty);
                    } else {
                        hcw.setDifficulty(world.getDifficulty());
                    }
                    plugin.HardcoreWorlds.AddWorld(world.getName(), hcw);
                }
            }
        } else {
            TrueHardcore.Warn("No worlds configured! Things will not work!");
        }

        // Database settings
        plugin.DBHost = Config().getString("mysql.host", "localhost");
        plugin.DBPort = Config().getString("mysql.port", "3306");
        plugin.DBName = Config().getString("mysql.database", "truehardcore");
        plugin.DBUser = Config().getString("mysql.username", "truehardcore");
        plugin.DBPass = Config().getString("mysql.password", "truehardcore");

        // BungeeChat broadcast channel
        plugin.BroadcastChannel = Config().getString("broadcast-channel", "GamesBCast");
        plugin.AutoSaveEnabled = Config().getBoolean("auto-save", false);
        plugin.antiCombatLog = Config().getBoolean("combat.Anti-Log", false);
        plugin.combatTime = Config().getInt("combat.time",30)*1000;

        //Difficulty Settings
        //Chunk Base time in hours - minimum 0 maximum 50
        plugin.baseChunkTime =  Config().getInt("chunk.baseTime",0)*60*60*20;
    }
}
