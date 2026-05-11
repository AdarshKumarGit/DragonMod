package org.chubby.github;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tilts the rider's player model so it pitches with the dragon when mounted.
 *
 * The dragon renderer applies {@code Axis.XP.rotationDegrees(-dragonPitch)}
 * to its own model in {@code EntityCustomDragonRenderer.applyRotations}.  The
 * vanilla player renderer doesn't know about that rotation, so the rider
 * stays bolt-upright while the dragon's back tilts away under them.  This
 * handler pushes the matching X-axis rotation onto the PoseStack before the
 * player is drawn (and pops it after) so the seated player follows the
 * dragon's climb/dive angle.
 */
@Mod.EventBusSubscriber(modid = DragonMod.MODID, value = Dist.CLIENT)
public final class DragonRiderRenderHandler {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!(player.getVehicle() instanceof EntityCustomDragon dragon)) return;

        float pitch = Mth.lerp(event.getPartialTick(),
                               dragon.prevDragonPitch,
                               dragon.getDragonPitch());

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // Rotate the player about their feet (local origin) so they lean with
        // the dragon's back.  The dragon model uses the matching transform
        // Axis.XP.rotationDegrees(-pitch) in EntityCustomDragonRenderer, so
        // the rider stays visually parallel to the spine.
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.getVehicle() instanceof EntityCustomDragon)) return;
        event.getPoseStack().popPose();
    }
}
