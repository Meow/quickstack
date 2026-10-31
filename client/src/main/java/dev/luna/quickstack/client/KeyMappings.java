package dev.luna.quickstack.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class KeyMappings {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(QuickStackClient.MOD_ID, "main"));

	public static final KeyMapping QUICK_STACK = unbound("key.quickstack.quick_stack");
	public static final KeyMapping RESTOCK = unbound("key.quickstack.restock");
	public static final KeyMapping LOCK_MODIFIER =
			new KeyMapping("key.quickstack.lock_modifier", InputConstants.KEY_LALT, CATEGORY);

	private KeyMappings() {
	}

	public static void register() {
		KeyMappingHelper.registerKeyMapping(QUICK_STACK);
		KeyMappingHelper.registerKeyMapping(RESTOCK);
		KeyMappingHelper.registerKeyMapping(LOCK_MODIFIER);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (QUICK_STACK.consumeClick()) {
				Actions.request(Action.QUICK_STACK);
			}
			while (RESTOCK.consumeClick()) {
				Actions.request(Action.RESTOCK);
			}
		});
	}

	/** Key mappings do not track their state while a screen is open, so ask the keyboard directly. */
	public static boolean isLockModifierDown() {
		InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(LOCK_MODIFIER);
		if (key.getType() != InputConstants.Type.KEYBOARD || key.equals(InputConstants.UNKNOWN)) {
			return false;
		}
		return InputConstants.isKeyDown(key.getValue());
	}

	private static KeyMapping unbound(String name) {
		return new KeyMapping(name, InputConstants.UNKNOWN.getValue(), CATEGORY);
	}
}
