package dev.luna.quickstack.server;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import dev.luna.quickstack.server.NearbyContainers.Target;

public final class QuickStackPlugin extends JavaPlugin implements PluginMessageListener, Listener {
	private static final String PERMISSION_QUICK_STACK = "quickstack.quickstack";
	private static final String PERMISSION_RESTOCK = "quickstack.restock";

	private final Map<UUID, Long> lastRequest = new HashMap<>();
	private Settings settings;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		settings = Settings.load(getConfig());

		getServer().getMessenger().registerOutgoingPluginChannel(this, Protocol.CONFIG);
		getServer().getMessenger().registerOutgoingPluginChannel(this, Protocol.RESULT);
		getServer().getMessenger().registerIncomingPluginChannel(this, Protocol.REQUEST, this);
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		getServer().getMessenger().unregisterOutgoingPluginChannel(this);
		getServer().getMessenger().unregisterIncomingPluginChannel(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
			return false;
		}

		reloadConfig();
		settings = Settings.load(getConfig());
		for (Player player : getServer().getOnlinePlayers()) {
			sendConfig(player);
		}
		sender.sendMessage("QuickStack configuration reloaded.");
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return args.length == 1 ? List.of("reload") : List.of();
	}

	/** The client mod announces itself by listening on the config channel, answer with the server rules. */
	@EventHandler
	public void onRegisterChannel(PlayerRegisterChannelEvent event) {
		if (event.getChannel().equals(Protocol.CONFIG)) {
			sendConfig(event.getPlayer());
		}
	}

	/** Covers clients whose channels were already known by the time they finished joining. */
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		sendConfig(event.getPlayer());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		lastRequest.remove(event.getPlayer().getUniqueId());
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals(Protocol.REQUEST) || message.length < 10) {
			return;
		}

		ByteBuffer buffer = ByteBuffer.wrap(message);
		int version = buffer.get();
		byte action = buffer.get();
		long slotMask = buffer.getLong() & Protocol.SLOT_MASK;

		if (version != Protocol.VERSION) {
			sendResult(player, action, Protocol.STATUS_VERSION_MISMATCH, 0, 0);
			return;
		}
		if (action != Protocol.ACTION_QUICK_STACK && action != Protocol.ACTION_RESTOCK) {
			return;
		}

		boolean quickStack = action == Protocol.ACTION_QUICK_STACK;
		if (!(quickStack ? settings.quickStack() : settings.restock())) {
			sendResult(player, action, Protocol.STATUS_DISABLED, 0, 0);
			return;
		}
		if (!player.hasPermission(quickStack ? PERMISSION_QUICK_STACK : PERMISSION_RESTOCK)
				|| player.isDead()
				|| player.getGameMode() == GameMode.SPECTATOR) {
			sendResult(player, action, Protocol.STATUS_DENIED, 0, 0);
			return;
		}

		long now = System.currentTimeMillis();
		Long previous = lastRequest.get(player.getUniqueId());
		if (previous != null && now - previous < settings.cooldownMs()) {
			sendResult(player, action, Protocol.STATUS_COOLDOWN, 0, 0);
			return;
		}
		lastRequest.put(player.getUniqueId(), now);

		List<Target> targets = NearbyContainers.find(player, settings);
		Transfers.Result result = quickStack
				? Transfers.quickStack(player, slotMask, targets, settings)
				: Transfers.restock(player, slotMask, targets, settings);

		if (result.items() > 0) {
			playEffects(player, result, quickStack);
		}
		sendResult(player, action, Protocol.STATUS_OK, result.items(), result.containers().size());
	}

	private void sendConfig(Player player) {
		if (!player.getListeningPluginChannels().contains(Protocol.CONFIG)) {
			return;
		}
		player.sendPluginMessage(this, Protocol.CONFIG, Protocol.config(
				settings.quickStack() && player.hasPermission(PERMISSION_QUICK_STACK),
				settings.restock() && player.hasPermission(PERMISSION_RESTOCK),
				settings.radius()));
	}

	private void sendResult(Player player, byte action, byte status, int items, int containers) {
		player.sendPluginMessage(this, Protocol.RESULT, Protocol.result(action, status, items, containers));
	}

	private void playEffects(Player player, Transfers.Result result, boolean quickStack) {
		if (settings.sound()) {
			player.playSound(player, quickStack ? Sound.ITEM_BUNDLE_INSERT : Sound.ITEM_BUNDLE_REMOVE_ONE,
					SoundCategory.PLAYERS, 0.8f, 1.0f);
		}
		if (settings.particles()) {
			for (Block block : result.containers()) {
				Location center = block.getLocation().add(0.5, 0.5, 0.5);
				player.spawnParticle(Particle.HAPPY_VILLAGER, center, 8, 0.4, 0.4, 0.4);
			}
		}
	}
}
