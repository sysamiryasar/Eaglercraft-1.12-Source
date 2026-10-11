package net.sythix.client.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.sythix.client.core.Hooks;
import net.sythix.client.module.visuals.CosmeticsModule;

/**
 * Render layer that adds Sythix wings to the local player.
 */
public class LayerSythixCosmetics implements LayerRenderer<AbstractClientPlayer>
{
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        if (player != Minecraft.getMinecraft().player || player.isInvisible())
        {
            return;
        }

        CosmeticsModule cosmetics = Hooks.on(CosmeticsModule.class);

        if (cosmetics != null && cosmetics.showWings())
        {
            WingRenderer.draw(ageInTicks, cosmetics.wingColor(), cosmetics.wingFlap(), cosmetics.wingSize(), player.isSneaking());
        }
    }

    public boolean shouldCombineTextures()
    {
        return false;
    }
}
