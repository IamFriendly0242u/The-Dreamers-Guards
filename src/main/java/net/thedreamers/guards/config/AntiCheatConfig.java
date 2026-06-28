package net.thedreamers.guards.config;

import net.thedreamers.guards.Thedreamers_guards;

public class AntiCheatConfig {

    public static void loadConfig() {
        Thedreamers_guards.CONFIG_ENGINE.load();
    }

    public static String getProperty(String key, String defaultValue) {
        return Thedreamers_guards.CONFIG_ENGINE.getProperty(key, defaultValue);
    }

    public static String getLanguage(String key, String defaultValue) {
        return Thedreamers_guards.CONFIG_ENGINE.getLanguageString(key, defaultValue);
    }

    public static String getKickVersionMismatchMessage() {
        return getProperty("kick_version_mismatch_message", "§c[The Dreamers Guards]\n§7Whoops! Your mod version doesn't match the server requirement.\n§7Server: §e%s §7| Yours: §c%s");
    }

    public static String getKickCheaterMessage() {
        return getProperty("kick_cheater_message", "§c[The Dreamers Guards]\n§7Nice try, but you got caught: §e%s");
    }

    public static String getBroadcastBanMessage() {
        return getProperty("broadcast_ban_message", "§3[BANNED] §e%s §7got caught cheating. See ya never!");
    }

    public static String getBroadcastKickMessage() {
        return getProperty("broadcast_kick_message", "§a[KICKED] §e%s §7was removed from the game. Trash cleared.");
    }

    public static String getConsoleLogFailed() {
        return getLanguage("console_log_failed", "§c[WARN] FAILED! §7Failed to execute action on %s.");
    }

    public static String[] getModBlacklist() {
        return getProperty("modBlacklist", "wurst,meteor,liquidbounce,bleachhack,aristois,kami,rusherhack,future,salhack,phobos,kami-blue,konas,inertia,mathax,vector,danielfrominternet,seppuku,coffee,lambda,abyss,w+3,w+2,gopro,earthhack,bleach,pixel,liquid,ares,novoline,flux,rise,tenacity,vape,astolfo,zeroday").split(",");
    }

    public static int getTotalTimeoutSeconds() {
        return Integer.parseInt(getProperty("total_timeout_seconds", "30"));
    }

    public static String getPunishmentMode() {
        return getProperty("punishment_mode", "KICK");
    }

    public static String getDiscordWebhookUrl() {
        return getProperty("webhookUrl", "YOUR_WEBHOOK_URL_HERE");
    }

    public static String getAvatarServiceUrl() {
        return getProperty("avatar_service_url", "https://crafthead.net/cube/{name}");
    }

    public static String getServerName() {
        return getProperty("serverName", "Change Me!");
    }

    public static boolean isEnableAdminPing() {
        return Boolean.parseBoolean(getProperty("enableAdminPing", "false"));
    }

    public static String getAdminRoleId() {
        return getProperty("adminRoleId", "Change Me!");
    }

    public static String getEmbedThumbnailUrl() {
        return getProperty("thumbnailUrl", "Change Me!");
    }

    public static boolean isWebhookOnAction() {
        return Boolean.parseBoolean(getProperty("webhook_on_action", "true"));
    }

    public static String getActionThumbnailUrl() {
        return getProperty("thumbnailUrl", "Change Me!");
    }

    public static boolean isWebhookOnPardon() {
        return Boolean.parseBoolean(getProperty("webhook_on_pardon", "true"));
    }

    public static String getPardonThumbnailUrl() {
        return getProperty("thumbnailUrl", "Change Me!");
    }

    public static boolean isWebhookOnVerify() {
        return Boolean.parseBoolean(getProperty("webhook_on_verify", "true"));
    }

    public static String getVerifyThumbnailUrl() {
        return getProperty("thumbnailUrl", "Change Me!");
    }
}