package dev.ianaduarte.barometry.network;

import dev.ianaduarte.barometry.Barometry;
import dev.ianaduarte.barometry.util.ExtServerPlayer;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record ServerboundExtraOptionsPacket(UUID playerId, int cloudRenderDistance) implements CustomPacketPayload {
	private static final ResourceLocation ID = Barometry.getLocation("extra_options_sync");
	public static final Type<ServerboundExtraOptionsPacket> TYPE = new Type<>(ID);
	public static final StreamCodec<ByteBuf, ServerboundExtraOptionsPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
		ServerboundExtraOptionsPacket::playerId,
		ByteBufCodecs.INT,
		ServerboundExtraOptionsPacket::cloudRenderDistance,
		ServerboundExtraOptionsPacket::new
	);
	
	@Override
	public Type<ServerboundExtraOptionsPacket> type() {
		return TYPE;
	}
	public static void handle(ServerboundExtraOptionsPacket packet, ServerPlayNetworking.Context context) {
		ServerPlayer player = context.server().getPlayerList().getPlayer(packet.playerId);
		if(player == null) return;
		
		((ExtServerPlayer)player).cloudDistance(packet.cloudRenderDistance);
	}
}
