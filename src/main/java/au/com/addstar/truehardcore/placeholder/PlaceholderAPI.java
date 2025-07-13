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
import au.com.addstar.truehardcore.objects.HardcoreWorlds.HardcoreWorld;
import au.com.addstar.truehardcore.objects.HardcorePlayers.HardcorePlayer;
import au.com.addstar.truehardcore.objects.HardcorePlayers.PlayerState;
import java.util.Map;
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
        else if (identifier.equals("cooldown_mins")) {
            long now = System.currentTimeMillis();
            long remaining = 0;

            for (Map.Entry<String, HardcoreWorld> entry : plugin.hardcoreWorlds.allRecords().entrySet()) {
                HardcorePlayer hcp = plugin.hcPlayers.get(entry.getKey(), player.getUniqueId());
                HardcoreWorld hcw = entry.getValue();

                if (hcp != null && hcp.getState() == PlayerState.DEAD && hcp.getGameEnd() != null) {
                    long diff = (now - hcp.getGameEnd().getTime()) / 1000;
                    long wait = hcw.getBantime() - diff;
                    if (wait > remaining) {
                        remaining = wait;
                    }
                }
            }

            if (remaining < 0) {
                remaining = 0;
            }

            long minutes = (remaining + 59) / 60;
            return String.valueOf(minutes);
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

