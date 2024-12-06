package dev.ianaduarte.barometry.mixin;

import dev.ianaduarte.barometry.Barometry;
import dev.ianaduarte.barometry.ExtCloudRenderer;
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
		float forecast = this.level.getRainLevel(1) + this.level.getThunderLevel(1);
		float speed    = Barometry.gradient(forecast / 2, 0.5f, 1.5f, 2.5f);
		
		((ExtCloudRenderer)this.cloudRenderer).tick(forecast, speed);
	}
}
