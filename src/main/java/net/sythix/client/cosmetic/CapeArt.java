package net.sythix.client.cosmetic;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

/**
 * The ten animated Sythix capes. Each is painted pixel by pixel about 20 times a second into two
 * textures: the cape itself and a glow layer, which is drawn a second time at full brightness
 * (added light), so cracks, circuits, flames and stars keep glowing in the dark.
 *
 * A cape texture is 64x32 cape units (the vanilla layout, 10x16 units per face) times SCALE pixels.
 */
public final class CapeArt
{
    /** Pixels per cape unit: 4 on desktop (40x64 per face), 2 in the slower browser version. */
    private static final int SCALE = GlStateManager.WEB ? 2 : 4;
    private static final int W = 64 * SCALE;
    private static final int H = 32 * SCALE;
    private static final long FRAME_MS = GlStateManager.WEB ? 100L : 50L;
    private static final Map<String, Cape> CAPES = new HashMap<String, Cape>();
    private static float[][] emblem;

    private static final class Cape
    {
        final int index;
        final DynamicTexture base = new DynamicTexture(W, H);
        final DynamicTexture glow = new DynamicTexture(W, H);
        final ResourceLocation baseLocation;
        final ResourceLocation glowLocation;
        long painted = -1L;

        Cape(int index)
        {
            this.index = index;
            this.baseLocation = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("sythix_cape_art", this.base);
            this.glowLocation = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("sythix_cape_glow", this.glow);
        }
    }

    private CapeArt()
    {
    }

    /** True for the ids of the animated Sythix capes. */
    public static boolean isArt(String id)
    {
        return CosmeticCatalog.capeById(id) != null;
    }

    /** The cape texture for an id (repainted when it is due), or null. */
    public static ResourceLocation texture(String id)
    {
        Cape cape = cape(id);
        return cape == null ? null : cape.baseLocation;
    }

    /** Its glow layer, or null. */
    public static ResourceLocation glow(String id)
    {
        Cape cape = cape(id);
        return cape == null ? null : cape.glowLocation;
    }

    private static Cape cape(String id)
    {
        CosmeticCatalog.Item item = CosmeticCatalog.capeById(id);

        if (item == null)
        {
            return null;
        }

        Cape cape = CAPES.get(id);

        if (cape == null)
        {
            cape = new Cape(item.number);
            CAPES.put(id, cape);
        }

        long now = Minecraft.getSystemTime();

        if (now - cape.painted >= FRAME_MS)
        {
            cape.painted = now;
            paint(cape, (float)(now % 3600000L) / 1000.0F);
        }

        return cape;
    }

    /**
     * Draws the glow layer over a cape that was just drawn (same position), at full brightness and
     * as added light. Call right after the cape model has been rendered with its normal texture.
     */
    public static void renderGlow(String id, Runnable drawCape)
    {
        ResourceLocation glow = glow(id);

        if (glow == null)
        {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(glow);
        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        drawCape.run();
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    }

    // ---- painting -------------------------------------------------------------------------------

    private static void paint(Cape cape, float t)
    {
        int[] base = cape.base.getTextureData();
        int[] glow = cape.glow.getTextureData();
        int faceW = 10 * SCALE;
        int faceH = 16 * SCALE;
        int[] out = new int[2];

        java.util.Arrays.fill(base, 0);
        java.util.Arrays.fill(glow, 0);

        for (int py = 0; py < faceH; ++py)
        {
            for (int px = 0; px < faceW; ++px)
            {
                // cape units: x 0-10 across, y 0-16 from the top (the neck) down
                float x = ((float)px + 0.5F) / (float)SCALE;
                float y = ((float)py + 0.5F) / (float)SCALE;
                pixel(cape.index, x, y, px, py, t, out);
                int b = 0xFF000000 | out[0];
                int g = out[1];
                // outer face (seen from behind) and inner face (seen when the cape lifts)
                set(base, glow, SCALE + px, SCALE + py, b, g);
                set(base, glow, 12 * SCALE + (faceW - 1 - px), SCALE + py, b, g);
            }
        }

        // the cape's thin edges take the colour of the nearest face pixel, a little darker
        for (int py = 0; py < faceH; ++py)
        {
            int left = base[(SCALE + py) * W + SCALE];
            int right = base[(SCALE + py) * W + SCALE + faceW - 1];

            for (int i = 0; i < SCALE; ++i)
            {
                base[(SCALE + py) * W + i] = darker(left);
                base[(SCALE + py) * W + 11 * SCALE + i] = darker(right);
            }
        }

        for (int px = 0; px < 22 * SCALE; ++px)
        {
            int top = base[SCALE * W + Math.min(px, 11 * SCALE - 1)];

            for (int i = 0; i < SCALE; ++i)
            {
                base[i * W + px] = darker(top);
                base[(17 * SCALE + i) * W + px] = darker(top);
            }
        }

        cape.base.updateDynamicTexture();
        cape.glow.updateDynamicTexture();
    }

    private static void set(int[] base, int[] glow, int x, int y, int b, int g)
    {
        base[y * W + x] = b;
        glow[y * W + x] = g;
    }

    private static int darker(int c)
    {
        return 0xFF000000 | CosmeticShapes.mix(c, 0xFF000000, 0.35F) & 0xFFFFFF;
    }

    /** out[0] = cape colour (RGB), out[1] = glow (ARGB, alpha = strength). */
    private static void pixel(int cape, float x, float y, int px, int py, float t, int[] out)
    {
        switch (cape)
        {
            case 1:
                voidRift(x, y, t, out);
                break;
            case 2:
                galaxy(x, y, t, out);
                break;
            case 3:
                circuit(x, y, t, out);
                break;
            case 4:
                inferno(x, y, t, out);
                break;
            case 5:
                frost(x, y, t, out);
                break;
            case 6:
                celestial(x, y, t, out);
                break;
            case 7:
                shadow(x, y, t, out);
                break;
            case 8:
                glitch(x, y, px, py, t, out);
                break;
            case 9:
                eclipse(x, y, t, out);
                break;
            default:
                genesis(x, y, px, py, t, out);
        }
    }

    // 01 Void Rift: black cape, a jagged purple crack down the middle that pulses
    private static void voidRift(float x, float y, float t, int[] out)
    {
        float haze = fbm(x * 0.35F, y * 0.25F + t * 0.05F);
        int base = mix(0x07040C, 0x1A0B2A, haze * 0.6F);
        float crack = Math.min(crackDistance(x, y), Math.min(branch(x, y, 5.0F, 1.0F), branch(x, y, 10.5F, -1.0F)));
        float core = (float)Math.exp(-crack * 4.5F);
        float pulse = 0.55F + 0.45F * (float)Math.sin(t * 3.0F - y * 0.35F);
        out[0] = mix(base, 0xE6B8FF, core * 0.9F);
        out[1] = argb(mix(0x9B30FF, 0xF0D0FF, core * core), Math.min(1.0F, core * pulse * 1.3F));
    }

    private static float crackDistance(float x, float y)
    {
        float cx = 5.0F + 1.4F * (float)Math.sin(y * 0.55F) + 1.2F * (noise(1.0F, y * 0.9F) - 0.5F);
        return Math.abs(x - cx);
    }

    private static float branch(float x, float y, float startY, float dir)
    {
        float along = (y - startY) * 0.9F;

        if (along < 0.0F || along > 4.0F)
        {
            return 9.0F;
        }

        float cx = 5.0F + 1.4F * (float)Math.sin(startY * 0.55F) + dir * along * 0.9F + 0.6F * (noise(7.0F + dir, y) - 0.5F);
        return Math.abs(x - cx) + along * 0.12F;
    }

    // 02 Galaxy Drift: a swirling nebula and twinkling stars
    private static void galaxy(float x, float y, float t, int[] out)
    {
        float dx = x - 5.0F;
        float dy = (y - 8.0F) * 0.7F;
        float r = (float)Math.sqrt(dx * dx + dy * dy);
        float a = (float)Math.atan2(dy, dx) + t * 0.25F + r * 0.35F;
        float n = fbm((float)Math.cos(a) * r * 0.6F + 10.0F, (float)Math.sin(a) * r * 0.6F + 10.0F);
        float arm = 0.5F + 0.5F * (float)Math.sin(a * 2.0F - r * 0.8F);
        float cloud = clamp(n * 1.3F * arm - 0.15F + 0.25F / (1.0F + r));
        int c = mix(0x070418, 0x3A1678, cloud);
        c = mix(c, 0xD05AA8, clamp(cloud * 1.6F - 0.7F));
        c = mix(c, 0x7FE6FF, clamp(cloud * 2.0F - 1.4F));
        float star = star(x, y, t, 2.2F);
        out[0] = mix(c, 0xFFFFFF, star);
        out[1] = argb(mix(0x8A5CFF, 0xFFFFFF, star), clamp(star + cloud * 0.25F));
    }

    // 03 Cyber Circuit: dark board, circuit traces with neon pulses running down
    private static void circuit(float x, float y, float t, int[] out)
    {
        int base = mix(0x04121C, 0x02070D, y / 16.0F);
        float grid = ((x * 2.0F) % 1.0F < 0.08F || (y * 2.0F) % 1.0F < 0.06F) ? 0.08F : 0.0F;
        base = mix(base, 0x0B3346, grid);
        float trace = 0.0F;
        float pulse = 0.0F;
        float[] columns = {1.5F, 3.5F, 6.5F, 8.5F};

        for (int i = 0; i < columns.length; ++i)
        {
            float cx = columns[i] + ((int)(y / 4.0F) % 2 == 0 ? 0.0F : (i % 2 == 0 ? 1.0F : -1.0F));
            float dist = Math.abs(x - cx);

            if (dist < 0.28F)
            {
                trace = 1.0F;
                pulse = Math.max(pulse, wave(y / 16.0F - t * 0.45F + i * 0.29F));
            }

            // jogs where a trace steps sideways (every 4 units)
            float row = (float)Math.round(y / 4.0F) * 4.0F;

            if (Math.abs(y - row) < 0.28F && row > 0.0F && row < 16.0F && x > Math.min(columns[i], columns[i] + (i % 2 == 0 ? 1.0F : -1.0F)) - 0.28F && x < Math.max(columns[i], columns[i] + (i % 2 == 0 ? 1.0F : -1.0F)) + 0.28F)
            {
                trace = 1.0F;
                pulse = Math.max(pulse, wave(row / 16.0F - t * 0.45F + i * 0.29F));
            }
        }

        // solder pads
        float pad = hash((int)(x / 2.5F), (int)(y / 2.0F)) > 0.7F && Math.abs(x % 2.5F - 1.25F) < 0.4F && Math.abs(y % 2.0F - 1.0F) < 0.4F ? 1.0F : 0.0F;
        out[0] = mix(base, trace > 0.0F ? 0x0E5B78 : 0x1A6E8A, Math.max(trace * 0.8F, pad * 0.6F));
        out[0] = mix(out[0], 0x9FF6FF, pulse);
        out[1] = argb(0x18D8FF, clamp(trace * (0.25F + pulse) + pad * 0.3F));
    }

    // 04 Inferno: dark cape, flames licking up from the bottom
    private static void inferno(float x, float y, float t, int[] out)
    {
        int base = mix(0x1A0605, 0x3A0A06, y / 16.0F);
        float height = 5.0F + 3.0F * fbm(x * 0.45F, t * 0.8F) + 1.5F * (float)Math.sin(x * 1.3F + t * 3.0F);
        float above = 16.0F - y;
        float flame = clamp(1.0F - above / height);
        flame *= 0.65F + 0.6F * fbm(x * 0.7F, y * 0.45F + t * 2.2F);
        flame = clamp(flame * 1.4F - 0.15F);
        int c = mix(base, 0x8A0E06, clamp(flame * 2.0F));
        c = mix(c, 0xFF4A12, clamp(flame * 2.0F - 0.6F));
        c = mix(c, 0xFFC23A, clamp(flame * 2.5F - 1.6F));
        out[0] = c;
        out[1] = argb(mix(0xFF3A0A, 0xFFD25A, flame), clamp(flame * 1.2F - 0.1F));
    }

    // 05 Frostbite: ice crystals with a shimmer sweeping across
    private static void frost(float x, float y, float t, int[] out)
    {
        int base = mix(0xA8E4FF, 0x1E5FA8, y / 16.0F);
        float edge = cellEdge(x * 0.55F, y * 0.55F);
        float crystal = clamp(1.0F - edge * 6.0F);
        float shimmer = (float)Math.pow(0.5F + 0.5F * Math.sin((x + y) * 0.7F - t * 2.6F), 8.0D);
        float glint = star(x, y, t, 1.6F);
        out[0] = mix(mix(base, 0xF2FCFF, crystal * 0.75F), 0xFFFFFF, Math.max(shimmer * 0.45F, glint));
        out[1] = argb(0x9EE8FF, clamp(crystal * shimmer * 0.9F + glint + crystal * 0.12F));
    }

    // 06 Celestial: white and gold, constellations that twinkle and draw their lines
    private static final float[][] STARS = {{2.2F, 3.0F}, {4.6F, 2.2F}, {6.8F, 3.6F}, {8.0F, 6.2F}, {5.4F, 7.0F}, {2.6F, 8.6F}, {4.2F, 11.4F}, {7.2F, 10.4F}, {8.4F, 13.6F}, {3.0F, 14.0F}};
    private static final int[][] LINKS = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 1}, {5, 6}, {6, 7}, {7, 8}, {6, 9}};

    private static void celestial(float x, float y, float t, int[] out)
    {
        int base = mix(0xFBF8F0, 0xE9DDB8, y / 16.0F);
        boolean trim = x < 0.6F || x > 9.4F || y > 15.3F;
        float glow = 0.0F;

        for (int i = 0; i < LINKS.length; ++i)
        {
            float[] a = STARS[LINKS[i][0]];
            float[] b = STARS[LINKS[i][1]];
            float drawn = clamp((float)Math.sin(t * 0.6F + i * 0.7F) * 1.5F + 0.8F);
            float d = segmentDistance(x, y, a[0], a[1], a[0] + (b[0] - a[0]) * drawn, a[1] + (b[1] - a[1]) * drawn);
            glow = Math.max(glow, clamp(1.0F - d * 4.0F) * 0.7F);
        }

        for (int i = 0; i < STARS.length; ++i)
        {
            float dx = x - STARS[i][0];
            float dy = y - STARS[i][1];
            float twinkle = 0.6F + 0.4F * (float)Math.sin(t * 4.0F + i * 1.7F);
            glow = Math.max(glow, clamp(1.0F - (float)Math.sqrt(dx * dx + dy * dy) * 2.2F) * twinkle * 1.3F);
        }

        int gold = 0xD9A520;
        out[0] = trim ? 0xC8901A : mix(base, gold, clamp(glow));
        out[1] = argb(0xFFD25A, clamp(glow * 0.9F + (trim ? 0.15F : 0.0F)));
    }

    // 07 Shadow Pulse: dark smoke drifting across, a faint purple pulse inside it
    private static void shadow(float x, float y, float t, int[] out)
    {
        float smoke = fbm(x * 0.35F + t * 0.35F, y * 0.22F - t * 0.12F);
        float wisp = fbm(x * 0.6F - t * 0.25F + 4.0F, y * 0.4F + t * 0.2F);
        float density = clamp(smoke * 1.4F - 0.25F);
        int c = mix(0x050507, 0x2A2533, density);
        c = mix(c, 0x55506A, clamp(wisp * 1.6F - 0.9F));
        float pulse = 0.5F + 0.5F * (float)Math.sin(t * 2.0F - y * 0.3F);
        out[0] = c;
        out[1] = argb(0x6A2BB8, clamp(density * pulse * 0.45F));
    }

    // 08 Glitchwave: RGB-shifted blocks and rows that jump sideways
    private static void glitch(float x, float y, int px, int py, float t, int[] out)
    {
        int frame = (int)(t * 8.0F);
        int row = (int)(y * 1.5F);
        float shift = hash(row, frame) > 0.75F ? (hash(row + 99, frame) - 0.5F) * 4.0F : 0.0F;
        float sx = x + shift;
        // each colour channel is read a little to the side: the red/blue fringes of a broken signal
        int left = glitchBlock(sx + 0.4F, y, frame);
        int mid = glitchBlock(sx, y, frame);
        int right = glitchBlock(sx - 0.4F, y, frame);
        int c = (left & 0xFF0000) | (mid & 0x00FF00) | (right & 0x0000FF);
        float scan = (py % 4 == 0) ? 0.6F : 1.0F;
        float stripe = ((int)(y * 2.0F + frame * 0.25F) % 5 == 0) ? 0.12F : 0.0F;
        int base = mix(0x07060C, 0x2A1240, stripe * 4.0F);
        int lit = mix(base, c, (c == 0 ? 0.0F : 0.9F) * scan);
        out[0] = lit;
        out[1] = argb(c, c == 0 ? stripe : 0.55F * scan);
    }

    /** A block of glitch colour (magenta, cyan or rarely white) or 0 for none. */
    private static int glitchBlock(float x, float y, int frame)
    {
        int bx = (int)Math.floor(x / 2.0F);
        int by = (int)Math.floor(y / 1.2F) + frame / 3;
        float h = hash(bx, by);

        if (h < 0.72F)
        {
            return 0;
        }

        return h > 0.97F ? 0xFFFFFF : (hash(by, bx + 11) > 0.5F ? 0xFF2AD8 : 0x22E8FF);
    }

    // 09 Royal Eclipse: black and gold, a slowly turning corona around a dark sun
    private static void eclipse(float x, float y, float t, int[] out)
    {
        float dx = x - 5.0F;
        float dy = y - 6.0F;
        float r = (float)Math.sqrt(dx * dx + dy * dy);
        float a = (float)Math.atan2(dy, dx);
        boolean trim = x < 0.5F || x > 9.5F || y > 15.4F || (y > 13.6F && y < 14.0F);
        float rays = clamp((float)Math.pow(0.5F + 0.5F * Math.sin(a * 8.0F + t * 0.6F), 3.0D) * clamp(1.0F - (r - 2.6F) / 3.2F));
        float ring = clamp(1.0F - Math.abs(r - 2.7F) * 3.0F);
        boolean disk = r < 2.45F;
        int c = mix(0x050505, 0x14110A, y / 16.0F);
        c = mix(c, 0xD9A520, Math.max(ring, rays * 0.8F));
        out[0] = trim ? 0xC79318 : (disk ? 0x020202 : c);
        out[1] = argb(0xFFC23A, disk ? 0.0F : clamp(ring + rays * 0.6F + (trim ? 0.2F : 0.0F)));
    }

    // 10 SYTHIX Genesis: purple energy flowing in towards the Sythix emblem
    private static void genesis(float x, float y, int px, int py, float t, int[] out)
    {
        float dx = x - 5.0F;
        float dy = (y - 6.5F) * 0.8F;
        float r = (float)Math.sqrt(dx * dx + dy * dy);
        float a = (float)Math.atan2(dy, dx);
        float flow = (float)Math.pow(0.5F + 0.5F * Math.sin(a * 5.0F + r * 1.4F + t * 4.0F), 4.0D) * clamp(r / 3.0F);
        int c = mix(0x0A0414, 0x1E0A3A, 1.0F - clamp(r / 9.0F));
        c = mix(c, 0x9A3CFF, flow * 0.8F);
        float[] e = emblemAt(px, py);
        float pulse = 0.7F + 0.3F * (float)Math.sin(t * 2.5F);

        if (e[0] > 0.0F)
        {
            // the red emblem turns Sythix purple; grey text stays light
            int col = e[2] > 0.5F ? mix(0x5A1EA0, 0xE0B0FF, e[1]) : mix(0x9A9AB0, 0xFFFFFF, e[1]);
            out[0] = mix(c, col, e[0]);
            out[1] = argb(e[2] > 0.5F ? 0xB050FF : 0xC8C8FF, e[0] * pulse * (e[2] > 0.5F ? 0.85F : 0.4F));
            return;
        }

        out[0] = c;
        out[1] = argb(0x9A3CFF, flow * 0.7F);
    }

    /** {coverage, brightness, red-ness} of the Sythix logo drawn 8 units wide near the top of the face. */
    private static float[] emblemAt(int px, int py)
    {
        if (emblem == null)
        {
            emblem = loadEmblem();
        }

        int size = 8 * SCALE;
        int left = SCALE;
        int top = (int)(2.5F * SCALE);
        int ex = px - left;
        int ey = py - top;

        if (ex < 0 || ey < 0 || ex >= size || ey >= size || emblem.length == 0)
        {
            return new float[] {0.0F, 0.0F, 0.0F};
        }

        float[] cell = emblem[ey * size + ex];
        return cell;
    }

    private static float[][] loadEmblem()
    {
        int size = 8 * SCALE;
        float[][] cells = new float[size * size][];

        try (InputStream in = CapeArt.class.getResourceAsStream("/assets/minecraft/textures/gui/sythix/logo.png"))
        {
            BufferedImage logo = in == null ? null : ImageIO.read(in);

            for (int y = 0; y < size; ++y)
            {
                for (int x = 0; x < size; ++x)
                {
                    float alpha = 0.0F;
                    float bright = 0.0F;
                    float red = 0.0F;
                    int samples = 0;

                    if (logo != null)
                    {
                        int step = Math.max(1, logo.getWidth() / size / 3);

                        for (int sy = y * logo.getHeight() / size; sy < (y + 1) * logo.getHeight() / size; sy += step)
                        {
                            for (int sx = x * logo.getWidth() / size; sx < (x + 1) * logo.getWidth() / size; sx += step)
                            {
                                int argb = logo.getRGB(sx, sy);
                                float a = (float)((argb >>> 24) & 255) / 255.0F;
                                int r = (argb >> 16) & 255;
                                int g = (argb >> 8) & 255;
                                int b = argb & 255;
                                alpha += a;
                                bright += a * (float)Math.max(r, Math.max(g, b)) / 255.0F;
                                red += a * (r > g + 40 ? 1.0F : 0.0F);
                                ++samples;
                            }
                        }
                    }

                    float coverage = samples == 0 ? 0.0F : alpha / (float)samples;
                    cells[y * size + x] = coverage < 0.35F ? new float[] {0.0F, 0.0F, 0.0F} : new float[] {Math.min(1.0F, coverage * 1.2F), bright / Math.max(0.001F, alpha), red / Math.max(0.001F, alpha)};
                }
            }
        }
        catch (Exception e)
        {
            return new float[0][];
        }

        return cells;
    }

    // ---- small helpers --------------------------------------------------------------------------

    private static float star(float x, float y, float t, float cell)
    {
        int cx = (int)Math.floor(x / cell);
        int cy = (int)Math.floor(y / cell);
        float h = hash(cx * 3 + 17, cy * 5 + 3);

        if (h < 0.55F)
        {
            return 0.0F;
        }

        float sx = ((float)cx + 0.2F + 0.6F * hash(cx, cy + 40)) * cell;
        float sy = ((float)cy + 0.2F + 0.6F * hash(cx + 40, cy)) * cell;
        float d = (float)Math.sqrt((x - sx) * (x - sx) + (y - sy) * (y - sy));
        float twinkle = 0.5F + 0.5F * (float)Math.sin(t * (2.0F + h * 4.0F) + h * 40.0F);
        return clamp(1.0F - d * 3.2F) * twinkle;
    }

    /** 0 at the line between two neighbouring cells of a cell pattern, growing towards cell centres. */
    private static float cellEdge(float x, float y)
    {
        int ix = (int)Math.floor(x);
        int iy = (int)Math.floor(y);
        float first = 9.0F;
        float second = 9.0F;

        for (int oy = -1; oy <= 1; ++oy)
        {
            for (int ox = -1; ox <= 1; ++ox)
            {
                float fx = (float)(ix + ox) + hash(ix + ox, iy + oy);
                float fy = (float)(iy + oy) + hash(iy + oy + 31, ix + ox + 7);
                float d = (x - fx) * (x - fx) + (y - fy) * (y - fy);

                if (d < first)
                {
                    second = first;
                    first = d;
                }
                else if (d < second)
                {
                    second = d;
                }
            }
        }

        return (float)Math.sqrt(second) - (float)Math.sqrt(first);
    }

    private static float segmentDistance(float px, float py, float ax, float ay, float bx, float by)
    {
        float vx = bx - ax;
        float vy = by - ay;
        float len = vx * vx + vy * vy;
        float k = len < 1.0E-4F ? 0.0F : clamp(((px - ax) * vx + (py - ay) * vy) / len);
        float dx = px - (ax + vx * k);
        float dy = py - (ay + vy * k);
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    /** A moving bright spot: 1 where phase is a whole number, fading out around it. */
    private static float wave(float phase)
    {
        float f = phase - (float)Math.floor(phase);
        return clamp(1.0F - Math.abs(f - 0.5F) * 9.0F);
    }

    private static float hash(int x, int y)
    {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (float)((h ^ (h >>> 16)) & 0xFFFF) / 65535.0F;
    }

    private static float noise(float x, float y)
    {
        int ix = (int)Math.floor(x);
        int iy = (int)Math.floor(y);
        float fx = x - (float)ix;
        float fy = y - (float)iy;
        fx = fx * fx * (3.0F - 2.0F * fx);
        fy = fy * fy * (3.0F - 2.0F * fy);
        float a = hash(ix, iy);
        float b = hash(ix + 1, iy);
        float c = hash(ix, iy + 1);
        float d = hash(ix + 1, iy + 1);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    private static float fbm(float x, float y)
    {
        return noise(x, y) * 0.55F + noise(x * 2.1F + 5.0F, y * 2.1F + 3.0F) * 0.3F + noise(x * 4.3F + 9.0F, y * 4.3F + 1.0F) * 0.15F;
    }

    private static float clamp(float f)
    {
        return f < 0.0F ? 0.0F : (f > 1.0F ? 1.0F : f);
    }

    private static int mix(int a, int b, float t)
    {
        return CosmeticShapes.mix(a, b, t) & 0xFFFFFF;
    }

    private static int argb(int rgb, float alpha)
    {
        return ((int)(255.0F * clamp(alpha)) << 24) | (rgb & 0xFFFFFF);
    }
}
