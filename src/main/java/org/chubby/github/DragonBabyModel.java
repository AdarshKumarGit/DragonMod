package org.chubby.github;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DragonBabyModel<T extends EntityDragon> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T t) {
        return ResourceLocation.fromNamespaceAndPath("dragonmod","geo/entity/dragon_baby.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T t) {
        return ResourceLocation.fromNamespaceAndPath("dragonmod","textures/entity/dragon_baby.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T t) {
        return ResourceLocation.fromNamespaceAndPath("dragonmod","animations/entity/dragon_baby.animations.json");
    }
}