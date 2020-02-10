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

package au.com.addstar.truehardcore.objects;

import au.com.addstar.truehardcore.config.HardcoreWorldConfig;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HardcoreWorlds {
    private final Map<String, HardcoreWorld> worlds;

    public HardcoreWorlds() {
        worlds = new HashMap<>();
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addWorld(String world, HardcoreWorld hcw) {
        worlds.put(world, hcw);
        return true;
    }

    /**
     * Get a world.
     * @param world string
     * @return hardcore world
     */
    public HardcoreWorld get(String world) {
        String key = StringUtils.replace(world, "_nether", "");
        if (worlds.containsKey(key)) {
            return worlds.get(key);
        }
        return null;
    }

    /**
     * Check if world present.
     * @param world string
     * @return bool
     */
    public boolean contains(String world) {
        String key = StringUtils.replace(world, "_nether", "");
        return worlds.containsKey(key);
    }

    /**
     * get all records.
     * @return map
     */
    public Map<String, HardcoreWorld> allRecords() {
        return worlds;
    }

    public String getNames() {
        List<String> names = new ArrayList<>(worlds.keySet());
        return StringUtils.join(names, ",");
    }

    @SuppressWarnings("unused")
    public static class HardcoreWorld {
        private World world;
        private HardcoreWorldConfig config;

        public HardcoreWorld(World world, HardcoreWorldConfig config) {
            this.world = world;
            this.config = config;
        }

        public World getWorld() {
            return world;
        }

        public void setWorld(World world) {
            this.world = world;
        }

        public String getGreeting() {
            return config.greeting;
        }

        public void setGreeting(String greeting) {
            this.config.greeting = greeting;
        }

        public Integer getBantime() {
            return config.banTime;
        }

        public void setBantime(Integer bantime) {
            this.config.banTime = bantime;
        }

        public Integer getSpawnDistance() {
            return config.spawnDistance;
        }

        public void setSpawnDistance(Integer spawndistance) {
            this.config.spawnDistance = spawndistance;
        }

        public Integer getSpawnProtection() {
            return config.spawnProtection;
        }

        public void setSpawnProtection(Integer spawnprotection) {
            this.config.spawnProtection = spawnprotection;
        }

        public Location getExitPos() {
            return config.exitLocation;
        }

        public void setExitPos(Location exitpos) {
            this.config.exitLocation = exitpos;
        }

        public Integer getRollbackDelay() {
            return config.rollbackdelay;
        }

        public void setRollbackDelay(Integer rollbackdelay) {
            this.config.rollbackdelay = rollbackdelay;
        }

        public Boolean getDeathDrops() {
            return config.deathdrops;
        }

        public void setDeathDrops(Boolean deathdrops) {
            this.config.deathdrops = deathdrops;
        }

        public Boolean isWhitelisted() {
            return config.whitelisted;
        }

        public void setWhitelisted(Boolean whitelisted) {
            this.config.whitelisted = whitelisted;
        }

        public Difficulty getDifficulty() {
            return config.bukkitDifficulty;
        }

        public void setDifficulty(Difficulty difficulty) {
            this.config.bukkitDifficulty = difficulty;
        }

        protected boolean checkandSetDifficulty() {
            if (world.getDifficulty() != getDifficulty()) {
                world.setDifficulty(getDifficulty());
            }
            return world.getDifficulty() == getDifficulty();
        }
    }

}