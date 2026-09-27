package dev.luna.quickstack.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.loot.Lootable;

public final class NearbyContainers {
	/** A container in reach of the player. Access is only checked once it is about to be used. */
	public static final class Target {
		private final Block block;
		private final Inventory inventory;
		private final boolean shulkerBox;
		private final double distanceSquared;
		private Boolean accessible;

		private Target(Block block, Inventory inventory, boolean shulkerBox, double distanceSquared) {
			this.block = block;
			this.inventory = inventory;
			this.shulkerBox = shulkerBox;
			this.distanceSquared = distanceSquared;
		}

		public Block block() {
			return block;
		}

		public Inventory inventory() {
			return inventory;
		}

		public boolean isShulkerBox() {
			return shulkerBox;
		}

		public boolean canAccess(Player player, Settings settings) {
			if (accessible == null) {
				accessible = !settings.checkProtection() || mayOpen(player, block);
			}
			return accessible;
		}
	}

	private NearbyContainers() {
	}

	/** Finds the containers around the player in loaded chunks, nearest first. */
	public static List<Target> find(Player player, Settings settings) {
		Location origin = player.getLocation().add(0, player.getHeight() / 2, 0);
		World world = player.getWorld();
		int radius = settings.radius();
		double radiusSquared = (double) radius * radius;

		int minChunkX = (origin.getBlockX() - radius) >> 4;
		int maxChunkX = (origin.getBlockX() + radius) >> 4;
		int minChunkZ = (origin.getBlockZ() - radius) >> 4;
		int maxChunkZ = (origin.getBlockZ() + radius) >> 4;

		List<Target> targets = new ArrayList<>();
		Set<Location> seen = new HashSet<>();

		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				if (!world.isChunkLoaded(chunkX, chunkZ)) {
					continue;
				}

				for (BlockState state : world.getChunkAt(chunkX, chunkZ)
						.getTileEntities(block -> distanceSquared(origin, block) <= radiusSquared, false)) {
					if (!(state instanceof Container container) || !isUsable(container, settings)) {
						continue;
					}

					Inventory inventory = container.getInventory();
					// Both halves of a double chest share one inventory, only take it once.
					Location key = inventory instanceof DoubleChestInventory doubleChest
							? doubleChest.getLeftSide().getLocation()
							: state.getLocation();
					if (key == null || !seen.add(key)) {
						continue;
					}

					targets.add(new Target(state.getBlock(), inventory, state instanceof ShulkerBox,
							distanceSquared(origin, state.getBlock())));
				}
			}
		}

		targets.sort(Comparator.comparingDouble(target -> target.distanceSquared));
		return targets.size() > settings.maxContainers() ? targets.subList(0, settings.maxContainers()) : targets;
	}

	private static boolean isUsable(Container container, Settings settings) {
		boolean enabled = switch (container) {
			case Chest chest -> settings.chests() && !(settings.skipBlockedChests() && chest.isBlocked());
			case Barrel _ -> settings.barrels();
			case ShulkerBox _ -> settings.shulkerBoxes();
			default -> false;
		};
		if (!enabled || container.isLocked()) {
			return false;
		}
		// Do not touch (and thereby generate) loot that nobody has opened yet.
		return !(container instanceof Lootable lootable && lootable.hasLootTable());
	}

	private static double distanceSquared(Location origin, Block block) {
		double dx = block.getX() + 0.5 - origin.getX();
		double dy = block.getY() + 0.5 - origin.getY();
		double dz = block.getZ() + 0.5 - origin.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	private static boolean mayOpen(Player player, Block block) {
		PlayerInteractEvent event = new PlayerInteractEvent(
				player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP, EquipmentSlot.HAND);
		player.getServer().getPluginManager().callEvent(event);
		return event.useInteractedBlock() != Event.Result.DENY;
	}
}
