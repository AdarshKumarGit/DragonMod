package org.chubby.github;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Item that places a {@link EntityDragonEgg} entity on right-click.
 *
 * <p>The egg always spawns on TOP of the block that was clicked, regardless
 * of which face was clicked. This prevents the egg from appearing to float
 * beside a wall or being placed one block above where expected.
 */
public class DragonEggItem extends Item {

    public DragonEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext ctx) {
        Level level = ctx.getLevel();

        // Let the server do the real work; just play the arm swing on the client
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // FIX (Bug 1): Always spawn on TOP of the clicked block.
        // The old code did ctx.getClickedPos().relative(ctx.getClickedFace()),
        // which placed the egg one block in the direction of the face hit –
        // when clicking a side face the egg floated in mid-air at the wrong Y.
        // Now we always resolve "one block above the block you clicked on."
        BlockPos clickedBlock = ctx.getClickedPos();
        BlockPos eggSpace     = clickedBlock.above();   // block-space the egg will occupy

        BlockState existing = level.getBlockState(eggSpace);

        // Refuse if a solid block is already occupying that space
        if (!existing.isAir() && !existing.canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        // Create the egg entity
        EntityDragonEgg egg = DragonMod.DRAGON_EGG.get().create(level);
        if (egg == null) {
            return InteractionResult.FAIL;
        }

        // Sit the egg exactly on the top surface of the clicked block
        egg.setPos(
                clickedBlock.getX() + 0.5,
                clickedBlock.getY() + 1.0,   // top face of the clicked block
                clickedBlock.getZ() + 0.5);

        level.addFreshEntity(egg);

        if (ctx.getPlayer() != null) {
            egg.setOwnerUUID(ctx.getPlayer().getUUID());
        }

        // Consume the item unless in creative
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) {
            ctx.getItemInHand().shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}