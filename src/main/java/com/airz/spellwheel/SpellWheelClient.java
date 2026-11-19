package com.airz.spellwheel;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.casting.ClientCastController;
import net.spell_engine.client.gui.HudMessages;
import net.spell_engine.client.gui.HudRenderHelper;
import net.spell_engine.client.input.Keybindings;
import net.spell_engine.client.input.SpellHotbar;
import net.spell_engine.client.util.SpellRender;
import net.spell_engine.client.input.WrappedKeybinding.VanillaAlternative;
import net.spell_engine.client.util.Rect;
import net.spell_engine.config.ClientConfig;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;

import java.util.ArrayList;
import java.util.List;

/**
 * Spell Wheel: hold the select key, scroll to pick one of the spells on Spell Engine's own
 * hotbar, and press the activate button to cast it.
 *
 * <p>Spell Engine keeps drawing its hotbar (wherever the player placed it in the HUD config);
 * this class only draws a selection frame on top of the chosen slot and then drives the same
 * {@link ClientCastController} / {@link HudMessages} calls that Spell Engine's own keybinds use,
 * so cast errors (cooldown, missing mana, ...) show up exactly like they do with a keybind.
 */
@EventBusSubscriber(modid = SpellWheel.MOD_ID, value = Dist.CLIENT)
public final class SpellWheelClient {
    /** Spell Engine's hotbar texture is the vanilla one: 20 px per slot, 22 px tall. */
    private static final int SLOT_PITCH = 20;

    /** Slot frames from Iron's Spells 'n Spellbooks' icon sheet (selected frame is at 0,84, 22x22). */
    private static final ResourceLocation IRONS_ICONS =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/gui/icons.png");
    private static final int IRONS_SELECTED_U = 0;
    private static final int IRONS_SELECTED_V = 84;
    private static final int IRONS_FRAME_SIZE = 22;

    /** Fallback when Iron's Spells is not installed. */
    private static final ResourceLocation HOTBAR_SELECTION_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/hotbar_selection");

    /**
     * NeoForge stacks HUD layers about 200 depth units apart, and RenderGuiEvent.Post fires after
     * all of them with the depth back at 0, so anything drawn there at z=0 sits *behind* Spell
     * Engine's layer (the depth test hides it). Lift the frame far above the whole layer stack.
     * The GUI projection clips at about +/-10000, so stay well under that.
     */
    private static final float FRAME_Z = 8000.0F;

    /** Mirrors Spell Engine's SpellHotbar debounce so hold-to-cast / toggle-to-cast behave identically. */
    private enum Debounce { NONE, START, STOP }

    /** Position inside the list of selectable slots (not an index into the raw slot list). */
    private static int selected = 0;
    private static boolean activateHeld = false;
    private static SpellCast.Option activeOption = null;
    private static Debounce debounce = Debounce.NONE;
    private static boolean attemptReported = false;
    private static Boolean ironsLoaded = null;
    private static boolean originalKeysCaptured = false;
    private static InputConstants.Key[] originalSpellKeys;
    private static VanillaAlternative[] originalDeferrals;
    private static boolean originalUseKeyCasting;

    private SpellWheelClient() { }

    // ------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!SpellWheelConfig.isEnabled() || !matchesActivate(InputConstants.Type.MOUSE, event.getButton())) return;

        boolean selecting = isSelecting(Minecraft.getInstance());
        if (event.getAction() == InputConstants.PRESS) {
            if (selecting) {
                activateHeld = true;
                // Swallow the click so it does not also attack / break blocks.
                event.setCanceled(true);
            }
        } else if (event.getAction() == InputConstants.RELEASE) {
            activateHeld = false;
            if (selecting) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (!SpellWheelConfig.isEnabled() || !matchesActivate(InputConstants.Type.KEYSYM, event.getKey())) return;

        if (event.getAction() == InputConstants.PRESS) {
            if (isSelecting(Minecraft.getInstance())) activateHeld = true;
        } else if (event.getAction() == InputConstants.RELEASE) {
            activateHeld = false;
        }
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isSelecting(mc)) return;

        List<Integer> selectable = selectableSlots();
        if (selectable.isEmpty()) return;

        double amount = event.getScrollDeltaY();
        if (amount == 0) return;

        selected = Math.floorMod(selected + (amount > 0 ? -1 : 1), selectable.size());
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.15F));
        event.setCanceled(true);
    }

    private static boolean matchesActivate(InputConstants.Type type, int value) {
        InputConstants.Key bound = SpellWheelKeyRegistration.ACTIVATE_KEY.getKey();
        return bound.getType() == type && bound.getValue() == value;
    }

    private static boolean isSelecting(Minecraft mc) {
        return SpellWheelConfig.isEnabled() && mc.player != null && mc.level != null && mc.screen == null
                && SpellWheelKeyRegistration.SELECT_KEY.isDown();
    }

    // ------------------------------------------------------------------------------------
    // Casting
    // ------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (!SpellWheelConfig.isEnabled()) {
            activateHeld = false;
            releaseActiveSpell(mc);
            restoreSpellEngineKeyCasting();
            return;
        }

        neutralizeSpellEngineKeyCasting();

        if (!isSelecting(mc) || !(mc.player instanceof SpellCaster.Client caster)) {
            activateHeld = false;
            releaseActiveSpell(mc);
            return;
        }

        List<Integer> selectable = selectableSlots();
        if (selectable.isEmpty()) {
            activateHeld = false;
            releaseActiveSpell(mc);
            return;
        }
        selected = Math.floorMod(selected, selectable.size());

        if (!activateHeld || mc.player.isSpectator()) {
            releaseActiveSpell(mc);
            return;
        }

        // Lock the chosen spell on the initial press; scrolling mid-cast does not switch it.
        boolean newPress = activeOption == null;
        if (newPress) {
            activeOption = SpellHotbar.INSTANCE.slots.get(selectable.get(selected)).option();
            debounce = Debounce.NONE;
            attemptReported = false;
        }
        SpellCast.Option option = activeOption;

        // Instant spells fire once per press; charged / channelled / cast-time spells need
        // keyHeld every tick, exactly like Spell Engine's own hotbar handling.
        if (option.mode() == SpellCast.Mode.INSTANT && !newPress) return;

        ClientCastController controller = caster.getCastController();
        ClientCastController.Reaction reaction = controller.keyHeld(
                option, debounce != Debounce.STOP, debounce != Debounce.START);

        switch (reaction.type()) {
            case STARTED -> {
                debounce = Debounce.START;
                reportAttempt(reaction.attempt());
            }
            case STOPPED, RELEASED -> debounce = Debounce.STOP;
            default -> { }
        }
    }

    /**
     * Same as Spell Engine's SpellHotbar#displayAttempt: a failed attempt goes through
     * HudMessages#castAttemptError, which draws the "on cooldown" / "missing mana" message and
     * which other mods (e.g. RunelessRPG's mana flash) hook into.
     */
    private static void reportAttempt(SpellCast.Attempt attempt) {
        if (attempt == null || attemptReported) return;
        attemptReported = true;
        if (attempt.isFail()) {
            HudMessages.INSTANCE.castAttemptError(attempt);
        }
    }

    private static void releaseActiveSpell(Minecraft mc) {
        if (activeOption != null && mc.player instanceof SpellCaster.Client caster) {
            caster.getCastController().keyUp(activeOption);
        }
        activeOption = null;
        debounce = Debounce.NONE;
        attemptReported = false;
    }

    /**
     * Slots the wheel can land on: real spells on Spell Engine's hotbar. Indices refer to
     * SpellHotbar.INSTANCE.slots, i.e. exactly what Spell Engine draws (this includes the
     * item-use slot, which is skipped here but still occupies a position on the bar).
     */
    private static List<Integer> selectableSlots() {
        List<SpellHotbar.Slot> slots = SpellHotbar.INSTANCE.slots;
        List<Integer> result = new ArrayList<>(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            SpellHotbar.Slot slot = slots.get(i);
            if (slot == null) continue;
            SpellCast.Option option = slot.option();
            if (option != null && option.mode() != SpellCast.Mode.PASSIVE) {
                result.add(i);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------------------
    // Replace Spell Engine's own keybind casting
    // ------------------------------------------------------------------------------------

    /**
     * Spell Engine casts hotbar spells from its own keybinds, from the vanilla number keys
     * (spell_hotbar_N_defer) and from the use key (spellHotbarUseKey). Turn all of those off
     * so the select + activate buttons are the only way to cast from the hotbar.
     */
    private static void neutralizeSpellEngineKeyCasting() {
        ClientConfig config = SpellEngineClient.config;
        if (config != null) captureOriginalSpellEngineKeyCasting(config);
        KeyMapping[] keys = {
                Keybindings.spell_hotbar_1, Keybindings.spell_hotbar_2, Keybindings.spell_hotbar_3,
                Keybindings.spell_hotbar_4, Keybindings.spell_hotbar_5, Keybindings.spell_hotbar_6,
                Keybindings.spell_hotbar_7, Keybindings.spell_hotbar_8, Keybindings.spell_hotbar_9
        };
        boolean changed = false;
        for (KeyMapping key : keys) {
            if (!key.isUnbound()) {
                key.setKey(InputConstants.UNKNOWN);
                changed = true;
            }
        }
        if (changed) KeyMapping.resetMapping();

        if (config != null) {
            config.spell_hotbar_1_defer = VanillaAlternative.NONE;
            config.spell_hotbar_2_defer = VanillaAlternative.NONE;
            config.spell_hotbar_3_defer = VanillaAlternative.NONE;
            config.spell_hotbar_4_defer = VanillaAlternative.NONE;
            config.spell_hotbar_5_defer = VanillaAlternative.NONE;
            config.spell_hotbar_6_defer = VanillaAlternative.NONE;
            config.spell_hotbar_7_defer = VanillaAlternative.NONE;
            config.spell_hotbar_8_defer = VanillaAlternative.NONE;
            config.spell_hotbar_9_defer = VanillaAlternative.NONE;
            config.spellHotbarUseKey = false;
        }
    }

    private static void captureOriginalSpellEngineKeyCasting(ClientConfig config) {
        if (originalKeysCaptured) return;
        KeyMapping[] keys = spellEngineKeys();
        originalSpellKeys = new InputConstants.Key[keys.length];
        for (int i = 0; i < keys.length; i++) originalSpellKeys[i] = keys[i].getKey();
        originalDeferrals = new VanillaAlternative[] {
                config.spell_hotbar_1_defer, config.spell_hotbar_2_defer, config.spell_hotbar_3_defer,
                config.spell_hotbar_4_defer, config.spell_hotbar_5_defer, config.spell_hotbar_6_defer,
                config.spell_hotbar_7_defer, config.spell_hotbar_8_defer, config.spell_hotbar_9_defer
        };
        originalUseKeyCasting = config.spellHotbarUseKey;
        originalKeysCaptured = true;
    }

    private static void restoreSpellEngineKeyCasting() {
        if (!originalKeysCaptured) return;
        KeyMapping[] keys = spellEngineKeys();
        boolean changed = false;
        for (int i = 0; i < keys.length; i++) {
            if (!keys[i].getKey().equals(originalSpellKeys[i])) {
                keys[i].setKey(originalSpellKeys[i]);
                changed = true;
            }
        }
        if (changed) KeyMapping.resetMapping();

        ClientConfig config = SpellEngineClient.config;
        if (config != null) {
            config.spell_hotbar_1_defer = originalDeferrals[0];
            config.spell_hotbar_2_defer = originalDeferrals[1];
            config.spell_hotbar_3_defer = originalDeferrals[2];
            config.spell_hotbar_4_defer = originalDeferrals[3];
            config.spell_hotbar_5_defer = originalDeferrals[4];
            config.spell_hotbar_6_defer = originalDeferrals[5];
            config.spell_hotbar_7_defer = originalDeferrals[6];
            config.spell_hotbar_8_defer = originalDeferrals[7];
            config.spell_hotbar_9_defer = originalDeferrals[8];
            config.spellHotbarUseKey = originalUseKeyCasting;
        }
        originalKeysCaptured = false;
    }

    private static KeyMapping[] spellEngineKeys() {
        return new KeyMapping[] {
                Keybindings.spell_hotbar_1, Keybindings.spell_hotbar_2, Keybindings.spell_hotbar_3,
                Keybindings.spell_hotbar_4, Keybindings.spell_hotbar_5, Keybindings.spell_hotbar_6,
                Keybindings.spell_hotbar_7, Keybindings.spell_hotbar_8, Keybindings.spell_hotbar_9
        };
    }

    // ------------------------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------------------------

    /**
     * Draws only the selection frame, on top of Spell Engine's own hotbar. Rendered after every
     * HUD layer (RenderGuiEvent.Post) and lifted to FRAME_Z so the depth test keeps it in front of
     * the vanilla hotbar and Spell Engine's slots.
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!SpellWheelConfig.isEnabled() || mc.options.hideGui || !isSelecting(mc) || mc.player.isSpectator()) return;

        List<SpellHotbar.Slot> slots = SpellHotbar.INSTANCE.slots;
        if (slots.isEmpty()) return;

        List<Integer> selectable = selectableSlots();
        if (selectable.isEmpty()) return;
        int slotIndex = selectable.get(Math.floorMod(selected, selectable.size()));
        GuiGraphics graphics = event.getGuiGraphics();

        // Draw the highlight over Spell Engine's left-side bar when that bar was rendered this frame.
        Rect bar = HudRenderHelper.SpellHotBarWidget.lastRendered;
        if (bar != null) {
            float expectedWidth = SLOT_PITCH * slots.size();
            if (Math.abs((bar.bottomRight().x - bar.topLeft().x) - expectedWidth) <= 1.0F) {
                int x = (int) bar.topLeft().x + slotIndex * SLOT_PITCH;
                int y = (int) bar.topLeft().y;
                graphics.pose().pushPose();
                graphics.pose().translate(0.0F, 0.0F, FRAME_Z);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                if (isIronsLoaded()) {
                    graphics.blit(IRONS_ICONS, x, y,
                            IRONS_SELECTED_U, IRONS_SELECTED_V, IRONS_FRAME_SIZE, IRONS_FRAME_SIZE);
                } else {
                    graphics.blitSprite(HOTBAR_SELECTION_SPRITE, x - 1, y - 1, 24, 23);
                }
                RenderSystem.disableBlend();
                graphics.pose().popPose();
            }
        }

        if (SpellWheelConfig.showPreviewBelowCrosshair()) {
            renderPreviewBelowCrosshair(graphics, mc, slots, selectable, selected);
        }
    }

    private static void renderPreviewBelowCrosshair(GuiGraphics graphics, Minecraft mc,
                                                     List<SpellHotbar.Slot> slots,
                                                     List<Integer> selectable, int selectedIndex) {
        int count = selectable.size();
        int slotSize = 22;
        int gap = 2;
        int totalWidth = count * slotSize + Math.max(0, count - 1) * gap;
        int left = (mc.getWindow().getGuiScaledWidth() - totalWidth) / 2;
        int top = mc.getWindow().getGuiScaledHeight() / 2 + 14;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, FRAME_Z);
        for (int i = 0; i < count; i++) {
            int x = left + i * (slotSize + gap);
            if (i == Math.floorMod(selectedIndex, count)) {
                graphics.blitSprite(HOTBAR_SELECTION_SPRITE, x - 1, top - 1, 24, 23);
            }
            SpellHotbar.Slot slot = slots.get(selectable.get(i));
            ItemStack itemIcon = slot.itemStack();
            if (itemIcon != null && !itemIcon.isEmpty()) {
                graphics.renderItem(itemIcon, x + 3, top + 3);
            } else if (slot.spell() != null) {
                ResourceLocation spellId = slot.spell().getKey().location();
                ResourceLocation iconTexture = SpellRender.iconTexture(spellId);
                graphics.blit(iconTexture, x + 3, top + 3, 0, 0, 16, 16, 16, 16);
            }
        }
        graphics.pose().popPose();
    }

    private static boolean isIronsLoaded() {
        if (ironsLoaded == null) {
            ironsLoaded = ModList.get().isLoaded("irons_spellbooks");
        }
        return ironsLoaded;
    }
}
