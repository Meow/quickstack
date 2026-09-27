package dev.luna.quickstack.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import dev.luna.quickstack.client.Actions;
import dev.luna.quickstack.client.ServerState;
import dev.luna.quickstack.client.SlotLocks;

public final class Network {
	private Network() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(ConfigPayload.TYPE, ConfigPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ResultPayload.TYPE, ResultPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RequestPayload.TYPE, RequestPayload.CODEC);

		// Listening on the config channel is what makes the server plugin send its config.
		ClientPlayNetworking.registerGlobalReceiver(ConfigPayload.TYPE, (payload, context) -> ServerState.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(ResultPayload.TYPE, (payload, context) -> Actions.onResult(payload));

		ClientPlayConnectionEvents.INIT.register((listener, client) -> ServerState.reset());
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> SlotLocks.onJoin(client));
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
			ServerState.reset();
			SlotLocks.onDisconnect();
		});
	}
}
