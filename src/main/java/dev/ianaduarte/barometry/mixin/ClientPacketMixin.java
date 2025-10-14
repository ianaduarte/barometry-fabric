package dev.ianaduarte.barometry.mixin;

import dev.ianaduarte.barometry.network.ServerboundExtraOptionsPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketMixin extends ClientCommonPacketListenerImpl implements ClientGamePacketListener, TickablePacketListener {
	protected ClientPacketMixin(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
		super(minecraft, connection, commonListenerCookie);
	}
	
	@Inject(method = "handleLogin", at = @At("TAIL"))
	private void sendExtraOptions(ClientboundLoginPacket packet, CallbackInfo ci) {
		LocalPlayer player = this.minecraft.player;
		
		if(player != null) {
			ClientPlayNetworking.send(new ServerboundExtraOptionsPacket(player.getGameProfile().id(), this.minecraft.options.cloudRange().get()));
		}
	}
}
