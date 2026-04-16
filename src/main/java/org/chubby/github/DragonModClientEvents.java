package org.chubby.github;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only event bus subscriber.
 *
 * <p>Registers client-side particle providers so Minecraft knows which
 * {@link DragonFireParticle.Provider} to use when it receives a
 * {@link DragonModParticles#DRAGON_FIRE} spawn packet from the server.
 *
 * <p>This class is automatically discovered because it is annotated with
 * {@code @Mod.EventBusSubscriber(bus = MOD, value = CLIENT)}.
 * No manual registration needed — just make sure this class is on the
 * classpath.
 */
@Mod.EventBusSubscriber(
        modid = DragonMod.MODID,
        bus   = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class DragonModClientEvents {

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        // Sprite-based provider: Vanilla loads the sprite sheet from
        //   assets/dragonmod/particles/dragon_fire.json
        // and injects the SpriteSet into our Provider constructor.
        event.registerSpriteSet(
                DragonModParticles.DRAGON_FIRE.get(),
                DragonFireParticle.Provider::new
        );
    }
}