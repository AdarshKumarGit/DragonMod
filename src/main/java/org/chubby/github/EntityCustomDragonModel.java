package org.chubby.github;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the custom dragon.
 *
 * <p>Stage 1 and 2 (baby dragons) use a dedicated baby geo/texture/animation set.
 * Stages 3-5 use the full adult assets.
 *
 * <p>Expected resource layout inside your mod's resources:
 * <pre>
 *   assets/dragonmod/geo/entity/dragon.geo.json          ← adult model
 *   assets/dragonmod/geo/entity/dragon_baby.geo.json     ← baby  model
 *   assets/dragonmod/textures/entity/dragon_tex.png      ← adult texture
 *   assets/dragonmod/textures/entity/dragon_baby_tex.png ← baby  texture
 *   assets/dragonmod/animations/entity/dragon.animations.json      ← adult anims
 *   assets/dragonmod/animations/entity/dragon_baby.animations.json ← baby  anims
 * </pre>
 */
public class EntityCustomDragonModel<T extends EntityDragon> extends GeoModel<T> {

    // -----------------------------------------------------------------------
    // Model
    // -----------------------------------------------------------------------

    @Override
    public ResourceLocation getModelResource(T dragon) {
        if (dragon.getDragonStage() <= 2) {
            // Stages 1 & 2 → dedicated baby geo
            return ResourceLocation.fromNamespaceAndPath("dragonmod", "geo/entity/dragon_baby.geo.json");
        }
        return ResourceLocation.fromNamespaceAndPath("dragonmod", "geo/entity/dragon.geo.json");
    }

    // -----------------------------------------------------------------------
    // Texture
    // -----------------------------------------------------------------------

    @Override
    public ResourceLocation getTextureResource(T dragon) {
        if (dragon.getDragonStage() <= 2) {
            return ResourceLocation.fromNamespaceAndPath("dragonmod", "textures/entity/dragon_baby.png");
        }
        return ResourceLocation.fromNamespaceAndPath("dragonmod", "textures/entity/dragon_tex.png");
    }

    // -----------------------------------------------------------------------
    // Animation file
    // -----------------------------------------------------------------------

    @Override
    public ResourceLocation getAnimationResource(T dragon) {
        if (dragon.getDragonStage() <= 2) {
            // The baby animation file should contain its own idle/walk/run loops.
            // If the baby shares the same animations, point both entries at the same file.
            return ResourceLocation.fromNamespaceAndPath("dragonmod", "animations/entity/dragon_baby.animations.json");
        }
        return ResourceLocation.fromNamespaceAndPath("dragonmod", "animations/entity/dragon.animations.json");
    }
}