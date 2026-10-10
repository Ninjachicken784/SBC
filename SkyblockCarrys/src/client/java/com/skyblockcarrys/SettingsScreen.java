package com.skyblockcarrys;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** /sbc settings: boss prices, active boss, overlay mode, chat mode. Drag the overlay to move it. */
public class SettingsScreen extends Screen {
	private static final int PER_PAGE = 5;

	private int page = 0;
	private boolean dragging = false;
	private Button hudButton;
	private Button chatButton;

	public SettingsScreen() {
		super(Component.literal("SkyblockCarrys"));
	}

	@Override
	protected void init() {
		CarryConfig cfg = CarryConfig.get();
		List<CarryConfig.Boss> bosses = cfg.bosses;
		int pages = Math.max(1, (bosses.size() + PER_PAGE - 1) / PER_PAGE);
		page = Math.min(page, pages - 1);

		int left = this.width / 2 - 160;
		int top = 44;

		for (int k = 0; k < PER_PAGE; k++) {
			int idx = page * PER_PAGE + k;
			if (idx >= bosses.size()) {
				break;
			}
			CarryConfig.Boss boss = bosses.get(idx);
			int y = top + k * 24;
			boolean active = boss.name.equalsIgnoreCase(cfg.activeBoss);

			this.addRenderableWidget(Button.builder(Component.literal(active ? "Active" : "Select"), b -> {
				cfg.activeBoss = boss.name;
				this.rebuildWidgets();
			}).bounds(left, y, 56, 20).build());

			EditBox name = new EditBox(this.font, left + 60, y, 130, 20, Component.literal("Boss name"));
			name.setMaxLength(32);
			name.setValue(boss.name);
			name.setResponder(v -> {
				if (boss.name.equalsIgnoreCase(cfg.activeBoss)) {
					cfg.activeBoss = v;
				}
				boss.name = v;
			});
			this.addRenderableWidget(name);

			EditBox price = new EditBox(this.font, left + 194, y, 70, 20, Component.literal("Price"));
			price.setMaxLength(16);
			price.setValue(CarryConfig.formatAmount(boss.price));
			price.setResponder(v -> {
				long p = CarryConfig.parseAmount(v);
				if (p > 0) {
					boss.price = p;
				}
			});
			this.addRenderableWidget(price);

			this.addRenderableWidget(Button.builder(Component.literal("X"), b -> {
				if (bosses.size() > 1) {
					bosses.remove(boss);
					if (boss.name.equalsIgnoreCase(cfg.activeBoss)) {
						cfg.activeBoss = bosses.get(0).name;
					}
					this.rebuildWidgets();
				}
			}).bounds(left + 268, y, 20, 20).build());
		}

		int y2 = top + PER_PAGE * 24 + 14;
		this.addRenderableWidget(Button.builder(Component.literal("Add boss"), b -> {
			bosses.add(new CarryConfig.Boss("New Boss", 1_000_000L));
			page = (bosses.size() - 1) / PER_PAGE;
			this.rebuildWidgets();
		}).bounds(left, y2, 90, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			if (page > 0) {
				page--;
				this.rebuildWidgets();
			}
		}).bounds(left + 96, y2, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			if (page < pages - 1) {
				page++;
				this.rebuildWidgets();
			}
		}).bounds(left + 120, y2, 20, 20).build());

		hudButton = Button.builder(hudLabel(), b -> {
			cfg.alwaysShow = !cfg.alwaysShow;
			b.setMessage(hudLabel());
		}).bounds(left, y2 + 24, 144, 20).build();
		this.addRenderableWidget(hudButton);

		chatButton = Button.builder(chatLabel(), b -> {
			CarryConfig.ChatMode[] modes = CarryConfig.ChatMode.values();
			cfg.chatMode = modes[(cfg.chatMode.ordinal() + 1) % modes.length];
			b.setMessage(chatLabel());
		}).bounds(left + 148, y2 + 24, 140, 20).build();
		this.addRenderableWidget(chatButton);

		this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
				.bounds(this.width / 2 - 50, y2 + 52, 100, 20).build());
	}

	private Component hudLabel() {
		return Component.literal("Overlay: " + (CarryConfig.get().alwaysShow ? "Always on" : "Only when carrying"));
	}

	private Component chatLabel() {
		return Component.literal("Chat: " + switch (CarryConfig.get().chatMode) {
			case PARTY -> "Party (/pc)";
			case PUBLIC -> "Public";
			case LOCAL -> "Only me";
		});
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int left = this.width / 2 - 160;
		graphics.text(this.font, "SkyblockCarrys - set price per boss, pick what you're carrying now",
				this.width / 2 - this.font.width("SkyblockCarrys - set price per boss, pick what you're carrying now") / 2, 14, 0xFFFFFFFF, true);
		graphics.text(this.font, "Boss", left + 60, 31, 0xFFAAAAAA, false);
		graphics.text(this.font, "Price (2m, 500k...)", left + 194, 31, 0xFFAAAAAA, false);
		int pages = Math.max(1, (CarryConfig.get().bosses.size() + PER_PAGE - 1) / PER_PAGE);
		graphics.text(this.font, "Page " + (page + 1) + "/" + pages, left + 146, top() + PER_PAGE * 24 + 20, 0xFFAAAAAA, false);
		String hint = "Tip: drag the overlay anywhere on screen to move it.";
		graphics.text(this.font, hint, (this.width - this.font.width(hint)) / 2, this.height - 14, 0xFF888888, false);
	}

	private int top() {
		return 44;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() == 0 && Overlay.overList(event.x(), event.y())) {
			dragging = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			CarryConfig cfg = CarryConfig.get();
			cfg.hudX = Math.max(0, Math.min(this.width - 20, cfg.hudX + (int) Math.round(dx)));
			cfg.hudY = Math.max(0, Math.min(this.height - 12, cfg.hudY + (int) Math.round(dy)));
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging) {
			dragging = false;
			CarryConfig.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		CarryConfig.save();
		super.onClose();
	}
}
