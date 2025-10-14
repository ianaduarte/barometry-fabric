package dev.ianaduarte.barometry.network;

import dev.ianaduarte.barometry.Barometry;
import dev.ianaduarte.barometry.render.WeatherLevelRenderer;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundWeatherPacket(int sampleRadius, byte[] coverage, byte[] forecast) implements CustomPacketPayload {
	private static final ResourceLocation ID = Barometry.getLocation("extra_options_sync");
	public static final Type<ClientboundWeatherPacket> TYPE = new Type<>(ID);
	public static final StreamCodec<ByteBuf, ClientboundWeatherPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT,
		ClientboundWeatherPacket::sampleRadius,
		ByteBufCodecs.BYTE_ARRAY,
		ClientboundWeatherPacket::coverage,
		ByteBufCodecs.BYTE_ARRAY,
		ClientboundWeatherPacket::forecast,
		ClientboundWeatherPacket::new
	);
	
	@Override
	public Type<ClientboundWeatherPacket> type() {
		return TYPE;
	}
	public static void handle(ClientboundWeatherPacket packet, ClientPlayNetworking.Context context) {
		var levelRenderer = context.client().levelRenderer;
		
		((WeatherLevelRenderer)levelRenderer).cloudRenderer().updateTextures(packet.sampleRadius, packet.coverage, packet.forecast);
	}
}
