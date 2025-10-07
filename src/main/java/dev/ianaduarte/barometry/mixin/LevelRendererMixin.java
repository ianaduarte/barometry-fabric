package dev.ianaduarte.barometry.mixin;

import dev.ianaduarte.barometry.ExtCloudRenderer;
import dev.ianaduarte.barometry.MathUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("DataFlowIssue")
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Shadow private @Nullable ClientLevel level;
	@Shadow @Final private CloudRenderer cloudRenderer;
 
	@Inject(method = "tick", at = @At("HEAD"))
	private void updateClouds(CallbackInfo ci) {
		ExtCloudRenderer extCloudRenderer = (ExtCloudRenderer)this.cloudRenderer;
		
		if(this.level.tickRateManager().isFrozen()) {
			extCloudRenderer.tick(this.level.getRainLevel(1) + this.level.getThunderLevel(1), 0);
			return;
		}
		float forecast = this.level.getRainLevel(1) + this.level.getThunderLevel(1);
		float speed    = (MathUtil.gradient(forecast / 2, 0.5f, 2.0f, 4.0f) + 0.5f) * (this.level.tickRateManager().tickrate() * 0.05f);
		
		extCloudRenderer.tick(forecast, speed);
	}
}
