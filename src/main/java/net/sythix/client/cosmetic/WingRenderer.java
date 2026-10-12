package net.sythix.client.cosmetic;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

/**
 * The eight Sythix wing sets, drawn in body space on the player's back. Each wing is built in a
 * flat plane: x goes outwards (mirrored for the left wing), y goes down (so up is negative).
 */
public final class WingRenderer
{
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
        float flap = MathHelper.sin(age * 0.15F * speed) * (dragon ? 10.0F : 14.0F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, sneaking ? 0.35F : 0.2F, 0.16F);

        if (sneaking)
        {
            GlStateManager.rotate(28.0F, 1.0F, 0.0F, 0.0F);
        }

        GlStateManager.scale(size, size, size);

        for (int side = -1; side <= 1; side += 2)
        {
            GlStateManager.pushMatrix();
            GlStateManager.rotate((float)side * (22.0F + flap), 0.0F, 1.0F, 0.0F);
            wing(style, (float)side, age, flap);
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();
    }

    private static void wing(String style, float s, float age, float flap)
    {
        float pulse = 0.6F + 0.4F * MathHelper.sin(age * 0.12F);

        switch (style)
        {
            case "Void Wings":
                solid();
                feathers(s, 7, 0.95F, 0.55F, 0.12F, 0.075F, 0xFF100914, 0xFF2C1846, true, age);
                CosmeticShapes.end();
                additive();
                featherEdges(s, 7, 0.95F, 0.55F, 0.12F, CosmeticShapes.alpha(0xFFA040FF, (int)(200 * pulse)), true);
                CosmeticShapes.end();
                break;
            case "Celestial Wings":
                solid();
                feathers(s, 9, 1.05F, 0.6F, 0.14F, 0.07F, 0xFFF4F2FA, 0xFFE3DCEF, false, age);
                coverts(s, 0xFFFFFFFF);
                CosmeticShapes.end();
                additive();
                tips(s, 9, 1.05F, 0.6F, 0.14F, CosmeticShapes.alpha(0xFFFFD25A, (int)(170 + 80 * pulse)));
                CosmeticShapes.end();
                break;
            case "Cyber Wings":
                cyber(s, age);
                break;
            case "Frost Wings":
                frost(s, age);
                break;
            case "Infernal Wings":
                solid();
                membrane(s, 0xFF1C0907, 0xFF3A0E08, 0.0F);
                CosmeticShapes.end();
                additive();
                veins(s, age);
                CosmeticShapes.end();
                break;
            case "Astral Wings":
                translucent();
                membrane(s, 0x9A2A1468, 0x701440A0, 0.0F);
                CosmeticShapes.end();
                additive();
                drift(s, age);
                CosmeticShapes.end();
                break;
            case "Phantom Wings":
                translucent();
                feathers(s, 7, 1.0F, 0.6F, 0.13F, 0.08F, CosmeticShapes.alpha(0xFFB060FF, (int)(170 + 50 * pulse)), CosmeticShapes.alpha(0xFF7A30E8, (int)(90 + 50 * pulse)), false, age);
                CosmeticShapes.end();
                additive();
                featherEdges(s, 7, 1.0F, 0.6F, 0.13F, CosmeticShapes.alpha(0xFFE0C0FF, (int)(160 * pulse)), false);
                CosmeticShapes.end();
                break;
            default:
                dragon(s, age, flap);
        }
    }

    private static void solid()
    {
        CosmeticShapes.begin(false);
        GlStateManager.disableCull();
    }

    private static void translucent()
    {
        CosmeticShapes.begin(true);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
    }

    private static void additive()
    {
        CosmeticShapes.beginAdditive();
    }

    private static float[] p(float s, float x, float y)
    {
        return new float[] {s * x, y, 0.0F};
    }

    /** Where feather i of n sits on the wing's top bone (root near the back, tip far out and up). */
    private static float[] bone(int i, int n, float reach, float lift)
    {
        float k = n <= 1 ? 1.0F : (float)i / (float)(n - 1);
        return new float[] {0.04F + reach * k, -0.05F - lift * k * (1.0F - 0.35F * k)};
    }

    /** Feathers hanging from the bone, longest at the tip; 'jagged' breaks them into shards. */
    private static void feathers(float s, int n, float reach, float lift, float rootLen, float width, int root, int tip, boolean jagged, float age)
    {
        for (int i = 0; i < n; ++i)
        {
            float k = (float)i / (float)(n - 1);
            float[] b = bone(i, n, reach, lift);
            float len = rootLen + 0.5F * k + (jagged && i % 2 == 1 ? -0.08F : 0.0F);
            float dx = 0.15F + 0.35F * k;
            float dy = 1.0F;
            float norm = (float)Math.sqrt(dx * dx + dy * dy);
            dx /= norm;
            dy /= norm;
            float w = width * (1.0F - 0.3F * k);
            float ex = b[0] + dx * len;
            float ey = b[1] + dy * len;
            int c0 = root;
            int c1 = CosmeticShapes.mix(root, tip, 0.5F);

            if (jagged)
            {
                // a fractured feather: two pieces with a small gap and a slight twist
                float mx = b[0] + dx * len * 0.55F;
                float my = b[1] + dy * len * 0.55F;
                CosmeticShapes.quad(p(s, b[0] - w, b[1]), p(s, b[0] + w, b[1]), p(s, mx + w * 0.8F, my), p(s, mx - w * 0.6F, my - 0.01F), c0, c0, c1, c1);
                CosmeticShapes.quad(p(s, mx - w * 0.5F, my + 0.03F), p(s, mx + w * 0.8F, my + 0.02F), p(s, ex + w * 0.2F, ey), p(s, ex - w * 0.3F, ey - 0.02F), c1, c1, tip, tip);
            }
            else
            {
                CosmeticShapes.quad(p(s, b[0] - w, b[1]), p(s, b[0] + w, b[1]), p(s, ex + w * 0.35F, ey), p(s, ex - w * 0.35F, ey), c0, c0, tip, tip);
            }
        }
    }

    private static void featherEdges(float s, int n, float reach, float lift, float rootLen, int color, boolean bone)
    {
        for (int i = 0; i < n; ++i)
        {
            float k = (float)i / (float)(n - 1);
            float[] b = bone(i, n, reach, lift);
            float len = rootLen + 0.5F * k;
            float dx = 0.15F + 0.35F * k;
            float norm = (float)Math.sqrt(dx * dx + 1.0F);
            CosmeticShapes.line(s * b[0], b[1], 0.0F, s * (b[0] + dx / norm * len), b[1] + len / norm, 0.0F, 0.012F, color);
        }

        if (bone)
        {
            float[] a = bone(0, n, reach, lift);

            for (int i = 1; i < n; ++i)
            {
                float[] b = bone(i, n, reach, lift);
                CosmeticShapes.line(s * a[0], a[1], 0.0F, s * b[0], b[1], 0.0F, 0.02F, color);
                a = b;
            }
        }
    }

    private static void tips(float s, int n, float reach, float lift, float rootLen, int color)
    {
        for (int i = 2; i < n; ++i)
        {
            float k = (float)i / (float)(n - 1);
            float[] b = bone(i, n, reach, lift);
            float len = rootLen + 0.5F * k;
            float dx = 0.15F + 0.35F * k;
            float norm = (float)Math.sqrt(dx * dx + 1.0F);
            CosmeticShapes.diamond(s * (b[0] + dx / norm * len), b[1] + len / norm, 0.0F, 0.025F, 0.035F, color);
        }
    }

    /** Short covering feathers along the bone (on top of the long ones). */
    private static void coverts(float s, int color)
    {
        for (int i = 1; i < 6; ++i)
        {
            float[] b = bone(i, 6, 0.75F, 0.45F);
            CosmeticShapes.quad(p(s, b[0] - 0.08F, b[1] - 0.01F), p(s, b[0] + 0.08F, b[1] - 0.01F), p(s, b[0] + 0.06F, b[1] + 0.16F), p(s, b[0] - 0.02F, b[1] + 0.18F), color, color, CosmeticShapes.mix(color, 0xFFD8D0E8, 0.6F), CosmeticShapes.mix(color, 0xFFD8D0E8, 0.6F));
        }
    }

    /** A bat-like membrane between finger bones; 'fold' (0-1) pulls the fingers in. */
    private static final float[][] FINGERS = {{0.92F, -0.42F}, {1.0F, 0.02F}, {0.78F, 0.38F}, {0.38F, 0.55F}};
    private static final float[][] DIPS = {{0.72F, -0.1F}, {0.7F, 0.24F}, {0.42F, 0.38F}};

    private static void membrane(float s, int inner, int outer, float fold)
    {
        for (int i = 0; i < 3; ++i)
        {
            float[] a = finger(i, fold);
            float[] b = finger(i + 1, fold);
            CosmeticShapes.quad(p(s, 0.03F, 0.0F), p(s, a[0], a[1]), p(s, DIPS[i][0] * (1.0F - fold * 0.2F), DIPS[i][1]), p(s, b[0], b[1]), inner, outer, CosmeticShapes.mix(outer, inner, 0.5F), outer);
        }
    }

    private static float[] finger(int i, float fold)
    {
        return new float[] {FINGERS[i][0] * (1.0F - fold * 0.25F), FINGERS[i][1] * (1.0F - fold * 0.1F)};
    }

    private static void veins(float s, float age)
    {
        for (int i = 0; i < 4; ++i)
        {
            // lava flows out along each finger: segments brighten one after another
            for (int seg = 0; seg < 6; ++seg)
            {
                float k0 = (float)seg / 6.0F;
                float k1 = (float)(seg + 1) / 6.0F;
                float flow = 0.35F + 0.65F * (float)Math.pow(0.5F + 0.5F * MathHelper.sin(age * 0.25F - (float)seg * 0.9F + (float)i), 3.0D);
                int c = CosmeticShapes.alpha(CosmeticShapes.mix(0xFFFF3A0A, 0xFFFFC040, flow), (int)(255 * flow));
                CosmeticShapes.line(s * (0.03F + (FINGERS[i][0] - 0.03F) * k0), FINGERS[i][1] * k0, 0.0F, s * (0.03F + (FINGERS[i][0] - 0.03F) * k1), FINGERS[i][1] * k1, 0.0F, 0.022F, c);
            }
        }

        for (int i = 0; i < 3; ++i)
        {
            float glow = 0.4F + 0.3F * MathHelper.sin(age * 0.2F + (float)i * 2.0F);
            CosmeticShapes.line(s * 0.2F, 0.05F, 0.0F, s * DIPS[i][0], DIPS[i][1], 0.0F, 0.012F, CosmeticShapes.alpha(0xFFFF5A12, (int)(255 * glow)));
        }
    }

    private static void drift(float s, float age)
    {
        for (int i = 0; i < 14; ++i)
        {
            float seed = (float)i * 0.618F;
            float travel = (age * 0.004F + seed) % 1.0F;
            float x = 0.1F + travel * 0.85F;
            float y = -0.35F + ((seed * 7.3F) % 1.0F) * 0.8F * (1.0F - travel * 0.4F);
            float twinkle = 0.4F + 0.6F * Math.abs(MathHelper.sin(age * 0.15F + (float)i));
            float fade = Math.min(1.0F, Math.min(travel * 6.0F, (1.0F - travel) * 6.0F));
            CosmeticShapes.diamond(s * x, y, 0.0F, 0.016F, 0.022F, CosmeticShapes.alpha(i % 3 == 0 ? 0xFFB8E0FF : 0xFFFFFFFF, (int)(255 * twinkle * fade)));
        }

        CosmeticShapes.line(0.0F, 0.0F, 0.0F, s * FINGERS[0][0], FINGERS[0][1], 0.0F, 0.015F, CosmeticShapes.alpha(0xFF9A7CFF, 150));
    }

    private static void cyber(float s, float age)
    {
        solid();
        int steel = 0xFF2A303A;
        int dark = 0xFF161A22;
        // main arm in two segments
        CosmeticShapes.quad(p(s, 0.02F, -0.06F), p(s, 0.5F, -0.32F), p(s, 0.5F, -0.26F), p(s, 0.02F, 0.0F), steel, steel, steel, steel);
        CosmeticShapes.quad(p(s, 0.5F, -0.32F), p(s, 0.95F, -0.46F), p(s, 0.95F, -0.41F), p(s, 0.5F, -0.26F), steel, steel, steel, steel);

        // five hanging plates
        for (int i = 0; i < 5; ++i)
        {
            float k = (float)i / 4.0F;
            float x = 0.12F + k * 0.78F;
            float top = -0.06F - k * 0.38F;
            float len = 0.3F + k * 0.35F;
            CosmeticShapes.quad(p(s, x - 0.06F, top), p(s, x + 0.07F, top - 0.02F), p(s, x + 0.12F, top + len), p(s, x - 0.02F, top + len + 0.03F), dark, dark, steel, steel);
        }

        CosmeticShapes.end();
        additive();

        for (int i = 0; i < 5; ++i)
        {
            float k = (float)i / 4.0F;
            float x = 0.12F + k * 0.78F;
            float top = -0.06F - k * 0.38F;
            float len = 0.3F + k * 0.35F;
            // a light runs down each plate in turn
            float run = (age * 0.03F + (float)i * 0.2F) % 1.0F;
            int strip = CosmeticShapes.alpha(0xFF20E0FF, 150);
            CosmeticShapes.line(s * (x + 0.07F), top - 0.02F, 0.0F, s * (x + 0.12F), top + len, 0.0F, 0.012F, strip);
            float ly = top + len * run;
            CosmeticShapes.diamond(s * (x + 0.07F + 0.05F * run), ly, 0.0F, 0.02F, 0.03F, 0xFFBFFBFF);
        }

        CosmeticShapes.line(s * 0.02F, -0.03F, 0.0F, s * 0.95F, -0.435F, 0.0F, 0.01F, CosmeticShapes.alpha(0xFF20E0FF, 200));
        CosmeticShapes.end();
    }

    private static void frost(float s, float age)
    {
        translucent();

        // crystal shards fanning out, each a long thin diamond in the wing plane
        for (int i = 0; i < 8; ++i)
        {
            float k = (float)i / 7.0F;
            float[] b = bone(i, 8, 0.9F, 0.55F);
            float len = 0.22F + 0.45F * k;
            float dx = 0.2F + 0.4F * k;
            float norm = (float)Math.sqrt(dx * dx + 1.0F);
            float ex = b[0] + dx / norm * len;
            float ey = b[1] + len / norm;
            float mx = (b[0] + ex) / 2.0F;
            float my = (b[1] + ey) / 2.0F;
            int body = 0x90BFEFFF;
            int edge = 0x60E8FAFF;
            CosmeticShapes.quad(p(s, b[0], b[1]), p(s, mx + 0.05F, my - 0.02F), p(s, ex, ey), p(s, mx - 0.05F, my + 0.02F), edge, body, edge, body);
        }

        CosmeticShapes.end();
        additive();

        for (int i = 0; i < 8; ++i)
        {
            float k = (float)i / 7.0F;
            float[] b = bone(i, 8, 0.9F, 0.55F);
            float len = 0.22F + 0.45F * k;
            float dx = 0.2F + 0.4F * k;
            float norm = (float)Math.sqrt(dx * dx + 1.0F);
            float sparkle = (float)Math.pow(Math.abs(MathHelper.sin(age * 0.08F + (float)i * 1.9F)), 6.0D);
            CosmeticShapes.diamond(s * (b[0] + dx / norm * len * 0.7F), b[1] + len / norm * 0.7F, 0.0F, 0.03F * sparkle + 0.005F, 0.04F * sparkle + 0.005F, CosmeticShapes.alpha(0xFFFFFFFF, (int)(255 * sparkle)));
            CosmeticShapes.line(s * b[0], b[1], 0.0F, s * (b[0] + dx / norm * len), b[1] + len / norm, 0.0F, 0.008F, CosmeticShapes.alpha(0xFF9EE8FF, 120));
        }

        CosmeticShapes.end();
    }

    private static void dragon(float s, float age, float flap)
    {
        // the fingers open and close a little with the slow flap
        float fold = 0.25F + 0.2F * MathHelper.sin(age * 0.07F);
        solid();
        membrane(s, 0xFF3A0A0C, 0xFF6E1414, fold);

        // scale bands across the membrane
        for (int band = 1; band < 4; ++band)
        {
            float k = (float)band / 4.0F;

            for (int i = 0; i < 3; ++i)
            {
                float[] a = finger(i, fold);
                float[] b = finger(i + 1, fold);
                CosmeticShapes.line(s * (0.03F + (a[0] - 0.03F) * k), a[1] * k, 0.001F, s * (0.03F + (b[0] - 0.03F) * k), b[1] * k, 0.001F, 0.01F, 0xFF8A2A1E);
            }
        }

        CosmeticShapes.end();
        CosmeticShapes.begin(false);

        // bones: an arm and four fingers, with a claw at the top
        for (int i = 0; i < 4; ++i)
        {
            float[] f = finger(i, fold);
            CosmeticShapes.line(s * 0.03F, 0.0F, 0.0F, s * f[0], f[1], 0.0F, 0.035F - (float)i * 0.005F, 0xFF240A08);
        }

        float[] top = finger(0, fold);
        CosmeticShapes.diamond(s * (top[0] + 0.04F), top[1] - 0.05F, 0.0F, 0.025F, 0.05F, 0xFFD8C8A8);
        CosmeticShapes.end();
    }
}
