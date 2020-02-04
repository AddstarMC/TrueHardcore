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

package au.com.addstar.truehardcore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;

public class HardcoreWorlds {
    private final Map<String, HardcoreWorld> Worlds;

    public HardcoreWorlds() {
        Worlds = new HashMap<>();
    }
    
    static class HardcoreWorld {
        public HardcoreWorld() {
            // Nothing needed here yet
        }
        
        private World world;
        private String greeting;
        private Integer bantime;
        private Integer spawndistance;
        private Integer spawnprotection;
        private Location exitpos;
        private Integer rollbackdelay;
        private Boolean deathdrops;
        private Boolean whitelisted;
        private Difficulty difficulty;

        public World getWorld() {
            return world;
        }
        public void setWorld(World world) {
            this.world = world;
        }

        public String getGreeting() {
            return greeting;
        }
        public void setGreeting(String greeting) {
            this.greeting = greeting;
        }

        public Integer getBantime() {
            return bantime;
        }
        public void setBantime(Integer bantime) {
            this.bantime = bantime;
        }

        public Integer getSpawnDistance() {
            return spawndistance;
        }
        public void setSpawnDistance(Integer spawndistance) {
            this.spawndistance = spawndistance;
        }

        public Integer getSpawnProtection() {
            return spawnprotection;
        }
        public void setSpawnProtection(Integer spawnprotection) {
            this.spawnprotection = spawnprotection;
        }

        public Location getExitPos() {
            return exitpos;
        }
        public void setExitPos(Location exitpos) {
            this.exitpos = exitpos;
        }

        public Integer getRollbackDelay() {
            return rollbackdelay;
        }
        public void setRollbackDelay(Integer rollbackdelay) {
            this.rollbackdelay = rollbackdelay;
        }

        public Boolean getDeathDrops() {
            return deathdrops;
        }
        public void setDeathDrops(Boolean deathdrops) {
            this.deathdrops = deathdrops;
        }

        public Boolean isWhitelisted() {
            return whitelisted;
        }
        public void setWhitelisted(Boolean whitelisted) {
            this.whitelisted = whitelisted;
        }

        public Difficulty getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
        }

        protected boolean checkandSetDifficulty(){
            if(world.getDifficulty() != difficulty){
                world.setDifficulty(difficulty);
            }
            return world.getDifficulty() == difficulty;
        }
    }

    public HardcoreWorld NewWorld(String world) {
        HardcoreWorld hcw = new HardcoreWorld();
        AddWorld(world, hcw);
        return hcw;
    }

    public boolean AddWorld(String world, HardcoreWorld hcw) {
        Worlds.put(world, hcw);
        return true;
    }

    public HardcoreWorld Get(String world) {
        String key = StringUtils.replace(world, "_nether", "");
        if (Worlds.containsKey(key)) {
            return Worlds.get(key);
        }
        return null;
    }

    public boolean Contains(String world) {
        String key = StringUtils.replace(world, "_nether", "");
        return Worlds.containsKey(key);
    }
    
    public Map<String, HardcoreWorld> AllRecords() {
        return Worlds;
    }
    
    public String GetNames() {
        List<String> names = new ArrayList<>(Worlds.keySet());
        return StringUtils.join(names, ",");
    }

}