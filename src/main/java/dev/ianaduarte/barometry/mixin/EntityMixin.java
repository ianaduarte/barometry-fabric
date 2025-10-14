package dev.ianaduarte.barometry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ianaduarte.barometry.util.ExtEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin implements ExtEntity {
	@Shadow private ChunkPos chunkPosition;
	@Unique ChunkPos chunkPositionPrev;
	
	@WrapOperation(method = "setPosRaw", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/Entity;chunkPosition:Lnet/minecraft/world/level/ChunkPos;"))
	private void storePrevChunkPos(Entity instance, ChunkPos value, Operation<Void> original) {
		this.chunkPositionPrev = this.chunkPosition;
		original.call(instance, value);
	}
	
	@Override
	public ChunkPos chunkPositionPrev() {
		return this.chunkPositionPrev;
	}
	@Override
	public boolean changedChunkPositions() {
		return this.chunkPositionPrev == null || !(this.chunkPositionPrev.equals(this.chunkPosition));
	}
}
