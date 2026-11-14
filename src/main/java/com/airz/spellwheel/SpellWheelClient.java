package com.airz.spellwheel;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.spell_engine.client.casting.ClientCastController;
import net.spell_engine.client.input.Keybindings;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;

import java.util.List;

@EventBusSubscriber(modid = SpellWheel.MOD_ID, value = Dist.CLIENT)
public final class SpellWheelClient {
    private static int selectedIndex = 0;
    private static boolean wasActivateDown = false;
    private static SpellCast.Option activatedOption = null;
    private static boolean legacyKeysDisabled = false;

    private SpellWheelClient() { }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !SpellWheelKeyRegistration.SELECT_KEY.isDown()) return;
        List<SpellCast.Option> options = currentOptions(mc);
        if (options.isEmpty()) return;
        double amount = event.getScrollDeltaY();
        if (amount == 0) return;
        selectedIndex = Math.floorMod(selectedIndex + (amount > 0 ? -1 : 1), options.size());
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.25F));
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
                || !(mc.player instanceof SpellCaster.Client caster)) {
            releaseIfNeeded();
            return;
        }
        List<SpellCast.Option> options = caster.getCastController().displayOptions();
        if (options.isEmpty()) {
            selectedIndex = 0;
            releaseIfNeeded();
            return;
        }
        selectedIndex = Math.floorMod(selectedIndex, options.size());
        boolean activateDown = SpellWheelKeyRegistration.ACTIVATE_KEY.isDown();
        if (activateDown) {
            SpellCast.Option current = options.get(selectedIndex);
            ClientCastController controller = caster.getCastController();
            if (activatedOption != null && !activatedOption.equals(current)) {
                controller.keyUp(activatedOption);
                activatedOption = null;
            }
            if (activatedOption == null) activatedOption = current;
            if (current.mode() != SpellCast.Mode.INSTANT || !wasActivateDown) {
                controller.keyHeld(current, true, false);
            }
        } else if (wasActivateDown && activatedOption != null) {
            caster.getCastController().keyUp(activatedOption);
            activatedOption = null;
        }
        wasActivateDown = activateDown;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !SpellWheelKeyRegistration.SELECT_KEY.isDown()
                || !(mc.player instanceof SpellCaster.Client caster)) return;
        List<SpellCast.Option> options = caster.getCastController().displayOptions();
        if (options.isEmpty()) return;
        SpellCast.Option selected = options.get(Math.floorMod(selectedIndex, options.size()));
        String name = selected.id().toString();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        event.getGuiGraphics().drawCenteredString(mc.font,
                Component.literal("‹  " + name + "  ›  [R to cast]"),
                width / 2, height - 54, 0xFFFFFFFF);
    }

    private static void disableLegacySpellKeys() {
        Keybindings.spell_hotbar_1.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_2.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_3.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_4.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_5.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_6.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_7.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_8.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
        Keybindings.spell_hotbar_9.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
    }

    private static List<SpellCast.Option> currentOptions(Minecraft mc) {
        if (mc.player instanceof SpellCaster.Client caster) return caster.getCastController().displayOptions();
        return List.of();
    }

    private static void releaseIfNeeded() {
        Minecraft mc = Minecraft.getInstance();
        if (activatedOption != null && mc.player instanceof SpellCaster.Client caster) {
            caster.getCastController().keyUp(activatedOption);
        }
        activatedOption = null;
        wasActivateDown = false;
    }
}
