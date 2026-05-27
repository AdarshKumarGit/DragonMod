package org.chubby.github.mixin;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.client.StatCollector;
import com.github.alexthe666.iceandfire.client.gui.GuiDragon;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.chubby.github.EntityCustomDragon;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiDragon.class)
public abstract class GuiDragonMixin extends AbstractContainerScreen
{
    // Dummy constructor — required because we extend AbstractContainerScreen
    protected GuiDragonMixin(Object menu, Object playerInventory, Object title) {
        super(null, null, null);
    }

    // Redeclare the texture locally so we don't need to @Shadow the private static field
    @Unique
    private static final ResourceLocation IAF_DRAGON_TEXTURE =
            ResourceLocation.parse("iceandfire:textures/gui/dragon.png");

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics matrixStack, float partialTicks, int mouseX, int mouseY, CallbackInfo ci)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // width/height/imageWidth/imageHeight all come from AbstractContainerScreen — no @Shadow needed
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;

        matrixStack.blit(IAF_DRAGON_TEXTURE, k, l, 0, 0, this.imageWidth, this.imageHeight);

        Entity entity = IceAndFire.PROXY.getReferencedMob();

        if (entity instanceof EntityDragonBase dragon) {
            float dragonScale = 1.0F / Math.max(1.0E-4F, dragon.getScale());
            Quaternionf quaternionf = (new Quaternionf())
                    .rotateY((float) Mth.lerp((double)((float) mouseX / (float) this.width), (double) 0.0F, Math.PI))
                    .rotateZ((float) Mth.lerp((double)((float) mouseY / (float) this.width), Math.PI, 3.3415926535897933));
            float scaleMultiplier = dragon instanceof EntityCustomDragon ? 8.0F : 23.0F;
            InventoryScreen.renderEntityInInventory(matrixStack, k + 88, l + (int)(0.5F * dragon.flyProgress) + 55,
                    (int)(dragonScale * scaleMultiplier), quaternionf, (Quaternionf) null, dragon);
        }

        if (entity instanceof EntityDragonBase dragon) {
            Font font = Minecraft.getInstance().font; // use getInstance() — no @Shadow needed
            String s3 = dragon.getCustomName() == null
                    ? StatCollector.translateToLocal("dragon.unnamed")
                    : StatCollector.translateToLocal("dragon.name") + " " + dragon.getCustomName().getString();
            font.drawInBatch(s3, (float)(k + this.imageWidth / 2 - font.width(s3) / 2), (float)(l + 75),
                    16777215, false, matrixStack.pose().last().pose(), matrixStack.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);

            String var10000 = StatCollector.translateToLocal("dragon.health");
            String s2 = var10000 + " " + Math.floor((double) Math.min(dragon.getHealth(), dragon.getMaxHealth())) + " / " + dragon.getMaxHealth();
            font.drawInBatch(s2, (float)(k + this.imageWidth / 2 - font.width(s2) / 2), (float)(l + 84),
                    16777215, false, matrixStack.pose().last().pose(), matrixStack.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);

            var10000 = StatCollector.translateToLocal("dragon.gender");
            String s5 = var10000 + StatCollector.translateToLocal(dragon.isMale() ? "dragon.gender.male" : "dragon.gender.female");
            font.drawInBatch(s5, (float)(k + this.imageWidth / 2 - font.width(s5) / 2), (float)(l + 93),
                    16777215, false, matrixStack.pose().last().pose(), matrixStack.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);

            var10000 = StatCollector.translateToLocal("dragon.hunger");
            String s6 = var10000 + dragon.getHunger() + "/100";
            font.drawInBatch(s6, (float)(k + this.imageWidth / 2 - font.width(s6) / 2), (float)(l + 102),
                    16777215, false, matrixStack.pose().last().pose(), matrixStack.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);

            var10000 = StatCollector.translateToLocal("dragon.stage");
            String s4 = var10000 + " " + dragon.getDragonStage() + " " + StatCollector.translateToLocal("dragon.days.front")
                    + dragon.getAgeInDays() + " " + StatCollector.translateToLocal("dragon.days.back");
            font.drawInBatch(s4, (float)(k + this.imageWidth / 2 - font.width(s4) / 2), (float)(l + 111),
                    16777215, false, matrixStack.pose().last().pose(), matrixStack.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);

            String s7 = dragon.getOwner() != null
                    ? StatCollector.translateToLocal("dragon.owner") + dragon.getOwner().getName().getString()
                    : StatCollector.translateToLocal("dragon.untamed");
            font.drawInBatch(s7, (float)(k + this.imageWidth / 2 - font.width(s7) / 2), (float)(l + 120),
                    16777215, false, matrixStack.pose().last().pose(), matrixStack.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        }

        ci.cancel();
    }
}