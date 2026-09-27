package dev.luna.quickstack.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

public final class QuickStackConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(QuickStackClient.MOD_ID + ".json");

	private static QuickStackConfig instance = new QuickStackConfig();

	public boolean quickStackHotbar = false;
	public boolean restockHotbar = true;
	public boolean restockLockedSlots = true;
	public boolean showButtons = true;
	public boolean showMessages = true;
	public LockCorner lockCorner = LockCorner.TOP_RIGHT;
	/**
	 * Locked items by server address: player inventory slot (0-8 hotbar, 9-35 main inventory)
	 * to the item that was locked there. Maintained by {@link SlotLocks}.
	 */
	public TreeMap<String, TreeMap<Integer, String>> locks = new TreeMap<>();

	public static QuickStackConfig get() {
		return instance;
	}

	public static void load() {
		if (!Files.exists(PATH)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(PATH)) {
			QuickStackConfig loaded = GSON.fromJson(reader, QuickStackConfig.class);
			if (loaded != null) {
				if (loaded.lockCorner == null) {
					loaded.lockCorner = LockCorner.TOP_RIGHT;
				}
				if (loaded.locks == null) {
					loaded.locks = new TreeMap<>();
				}
				loaded.locks.values().removeIf(locks -> locks == null);
				instance = loaded;
			}
		} catch (IOException | JsonParseException e) {
			QuickStackClient.LOGGER.warn("Could not read {}, using defaults", PATH, e);
		}
	}

	public void save() {
		locks.values().removeIf(TreeMap::isEmpty);
		try (Writer writer = Files.newBufferedWriter(PATH)) {
			GSON.toJson(this, writer);
		} catch (IOException e) {
			QuickStackClient.LOGGER.warn("Could not write {}", PATH, e);
		}
	}
}
