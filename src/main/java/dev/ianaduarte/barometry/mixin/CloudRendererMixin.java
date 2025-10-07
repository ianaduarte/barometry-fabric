package dev.ianaduarte.barometry.mixin;

import com.mojang.blaze3d.ProjectionType;
//import com.mojang.blaze3d.buffers.BufferType;
//import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import dev.ianaduarte.barometry.Barometry;
import dev.ianaduarte.barometry.ExtCloudRenderer;
import dev.ianaduarte.barometry.ProjectionGetter;
import dev.ianaduarte.barometry.render.BarometryCloudsRenderer;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.*;

import java.util.OptionalDouble;
import java.util.OptionalInt;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin implements ExtCloudRenderer {
	@Shadow private boolean needsRebuild;
	@Unique BarometryCloudsRenderer cloudsRenderer = new BarometryCloudsRenderer();
	@Unique float forecastPrev;
	@Unique float forecast;
	@Unique double cloudOffsetPrev;
	@Unique double cloudOffset;
	
	/**
	 * @author ianaduarte
	 * @reason i can't just patch this function, it's better to overwrite it. sorry
	 */
	@Overwrite
	public void render(int cloudColor, CloudStatus cloudStatus, float height, Vec3 cameraPos, float ticks) {
		float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		cloudsRenderer.render(this.needsRebuild, cloudColor, height, Mth.lerp(partialTicks, cloudOffsetPrev, cloudOffset), Mth.lerp(partialTicks, forecastPrev, forecast), cameraPos, partialTicks);
		this.needsRebuild = false;
	}
	
	@Override
	public void tick(float forecast, double cloudOffset) {
		this.forecastPrev = this.forecast;
		this.cloudOffsetPrev = this.cloudOffset;
		this.forecast = forecast;
		this.cloudOffset += cloudOffset;
	}
}
