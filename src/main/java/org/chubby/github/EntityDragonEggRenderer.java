package org.chubby.github;


import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EntityDragonEggRenderer extends GeoEntityRenderer<EntityDragonEgg>
{

    public EntityDragonEggRenderer(EntityRendererProvider.Context context) {
        super(context, new EntityDragonEggModel());
    }
}