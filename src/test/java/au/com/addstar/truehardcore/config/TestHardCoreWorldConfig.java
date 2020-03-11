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

import au.com.addstar.truehardcore.objects.MockWorld;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 10/03/2020.
 */
public class TestHardCoreWorldConfig extends HardcoreWorldConfig {
    protected TestHardCoreWorldConfig(File file, String pluginName) {
        super(file, pluginName);
    }

    protected TestHardCoreWorldConfig(File file, String pluginName, String worldName) {
        super(file, pluginName, worldName);
    }

    @Override
    protected void onPreSave() {
        super.onPreSave();
    }

    @Override
    protected void onPostLoad(YamlConfiguration yaml) throws InvalidConfigurationException {
        try {
            bukkitDifficulty = Difficulty.valueOf(difficulty);
        } catch (IllegalArgumentException e) {
            bukkitDifficulty = Difficulty.HARD;
            InvalidConfigurationException er = new InvalidConfigurationException("Invalid config");
            er.addSuppressed(e);
            throw er;
        }
        exitLocation = testLocation(exitPos);

    }

    private Location testLocation(String input){
            if (input == null) {
                return null;
            }

            Location loc;
            String[] parts = input.split(",");
            World world = new MockWorld("test");
            try {
                loc = new Location(world,
                      Double.parseDouble(parts[1]),
                      Double.parseDouble(parts[2]),
                      Double.parseDouble(parts[3]),
                      Float.parseFloat(parts[4]),
                      Float.parseFloat(parts[5]));
                return loc;
            } catch (NumberFormatException | NullPointerException e) {
                IllegalArgumentException ex = new IllegalArgumentException("Location could not be "
                      + "determined: " + input);
                ex.addSuppressed(e);
                throw ex;
            }
        }
}
