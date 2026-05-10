package org.chubby.github;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EntityCustomDragonRenderer extends GeoEntityRenderer<EntityDragon> {

    public EntityCustomDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new EntityCustomDragonModel<>());
    }

    // -------------------------------------------------------------------------
    // Per-frame hitbox sync.
    //
    // The method EntityCustomDragon.updatePartsForRender(partialTick) was
    // specifically designed to be called here (see the Javadoc on that method).
    // What it does:
    //   1. Saves the real yBodyRot.
    //   2. Substitutes the per-frame INTERPOLATED yBodyRot (lerp of yBodyRotO→yBodyRot).
    //   3. Calls computePartPositions() so every hitbox part world-XYZ matches the
    //      drawn model position for this exact render fraction.
    //   4. Restores the real yBodyRot so physics/AI are unaffected.
    //
    // IMPORTANT — we do NOT save/restore xo/yo/zo here.  The existing
    // savePartOldPositions() call inside updateParts() (which runs at 20 TPS on
    // the game tick) is the only place that writes xo/yo/zo.  The render-time
    // call only writes x/y/z.  This means:
    //   • Interpolation origins (xo/yo/zo) are always the correctly snapshotted
    //     tick-start values.
    //   • The current positions (x/y/z) are the per-frame interpolated values.
    //   • Minecraft's Entity.render interpolation (xo→x, etc.) therefore produces
    //     a smooth and correctly positioned hitbox every frame.
    //
    // Without this call, hitbox boxes lag the model by up to one tick at 20 TPS,
    // and the player sits at a position computed from a stale yBodyRot, causing
    // the "rider floating / not moving with dragon" issue.
    // -------------------------------------------------------------------------
    @Override
    public void render(EntityDragon animatable,
                       float entityYaw,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource bufferSource,
                       int packedLight) {
        animatable.updatePartsForRender(partialTick);
        super.render(animatable, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    // -------------------------------------------------------------------------
    // Visual scale — decouple from physics scale for baby dragons.
    // -------------------------------------------------------------------------
    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
                                    EntityDragon animatable, BakedGeoModel model, boolean isReRender,
                                    float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model,
                isReRender, partialTick, packedLight, packedOverlay);
        poseStack.scale(animatable.getVisualScale(),
                animatable.getVisualScale(),
                animatable.getVisualScale());
    }

    // -------------------------------------------------------------------------
    // Tilt the whole model with the dragon's flight/dive pitch.
    // -------------------------------------------------------------------------
    @Override
    protected void applyRotations(EntityDragon animatable,
                                  PoseStack poseStack,
                                  float ageInTicks,
                                  float rotationYaw,
                                  float partialTick) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);
        float pitch = Mth.lerp(partialTick, animatable.prevDragonPitch, animatable.getDragonPitch());
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    }
}