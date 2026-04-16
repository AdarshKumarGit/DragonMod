package org.chubby.github;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers all custom particle types for DragonMod.
 *
 * <p>Call {@link #register(IEventBus)} from your mod constructor, e.g.:
 * <pre>
 *   DragonModParticles.register(modEventBus);
 * </pre>
 *
 * <p>Client-side particle providers are subscribed separately in
 * {@link DragonModClientEvents} via the {@code RegisterParticleProvidersEvent}.
 */
public class DragonModParticles {

    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, DragonMod.MODID);

    /**
     * The animated dragon fire breath particle.
     *
     * <p>Sprite frames are defined in:
     *   {@code assets/dragonmod/particles/dragon_fire.json}
     *
     * <p>Textures (8 frames, 16×16 each):
     *   {@code assets/dragonmod/textures/particle/dragon_fire_0.png}  ← bright/full
     *   {@code assets/dragonmod/textures/particle/dragon_fire_1.png}
     *   …
     *   {@code assets/dragonmod/textures/particle/dragon_fire_7.png}  ← fully faded/transparent
     *
     * <p>{@code false} = not always-show (obeys the particle setting slider).
     */
    public static final RegistryObject<SimpleParticleType> DRAGON_FIRE =
            PARTICLES.register("dragon_fire", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> FIRE_SPIT =
            PARTICLES.register("fire_spit", () -> new SimpleParticleType(false));

    public static void register(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
    }
}