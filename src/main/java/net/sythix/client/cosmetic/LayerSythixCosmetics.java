package net.sythix.client.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.sythix.client.core.Hooks;
import net.sythix.client.module.visuals.CosmeticsModule;

/**
 * Render layer that adds Sythix wings, hats and shoulder buddies to the local player.
 */
public class LayerSythixCosmetics implements LayerRenderer<AbstractClientPlayer>
{
    private final net.minecraft.client.renderer.entity.RenderPlayer renderer;

    public LayerSythixCosmetics(net.minecraft.client.renderer.entity.RenderPlayer renderer)
    {
        this.renderer = renderer;
    }

    /** Draws wings, hat and buddy on a model (also used by the profile preview). */
    public static void drawAll(net.minecraft.client.model.ModelPlayer model, CosmeticsModule cosmetics, float ageInTicks, boolean sneaking)
    {
        if (cosmetics.showWings())
        {
            WingRenderer.draw(cosmetics.wingStyle(), ageInTicks, cosmetics.wingColor(), cosmetics.wingFlap(), cosmetics.wingSize(), sneaking);
        }

        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        BuddyRenderer.draw(cosmetics.buddy(), cosmetics.buddyColor(), ageInTicks, sneaking);
        net.minecraft.client.renderer.GlStateManager.popMatrix();

        if (!"None".equals(cosmetics.hat()))
        {
            net.minecraft.client.renderer.GlStateManager.pushMatrix();

            if (sneaking)
            {
                net.minecraft.client.renderer.GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }

            model.bipedHead.postRender(0.0625F);
            HatRenderer.draw(cosmetics.hat(), cosmetics.hatColor(), ageInTicks);
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }
    }
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        if (player != Minecraft.getMinecraft().player || player.isInvisible())
        {
            return;
        }

        CosmeticsModule cosmetics = Hooks.on(CosmeticsModule.class);

        if (cosmetics != null)
        {
            drawAll(this.renderer.getMainModel(), cosmetics, ageInTicks, player.isSneaking());
        }
    }

    public boolean shouldCombineTextures()
    {
        return false;
    }
}
