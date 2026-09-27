package dev.luna.quickstack.server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import dev.luna.quickstack.server.NearbyContainers.Target;

public final class Transfers {
	public record Result(int items, Set<Block> containers) {
	}

	private Transfers() {
	}

	/**
	 * Moves the items of the given player slots into nearby containers that already hold
	 * the same kind of item. Tools and armor do not stack, they go into free slots.
	 */
	public static Result quickStack(Player player, long slotMask, List<Target> targets, Settings settings) {
		PlayerInventory inventory = player.getInventory();
		Set<Block> touched = new LinkedHashSet<>();
		int total = 0;

		for (Target target : targets) {
			List<ItemStack> present = contents(target.inventory());
			if (present.isEmpty()) {
				continue;
			}
			boolean shulkerBox = target.isShulkerBox();

			for (int slot = 0; slot < Protocol.SLOT_COUNT; slot++) {
				if ((slotMask & (1L << slot)) == 0) {
					continue;
				}
				ItemStack stack = inventory.getItem(slot);
				if (stack == null || stack.isEmpty() || !holds(present, stack, settings.matchItemType())) {
					continue;
				}
				if (shulkerBox && Tag.SHULKER_BOXES.isTagged(stack.getType())) {
					continue;
				}
				if (!target.canAccess(player, settings)) {
					break;
				}

				int remaining = target.inventory().addItem(stack.clone()).values().stream()
						.mapToInt(ItemStack::getAmount)
						.sum();
				int moved = stack.getAmount() - remaining;
				if (moved <= 0) {
					continue;
				}

				inventory.setItem(slot, remaining == 0 ? null : stack.asQuantity(remaining));
				total += moved;
				touched.add(target.block());
			}
		}

		return new Result(total, touched);
	}

	/**
	 * Tops up the stacks in the given player slots with matching items from nearby containers.
	 */
	public static Result restock(Player player, long slotMask, List<Target> targets, Settings settings) {
		PlayerInventory inventory = player.getInventory();
		Set<Block> touched = new LinkedHashSet<>();
		int total = 0;

		for (int slot = 0; slot < Protocol.SLOT_COUNT; slot++) {
			if ((slotMask & (1L << slot)) == 0) {
				continue;
			}
			ItemStack stack = inventory.getItem(slot);
			if (stack == null || stack.isEmpty()) {
				continue;
			}
			int missing = stack.getMaxStackSize() - stack.getAmount();
			if (missing <= 0) {
				continue;
			}

			int taken = 0;
			for (Target target : targets) {
				int fromTarget = take(player, target, stack, missing - taken, settings);
				if (fromTarget > 0) {
					taken += fromTarget;
					touched.add(target.block());
				}
				if (taken >= missing) {
					break;
				}
			}

			if (taken > 0) {
				inventory.setItem(slot, stack.asQuantity(stack.getAmount() + taken));
				total += taken;
			}
		}

		return new Result(total, touched);
	}

	private static int take(Player player, Target target, ItemStack wanted, int amount, Settings settings) {
		Inventory source = target.inventory();
		int taken = 0;

		for (int i = 0; i < source.getSize() && taken < amount; i++) {
			ItemStack candidate = source.getItem(i);
			if (candidate == null || candidate.isEmpty() || !candidate.isSimilar(wanted)) {
				continue;
			}
			if (!target.canAccess(player, settings)) {
				return 0;
			}

			int count = Math.min(amount - taken, candidate.getAmount());
			source.setItem(i, count == candidate.getAmount() ? null : candidate.asQuantity(candidate.getAmount() - count));
			taken += count;
		}

		return taken;
	}

	private static List<ItemStack> contents(Inventory inventory) {
		List<ItemStack> present = new ArrayList<>();
		for (ItemStack stack : inventory.getStorageContents()) {
			if (stack != null && !stack.isEmpty()) {
				present.add(stack);
			}
		}
		return present;
	}

	/** Whether the container counts as a home for the stack, going by item type or by identical items. */
	private static boolean holds(List<ItemStack> present, ItemStack stack, boolean matchItemType) {
		for (ItemStack other : present) {
			if (matchItemType ? other.getType() == stack.getType() : other.isSimilar(stack)) {
				return true;
			}
		}
		return false;
	}
}
