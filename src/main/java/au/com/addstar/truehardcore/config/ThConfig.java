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

package au.com.addstar.truehardcore.config;

import au.com.addstar.monolith.util.configuration.AutoConfig;
import au.com.addstar.monolith.util.configuration.ConfigField;
import au.com.addstar.truehardcore.TrueHardcore;
import au.com.addstar.truehardcore.functions.Util;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 5/02/2020.
 */
public class ThConfig extends AutoConfig {


    @ConfigField(comment = "Set true to enable debug mode ")
    public boolean debugEnabled = false;

    @ConfigField(comment = "The hardcore world for this server")
    public String world = "";

    @ConfigField(comment = "Set true to enable ")
    public boolean gameEnabled = true;

    @ConfigField(comment = "The ChatControlRed channel name to broadcast messages")
    public String broadcastChannel = "GamesBCast";

    @ConfigField(name = "AutoSaveEnabled", comment = "Boolean - set true to enable autosaving")
    public boolean autoSaveEnabled = false;

    @ConfigField(category = "Database", name = "Host",
          comment = "The ip or hostname of the database")
    public String host = "localhost";

    @ConfigField(category = "Database", name = "Port",
          comment = "The port of the database")
    public String port = "3306";

    @ConfigField(category = "Database", name = "Name",
          comment = "The Name of the database")
    public String name = "hardcore";

    @ConfigField(category = "Database", name = "Username",
          comment = "The Username of the database")
    public String user = "username";

    @ConfigField(category = "Database", name = "Password",
          comment = "The password of the database")
    public String password = "password";

    @ConfigField(category = "Difficulty",
          comment = "Set from 0 - 50 will make a chunk harder on initial load")
    public int baseChunkTime = 0;

    @ConfigField(comment = "The lobby world name")
    public String lobbyWorld = "lobby";

    // World-specific settings

    public Location exitLocation;
    public Difficulty bukkitDifficulty = Difficulty.HARD;

    @ConfigField(category = "world-settings", comment = "Greeting on World Entry")
    public String greeting = "Welcome to Hardcore";

    @ConfigField(category = "world-settings", comment = "Time you are banned on death. Default 12hrs(43200s)")
    public int banTime = 43200;

    @ConfigField(category = "world-settings", comment = "Potential distance to look for random spawn")
    public int spawnDistance = 5000;

    @ConfigField(category = "world-settings", comment = "Time you are safe after respawing, Default 60sec")
    public int spawnProtection = 60;

    @ConfigField(category = "world-settings", comment = "A location: worldname,x,y,z,pitch,yaw")
    public String exitPos = "lobby,0,0,0,0,-2";

    @ConfigField(category = "world-settings", comment = "The time in seconds before the world rolls back after death, Default 0 seconds")
    public int rollbackdelay = 0;

    @ConfigField(category = "world-settings", comment = "Enable periodic purge of a dead player's Prism activity history")
    public boolean historyPurgeEnabled = true;

    @ConfigField(category = "world-settings", comment = "Days after a death before that death's Prism history is purged. Default 7 days")
    public int historyPurgeRetentionDays = 7;

    @ConfigField(category = "world-settings", comment = "Real-time minutes between history-purge sweeps. Default 1440 (24h)")
    public int historyPurgeIntervalMinutes = 1440;

    @ConfigField(category = "world-settings", comment = "Real-time seconds to wait between purging each player in a sweep (spreads DB load). Default 5s")
    public int historyPurgeDelaySeconds = 5;

    @ConfigField(category = "world-settings", comment = "Does the player drop items on death")
    public boolean deathdrops = true;

    @ConfigField(category = "world-settings", comment = "How long to keep a chunk loaded after death. Default 5mins (300s)")
    public int chunkHoldOnDeath = 300;

    @ConfigField(category = "world-settings", comment = "This prevents a player quiting hardcore properly while in combat")
    public boolean antiCombatLog = false;

    @ConfigField(category = "world-settings", comment = "How long to consider players in combat mode, in seconds")
    public int combatTime = 30;

    @ConfigField(category = "world-settings", comment = "Does this world use the whitelist")
    public boolean whitelisted = true;

    @ConfigField(category = "world-settings", comment = "World difficulty: PEACEFUL, NORMAL, HARD")
    public String difficulty = "HARD";

    @ConfigField(category = "world-settings",
          comment = "Command to be run on death. Can use <player> <displayname> <world> <score> <cause> %place_holders%")
    public String deathcommand = "";

    @ConfigField(category = "world-settings",
          comment = "Command to be run when a player kills the Ender Dragon. Can use <player> <displayname> <world> <score> <cause> %place_holders%")
    public String dragonkillcommand = "";

    @ConfigField(category = "world-settings",
          comment = "Command to be run when a player respawns the Ender Dragon. Can use <player> <displayname> <world> <score> <cause> %place_holders%")
    public String dragonrespawncommand = "";

    @ConfigField(category = "world-settings",
          comment = "Command to be run when a player starts a new life. Can use <player> <displayname> <world> <score> <cause> %place_holders%")
    public String newlifecommand = "";

    @ConfigField(category = "world-settings", comment = "Message to be sent to all players in the world when someone dies")
    public String rollbackBroadcast = "&eYou now have %time% to raid &b%player%'s &estuff before it all disappears!";

    /**
     * Create a config.
     * @param file plugin config.yml file
     * @param pluginName name of the plugin.
     */
    public ThConfig(File file, String pluginName) {
        super(file, pluginName);
        final List<String> desc = new ArrayList<>();
        desc.add("HardCore Configuration");
        setDescription(desc);
    }

    @Override
    protected void onPostLoad(YamlConfiguration yaml) throws InvalidConfigurationException {
        try {
            exitLocation = Util.str2Loc(exitPos);
        } catch (IllegalArgumentException e) {
            exitLocation = null;
            TrueHardcore.warn("Exit location world is not available ('" + exitPos
                  + "'). Please update exitPos in config.yml.");
        }
        try {
            bukkitDifficulty = Difficulty.valueOf(difficulty);
        } catch (IllegalArgumentException e) {
            bukkitDifficulty = Difficulty.HARD;
            TrueHardcore.warn("Invalid difficulty '" + difficulty + "' in config, defaulting to HARD.");
        }
    }

    @Override
    protected void onPreSave() {
        if (exitLocation != null) {
            exitPos = Util.loc2Str(exitLocation);
        }
        difficulty = bukkitDifficulty.name();
    }
}
