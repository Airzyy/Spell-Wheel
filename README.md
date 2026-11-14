# Spell Wheel (NeoForge 1.21.1)

A client-side add-on for Spell Engine 1.10.9 that changes how its existing spell hotbar is selected and activated.

## Controls
- Hold **Left Alt** to enter spell selection mode.
- Scroll to move the selection across the original Spell Engine hotbar slots.
- Spell Engine continues to draw its own slot backgrounds, spell icons, cooldown overlays, and key labels. Spell Wheel only adds the selected-slot highlight on top of the original HUD.
- Hold **Left Click** to activate the selected spell. Release Left Click to stop charged or channelled casts.

Both controls can be changed in Minecraft's Controls menu under the Spell Wheel category.

## Build
Requires Java 21 and Gradle 8.10+ with internet access.

Run `gradle build`. The resulting mod jar will be in `build/libs/`.

Spell Engine 1.10.9 is resolved from the Modrinth Maven repository as a compile dependency. In-game, install the matching Spell Engine mod and its normal required dependencies.
