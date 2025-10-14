package dev.ianaduarte.barometry;

import dev.ianaduarte.barometry.network.BarometryPackets;
import dev.ianaduarte.barometry.network.ClientboundWeatherPacket;
import dev.ianaduarte.barometry.util.ExtEntity;
import dev.ianaduarte.barometry.util.ExtServerPlayer;
import dev.ianaduarte.barometry.weather.WeatherLevel;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

public class Barometry implements ModInitializer {
	public static final String MOD_ID = "barometry";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	public static ResourceLocation getLocation(String path){
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
	
	@Override
	public void onInitialize() {
		BarometryPackets.registerPackets();
		ServerTickEvents.START_WORLD_TICK.register(level -> {
			var weatherSystem = ((WeatherLevel)level).weatherSystem();
			
			for(var player : level.players()) {
				if(!((ExtEntity)player).changedChunkPositions()) continue;
				
				var chunkRadius = ((ExtServerPlayer)player).cloudDistance() + 1;
				if(chunkRadius <= 1) continue;
				
				var chunkDiameter = chunkRadius * 2 + 1;
				var forecast = new byte[chunkDiameter * chunkDiameter];
				var coverage = new byte[chunkDiameter * chunkDiameter];
				var chunkX = (int)Math.floor(player.getX() / 16);
				var chunkZ = (int)Math.floor(player.getZ() / 16);
				
				var chunkCount = chunkDiameter * chunkDiameter;
				for(int i = 0; i < chunkCount; i++) {
					int x = chunkX + (i % chunkDiameter) - chunkRadius;
					int z = chunkZ + (i / chunkDiameter) - chunkRadius;
					coverage[i] = (byte)(weatherSystem.getCoverageNoise(x, z) * 255.0);
					forecast[i] = (byte)((weatherSystem.getRainLevel(x, z) + weatherSystem.getThunderLevel(x, z)) * 0.5 * 255.0);
				}
				
				ServerPlayNetworking.send(player, new ClientboundWeatherPacket(chunkRadius, coverage, forecast));
			}
		});
	}
}