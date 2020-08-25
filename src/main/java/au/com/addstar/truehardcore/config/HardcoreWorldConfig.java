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
import au.com.addstar.truehardcore.functions.Util;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 6/02/2020.
 */
public class HardcoreWorldConfig extends AutoConfig {

    private String worldName;
    public Location exitLocation;
    public Difficulty bukkitDifficulty = Difficulty.HARD;

    @ConfigField(comment = "Greeting on World Entry")
    public String greeting = "Welcome to Hardcore";

    @ConfigField(comment = "Time you are banned on death. Default 12hrs(43200s)")
    public int banTime = 43200;

    @ConfigField(comment = "Potential distance to look for random spawn")
    public int spawnDistance = 5000;

    @ConfigField(comment = "Time you are safe after respawing, Default 60sec")
    public int spawnProtection = 60;

    @ConfigField(comment = "A location: worldname,x,y,z,pitch,yaw")
    public String exitPos = "lobby,0,0,0,0,-2";

    @ConfigField(comment = "The time before the world rolls back after death, Default 0 ticks")
    public int rollbackdelay = 0;

    @ConfigField(comment = "Does the player drop items on death")
    public boolean deathdrops = true;

    @ConfigField(comment = "How long to keep a chunk loaded after death.")
    public int chunkHoldOnDeath = 6000;

    @ConfigField(comment = "Does this world use the whitelist")
    public boolean whitelisted = true;

    @ConfigField(comment = "World difficulty: PEACEFUL, NORMAL, HARD")
    public String difficulty = "HARD";

    @ConfigField(comment = "Command to be run on death (can use <player> <displayname> <score> <cause>")
    public String deathcommand = "";

    /**
     * Constructor.
     *
     * @param file       a file.
     * @param pluginName the plugin creating this config.
     */
    protected HardcoreWorldConfig(File file, String pluginName) {
        super(file, pluginName);
    }

    /**
     * Constructor.
     * @param file the config file
     * @param pluginName the plugin name
     * @param worldName the world this config is for
     */
    protected HardcoreWorldConfig(File file,String pluginName, String worldName) {
        this(file,pluginName);
        this.worldName = worldName;
        final List<String> desc = new ArrayList<>();
        desc.add("World Configuration for:" + worldName);
        setDescription(desc);
    }

    @Override
    protected void onPreSave() {
        exitPos = Util.loc2Str(exitLocation);
        difficulty = bukkitDifficulty.name();
    }

    @Override
    protected void onPostLoad(YamlConfiguration yaml) throws InvalidConfigurationException {

        exitLocation = Util.str2Loc(exitPos);
        try {
            bukkitDifficulty = Difficulty.valueOf(difficulty);
        } catch (IllegalArgumentException e) {
            bukkitDifficulty = Difficulty.HARD;
            InvalidConfigurationException er = new InvalidConfigurationException("Invalid config");
            er.addSuppressed(e);
            throw er;
        }
    }

}
