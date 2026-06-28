package net.thedreamers.guards.webhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.thedreamers.guards.config.AntiCheatConfig;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiscordWebhook {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path BASE_DIR = FabricLoader.getInstance().getConfigDir().resolve("TheDreamers_Guards");
    private static final Path EMBEDS_DIR = BASE_DIR.resolve("embeds");

    private static final Path EMBED_THREAT_FILE = EMBEDS_DIR.resolve("threat_alert.json");
    private static final Path EMBED_ACTION_FILE = EMBEDS_DIR.resolve("action_alert.json");
    private static final Path EMBED_PARDON_FILE = EMBEDS_DIR.resolve("pardon_alert.json");
    private static final Path EMBED_VERIFY_FILE = EMBEDS_DIR.resolve("verify_alert.json");
    private static final Path EMBED_RELOAD_FILE = EMBEDS_DIR.resolve("reload_alert.json");

    public static void init() {
        try {
            if (!Files.exists(EMBEDS_DIR)) {
                Files.createDirectories(EMBEDS_DIR);
            }
            generateDefaults();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateDefaults() throws IOException {
        if (!Files.exists(EMBED_THREAT_FILE)) {
            String json = "{\n" +
                    "  \"username\": \"The Dreamers Guards\",\n" +
                    "  \"avatar_url\": \"DEFAULT\",\n" +
                    "  \"content\": \"Yo, an illegal modification threat has been flagged! Please check this out ASAP!\",\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"ANTI-CHEAT THREAT DETECTION\",\n" +
                    "      \"color\": 16733525,\n" +
                    "      \"author\": {\n" +
                    "        \"name\": \"Target Player: %player_name%\",\n" +
                    "        \"icon_url\": \"%player_avatar%\"\n" +
                    "      },\n" +
                    "      \"thumbnail\": {\n" +
                    "        \"url\": \"Change Me!\"\n" +
                    "      },\n" +
                    "      \"footer\": {\n" +
                    "        \"text\": \"System Integration | %server_name%\"\n" +
                    "      },\n" +
                    "      \"fields\": [\n" +
                    "        { \"name\": \"Target Player:\", \"value\": \"%player_name%\", \"inline\": true },\n" +
                    "        { \"name\": \"Action Taken:\", \"value\": \"%action_type%\", \"inline\": true },\n" +
                    "        { \"name\": \"Infraction Stage:\", \"value\": \"%phase_text%\", \"inline\": false },\n" +
                    "        { \"name\": \"Violation Reason:\", \"value\": \"%violation_reason%\", \"inline\": false },\n" +
                    "        { \"name\": \"Player UUID:\", \"value\": \"%player_uuid%\", \"inline\": false },\n" +
                    "        { \"name\": \"Network IP Address:\", \"value\": \"%player_ip%\", \"inline\": false }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_THREAT_FILE, StandardCharsets.UTF_8)) {
                writer.write(json);
            }
        }

        if (!Files.exists(EMBED_ACTION_FILE)) {
            String json = "{\n" +
                    "  \"username\": \"The Dreamers Guards\",\n" +
                    "  \"avatar_url\": \"DEFAULT\",\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"ADMIN COMMAND EXECUTION\",\n" +
                    "      \"description\": \"Admin %admin_name% executed forced punishment action on target player.\",\n" +
                    "      \"color\": 16755200,\n" +
                    "      \"thumbnail\": {\n" +
                    "        \"url\": \"Change Me!\"\n" +
                    "      },\n" +
                    "      \"footer\": {\n" +
                    "        \"text\": \"Admin Enforcement | %server_name%\"\n" +
                    "      },\n" +
                    "      \"fields\": [\n" +
                    "        { \"name\": \"Administrator:\", \"value\": \"%admin_name%\", \"inline\": true },\n" +
                    "        { \"name\": \"Target Player:\", \"value\": \"%player_name%\", \"inline\": true },\n" +
                    "        { \"name\": \"Action Type:\", \"value\": \"%action_type%\", \"inline\": false },\n" +
                    "        { \"name\": \"Enforcement Reason:\", \"value\": \"%reason%\", \"inline\": false }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_ACTION_FILE, StandardCharsets.UTF_8)) {
                writer.write(json);
            }
        }

        if (!Files.exists(EMBED_PARDON_FILE)) {
            String json = "{\n" +
                    "  \"username\": \"The Dreamers Guards\",\n" +
                    "  \"avatar_url\": \"DEFAULT\",\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"ADMIN PARDON EXECUTION\",\n" +
                    "      \"description\": \"Admin %admin_name% pardoned and removed target player from suspension list.\",\n" +
                    "      \"color\": 5635925,\n" +
                    "      \"thumbnail\": {\n" +
                    "        \"url\": \"Change Me!\"\n" +
                    "      },\n" +
                    "      \"footer\": {\n" +
                    "        \"text\": \"Pardon Resolution | %server_name%\"\n" +
                    "      },\n" +
                    "      \"fields\": [\n" +
                    "        { \"name\": \"Administrator:\", \"value\": \"%admin_name%\", \"inline\": true },\n" +
                    "        { \"name\": \"Target Player:\", \"value\": \"%player_name%\", \"inline\": true },\n" +
                    "        { \"name\": \"Pardon Mode:\", \"value\": \"%pardon_mode%\", \"inline\": false }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_PARDON_FILE, StandardCharsets.UTF_8)) {
                writer.write(json);
            }
        }

        if (!Files.exists(EMBED_VERIFY_FILE)) {
            String json = "{\n" +
                    "  \"username\": \"The Dreamers Guards\",\n" +
                    "  \"avatar_url\": \"DEFAULT\",\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"PLAYER VERIFICATION MONITOR\",\n" +
                    "      \"description\": \"Join network authentication scanner processed entry details.\",\n" +
                    "      \"color\": 5592575,\n" +
                    "      \"author\": {\n" +
                    "        \"name\": \"Target Player: %player_name%\",\n" +
                    "        \"icon_url\": \"%player_avatar%\"\n" +
                    "      },\n" +
                    "      \"thumbnail\": {\n" +
                    "        \"url\": \"Change Me!\"\n" +
                    "      },\n" +
                    "      \"footer\": {\n" +
                    "        \"text\": \"Gate Verification | %server_name%\"\n" +
                    "      },\n" +
                    "      \"fields\": [\n" +
                    "        { \"name\": \"Player Name:\", \"value\": \"%player_name%\", \"inline\": true },\n" +
                    "        { \"name\": \"Player UUID:\", \"value\": \"%player_uuid%\", \"inline\": true },\n" +
                    "        { \"name\": \"Result Status:\", \"value\": \"%violation_reason%\", \"inline\": false },\n" +
                    "        { \"name\": \"Detailed Notes:\", \"value\": \"%details%\", \"inline\": false }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_VERIFY_FILE, StandardCharsets.UTF_8)) {
                writer.write(json);
            }
        }

        if (!Files.exists(EMBED_RELOAD_FILE)) {
            String json = "{\n" +
                    "  \"username\": \"The Dreamers Guards\",\n" +
                    "  \"avatar_url\": \"DEFAULT\",\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"SYSTEM CONFIGURATION RELOAD\",\n" +
                    "      \"description\": \"Admin %admin_name% reloaded the anti-cheat core security configuration variables.\",\n" +
                    "      \"color\": 5636095,\n" +
                    "      \"thumbnail\": {\n" +
                    "        \"url\": \"Change Me!\"\n" +
                    "      },\n" +
                    "      \"footer\": {\n" +
                    "        \"text\": \"System Maintenance | %server_name%\"\n" +
                    "      },\n" +
                    "      \"fields\": [\n" +
                    "        { \"name\": \"Administrator:\", \"value\": \"%admin_name%\", \"inline\": true }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_RELOAD_FILE, StandardCharsets.UTF_8)) {
                writer.write(json);
            }
        }
    }

    private static void executePipelineSend(String payload) {
        String urlSetting = AntiCheatConfig.getDiscordWebhookUrl();
        if (urlSetting == null || urlSetting.trim().isEmpty() || "Change Me!".equals(urlSetting)) {
            return;
        }
        String[] urls = urlSetting.split("\\s*,\\s*");
        for (String url : urls) {
            if (url.trim().isEmpty()) continue;
            net.thedreamers.lib.webhook.DiscordWebhook coreEngine = new net.thedreamers.lib.webhook.DiscordWebhook(url.trim());
            coreEngine.send(payload);
        }
    }

    private static String readTemplate(Path path) {
        StringBuilder sb = new StringBuilder();
        if (!Files.exists(path)) return "";
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private static String sanitizePayload(String payload) {
        if (payload.contains("\"url\": \"Change Me!\"")) {
            payload = payload.replace("\"thumbnail\": {\n        \"url\": \"Change Me!\"\n      },", "")
                    .replace("\"thumbnail\": {\n        \"url\": \"Change Me!\"\n      }", "")
                    .replace("\"thumbnail\": {\n      \"url\": \"Change Me!\"\n    },", "")
                    .replace("\"thumbnail\": {\n      \"url\": \"Change Me!\"\n    }", "")
                    .replace("\"thumbnail\": {\"url\": \"Change Me!\"},", "")
                    .replace("\"thumbnail\": {\"url\": \"Change Me!\"}", "");
        }
        if (payload.contains("\"avatar_url\": \"DEFAULT\"")) {
            payload = payload.replace("\"avatar_url\": \"DEFAULT\",", "").replace("\"avatar_url\": \"DEFAULT\"", "");
        }
        return payload;
    }

    public static void sendAlert(MinecraftServer server, String username, String uuid, String ip, String reason, int phase, String action) {
        String template = readTemplate(EMBED_THREAT_FILE);
        if (template.isEmpty()) return;
        String stageText = phase >= 4 ? AntiCheatConfig.getLanguage("webhook.stage.terminated", "PHASE 4 (TERMINATED)") : String.format(AntiCheatConfig.getLanguage("webhook.stage.warn", "PHASE %s / 3"), phase);
        String playerAvatarUrl = AntiCheatConfig.getAvatarServiceUrl().replace("{uuid}", uuid).replace("{name}", username);
        String currentServerName = AntiCheatConfig.getServerName();
        String payload = template.replace("%player_name%", username)
                .replace("%player_uuid%", uuid)
                .replace("%player_avatar%", playerAvatarUrl)
                .replace("%action_type%", action)
                .replace("%phase_text%", stageText)
                .replace("%violation_reason%", reason)
                .replace("%player_ip%", ip)
                .replace("%server_name%", currentServerName);
        payload = sanitizePayload(payload);
        if (AntiCheatConfig.isEnableAdminPing() && !AntiCheatConfig.getAdminRoleId().equals("Change Me!") && !AntiCheatConfig.getAdminRoleId().trim().isEmpty()) {
            payload = payload.replace("\"content\": \"Yo,", "\"content\": \"Yo <@&" + AntiCheatConfig.getAdminRoleId().trim() + ">,");
        }
        executePipelineSend(payload);
    }

    public static void sendActionAlert(MinecraftServer server, String adminName, String targetName, String actionType, String reason) {
        String template = readTemplate(EMBED_ACTION_FILE);
        if (template.isEmpty()) return;
        String currentServerName = AntiCheatConfig.getServerName();
        String payload = template.replace("%admin_name%", adminName)
                .replace("%player_name%", targetName)
                .replace("%action_type%", actionType)
                .replace("%reason%", reason)
                .replace("%server_name%", currentServerName);
        payload = sanitizePayload(payload);
        executePipelineSend(payload);
    }

    public static void sendPardonAlert(MinecraftServer server, String adminName, String targetName, String mode) {
        String template = readTemplate(EMBED_PARDON_FILE);
        if (template.isEmpty()) return;
        String currentServerName = AntiCheatConfig.getServerName();
        String payload = template.replace("%admin_name%", adminName)
                .replace("%player_name%", targetName)
                .replace("%pardon_mode%", mode)
                .replace("%server_name%", currentServerName);
        payload = sanitizePayload(payload);
        executePipelineSend(payload);
    }

    public static void sendVerifyAlert(MinecraftServer server, String playerName, String playerUuid, String status, String details) {
        String template = readTemplate(EMBED_VERIFY_FILE);
        if (template.isEmpty()) return;
        String currentServerName = AntiCheatConfig.getServerName();
        String playerAvatarUrl = AntiCheatConfig.getAvatarServiceUrl().replace("{uuid}", playerUuid).replace("{name}", playerName);
        String payload = template.replace("%player_name%", playerName)
                .replace("%player_uuid%", playerUuid)
                .replace("%player_avatar%", playerAvatarUrl)
                .replace("%violation_reason%", status)
                .replace("%details%", details)
                .replace("%server_name%", currentServerName);
        payload = sanitizePayload(payload);
        executePipelineSend(payload);
    }

    public static void sendReloadAlert(MinecraftServer server, String adminName) {
        String template = readTemplate(EMBED_RELOAD_FILE);
        if (template.isEmpty()) return;
        String currentServerName = AntiCheatConfig.getServerName();
        String payload = template.replace("%admin_name%", adminName)
                .replace("%server_name%", currentServerName);
        payload = sanitizePayload(payload);
        executePipelineSend(payload);
    }
}