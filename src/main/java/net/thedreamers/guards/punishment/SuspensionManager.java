package net.thedreamers.guards.punishment;

import net.thedreamers.lib.punishment.SuspensionEngine;
import net.minecraft.network.chat.Component;
import net.thedreamers.guards.Thedreamers_guards;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SuspensionManager {

    private static SuspensionEngine engine;
    private static final File FILE = new File("config/TheDreamers_Guards/suspended-list.txt");

    public static void load() {
        engine = new SuspensionEngine(FILE);
        engine.load();
    }

    public static void suspend(String username, String uuid, String ip) {
        engine.suspend(username, 4, -1, "Repeated or Critical Anti-Cheat Infractions.");
        engine.suspend(uuid, 4, -1, "Repeated or Critical Anti-Cheat Infractions.");
        engine.suspend(ip, 4, -1, "Repeated or Critical Anti-Cheat Infractions.");
    }

    public static void pardon(String target) {
        engine.pardon(target);
    }

    public static boolean isSuspended(String username, String uuid, String ip) {
        return engine.isSuspended(username, uuid, ip);
    }

    public static Component getSuspensionComponent(String username, String uuid, String ip) {
        Properties props = new Properties();
        if (FILE.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String data = props.getProperty(username.toLowerCase());
        if (data == null) data = props.getProperty(uuid.toLowerCase());
        if (data == null) data = props.getProperty(ip.toLowerCase());
        if (data == null) {
            return Component.literal("§c[The Dreamers Guards]\n§7Connection terminated by security firewall registry configuration.");
        }
        String[] parts = data.split(",", 3);
        int stage = Integer.parseInt(parts[0]);
        long expiry = Long.parseLong(parts[1]);
        String reason = parts.length > 2 ? parts[2] : "Repeated or Critical Anti-Cheat Infractions.";
        if (reason.contains("[")) {
            reason = reason.substring(0, reason.indexOf("[")).trim();
        }
        if (expiry == -1 || stage >= 4) {
            String appealLink = Thedreamers_guards.CONFIG_ENGINE.getProperty("appeal_link", "No appeal system linked. Please contact the Server Administration directly.");
            String permanentBanRaw = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("suspension.permanent_ban", "§c[The Dreamers Guards]\n\n§c■ CRITICAL SYSTEM SECURITY BREACH ■\n§fYour client has been §cPERMANENTLY ISOLATED §ffrom the server.\\n\\n§7Reason: §e%1$s\n§7Strikes: §c3 / 3 Exceeded\n§7Status: §cTERMINATED\n\n§fAppeal Resolution:\n§b%2$s");
            return Component.literal(String.format(permanentBanRaw, reason, appealLink));
        }
        long now = System.currentTimeMillis();
        long diff = expiry - now;
        if (diff <= 0) {
            return Component.literal("§a[The Dreamers Guards]\n§7Your security lock suspension has expired. Please reconnect to the server.");
        }
        long diffSeconds = (diff / 1000) % 60;
        long diffMinutes = (diff / (60 * 1000)) % 60;
        long diffHours = (diff / (60 * 60 * 1000));

        String header = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("suspension.temporary_header", "§c[The Dreamers Guards]\n\n§6■ SECURITY PROTOCOL: CLIENT ISOLATION ■\n§7An infraction was flagged. Access temporarily restricted.\\n\\n");
        String phaseRaw = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("suspension.temporary_phase", "§7Current Phase: §e%s / 3\n");
        String penaltyRaw = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("suspension.temporary_penalty", "§7Remaining Penalty: §f%sh %sm %ss\\n\\n");
        String footer = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("suspension.temporary_footer", "§7Please completely purge any illegal modifications from your game directory before trying to reconnect.");

        StringBuilder sb = new StringBuilder();
        sb.append(header);
        sb.append("§7Reason: §e").append(reason).append("\n");
        sb.append(String.format(phaseRaw, stage));
        sb.append(String.format(penaltyRaw, diffHours, diffMinutes, diffSeconds));
        sb.append(footer);

        return Component.literal(sb.toString());
    }

    public static SuspensionEngine getEngine() {
        if (engine == null) {
            load();
        }
        return engine;
    }

    public static int getPhase(String username, String uuid, String ip) {
        if (engine == null) {
            load();
        }
        int stage = engine.getStage(username);
        if (stage == 0) stage = engine.getStage(uuid);
        if (stage == 0) stage = engine.getStage(ip);
        return stage;
    }
}