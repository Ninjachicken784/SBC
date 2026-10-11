package com.skyblockcarrys;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** All carry state: trade detection, pending requests, tracked clients. */
public final class CarryManager {
	public record Pending(String player, int count, String bossName) {}

	public static final class Entry {
		public final String player;
		public int done;
		public int total;

		Entry(String player, int total) {
			this.player = player;
			this.total = total;
		}
	}

	public static final List<Pending> pending = new ArrayList<>();
	public static final List<Entry> entries = new ArrayList<>();

	public static boolean debug = false;

	private static String tradeName;
	private static long tradeTime;
	private static final long TRADE_WINDOW_MS = 4000;

	private CarryManager() {}

	/** Called for every system chat line. */
	public static void onChat(String line) {
		CarryConfig cfg = CarryConfig.get();
		long now = System.currentTimeMillis();

		try {
			Matcher t = Pattern.compile(cfg.tradeRegex).matcher(line);
			if (t.find()) {
				tradeName = t.group(1);
				tradeTime = now;
				if (debug) info("[debug] trade detected with " + tradeName);
				return;
			}
		} catch (Exception e) {
			info("Bad tradeRegex in config: " + e.getMessage());
		}

		if (tradeName != null && now - tradeTime <= TRADE_WINDOW_MS) {
			String trimmed = line.trim();
			if (trimmed.startsWith("-")) {
				return; // coins you gave away, not received
			}
			try {
				Matcher c = Pattern.compile(cfg.coinsRegex).matcher(line);
				if (c.find()) {
					long coins = CarryConfig.parseAmount(c.group(1), c.groupCount() >= 2 ? c.group(2) : null);
					if (coins > 0) {
						String who = tradeName;
						tradeName = null;
						offer(who, coins);
					}
				}
			} catch (Exception e) {
				info("Bad coinsRegex in config: " + e.getMessage());
			}
		} else if (debug && (line.toLowerCase().contains("trade") || line.toLowerCase().contains("coins"))) {
			info("[debug] unmatched line: " + line);
		}
	}

	/** Someone paid {@code coins}: work out how many carries of the active boss that buys and ask the user. */
	public static void offer(String player, long coins) {
		CarryConfig.Boss boss = CarryConfig.get().active();
		if (boss == null || boss.price <= 0) {
			info("No active boss price set. Open /sbc and set one.");
			return;
		}
		int count = (int) Math.min(coins / boss.price, 999);
		if (count <= 0) {
			info(player + " traded " + CarryConfig.formatAmount(coins) + " but " + boss.name + " costs "
					+ CarryConfig.formatAmount(boss.price) + " - ignoring.");
			return;
		}
		pending.add(new Pending(player, count, boss.name));
	}

	public static void accept(Pending p) {
		pending.remove(p);
		for (Entry e : entries) {
			if (e.player.equalsIgnoreCase(p.player())) {
				e.total += p.count();
				return;
			}
		}
		entries.add(new Entry(p.player(), p.count()));
	}

	public static void decline(Pending p) {
		pending.remove(p);
	}

	public static void remove(Entry e) {
		entries.remove(e);
	}

	/** [+] pressed: only this client's counter goes up, then post it to chat. */
	public static void increment(Entry e) {
		if (e.done >= e.total) {
			return;
		}
		e.done++;
		recordBoss();
		sendProgress(e);
	}

	private static void sendProgress(Entry e) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		String text = "[" + e.player + "] [" + e.done + "/" + e.total + "]";
		switch (CarryConfig.get().chatMode) {
			case PARTY -> mc.player.connection.sendCommand("pc " + text);
			case PUBLIC -> mc.player.connection.sendChat(text);
			case LOCAL -> mc.player.sendSystemMessage(Component.literal(text));
		}
	}

	// ---- boss timing: time between two [+] clicks ----
	private static final long MAX_GAP_MS = 5 * 60 * 1000L; // longer pause = break, not a boss time
	private static final int KEEP = 10;
	private static final ArrayDeque<Double> recent = new ArrayDeque<>();
	private static long lastPlusTime = 0;
	private static double lastBossSec = -1;

	private static void recordBoss() {
		long now = System.currentTimeMillis();
		if (lastPlusTime > 0 && now - lastPlusTime <= MAX_GAP_MS) {
			lastBossSec = (now - lastPlusTime) / 1000.0;
			recent.addLast(lastBossSec);
			while (recent.size() > KEEP) {
				recent.removeFirst();
			}
		}
		lastPlusTime = now;
	}

	public static void resetStats() {
		recent.clear();
		lastPlusTime = 0;
		lastBossSec = -1;
	}

	/** "42s" / "1m05s" / "0s" */
	public static String lastTimeText() {
		if (lastBossSec < 0) {
			return "0s";
		}
		int s = (int) Math.round(lastBossSec);
		return s < 60 ? s + "s" : (s / 60) + "m" + String.format("%02d", s % 60) + "s";
	}

	/** Bosses per hour from the average of the last few boss times. */
	public static String perHourText() {
		if (recent.isEmpty()) {
			return "0";
		}
		double sum = 0;
		for (double d : recent) {
			sum += d;
		}
		double avg = sum / recent.size();
		return avg <= 0 ? "0" : Long.toString(Math.round(3600.0 / avg));
	}

	public static void info(String msg) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.sendSystemMessage(Component.literal("§b[SBC] §f" + msg));
		}
	}
}
