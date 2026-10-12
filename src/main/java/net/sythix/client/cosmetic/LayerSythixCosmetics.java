package net.sythix.client.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.sythix.client.core.Hooks;
import net.sythix.client.module.visuals.CosmeticsModule;

/**
 * Render layer that adds the Sythix headwear, wings, aura, Rift Companion and Afterimage to the
 * local player (capes go through the normal cape layer).
 */
public class LayerSythixCosmetics implements LayerRenderer<AbstractClientPlayer>
{
    private final net.minecraft.client.renderer.entity.RenderPlayer renderer;

    public LayerSythixCosmetics(net.minecraft.client.renderer.entity.RenderPlayer renderer)
    {
        this.renderer = renderer;
    }

    /** Draws a look on a model in model space (also used by the previews). */
    public static void drawAll(ModelPlayer model, PlayerPreview.Look look, float age, boolean sneaking)
    {
        WingRenderer.draw(look.wings, age, look.wingFlap, look.wingSize, sneaking);
        AuraRenderer.draw(look.aura, age, false);

        if (look.companion)
        {
            GlStateManager.pushMatrix();
            ExtrasRenderer.companion(age);
            GlStateManager.popMatrix();
        }

        if (!CosmeticCatalog.NONE.equals(look.headwear))
        {
            GlStateManager.pushMatrix();

            if (sneaking)
            {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }

            model.bipedHead.postRender(0.0625F);
            HeadwearRenderer.draw(look.headwear, age);
            GlStateManager.popMatrix();
        }
    }

    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        if (player != Minecraft.getMinecraft().player || player.isInvisible())
        {
            return;
        }

        CosmeticsModule cosmetics = Hooks.on(CosmeticsModule.class);

        if (cosmetics == null)
        {
            return;
        }

        drawAll(this.renderer.getMainModel(), PlayerPreview.Look.current(cosmetics), ageInTicks, player.isSneaking());

        if (cosmetics.afterimage())
        {
            double x = player.prevPosX + (player.posX - player.prevPosX) * (double)partialTicks;
            double y = player.prevPosY + (player.posY - player.prevPosY) * (double)partialTicks;
            double z = player.prevPosZ + (player.posZ - player.prevPosZ) * (double)partialTicks;
            float yaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
            ExtrasRenderer.afterimage(this.renderer.getMainModel(), x, y, z, yaw);
        }
    }

    public boolean shouldCombineTextures()
    {
        return false;
    }
}
