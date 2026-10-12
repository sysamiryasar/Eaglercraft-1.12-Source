package net.sythix.client.cosmetic;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;

/**
 * Untextured shapes for cosmetics, in model space (1 unit = 1 block, +Y down, front = -Z).
 * Faces are shaded by direction so the shapes read as 3D without lighting.
 */
public final class CosmeticShapes
{
    private CosmeticShapes()
    {
    }

    /** Sets up flat-colour drawing; call end() afterwards. */
    public static void begin(boolean glow)
    {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        if (glow)
        {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        }
    }

    public static void end()
    {
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static float r(int c, float shade)
    {
        return (float)((c >> 16) & 255) / 255.0F * shade;
    }

    private static float g(int c, float shade)
    {
        return (float)((c >> 8) & 255) / 255.0F * shade;
    }

    private static float b(int c, float shade)
    {
        return (float)(c & 255) / 255.0F * shade;
    }

    private static float a(int c)
    {
        int alpha = (c >>> 24) & 255;
        return alpha == 0 ? 1.0F : (float)alpha / 255.0F;
    }

    private static void quad(BufferBuilder buf, int c, float shade, double[] p)
    {
        for (int i = 0; i < 12; i += 3)
        {
            buf.pos(p[i], p[i + 1], p[i + 2]).color(r(c, shade), g(c, shade), b(c, shade), a(c)).endVertex();
        }
    }

    /** An axis-aligned box. */
    public static void box(float x1, float y1, float z1, float x2, float y2, float z2, int color)
    {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder buf = t.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
        // top (-Y is up)
        quad(buf, color, 1.0F, new double[] {x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1});
        quad(buf, color, 0.5F, new double[] {x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2});
        quad(buf, color, 0.8F, new double[] {x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1});
        quad(buf, color, 0.8F, new double[] {x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2});
        quad(buf, color, 0.65F, new double[] {x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2});
        quad(buf, color, 0.65F, new double[] {x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1});
        t.draw();
    }

    /** A box centred on (x, z) with its top at y. */
    public static void column(float x, float z, float halfWidth, float top, float bottom, int color)
    {
        box(x - halfWidth, top, z - halfWidth, x + halfWidth, bottom, z + halfWidth, color);
    }

    /** A flat ring (halo) lying horizontally at height y. */
    public static void ring(float y, float radius, float width, int color)
    {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder buf = t.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
        int segments = 32;

        for (int i = 0; i < segments; ++i)
        {
            float a0 = (float)i / (float)segments * (float)Math.PI * 2.0F;
            float a1 = (float)(i + 1) / (float)segments * (float)Math.PI * 2.0F;
            float ri = radius - width;
            double[] p = new double[] {
                MathHelper.cos(a0) * ri, y, MathHelper.sin(a0) * ri,
                MathHelper.cos(a0) * radius, y, MathHelper.sin(a0) * radius,
                MathHelper.cos(a1) * radius, y, MathHelper.sin(a1) * radius,
                MathHelper.cos(a1) * ri, y, MathHelper.sin(a1) * ri};
            quad(buf, color, 1.0F, p);
            // Back side so it is visible from below too
            quad(buf, color, 0.8F, new double[] {p[9], p[10], p[11], p[6], p[7], p[8], p[3], p[4], p[5], p[0], p[1], p[2]});
        }

        t.draw();
    }
}
