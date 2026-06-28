package net.thedreamers.guards.punishment;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.thedreamers.guards.Thedreamers_guards;
import net.thedreamers.guards.config.AntiCheatConfig;
import net.thedreamers.guards.webhook.DiscordWebhook;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FlagSession {

    private static final Map<UUID, SessionInstance> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    private static final File HISTORY_FILE = new File("config/TheDreamers_Guards/suspension-history.txt");

    private static String cleanFormatting(String input) {
        if (input == null) return "";
        return input.replace("\\u00A7", "§").replace("&", "§").replace("\\n", "\n");
    }

    public static List<String> getActiveFlaggedNames() {
        List<String> names = new ArrayList<>();
        for (SessionInstance session : ACTIVE_SESSIONS.values()) {
            names.add(session.playerName);
        }
        return names;
    }

    public static void start(MinecraftServer server, ServerPlayer player, String reason) {
        if (player == null || ACTIVE_SESSIONS.containsKey(player.getUUID())) return;
        int totalTimeout = AntiCheatConfig.getTotalTimeoutSeconds();
        ACTIVE_SESSIONS.put(player.getUUID(), new SessionInstance(player.getUUID(), player.getScoreboardName(), player.getIpAddress(), reason, totalTimeout));
        String rawAlert = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.alert_message", "§d[GUARDS ALERT] §e%s §7flagged for §c%s (Client-Side Checked).\n§8Yo admin, use §7/guards action §8to execute this cheater or §7/guards trust §8to let them stay.");
        String customAlert = String.format(rawAlert, player.getScoreboardName(), reason);
        server.getPlayerList().getPlayers().stream()
                .filter(p -> server.getPlayerList().isOp(new NameAndId(p.getGameProfile().id(), p.getGameProfile().name())))
                .forEach(admin -> admin.sendSystemMessage(Component.literal(cleanFormatting(customAlert))));
    }

    public static boolean stopSession(MinecraftServer server, String playerName) {
        for (UUID uuid : ACTIVE_SESSIONS.keySet()) {
            SessionInstance session = ACTIVE_SESSIONS.get(uuid);
            if (session.playerName.equalsIgnoreCase(playerName)) {
                ACTIVE_SESSIONS.remove(uuid);
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    player.removeEffect(MobEffects.HUNGER);
                    player.removeEffect(MobEffects.SLOWNESS);
                    player.removeEffect(MobEffects.DARKNESS);
                    player.removeEffect(MobEffects.POISON);
                    String trustPermittedMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.trust_permitted", "§d[The Dreamers Guards] §aYou have been permitted to stay in this server for a while.");
                    player.sendSystemMessage(Component.literal(cleanFormatting(trustPermittedMsg)));
                }
                return true;
            }
        }
        return false;
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE_SESSIONS.isEmpty()) return;
        for (UUID uuid : ACTIVE_SESSIONS.keySet()) {
            SessionInstance session = ACTIVE_SESSIONS.get(uuid);
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                ACTIVE_SESSIONS.remove(uuid);
                server.execute(() -> executeProgressivePunishment(server, null, session.playerName, uuid.toString(), session.ipAddress, session.reason));
                continue;
            }
            session.tickCounter++;
            if (session.tickCounter >= 20) {
                session.tickCounter = 0;
                session.remainingSeconds--;
                int remaining = session.remainingSeconds;
                if (remaining == 5) {
                    server.execute(() -> {
                        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 255, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 255, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 255, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 255, false, false));
                    });
                }
                if (remaining == 3 || remaining == 2 || remaining == 1) {
                    String rawBroadcast = AntiCheatConfig.getProperty("countdown_broadcast_message", "§c§l[WARN] §e%s §cgets dropped in §l%ss!");
                    String broadcastMsg = String.format(rawBroadcast, session.playerName, remaining);
                    server.getPlayerList().broadcastSystemMessage(Component.literal(cleanFormatting(broadcastMsg)), false);
                    server.execute(() -> {
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                    });
                }
                if (remaining <= 0) {
                    ACTIVE_SESSIONS.remove(uuid);
                    server.execute(() -> executeProgressivePunishment(server, player, session.playerName, uuid.toString(), session.ipAddress, session.reason));
                }
            }
        }
    }

    private static void executeProgressivePunishment(MinecraftServer server, ServerPlayer player, String name, String uuidStr, String ip, String reason) {
        int violationCount = getAndUpdateViolationCount(name, uuidStr, ip);
        String mode = AntiCheatConfig.getPunishmentMode();
        int durationMinutes;
        switch (violationCount) {
            case 1 -> durationMinutes = Integer.parseInt(AntiCheatConfig.getProperty("suspension_phase_1_mins", "20"));
            case 2 -> durationMinutes = Integer.parseInt(AntiCheatConfig.getProperty("suspension_phase_2_mins", "120"));
            case 3 -> durationMinutes = Integer.parseInt(AntiCheatConfig.getProperty("suspension_phase_3_mins", "360"));
            default -> durationMinutes = Integer.parseInt(AntiCheatConfig.getProperty("suspension_phase_4_mins", "-1"));
        }
        String phaseName = durationMinutes == -1 ? "PHASE 4 (TERMINATED)" : "PHASE " + violationCount + " / 3";
        String finalReason = reason + " [" + phaseName + "]";
        String uniqueBroadcast = "§c[The Dreamers Guards]\n\n§c■ AUTOMATED DETECTOR DROP ■\n§7Player §e" + name + " §7failed security telemetry integrity checks!\n§c» Verdict: §fBanishment Confirmed [" + phaseName + "]";
        server.getPlayerList().broadcastSystemMessage(Component.literal(cleanFormatting(uniqueBroadcast)), false);
        SuspensionManager.getEngine().load();
        SuspensionManager.getEngine().suspend(name, violationCount, durationMinutes, finalReason);
        SuspensionManager.getEngine().suspend(uuidStr, violationCount, durationMinutes, finalReason);
        SuspensionManager.getEngine().suspend(ip, violationCount, durationMinutes, finalReason);
        if (player != null) {
            String finalAction = (durationMinutes == -1 || "BAN".equalsIgnoreCase(mode)) ? "BAN" : "KICK";
            player.connection.disconnect(SuspensionManager.getSuspensionComponent(name, uuidStr, ip));
            DiscordWebhook.sendAlert(server, name, uuidStr, ip, finalReason, violationCount, finalAction);
        }
    }

    private static int getAndUpdateViolationCount(String playerName, String uuidStr, String ipAddress) {
        Properties props = new Properties();
        if (HISTORY_FILE.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(HISTORY_FILE), java.nio.charset.StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            HISTORY_FILE.getParentFile().mkdirs();
        }
        String record = props.getProperty(playerName, "0");
        int currentCount = 0;
        if (record.contains(",")) {
            currentCount = Integer.parseInt(record.split(",")[0]);
        } else {
            currentCount = Integer.parseInt(record);
        }
        currentCount++;
        props.setProperty(playerName, currentCount + "," + uuidStr + "," + ipAddress);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(HISTORY_FILE), java.nio.charset.StandardCharsets.UTF_8)) {
            props.store(writer, "The Dreamers Guards Infraction Tracking History");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return currentCount;
    }

    private static class SessionInstance {
        final UUID playerUuid;
        final String playerName;
        final String ipAddress;
        final String reason;
        int remainingSeconds;
        int tickCounter = 0;

        SessionInstance(UUID uuid, String name, String ip, String reason, int timeout) {
            this.playerUuid = uuid;
            this.playerName = name;
            this.ipAddress = ip;
            this.reason = reason;
            this.remainingSeconds = timeout;
        }
    }
}