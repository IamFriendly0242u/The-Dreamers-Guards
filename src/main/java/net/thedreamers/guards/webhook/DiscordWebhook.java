package net.thedreamers.guards.webhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.thedreamers.guards.config.AntiCheatConfig;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path BASE_DIR = FabricLoader.getInstance().getConfigDir().resolve("TheDreamers_Guards");
    private static final Path EMBEDS_DIR = BASE_DIR.resolve("embeds");
    private static final Path LANG_DIR = BASE_DIR.resolve("language");

    private static final Path EMBED_THREAT_FILE = EMBEDS_DIR.resolve("threat_alert.json");
    private static final Path EMBED_ACTION_FILE = EMBEDS_DIR.resolve("action_alert.json");
    private static final Path EMBED_PARDON_FILE = EMBEDS_DIR.resolve("pardon_alert.json");
    private static final Path EMBED_VERIFY_FILE = EMBEDS_DIR.resolve("verify_alert.json");
    private static final Path EMBED_RELOAD_FILE = EMBEDS_DIR.resolve("reload_alert.json");
    private static final Path EMBED_DEFAULT_FILE = EMBEDS_DIR.resolve("default.json");
    private static final Path LANG_FILE = LANG_DIR.resolve("en_us.json");

    private static final Map<String, String> TRANSLATIONS = new HashMap<>();

    public static void init() {
        try {
            if (!Files.exists(EMBEDS_DIR)) Files.createDirectories(EMBEDS_DIR);
            if (!Files.exists(LANG_DIR)) Files.createDirectories(LANG_DIR);
            generateDefaults();
            loadLanguage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateDefaults() throws IOException {
        if (!Files.exists(LANG_FILE)) {
            JsonObject lang = new JsonObject();
            lang.addProperty("webhook.embed.title", "ANTI-CHEAT THREAT DETECTION");
            lang.addProperty("webhook.embed.footer", "System Integration");
            lang.addProperty("webhook.stage.terminated", "PHASE 4 (TERMINATED)");
            lang.addProperty("webhook.stage.warn", "PHASE %s / 3");
            lang.addProperty("webhook.field.player", "Target Player");
            lang.addProperty("webhook.field.action", "Action Taken");
            lang.addProperty("webhook.field.stage", "Infraction Stage");
            lang.addProperty("webhook.field.reason", "Violation Reason");
            lang.addProperty("webhook.field.uuid", "Player UUID");
            lang.addProperty("webhook.field.ip", "Network IP Address");
            try (BufferedWriter writer = Files.newBufferedWriter(LANG_FILE, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(lang));
            }
        }

        if (!Files.exists(EMBED_THREAT_FILE)) {
            JsonObject embed = new JsonObject();
            embed.addProperty("color", "#FF5555");
            embed.addProperty("title", "%embed_title%");
            embed.addProperty("description", "");

            JsonObject author = new JsonObject();
            author.addProperty("name", "%player_name% has been flagged");
            author.addProperty("icon_url", "%player_avatar%");
            embed.add("author", author);

            JsonObject thumbnail = new JsonObject();
            thumbnail.addProperty("url", "%embed_thumbnail%");
            embed.add("thumbnail", thumbnail);

            JsonArray fields = new JsonArray();
            fields.add(createFieldTemplate("%field_player%", "%player%", true));
            fields.add(createFieldTemplate("%field_action%", "%action%", true));
            fields.add(createFieldTemplate("%field_stage%", "%stage%", false));
            fields.add(createFieldTemplate("%field_reason%", "%reason%", false));
            fields.add(createFieldTemplate("%field_uuid%", "%uuid%", false));
            fields.add(createFieldTemplate("%field_ip%", "%ip%", false));
            embed.add("fields", fields);

            JsonObject footer = new JsonObject();
            footer.addProperty("text", "%embed_footer% | %server_name%");
            embed.add("footer", footer);

            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_THREAT_FILE, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(embed));
            }
        }

        if (!Files.exists(EMBED_ACTION_FILE)) {
            JsonObject embed = new JsonObject();
            embed.addProperty("color", "#FFAA00");
            embed.addProperty("title", "ADMIN COMMAND EXECUTION");
            embed.addProperty("description", "Admin %admin_name% executed forced punishment action on target player.");

            JsonObject thumbnail = new JsonObject();
            thumbnail.addProperty("url", "%action_thumbnail%");
            embed.add("thumbnail", thumbnail);

            JsonArray fields = new JsonArray();
            fields.add(createFieldTemplate("Administrator", "%admin_name%", true));
            fields.add(createFieldTemplate("Target Player", "%target_name%", true));
            fields.add(createFieldTemplate("Action Type", "%action_type%", false));
            embed.add("fields", fields);

            JsonObject footer = new JsonObject();
            footer.addProperty("text", "Admin Enforcement | %server_name%");
            embed.add("footer", footer);

            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_ACTION_FILE, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(embed));
            }
        }

        if (!Files.exists(EMBED_PARDON_FILE)) {
            JsonObject embed = new JsonObject();
            embed.addProperty("color", "#55FF55");
            embed.addProperty("title", "ADMIN PARDON EXECUTION");
            embed.addProperty("description", "Admin %admin_name% pardoned and removed target player from suspension list.");

            JsonObject thumbnail = new JsonObject();
            thumbnail.addProperty("url", "%pardon_thumbnail%");
            embed.add("thumbnail", thumbnail);

            JsonArray fields = new JsonArray();
            fields.add(createFieldTemplate("Administrator", "%admin_name%", true));
            fields.add(createFieldTemplate("Target Player", "%target_name%", true));
            embed.add("fields", fields);

            JsonObject footer = new JsonObject();
            footer.addProperty("text", "Pardon Resolution | %server_name%");
            embed.add("footer", footer);

            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_PARDON_FILE, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(embed));
            }
        }

        if (!Files.exists(EMBED_VERIFY_FILE)) {
            JsonObject embed = new JsonObject();
            embed.addProperty("color", "#5555FF");
            embed.addProperty("title", "PLAYER VERIFICATION MONITOR");
            embed.addProperty("description", "Join network authentication scanner processed entry details.");

            JsonObject author = new JsonObject();
            author.addProperty("name", "Verification Processing: %player_name%");
            author.addProperty("icon_url", "%player_avatar%");
            embed.add("author", author);

            JsonObject thumbnail = new JsonObject();
            thumbnail.addProperty("url", "%verify_thumbnail%");
            embed.add("thumbnail", thumbnail);

            JsonArray fields = new JsonArray();
            fields.add(createFieldTemplate("Player Name", "%player_name%", true));
            fields.add(createFieldTemplate("Result Status", "%verify_status%", true));
            fields.add(createFieldTemplate("Details", "%verify_details%", false));
            embed.add("fields", fields);

            JsonObject footer = new JsonObject();
            footer.addProperty("text", "Gate Verification | %server_name%");
            embed.add("footer", footer);

            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_VERIFY_FILE, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(embed));
            }
        }

        if (!Files.exists(EMBED_RELOAD_FILE)) {
            JsonObject embed = new JsonObject();
            embed.addProperty("color", "#55FFFF");
            embed.addProperty("title", "SYSTEM CONFIGURATION RELOAD");
            embed.addProperty("description", "Admin %admin_name% reloaded the anti-cheat core security configuration variables.");

            JsonObject thumbnail = new JsonObject();
            thumbnail.addProperty("url", "%reload_thumbnail%");
            embed.add("thumbnail", thumbnail);

            JsonArray fields = new JsonArray();
            fields.add(createFieldTemplate("Administrator", "%admin_name%", true));
            embed.add("fields", fields);

            JsonObject footer = new JsonObject();
            footer.addProperty("text", "System Maintenance | %server_name%");
            embed.add("footer", footer);

            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_RELOAD_FILE, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(embed));
            }
        }

        if (!Files.exists(EMBED_DEFAULT_FILE)) {
            JsonObject guide = new JsonObject();
            guide.addProperty("documentation", "The Dreamers Guards Embed Customization Guide");
            guide.addProperty("notice", "Administrators can customize thumbnails and layouts directly inside these JSON files instead of the properties configuration");
            JsonObject variables = new JsonObject();
            variables.addProperty("%player_name%", "Displays the flagged or verifying player name");
            variables.addProperty("%player_avatar%", "Fetches the 3D cube render of the player head");
            variables.addProperty("%embed_thumbnail%", "Uses the static threat alert thumbnail link");
            variables.addProperty("%action_thumbnail%", "Uses the static admin enforcement action thumbnail link");
            variables.addProperty("%pardon_thumbnail%", "Uses the static player pardon resolution thumbnail link");
            variables.addProperty("%verify_thumbnail%", "Uses the static secure gateway verification thumbnail link");
            variables.addProperty("%reload_thumbnail%", "Uses the static system maintenance reload thumbnail link");
            variables.addProperty("%admin_name%", "Displays the name of the executing administrator");
            variables.addProperty("%action_type%", "Shows the punishment type applied such as KICK or BAN");
            guide.add("available_placeholders", variables);
            try (BufferedWriter writer = Files.newBufferedWriter(EMBED_DEFAULT_FILE, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(guide));
            }
        }
    }

    private static JsonObject createFieldTemplate(String name, String value, boolean inline) {
        JsonObject f = new JsonObject();
        f.addProperty("name", name);
        f.addProperty("value", value);
        f.addProperty("inline", inline);
        return f;
    }

    private static void loadLanguage() {
        TRANSLATIONS.clear();
        try (BufferedReader reader = Files.newBufferedReader(LANG_FILE, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (String key : json.keySet()) {
                TRANSLATIONS.put(key, json.get(key).getAsString());
            }
        } catch (Exception e) {
            backupFile(LANG_FILE);
        }
    }

    private static void backupFile(Path file) {
        try {
            Path backup = file.resolveSibling(file.getFileName().toString() + ".old");
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getLang(String key) {
        return TRANSLATIONS.getOrDefault(key, key);
    }

    private static void broadcastPayload(JsonObject payload) {
        String urlSetting = AntiCheatConfig.getDiscordWebhookUrl();
        if (urlSetting == null || urlSetting.trim().isEmpty() || urlSetting.equals("YOUR_DISCORD_WEBHOOK_URL_HERE")) {
            return;
        }
        String[] urls = urlSetting.split("\\s*,\\s*");
        for (String url : urls) {
            String targetUrl = url.trim();
            if (targetUrl.isEmpty()) continue;
            CompletableFuture.runAsync(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(targetUrl))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                            .build();
                    HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() >= 400) {
                        System.err.println("[The Dreamers Guards] Discord Webhook returned error status: " + response.statusCode() + " - " + response.body());
                    } else {
                        System.out.println("[The Dreamers Guards] Discord Webhook message broadcasted successfully!");
                    }
                } catch (Exception e) {
                    System.err.println("[The Dreamers Guards] Exception occurred while sending HTTP payload to Discord Webhook:");
                    e.printStackTrace();
                }
            });
        }
    }

    public static void sendAlert(MinecraftServer server, String username, String uuid, String ip, String reason, int phase, String action) {
        CompletableFuture.runAsync(() -> {
            try {
                loadLanguage();
                JsonObject template;
                try (BufferedReader reader = Files.newBufferedReader(EMBED_THREAT_FILE, StandardCharsets.UTF_8)) {
                    template = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (Exception e) {
                    backupFile(EMBED_THREAT_FILE);
                    return;
                }

                String stageText = phase >= 4 ? getLang("webhook.stage.terminated") : String.format(getLang("webhook.stage.warn"), phase);
                String playerAvatarUrl = AntiCheatConfig.getAvatarServiceUrl().replace("{uuid}", uuid).replace("{name}", username);
                String currentServerName = AntiCheatConfig.getServerName();

                String title = template.get("title").getAsString().replace("%embed_title%", getLang("webhook.embed.title"));
                String description = template.has("description") ? template.get("description").getAsString() : "";

                int rawColor = 16755200;
                if (template.has("color")) {
                    rawColor = Integer.parseInt(template.get("color").getAsString().replace("#", ""), 16);
                }

                JsonArray processedFields = new JsonArray();
                if (template.has("fields")) {
                    JsonArray fields = template.getAsJsonArray("fields");
                    for (int i = 0; i < fields.size(); i++) {
                        JsonObject f = fields.get(i).getAsJsonObject();
                        String fName = f.get("name").getAsString()
                                .replace("%field_player%", getLang("webhook.field.player"))
                                .replace("%field_action%", getLang("webhook.field.action"))
                                .replace("%field_stage%", getLang("webhook.field.stage"))
                                .replace("%field_reason%", getLang("webhook.field.reason"))
                                .replace("%field_uuid%", getLang("webhook.field.uuid"))
                                .replace("%field_ip%", getLang("webhook.field.ip"));

                        String fValue = f.get("value").getAsString()
                                .replace("%player%", username)
                                .replace("%action%", action)
                                .replace("%stage%", stageText)
                                .replace("%reason%", reason)
                                .replace("%uuid%", uuid)
                                .replace("%ip%", ip);

                        JsonObject newField = new JsonObject();
                        newField.addProperty("name", fName);
                        newField.addProperty("value", fValue);
                        newField.addProperty("inline", f.get("inline").getAsBoolean());
                        processedFields.add(newField);
                    }
                }

                JsonObject payload = new JsonObject();
                payload.addProperty("username", "The Dreamers Guards");

                if (AntiCheatConfig.isEnableAdminPing() && !AntiCheatConfig.getAdminRoleId().equals("YOUR_ADMIN_ROLE_ID_HERE") && !AntiCheatConfig.getAdminRoleId().trim().isEmpty()) {
                    payload.addProperty("content", "Yo <@&" + AntiCheatConfig.getAdminRoleId().trim() + ">, an illegal modification threat has been flagged! Please check this out ASAP!");
                }

                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();
                embed.addProperty("title", title);
                if (!description.isEmpty()) embed.addProperty("description", description);
                embed.addProperty("color", rawColor);
                embed.addProperty("timestamp", Instant.now().toString());

                if (template.has("author")) {
                    JsonObject authTemplate = template.getAsJsonObject("author");
                    JsonObject author = new JsonObject();
                    author.addProperty("name", authTemplate.get("name").getAsString().replace("%player_name%", username));
                    author.addProperty("icon_url", authTemplate.get("icon_url").getAsString().replace("%player_avatar%", playerAvatarUrl));
                    embed.add("author", author);
                }

                if (template.has("thumbnail")) {
                    JsonObject thumbTemplate = template.getAsJsonObject("thumbnail");
                    JsonObject thumbnail = new JsonObject();
                    thumbnail.addProperty("url", thumbTemplate.get("url").getAsString().replace("%embed_thumbnail%", AntiCheatConfig.getEmbedThumbnailUrl()));
                    embed.add("thumbnail", thumbnail);
                }

                embed.add("fields", processedFields);

                if (template.has("footer")) {
                    JsonObject fObj = template.getAsJsonObject("footer");
                    JsonObject footer = new JsonObject();
                    footer.addProperty("text", fObj.get("text").getAsString()
                            .replace("%embed_footer%", getLang("webhook.embed.footer"))
                            .replace("%server_name%", currentServerName));
                    embed.add("footer", footer);
                }

                embeds.add(embed);
                payload.add("embeds", embeds);
                broadcastPayload(payload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendActionAlert(MinecraftServer server, String adminName, String targetName, String actionType) {
        if (!AntiCheatConfig.isWebhookOnAction()) return;
        try {
            JsonObject template;
            try (BufferedReader reader = Files.newBufferedReader(EMBED_ACTION_FILE, StandardCharsets.UTF_8)) {
                template = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                backupFile(EMBED_ACTION_FILE);
                return;
            }

            int rawColor = 16755200;
            if (template.has("color")) {
                rawColor = Integer.parseInt(template.get("color").getAsString().replace("#", ""), 16);
            }
            String currentServerName = AntiCheatConfig.getServerName();

            JsonArray processedFields = new JsonArray();
            JsonArray fields = template.getAsJsonArray("fields");
            for (int i = 0; i < fields.size(); i++) {
                JsonObject f = fields.get(i).getAsJsonObject();
                JsonObject newField = new JsonObject();
                newField.addProperty("name", f.get("name").getAsString());
                newField.addProperty("value", f.get("value").getAsString()
                        .replace("%admin_name%", adminName)
                        .replace("%target_name%", targetName)
                        .replace("%action_type%", actionType));
                newField.addProperty("inline", f.get("inline").getAsBoolean());
                processedFields.add(newField);
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("username", "The Dreamers Guards");

            JsonArray embeds = new JsonArray();
            JsonObject embed = new JsonObject();
            embed.addProperty("title", template.get("title").getAsString());
            embed.addProperty("description", template.get("description").getAsString()
                    .replace("%admin_name%", adminName)
                    .replace("%action_type%", actionType)
                    .replace("%target_name%", targetName));
            embed.addProperty("color", rawColor);
            embed.addProperty("timestamp", Instant.now().toString());
            embed.add("fields", processedFields);

            if (template.has("thumbnail")) {
                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", template.getAsJsonObject("thumbnail").get("url").getAsString().replace("%action_thumbnail%", AntiCheatConfig.getActionThumbnailUrl()));
                embed.add("thumbnail", thumbnail);
            }

            if (template.has("footer")) {
                JsonObject fObj = template.getAsJsonObject("footer");
                JsonObject footer = new JsonObject();
                footer.addProperty("text", fObj.get("text").getAsString().replace("%server_name%", currentServerName));
                embed.add("footer", footer);
            }

            embeds.add(embed);
            payload.add("embeds", embeds);
            broadcastPayload(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendPardonAlert(MinecraftServer server, String adminName, String targetName) {
        if (!AntiCheatConfig.isWebhookOnPardon()) return;
        try {
            JsonObject template;
            try (BufferedReader reader = Files.newBufferedReader(EMBED_PARDON_FILE, StandardCharsets.UTF_8)) {
                template = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                backupFile(EMBED_PARDON_FILE);
                return;
            }

            int rawColor = 16755200;
            if (template.has("color")) {
                rawColor = Integer.parseInt(template.get("color").getAsString().replace("#", ""), 16);
            }
            String currentServerName = AntiCheatConfig.getServerName();

            JsonArray processedFields = new JsonArray();
            JsonArray fields = template.getAsJsonArray("fields");
            for (int i = 0; i < fields.size(); i++) {
                JsonObject f = fields.get(i).getAsJsonObject();
                JsonObject newField = new JsonObject();
                newField.addProperty("name", f.get("name").getAsString());
                newField.addProperty("value", f.get("value").getAsString()
                        .replace("%admin_name%", adminName)
                        .replace("%target_name%", targetName));
                newField.addProperty("inline", f.get("inline").getAsBoolean());
                processedFields.add(newField);
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("username", "The Dreamers Guards");

            JsonArray embeds = new JsonArray();
            JsonObject embed = new JsonObject();
            embed.addProperty("title", template.get("title").getAsString());
            embed.addProperty("description", template.get("description").getAsString()
                    .replace("%admin_name%", adminName)
                    .replace("%target_name%", targetName));
            embed.addProperty("color", rawColor);
            embed.addProperty("timestamp", Instant.now().toString());
            embed.add("fields", processedFields);

            if (template.has("thumbnail")) {
                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", template.getAsJsonObject("thumbnail").get("url").getAsString().replace("%pardon_thumbnail%", AntiCheatConfig.getPardonThumbnailUrl()));
                embed.add("thumbnail", thumbnail);
            }

            if (template.has("footer")) {
                JsonObject fObj = template.getAsJsonObject("footer");
                JsonObject footer = new JsonObject();
                footer.addProperty("text", fObj.get("text").getAsString().replace("%server_name%", currentServerName));
                embed.add("footer", footer);
            }

            embeds.add(embed);
            payload.add("embeds", embeds);
            broadcastPayload(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendVerifyAlert(MinecraftServer server, String playerName, String playerUuid, String status, String details) {
        if (!AntiCheatConfig.isWebhookOnVerify()) return;
        try {
            JsonObject template;
            try (BufferedReader reader = Files.newBufferedReader(EMBED_VERIFY_FILE, StandardCharsets.UTF_8)) {
                template = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                backupFile(EMBED_VERIFY_FILE);
                return;
            }

            int rawColor = 16755200;
            if (template.has("color")) {
                rawColor = Integer.parseInt(template.get("color").getAsString().replace("#", ""), 16);
            }
            String currentServerName = AntiCheatConfig.getServerName();
            String playerAvatarUrl = AntiCheatConfig.getAvatarServiceUrl().replace("{uuid}", playerUuid).replace("{name}", playerName);

            JsonArray processedFields = new JsonArray();
            JsonArray fields = template.getAsJsonArray("fields");
            for (int i = 0; i < fields.size(); i++) {
                JsonObject f = fields.get(i).getAsJsonObject();
                JsonObject newField = new JsonObject();
                newField.addProperty("name", f.get("name").getAsString());
                newField.addProperty("value", f.get("value").getAsString()
                        .replace("%player_name%", playerName)
                        .replace("%verify_status%", status)
                        .replace("%verify_details%", details));
                newField.addProperty("inline", f.get("inline").getAsBoolean());
                processedFields.add(newField);
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("username", "The Dreamers Guards");

            JsonArray embeds = new JsonArray();
            JsonObject embed = new JsonObject();
            embed.addProperty("title", template.get("title").getAsString());
            embed.addProperty("description", template.get("description").getAsString());
            embed.addProperty("color", rawColor);
            embed.addProperty("timestamp", Instant.now().toString());
            embed.add("fields", processedFields);

            JsonObject author = new JsonObject();
            author.addProperty("name", template.getAsJsonObject("author").get("name").getAsString().replace("%player_name%", playerName));
            author.addProperty("icon_url", template.getAsJsonObject("author").get("icon_url").getAsString().replace("%player_avatar%", playerAvatarUrl));
            embed.add("author", author);

            if (template.has("thumbnail")) {
                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", template.getAsJsonObject("thumbnail").get("url").getAsString().replace("%verify_thumbnail%", AntiCheatConfig.getVerifyThumbnailUrl()));
                embed.add("thumbnail", thumbnail);
            }

            if (template.has("footer")) {
                JsonObject fObj = template.getAsJsonObject("footer");
                JsonObject footer = new JsonObject();
                footer.addProperty("text", fObj.get("text").getAsString().replace("%server_name%", currentServerName));
                embed.add("footer", footer);
            }

            embeds.add(embed);
            payload.add("embeds", embeds);
            broadcastPayload(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendReloadAlert(MinecraftServer server, String adminName) {
        try {
            JsonObject template;
            try (BufferedReader reader = Files.newBufferedReader(EMBED_RELOAD_FILE, StandardCharsets.UTF_8)) {
                template = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                backupFile(EMBED_RELOAD_FILE);
                return;
            }

            int rawColor = 16755200;
            if (template.has("color")) {
                rawColor = Integer.parseInt(template.get("color").getAsString().replace("#", ""), 16);
            }
            String currentServerName = AntiCheatConfig.getServerName();

            JsonArray processedFields = new JsonArray();
            JsonArray fields = template.getAsJsonArray("fields");
            for (int i = 0; i < fields.size(); i++) {
                JsonObject f = fields.get(i).getAsJsonObject();
                JsonObject newField = new JsonObject();
                newField.addProperty("name", f.get("name").getAsString());
                newField.addProperty("value", f.get("value").getAsString().replace("%admin_name%", adminName));
                newField.addProperty("inline", f.get("inline").getAsBoolean());
                processedFields.add(newField);
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("username", "The Dreamers Guards");

            JsonArray embeds = new JsonArray();
            JsonObject embed = new JsonObject();
            embed.addProperty("title", template.get("title").getAsString());
            embed.addProperty("description", template.get("description").getAsString().replace("%admin_name%", adminName));
            embed.addProperty("color", rawColor);
            embed.addProperty("timestamp", Instant.now().toString());
            embed.add("fields", processedFields);

            if (template.has("thumbnail")) {
                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", template.getAsJsonObject("thumbnail").get("url").getAsString().replace("%reload_thumbnail%", AntiCheatConfig.getReloadThumbnailUrl()));
                embed.add("thumbnail", thumbnail);
            }

            if (template.has("footer")) {
                JsonObject fObj = template.getAsJsonObject("footer");
                JsonObject footer = new JsonObject();
                footer.addProperty("text", fObj.get("text").getAsString().replace("%server_name%", currentServerName));
                embed.add("footer", footer);
            }

            embeds.add(embed);
            payload.add("embeds", embeds);
            broadcastPayload(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}