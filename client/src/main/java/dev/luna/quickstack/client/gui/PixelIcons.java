package dev.luna.quickstack.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import dev.luna.quickstack.client.LockCorner;

/** Tiny icons drawn pixel by pixel, which saves shipping textures for them. */
public final class PixelIcons {
	private static final int BACKDROP = 0xB0000000;
	private static final int SHADOW = 0xFF202020;
	private static final int LOCK_SHACKLE = 0xFFE0E0E0;
	private static final int LOCK_BODY = 0xFFFFC83D;
	private static final int LOCK_KEYHOLE = 0xFF6B4A00;
	private static final int ARROW_DEPOSIT = 0xFF7CFC6A;
	private static final int ARROW_WITHDRAW = 0xFFFFB347;

	private static final String[] LOCK = {
			" sss ",
			"s   s",
			"s   s",
			"bbbbb",
			"bbkbb",
			"bbbbb",
	};

	private static final int LOCK_WIDTH = LOCK[0].length() + 2;
	private static final int LOCK_HEIGHT = LOCK.length + 2;

	private static final String[] ARROW_DOWN = {
			" aaa ",
			" aaa ",
			"aaaaa",
			" aaa ",
			"  a  ",
	};

	private static final String[] ARROW_UP = {
			"  a  ",
			" aaa ",
			"aaaaa",
			" aaa ",
			" aaa ",
	};

	private PixelIcons() {
	}

	/** Padlock badge in a corner of a slot. */
	public static void lock(GuiGraphicsExtractor graphics, int slotX, int slotY, LockCorner corner) {
		int x = corner.x(slotX, LOCK_WIDTH);
		int y = corner.y(slotY, LOCK_HEIGHT);
		graphics.fill(x, y, x + LOCK_WIDTH, y + LOCK_HEIGHT, BACKDROP);
		draw(graphics, LOCK, x + 1, y + 1, 0);
	}

	/** Arrow pointing into (deposit) or out of (withdraw) the chest icon of a button. */
	public static void arrow(GuiGraphicsExtractor graphics, int x, int y, boolean deposit) {
		String[] shape = deposit ? ARROW_DOWN : ARROW_UP;
		int color = deposit ? ARROW_DEPOSIT : ARROW_WITHDRAW;
		draw(graphics, shape, x + 1, y + 1, SHADOW);
		draw(graphics, shape, x, y, color);
	}

	private static void draw(GuiGraphicsExtractor graphics, String[] shape, int x, int y, int arrowColor) {
		for (int row = 0; row < shape.length; row++) {
			String line = shape[row];
			for (int column = 0; column < line.length(); column++) {
				int color = switch (line.charAt(column)) {
					case 's' -> LOCK_SHACKLE;
					case 'b' -> LOCK_BODY;
					case 'k' -> LOCK_KEYHOLE;
					case 'a' -> arrowColor;
					default -> 0;
				};
				if (color != 0) {
					graphics.fill(x + column, y + row, x + column + 1, y + row + 1, color);
				}
			}
		}
	}
}
