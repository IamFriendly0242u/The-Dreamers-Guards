package net.thedreamers.guards.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.thedreamers.guards.Thedreamers_guards;
import net.thedreamers.guards.config.AntiCheatConfig;
import net.thedreamers.guards.punishment.SuspensionManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerPlayerListener {

    private static final Map<UUID, Long> JOIN_TIMESTAMPS = new ConcurrentHashMap<>();

    public static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String playerName = handler.player.getScoreboardName();
            String uuidStr = handler.player.getUUID().toString();
            String ip = handler.player.getIpAddress();
            UUID uuid = handler.player.getUUID();
            SuspensionManager.getEngine().load();
            if (SuspensionManager.isSuspended(playerName, uuidStr, ip)) {
                server.execute(() -> handler.disconnect(SuspensionManager.getSuspensionComponent(playerName, uuidStr, ip)));
                return;
            }
            Thedreamers_guards.PENDING_VERIFICATION.add(uuid);
            JOIN_TIMESTAMPS.put(uuid, System.currentTimeMillis());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            String playerName = handler.player.getScoreboardName().toLowerCase();
            Thedreamers_guards.TRUSTED_PLAYERS.remove(playerName);
            Thedreamers_guards.PENDING_VERIFICATION.remove(uuid);
            JOIN_TIMESTAMPS.remove(uuid);
        });
    }

    public static void clearTimeout(UUID uuid) {
        JOIN_TIMESTAMPS.remove(uuid);
    }

    public static void tick(MinecraftServer server) {
        if (JOIN_TIMESTAMPS.isEmpty()) return;
        long now = System.currentTimeMillis();
        long delayThreshold = Long.parseLong(AntiCheatConfig.getProperty("validation_delay_ms", "10000"));
        for (UUID uuid : JOIN_TIMESTAMPS.keySet()) {
            long joinTime = JOIN_TIMESTAMPS.get(uuid);
            if (now - joinTime > delayThreshold) {
                JOIN_TIMESTAMPS.remove(uuid);
                Thedreamers_guards.PENDING_VERIFICATION.remove(uuid);
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    server.execute(() -> {
                        String msg = "§c[The Dreamers Guards]\n§7Verification timeout. Client failed to provide security telemetry.";
                        player.connection.disconnect(Component.literal(msg));
                    });
                }
            }
        }
    }
}