# Spell Wheel (NeoForge 1.21.1)

A client-side add-on for Spell Engine 1.10.9 that replaces its per-slot spell keybinds with a
single "hold to select" button.

## Controls
- Hold **Left Alt** to enter spell selection mode.
- Scroll to move the highlight across the spells on Spell Engine's own hotbar (passive spells and
  the item-use slot are skipped).
- Press **Left Click** (the *Cast Selected Spell* binding) to cast the highlighted spell. Hold it
  for charged / channelled casts and release to stop them. Instant spells fire once per press.

Both controls can be changed in Minecraft's Controls menu under the **Spell Wheel** category.

## How it works
- Spell Engine keeps drawing its own hotbar, wherever your HUD config puts it. Spell Wheel only
  draws a selection frame over the chosen slot, after every other HUD layer. If Iron's Spells 'n
  Spellbooks is installed the frame uses its slot highlight; otherwise the vanilla hotbar
  selection frame is used.
- Casting goes through the same Spell Engine calls its own keybinds use, including the HUD cast
  error (on cooldown, missing mana, ...). Mods that react to it, such as RunelessRPG's out-of-mana
  flash, behave exactly as with a keybind.
- While Spell Wheel is installed, Spell Engine's per-slot keybinds, the vanilla number-key
  deferral (`spell_hotbar_N_defer`) and the "use key casts the first spell" option are switched
  off in memory, so number keys select the normal hotbar again.
- A tiny mixin hides the empty key-cap bubbles Spell Engine would otherwise draw above each slot.

## Build
Requires Java 21 and Gradle 8.10+ with internet access.

Run `gradle build`. The resulting mod jar will be in `build/libs/`. The included GitHub Actions
workflow does the same on every push and uploads the jar as an artifact.

Spell Engine 1.10.9 is resolved from the Modrinth Maven repository as a compile dependency. In-game,
install the matching Spell Engine mod and its normal required dependencies.
