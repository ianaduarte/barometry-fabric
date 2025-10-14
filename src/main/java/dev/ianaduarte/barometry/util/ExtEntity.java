package dev.ianaduarte.barometry.util;

import net.minecraft.world.level.ChunkPos;

public interface ExtEntity {
	ChunkPos chunkPositionPrev();
	boolean changedChunkPositions();
}
