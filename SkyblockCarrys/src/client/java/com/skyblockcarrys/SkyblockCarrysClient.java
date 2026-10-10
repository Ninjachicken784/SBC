package com.skyblockcarrys;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class SkyblockCarrysClient implements ClientModInitializer {
	public static final String MOD_ID = "skyblockcarrys";

	public static KeyMapping INTERACT_KEY;
	private static boolean openSettingsNextTick = false;

	@Override
	public void onInitializeClient() {
		CarryConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		INTERACT_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key." + MOD_ID + ".interact",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_LALT,
				category));

		// Overlay
		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "overlay"), Overlay::render);

		// Trade detection
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				CarryManager.onChat(message.getString());
			}
		});

		// Key + deferred screen opening (a command runs while chat is closing, so wait one tick)
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (openSettingsNextTick) {
				openSettingsNextTick = false;
				mc.setScreen(new SettingsScreen());
			}
			while (INTERACT_KEY.consumeClick()) {
				if (mc.screen == null && mc.player != null) {
					mc.setScreen(new InteractScreen());
				}
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			for (String name : new String[] {"sbc", "SBC", "skyblockcarry", "skyblockCarry", "SkyblockCarry"}) {
				dispatcher.register(buildCommand(name));
			}
		});
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand(String name) {
		return ClientCommands.literal(name)
				.executes(ctx -> {
					openSettingsNextTick = true;
					return 1;
				})
				// /sbc add <player> <count>   - manual fallback if trade detection misses something
				.then(ClientCommands.literal("add")
						.then(ClientCommands.argument("player", StringArgumentType.word())
								.then(ClientCommands.argument("count", IntegerArgumentType.integer(1, 999))
										.executes(ctx -> {
											String player = StringArgumentType.getString(ctx, "player");
											int count = IntegerArgumentType.getInteger(ctx, "count");
											CarryManager.accept(new CarryManager.Pending(player, count, "manual"));
											return 1;
										}))))
				// /sbc test <player> <coins>  - fakes a trade, e.g. /sbc test Bob 20m
				.then(ClientCommands.literal("test")
						.then(ClientCommands.argument("player", StringArgumentType.word())
								.then(ClientCommands.argument("coins", StringArgumentType.word())
										.executes(ctx -> {
											String player = StringArgumentType.getString(ctx, "player");
											long coins = CarryConfig.parseAmount(StringArgumentType.getString(ctx, "coins"));
											if (coins <= 0) {
												CarryManager.info("Invalid coin amount.");
												return 0;
											}
											CarryManager.offer(player, coins);
											return 1;
										}))))
				// /sbc debug  - prints trade/coin chat lines the parser did not understand
				.then(ClientCommands.literal("debug")
						.executes(ctx -> {
							CarryManager.debug = !CarryManager.debug;
							CarryManager.info("Debug " + (CarryManager.debug ? "on" : "off"));
							return 1;
						}))
				.then(ClientCommands.literal("clear")
						.executes(ctx -> {
							CarryManager.entries.clear();
							CarryManager.pending.clear();
							return 1;
						}));
	}
}
