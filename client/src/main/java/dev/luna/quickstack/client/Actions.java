package dev.luna.quickstack.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import dev.luna.quickstack.client.network.Protocol;
import dev.luna.quickstack.client.network.RequestPayload;
import dev.luna.quickstack.client.network.ResultPayload;

public final class Actions {
	private Actions() {
	}

	/** Asks the server to quick stack or restock, leaving out the slots the player excluded. */
	public static void request(Action action) {
		if (Minecraft.getInstance().player == null) {
			return;
		}

		if (!ServerState.isPresent() || !ClientPlayNetworking.canSend(RequestPayload.TYPE)) {
			Feedback.show(Component.translatable("quickstack.message.unsupported"));
		} else if (!ServerState.isCompatible()) {
			Feedback.show(Component.translatable("quickstack.message.version_mismatch"));
		} else if (!ServerState.isAllowed(action)) {
			Feedback.show(Component.translatable("quickstack.message.denied"));
		} else {
			ClientPlayNetworking.send(new RequestPayload(action, SlotLocks.selectSlots(action)));
		}
	}

	public static void onResult(ResultPayload result) {
		Component message = switch (result.status()) {
			case Protocol.STATUS_OK -> QuickStackConfig.get().showMessages ? describe(result) : null;
			case Protocol.STATUS_DENIED -> Component.translatable("quickstack.message.denied");
			case Protocol.STATUS_DISABLED -> Component.translatable("quickstack.message.disabled");
			case Protocol.STATUS_VERSION_MISMATCH -> Component.translatable("quickstack.message.version_mismatch");
			default -> null;
		};
		if (message != null) {
			Feedback.show(message);
		}
	}

	private static Component describe(ResultPayload result) {
		String key = "quickstack.message." + result.action().key();
		if (result.items() <= 0) {
			return Component.translatable(key + ".nothing");
		}
		return Component.translatable(key + ".done",
				counted("quickstack.items", result.items()),
				counted("quickstack.containers", result.containers()));
	}

	private static Component counted(String key, int count) {
		return Component.translatable(key + (count == 1 ? ".one" : ".many"), count);
	}
}
