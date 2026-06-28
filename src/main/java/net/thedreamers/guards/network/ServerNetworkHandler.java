package net.thedreamers.guards.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.thedreamers.guards.Thedreamers_guards;
import net.thedreamers.guards.config.AntiCheatConfig;
import net.thedreamers.guards.punishment.FlagSession;
import net.thedreamers.guards.webhook.DiscordWebhook;
import net.thedreamers.lib.anticheat.AntiCheatCore;
import net.thedreamers.lib.security.SecurityUtils;
import net.thedreamers.guards.punishment.SuspensionManager;

public class ServerNetworkHandler {

    private static final AntiCheatCore CORE = new AntiCheatCore(SuspensionManager.getEngine());

    private static String cleanFormatting(String input) {
        if (input == null) return "";
        return input.replace("\\u00A7", "§").replace("&", "§").replace("\\n", "\n");
    }

    public static void processVerification(MinecraftServer server, ServerPlayer player, String clientVer, String token, String mods, String name, String uuid) {
        if (Thedreamers_guards.TRUSTED_PLAYERS.contains(name.toLowerCase())) {
            DiscordWebhook.sendVerifyAlert(server, name, uuid, "SAFE", "Bypassed via Trust Protocol");
            return;
        }
        String decryptedVersion = SecurityUtils.decrypt(clientVer);
        String decryptedToken = SecurityUtils.decrypt(token);
        String decryptedMods = SecurityUtils.decrypt(mods);
        if (decryptedVersion == null) decryptedVersion = "";
        decryptedVersion = decryptedVersion.trim();
        String serverVer = Thedreamers_guards.CONFIG_ENGINE.getModVersion().trim();
        boolean enforceVersion = Boolean.parseBoolean(Thedreamers_guards.CONFIG_ENGINE.getProperty("enforce_version_match", "true"));
        if (enforceVersion && !decryptedVersion.equals(serverVer)) {
            String rawMismatch = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("kick.version_mismatch", "§c[The Dreamers Guards]\\n\\n§6■ SECURITY NOTICE: VERSION MISMATCH ■\\n§7Your client mod version does not match the server requirements.\\n\\n§7Required Server Version: §e%s\\n§7Your Client Version: §c%s\\n\\n§7Please install the exact matching mod version before trying to reconnect.");
            String styledMismatchMessage = String.format(rawMismatch, serverVer, decryptedVersion);
            player.connection.disconnect(Component.literal(cleanFormatting(styledMismatchMessage)));
            DiscordWebhook.sendVerifyAlert(server, name, uuid, "FAILED", "Version Mismatch");
            return;
        }
        if ("DIRTY_CHEATER".equals(decryptedToken)) {
            FlagSession.start(server, player, "Illegal Modifications");
            DiscordWebhook.sendVerifyAlert(server, name, uuid, "CHEATER", "Client Scanner Flagged");
            return;
        }
        if (CORE.scanModHandshake(decryptedMods, AntiCheatConfig.getModBlacklist())) {
            FlagSession.start(server, player, "Blacklisted Modification Signature Found");
            DiscordWebhook.sendVerifyAlert(server, name, uuid, "CHEATER", "Blacklisted Mod Detected");
            return;
        }
        DiscordWebhook.sendVerifyAlert(server, name, uuid, "SAFE", "Passed secure authentication");
    }
}