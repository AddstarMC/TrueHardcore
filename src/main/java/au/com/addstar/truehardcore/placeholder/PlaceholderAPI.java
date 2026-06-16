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
import au.com.addstar.truehardcore.objects.AccessState;
import au.com.addstar.truehardcore.objects.HardcoreWorlds.HardcoreWorld;
import au.com.addstar.truehardcore.objects.HardcorePlayers.HardcorePlayer;
import au.com.addstar.truehardcore.objects.HardcorePlayers.PlayerState;
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
            return TrueHardcore.getCfg().world;
        }
        else if (identifier.equals("canplay")) {
            String world = TrueHardcore.getCfg().world;
            return plugin.getAccessState(world, player).canPlay() ? "yes" : "no";
        }
        else if (identifier.equals("access_reason")) {
            String world = TrueHardcore.getCfg().world;
            AccessState state = plugin.getAccessState(world, player);
            if (state == AccessState.COOLDOWN) {
                // Append the remaining cooldown so the hologram shows how long is left
                long minutes = cooldownMinutes(world, player);
                return state.getReason() + ": " + minutes + "m";
            }
            return state.getReason();
        }
        else if (identifier.startsWith("cooldown_mins_")) {
            String worldName = identifier.substring("cooldown_mins_".length());
            if (plugin.hardcoreWorlds.get(worldName) == null) {
                return String.valueOf(0); // No such world
            }
            return String.valueOf(cooldownMinutes(worldName, player));
        }
        return null;
    }

    /**
     * Remaining death-cooldown for a player in a world, rounded up to whole minutes.
     *
     * @return minutes left before the player may play again, or 0 if not on cooldown
     */
    private long cooldownMinutes(final String worldName, final OfflinePlayer player) {
        long remaining = 0;
        HardcoreWorld hcw = plugin.hardcoreWorlds.get(worldName);
        if (hcw == null) {
            return 0;
        }
        HardcorePlayer hcp = plugin.hcPlayers.get(worldName, player.getUniqueId());
        if (hcp != null && hcp.getState() == PlayerState.DEAD && hcp.getGameEnd() != null) {
            long diff = (System.currentTimeMillis() - hcp.getGameEnd().getTime()) / 1000;
            long wait = hcw.getBantime() - diff;
            if (wait > remaining) {
                remaining = wait;
            }
        }
        return (remaining + 59) / 60;
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

