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

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Consumer;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 10/03/2020.
 */
public class MockWorld implements World {

    private String name;

    public MockWorld(String name) {
        this.name = name;
    }

    @Override
    public @NotNull Block getBlockAt(int i, int i1, int i2) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Block getBlockAt(@NotNull Location location) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public int getHighestBlockYAt(int i, int i1) {
        return 0;
    }

    @Override
    public int getHighestBlockYAt(@NotNull Location location) {
        return 0;
    }

    @Override
    public @NotNull Block getHighestBlockAt(int i, int i1) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Block getHighestBlockAt(@NotNull Location location) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public int getHighestBlockYAt(int i, int i1, @NotNull HeightMap heightMap) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public int getHighestBlockYAt(@NotNull Location location, @NotNull HeightMap heightMap) {
        return 0;
    }

    @Override
    public @NotNull Block getHighestBlockAt(int i, int i1, @NotNull HeightMap heightMap) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Block getHighestBlockAt(@NotNull Location location, @NotNull HeightMap heightMap) {
        return null;
    }

    @Override
    public @NotNull Chunk getChunkAt(int i, int i1) {
        return null;
    }

    @Override
    public @NotNull Chunk getChunkAt(@NotNull Location location) {
        return null;
    }

    @Override
    public @NotNull Chunk getChunkAt(@NotNull Block block) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public boolean isChunkLoaded(@NotNull Chunk chunk) {
        return false;
    }

    @NotNull
    @Override
    public Chunk[] getLoadedChunks() {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public void loadChunk(@NotNull Chunk chunk) {

    }

    @Override
    public boolean isChunkLoaded(int i, int i1) {
        return false;
    }

    @Override
    public boolean isChunkGenerated(int i, int i1) {
        return false;
    }

    @Override
    public boolean isChunkInUse(int i, int i1) {
        return false;
    }

    @Override
    public void loadChunk(int i, int i1) {

    }

    @Override
    public boolean loadChunk(int i, int i1, boolean b) {
        return false;
    }

    @Override
    public boolean unloadChunk(@NotNull Chunk chunk) {
        return false;
    }

    @Override
    public boolean unloadChunk(int i, int i1) {
        return false;
    }

    @Override
    public boolean unloadChunk(int i, int i1, boolean b) {
        return false;
    }

    @Override
    public boolean unloadChunkRequest(int i, int i1) {
        return false;
    }

    @Override
    public boolean regenerateChunk(int i, int i1) {
        return false;
    }

    @Override
    public boolean refreshChunk(int i, int i1) {
        return false;
    }

    @Override
    public boolean isChunkForceLoaded(int i, int i1) {
        return false;
    }

    @Override
    public void setChunkForceLoaded(int i, int i1, boolean b) {

    }

    @Override
    public @NotNull Collection<Chunk> getForceLoadedChunks() {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public boolean addPluginChunkTicket(int i, int i1, @NotNull Plugin plugin) {
        return false;
    }

    @Override
    public boolean removePluginChunkTicket(int i, int i1, @NotNull Plugin plugin) {
        return false;
    }

    @Override
    public void removePluginChunkTickets(@NotNull Plugin plugin) {

    }

    @Override
    public @NotNull Collection<Plugin> getPluginChunkTickets(int i, int i1) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Map<Plugin, Collection<Chunk>> getPluginChunkTickets() {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Item dropItem(@NotNull Location location, @NotNull ItemStack itemStack) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Item dropItemNaturally(@NotNull Location location, @NotNull ItemStack itemStack) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Arrow spawnArrow(@NotNull Location location, @NotNull Vector vector, float v, float v1) {
        throw new UnsupportedOperationException("Method not valid");

    }

    @Override
    public <T extends AbstractArrow> @NotNull T spawnArrow(@NotNull Location location, @NotNull Vector vector, float v, float v1, @NotNull Class<T> aClass) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public boolean generateTree(@NotNull Location location, @NotNull TreeType treeType) {
        return false;
    }

    @Override
    public boolean generateTree(@NotNull Location location, @NotNull TreeType treeType, @NotNull BlockChangeDelegate blockChangeDelegate) {
        return false;
    }

    @Override
    public @NotNull Entity spawnEntity(@NotNull Location location, @NotNull EntityType entityType) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull LightningStrike strikeLightning(@NotNull Location location) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull LightningStrike strikeLightningEffect(@NotNull Location location) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull List<Entity> getEntities() {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull List<LivingEntity> getLivingEntities() {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull <T extends Entity> Collection<T> getEntitiesByClass( @NotNull Class<T>... classes) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @NotNull
    @Override
    public <T extends Entity> Collection<T> getEntitiesByClass(@NotNull Class<T> aClass) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Collection<Entity> getEntitiesByClasses(@NotNull Class<?>... classes) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull List<Player> getPlayers() {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Collection<Entity> getNearbyEntities(@NotNull Location location, double v, double v1, double v2) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Collection<Entity> getNearbyEntities(@NotNull Location location, double v, double v1, double v2, @Nullable Predicate<Entity> predicate) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Collection<Entity> getNearbyEntities(@NotNull BoundingBox boundingBox) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull Collection<Entity> getNearbyEntities(@NotNull BoundingBox boundingBox, @Nullable Predicate<Entity> predicate) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @Nullable RayTraceResult rayTraceEntities(@NotNull Location location, @NotNull Vector vector, double v) {
        return null;
    }

    @Override
    public @Nullable RayTraceResult rayTraceEntities(@NotNull Location location, @NotNull Vector vector, double v, double v1) {
        return null;
    }

    @Override
    public @Nullable RayTraceResult rayTraceEntities(@NotNull Location location, @NotNull Vector vector, double v, @Nullable Predicate<Entity> predicate) {
        return null;
    }

    @Override
    public @Nullable RayTraceResult rayTraceEntities(@NotNull Location location, @NotNull Vector vector, double v, double v1, @Nullable Predicate<Entity> predicate) {
        return null;
    }

    @Override
    public @Nullable RayTraceResult rayTraceBlocks(@NotNull Location location, @NotNull Vector vector, double v) {
        return null;
    }

    @Override
    public @Nullable RayTraceResult rayTraceBlocks(@NotNull Location location, @NotNull Vector vector, double v, @NotNull FluidCollisionMode fluidCollisionMode) {
        return null;
    }

    @Override
    public @Nullable RayTraceResult rayTraceBlocks(@NotNull Location location, @NotNull Vector vector, double v, @NotNull FluidCollisionMode fluidCollisionMode, boolean b) {
        return null;
    }

    @Override
    public @Nullable RayTraceResult rayTrace(@NotNull Location location, @NotNull Vector vector, double v, @NotNull FluidCollisionMode fluidCollisionMode, boolean b, double v1, @Nullable Predicate<Entity> predicate) {
        return null;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull UUID getUID() {
        return UUID.randomUUID();
    }

    @Override
    public @NotNull Location getSpawnLocation() {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull boolean setSpawnLocation(@NotNull Location location) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public boolean setSpawnLocation(int i, int i1, int i2) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public long getTime() {
        return 0;
    }

    @Override
    public void setTime(long l) {

    }

    @Override
    public long getFullTime() {
        return 0;
    }

    @Override
    public void setFullTime(long l) {
        throw new UnsupportedOperationException("Method not valid");

    }

    @Override
    public boolean hasStorm() {
        return false;
    }

    @Override
    public void setStorm(boolean b) {
        throw new UnsupportedOperationException("Method not valid");

    }

    @Override
    public int getWeatherDuration() {
        return 0;
    }

    @Override
    public void setWeatherDuration(int i) {

    }

    @Override
    public boolean isThundering() {
        return false;
    }

    @Override
    public void setThundering(boolean b) {
        throw new UnsupportedOperationException("Method not valid");

    }

    @Override
    public int getThunderDuration() {
        return 0;
    }

    @Override
    public void setThunderDuration(int i) {

    }

    @Override
    public boolean createExplosion(double v, double v1, double v2, float v3) {
        return false;
    }

    @Override
    public boolean createExplosion(double v, double v1, double v2, float v3, boolean b) {
        return false;
    }

    @Override
    public boolean createExplosion(double v, double v1, double v2, float v3, boolean b, boolean b1) {
        return false;
    }

    @Override
    public boolean createExplosion(double v, double v1, double v2, float v3, boolean b, boolean b1, @Nullable Entity entity) {
        return false;
    }

    @Override
    public boolean createExplosion(@NotNull Location location, float v) {
        return false;
    }

    @Override
    public boolean createExplosion(@NotNull Location location, float v, boolean b) {
        return false;
    }

    @Override
    public boolean createExplosion(@NotNull Location location, float v, boolean b, boolean b1) {
        return false;
    }

    @Override
    public boolean createExplosion(@NotNull Location location, float v, boolean b, boolean b1, @Nullable Entity entity) {
        return false;
    }

    @Override
    public @NotNull Environment getEnvironment() {
        return Environment.NORMAL;
    }

    @Override
    public long getSeed() {
        return 0;
    }

    @Override
    public boolean getPVP() {
        return false;
    }

    @Override
    public void setPVP(boolean b) {
        throw new UnsupportedOperationException("Method not valid");

    }

    @Override
    public @Nullable ChunkGenerator getGenerator() {
        return null;
    }

    @Override
    public void save() {

    }

    @Override
    public @NotNull List<BlockPopulator> getPopulators() {
        return null;
    }

    @Override
    public <T extends Entity> @NotNull T spawn(@NotNull Location location, @NotNull Class<T> aClass) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public <T extends Entity> @NotNull T spawn(@NotNull Location location, @NotNull Class<T> aClass, @Nullable Consumer<T> consumer) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull FallingBlock spawnFallingBlock(@NotNull Location location, @NotNull MaterialData materialData) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull FallingBlock spawnFallingBlock(@NotNull Location location, @NotNull BlockData blockData) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @NotNull FallingBlock spawnFallingBlock(@NotNull Location location, @NotNull Material material, byte b) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public void playEffect(@NotNull Location location, @NotNull Effect effect, int i) {

    }

    @Override
    public void playEffect(@NotNull Location location, @NotNull Effect effect, int i, int i1) {

    }

    @Override
    public <T> void playEffect(@NotNull Location location, @NotNull Effect effect, @Nullable T t) {

    }

    @Override
    public <T> void playEffect(@NotNull Location location, @NotNull Effect effect, @Nullable T t, int i) {

    }

    @Override
    public @NotNull ChunkSnapshot getEmptyChunkSnapshot(int i, int i1, boolean b, boolean b1) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public void setSpawnFlags(boolean b, boolean b1) {

    }

    @Override
    public boolean getAllowAnimals() {
        return false;
    }

    @Override
    public boolean getAllowMonsters() {
        return false;
    }

    @Override
    public @NotNull Biome getBiome(int i, int i1) {
        return Biome.PLAINS;
    }

    @Override
    public @NotNull Biome getBiome(int i, int i1, int i2) {
        return Biome.PLAINS;
    }

    @Override
    public void setBiome(int i, int i1, @NotNull Biome biome) {

    }

    @Override
    public void setBiome(int i, int i1, int i2, @NotNull Biome biome) {

    }

    @Override
    public double getTemperature(int i, int i1) {
        return 0;
    }

    @Override
    public double getTemperature(int i, int i1, int i2) {
        return 0;
    }

    @Override
    public double getHumidity(int i, int i1) {
        return 0;
    }

    @Override
    public double getHumidity(int i, int i1, int i2) {
        return 0;
    }

    @Override
    public int getMaxHeight() {
        return 0;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public boolean getKeepSpawnInMemory() {
        return false;
    }

    @Override
    public void setKeepSpawnInMemory(boolean b) {

    }

    @Override
    public boolean isAutoSave() {
        return false;
    }

    @Override
    public void setAutoSave(boolean b) {

    }

    @Override
    public void setDifficulty(@NotNull Difficulty difficulty) {

    }

    @Override
    public @NotNull Difficulty getDifficulty() {
        return null;
    }

    @Override
    public @NotNull File getWorldFolder() {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public @Nullable WorldType getWorldType() {
        return null;
    }

    @Override
    public boolean canGenerateStructures() {
        return false;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public void setHardcore(boolean b) {

    }

    @Override
    public long getTicksPerAnimalSpawns() {
        return 0;
    }

    @Override
    public void setTicksPerAnimalSpawns(int i) {

    }

    @Override
    public long getTicksPerMonsterSpawns() {
        return 0;
    }

    @Override
    public void setTicksPerMonsterSpawns(int i) {

    }

    @Override
    public long getTicksPerWaterSpawns() {
        return 0;
    }

    @Override
    public void setTicksPerWaterSpawns(int i) {

    }

    @Override
    public long getTicksPerAmbientSpawns() {
        return 0;
    }

    @Override
    public void setTicksPerAmbientSpawns(int i) {

    }

    @Override
    public int getMonsterSpawnLimit() {
        return 0;
    }

    @Override
    public void setMonsterSpawnLimit(int i) {

    }

    @Override
    public int getAnimalSpawnLimit() {
        return 0;
    }

    @Override
    public void setAnimalSpawnLimit(int i) {

    }

    @Override
    public int getWaterAnimalSpawnLimit() {
        return 0;
    }

    @Override
    public void setWaterAnimalSpawnLimit(int i) {

    }

    @Override
    public int getAmbientSpawnLimit() {
        return 0;
    }

    @Override
    public void setAmbientSpawnLimit(int i) {

    }

    @Override
    public void playSound(@NotNull Location location, @NotNull Sound sound, float v, float v1) {

    }

    @Override
    public void playSound(@NotNull Location location, @NotNull String s, float v, float v1) {

    }

    @Override
    public void playSound(@NotNull Location location, @NotNull Sound sound, @NotNull SoundCategory soundCategory, float v, float v1) {

    }

    @Override
    public void playSound(@NotNull Location location, @NotNull String s, @NotNull SoundCategory soundCategory, float v, float v1) {

    }

    @NotNull
    @Override
    public String[] getGameRules() {
        return new String[0];
    }

    @Override
    public @Nullable String getGameRuleValue(@Nullable String s) {
        return null;
    }

    @Override
    public boolean setGameRuleValue(@NotNull String s, @NotNull String s1) {
        return false;
    }

    @Override
    public boolean isGameRule(@NotNull String s) {
        return false;
    }

    @Override
    public <T> @Nullable T getGameRuleValue(@NotNull GameRule<T> gameRule) {
        return null;
    }

    @Override
    public <T> @Nullable T getGameRuleDefault(@NotNull GameRule<T> gameRule) {
        return null;
    }

    @Override
    public <T> boolean setGameRule(@NotNull GameRule<T> gameRule, @NotNull T t) {
        return false;
    }

    @Override
    public @NotNull WorldBorder getWorldBorder() {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i) {

    }

    @Override
    public void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i) {

    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i, @Nullable T t) {

    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i, @Nullable T t) {

    }

    @Override
    public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i, double v, double v1, double v2) {

    }

    @Override
    public void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5) {

    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i, double v, double v1, double v2, @Nullable T t) {

    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5, @Nullable T t) {

    }

    @Override
    public void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i, double v, double v1, double v2, double v3) {

    }

    @Override
    public void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5, double v6) {

    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i, double v, double v1, double v2, double v3, @Nullable T t) {

    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5, double v6, @Nullable T t) {

    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int i, double v, double v1, double v2, double v3, @Nullable T t, boolean b) {

    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5, double v6, @Nullable T t, boolean b) {

    }

    @Override
    public @Nullable Location locateNearestStructure(@NotNull Location location, @NotNull StructureType structureType, int i, boolean b) {
        return null;
    }

    @Override
    public int getViewDistance() {
        return 0;
    }

    @Override
    public @NotNull Spigot spigot() {
        return null;
    }

    @Override
    public @Nullable Raid locateNearestRaid(@NotNull Location location, int i) {
        return null;
    }

    @Override
    public @NotNull List<Raid> getRaids() {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public void setMetadata(@NotNull String s, @NotNull MetadataValue metadataValue) {

    }

    @Override
    public @NotNull List<MetadataValue> getMetadata(@NotNull String s) {
        throw new UnsupportedOperationException("Method not valid");
    }

    @Override
    public boolean hasMetadata(@NotNull String s) {
        return false;
    }

    @Override
    public void removeMetadata(@NotNull String s, @NotNull Plugin plugin) {

    }

    @Override
    public void sendPluginMessage(@NotNull Plugin plugin, @NotNull String s, @NotNull byte[] bytes) {

    }

    @Override
    public @NotNull Set<String> getListeningPluginChannels() {
        throw new UnsupportedOperationException("Method not valid");
    }
}
