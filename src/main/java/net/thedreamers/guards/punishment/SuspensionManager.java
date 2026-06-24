package net.thedreamers.guards.punishment;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.thedreamers.guards.config.AntiCheatConfig;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SuspensionManager {

    private static final Path FILE_PATH = FabricLoader.getInstance().getConfigDir().resolve("TheDreamers_Guards").resolve("suspended-list.txt");
    private static final Path HISTORY_PATH = FabricLoader.getInstance().getConfigDir().resolve("TheDreamers_Guards").resolve("suspension-history.txt");

    private static final List<SuspensionRecord> SUSPENDED = new CopyOnWriteArrayList<>();
    private static final List<HistoryRecord> PHASES = new CopyOnWriteArrayList<>();

    public static class SuspensionRecord {
        public final String username;
        public final String uuid;
        public final String ip;
        public final long expiry;

        public SuspensionRecord(String username, String uuid, String ip, long expiry) {
            this.username = username.toLowerCase();
            this.uuid = uuid;
            this.ip = ip;
            this.expiry = expiry;
        }
    }

    public static class HistoryRecord {
        public final String username;
        public final String uuid;
        public final String ip;
        public final int phase;

        public HistoryRecord(String username, String uuid, String ip, int phase) {
            this.username = username.toLowerCase();
            this.uuid = uuid;
            this.ip = ip;
            this.phase = phase;
        }
    }

    public static void load() {
        SUSPENDED.clear();
        PHASES.clear();

        if (Files.exists(FILE_PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(FILE_PATH, StandardCharsets.UTF_8)) {
                String line;
                long now = System.currentTimeMillis();
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || !line.contains("=")) continue;
                    String[] parts = line.split("=", 2);
                    long expiry = Long.parseLong(parts[1].trim());
                    if (expiry > now) {
                        String[] info = parts[0].split("\\|");
                        if (info.length == 3) {
                            SUSPENDED.add(new SuspensionRecord(info[0], info[1], info[2], expiry));
                        }
                    }
                }
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if (Files.exists(HISTORY_PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(HISTORY_PATH, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || !line.contains("=")) continue;
                    String[] parts = line.split("=", 2);
                    int phase = Integer.parseInt(parts[1].trim());
                    String[] info = parts[0].split("\\|");
                    if (info.length == 3) {
                        PHASES.add(new HistoryRecord(info[0], info[1], info[2], phase));
                    }
                }
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try {
            if (!Files.exists(FILE_PATH.getParent())) {
                Files.createDirectories(FILE_PATH.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
                long now = System.currentTimeMillis();
                for (SuspensionRecord record : SUSPENDED) {
                    if (record.expiry > now) {
                        writer.write(record.username + "|" + record.uuid + "|" + record.ip + "=" + record.expiry);
                        writer.newLine();
                    }
                }
            }
            try (BufferedWriter writer = Files.newBufferedWriter(HISTORY_PATH, StandardCharsets.UTF_8)) {
                for (HistoryRecord record : PHASES) {
                    writer.write(record.username + "|" + record.uuid + "|" + record.ip + "=" + record.phase);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void suspend(String username, String uuid, String ip) {
        String user = username.toLowerCase();
        int currentPhase = 0;
        HistoryRecord existingHistory = null;

        for (HistoryRecord record : PHASES) {
            if (record.username.equals(user) || record.uuid.equals(uuid) || record.ip.equals(ip)) {
                currentPhase = record.phase;
                existingHistory = record;
                break;
            }
        }

        int nextPhase = currentPhase + 1;
        if (nextPhase > 4) nextPhase = 4;

        if (existingHistory != null) {
            PHASES.remove(existingHistory);
        }
        PHASES.add(new HistoryRecord(user, uuid, ip, nextPhase));

        long durationMs;
        if (nextPhase == 1) {
            durationMs = (long) AntiCheatConfig.getSuspensionPhase1Mins() * 60 * 1000;
        } else if (nextPhase == 2) {
            durationMs = (long) AntiCheatConfig.getSuspensionPhase2Mins() * 60 * 1000;
        } else if (nextPhase == 3) {
            durationMs = (long) AntiCheatConfig.getSuspensionPhase3Mins() * 60 * 1000;
        } else {
            int phase4Mins = AntiCheatConfig.getSuspensionPhase4Mins();
            if (phase4Mins == -1) {
                durationMs = 100L * 365 * 24 * 60 * 60 * 1000;
            } else {
                durationMs = (long) phase4Mins * 60 * 1000;
            }
        }

        long expiry = System.currentTimeMillis() + durationMs;

        for (SuspensionRecord record : SUSPENDED) {
            if (record.username.equals(user) || record.uuid.equals(uuid) || record.ip.equals(ip)) {
                SUSPENDED.remove(record);
            }
        }

        SUSPENDED.add(new SuspensionRecord(user, uuid, ip, expiry));
        save();
    }

    public static boolean isSuspended(String username, String uuid, String ip) {
        long now = System.currentTimeMillis();
        for (SuspensionRecord record : SUSPENDED) {
            if (record.username.equals(username.toLowerCase()) || record.uuid.equals(uuid) || record.ip.equals(ip)) {
                if (now < record.expiry) {
                    return true;
                } else {
                    SUSPENDED.remove(record);
                    save();
                }
            }
        }
        return false;
    }

    public static int getPhase(String username, String uuid, String ip) {
        for (HistoryRecord record : PHASES) {
            if (record.username.equals(username.toLowerCase()) || record.uuid.equals(uuid) || record.ip.equals(ip)) {
                return record.phase;
            }
        }
        return 1;
    }

    public static String getRemainingTime(String username, String uuid, String ip) {
        long now = System.currentTimeMillis();
        for (SuspensionRecord record : SUSPENDED) {
            if (record.username.equals(username.toLowerCase()) || record.uuid.equals(uuid) || record.ip.equals(ip)) {
                long remainingMs = record.expiry - now;
                if (remainingMs <= 0) return "0s";

                int phase = getPhase(username, uuid, ip);
                if (phase == 4 && AntiCheatConfig.getSuspensionPhase4Mins() == -1) {
                    return "PERMANENT";
                }

                long hours = remainingMs / 3600000;
                long mins = (remainingMs % 3600000) / 60000;
                long secs = (remainingMs % 60000) / 1000;
                if (hours > 24000) return "PERMANENT";
                return hours + "h " + mins + "m " + secs + "s";
            }
        }
        return "0s";
    }

    public static Component getSuspensionComponent(String username, String uuid, String ip) {
        int phase = getPhase(username, uuid, ip);

        if (phase >= 4) {
            return Component.literal(
                    "§c[The Dreamers Guards]\n\n" +
                            "§4§l■ CRITICAL SYSTEM SECURITY BREACH ■\n" +
                            "§7Your client has been §cPERMANENTLY ISOLATED§7 from the server.\n\n" +
                            "§7Reason: §eRepeated or Critical Anti-Cheat Infractions.\n" +
                            "§7Strikes: §c3 / 3 Exceeded\n" +
                            "§7Status: §4§lTERMINATED\n\n" +
                            "§7Appeal Resolution:\n" +
                            "§b" + AntiCheatConfig.getAppealUrl()
            );
        }

        return Component.literal(
                "§c[The Dreamers Guards]\n\n" +
                        "§6§l■ SECURITY PROTOCOL: CLIENT ISOLATION ■\n" +
                        "§7An infraction was flagged. Access temporarily restricted.\n\n" +
                        "§7Current Phase: §e" + phase + " / 3\n" +
                        "§7Remaining Penalty: §f" + getRemainingTime(username, uuid, ip) + "\n\n" +
                        "§7Please completely purge any illegal modifications\n" +
                        "§7from your game directory before trying to reconnect."
        );
    }

    public static boolean pardon(String target) {
        String cleanTarget = target.toLowerCase();
        boolean removed = false;

        for (SuspensionRecord record : SUSPENDED) {
            if (record.username.equals(cleanTarget) || record.uuid.equalsIgnoreCase(cleanTarget) || record.ip.equals(cleanTarget)) {
                SUSPENDED.remove(record);
                removed = true;
            }
        }
        for (HistoryRecord record : PHASES) {
            if (record.username.equals(cleanTarget) || record.uuid.equalsIgnoreCase(cleanTarget) || record.ip.equals(cleanTarget)) {
                PHASES.remove(record);
                removed = true;
            }
        }

        if (removed) {
            save();
            return true;
        }
        return false;
    }
}