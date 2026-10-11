package com.skyblockcarrys;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Two separate overlays, each draggable on its own:
 *  1) the carry list  - one row per client: "Name [0/10] [+] [x]"
 *  2) the stats text  - "[SBC] Carries / Bosses (42s) 72/h", no background
 */
public final class Overlay {
	public record Hit(int x1, int y1, int x2, int y2, Runnable action) {
		boolean contains(double x, double y) {
			return x >= x1 && x <= x2 && y >= y1 && y <= y2;
		}
	}

	private static final List<Hit> hits = new ArrayList<>();
	/** x1,y1,x2,y2 of the carry list / stats text, or null when not drawn. */
	public static int[] listBounds = null;
	public static int[] statsBounds = null;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int ROW_BG = 0xB0101018;
	private static final int ACCENT = 0xFF55FFFF;
	private static final int DONE = 0xFF55FF55;
	private static final int BTN_GREEN = 0xFF2E9E3E;
	private static final int BTN_GRAY = 0xFF4A4A5E;
	private static final int BTN_RED = 0xFFB03030;

	private Overlay() {}

	/** HUD layer: only draws while no screen is open (screens draw the overlay themselves, on top). */
	public static void renderHud(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (Minecraft.getInstance().screen == null) {
			render(g);
		}
	}

	/** Draws everything; called from the HUD and after every screen (inventory, chat, ...). */
	public static void render(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		Font font = mc.font;
		hits.clear();
		listBounds = null;
		statsBounds = null;
		int lh = font.lineHeight;
		drawPopups(g, font, lh, mc.getWindow().getGuiScaledWidth());

		CarryConfig cfg = CarryConfig.get();
		boolean preview = mc.screen instanceof SettingsScreen;
		boolean show = !CarryManager.entries.isEmpty() || cfg.alwaysShow || preview;
		if (show) {
			drawList(g, mc, font, lh, cfg);
			if (cfg.showStats) {
				drawStats(g, font, lh, cfg);
			}
		}
	}

	// ---------------------------------------------------------------- popup

	private static void drawPopups(GuiGraphicsExtractor g, Font font, int lh, int sw) {
		int py = 28;
		for (CarryManager.Pending p : new ArrayList<>(CarryManager.pending)) {
			String msg = "§b" + p.player() + " §fwants §e" + p.count() + " " + p.bossName() + " §fCarry";
			int w = font.width(msg) + 14;
			int x1 = (sw - w) / 2;
			g.fill(x1, py, x1 + w, py + lh + 6, ROW_BG);
			g.fill(x1, py, x1 + 2, py + lh + 6, ACCENT);
			g.text(font, msg, x1 + 8, py + 3, WHITE, true);
			int yesW = font.width("Yes") + 12;
			int noW = font.width("No") + 12;
			int bx = sw / 2 - (yesW + noW + 4) / 2;
			int by = py + lh + 9;
			button(g, font, bx, by, yesW, lh + 5, "Yes", BTN_GREEN, () -> CarryManager.accept(p));
			button(g, font, bx + yesW + 4, by, noW, lh + 5, "No", BTN_RED, () -> CarryManager.decline(p));
			py += lh * 2 + 22;
		}
	}

	// ----------------------------------------------------------- carry list

	private static void drawList(GuiGraphicsExtractor g, Minecraft mc, Font font, int lh, CarryConfig cfg) {
		List<CarryManager.Entry> list = new ArrayList<>(CarryManager.entries);
		int x = cfg.hudX;
		int y = cfg.hudY;
		int rowH = lh + 6;
		int plusW = font.width("+") + 8;
		int xW = font.width("x") + 8;

		if (list.isEmpty()) {
			String idle = "§7" + mc.getUser().getName() + " [0/0]";
			int w = font.width(idle) + 12;
			g.fill(x, y, x + w, y + rowH, ROW_BG);
			g.fill(x, y, x + 2, y + rowH, 0xFF666680);
			g.text(font, idle, x + 8, y + 3, WHITE, true);
			listBounds = new int[] {x, y, x + w, y + rowH};
			return;
		}

		List<String> texts = new ArrayList<>();
		int w = 0;
		for (CarryManager.Entry e : list) {
			String t = "§f" + e.player + " " + (e.done >= e.total ? "§a" : "§b") + "[" + e.done + "/" + e.total + "]";
			texts.add(t);
			w = Math.max(w, 8 + font.width(t) + 8 + plusW + 3 + xW + 4);
		}

		int ry = y;
		for (int i = 0; i < list.size(); i++) {
			CarryManager.Entry e = list.get(i);
			boolean full = e.done >= e.total;
			g.fill(x, ry, x + w, ry + rowH, ROW_BG);
			g.fill(x, ry, x + 2, ry + rowH, full ? DONE : ACCENT);
			g.text(font, texts.get(i), x + 8, ry + 3, WHITE, true);
			int xBtn = x + w - 4 - xW;
			button(g, font, xBtn, ry + 2, xW, rowH - 4, "x", BTN_GRAY, () -> CarryManager.remove(e));
			if (!full) {
				button(g, font, xBtn - 3 - plusW, ry + 2, plusW, rowH - 4, "+", BTN_GREEN, () -> CarryManager.increment(e));
			}
			ry += rowH + 2;
		}
		listBounds = new int[] {x, y, x + w, ry - 2};
	}

	// ---------------------------------------------------------- stats text

	private static void drawStats(GuiGraphicsExtractor g, Font font, int lh, CarryConfig cfg) {
		String title = "§e§l[SBC] §d§lCarries";
		String stats = "§b§lBosses  §7(" + CarryManager.lastTimeText() + ")  §f" + CarryManager.perHourText() + "/h";
		int x = cfg.statsX;
		int y = cfg.statsY;
		g.text(font, title, x, y, WHITE, true);
		g.text(font, stats, x, y + lh + 3, WHITE, true);
		int w = Math.max(font.width(title), font.width(stats));
		statsBounds = new int[] {x - 2, y - 2, x + w + 2, y + lh * 2 + 5};
	}

	// ------------------------------------------------------------- helpers

	private static void button(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, String label, int color, Runnable action) {
		g.fill(x, y, x + w, y + h, color);
		g.fill(x, y, x + w, y + 1, 0x40FFFFFF);
		g.text(font, label, x + (w - font.width(label)) / 2, y + (h - font.lineHeight) / 2 + 1, WHITE, false);
		hits.add(new Hit(x, y, x + w, y + h, action));
	}

	/** Returns true if a button was hit. */
	public static boolean click(double mx, double my) {
		for (Hit h : new ArrayList<>(hits)) {
			if (h.contains(mx, my)) {
				h.action().run();
				return true;
			}
		}
		return false;
	}

	private static boolean inside(int[] b, double mx, double my) {
		return b != null && mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3];
	}

	/** Which overlay is under the mouse: 1 = carry list, 2 = stats text, 0 = none. */
	public static int target(double mx, double my) {
		if (inside(listBounds, mx, my)) {
			return 1;
		}
		return inside(statsBounds, mx, my) ? 2 : 0;
	}
}
