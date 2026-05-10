package org.chubby.github;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side camera handler for dragon riding.
 *
 * Effects applied while mounted on an {@link EntityCustomDragon}:
 *   • Pitch tilt   – camera nods with the dragon's dive / climb angle.
 *   • Banking roll – camera tilts sideways from yaw velocity (turning).
 *   • Strafe roll  – camera leans into A/D key presses in flight.
 *   • Smooth recovery – all effects lerp to zero on dismount.
 */
@Mod.EventBusSubscriber(modid = DragonMod.MODID, value = Dist.CLIENT)
public final class DragonCameraHandler {

    // ── Smoothed state ────────────────────────────────────────────────────────
    private static float smoothPitch      = 0.0f;
    /** Roll from yaw velocity (turning). */
    private static float smoothRoll       = 0.0f;
    /** Roll from lateral strafe input. */
    private static float smoothStrafeRoll = 0.0f;
    private static float prevYBodyRot     = Float.NaN;

    // ── Pitch tuning ──────────────────────────────────────────────────────────
    private static final float PITCH_SCALE   = 0.40f;
    private static final float MAX_PITCH_DEG = 25.0f;
    private static final float PITCH_LERP    = 0.12f;

    // ── Banking roll (yaw-velocity) tuning ────────────────────────────────────
    private static final float MAX_ROLL_DEG  = 18.0f;
    /** Degrees of roll per degree/tick of yaw velocity. */
    private static final float ROLL_YAW_K   = 3.5f;
    private static final float ROLL_LERP    = 0.10f;

    // ── Strafe roll tuning ────────────────────────────────────────────────────
    /**
     * Max strafe roll in degrees.  12° is clearly visible without being
     * disorienting.  Raise toward 18° for a more aggressive lean.
     */
    private static final float MAX_STRAFE_ROLL = 12.0f;
    /**
     * Degrees of roll applied per unit of strafe axis (−1 … +1).
     * At 10° a full side-press leans the camera 10°.
     */
    private static final float STRAFE_ROLL_K  = 10.0f;
    /**
     * Slightly faster lerp than banking roll so the lean responds to key
     * presses immediately while still smoothing out tap-strafing jitter.
     */
    private static final float STRAFE_LERP    = 0.18f;

    // ── Recovery ──────────────────────────────────────────────────────────────
    private static final float RECOVER_LERP = 0.20f;

    private DragonCameraHandler() {}

    // ── Per-tick smoothing ────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc     = Minecraft.getInstance();
        Player    player = mc.player;
        if (player == null) return;

        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof EntityCustomDragon dragon)) {
            // Not riding — recover all effects to neutral.
            smoothPitch      = Mth.lerp(RECOVER_LERP, smoothPitch,      0.0f);
            smoothRoll       = Mth.lerp(RECOVER_LERP, smoothRoll,       0.0f);
            smoothStrafeRoll = Mth.lerp(RECOVER_LERP, smoothStrafeRoll, 0.0f);
            prevYBodyRot     = Float.NaN;
            return;
        }

        // ── Pitch ──────────────────────────────────────────────────────────────
        float targetPitch = Mth.clamp(dragon.getDragonPitch() * PITCH_SCALE,
                -MAX_PITCH_DEG, MAX_PITCH_DEG);
        smoothPitch = Mth.lerp(PITCH_LERP, smoothPitch, targetPitch);

        // ── Banking roll (from yaw velocity) ──────────────────────────────────
        float currentYaw = dragon.yBodyRot;
        if (!Float.isNaN(prevYBodyRot)) {
            float yawDelta = currentYaw - prevYBodyRot;
            while (yawDelta >  180.0f) yawDelta -= 360.0f;
            while (yawDelta < -180.0f) yawDelta += 360.0f;
            // Turning right (positive yaw delta) → lean left (negative roll).
            float targetRoll = Mth.clamp(-yawDelta * ROLL_YAW_K, -MAX_ROLL_DEG, MAX_ROLL_DEG);
            smoothRoll = Mth.lerp(ROLL_LERP, smoothRoll, targetRoll);
        }
        prevYBodyRot = currentYaw;

        // ── Strafe roll (from A/D key) ─────────────────────────────────────────
        // Only apply in flight — on the ground a camera lean during a ground-turn
        // feels wrong since the dragon slides rather than banks.
        // player.xxa: +1 = strafe right, −1 = strafe left.
        // We lean the camera INTO the strafe (press D → lean right = positive roll).
        float strafeTarget = 0.0f;
        if (dragon.isFlying() || dragon.isHovering()) {
            // Negate xxa so rightward strafe (+xxa) produces positive roll (lean right).
            strafeTarget = Mth.clamp(-player.xxa * STRAFE_ROLL_K,
                    -MAX_STRAFE_ROLL, MAX_STRAFE_ROLL);
        }
        smoothStrafeRoll = Mth.lerp(STRAFE_LERP, smoothStrafeRoll, strafeTarget);
    }

    // ── Per-frame angle injection ─────────────────────────────────────────────

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.player.getVehicle() instanceof EntityCustomDragon)) return;

        event.setPitch(event.getPitch() + smoothPitch);
        // Combine banking + strafe rolls on the same axis so they naturally
        // reinforce (banking right + strafing right = extra lean) or cancel
        // (banking left + strafing right = moderate feel).
        event.setRoll(event.getRoll() + smoothRoll + smoothStrafeRoll);
    }
}