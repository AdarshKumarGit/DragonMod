package org.chubby.github;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EntityCustomDragonRenderer extends GeoEntityRenderer<EntityDragon> {

    public EntityCustomDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new EntityCustomDragonModel<>());
    }

    // -------------------------------------------------------------------------
    // FIX 1: Use getVisualScale() instead of getScale().
    //
    // For stage 1-2 babies the visual scale is boosted (~2.85-4.95) so the
    // tiny baby geo renders visibly at a size that matches a real baby dragon.
    // The collision box (getDimensions / getScale) stays physically small.
    // For stage 3-5 adults getVisualScale() == getScale() so behaviour is
    // identical to before.
    // -------------------------------------------------------------------------
    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
                                    EntityDragon animatable, BakedGeoModel model, boolean isReRender,
                                    float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model,
                isReRender, partialTick, packedLight, packedOverlay);
        float renderScale = animatable.getVisualScale(); // decoupled from hitbox scale for babies
        poseStack.scale(renderScale, renderScale, renderScale);
    }

    // -------------------------------------------------------------------------
    // Apply the dragon's flight/dive pitch to the whole model.
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