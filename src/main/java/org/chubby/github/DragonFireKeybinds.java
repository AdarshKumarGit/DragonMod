package org.chubby.github;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Registers two keybinds for the custom dragon fire system.
 *
 *  FIRE_BREATH  – hold to sustain a continuous fire-breath stream
 *  FIRE_BALL    – tap to launch a single fireball projectile
 *
 * Wire up in your mod's client setup:
 *   @Mod.EventBusSubscriber(modid = YOUR_MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
 *   public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
 *       DragonFireKeybinds.register(e);
 *   }
 */
public final class DragonFireKeybinds {

    public static final KeyMapping FIRE_BREATH = new KeyMapping(
            "key.dragonmod.fire_breath",          // translation key
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,                          // default: R — change freely
            "key.categories.dragonmod"
    );

    public static final KeyMapping FIRE_BALL = new KeyMapping(
            "key.dragonmod.fire_ball",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,                          // default: F
            "key.categories.dragonmod"
    );

    /** Call once from RegisterKeyMappingsEvent. */
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(FIRE_BREATH);
        event.register(FIRE_BALL);
    }

    private DragonFireKeybinds() {}
}