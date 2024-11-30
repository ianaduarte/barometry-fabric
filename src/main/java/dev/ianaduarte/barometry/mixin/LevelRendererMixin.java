package dev.ianaduarte.barometry.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.ianaduarte.barometry.Barometry;
import dev.ianaduarte.barometry.ProjectionGetter;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("DataFlowIssue")
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Shadow @Nullable private ClientLevel level;
	@Shadow @Nullable private CloudStatus prevCloudsType;
	@Shadow @Nullable private VertexBuffer cloudBuffer;
	@Shadow @Final private Minecraft minecraft;
	
	@Unique double cloudOffsetPrev = 0;
	@Unique double cloudOffset = 0;
	
	@Unique
	void renderCloudLayer(PoseStack poseStack, Matrix4f projectionMatrix, ShaderInstance shader, int layer, float forecast, Vector4f color, float x, float y, float z) {
		ResourceLocation cloudTexture = Barometry.getCloudTexture(forecast, layer);
		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		
		poseStack.pushPose();
		poseStack.translate(0, y, 0);
		
		textureManager.getTexture(cloudTexture).setFilter(false, false);
		RenderSystem.setShaderTexture(0, cloudTexture);
		shader.safeGetUniform("cloudColor").set(color.x, color.y, color.z, color.w);
		shader.safeGetUniform("uvOffset").set((x % 256) / 256, (z % 256) / 256);
		this.cloudBuffer.bind();
		this.cloudBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
		
		poseStack.popPose();
	}
	
	/**
	 * @author ianaduarte
	 * @reason guhh
	 */
	@Overwrite
	public void renderClouds(PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, double camX, double camY, double camZ) {
		float cloudHeight = this.level.effects().getCloudHeight();
		if(Float.isNaN(cloudHeight)) return;
		
		if(this.cloudBuffer == null) {
			this.cloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
			this.cloudBuffer.bind();
			this.cloudBuffer.upload(this.buildClouds(Tesselator.getInstance()));
			VertexBuffer.unbind();
		}
		
		FogRenderer.levelFogColor();
		poseStack.pushPose();
		poseStack.mulPose(frustumMatrix);
		poseStack.scale(12.0F, 1.0F, 12.0F);
		poseStack.translate(0, cloudHeight - camY, 0);
		
		
		RenderType renderType = RenderType.clouds();
		renderType.setupRenderState();
		
		ShaderInstance shaderInstance = RenderSystem.getShader();
		Matrix4f extendedFarplane = ((ProjectionGetter)this.minecraft.gameRenderer).getProjectionMatrix(10_000, partialTick);
		Vector4f color = Barometry.getCloudColor(level, partialTick);
		float forecast = this.level.getRainLevel(partialTick) + this.level.getThunderLevel(partialTick);
		
		float offset = (float)Mth.lerp(partialTick, cloudOffsetPrev, cloudOffset);
		float cloudX  = (float)(camX / 12);
		float cloudZ  = (float)(camZ / 12);
		float cloudX1 = (float)(cloudX + offset * 0.01);
		
		float darkness = 1 - (forecast * 0.25f);
		color.mul(darkness, darkness, darkness, 1);
		renderCloudLayer(poseStack, extendedFarplane, shaderInstance, 3, forecast, color, cloudX1,  12f, cloudZ);
		renderCloudLayer(poseStack, extendedFarplane, shaderInstance, 2, forecast, color, cloudX1,  06f, cloudZ);
		renderCloudLayer(poseStack, extendedFarplane, shaderInstance, 1, forecast, color, cloudX1,  00f, cloudZ);
		renderCloudLayer(poseStack, extendedFarplane, shaderInstance, 0, forecast, color, cloudX1, -06f, cloudZ);
		//color.w *= 0.75f;
		//renderCloudLayer(poseStack, extendedFarplane, shaderInstance, 1, forecast, color, cloudX2 + 64f, -8f, cloudZ2 + 64f);
		
		renderType.clearRenderState();
		VertexBuffer.unbind();
		poseStack.popPose();
	}
	@Inject(method = "tick", at = @At("HEAD"))
	private void updateClouds(CallbackInfo ci) {
		float forecast = this.level.getRainLevel(1) + this.level.getThunderLevel(1);
		float speed    = Barometry.gradient(forecast / 2, 0.5f, 1.5f, 2.5f);
		
		cloudOffsetPrev = cloudOffset;
		cloudOffset += speed;
	}

	@Unique
	private MeshData buildClouds(Tesselator tesselator) {
		BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		
		builder.addVertex(-256, 0,  256).setUv(-1,  1).setColor(255, 255, 255, 1f).setNormal(0, -1, 0);
		builder.addVertex( 256, 0,  256).setUv( 1,  1).setColor(255, 255, 255, 1f).setNormal(0, -1, 0);
		builder.addVertex( 256, 0, -256).setUv( 1, -1).setColor(255, 255, 255, 1f).setNormal(0, -1, 0);
		builder.addVertex(-256, 0, -256).setUv(-1, -1).setColor(255, 255, 255, 1f).setNormal(0, -1, 0);
		return builder.build();
	}
}
