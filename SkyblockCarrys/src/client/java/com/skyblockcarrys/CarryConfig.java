package com.skyblockcarrys;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Saved to config/skyblockcarrys.json */
public final class CarryConfig {
	public enum ChatMode { PARTY, PUBLIC, LOCAL }

	public static final class Boss {
		public String name;
		public long price;

		public Boss() {}

		public Boss(String name, long price) {
			this.name = name;
			this.price = price;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skyblockcarrys.json");
	private static CarryConfig instance;

	public List<Boss> bosses = new ArrayList<>();
	/** Name of the boss you are carrying right now. Trade coins are divided by its price. */
	public String activeBoss = "T4 Eman";
	/** true = overlay always visible, false = only while someone is being carried */
	public boolean alwaysShow = true;
	public ChatMode chatMode = ChatMode.PARTY;
	public int hudX = 6;
	public int hudY = 60;
	/** Regex used to detect the end of a trade; group 1 = player name. Edit if Hypixel's wording differs. */
	public String tradeRegex = "(?i)trade completed with (?:\\[[^\\]]+\\]\\s*)?(\\w{1,16})";
	/** Regex used to read coins out of a chat line shortly after a trade. group 1 = number, group 2 = optional k/m/b. */
	public String coinsRegex = "(?i)(\\d[\\d,.]*)\\s*([kmb])?\\s*coins";

	public static CarryConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				instance = GSON.fromJson(Files.readString(FILE), CarryConfig.class);
			}
		} catch (Exception e) {
			System.err.println("[SkyblockCarrys] Could not read config, using defaults: " + e);
		}
		if (instance == null) {
			instance = new CarryConfig();
		}
		if (instance.bosses == null || instance.bosses.isEmpty()) {
			instance.bosses = new ArrayList<>();
			instance.bosses.add(new Boss("T4 Eman", 2_000_000L));
			instance.bosses.add(new Boss("T4 Blaze", 2_000_000L));
		}
		if (instance.chatMode == null) {
			instance.chatMode = ChatMode.PARTY;
		}
		save();
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(get()));
		} catch (IOException e) {
			System.err.println("[SkyblockCarrys] Could not save config: " + e);
		}
	}

	public Boss active() {
		for (Boss b : bosses) {
			if (b.name.equalsIgnoreCase(activeBoss)) {
				return b;
			}
		}
		return bosses.isEmpty() ? null : bosses.get(0);
	}

	/** "2m", "1.5m", "500k", "2,000,000" -> long. Returns -1 when invalid. */
	public static long parseAmount(String text, String suffix) {
		try {
			double v = Double.parseDouble(text.replace(",", ""));
			if (suffix != null) {
				switch (suffix.toLowerCase()) {
					case "k" -> v *= 1_000;
					case "m" -> v *= 1_000_000;
					case "b" -> v *= 1_000_000_000;
					default -> { }
				}
			}
			return (long) v;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public static long parseAmount(String s) {
		s = s.trim().toLowerCase();
		if (s.isEmpty()) {
			return -1;
		}
		char last = s.charAt(s.length() - 1);
		if (last == 'k' || last == 'm' || last == 'b') {
			return parseAmount(s.substring(0, s.length() - 1), String.valueOf(last));
		}
		return parseAmount(s, null);
	}

	public static String formatAmount(long v) {
		if (v >= 1_000_000_000L && v % 1_000_000_000L == 0) return (v / 1_000_000_000L) + "b";
		if (v >= 1_000_000L && v % 1_000_000L == 0) return (v / 1_000_000L) + "m";
		if (v >= 1_000_000L && v % 100_000L == 0) return (v / 1_000_000.0) + "m";
		if (v >= 1_000L && v % 1_000L == 0) return (v / 1_000L) + "k";
		return Long.toString(v);
	}
}
