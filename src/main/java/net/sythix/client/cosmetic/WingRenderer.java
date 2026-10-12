package net.sythix.client.cosmetic;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

/**
 * The eight Sythix wing sets, drawn in body space on the player's back.
 *
 * Every wing is built like a real one, in a flat plane where x goes outwards (mirrored for the left
 * wing) and y goes down (up is negative): an arm bone rises from the shoulder blade to the wrist.
 * Feathered wings hang three layers on it: long primaries fanning out from the wrist, secondaries
 * along the arm (they make the trailing edge) and short coverts on top. Bat-like wings spread
 * finger bones from the wrist with a scalloped membrane between them. The wing opens and closes
 * (the flap) around the shoulder and sweeps back a little.
 */
public final class WingRenderer
{
    /** Root (shoulder blade), elbow (curve control point) and wrist of the arm. */
    private static final float[] ROOT = {0.06F, 0.06F};
    private static final float[] ELBOW = {0.42F, -0.38F};
    private static final float[] WRIST = {0.82F, -0.44F};

    private WingRenderer()
    {
    }

    public static void draw(String style, float age, float flapSpeed, float size, boolean sneaking)
    {
        if (style == null || CosmeticCatalog.NONE.equals(style))
        {
            return;
        }

        boolean dragon = "Dragon Wings".equals(style);
        float speed = flapSpeed * (dragon ? 0.45F : 1.0F);
        // open/close: mostly open, folding back a little on each beat
        float beat = MathHelper.sin(age * 0.15F * speed);
        float open = 18.0F + beat * (dragon ? 9.0F : 13.0F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, sneaking ? 0.32F : 0.14F, 0.155F);

        if (sneaking)
        {
            GlStateManager.rotate(28.0F, 1.0F, 0.0F, 0.0F);
        }

        GlStateManager.scale(0.85F * size, 0.85F * size, 0.85F * size);

        for (int side = -1; side <= 1; side += 2)
        {
            float s = (float)side;
            GlStateManager.pushMatrix();
            GlStateManager.translate(s * 0.05F, 0.0F, 0.0F);
            // swept back from the shoulder, and tilted up a touch on the down-beat
            GlStateManager.rotate(s * open, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(s * (6.0F - beat * 5.0F), 0.0F, 0.0F, 1.0F);
            wing(style, s, age, beat);
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();
    }

    // ---- the arm ---------------------------------------------------------------------------------

    /** A point along the arm, t from 0 (shoulder) to 1 (wrist). */
    private static float[] arm(float t)
    {
        float u = 1.0F - t;
        return new float[] {u * u * ROOT[0] + 2.0F * u * t * ELBOW[0] + t * t * WRIST[0], u * u * ROOT[1] + 2.0F * u * t * ELBOW[1] + t * t * WRIST[1]};
    }

    private static float[] p(float s, float x, float y, float z)
    {
        return new float[] {s * x, y, z};
    }

    /** One feather, kite-shaped: base point, direction in degrees (0 = outwards, 90 = down). */
    private static void feather(float s, float bx, float by, float angle, float length, float width, int root, int tip, float z, boolean shard)
    {
        float a = angle * 0.017453292F;
        float dx = MathHelper.cos(a);
        float dy = MathHelper.sin(a);
        // perpendicular in the wing plane
        float px = -dy;
        float py = dx;
        float tx = bx + dx * length;
        float ty = by + dy * length;
        float mx = bx + dx * length * (shard ? 0.45F : 0.6F);
        float my = by + dy * length * (shard ? 0.45F : 0.6F);
        float hw = width / 2.0F;
        int mid = CosmeticShapes.mix(root, tip, 0.55F);

        if (shard)
        {
            CosmeticShapes.quad(p(s, bx, by, z), p(s, mx + px * hw, my + py * hw, z), p(s, tx, ty, z), p(s, mx - px * hw, my - py * hw, z), root, mid, tip, mid);
        }
        else
        {
            // a long feather: straight sides, then a rounded point
            CosmeticShapes.quad(p(s, bx - px * hw * 0.8F, by - py * hw * 0.8F, z), p(s, bx + px * hw * 0.8F, by + py * hw * 0.8F, z), p(s, mx + px * hw, my + py * hw, z), p(s, mx - px * hw, my - py * hw, z), root, root, mid, mid);
            CosmeticShapes.quad(p(s, mx - px * hw, my - py * hw, z), p(s, mx + px * hw, my + py * hw, z), p(s, tx + px * hw * 0.35F, ty + py * hw * 0.35F, z), p(s, tx - px * hw * 0.35F, ty - py * hw * 0.35F, z), mid, mid, tip, tip);
        }
    }

    /** Colours and shape of a feathered wing. */
    private static final class Plumage
    {
        int primaryRoot;
        int primaryTip;
        int secondaryRoot;
        int secondaryTip;
        int covert;
        boolean shards;
        float gap;
    }

    /** Draws the three feather layers (call between CosmeticShapes.begin... and end). */
    private static void feathers(float s, Plumage c, float beat)
    {
        float spread = 1.0F + beat * 0.06F;

        // secondaries along the arm: hanging down, longer towards the wrist (the trailing edge)
        for (int i = 0; i < 9; ++i)
        {
            float t = 0.16F + 0.8F * (float)i / 8.0F;
            float[] b = arm(t);
            feather(s, b[0], b[1], 88.0F - 22.0F * t * spread, 0.16F + 0.5F * t - (c.gap > 0.0F && i % 3 == 1 ? c.gap : 0.0F), 0.13F, c.secondaryRoot, c.secondaryTip, 0.0F, c.shards);
        }

        // primaries fanning out from the hand, the outermost pointing out and up
        for (int i = 0; i < 7; ++i)
        {
            float k = (float)i / 6.0F;
            float[] b = arm(0.94F + 0.06F * k);
            float angle = (-22.0F + 92.0F * k) * spread;
            float length = 0.72F - 0.16F * k - (c.gap > 0.0F && i % 2 == 1 ? c.gap : 0.0F);
            feather(s, b[0] + 0.04F * (1.0F - k), b[1], angle, length, 0.12F, c.primaryRoot, c.primaryTip, 0.004F, c.shards);
        }

        // coverts: short feathers over the arm
        for (int i = 0; i < 8; ++i)
        {
            float t = 0.18F + 0.78F * (float)i / 7.0F;
            float[] b = arm(t);
            feather(s, b[0], b[1] - 0.02F, 95.0F - 25.0F * t, 0.1F + 0.13F * t, 0.15F, c.covert, CosmeticShapes.mix(c.covert, c.secondaryRoot, 0.5F), 0.008F, c.shards);
        }
    }

    /** Lines along the feather centres and the arm (for glowing edges). */
    private static void featherLines(float s, int color, float beat)
    {
        float spread = 1.0F + beat * 0.06F;

        for (int i = 0; i < 7; ++i)
        {
            float k = (float)i / 6.0F;
            float[] b = arm(0.94F + 0.06F * k);
            float a = (-22.0F + 92.0F * k) * spread * 0.017453292F;
            float length = 0.72F - 0.16F * k;
            CosmeticShapes.line(s * b[0], b[1], 0.012F, s * (b[0] + MathHelper.cos(a) * length), b[1] + MathHelper.sin(a) * length, 0.012F, 0.012F, color);
        }

        armLine(s, color, 0.02F);
    }

    private static void armLine(float s, int color, float width)
    {
        float[] a = arm(0.0F);

        for (int i = 1; i <= 10; ++i)
        {
            float[] b = arm((float)i / 10.0F);
            CosmeticShapes.line(s * a[0], a[1], 0.012F, s * b[0], b[1], 0.012F, width, color);
            a = b;
        }
    }

    // ---- bat-like wings ----------------------------------------------------------------------------

    /** Finger tips spreading from the wrist; fold (0-1) closes them a little. */
    private static float[][] fingers(float fold)
    {
        float[][] tips = {{1.32F, -0.36F}, {1.28F, 0.08F}, {1.02F, 0.44F}, {0.6F, 0.66F}};
        float[][] out = new float[4][];

        for (int i = 0; i < 4; ++i)
        {
            float dx = tips[i][0] - WRIST[0];
            float dy = tips[i][1] - WRIST[1];
            float a = (float)Math.atan2(dy, dx) + fold * 0.25F * (float)i / 3.0F;
            float r = (float)Math.sqrt(dx * dx + dy * dy) * (1.0F - fold * 0.15F);
            out[i] = new float[] {WRIST[0] + MathHelper.cos(a) * r, WRIST[1] + MathHelper.sin(a) * r};
        }

        return out;
    }

    /** The membrane: between neighbouring fingers (with a scalloped edge) and back to the body. */
    private static void membrane(float s, float[][] tips, int inner, int outer)
    {
        // between the arm and the first finger
        float[] e = arm(0.5F);
        float[] low = {0.36F, 0.38F};
        CosmeticShapes.quad(p(s, ROOT[0], ROOT[1], 0.0F), p(s, e[0], e[1], 0.0F), p(s, WRIST[0], WRIST[1], 0.0F), p(s, low[0], low[1], 0.0F), inner, inner, inner, outer);

        for (int i = 0; i < 4; ++i)
        {
            float[] a = tips[i];
            float[] b = i < 3 ? tips[i + 1] : new float[] {0.36F, 0.38F};
            // the scallop: the edge curves in towards the wrist between two tips
            float sx = (a[0] + b[0]) / 2.0F * 0.78F + WRIST[0] * 0.22F;
            float sy = (a[1] + b[1]) / 2.0F * 0.78F + WRIST[1] * 0.22F;
            float[] base = WRIST;
            CosmeticShapes.quad(p(s, base[0], base[1], 0.0F), p(s, a[0], a[1], 0.0F), p(s, sx, sy, 0.0F), p(s, b[0], b[1], 0.0F), inner, outer, CosmeticShapes.mix(inner, outer, 0.6F), outer);
        }
    }

    private static void bones(float s, float[][] tips, int color, float width)
    {
        armLine(s, color, width * 1.3F);

        for (float[] tip : tips)
        {
            CosmeticShapes.line(s * WRIST[0], WRIST[1], 0.012F, s * tip[0], tip[1], 0.012F, width, color);
        }
    }

    // ---- the eight styles ----------------------------------------------------------------------------

    private static void wing(String style, float s, float age, float beat)
    {
        float pulse = 0.6F + 0.4F * MathHelper.sin(age * 0.12F);
        Plumage c = new Plumage();

        switch (style)
        {
            case "Void Wings":
                c.primaryRoot = 0xFF1A0F26;
                c.primaryTip = 0xFF070409;
                c.secondaryRoot = 0xFF201230;
                c.secondaryTip = 0xFF0A0610;
                c.covert = 0xFF2E1A48;
                c.gap = 0.08F;
                solid();
                feathers(s, c, beat);
                CosmeticShapes.end();
                CosmeticShapes.beginAdditive();
                featherLines(s, CosmeticShapes.alpha(0xFFA040FF, (int)(170 * pulse + 50)), beat);
                CosmeticShapes.end();
                break;
            case "Celestial Wings":
                c.primaryRoot = 0xFFFFFFFF;
                c.primaryTip = 0xFFE4DFF0;
                c.secondaryRoot = 0xFFFBFAFF;
                c.secondaryTip = 0xFFDAD4EA;
                c.covert = 0xFFFFFFFF;
                solid();
                feathers(s, c, beat);
                CosmeticShapes.end();
                CosmeticShapes.beginAdditive();
                featherTips(s, CosmeticShapes.alpha(0xFFFFC63A, (int)(170 + 80 * pulse)), beat);
                CosmeticShapes.end();
                break;
            case "Cyber Wings":
                cyber(s, age, beat);
                break;
            case "Frost Wings":
                c.primaryRoot = 0xC8D8F6FF;
                c.primaryTip = 0x80A8E4FF;
                c.secondaryRoot = 0xC0C8EEFF;
                c.secondaryTip = 0x7098D8FF;
                c.covert = 0xD0F0FBFF;
                c.shards = true;
                CosmeticShapes.beginTranslucent();
                feathers(s, c, beat);
                CosmeticShapes.end();
                CosmeticShapes.beginAdditive();
                sparkles(s, age, beat);
                CosmeticShapes.end();
                break;
            case "Infernal Wings":
            {
                float[][] tips = fingers(0.0F);
                solid();
                membrane(s, tips, 0xFF240A08, 0xFF120403);
                bones(s, tips, 0xFF0C0302, 0.035F);
                CosmeticShapes.end();
                CosmeticShapes.beginAdditive();
                lava(s, tips, age);
                CosmeticShapes.end();
                break;
            }
            case "Astral Wings":
            {
                float[][] tips = fingers(0.0F);
                CosmeticShapes.beginTranslucent();
                membrane(s, tips, 0xB0301A78, 0x80104090);
                CosmeticShapes.end();
                CosmeticShapes.beginAdditive();
                bones(s, tips, CosmeticShapes.alpha(0xFF9A7CFF, 160), 0.018F);
                stars(s, tips, age);
                CosmeticShapes.end();
                break;
            }
            case "Phantom Wings":
                c.primaryRoot = CosmeticShapes.alpha(0xFFB060FF, (int)(170 + 50 * pulse));
                c.primaryTip = CosmeticShapes.alpha(0xFF7A30E8, (int)(70 + 50 * pulse));
                c.secondaryRoot = CosmeticShapes.alpha(0xFFA050F8, (int)(160 + 50 * pulse));
                c.secondaryTip = CosmeticShapes.alpha(0xFF6A20D0, (int)(60 + 40 * pulse));
                c.covert = CosmeticShapes.alpha(0xFFD0A8FF, (int)(170 + 50 * pulse));
                CosmeticShapes.beginTranslucent();
                feathers(s, c, beat);
                CosmeticShapes.end();
                CosmeticShapes.beginAdditive();
                featherLines(s, CosmeticShapes.alpha(0xFFE8D0FF, (int)(130 * pulse)), beat);
                CosmeticShapes.end();
                break;
            default:
                dragon(s, age);
        }
    }

    private static void solid()
    {
        CosmeticShapes.begin(false);
        GlStateManager.disableCull();
    }

    private static void featherTips(float s, int color, float beat)
    {
        float spread = 1.0F + beat * 0.06F;

        for (int i = 0; i < 7; ++i)
        {
            float k = (float)i / 6.0F;
            float[] b = arm(0.94F + 0.06F * k);
            float a = (-22.0F + 92.0F * k) * spread * 0.017453292F;
            float length = 0.72F - 0.16F * k;
            float x = b[0] + 0.04F * (1.0F - k) + MathHelper.cos(a) * length;
            float y = b[1] + MathHelper.sin(a) * length;
            CosmeticShapes.diamond(s * x, y, 0.012F, 0.028F, 0.04F, color);
        }
    }

    private static void sparkles(float s, float age, float beat)
    {
        for (int i = 0; i < 9; ++i)
        {
            float t = (float)i / 8.0F;
            float sparkle = (float)Math.pow(Math.abs(MathHelper.sin(age * 0.08F + (float)i * 1.9F)), 6.0D);
            float[] b = arm(0.1F + 0.85F * t);
            float y = b[1] + 0.15F + 0.25F * ((i * 7) % 3) / 2.0F;
            CosmeticShapes.diamond(s * (b[0] + 0.05F), y, 0.012F, 0.03F * sparkle + 0.004F, 0.045F * sparkle + 0.004F, CosmeticShapes.alpha(0xFFFFFFFF, (int)(255 * sparkle)));
        }

        armLine(s, CosmeticShapes.alpha(0xFFBFF0FF, 150), 0.016F);
    }

    private static void lava(float s, float[][] tips, float age)
    {
        // molten veins flowing out along the arm and each finger
        float[][] from = {ROOT, WRIST, WRIST, WRIST, WRIST};
        float[][] to = {WRIST, tips[0], tips[1], tips[2], tips[3]};

        for (int v = 0; v < 5; ++v)
        {
            for (int seg = 0; seg < 6; ++seg)
            {
                float k0 = (float)seg / 6.0F;
                float k1 = (float)(seg + 1) / 6.0F;
                float flow = 0.35F + 0.65F * (float)Math.pow(0.5F + 0.5F * MathHelper.sin(age * 0.25F - (float)seg * 0.9F - (float)v), 3.0D);
                int c = CosmeticShapes.alpha(CosmeticShapes.mix(0xFFFF3A0A, 0xFFFFC040, flow), (int)(255 * flow));
                float x0 = from[v][0] + (to[v][0] - from[v][0]) * k0;
                float y0 = from[v][1] + (to[v][1] - from[v][1]) * k0;
                float x1 = from[v][0] + (to[v][0] - from[v][0]) * k1;
                float y1 = from[v][1] + (to[v][1] - from[v][1]) * k1;
                CosmeticShapes.line(s * x0, y0, 0.014F, s * x1, y1, 0.014F, 0.022F, c);
            }
        }

        // thin glowing cracks across the membrane
        for (int i = 0; i < 3; ++i)
        {
            float glow = 0.4F + 0.3F * MathHelper.sin(age * 0.2F + (float)i * 2.0F);
            float mx = (tips[i][0] + tips[i + 1][0]) / 2.0F * 0.85F;
            float my = (tips[i][1] + tips[i + 1][1]) / 2.0F * 0.85F;
            CosmeticShapes.line(s * (WRIST[0] * 0.6F), WRIST[1] * 0.2F + 0.2F, 0.014F, s * mx, my, 0.014F, 0.012F, CosmeticShapes.alpha(0xFFFF5A12, (int)(255 * glow)));
        }
    }

    private static void stars(float s, float[][] tips, float age)
    {
        for (int i = 0; i < 16; ++i)
        {
            float seed = (float)i * 0.618F;
            float travel = (age * 0.004F + seed) % 1.0F;
            int f = i % 4;
            // drifting out from the wrist towards a finger tip
            float x = WRIST[0] * 0.6F + (tips[f][0] - WRIST[0] * 0.6F) * travel * 0.9F;
            float y = WRIST[1] + 0.25F + (tips[f][1] - WRIST[1] - 0.25F) * travel * 0.9F;
            float twinkle = 0.4F + 0.6F * Math.abs(MathHelper.sin(age * 0.15F + (float)i));
            float fade = Math.min(1.0F, Math.min(travel * 6.0F, (1.0F - travel) * 6.0F));
            CosmeticShapes.diamond(s * x, y, 0.014F, 0.016F, 0.024F, CosmeticShapes.alpha(i % 3 == 0 ? 0xFFB8E0FF : 0xFFFFFFFF, (int)(255 * twinkle * fade)));
        }
    }

    private static void cyber(float s, float age, float beat)
    {
        int steel = 0xFF2E3542;
        int dark = 0xFF161A22;
        solid();

        // the arm as a thick frame
        armLine(s, 0xFF3A4252, 0.06F);

        // panels: rigid blades along the arm and fanning from the wrist
        for (int i = 0; i < 9; ++i)
        {
            float t = 0.1F + 0.85F * (float)i / 8.0F;
            float[] b = arm(t);
            panel(s, b[0], b[1], 98.0F - 26.0F * t, 0.48F + 0.1F * t, 0.12F, dark, steel);
        }

        for (int i = 0; i < 5; ++i)
        {
            float k = (float)i / 4.0F;
            panel(s, WRIST[0], WRIST[1], -18.0F + 78.0F * k, 0.68F - 0.14F * k, 0.11F, dark, steel);
        }

        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();

        // a light runs down each blade in turn, and the frame glows
        for (int i = 0; i < 5; ++i)
        {
            float k = (float)i / 4.0F;
            float a = (-18.0F + 78.0F * k) * 0.017453292F;
            float len = 0.68F - 0.14F * k;
            float run = (age * 0.03F + (float)i * 0.2F) % 1.0F;
            CosmeticShapes.line(s * WRIST[0], WRIST[1], 0.012F, s * (WRIST[0] + MathHelper.cos(a) * len), WRIST[1] + MathHelper.sin(a) * len, 0.012F, 0.012F, CosmeticShapes.alpha(0xFF20E0FF, 150));
            CosmeticShapes.diamond(s * (WRIST[0] + MathHelper.cos(a) * len * run), WRIST[1] + MathHelper.sin(a) * len * run, 0.014F, 0.02F, 0.03F, 0xFFBFFBFF);
        }

        armLine(s, CosmeticShapes.alpha(0xFF20E0FF, 200), 0.014F);
        CosmeticShapes.end();
    }

    /** A rigid rectangular blade. */
    private static void panel(float s, float bx, float by, float angle, float length, float width, int root, int tip)
    {
        float a = angle * 0.017453292F;
        float dx = MathHelper.cos(a);
        float dy = MathHelper.sin(a);
        float hw = width / 2.0F;
        float px = -dy * hw;
        float py = dx * hw;
        float tx = bx + dx * length;
        float ty = by + dy * length;
        CosmeticShapes.quad(p(s, bx - px, by - py, 0.0F), p(s, bx + px, by + py, 0.0F), p(s, tx + px * 0.6F, ty + py * 0.6F, 0.0F), p(s, tx - px, ty - py, 0.0F), root, root, tip, tip);
    }

    private static void dragon(float s, float age)
    {
        // the fingers slowly open and close with the idle flap
        float fold = 0.2F + 0.2F * MathHelper.sin(age * 0.07F);
        float[][] tips = fingers(fold);
        solid();
        membrane(s, tips, 0xFF5A1012, 0xFF8A1E1A);

        // scale bands across the membrane
        for (int band = 1; band < 4; ++band)
        {
            float k = (float)band / 4.0F;

            for (int i = 0; i < 3; ++i)
            {
                float x0 = WRIST[0] + (tips[i][0] - WRIST[0]) * k;
                float y0 = WRIST[1] + (tips[i][1] - WRIST[1]) * k;
                float x1 = WRIST[0] + (tips[i + 1][0] - WRIST[0]) * k;
                float y1 = WRIST[1] + (tips[i + 1][1] - WRIST[1]) * k;
                CosmeticShapes.line(s * x0, y0, 0.006F, s * x1, y1, 0.006F, 0.012F, 0xFFA8382A);
            }
        }

        bones(s, tips, 0xFF2A0C0A, 0.04F);

        // claws: one at the wrist and one on each finger tip
        CosmeticShapes.diamond(s * WRIST[0], WRIST[1] - 0.06F, 0.012F, 0.03F, 0.07F, 0xFFE0D2B0);

        for (float[] tip : tips)
        {
            CosmeticShapes.diamond(s * tip[0], tip[1], 0.012F, 0.018F, 0.03F, 0xFFD0C098);
        }

        CosmeticShapes.end();
    }
}
