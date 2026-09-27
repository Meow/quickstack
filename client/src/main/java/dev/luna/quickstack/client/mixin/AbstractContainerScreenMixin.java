package dev.luna.quickstack.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.luna.quickstack.client.QuickStackConfig;
import dev.luna.quickstack.client.gui.InventoryControls;
import dev.luna.quickstack.client.gui.PixelIcons;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {
	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void quickstack$markLockedSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (InventoryControls.isLocked((AbstractContainerScreen<?>) (Object) this, slot)) {
			PixelIcons.lock(graphics, slot.x, slot.y, QuickStackConfig.get().lockCorner);
		}
	}
}
