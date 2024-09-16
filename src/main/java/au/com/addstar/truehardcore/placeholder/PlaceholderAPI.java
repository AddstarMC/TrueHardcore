/*
 * TrueHardcore
 * Copyright (C) 2013 - 2024  AddstarMC <copyright at addstar dot com dot au>
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

package au.com.addstar.truehardcore.placeholder;

import au.com.addstar.truehardcore.TrueHardcore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PlaceholderAPI extends PlaceholderExpansion {
    public TrueHardcore plugin;
    public PlaceholderAPI(final TrueHardcore plugin) {
        this.plugin = plugin;
    }
    public String onRequest(final OfflinePlayer player, final String identifier) {
        if (identifier.equals("status")) {
            return plugin.getConfig().getString("gameEnabled");
        }
        else if (identifier.equals("world")) {
            return plugin.hardcoreWorlds.getNames();
        }
        return null;
    }

    @Override
    public String getAuthor() {
        return "AddstarMC";
    }

    @Override
    public String getIdentifier() {
        return "TrueHardcore";
    }

    @Override
    public String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }
}

