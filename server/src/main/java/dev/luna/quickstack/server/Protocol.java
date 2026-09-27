package dev.luna.quickstack.server;

import java.nio.ByteBuffer;

/**
 * Wire format shared with the client mod, see the README. All numbers are big endian.
 */
public final class Protocol {
	public static final int VERSION = 1;

	/** S2C: int version, boolean quickStack, boolean restock, int radius. Sent once the client registers the channel. */
	public static final String CONFIG = "quickstack:config";
	/** C2S: byte version, byte action, long slotMask. */
	public static final String REQUEST = "quickstack:request";
	/** S2C: byte action, byte status, int items, int containers. */
	public static final String RESULT = "quickstack:result";

	public static final byte ACTION_QUICK_STACK = 0;
	public static final byte ACTION_RESTOCK = 1;

	public static final byte STATUS_OK = 0;
	public static final byte STATUS_COOLDOWN = 1;
	public static final byte STATUS_DENIED = 2;
	public static final byte STATUS_DISABLED = 3;
	public static final byte STATUS_VERSION_MISMATCH = 4;

	/** Hotbar (0-8) and main inventory (9-35), same numbering as the vanilla player inventory. */
	public static final int SLOT_COUNT = 36;
	public static final long SLOT_MASK = (1L << SLOT_COUNT) - 1;

	private Protocol() {
	}

	public static byte[] config(boolean quickStack, boolean restock, int radius) {
		return ByteBuffer.allocate(10)
				.putInt(VERSION)
				.put((byte) (quickStack ? 1 : 0))
				.put((byte) (restock ? 1 : 0))
				.putInt(radius)
				.array();
	}

	public static byte[] result(byte action, byte status, int items, int containers) {
		return ByteBuffer.allocate(10)
				.put(action)
				.put(status)
				.putInt(items)
				.putInt(containers)
				.array();
	}
}
