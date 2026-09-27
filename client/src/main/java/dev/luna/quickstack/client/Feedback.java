package dev.luna.quickstack.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/** Short status messages, shown above the hotbar or next to the inventory while one is open. */
public final class Feedback {
	private static final long SCREEN_MESSAGE_MILLIS = 2500;

	private static @Nullable Component screenMessage;
	private static long screenMessageExpiry;

	private Feedback() {
	}

	public static void show(Component message) {
		Minecraft minecraft = Minecraft.getInstance();
		// The inventory covers the spot above the hotbar, so screens draw the message themselves.
		if (minecraft.gui.screen() instanceof AbstractContainerScreen<?>) {
			screenMessage = message;
			screenMessageExpiry = Util.getMillis() + SCREEN_MESSAGE_MILLIS;
		} else if (minecraft.player != null) {
			minecraft.player.sendOverlayMessage(message);
		}
	}

	public static @Nullable Component screenMessage() {
		if (screenMessage != null && Util.getMillis() > screenMessageExpiry) {
			screenMessage = null;
		}
		return screenMessage;
	}
}
