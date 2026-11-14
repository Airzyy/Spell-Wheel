# Spell Wheel (NeoForge 1.21.1)

A client-side add-on for Spell Engine 1.10.9.

## Controls
- Hold **Left Alt** to enter spell selection mode.
- Scroll the mouse wheel to move through the spells currently offered by Spell Engine.
- Press/hold **R** to activate the highlighted spell. Instant spells fire once per key press; charge/channel/cast spells continue to receive the normal Spell Engine held-key and release behavior.
- The selected spell's registry ID is shown above the hotbar while Left Alt is held.

Both controls can be changed in Minecraft's Controls menu under the Spell Wheel category.

## Build
Requires Java 21 and Gradle 8.10+ with internet access.

Run `gradle build`. The resulting mod jar will be in `build/libs/`.

Spell Engine 1.10.9 is resolved from the Modrinth Maven repository as a compile dependency. In-game, install the matching Spell Engine mod and its normal required dependencies.
