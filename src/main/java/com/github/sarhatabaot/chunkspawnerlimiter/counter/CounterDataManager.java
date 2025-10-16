package com.github.sarhatabaot.chunkspawnerlimiter.counter;

import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CounterDataManager {
    private final Map<ChunkCoord, CounterData> loadedChunkCounters = new ConcurrentHashMap<>();

    public CounterData getCounterData(final ChunkCoord chunkCoord) {
        if (!loadedChunkCounters.containsKey(chunkCoord))  {
            loadedChunkCounters.put(chunkCoord, new CounterData());
        }

        return loadedChunkCounters.get(chunkCoord);
    }

    public void removeCounterData(final ChunkCoord chunkCoord) {
        loadedChunkCounters.remove(chunkCoord);
    }


}
