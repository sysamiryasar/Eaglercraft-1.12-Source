package net.sythix.client.ui.font;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/**
 * A smooth TrueType font (Poppins, SIL Open Font License) for Sythix's interface. Glyphs are drawn
 * once by Java's font system into 1024x1024 atlas pages of 256 characters, then drawn as textured
 * quads. Sizes are in Minecraft GUI units: a capital letter is 7 units tall at size 1, like the
 * normal Minecraft font, so it can replace it anywhere.
 */
public final class SmoothFont
{
    private static final int PX = 36;
    private static final int CELL = 64;
    private static final int PAD = 6;

    public static final SmoothFont REGULAR = new SmoothFont("/sythix/fonts/Poppins-Medium.ttf");
    public static final SmoothFont BOLD = new SmoothFont("/sythix/fonts/Poppins-SemiBold.ttf");

    private final String path;
    private Font font;
    private Font fallback;
    private float ascent;
    private float scale;
    private boolean failed;
    private final Map<Integer, Page> pages = new HashMap<Integer, Page>();

    private static final class Page
    {
        int texture;
        final float[] advance = new float[256];
        final boolean[] fallback = new boolean[256];
    }

    private SmoothFont(String path)
    {
        this.path = path;
    }

    private boolean load()
    {
        if (this.font != null)
        {
            return true;
        }

        if (this.failed)
        {
            return false;
        }

        try (InputStream in = SmoothFont.class.getResourceAsStream(this.path))
        {
            this.font = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont((float)PX);
            this.fallback = new Font(Font.SANS_SERIF, Font.PLAIN, PX);
            BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = probe.createGraphics();
            FontMetrics fm = g.getFontMetrics(this.font);
            this.ascent = fm.getAscent();
            float cap = (float)this.font.createGlyphVector(g.getFontRenderContext(), "H").getVisualBounds().getHeight();
            g.dispose();
            this.scale = 7.0F / cap;
            return true;
        }
        catch (Throwable t)
        {
            this.failed = true;
            t.printStackTrace();
            return false;
        }
    }

    private Page page(int index)
    {
        Page p = this.pages.get(index);

        if (p != null)
        {
            return p;
        }

        p = new Page();
        BufferedImage img = new BufferedImage(CELL * 16, CELL * 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(java.awt.Color.WHITE);
        FontRenderContext frc = g.getFontRenderContext();

        for (int i = 0; i < 256; ++i)
        {
            char c = (char)(index * 256 + i);

            if (Character.isISOControl(c) || Character.isSurrogate(c))
            {
                continue;
            }

            Font f = this.font.canDisplay(c) ? this.font : this.fallback;
            p.fallback[i] = f != this.font;
            GlyphVector gv = f.createGlyphVector(frc, String.valueOf(c));
            p.advance[i] = gv.getGlyphMetrics(0).getAdvanceX();
            g.setFont(f);
            g.drawString(String.valueOf(c), (float)((i % 16) * CELL + PAD), (float)((i / 16) * CELL + PAD) + this.ascent);
        }

        g.dispose();
        p.texture = TextureUtil.glGenTextures();
        TextureUtil.uploadTextureImageAllocate(p.texture, img, true, true);
        GlStateManager.bindTexture(p.texture);

        if (GLContext.getCapabilities().OpenGL30)
        {
            // allocateTexture limits the texture to one level; allow the mipmaps
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 10);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 10.0F);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        }

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        this.pages.put(index, p);
        return p;
    }

    /** Width of one character in GUI units at size 1. */
    public float charWidth(char c)
    {
        if (!this.load())
        {
            return 6.0F;
        }

        return this.page(c >> 8).advance[c & 255] * this.scale + 0.25F;
    }

    /** Width of a string (formatting codes ignored) in GUI units at the given size. */
    public float width(String text, float size)
    {
        float w = 0.0F;

        for (int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length())
            {
                ++i;
                continue;
            }

            w += this.charWidth(c);
        }

        return w * size;
    }

    /** Draws a plain string (formatting codes allowed) at the given size; returns its width. */
    public float draw(String text, float x, float y, float size, int color)
    {
        return this.draw(text, x, y, size, color, false, null);
    }

    public float drawCentered(String text, float cx, float y, float size, int color)
    {
        return this.draw(text, cx - this.width(text, size) / 2.0F, y, size, color);
    }

    /**
     * Draws a string the way Minecraft's font renderer does (formatting codes, shadow colours from
     * colorCodes when shadow is set). y is the top of the line; the baseline is 7 units lower.
     */
    public float draw(String text, float x, float y, float size, int color, boolean shadow, int[] colorCodes)
    {
        if (text == null || text.isEmpty() || !this.load())
        {
            return 0.0F;
        }

        if ((color & 0xFC000000) == 0)
        {
            color |= 0xFF000000;
        }

        int alpha = color >>> 24;
        int base = color;
        boolean bold = false;
        boolean underline = false;
        boolean strike = false;
        boolean italic = false;
        float s = this.scale * size;
        float cx = x;
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.004F);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        for (int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length())
            {
                char code = Character.toLowerCase(text.charAt(++i));
                int idx = "0123456789abcdef".indexOf(code);

                if (idx >= 0)
                {
                    int rgb = colorCodes != null ? colorCodes[shadow ? idx + 16 : idx] : DEFAULT_COLORS[idx];
                    color = (alpha << 24) | (rgb & 0xFFFFFF);
                    bold = underline = strike = italic = false;
                }
                else if (code == 'l')
                {
                    bold = true;
                }
                else if (code == 'n')
                {
                    underline = true;
                }
                else if (code == 'm')
                {
                    strike = true;
                }
                else if (code == 'o')
                {
                    italic = true;
                }
                else if (code == 'r')
                {
                    color = base;
                    bold = underline = strike = italic = false;
                }

                continue;
            }

            SmoothFont f = bold && this == REGULAR ? BOLD : this;

            if (!f.load())
            {
                f = this;
            }

            Page p = f.page(c >> 8);
            int g = c & 255;
            float adv = (p.advance[g] * f.scale + 0.25F) * size;

            if (c != ' ')
            {
                float u0 = (float)((g % 16) * CELL) / (CELL * 16);
                float v0 = (float)((g / 16) * CELL) / (CELL * 16);
                float uv = (float)CELL / (CELL * 16);
                float left = cx - PAD * s;
                float top = y + 7.0F * size - (f.ascent + PAD) * s;
                float w = CELL * s;
                float skew = italic ? 1.2F * size : 0.0F;
                int r = color >> 16 & 255;
                int gr = color >> 8 & 255;
                int b = color & 255;
                int a = color >>> 24;
                GlStateManager.bindTexture(p.texture);
                buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                buf.pos(left + skew, top, 0.0D).tex(u0, v0).color(r, gr, b, a).endVertex();
                buf.pos(left, top + w, 0.0D).tex(u0, v0 + uv).color(r, gr, b, a).endVertex();
                buf.pos(left + w, top + w, 0.0D).tex(u0 + uv, v0 + uv).color(r, gr, b, a).endVertex();
                buf.pos(left + w + skew, top, 0.0D).tex(u0 + uv, v0).color(r, gr, b, a).endVertex();
                tess.draw();
            }

            if (underline || strike)
            {
                float ly = underline ? y + 8.2F * size : y + 4.0F * size;
                line(cx, ly, cx + adv, ly + 0.8F * size, color);
            }

            cx += adv;
        }

        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

        if (!blend)
        {
            GlStateManager.disableBlend();
        }

        return cx - x;
    }

    private static void line(float x0, float y0, float x1, float y1, int color)
    {
        GlStateManager.disableTexture2D();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        int a = color >>> 24;
        buf.pos(x0, y1, 0.0D).color(r, g, b, a).endVertex();
        buf.pos(x1, y1, 0.0D).color(r, g, b, a).endVertex();
        buf.pos(x1, y0, 0.0D).color(r, g, b, a).endVertex();
        buf.pos(x0, y0, 0.0D).color(r, g, b, a).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
    }

    private static final int[] DEFAULT_COLORS = new int[] {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    // ---- Use as Minecraft's font -------------------------------------------------------------

    /** Off, Menus (any screen open) or Everywhere (also the in-game HUD). Set by Interface Theme. */
    public static String mode = "Menus";

    /** Whether Minecraft's main font should draw with the smooth font right now. */
    public static boolean replacesMinecraftFont()
    {
        if ("Off".equals(mode) || REGULAR.failed)
        {
            return false;
        }

        if ("Everywhere".equals(mode))
        {
            return true;
        }

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        return mc.currentScreen != null || mc.world == null;
    }
}
