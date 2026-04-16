package org.chubby.github;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.api.event.DragonFireEvent;
import com.github.alexthe666.iceandfire.entity.*;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.github.alexthe666.iceandfire.message.MessageDragonSyncFire;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import com.github.alexthe666.iceandfire.misc.IafTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class EntityDragon extends EntityCustomDragon {

    public static final ResourceLocation FEMALE_LOOT   = ResourceLocation.fromNamespaceAndPath("iceandfire", "entities/dragon/fire_dragon_female");
    public static final ResourceLocation MALE_LOOT     = ResourceLocation.fromNamespaceAndPath("iceandfire", "entities/dragon/fire_dragon_male");
    public static final ResourceLocation SKELETON_LOOT = ResourceLocation.fromNamespaceAndPath("iceandfire", "entities/dragon/fire_dragon_skeleton");

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR — max health hardcoded to 750 (min = 750 × 0.04 = 30).
    //  The health scales linearly from minimumHealth to maximumHealth as the
    //  dragon ages through 125 in-game days (see EntityCustomDragon.updateAttributes).
    // ═══════════════════════════════════════════════════════════════════════
    public EntityDragon(EntityType<? extends EntityCustomDragon> t, Level worldIn) {
        super(t, worldIn, DragonType.FIRE,
                (double) 1.0F,
                (double) (1 + IafConfig.dragonAttackDamage),
                30.0,      // minimumHealth  (was IafConfig.dragonHealth * 0.04)
                750.0,     // maximumHealth  ← hardcoded to 750
                (double) 0.15F,
                (double) 0.4F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 8.0F);
    }

    /** Dragon is completely immune to fire and lava damage. */
    @Override
    public boolean fireImmune() {
        return true;
    }

    protected boolean shouldTarget(Entity entity) {
        if (entity instanceof EntityDragonBase && !this.isTame()) {
            return entity.getType() != this.getType()
                    && this.getBbWidth() >= entity.getBbWidth()
                    && !((EntityDragonBase) entity).isMobDead();
        } else {
            return entity instanceof Player
                    || DragonUtils.isDragonTargetable(entity, IafTagRegistry.FIRE_DRAGON_TARGETS)
                    || !this.isTame() && DragonUtils.isVillager(entity);
        }
    }

    @Override
    public String getVariantName(int var1) { return "custom"; }

    @Override
    public Item getVariantScale(int var1) { return null; }

    @Override
    public Item getVariantEgg(int var1) { return DragonMod.DRAGON_EGG_ITEM.get(); }

    public Item getSummoningCrystal() {
        return (Item) IafItemRegistry.SUMMONING_CRYSTAL_FIRE.get();
    }

    /** Set to true for one tick by the network handler when the rider taps R. */
    public volatile boolean triggerRiderFireball = false;

    /** Set to true each tick by the network handler while the rider holds R. */
    public volatile boolean riderStreamHoldTick  = false;

    /** Set to true for one tick by the network handler when the rider releases R after streaming. */
    public volatile boolean riderStreamStop      = false;

    /** Cooldown ticks between rider fireballs. */
    private int riderFireballCooldown = 0;

    private static final int RIDER_FIREBALL_COOLDOWN = 25;

    public void aiStep() {
        super.aiStep();
        LivingEntity attackTarget = this.getTarget();
        if (!this.level().isClientSide && attackTarget != null) {
            // ── Melee proximity check ────────────────────────────────────────
            // Base sphere — catches anything within the dragon's general radius.
            float inflate = 2.5F + this.getRenderSize() * 0.33F;
            boolean sphereHit = this.getBoundingBox()
                    .inflate(inflate, inflate, inflate)
                    .intersects(attackTarget.getBoundingBox());

            // Hitbox/animation sync — during bite and wing-blast the head and
            // wings sweep forward past the sphere boundary.  We supplement with
            // a narrow forward AABB aligned to the dragon's look vector so that
            // the damage hitbox matches the visual reach of the animated attack.
            boolean forwardHit = false;
            AnimationState currentAnim = getCurrentAnimation();
            if (!sphereHit && (currentAnim == ANIMATION_BITE || currentAnim == ANIMATION_WINGBLAST)) {
                net.minecraft.world.phys.Vec3 look = this.getLookAngle();
                // Reach: 1.8× bounding width + scale factor to cover the head sweep
                double reach  = this.getBbWidth() * 1.8 + this.getRenderSize() * 0.15;
                double halfW  = this.getBbWidth() * 0.55;
                // Project the AABB centre forward from the dragon's eye level
                double cx = this.getX() + look.x * reach * 0.5;
                double cy = this.getY() + this.getBbHeight() * 0.65 + look.y * reach * 0.4;
                double cz = this.getZ() + look.z * reach * 0.5;
                forwardHit = new net.minecraft.world.phys.AABB(
                        cx - halfW, cy - halfW, cz - halfW,
                        cx + halfW, cy + halfW, cz + halfW)
                        .inflate(reach * 0.5)
                        .intersects(attackTarget.getBoundingBox());
            }

            if (sphereHit || forwardHit) {
                this.doHurtTarget(attackTarget);
            }

            // FIX 1: The old condition `groundAttack == FIRE && (usingGroundAttack || onGround())`
            // fired every ground tick regardless of attack type, and never covered air fire modes.
            // Corrected to gate on the actual attack type flags for both ground and air.
            if ((this.usingGroundAttack && this.groundAttack == IafDragonAttacks.Ground.FIRE)
                    || (!this.usingGroundAttack && (this.airAttack == IafDragonAttacks.Air.SCORCH_STREAM
                    || this.airAttack == IafDragonAttacks.Air.HOVER_BLAST))) {
                this.shootFireAtMob(attackTarget);
            }

            if (this.airAttack == IafDragonAttacks.Air.TACKLE && !this.usingGroundAttack && this.distanceToSqr(attackTarget) < (double)100.0F) {
                double difX = attackTarget.getX() - this.getX();
                double difY = attackTarget.getY() + (double)attackTarget.getBbHeight() - this.getY();
                double difZ = attackTarget.getZ() - this.getZ();
                this.setDeltaMovement(this.getDeltaMovement().add(difX * 0.1, difY * 0.1, difZ * 0.1));
                if (this.getBoundingBox().inflate((double)(1.0F + this.getRenderSize() * 0.5F), (double)(1.0F + this.getRenderSize() * 0.5F), (double)(1.0F + this.getRenderSize() * 0.5F)).intersects(attackTarget.getBoundingBox())) {
                    this.doHurtTarget(attackTarget);
                    this.usingGroundAttack = true;
                    this.randomizeAttacks();
                    this.setFlying(false);
                    this.setHovering(false);
                }
            }
        }

        if (!this.level().isClientSide) {
            // Count up while any fire-initiation animation is playing.
            // ANIMATION_FIRECHARGE is used for the stream; ANIMATION_ROAR /
            // ANIMATION_EPIC_ROAR are used when launching a fireball projectile.
            if (this.currentActiveAnimation == ANIMATION_FIRECHARGE
                    || this.currentActiveAnimation == ANIMATION_ROAR
                    || this.currentActiveAnimation == ANIMATION_EPIC_ROAR) {
                ++this.aiFireChargeTimer;
            } else {
                this.aiFireChargeTimer = 0;
            }
        }

        // FIX 2: riderShootFire() was never called from aiStep. The volatile flags
        // (triggerRiderFireball / riderStreamHoldTick / riderStreamStop) set by the
        // network handler were simply never consumed, so rider fire never worked at all.
        if (!this.level().isClientSide) {
            LivingEntity rider = this.getControllingPassenger();
            if (rider != null) {
                this.riderShootFire(rider);
            }
        }
    }

    public boolean doHurtTarget(@NotNull Entity entityIn) {
        this.getLookControl().setLookAt(entityIn, 30.0F, 30.0F);
        if (!this.isPlayingAttackAnimation()) {
            switch (this.groundAttack) {
                case BITE:
                    startAnimation(ANIMATION_BITE);
                    break;
                case TAIL_WHIP:
                    // Use startAnimation() so GeckoLib's tick counter resets cleanly,
                    // then arm the lifetime timer so the finished animation is cleared.
                    startAnimation(ANIMATION_TAILWHACK);
                    this.tailAnimTicks  = 48; // 47 ticks (2.333 s) + 1 buffer
                    this.tailSwingCooldown = 60;
                    break;
                case SHAKE_PREY:
                    boolean grabbed = new Random().nextInt(2) == 0
                            && this.isDirectPathBetweenPoints(this,
                            this.position().add(0.0, this.getBbHeight() / 2.0, 0.0),
                            entityIn.position().add(0.0, entityIn.getBbHeight() / 2.0, 0.0))
                            && entityIn.getBbWidth() < this.getBbWidth() * 0.5F
                            && this.getControllingPassenger() == null
                            && this.getDragonStage() > 1
                            && !(entityIn instanceof EntityDragonBase);

                    if (grabbed) {
                        startAnimation(ANIMATION_SHAKEPREY);
                        entityIn.startRiding(this);
                        // Damage is dealt by updatePreyInMouth() — return early so
                        // super.doHurtTarget() doesn't also call hurt() on the passenger.
                        return true;
                    }
                    // Grab failed — fall back to a normal bite.
                    this.groundAttack = IafDragonAttacks.Ground.BITE;
                    startAnimation(ANIMATION_BITE);
                    break;
                case WING_BLAST:
                    startAnimation(ANIMATION_WINGBLAST);
                    break;
            }
        }
        // Delegate actual damage to the parent (EntityCustomDragon.doHurtTarget),
        // which performs the directional tail-swing check and calls entityIn.hurt().
        return super.doHurtTarget(entityIn);
    }

    // -----------------------------------------------------------------------
    // riderShootFire — player-controlled fire while mounted
    // -----------------------------------------------------------------------
    // FIX 3: The old implementation used getRandom().nextInt(5) == 0 to randomly
    //        decide between fireball and stream, completely ignoring the volatile
    //        flags set by the network handler. Rewritten to properly consume them.
    //
    // FIX 4: The old code used `currentActiveAnimation = ANIMATION_FIRECHARGE`
    //        (direct field assignment) to start animations. This skips the internal
    //        tick reset, so getAnimationTick() never reliably reached 20 and the
    //        fireball was never spawned. Fixed to use startAnimation().
    //
    // FIX 5: The old code used the no-arg getAnimationTick() while stimulateFire()
    //        (which works correctly) uses getAnimationTick(ANIMATION_FIRECHARGE).
    //        Made consistent throughout by using the specific-animation form.
    // -----------------------------------------------------------------------
    @Override
    public void riderShootFire(Entity controller) {
        if (this.isBaby()) return;

        // Tick down fireball cooldown every server tick.
        if (this.riderFireballCooldown > 0) {
            --this.riderFireballCooldown;
        }

        // ── FIREBALL mode (rider tapped R — flag set for exactly one tick) ────
        if (this.triggerRiderFireball) {
            this.triggerRiderFireball = false;
            if (this.riderFireballCooldown == 0) {
                // Roar animation plays when the rider fires a projectile.
                if (this.getCurrentAnimation() != ANIMATION_ROAR
                        && this.getCurrentAnimation() != ANIMATION_EPIC_ROAR) {
                    this.startAnimation(ANIMATION_ROAR);  // resets tick counter
                } else if (this.aiFireChargeTimer == 20) {

                    this.setYRot(this.yBodyRot);
                    // Spawn fireball from the actual head-part position.
                    Vec3 headVec = this.getHeadPartPosition();
                    this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4.0F, 1.0F);
                    double d2 = controller.getLookAngle().x;
                    double d3 = controller.getLookAngle().y;
                    double d4 = controller.getLookAngle().z;
                    d2 += this.random.nextGaussian() * 0.0075;
                    d3 += this.random.nextGaussian() * 0.0075;
                    d4 += this.random.nextGaussian() * 0.0075;
                    EntityDragonFireCharge fireball = new EntityDragonFireCharge(
                            (EntityType) IafEntityRegistry.FIRE_DRAGON_CHARGE.get(),
                            this.level(), this, d2, d3, d4);
                    fireball.setPos(headVec.x, headVec.y, headVec.z);
                    if (!this.level().isClientSide) {
                        this.level().addFreshEntity(fireball);
                    }
                    this.stopCurrentAnimation();       // clear so next tap can re-trigger
                    this.aiFireChargeTimer = 0;
                    this.riderFireballCooldown = RIDER_FIREBALL_COOLDOWN;
                }
            }
            return; // Don't process stream flags in the same tick as a fireball tap
        }

        // ── STREAM stop (rider released R after streaming) ────────────────────
        if (this.riderStreamStop) {
            this.riderStreamStop = false;
            this.setBreathingFire(false);
            this.fireStopTicks = 10; // short grace window so stream doesn't snap off abruptly
            // Clear the fire animation so the jaw closes when the stream stops.
            if (this.getCurrentAnimation() == ANIMATION_FIRECHARGE) {
                this.stopCurrentAnimation();
                this.ANIMATION_FIRECHARGE.stop();
            }
            return;
        }

        // ── STREAM mode (rider is holding R) ──────────────────────────────────
        if (this.riderStreamHoldTick) {
            // Keep the fire_breath animation active the whole time the key is held.
            // Without this, getCurrentAnimation() never equals ANIMATION_FIRECHARGE
            // during stream mode, so the GeckoLib controller never fires.
            if (this.getCurrentAnimation() != ANIMATION_FIRECHARGE) {
                this.startAnimation(ANIMATION_FIRECHARGE);
            }
            if (this.isBreathingFire()) {
                if (this.isActuallyBreathingFire()) {
                    this.setYRot(this.yBodyRot);
                    if (this.tickCount % 5 == 0) {
                        this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4.0F, 1.0F);
                    }
                    HitResult mop = this.rayTraceRider(controller, 60.0, 1.0F);
                    if (mop != null) {
                        this.stimulateFire(mop.getLocation().x, mop.getLocation().y, mop.getLocation().z, 1);
                    }
                }
                // else: fireTicks < 20 still ramping up; isActuallyBreathingFire() will
                // become true once the counter (incremented in EntityCustomDragon.aiStep)
                // passes 20.
            } else {
                this.setBreathingFire(true);
            }
        }
    }

    public void travel(@NotNull Vec3 pTravelVector) {
        if (this.isInLava()) {
            if (this.isEffectiveAi() && this.getControllingPassenger() == null) {
                this.moveRelative(this.getSpeed(), pTravelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.7));
                if (this.getTarget() == null) {
                }
            } else if (this.allowLocalMotionControl && this.getControllingPassenger() != null && !this.isHovering() && !this.isFlying()) {
                LivingEntity rider = this.getControllingPassenger();
                float speed = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
                float lavaSpeedMod = (float)((double)0.28F + 0.1 * Mth.map((double)speed, this.minimumSpeed, this.maximumSpeed, (double)0.0F, (double)1.5F));
                speed *= lavaSpeedMod;
                speed *= rider.isSprinting() ? 1.4F : 1.0F;
                float vertical = 0.0F;
                if (this.isGoingUp() && !this.isGoingDown()) {
                    vertical = 0.8F;
                } else if (this.isGoingDown() && !this.isGoingUp()) {
                    vertical = -0.8F;
                } else if (this.isGoingUp() && this.isGoingDown() && this.isControlledByLocalInstance()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply((double)1.0F, (double)0.3F, (double)1.0F));
                }

                Vec3 travelVector = new Vec3((double)rider.xxa, (double)vertical, (double)rider.zza);
                if (this.isControlledByLocalInstance()) {
                    this.setSpeed(speed);
                    this.moveRelative(this.getSpeed(), travelVector);
                    this.move(MoverType.SELF, this.getDeltaMovement());
                    Vec3 currentMotion = this.getDeltaMovement();
                    if (this.horizontalCollision) {
                        currentMotion = new Vec3(currentMotion.x, 0.2, currentMotion.z);
                    }

                    this.setDeltaMovement(currentMotion.scale(0.7));
                    this.calculateEntityAnimation(false);
                } else {
                    this.setDeltaMovement(Vec3.ZERO);
                }

                this.tryCheckInsideBlocks();
            } else {
                super.travel(pTravelVector);
            }
        } else {
            if (this.allowLocalMotionControl && this.getControllingPassenger() != null && !this.isHovering() && !this.isFlying() && this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getFluidState().is(FluidTags.LAVA)) {
                LivingEntity rider = this.getControllingPassenger();
                double forward = (double)rider.zza;
                double strafing = (double)rider.xxa;
                double vertical = pTravelVector.y;
                float speed = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
                float groundSpeedModifier = (float)((double)1.8F * this.getFlightSpeedModifier());
                speed *= groundSpeedModifier;
                forward *= rider.isSprinting() ? (double)1.2F : (double)1.0F;
                forward *= rider.zza > 0.0F ? (double)1.0F : (double)0.2F;
                strafing *= (double)0.05F;
                if (this.isControlledByLocalInstance()) {
                    float flyingSpeed = speed * 0.1F;
                    this.setSpeed(speed);
                    super.travel(new Vec3(strafing, vertical, forward));
                    Vec3 currentMotion = this.getDeltaMovement();
                    if (this.horizontalCollision) {
                        currentMotion = new Vec3(currentMotion.x, 0.2, currentMotion.z);
                    }

                    this.setDeltaMovement(currentMotion.scale(0.7));
                } else {
                    this.setDeltaMovement(Vec3.ZERO);
                }

                this.tryCheckInsideBlocks();
                return;
            }

            super.travel(pTravelVector);
        }

    }

    // -----------------------------------------------------------------------
    // positionRider
    // -----------------------------------------------------------------------
    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull Entity.@NotNull MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (this.hasPassenger(passenger)
                && this.getControllingPassenger() != null
                && this.getControllingPassenger().getUUID().equals(passenger.getUUID())) {
            Vec3 riderPos = this.getRiderPosition();
            passenger.setPos(riderPos.x, riderPos.y, riderPos.z);
        }
    }

    // -----------------------------------------------------------------------
    // breathFireAtPos — dragonforge fuelling (unchanged in behaviour).
    // -----------------------------------------------------------------------
    @Override
    protected void breathFireAtPos(BlockPos burningTarget) {
        if (this.isBreathingFire()) {
            if (this.isActuallyBreathingFire()) {
                this.setYRot(this.yBodyRot);
                if (this.tickCount % 5 == 0) {
                    this.playSound(DragonMod.SOUND_ROAR.get(), 4.0F, 1.0F);
                }
                this.stimulateFire(
                        burningTarget.getX() + 0.5,
                        burningTarget.getY() + 0.5,
                        burningTarget.getZ() + 0.5, 1);
            }
        } else {
            this.setBreathingFire(true);
        }
    }

    // -----------------------------------------------------------------------
    // shootFireAtMob — AI fire-breath
    // -----------------------------------------------------------------------
    private int aiFireChargeTimer = 0;

    private void shootFireAtMob(LivingEntity entity) {
        if (this.usingGroundAttack && this.groundAttack == IafDragonAttacks.Ground.FIRE || !this.usingGroundAttack && (this.airAttack == IafDragonAttacks.Air.SCORCH_STREAM || this.airAttack == IafDragonAttacks.Air.HOVER_BLAST)) {
            if (this.usingGroundAttack && this.getRandom().nextInt(5) == 0 || !this.usingGroundAttack && this.airAttack == IafDragonAttacks.Air.HOVER_BLAST) {
                // AI fireball: roar animation plays while the shot charges (20 ticks),
                // then the projectile is launched from the head-part position.
                if (this.getCurrentAnimation() != ANIMATION_ROAR
                        && this.getCurrentAnimation() != ANIMATION_EPIC_ROAR) {
                    this.startAnimation(ANIMATION_ROAR);
                } else if (this.aiFireChargeTimer == 20) {
                    this.setYRot(this.yBodyRot);
                    // Spawn from actual mouth position.
                    Vec3 headVec = this.getHeadPartPosition();
                    double d2 = entity.getX() - headVec.x;
                    double d3 = entity.getY() - headVec.y;
                    double d4 = entity.getZ() - headVec.z;
                    float inaccuracy = 1.0F;
                    d2 += this.random.nextGaussian() * (double)0.0075F * (double)inaccuracy;
                    d3 += this.random.nextGaussian() * (double)0.0075F * (double)inaccuracy;
                    d4 += this.random.nextGaussian() * (double)0.0075F * (double)inaccuracy;
                    this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4.0F, 1.0F);
                    EntityDragonFireCharge entitylargefireball = new EntityDragonFireCharge((EntityType)IafEntityRegistry.FIRE_DRAGON_CHARGE.get(), this.level(), this, d2, d3, d4);
                    entitylargefireball.setPos(headVec.x, headVec.y, headVec.z);
                    if (!this.level().isClientSide) {
                        this.level().addFreshEntity(entitylargefireball);
                    }
                    this.stopCurrentAnimation();              // clear so timer resets
                    this.aiFireChargeTimer = 0;
                    // FIX 6: Null check MUST precede isAlive() to avoid NPE.
                    if (entity == null || !entity.isAlive()) {
                        this.setBreathingFire(false);
                    }

                    this.randomizeAttacks();
                }
            } else if (this.isBreathingFire()) {
                if (this.isActuallyBreathingFire()) {
                    // Activate the fire_breath GeckoLib controller so the jaw animates
                    // open while the stream is active. Without this the controller never
                    // sees ANIMATION_FIRECHARGE and the animation never plays.
                    if (this.getCurrentAnimation() != ANIMATION_FIRECHARGE) {
                        startAnimation(ANIMATION_FIRECHARGE);
                    }
                    this.setYRot(this.yBodyRot);
                    if (this.tickCount % 5 == 0) {
                        this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4.0F, 1.0F);
                    }

                    this.stimulateFire(entity.getX(), entity.getY(), entity.getZ(), 1);
                    // FIX 6: Null check MUST precede isAlive() to avoid NPE.
                    if (entity == null || !entity.isAlive()) {
                        this.setBreathingFire(false);
                        // Clear fire animation so the jaw closes immediately.
                        if (this.getCurrentAnimation() == ANIMATION_FIRECHARGE) {
                            this.stopCurrentAnimation();
                            this.ANIMATION_FIRECHARGE.stop();
                        }
                        this.randomizeAttacks();
                    }
                }
            } else {
                this.setBreathingFire(true);
            }
        }

        this.lookAt(entity, 360.0F, 360.0F);
    }

    // -----------------------------------------------------------------------
    // stimulateFire — particle + block/mob damage sync (unchanged in behaviour).
    // -----------------------------------------------------------------------
    public void stimulateFire(double burnX, double burnY, double burnZ, int syncType) {
        if (!MinecraftForge.EVENT_BUS.post(new DragonFireEvent(this, burnX, burnY, burnZ))) {
            if (syncType == 1 && !this.level().isClientSide) {
                IceAndFire.sendMSGToAll(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 0));
            }
            if (syncType == 2 && this.level().isClientSide) {
                IceAndFire.NETWORK_WRAPPER.sendToServer(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 0));
            }
            if (syncType == 3 && !this.level().isClientSide) {
                IceAndFire.sendMSGToAll(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 5));
            }
            if (syncType == 4 && this.level().isClientSide) {
                IceAndFire.NETWORK_WRAPPER.sendToServer(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 5));
            }

            if (syncType > 2 && syncType < 6) {
                if (this.currentActiveAnimation != ANIMATION_ROAR
                        && this.currentActiveAnimation != ANIMATION_EPIC_ROAR) {
                    this.startAnimation(ANIMATION_ROAR);
                } else if (this.aiFireChargeTimer == 20) {

                    this.setYRot(this.yBodyRot);
                    Vec3 headVec = this.getHeadPartPosition();
                    double d2 = burnX - headVec.x;
                    double d3 = burnY - headVec.y;
                    double d4 = burnZ - headVec.z;
                    d2 += this.random.nextGaussian() * 0.0075F;
                    d3 += this.random.nextGaussian() * 0.0075F;
                    d4 += this.random.nextGaussian() * 0.0075F;
                    this.playSound(DragonMod.SOUND_ROAR.get(), 4.0F, 1.0F);
                    EntityDragonFireCharge fireball = new EntityDragonFireCharge(
                            (EntityType) IafEntityRegistry.FIRE_DRAGON_CHARGE.get(),
                            this.level(), this, d2, d3, d4);
                    fireball.setPos(headVec.x, headVec.y, headVec.z);
                    if (!this.level().isClientSide) {
                        this.level().addFreshEntity(fireball);
                    }
                    this.stopCurrentAnimation();
                    this.aiFireChargeTimer = 0;
                    this.randomizeAttacks();
                }
            } else {
                this.getNavigation().stop();
                this.burnParticleX = burnX;
                this.burnParticleY = burnY;
                this.burnParticleZ = burnZ;
                // Use getHeadPosition() (animation-aware: accounts for flyProgress,
                // hoverProgress, dragonPitch, etc.) instead of getHeadPartPosition()
                // (static hitbox XYZ that ignores pose). This ensures the particle
                // stream visually originates from the rendered mouth in all states.
                Vec3 headPos = this.getHeadPosition();
                double d2 = burnX - headPos.x;
                double d3 = burnY - headPos.y;
                double d4 = burnZ - headPos.z;
                double distance   = Math.max(2.5F * this.distanceToSqr(burnX, burnY, burnZ), 0.0);
                // burnProgress is only incremented server-side and is never synced
                // to clients, so it is always 0 on the client — making conquered=0
                // and skipping the entire particle loop.  Use 40 (full stream) on
                // the client so particles always render along the whole visible ray.
                int effectiveBurnProgress = this.level().isClientSide ? 40 : this.burnProgress;
                double conquered  = (double) effectiveBurnProgress / 40.0 * distance;
                int increment     = (int) Math.ceil(conquered / 100.0);
                int particleCount = this.getDragonStage() <= 3 ? 2 : 1;

                // Offset the particle stream 2.0 world units away from the mouth so
                // fire_spit particles don't spawn clipped inside the dragon's head.
                // The parametric variable i runs from 0 to distance, where distance
                // corresponds to the full world-space separation between headPos and
                // the burn target.  To skip the first MOUTH_OFFSET world units we
                // compute the equivalent number of i-units and start the loop there.
                final double MOUTH_OFFSET = 2.0;
                double mouthWorldDist = Math.sqrt(d2 * d2 + d3 * d3 + d4 * d4);
                int iStart = (mouthWorldDist > 0.0)
                        ? (int) (MOUTH_OFFSET * distance / mouthWorldDist)
                        : 0;

                for (int i = iStart; (double) i < conquered; i += increment) {
                    double px = headPos.x + d2 * ((float) i / (float) distance);
                    double py = headPos.y + d3 * ((float) i / (float) distance);
                    double pz = headPos.z + d4 * ((float) i / (float) distance);
                    if (this.canPositionBeSeen(px, py, pz)) {
                        if (this.level().isClientSide && this.random.nextInt(particleCount) == 0) {
                            // fire_spit particle: velocity encodes ray direction so
                            // each puff drifts toward the target with turbulent spread.
                            // Tighter spread (0.06) gives a dense, focused fire beam.
                            double spread = 0.06;
                            double vx = (d2 / distance) * 0.3 + (this.random.nextDouble() - 0.5) * spread;
                            double vy = (d3 / distance) * 0.3 + (this.random.nextDouble() - 0.5) * spread;
                            double vz = (d4 / distance) * 0.3 + (this.random.nextDouble() - 0.5) * spread;
                            this.level().addParticle(
                                    DragonModParticles.FIRE_SPIT.get(),
                                    px, py, pz,
                                    vx, vy, vz);
                        }
                    } else if (!this.level().isClientSide) {
                        HitResult result = this.level().clip(new ClipContext(
                                new Vec3(this.getX(), this.getY() + this.getEyeHeight(), this.getZ()),
                                new Vec3(px, py, pz),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                        Vec3 vec3 = result.getLocation();
                        BlockPos pos = BlockPos.containing(vec3);
                        IafDragonDestructionManager.destroyAreaBreath(this.level(), pos, this);
                    }
                }

                if (this.burnProgress >= 40 && this.canPositionBeSeen(burnX, burnY, burnZ)) {
                    double sx = burnX + this.random.nextFloat() * 3.0F - 1.5F;
                    double sy = burnY + this.random.nextFloat() * 3.0F - 1.5F;
                    double sz = burnZ + this.random.nextFloat() * 3.0F - 1.5F;
                    if (!this.level().isClientSide) {
                        IafDragonDestructionManager.destroyAreaBreath(this.level(), BlockPos.containing(sx, sy, sz), this);
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CUSTOM SOUNDS
    //
    //  All IaF sound references are replaced with DragonMod registered sounds.
    //  Sound mapping:
    //    ambient / idle  → SOUND_SPEAK      (gentle idle vocalisation)
    //    hurt            → SOUND_ROAR       (pained roar)
    //    death           → SOUND_EPIC_ROAR  (death cry)
    //    roar            → SOUND_EPIC_ROAR  (combat roar)
    //    fire breath     → SOUND_ROAR       (breath attack)
    //
    //  Animation-triggered sounds (walk, run, bite, etc.) are played by
    //  EntityCustomDragon's animation callbacks — override those methods here
    //  so they use the custom sounds too.
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    protected SoundEvent getAmbientSound() {
        return DragonMod.SOUND_SPEAK.get();
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return DragonMod.SOUND_ROAR.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DragonMod.SOUND_EPIC_ROAR.get();
    }

    @Override
    public SoundEvent getRoarSound() {
        return DragonMod.SOUND_EPIC_ROAR.get();
    }

    // -----------------------------------------------------------------------
    // Animation sound hooks — called by EntityCustomDragon when an animation
    // reaches its sound-trigger tick.
    // -----------------------------------------------------------------------

    /** Plays the walking step sound (used by ANIMATION_WALK). */
    public SoundEvent getWalkSound() {
        return DragonMod.SOUND_WALK.get();
    }

    /** Plays the running step sound (used by ANIMATION_RUN). */
    public SoundEvent getRunSound() {
        return DragonMod.SOUND_RUN.get();
    }

    /** Plays the flight wing-beat sound (used by ANIMATION_WINGBLAST / flight). */
    public SoundEvent getFlightSound() {
        return DragonMod.SOUND_FLIGHT.get();
    }

    /** Plays the bite snap sound (used by ANIMATION_BITE). */
    public SoundEvent getBiteSound() {
        return DragonMod.SOUND_BITE.get();
    }

    /** Plays the shake-prey sound (used by ANIMATION_SHAKEPREY). */
    public SoundEvent getShakePreySound() {
        return DragonMod.SOUND_SHAKE_PREY.get();
    }

    /** Plays the tail-whack sound (used by ANIMATION_TAILWHACK). */
    public SoundEvent getTailWhackSound() {
        return DragonMod.SOUND_TAIL_WHACK.get();
    }

    /** Plays the eating sound (used by ANIMATION_EAT). */
    public SoundEvent getEatingSound() {
        return DragonMod.SOUND_EATING.get();
    }

    // -----------------------------------------------------------------------
    // Food / inventory
    // -----------------------------------------------------------------------
    public boolean isFood(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() != null
                && stack.getItem() == IafItemRegistry.FIRE_STEW.get();
    }

    // -----------------------------------------------------------------------
    // Particles
    // -----------------------------------------------------------------------
    protected void spawnDeathParticles() {
        for (int k = 0; k < 3; ++k) {
            double d2 = this.random.nextGaussian() * 0.02;
            double d0 = this.random.nextGaussian() * 0.02;
            double d1 = this.random.nextGaussian() * 0.02;
            if (this.level().isClientSide) {
                this.level().addParticle(ParticleTypes.FLAME,
                        this.getX() + (this.random.nextFloat() * this.getBbWidth() * 2.0F) - this.getBbWidth(),
                        this.getY() + (this.random.nextFloat() * this.getBbHeight()),
                        this.getZ() + (this.random.nextFloat() * this.getBbWidth() * 2.0F) - this.getBbWidth(),
                        d2, d0, d1);
            }
        }
    }

    protected void spawnBabyParticles() {
        for (int i = 0; i < 5; ++i) {
            float radiusAdd = i * 0.15F;
            float headPosX = (float) (this.getX() + 1.8F * this.getRenderSize() * (0.3F + radiusAdd)
                    * Mth.cos((float) ((this.getYRot() + 90.0F) * Math.PI / 180.0)));
            float headPosZ = (float) (this.getY() + 1.8F * this.getRenderSize() * (0.3F + radiusAdd)
                    * Mth.sin((float) ((this.getYRot() + 90.0F) * Math.PI / 180.0)));
            float headPosY = (float) (this.getZ() + 0.5F * this.getRenderSize() * 0.3F);
            this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                    headPosX, headPosY, headPosZ, 0.0, 0.0, 0.0);
        }
    }

    // -----------------------------------------------------------------------
    // Loot / drops
    // -----------------------------------------------------------------------
    public ItemStack getSkull() {
        return new ItemStack((ItemLike) IafItemRegistry.DRAGON_SKULL_FIRE.get());
    }

    @Override
    public ResourceLocation getDeadLootTable() {
        if (this.getDeathStage() >= this.getAgeInDays() / 5 / 2) {
            return SKELETON_LOOT;
        } else {
            return this.isMale() ? MALE_LOOT : FEMALE_LOOT;
        }
    }
    public Item getBloodItem()  { return (Item)     IafItemRegistry.FIRE_DRAGON_BLOOD.get();  }
    public Item getFleshItem()  { return (Item)     IafItemRegistry.FIRE_DRAGON_FLESH.get();  }
    public ItemLike getHeartItem() { return (ItemLike) IafItemRegistry.FIRE_DRAGON_HEART.get(); }
}