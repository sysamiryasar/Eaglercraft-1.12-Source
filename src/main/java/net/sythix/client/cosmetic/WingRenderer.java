package net.sythix.client.cosmetic;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;

/**
 * Draws a pair of simple feathered wings in model space (the player's back).
 */
public final class WingRenderer
{
    private WingRenderer()
    {
    }

    public static void draw(float ageInTicks, int color, float flapSpeed, float size, boolean sneaking)
    {
        float r = (float)((color >> 16) & 255) / 255.0F;
        float g = (float)((color >> 8) & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        float a = (float)((color >>> 24) & 255) / 255.0F;
        float flap = MathHelper.sin(ageInTicks * 0.15F * flapSpeed) * 15.0F;
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.translate(0.0F, sneaking ? 0.35F : 0.2F, 0.16F);

        if (sneaking)
        {
            GlStateManager.rotate(28.0F, 1.0F, 0.0F, 0.0F);
        }

        GlStateManager.scale(size, size, size);

        for (int side = -1; side <= 1; side += 2)
        {
            GlStateManager.pushMatrix();
            GlStateManager.rotate((float)side * (25.0F + flap), 0.0F, 1.0F, 0.0F);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);

            // Three feathers per wing, longest at the top
            for (int f = 0; f < 3; ++f)
            {
                float top = -0.1F + (float)f * 0.18F;
                float length = 0.9F - (float)f * 0.18F;
                float s = (float)side;
                buffer.pos((double)(s * 0.02F), (double)top, 0.0D).color(r, g, b, a).endVertex();
                buffer.pos((double)(s * length), (double)(top - 0.25F), 0.0D).color(r * 0.7F, g * 0.7F, b * 0.7F, a * 0.6F).endVertex();
                buffer.pos((double)(s * length * 0.95F), (double)(top - 0.05F), 0.0D).color(r * 0.7F, g * 0.7F, b * 0.7F, a * 0.6F).endVertex();
                buffer.pos((double)(s * 0.02F), (double)(top + 0.18F), 0.0D).color(r, g, b, a).endVertex();
            }

            tessellator.draw();
            GlStateManager.popMatrix();
        }

        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
