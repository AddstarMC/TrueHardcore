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
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 5/02/2020.
 */
public class ThConfig extends AutoConfig {


    @ConfigField(comment = "Set true to enable debug mode ")
    public static Boolean debugEnabled = false;
    @ConfigField(comment = "A list of worlds")
    public HashSet<String> worlds = new HashSet<>();
    @ConfigField(comment = "Set true to enable ")
    public Boolean gameEnabled = true;

    @ConfigField(comment = "The Bungeechat channel name to broadcast messages")
    public String broadcastChannel = "GamesBCast";

    @ConfigField(name = "AutoSaveEnabled", comment = "Boolean - set true to enable autosaving")
    public Boolean autoSaveEnabled = false;

    @ConfigField(category = "Database", name = "Host",
          comment = "The ip or hostname of the database")
    public String host;

    @ConfigField(category = "Database", name = "Port",
          comment = "The port of the database")
    public String port;

    @ConfigField(category = "Database", name = "Name",
          comment = "The Name of the database")
    public String name;

    @ConfigField(category = "Database", name = "Username",
          comment = "The Username of the database")
    public String user;

    @ConfigField(category = "Database", name = "Password",
          comment = "The password of the database")
    public String password;
    @ConfigField(category = "CombatLogPrevention", name = "enabled",
          comment = "This prevents a player quiting hardcore properly while in combat ")
    public Boolean antiCombatLog = false;
    @ConfigField(category = "CombatLogPrevention",
          comment = "How long to hold them in combat in s")
    public Integer combatTime = 30;
    @ConfigField(category = "Difficulty",
          comment = "Set from 0 - 50 will make a chunk harder on initial load")
    public Integer baseChunkTime = 0;
    @ConfigField(comment = "The lobby game world name")
    public String lobbyWorld = "games";

    private Map<String, HardcoreWorldConfig> configs;
    private File pluginDirectory;

    /**
     * Create a config.
     * @param file plugin config.yml file
     * @param pluginName name of the plugin.
     */
    public ThConfig(File file, String pluginName) {
        super(file, pluginName);
        pluginDirectory = file.getParentFile();
        configs = new HashMap<>();
    }


    @Override
    protected void onPostLoad(YamlConfiguration yaml) throws InvalidConfigurationException {
        super.onPostLoad(yaml);
        TrueHardcore.debug("Loading worlds...");
        for (String world : worlds) {
            File file = new File(pluginDirectory, world + ".yml");
            HardcoreWorldConfig hcwConfig
                  = new HardcoreWorldConfig(file, "TrueHardCore: " + world, world);
            hcwConfig.load();
            configs.put(world, hcwConfig);
        }
    }

    @Override
    protected void onPreSave() {
        for (Map.Entry<String, HardcoreWorldConfig> e : configs.entrySet()) {
            e.getValue().save();
        }
    }

    @Nullable
    public HardcoreWorldConfig getWorldConfig(String worldName) {
        return configs.get(worldName);
    }
}
