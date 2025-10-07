package dev.ianaduarte.barometry;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

public class Barometry implements ModInitializer {
	public static final String MOD_ID = "barometry";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	public static final RenderPipeline BAROMETRY_CLOUDS_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
			.withVertexShader(getLocation("core/rendertype_bclouds"))
			.withFragmentShader(getLocation("core/rendertype_bclouds"))
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
			.withBlend(BlendFunction.TRANSLUCENT)
			.withCull(false)
			.withSampler("Sampler0")
			.withSampler("Sampler1")
			.withUniform("CloudInfo", UniformType.UNIFORM_BUFFER)
			.withLocation(getLocation("pipeline/bclouds"))
			.build()
	);
	public static final ResourceLocation CLEAN_CLOUDS_LOCATION = getLocation("textures/environment/clouds_clean.png");
	public static final ResourceLocation CLEAR_CLEAN_CLOUDS_LOCATION = getLocation("textures/environment/clouds_clear_clean.png");
	public static final ResourceLocation CLEAR_CLOUDS_LOCATION = getLocation("textures/environment/clouds_clear.png");
	public static final ResourceLocation CLEAR_RAIN_CLOUDS_LOCATION = getLocation("textures/environment/clouds_clear_rain.png");
	public static final ResourceLocation RAIN_CLOUDS_LOCATION = getLocation("textures/environment/clouds_rain.png");
	public static final ResourceLocation RAIN_THUNDER_CLOUDS_LOCATION = getLocation("textures/environment/clouds_rain_thunder.png");
	public static final ResourceLocation THUNDER_CLOUDS_LOCATION = getLocation("textures/environment/clouds_thunder.png");
	public static final ResourceLocation NOISE2_LOCATION = getLocation("textures/environment/noise2.png");
	public static final ResourceLocation NOISE2_ALT_LOCATION = getLocation("textures/environment/noise2_alt.png");
	public static final ResourceLocation[] CLOUD_TEXTURES = {
		CLEAN_CLOUDS_LOCATION,
		CLEAR_CLEAN_CLOUDS_LOCATION,
		CLEAR_CLOUDS_LOCATION,
		CLEAR_RAIN_CLOUDS_LOCATION,
		RAIN_CLOUDS_LOCATION,
		RAIN_THUNDER_CLOUDS_LOCATION,
		THUNDER_CLOUDS_LOCATION
	};
	public static ResourceLocation getLocation(String path){
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
	
	
	
	private static final int[] cloudLayerIndexOffset = { 0, 2, 1, 0 };
	public static ResourceLocation getCloudTexture(float forecast, int layer, boolean lower) {
		if(lower) return THUNDER_CLOUDS_LOCATION;
		float f = (MathUtil.roundn(forecast, 0.5f) / 2f) * 4f;
		return Barometry.CLOUD_TEXTURES[Math.min((int)f + cloudLayerIndexOffset[layer], CLOUD_TEXTURES.length)];
	}
	public static Vector4f getCloudColor(ClientLevel level, float partialTick) {
		float timeOfDay = level.getTimeOfDay(partialTick);
		float colorFactor = Mth.cos(timeOfDay * (float) (Math.PI * 2)) * 2.0F + 0.5F;
		colorFactor = Mth.clamp(colorFactor, 0.0F, 1.0F);
		
		float r = 1;
		float g = 1;
		float b = 1;
		float rainLevel = level.getRainLevel(partialTick);
		if (rainLevel > 0) {
			float modulator = (r * 0.3f + g * 0.59f + b * 0.11f) * 0.6f;
			float rainFactor = (1 - rainLevel * 0.95f) + (modulator * rainLevel * 0.95f);
			
			r *= rainFactor;
			g *= rainFactor;
			b *= rainFactor;
		}
		
		r *= colorFactor * 0.9f + 0.1f;
		g *= colorFactor * 0.9f + 0.1f;
		b *= colorFactor * 0.85f + 0.15f;
		float thunderLevel = level.getThunderLevel(partialTick);
		if(thunderLevel > 0) {
			float modulator = (r * 0.3f + g * 0.59f + b * 0.11f) * 0.6f;
			float thunderFactor = (1 - thunderLevel * 0.95f) + (modulator * thunderLevel * 0.95f);
			
			r *= thunderFactor;
			g *= thunderFactor;
			b *= thunderFactor;
		}
		return new Vector4f(r, g, b, 0.8f);
	}
	
	private static final float[] cloudLayerOffset = { -0.1f, 0.2f, 0f, -0.1f };
	public static float remap(float value, float oldMin, float oldMax, float newMin, float newMax) {
		return newMin + (value - oldMin) * (newMax - newMin) / (oldMax - oldMin);
	}
	public static float scaleForecast(int layer, float forecast) {
		//float frac = 1f / cloudLayerOffset[(int)(roundN(forecast, 0.5f) * 2)];
		//float mul = 1 + cloudLayerOffset[layer];
		//return forecast * mul;
		//return (forecast + cloudLayerOffset[layer]);
		return remap(forecast + cloudLayerOffset[layer], -0.1f, 1.2f, 0, 1);
	}
	public static float scaleAlpha(int layer, float alpha) {
		float scale = (0.8f + cloudLayerOffset[layer]);
		return alpha * scale * scale;
	}
	
	@Override
	public void onInitialize() {
	}
}