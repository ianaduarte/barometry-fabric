package dev.ianaduarte.barometry.mixin;

import dev.ianaduarte.barometry.network.ServerboundExtraOptionsPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class OptionsMixin {
	@Shadow protected Minecraft minecraft;
	@Shadow @Final private OptionInstance<Integer> cloudRange;
	
	@Inject(method = "broadcastOptions", at = @At("HEAD"))
	private void synchExtraOptions(CallbackInfo ci) {
		LocalPlayer player = this.minecraft.player;
		if(player != null) {
			ClientPlayNetworking.send(new ServerboundExtraOptionsPacket(player.getGameProfile().id(), this.cloudRange.get()));
		}
	}
}
