package org.chubby.github;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.world.entity.AnimationState;

public interface IAnimatedEntity {
    AnimationState NO_ANIMATION = new AnimationState();

    int getAnimationTick();

    void setAnimationTick(int var1);

    AnimationState getAnimation();

    void setAnimation(Animation var1);

    AnimationState[] getAnimations();
}
