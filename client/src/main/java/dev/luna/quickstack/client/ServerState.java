package dev.luna.quickstack.client;

import dev.luna.quickstack.client.network.ConfigPayload;
import dev.luna.quickstack.client.network.Protocol;

/** What the server told us about its QuickStack plugin, if it has one. */
public final class ServerState {
	private static boolean present;
	private static boolean compatible;
	private static boolean quickStackAllowed;
	private static boolean restockAllowed;
	private static int radius;

	private ServerState() {
	}

	public static void accept(ConfigPayload config) {
		present = true;
		compatible = config.version() == Protocol.VERSION;
		quickStackAllowed = config.quickStack();
		restockAllowed = config.restock();
		radius = config.radius();
	}

	public static void reset() {
		present = false;
		compatible = false;
		quickStackAllowed = false;
		restockAllowed = false;
		radius = 0;
	}

	/** Whether the server runs the plugin at all, regardless of its version. */
	public static boolean isPresent() {
		return present;
	}

	public static boolean isCompatible() {
		return present && compatible;
	}

	public static boolean isAllowed(Action action) {
		return isCompatible() && (action == Action.QUICK_STACK ? quickStackAllowed : restockAllowed);
	}

	public static int radius() {
		return radius;
	}
}
