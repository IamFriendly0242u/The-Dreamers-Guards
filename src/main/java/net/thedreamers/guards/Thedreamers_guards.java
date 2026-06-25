package net.thedreamers.guards;

import net.thedreamers.guards.config.AntiCheatConfig;
import net.thedreamers.guards.network.AntiCheatAuthPayload;
import net.thedreamers.guards.network.SecurityUtils;
import net.thedreamers.guards.punishment.FlagSession;
import net.thedreamers.guards.punishment.SuspensionManager;
import net.thedreamers.guards.server.GuardsCommand;
import net.thedreamers.guards.webhook.DiscordWebhook;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Thedreamers_guards implements ModInitializer {

	private static final String MOD_ID = "thedreamers_guards";
	public static final Set<UUID> PENDING_VERIFICATION = ConcurrentHashMap.newKeySet();

	static {
		long count = FabricLoader.getInstance().getAllMods().stream()
				.filter(mod -> mod.getMetadata().getId().equals(MOD_ID))
				.count();

		if (count > 1) {
			String art = "\n" +
					"**********************************************************\n" +
					"   [!] CRITICAL SYSTEM ALERT: DUPLICATE DETECTED [!]\n" +
					"**********************************************************\n" +
					" TheDreamers Guards has detected more than one instance of \n" +
					" itself running in the server environment! \n\n" +
					" Conflict detected: Multiple versions running simultaneously.\n" +
					" Policy: Only one instance is allowed for stability.\n" +
					" STATUS: Server forcefully shutting down to prevent corruption.\n" +
					"**********************************************************\n";
			System.err.println(art);
			System.exit(1);
		}
	}

	@Override
	public void onInitialize() {
		AntiCheatConfig.loadConfig();
		SuspensionManager.load();
		DiscordWebhook.init();

		PayloadTypeRegistry.serverboundPlay().register(AntiCheatAuthPayload.TYPE, AntiCheatAuthPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(AntiCheatAuthPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			MinecraftServer server = context.server();

			String decryptedToken = SecurityUtils.decrypt(payload.clientAuthToken());
			String decryptedModList = SecurityUtils.decrypt(payload.modHashList());

			server.execute(() -> {
				PENDING_VERIFICATION.remove(player.getUUID());
				String playerName = player.getScoreboardName();
				String playerUuid = player.getUUID().toString();

				if (decryptedToken.equals("CORRUPTED_PACKET")) {
					FlagSession.start(server, player, "Packet Tampering (Decryption Failed)");
					DiscordWebhook.sendVerifyAlert(server, playerName, playerUuid, "FAILED", "Packet Tampering (Decryption Failed)");
					return;
				}

				if (decryptedToken.equals("DIRTY_CHEATER")) {
					FlagSession.start(server, player, "Illegal Modifications (Client-Side Checked)");
					DiscordWebhook.sendVerifyAlert(server, playerName, playerUuid, "FAILED", "Client-Side Scanner flagged illegal mods loaded.");
					return;
				}

				String[] blacklist = AntiCheatConfig.getModBlacklist();
				for (String cheat : blacklist) {
					if (!cheat.trim().isEmpty() && decryptedModList.toLowerCase().contains(cheat.toLowerCase())) {
						FlagSession.start(server, player, "Illegal Mod: " + cheat);
						DiscordWebhook.sendVerifyAlert(server, playerName, playerUuid, "FAILED", "Blacklisted modification signature found: " + cheat);
						return;
					}
				}

				DiscordWebhook.sendVerifyAlert(server, playerName, playerUuid, "SUCCESS", "Passed secure authentication payload validation check.");
			});
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			String playerName = player.getScoreboardName();
			String playerUuid = player.getUUID().toString();
			String playerIp = player.getIpAddress();

			server.execute(() -> {
				if (SuspensionManager.isSuspended(playerName, playerUuid, playerIp)) {
					player.connection.disconnect(SuspensionManager.getSuspensionComponent(playerName, playerUuid, playerIp));
					return;
				}

				UUID uuid = player.getUUID();
				PENDING_VERIFICATION.add(uuid);

				new Thread(() -> {
					try {
						Thread.sleep(AntiCheatConfig.getValidationDelayMs());
					} catch (InterruptedException exception) {
						exception.printStackTrace();
					}
					server.execute(() -> {
						if (PENDING_VERIFICATION.contains(uuid)) {
							ServerPlayer pendingPlayer = server.getPlayerList().getPlayer(uuid);
							if (pendingPlayer != null) {
								Component kickMessage = Component.literal(AntiCheatConfig.getKickNoModMessage());
								pendingPlayer.connection.disconnect(kickMessage);
								DiscordWebhook.sendVerifyAlert(server, pendingPlayer.getScoreboardName(), uuid.toString(), "FAILED", "No authentication packet received (Missing Mod).");
							}
						}
					});
				}).start();
			});
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			PENDING_VERIFICATION.remove(handler.getPlayer().getUUID());
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			GuardsCommand.register(dispatcher);
		});
	}
}