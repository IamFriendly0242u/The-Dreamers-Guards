package net.thedreamers.guards.punishment;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.thedreamers.guards.Thedreamers_guards;
import net.thedreamers.guards.config.AntiCheatConfig;
import net.thedreamers.guards.webhook.DiscordWebhook;
import net.thedreamers.lib.anticheat.AdminCommandCore;
import java.util.UUID;

public class PunishmentExecutor {

    private static final AdminCommandCore ADMIN_CORE = new AdminCommandCore(
            SuspensionManager.getEngine(),
            Thedreamers_guards.CONFIG_ENGINE
    );

    private static String cleanFormatting(String input) {
        if (input == null) return "";
        return input.replace("\\u00A7", "§").replace("&", "§").replace("\\n", "\n");
    }

    public static void execute(MinecraftServer server, ServerPlayer player, String reason, String mode) {
        if (player == null) return;
        String name = player.getScoreboardName();
        String uuid = player.getUUID().toString();
        String ip = player.getIpAddress();

        String automatedKickMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("kick.caught_message", "Illegal Modifications");
        boolean isAutomated = reason.equals(automatedKickMsg);

        int phase = SuspensionManager.getPhase(name, uuid, ip);
        try {
            SuspensionManager.getEngine().load();
            if (!isAutomated) {
                if ("BAN".equalsIgnoreCase(mode)) {
                    SuspensionManager.getEngine().suspend(name, 4, -1, reason);
                    SuspensionManager.getEngine().suspend(uuid, 4, -1, reason);
                    SuspensionManager.getEngine().suspend(ip, 4, -1, reason);
                    String rawBroadcast = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("broadcast_ban_message", "§3[BANNED] §e%s §7got caught cheating. See ya never!");
                    server.getPlayerList().broadcastSystemMessage(Component.literal(cleanFormatting(String.format(rawBroadcast, name))), false);
                    DiscordWebhook.sendAlert(server, name, uuid, ip, reason, 4, "BAN");
                } else {
                    int nextPhase = phase == 0 ? 1 : Math.min(phase + 1, 3);
                    int duration = 20;
                    if (nextPhase == 2) duration = 120;
                    if (nextPhase == 3) duration = 360;
                    SuspensionManager.getEngine().suspend(name, nextPhase, duration, reason);
                    SuspensionManager.getEngine().suspend(uuid, nextPhase, duration, reason);
                    SuspensionManager.getEngine().suspend(ip, nextPhase, duration, reason);
                    String rawBroadcast = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("broadcast_kick_message", "§a[KICKED] §e%s §7was removed from the game. Trash cleared.");
                    server.getPlayerList().broadcastSystemMessage(Component.literal(cleanFormatting(String.format(rawBroadcast, name))), false);
                    DiscordWebhook.sendAlert(server, name, uuid, ip, reason, nextPhase, "KICK");
                }
            }
            player.connection.disconnect(SuspensionManager.getSuspensionComponent(name, uuid, ip));
        } catch (Exception e) {
            String consoleLogFailed = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("console_log_failed", "§c[WARN] FAILED! §7Failed to execute action on %s.");
            server.getPlayerList().broadcastSystemMessage(Component.literal(cleanFormatting(String.format(consoleLogFailed, name))), false);
        }
    }
}