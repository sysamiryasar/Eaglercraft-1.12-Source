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

    public static final String[] STYLES = new String[] {"Feathered", "Angel", "Butterfly", "Bat"};

    public static void draw(float ageInTicks, int color, float flapSpeed, float size, boolean sneaking)
    {
        draw("Feathered", ageInTicks, color, flapSpeed, size, sneaking);
    }

    public static void draw(String style, float ageInTicks, int color, float flapSpeed, float size, boolean sneaking)
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

            shape(buffer, style, (float)side, r, g, b, a);
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

    private static void v(BufferBuilder buffer, float x, float y, float r, float g, float b, float a)
    {
        buffer.pos((double)x, (double)y, 0.0D).color(r, g, b, a).endVertex();
    }

    /** One wing as quads, x mirrored by s (-1 left, 1 right). */
    private static void shape(BufferBuilder buffer, String style, float s, float r, float g, float b, float a)
    {
        float dr = r * 0.7F;
        float dg = g * 0.7F;
        float db = b * 0.7F;
        float da = a * 0.6F;

        switch (style)
        {
            case "Angel":
                // Five long layered feathers
                for (int f = 0; f < 5; ++f)
                {
                    float top = -0.25F + (float)f * 0.15F;
                    float length = 1.1F - (float)f * 0.17F;
                    v(buffer, s * 0.02F, top, r, g, b, a);
                    v(buffer, s * length, top - 0.32F + (float)f * 0.04F, dr, dg, db, da);
                    v(buffer, s * length * 0.92F, top - 0.12F + (float)f * 0.04F, dr, dg, db, da);
                    v(buffer, s * 0.02F, top + 0.2F, r, g, b, a);
                }

                break;
            case "Butterfly":
                // Upper and lower lobes
                v(buffer, s * 0.02F, -0.05F, r, g, b, a);
                v(buffer, s * 0.55F, -0.55F, dr, dg, db, da);
                v(buffer, s * 0.8F, -0.25F, dr, dg, db, da);
                v(buffer, s * 0.05F, 0.12F, r, g, b, a);
                v(buffer, s * 0.05F, 0.12F, r, g, b, a);
                v(buffer, s * 0.55F, 0.1F, dr, dg, db, da);
                v(buffer, s * 0.45F, 0.5F, dr, dg, db, da);
                v(buffer, s * 0.03F, 0.3F, r, g, b, a);
                break;
            case "Bat":
                // Membrane between three finger bones
                float[][] tips = new float[][] {{0.9F, -0.35F}, {0.95F, 0.05F}, {0.7F, 0.4F}, {0.35F, 0.55F}};
                float[][] dips = new float[][] {{0.75F, -0.1F}, {0.7F, 0.22F}, {0.42F, 0.38F}};

                for (int i = 0; i < 3; ++i)
                {
                    v(buffer, s * 0.02F, 0.0F, r, g, b, a);
                    v(buffer, s * tips[i][0], tips[i][1], dr, dg, db, a);
                    v(buffer, s * dips[i][0], dips[i][1], dr * 0.8F, dg * 0.8F, db * 0.8F, da);
                    v(buffer, s * tips[i + 1][0], tips[i + 1][1], dr, dg, db, a);
                }

                break;
            default:
                // Feathered: three feathers per wing, longest at the top
                for (int f = 0; f < 3; ++f)
                {
                    float top = -0.1F + (float)f * 0.18F;
                    float length = 0.9F - (float)f * 0.18F;
                    v(buffer, s * 0.02F, top, r, g, b, a);
                    v(buffer, s * length, top - 0.25F, dr, dg, db, da);
                    v(buffer, s * length * 0.95F, top - 0.05F, dr, dg, db, da);
                    v(buffer, s * 0.02F, top + 0.18F, r, g, b, a);
                }
        }
    }
}
