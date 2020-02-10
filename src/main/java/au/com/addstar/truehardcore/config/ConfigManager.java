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
import java.util.Set;

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
        //write to a file
        config.save();
        // Get the list of worlds
        Set<String> worlds = config.worlds;

        // Load each world's settings
        if (worlds != null) {
            TrueHardcore.debug("Setting up worlds...");

            for (String w : worlds) {
                World world = Bukkit.getWorld(w);
                if (world == null) {
                    TrueHardcore.log("No world found named:" + w);
                    continue;
                }
                HardcoreWorld hcw = new HardcoreWorld(world, config.getWorldConfig(w));
                plugin.hardcoreWorlds.addWorld(world.getName(), hcw);
            }
        } else {
            TrueHardcore.warn("No worlds configured! Things will not work!");
        }
    }
}
