package dev.ianaduarte.barometry.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ianaduarte.barometry.render.ProjectionGetter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements ProjectionGetter {
	@Shadow @Final private Camera mainCamera;
	@Shadow @Final private Minecraft minecraft;
	@Shadow protected abstract void bobHurt(PoseStack poseStack, float partialTicks);
	@Shadow protected abstract void bobView(PoseStack poseStack, float partialTicks);
	
	@Shadow protected abstract float getFov(Camera camera, float partialTicks, boolean useFOVSetting);
	@Shadow private float spinningEffectTime;
	@Shadow private float spinningEffectSpeed;
	
	public Matrix4f calculateExtendedFarplaneMatrix(float farPlane, float partialTicks) {
		Matrix4f projMatrix = this.calcProjMatrix(farPlane, partialTicks);
		this.bobMatrix(projMatrix, this.mainCamera.getPartialTickTime());
		
		
		LocalPlayer localPlayer = this.minecraft.player;
		if(localPlayer == null) return projMatrix;
		
		float effectsScale = this.minecraft.options.screenEffectScale().get().floatValue();
		float portalFactor = Mth.lerp(partialTicks, localPlayer.oPortalEffectIntensity, localPlayer.portalEffectIntensity);
		float nauseaFactor = localPlayer.getEffectBlendFactor(MobEffects.NAUSEA, partialTicks);
		float screenWobble = Math.max(portalFactor, nauseaFactor) * (effectsScale * effectsScale);
		
		if(screenWobble > 0.0F) {
			float wobbleFactor = 5.0F / (screenWobble * screenWobble + 5.0F) - screenWobble * 0.04F;
			wobbleFactor *= wobbleFactor;
			
			Vector3f spinAxis = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
			float spinAngle = (this.spinningEffectTime + partialTicks * this.spinningEffectSpeed) * (float) (Math.PI / 180.0);
			
			projMatrix.rotate(spinAngle, spinAxis);
			projMatrix.scale(1.0F / wobbleFactor, 1.0F, 1.0F);
			projMatrix.rotate(-spinAngle, spinAxis);
		}
		return projMatrix;
	}
	
	@Unique
	private Matrix4f calcProjMatrix(float farPlane, float partialTicks) {
		return new Matrix4f().perspective(
			this.getFov(this.mainCamera, partialTicks, true) * Mth.DEG_TO_RAD,
			(float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight(),
			0.05F, farPlane
		);
	}
	@Unique
	private void bobMatrix(Matrix4f matrix, float partialTicks) {
		PoseStack poseStack = new PoseStack();
		this.bobHurt(poseStack, partialTicks);
		
		if(this.minecraft.options.bobView().get()) this.bobView(poseStack, partialTicks);
		matrix.mul(poseStack.last().pose());
	}
}
