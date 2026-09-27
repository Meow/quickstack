package dev.luna.quickstack.server;

import org.bukkit.configuration.file.FileConfiguration;

public record Settings(
		boolean quickStack,
		boolean restock,
		boolean matchItemType,
		int radius,
		int maxContainers,
		long cooldownMs,
		boolean chests,
		boolean barrels,
		boolean shulkerBoxes,
		boolean skipBlockedChests,
		boolean checkProtection,
		boolean sound,
		boolean particles
) {
	public static final int MAX_RADIUS = 32;

	public static Settings load(FileConfiguration config) {
		return new Settings(
				config.getBoolean("quick-stack", true),
				config.getBoolean("restock", true),
				config.getBoolean("match-item-type", true),
				Math.clamp(config.getInt("radius", 8), 1, MAX_RADIUS),
				Math.max(1, config.getInt("max-containers", 64)),
				Math.max(0, config.getLong("cooldown-ms", 500)),
				config.getBoolean("containers.chests", true),
				config.getBoolean("containers.barrels", true),
				config.getBoolean("containers.shulker-boxes", true),
				config.getBoolean("skip-blocked-chests", true),
				config.getBoolean("check-protection", true),
				config.getBoolean("effects.sound", true),
				config.getBoolean("effects.particles", true)
		);
	}
}
