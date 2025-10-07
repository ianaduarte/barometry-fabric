package dev.ianaduarte.barometry.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ianaduarte.barometry.ProjectionGetter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings("DataFlowIssue")
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements ProjectionGetter {
	@Shadow @Final private Camera mainCamera;
	@Shadow @Final private Minecraft minecraft;
	@Shadow protected abstract void bobHurt(PoseStack poseStack, float partialTicks);
	@Shadow protected abstract void bobView(PoseStack poseStack, float partialTicks);
	
	@Shadow protected abstract float getFov(Camera camera, float partialTicks, boolean useFOVSetting);
	@Shadow private float spinningEffectTime;
	@Shadow private float spinningEffectSpeed;
	
	public Matrix4f fetchProjectionMatrix(float farPlane, float partialTicks) {
		float fov = this.getFov(this.mainCamera, partialTicks, true);
		Matrix4f matrix4f = new Matrix4f().perspective(
			fov * (float) (Math.PI / 180.0),
			(float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight(),
			0.05F, farPlane
		);
		LocalPlayer localPlayer = this.minecraft.player;
		
		PoseStack poseStack = new PoseStack();
		this.bobHurt(poseStack, this.mainCamera.getPartialTickTime());
		if(this.minecraft.options.bobView().get()) {
			this.bobView(poseStack, this.mainCamera.getPartialTickTime());
		}
		
		matrix4f.mul(poseStack.last().pose());
		float i = this.minecraft.options.screenEffectScale().get().floatValue();
		float j = Mth.lerp(partialTicks, localPlayer.oPortalEffectIntensity, localPlayer.portalEffectIntensity);
		float k = localPlayer.getEffectBlendFactor(MobEffects.NAUSEA, partialTicks);
		float l = Math.max(j, k) * (i * i);
		if (l > 0.0F) {
			float m = 5.0F / (l * l + 5.0F) - l * 0.04F;
			m *= m;
			Vector3f vector3f = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
			float n = (this.spinningEffectTime + partialTicks * this.spinningEffectSpeed) * (float) (Math.PI / 180.0);
			matrix4f.rotate(n, vector3f);
			matrix4f.scale(1.0F / m, 1.0F, 1.0F);
			matrix4f.rotate(-n, vector3f);
		}
		return matrix4f;
	}
}
