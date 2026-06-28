package net.thedreamers.guards;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.thedreamers.guards.network.AntiCheatAuthPayload;
import net.thedreamers.guards.network.ServerNetworkHandler;
import net.thedreamers.guards.server.GuardsCommand;
import net.thedreamers.guards.server.ServerPlayerListener;
import net.thedreamers.guards.webhook.DiscordWebhook;
import net.thedreamers.lib.config.ConfigEngine;
import net.thedreamers.guards.punishment.FlagSession;
import net.thedreamers.guards.punishment.SuspensionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Thedreamers_guards implements ModInitializer {

	public static final Set<UUID> PENDING_VERIFICATION = new HashSet<>();
	public static final Set<String> TRUSTED_PLAYERS = new HashSet<>();
	public static final ConfigEngine CONFIG_ENGINE = new ConfigEngine("config");
	private static final Logger LOGGER = LoggerFactory.getLogger("TheDreamersGuards");

	@Override
	public void onInitialize() {
		CONFIG_ENGINE.load();
		SuspensionManager.load();
		DiscordWebhook.init();
		PayloadTypeRegistry.serverboundPlay().register(AntiCheatAuthPayload.TYPE, AntiCheatAuthPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AntiCheatAuthPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				if (!PENDING_VERIFICATION.contains(context.player().getUUID())) return;
				PENDING_VERIFICATION.remove(context.player().getUUID());
				ServerPlayerListener.clearTimeout(context.player().getUUID());
				ServerNetworkHandler.processVerification(
						context.server(),
						context.player(),
						payload.version(),
						payload.token(),
						payload.modList(),
						context.player().getScoreboardName(),
						context.player().getUUID().toString()
				);
			});
		});
		ServerTickEvents.START_SERVER_TICK.register(FlagSession::tick);
		ServerTickEvents.START_SERVER_TICK.register(ServerPlayerListener::tick);
		ServerPlayerListener.registerEvents();
		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> GuardsCommand.register(dispatcher));

		String loadSuccessMsg = CONFIG_ENGINE.getLanguageString("system.load_success", "§b[The Dreamers Guards] §aCore security framework initialized successfully and securely!");
		LOGGER.info(loadSuccessMsg.replaceAll("§[0-9a-fk-or]", ""));
	}
}