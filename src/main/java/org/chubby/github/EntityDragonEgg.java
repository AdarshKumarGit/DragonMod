package org.chubby.github;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EntityDragonEgg extends Entity implements GeoEntity {

    public final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final int TICKS_TO_HATCH = 600;

    @Nullable
    private java.util.UUID ownerUUID = null;

    // ---------------- Synced Data ----------------
    private static final EntityDataAccessor<Integer> DATA_FIRE_TICKS =
            SynchedEntityData.defineId(EntityDragonEgg.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> DATA_HATCHING =
            SynchedEntityData.defineId(EntityDragonEgg.class, EntityDataSerializers.BOOLEAN);

    // ---------------- Constructor ----------------
    public EntityDragonEgg(EntityType<? extends EntityDragonEgg> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FIRE_TICKS, 0);
        this.entityData.define(DATA_HATCHING, false);
    }

    // ---------------- Save / Load ----------------
    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        setFireTicks(tag.getInt("FireTicks"));
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("FireTicks", getFireTicks());
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    // ---------------- Getters ----------------
    public int getFireTicks() {
        return this.entityData.get(DATA_FIRE_TICKS);
    }

    public void setFireTicks(int ticks) {
        this.entityData.set(DATA_FIRE_TICKS, Math.max(0, ticks));
    }

    public boolean isHatching() {
        return this.entityData.get(DATA_HATCHING);
    }

    public void setHatching(boolean value) {
        this.entityData.set(DATA_HATCHING, value);
    }

    public void setOwnerUUID(@Nullable java.util.UUID uuid) {
        this.ownerUUID = uuid;
    }

    public float getHatchProgress() {
        return (float) getFireTicks() / (float) TICKS_TO_HATCH;
    }

    // ---------------- Damage ----------------
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        this.spawnAtLocation(new ItemStack(DragonMod.DRAGON_EGG_ITEM.get()), 0.0F);
        this.discard();
        return true;
    }

    // ---------------- Tick ----------------
    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            tickClientParticles();
            return;
        }

        BlockPos pos = this.blockPosition();
        boolean inFire = isFireAt(pos) || isFireAt(pos.below()) || isFireAt(pos.above());

        if (inFire) {
            int newTicks = getFireTicks() + 1;
            setFireTicks(newTicks);
            setHatching(true);

            if (newTicks % 40 == 0) {
                this.level().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.FIRE_AMBIENT,
                        SoundSource.NEUTRAL,
                        0.6f, 0.8f + this.random.nextFloat() * 0.3f);
            }

            if (newTicks >= TICKS_TO_HATCH) {
                hatch();
            }

        } else {
            if (getFireTicks() > 0) {
                setFireTicks(0);
            }
            setHatching(false);
        }
    }

    private boolean isFireAt(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
    }

    // ---------------- Hatch ----------------
    private void hatch() {
        if (this.level().isClientSide) return;

        ServerLevel server = (ServerLevel) this.level();
        BlockPos pos = this.blockPosition();

        if (isFireAt(pos)) {
            server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (isFireAt(pos.below())) {
            server.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
        }

        EntityDragon baby = DragonMod.DRAGON.get().create(server);
        if (baby != null) {
            baby.setPos(this.getX(), this.getY(), this.getZ());

            baby.finalizeSpawn(
                    server,
                    server.getCurrentDifficultyAt(pos),
                    MobSpawnType.BREEDING,
                    null,
                    null);

            baby.setAgeInDays(1);
            baby.setGender(this.random.nextBoolean());
            baby.setHunger(50);

            if (ownerUUID != null) {
                baby.setOwnerUUID(ownerUUID);
                baby.setTame(true);
            }

            server.addFreshEntity(baby);
        }

        server.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                IafSoundRegistry.FIREDRAGON_CHILD_IDLE,
                SoundSource.NEUTRAL,
                1.0f, 1.0f);

        this.discard();
    }

    // ---------------- Client Particles ----------------
    private void tickClientParticles() {
        int fireTicks = getFireTicks();
        if (fireTicks <= 0) return;

        float progress = getHatchProgress();
        int count = (int) (progress * 5) + 1;

        for (int i = 0; i < count; i++) {
            if (this.random.nextFloat() > 0.45f) continue;

            this.level().addParticle(
                    progress > 0.7f ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
                    this.getX(),
                    this.getY() + 0.5,
                    this.getZ(),
                    0, 0.02, 0);
        }
    }

    // ---------------- GeckoLib ----------------
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 0, this::predicate));
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {

        if (this.isHatching()) {
            state.getController().setAnimation(
                    RawAnimation.begin().thenLoop("warming")
            );
        } else {
            state.getController().setAnimation(
                    RawAnimation.begin().thenLoop("idle")
            );
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ---------------- Misc ----------------
    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean shouldBeSaved() { return true; }

    @Override
    public boolean isPickable() { return true; }
}