package com.airz.spellwheel;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = SpellWheel.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SpellWheelKeyRegistration {
    public static final KeyMapping SELECT_KEY = new KeyMapping(
            "key.spellwheel.select", InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT, "key.categories.spellwheel");
    public static final KeyMapping ACTIVATE_KEY = new KeyMapping(
            "key.spellwheel.activate", InputConstants.Type.KEYSYM,
            InputConstants.KEY_R, "key.categories.spellwheel");

    private SpellWheelKeyRegistration() { }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SELECT_KEY);
        event.register(ACTIVATE_KEY);
    }
}
