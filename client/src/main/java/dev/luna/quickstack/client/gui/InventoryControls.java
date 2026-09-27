package dev.luna.quickstack.client.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;

import dev.luna.quickstack.client.Action;
import dev.luna.quickstack.client.Actions;
import dev.luna.quickstack.client.Feedback;
import dev.luna.quickstack.client.KeyMappings;
import dev.luna.quickstack.client.QuickStackConfig;
import dev.luna.quickstack.client.ServerState;
import dev.luna.quickstack.client.SlotLocks;
import dev.luna.quickstack.client.mixin.AbstractContainerScreenAccessor;

/** Everything the mod adds to inventory screens: slot locking, the buttons and the key binds. */
public final class InventoryControls {
	private static final int BUTTON_GAP = 2;
	private static final int MESSAGE_GAP = 6;

	private InventoryControls() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof AbstractContainerScreen<?> containerScreen && supports(containerScreen)) {
				attach(containerScreen);
			}
		});
	}

	/**
	 * The creative inventory numbers its slots differently and has no use for stacking,
	 * so the mod stays out of it.
	 */
	private static boolean supports(AbstractContainerScreen<?> screen) {
		return !(screen instanceof CreativeModeInventoryScreen);
	}

	public static boolean isLocked(AbstractContainerScreen<?> screen, Slot slot) {
		return supports(screen) && SlotLocks.isLocked(slot);
	}

	private static void attach(AbstractContainerScreen<?> screen) {
		ScreenMouseEvents.allowMouseClick(screen).register(InventoryControls::allowMouseClick);
		ScreenKeyboardEvents.allowKeyPress(screen).register(InventoryControls::allowKeyPress);
		ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> extractMessage(screen, graphics));

		if (screen.getMenu().slots.stream().noneMatch(SlotLocks::isLockable)) {
			return;
		}

		ActionButton quickStack = new ActionButton(Action.QUICK_STACK);
		ActionButton restock = new ActionButton(Action.RESTOCK);
		Screens.getWidgets(screen).add(quickStack);
		Screens.getWidgets(screen).add(restock);

		// The inventory moves when the recipe book opens, so the buttons are placed every frame.
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
		Runnable layout = () -> {
			boolean show = QuickStackConfig.get().showButtons && ServerState.isCompatible();
			int x = accessor.getLeftPos() + accessor.getImageWidth() + BUTTON_GAP;
			int bottom = accessor.getTopPos() + accessor.getImageHeight() - 4;
			quickStack.update(x, bottom - 2 * ActionButton.SIZE - BUTTON_GAP, show);
			restock.update(x, bottom - ActionButton.SIZE, show);
		};
		layout.run();
		ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> layout.run());
	}

	/** Result messages go below the inventory, or above it when there is no room left. */
	private static void extractMessage(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics) {
		Component message = Feedback.screenMessage();
		if (message == null) {
			return;
		}

		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
		Font font = screen.getFont();
		int width = font.width(message);
		int x = accessor.getLeftPos() + (accessor.getImageWidth() - width) / 2;
		int y = accessor.getTopPos() + accessor.getImageHeight() + MESSAGE_GAP;
		if (y + font.lineHeight + 2 > screen.height) {
			y = accessor.getTopPos() - MESSAGE_GAP - font.lineHeight;
		}
		graphics.textWithBackdrop(font, message, x, y, width, 0xFFFFFFFF);
	}

	/** Clicking an item while holding the lock key toggles its lock instead of picking it up. */
	private static boolean allowMouseClick(Screen screen, MouseButtonEvent event) {
		if (event.button() != 0 || !KeyMappings.isLockModifierDown()) {
			return true;
		}

		Slot slot = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
		if (slot == null || !SlotLocks.isLockable(slot) || !SlotLocks.toggle(slot)) {
			return true;
		}

		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		return false;
	}

	private static boolean allowKeyPress(Screen screen, KeyEvent event) {
		if (isTyping(screen)) {
			return true;
		}

		if (KeyMappings.QUICK_STACK.matches(event)) {
			Actions.request(Action.QUICK_STACK);
			return false;
		}
		if (KeyMappings.RESTOCK.matches(event)) {
			Actions.request(Action.RESTOCK);
			return false;
		}
		return true;
	}

	/** Whether key presses currently go into a text field, such as the recipe book search. */
	private static boolean isTyping(Screen screen) {
		GuiEventListener focused = screen.getFocused();
		while (focused instanceof ContainerEventHandler container) {
			focused = container.getFocused();
		}
		return focused instanceof EditBox editBox && editBox.isFocused();
	}
}
