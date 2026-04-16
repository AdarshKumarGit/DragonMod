package org.chubby.github;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side particle for the dragon fire breath stream.
 *
 * <h2>How the animation works</h2>
 * The particle uses a {@link SpriteSet} loaded from
 * {@code assets/dragonmod/particles/dragon_fire.json}.
 * That JSON lists 8 texture names in order (frame 0 = brightest, frame 7 = faded).
 * {@code setSpriteFromAge(sprites)} maps the particle's current age onto
 * that list each tick, so it automatically advances through every frame
 * and ends on the transparent/faded one just as the particle expires.
 *
 * <p>You do NOT need to touch alpha in code — the fade is baked into your
 * texture frames. Just make each PNG progressively more transparent /
 * dimmer, and the particle engine handles the rest.
 *
 * <h2>Texture requirements</h2>
 * <pre>
 *   assets/dragonmod/textures/particle/dragon_fire_0.png  ← full, bright fire puff
 *   assets/dragonmod/textures/particle/dragon_fire_1.png
 *   assets/dragonmod/textures/particle/dragon_fire_2.png
 *   assets/dragonmod/textures/particle/dragon_fire_3.png
 *   assets/dragonmod/textures/particle/dragon_fire_4.png
 *   assets/dragonmod/textures/particle/dragon_fire_5.png
 *   assets/dragonmod/textures/particle/dragon_fire_6.png
 *   assets/dragonmod/textures/particle/dragon_fire_7.png  ← mostly transparent
 * </pre>
 * Recommended size: 16×16 px (can go up to 32×32 for more detail).
 * Use PNG-32 (RGBA) so transparency in the later frames actually works.
 *
 * <h2>Tuning constants (all near the top of the constructor)</h2>
 * <ul>
 *   <li>{@code this.lifetime}    — how many ticks the particle lives (default 18 = ~0.9s)</li>
 *   <li>{@code this.quadSize}   — visual radius in blocks (randomised per particle)</li>
 *   <li>{@code this.gravity}    — positive = falls, negative = rises (default slight rise)</li>
 *   <li>velocity xz spread     — controlled in {@code DragonFireParticle.Provider}</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class DragonFireParticle extends TextureSheetParticle {

    // -----------------------------------------------------------------------
    // The SpriteSet reference — used every tick to advance the frame.
    // -----------------------------------------------------------------------
    private final SpriteSet sprites;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    protected DragonFireParticle(ClientLevel level,
                                 double x, double y, double z,
                                 double vx, double vy, double vz,
                                 SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;

        // ── Lifetime ───────────────────────────────────────────────────────
        // 18 ticks ≈ 0.9 s.  Increase for a longer-lingering flame puff.
        this.lifetime = 14 + this.random.nextInt(8);   // 14–21 ticks

        // ── Size ────────────────────────────────────────────────────────────
        // Each particle gets a slightly different size so the stream looks
        // organic rather than identical blobs.
        this.quadSize = 0.35f + this.random.nextFloat() * 0.45f;  // 0.35–0.80 blocks

        // ── Physics ─────────────────────────────────────────────────────────
        // hasPhysics = false → not blocked by blocks (the stream is a visual
        // effect; the real damage comes from stimulateFire ray-tracing).
        this.hasPhysics = false;

        // ── Gravity ─────────────────────────────────────────────────────────
        // Negative → particles drift upward slightly, like rising hot air.
        // Set to 0 for dead-straight, positive for falling embers.
        this.gravity = -0.015f;

        // ── Lighting ────────────────────────────────────────────────────────
        // Full-bright so fire puffs glow even in dark areas.
        this.rCol = 1.0f;
        this.gCol = 0.9f;
        this.bCol = 0.4f;

        // Pick the first sprite frame immediately.
        this.setSpriteFromAge(sprites);
    }

    // -----------------------------------------------------------------------
    // Tick — advance animation frame based on how old the particle is.
    // -----------------------------------------------------------------------
    @Override
    public void tick() {
        // Save previous position for interpolation.
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // Expire when lifetime is up.
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Advance the sprite to the frame that matches the current age/lifetime.
        // When age == 0 → frame 0 (bright).  When age == lifetime-1 → last frame (faded).
        this.setSpriteFromAge(sprites);

        // Apply gravity and velocity.
        this.yd += this.gravity;
        this.move(this.xd, this.yd, this.zd);

        // Drag — slows the particle over time so it doesn't fly off into the distance.
        this.xd *= 0.92;
        this.yd *= 0.92;
        this.zd *= 0.92;
    }

    // -----------------------------------------------------------------------
    // Render type — PARTICLE_SHEET_TRANSLUCENT allows per-pixel transparency,
    // which is what lets the fade-in-texture approach actually work.
    // Use PARTICLE_SHEET_OPAQUE if your textures have no transparency at all.
    // -----------------------------------------------------------------------
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Provider — registered in DragonModClientEvents via
    //  RegisterParticleProvidersEvent.
    // ═══════════════════════════════════════════════════════════════════════
    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        /** Forge/Vanilla injects the SpriteSet that was loaded from the JSON. */
        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type,
                                       ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new DragonFireParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}