package net.thedreamers.guards.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class GuardsClientNetworkMixin {

    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void onClientLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        System.out.println("[Thedreamers Guards] Client network listener verified.");
    }
}