package org.chubby.github;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DragonBabyRenderer extends GeoEntityRenderer<EntityDragon> {
    public DragonBabyRenderer(EntityRendererProvider.Context context, EntityType<? extends EntityDragon> entityType) {
        super(context, entityType);
    }
}
