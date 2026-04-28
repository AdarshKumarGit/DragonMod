package org.chubby.github;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Registers keybinds for dragon control.
 *
 *  FIRE_BREATH      – hold to sustain a continuous fire-breath stream (R)
 *  FIRE_BALL        – tap to launch a single fireball projectile (F)
 *  SIT_TOGGLE       – while mounted, toggles dragon between sit and follow (G)
 *  MOUNT_DRAGON     – dismount when riding; mount nearest tamed dragon when on foot (V)
 */
public final class DragonFireKeybinds {

    public static final KeyMapping FIRE_BREATH = new KeyMapping(
            "key.dragonmod.fire_breath",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.dragonmod"
    );

    public static final KeyMapping FIRE_BALL = new KeyMapping(
            "key.dragonmod.fire_ball",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.categories.dragonmod"
    );

    /** Toggle sit ↔ follow while mounted (no staff required). */
    public static final KeyMapping SIT_TOGGLE = new KeyMapping(
            "key.dragonmod.sit_toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.dragonmod"
    );

    /** Mount the nearest tamed dragon when on foot, or dismount when riding. */
    public static final KeyMapping MOUNT_DRAGON = new KeyMapping(
            "key.dragonmod.mount_dragon",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.dragonmod"
    );

    /** Call once from RegisterKeyMappingsEvent. */
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(FIRE_BREATH);
        event.register(FIRE_BALL);
        event.register(SIT_TOGGLE);
        event.register(MOUNT_DRAGON);
    }

    private DragonFireKeybinds() {}
}