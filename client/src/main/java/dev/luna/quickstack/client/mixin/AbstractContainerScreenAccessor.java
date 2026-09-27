package dev.luna.quickstack.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor
	int getLeftPos();

	@Accessor
	int getTopPos();

	@Accessor
	int getImageWidth();

	@Accessor
	int getImageHeight();

	@Accessor
	@Nullable Slot getHoveredSlot();
}
