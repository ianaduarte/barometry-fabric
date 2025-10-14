package dev.ianaduarte.barometry.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class BarometryPackets {
	private BarometryPackets() {}
	
	private static <T extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<T> type, StreamCodec<ByteBuf, T> streamCodec, ServerPlayNetworking.PlayPayloadHandler<T> handler) {
		PayloadTypeRegistry.playC2S().register(type, streamCodec);
		ServerPlayNetworking.registerGlobalReceiver(type, handler);
	}
	private static <T extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<T> type, StreamCodec<ByteBuf, T> streamCodec, ClientPlayNetworking.PlayPayloadHandler<T> handler) {
		PayloadTypeRegistry.playS2C().register(type, streamCodec);
		ClientPlayNetworking.registerGlobalReceiver(type, handler);
	}
	public static void registerPackets() {
		registerC2S(ServerboundExtraOptionsPacket.TYPE, ServerboundExtraOptionsPacket.STREAM_CODEC, ServerboundExtraOptionsPacket::handle);
		registerS2C(ClientboundWeatherPacket.TYPE, ClientboundWeatherPacket.STREAM_CODEC, ClientboundWeatherPacket::handle);
	}
}
