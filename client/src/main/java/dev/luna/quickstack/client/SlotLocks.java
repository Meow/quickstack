package dev.luna.quickstack.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Locks keep items out of quick stacking. A lock belongs to the item it was put on: it follows
 * the item around the inventory and ends when the item leaves it. Since the server knows nothing
 * about locks, the inventory is compared tick by tick to see where locked items went.
 */
public final class SlotLocks {
	private static final String LOCAL = "singleplayer";
	/** How long a locked item may be out of sight, which covers updates that arrive in pieces. */
	private static final int GRACE_TICKS = 5;
	private static final int SAVE_INTERVAL_TICKS = 20;

	/** A locked item that is on the cursor or has not shown up in its new slot yet. */
	private static final class Floating {
		private final ItemStack item;
		private final String id;
		private int ticksLeft = GRACE_TICKS;

		private Floating(ItemStack item, String id) {
			this.item = item;
			this.id = id;
		}
	}

	private static final List<Floating> FLOATING = new ArrayList<>();
	private static String server = LOCAL;
	private static @Nullable LocalPlayer trackedPlayer;
	private static ItemStack @Nullable [] previous;
	private static boolean dirty;
	private static int ticksSinceSave;

	private SlotLocks() {
	}

	/** Only the hotbar and main inventory of the player can hold locked items. */
	public static boolean isLockable(Slot slot) {
		return slot.container instanceof Inventory
				&& slot.getContainerSlot() >= 0
				&& slot.getContainerSlot() < Inventory.INVENTORY_SIZE;
	}

	public static boolean isLocked(Slot slot) {
		return isLockable(slot) && isLocked(slot.getContainerSlot());
	}

	public static boolean isLocked(int inventorySlot) {
		Map<Integer, String> locks = QuickStackConfig.get().locks.get(server);
		LocalPlayer player = Minecraft.getInstance().player;
		if (locks == null || player == null) {
			return false;
		}

		String id = locks.get(inventorySlot);
		ItemStack stack = player.getInventory().getItem(inventorySlot);
		return id != null && !stack.isEmpty() && id.equals(itemId(stack));
	}

	/** Locks or unlocks the item in the slot. Returns false if there is nothing to lock. */
	public static boolean toggle(Slot slot) {
		int inventorySlot = slot.getContainerSlot();
		if (isLocked(inventorySlot)) {
			locks().remove(inventorySlot);
		} else if (slot.hasItem()) {
			locks().put(inventorySlot, itemId(slot.getItem()));
		} else {
			return false;
		}

		dirty = true;
		return true;
	}

	public static void clearAll() {
		QuickStackConfig.get().locks.clear();
		FLOATING.clear();
	}

	/** The slots the server may touch for the given action, one bit per player inventory slot. */
	public static long selectSlots(Action action) {
		QuickStackConfig config = QuickStackConfig.get();
		long mask = 0;

		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			boolean hotbar = Inventory.isHotbarSlot(slot);
			boolean included = switch (action) {
				case QUICK_STACK -> !isLocked(slot) && (config.quickStackHotbar || !hotbar);
				case RESTOCK -> (config.restockLockedSlots || !isLocked(slot)) && (config.restockHotbar || !hotbar);
			};
			if (included) {
				mask |= 1L << slot;
			}
		}

		return mask;
	}

	/** Locks are kept per server, as inventories differ between them. */
	public static void onJoin(Minecraft minecraft) {
		ServerData data = minecraft.getCurrentServer();
		server = data != null ? data.ip : LOCAL;
		forget();
	}

	public static void onDisconnect() {
		save();
		forget();
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null) {
			forget();
			return;
		}

		ItemStack[] current = snapshot(player.getInventory());
		// Respawning and changing dimension hand out a new player whose inventory arrives later.
		// Treating that as items leaving would cost all locks, so tracking starts over instead.
		if (player != trackedPlayer || previous == null) {
			forget();
			trackedPlayer = player;
			previous = current;
			return;
		}

		// An item on the cursor may hold the only lock of this server, which needs a place to return to.
		Map<Integer, String> locks = FLOATING.isEmpty() ? QuickStackConfig.get().locks.get(server) : locks();
		if (locks != null && !(locks.isEmpty() && FLOATING.isEmpty())) {
			track(locks, previous, current, player.containerMenu.getCarried());
		}
		previous = current;

		if (++ticksSinceSave >= SAVE_INTERVAL_TICKS) {
			save();
		}
	}

	private static void track(Map<Integer, String> locks, ItemStack[] before, ItemStack[] now, ItemStack carried) {
		Set<Integer> claimed = new HashSet<>();
		Map<Integer, String> arrived = new TreeMap<>();

		for (Iterator<Map.Entry<Integer, String>> iterator = locks.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<Integer, String> lock = iterator.next();
			int slot = lock.getKey();
			String id = lock.getValue();

			boolean wasThere = !before[slot].isEmpty() && id.equals(itemId(before[slot]));
			if (!wasThere) {
				// Not seen yet since joining or respawning. It is over once something else moved in.
				if (!now[slot].isEmpty() && !id.equals(itemId(now[slot]))) {
					iterator.remove();
					dirty = true;
				}
				continue;
			}
			if (ItemStack.isSameItemSameComponents(before[slot], now[slot])) {
				continue;
			}

			int destination = findArrival(before[slot], before, now, claimed);
			if (destination >= 0) {
				claimed.add(destination);
				arrived.put(destination, id);
			} else if (now[slot].getItem() == before[slot].getItem()) {
				// Still the same item, it only changed, for example by taking damage.
				continue;
			} else {
				FLOATING.add(new Floating(before[slot], id));
			}
			iterator.remove();
			dirty = true;
		}

		for (Iterator<Floating> iterator = FLOATING.iterator(); iterator.hasNext(); ) {
			Floating floating = iterator.next();
			if (ItemStack.isSameItemSameComponents(carried, floating.item)) {
				floating.ticksLeft = GRACE_TICKS;
				continue;
			}

			int destination = findArrival(floating.item, before, now, claimed);
			if (destination >= 0) {
				claimed.add(destination);
				arrived.put(destination, floating.id);
				dirty = true;
				iterator.remove();
			} else if (--floating.ticksLeft <= 0) {
				iterator.remove();
			}
		}

		locks.putAll(arrived);
	}

	/** Finds the slot an item was just put into, either as a new stack or by adding to one. */
	private static int findArrival(ItemStack item, ItemStack[] before, ItemStack[] now, Set<Integer> claimed) {
		int found = -1;
		for (int slot = 0; slot < now.length; slot++) {
			if (claimed.contains(slot) || !ItemStack.isSameItemSameComponents(now[slot], item)) {
				continue;
			}

			boolean isNew = !ItemStack.isSameItemSameComponents(before[slot], item);
			if (isNew && now[slot].getCount() == item.getCount()) {
				return slot;
			}
			if (found < 0 && (isNew || now[slot].getCount() > before[slot].getCount())) {
				found = slot;
			}
		}
		return found;
	}

	private static ItemStack[] snapshot(Inventory inventory) {
		ItemStack[] items = new ItemStack[Inventory.INVENTORY_SIZE];
		for (int slot = 0; slot < items.length; slot++) {
			items[slot] = inventory.getItem(slot).copy();
		}
		return items;
	}

	private static Map<Integer, String> locks() {
		return QuickStackConfig.get().locks.computeIfAbsent(server, key -> new TreeMap<>());
	}

	private static String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static void forget() {
		trackedPlayer = null;
		previous = null;
		FLOATING.clear();
	}

	private static void save() {
		ticksSinceSave = 0;
		if (dirty) {
			dirty = false;
			QuickStackConfig.get().save();
		}
	}
}
