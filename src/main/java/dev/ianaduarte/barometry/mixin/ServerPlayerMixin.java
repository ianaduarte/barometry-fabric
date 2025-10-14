package dev.ianaduarte.barometry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ianaduarte.barometry.util.ExtServerPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements ExtServerPlayer {
	@Unique private int cloudDistance;
	
	@Override
	public void cloudDistance(int cloudRenderDistance) {
		this.cloudDistance = cloudRenderDistance;
	}
	@Override
	public int cloudDistance() {
		return this.cloudDistance;
	}
}