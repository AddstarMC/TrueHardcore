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

import au.com.addstar.truehardcore.TrueHardcore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HardcorePlayers {

    private final Map<String, HardcorePlayer> players;

    public HardcorePlayers() {
        players = new HashMap<>();
    }

    /**
     * Create a HCP.
     * @param world world
     * @param id uuid
     * @param name name
     * @return the HardcorePlayer
     */
    public HardcorePlayer newPlayer(String world, UUID id, String name) {
        HardcorePlayer hcp = new HardcorePlayer();
        TrueHardcore.debug("Creating new player record: " + world + "/" + name);
        hcp.loadDataOnly = true;
        hcp.setPlayerName(name);
        hcp.setUniqueId(id);
        hcp.setWorld(world);
        hcp.loadDataOnly = false;
        addPlayer(world, id, hcp);
        return hcp;
    }

    /**
     * Get an hardcore player.
     * @param world world
     * @param id uuid
     * @return player
     */
    @Nullable
    public HardcorePlayer get(String world, @Nonnull UUID id) {
        String key = world.replaceAll("_nether|_the_end", "") + "/" + id;
        if (players.containsKey(key)) {
            return players.get(key);
        }
        return null;
    }

    /**
     * Get a Hardcore player.
     * @param world world
     * @param player player
     * @return HCP
     */
    @Nullable
    public HardcorePlayer get(@Nullable World world,@Nullable Player player) {
        if ((world == null) || (player == null)) {
            return null;
        }
        return get(world.getName(), player.getUniqueId());
    }

    /**
     * Get a player.
     * @param player player
     * @return hcp
     */
    public HardcorePlayer get(Player player) {
        if (player == null) {
            return null;
        }
        return get(player.getWorld().getName(), player.getUniqueId());
    }

    /**
     * Get a player.
     * @param key name
     * @return HCP
     */
    @Nullable
    public HardcorePlayer get(String key) {
        if (players.containsKey(key)) {
            return null;
        }
        return players.get(key.replaceAll("_nether|_the_end", ""));
    }

    /**
     * Add a player.
     * @param world world
     * @param id uuid
     * @param hcp hcp
     * @return boolean
     */
    private boolean addPlayer(String world, UUID id, HardcorePlayer hcp) {
        String key = world + "/" + id.toString();
        players.put(key, hcp);
        return true;
    }

    /**
     * Checks a player is in the plugin.
     * @param player the player to check
     * @return boolean
     */
    @SuppressWarnings("unused")
    public boolean isHardcorePlayer(Player player) {
        return (get(player) != null);
    }

    public void clear() {
        players.clear();
    }

    public Map<String, HardcorePlayer> allRecords() {
        return players;
    }

    public enum PlayerState {
        NOT_IN_GAME,
        IN_GAME,
        ALIVE,
        DEAD
    }

    public static class HardcorePlayer {
        private String playerName;
        private UUID playerId;
        private String world;
        private Location spawnPos;
        private Location lastPos;
        private Date lastJoin;
        private Date lastQuit;
        private Date gameStart;
        private Date gameEnd;
        private Integer gameTime = 0;
        private Integer level = 0;
        private float exp = 0;
        private Integer score = 0;
        private Integer topScore = 0;
        private PlayerState state = PlayerState.NOT_IN_GAME;
        private String deathMsg;
        private Location deathPos;
        private Integer deaths = 0;
        private Integer cowKills = 0;
        private Integer pigKills = 0;
        private Integer sheepKills = 0;
        private Integer chickenKills = 0;
        private Integer creeperKills = 0;
        private Integer zombieKills = 0;
        private Integer skeletonKills = 0;
        private Integer spiderKills = 0;
        private Integer enderKills = 0;
        private Integer slimeKills = 0;
        private Integer mooshKills = 0;
        private Integer otherKills = 0;
        private Integer playerKills = 0;
        private boolean modified = false;
        private boolean loadDataOnly = false;
        private boolean godMode = false;

        private boolean combat = false;
        private long combatExpiry = 0;

        /**
         * Check if its only load data.
         * @return bool
         */
        @SuppressWarnings("unused")
        public boolean isLoadDataOnly() {
            return loadDataOnly;
        }

        public void setLoadDataOnly(boolean loadDataOnly) {
            this.loadDataOnly = loadDataOnly;
        }

        public String getPlayerName() {
            return playerName;
        }

        /**
         * set the name.
         * @param playerName string
         */
        public void setPlayerName(String playerName) {
            this.playerName = playerName;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get the UUID.
         * @return uuid
         */
        public UUID getUniqueId() {
            return playerId;
        }

        /**
         * Set the UUID.
         * @param id the uuid
         */
        public void setUniqueId(UUID id) {
            playerId = id;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        public String getWorld() {
            return world;
        }

        /**
         * Set the World.
         * @param world world.
         */
        public void setWorld(String world) {
            this.world = world;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        public Location getSpawnPos() {
            return spawnPos;
        }

        /**
         * Set the spawn.
         * @param spawnPos location
         */
        public void setSpawnPos(Location spawnPos) {
            this.spawnPos = spawnPos;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        public Location getLastPos() {
            return lastPos;
        }

        /**
         * Set last location.
         * @param lastPos location
         */
        public void setLastPos(Location lastPos) {
            this.lastPos = lastPos;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * get last join date.
         * @return date
         */
        public Date getLastJoin() {
            return lastJoin;
        }

        /**
         * Set last join date.
         * @param lastJoin date
         */
        public void setLastJoin(Date lastJoin) {
            this.lastJoin = lastJoin;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * get last quit date.
         * @return date
         */
        public Date getLastQuit() {
            return lastQuit;
        }

        /**
         * Set the date.
         * @param lastQuit date.
         */
        public void setLastQuit(Date lastQuit) {
            this.lastQuit = lastQuit;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Set game start.
         * @return Date.
         */
        public Date getGameStart() {
            return gameStart;
        }

        /**
         * Set .
         * @param gameStart date.
         */
        public void setGameStart(Date gameStart) {
            this.gameStart = gameStart;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get date end.
         * @return date.
         */
        public Date getGameEnd() {
            return gameEnd;
        }

        /**
         * Set end date.
         * @param gameEnd date
         */
        public void setGameEnd(Date gameEnd) {
            this.gameEnd = gameEnd;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * get time in game.
         * @return integer
         */
        public Integer getGameTime() {
            return gameTime;
        }

        /**
         * Set game time.
         * @param gameTime time as a int
         */
        public void setGameTime(Integer gameTime) {
            this.gameTime = gameTime;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get level.
         * @return integer level
         */
        public Integer getLevel() {
            return level;
        }

        /**
         * Set the level.
         * @param level int
         */
        public void setLevel(Integer level) {
            this.level = level;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        public float getExp() {
            return exp;
        }

        /**
         * Set xp.
         * @param exp xp
         */
        public void setExp(float exp) {
            this.exp = exp;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get Score.
         * @return score
         */
        public Integer getScore() {
            return score;
        }

        /**
         * Set Score.
         * @param score the score
         */
        public void setScore(Integer score) {
            this.score = score;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * get Top Score.
         * @return score
         */
        public Integer getTopScore() {
            return topScore;
        }

        /**
         * Set top score.
         * @param topScore score
         */
        public void setTopScore(Integer topScore) {
            this.topScore = topScore;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get State.
         * @return PlayerState
         */
        public PlayerState getState() {
            return state;
        }

        /**
         * Set State.
         * @param state PlayerState
         */
        public void setState(PlayerState state) {
            if (!loadDataOnly) {
                if ((state == PlayerState.DEAD) && (this.state != PlayerState.DEAD)) {
                    // Player has died
                    setGameEnd(new Date());
                    setLastQuit(new Date());
                } else if ((state == PlayerState.IN_GAME) && (this.state != PlayerState.IN_GAME)) {
                    // Joining a game
                    if (this.state != PlayerState.ALIVE) {
                        // Starting a new game
                        setGameStart(new Date());
                    }
                    // Always set the join date when transitioning -> IN_GAME
                    setLastJoin(new Date());
                } else if ((this.state == PlayerState.IN_GAME) && (state != PlayerState.IN_GAME)) {
                    // Leaving a game (for any reason)
                    setLastQuit(new Date());
                }
            }
            this.state = state;
        }

        /**
         * Get Death msg.
         * @return string
         */
        public String getDeathMsg() {
            return deathMsg;
        }

        /**
         * Set Death msg.
         * @param deathMsg string
         */
        public void setDeathMsg(String deathMsg) {
            this.deathMsg = deathMsg;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get Death position.
         * @return location
         */
        public Location getDeathPos() {
            return deathPos;
        }

        /**
         * Set Death position.
         * @param deathPos location
         */
        public void setDeathPos(Location deathPos) {
            this.deathPos = deathPos;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get deaths.
         * @return int
         */
        public Integer getDeaths() {
            return deaths;
        }

        /**
         * Set Deaths.
         * @param deaths int
         */
        public void setDeaths(Integer deaths) {
            this.deaths = deaths;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get cow kills.
         * @return int.
         */
        public Integer getCowKills() {
            return cowKills;
        }

        /**
         * set cow kills.
         * @param cowKills int
         */
        public void setCowKills(Integer cowKills) {
            this.cowKills = cowKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get pig kills.
         * @return kills
         */
        public Integer getPigKills() {
            return pigKills;
        }

        /**
         * Set kils for pigs.
         * @param pigKills pig kills
         */
        public void setPigKills(Integer pigKills) {
            this.pigKills = pigKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get sheep kills.
         * @return int
         */
        public Integer getSheepKills() {
            return sheepKills;
        }

        /**
         * Set Sheep kills.
         * @param sheepKills kills
         */
        public void setSheepKills(Integer sheepKills) {
            this.sheepKills = sheepKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * get Chicken kills.
         * @return int
         */
        public Integer getChickenKills() {
            return chickenKills;
        }

        /**
         * Set chicken kills.
         * @param chickenKills kills.
         */
        public void setChickenKills(Integer chickenKills) {
            this.chickenKills = chickenKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Set Creeper kills.
         * @return int
         */
        public Integer getCreeperKills() {
            return creeperKills;
        }

        /**
         * Set creeper kills.
         * @param creeperKills int
         */
        public void setCreeperKills(Integer creeperKills) {
            this.creeperKills = creeperKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * get zombie kills.
         * @return int
         */
        public Integer getZombieKills() {
            return zombieKills;
        }

        /**
         * set zombie kills.
         * @param zombieKills int
         */
        public void setZombieKills(Integer zombieKills) {
            this.zombieKills = zombieKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Get Skeleton kills.
         * @return int
         */
        public Integer getSkeletonKills() {
            return skeletonKills;
        }

        /**
         * set skeleton kills.
         * @param skeletonKills int
         */
        public void setSkeletonKills(Integer skeletonKills) {
            this.skeletonKills = skeletonKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * get spider kills.
         * @return int
         */
        public Integer getSpiderKills() {
            return spiderKills;
        }

        /**
         * set spider kills.
         * @param spiderKills int
         */
        public void setSpiderKills(Integer spiderKills) {
            this.spiderKills = spiderKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         *  Get enderman kills.
         * @return int
         */
        public Integer getEnderKills() {
            return enderKills;
        }

        /**
         * Set enderman kills.
         * @param enderKills int
         */
        public void setEnderKills(Integer enderKills) {
            this.enderKills = enderKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Slimes.
         * @return int
         */
        public Integer getSlimeKills() {
            return slimeKills;
        }

        /**
         * Slimes.
         * @param slimeKills int
         */
        public void setSlimeKills(Integer slimeKills) {
            this.slimeKills = slimeKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * get Mooshroom kills.
         * @return int
         */
        public Integer getMooshKills() {
            return mooshKills;
        }

        /**
         * Set Moosh kills.
         * @param mooshKills int
         */
        public void setMooshKills(Integer mooshKills) {
            this.mooshKills = mooshKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Set player kills.
         * @return int
         */
        public Integer getPlayerKills() {
            return playerKills;
        }

        /**
         * Get Player kills.
         * @param playerKills int
         */
        public void setPlayerKills(Integer playerKills) {
            this.playerKills = playerKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Other kills.
         * @return int
         */
        public Integer getOtherKills() {
            return otherKills;
        }

        /**
         * Set other.
         * @param otherKills int
         */
        public void setOtherKills(Integer otherKills) {
            this.otherKills = otherKills;
            if (loadDataOnly) {
                return;
            }
            setModified(true);
        }

        /**
         * Update a player.
         * @param player  player
         */
        public void updatePlayer(Player player) {
            setModified(true);
            setExp(player.getExp());
            setLastPos(player.getLocation());
            setScore(player.getTotalExperience());
            setLevel(player.getLevel());
        }

        /**
         * diff between 2 times.
         *
         * @param d1 date
         * @param d2 date
         * @return int
         */
        public Integer timeDiff(Date d1, Date d2) {

            if (d2.after(d1)) {
                return (int) ((d2.getTime() - d1.getTime()) / 1000);
            }
            return null;
        }

        /**
         * Calculate game time.
         */
        public void calcGameTime() {
            calcGameTime(getLastQuit());
        }

        /**
         * Calc game time.
         * @param when from
         */
        public void calcGameTime(Date when) {
            Integer diff = timeDiff(getLastJoin(), when);
            if (diff != null) {
                setGameTime(getGameTime() + diff);
            }
        }

        /**
         * True if changed.
         * @return bool
         */
        public boolean isModified() {
            return modified;
        }

        /**
         * Set changed.
         * @param modified bool
         */
        public void setModified(boolean modified) {
            this.modified = modified;
        }

        /**
         * True if in god mode.
         * @return bool
         */
        public boolean isGodMode() {
            return godMode;
        }

        /**
         * Set god.
         * @param godMode bool
         */
        public void setGodMode(boolean godMode) {
            this.godMode = godMode;
        }

        public boolean isInCombat() {
            return combat;
        }

        public void setInCombat(boolean combat) {
            this.combat = combat;
        }

        public long getCombatExpiry() {
            return combatExpiry;
        }

        public void setCombatExpiry(long combatExpiry) {
            this.combatExpiry = combatExpiry;
        }
    }
}
