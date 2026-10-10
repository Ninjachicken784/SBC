package com.skyblockcarrys;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Draws the popup + carry list and records clickable regions for the click screens. */
public final class Overlay {
	public record Hit(int x1, int y1, int x2, int y2, Runnable action) {
		boolean contains(double x, double y) {
			return x >= x1 && x <= x2 && y >= y1 && y <= y2;
		}
	}

	private static final List<Hit> hits = new ArrayList<>();
	/** x1,y1,x2,y2 of the draggable carry list, or null when nothing is drawn. */
	public static int[] bounds = null;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREEN = 0xFF55FF55;
	private static final int GRAY = 0xFFAAAAAA;
	private static final int BG = 0xB0000000;

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
		CarryConfig cfg = CarryConfig.get();
		hits.clear();
		bounds = null;

		int lh = font.lineHeight;
		int sw = mc.getWindow().getGuiScaledWidth();

		// ---- popups: "<name> wants N <boss> Carry   [Yes] [No]" ----
		int py = 28;
		for (CarryManager.Pending p : new ArrayList<>(CarryManager.pending)) {
			String msg = p.player() + " wants " + p.count() + " " + p.bossName() + " Carry";
			int yesW = font.width("Yes") + 10;
			int noW = font.width("No") + 10;
			int w = font.width(msg) + 12;
			int x1 = (sw - w) / 2;
			g.fill(x1, py, x1 + w, py + lh + 6, BG);
			g.text(font, msg, x1 + 6, py + 3, WHITE, true);
			int by = py + lh + 8;
			int bx = sw / 2 - (yesW + noW + 6) / 2;
			button(g, font, bx, by, yesW, lh + 6, "Yes", 0xFF2E7D32, () -> CarryManager.accept(p));
			button(g, font, bx + yesW + 6, by, noW, lh + 6, "No", 0xFF8B2020, () -> CarryManager.decline(p));
			py += lh * 2 + 20;
		}

		// ---- carry list ----
		boolean preview = mc.screen instanceof SettingsScreen;
		if (CarryManager.entries.isEmpty() && !(cfg.alwaysShow || preview)) {
			return;
		}

		int x = cfg.hudX;
		int y = cfg.hudY;
		int rowH = lh + 6;

		if (CarryManager.entries.isEmpty()) {
			String text = mc.getUser().getName() + " [0/0]";
			int w = font.width(text) + 10;
			g.fill(x, y, x + w, y + rowH, BG);
			g.text(font, text, x + 5, y + 3, GRAY, true);
			bounds = new int[] {x, y, x + w, y + rowH};
			return;
		}

		int maxX = x;
		int rowY = y;
		for (CarryManager.Entry e : new ArrayList<>(CarryManager.entries)) {
			String text = e.player + " [" + e.done + "/" + e.total + "]";
			boolean full = e.done >= e.total;
			int tw = font.width(text);
			int plusW = font.width("+") + 8;
			int xW = font.width("x") + 8;
			int w = 5 + tw + 4 + plusW + 3 + xW + 3;
			g.fill(x, rowY, x + w, rowY + rowH, BG);
			g.text(font, text, x + 5, rowY + 3, full ? GREEN : WHITE, true);
			int bx = x + 5 + tw + 4;
			if (!full) {
				button(g, font, bx, rowY + 1, plusW, rowH - 2, "+", 0xFF2E7D32, () -> CarryManager.increment(e));
			}
			button(g, font, bx + plusW + 3, rowY + 1, xW, rowH - 2, "x", 0xFF555555, () -> CarryManager.remove(e));
			maxX = Math.max(maxX, x + w);
			rowY += rowH + 2;
		}
		bounds = new int[] {x, y, maxX, rowY - 2};
	}

	private static void button(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, String label, int color, Runnable action) {
		g.fill(x, y, x + w, y + h, color);
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
