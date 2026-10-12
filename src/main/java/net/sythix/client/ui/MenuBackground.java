package net.sythix.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

/**
 * The blurred backdrop of Sythix's menus. In the main menus it is the slowly turning Minecraft
 * panorama (or the Sythix artwork); behind in-game menus it is the game world itself. The picture is
 * drawn into a tiny framebuffer and scaled back up twice, which gives a soft, heavy blur cheaply.
 */
public final class MenuBackground
{
    private static final ResourceLocation[] PANORAMA = new ResourceLocation[6];
    private static final Framebuffer[] LEVELS = new Framebuffer[7];
    private static Framebuffer small;
    private static long start = System.currentTimeMillis();

    /** Panorama (default), Artwork or Plain. Set by the Main Menu module. */
    public static String source = "Panorama";
    /** 0 = sharp, 1 = very blurry. */
    public static float blur = 0.85F;

    static
    {
        for (int i = 0; i < 6; ++i)
        {
            PANORAMA[i] = new ResourceLocation("textures/gui/title/background/panorama_" + i + ".png");
        }
    }

    private MenuBackground()
    {
    }

    private static Framebuffer fit(Framebuffer fb, int w, int h)
    {
        w = Math.max(2, w);
        h = Math.max(2, h);

        if (fb == null)
        {
            fb = new Framebuffer(w, h, false);
            fb.setFramebufferFilter(GL11.GL_LINEAR);
        }
        else if (fb.framebufferWidth != w || fb.framebufferHeight != h)
        {
            fb.createBindFramebuffer(w, h);
            fb.setFramebufferFilter(GL11.GL_LINEAR);
        }

        return fb;
    }

    private static long lastPrepared;
    private static boolean lastWorld;

    /** Draws the menu background over the whole screen (GUI coordinates width x height). */
    public static void draw(int width, int height, float dim)
    {
        drawRegion(width, height, 0, 0, width, height, dim);
    }

    /** Draws the part of the background inside left..right x top..bottom, lined up with draw(). */
    public static void drawRegion(int width, int height, int left, int top, int right, int bottom, float dim)
    {
        if (right <= left || bottom <= top)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        boolean world = mc.world != null;

        if (!world && "Plain".equals(source))
        {
            SythixTheme.drawQuad(left, top, right, bottom, shade(0xFF17121C, 0xFF07060A, (float)top / height), shade(0xFF17121C, 0xFF07060A, (float)top / height),
                shade(0xFF17121C, 0xFF07060A, (float)bottom / height), shade(0xFF17121C, 0xFF07060A, (float)bottom / height));
        }
        else
        {
            prepare(mc, world);
            GlStateManager.enableTexture2D();
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.bindTexture(small.framebufferTexture);
            float u = (float)small.framebufferWidth / (float)small.framebufferTextureWidth;
            float v = (float)small.framebufferHeight / (float)small.framebufferTextureHeight;
            quad(left, top, right, bottom, u * left / width, v * (1.0F - (float)top / height), u * right / width, v * (1.0F - (float)bottom / height));
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
        }

        // tint: dark glass with a warm glow rising from the bottom (Sythix red)
        SythixTheme.drawRect(left, top, right, bottom, SythixTheme.withAlpha(0xFF09080C, (int)(255 * dim)));
        int glow = SythixTheme.withAlpha(SythixTheme.RED, world ? 0x16 : 0x2C);
        float g0 = Math.max(0.0F, ((float)top / height - 0.5F) / 0.5F);
        float g1 = Math.max(0.0F, ((float)bottom / height - 0.5F) / 0.5F);

        if (g1 > 0.0F)
        {
            int c0 = SythixTheme.withAlpha(glow, (int)((glow >>> 24) * g0));
            int c1 = SythixTheme.withAlpha(glow, (int)((glow >>> 24) * g1));
            SythixTheme.drawGradientV(left, top, right, bottom, c0, c1);
        }
    }

    private static int shade(int a, int b, float t)
    {
        return SythixTheme.lerpColor(a, b, Math.max(0.0F, Math.min(1.0F, t)));
    }

    /** Renders and blurs the background once per frame (several screen parts may draw it). */
    private static void prepare(Minecraft mc, boolean world)
    {
        long now = System.nanoTime();

        if (small != null && world == lastWorld && now - lastPrepared < 2000000L)
        {
            return;
        }

        lastPrepared = now;
        lastWorld = world;
        int levels = 2 + Math.round(blur * 4.0F);

        for (int i = 0; i <= levels; ++i)
        {
            LEVELS[i] = fit(LEVELS[i], mc.displayWidth >> (i + 1), mc.displayHeight >> (i + 1));
        }

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();

        // 1. the source picture at half size
        Framebuffer first = LEVELS[0];
        first.bindFramebuffer(true);
        GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);

        if (world)
        {
            sample(mc.getFramebuffer(), first, false);
        }
        else if ("Artwork".equals(source))
        {
            ortho(1, 1);
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(SythixTheme.BACKGROUND);
            quad(0, 0, 1, 1, 0.0F, 0.0F, 1.0F, 1.0F);
        }
        else
        {
            panorama(mc, (float)first.framebufferWidth / (float)first.framebufferHeight);
        }

        // 2. halve it again and again, averaging four offset copies each time (a "dual filter" blur)
        for (int i = 1; i <= levels; ++i)
        {
            sample(LEVELS[i - 1], LEVELS[i], true);
        }

        // 3. and scale it back up step by step, which keeps the result smooth
        for (int i = levels - 1; i >= 1; --i)
        {
            sample(LEVELS[i + 1], LEVELS[i], true);
        }

        small = LEVELS[1];
        mc.getFramebuffer().bindFramebuffer(true);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
    }

    /** Draws src over the whole of target; with spread, as four copies shifted by half a texel. */
    private static void sample(Framebuffer src, Framebuffer target, boolean spread)
    {
        target.bindFramebuffer(true);
        GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
        ortho(1, 1);
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.bindTexture(src.framebufferTexture);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        float u = (float)src.framebufferWidth / (float)src.framebufferTextureWidth;
        float v = (float)src.framebufferHeight / (float)src.framebufferTextureHeight;

        if (!spread)
        {
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            quad(0, 0, 1, 1, 0.0F, v, u, 0.0F);
        }
        else
        {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.25F);
            float du = u / src.framebufferWidth;
            float dv = v / src.framebufferHeight;

            for (int k = 0; k < 4; ++k)
            {
                float ox = (k & 1) == 0 ? -du : du;
                float oy = (k & 2) == 0 ? -dv : dv;
                quad(0, 0, 1, 1, ox, v + oy, u + ox, oy);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableBlend();
        }

        if (src == Minecraft.getMinecraft().getFramebuffer())
        {
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }

        GlStateManager.enableAlpha();
    }

    private static void ortho(float w, float h)
    {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, w, h, 0.0D, -1.0D, 1.0D);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
    }

    private static void quad(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1)
    {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x0, y1, 0.0D).tex(u0, v1).endVertex();
        buf.pos(x1, y1, 0.0D).tex(u1, v1).endVertex();
        buf.pos(x1, y0, 0.0D).tex(u1, v0).endVertex();
        buf.pos(x0, y0, 0.0D).tex(u0, v0).endVertex();
        tess.draw();
    }

    /** The six panorama pictures on the inside of a slowly turning cube. */
    private static void panorama(Minecraft mc, float aspect)
    {
        float t = SythixTheme.reducedMotion ? 0.0F : (System.currentTimeMillis() - start) / 50.0F;
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        Project.gluPerspective(80.0F, aspect, 0.05F, 10.0F);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.enableTexture2D();
        GlStateManager.rotate((float)Math.sin(t / 400.0F) * 12.0F + 10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-t * 0.08F, 0.0F, 1.0F, 0.0F);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        for (int k = 0; k < 6; ++k)
        {
            GlStateManager.pushMatrix();

            if (k == 1)
            {
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            }
            else if (k == 2)
            {
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            }
            else if (k == 3)
            {
                GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            }
            else if (k == 4)
            {
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            }
            else if (k == 5)
            {
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
            }

            mc.getTextureManager().bindTexture(PANORAMA[k]);
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buf.pos(-1.0D, -1.0D, 1.0D).tex(0.0D, 0.0D).endVertex();
            buf.pos(1.0D, -1.0D, 1.0D).tex(1.0D, 0.0D).endVertex();
            buf.pos(1.0D, 1.0D, 1.0D).tex(1.0D, 1.0D).endVertex();
            buf.pos(-1.0D, 1.0D, 1.0D).tex(0.0D, 1.0D).endVertex();
            tess.draw();
            GlStateManager.popMatrix();
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
    }
}
