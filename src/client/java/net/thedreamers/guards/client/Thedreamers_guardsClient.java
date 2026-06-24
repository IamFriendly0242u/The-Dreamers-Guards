package net.thedreamers.guards.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.thedreamers.guards.network.AntiCheatAuthPayload;
import net.thedreamers.guards.network.SecurityUtils;

public class Thedreamers_guardsClient implements ClientModInitializer {

	private static final String STATUS_CLEAN = "CLEAN";
	private static final String STATUS_DIRTY = "DIRTY_CHEATER";

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			String rawModList = ModScanner.getInstalledModsString();
			String authenticationToken = STATUS_CLEAN;

			if (ModScanner.containsIllegalMod()) {
				authenticationToken = STATUS_DIRTY;
			}

			String encryptedToken = SecurityUtils.encrypt(authenticationToken);
			String encryptedModList = SecurityUtils.encrypt(rawModList);

			sender.sendPacket(new AntiCheatAuthPayload(encryptedToken, encryptedModList));
		});
	}
}