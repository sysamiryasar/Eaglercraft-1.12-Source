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
    /** Master switch for the themed UI. Becomes a user setting with the mod menu. */
    public static boolean enabled = true;

    // Colours are ARGB (0xAARRGGBB)
    public static final int BLACK = 0xFF0B0B0E;
    public static final int CHARCOAL = 0xFF16161B;
    public static final int CHARCOAL_LIGHT = 0xFF22222A;
    public static final int BORDER = 0xFF2E2E37;
    public static final int RED = 0xFFD81820;
    public static final int RED_DARK = 0xFF98080C;
    public static final int BLUE = 0xFF2F6BFF;
    public static final int LIGHT_BLUE = 0xFF6CC6FF;
    public static final int TEXT = 0xFFE6E6EC;
    public static final int TEXT_MUTED = 0xFF9A9AA6;
    public static final int TEXT_DISABLED = 0xFF5C5C66;

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
        drawBackgroundRegion(screenWidth, screenHeight, 0, 0, screenWidth, screenHeight, mouseX, mouseY, dim);
    }

    /**
     * Draws only part of the full-screen background (for example the bars above and below a list),
     * lined up exactly with what drawBackground would show there.
     */
    public static void drawBackgroundRegion(int screenWidth, int screenHeight, int left, int top, int right, int bottom, int mouseX, int mouseY, float dim)
    {
        if (right <= left || bottom <= top)
        {
            return;
        }

        float imageAspect = 2.0F;
        float sw = (float)Math.max(1, screenWidth);
        float sh = (float)Math.max(1, screenHeight);
        float screenAspect = sw / sh;
        float uSpan = 1.0F;
        float vSpan = 1.0F;

        if (screenAspect < imageAspect)
        {
            uSpan = screenAspect / imageAspect;
        }
        else
        {
            vSpan = imageAspect / screenAspect;
        }

        // Leave 3% of slack for the parallax movement
        uSpan *= 0.97F;
        vSpan *= 0.97F;
        float px = mouseX < 0 ? 0.5F : (float)mouseX / sw;
        float py = mouseY < 0 ? 0.5F : (float)mouseY / sh;
        float u0 = (1.0F - uSpan) * (1.0F - px);
        float v0 = (1.0F - vSpan) * (1.0F - py);
        GlStateManager.disableAlpha();
        drawTexture(BACKGROUND, (float)left, (float)top, (float)(right - left), (float)(bottom - top), u0 + uSpan * left / sw, v0 + vSpan * top / sh, u0 + uSpan * right / sw, v0 + vSpan * bottom / sh, 1.0F);
        GlStateManager.enableAlpha();

        if (dim > 0.0F)
        {
            drawRect((float)left, (float)top, (float)right, (float)bottom, withAlpha(BLACK, (int)(255 * dim)));
        }
    }

    /** Background, border and hover accent of a themed button. hover is an animated 0..1 value. */
    public static void drawButton(float x, float y, float width, float height, boolean enabled, float hover)
    {
        if (!enabled)
        {
            drawRect(x, y, x + width, y + height, 0x9010101A);
            drawOutline(x, y, x + width, y + height, 0xFF1E1E25);
            return;
        }

        drawRect(x, y, x + width, y + height, lerpColor(withAlpha(CHARCOAL, 0xD8), withAlpha(CHARCOAL_LIGHT, 0xF0), hover));
        drawOutline(x, y, x + width, y + height, lerpColor(BORDER, RED_DARK, hover));

        if (hover > 0.01F)
        {
            float half = (width - 2) * 0.5F * hover;
            float mid = x + width * 0.5F;
            drawGradientH(mid - half, y + height - 2, mid, y + height - 1, withAlpha(RED, (int)(255 * hover)), withAlpha(RED, (int)(255 * hover)));
            drawGradientH(mid, y + height - 2, mid + half, y + height - 1, withAlpha(RED, (int)(255 * hover)), withAlpha(BLUE, (int)(255 * hover)));
        }
    }

    public static int getButtonTextColor(boolean enabled, float hover)
    {
        return enabled ? lerpColor(TEXT, 0xFFFFFFFF, hover) : TEXT_DISABLED;
    }

    /** Slider handle, drawn over a themed button background. */
    public static void drawSliderKnob(float x, float y, float width, float height)
    {
        drawGradientV(x, y, x + width, y + height, RED, RED_DARK);
        drawRect(x, y, x + width, y + 1, 0xFFFF5A60);
        drawOutline(x, y, x + width, y + height, 0xFF5A0408);
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
