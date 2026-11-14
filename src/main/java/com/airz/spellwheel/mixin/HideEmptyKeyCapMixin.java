package com.airz.spellwheel.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.spell_engine.client.gui.Drawable;
import net.spell_engine.client.gui.HudRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spell Wheel turns off Spell Engine's per-slot keybinds, so every slot's key label is empty.
 * Spell Engine would still draw an empty key-cap bubble above each slot; skip drawing those.
 * Slots that do have a label or icon (if some other mod provides one) are drawn as usual.
 */
@Mixin(value = HudRenderHelper.SpellHotBarWidget.class, remap = false)
public abstract class HideEmptyKeyCapMixin {
    @Inject(method = "drawKeybinding", at = @At("HEAD"), cancellable = true, require = 0)
    private static void spellwheel$skipEmptyKeyCap(
            GuiGraphics context, Font textRenderer,
            HudRenderHelper.SpellHotBarWidget.KeyBindingViewModel keybinding,
            int x, int y, Drawable.Anchor horizontalAnchor, Drawable.Anchor verticalAnchor,
            CallbackInfo ci) {
        if (keybinding.drawable() == null && keybinding.label().isEmpty()) {
            ci.cancel();
        }
    }
}
