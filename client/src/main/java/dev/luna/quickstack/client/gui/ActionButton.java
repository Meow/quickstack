package dev.luna.quickstack.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.luna.quickstack.client.Action;
import dev.luna.quickstack.client.Actions;
import dev.luna.quickstack.client.ServerState;

/** Chest icon with an arrow that triggers an action when clicked. */
final class ActionButton extends Button {
	static final int SIZE = 20;

	private final ItemStack icon = new ItemStack(Items.CHEST);
	private final Action action;
	private final Tooltip tooltip;
	private final Tooltip notAllowedTooltip;

	ActionButton(Action action) {
		Component label = Component.translatable("quickstack.button." + action.key());
		super(0, 0, SIZE, SIZE, label, button -> Actions.request(action), DEFAULT_NARRATION);
		this.action = action;
		this.tooltip = Tooltip.create(label);
		this.notAllowedTooltip = Tooltip.create(Component.translatable("quickstack.button.not_allowed"));
	}

	/** Follows the inventory around and hides on servers without the plugin. */
	void update(int x, int y, boolean show) {
		boolean allowed = ServerState.isAllowed(action);
		setPosition(x, y);
		visible = show;
		active = allowed;
		setTooltip(allowed ? tooltip : notAllowedTooltip);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		extractDefaultSprite(graphics);
		graphics.item(icon, getX() + 2, getY() + 2);
		PixelIcons.arrow(graphics, getX() + SIZE - 8, getY() + 2, action == Action.QUICK_STACK);
	}
}
