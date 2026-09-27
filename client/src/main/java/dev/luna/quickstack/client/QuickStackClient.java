package dev.luna.quickstack.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.luna.quickstack.client.gui.InventoryControls;
import dev.luna.quickstack.client.network.Network;

public final class QuickStackClient implements ClientModInitializer {
	public static final String MOD_ID = "quickstack";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		QuickStackConfig.load();
		Network.register();
		KeyMappings.register();
		InventoryControls.register();
		ClientTickEvents.END_CLIENT_TICK.register(SlotLocks::tick);
	}
}
