package com.airz.spellwheel;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.spell_engine.client.casting.ClientCastController;
import net.spell_engine.client.input.Keybindings;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;

import java.util.List;

@EventBusSubscriber(modid = SpellWheel.MOD_ID, value = Dist.CLIENT)
public final class SpellWheelClient {
    private static final int HOTBAR_SLOTS = 9;
    private static final ResourceLocation HOTBAR_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation HOTBAR_SELECTION_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/hotbar_selection");

    private static int selectedIndex = 0;
    private static boolean mouseLeftDown = false;
    private static SpellCast.Option activatedOption = null;
    private static boolean legacyKeysDisabled = false;

    private SpellWheelClient() { }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != 0) return;

        if (event.getAction() == InputConstants.PRESS) {
            mouseLeftDown = true;
        } else if (event.getAction() == InputConstants.RELEASE) {
            mouseLeftDown = false;
        }

        // While selecting spells, left-click belongs to the spell caster, not block/entity attack.
        if (SpellWheelKeyRegistration.SELECT_KEY.isDown()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null
                || !SpellWheelKeyRegistration.SELECT_KEY.isDown()) return;

        List<SpellCast.Option> options = currentOptions(mc);
        if (options.isEmpty()) return;

        double amount = event.getScrollDeltaY();
        if (amount == 0) return;

        selectedIndex = Math.floorMod(selectedIndex + (amount > 0 ? -1 : 1), options.size());
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.15F));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (!legacyKeysDisabled) {
            disableLegacySpellKeys();
            legacyKeysDisabled = true;
        }

        if (mc.player == null || mc.level == null || mc.screen != null
                || !(mc.player instanceof SpellCaster.Client caster)
                || !SpellWheelKeyRegistration.SELECT_KEY.isDown()) {
            releaseActiveSpell(mc);
            return;
        }

        List<SpellCast.Option> options = caster.getCastController().displayOptions();
        if (options.isEmpty()) {
            selectedIndex = 0;
            releaseActiveSpell(mc);
            return;
        }

        selectedIndex = Math.floorMod(selectedIndex, options.size());

        // A spell is selected on the left-click press and remains the active cast until release,
        // even if the wheel is scrolled while the mouse button is held.
        if (mouseLeftDown && activatedOption == null) {
            SpellCast.Option selected = options.get(selectedIndex);
            if (selected.mode() != SpellCast.Mode.PASSIVE) {
                ClientCastController controller = caster.getCastController();
                activatedOption = selected;
                controller.keyHeld(selected, true, false);
            }
        } else if (!mouseLeftDown) {
            releaseActiveSpell(mc);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.options.hideGui
                || !SpellWheelKeyRegistration.SELECT_KEY.isDown()
                || !(mc.player instanceof SpellCaster.Client caster)) return;

        List<SpellCast.Option> options = caster.getCastController().displayOptions();
        if (options.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int barX = screenWidth / 2 - 91;
        int barY = screenHeight - 22;

        // Use the actual vanilla hotbar sprites so the selector matches Minecraft's HUD.
        graphics.blitSprite(HOTBAR_SPRITE, barX, barY, 182, 22);

        // Cover the normal item icons while this spell selector is active.
        for (int slot = 0; slot < HOTBAR_SLOTS; slot++) {
            graphics.fill(barX + 3 + slot * 20, barY + 3,
                    barX + 19 + slot * 20, barY + 19, 0xAA000000);
        }

        int visibleCount = Math.min(HOTBAR_SLOTS, options.size());
        int firstVisible = Math.max(0, Math.min(
                selectedIndex - visibleCount / 2, options.size() - visibleCount));
        int selectedVisibleSlot = selectedIndex - firstVisible;

        for (int slot = 0; slot < visibleCount; slot++) {
            SpellCast.Option option = options.get(firstVisible + slot);
            ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
                    option.id().getNamespace(),
                    "textures/spell/" + option.id().getPath() + ".png");

            graphics.blit(icon, barX + 3 + slot * 20, barY + 3,
                    0, 0, 16, 16, 16, 16);
        }

        // Vanilla's selected-slot frame, positioned over the currently selected spell.
        graphics.blitSprite(HOTBAR_SELECTION_SPRITE,
                barX - 1 + selectedVisibleSlot * 20, barY - 1, 24, 23);
    }

    private static void releaseActiveSpell(Minecraft mc) {
        if (activatedOption != null && mc.player instanceof SpellCaster.Client caster) {
            caster.getCastController().keyUp(activatedOption);
        }
        activatedOption = null;
    }

    private static void disableLegacySpellKeys() {
        Keybindings.spell_hotbar_1.setKey(InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_2.setKey(InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_3.setKey(InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_4.setKey(InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_5.setKey(InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_6.setKey(InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_7.setKey(InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_8.setKey(InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_9.setKey(InputConstants.UNKNOWN);
    }

    private static List<SpellCast.Option> currentOptions(Minecraft mc) {
        if (mc.player instanceof SpellCaster.Client caster) {
            return caster.getCastController().displayOptions();
        }
        return List.of();
    }
}
