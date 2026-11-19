package com.airz.spellwheel;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side settings for Spell Wheel. */
public final class SpellWheelConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.BooleanValue SHOW_PREVIEW_BELOW_CROSSHAIR;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("general");
        ENABLED = BUILDER
                .comment("Enable Spell Wheel and replace Spell Engine's individual spell keybinds.")
                .define("enabled", true);
        SHOW_PREVIEW_BELOW_CROSSHAIR = BUILDER
                .comment("Show a compact spell-slot preview below the crosshair while selecting.")
                .define("showPreviewBelowCrosshair", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private SpellWheelConfig() { }

    public static void register() {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static boolean showPreviewBelowCrosshair() {
        return SHOW_PREVIEW_BELOW_CROSSHAIR.get();
    }
}
