package dev.ianaduarte.barometry.mixin;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import dev.ianaduarte.barometry.render.BarometryCloudRenderer;
import dev.ianaduarte.barometry.render.WeatherLevelRenderer;
import dev.ianaduarte.barometry.util.MathUtil;
import dev.ianaduarte.barometry.weather.WeatherSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin implements WeatherLevelRenderer {
	@Shadow private @Nullable ClientLevel level;
	@Shadow @Final private LevelTargetBundle targets;
	@Shadow @Final private Minecraft minecraft;
	@Unique private BarometryCloudRenderer barometryCloudsRenderer;
	
	@Inject(method = "<init>", at = @At("TAIL"))
	private void initializeRenderer(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, LevelRenderState levelRenderState, FeatureRenderDispatcher featureRenderDispatcher, CallbackInfo ci) {
		this.barometryCloudsRenderer = new BarometryCloudRenderer(minecraft);
	}
	
	
	@Inject(method = "tick", at = @At("TAIL"))
	private void tickClouds(Camera camera, CallbackInfo ci) {
		this.barometryCloudsRenderer.tick(this.level);
	}
	@Inject(method = "addCloudsPass", at = @At("HEAD"), cancellable = true)
	private void renderClouds(FrameGraphBuilder frameGraphBuilder, CloudStatus cloudStatus, Vec3 cameraPosition, float ticks, int cloudColor, float cloudHeight, CallbackInfo ci) {
		FramePass cloudPass = frameGraphBuilder.addPass("clouds");
		if(this.targets.clouds != null) {
			this.targets.clouds = cloudPass.readsAndWrites(this.targets.clouds);
		} else {
			this.targets.main = cloudPass.readsAndWrites(this.targets.main);
		}
		
		cloudPass.executes(() -> this.barometryCloudsRenderer.render(cloudColor, cloudHeight, cameraPosition, MathUtil.fract(ticks)));
		ci.cancel();
	}
	@Redirect(method = "needsUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CloudRenderer;markForRebuild()V"))
	private void rebuildClouds(CloudRenderer instance) {
		this.barometryCloudsRenderer.markForRebuild();
		//BarometryClient.CLOUDS_RENDERER.markForRebuild();
	}
	@Redirect(method = "allChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CloudRenderer;markForRebuild()V"))
	private void rebuildClouds2(CloudRenderer instance) {
		this.barometryCloudsRenderer.markForRebuild();
	}
	
	@Override
	public BarometryCloudRenderer cloudRenderer() {
		return this.barometryCloudsRenderer;
	}
}
