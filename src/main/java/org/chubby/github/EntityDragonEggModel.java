package org.chubby.github;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntityDragonEggModel extends GeoModel<EntityDragonEgg>
{

    @Override
    public ResourceLocation getModelResource(EntityDragonEgg animatable) {
        return ResourceLocation.fromNamespaceAndPath("dragonmod","geo/entity/dragon_egg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntityDragonEgg animatable) {
        return ResourceLocation.fromNamespaceAndPath("dragonmod","textures/entity/dragon_egg.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EntityDragonEgg animatable) {
        return ResourceLocation.fromNamespaceAndPath("dragonmod","animations/entity/dragon_egg.animations.json");
    }
}