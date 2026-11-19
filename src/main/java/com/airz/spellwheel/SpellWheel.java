package com.airz.spellwheel;

import net.neoforged.fml.common.Mod;

@Mod(SpellWheel.MOD_ID)
public final class SpellWheel {
    public static final String MOD_ID = "spellwheel";
    public SpellWheel() {
        SpellWheelConfig.register();
    }
}
