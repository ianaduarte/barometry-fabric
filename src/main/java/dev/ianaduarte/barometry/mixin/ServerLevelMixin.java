package dev.ianaduarte.barometry.mixin;

import dev.ianaduarte.barometry.render.BarometryCloudRenderer;
import dev.ianaduarte.barometry.weather.WeatherLevel;
import dev.ianaduarte.barometry.weather.WeatherSystem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements WeatherLevel {
	@Unique private WeatherSystem weatherSystem;
	
	@Inject(method = "<init>", at = @At("TAIL"))
	private void initializeWeather(MinecraftServer server, Executor dispatcher, LevelStorageSource.LevelStorageAccess storageSource, ServerLevelData levelData, ResourceKey dimension, LevelStem levelStem, boolean isDebug, long biomeZoomSeed, List customSpawners, boolean tickTime, RandomSequences randomSequences, CallbackInfo ci) {
		this.weatherSystem = new WeatherSystem((ServerLevel)(Object)this);
	}
	
	@Override
	public WeatherSystem weatherSystem() {
		return this.weatherSystem;
	}
}
