package dev.ianaduarte.barometry.mixin;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.*;
import dev.ianaduarte.barometry.Barometry;
import dev.ianaduarte.barometry.ExtCloudRenderer;
import dev.ianaduarte.barometry.ProjectionGetter;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
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
	@Shadow private GpuBuffer vertexBuffer;
	@Shadow private boolean needsRebuild;
	@Shadow private CloudRenderer.RelativeCameraPos prevRelativeCameraPos;
	@Shadow private @Nullable CloudStatus prevType;
	@Shadow @Nullable private CloudRenderer.@Nullable TextureData texture;
	
	@Shadow private int indexCount;
	@Shadow @Final private RenderSystem.AutoStorageIndexBuffer indices;
	@Unique float forecastPrev = 0;
	@Unique float forecast = 0;
	@Unique double cloudOffsetPrev = 0;
	@Unique double cloudOffset = 0;
	
	/**
	 * @author ianaduarte
	 * @reason i can't just patch this function, it's better to overwrite it. sorry
	 */
	@Overwrite
	public void render(int cloudColor, CloudStatus cloudStatus, float height, Vec3 cameraPos, float ticks) {
		if(this.texture == null) return;
		
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		float relativeY = (float)(height - cameraPos.y);
		CloudRenderer.RelativeCameraPos relativeCameraPos;
		
		if(relativeY < 0) relativeCameraPos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
		else if(relativeY > 0) relativeCameraPos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
		else relativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
		
		if(this.needsRebuild || relativeCameraPos != this.prevRelativeCameraPos || cloudStatus != this.prevType) {
			this.needsRebuild = false;
			this.prevRelativeCameraPos = relativeCameraPos;
			this.prevType = cloudStatus;
			
			try(MeshData meshData = this.buildClouds(Tesselator.getInstance())) {
				if(this.vertexBuffer != null && this.vertexBuffer.size >= meshData.vertexBuffer().remaining()) {
					CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
					commandEncoder.writeToBuffer(this.vertexBuffer, meshData.vertexBuffer(), 0);
				}
				else {
					if(this.vertexBuffer != null) this.vertexBuffer.close();
					
					this.vertexBuffer = RenderSystem
						.getDevice()
						.createBuffer(() -> "Cloud vertex buffer", BufferType.VERTICES, BufferUsage.DYNAMIC_WRITE, meshData.vertexBuffer());
				}
				
				this.indexCount = meshData.drawState().indexCount();
			}
		}
		
		if(this.indexCount == 0) return;
		
		FogParameters fogParams = RenderSystem.getShaderFog();
		Vector4f color = new Vector4f(
			Mth.lerp(0.125f, ARGB.redFloat(cloudColor), fogParams.red()),
			Mth.lerp(0.125f, ARGB.greenFloat(cloudColor), fogParams.green()),
			Mth.lerp(0.125f, ARGB.blueFloat(cloudColor), fogParams.blue()),
			0.8f
		);
		float darkness = 1 - (forecast * 0.25f);
		color.mul(darkness, darkness, darkness, 1);
		RenderSystem.setShaderColor(color.x, color.y, color.z, color.w);
		
		float offset = (float)Mth.lerp(partialTick, cloudOffsetPrev, cloudOffset);
		float cloudX  = (float)(cameraPos.x / 12 + offset * 0.01);
		float cloudZ  = (float)(cameraPos.z / 12);
		
		float cForecast = Mth.lerp(partialTick, forecastPrev, forecast);
		
		Matrix4f extendedFarplane = ((ProjectionGetter)Minecraft.getInstance().gameRenderer).fetchProjectionMatrix(10_000, partialTick);
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(extendedFarplane, ProjectionType.PERSPECTIVE);
		RenderSystem.setShaderFog(new FogParameters(
			fogParams.start(), fogParams.end(),
			fogParams.shape(),
			fogParams.red(),
			fogParams.green(),
			fogParams.blue(),
			fogParams.alpha()
		));
		switch(relativeCameraPos) {
			case INSIDE_CLOUDS, ABOVE_CLOUDS -> {
				this.drawWithRenderType(cloudX, relativeY - 1, cloudZ, color, 0, cForecast);
				this.drawWithRenderType(cloudX, relativeY    , cloudZ, color, 1, cForecast);
				this.drawWithRenderType(cloudX, relativeY + 1, cloudZ, color, 2, cForecast);
				this.drawWithRenderType(cloudX, relativeY + 2, cloudZ, color, 3, cForecast);
			}
			case BELOW_CLOUDS -> {
				this.drawWithRenderType(cloudX, relativeY + 2, cloudZ, color, 3, cForecast);
				this.drawWithRenderType(cloudX, relativeY + 1, cloudZ, color, 2, cForecast);
				this.drawWithRenderType(cloudX, relativeY    , cloudZ, color, 1, cForecast);
				this.drawWithRenderType(cloudX, relativeY - 1, cloudZ, color, 0, cForecast);
			}
		}
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.restoreProjectionMatrix();
	}
	
	@Unique
	private void drawWithRenderType(float x, float y, float z, Vector4f color, int layer, float forecast) {
		RenderSystem.setModelOffset(0, y, 0);
		ResourceLocation cloudTexture = Barometry.getCloudTexture(forecast, layer);
		RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
		RenderTarget cloudsTarget = Minecraft.getInstance().levelRenderer.getCloudsTarget();
		GpuTexture colorBuffer;
		GpuTexture depthBuffer;
		if(cloudsTarget != null) {
			colorBuffer = cloudsTarget.getColorTexture();
			depthBuffer = cloudsTarget.getDepthTexture();
		}
		else {
			colorBuffer = mainTarget.getColorTexture();
			depthBuffer = mainTarget.getDepthTexture();
		}
		
		GpuBuffer gpuBuffer = this.indices.getBuffer(this.indexCount);
		GpuTexture gpuTexture = Minecraft.getInstance().getTextureManager().getTexture(cloudTexture).getTexture();
		//RenderSystem.setShaderTexture(0, Minecraft.getInstance().getTextureManager().getTexture(cloudTexture).getTexture());
		
		try(RenderPass renderPass = RenderSystem
			.getDevice()
			.createCommandEncoder()
			.createRenderPass(colorBuffer, OptionalInt.empty(), depthBuffer, OptionalDouble.empty())
		) {
			renderPass.setPipeline(Barometry.BAROMETRY_CLOUDS_PIPELINE);
			renderPass.bindSampler("Sampler0", gpuTexture);
			renderPass.setUniform("cloudColor", color.x, color.y, color.z, color.w);
			renderPass.setUniform("uvOffset", (x % 256) / 256, (z % 256) / 256);
			renderPass.setIndexBuffer(gpuBuffer, this.indices.type());
			renderPass.setVertexBuffer(0, this.vertexBuffer);
			renderPass.drawIndexed(0, this.indexCount);
		}
		
		RenderSystem.resetModelOffset();
	}
	
	@Unique
	private MeshData buildClouds(Tesselator tesselator) {
		BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		
		builder.addVertex(-256, 0,  256).setUv(-1,  1);
		builder.addVertex( 256, 0,  256).setUv( 1,  1);
		builder.addVertex( 256, 0, -256).setUv( 1, -1);
		builder.addVertex(-256, 0, -256).setUv(-1, -1);
		return builder.build();
	}
	
	@Override
	public void tick(float forecast, double cloudOffset) {
		this.forecastPrev = this.forecast;
		this.cloudOffsetPrev = this.cloudOffset;
		this.forecast = forecast;
		this.cloudOffset += cloudOffset;
	}
}
