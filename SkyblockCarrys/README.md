# SkyblockCarrys (Fabric, Minecraft 26.1.2)

Standalone client mod (only needs Fabric API) for tracking Hypixel Skyblock slayer carries.

## Build
Needs JDK 25. Then: `./gradlew build` -> `build/libs/SkyblockCarrys-1.0.0.jar`
(IntelliJ: open the folder, run the `runClient` Gradle task to test.)

## Use
- `/sbc` or `/skyblockcarry`: settings. Set prices, add bosses (T4 Eman, T4 Blaze...), pick the one you are carrying now, drag the overlay.
- When someone trades you coins, a popup shows `Name wants 10 T4 Eman Carry [Yes] [No]`.
- Press **Left Alt** (rebindable) to free your mouse, click **[+]** after each boss, press it again or ESC to go back.
- Each [+] posts `[Name] [1/10]` to party chat (changeable to public/only-me in settings).

## Commands
- `/sbc test <name> <coins>`  fake a trade, e.g. `/sbc test Bob 20m`
- `/sbc add <name> <count>`   add a client by hand
- `/sbc debug`                print trade/coin chat lines to help fix detection
- `/sbc clear`

Trade detection uses two regexes in `config/skyblockcarrys.json` (`tradeRegex`, `coinsRegex`).
