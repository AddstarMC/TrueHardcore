package au.com.addstar.truehardcore;

import io.papermc.lib.PaperLib;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Created for the Addstar MC Project.
 * Created by Narimm on 4/02/2020.
 */
public class ChunkListener implements Listener {
    private long baseTime;

    public ChunkListener(long baseTime) {
        this.baseTime = baseTime;
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onChunkLoad(ChunkLoadEvent event){
        Chunk chunk = event.getChunk();
        Long time = chunk.getInhabitedTime();
        if(time<baseTime){
            chunk.setInhabitedTime(baseTime);
        }
    }
}
