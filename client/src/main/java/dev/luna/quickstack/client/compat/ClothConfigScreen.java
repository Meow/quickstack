package dev.luna.quickstack.client.compat;

import java.util.Locale;
import java.util.function.Consumer;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import dev.luna.quickstack.client.LockCorner;
import dev.luna.quickstack.client.QuickStackConfig;
import dev.luna.quickstack.client.SlotLocks;

final class ClothConfigScreen {
	private ClothConfigScreen() {
	}

	static Screen create(Screen parent) {
		QuickStackConfig config = QuickStackConfig.get();
		QuickStackConfig defaults = new QuickStackConfig();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("quickstack.config.title"))
				.setSavingRunnable(config::save);
		ConfigEntryBuilder entries = builder.entryBuilder();
		ConfigCategory general = builder.getOrCreateCategory(Component.translatable("quickstack.config.category.general"));

		general.addEntry(toggle(entries, "quick_stack_hotbar", config.quickStackHotbar, defaults.quickStackHotbar,
				value -> config.quickStackHotbar = value));
		general.addEntry(toggle(entries, "restock_hotbar", config.restockHotbar, defaults.restockHotbar,
				value -> config.restockHotbar = value));
		general.addEntry(toggle(entries, "restock_locked_slots", config.restockLockedSlots, defaults.restockLockedSlots,
				value -> config.restockLockedSlots = value));
		general.addEntry(toggle(entries, "show_buttons", config.showButtons, defaults.showButtons,
				value -> config.showButtons = value));
		general.addEntry(toggle(entries, "show_messages", config.showMessages, defaults.showMessages,
				value -> config.showMessages = value));
		general.addEntry(entries.startEnumSelector(
						Component.translatable("quickstack.config.lock_corner"), LockCorner.class, config.lockCorner)
				.setDefaultValue(defaults.lockCorner)
				.setEnumNameProvider(corner -> Component.translatable(
						"quickstack.config.lock_corner." + corner.name().toLowerCase(Locale.ROOT)))
				.setTooltip(Component.translatable("quickstack.config.lock_corner.tooltip"))
				.setSaveConsumer(value -> config.lockCorner = value)
				.build());
		general.addEntry(toggle(entries, "clear_locks", false, false, value -> {
			if (value) {
				SlotLocks.clearAll();
			}
		}));

		return builder.build();
	}

	private static AbstractConfigListEntry<Boolean> toggle(
			ConfigEntryBuilder entries, String name, boolean value, boolean defaultValue, Consumer<Boolean> save) {
		String key = "quickstack.config." + name;
		return entries.startBooleanToggle(Component.translatable(key), value)
				.setDefaultValue(defaultValue)
				.setTooltip(Component.translatable(key + ".tooltip"))
				.setSaveConsumer(save)
				.build();
	}
}
