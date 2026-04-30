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
 * <h3>What it does</h3>
 * <ul>
 *   <li><b>Pitch tilt</b> – camera nods forward/back with the dragon's dive or climb
 *       angle, giving a visceral sense of the dragon's orientation in the air.</li>
 *   <li><b>Banking roll</b> – camera tilts sideways proportional to the dragon's
 *       yaw velocity, so sharp turns feel like actual flight banking manoeuvres.</li>
 *   <li><b>Smooth recovery</b> – both effects lerp back to neutral the instant the
 *       player dismounts so there is no jarring camera snap.</li>
 * </ul>
 *
 * <h3>Registration</h3>
 * Discovered automatically via {@code @Mod.EventBusSubscriber} — no manual
 * registration needed.
 */
@Mod.EventBusSubscriber(modid = DragonMod.MODID, value = Dist.CLIENT)
public final class DragonCameraHandler {

    // ── Smoothed state (updated every client tick) ────────────────────────────

    /** Smoothed camera pitch offset (degrees). */
    private static float smoothPitch   = 0.0f;
    /** Smoothed camera roll (degrees). Positive = tilt right. */
    private static float smoothRoll    = 0.0f;
    /** Dragon body yaw from the previous tick — used to derive yaw velocity. */
    private static float prevYBodyRot  = Float.NaN;

    // ── Tuning constants ──────────────────────────────────────────────────────

    /**
     * Fraction of the dragon's pitch angle that feeds into the camera each tick.
     * 0.40 = 40 % of the dragon's pitch angle is echoed to the camera.
     * Raise toward 1.0 for a first-person "locked to the back" feel; lower for
     * a subtler cinematic nudge.
     */
    private static final float PITCH_SCALE   = 0.40f;
    /** Hard ceiling on the pitch contribution (degrees). */
    private static final float MAX_PITCH_DEG = 25.0f;
    /** Hard ceiling on the banking roll (degrees). */
    private static final float MAX_ROLL_DEG  = 18.0f;
    /**
     * Converts yaw-delta (degrees/tick) to roll (degrees).
     * Higher = more aggressive banking; 3.5 gives a noticeable but not nauseating
     * effect even during fast turns.
     */
    private static final float ROLL_YAW_K   = 3.5f;

    // Lerp factors (per tick, toward target).  Smaller = slower / smoother.
    private static final float PITCH_LERP    = 0.12f;
    private static final float ROLL_LERP     = 0.10f;
    /** Recovery speed when not riding (lerp toward 0 each tick). */
    private static final float RECOVER_LERP  = 0.20f;

    // ── Constructor ───────────────────────────────────────────────────────────

    private DragonCameraHandler() {}

    // ── Per-tick smoothing ────────────────────────────────────────────────────

    /**
     * Every client tick: read the dragon's current state and advance the smoothed
     * pitch and roll one step toward their targets.  This decouples the camera
     * physics from the render frame rate so the effect looks consistent at any FPS.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc     = Minecraft.getInstance();
        Player    player = mc.player;
        if (player == null) return;

        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof EntityCustomDragon dragon)) {
            // Not riding — gently recover to neutral so there is no snap on dismount
            smoothPitch  = Mth.lerp(RECOVER_LERP, smoothPitch, 0.0f);
            smoothRoll   = Mth.lerp(RECOVER_LERP, smoothRoll,  0.0f);
            prevYBodyRot = Float.NaN;
            return;
        }

        // ── Pitch ──────────────────────────────────────────────────────────────
        // getDragonPitch() is positive = nose-up, negative = nose-down.
        // We apply a fraction of that to the camera so it feels like the
        // player's view tilts with the dragon without fully locking to it.
        float dragonPitch = dragon.getDragonPitch();
        float targetPitch = Mth.clamp(dragonPitch * PITCH_SCALE, -MAX_PITCH_DEG, MAX_PITCH_DEG);
        smoothPitch = Mth.lerp(PITCH_LERP, smoothPitch, targetPitch);

        // ── Roll (banking) ─────────────────────────────────────────────────────
        // Derive yaw velocity from the change in body yaw between ticks.
        float currentYaw = dragon.yBodyRot;
        if (!Float.isNaN(prevYBodyRot)) {
            float yawDelta = currentYaw - prevYBodyRot;
            // Wrap to [-180, 180] to avoid sign flips at the 0/360 boundary
            while (yawDelta >  180.0f) yawDelta -= 360.0f;
            while (yawDelta < -180.0f) yawDelta += 360.0f;

            // Turning right (positive MC yaw delta) → camera rolls left (negative roll)
            float targetRoll = Mth.clamp(-yawDelta * ROLL_YAW_K, -MAX_ROLL_DEG, MAX_ROLL_DEG);
            smoothRoll = Mth.lerp(ROLL_LERP, smoothRoll, targetRoll);
        }
        prevYBodyRot = currentYaw;
    }

    // ── Per-frame angle injection ─────────────────────────────────────────────

    /**
     * Every render frame: add the smoothed pitch and roll offsets to the camera
     * angles just before the frame is drawn.  Using {@code event.setPitch} /
     * {@code event.setRoll} rather than touching the camera directly ensures the
     * offsets work correctly alongside Minecraft's own camera smoothing (e.g. the
     * built-in cinematic mode).
     */
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.player.getVehicle() instanceof EntityCustomDragon)) return;

        // Pitch: positive = look down (nose-down dive feels like camera tilts forward)
        event.setPitch(event.getPitch() + smoothPitch);
        // Roll: positive = tilt right
        event.setRoll(event.getRoll() + smoothRoll);
    }
}