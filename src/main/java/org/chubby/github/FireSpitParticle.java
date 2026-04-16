package org.chubby.github;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Visual translation of fire_spit.json for Minecraft 1.20.1 / Forge.
 *
 * Design summary (from the JSON):
 *   • Billboard facing camera on all axes (rotate_xyz)
 *   • Lifetime  2–3 s (40–60 ticks) — math.random(2, 3) × 20
 *   • Size grows with age: 0.05 + (t * 0.05) + (random_1 * t * 0.25)
 *   • Random initial rotation 0–360°, random spin rate ±150°/s
 *   • Strong drag (drag_coefficient ≈ 4), slight upward drift
 *   • Colour gradient:
 *       0.07  → #FFFFFF  white
 *       0.24  → #FFE307  warm yellow
 *       0.48  → #FF8800  orange
 *       0.67  → #FF5843  red-orange  (alpha 0.741)
 *       0.87  → #590000  dark red    (alpha 0.780)
 *       0.96  → #000000  black       (alpha 0.580)
 *       1.00  → transparent
 *
 * 1.20.1 API notes:
 *   • No getRoll() override — use the inherited {@code roll} / {@code oRoll} fields.
 *   • No this.sprites field — store the SpriteSet ourselves and pass it explicitly
 *     to {@code setSpriteFromAge(SpriteSet)}.
 */
@OnlyIn(Dist.CLIENT)
public class FireSpitParticle extends TextureSheetParticle {

    // Must be stored explicitly — TextureSheetParticle does not expose this.sprites
    private final SpriteSet spriteSet;

    // Per-particle random scale factor (maps to particle_random_1 in JSON)
    private final float randomScale;

    // Spin rate in radians per tick (±150 deg/s / 20 tps = ±7.5 deg/tick → radians)
    private final float spinRate;

    protected FireSpitParticle(ClientLevel level,
                               double x, double y, double z,
                               double vx, double vy, double vz,
                               SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);

        this.spriteSet  = sprites;

        // lifetime: 2–3 s → 40–60 ticks
        this.lifetime   = 40 + this.random.nextInt(21);

        // random_1 equivalent — influences quad size growth
        this.randomScale = this.random.nextFloat();

        // Initial rotation 0–360° stored in the inherited `roll` field.
        // `oRoll` must be set to the same value so interpolation starts cleanly.
        float initialAngleDeg = this.random.nextFloat() * 360f;
        this.roll  = (float) Math.toRadians(initialAngleDeg);
        this.oRoll = this.roll;

        // ±150°/s  →  ±7.5°/tick  →  radians/tick
        this.spinRate = (float) Math.toRadians((this.random.nextFloat() - 0.5f) * 15f);

        // Velocity encodes ray direction from caller; add tiny turbulent spread
        this.xd = vx + (this.random.nextDouble() - 0.5) * 0.05;
        this.yd = vy + (this.random.nextDouble() - 0.5) * 0.04;
        this.zd = vz + (this.random.nextDouble() - 0.5) * 0.05;

        // Approximate drag_coefficient = 4 → strong friction
        this.friction  = 0.78f;
        // Slight upward drift (JSON: linear_acceleration y = age * 1.5, damped)
        this.gravity   = -0.015f;

        // Don't collide with terrain — this is a visual effect only
        this.hasPhysics = false;

        // Pick the first sprite frame
        this.setSpriteFromAge(sprites);
    }

    // ── Per-tick update ──────────────────────────────────────────────────────

    @Override
    public void tick() {
        // Save previous position for interpolation
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Save previous roll for interpolation, then advance
        this.oRoll  = this.roll;
        this.roll  += this.spinRate;

        // Manual velocity damping (approximates drag_coefficient = 4)
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        // Slight upward drift that grows with particle age
        this.yd += 0.003f * (this.age / (float) this.lifetime);

        this.move(this.xd, this.yd, this.zd);

        // Animate sprite sheet frame with age
        this.setSpriteFromAge(this.spriteSet);
    }

    // ── Visual helpers ───────────────────────────────────────────────────────

    @Override
    public float getQuadSize(float partial) {
        // Translated from JSON size expression:
        //   0.05 + (v.particle_age * 0.05) + (v.particle_random_1 * v.particle_age * 0.25)
        // where particle_age is normalised [0,1]
        float t = (this.age + partial) / this.lifetime;
        return 0.05f + t * 0.05f + this.randomScale * t * 0.25f;
    }

    @Override
    public int getLightColor(float partial) {
        // Full-brightness: fire should glow even in dark areas
        return 0xF000F0;
    }

    // ── Colour gradient (translated from JSON) ───────────────────────────────

    /**
     * Writes RGBA into {@link #rCol}, {@link #gCol}, {@link #bCol}, {@link #alpha}
     * by interpolating the 7-stop gradient from fire_spit.json.
     */
    private void applyColour(float t) {
        final float r, g, b, a;

        if (t <= 0.07f) {
            // transparent → white
            float u = t / 0.07f;
            r = 1f; g = 1f; b = 1f; a = u;
        } else if (t <= 0.24f) {
            // white (#FFFFFF) → warm yellow (#FFE307)
            float u = (t - 0.07f) / (0.24f - 0.07f);
            r = 1f;
            g = lerp(1f,     0.890f, u);
            b = lerp(1f,     0.027f, u);
            a = 1f;
        } else if (t <= 0.48f) {
            // yellow (#FFE307) → orange (#FF8800)
            float u = (t - 0.24f) / (0.48f - 0.24f);
            r = 1f;
            g = lerp(0.890f, 0.533f, u);
            b = lerp(0.027f, 0f,     u);
            a = 1f;
        } else if (t <= 0.67f) {
            // orange (#FF8800) → red-orange (#FF5843), alpha 1 → 0.741
            float u = (t - 0.48f) / (0.67f - 0.48f);
            r = 1f;
            g = lerp(0.533f, 0.345f, u);
            b = lerp(0f,     0.263f, u);
            a = lerp(1f,     0.741f, u);
        } else if (t <= 0.87f) {
            // red-orange (#FF5843) → dark red (#590000), alpha 0.741 → 0.780
            float u = (t - 0.67f) / (0.87f - 0.67f);
            r = lerp(1f,     0.349f, u);
            g = lerp(0.345f, 0f,     u);
            b = lerp(0.263f, 0f,     u);
            a = lerp(0.741f, 0.780f, u);
        } else if (t <= 0.96f) {
            // dark red (#590000) → black (#000000), alpha 0.780 → 0.580
            float u = (t - 0.87f) / (0.96f - 0.87f);
            r = lerp(0.349f, 0f, u);
            g = 0f; b = 0f;
            a = lerp(0.780f, 0.580f, u);
        } else {
            // black → fully transparent
            float u = (t - 0.96f) / (1f - 0.96f);
            r = 0f; g = 0f; b = 0f;
            a = lerp(0.580f, 0f, u);
        }

        this.rCol  = r;
        this.gCol  = g;
        this.bCol  = b;
        this.alpha = Math.max(0f, a);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // ── Render ───────────────────────────────────────────────────────────────

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partial) {
        float t = Math.min((this.age + partial) / this.lifetime, 1f);
        applyColour(t);
        super.render(buffer, camera, partial);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites)
            implements ParticleProvider<SimpleParticleType> {

        @Override
        public Particle createParticle(SimpleParticleType type,
                                       ClientLevel level,
                                       double x,  double y,  double z,
                                       double vx, double vy, double vz) {
            return new FireSpitParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}