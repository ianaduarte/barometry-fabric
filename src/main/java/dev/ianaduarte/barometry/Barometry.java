package dev.ianaduarte.barometry;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

public class Barometry implements ModInitializer {
	public static final String MOD_ID = "barometry";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	public static final ResourceLocation CLEAN_CLOUDS_LOCATION = getLocation("textures/environment/clouds_clean.png");
	public static final ResourceLocation CLEAR_CLEAN_CLOUDS_LOCATION = getLocation("textures/environment/clouds_clear_clean.png");
	public static final ResourceLocation CLEAR_CLOUDS_LOCATION = getLocation("textures/environment/clouds_clear.png");
	public static final ResourceLocation CLEAR_RAIN_CLOUDS_LOCATION = getLocation("textures/environment/clouds_clear_rain.png");
	public static final ResourceLocation RAIN_CLOUDS_LOCATION = getLocation("textures/environment/clouds_rain.png");
	public static final ResourceLocation RAIN_THUNDER_CLOUDS_LOCATION = getLocation("textures/environment/clouds_rain_thunder.png");
	public static final ResourceLocation THUNDER_CLOUDS_LOCATION = getLocation("textures/environment/clouds_thunder.png");
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
	
	public static float roundN(float f, float n) {
		return (float)Math.round(f / n) * n;
	}
	public static float gradient(float delta, float... values) {
		if(values.length == 0) throw new IllegalArgumentException("Gradient array cannot be empty.");
		if(delta <= 0) return values[0];
		if(delta >= 1) return values[values.length - 1];
		
		int index = (int) (delta * (values.length - 1));
		float t = delta * (values.length - 1) - index;
		return values[index] * (1 - t) + values[index + 1] * t;
	}
	
	private static final int[] cloudLayerTexOffset = { 0, 2, 1, 0 };
	public static ResourceLocation getCloudTexture(float forecast, int layer) {
		float f = (roundN(forecast, 0.5f) / 2f) * 4f;
		return Barometry.CLOUD_TEXTURES[(int)f + cloudLayerTexOffset[layer]];
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

	@Override
	public void onInitialize() {
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(modContainer -> {
			ResourceManagerHelper.registerBuiltinResourcePack(getLocation("shader_patch"), modContainer, ResourcePackActivationType.ALWAYS_ENABLED);
		});
	}
}