package com.skyblockcarrys;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

/**
 * Transparent screen that frees the mouse so you can click [+], [x], Yes and No on the HUD.
 * Open with the keybind (default Left Alt), close with the same key or ESC.
 */
public class InteractScreen extends Screen {
	public InteractScreen() {
		super(Component.literal("SkyblockCarrys"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// Intentionally no super call: no dimmed background, the HUD overlay underneath stays visible.
		String hint = "Click [+] / Yes / No  -  press " + SkyblockCarrysClient.INTERACT_KEY.getTranslatedKeyMessage().getString() + " or ESC to return";
		graphics.text(this.font, hint, (this.width - this.font.width(hint)) / 2, this.height - 40, 0xFFAAAAAA, true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && Overlay.click(event.x(), event.y())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (SkyblockCarrysClient.INTERACT_KEY.matches(event)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}
}
