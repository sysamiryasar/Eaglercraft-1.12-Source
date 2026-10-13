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
    private static int savedShade = 7424;

    private CosmeticShapes()
    {
    }

    /** Sets up flat-colour drawing; call end() afterwards. */
    public static void begin(boolean glow)
    {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        // colours fade across each shape (otherwise a shape takes one corner's colour)
        savedShade = GlStateManager.getShadeModel();
        GlStateManager.shadeModel(7425);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        if (glow)
        {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        }
    }

    /**
     * Sets up glowing, see-through drawing (light adds up, never darkens; nothing writes depth, so
     * energy shapes don't hide each other). Call end() afterwards.
     */
    public static void beginAdditive()
    {
        begin(true);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }

    /** See-through colour that still shows in daylight (normal blending, full brightness). Call end(). */
    public static void beginTranslucent()
    {
        begin(true);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
    }

    public static void end()
    {
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(savedShade);
        GlStateManager.enableCull();
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

    /** A diamond (two pyramids) centred on (x, y, z): rx wide, ry tall. Good for crystals and gems. */
    public static void diamond(float x, float y, float z, float rx, float ry, int color)
    {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder buf = t.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
        float[][] around = {{rx, 0.0F}, {0.0F, rx}, {-rx, 0.0F}, {0.0F, -rx}};

        for (int i = 0; i < 4; ++i)
        {
            float[] a = around[i];
            float[] b = around[(i + 1) % 4];
            float shade = i % 2 == 0 ? 1.0F : 0.75F;
            // upper and lower faces as quads with a repeated tip
            quad(buf, color, shade, new double[] {x, y - ry, z, x, y - ry, z, x + b[0], y, z + b[1], x + a[0], y, z + a[1]});
            quad(buf, color, shade * 0.7F, new double[] {x + a[0], y, z + a[1], x + b[0], y, z + b[1], x, y + ry, z, x, y + ry, z});
        }

        t.draw();
    }

    /** A flat four-cornered shape with its own colour at each corner (corners in order around it). */
    public static void quad(float[] a, float[] b, float[] c, float[] d, int ca, int cb, int cc, int cd)
    {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder buf = t.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
        vertex(buf, a, ca);
        vertex(buf, b, cb);
        vertex(buf, c, cc);
        vertex(buf, d, cd);
        t.draw();
    }

    private static void vertex(BufferBuilder buf, float[] p, int c)
    {
        buf.pos((double)p[0], (double)p[1], (double)p[2]).color(r(c, 1.0F), g(c, 1.0F), b(c, 1.0F), a(c)).endVertex();
    }

    /** A thin glowing line from one point to another (two crossed strips, so it shows from all sides). */
    public static void line(float x1, float y1, float z1, float x2, float y2, float z2, float width, int color)
    {
        float w = width / 2.0F;
        quad(new float[] {x1 - w, y1, z1}, new float[] {x1 + w, y1, z1}, new float[] {x2 + w, y2, z2}, new float[] {x2 - w, y2, z2}, color, color, color, color);
        quad(new float[] {x1, y1, z1 - w}, new float[] {x1, y1, z1 + w}, new float[] {x2, y2, z2 + w}, new float[] {x2, y2, z2 - w}, color, color, color, color);
        quad(new float[] {x1, y1 - w, z1}, new float[] {x1, y1 + w, z1}, new float[] {x2, y2 + w, z2}, new float[] {x2, y2 - w, z2}, color, color, color, color);
    }

    /** Colour with a new alpha (0-255). */
    public static int alpha(int color, int alpha)
    {
        return (color & 0xFFFFFF) | (Math.max(1, Math.min(255, alpha)) << 24);
    }

    /** Blends two colours (t = 0 gives a, 1 gives b), keeping a's alpha. */
    public static int mix(int a, int b, float t)
    {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int r = (int)(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int)(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int)((a & 255) + ((b & 255) - (a & 255)) * t);
        return (a & 0xFF000000) | r << 16 | g << 8 | bl;
    }

    /** A colour around the colour wheel (0-1), fully opaque. */
    public static int hue(float h)
    {
        return 0xFF000000 | java.awt.Color.HSBtoRGB(h - (float)Math.floor(h), 0.85F, 1.0F);
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
