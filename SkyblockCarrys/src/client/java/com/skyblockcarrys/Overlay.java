package com.skyblockcarrys;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Draws the popup + carry panel and records clickable regions. */
public final class Overlay {
	public record Hit(int x1, int y1, int x2, int y2, Runnable action) {
		boolean contains(double x, double y) {
			return x >= x1 && x <= x2 && y >= y1 && y <= y2;
		}
	}

	private static final List<Hit> hits = new ArrayList<>();
	/** x1,y1,x2,y2 of the draggable panel, or null when nothing is drawn. */
	public static int[] bounds = null;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int PANEL = 0xD00B0B14;
	private static final int BORDER = 0xFF34344F;
	private static final int PINK = 0xFFFF55FF;
	private static final int CYAN = 0xFF55FFFF;
	private static final int GREEN = 0xFF55FF55;
	private static final int TRACK = 0xFF2A2A3C;
	private static final int BTN_GREEN = 0xFF2E9E3E;
	private static final int BTN_RED = 0xFFB03030;
	private static final int BTN_GRAY = 0xFF4A4A5E;

	private Overlay() {}

	/** HUD layer: only draws while no screen is open (screens draw the overlay themselves, on top). */
	public static void renderHud(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (Minecraft.getInstance().screen == null) {
			render(g);
		}
	}

	/** Draws the overlay; called from the HUD and after every screen (inventory, chat, ...). */
	public static void render(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		Font font = mc.font;
		hits.clear();
		bounds = null;
		int lh = font.lineHeight;
		drawPopups(g, font, lh, mc.getWindow().getGuiScaledWidth());
		drawList(g, mc, font, lh);
	}

	private static void drawPopups(GuiGraphicsExtractor g, Font font, int lh, int sw) {
		int py = 28;
		for (CarryManager.Pending p : new ArrayList<>(CarryManager.pending)) {
			String msg = "§b" + p.player() + " §fwants §e" + p.count() + " " + p.bossName() + " §fCarry";
			int w = font.width(msg) + 20;
			int h = lh * 2 + 22;
			int x1 = (sw - w) / 2;
			panel(g, x1, py, x1 + w, py + h);
			g.text(font, msg, x1 + 10, py + 7, WHITE, true);
			int yesW = font.width("Yes") + 14;
			int noW = font.width("No") + 14;
			int bx = sw / 2 - (yesW + noW + 6) / 2;
			int by = py + lh + 13;
			button(g, font, bx, by, yesW, lh + 6, "Yes", BTN_GREEN, () -> CarryManager.accept(p));
			button(g, font, bx + yesW + 6, by, noW, lh + 6, "No", BTN_RED, () -> CarryManager.decline(p));
			py += h + 6;
		}
	}

	private static String rowText(CarryManager.Entry e) {
		return "§f" + e.player + " " + (e.done >= e.total ? "§a" : "§b") + "[" + e.done + "/" + e.total + "]";
	}

	private static void drawList(GuiGraphicsExtractor g, Minecraft mc, Font font, int lh) {
		CarryConfig cfg = CarryConfig.get();
		List<CarryManager.Entry> list = new ArrayList<>(CarryManager.entries);
		boolean preview = mc.screen instanceof SettingsScreen;
		if (list.isEmpty() && !(cfg.alwaysShow || preview)) {
			return;
		}

		String title = "§e§l[SBC] §d§lCarries";
		String stats = "§b§lBosses  §7(" + CarryManager.lastTimeText() + ")  §f" + CarryManager.perHourText() + "/h";
		String idle = "§7" + mc.getUser().getName() + " [0/0]";

		int rowH = lh + 9;
		int plusW = font.width("+") + 10;
		int xW = font.width("x") + 10;

		int w = Math.max(110, Math.max(font.width(title), cfg.showStats ? font.width(stats) : 0) + 12);
		List<String> texts = new ArrayList<>();
		for (CarryManager.Entry e : list) {
			String t = rowText(e);
			texts.add(t);
			w = Math.max(w, 6 + font.width(t) + 12 + plusW + 3 + xW + 6);
		}
		if (list.isEmpty()) {
			w = Math.max(w, font.width(idle) + 12);
		}

		int x = cfg.hudX;
		int y = cfg.hudY;
		int headerH = 6 + lh + (cfg.showStats ? lh + 3 : 0) + 6;
		int h = headerH + Math.max(1, list.size()) * rowH + 3;

		panel(g, x, y, x + w, y + h);
		g.text(font, title, x + 6, y + 6, WHITE, true);
		if (cfg.showStats) {
			g.text(font, stats, x + 6, y + 6 + lh + 3, WHITE, true);
		}
		int ry = y + headerH;
		g.fill(x + 4, ry - 3, x + w - 4, ry - 2, BORDER);

		if (list.isEmpty()) {
			g.text(font, idle, x + 6, ry + 1, WHITE, true);
			bar(g, x + 6, ry + lh + 5, w - 12, 0, 0);
		} else {
			for (int i = 0; i < list.size(); i++) {
				CarryManager.Entry e = list.get(i);
				int ty = ry + i * rowH;
				g.text(font, texts.get(i), x + 6, ty + 1, WHITE, true);
				int xBtn = x + w - 6 - xW;
				button(g, font, xBtn, ty - 1, xW, lh + 4, "x", BTN_GRAY, () -> CarryManager.remove(e));
				if (e.done < e.total) {
					button(g, font, xBtn - 3 - plusW, ty - 1, plusW, lh + 4, "+", BTN_GREEN, () -> CarryManager.increment(e));
				}
				bar(g, x + 6, ty + lh + 4, w - 12, e.done, e.total);
			}
		}
		bounds = new int[] {x, y, x + w, y + h};
	}

	private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int done, int total) {
		g.fill(x, y, x + w, y + 2, TRACK);
		if (total > 0 && done > 0) {
			int fw = Math.min(w, (int) ((long) w * done / total));
			g.fill(x, y, x + fw, y + 2, done >= total ? GREEN : CYAN);
		}
	}

	private static void panel(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
		g.fill(x1, y1, x2, y2, PANEL);
		g.outline(x1, y1, x2 - x1, y2 - y1, BORDER);
		int w = x2 - x1;
		for (int i = 0; i < w; i += 2) {
			float t = w <= 1 ? 0f : (float) i / (w - 1);
			g.fill(x1 + i, y1, Math.min(x2, x1 + i + 2), y1 + 2, lerp(PINK, CYAN, t));
		}
	}

	private static int lerp(int a, int b, float t) {
		int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int gr = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
		return 0xFF000000 | (r << 16) | (gr << 8) | bl;
	}

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

	public static boolean overList(double mx, double my) {
		return bounds != null && mx >= bounds[0] && mx <= bounds[2] && my >= bounds[1] && my <= bounds[3];
	}
}
