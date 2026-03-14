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

import au.com.addstar.truehardcore.TrueHardcore;
import au.com.addstar.truehardcore.objects.HardcoreWorlds.HardcoreWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;

public class ConfigManager {
    private final TrueHardcore plugin;
    private ThConfig config;

    /**
     * Constructor.
     * @param instance plugin
     */
    public ConfigManager(TrueHardcore instance) {
        plugin = instance;
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = new ThConfig(configFile, plugin.getName());
    }

    public ThConfig getConfig() {
        return config;
    }

    /**
     * Load the config.
     */
    public void loadConfig() {
        config.load();
        config.save();

        String worldName = config.world;
        if (worldName == null || worldName.isEmpty()) {
            TrueHardcore.warn("No world configured! Things will not work!");
            return;
        }

        TrueHardcore.log("Setting up world: " + worldName);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            TrueHardcore.warn("No world found named: " + worldName);
            return;
        }
        HardcoreWorld hcw = new HardcoreWorld(world, config);
        plugin.hardcoreWorlds.addWorld(world.getName(), hcw);
    }
}
