package net.thedreamers.guards.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AntiCheatAuthPayload(String version, String token, String modList) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AntiCheatAuthPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thedreamers_guards", "auth_packet"));

    public static final StreamCodec<FriendlyByteBuf, AntiCheatAuthPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AntiCheatAuthPayload decode(FriendlyByteBuf buf) {
            return new AntiCheatAuthPayload(buf.readUtf(32767), buf.readUtf(32767), buf.readUtf(262144));
        }

        @Override
        public void encode(FriendlyByteBuf buf, AntiCheatAuthPayload payload) {
            buf.writeUtf(payload.version(), 32767);
            buf.writeUtf(payload.token(), 32767);
            buf.writeUtf(payload.modList(), 262144);
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}