package net.thedreamers.guards.punishment;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.thedreamers.guards.config.AntiCheatConfig;
import net.thedreamers.guards.webhook.DiscordWebhook;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FlagSession {

    private static final Map<UUID, FlagSession> SESSIONS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private final MinecraftServer server;
    private final UUID playerUuid;
    private final String playerName;
    private final String reason;
    private final String playerIp;
    private int timeLeft;
    private ScheduledFuture<?> task;

    private FlagSession(MinecraftServer server, ServerPlayer player, String reason) {
        this.server = server;
        this.playerUuid = player.getUUID();
        this.playerName = player.getScoreboardName();
        this.reason = reason;
        this.playerIp = player.getIpAddress();
        this.timeLeft = AntiCheatConfig.getTotalTimeoutSeconds();
    }

    public static void start(MinecraftServer server, ServerPlayer player, String reason) {
        if (SESSIONS.containsKey(player.getUUID())) return;
        FlagSession session = new FlagSession(server, player, reason);
        SESSIONS.put(player.getUUID(), session);
        session.init();
    }

    private void init() {
        String alert = String.format(AntiCheatConfig.getAlertAdminMessage(), playerName, reason);
        System.out.println(String.format(AntiCheatConfig.getConsoleLogAlert(), playerName, reason));

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            NameAndId nameAndId = new NameAndId(p.getGameProfile().id(), p.getGameProfile().name());
            if (server.getPlayerList().isOp(nameAndId)) {
                p.sendSystemMessage(Component.literal(alert));
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "playsound minecraft:entity.experience_orb.pickup master " + p.getScoreboardName());
            }
        }

        this.task = EXECUTOR.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
    }

    private void tick() {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);

            if (timeLeft <= 0) {
                cancel();
                if (player != null) {
                    PunishmentExecutor.execute(server, player, reason, AntiCheatConfig.getPunishmentMode());
                } else {
                    int phase = SuspensionManager.getPhase(playerName, playerUuid.toString(), playerIp);
                    if (AntiCheatConfig.getPunishmentMode().equals("BAN")) {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "ban " + playerName + " Evading Anti-Cheat Punishment");
                        DiscordWebhook.sendAlert(server, playerName, playerUuid.toString(), playerIp, reason + " (Offline Evaded)", phase, "BAN");
                    } else {
                        SuspensionManager.suspend(playerName, playerUuid.toString(), playerIp);
                        int postPhase = SuspensionManager.getPhase(playerName, playerUuid.toString(), playerIp);
                        DiscordWebhook.sendAlert(server, playerName, playerUuid.toString(), playerIp, reason + " (Offline Evaded)", postPhase, "KICK (SUSPEND)");
                    }
                }
                return;
            }

            if (player == null) {
                cancel();
                int phase = SuspensionManager.getPhase(playerName, playerUuid.toString(), playerIp);
                if (AntiCheatConfig.getPunishmentMode().equals("BAN")) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "ban " + playerName + " Evading Anti-Cheat Countdown");
                    DiscordWebhook.sendAlert(server, playerName, playerUuid.toString(), playerIp, reason + " (Logged Out During Countdown)", phase, "BAN");
                } else {
                    SuspensionManager.suspend(playerName, playerUuid.toString(), playerIp);
                    int postPhase = SuspensionManager.getPhase(playerName, playerUuid.toString(), playerIp);
                    DiscordWebhook.sendAlert(server, playerName, playerUuid.toString(), playerIp, reason + " (Logged Out During Countdown)", postPhase, "KICK (SUSPEND)");
                }
                return;
            }

            if (timeLeft == 5) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "effect give " + playerName + " minecraft:darkness 10 255 true");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "effect give " + playerName + " minecraft:slowness 10 255 true");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "effect give " + playerName + " minecraft:hunger 10 255 true");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "effect give " + playerName + " minecraft:poison 10 255 true");
            }

            if (timeLeft <= 3 && timeLeft > 0) {
                String broadcast = String.format(AntiCheatConfig.getCountdownBroadcastMessage(), playerName, timeLeft);
                server.getPlayerList().broadcastSystemMessage(Component.literal(broadcast), false);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "playsound minecraft:block.anvil.destroy master @a");
            }

            timeLeft--;
        });
    }

    public static boolean resolve(UUID uuid, String type) {
        FlagSession session = SESSIONS.remove(uuid);
        if (session != null) {
            session.cancel();
            ServerPlayer player = session.server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                PunishmentExecutor.execute(session.server, player, session.reason, type);
            } else {
                int phase = SuspensionManager.getPhase(session.playerName, session.playerUuid.toString(), session.playerIp);
                if (type.equals("BAN")) {
                    session.server.getCommands().performPrefixedCommand(session.server.createCommandSourceStack(), "ban " + session.playerName + " Admin Decision (Offline)");
                    DiscordWebhook.sendAlert(session.server, session.playerName, session.playerUuid.toString(), session.playerIp, session.reason + " (Admin Resolve Offline)", phase, "BAN");
                } else if (type.equals("KICK")) {
                    SuspensionManager.suspend(session.playerName, session.playerUuid.toString(), session.playerIp);
                    int postPhase = SuspensionManager.getPhase(session.playerName, session.playerUuid.toString(), session.playerIp);
                    DiscordWebhook.sendAlert(session.server, session.playerName, session.playerUuid.toString(), session.playerIp, session.reason + " (Admin Resolve Offline)", postPhase, "KICK (SUSPEND)");
                }
            }
            return true;
        }
        return false;
    }

    private void cancel() {
        if (task != null) {
            task.cancel(false);
        }
        SESSIONS.remove(playerUuid);
    }
}