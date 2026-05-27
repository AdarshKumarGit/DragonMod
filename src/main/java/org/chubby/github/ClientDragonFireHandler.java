package org.chubby.github;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Client-side tick handler that polls the two dragon-fire keybinds and sends
 * PacketDragonFireInput over DragonMod.NETWORK_WRAPPER when the mode changes.
 *
 * Registered automatically via @Mod.EventBusSubscriber on the FORGE event bus.
 *
 * Fire modes sent:
 *   0  keys released     -> stop fire
 *   1  FIRE_BREATH held  -> sustain stream (sent every tick while held)
 *   2  FIRE_BALL tapped  -> single fireball (sent once per key-press)
 */
@Mod.EventBusSubscriber(modid = DragonMod.MODID, value = Dist.CLIENT)
public final class ClientDragonFireHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Tracks the last mode sent so we avoid sending the same packet every tick.
     * Only changes to 0/1 are de-duped; mode 2 (fireball) is always a one-shot.
     */
    private static byte lastSentMode = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        // Act only at the END phase so input is fully processed first
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc     = Minecraft.getInstance();
        Player    player = mc.player;

        // No player or a screen is open -> stop any ongoing fire immediately
        if (player == null || mc.screen != null) {
            stopFire(player);
            return;
        }

        // MOUNT_DRAGON (V): mount nearest owned dragon when on foot, dismount when riding.
        // Checked before the vehicle guard so it works in both states.
        if (DragonFireKeybinds.MOUNT_DRAGON.consumeClick()) {
            byte action = (player.getVehicle() instanceof EntityCustomDragon)
                    ? PacketDragonControl.DISMOUNT
                    : PacketDragonControl.MOUNT;
            DragonMod.NETWORK_WRAPPER.sendToServer(new PacketDragonControl(action));
            return;
        }

        // DRAGON_WHISTLE (Z): call the dragon to you.  Works on foot or mounted.
        if (DragonFireKeybinds.DRAGON_WHISTLE.consumeClick()) {
            DragonMod.NETWORK_WRAPPER.sendToServer(
                    new PacketDragonControl(PacketDragonControl.WHISTLE));
            mc.player.displayClientMessage(
                    Component.literal("Dragon: Whistling..."), true);
            return;
        }

        // Must be riding a custom dragon for all remaining keybinds.
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof EntityCustomDragon dragon)) {
            stopFire(player);
            return;
        }

        // SIT_TOGGLE (G): works for all stages including baby — checked
        // BEFORE the fire-only baby guard, otherwise hatchling riders could
        // never toggle sit/follow.
        if (DragonFireKeybinds.SIT_TOGGLE.consumeClick()) {
            DragonMod.NETWORK_WRAPPER.sendToServer(
                    new PacketDragonControl(PacketDragonControl.SIT_TOGGLE));
            // Action-bar popup: show what the dragon will do after toggle.
            // command 1 = currently sitting → will start following.
            // command 0/2 = currently following/sleeping → will sit.
            String nextState = (dragon.getCommand() == 1) ? "Following" : "Sitting";
            mc.player.displayClientMessage(
                    Component.literal("Dragon: " + nextState), true);
            return;
        }

        // DRAGON_BITE (B): trigger a melee bite.
        // Routed through PacketDragonFireInput (mode 3) — the same confirmed-working
        // channel as fire — so it lands in handleFireInput() on EntityCustomDragon and
        // is handled entirely server-side, independent of player look direction.
        if (DragonFireKeybinds.DRAGON_BITE.consumeClick()) {
            DragonMod.NETWORK_WRAPPER.sendToServer(
                    new PacketDragonFireInput(dragon.getId(), (byte) 3));
            return;
        }

        // Babies can't breathe fire
        if (dragon.getDragonStage() < 2) {
            stopFire(player);
            return;
        }

        // FIRE_BALL: consumeClick() fires exactly once per physical key-press.
        // Handled first so it can't be clobbered by breath-mode logic in the
        // same tick. We return early without touching lastSentMode so the next
        // tick settles naturally on breath-or-none.
        if (DragonFireKeybinds.FIRE_BALL.consumeClick()) {
            DragonMod.NETWORK_WRAPPER.sendToServer(
                    new PacketDragonFireInput(dragon.getId(), (byte) 2));
            return;
        }

        // FIRE_BREATH: isDown() is true every tick the key is physically held.
        byte desiredMode = DragonFireKeybinds.FIRE_BREATH.isDown() ? (byte) 1 : (byte) 0;
        if (DragonFireKeybinds.FIRE_BREATH.isDown()) {
            LOGGER.info("[DragonFire] CLIENT: FIRE_BREATH isDown=true, lastSentMode={}, desiredMode={}",
                    lastSentMode, desiredMode);
        }
        sendIfChanged(desiredMode, dragon);
    }

    // Helpers

    /** Sends mode=0 (stop) if we haven't already. Safe to call when player is null. */
    private static void stopFire(Player player) {
        if (lastSentMode == 0) return;
        if (player == null) { lastSentMode = 0; return; }

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof EntityCustomDragon dragon) {
            DragonMod.NETWORK_WRAPPER.sendToServer(
                    new PacketDragonFireInput(dragon.getId(), (byte) 0));
        }
        lastSentMode = 0;
    }

    /** Sends a packet only when the desired mode differs from what was last sent. */
    private static void sendIfChanged(byte mode, EntityCustomDragon dragon) {
        if (mode == lastSentMode) return;
        LOGGER.info("[DragonFire] CLIENT: sending packet mode={} (was {})", mode, lastSentMode);
        DragonMod.NETWORK_WRAPPER.sendToServer(
                new PacketDragonFireInput(dragon.getId(), mode));
        lastSentMode = mode;
    }
}