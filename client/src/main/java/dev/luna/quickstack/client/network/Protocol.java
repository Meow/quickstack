package dev.luna.quickstack.client.network;

import net.minecraft.resources.Identifier;

/**
 * Wire format shared with the server plugin, see the README. All numbers are big endian.
 */
public final class Protocol {
	public static final int VERSION = 1;

	public static final Identifier CONFIG = id("config");
	public static final Identifier REQUEST = id("request");
	public static final Identifier RESULT = id("result");

	public static final byte STATUS_OK = 0;
	public static final byte STATUS_COOLDOWN = 1;
	public static final byte STATUS_DENIED = 2;
	public static final byte STATUS_DISABLED = 3;
	public static final byte STATUS_VERSION_MISMATCH = 4;

	private Protocol() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("quickstack", path);
	}
}
