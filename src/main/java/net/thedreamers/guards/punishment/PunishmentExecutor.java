package net.thedreamers.guards.punishment;

import net.thedreamers.guards.config.AntiCheatConfig;
import net.thedreamers.guards.webhook.DiscordWebhook;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.NameAndId;

public class PunishmentExecutor {

    public static void executeAuto(MinecraftServer server, ServerPlayer player, String reason) {
        String configuredMode = AntiCheatConfig.getPunishmentMode();
        execute(server, player, reason, configuredMode);
    }

    public static void executeManual(MinecraftServer server, ServerPlayer player, String reason, String action) {
        execute(server, player, reason, action.toUpperCase());
    }

    public static void execute(MinecraftServer server, ServerPlayer player, String reason, String mode) {
        if (player == null) {
            String failedLog = String.format(AntiCheatConfig.getConsoleLogFailed(), "Unknown Player");
            broadcastToAll(server, AntiCheatConfig.getConsoleLogFailed().substring(0, 24) + failedLog);
            return;
        }

        String playerName = player.getScoreboardName();
        String playerUuid = player.getUUID().toString();
        String playerIp = player.getIpAddress();
        String kickScreenTemplate = AntiCheatConfig.getKickCheaterMessage();
        Component kickScreenMessage = Component.literal(String.format(kickScreenTemplate, reason));

        try {
            int phase = SuspensionManager.getPhase(playerName, playerUuid, playerIp);
            if ("BAN".equals(mode)) {
                applyBan(server, player, reason);
                player.connection.disconnect(kickScreenMessage);
                String format = AntiCheatConfig.getBroadcastBanMessage();
                broadcastToAll(server, String.format(format, playerName));
                DiscordWebhook.sendAlert(server, playerName, playerUuid, playerIp, reason, phase, "BAN");
            } else {
                SuspensionManager.suspend(playerName, playerUuid, playerIp);
                int postPhase = SuspensionManager.getPhase(playerName, playerUuid, playerIp);
                player.connection.disconnect(kickScreenMessage);
                String format = AntiCheatConfig.getBroadcastKickMessage();
                broadcastToAll(server, String.format(format, playerName));
                DiscordWebhook.sendAlert(server, playerName, playerUuid, playerIp, reason, postPhase, "KICK (SUSPEND)");
            }
        } catch (Exception exception) {
            String failedLog = String.format(AntiCheatConfig.getConsoleLogFailed(), playerName);
            broadcastToAll(server, failedLog);
            exception.printStackTrace();
        }
    }

    private static void applyBan(MinecraftServer server, ServerPlayer player, String reason) {
        GameProfile profile = player.getGameProfile();
        UserBanList banList = server.getPlayerList().getBans();
        NameAndId nameAndId = new NameAndId(profile.id(), profile.name());
        UserBanListEntry banEntry = new UserBanListEntry(nameAndId, null, "TheDreamers Guards", null, reason);
        banList.add(banEntry);
    }

    private static void broadcastToAll(MinecraftServer server, String message) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }
}