package org.chubby.github;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.api.FoodUtils;
import com.github.alexthe666.iceandfire.api.event.GenericGriefEvent;
import com.github.alexthe666.iceandfire.block.IDragonProof;
import com.github.alexthe666.iceandfire.client.model.IFChainBuffer;
import com.github.alexthe666.iceandfire.client.model.util.LegSolverQuadruped;
import com.github.alexthe666.iceandfire.datagen.tags.IafBlockTags;
import com.github.alexthe666.iceandfire.datagen.tags.IafItemTags;
import com.github.alexthe666.iceandfire.api.event.DragonFireEvent;
import com.github.alexthe666.iceandfire.entity.*;
import com.github.alexthe666.iceandfire.entity.EntityDragonEgg;
import com.github.alexthe666.iceandfire.message.MessageDragonSyncFire;
import com.github.alexthe666.iceandfire.entity.IafDragonAttacks.Air;
import com.github.alexthe666.iceandfire.entity.IafDragonAttacks.Ground;
import com.github.alexthe666.iceandfire.entity.ai.*;
import com.github.alexthe666.iceandfire.entity.props.EntityDataProvider;
import com.github.alexthe666.iceandfire.entity.tile.TileEntityDragonforgeInput;
import com.github.alexthe666.iceandfire.entity.util.*;
import com.github.alexthe666.iceandfire.enums.EnumDragonEgg;
import com.github.alexthe666.iceandfire.inventory.ContainerDragon;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.github.alexthe666.iceandfire.item.ItemDragonArmor;
import com.github.alexthe666.iceandfire.item.ItemSummoningCrystal;
import com.github.alexthe666.iceandfire.message.MessageDragonSetBurnBlock;
import com.github.alexthe666.iceandfire.message.MessageStartRidingMob;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import com.github.alexthe666.iceandfire.pathfinding.raycoms.AdvancedPathNavigate;
import com.github.alexthe666.iceandfire.pathfinding.raycoms.AdvancedPathNavigate.MovementType;
import com.github.alexthe666.iceandfire.pathfinding.raycoms.PathingStuckHandler;
import com.github.alexthe666.iceandfire.world.DragonPosWorldData;
import com.google.common.base.Predicate;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
public abstract class EntityCustomDragon extends EntityDragonBase implements GeoEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public static final int FLIGHT_CHANCE_PER_TICK = 1500;
    protected static final EntityDataAccessor<Boolean> SWIMMING;
    private static final UUID ARMOR_MODIFIER_UUID;
    private static final EntityDataAccessor<Integer> HUNGER;
    private static final EntityDataAccessor<Integer> AGE_TICKS;
    private static final EntityDataAccessor<Boolean> GENDER;
    private static final EntityDataAccessor<Integer> VARIANT;
    private static final EntityDataAccessor<Boolean> SLEEPING;
    private static final EntityDataAccessor<Boolean> FIREBREATHING;
    private static final EntityDataAccessor<Boolean> HOVERING;
    private static final EntityDataAccessor<Boolean> FLYING;
    private static final EntityDataAccessor<Boolean> MODEL_DEAD;
    private static final EntityDataAccessor<Integer> DEATH_STAGE;
    private static final EntityDataAccessor<Byte> CONTROL_STATE;
    private static final EntityDataAccessor<Boolean> TACKLE;
    private static final EntityDataAccessor<Boolean> AGINGDISABLED;
    private static final EntityDataAccessor<Integer> COMMAND;
    private static final EntityDataAccessor<Float> DRAGON_PITCH;
    private static final EntityDataAccessor<Boolean> CRYSTAL_BOUND;
    private static final EntityDataAccessor<String> CUSTOM_POSE;
    private static final EntityDataAccessor<Boolean> IS_EATING;

    /** Custom fire mode synced to all clients. 0=off  1=breath  2=fireball */
    private static final EntityDataAccessor<Byte> FIRE_MODE;

    /**
     * Synced animation identifier. Replaces Minecraft AnimationState as the
     * authority for GeckoLib controllers on BOTH sides — server sets it,
     * client reads it every frame. 0 = no one-shot animation active.
     */
    private static final EntityDataAccessor<Byte> SYNCED_ANIM_ID;

    // ── Animation IDs for SYNCED_ANIM_ID ──────────────────────────────────
    public static final byte ANIM_ID_NONE       = 0;
    public static final byte ANIM_ID_BITE       = 1;
    public static final byte ANIM_ID_SHAKEPREY  = 2;
    public static final byte ANIM_ID_WINGBLAST  = 3;
    public static final byte ANIM_ID_ROAR       = 4;
    public static final byte ANIM_ID_EPIC_ROAR  = 5;
    public static final byte ANIM_ID_TAILWHACK  = 6;
    public static final byte ANIM_ID_SPEAK      = 7;
    public static final byte ANIM_ID_FIRECHARGE = 8;
    public static final byte ANIM_ID_EAT        = 9;
    public final AnimationState NO_ANIMATION = new AnimationState();
    public final AnimationState ANIMATION_FIRECHARGE= new AnimationState();
    public final AnimationState ANIMATION_EAT=new AnimationState();
    public final AnimationState ANIMATION_SPEAK=new AnimationState();
    public final AnimationState ANIMATION_BITE=new AnimationState();
    public final AnimationState ANIMATION_SHAKEPREY=new AnimationState();
    public final AnimationState ANIMATION_WINGBLAST=new AnimationState();
    public final AnimationState ANIMATION_ROAR=new AnimationState();
    public final AnimationState ANIMATION_EPIC_ROAR=new AnimationState();
    public final AnimationState ANIMATION_TAILWHACK=new AnimationState();
    public final AnimationState ANIMATION_IDLE = new AnimationState();
    public final AnimationState ANIMATION_WALK = new AnimationState();
    public final AnimationState ANIMATION_RUN  = new AnimationState();
    public DragonType dragonType;
    public double minimumDamage;
    public double maximumDamage;
    public double minimumHealth;
    public double maximumHealth;
    public double minimumSpeed;
    public double maximumSpeed;
    public double minimumArmor;
    public double maximumArmor;
    public float sitProgress;
    public float sleepProgress;
    public float hoverProgress;
    public float flyProgress;
    public float fireBreathProgress;
    public float diveProgress;
    public float prevDiveProgress;
    public float diveBlend = 0.0f;
    private int diveCooldownTicks = 0;
    /** Dedicated fireball cooldown — separate from fireStopTicks so the striking
     *  system (which sets fireStopTicks=10 every tick) can't block fireballs. */
    private int fireballCooldownTicks = 0;
    /**
     * Countdown timer (in ticks) for the eat animation.
     * While > 0 the animation is active and new feeds will be ignored.
     * Matches eat JSON animation_length: 9.7917 s × 20 ticks/s = 196 ticks.
     */
    private int eatAnimTicks = 0;
    /**
     * Countdown timer that holds SYNCED_ANIM_ID at ANIM_ID_SPEAK for the
     * duration of the talk animation (~2 s = 40 ticks).  ANIMATION_SPEAK is
     * NOT stored in currentActiveAnimation, so without this the per-tick sync
     * in aiStep() would immediately overwrite the speak ID with ANIM_ID_NONE.
     */
    private int speakAnimTicks = 0;
    private static final int SPEAK_ANIM_DURATION = 40; // ~2 s at 20 t/s
    public static final int EAT_ANIM_DURATION = 196;

    /**
     * Duration (ticks) the fire_breath_stop animation is given to finish after
     * isBreathingFire() drops to false.  Must match your animation length:
     *   fire_breath_stop = 2.75 s × 20 ticks/s = 55 ticks.
     * +1 because aiStep() decrements the counter BEFORE the GeckoLib controller
     * reads it on the same tick, so the effective active window = value − 1.
     * 56 − 1 = 55 active ticks → exactly covers the animation.
     */
    private static final int FIRE_BREATH_STOP_DURATION = 56;

    /**
     * Counts down while fire_breath_stop is playing.
     * Set to FIRE_BREATH_STOP_DURATION the tick breathing stops.
     */
    private int fireBreathStopTicks = 0;

    /**
     * Tracks FIRE_MODE from the previous tick so we detect the 1→other edge
     * that arms the fire_breath_stop animation.  Using FIRE_MODE (synced data)
     * instead of isBreathingFire() means IaF's internal fireStopTicks resets
     * can never cause a false stop trigger mid-hold.
     */
    private byte prevFireMode = 0;

    /** Cooldown so tail_swing doesn't fire every doHurtTarget call. */
    int tailSwingCooldown = 0;

    /**
     * Lifetime counter for the tail_swing animation (ticks).
     * Set to 48 when the animation starts (47 ticks = 2.333 s, +1 buffer).
     * When it reaches 0, currentActiveAnimation is cleared so GeckoLib's
     * PLAY_ONCE controller stops restarting the finished animation every frame.
     */
    int tailAnimTicks = 0;

    public float prevFireBreathProgress;
    public int fireStopTicks;
    public int flyTicks;
    public float modelDeadProgress;
    public float prevModelDeadProgress;
    public float ridingProgress;
    public float tackleProgress;
    public boolean isSwimming;
    public float prevSwimProgress;
    public float swimProgress;
    public int ticksSwiming;
    public int swimCycle;
    public float[] prevAnimationProgresses = new float[10];
    public boolean isDaytime;
    public int flightCycle;
    public HomePosition homePos;
    public boolean hasHomePosition = false;
    public IFChainBuffer roll_buffer;
    public IFChainBuffer pitch_buffer;
    public IFChainBuffer pitch_buffer_body;
    public ReversedBuffer turn_buffer;
    public ChainBuffer tail_buffer;
    public int spacebarTicks;
    public static final float[] growth_stage_1;
    public static final float[] growth_stage_2;
    public static final float[] growth_stage_3;
    public static final float[] growth_stage_4;
    public static final float[] growth_stage_5;
    public float[][] growth_stages;
    public LegSolverQuadruped legSolver;
    public int walkCycle;
    public BlockPos burningTarget;
    public int burnProgress;
    public double burnParticleX;
    public double burnParticleY;
    public double burnParticleZ;
    public float prevDragonPitch;
    public IafDragonAttacks.Air airAttack;
    public IafDragonAttacks.Ground groundAttack;
    public boolean usingGroundAttack;
    public IafDragonLogic logic;
    public int hoverTicks;
    public int tacklingTicks;
    public int ticksStill;
    public int navigatorType;
    // dragonInventory is declared and owned by EntityDragonBase (IaF).
    // It must NOT be re-declared here — doing so creates a second, separate
    // field that shadows the IaF field.  EntityDragonBase's own compiled code
    // always resolves "this.dragonInventory" to its own class-level field,
    // so if we shadow it with a child-class field, IaF's field is never
    // initialised and readAdditionalSaveData throws a NullPointerException
    // (visible as the Dragon Horn crash: EntityDragonBase.java:792).
    public String prevArmorResLoc;
    public String armorResLoc;
    public IafDragonFlightManager flightManager;
    public boolean lookingForRoostAIFlag;
    protected int flyHovering;
    protected boolean hasHadHornUse;
    protected int fireTicks;
    protected int blockBreakCounter;
    /** Rider-fire counters — completely separate from IaF's fireTicks/burnProgress
     *  so IaF's updateDragonServer() resets can never interfere. */
    private int riderFireTicks    = 0;
    private int riderBurnProgress = 0;
    private int prevFlightCycle;
    private boolean isModelDead;
    AnimationState currentActiveAnimation;
    private float lastScale;
    private int lastStage = -1;
    protected EntityDragonPart headPart;
    private EntityDragonPart neckPart;
    private EntityDragonPart rightWingUpperPart;
    private EntityDragonPart rightWingLowerPart;
    private EntityDragonPart leftWingUpperPart;
    private EntityDragonPart leftWingLowerPart;
    private EntityDragonPart tail1Part;
    private EntityDragonPart tail2Part;
    private EntityDragonPart tail3Part;
    private EntityDragonPart tail4Part;
    private boolean isOverAir;
    private LazyOptional<?> itemHandler;
    public boolean allowLocalMotionControl;
    public boolean allowMousePitchControl;
    protected boolean gliding;
    protected float glidingSpeedBonus;
    protected float riderWalkingExtraY;

    public EntityCustomDragon(EntityType t, Level world, DragonType type, double minimumDamage, double maximumDamage, double minimumHealth, double maximumHealth, double minimumSpeed, double maximumSpeed) {
        super(t, world,type,minimumDamage,maximumDamage,minimumHealth,maximumHealth,minimumSpeed,maximumSpeed);
        this.growth_stages = new float[][]{growth_stage_1, growth_stage_2, growth_stage_3, growth_stage_4, growth_stage_5};
        this.usingGroundAttack = true;
        this.prevArmorResLoc = "0|0|0|0";
        this.armorResLoc = "0|0|0|0";
        this.lookingForRoostAIFlag = false;
        this.hasHadHornUse = false;
        this.itemHandler = null;
        this.allowLocalMotionControl = true;
        this.allowMousePitchControl = true;
        this.gliding = false;
        this.glidingSpeedBonus = 0.0F;
        this.riderWalkingExtraY = 0.0F;
        this.dragonType = type;
        this.minimumDamage = minimumDamage;
        this.maximumDamage = maximumDamage;
        this.minimumHealth = minimumHealth;
        this.maximumHealth = maximumHealth;
        this.minimumSpeed = minimumSpeed;
        this.maximumSpeed = maximumSpeed;
        this.minimumArmor = (double)1.0F;
        this.maximumArmor = (double)20.0F;
        this.createInventory();
        if (world.isClientSide) {
            this.roll_buffer = new IFChainBuffer();
            this.pitch_buffer = new IFChainBuffer();
            this.pitch_buffer_body = new IFChainBuffer();
            this.turn_buffer = new ReversedBuffer();
            this.tail_buffer = new ChainBuffer();
        }

        this.legSolver = new LegSolverQuadruped(0.3F, 0.35F, 0.2F, 1.45F, 1.0F);
        this.flightManager = new IafDragonFlightManager(this);
        this.logic = this.createDragonLogic();
        this.noCulling = true;
        this.switchNavigator(0);
        this.randomizeAttacks();
        this.resetParts(1.0F);
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, (double)20.0F).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, (double)1.0F).add(Attributes.FOLLOW_RANGE, (double)Math.min(2048, IafConfig.dragonTargetSearchLength)).add(Attributes.ARMOR, (double)4.0F);
    }

    public void setConfigurableAttributes() {
        this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue((double)Math.min(2048, IafConfig.dragonTargetSearchLength));
    }

    public @NotNull BlockPos getRestrictCenter() {
        return this.homePos == null ? super.getRestrictCenter() : this.homePos.getPosition();
    }

    public float getRestrictRadius() {
        return (float)IafConfig.dragonWanderFromHomeDistance;
    }

    public String getHomeDimensionName() {
        return this.homePos == null ? "" : this.homePos.getDimension();
    }

    public boolean hasRestriction() {
        return this.hasHomePosition && this.getHomeDimensionName().equals(DragonUtils.getDimensionName(this.level())) || super.hasRestriction();
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new DragonAIMate(this, (double)1.0F));
        this.goalSelector.addGoal(3, new DragonAIReturnToRoost(this, (double)1.0F));
        this.goalSelector.addGoal(4, new DragonAIEscort(this, (double)1.0F));
        this.goalSelector.addGoal(5, new DragonAIAttackMelee(this, (double)1.5F, false));
        this.goalSelector.addGoal(6, new TemptGoal(this, (double)1.0F, Ingredient.of(IafItemTags.TEMPT_DRAGON), false));
        this.goalSelector.addGoal(7, new DragonAIWander(this, (double)1.0F));
        this.goalSelector.addGoal(8, new DragonAIWatchClosest(this, LivingEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new DragonAILookIdle(this));
        this.targetSelector.addGoal(1, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this, new Class[0]));
        this.targetSelector.addGoal(4, new DragonAITargetItems(this, 60, false, false, true));
        this.targetSelector.addGoal(5, new DragonAITargetNonTamed<LivingEntity>(this, LivingEntity.class, false, (Predicate<LivingEntity>)(entity) -> {
            if (entity instanceof Player player) {
                return !player.isCreative();
            } else if (this.getRandom().nextInt(100) <= this.getHunger()) {
                return false;
            } else {
                return entity.getType() != this.getType() && DragonUtils.canHostilesTarget(entity) && DragonUtils.isAlive(entity) && this.shouldTarget(entity);
            }
        }));
        this.targetSelector.addGoal(6, new DragonAITarget<>(this, LivingEntity.class, true, (entity) -> DragonUtils.canHostilesTarget(entity) && entity.getType() != this.getType() && this.shouldTarget(entity) && DragonUtils.isAlive(entity)));
        this.targetSelector.addGoal(7, new DragonAITargetItems(this, false));
    }

    protected abstract boolean shouldTarget(Entity var1);

    public void resetParts(float scale) {
        this.removeParts();

        if (this.getDragonStage() <= 2) {
            // ── Baby geo part hitboxes ─────────────────────────────────────────
            // `scale` == getVisualScale() here (set by refreshDimensions).
            // Box sizes are based on the geo cubes with a small padding margin.
            // The parts cover the visually rendered model, not the tiny collision box.

            // Head (geo cube ≈ 2×1×2 units → 0.125×0.063×0.125 b; padded × ~1.5)
            this.headPart = new EntityDragonPart(this,
                    0.639f * scale,  0.0f,
                    0.22f  * scale,  0.14f * scale,  0.22f * scale, 1.5f);
            this.headPart.copyPosition(this);
            this.headPart.setParent(this);

            // Neck (geo cube ≈ 2×2×4 units → 0.125×0.125×0.25 b)
            this.neckPart = new EntityDragonPart(this,
                    0.453f * scale,  0.0f,
                    0.16f  * scale,  0.13f * scale,  0.16f * scale, 1.0f);
            this.neckPart.copyPosition(this);
            this.neckPart.setParent(this);

            // Right wing – upper/shoulder (RightWing shoulder arm, x≈-3.50 → 0.219 b)
            this.rightWingUpperPart = new EntityDragonPart(this,
                    0.219f * scale, 90.0f,
                    0.40f  * scale,  0.10f * scale,  0.42f * scale, 0.5f);
            this.rightWingUpperPart.copyPosition(this);
            this.rightWingUpperPart.setParent(this);

            // Right wing – lower/arm (RightWingArm cube, x≈-10.25 → 0.641 b)
            this.rightWingLowerPart = new EntityDragonPart(this,
                    0.641f * scale, 92.0f,
                    0.32f  * scale,  0.08f * scale,  0.36f * scale, 0.5f);
            this.rightWingLowerPart.copyPosition(this);
            this.rightWingLowerPart.setParent(this);

            // Left wing – upper (mirror of right)
            this.leftWingUpperPart = new EntityDragonPart(this,
                    0.219f * scale, -90.0f,
                    0.40f  * scale,  0.10f * scale,  0.42f * scale, 0.5f);
            this.leftWingUpperPart.copyPosition(this);
            this.leftWingUpperPart.setParent(this);

            // Left wing – lower (mirror of right)
            this.leftWingLowerPart = new EntityDragonPart(this,
                    0.641f * scale, -92.0f,
                    0.32f  * scale,  0.08f * scale,  0.36f * scale, 0.5f);
            this.leftWingLowerPart.copyPosition(this);
            this.leftWingLowerPart.setParent(this);

            // Tail segment 1 (Tail bone, z≈+3.25 units → 0.203 b back)
            this.tail1Part = new EntityDragonPart(this,
                    -0.203f * scale,  0.0f,
                    0.24f   * scale,  0.14f * scale,  0.24f * scale, 1.0f);
            this.tail1Part.copyPosition(this);
            this.tail1Part.setParent(this);

            // Tail segment 2 (Tail2 bone inner cube, z≈+6.75 units → 0.422 b back)
            this.tail2Part = new EntityDragonPart(this,
                    -0.422f * scale,  0.0f,
                    0.19f   * scale,  0.11f * scale,  0.22f * scale, 1.0f);
            this.tail2Part.copyPosition(this);
            this.tail2Part.setParent(this);

            // Tail segments 3 & 4 — baby geo only has two tail bones; these are
            // placed further back as thin "tip" markers so they still exist for
            // code that assumes 4 tail parts.
            this.tail3Part = new EntityDragonPart(this,
                    -0.60f * scale,   0.0f,
                    0.14f  * scale,   0.09f * scale,  0.16f * scale, 1.0f);
            this.tail3Part.copyPosition(this);
            this.tail3Part.setParent(this);

            this.tail4Part = new EntityDragonPart(this,
                    -0.78f * scale,   0.0f,
                    0.11f  * scale,   0.07f * scale,  0.13f * scale, 1.5f);
            this.tail4Part.copyPosition(this);
            this.tail4Part.setParent(this);

        } else {
            // ── Adult geo part hitboxes ────────────────────────────────────────
            // `scale` == getVisualScale() == getScale() for adults.
            // Offsets in MC blocks × scale, derived from dragon_geo.json bone centres.

            // Head (head2 + jaw, z≈-8.60 b forward)
            this.headPart = new EntityDragonPart(this,
                    8.6f  * scale, 0.0f,
                    1.80f * scale, 1.25f * scale, 2.20f * scale, 1.5f);
            this.headPart.copyPosition(this);
            this.headPart.setParent(this);

            // Neck (avg neck1 + neck2 centre, z≈-6.05 b)
            this.neckPart = new EntityDragonPart(this,
                    6.05f * scale, 0.0f,
                    1.40f * scale, 1.30f * scale, 1.60f * scale, 1.0f);
            this.neckPart.copyPosition(this);
            this.neckPart.setParent(this);

            // Right wing – upper (RightElbow, x≈-5.75 b sideways)
            this.rightWingUpperPart = new EntityDragonPart(this,
                    5.75f  * scale, 90.0f,
                    1.35f  * scale, 1.10f * scale, 1.60f * scale, 0.5f);
            this.rightWingUpperPart.copyPosition(this);
            this.rightWingUpperPart.setParent(this);

            // Right wing – lower (RightWingBone, x≈-13.61 b sideways).
            // Pushed from 13.61→18.5 b to reach the visible wing membrane tip,
            // and box widened from 1.15×0.90×1.45 → 2.20×0.80×2.60 to cover
            // the broad wing surface rather than just the bone.
            this.rightWingLowerPart = new EntityDragonPart(this,
                    18.5f  * scale, 92.0f,
                    2.20f  * scale, 0.80f * scale, 2.60f * scale, 0.5f);
            this.rightWingLowerPart.copyPosition(this);
            this.rightWingLowerPart.setParent(this);

            // Left wing – upper (mirror of right)
            this.leftWingUpperPart = new EntityDragonPart(this,
                    5.75f  * scale, -90.0f,
                    1.35f  * scale,  1.10f * scale, 1.60f * scale, 0.5f);
            this.leftWingUpperPart.copyPosition(this);
            this.leftWingUpperPart.setParent(this);

            // Left wing – lower (mirror of right — same wider dimensions)
            this.leftWingLowerPart = new EntityDragonPart(this,
                    18.5f  * scale, -92.0f,
                    2.20f  * scale,  0.80f * scale, 2.60f * scale, 0.5f);
            this.leftWingLowerPart.copyPosition(this);
            this.leftWingLowerPart.setParent(this);

            // Tail segment 1 (TailSeg1, z≈+1.69 b back)
            this.tail1Part = new EntityDragonPart(this,
                    -1.69f * scale,  0.0f,
                    1.35f  * scale, 1.10f * scale, 1.35f * scale, 1.0f);
            this.tail1Part.copyPosition(this);
            this.tail1Part.setParent(this);

            // Tail segment 2 (TailSeg2, z≈+3.81 b back)
            this.tail2Part = new EntityDragonPart(this,
                    -3.81f * scale,  0.0f,
                    1.15f  * scale, 0.95f * scale, 1.15f * scale, 1.0f);
            this.tail2Part.copyPosition(this);
            this.tail2Part.setParent(this);

            // Tail segment 3 (TailSeg3, z≈+7.84 b back)
            this.tail3Part = new EntityDragonPart(this,
                    -7.84f * scale,  0.0f,
                    0.92f  * scale, 0.80f * scale, 0.92f * scale, 1.0f);
            this.tail3Part.copyPosition(this);
            this.tail3Part.setParent(this);

            // Tail segment 4 (TailSeg4 tip, z≈+13.03 b back)
            this.tail4Part = new EntityDragonPart(this,
                    -13.03f * scale,  0.0f,
                    0.68f   * scale, 0.65f * scale, 0.68f * scale, 1.5f);
            this.tail4Part.copyPosition(this);
            this.tail4Part.setParent(this);
        }
    }

    @Override
    public void updateParts() {
        // ── Snapshot old positions BEFORE moving parts ─────────────────────────
        // Minecraft renders entities by interpolating between (xo,yo,zo) and
        // (x,y,z) using the current partial tick.  EntityDragonPart inherits
        // this behaviour but setPos() does NOT update xo/yo/zo — only
        // Entity.baseTick() does that, and dragon parts never get their own tick.
        // Without this snapshot, every part always interpolates from its creation
        // origin toward its current position, making the hitbox visually trail
        // the animated model by anything up to a full tick.
        // Saving x/y/z → xo/yo/zo here (before we write new x/y/z below) gives
        // a clean one-tick delta so the interpolation stays in frame with the model.
        savePartOldPositions();

        // Recompute all part world positions from the current entity state.
        computePartPositions();
    }

    /**
     * Recomputes world positions for all hitbox parts from the current entity
     * position and {@code yBodyRot}, <em>without</em> touching the interpolation
     * origin fields ({@code xo/yo/zo}).
     *
     * <p>Called by {@link #updateParts()} every tick <em>and</em> by
     * {@link #updatePartsForRender(float)} every render frame so the hitbox
     * debug boxes (F3+B) stay tightly aligned with the model at any partial tick.
     */
    private void computePartPositions() {
        // Step 1 — IaF utility sets X/Z from each part's forwardOffset + yawOffset.
        EntityUtil.updatePart(this.headPart,           this);
        EntityUtil.updatePart(this.neckPart,           this);
        EntityUtil.updatePart(this.rightWingUpperPart, this);
        EntityUtil.updatePart(this.rightWingLowerPart, this);
        EntityUtil.updatePart(this.leftWingUpperPart,  this);
        EntityUtil.updatePart(this.leftWingLowerPart,  this);
        EntityUtil.updatePart(this.tail1Part,          this);
        EntityUtil.updatePart(this.tail2Part,          this);
        EntityUtil.updatePart(this.tail3Part,          this);
        EntityUtil.updatePart(this.tail4Part,          this);

        // Step 2 — Fix Y: EntityUtil.updatePart places parts at entity.getY() (feet).
        // Lift each part to its actual bone-centre height in the world.
        // Formula:  worldY = entity.getY()  +  geo_bone_y_blocks * visualScale
        float vs = this.getVisualScale();   // parts track the rendered model

        if (this.getDragonStage() <= 2) {
            // ── Baby geo Y positions ───────────────────────────────────────────
            // geo bone Y values (in MC blocks at scale 1.0):
            //   head cubes     y≈4.20/16 = 0.263 b
            //   neck cubes     y≈3.75/16 = 0.234 b
            //   wing bones     y≈4.00/16 = 0.250 b
            //   tail bones     y≈3.50/16 = 0.219 b
            double headY  = this.getY() + 0.263f * vs;
            double neckY  = this.getY() + 0.234f * vs;
            double wingY  = this.getY() + 0.250f * vs;
            double tailY  = this.getY() + 0.219f * vs;

            liftPart(this.headPart,           headY);
            liftPart(this.neckPart,           neckY);
            liftPart(this.rightWingUpperPart, wingY);
            liftPart(this.rightWingLowerPart, wingY);
            liftPart(this.leftWingUpperPart,  wingY);
            liftPart(this.leftWingLowerPart,  wingY);
            liftPart(this.tail1Part,          tailY);
            liftPart(this.tail2Part,          tailY);
            liftPart(this.tail3Part,          tailY);
            liftPart(this.tail4Part,          tailY);

        } else {
            // ── Adult geo Y positions ──────────────────────────────────────────
            // geo bone Y values (in MC blocks at scale 1.0, from dragon_geo.json):
            //   head2 / jaw    y≈3.00 b
            //   neck avg       y≈2.91 b
            //   wing bones     y≈3.12 b
            //   tail seg 1–4   y≈2.75 / 2.97 / 3.00 / 3.03 b
            double headY  = this.getY() + 3.00f * vs;
            double neckY  = this.getY() + 2.91f * vs;
            double wingY  = this.getY() + 3.12f * vs;

            liftPart(this.headPart,           headY);
            liftPart(this.neckPart,           neckY);
            liftPart(this.rightWingUpperPart, wingY);
            liftPart(this.rightWingLowerPart, wingY);
            liftPart(this.leftWingUpperPart,  wingY);
            liftPart(this.leftWingLowerPart,  wingY);
            liftPart(this.tail1Part,          this.getY() + 2.75f * vs);
            liftPart(this.tail2Part,          this.getY() + 2.97f * vs);
            liftPart(this.tail3Part,          this.getY() + 3.00f * vs);
            liftPart(this.tail4Part,          this.getY() + 3.03f * vs);
        }
    }

    /**
     * Call this from your {@code GeoEntityRenderer.render()} override
     * <em>before</em> the {@code super.render()} call.
     *
     * <h3>Why this is needed</h3>
     * {@link #updateParts()} runs at 20 TPS.  Between ticks the game
     * renders at 60+ FPS using partial-tick interpolation: the model's body
     * rotation interpolates from {@code yBodyRotO} → {@code yBodyRot} each
     * frame, while the hitbox parts stay fixed at their tick-end positions.
     * Two problems arise:
     * <ol>
     *   <li><b>Cartesian drift.</b>  Linear interpolation between two
     *       circle-offset positions curves <em>inward</em> — parts appear to
     *       slide toward the entity centre during fast yaw changes (mouse
     *       movement), then pop back on the next tick.</li>
     *   <li><b>Animation de-sync.</b>  GeckoLib bone positions change every
     *       frame; the static offset hitboxes don't track neck/head sway.</li>
     * </ol>
     *
     * <p>This method temporarily substitutes the per-frame interpolated yaw
     * for {@code yBodyRot}, recomputes all part positions with
     * {@link #computePartPositions()}, then restores the original value.
     * The interpolation origins ({@code xo/yo/zo}) are intentionally left
     * unchanged so per-tick translation interpolation still works correctly.
     *
     * <h3>Renderer usage (Kotlin-style pseudocode)</h3>
     * <pre>{@code
     * override fun render(entity, entityYaw, partialTick, poseStack, buffer, light) {
     *     entity.updatePartsForRender(partialTick)   // <-- add this line
     *     super.render(entity, entityYaw, partialTick, poseStack, buffer, light)
     * }
     * }</pre>
     */
    public void updatePartsForRender(float partialTick) {
        if (this.headPart == null) return;

        // Substitute the per-frame interpolated yaw so computePartPositions()
        // places hitbox parts exactly where the rendered model is this frame.
        float savedYaw = this.yBodyRot;
        this.yBodyRot  = net.minecraft.util.Mth.rotLerp(partialTick, this.yBodyRotO, this.yBodyRot);
        computePartPositions();
        this.yBodyRot  = savedYaw;

        // Align each part's xo/yo/zo to its new x/y/z.
        //
        // WHY THIS IS NEEDED:
        // computePartPositions() only writes x/y/z (current position).
        // Minecraft renders entities by interpolating xo→x each frame.
        // The game-tick savePartOldPositions() (called at 20 TPS inside
        // updateParts()) is the *authoritative* writer of xo/yo/zo, but it
        // runs BEFORE computePartPositions() on that same tick.  By the time
        // the renderer calls updatePartsForRender() the x/y/z values are
        // frame-interpolated positions, not tick-end positions, so the delta
        // (x - xo) is a mix of physics-tick delta and frame-fraction delta.
        // This produces visible part "scatter" — the boxes drift in the
        // direction of motion and snap back each tick.
        //
        // By writing xo = x here (after the frame computation) we set the
        // delta to zero so Minecraft renders the part exactly at x/y/z with
        // no additional interpolation.  On the next physics tick,
        // savePartOldPositions() overwrites xo/yo/zo with the correct
        // tick-start snapshot, restoring normal per-tick interpolation.
        syncPartOldToCurrentForRender();
    }

    private void syncPartOldToCurrentForRender() {
        syncOldToCurrentForRender(this.headPart);
        syncOldToCurrentForRender(this.neckPart);
        syncOldToCurrentForRender(this.rightWingUpperPart);
        syncOldToCurrentForRender(this.rightWingLowerPart);
        syncOldToCurrentForRender(this.leftWingUpperPart);
        syncOldToCurrentForRender(this.leftWingLowerPart);
        syncOldToCurrentForRender(this.tail1Part);
        syncOldToCurrentForRender(this.tail2Part);
        syncOldToCurrentForRender(this.tail3Part);
        syncOldToCurrentForRender(this.tail4Part);
    }

    private static void syncOldToCurrentForRender(EntityDragonPart part) {
        if (part == null) return;
        part.xo = part.getX();
        part.yo = part.getY();
        part.zo = part.getZ();
    }

    private void liftPart(EntityDragonPart part, double worldY) {
        if (part == null) return;
        part.setPos(part.getX(), worldY, part.getZ());
    }

    /**
     * Copies each part's current world position into its xo/yo/zo "old-position"
     * fields before the part is repositioned this tick.
     *
     * <p>Minecraft interpolates entity rendering between (xo,yo,zo) and (x,y,z)
     * using the current partial-tick fraction.  Because dragon parts don't run
     * their own baseTick(), their xo/yo/zo are never automatically updated.
     * Calling this at the start of updateParts() ensures the old-position is the
     * where the part was last tick, so the visual hitbox box tracks the animated
     * model frame-accurately rather than drifting behind it.
     */
    private void savePartOldPositions() {
        saveOldPos(this.headPart);
        saveOldPos(this.neckPart);
        saveOldPos(this.rightWingUpperPart);
        saveOldPos(this.rightWingLowerPart);
        saveOldPos(this.leftWingUpperPart);
        saveOldPos(this.leftWingLowerPart);
        saveOldPos(this.tail1Part);
        saveOldPos(this.tail2Part);
        saveOldPos(this.tail3Part);
        saveOldPos(this.tail4Part);
    }

    private static void saveOldPos(EntityDragonPart part) {
        if (part == null) return;
        part.xo = part.getX();
        part.yo = part.getY();
        part.zo = part.getZ();
    }

    protected void updateBurnTarget() {
        if (this.burningTarget != null && !this.isSleeping() && !this.isModelDead() && !this.isBaby()) {
            float maxDist = (float)(115 * this.getDragonStage());
            BlockEntity var3 = this.level().getBlockEntity(this.burningTarget);
            if (var3 instanceof TileEntityDragonforgeInput) {
                TileEntityDragonforgeInput forge = (TileEntityDragonforgeInput)var3;
                if (forge.isAssembled() && this.distanceToSqr((double)this.burningTarget.getX() + (double)0.5F, (double)this.burningTarget.getY() + (double)0.5F, (double)this.burningTarget.getZ() + (double)0.5F) < (double)maxDist && this.canPositionBeSeen((double)this.burningTarget.getX() + (double)0.5F, (double)this.burningTarget.getY() + (double)0.5F, (double)this.burningTarget.getZ() + (double)0.5F)) {
                    this.getLookControl().setLookAt((double)this.burningTarget.getX() + (double)0.5F, (double)this.burningTarget.getY() + (double)0.5F, (double)this.burningTarget.getZ() + (double)0.5F, 180.0F, 180.0F);
                    this.breathFireAtPos(this.burningTarget);
                    this.setBreathingFire(true);
                    return;
                }
            }

            if (!this.level().isClientSide) {
                IceAndFire.sendMSGToAll(new MessageDragonSetBurnBlock(this.getId(), true, this.burningTarget));
            }

            this.burningTarget = null;
        }

    }

    // breathFireAtPos is implemented further below in the CUSTOM FIRE SYSTEM section.

    protected PathingStuckHandler createStuckHandler() {
        return PathingStuckHandler.createStuckHandler();
    }

    protected @NotNull PathNavigation createNavigation(@NotNull Level worldIn) {
        return this.createNavigator(worldIn, MovementType.WALKING);
    }

    protected PathNavigation createNavigator(Level worldIn, AdvancedPathNavigate.MovementType type) {
        return this.createNavigator(worldIn, type, this.createStuckHandler());
    }

    protected PathNavigation createNavigator(Level worldIn, AdvancedPathNavigate.MovementType type, PathingStuckHandler stuckHandler) {
        return this.createNavigator(worldIn, type, stuckHandler, 4.0F, 4.0F);
    }

    protected PathNavigation createNavigator(Level worldIn, AdvancedPathNavigate.MovementType type, PathingStuckHandler stuckHandler, float width, float height) {
        AdvancedPathNavigate newNavigator = new AdvancedPathNavigate(this, this.level(), type, width, height);
        this.navigation = newNavigator;
        newNavigator.setCanFloat(true);
        newNavigator.getNodeEvaluator().setCanOpenDoors(true);
        return newNavigator;
    }

    protected void switchNavigator(int navigatorType) {
        super.switchNavigator(navigatorType);

    }

    public boolean canRide(@NotNull Entity rider) {
        return true;
    }

    protected void customServerAiStep() {
        super.customServerAiStep();
        this.breakBlocks(false);
    }

    public void checkDespawn() {
        if (IafConfig.canDragonsDespawn) {
            super.checkDespawn();
        }

    }

    public boolean canDestroyBlock(BlockPos pos, BlockState state) {
        return state.getBlock().canEntityDestroy(state, this.level(), pos, this);
    }

    public boolean isMobDead() {
        return this.isModelDead();
    }

    public int getMaxHeadYRot() {
        return 30 * this.getDragonStage() / 5;
    }

    public void openInventory(Player player) {
        if (!this.level().isClientSide) {
            NetworkHooks.openScreen((ServerPlayer)player, this.getMenuProvider());
        }

        IceAndFire.PROXY.setReferencedMob(this);
    }

    public MenuProvider getMenuProvider() {
        return new SimpleMenuProvider((containerId, playerInventory, player) -> new ContainerDragon(containerId, this.dragonInventory, playerInventory, this), this.getDisplayName());
    }

    public int getAmbientSoundInterval() {
        return 90;
    }

    protected void tickDeath() {
        this.deathTime = 0;
        this.setModelDead(true);
        this.ejectPassengers();
        if (this.getDeathStage() >= this.getAgeInDays() / 5) {
            this.remove(RemovalReason.KILLED);

            for(int k = 0; k < 40; ++k) {
                double d2 = this.random.nextGaussian() * 0.02;
                double d0 = this.random.nextGaussian() * 0.02;
                double d1 = this.random.nextGaussian() * 0.02;
                if (this.level().isClientSide) {
                    this.level().addParticle(ParticleTypes.CLOUD, this.getX() + (double)(this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(), this.getY() + (double)(this.random.nextFloat() * this.getBbHeight()), this.getZ() + (double)(this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(), d2, d0, d1);
                }
            }

            this.spawnDeathParticles();
        }

    }

    protected void spawnDeathParticles() {
    }

    protected void spawnBabyParticles() {
    }

    public void remove(Entity.@NotNull RemovalReason reason) {
        this.removeParts();
        super.remove(reason);
    }

    public int getExperienceReward() {
        short var10000;
        switch (this.getDragonStage()) {
            case 2 -> var10000 = 20;
            case 3 -> var10000 = 150;
            case 4 -> var10000 = 300;
            case 5 -> var10000 = 650;
            default -> var10000 = 5;
        }

        return var10000;
    }

    public int getArmorOrdinal(ItemStack stack) {
        if (!stack.isEmpty()) {
            Item var3 = stack.getItem();
            if (var3 instanceof ItemDragonArmor) {
                ItemDragonArmor armorItem = (ItemDragonArmor)var3;
                return armorItem.type.ordinal() + 1;
            }
        }

        return 0;
    }

    public boolean isNoAi() {
        return this.isModelDead() || super.isNoAi();
    }

    public boolean isAiDisabled() {
        return super.isNoAi();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        // *** FIX: register the SWIMMING accessor that was allocated in the
        //          static block but never defined per-instance. ***
        this.entityData.define(SWIMMING, false);

        // All previously existing entries — unchanged:
        this.entityData.define(HUNGER,        0);
        this.entityData.define(AGE_TICKS,     0);
        this.entityData.define(GENDER,        false);
        this.entityData.define(VARIANT,       0);
        this.entityData.define(SLEEPING,      false);
        this.entityData.define(FIREBREATHING, false);
        this.entityData.define(HOVERING,      false);
        this.entityData.define(FLYING,        false);
        this.entityData.define(DEATH_STAGE,   0);
        this.entityData.define(MODEL_DEAD,    false);
        this.entityData.define(CONTROL_STATE, (byte) 0);
        this.entityData.define(TACKLE,        false);
        this.entityData.define(AGINGDISABLED, false);
        this.entityData.define(COMMAND,       0);
        this.entityData.define(DRAGON_PITCH,  0.0F);
        this.entityData.define(CRYSTAL_BOUND, false);
        this.entityData.define(CUSTOM_POSE,   "");
        this.entityData.define(FIRE_MODE,       (byte) 0);
        this.entityData.define(SYNCED_ANIM_ID,  (byte) 0);
        this.entityData.define(IS_EATING,       false);
    }

    public boolean isGoingUp() {
        return ((Byte)this.entityData.get(CONTROL_STATE) & 1) == 1;
    }

    public boolean isGoingDown() {
        return ((Byte)this.entityData.get(CONTROL_STATE) >> 1 & 1) == 1;
    }

    public boolean isAttacking() {
        return ((Byte)this.entityData.get(CONTROL_STATE) >> 2 & 1) == 1;
    }

    public boolean isStriking() {
        return ((Byte)this.entityData.get(CONTROL_STATE) >> 3 & 1) == 1;
    }

    public boolean isDismounting() {
        return ((Byte)this.entityData.get(CONTROL_STATE) >> 4 & 1) == 1;
    }

    public void up(boolean up) {
        this.setStateField(0, up);
    }

    public void down(boolean down) {
        this.setStateField(1, down);
    }

    public void attack(boolean attack) {
        this.setStateField(2, attack);
    }

    public void strike(boolean strike) {
        this.setStateField(3, strike);
    }

    public void dismount(boolean dismount) {
        this.setStateField(4, dismount);
    }

    private void setStateField(int i, boolean newState) {
        byte prevState = (Byte)this.entityData.get(CONTROL_STATE);
        if (newState) {
            this.entityData.set(CONTROL_STATE, (byte)(prevState | 1 << i));
        } else {
            this.entityData.set(CONTROL_STATE, (byte)(prevState & ~(1 << i)));
        }

    }

    public byte getControlState() {
        return (Byte)this.entityData.get(CONTROL_STATE);
    }

    public void setControlState(byte state) {
        this.entityData.set(CONTROL_STATE, state);
    }

    public int getCommand() {
        return (Integer)this.entityData.get(COMMAND);
    }

    public void setCommand(int command) {
        this.entityData.set(COMMAND, command);
        this.setOrderedToSit(command == 1);
    }

    public float getDragonPitch() {
        return (Float)this.entityData.get(DRAGON_PITCH);
    }

    public void setDragonPitch(float pitch) {
        this.entityData.set(DRAGON_PITCH, pitch);
    }

    public void incrementDragonPitch(float pitch) {
        this.entityData.set(DRAGON_PITCH, this.getDragonPitch() + pitch);
    }

    public void decrementDragonPitch(float pitch) {
        this.entityData.set(DRAGON_PITCH, this.getDragonPitch() - pitch);
    }

    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Hunger", this.getHunger());
        compound.putInt("AgeTicks", this.getAgeInTicks());
        compound.putBoolean("Gender", this.isMale());
        compound.putInt("Variant", this.getVariant());
        compound.putBoolean("Sleeping", this.isSleeping());
        compound.putBoolean("TamedDragon", this.isTame());
        compound.putBoolean("FireBreathing", this.isBreathingFire());
        compound.putBoolean("AttackDecision", this.usingGroundAttack);
        compound.putBoolean("Hovering", this.isHovering());
        compound.putBoolean("Flying", this.isFlying());
        compound.putInt("DeathStage", this.getDeathStage());
        compound.putBoolean("ModelDead", this.isModelDead());
        compound.putFloat("DeadProg", this.modelDeadProgress);
        compound.putBoolean("Tackle", this.isTackling());
        compound.putBoolean("HasHomePosition", this.hasHomePosition);
        compound.putString("CustomPose", this.getCustomPose());
        if (this.homePos != null && this.hasHomePosition) {
            this.homePos.write(compound);
        }

        compound.putBoolean("AgingDisabled", this.isAgingDisabled());
        compound.putInt("Command", this.getCommand());
        if (this.dragonInventory != null) {
            ListTag nbttaglist = new ListTag();

            for(int i = 0; i < this.dragonInventory.getContainerSize(); ++i) {
                ItemStack itemstack = this.dragonInventory.getItem(i);
                if (!itemstack.isEmpty()) {
                    CompoundTag CompoundNBT = new CompoundTag();
                    CompoundNBT.putByte("Slot", (byte)i);
                    itemstack.save(CompoundNBT);
                    nbttaglist.add(CompoundNBT);
                }
            }

            compound.put("Items", nbttaglist);
        }

        compound.putBoolean("CrystalBound", this.isBoundToCrystal());
        if (this.hasCustomName()) {
            compound.putString("CustomName", Serializer.toJson(this.getCustomName()));
        }

    }

    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setHunger(compound.getInt("Hunger"));
        this.setAgeInTicks(compound.getInt("AgeTicks"));
        this.setGender(compound.getBoolean("Gender"));
        this.setVariant(compound.getInt("Variant"));
        this.setInSittingPose(compound.getBoolean("Sleeping"));
        this.setTame(compound.getBoolean("TamedDragon"));
        this.setBreathingFire(compound.getBoolean("FireBreathing"));
        this.usingGroundAttack = compound.getBoolean("AttackDecision");
        this.setHovering(compound.getBoolean("Hovering"));
        this.setFlying(compound.getBoolean("Flying"));
        this.setDeathStage(compound.getInt("DeathStage"));
        this.setModelDead(compound.getBoolean("ModelDead"));
        this.modelDeadProgress = compound.getFloat("DeadProg");
        this.setCustomPose(compound.getString("CustomPose"));
        this.hasHomePosition = compound.getBoolean("HasHomePosition");
        if (this.hasHomePosition && compound.getInt("HomeAreaX") != 0 && compound.getInt("HomeAreaY") != 0 && compound.getInt("HomeAreaZ") != 0) {
            this.homePos = new HomePosition(compound, this.level());
        }

        this.setTackling(compound.getBoolean("Tackle"));
        this.setAgingDisabled(compound.getBoolean("AgingDisabled"));
        this.setCommand(compound.getInt("Command"));
        if (this.dragonInventory != null) {
            ListTag nbttaglist = compound.getList("Items", 10);
            this.createInventory();

            for(Tag inbt : nbttaglist) {
                CompoundTag CompoundNBT = (CompoundTag)inbt;
                int j = CompoundNBT.getByte("Slot") & 255;
                if (j <= 4) {
                    this.dragonInventory.setItem(j, ItemStack.of(CompoundNBT));
                }
            }
        } else {
            ListTag nbttaglist = compound.getList("Items", 10);
            this.createInventory();

            for(Tag inbt : nbttaglist) {
                CompoundTag CompoundNBT = (CompoundTag)inbt;
                int j = CompoundNBT.getByte("Slot") & 255;
                this.dragonInventory.setItem(j, ItemStack.of(CompoundNBT));
            }
        }

        this.setCrystalBound(compound.getBoolean("CrystalBound"));
        if (compound.contains("CustomName", 8) && !compound.getString("CustomName").startsWith("TextComponent")) {
            this.setCustomName(Serializer.fromJson(compound.getString("CustomName")));
        }

        this.setConfigurableAttributes();
        this.updateAttributes();
    }

    public int getContainerSize() {
        return 5;
    }

    protected void createInventory() {
        SimpleContainer tempInventory = this.dragonInventory;
        this.dragonInventory = new SimpleContainer(this.getContainerSize());
        if (tempInventory != null) {
            tempInventory.removeListener(this);
            int i = Math.min(tempInventory.getContainerSize(), this.dragonInventory.getContainerSize());

            for(int j = 0; j < i; ++j) {
                ItemStack itemstack = tempInventory.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.dragonInventory.setItem(j, itemstack.copy());
                }
            }
        }

        this.dragonInventory.addListener(this);
        this.updateContainerEquipment();
        this.itemHandler = LazyOptional.of(() -> new InvWrapper(this.dragonInventory));
    }

    protected void updateContainerEquipment() {
        if (!this.level().isClientSide) {
            this.updateAttributes();
        }

    }

    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
        return this.isAlive() && capability == ForgeCapabilities.ITEM_HANDLER && this.itemHandler != null ? this.itemHandler.cast() : super.getCapability(capability, facing);
    }

    public void invalidateCaps() {
        super.invalidateCaps();
        if (this.itemHandler != null) {
            LazyOptional<?> oldHandler = this.itemHandler;
            this.itemHandler = null;
            oldHandler.invalidate();
        }

    }

    public boolean hasInventoryChanged(Container pInventory) {
        return this.dragonInventory != pInventory;
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        for(Entity passenger : this.getPassengers()) {
            if (passenger instanceof Player player) {
                if (this.getTarget() != passenger && this.isTame() && this.getOwnerUUID() != null && this.getOwnerUUID().equals(player.getUUID())) {
                    return player;
                }
            }
        }

        return null;
    }

    public boolean isRidingPlayer(Player player) {
        return this.getRidingPlayer() != null && player != null && this.getRidingPlayer().getUUID().equals(player.getUUID());
    }

    @Nullable
    public Player getRidingPlayer() {
        LivingEntity var2 = this.getControllingPassenger();
        if (var2 instanceof Player player) {
            return player;
        } else {
            return null;
        }
    }

    protected void updateAttributes() {
        this.prevArmorResLoc = this.armorResLoc;
        int armorHead = this.getArmorOrdinal(this.getItemBySlot(EquipmentSlot.HEAD));
        int armorNeck = this.getArmorOrdinal(this.getItemBySlot(EquipmentSlot.CHEST));
        int armorLegs = this.getArmorOrdinal(this.getItemBySlot(EquipmentSlot.LEGS));
        int armorFeet = this.getArmorOrdinal(this.getItemBySlot(EquipmentSlot.FEET));
        //this.armorResLoc = this.dragonType.getName() + "|" + armorHead + "|" + armorNeck + "|" + armorLegs + "|" + armorFeet;
        //IceAndFire.PROXY.updateDragonArmorRender(this.armorResLoc);
        double age = (double)125.0F;
        if (this.getAgeInDays() <= 125) {
            age = (double)this.getAgeInDays();
        }

        double healthStep = (this.maximumHealth - this.minimumHealth) / (double)125.0F;
        double attackStep = (this.maximumDamage - this.minimumDamage) / (double)125.0F;
        double speedStep = (this.maximumSpeed - this.minimumSpeed) / (double)125.0F;
        double armorStep = (this.maximumArmor - this.minimumArmor) / (double)125.0F;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)Math.round(this.minimumHealth + healthStep * age));
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((double)Math.round(this.minimumDamage + attackStep * age));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(this.minimumSpeed + speedStep * age);
        double baseValue = this.minimumArmor + armorStep * (double)this.getAgeInDays();
        this.getAttribute(Attributes.ARMOR).setBaseValue(baseValue);
        if (!this.level().isClientSide) {
            this.getAttribute(Attributes.ARMOR).removeModifier(ARMOR_MODIFIER_UUID);
            this.getAttribute(Attributes.ARMOR).addPermanentModifier(new AttributeModifier(ARMOR_MODIFIER_UUID, "Dragon armor bonus", this.calculateArmorModifier(), Operation.ADDITION));
        }

        this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue((double)Math.min(2048, IafConfig.dragonTargetSearchLength));
    }

    public int getHunger() {
        return (Integer)this.entityData.get(HUNGER);
    }

    public void setHunger(int hunger) {
        this.entityData.set(HUNGER, Mth.clamp(hunger, 0, 100));
    }

    public int getVariant() {
        return (Integer)this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    public int getAgeInDays() {
        return (Integer)this.entityData.get(AGE_TICKS) / 24000;
    }

    public void setAgeInDays(int age) {
        this.entityData.set(AGE_TICKS, age * 24000);
    }

    public int getAgeInTicks() {
        return (Integer)this.entityData.get(AGE_TICKS);
    }

    public void setAgeInTicks(int age) {
        this.entityData.set(AGE_TICKS, age);
    }

    public int getDeathStage() {
        return (Integer)this.entityData.get(DEATH_STAGE);
    }

    public void setDeathStage(int stage) {
        this.entityData.set(DEATH_STAGE, stage);
    }

    public boolean isMale() {
        return (Boolean)this.entityData.get(GENDER);
    }

    public boolean isModelDead() {
        return this.level().isClientSide ? (this.isModelDead = (Boolean)this.entityData.get(MODEL_DEAD)) : this.isModelDead;
    }

    public void setModelDead(boolean modeldead) {
        this.entityData.set(MODEL_DEAD, modeldead);
        if (!this.level().isClientSide) {
            this.isModelDead = modeldead;
        }

    }

    public boolean isHovering() {
        return (Boolean)this.entityData.get(HOVERING);
    }

    public void setHovering(boolean hovering) {
        this.entityData.set(HOVERING, hovering);
    }

    public boolean isFlying() {
        return (Boolean)this.entityData.get(FLYING);
    }

    public void setFlying(boolean flying) {
        this.entityData.set(FLYING, flying);
    }

    public boolean useFlyingPathFinder() {
        return this.isFlying() && this.getControllingPassenger() == null;
    }

    public void setGender(boolean male) {
        this.entityData.set(GENDER, male);
    }

    public boolean isSleeping() {
        return (Boolean)this.entityData.get(SLEEPING);
    }

    public boolean isBlinking() {
        return this.tickCount % 50 > 43;
    }

    public boolean isBreathingFire() {
        return (Boolean)this.entityData.get(FIREBREATHING);
    }

    public void setBreathingFire(boolean breathing) {
        this.entityData.set(FIREBREATHING, breathing);
    }

    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return this.getPassengers().size() < 2;
    }

    public boolean isOrderedToSit() {
        return ((Byte)this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    public void setInSittingPose(boolean sleeping) {
        this.entityData.set(SLEEPING, sleeping);
        if (sleeping) {
            this.getNavigation().stop();
        }

    }

    public void setOrderedToSit(boolean sitting) {
        byte b0 = (Byte)this.entityData.get(DATA_FLAGS_ID);
        if (sitting) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(b0 | 1));
            this.getNavigation().stop();
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(b0 & -2));
        }

    }

    public String getCustomPose() {
        return (String)this.entityData.get(CUSTOM_POSE);
    }

    public void setCustomPose(String customPose) {
        this.entityData.set(CUSTOM_POSE, customPose);
        this.modelDeadProgress = 20.0F;
    }

    public void riderShootFire(Entity controller) {
    }

    private double calculateArmorModifier() {
        double val = (double)1.0F;
        EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        for(EquipmentSlot slot : slots) {
            switch (this.getArmorOrdinal(this.getItemBySlot(slot))) {
                case 1:
                    val += (double)2.0F;
                    break;
                case 2:
                case 4:
                    val += (double)3.0F;
                    break;
                case 3:
                    val += (double)5.0F;
                    break;
                case 5:
                case 6:
                case 8:
                    val += (double)10.0F;
                    break;
                case 7:
                    ++val;
            }
        }

        return val;
    }

    public boolean canMove() {
        return !this.isOrderedToSit() && !this.isSleeping() && this.getControllingPassenger() == null && !this.isPassenger() && !this.isModelDead() && this.sleepProgress == 0.0F && getCurrentAnimation() != ANIMATION_SHAKEPREY;
    }

    public boolean isFuelingForge() {
        return this.burningTarget != null && this.level().getBlockEntity(this.burningTarget) instanceof TileEntityDragonforgeInput;
    }

    public boolean isAlive() {
        if (this.isModelDead()) {
            return !this.isRemoved();
        } else {
            return super.isAlive();
        }
    }

    public @NotNull InteractionResult interactAt(Player player, @NotNull Vec3 vec, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int lastDeathStage = Math.min(this.getAgeInDays() / 5, 25);
        if (stack.getItem() == IafItemRegistry.DRAGON_DEBUG_STICK.get()) {
            this.logic.debug();
            return InteractionResult.SUCCESS;
        } else if (this.isModelDead() && this.getDeathStage() < lastDeathStage && player.mayBuild()) {
            if (!this.level().isClientSide && !stack.isEmpty() && stack.getItem() != null && stack.getItem() == Items.GLASS_BOTTLE && this.getDeathStage() < lastDeathStage / 2 && IafConfig.dragonDropBlood) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }

                this.setDeathStage(this.getDeathStage() + 1);
                player.getInventory().add(new ItemStack(this.getBloodItem(), 1));
                return InteractionResult.SUCCESS;
            } else {
                if (!this.level().isClientSide && stack.isEmpty() && IafConfig.dragonDropSkull) {
                    if (this.getDeathStage() >= lastDeathStage - 1) {
                        ItemStack skull = this.getSkull().copy();
                        skull.setTag(new CompoundTag());
                        skull.getTag().putInt("Stage", this.getDragonStage());
                        skull.getTag().putInt("DragonType", 0);
                        skull.getTag().putInt("DragonAge", this.getAgeInDays());
                        this.setDeathStage(this.getDeathStage() + 1);
                        if (!this.level().isClientSide) {
                            this.spawnAtLocation(skull, 1.0F);
                        }

                        this.remove(RemovalReason.DISCARDED);
                    } else if (this.getDeathStage() == lastDeathStage / 2 - 1 && IafConfig.dragonDropHeart) {
                        ItemStack heart = new ItemStack(this.getHeartItem(), 1);
                        ItemStack egg = new ItemStack(this.getVariantEgg(this.random.nextInt(4)), 1);
                        if (!this.level().isClientSide) {
                            this.spawnAtLocation(heart, 1.0F);
                            if (!this.isMale() && this.getDragonStage() > 3) {
                                this.spawnAtLocation(egg, 1.0F);
                            }
                        }

                        this.setDeathStage(this.getDeathStage() + 1);
                    } else {
                        this.setDeathStage(this.getDeathStage() + 1);
                        ItemStack drop = this.getRandomDrop();
                        if (!drop.isEmpty() && !this.level().isClientSide) {
                            this.spawnAtLocation(drop, 1.0F);
                        }
                    }
                }

                return InteractionResult.SUCCESS;
            }
        } else {
            return super.interactAt(player, vec, hand);
        }
    }

    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (stack == ItemStack.EMPTY) {
            stack = player.getItemInHand(hand);
        }

        if (stack.getItem() == IafItemRegistry.DRAGON_DEBUG_STICK.get()) {
            this.logic.debug();
            return InteractionResult.SUCCESS;
        } else {
            if (!this.isModelDead()) {
                if (stack.getItem() == IafItemRegistry.CREATIVE_DRAGON_MEAL.get()) {
                    this.setTame(true);
                    this.tame(player);
                    this.setHunger(this.getHunger() + 20);
                    this.heal(Math.min(this.getHealth(), (float)((int)(this.getMaxHealth() / 2.0F))));
                    this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
                    this.spawnItemCrackParticles(stack.getItem());
                    this.spawnItemCrackParticles(Items.BONE);
                    this.spawnItemCrackParticles(Items.BONE_MEAL);
                    this.eatFoodBonus(stack);
                    if (this.eatAnimTicks <= 0) {
                        this.eatAnimTicks = EAT_ANIM_DURATION;
                        this.ANIMATION_EAT.start(this.tickCount);
                        this.setEating(true); // sync to client
                    }
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }

                    return InteractionResult.SUCCESS;
                }

                if (this.isFood(stack) && this.shouldDropLoot()) {
                    this.setAge(0);
                    this.usePlayerItem(player, InteractionHand.MAIN_HAND, stack);
                    this.setInLove(player);
                    return InteractionResult.SUCCESS;
                }

                if (this.isOwnedBy(player)) {
                    if (stack.getItem() == this.getSummoningCrystal() && !ItemSummoningCrystal.hasDragon(stack)) {
                        this.setCrystalBound(true);
                        CompoundTag compound = stack.getOrCreateTag();
                        CompoundTag dragonTag = new CompoundTag();
                        dragonTag.putUUID("DragonUUID", this.getUUID());
                        if (this.getCustomName() != null) {
                            dragonTag.putString("CustomName", this.getCustomName().getString());
                        }

                        compound.put("Dragon", dragonTag);
                        this.playSound(SoundEvents.BOTTLE_FILL_DRAGONBREATH, 1.0F, 1.0F);
                        player.swing(hand);
                        return InteractionResult.SUCCESS;
                    }

                    this.tame(player);
                    if (stack.getItem() == IafItemRegistry.DRAGON_HORN.get()) {
                        return super.mobInteract(player, hand);
                    }

                    if (stack.isEmpty() && !player.isShiftKeyDown()) {
                        if (!this.level().isClientSide) {
                            int dragonStage = this.getDragonStage();
                            if (dragonStage < 2) {
                                if (player.getPassengers().size() >= 3) {
                                    return InteractionResult.FAIL;
                                }

                                this.startRiding(player, true);
                                IceAndFire.sendMSGToAll(new MessageStartRidingMob(this.getId(), true, true));
                            } else if (dragonStage > 2 && !player.isPassenger()) {
                                player.setShiftKeyDown(false);
                                player.startRiding(this, true);
                                IceAndFire.sendMSGToAll(new MessageStartRidingMob(this.getId(), true, false));
                                this.setInSittingPose(false);
                            }

                            this.getNavigation().stop();
                        }

                        return InteractionResult.SUCCESS;
                    }

                    if (stack.isEmpty() && player.isShiftKeyDown()) {
                        this.openInventory(player);
                        return InteractionResult.SUCCESS;
                    }

                    int itemFoodAmount = FoodUtils.getFoodPoints(stack, true, this.dragonType.isPiscivore());
                    if (itemFoodAmount > 0 && (this.getHunger() < 100 || this.getHealth() < this.getMaxHealth())) {
                        this.setHunger(this.getHunger() + itemFoodAmount);
                        this.setHealth(Math.min(this.getMaxHealth(), (float)((int)(this.getHealth() + (float)(itemFoodAmount / 10)))));
                        this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
                        this.spawnItemCrackParticles(stack.getItem());
                        this.eatFoodBonus(stack);
                        // ── Eat animation: only start if not already playing ──────────
                        if (this.eatAnimTicks <= 0) {
                            this.eatAnimTicks = EAT_ANIM_DURATION;
                            this.ANIMATION_EAT.start(this.tickCount);
                            this.setEating(true); // sync to client so eat_controller fires
                        }
                        // ─────────────────────────────────────────────────────────────
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }

                        return InteractionResult.SUCCESS;
                    }

                    Item stackItem = stack.getItem();
                    if (stackItem == IafItemRegistry.DRAGON_MEAL.get()) {
                        this.growDragon(1);
                        this.setHunger(this.getHunger() + 20);
                        this.heal(Math.min(this.getHealth(), (float)((int)(this.getMaxHealth() / 2.0F))));
                        this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
                        this.spawnItemCrackParticles(stackItem);
                        this.spawnItemCrackParticles(Items.BONE);
                        this.spawnItemCrackParticles(Items.BONE_MEAL);
                        this.eatFoodBonus(stack);
                        if (this.eatAnimTicks <= 0) {
                            this.eatAnimTicks = EAT_ANIM_DURATION;
                            this.ANIMATION_EAT.start(this.tickCount);
                            this.setEating(true); // sync to client so eat_controller fires
                        }
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }

                        return InteractionResult.SUCCESS;
                    }

                    if (stackItem == IafItemRegistry.SICKLY_DRAGON_MEAL.get() && !this.isAgingDisabled()) {
                        this.setHunger(this.getHunger() + 20);
                        this.heal(this.getMaxHealth());
                        this.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, this.getSoundVolume(), this.getVoicePitch());
                        this.spawnItemCrackParticles(stackItem);
                        this.spawnItemCrackParticles(Items.BONE);
                        this.spawnItemCrackParticles(Items.BONE_MEAL);
                        this.spawnItemCrackParticles(Items.POISONOUS_POTATO);
                        this.spawnItemCrackParticles(Items.POISONOUS_POTATO);
                        this.setAgingDisabled(true);
                        this.eatFoodBonus(stack);
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }

                        return InteractionResult.SUCCESS;
                    }

                    if (stackItem == IafItemRegistry.DRAGON_STAFF.get()) {
                        if (player.isShiftKeyDown()) {
                            if (this.hasHomePosition) {
                                this.hasHomePosition = false;
                                player.displayClientMessage(Component.translatable("dragon.command.remove_home"), true);
                                return InteractionResult.SUCCESS;
                            }

                            BlockPos pos = this.blockPosition();
                            this.homePos = new HomePosition(pos, this.level());
                            this.hasHomePosition = true;
                            player.displayClientMessage(Component.translatable("dragon.command.new_home", new Object[]{pos.getX(), pos.getY(), pos.getZ(), this.homePos.getDimension()}), true);
                            return InteractionResult.SUCCESS;
                        }

                        this.playSound(SoundEvents.ZOMBIE_INFECT, this.getSoundVolume(), this.getVoicePitch());
                        if (!this.level().isClientSide) {
                            this.setCommand(this.getCommand() + 1);
                            if (this.getCommand() > 2) {
                                this.setCommand(0);
                            }
                        }

                        String commandText = "stand";
                        if (this.getCommand() == 1) {
                            commandText = "sit";
                        } else if (this.getCommand() == 2) {
                            commandText = "escort";
                        }

                        player.displayClientMessage(Component.translatable("dragon.command." + commandText), true);
                        return InteractionResult.SUCCESS;
                    }
                }
            }

            return super.mobInteract(player, hand);
        }
    }

    public abstract ItemLike getHeartItem();

    public abstract Item getBloodItem();

    public abstract Item getFleshItem();

    public ItemStack getSkull() {
        return ItemStack.EMPTY;
    }

    private ItemStack getRandomDrop() {
        ItemStack stack = this.getItemFromLootTable();
        if (stack.getItem() == IafItemRegistry.DRAGON_BONE.get()) {
            this.playSound(SoundEvents.SKELETON_AMBIENT, 1.0F, 1.0F);
        } else {
            this.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.0F);
        }

        return stack;
    }

    public boolean canPositionBeSeen(double x, double y, double z) {
        HitResult result = this.level().clip(new ClipContext(new Vec3(this.getX(), this.getY() + (double)this.getEyeHeight(), this.getZ()), new Vec3(x, y, z), Block.COLLIDER, Fluid.NONE, this));
        double dist = result.getLocation().distanceToSqr(x, y, z);
        return dist <= (double)1.0F || result.getType() == Type.MISS;
    }

    public abstract ResourceLocation getDeadLootTable();

    public ItemStack getItemFromLootTable() {
        LootTable loottable = this.level().getServer().getServerResources().managers().getLootData().getLootTable(this.getDeadLootTable());
        LootParams.Builder lootparams$builder = (new LootParams.Builder((ServerLevel)this.level())).withParameter(LootContextParams.THIS_ENTITY, this).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.DAMAGE_SOURCE, this.level().damageSources().generic());
        ObjectListIterator var3 = loottable.getRandomItems(lootparams$builder.create(LootContextParamSets.ENTITY)).iterator();
        if (var3.hasNext()) {
            ItemStack itemstack = (ItemStack)var3.next();
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public void eatFoodBonus(ItemStack stack) {
    }

    public boolean requiresCustomPersistence() {
        return true;
    }

    public boolean isPersistenceRequired() {
        return true;
    }

    public void growDragon(int ageInDays) {
        if (!this.isAgingDisabled()) {
            this.setAgeInDays(this.getAgeInDays() + ageInDays);
            this.setBoundingBox(this.getBoundingBox());
            if (this.level().isClientSide && this.getAgeInDays() % 25 == 0) {
                for(int i = 0; (float)i < this.getRenderSize() * 4.0F; ++i) {
                    float f = (float)((double)this.getRandom().nextFloat() * (this.getBoundingBox().maxX - this.getBoundingBox().minX) + this.getBoundingBox().minX);
                    float f1 = (float)((double)this.getRandom().nextFloat() * (this.getBoundingBox().maxY - this.getBoundingBox().minY) + this.getBoundingBox().minY);
                    float f2 = (float)((double)this.getRandom().nextFloat() * (this.getBoundingBox().maxZ - this.getBoundingBox().minZ) + this.getBoundingBox().minZ);
                    double motionX = this.getRandom().nextGaussian() * 0.07;
                    double motionY = this.getRandom().nextGaussian() * 0.07;
                    double motionZ = this.getRandom().nextGaussian() * 0.07;
                    this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, (double)f, (double)f1, (double)f2, motionX, motionY, motionZ);
                }
            }

            if (this.getDragonStage() >= 2) {
                this.removeVehicle();
            }

            this.updateAttributes();
        }
    }

    public void spawnItemCrackParticles(Item item) {
        for(int i = 0; i < 15; ++i) {
            double motionX = this.getRandom().nextGaussian() * 0.07;
            double motionY = this.getRandom().nextGaussian() * 0.07;
            double motionZ = this.getRandom().nextGaussian() * 0.07;
            Vec3 headVec = this.getHeadPosition();
            if (!this.level().isClientSide) {
                ((ServerLevel)this.level()).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(item)), headVec.x, headVec.y, headVec.z, 1, motionX, motionY, motionZ, 0.1);
            } else {
                this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(item)), headVec.x, headVec.y, headVec.z, motionX, motionY, motionZ);
            }
        }

    }

    public boolean isTimeToWake() {
        return this.level().isDay() || this.getCommand() == 2;
    }

    private boolean isStuck() {
        boolean skip = this.isChained() || this.isTame();
        if (skip) {
            return false;
        } else {
            boolean checkNavigation = this.ticksStill > 80 && this.canMove() && !this.isHovering();
            if (checkNavigation) {
                PathNavigation navigation = this.getNavigation();
                Path path = navigation.getPath();
                if (!navigation.isDone() && (path == null || path.getEndNode() != null || this.blockPosition().distSqr(path.getEndNode().asBlockPos()) > (double)15.0F)) {
                    return true;
                }
            }

            return false;
        }
    }

    protected boolean isOverAir() {
        return this.isOverAir;
    }

    private boolean isOverAirLogic() {
        return this.level().isEmptyBlock(BlockPos.containing((double)this.getBlockX(), this.getBoundingBox().minY - (double)1.0F, (double)this.getBlockZ()));
    }

    public boolean isDiving() {
        return false;
    }

    public boolean isBeyondHeight() {
        if (this.getY() > (double)this.level().getMaxBuildHeight()) {
            return true;
        } else {
            return this.getY() > (double)IafConfig.maxDragonFlight;
        }
    }

    private int calculateDownY() {
        if (this.getNavigation().getPath() != null) {
            Path path = this.getNavigation().getPath();
            Vec3 p = path.getEntityPosAtNode(this, Math.min(path.getNodeCount() - 1, path.getNextNodeIndex() + 1));
            if (p.y < this.getY() - (double)1.0F) {
                return -1;
            }
        }

        return 1;
    }

    public void breakBlock(BlockPos position) {
        if (!MinecraftForge.EVENT_BUS.post(new GenericGriefEvent(this, (double)position.getX(), (double)position.getY(), (double)position.getZ()))) {
            BlockState state = this.level().getBlockState(position);
            float hardness = IafConfig.dragonGriefing != 1 && this.getDragonStage() > 3 ? 5.0F : 2.0F;
            if (this.isBreakable(position, state, hardness, this)) {
                this.setDeltaMovement(this.getDeltaMovement().multiply((double)0.6F, (double)1.0F, (double)0.6F));
                if (!this.level().isClientSide()) {
                    this.level().destroyBlock(position, !state.is(IafBlockTags.DRAGON_BLOCK_BREAK_NO_DROPS) && (double)this.random.nextFloat() <= IafConfig.dragonBlockBreakingDropChance);
                }
            }

        }
    }

    public void breakBlocks(boolean force) {
        boolean doBreak = force;
        if (this.blockBreakCounter > 0 || IafConfig.dragonBreakBlockCooldown == 0) {
            --this.blockBreakCounter;
            if (this.blockBreakCounter == 0 || IafConfig.dragonBreakBlockCooldown == 0) {
                doBreak = true;
            }
        }

        if (doBreak && ForgeEventFactory.getMobGriefingEvent(this.level(), this) && DragonUtils.canGrief(this) && !this.isModelDead() && this.getDragonStage() >= 3 && (this.canMove() || this.getControllingPassenger() != null)) {
            int bounds = 1;
            int flightModifier = this.isFlying() && this.getTarget() != null ? -1 : 1;
            int yMinus = this.calculateDownY();
            BlockPos.betweenClosedStream((int)Math.floor(this.getBoundingBox().minX) - 1, (int)Math.floor(this.getBoundingBox().minY) + yMinus, (int)Math.floor(this.getBoundingBox().minZ) - 1, (int)Math.floor(this.getBoundingBox().maxX) + 1, (int)Math.floor(this.getBoundingBox().maxY) + 1 + flightModifier, (int)Math.floor(this.getBoundingBox().maxZ) + 1).forEach(this::breakBlock);
        }

    }

    protected boolean isBreakable(BlockPos pos, BlockState state, float hardness, com.github.alexthe666.iceandfire.entity.EntityDragonBase entity) {
        return state.blocksMotion() && !state.isAir() && state.getFluidState().isEmpty() && !state.getShape(this.level(), pos).isEmpty() && state.getDestroySpeed(this.level(), pos) >= 0.0F && state.getDestroySpeed(this.level(), pos) <= hardness && DragonUtils.canDragonBreak(state, entity) && this.canDestroyBlock(pos, state);
    }

    public boolean isBlockExplicitlyPassable(BlockState state, BlockPos pos, BlockPos entityPos) {
        return !this.isModelDead() && this.getDragonStage() >= 3 && DragonUtils.canGrief(this) && (double)pos.getY() >= this.getY() ? this.isBreakable(pos, state, IafConfig.dragonGriefing != 1 && this.getDragonStage() > 3 ? 5.0F : 2.0F, this) : false;
    }

    public boolean isBlockExplicitlyNotPassable(BlockState state, BlockPos pos, BlockPos entityPos) {
        return false;
    }

    public void spawnGroundEffects() {
        if (this.level().isClientSide) {
            for(int i = 0; (float)i < this.getRenderSize(); ++i) {
                for(int i1 = 0; i1 < 20; ++i1) {
                    float radius = 0.75F * (0.7F * this.getRenderSize() / 3.0F) * -3.0F;
                    float angle = ((float)Math.PI / 180F) * this.yBodyRot + (float)i1 * 1.0F;
                    double extraX = (double)(radius * Mth.sin((float)(Math.PI + (double)angle)));
                    double extraY = (double)0.8F;
                    double extraZ = (double)(radius * Mth.cos(angle));
                    BlockPos ground = this.getGround(BlockPos.containing(this.getX() + extraX, this.getY() + (double)0.8F - (double)1.0F, this.getZ() + extraZ));
                    BlockState BlockState = this.level().getBlockState(ground);
                    if (BlockState.isAir()) {
                        double motionX = this.getRandom().nextGaussian() * 0.07;
                        double motionY = this.getRandom().nextGaussian() * 0.07;
                        double motionZ = this.getRandom().nextGaussian() * 0.07;
                        this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, BlockState), true, this.getX() + extraX, (double)ground.getY() + (double)0.8F, this.getZ() + extraZ, motionX, motionY, motionZ);
                    }
                }
            }
        }

    }

    private BlockPos getGround(BlockPos blockPos) {
        while(this.level().isEmptyBlock(blockPos) && blockPos.getY() > 1) {
            blockPos = blockPos.below();
        }

        return blockPos;
    }

    public boolean doesWantToLand() {
        return this.flyTicks > 6000 || this.isGoingDown() || this.flyTicks > 40 && this.flyProgress == 0.0F || this.isChained() && this.flyTicks > 100;
    }

    public abstract String getVariantName(int var1);

    public boolean shouldRiderSit() {
        return this.getControllingPassenger() != null;
    }

    public void positionRider(@NotNull Entity passenger, @NotNull Entity.@NotNull MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (this.hasPassenger(passenger)) {
            if (this.getControllingPassenger() != null && this.getControllingPassenger().getUUID().equals(passenger.getUUID())) {
                if (this.isModelDead()) {
                    passenger.stopRiding();
                }

                this.setYRot(passenger.getYRot());
                this.setYHeadRot(passenger.getYHeadRot());
                Vec3 riderPos = this.getRiderPosition();

                // Safety floor: ensure the rider can never sink below the
                // dragon's own feet level plus a minimum body clearance.
                // getRiderPosition() accounts for pitch via 2-D rotation, but
                // at extreme dive angles (nose-down > ~50°) worldUp can go
                // negative and the player clips through the terrain.
                double safeY = Math.max(riderPos.y,
                        this.getY() + (this.getDragonStage() <= 2 ? 0.5 : 1.5));
                passenger.setPos(riderPos.x, safeY, riderPos.z);

            } else {
                this.updatePreyInMouth(passenger);
            }
        }

    }

    private float bob(float speed, float degree, boolean bounce, float f, float f1) {
        double a = (double)(Mth.sin(f * speed) * f1 * degree);
        float bob = (float)(a - (double)(f1 * degree));
        if (bounce) {
            bob = (float)(-Math.abs(a));
        }

        return bob * this.getRenderSize() / 3.0F;
    }

    protected void updatePreyInMouth(Entity prey) {
        if (getCurrentAnimation() != ANIMATION_SHAKEPREY) {
            startAnimation(ANIMATION_SHAKEPREY);
        }

        int animTick = getAnimationTick(ANIMATION_SHAKEPREY);
        // swing_prey animation = 2.474 s = 49 ticks.
        // Jaw opens widest at keyframe ~2.127 s (tick ≈43) — the visual "throw" moment.
        // Releasing here ensures a clean single-play: prey is flung before
        // the PLAY_ONCE animation restarts at tick 49.
        if (getCurrentAnimation() == ANIMATION_SHAKEPREY && animTick > 43 && prey != null) {
            float baseDamage = (float)this.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
            float damage = baseDamage * 2.0F;
            boolean didDamage = prey.hurt(this.level().damageSources().mobAttack(this), damage);
            if (didDamage && IafConfig.canDragonsHealFromBiting) {
                this.heal(damage * 0.5F);
            }

            if (!(prey instanceof Player)) {
                this.setHunger(this.getHunger() + 1);
            }

            prey.stopRiding();
        } else {
            this.yBodyRot = this.getYRot();
            float modTick_0 = (float)(animTick - 25);
            float modTick_1 = animTick > 25 && animTick < 44 ? 8.0F * Mth.clamp(Mth.sin((float)(Math.PI + (double)modTick_0 * (double)0.25F)), -0.8F, 0.8F) : 0.0F;
            float modTick_2 = animTick > 30 ? 10.0F : (float)Math.max(0, animTick - 20);
            float radius = 0.75F * (0.6F * this.getRenderSize() / 3.0F) * -3.0F;
            float angle = ((float)Math.PI / 180F) * this.yBodyRot + 3.15F + modTick_1 * 2.0F * 0.015F;
            double extraX = (double)(radius * Mth.sin((float)(Math.PI + (double)angle)));
            double extraZ = (double)(radius * Mth.cos(angle));
            double extraY = modTick_2 == 0.0F ? (double)0.0F : (double)0.035F * ((double)(this.getRenderSize() / 3.0F) + (double)modTick_2 * (double)0.5F * (double)(this.getRenderSize() / 3.0F));
            prey.setPos(this.getX() + extraX, this.getY() + extraY, this.getZ() + extraZ);
        }

    }

    public int getDragonStage() {
        int age = this.getAgeInDays();
        if (age >= 100) {
            return 5;
        } else if (age >= 75) {
            return 4;
        } else if (age >= 50) {
            return 3;
        } else {
            return age >= 25 ? 2 : 1;
        }
    }

    public boolean isTeen() {
        return this.getDragonStage() < 4 && this.getDragonStage() > 2;
    }

    public boolean shouldDropLoot() {
        return this.getDragonStage() >= 4;
    }

    public boolean isBaby() {
        return this.getDragonStage() < 2;
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor worldIn, @NotNull DifficultyInstance difficultyIn, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        spawnDataIn = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        this.setGender(this.getRandom().nextBoolean());
        int age = this.getRandom().nextInt(80) + 1;
        this.growDragon(age);
        this.setVariant((new Random()).nextInt(4));
        this.setInSittingPose(false);
        double healthStep = (this.maximumHealth - this.minimumHealth) / (double)125.0F;
        this.heal((float)Math.round(this.minimumHealth + healthStep * (double)age));
        this.usingGroundAttack = true;
        this.setHunger(50);
        // Randomise initial attack profile so the fire/melee AI triggers correctly
        // on the very first tick. Without this groundAttack/airAttack are null and
        // all fire-breath gate checks silently fail.
        this.randomizeAttacks();
        return spawnDataIn;
    }

    /** This dragon is immune to all fire and lava damage. */
    @Override
    public boolean fireImmune() {
        return true;
    }

    public boolean hurt(@NotNull DamageSource dmg, float i) {
        if (this.isModelDead() && dmg != this.level().damageSources().fellOutOfWorld()) {
            return false;
        } else if (this.isVehicle() && dmg.getEntity() != null && this.getControllingPassenger() != null && dmg.getEntity() == this.getControllingPassenger()) {
            return false;
        } else if ((dmg.type().msgId().contains("arrow") || this.getVehicle() != null && dmg.getEntity() != null && dmg.getEntity().is(this.getVehicle())) && this.isPassenger()) {
            return false;
        } else if (!dmg.is(DamageTypes.IN_WALL) && !dmg.is(DamageTypes.FALLING_BLOCK) && !dmg.is(DamageTypes.CRAMMING)) {
            if (!this.level().isClientSide && dmg.getEntity() != null && this.getRandom().nextInt(4) == 0) {
                this.roar();
            }

            if (i > 0.0F && this.isSleeping()) {
                this.setInSittingPose(false);
                if (!this.isTame() && dmg.getEntity() instanceof Player) {
                    this.setTarget((Player)dmg.getEntity());
                }
            }

            return super.hurt(dmg, i);
        } else {
            return false;
        }
    }

    @Override
    public void refreshDimensions() {
        super.refreshDimensions();
        float scale = getScale();
        int   stage = getDragonStage();
        if (scale != this.lastScale || stage != this.lastStage) {
            // Pass visual scale so part offsets match the rendered model.
            this.resetParts(this.getVisualScale());
        }
        this.lastScale = scale;
        this.lastStage = stage;   // requires the new field added in change A
    }

    public float getStepHeight() {
        return Math.max(1.2F, 1.2F + (float)(Math.min(this.getAgeInDays(), 125) - 25) * 1.8F / 100.0F);
    }

    public void tick() {
        super.tick();
        this.refreshDimensions();
        this.updateParts();
        this.prevDragonPitch = this.getDragonPitch();
        this.level().getProfiler().push("dragonLogic");
        this.setMaxUpStep(this.getStepHeight());
        this.isOverAir = this.isOverAirLogic();
        this.logic.updateDragonCommon();
        if (this.isModelDead()) {
            if (!this.level().isClientSide && this.level().isEmptyBlock(BlockPos.containing((double)this.getBlockX(), this.getBoundingBox().minY, (double)this.getBlockZ())) && this.getY() > (double)-1.0F) {
                this.move(MoverType.SELF, new Vec3((double)0.0F, (double)-0.2F, (double)0.0F));
            }

            this.setBreathingFire(false);
            float dragonPitch = this.getDragonPitch();
            if (dragonPitch > 0.0F) {
                dragonPitch = Math.min(0.0F, dragonPitch - 5.0F);
                this.setDragonPitch(dragonPitch);
            }

            if (dragonPitch < 0.0F) {
                this.setDragonPitch(Math.max(0.0F, dragonPitch + 5.0F));
            }
        } else if (this.level().isClientSide) {
            this.logic.updateDragonClient();
        } else {
            this.logic.updateDragonServer();
            this.logic.updateDragonAttack();
        }

        this.level().getProfiler().pop();
        this.level().getProfiler().push("dragonFlight");
        if (this.useFlyingPathFinder() && !this.level().isClientSide) {
            this.flightManager.update();
        }

        this.level().getProfiler().pop();
        this.level().getProfiler().pop();
        if (!this.level().isClientSide() && IafConfig.dragonDigWhenStuck && this.isStuck()) {
            this.breakBlocks(true);
            this.resetStuck();
        }

    }

    private void resetStuck() {
        this.ticksStill = 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.prevModelDeadProgress = this.modelDeadProgress;
        this.prevDiveProgress = this.diveProgress;
        this.prevAnimationProgresses[0] = this.sitProgress;
        this.prevAnimationProgresses[1] = this.sleepProgress;
        this.prevAnimationProgresses[2] = this.hoverProgress;
        this.prevAnimationProgresses[3] = this.flyProgress;
        this.prevAnimationProgresses[4] = this.fireBreathProgress;
        this.prevAnimationProgresses[5] = this.ridingProgress;
        this.prevAnimationProgresses[6] = this.tackleProgress;

        // ── Eat / tail-swing / speak animation timers ────────────────────────
        if (this.eatAnimTicks > 0) {
            this.eatAnimTicks--;
            if (this.eatAnimTicks == 0) {
                this.ANIMATION_EAT.stop();
                this.setEating(false); // clear synced flag so client animation stops
            }
        }
        if (this.tailSwingCooldown > 0) {
            this.tailSwingCooldown--;
        }
        // tailAnimTicks counts down the tail_swing animation duration (47 ticks).
        // When it hits 0 we clear currentActiveAnimation so GeckoLib's PLAY_ONCE
        // controller stops restarting the already-finished animation each frame.
        if (this.tailAnimTicks > 0) {
            this.tailAnimTicks--;
            if (this.tailAnimTicks == 0 && this.getCurrentAnimation() == ANIMATION_TAILWHACK) {
                this.stopCurrentAnimation();
                this.ANIMATION_TAILWHACK.stop();
            }
        }
        // ANIMATION_BITE is 0.6 s = 12 ticks.  Without an explicit stop the
        // SYNCED_ANIM_ID stays ANIM_ID_BITE, the controller's setAndContinue call
        // restarts the animation every frame, and isPlayingAttackAnimation() blocks
        // all subsequent attacks.  Clear it at tick 14 (12 + 2 buffer).
        if (getCurrentAnimation() == ANIMATION_BITE
                && getAnimationTick(ANIMATION_BITE) >= 14) {
            this.stopCurrentAnimation();
            this.ANIMATION_BITE.stop();
        }
        // speakAnimTicks counts down the talk animation lifetime (see SPEAK_ANIM_DURATION).
        // The per-tick SYNCED_ANIM_ID sync block below reads this to hold the SPEAK ID.
        if (!this.level().isClientSide) {
            if (this.speakAnimTicks > 0) {
                this.speakAnimTicks--;
            }
        }

        // ── Fire-breath stop animation timer ─────────────────────────────────
        // Detect the falling edge of FIRE_MODE (1 → anything else) and arm the
        // stop animation timer.
        //
        // We deliberately track FIRE_MODE (synced data set by our packet handler)
        // instead of isBreathingFire() (IaF's boolean).  IaF's internal
        // updateDragonServer() resets fireStopTicks to 0 every ~10 ticks, which
        // briefly flips isBreathingFire() to false even while the rider is still
        // holding the key.  Basing the edge-detect on isBreathingFire() caused:
        //   • fire_breath_stop playing spuriously mid-hold (the flicker triggered
        //     the stop timer before the next aiStep() refreshed isBreathingFire).
        //   • fire_breath_continue cutting out randomly on long holds.
        //
        // FIRE_MODE is only changed by handleFireInput() (packet) or when the
        // rider dismounts / is null, so it is immune to IaF's internal resets.
        byte currentFireMode = this.getFireMode();
        if (this.prevFireMode == 1 && currentFireMode != 1) {
            this.fireBreathStopTicks = FIRE_BREATH_STOP_DURATION;
        }
        if (this.fireBreathStopTicks > 0) {
            this.fireBreathStopTicks--;
        }
        this.prevFireMode = currentFireMode;

        if (this.level().getDifficulty() == Difficulty.PEACEFUL && this.getTarget() instanceof Player) {
            this.setTarget((LivingEntity) null);
        }



        if (this.isModelDead()) {
            if (this.isVehicle()) {
                this.ejectPassengers();
            }
            this.setHovering(false);
            this.setFlying(false);
        }

        // fireTicks/burnProgress for rider fire are managed in the
        // "Rider fire-breath tick" block below. IaF's updateDragonCommon
        // handles these for IaF's own fire paths (burningTarget etc).

        // ── Rider fire-breath tick ───────────────────────────────────────────
        //
        // IaF's updateDragonServer() owns isBreathingFire / fireTicks /
        // fireStopTicks and resets them every tick for any dragon when the
        // rider-owned fireStopTicks races to zero (confirmed via log analysis
        // of the IafDragonLogic source).
        //
        // Fix: use PRIVATE riderFireTicks / riderBurnProgress counters that
        // IaF can never see or reset. Temporarily swap riderBurnProgress into
        // this.burnProgress around the stimulateFire() call (stimulateFire
        // reads this.burnProgress internally for stream reach), then restore.
        // ────────────────────────────────────────────────────────────────────
        if (!this.level().isClientSide && this.getFireMode() == 1) {
            LivingEntity rider = this.getControllingPassenger();
            if (rider != null) {
                // Keep isBreathingFire and fireStopTicks refreshed every tick so
                // IaF's internal fireStopTicks countdown (which reaches 0 after ~10
                // ticks and calls setBreathingFire(false)) can never cut the stream
                // short while the rider is still holding the key. Without this the
                // stop animation plays spuriously mid-hold.
                this.setBreathingFire(true);
                this.fireStopTicks = 20;

                if (riderFireTicks    < 40) ++riderFireTicks;
                if (riderBurnProgress < 40) ++riderBurnProgress;

                LOGGER.info("[DragonFire] riderBreathTick riderFireTicks={} riderBurnProgress={}", riderFireTicks, riderBurnProgress);

                if (riderFireTicks > 20) {
                    this.setYRot(this.yBodyRot);
                    if (this.tickCount % 5 == 0) {
                        this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4.0F, 1.0F);
                    }
                    HitResult hit = this.rayTraceRider(
                            rider, 60.0, 1.0F);
                    LOGGER.info("[DragonFire] FIRING — hit={}", hit != null ? hit.getType() + " @ " + hit.getLocation() : "null");
                    if (hit != null) {
                        int savedBurnProgress = this.burnProgress;
                        this.burnProgress = this.riderBurnProgress;
                        this.stimulateFire(
                                hit.getLocation().x,
                                hit.getLocation().y,
                                hit.getLocation().z, 1);
                        this.burnProgress = savedBurnProgress;
                    }
                }
            }
        } else if (!this.level().isClientSide) {
            riderFireTicks    = 0;
            riderBurnProgress = 0;
        }

        if (this.diveCooldownTicks > 0) {
            --this.diveCooldownTicks;
        }
        if (this.fireballCooldownTicks > 0) {
            --this.fireballCooldownTicks;
        }

        boolean isDivingNow = this.isFlying()
                && !this.isHovering()
                && this.isGoingDown()
                && this.getDeltaMovement().y < -0.35
                && this.diveCooldownTicks == 0;

        if (isDivingNow) {
            // Ramp blend in over ~8 ticks
            this.diveBlend = Math.min(1.0f, this.diveBlend + 0.125f);
        } else {
            if (this.diveBlend > 0.0f) {
                // Ramp blend out over ~8 ticks; start cooldown once fully exited
                this.diveBlend = Math.max(0.0f, this.diveBlend - 0.125f);
                if (this.diveBlend == 0.0f) {
                    // 25-tick cooldown stops the animation re-triggering if the
                    // rider immediately re-presses down after levelling off.
                    this.diveCooldownTicks = 25;
                }
            }
        }

        // ── Autonomous flight for untamed dragons ────────────────────────────
        // IaF's EntityFireDragon-gated goals never fire for our custom class.
        // Drive random flight here so untamed adults still take to the air.
        if (!this.level().isClientSide
                && !this.isTame()
                && !this.isModelDead()
                && this.isAllowedToTriggerFlight()
                && this.random.nextInt(getFlightChancePerTick()) == 0) {
            this.setHovering(true);
            this.flyTicks = 0;
        }

        // ── Sync currentActiveAnimation → SYNCED_ANIM_ID every server tick ──
        // This is the single source of truth that drives client-side GeckoLib
        // controllers. AnimationState objects are server-only and are invisible
        // to clients; SYNCED_ANIM_ID bridges the gap via entity data packets.
        // MC dedupes entityData.set() so this only sends a packet on change.
        if (!this.level().isClientSide) {
            byte newId = animStateToId(this.currentActiveAnimation);
            // While the speak animation is still running, hold the SPEAK ID
            // even though ANIMATION_SPEAK is not stored in currentActiveAnimation.
            if (newId == ANIM_ID_NONE && this.speakAnimTicks > 0) {
                newId = ANIM_ID_SPEAK;
            }
            if (newId != getSyncedAnimId()) {
                this.entityData.set(SYNCED_ANIM_ID, newId);
            }
        }
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose poseIn) {
        float s = this.getScale();
        if (this.getDragonStage() <= 2) {
            // Baby: wide and flat so F3+B shows a disc rather than a clump of
            // equal-sided boxes.  Width > height gives a "lying dragon" profile.
            return EntityDimensions.scalable(4.0f, 1.5f).scale(s);
        } else {
            // Adult: similarly wider than tall so the hitbox covers the body
            // footprint without towering over the model.
            return EntityDimensions.scalable(5.5f, 2.5f).scale(s);
        }
    }

    public float getScale() {
        // -----------------------------------------------------------------------
        // Maps renderSize (1 – 30) to a visual + hitbox scale factor.
        //
        // Physical/visual size is CAPPED at the 85-day renderSize (≈15.5) so the
        // dragon stops growing physically after day 85, even as health and stage
        // continue to increase past that point.
        //
        // Baby dragons (stage 1–2) use a separate, much smaller formula so the
        // hatchling appears appropriately tiny when it first emerges from the egg.
        //
        // Adult compact range (capped at day-85 size ≈ scale 1.41):
        //   Stage 3  (renderSize  7–13)  → scale 0.78 – 1.23
        //   Stage 4  (renderSize 13–15.5)→ scale 1.23 – 1.41  ← hard cap
        // -----------------------------------------------------------------------
        // Size is capped at the scale equivalent to 85 in-game days (renderSize ≈ 15.5).
        final float SIZE_RS_CAP = 15.5f;
        float rs = Math.min(this.getRenderSize(), SIZE_RS_CAP);

        if (this.getDragonStage() <= 2) {
            // Baby hitbox/collision scale.  Kept large enough that the bounding
            // box height (≈ 2.5 × scale ≈ 1.7 b) is comparable to the rendered
            // baby model. The previous tiny value (~0.19) caused IaF's dragon
            // container GUI to compute an enormous zoom factor (it divides a
            // base size by getBbHeight()), then GeckoLib applied the visual
            // scale on top — making the GUI dragon look gigantic.
            // Visual rendering still uses getVisualScale() (decoupled), so the
            // in-world appearance of the baby is unchanged by this value.
            final float BABY_BASE  = 0.60F;
            final float BABY_COEFF = 0.09F;
            return BABY_BASE + rs * BABY_COEFF;
        }

        final float BASE  = 0.25F;   // minimum adult scale
        final float COEFF = 0.075F;  // growth per renderSize unit
        final float MAX   = 2.40F;   // ceiling (unreachable after 85-day cap)
        return Math.min(BASE + rs * COEFF, MAX);
    }

    public float getVisualScale() {
        // Size is capped at the visual scale equivalent to 85 in-game days.
        final float SIZE_RS_CAP = 15.5f;
        float rs = Math.min(this.getRenderSize(), SIZE_RS_CAP);

        if (this.getDragonStage() <= 2) {
            // Baby geo: kept intentionally small at hatch so the dragon looks
            // like a true hatchling.  At rs=1: vs = 1.20 + 0.15 = 1.35.
            final float BABY_BASE  = 1.20f;
            final float BABY_COEFF = 0.15f;
            return BABY_BASE + rs * BABY_COEFF;
        } else {
            // Adult geo: cap mirrors getScale() → hitbox parts stay aligned.
            final float BASE  = 0.45f;
            final float COEFF = 0.055f;
            final float MAX   = 2.10f;
            return Math.min(BASE + rs * COEFF, MAX);
        }
    }

    protected void checkFallDamage(double y, boolean onGroundIn, @NotNull BlockState state, @NotNull BlockPos pos) {
    }

    public float getRenderSize() {
        int stage = this.getDragonStage() - 1;
        float step = (this.growth_stages[stage][1] - this.growth_stages[stage][0]) / 25.0F;
        return this.getAgeInDays() > 125 ? this.growth_stages[stage][0] + step * 25.0F : this.growth_stages[stage][0] + step * (float)this.getAgeFactor();
    }

    private int getAgeFactor() {
        return this.getDragonStage() > 1 ? this.getAgeInDays() - 25 * (this.getDragonStage() - 1) : this.getAgeInDays();
    }

    public boolean doHurtTarget(@NotNull Entity entityIn) {
        this.getLookControl().setLookAt(entityIn, 30.0F, 30.0F);
        if (!this.isTackling() && !this.isModelDead()) {
            // ── tail_swing: play when target is behind the dragon ─────────
            if (this.tailSwingCooldown <= 0 && getCurrentAnimation() != ANIMATION_TAILWHACK) {
                Vec3 toLook = this.getLookAngle();
                Vec3 toTarget = entityIn.position().subtract(this.position()).normalize();
                double dot = toLook.dot(toTarget); // +1 = ahead, -1 = behind
                if (dot < 0.0) {
                    startAnimation(ANIMATION_TAILWHACK);
                    this.tailAnimTicks  = 48; // 47 ticks (2.333 s) + 1 tick buffer
                    this.tailSwingCooldown = 60; // 3-second re-trigger cooldown
                }
            }
            // ─────────────────────────────────────────────────────────────
            boolean flag = entityIn.hurt(this.level().damageSources().mobAttack(this), (float)((int)this.getAttribute(Attributes.ATTACK_DAMAGE).getValue()));
            if (flag) {
                this.doEnchantDamageEffects(this, entityIn);
            }

            return flag;
        } else {
            return false;
        }
    }

    public void rideTick() {
        Entity entity = this.getVehicle();
        if (this.isPassenger() && !entity.isAlive()) {
            this.stopRiding();
        } else {
            this.setDeltaMovement((double)0.0F, (double)0.0F, (double)0.0F);
            this.tick();
            if (this.isPassenger()) {
                this.updateRiding(entity);
            }
        }

    }

    public void updateRiding(Entity riding) {
        if (riding != null && riding.hasPassenger(this) && riding instanceof Player) {
            int i = riding.getPassengers().indexOf(this);
            float radius = (i == 2 ? -0.2F : 0.5F) + (float)(((Player)riding).isFallFlying() ? 2 : 0);
            float angle = ((float)Math.PI / 180F) * ((Player)riding).yBodyRot + (float)(i == 1 ? 90 : (i == 0 ? -90 : 0));
            double extraX = (double)(radius * Mth.sin((float)(Math.PI + (double)angle)));
            double extraZ = (double)(radius * Mth.cos(angle));
            double extraY = (riding.isShiftKeyDown() ? 1.2 : 1.4) + (i == 2 ? 0.4 : (double)0.0F);
            this.yHeadRot = ((Player)riding).yHeadRot;
            this.setYRot(((Player)riding).yHeadRot);
            this.setPos(riding.getX() + extraX, riding.getY() + extraY, riding.getZ() + extraZ);
            if ((this.getControlState() == 16 || ((Player)riding).isFallFlying()) && !riding.isPassenger()) {
                this.stopRiding();
                if (this.level().isClientSide) {
                    IceAndFire.sendMSGToServer(new MessageStartRidingMob(this.getId(), false, true));
                }
            }
        }

    }

    // Animation helper methods using Minecraft's AnimationState
    AnimationState getCurrentAnimation() {
        if (this.isModelDead()) return NO_ANIMATION;
        return this.currentActiveAnimation != null ? this.currentActiveAnimation : NO_ANIMATION;
    }

    void startAnimation(AnimationState animation) {
        if (!this.isModelDead() && animation != null) {
            this.currentActiveAnimation = animation;
            animation.start(this.tickCount);
            // Immediately sync the new animation ID to clients so there is no
            // one-tick delay. (The per-tick sync in aiStep() will keep it current.)
            if (!this.level().isClientSide) {
                byte id = animStateToId(animation);
                this.entityData.set(SYNCED_ANIM_ID, id);
            }
        }
    }

    protected void stopCurrentAnimation() {
        if (this.currentActiveAnimation != null) {
            this.currentActiveAnimation.stop();
            this.currentActiveAnimation = null;
            if (!this.level().isClientSide) {
                this.entityData.set(SYNCED_ANIM_ID, ANIM_ID_NONE);
            }
        }
    }

    int getAnimationTick(AnimationState animation) {
        if (animation != null && animation.isStarted()) {
            return (int) (animation.getAccumulatedTime() / 50); // Convert milliseconds to ticks
        }
        return 0;
    }

    public void playAmbientSound() {
        if (!this.isSleeping() && !this.isModelDead() && !this.level().isClientSide) {
            ANIMATION_SPEAK.start(this.tickCount);
            // Sync to clients so talk_controller fires on both sides.
            this.entityData.set(SYNCED_ANIM_ID, ANIM_ID_SPEAK);
            this.speakAnimTicks = SPEAK_ANIM_DURATION;
            super.playAmbientSound();
        }
    }

    protected void playHurtSound(@NotNull DamageSource source) {
        if (!this.isModelDead()) {
            ANIMATION_SPEAK.start(this.tickCount);
            if (!this.level().isClientSide) {
                this.entityData.set(SYNCED_ANIM_ID, ANIM_ID_SPEAK);
                this.speakAnimTicks = SPEAK_ANIM_DURATION;
            }
            super.playHurtSound(source);
        }
    }

    public AgeableMob getBreedOffspring(@NotNull ServerLevel serverWorld, @NotNull AgeableMob ageable) {
        return null;
    }

    public boolean canMate(@NotNull Animal otherAnimal) {
        if (otherAnimal instanceof com.github.alexthe666.iceandfire.entity.EntityDragonBase dragon) {
            if (otherAnimal != this && otherAnimal.getClass() == this.getClass()) {
                return this.isMale() && !dragon.isMale() || !this.isMale() && dragon.isMale();
            }
        }

        return false;
    }

    @Override
    public EntityDragonEgg createEgg(EntityDragonBase ageable) {
        return super.createEgg(ageable);
    }

    public int getStartMetaForType() {
        return 0;
    }

    public boolean isTargetBlocked(Vec3 target) {
        if (target != null) {
            BlockHitResult rayTrace = this.level().clip(new ClipContext(this.position().add((double)0.0F, (double)this.getEyeHeight(), (double)0.0F), target, Block.COLLIDER, Fluid.NONE, this));
            BlockPos sidePos = rayTrace.getBlockPos();
            if (!this.level().isEmptyBlock(sidePos)) {
                return true;
            } else {
                return rayTrace.getType() == Type.BLOCK;
            }
        } else {
            return false;
        }
    }

    private double getFlySpeed() {
        return (double)((2 + this.getAgeInDays() / 125 * 2) * (this.isTackling() ? 2 : 1));
    }

    public boolean isTackling() {
        return (Boolean)this.entityData.get(TACKLE);
    }

    public void setTackling(boolean tackling) {
        this.entityData.set(TACKLE, tackling);
    }

    public boolean isAgingDisabled() {
        return (Boolean)this.entityData.get(AGINGDISABLED);
    }

    public void setAgingDisabled(boolean isAgingDisabled) {
        this.entityData.set(AGINGDISABLED, isAgingDisabled);
    }

    public boolean isBoundToCrystal() {
        return (Boolean)this.entityData.get(CRYSTAL_BOUND);
    }

    public void setCrystalBound(boolean crystalBound) {
        this.entityData.set(CRYSTAL_BOUND, crystalBound);
    }

    /** Returns true while the eat animation is active (synced to client). */
    public boolean isEating() {
        return this.entityData.get(IS_EATING);
    }

    public void setEating(boolean eating) {
        this.entityData.set(IS_EATING, eating);
    }

    // ── SYNCED_ANIM_ID helpers ───────────────────────────────────────────────

    /**
     * Returns the animation ID that is synced to all clients.
     * GeckoLib controllers read this instead of currentActiveAnimation
     * (which is server-only and never seen by clients).
     */
    public byte getSyncedAnimId() {
        return this.entityData.get(SYNCED_ANIM_ID);
    }

    /**
     * Maps a server-side AnimationState object to a byte ID.
     * Null (no active animation) maps to ANIM_ID_NONE (0).
     */
    private byte animStateToId(@Nullable AnimationState state) {
        if (state == null)                    return ANIM_ID_NONE;
        if (state == ANIMATION_BITE)          return ANIM_ID_BITE;
        if (state == ANIMATION_SHAKEPREY)     return ANIM_ID_SHAKEPREY;
        if (state == ANIMATION_WINGBLAST)     return ANIM_ID_WINGBLAST;
        if (state == ANIMATION_ROAR)          return ANIM_ID_ROAR;
        if (state == ANIMATION_EPIC_ROAR)     return ANIM_ID_EPIC_ROAR;
        if (state == ANIMATION_TAILWHACK)     return ANIM_ID_TAILWHACK;
        if (state == ANIMATION_SPEAK)         return ANIM_ID_SPEAK;
        if (state == ANIMATION_FIRECHARGE)    return ANIM_ID_FIRECHARGE;
        if (state == ANIMATION_EAT)           return ANIM_ID_EAT;
        return ANIM_ID_NONE;
    }

    public float getDistanceSquared(Vec3 Vector3d) {
        float f = (float)(this.getX() - Vector3d.x);
        float f1 = (float)(this.getY() - Vector3d.y);
        float f2 = (float)(this.getZ() - Vector3d.z);
        return f * f + f1 * f1 + f2 * f2;
    }

    public abstract Item getVariantScale(int var1);

    public abstract Item getVariantEgg(int var1);

    public abstract Item getSummoningCrystal();

    public boolean isImmobile() {
        return this.getHealth() <= 0.0F || this.isOrderedToSit() && !this.isVehicle() || this.isModelDead() || this.isPassenger();
    }

    public boolean isInWater() {
        return super.isInWater() && this.getFluidHeight(FluidTags.WATER) > (double)Mth.floor((float)this.getDragonStage() / 2.0F);
    }

    public void travel(@NotNull Vec3 pTravelVector) {
        if (getCurrentAnimation() == ANIMATION_SHAKEPREY || !this.canMove() && !this.isVehicle() || this.isOrderedToSit()) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }

            pTravelVector = new Vec3((double)0.0F, (double)0.0F, (double)0.0F);
        }

        if (this.allowLocalMotionControl && this.getControllingPassenger() != null) {
            LivingEntity rider = this.getControllingPassenger();
            if (rider == null) {
                super.travel(pTravelVector);
            } else if (!this.isHovering() && !this.isFlying()) {
                if (!this.isInWater() && !this.isInLava()) {
                    double forward = (double)rider.zza;
                    double strafing = (double)(rider.xxa * 0.5F);
                    double vertical = pTravelVector.y;
                    float speed = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
                    float groundSpeedModifier = (float)((double)1.8F * this.getFlightSpeedModifier());
                    speed *= groundSpeedModifier;
                    forward *= (double)speed;
                    forward *= rider.isSprinting() ? (double)1.2F : (double)1.0F;
                    forward *= rider.zza > 0.0F ? (double)1.0F : (double)0.2F;
                    if (this.isControlledByLocalInstance()) {
                        this.setSpeed(speed);
                        super.travel(new Vec3(strafing, vertical, forward));
                    } else {
                        this.setDeltaMovement(Vec3.ZERO);
                    }

                    this.tryCheckInsideBlocks();
                    this.updatePitch(this.yOld - this.getY());
                } else {
                    double forward = (double)rider.zza;
                    double strafing = (double)rider.xxa;
                    double vertical = (double)0.0F;
                    float speed = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
                    if (this.isGoingUp() && !this.isGoingDown()) {
                        vertical = (double)0.5F;
                    } else if (this.isGoingDown() && !this.isGoingUp()) {
                        vertical = (double)-0.5F;
                    }

                    this.setSpeed(speed);
                    this.setZza((float)forward);
                    super.travel(pTravelVector.add(strafing, vertical, forward));
                }
            } else {
                double forward = (double)rider.zza;
                double strafing = (double)rider.xxa;
                double vertical = (double)0.0F;
                float speed = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
                float airSpeedModifier = (float)((double)4.0F + (double)0.8F * Mth.map((double)speed, this.minimumSpeed, this.maximumSpeed, (double)0.0F, (double)1.5F));
                speed *= airSpeedModifier;
                if (forward > (double)0.0F) {
                    this.setFlying(true);
                    this.setHovering(false);
                }

                if (this.isAttacking() && this.getXRot() > -5.0F && this.getDeltaMovement().length() > (double)1.0F) {
                    this.setTackling(true);
                } else {
                    this.setTackling(false);
                }

                this.gliding = this.allowMousePitchControl && rider.isSprinting();
                if (!this.gliding) {
                    speed += this.glidingSpeedBonus;
                    forward *= rider.zza > 0.0F ? (double)1.0F : (double)0.5F;
                    strafing *= (double)0.4F;
                    if (this.isGoingUp() && !this.isGoingDown()) {
                        vertical = (double)1.0F;
                    } else if (this.isGoingDown() && !this.isGoingUp()) {
                        vertical = (double)-1.0F;
                    } else if (this.isControlledByLocalInstance()) {
                    }
                } else {
                    speed *= 1.5F;
                    strafing *= (double)0.1F;
                    this.glidingSpeedBonus = (float)Mth.clamp((double)this.glidingSpeedBonus + this.getDeltaMovement().y * -0.05, -0.8, (double)1.5F);
                    speed += this.glidingSpeedBonus;
                    forward = (double)Mth.abs(Mth.cos(this.getXRot() * ((float)Math.PI / 180F)));
                    vertical = (double)Mth.abs(Mth.sin(this.getXRot() * ((float)Math.PI / 180F)));
                    if (this.isGoingUp() && !this.isGoingDown()) {
                        vertical = Math.max(vertical, (double)0.5F);
                    } else if (this.isGoingDown() && !this.isGoingUp()) {
                        vertical = Math.min(vertical, (double)-0.5F);
                    } else if (this.isGoingUp() && this.isGoingDown()) {
                        vertical = (double)0.0F;
                    } else if (this.getXRot() < 0.0F) {
                        vertical *= (double)1.0F;
                    } else if (this.getXRot() > 0.0F) {
                        vertical *= (double)-1.0F;
                    } else if (this.isControlledByLocalInstance()) {
                    }
                }

                this.glidingSpeedBonus -= (float)((double)this.glidingSpeedBonus * 0.01);
                if (this.isControlledByLocalInstance()) {
                    float flyingSpeed = speed * 0.1F;
                    this.setSpeed(flyingSpeed);
                    this.moveRelative(flyingSpeed, new Vec3(strafing, vertical, forward));
                    this.move(MoverType.SELF, this.getDeltaMovement());
                    this.setDeltaMovement(this.getDeltaMovement().multiply(new Vec3(0.9, 0.9, 0.9)));
                    Vec3 currentMotion = this.getDeltaMovement();
                    if (this.horizontalCollision) {
                        currentMotion = new Vec3(currentMotion.x, 0.1, currentMotion.z);
                    }

                    this.setDeltaMovement(currentMotion);
                    this.calculateEntityAnimation(false);
                } else {
                    this.setDeltaMovement(Vec3.ZERO);
                }

                this.tryCheckInsideBlocks();
                this.updatePitch(this.yOld - this.getY());
            }
        } else {
            super.travel(pTravelVector);
        }
    }

    protected void updatePitch(double verticalDelta) {
        if (this.isOverAir() && !this.isPassenger()) {
            if (!this.isHovering()) {
                this.incrementDragonPitch((float)verticalDelta * 10.0F);
            }

            this.setDragonPitch(Mth.clamp(this.getDragonPitch(), -60.0F, 40.0F));
            float plateau = 2.0F;
            float planeDist = (float)((Math.abs(this.getDeltaMovement().x) + Math.abs(this.getDeltaMovement().z)) * (double)6.0F);
            if (this.getDragonPitch() > 2.0F) {
                this.decrementDragonPitch(planeDist * Math.abs(this.getDragonPitch()) / 90.0F);
            }

            if (this.getDragonPitch() < -2.0F) {
                this.incrementDragonPitch(planeDist * Math.abs(this.getDragonPitch()) / 90.0F);
            }

            if (this.getDragonPitch() > 2.0F) {
                this.decrementDragonPitch(1.0F);
            } else if (this.getDragonPitch() < -2.0F) {
                this.incrementDragonPitch(1.0F);
            }

            if (this.getControllingPassenger() == null && this.getDragonPitch() < -45.0F && planeDist < 3.0F && this.isFlying() && !this.isHovering()) {
                this.setHovering(true);
            }
        } else if (Mth.abs(this.getDragonPitch()) < 1.0F) {
            this.setDragonPitch(0.0F);
        } else {
            this.setDragonPitch(this.getDragonPitch() / 1.5F);
        }

    }

    public void updateRider() {
        Entity controllingPassenger = this.getControllingPassenger();
        if (controllingPassenger instanceof Player rider) {
            this.ticksStill = 0;
            this.hoverTicks = 0;
            this.flyTicks = 0;
            if (this.isGoingUp()) {
                if (!this.isFlying() && !this.isHovering()) {
                    this.spacebarTicks += 2;
                }
            } else if (this.isDismounting() && (this.isFlying() || this.isHovering())) {
                this.setCommand(2);
            }

            if (this.spacebarTicks > 0) {
                --this.spacebarTicks;
            }

            if (this.spacebarTicks > 20 && this.getOwner() != null && this.getPassengers().contains(this.getOwner()) && !this.isFlying() && !this.isHovering() && !this.isInWater()) {
                this.setHovering(true);
                this.spacebarTicks = 0;
                this.glidingSpeedBonus = 0.0F;
            }

            if (this.isFlying() || this.isHovering()) {
                if (rider.zza > 0.0F) {
                    this.setFlying(true);
                    this.setHovering(false);
                } else {
                    this.setFlying(false);
                    this.setHovering(true);
                }

                if (!this.isOverAir() && this.isFlying() && rider.getXRot() > 10.0F && !this.isInWater()) {
                    this.setHovering(false);
                    this.setFlying(false);
                }

                if (!this.isOverAir() && this.isGoingDown() && !this.isInWater()) {
                    this.setFlying(false);
                    this.setHovering(false);
                }
            }

            if (this.isTackling()) {
                ++this.tacklingTicks;
                if (this.tacklingTicks == 40) {
                    this.tacklingTicks = 0;
                }

                if (!this.isFlying() && this.onGround()) {
                    this.tacklingTicks = 0;
                    this.setTackling(false);
                }

                List<Entity> victims = this.level().getEntities(this, this.getBoundingBox().expandTowards((double)2.0F, (double)2.0F, (double)2.0F), (potentialVictim) -> potentialVictim != rider && potentialVictim instanceof LivingEntity);
                victims.forEach((victim) -> this.logic.attackTarget(victim, rider, (float)(this.getDragonStage() * 3)));
            }

            if (this.isStriking() && this.getControllingPassenger() != null && this.getDragonStage() > 1) {
                this.setBreathingFire(true);
                this.riderShootFire(this.getControllingPassenger());
                this.fireStopTicks = 10;
            }

            if (this.isAttacking() && this.getControllingPassenger() != null && this.getControllingPassenger() instanceof Player) {
                LivingEntity target = DragonUtils.riderLookingAtEntity(this, this.getControllingPassenger(), (double)this.getDragonStage() + (this.getBoundingBox().maxX - this.getBoundingBox().minX));
                if (getCurrentAnimation() != ANIMATION_BITE) {
                    startAnimation(ANIMATION_BITE);
                }

                if (target != null && !DragonUtils.hasSameOwner(this, target)) {
                    int damage = (int)this.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
                    boolean didDamage = this.logic.attackTarget(target, rider, (float)damage);
                    if (didDamage && IafConfig.canDragonsHealFromBiting) {
                        this.heal((float)damage * 0.1F);
                    }
                }
            }

            if (this.getControllingPassenger() != null && this.getControllingPassenger().isShiftKeyDown()) {
                EntityDataProvider.getCapability(this.getControllingPassenger()).ifPresent((data) -> data.miscData.setDismounted(true));
                this.getControllingPassenger().stopRiding();
            }

            if (this.getTarget() != null && !this.getPassengers().isEmpty() && this.getOwner() != null && this.getPassengers().contains(this.getOwner())) {
                this.setTarget((LivingEntity)null);
            }

            if (this.getFeetBlockState().getFluidState().isSource() && this.isInWater() && !this.isGoingUp()) {
                this.setFlying(false);
                this.setHovering(false);
            }
        } else if (controllingPassenger instanceof EntityDreadQueen) {
            Player ridingPlayer = this.getRidingPlayer();
            if (ridingPlayer != null) {
                if (this.isGoingUp()) {
                    if (!this.isFlying() && !this.isHovering()) {
                        this.spacebarTicks += 2;
                    }
                } else if (this.isDismounting() && (this.isFlying() || this.isHovering())) {
                    this.setDeltaMovement(this.getDeltaMovement().add((double)0.0F, -0.04, (double)0.0F));
                    this.setFlying(false);
                    this.setHovering(false);
                }
            }

            if (!this.isDismounting() && (this.isFlying() || this.isHovering())) {
                this.setDeltaMovement(this.getDeltaMovement().add((double)0.0F, 0.01, (double)0.0F));
            }

            if (this.isStriking() && this.getControllingPassenger() != null && this.getDragonStage() > 1) {
                this.setBreathingFire(true);
                this.riderShootFire(this.getControllingPassenger());
                this.fireStopTicks = 10;
            }

            if (this.isAttacking() && this.getControllingPassenger() != null && this.getControllingPassenger() instanceof Player) {
                LivingEntity target = DragonUtils.riderLookingAtEntity(this, this.getControllingPassenger(), (double)this.getDragonStage() + (this.getBoundingBox().maxX - this.getBoundingBox().minX));
                if (getCurrentAnimation() != ANIMATION_BITE) {
                    startAnimation(ANIMATION_BITE);
                }

                if (target != null && !DragonUtils.hasSameOwner(this, target)) {
                    this.logic.attackTarget(target, ridingPlayer, (float)((int)this.getAttribute(Attributes.ATTACK_DAMAGE).getValue()));
                }
            }

            if (this.getControllingPassenger() != null && this.getControllingPassenger().isShiftKeyDown()) {
                EntityDataProvider.getCapability(this.getControllingPassenger()).ifPresent((data) -> data.miscData.setDismounted(true));
                this.getControllingPassenger().stopRiding();
            }

            if (this.isFlying()) {
                if (!this.isHovering() && this.getControllingPassenger() != null && !this.onGround() && Math.max(Math.abs(this.getDeltaMovement().x()), Math.abs(this.getDeltaMovement().z())) < (double)0.1F) {
                    this.setHovering(true);
                    this.setFlying(false);
                }
            } else if (this.isHovering() && this.getControllingPassenger() != null && !this.onGround() && Math.max(Math.abs(this.getDeltaMovement().x()), Math.abs(this.getDeltaMovement().z())) > (double)0.1F) {
                this.setFlying(true);
                this.usingGroundAttack = false;
                this.setHovering(false);
            }

            if (this.spacebarTicks > 0) {
                --this.spacebarTicks;
            }

            if (this.spacebarTicks > 20 && this.getOwner() != null && this.getPassengers().contains(this.getOwner()) && !this.isFlying() && !this.isHovering()) {
                this.setHovering(true);
            }

            if (this.isVehicle() && !this.isOverAir() && this.isFlying() && !this.isHovering() && this.flyTicks > 40) {
                this.setFlying(false);
            }
        }

    }

    public void move(@NotNull MoverType pType, @NotNull Vec3 pPos) {
        if (this.isOrderedToSit() && !this.isVehicle()) {
            pPos = new Vec3((double)0.0F, pPos.y(), (double)0.0F);
        }

        if (this.isVehicle()) {
            if (this.isControlledByLocalInstance()) {
                if (this.horizontalCollision) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply((double)0.6F, (double)1.0F, (double)0.6F));
                }

                super.move(pType, pPos);
            } else {
                super.move(pType, pPos);
            }

            this.setNoGravity(this.isHovering() || this.isFlying());
        } else {
            this.setNoGravity(false);
            super.move(pType, pPos);
        }

    }

    public void updateCheckPlayer() {
        double checkLength = this.getBoundingBox().getSize() * (double)3.0F;
        Player player = this.level().getNearestPlayer(this, checkLength);
        if (this.isSleeping() && player != null && !this.isOwnedBy(player) && !player.isCreative()) {
            this.setInSittingPose(false);
            this.setOrderedToSit(false);
            this.setTarget(player);
        }

    }

    public boolean isDirectPathBetweenPoints(Vec3 vec1, Vec3 vec2) {
        BlockHitResult rayTrace = this.level().clip(new ClipContext(vec1, new Vec3(vec2.x, vec2.y + (double)this.getBbHeight() * (double)0.5F, vec2.z), Block.COLLIDER, Fluid.NONE, this));
        return rayTrace.getType() != Type.BLOCK;
    }

    public void die(@NotNull DamageSource cause) {
        super.die(cause);
        this.setHunger(this.getHunger() + FoodUtils.getFoodPoints(this));
    }

    public void onHearFlute(Player player) {
        if (this.isTame() && this.isOwnedBy(player) && (this.isFlying() || this.isHovering())) {
            this.setFlying(false);
            this.setHovering(false);
        }

    }

    public abstract SoundEvent getRoarSound();

    public void roar() {
        if (!EntityGorgon.isStoneMob(this) && !this.isModelDead()) {
            if (this.random.nextBoolean()) {
                if (getCurrentAnimation() != ANIMATION_EPIC_ROAR) {
                    startAnimation(ANIMATION_EPIC_ROAR);
                    this.playSound(this.getRoarSound(), this.getSoundVolume() + 3.0F + (float)Math.max(0, this.getDragonStage() - 2), this.getVoicePitch() * 0.7F);
                }

                if (this.getDragonStage() > 3) {
                    int size = (this.getDragonStage() - 3) * 30;

                    for(Entity entity : this.level().getEntities(this, this.getBoundingBox().expandTowards((double)size, (double)size, (double)size))) {
                        boolean isStrongerDragon = entity instanceof com.github.alexthe666.iceandfire.entity.EntityDragonBase && ((com.github.alexthe666.iceandfire.entity.EntityDragonBase)entity).getDragonStage() >= this.getDragonStage();
                        if (entity instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity)entity;
                            if (!isStrongerDragon) {
                                if (!this.isOwnedBy(living) && !this.isOwnersPet(living)) {
                                    if (living.getItemBySlot(EquipmentSlot.HEAD).getItem() != IafItemRegistry.EARPLUGS.get()) {
                                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50 * size));
                                    }
                                } else {
                                    living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50 * size));
                                }
                            }
                        }
                    }
                }
            } else {
                if (getCurrentAnimation() != ANIMATION_ROAR) {
                    startAnimation(ANIMATION_ROAR);
                    this.playSound(this.getRoarSound(), this.getSoundVolume() + 2.0F + (float)Math.max(0, this.getDragonStage() - 3), this.getVoicePitch());
                }

                if (this.getDragonStage() > 3) {
                    int size = (this.getDragonStage() - 3) * 30;

                    for(Entity entity : this.level().getEntities(this, this.getBoundingBox().expandTowards((double)size, (double)size, (double)size))) {
                        boolean isStrongerDragon = entity instanceof com.github.alexthe666.iceandfire.entity.EntityDragonBase && ((com.github.alexthe666.iceandfire.entity.EntityDragonBase)entity).getDragonStage() >= this.getDragonStage();
                        if (entity instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity)entity;
                            if (!isStrongerDragon) {
                                if (!this.isOwnedBy(living) && !this.isOwnersPet(living)) {
                                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30 * size));
                                } else {
                                    living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * size));
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private boolean isOwnersPet(LivingEntity living) {
        return this.isTame() && this.getOwner() != null && living instanceof TamableAnimal && ((TamableAnimal)living).getOwner() != null && this.getOwner().is(((TamableAnimal)living).getOwner());
    }

    public boolean isDirectPathBetweenPoints(Entity entity, Vec3 vec1, Vec3 vec2) {
        HitResult movingobjectposition = this.level().clip(new ClipContext(vec1, vec2, Block.COLLIDER, Fluid.NONE, this));
        return movingobjectposition.getType() != Type.BLOCK;
    }

    public boolean shouldRenderEyes() {
        return !this.isSleeping() && !this.isModelDead() && !this.isBlinking() && !EntityGorgon.isStoneMob(this);
    }

    public boolean shouldAnimalsFear(Entity entity) {
        return DragonUtils.canTameDragonAttack(this, entity);
    }

    public void dropArmor() {
    }

    public boolean isChained() {
        AtomicBoolean isChained = new AtomicBoolean(false);
        EntityDataProvider.getCapability(this).ifPresent((data) -> isChained.set(data.chainData.getChainedTo().isEmpty()));
        return isChained.get();
    }

    protected void dropFromLootTable(@NotNull DamageSource damageSourceIn, boolean attackedRecently) {
    }

    public HitResult rayTraceRider(Entity rider, double blockReachDistance, float partialTicks) {
        Vec3 Vector3d = rider.getEyePosition(partialTicks);
        Vec3 Vector3d1 = rider.getViewVector(partialTicks);
        Vec3 Vector3d2 = Vector3d.add(Vector3d1.x * blockReachDistance, Vector3d1.y * blockReachDistance, Vector3d1.z * blockReachDistance);
        return this.level().clip(new ClipContext(Vector3d, Vector3d2, Block.COLLIDER, Fluid.NONE, this));
    }

    protected float getRideHeightBase() {
        // Shoulder / between-wing-root height.  The adult geo wing bone sits at
        // ~2.0 blocks above the entity origin at visualScale 1.0.  We scale by
        // visualScale so the saddle tracks the model at every size.
        // Formula keeps the same quadratic curve shape as the original but
        // anchored to the shoulderblade Y rather than the neck/head.
        return 0.00150F * Mth.square(this.getRenderSize()) + 0.17F * this.getRenderSize() - 0.8F;
    }

    protected float getRideHorizontalBase() {
        // Between the shoulderblades → forward offset is much smaller than the
        // old "neck" position.  Positive values push the saddle forward of the
        // hip pivot; ~0.5–1.0 blocks forward lands between the wing roots.
        return 0.00120F * Mth.square(this.getRenderSize()) + 0.06F * this.getRenderSize() + 0.2F;
    }

    public Vec3 getRiderPosition() {
        float vs          = this.getVisualScale();
        float dragonPitch = this.getDragonPitch();
        LivingEntity rider = this.getControllingPassenger();

        // Forward offset: +2.4 lands the rider at the wing-root saddle area.
        float xzMod = this.getRideHorizontalBase() + 2.4F;

        // ── Saddle height ─────────────────────────────────────────────────────
        // We want the rider to sit ON the dragon's back surface, not above it.
        //
        // Adult geo back/spine sits at roughly 2.10 blocks above the entity
        // origin at visualScale = 1.0 (measured from dragon_geo.json bone Y
        // values for the body/torso, not the wing-root which is higher).
        // Using vs*3.12 (the wing bone) was too high — the player floated.
        //
        // A small negative clearance (-0.15f) sinks the player slightly into
        // the saddle so they look seated rather than balanced on top.
        float bodyTop   = (this.getDragonStage() <= 2) ? (vs * 0.34f) : (vs * 2.10f);
        float shoulderY = bodyTop - 0.15f;

        // Walking bounce: small upward push when the rider drives forward on ground.
        if (!this.isFlying() && !this.isHovering()) {
            if (rider != null && rider.zza > 0.0F) {
                this.riderWalkingExtraY = Math.min(0.25F, this.riderWalkingExtraY + 0.04F);
            } else {
                this.riderWalkingExtraY = Math.max(0.0F, this.riderWalkingExtraY - 0.08F);
            }
            shoulderY += this.riderWalkingExtraY;
        }

        // Full 2-D pitch rotation in the (forward, up) plane.
        float pitchRad   = dragonPitch * (float)(Math.PI / 180.0);
        float cosP       = (float)Math.cos(pitchRad);
        float sinP       = (float)Math.sin(pitchRad);
        float worldForward = xzMod * cosP - shoulderY * sinP;
        float worldUp      = xzMod * sinP + shoulderY * cosP;

        // Floor: prevent rider going below entity feet on steep dives.
        float minWorldUp = (this.getDragonStage() <= 2) ? 0.8f : 2.0f;
        worldUp = Math.max(worldUp, minWorldUp);

        float yawRad   = (this.getYRot() + 90.0F) * (float)(Math.PI / 180.0);
        float headPosX = (float)(this.getX() + worldForward * Math.cos(yawRad));
        float headPosY = (float)(this.getY() + worldUp);
        float headPosZ = (float)(this.getZ() + worldForward * Math.sin(yawRad));
        return new Vec3(headPosX, headPosY, headPosZ);
    }

    public @NotNull Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return passenger.isInWall() ? this.position().add((double)0.0F, (double)1.0F, (double)0.0F) : this.getRiderPosition().add((double)0.0F, (double)passenger.getBbHeight(), (double)0.0F);
    }

    public void kill() {
        this.remove(RemovalReason.KILLED);
        this.setDeathStage(this.getAgeInDays() / 5);
        this.setModelDead(false);
    }

    public boolean isAlliedTo(@NotNull Entity entityIn) {
        if (this.isModelDead()) {
            return true;
        } else {
            if (this.isTame()) {
                LivingEntity livingentity = this.getOwner();
                if (entityIn == livingentity) {
                    return true;
                }

                if (entityIn instanceof TamableAnimal) {
                    return ((TamableAnimal)entityIn).isOwnedBy(livingentity);
                }

                if (livingentity != null) {
                    return livingentity.isAlliedTo(entityIn);
                }
            }

            return super.isAlliedTo(entityIn);
        }
    }

    /**
     * Returns the world position of the head hitbox part, which tracks the
     * rendered jaw/snout position. Used as the fire-stream origin so particles
     * and projectiles visually emerge from the dragon's mouth rather than its
     * body centre.  Falls back to {@link #getHeadPosition()} when the part
     * hasn't been initialised yet (e.g. very first tick after spawn).
     */
    public Vec3 getHeadPartPosition() {
        if (this.headPart != null) {
            return new Vec3(this.headPart.getX(), this.headPart.getY(), this.headPart.getZ());
        }
        return this.getHeadPosition();
    }

    public Vec3 getHeadPosition() {
        float sitProg = this.sitProgress * 0.015F;
        float deadProg = this.modelDeadProgress * -0.02F;
        float hoverProg = this.hoverProgress * 0.03F;
        float flyProg = this.flyProgress * 0.01F;
        int animTick = getAnimationTick(ANIMATION_EPIC_ROAR);
        int tick;
        if (animTick < 10) {
            tick = animTick;
        } else if (animTick > 50) {
            tick = 60 - animTick;
        } else {
            tick = 10;
        }

        float epicRoarProg = getCurrentAnimation() == ANIMATION_EPIC_ROAR ? (float)tick * 0.1F : 0.0F;
        float sleepProg = this.sleepProgress * -0.025F;
        float pitchMulti = 0.0F;
        float pitchAdjustment = 0.0F;
        float pitchMinus = 0.0F;
        float dragonPitch = -this.getDragonPitch();
        if (this.isFlying() || this.isHovering()) {
            pitchMulti = Mth.sin((float)Math.toRadians((double)dragonPitch));
            pitchAdjustment = 1.2F;
            pitchMulti *= 2.1F * Math.abs(dragonPitch) / 90.0F;
            if (pitchMulti > 0.0F) {
                pitchMulti *= 1.5F - pitchMulti * 0.5F;
            }

            if (pitchMulti < 0.0F) {
                pitchMulti *= 1.3F - pitchMulti * 0.1F;
            }

            pitchMinus = 0.3F * Math.abs(dragonPitch / 90.0F);
            if (dragonPitch >= 0.0F) {
                pitchAdjustment = 0.6F * Math.abs(dragonPitch / 90.0F);
                pitchMinus = 0.95F * Math.abs(dragonPitch / 90.0F);
            }
        }

        // ── Corrected head position ───────────────────────────────────────────
        // The adult geo head bone sits at ~8.6 blocks forward and ~3.0 blocks up
        // from the entity origin at visualScale = 1.0.  Scale these by the actual
        // visual scale so the fire stream exits from the dragon's mouth at every
        // size, not from its chest (the old constant-factor formula anchored to
        // renderSize * 0.3 under-estimated both X/Z and Y at adult sizes).
        float vs = this.getVisualScale();
        float baseHeadForward = vs * 8.6f; // geo forward offset at this scale
        float baseHeadY       = vs * 3.0f; // geo head-bone height at this scale
        float flightXz = 1.0F + flyProg + hoverProg;

        float xzMod = baseHeadForward * flightXz
                + this.getRenderSize() * (0.3F * Mth.sin((float)((double)(dragonPitch + 90.0F) * Math.PI / (double)180.0F)) * pitchAdjustment - pitchMinus - hoverProg * 0.45F);

        float headPosX = (float)(this.getX() + (double)(xzMod * Mth.cos((float)((double)(this.getYRot() + 90.0F) * Math.PI / (double)180.0F))));
        float headPosY = (float)(this.getY() + baseHeadY + (sitProg + hoverProg + deadProg + epicRoarProg + sleepProg + flyProg + pitchMulti) * this.getRenderSize() * 0.3F);
        float headPosZ = (float)(this.getZ() + (double)(xzMod * Mth.sin((float)((double)(this.getYRot() + 90.0F) * Math.PI / (double)180.0F))));
        return new Vec3((double)headPosX, (double)headPosY, (double)headPosZ);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CUSTOM FIRE SYSTEM
    // ═══════════════════════════════════════════════════════════════════════
    //
    // Why this exists
    // ───────────────
    // IaF's fire-routing inside IafDragonLogic.updateDragonAttack() is gated
    // on instanceof EntityFireDragon / EntityIceDragon.  Our class will never
    // pass those checks, so IaF will never call tryScorchTarget() or
    // breathFireAtPos() for us.
    //
    // Instead we run the entire fire pipeline from our own keybind packets:
    //
    //   DragonFireKeybinds  (client, polls keys every tick)
    //       ↓  PacketDragonFireInput
    //   DragonFireNetwork   (Forge SimpleChannel, C→S)
    //       ↓  handleFireInput(byte mode)
    //   EntityCustomDragon  (server, drives fire state + stimulateFire)
    //
    // Fire modes
    // ──────────
    //   0  player released keys → stop fire
    //   1  FIRE_BREATH held     → continuous stream toward raytrace hit
    //   2  FIRE_BALL tapped     → one EntityDragonFireCharge projectile
    // ═══════════════════════════════════════════════════════════════════════

    // ── Synced-data accessors ────────────────────────────────────────────────

    public byte getFireMode() {
        return this.entityData.get(FIRE_MODE);
    }

    public void setFireMode(byte mode) {
        this.entityData.set(FIRE_MODE, mode);
    }

    // ── FIX: override so it reads this class's shadow fireTicks field ────────
    //
    // EntityDragonBase.isActuallyBreathingFire() is compiled to read
    // EntityDragonBase.fireTicks (always 0) because field access in Java is
    // NOT polymorphic.  Without this override the stream gate is permanently
    // false and stimulateFire() is never reached.
    @Override
    public boolean isActuallyBreathingFire() {
        return this.fireTicks > 20 && this.isBreathingFire();
    }

    // ── Packet entry-point (called on the server thread) ─────────────────────

    /**
     * Called by DragonFireNetwork.PacketDragonFireInput.handle() on the server
     * thread after the packet has been security-checked.
     *
     * @param mode  0=stop  1=breath  2=fireball
     */
    public void handleFireInput(byte mode) {
        LivingEntity rider = this.getControllingPassenger();
        LOGGER.info("[DragonFire] handleFireInput mode={} rider={} isBaby={}",
                mode, rider != null ? rider.getName().getString() : "null", this.isBaby());

        if (rider == null || this.isBaby()) {
            this.setFireMode((byte) 0);
            this.setBreathingFire(false);
            return;
        }

        switch (mode) {

            case 1 -> {
                // ── Breath mode ─────────────────────────────────────────────
                // Only set state here. stimulateFire is called every server
                // tick in aiStep() because the client de-dupes mode=1 packets
                // and only sends one on key-press — not once per tick.
                this.setFireMode((byte) 1);
                this.setBreathingFire(true);
                this.fireStopTicks = 10; // grace window before counters reset
                LOGGER.info("[DragonFire] Breath state SET — isBreathingFire={} fireTicks={} burnProgress={}",
                        this.isBreathingFire(), this.fireTicks, this.burnProgress);
            }

            case 2 -> {
                // ── Fireball mode ────────────────────────────────────────────
                // Use a dedicated cooldown field — fireStopTicks is set to 10
                // every tick by the striking system and cannot be used here.
                if (this.fireballCooldownTicks > 0) break;

                this.setFireMode((byte) 2);
                // Roar animation plays when launching a fireball projectile.
                if (getCurrentAnimation() != ANIMATION_ROAR
                        && getCurrentAnimation() != ANIMATION_EPIC_ROAR) {
                    startAnimation(ANIMATION_ROAR);
                }

                // Spawn the fireball from the actual head-part world position so it
                // visually leaves the mouth rather than the body centre.
                Vec3 headVec = this.getHeadPartPosition();
                double d2 = rider.getLookAngle().x;
                double d3 = rider.getLookAngle().y;
                double d4 = rider.getLookAngle().z;
                // tiny spread so it doesn't feel laser-precise
                d2 += this.random.nextGaussian() * 0.0075;
                d3 += this.random.nextGaussian() * 0.0075;
                d4 += this.random.nextGaussian() * 0.0075;

                this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4.0F, 1.0F);
                EntityDragonFireCharge fireball = new EntityDragonFireCharge(
                        (EntityType<?>) IafEntityRegistry.FIRE_DRAGON_CHARGE.get(),
                        this.level(), this, d2, d3, d4);
                fireball.setPos(headVec.x, headVec.y, headVec.z);
                this.level().addFreshEntity(fireball);

                // 20-tick cooldown on its own counter
                this.fireballCooldownTicks = 20;

                // Immediately reset mode so the client doesn't lock in fireball
                this.setFireMode((byte) 0);
            }

            default -> {
                // ── Stop mode (0) ────────────────────────────────────────────
                this.setFireMode((byte) 0);
                this.setBreathingFire(false);
                // Clear any lingering fireball (roar) or charge animation.
                byte cur = this.getSyncedAnimId();
                if (cur == ANIM_ID_FIRECHARGE || cur == ANIM_ID_ROAR || cur == ANIM_ID_EPIC_ROAR) {
                    this.stopCurrentAnimation();
                }
            }
        }
    }

    // ── breathFireAtPos (dragonforge fuelling) ───────────────────────────────

    /**
     * Called by EntityDragonBase.updateBurnTarget() when this dragon is
     * assigned to heat a dragonforge.  IaF calls this on the base class;
     * our override performs the actual fire stimulation.
     */
    @Override
    protected void breathFireAtPos(BlockPos target) {
        if (this.isBreathingFire()) {
            if (this.isActuallyBreathingFire()) {
                this.setYRot(this.yBodyRot);
                if (this.tickCount % 5 == 0) {
                    this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4.0F, 1.0F);
                }
                this.stimulateFire(
                        target.getX() + 0.5,
                        target.getY() + 0.5,
                        target.getZ() + 0.5, 1);
            }
        } else {
            this.setBreathingFire(true);
        }
    }

    // ── stimulateFire ────────────────────────────────────────────────────────

    /**
     * Drives the visual fire stream + block destruction toward (burnX,burnY,burnZ).
     *
     * syncType values (matching EntityFireDragon convention):
     *   1  server → all clients  (stream particles)
     *   2  client → server       (rider, stream)
     *   3  server → all clients  (fireball charge)
     *   4  client → server       (rider, fireball)
     */
    @Override
    public void stimulateFire(double burnX, double burnY, double burnZ, int syncType) {
        // Respect the DragonFireEvent so other mods can cancel or react
        if (MinecraftForge.EVENT_BUS.post(new DragonFireEvent(this, burnX, burnY, burnZ))) {
            return;
        }

        // ── Sync particles to clients ────────────────────────────────────────
        if (syncType == 1 && !this.level().isClientSide) {
            IceAndFire.sendMSGToAll(new MessageDragonSyncFire(
                    this.getId(), burnX, burnY, burnZ, 0));
        }
        if (syncType == 2 && this.level().isClientSide) {
            IceAndFire.NETWORK_WRAPPER.sendToServer(new MessageDragonSyncFire(
                    this.getId(), burnX, burnY, burnZ, 0));
        }

        // ── Fireball charge variants (syncType 3/4) ──────────────────────────
        if (syncType == 3 && !this.level().isClientSide) {
            IceAndFire.sendMSGToAll(new MessageDragonSyncFire(
                    this.getId(), burnX, burnY, burnZ, 5));
        }
        if (syncType == 4 && this.level().isClientSide) {
            IceAndFire.NETWORK_WRAPPER.sendToServer(new MessageDragonSyncFire(
                    this.getId(), burnX, burnY, burnZ, 5));
        }

        // syncType 3/4: spawn a fireball at the charge target
        if (syncType > 2 && syncType < 6) {
            if (getCurrentAnimation() != ANIMATION_FIRECHARGE) {
                startAnimation(ANIMATION_FIRECHARGE);
            } else if (getAnimationTick(ANIMATION_FIRECHARGE) == 20) {
                this.setYRot(this.yBodyRot);
                Vec3 head = this.getHeadPosition();
                double d2 = burnX - head.x;
                double d3 = burnY - head.y;
                double d4 = burnZ - head.z;
                d2 += this.random.nextGaussian() * 0.0075;
                d3 += this.random.nextGaussian() * 0.0075;
                d4 += this.random.nextGaussian() * 0.0075;
                this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4.0F, 1.0F);
                EntityDragonFireCharge fb = new EntityDragonFireCharge(
                        (EntityType<?>) IafEntityRegistry.FIRE_DRAGON_CHARGE.get(),
                        this.level(), this, d2, d3, d4);
                fb.setPos(head.x, head.y, head.z);
                if (!this.level().isClientSide) {
                    this.level().addFreshEntity(fb);
                }
                this.randomizeAttacks();
            }
            return;
        }

        // ── Continuous stream (syncType 1/2) ─────────────────────────────────
        this.getNavigation().stop();
        this.burnParticleX = burnX;
        this.burnParticleY = burnY;
        this.burnParticleZ = burnZ;

        // Use getHeadPosition() (animation-aware) rather than getHeadPartPosition()
        // (static hitbox part XYZ).  getHeadPartPosition() returns the server-tick
        // body-offset position of the hitbox part, which does NOT account for fly/
        // hover/pitch animation progress — so the stream origin drifts away from
        // the visible mouth when the dragon is airborne or pitching.
        // getHeadPosition() recomputes the mouth world-position from the same
        // progress values the renderer uses, giving a correct visual match.
        Vec3 headPos  = this.getHeadPosition();
        double d2     = burnX - headPos.x;
        double d3     = burnY - headPos.y;
        double d4     = burnZ - headPos.z;
        double distance = Math.max(2.5 * this.distanceToSqr(burnX, burnY, burnZ), 0.0);
        double conquered = (double) this.burnProgress / 40.0 * distance;
        int increment    = (int) Math.ceil(conquered / 100.0);
        // Lower number = more frequent particles = thicker/denser beam.
        // Stage 1-3 → every other particle; stage 4-5 → every particle.
        int particleFreq = this.getDragonStage() <= 3 ? 2 : 1;

        // Offset the particle stream origin forward along the beam so particles
        // emerge from just in front of the snout rather than from the head-bone
        // centre, which often sits inside the mesh and clips visually.
        // Base of 1.8 keeps particles outside even on small/baby dragons;
        // the scale factor (0.22) grows the gap proportionally for large adults.
        double beamLen = Math.sqrt(d2 * d2 + d3 * d3 + d4 * d4);
        double mouthOffset = 1.8 + this.getRenderSize() * 0.22;
        double ox = beamLen > 0 ? (d2 / beamLen) * mouthOffset : 0;
        double oy = beamLen > 0 ? (d3 / beamLen) * mouthOffset : 0;
        double oz = beamLen > 0 ? (d4 / beamLen) * mouthOffset : 0;

        for (int i = 0; (double) i < conquered; i += increment) {
            double px = headPos.x + ox + d2 * ((float) i / (float) distance);
            double py = headPos.y + oy + d3 * ((float) i / (float) distance);
            double pz = headPos.z + oz + d4 * ((float) i / (float) distance);

            if (this.canPositionBeSeen(px, py, pz)) {
                // Client: spawn fire_spit particles along the stream.
                // The velocity encodes the direction along the ray so the
                // particle drifts toward the target with a little spread.
                if (this.level().isClientSide && this.random.nextInt(particleFreq) == 0) {
                    // Reduced spread (0.06) keeps the beam tight and focused while still
                    // looking organic; the old 0.12 made it appear ragged and thin.
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
                // Server: destroy blocks the stream hits
                HitResult result = this.level().clip(new ClipContext(
                        new Vec3(this.getX(), this.getY() + this.getEyeHeight(), this.getZ()),
                        new Vec3(px, py, pz),
                        Block.COLLIDER, Fluid.NONE, this));
                Vec3 hit = result.getLocation();
                IafDragonDestructionManager.destroyAreaBreath(
                        this.level(), BlockPos.containing(hit), this);
            }
        }

        // At full charge: burn the target block/area
        if ((double) this.burnProgress >= 40.0 && this.canPositionBeSeen(burnX, burnY, burnZ)) {
            double sx = burnX + this.random.nextFloat() * 3.0 - 1.5;
            double sy = burnY + this.random.nextFloat() * 3.0 - 1.5;
            double sz = burnZ + this.random.nextFloat() * 3.0 - 1.5;
            if (!this.level().isClientSide) {
                IafDragonDestructionManager.destroyAreaBreath(
                        this.level(), BlockPos.containing(sx, sy, sz), this);
            }
        }
    }

    public void randomizeAttacks() {
        this.airAttack = Air.values()[this.getRandom().nextInt(Air.values().length)];
        this.groundAttack = Ground.values()[this.getRandom().nextInt(Ground.values().length)];
    }

    public boolean shouldBlockExplode(@NotNull Explosion explosionIn, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, BlockState blockStateIn, float explosionPower) {
        return !(blockStateIn.getBlock() instanceof IDragonProof) && DragonUtils.canDragonBreak(blockStateIn, this);
    }

    public void tryScorchTarget() {
        LivingEntity entity = this.getTarget();
        if (entity != null) {
            float distX = (float)(entity.getX() - this.getX());
            float distZ = (float)(entity.getZ() - this.getZ());
            if (this.isBreathingFire()) {
                if (this.isActuallyBreathingFire()) {
                    this.setYRot(this.yBodyRot);
                    if (this.tickCount % 5 == 0) {
                        this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4.0F, 1.0F);
                    }

                    this.stimulateFire(this.getX() + (double)(distX * (float)this.fireTicks / 40.0F), entity.getY(), this.getZ() + (double)(distZ * (float)this.fireTicks / 40.0F), 1);
                }
            } else {
                this.setBreathingFire(true);
            }
        }

    }

    public void setTarget(@Nullable LivingEntity LivingEntityIn) {
        super.setTarget(LivingEntityIn);
        this.flightManager.onSetAttackTarget(LivingEntityIn);
    }

    public boolean wantsToAttack(@NotNull LivingEntity target, @NotNull LivingEntity owner) {
        if (this.isTame() && target instanceof TamableAnimal tamableTarget) {
            UUID targetOwner = tamableTarget.getOwnerUUID();
            if (targetOwner != null && targetOwner.equals(this.getOwnerUUID())) {
                return false;
            }
        }

        return super.wantsToAttack(target, owner);
    }

    public boolean canAttack(@NotNull LivingEntity target) {
        return super.canAttack(target) && DragonUtils.isAlive(target);
    }

    public boolean isPart(Entity entityHit) {
        return this.headPart != null && this.headPart.is(entityHit) || this.neckPart != null && this.neckPart.is(entityHit) || this.leftWingLowerPart != null && this.leftWingLowerPart.is(entityHit) || this.rightWingLowerPart != null && this.rightWingLowerPart.is(entityHit) || this.leftWingUpperPart != null && this.leftWingUpperPart.is(entityHit) || this.rightWingUpperPart != null && this.rightWingUpperPart.is(entityHit) || this.tail1Part != null && this.tail1Part.is(entityHit) || this.tail2Part != null && this.tail2Part.is(entityHit) || this.tail3Part != null && this.tail3Part.is(entityHit) || this.tail4Part != null && this.tail4Part.is(entityHit);
    }

    public double getFlightSpeedModifier() {
        return IafConfig.dragonFlightSpeedMod;
    }

    public boolean isAllowedToTriggerFlight() {
        return (this.hasFlightClearance() && this.onGround() || this.isInWater()) && !this.isOrderedToSit() && this.getPassengers().isEmpty() && !this.isBaby() && !this.isSleeping() && this.canMove();
    }

    public BlockPos getEscortPosition() {
        return this.getOwner() != null ? new BlockPos(this.getOwner().blockPosition()) : this.blockPosition();
    }

    public boolean shouldTPtoOwner() {
        return this.getOwner() != null && this.distanceTo(this.getOwner()) > 10.0F;
    }

    public boolean isSkeletal() {
        return this.getDeathStage() >= this.getAgeInDays() / 5 / 2;
    }

    public boolean save(@NotNull CompoundTag compound) {
        return this.saveAsPassenger(compound);
    }

    public void playSound(@NotNull SoundEvent soundIn, float volume, float pitch) {
        if (soundIn != SoundEvents.GENERIC_EAT && soundIn != this.getAmbientSound() && soundIn != this.getHurtSound(this.level().damageSources().generic()) && soundIn != this.getDeathSound() && soundIn != this.getRoarSound()) {
            super.playSound(soundIn, volume, pitch);
        } else if (!this.isSilent() && this.headPart != null) {
            this.level().playSound((Player)null, this.headPart.getX(), this.headPart.getY(), this.headPart.getZ(), soundIn, this.getSoundSource(), volume, pitch);
        }

    }

    public @NotNull SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    public boolean hasFlightClearance() {
        BlockPos topOfBB = BlockPos.containing((double)this.getBlockX(), this.getBoundingBox().maxY, (double)this.getBlockZ());

        for(int i = 1; i < 4; ++i) {
            if (!this.level().isEmptyBlock(topOfBB.above(i))) {
                return false;
            }
        }

        return true;
    }

    public @NotNull ItemStack getItemBySlot(EquipmentSlot slotIn) {
        ItemStack var10000;
        switch (slotIn) {
            case OFFHAND -> var10000 = this.dragonInventory.getItem(0);
            case HEAD -> var10000 = this.dragonInventory.getItem(1);
            case CHEST -> var10000 = this.dragonInventory.getItem(2);
            case LEGS -> var10000 = this.dragonInventory.getItem(3);
            case FEET -> var10000 = this.dragonInventory.getItem(4);
            default -> var10000 = super.getItemBySlot(slotIn);
        }

        return var10000;
    }

    public void setItemSlot(EquipmentSlot slotIn, @NotNull ItemStack stack) {
        switch (slotIn) {
            case OFFHAND -> this.dragonInventory.setItem(0, stack);
            case HEAD -> this.dragonInventory.setItem(1, stack);
            case CHEST -> this.dragonInventory.setItem(2, stack);
            case LEGS -> this.dragonInventory.setItem(3, stack);
            case FEET -> this.dragonInventory.setItem(4, stack);
            default -> super.getItemBySlot(slotIn);
        }

    }

    public SoundEvent getBabyFireSound() {
        return SoundEvents.FIRE_EXTINGUISH;
    }

    protected boolean isPlayingAttackAnimation() {
        AnimationState current = getCurrentAnimation();
        return current == ANIMATION_BITE || current == ANIMATION_SHAKEPREY || current == ANIMATION_WINGBLAST || current == ANIMATION_TAILWHACK;
    }

    protected IafDragonLogic createDragonLogic() {
        return new IafDragonLogic(this);
    }

    protected int getFlightChancePerTick() {
        return 1500;
    }

    public void onRemovedFromWorld() {
        if (IafConfig.chunkLoadSummonCrystal && this.isBoundToCrystal()) {
            DragonPosWorldData data = DragonPosWorldData.get(this.level());
            if (data != null) {
                data.addDragon(this.getUUID(), this.blockPosition());
            }
        }

        super.onRemovedFromWorld();
    }

    public int maxSearchNodes() {
        return (int)this.getAttribute(Attributes.FOLLOW_RANGE).getValue();
    }

    public boolean isSmallerThanBlock() {
        return false;
    }

    public float getXZNavSize() {
        return Math.max(1.4F, this.getBbWidth() / 2.0F);
    }

    public int getYNavSize() {
        return Mth.ceil(this.getBbHeight());
    }

    public void containerChanged(@NotNull Container invBasic) {
        if (!this.level().isClientSide) {
            this.updateAttributes();
        }

    }

    // ── Sound-keyframe helpers ───────────────────────────────────────────────

    /**
     * Maps a GeckoLib sound-keyframe marker name (as written in the animation
     * JSON file) to the corresponding {@link SoundEvent}.  Returns {@code null}
     * for unrecognised names so callers can silently skip them.
     *
     * <p>Add new entries here whenever you add a sound keyframe to any
     * animation, keeping the string identical to the JSON marker value.
     */
    @Nullable
    private static SoundEvent resolveDragonSound(String markerName) {
        return switch (markerName) {
            case "tail_whack"              -> DragonMod.SOUND_TAIL_WHACK.get();
            case "walk"                    -> DragonMod.SOUND_WALK.get();
            case "bite"                    -> DragonMod.SOUND_BITE.get();
            case "eating"                  -> DragonMod.SOUND_EATING.get();
            case "epic_roar", "epicroar"   -> DragonMod.SOUND_EPIC_ROAR.get();
            case "fly"                     -> DragonMod.SOUND_FLIGHT.get();
            case "roar"                    -> DragonMod.SOUND_ROAR.get();
            case "run"                     -> DragonMod.SOUND_RUN.get();
            case "shake_prey"              -> DragonMod.SOUND_SHAKE_PREY.get();
            case "speak"                   -> DragonMod.SOUND_SPEAK.get();
            default                        -> null;
        };
    }

    /**
     * Returns a GeckoLib {@code SoundKeyframeHandler} that every animation
     * controller should share.
     *
     * <h3>How it works</h3>
     * GeckoLib fires the handler on <em>both</em> sides when animation
     * processing passes a sound keyframe marker.  We gate on the server
     * ({@code if (isClientSide) return}) so the sound is played once via
     * {@link Level#playSound(Player, BlockPos, SoundEvent, SoundSource, float, float)}
     * with a {@code null} player, which broadcasts the event to all nearby
     * clients.  This avoids the handler firing twice (client render + server
     * tick) and correctly distances-attenuates the sound for all players.
     *
     * <h3>Adding new sounds</h3>
     * Add the marker name → SoundEvent mapping to {@link #resolveDragonSound}.
     * Then tag your animation bone keyframe with that exact string in the
     * {@code .animation.json} file — no code changes needed beyond that.
     */
    private AnimationController.SoundKeyframeHandler<EntityCustomDragon> buildSoundKeyframeHandler() {
        return event -> {
            EntityCustomDragon dragon = event.getAnimatable();
            // Server-only: Level.playSound(null, …) broadcasts to nearby clients.
            if (dragon.level().isClientSide) return;
            SoundEvent soundEvent = resolveDragonSound(event.getKeyframeData().getSound());
            if (soundEvent != null) {
                dragon.level().playSound(
                        null,
                        dragon.blockPosition(),
                        soundEvent,
                        dragon.getSoundSource(),
                        2.0F,
                        1.0F + (dragon.getRandom().nextFloat() - 0.5F) * 0.2F
                );
            }
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        // ── movement_controller ───────────────────────────────────────────────
        // Drives idle / walk / run / fly from physics state — already synced.
        controllers.add(new AnimationController<>(
                this,
                "movement_controller",
                5,
                animState -> {
                    EntityCustomDragon dragon = animState.getAnimatable();

                    if (dragon.isFlying() || dragon.isHovering()) {
                        return animState.setAndContinue(
                                RawAnimation.begin().thenLoop("fly"));
                    }

                    double speedSq = dragon.getDeltaMovement().horizontalDistanceSqr();

                    if (speedSq > 0.07) {
                        return animState.setAndContinue(
                                RawAnimation.begin().thenLoop("running"));
                    }

                    if (speedSq > 0.003) {
                        return animState.setAndContinue(
                                RawAnimation.begin().thenLoop("walking"));
                    }

                    return animState.setAndContinue(
                            RawAnimation.begin().thenLoop("idle"));
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));

        // ── head_controller (roar / epic_roar) ────────────────────────────────
        // Reads SYNCED_ANIM_ID instead of the server-only getCurrentAnimation().
        controllers.add(new AnimationController<>(
                this,
                "head_controller",
                2,
                animState -> {
                    byte id = animState.getAnimatable().getSyncedAnimId();
                    if (id == ANIM_ID_ROAR || id == ANIM_ID_EPIC_ROAR) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("roar", Animation.LoopType.PLAY_ONCE));
                    }
                    return PlayState.STOP;
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));

        // ── dive_controller ───────────────────────────────────────────────────
        controllers.add(new AnimationController<>(
                this,
                "dive_controller",
                10,
                animState -> {
                    EntityCustomDragon dragon = animState.getAnimatable();

                    if (dragon.diveBlend > 0.0f) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("dive", Animation.LoopType.HOLD_ON_LAST_FRAME));
                    }

                    return PlayState.STOP;
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));

        // ── bite_controller ───────────────────────────────────────────────────
        // Was missing entirely — without this ANIMATION_BITE never rendered.
        controllers.add(new AnimationController<>(
                this,
                "bite_controller",
                0,
                animState -> {
                    if (animState.getAnimatable().getSyncedAnimId() == ANIM_ID_BITE) {
                        // forceAnimationReset() clears GeckoLib's "already playing this
                        // animation" guard so PLAY_ONCE always restarts from frame 0
                        // even if the controller previously held the final frame.
                        // Without this, every bite after the first is silently ignored.
                        animState.getController().forceAnimationReset();
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("bite", Animation.LoopType.PLAY_ONCE));
                    }
                    return PlayState.STOP;
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));

        // ── wing_blast_controller ─────────────────────────────────────────────
        // Was missing entirely — without this ANIMATION_WINGBLAST never rendered.
        controllers.add(new AnimationController<>(
                this,
                "wing_blast_controller",
                2,
                animState -> {
                    if (animState.getAnimatable().getSyncedAnimId() == ANIM_ID_WINGBLAST) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("wing_blast", Animation.LoopType.PLAY_ONCE));
                    }
                    return PlayState.STOP;
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));

        // ── swing_prey_controller ─────────────────────────────────────────────
        controllers.add(new AnimationController<>(
                this,
                "swing_prey_controller",
                2,
                animState -> {
                    if (animState.getAnimatable().getSyncedAnimId() == ANIM_ID_SHAKEPREY) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("swing_prey", Animation.LoopType.PLAY_ONCE));
                    }
                    return PlayState.STOP;
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));

        // ── eat_controller ────────────────────────────────────────────────────
        // isEating() is already a synced boolean — keep using it directly.
        controllers.add(new AnimationController<>(
                this,
                "eat_controller",
                2,
                animState -> {
                    EntityCustomDragon dragon = animState.getAnimatable();
                    if (dragon.isEating()) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("eat", Animation.LoopType.PLAY_ONCE));
                    }
                    return PlayState.STOP;
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));

        // ── tail_swing_controller ─────────────────────────────────────────────
        controllers.add(new AnimationController<>(
                this,
                "tail_swing_controller",
                2,
                animState -> {
                    if (animState.getAnimatable().getSyncedAnimId() == ANIM_ID_TAILWHACK) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("tail_swing", Animation.LoopType.PLAY_ONCE));
                    }
                    return PlayState.STOP;
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));

        // ── fire_breath_controller ────────────────────────────────────────────
        //
        // Three-phase breath animation:
        //   1. fire_breath_start  (PLAY_ONCE) — jaw opens as breathing begins.
        //   2. fire_breath_continue (LOOP)    — sustained stream while key held.
        //   3. fire_breath_stop   (PLAY_ONCE) — jaw closes after key released.
        //
        // setAndContinue() will NOT restart the chain while the same animation
        // is already playing, so calling it every tick is safe and correct.
        //
        // For the one-shot fireball charge we still use fire_breath_start →
        // HOLD_ON_LAST_FRAME so the jaw stays open until the ball clears.
        controllers.add(new AnimationController<>(
                this,
                "fire_breath_controller",
                2,
                animState -> {
                    EntityCustomDragon dragon = animState.getAnimatable();

                    // ── Phase 1 & 2: sustained breath stream ─────────────────
                    // Gate on FIRE_MODE == 1 (synced data) rather than
                    // isBreathingFire() (IaF's boolean).  IaF's internal
                    // fireStopTicks can flip isBreathingFire() to false every
                    // ~10 ticks even while the key is held, which caused:
                    //   • fire_breath_stop playing spuriously mid-hold
                    //   • fire_breath_continue cutting out randomly
                    // FIRE_MODE is only changed by our packet handler and is
                    // immune to IaF's internal resets, so it is always reliable.
                    if (dragon.getFireMode() == 1) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("fire_breath_start",    Animation.LoopType.PLAY_ONCE)
                                        .then("fire_breath_continue", Animation.LoopType.LOOP));
                    }

                    // ── Phase 3: stop animation after breath ends ─────────────
                    // fireBreathStopTicks > 0 for FIRE_BREATH_STOP_DURATION ticks
                    // after FIRE_MODE drops from 1 to anything else.
                    if (dragon.fireBreathStopTicks > 0) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("fire_breath_stop", Animation.LoopType.PLAY_ONCE));
                    }

                    // ── One-shot fireball charge ──────────────────────────────
                    if (dragon.getSyncedAnimId() == ANIM_ID_FIRECHARGE) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("fire_breath_start", Animation.LoopType.HOLD_ON_LAST_FRAME));
                    }

                    return PlayState.STOP;
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));

        // ── talk_controller ───────────────────────────────────────────────────
        // OLD: dragon.ANIMATION_SPEAK.isStarted() — AnimationState.isStarted()
        // is driven on the server only; clients always return false.
        // FIX: use SYNCED_ANIM_ID == ANIM_ID_SPEAK.
        controllers.add(new AnimationController<>(
                this,
                "talk_controller",
                2,
                animState -> {
                    if (animState.getAnimatable().getSyncedAnimId() == ANIM_ID_SPEAK) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("talk", Animation.LoopType.PLAY_ONCE));
                    }
                    return PlayState.STOP;
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));

        // ── dead_controller ───────────────────────────────────────────────────
        // Plays "model_dead" once (5.3 s = 106 ticks) when isModelDead() is true,
        // then holds on the final frame so the corpse pose is frozen until the
        // entity is removed. Transition speed 0 so the death pose snaps in
        // immediately without blending from whatever the dragon was doing.
        controllers.add(new AnimationController<>(
                this,
                "dead_controller",
                0,
                animState -> {
                    if (animState.getAnimatable().isModelDead()) {
                        return animState.setAndContinue(
                                RawAnimation.begin()
                                        .then("model_dead", Animation.LoopType.HOLD_ON_LAST_FRAME));
                    }
                    return PlayState.STOP;
                }
        ).setSoundKeyframeHandler(buildSoundKeyframeHandler()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    static {
        SWIMMING = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        ARMOR_MODIFIER_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");
        HUNGER = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.INT);
        AGE_TICKS = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.INT);
        GENDER = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        VARIANT = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.INT);
        SLEEPING = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        FIREBREATHING = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        HOVERING = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        FLYING = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        MODEL_DEAD = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        DEATH_STAGE = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.INT);
        CONTROL_STATE = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BYTE);
        TACKLE = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        AGINGDISABLED = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        COMMAND = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.INT);
        DRAGON_PITCH = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.FLOAT);
        CRYSTAL_BOUND = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        CUSTOM_POSE = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.STRING);
        IS_EATING = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BOOLEAN);
        FIRE_MODE   = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BYTE);
        SYNCED_ANIM_ID = SynchedEntityData.defineId(EntityCustomDragon.class, EntityDataSerializers.BYTE);
        growth_stage_1 = new float[]{1.0F, 3.0F};
        growth_stage_2 = new float[]{3.0F, 7.0F};
        growth_stage_3 = new float[]{7.0F, 12.5F};
        growth_stage_4 = new float[]{12.5F, 20.0F};
        growth_stage_5 = new float[]{20.0F, 30.0F};
    }
}