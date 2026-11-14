package com.airz.spellwheel;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = SpellWheel.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SpellWheelKeyRegistration {
    private static final String CATEGORY = "key.categories.spellwheel";

    /** Hold to enter spell selection mode. */
    public static final KeyMapping SELECT_KEY = new KeyMapping(
            "key.spellwheel.select", InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT, CATEGORY);

    /**
     * Casts the selected spell. Defaults to the left mouse button (button 0).
     *
     * <p>Spell Wheel reads this binding straight from the input events (it is only meaningful
     * while the select key is held), so it uses the GUI conflict context: the Controls menu then
     * does not flag the default left click as a conflict with Attack/Destroy.
     */
    public static final KeyMapping ACTIVATE_KEY = new KeyMapping(
            "key.spellwheel.activate", KeyConflictContext.GUI,
            InputConstants.Type.MOUSE.getOrCreate(0), CATEGORY);

    private SpellWheelKeyRegistration() { }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SELECT_KEY);
        event.register(ACTIVATE_KEY);
    }
}
