package dev.ianaduarte.barometry.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.ianaduarte.barometry.Barometry;
import dev.ianaduarte.barometry.ExtCloudRenderer;
import dev.ianaduarte.barometry.ProjectionGetter;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.*;

@SuppressWarnings("DataFlowIssue")
@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin implements ExtCloudRenderer {
	@Shadow @Final private VertexBuffer vertexBuffer;
	@Shadow private boolean needsRebuild;
	@Shadow private CloudRenderer.RelativeCameraPos prevRelativeCameraPos;
	@Shadow private @Nullable CloudStatus prevType;
	@Shadow private boolean vertexBufferEmpty;
	@Shadow @Nullable private CloudRenderer.@Nullable TextureData texture;
	
	@Unique float forecastPrev = 0;
	@Unique float forecast = 0;
	@Unique double cloudOffsetPrev = 0;
	@Unique double cloudOffset = 0;
	
	/**
	 * @author ianaduarte
	 * @reason testing testing 123
	 */
	@Overwrite
	public void render(int cloudColor, CloudStatus cloudStatus, float cloudHeight, Matrix4f modelMatrix, Matrix4f projectionMatrix, Vec3 cameraPos, float currentTick) {
		if(this.texture == null) return;
		
		float partialTick = currentTick - (long)currentTick;
		float relativeY = (float)(cloudHeight - cameraPos.y);
		CloudRenderer.RelativeCameraPos relativeCameraPos;
		
		if(relativeY < 0) relativeCameraPos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
		else if(relativeY > 0) relativeCameraPos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
		else relativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
		
		this.vertexBuffer.bind();
		if(this.needsRebuild || relativeCameraPos != this.prevRelativeCameraPos || cloudStatus != this.prevType) {
			this.needsRebuild = false;
			this.prevRelativeCameraPos = relativeCameraPos;
			this.prevType = cloudStatus;
			
			MeshData meshData = this.buildClouds(Tesselator.getInstance());
			if(meshData != null) {
				this.vertexBuffer.upload(meshData);
				this.vertexBufferEmpty = false;
			} else {
				this.vertexBufferEmpty = true;
			}
		}
		
		if(this.vertexBufferEmpty) return;
		
		Vector4f color = Barometry.getCloudColor(Minecraft.getInstance().player.clientLevel, partialTick);
		float darkness = 1 - (forecast * 0.25f);
		color.mul(darkness, darkness, darkness, 1);
		//RenderSystem.setShaderColor(ARGB.redFloat(cloudColor), ARGB.greenFloat(cloudColor), ARGB.blueFloat(cloudColor), 1.0F);
		
		float offset = (float)Mth.lerp(partialTick, cloudOffsetPrev, cloudOffset);
		float cloudX  = (float)(cameraPos.x / 12 + offset * 0.01);
		float cloudZ  = (float)(cameraPos.z / 12);
		
		float cForecast = Mth.lerp(partialTick, forecastPrev, forecast);
		
		Matrix4f extendedFarplane = ((ProjectionGetter)Minecraft.getInstance().gameRenderer).getProjectionMatrix(10_000, partialTick);
		switch(relativeCameraPos) {
			case INSIDE_CLOUDS, ABOVE_CLOUDS -> {
				this.drawWithRenderType(RenderType.clouds(), modelMatrix, extendedFarplane, cloudX, relativeY - 1, cloudZ, color, 0, cForecast);
				this.drawWithRenderType(RenderType.clouds(), modelMatrix, extendedFarplane, cloudX, relativeY    , cloudZ, color, 1, cForecast);
				this.drawWithRenderType(RenderType.clouds(), modelMatrix, extendedFarplane, cloudX, relativeY + 1, cloudZ, color, 2, cForecast);
				this.drawWithRenderType(RenderType.clouds(), modelMatrix, extendedFarplane, cloudX, relativeY + 2, cloudZ, color, 3, cForecast);
			}
			case BELOW_CLOUDS -> {
				this.drawWithRenderType(RenderType.clouds(), modelMatrix, extendedFarplane, cloudX, relativeY + 2, cloudZ, color, 3, cForecast);
				this.drawWithRenderType(RenderType.clouds(), modelMatrix, extendedFarplane, cloudX, relativeY + 1, cloudZ, color, 2, cForecast);
				this.drawWithRenderType(RenderType.clouds(), modelMatrix, extendedFarplane, cloudX, relativeY    , cloudZ, color, 1, cForecast);
				this.drawWithRenderType(RenderType.clouds(), modelMatrix, extendedFarplane, cloudX, relativeY - 1, cloudZ, color, 0, cForecast);
			}
		}
		
		VertexBuffer.unbind();
	}
	
	
	@Unique
	void drawWithRenderType(RenderType renderType, Matrix4f modelMatrix, Matrix4f projectionMatrix, float xOffset, float height, float zOffset, Vector4f color, int layer, float forecast) {
		renderType.setupRenderState();
		RenderSystem.disableCull();
		ResourceLocation cloudTexture = Barometry.getCloudTexture(forecast, layer);
		Minecraft.getInstance().getTextureManager().getTexture(cloudTexture).setFilter(false, false);
		
		RenderSystem.setShaderTexture(0, cloudTexture);
		CompiledShaderProgram compiledShaderProgram = RenderSystem.getShader();
		if (compiledShaderProgram != null) {
			compiledShaderProgram.safeGetUniform("cloudColor").set(color.x, color.y, color.z, color.w);
			compiledShaderProgram.safeGetUniform("uvOffset").set((xOffset % 256) / 256, (zOffset % 256) / 256);
			if(compiledShaderProgram.MODEL_OFFSET != null) compiledShaderProgram.MODEL_OFFSET.set(0, height, 0);
		}
		
		this.vertexBuffer.drawWithShader(modelMatrix, projectionMatrix, compiledShaderProgram);
		RenderSystem.enableCull();
		renderType.clearRenderState();
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
