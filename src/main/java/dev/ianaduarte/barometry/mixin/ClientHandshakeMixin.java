package dev.ianaduarte.barometry.mixin;

import dev.ianaduarte.barometry.network.ServerboundExtraOptionsPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientHandshakeMixin {
	@Shadow @Final private Minecraft minecraft;
	
	@Inject(method = "handleLoginFinished", at = @At("TAIL"))
	private void sendExtraOptions(ClientboundLoginFinishedPacket packet, CallbackInfo ci) {
		LocalPlayer player = this.minecraft.player;
		
		if(player != null) {
			ClientPlayNetworking.send(new ServerboundExtraOptionsPacket(player.getGameProfile().id(), this.minecraft.options.cloudRange().get()));
		}
	}
}
