package dev.luna.quickstack.client;

/** Corner of a slot that shows the padlock. Other mods mark slots too, so it can be moved. */
public enum LockCorner {
	TOP_LEFT(false, false),
	TOP_RIGHT(true, false),
	BOTTOM_LEFT(false, true),
	BOTTOM_RIGHT(true, true);

	private final boolean right;
	private final boolean bottom;

	LockCorner(boolean right, boolean bottom) {
		this.right = right;
		this.bottom = bottom;
	}

	/** Left edge of something that wide, within a slot starting at slotX. */
	public int x(int slotX, int width) {
		return right ? slotX + 16 - width : slotX;
	}

	public int y(int slotY, int height) {
		return bottom ? slotY + 16 - height : slotY;
	}
}
