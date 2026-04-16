package org.chubby.github;

import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.Objects;

@Mod(DragonMod.MODID)
public class DragonMod
{
    public static final String MODID = "dragonmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    // ── Network ──────────────────────────────────────────────────────────────
    public static final SimpleChannel NETWORK_WRAPPER;
    private static final String PROTOCOL_VERSION = Integer.toString(1);

    // ── Registries ───────────────────────────────────────────────────────────
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item>          ITEMS    = DeferredRegister.create(ForgeRegistries.ITEMS,        MODID);
    public static final DeferredRegister<SoundEvent>    SOUNDS   = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    // ── Entities ─────────────────────────────────────────────────────────────
    public static final RegistryObject<EntityType<EntityDragon>> DRAGON = ENTITIES.register("dragon", () ->
            EntityType.Builder.of(EntityDragon::new, MobCategory.MONSTER).build("dragon"));
    public static final RegistryObject<EntityType<EntityDragonEgg>> DRAGON_EGG = ENTITIES.register("dragon_egg", () ->
            EntityType.Builder.of(EntityDragonEgg::new, MobCategory.MISC).build("dragon_egg"));

    // ── Items ────────────────────────────────────────────────────────────────
    public static final RegistryObject<Item> DRAGON_EGG_ITEM = ITEMS.register("dragon_egg",
            () -> new DragonEggItem(new Item.Properties().stacksTo(1)));

    // ── Sounds ───────────────────────────────────────────────────────────────
    // These must match the sound event names in your sounds.json resource file.
    public static final RegistryObject<SoundEvent> SOUND_TAIL_WHACK  = registerSoundEvent("tail_whack");
    public static final RegistryObject<SoundEvent> SOUND_WALK        = registerSoundEvent("walk");
    public static final RegistryObject<SoundEvent> SOUND_BITE        = registerSoundEvent("bite");
    public static final RegistryObject<SoundEvent> SOUND_EATING      = registerSoundEvent("eating");
    public static final RegistryObject<SoundEvent> SOUND_EPIC_ROAR   = registerSoundEvent("epic_roar");
    public static final RegistryObject<SoundEvent> SOUND_FLIGHT      = registerSoundEvent("fly");
    public static final RegistryObject<SoundEvent> SOUND_ROAR        = registerSoundEvent("roar");
    public static final RegistryObject<SoundEvent> SOUND_RUN         = registerSoundEvent("run");
    public static final RegistryObject<SoundEvent> SOUND_SHAKE_PREY  = registerSoundEvent("shake_prey");
    public static final RegistryObject<SoundEvent> SOUND_SPEAK       = registerSoundEvent("speak");

    public DragonMod(FMLJavaModLoadingContext context) {
        ENTITIES.register(context.getModEventBus());
        ITEMS.register(context.getModEventBus());
        SOUNDS.register(context.getModEventBus());
        DragonModParticles.register(context.getModEventBus());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public static <MSG> void sendMSGToAll(MSG message) {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            NETWORK_WRAPPER.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CLIENT MOD-BUS EVENTS
    // ════════════════════════════════════════════════════════════════════════

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModBus {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                EntityRenderers.register(DRAGON.get(),     EntityCustomDragonRenderer::new);
                EntityRenderers.register(DRAGON_EGG.get(), EntityDragonEggRenderer::new);
            });

        }

        /**
         * Registers the fire key (R) with Minecraft's key-mapping system.
         * The binding appears in Options → Controls under "Dragon Mod".
         */
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
            DragonFireKeybinds.register(e);
        }

        @SubscribeEvent
        public static void registerAtts(EntityAttributeCreationEvent event) {
            event.put(DRAGON.get(), EntityDragon.bakeAttributes().build());
        }

        @SubscribeEvent
        public static void registerParticle(RegisterParticleProvidersEvent event)
        {
            event.registerSpriteSet(DragonModParticles.FIRE_SPIT.get(), FireSpitParticle.Provider::new);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STATIC INITIALISER — network channel setup
    //
    //  Message index table:
    //    0  →  MessageRiderFireInput   (client → server, rider fire mode)
    // ════════════════════════════════════════════════════════════════════════

    static {
        // Build the channel.
        NetworkRegistry.ChannelBuilder channelBuilder = NetworkRegistry.ChannelBuilder.named(
                ResourceLocation.fromNamespaceAndPath("dragonmod", "main_channel"));
        String version = PROTOCOL_VERSION;
        version.getClass();
        Objects.requireNonNull(version);
        channelBuilder = channelBuilder.clientAcceptedVersions(version::equals);
        version = PROTOCOL_VERSION;
        version.getClass();
        Objects.requireNonNull(version);
        NETWORK_WRAPPER = channelBuilder
                .serverAcceptedVersions(version::equals)
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .simpleChannel();

        // ── Message registrations ──────────────────────────────────────────
        // 0 -> PacketDragonFireInput  (client -> server, rider fire mode)
        NETWORK_WRAPPER.registerMessage(
                0,
                PacketDragonFireInput.class,
                PacketDragonFireInput::encode,
                PacketDragonFireInput::decode,
                PacketDragonFireInput::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
}