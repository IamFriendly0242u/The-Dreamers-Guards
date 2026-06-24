package net.thedreamers.guards.config;

import net.fabricmc.loader.api.FabricLoader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public class AntiCheatConfig {

    private static final String MOD_ID = "thedreamers_guards";
    private static final Path MOD_CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("TheDreamers_Guards");
    private static final Path BACKUP_DIR = MOD_CONFIG_DIR.resolve("backups");
    private static final Path CONFIG_PATH = MOD_CONFIG_DIR.resolve("thedreamers_guards.properties");

    private static String currentModVersion = "1.0.0";
    private static String configVersion = "1.0.0";
    private static String punishmentMode = "KICK";
    private static String validationDelayMs = "10000";
    private static String totalTimeoutSeconds = "30";
    private static String countdownStartSeconds = "3";
    private static String suspensionDurationMins = "10";
    private static String appealUrl = "No appeal system linked. Please contact the Server Administration directly.";
    private static String discordWebhookUrl = "YOUR_DISCORD_WEBHOOK_URL_HERE";
    private static String serverName = "The Dreamers Minecraft Server";
    private static String webhookAvatarUrl = "https://assets.mojang.com/3d/assets/main-menu/minecraft-logo.png";
    private static String embedThumbnailUrl = "https://assets.mojang.com/3d/assets/main-menu/minecraft-logo.png";
    private static String avatarServiceUrl = "https://crafthead.net/cube/{name}";
    private static String webhookOnAction = "true";
    private static String webhookOnPardon = "true";
    private static String webhookOnVerify = "true";
    private static String enableAdminPing = "false";
    private static String adminRoleId = "YOUR_ADMIN_ROLE_ID_HERE";
    private static String modBlacklist = "wurst,meteor,liquidbounce,bleachhack,aristois,kami,rusherhack,future,inertia,phobos,salhack,mathax,vector,danielfrominternet,kami-blue,seppuku,coffee,konas,lambda,abyss,w+3,w+2,gopro,earthhack,bleach,pixel,liquid,ares,novoline,flux,rise,tenacity,vape,astolfo,zeroday";

    private static String suspensionPhase1Mins = "20";
    private static String suspensionPhase2Mins = "120";
    private static String suspensionPhase3Mins = "360";
    private static String suspensionPhase4Mins = "-1";

    private static String kickNoModMessage = "§c[The Dreamers Guards]\n§7Grab our mod first if you want to play here, mate!";
    private static String kickCheaterMessage = "§c[The Dreamers Guards]\n§7Nice try, but you got caught: §e%s";
    private static String kickVersionMismatchMessage = "§c[The Dreamers Guards]\n§7Whoops! Your mod version doesn't match the server requirement.\n§7Server: §e%s §7| Yours: §c%s";

    private static String alertAdminMessage = "§d§l[GUARDS ALERT] §e%s §7flagged for §c%s.\\n§7Yo admin, use /guards action to resolve this!";
    private static String countdownBroadcastMessage = "§c§l[WARN] §e%s §cgets dropped in §l%ss!";
    private static String broadcastBanMessage = "§3[BANNED] §e%s §7got caught cheating. See ya never!";
    private static String broadcastKickMessage = "§a[KICKED] §e%s §7was removed from the game. Trash cleared.";

    private static String consoleLogAlert = "[The Dreamers Guards] ALERT: %s detected using illegal modifications (%s).";
    private static String consoleLogFailed = "§c[WARN] FAILED! §7Failed to execute action on %s.";

    public static void loadConfig() {
        currentModVersion = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("1.0.0");

        try {
            if (!Files.exists(MOD_CONFIG_DIR)) {
                Files.createDirectories(MOD_CONFIG_DIR);
            }
            if (!Files.exists(BACKUP_DIR)) {
                Files.createDirectories(BACKUP_DIR);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        if (Files.exists(CONFIG_PATH)) {
            configVersion = "1.0.0";
            try {
                List<String> lines = Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.trim().startsWith("config_version=")) {
                        configVersion = line.split("=", 2)[1].trim();
                        break;
                    }
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }

            int versionComparison = compareVersions(currentModVersion, configVersion);

            if (versionComparison > 0) {
                backupOldConfig();
                generateDefaultConfig();
                return;
            }

            Properties properties = new Properties();
            try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                properties.load(reader);
                punishmentMode = properties.getProperty("punishment_mode", punishmentMode).toUpperCase();
                validationDelayMs = properties.getProperty("validation_delay_ms", validationDelayMs);
                totalTimeoutSeconds = properties.getProperty("total_timeout_seconds", totalTimeoutSeconds);
                countdownStartSeconds = properties.getProperty("countdown_start_seconds", countdownStartSeconds);
                suspensionDurationMins = properties.getProperty("suspension_duration_mins", suspensionDurationMins);
                appealUrl = properties.getProperty("appeal_url", appealUrl);
                discordWebhookUrl = properties.getProperty("discord_webhook_url", discordWebhookUrl);
                webhookAvatarUrl = properties.getProperty("webhook_avatar_url", webhookAvatarUrl);
                embedThumbnailUrl = properties.getProperty("embed_thumbnail_url", embedThumbnailUrl);
                avatarServiceUrl = properties.getProperty("avatar_service_url", avatarServiceUrl);
                serverName = properties.getProperty("server_name", serverName);
                webhookOnAction = properties.getProperty("webhook_on_action", webhookOnAction);
                webhookOnPardon = properties.getProperty("webhook_on_pardon", webhookOnPardon);
                webhookOnVerify = properties.getProperty("webhook_on_verify", webhookOnVerify);
                enableAdminPing = properties.getProperty("enable_admin_ping", enableAdminPing);
                adminRoleId = properties.getProperty("admin_role_id", adminRoleId);
                modBlacklist = properties.getProperty("mod_blacklist", modBlacklist);
                suspensionPhase1Mins = properties.getProperty("suspension_phase_1_mins", suspensionPhase1Mins);
                suspensionPhase2Mins = properties.getProperty("suspension_phase_2_mins", suspensionPhase2Mins);
                suspensionPhase3Mins = properties.getProperty("suspension_phase_3_mins", suspensionPhase3Mins);
                suspensionPhase4Mins = properties.getProperty("suspension_phase_4_mins", suspensionPhase4Mins);
                kickNoModMessage = properties.getProperty("kick_no_mod_message", kickNoModMessage);
                kickCheaterMessage = properties.getProperty("kick_cheater_message", kickCheaterMessage);
                kickVersionMismatchMessage = properties.getProperty("kick_version_mismatch_message", kickVersionMismatchMessage);
                alertAdminMessage = properties.getProperty("alert_admin_message", alertAdminMessage);
                countdownBroadcastMessage = properties.getProperty("countdown_broadcast_message", countdownBroadcastMessage);
                broadcastBanMessage = properties.getProperty("broadcast_ban_message", broadcastBanMessage);
                broadcastKickMessage = properties.getProperty("broadcast_kick_message", broadcastKickMessage);
                consoleLogAlert = properties.getProperty("console_log_alert", consoleLogAlert);
                consoleLogFailed = properties.getProperty("console_log_failed", consoleLogFailed);
            } catch (IOException exception) {
                exception.printStackTrace();
            }

            if (versionComparison < 0) {
                generateDefaultConfig();
            }
        } else {
            generateDefaultConfig();
        }
    }

    private static void generateDefaultConfig() {
        StringBuilder configBuilder = new StringBuilder();
        configBuilder.append("# -----------------------------------------------------------------------\n");
        configBuilder.append("#                  The Dreamers Guards Configuration File                 \n");
        configBuilder.append("# -----------------------------------------------------------------------\n\n");
        configBuilder.append("config_version=").append(currentModVersion).append("\n\n");
        configBuilder.append("punishment_mode=").append(punishmentMode).append("\n\n");
        configBuilder.append("validation_delay_ms=").append(validationDelayMs).append("\n\n");
        configBuilder.append("total_timeout_seconds=").append(totalTimeoutSeconds).append("\n\n");
        configBuilder.append("countdown_start_seconds=").append(countdownStartSeconds).append("\n\n");
        configBuilder.append("suspension_duration_mins=").append(suspensionDurationMins).append("\n\n");
        configBuilder.append("appeal_url=").append(appealUrl).append("\n\n");
        configBuilder.append("discord_webhook_url=").append(discordWebhookUrl).append("\n\n");
        configBuilder.append("webhook_avatar_url=").append(webhookAvatarUrl).append("\n\n");
        configBuilder.append("embed_thumbnail_url=").append(embedThumbnailUrl).append("\n\n");
        configBuilder.append("avatar_service_url=").append(avatarServiceUrl).append("\n\n");
        configBuilder.append("server_name=").append(serverName).append("\n\n");
        configBuilder.append("# --- DISCORD WEBHOOK FEATURE TOGGLES ---\n");
        configBuilder.append("webhook_on_action=").append(webhookOnAction).append("\n");
        configBuilder.append("webhook_on_pardon=").append(webhookOnPardon).append("\n");
        configBuilder.append("webhook_on_verify=").append(webhookOnVerify).append("\n");
        configBuilder.append("enable_admin_ping=").append(enableAdminPing).append("\n");
        configBuilder.append("admin_role_id=").append(adminRoleId).append("\n\n");
        configBuilder.append("mod_blacklist=").append(modBlacklist).append("\n\n");
        configBuilder.append("suspension_phase_1_mins=").append(suspensionPhase1Mins).append("\n");
        configBuilder.append("suspension_phase_2_mins=").append(suspensionPhase2Mins).append("\n");
        configBuilder.append("suspension_phase_3_mins=").append(suspensionPhase3Mins).append("\n");
        configBuilder.append("suspension_phase_4_mins=").append(suspensionPhase4Mins).append("\n\n");
        configBuilder.append("kick_no_mod_message=").append(kickNoModMessage.replace("\n", "\\n")).append("\n\n");
        configBuilder.append("kick_cheater_message=").append(kickCheaterMessage.replace("\n", "\\n")).append("\n\n");
        configBuilder.append("kick_version_mismatch_message=").append(kickVersionMismatchMessage.replace("\n", "\\n")).append("\n\n");
        configBuilder.append("alert_admin_message=").append(alertAdminMessage).append("\n\n");
        configBuilder.append("countdown_broadcast_message=").append(countdownBroadcastMessage).append("\n\n");
        configBuilder.append("broadcast_ban_message=").append(broadcastBanMessage).append("\n\n");
        configBuilder.append("broadcast_kick_message=").append(broadcastKickMessage).append("\n\n");
        configBuilder.append("console_log_alert=").append(consoleLogAlert).append("\n\n");
        configBuilder.append("console_log_failed=").append(consoleLogFailed).append("\n");

        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            writer.write(configBuilder.toString());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static void backupOldConfig() {
        int highest = 0;
        try (Stream<Path> stream = Files.list(BACKUP_DIR)) {
            List<Path> files = stream.toList();
            for (Path p : files) {
                String name = p.getFileName().toString();
                if (name.startsWith("thedreamers_guards_backup") && name.endsWith(".properties")) {
                    try {
                        int num = Integer.parseInt(name.substring(25, name.length() - 11));
                        if (num > highest) {
                            highest = num;
                        }
                    } catch (NumberFormatException exception) {
                    }
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        int nextIndex = highest + 1;
        if (nextIndex > 10) {
            Path toDelete = BACKUP_DIR.resolve("thedreamers_guards_backup" + (nextIndex - 10) + ".properties");
            try {
                Files.deleteIfExists(toDelete);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        Path backupFile = BACKUP_DIR.resolve("thedreamers_guards_backup" + nextIndex + ".properties");
        try {
            Files.move(CONFIG_PATH, backupFile);
            System.out.println("[The Dreamers Guards] Outdated configuration version backed up to: " + backupFile.getFileName());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static int compareVersions(String version1, String version2) {
        String[] cleanV1 = version1.split("-")[0].split("\\.");
        String[] cleanV2 = version2.split("-")[0].split("\\.");
        int length = Math.max(cleanV1.length, cleanV2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < cleanV1.length ? Integer.parseInt(cleanV1[i]) : 0;
            int num2 = i < cleanV2.length ? Integer.parseInt(cleanV2[i]) : 0;
            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }
        return 0;
    }

    public static String getPunishmentMode() { return punishmentMode; }
    public static int getValidationDelayMs() { return Integer.parseInt(validationDelayMs); }
    public static int getTotalTimeoutSeconds() { return Integer.parseInt(totalTimeoutSeconds); }
    public static int getCountdownStartSeconds() { return Integer.parseInt(countdownStartSeconds); }
    public static int getSuspensionDurationMins() { return Integer.parseInt(suspensionDurationMins); }
    public static String getAppealUrl() { return appealUrl; }
    public static String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public static String getWebhookAvatarUrl() { return webhookAvatarUrl; }
    public static String getEmbedThumbnailUrl() { return embedThumbnailUrl; }
    public static String getAvatarServiceUrl() { return avatarServiceUrl; }
    public static String getServerName() { return serverName; }
    public static boolean isWebhookOnAction() { return Boolean.parseBoolean(webhookOnAction); }
    public static boolean isWebhookOnPardon() { return Boolean.parseBoolean(webhookOnPardon); }
    public static boolean isWebhookOnVerify() { return Boolean.parseBoolean(webhookOnVerify); }
    public static boolean isEnableAdminPing() { return Boolean.parseBoolean(enableAdminPing); }
    public static String getAdminRoleId() { return adminRoleId; }
    public static String[] getModBlacklist() { return modBlacklist.split("\\s*,\\s*"); }
    public static int getSuspensionPhase1Mins() { return Integer.parseInt(suspensionPhase1Mins); }
    public static int getSuspensionPhase2Mins() { return Integer.parseInt(suspensionPhase2Mins); }
    public static int getSuspensionPhase3Mins() { return Integer.parseInt(suspensionPhase3Mins); }
    public static int getSuspensionPhase4Mins() { return Integer.parseInt(suspensionPhase4Mins); }
    public static String getKickNoModMessage() { return kickNoModMessage.replace("\\n", "\n"); }
    public static String getKickCheaterMessage() { return kickCheaterMessage.replace("\\n", "\n"); }
    public static String getKickVersionMismatchMessage() { return kickVersionMismatchMessage.replace("\\n", "\n"); }
    public static String getAlertAdminMessage() { return alertAdminMessage.replace("\\n", "\n"); }
    public static String getCountdownBroadcastMessage() { return countdownBroadcastMessage; }
    public static String getBroadcastBanMessage() { return broadcastBanMessage; }
    public static String getBroadcastKickMessage() { return broadcastKickMessage; }
    public static String getConsoleLogAlert() { return consoleLogAlert; }
    public static String getConsoleLogFailed() { return consoleLogFailed; }
}