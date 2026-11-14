# Spell Wheel (NeoForge 1.21.1)

A client-side add-on for Spell Engine 1.10.9 that replaces the normal spell hotkeys with a mouse-driven spell selector.

## Controls
- Hold **Left Alt** to show your available spells in a row of vanilla Minecraft hotbar slots.
- Scroll the mouse wheel to change the highlighted spell. The selection uses Minecraft's normal hotbar highlight.
- Hold **Left Click** to cast the highlighted spell. Releasing the button sends the normal Spell Engine key-up event, so charged/channelled spells can finish or stop according to Spell Engine's casting settings.
- While the selector is open, left-click is reserved for spell casting rather than attacking blocks or entities.

The selector displays spell icons using Spell Engine's normal `textures/spell/<spell_id>.png` resource convention. Both controls can be changed in Minecraft's Controls menu under the Spell Wheel category.

## Build
Requires Java 21 and Gradle 8.10+ with internet access.

Run `gradle build`. The resulting mod jar will be in `build/libs/`.

Spell Engine 1.10.9 is resolved from the Modrinth Maven repository as a compile dependency. In-game, install the matching Spell Engine mod and its normal required dependencies.
