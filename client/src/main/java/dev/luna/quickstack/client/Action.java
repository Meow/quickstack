package dev.luna.quickstack.client;

public enum Action {
	QUICK_STACK((byte) 0, "quick_stack"),
	RESTOCK((byte) 1, "restock");

	private final byte id;
	private final String key;

	Action(byte id, String key) {
		this.id = id;
		this.key = key;
	}

	public byte id() {
		return id;
	}

	/** Name used in translation keys. */
	public String key() {
		return key;
	}

	public static Action byId(byte id) {
		return id == RESTOCK.id ? RESTOCK : QUICK_STACK;
	}
}
