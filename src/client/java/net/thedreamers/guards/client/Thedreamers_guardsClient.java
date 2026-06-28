package net.thedreamers.guards.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.thedreamers.guards.network.AntiCheatAuthPayload;
import net.thedreamers.lib.security.SecurityUtils;

public class Thedreamers_guardsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			String clientVersion = FabricLoader.getInstance()
					.getModContainer("thedreamers_guards")
					.map(c -> c.getMetadata().getVersion().getFriendlyString())
					.orElse("1.0.0");
			String rawModList = ModScanner.getInstalledModsString();
			String token = ModScanner.containsIllegalMod() ? "DIRTY_CHEATER" : "SECURE_HANDSHAKE_TOKEN";
			String encryptedVersion = SecurityUtils.encrypt(clientVersion);
			String encryptedToken = SecurityUtils.encrypt(token);
			String encryptedModList = SecurityUtils.encrypt(rawModList);
			sender.sendPacket(new AntiCheatAuthPayload(encryptedVersion, encryptedToken, encryptedModList));
		});
	}
}