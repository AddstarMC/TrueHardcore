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
import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 25/08/2020.
 */
public final class ChunkStorage {

    private final Map<UUID, List<TrueHardCoreChunk>> heldChunks = new HashMap<>();

    /**
     * Stores References to chunks that might be forceloaded.
     */
    public ChunkStorage() {
    }

    public void enable() {
        for (Map.Entry<String, HardcoreWorlds.HardcoreWorld> entry : TrueHardcore.instance.hardcoreWorlds.allRecords().entrySet()) {
            heldChunks.put(entry.getValue().getWorld().getUID(), new ArrayList<>());
        }
    }

    public void disable() {
        heldChunks.forEach((uuid, trueHardCoreChunks) -> {
            Iterator<TrueHardCoreChunk> i = trueHardCoreChunks.iterator();
            while (i.hasNext()) {
                TrueHardCoreChunk coreChunk = i.next();
                World world = Bukkit.getWorld(uuid);
                if (world != null && world.getUID() == coreChunk.world) {
                    Chunk chunk = world.getChunkAt(coreChunk.coOrdX, coreChunk.coOrdZ);
                    chunk.setForceLoaded(false);
                    i.remove();
                } else {
                    TrueHardcore.debug("Null Chunk on shutdown.");
                }
            }
        });
    }

    /**
     * Gets an unchangeable snapshot for reporting.
     *
     * @return Map
     */
    public Map<UUID, List<TrueHardCoreChunk>> getSnapShot() {
        return ImmutableMap.copyOf(heldChunks);
    }

    public void expireOldChunks() {
        for (List<TrueHardCoreChunk> item : heldChunks.values()) {
            for (int i = 0; i < item.size(); i++) {
                TrueHardCoreChunk c = item.get(i);
                if (c.getExpiry() < System.currentTimeMillis()) {
                    TrueHardcore.debug("Chunk expired: " + c.coOrdX + " " + c.coOrdZ + " " + c.world);
                    clearRealChunk(c);
                    item.remove(c);
                }
            }
        }
    }

    private void clearRealChunk(final TrueHardCoreChunk coreChunk) {
        Bukkit.getScheduler().runTask(TrueHardcore.instance, () -> {
            World world = Bukkit.getServer().getWorld(coreChunk.world);
            if (world != null) {
                Chunk chunk = world.getChunkAt(coreChunk.coOrdX, coreChunk.coOrdZ);
                if (chunk.isLoaded() && chunk.isForceLoaded()) {
                    TrueHardcore.debug("Chunk setForceLoaded(false): " + chunk.getX() + " " + chunk.getZ() + " " + chunk.getWorld().getName());
                    chunk.setForceLoaded(false);
                }
            }
        });
    }

    /**
     * Adds a Chunk with an expiry.
     *
     * @param chunk  Chunk
     * @param expiry Long
     */
    public void addChunk(Chunk chunk, long expiry) {
        UUID uuid = chunk.getWorld().getUID();
        TrueHardCoreChunk thChunk = new TrueHardCoreChunk(chunk.getX(), chunk.getZ(), uuid);
        thChunk.setExpiry(expiry);
        List<TrueHardCoreChunk> chunks = heldChunks.computeIfAbsent(uuid, k -> new ArrayList<>());
        chunks.add(thChunk);
    }

    /**
     * Removes a chunk and sets it to NOT be force loaded.
     *
     * @param chunk Chunk
     * @return if removed successfully.
     */
    public boolean removeChunk(Chunk chunk) {
        TrueHardCoreChunk c = new TrueHardCoreChunk(chunk);
        List<TrueHardCoreChunk> chunks = heldChunks.get(c.world);
        chunk.setForceLoaded(false);
        return chunks.remove(c);
    }

    /**
     * Checks if a chunk should still be force loaded.
     *
     * @param chunk Chunk
     * @return boolean
     */
    public boolean checkChunkState(Chunk chunk) {
        UUID uuid = chunk.getWorld().getUID();
        List<TrueHardCoreChunk> chunksHeld = heldChunks.get(uuid);
        if (chunksHeld.size() == 0) {
            return false;
        }
        TrueHardCoreChunk test = new TrueHardCoreChunk(chunk);
        for (TrueHardCoreChunk chunk1 : chunksHeld) {
            if (chunk1.equals(test)) {
                if (System.currentTimeMillis() < chunk1.getExpiry()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A reference that wont hold chunks in memory.
     */
    public static class TrueHardCoreChunk {

        final int coOrdX;

        final int coOrdZ;

        final UUID world;
        transient Long expiry = null;

        public TrueHardCoreChunk(Chunk chunk) {
            this(chunk.getX(), chunk.getZ(), chunk.getWorld().getUID());
        }

        TrueHardCoreChunk(int coOrdX, int coOrdZ, UUID uuid) {
            this.coOrdX = coOrdX;
            this.coOrdZ = coOrdZ;
            this.world = uuid;
        }

        public long getExpiry() {
            return expiry;
        }

        public void setExpiry(long expiry) {
            this.expiry = expiry;
        }

        public int getX() {
            return coOrdX;
        }

        public int getZ() {
            return coOrdZ;
        }

        public UUID getWorldUuid() {
            return world;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TrueHardCoreChunk)) {
                return false;
            }

            TrueHardCoreChunk that = (TrueHardCoreChunk) o;

            if (coOrdX != that.coOrdX) {
                return false;
            }
            if (coOrdZ != that.coOrdZ) {
                return false;
            }
            return world.equals(that.world);
        }

        @Override
        public int hashCode() {
            int result = coOrdX;
            result = 31 * result + coOrdZ;
            result = 31 * result + world.hashCode();
            return result;
        }
    }
}
