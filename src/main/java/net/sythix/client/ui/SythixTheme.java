package net.sythix.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Sythix UI theme: colours taken from the logo (charcoal, black, red) plus blue accents,
 * and small drawing helpers shared by the menus and buttons.
 *
 * Only Minecraft's own rendering classes are used here (no direct LWJGL calls), so this
 * code carries over unchanged to a future browser (WebGL) runtime.
 */
public final class SythixTheme
{
    /** Master switch for the themed UI (Interface Theme module). */
    public static boolean enabled = true;

    /** Accessibility: turn off decorative animations (fades, bobbing, parallax). */
    public static boolean reducedMotion = false;

    /** Interface Theme: draw outlines around buttons and panels. */
    public static boolean borders = true;

    /** Interface Theme: opacity multiplier for panels (0..1). */
    public static float panelOpacity = 1.0F;

    // Colours are ARGB (0xAARRGGBB)
    public static int BLACK = 0xFF0B0B0E;
    public static int CHARCOAL = 0xFF16161B;
    public static int CHARCOAL_LIGHT = 0xFF22222A;
    public static int BORDER = 0xFF2E2E37;
    public static int RED = 0xFFD81820;
    public static int RED_DARK = 0xFF98080C;
    public static int BLUE = 0xFFFF7A3D;
    public static int LIGHT_BLUE = 0xFF6CC6FF;
    public static int TEXT = 0xFFE6E6EC;
    public static int TEXT_MUTED = 0xFF9A9AA6;
    public static int TEXT_DISABLED = 0xFF5C5C66;

    public static final ResourceLocation LOGO = new ResourceLocation("textures/gui/sythix/logo.png");
    public static final ResourceLocation BACKGROUND = new ResourceLocation("textures/gui/sythix/background.png");

    /** The logo texture is 512x512 with the artwork (376x512) centred horizontally. */
    public static final float LOGO_ASPECT = 376.0F / 512.0F;

    private SythixTheme()
    {
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    /** Linear blend between two ARGB colours, t in [0, 1]. */
    public static int lerpColor(int from, int to, float t)
    {
        t = t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
        int a = (int)(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * t);
        int r = (int)(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = (int)(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = (int)((from & 255) + ((to & 255) - (from & 255)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int withAlpha(int color, int alpha)
    {
        return (alpha & 255) << 24 | (color & 0xFFFFFF);
    }

    /** Filled rectangle with a separate colour at each corner (top-left, top-right, bottom-right, bottom-left). */
    public static void drawQuad(float left, float top, float right, float bottom, int tl, int tr, int br, int bl)
    {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, right, top, tr);
        vertex(buffer, left, top, tl);
        vertex(buffer, left, bottom, bl);
        vertex(buffer, right, bottom, br);
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private static void vertex(BufferBuilder buffer, float x, float y, int color)
    {
        buffer.pos((double)x, (double)y, 0.0D).color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24) & 255).endVertex();
    }

    public static void drawRect(float left, float top, float right, float bottom, int color)
    {
        drawQuad(left, top, right, bottom, color, color, color, color);
    }

    public static void drawGradientH(float left, float top, float right, float bottom, int leftColor, int rightColor)
    {
        drawQuad(left, top, right, bottom, leftColor, rightColor, rightColor, leftColor);
    }

    public static void drawGradientV(float left, float top, float right, float bottom, int topColor, int bottomColor)
    {
        drawQuad(left, top, right, bottom, topColor, topColor, bottomColor, bottomColor);
    }

    /** 1px outline inside the given bounds. */
    public static void drawOutline(float left, float top, float right, float bottom, int color)
    {
        drawRect(left, top, right, top + 1, color);
        drawRect(left, bottom - 1, right, bottom, color);
        drawRect(left, top + 1, left + 1, bottom - 1, color);
        drawRect(right - 1, top + 1, right, bottom - 1, color);
    }

    /** Red-to-blue accent line, the signature Sythix detail. */
    public static void drawAccentLine(float left, float y, float right, float alpha)
    {
        int a = (int)(255 * alpha);
        drawGradientH(left, y, right, y + 1, withAlpha(RED, a), withAlpha(BLUE, a));
    }

    /** Draws a whole texture into a rectangle with smooth (linear) filtering. */
    public static void drawTexture(ResourceLocation texture, float x, float y, float width, float height, float alpha)
    {
        drawTexture(texture, x, y, width, height, 0.0F, 0.0F, 1.0F, 1.0F, alpha);
    }

    public static void drawTexture(ResourceLocation texture, float x, float y, float width, float height, float u0, float v0, float u1, float v1, float alpha)
    {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.glTexParameteri(3553, 10241, 9729);
        GlStateManager.glTexParameteri(3553, 10240, 9729);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos((double)x, (double)(y + height), 0.0D).tex((double)u0, (double)v1).endVertex();
        buffer.pos((double)(x + width), (double)(y + height), 0.0D).tex((double)u1, (double)v1).endVertex();
        buffer.pos((double)(x + width), (double)y, 0.0D).tex((double)u1, (double)v0).endVertex();
        buffer.pos((double)x, (double)y, 0.0D).tex((double)u0, (double)v0).endVertex();
        tessellator.draw();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    /**
     * Full-screen background image, cropped to keep its aspect ratio. The mouse position adds a
     * gentle parallax shift; pass a negative mouseX to disable it.
     */
    public static void drawBackground(int screenWidth, int screenHeight, int mouseX, int mouseY, float dim)
    {
        net.sythix.client.SythixClient client = net.sythix.client.SythixClient.get();
        net.sythix.client.module.hud.MainMenuModule menu = client == null ? null : client.getModuleManager().get(net.sythix.client.module.hud.MainMenuModule.class);

        if (menu != null)
        {
            menu.applyBackground();
        }

        MenuBackground.draw(screenWidth, screenHeight, dim);
    }

    /**
     * Draws only part of the full-screen background (for example the bars above and below a list),
     * lined up exactly with what drawBackground would show there.
     */
    public static void drawBackgroundRegion(int screenWidth, int screenHeight, int left, int top, int right, int bottom, int mouseX, int mouseY, float dim)
    {
        MenuBackground.drawRegion(screenWidth, screenHeight, left, top, right, bottom, dim);
    }

    // ---- rounded shapes --------------------------------------------------------------------------

    private static final int ARC = 6;

    /** Adds the outline points of a rounded rectangle, clockwise from the top-left corner. */
    private static void roundedPath(java.util.List<float[]> out, float x0, float y0, float x1, float y1, float r)
    {
        r = Math.max(0.0F, Math.min(r, Math.min(x1 - x0, y1 - y0) / 2.0F));
        float[][] corners = {{x0 + r, y0 + r, 180.0F}, {x1 - r, y0 + r, 270.0F}, {x1 - r, y1 - r, 0.0F}, {x0 + r, y1 - r, 90.0F}};

        for (float[] c : corners)
        {
            for (int i = 0; i <= ARC; ++i)
            {
                double a = Math.toRadians(c[2] + 90.0D * i / ARC);
                out.add(new float[] {c[0] + (float)Math.cos(a) * r, c[1] + (float)Math.sin(a) * r});
            }
        }
    }

    /** A filled rectangle with rounded corners of radius r. */
    public static void drawRounded(float x0, float y0, float x1, float y1, float r, int color)
    {
        if ((color >>> 24) == 0 || x1 <= x0 || y1 <= y0)
        {
            return;
        }

        java.util.List<float[]> pts = new java.util.ArrayList<float[]>();
        roundedPath(pts, x0, y0, x1, y1, r);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        int a = color >>> 24, rr = color >> 16 & 255, g = color >> 8 & 255, bl = color & 255;
        buf.pos((x0 + x1) / 2.0F, (y0 + y1) / 2.0F, 0.0D).color(rr, g, bl, a).endVertex();

        for (int i = pts.size() - 1; i >= 0; --i)
        {
            buf.pos(pts.get(i)[0], pts.get(i)[1], 0.0D).color(rr, g, bl, a).endVertex();
        }

        buf.pos(pts.get(pts.size() - 1)[0], pts.get(pts.size() - 1)[1], 0.0D).color(rr, g, bl, a).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
    }

    /** The outline of a rounded rectangle, thickness units wide, drawn inside the edge. */
    public static void drawRoundedOutline(float x0, float y0, float x1, float y1, float r, float thickness, int color)
    {
        if ((color >>> 24) == 0 || x1 <= x0 || y1 <= y0)
        {
            return;
        }

        java.util.List<float[]> outer = new java.util.ArrayList<float[]>();
        java.util.List<float[]> inner = new java.util.ArrayList<float[]>();
        roundedPath(outer, x0, y0, x1, y1, r);
        roundedPath(inner, x0 + thickness, y0 + thickness, x1 - thickness, y1 - thickness, Math.max(0.0F, r - thickness));
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int a = color >>> 24, rr = color >> 16 & 255, g = color >> 8 & 255, bl = color & 255;

        for (int i = 0; i <= outer.size(); ++i)
        {
            float[] o = outer.get(i % outer.size());
            float[] n = inner.get(i % inner.size());
            buf.pos(o[0], o[1], 0.0D).color(rr, g, bl, a).endVertex();
            buf.pos(n[0], n[1], 0.0D).color(rr, g, bl, a).endVertex();
        }

        tess.draw();
        GlStateManager.enableTexture2D();
    }

    /** A frosted-glass panel: translucent dark fill with a faint light edge. */
    public static void drawGlass(float x0, float y0, float x1, float y1, float r, float alpha)
    {
        drawRounded(x0, y0, x1, y1, r, withAlpha(0xFF0E0D12, (int)(0xB4 * alpha * panelOpacity)));
        drawRoundedOutline(x0, y0, x1, y1, r, 0.5F, withAlpha(0xFFFFFFFF, (int)(0x1E * alpha)));
    }

    /** The Sythix accent: a short gradient from the primary colour (red) to the secondary (ember). */
    public static void drawAccentGradient(float x0, float y0, float x1, float y1, float alpha)
    {
        drawGradientH(x0, y0, x1, y1, withAlpha(RED, (int)(255 * alpha)), withAlpha(BLUE, (int)(255 * alpha)));
    }

    /** Background, border and hover accent of a themed button. hover is an animated 0..1 value. */
    public static void drawButton(float x, float y, float width, float height, boolean enabled, float hover)
    {
        float r = Math.min(height / 2.0F, 4.0F);

        if (!enabled)
        {
            drawRounded(x, y, x + width, y + height, r, 0x700C0B10);
            drawRoundedOutline(x, y, x + width, y + height, r, 0.5F, 0x10FFFFFF);
            return;
        }

        drawRounded(x, y, x + width, y + height, r, lerpColor(withAlpha(0xFF110F15, (int)(0xA8 * panelOpacity)), withAlpha(0xFF221D27, (int)(0xD8 * panelOpacity)), hover));

        if (borders)
        {
            drawRoundedOutline(x, y, x + width, y + height, r, 0.5F, lerpColor(0x1FFFFFFF, withAlpha(lerpColor(RED, BLUE, 0.35F), 0xB0), hover));
        }

        if (hover > 0.01F)
        {
            // a glowing accent line grows from the middle of the bottom edge
            float half = (width - 2.0F * r) * 0.5F * hover;
            float mid = x + width * 0.5F;
            drawGradientH(mid - half, y + height - 1.25F, mid, y + height - 0.5F, withAlpha(RED, (int)(220 * hover)), withAlpha(lerpColor(RED, BLUE, 0.5F), (int)(255 * hover)));
            drawGradientH(mid, y + height - 1.25F, mid + half, y + height - 0.5F, withAlpha(lerpColor(RED, BLUE, 0.5F), (int)(255 * hover)), withAlpha(BLUE, (int)(220 * hover)));
        }
    }

    public static int getButtonTextColor(boolean enabled, float hover)
    {
        return enabled ? lerpColor(TEXT, 0xFFFFFFFF, hover) : TEXT_DISABLED;
    }

    /** Slider handle, drawn over a themed button background. */
    public static void drawSliderKnob(float x, float y, float width, float height)
    {
        drawRounded(x + 1.0F, y + 2.0F, x + width - 1.0F, y + height - 2.0F, 2.0F, lerpColor(RED, BLUE, 0.3F));
        drawRounded(x + 1.0F, y + 2.0F, x + width - 1.0F, y + 4.0F, 1.0F, 0x40FFFFFF);
    }

    /** Dark bar used above and below scrolling lists. */
    public static void drawListBar(float left, float top, float right, float bottom, int alpha)
    {
        drawRect(left, top, right, bottom, withAlpha(BLACK, alpha));
    }

    /**
     * Clips drawing to a rectangle in GUI coordinates until endClip is called. This is the only
     * place that uses raw OpenGL, so a browser build only needs to adapt this method.
     */
    public static void beginClip(int left, int top, int right, int bottom)
    {
        Minecraft mc = Minecraft.getMinecraft();
        int factor = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left * factor, mc.displayHeight - bottom * factor, Math.max(0, (right - left) * factor), Math.max(0, (bottom - top) * factor));
    }

    public static void endClip()
    {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** Smoothly moves current toward target; speed is in units per second. */
    public static float approach(float current, float target, float speed, float seconds)
    {
        float step = speed * seconds;
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }
}
