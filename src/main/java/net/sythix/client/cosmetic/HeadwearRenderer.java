package net.sythix.client.cosmetic;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

/**
 * The 15 Sythix headwear pieces, drawn in head space (after bipedHead.postRender): the head is the
 * box from y = -0.5 (top) to 0, x and z from -0.25 to 0.25. Up is -Y, the face is at -Z.
 * Solid parts use CosmeticShapes.begin(), glowing parts beginAdditive().
 */
public final class HeadwearRenderer
{
    private static final int VOID = 0xFFA040FF;
    private static final int CYAN = 0xFF20E0FF;
    private static final int GOLD = 0xFFFFC63A;

    private HeadwearRenderer()
    {
    }

    public static void draw(String name, float age)
    {
        if (name == null || CosmeticCatalog.NONE.equals(name))
        {
            return;
        }

        float pulse = 0.6F + 0.4F * MathHelper.sin(age * 0.15F);

        switch (name)
        {
            case "Void Headband":
                voidHeadband(age, pulse);
                break;
            case "Cyber Band":
                cyberBand(age);
                break;
            case "Shadow Ninja":
                shadowNinja(age);
                break;
            case "Rift Crown":
                riftCrown(age, pulse);
                break;
            case "Phantom Ears":
                phantomEars(pulse);
                break;
            case "Solar Halo":
                solarHalo(age, pulse);
                break;
            case "Frost Horns":
                horns(0xD098DCFF, 0xFFBDEFFF, true, pulse);
                break;
            case "Infernal Horns":
                horns(0xFF2A0C0C, 0xFFFF6A1A, false, pulse);
                break;
            case "Holo Visor":
                holoVisor(age);
                break;
            case "Arctic Fox":
                arcticFox();
                break;
            case "Astral Hat":
                astralHat(age);
                break;
            case "Pulse Headphones":
                headphones(age);
                break;
            case "Champion Laurel":
                laurel();
                break;
            case "Rift Shards":
                riftShards(age);
                break;
            case "Overlord Crown":
                overlordCrown(pulse);
                break;
            default:
        }
    }

    /** A band around the head between two heights, slightly bigger than the head. */
    private static void band(float top, float bottom, float out, int color)
    {
        CosmeticShapes.box(-0.25F - out, top, -0.25F - out, 0.25F + out, bottom, -0.25F, color);
        CosmeticShapes.box(-0.25F - out, top, 0.25F, 0.25F + out, bottom, 0.25F + out, color);
        CosmeticShapes.box(-0.25F - out, top, -0.25F, -0.25F, bottom, 0.25F, color);
        CosmeticShapes.box(0.25F, top, -0.25F, 0.25F + out, bottom, 0.25F, color);
    }

    /** Two cloth tails hanging from the back of the head, swaying. */
    private static void tails(float age, int color, int length, float y)
    {
        for (int side = -1; side <= 1; side += 2)
        {
            float x = (float)side * 0.06F;
            float z = 0.27F;
            float yy = y;

            for (int i = 0; i < length; ++i)
            {
                float sway = MathHelper.sin(age * 0.18F - (float)i * 0.7F + (float)side) * 0.025F * (float)(i + 1);
                float nx = x + (float)side * 0.035F + sway;
                float nz = z + 0.06F;
                float ny = yy + 0.07F;
                CosmeticShapes.box(Math.min(x, nx) - 0.03F, yy, Math.min(z, nz), Math.max(x, nx) + 0.03F, ny, Math.max(z, nz) + 0.01F, color);
                x = nx;
                z = nz;
                yy = ny;
            }
        }
    }

    private static void voidHeadband(float age, float pulse)
    {
        CosmeticShapes.begin(false);
        band(-0.32F, -0.24F, 0.02F, 0xFF121018);
        tails(age, 0xFF1C1028, 5, -0.31F);
        CosmeticShapes.box(-0.07F, -0.335F, -0.285F, 0.07F, -0.225F, -0.27F, 0xFF2A1840);
        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        CosmeticShapes.diamond(0.0F, -0.28F, -0.29F, 0.045F, 0.06F, CosmeticShapes.alpha(VOID, (int)(255 * pulse)));
        CosmeticShapes.diamond(0.0F, -0.28F, -0.29F, 0.08F, 0.1F, CosmeticShapes.alpha(VOID, (int)(70 * pulse)));
        CosmeticShapes.end();
    }

    private static void cyberBand(float age)
    {
        CosmeticShapes.begin(false);
        band(-0.33F, -0.24F, 0.02F, 0xFF1A1F2A);
        // ear modules
        CosmeticShapes.box(-0.3F, -0.34F, -0.06F, -0.27F, -0.2F, 0.08F, 0xFF262E3C);
        CosmeticShapes.box(0.27F, -0.34F, -0.06F, 0.3F, -0.2F, 0.08F, 0xFF262E3C);
        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        CosmeticShapes.box(-0.27F, -0.29F, -0.275F, 0.27F, -0.275F, -0.27F, CosmeticShapes.alpha(CYAN, 170));
        // the scanning light runs left and right across the front
        float scan = MathHelper.sin(age * 0.12F) * 0.22F;
        CosmeticShapes.box(scan - 0.04F, -0.31F, -0.285F, scan + 0.04F, -0.255F, -0.27F, 0xFFBFFBFF);
        CosmeticShapes.box(scan - 0.09F, -0.32F, -0.29F, scan + 0.09F, -0.245F, -0.27F, CosmeticShapes.alpha(CYAN, 90));

        for (int side = -1; side <= 1; side += 2)
        {
            float blink = 0.5F + 0.5F * MathHelper.sin(age * 0.3F + (float)side);
            CosmeticShapes.box((float)side * 0.3F - 0.012F, -0.3F, -0.03F, (float)side * 0.3F + 0.012F, -0.24F, 0.05F, CosmeticShapes.alpha(CYAN, (int)(255 * blink)));
        }

        CosmeticShapes.end();
    }

    private static void shadowNinja(float age)
    {
        CosmeticShapes.begin(false);
        band(-0.34F, -0.25F, 0.02F, 0xFF101014);
        CosmeticShapes.box(-0.1F, -0.335F, -0.285F, 0.1F, -0.255F, -0.27F, 0xFF8C93A0);
        CosmeticShapes.box(-0.06F, -0.31F, -0.29F, 0.06F, -0.28F, -0.285F, 0xFF3A3F48);
        tails(age, 0xFF15151C, 7, -0.33F);
        CosmeticShapes.end();
    }

    private static void riftCrown(float age, float pulse)
    {
        for (int pass = 0; pass < 2; ++pass)
        {
            if (pass == 0)
            {
                CosmeticShapes.beginTranslucent();
                CosmeticShapes.ring(-0.56F, 0.3F, 0.025F, 0xD07A2AD0);
            }
            else
            {
                CosmeticShapes.beginAdditive();
            }

            GlStateManager.rotate(age * 2.0F, 0.0F, 1.0F, 0.0F);

            for (int i = 0; i < 6; ++i)
            {
                float a = (float)i / 6.0F * (float)Math.PI * 2.0F;
                float bob = MathHelper.sin(age * 0.12F + (float)i) * 0.025F;
                float tall = i % 2 == 0 ? 0.12F : 0.08F;

                if (pass == 0)
                {
                    CosmeticShapes.diamond(MathHelper.cos(a) * 0.29F, -0.66F + bob, MathHelper.sin(a) * 0.29F, 0.045F, tall, 0xE08A30E8);
                }
                else
                {
                    CosmeticShapes.diamond(MathHelper.cos(a) * 0.29F, -0.66F + bob, MathHelper.sin(a) * 0.29F, 0.02F, tall * 0.6F, CosmeticShapes.alpha(0xFFE8C8FF, (int)(200 * pulse)));
                    CosmeticShapes.diamond(MathHelper.cos(a) * 0.29F, -0.66F + bob, MathHelper.sin(a) * 0.29F, 0.07F, tall * 1.5F, CosmeticShapes.alpha(VOID, (int)(50 * pulse)));
                }
            }

            CosmeticShapes.end();
        }
    }

    private static void ears(int color, int inner, float tall)
    {
        for (int side = -1; side <= 1; side += 2)
        {
            float s = (float)side;
            CosmeticShapes.box(s * 0.17F - 0.08F, -0.58F, -0.04F, s * 0.17F + 0.08F, -0.49F, 0.03F, color);
            CosmeticShapes.box(s * 0.18F - 0.055F, -0.58F - tall * 0.08F, -0.04F, s * 0.18F + 0.055F, -0.58F, 0.03F, color);
            CosmeticShapes.box(s * 0.19F - 0.03F, -0.58F - tall * 0.15F, -0.04F, s * 0.19F + 0.03F, -0.58F - tall * 0.08F, 0.03F, color);

            if (inner != 0)
            {
                CosmeticShapes.box(s * 0.175F - 0.04F, -0.6F, -0.045F, s * 0.175F + 0.04F, -0.5F, -0.04F, inner);
            }
        }
    }

    private static void phantomEars(float pulse)
    {
        CosmeticShapes.beginTranslucent();
        ears(CosmeticShapes.alpha(0xFF8A3AE8, (int)(120 + 70 * pulse)), CosmeticShapes.alpha(0xFFD8B0FF, (int)(150 + 60 * pulse)), 1.2F);
        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        ears(CosmeticShapes.alpha(0xFF9A50FF, (int)(50 * pulse)), 0, 1.2F);
        CosmeticShapes.end();
    }

    private static void solarHalo(float age, float pulse)
    {
        float bob = MathHelper.sin(age * 0.1F) * 0.02F;
        CosmeticShapes.begin(true);
        GlStateManager.translate(0.0F, bob, 0.0F);
        CosmeticShapes.ring(-0.72F, 0.3F, 0.05F, 0xFFE8A820);
        GlStateManager.rotate(age * 1.5F, 0.0F, 1.0F, 0.0F);

        for (int i = 0; i < 12; ++i)
        {
            float a = (float)i / 12.0F * (float)Math.PI * 2.0F;
            float len = i % 2 == 0 ? 0.12F : 0.07F;
            CosmeticShapes.line(MathHelper.cos(a) * 0.34F, -0.72F, MathHelper.sin(a) * 0.34F, MathHelper.cos(a) * (0.34F + len), -0.72F, MathHelper.sin(a) * (0.34F + len), 0.022F, 0xFFF2C040);
        }

        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        GlStateManager.translate(0.0F, bob, 0.0F);
        CosmeticShapes.ring(-0.72F, 0.36F, 0.12F, CosmeticShapes.alpha(GOLD, (int)(60 * pulse)));
        CosmeticShapes.end();
    }

    private static void horns(int body, int tip, boolean ice, float pulse)
    {
        if (ice)
        {
            CosmeticShapes.beginTranslucent();
        }
        else
        {
            CosmeticShapes.begin(false);
        }

        for (int side = -1; side <= 1; side += 2)
        {
            float s = (float)side;
            // curving up and outwards, getting thinner
            CosmeticShapes.box(s * 0.17F - 0.05F, -0.57F, -0.13F, s * 0.17F + 0.05F, -0.47F, -0.03F, body);
            CosmeticShapes.box(s * 0.23F - 0.04F, -0.67F, -0.12F, s * 0.23F + 0.04F, -0.57F, -0.04F, body);
            CosmeticShapes.box(s * 0.28F - 0.03F, -0.77F, -0.1F, s * 0.28F + 0.03F, -0.67F, -0.04F, body);
            CosmeticShapes.box(s * 0.3F - 0.02F, -0.85F, -0.07F, s * 0.3F + 0.02F, -0.77F, -0.03F, ice ? body : tip);
        }

        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();

        for (int side = -1; side <= 1; side += 2)
        {
            CosmeticShapes.diamond((float)side * 0.3F, -0.86F, -0.05F, 0.035F, 0.05F, CosmeticShapes.alpha(tip, (int)(255 * pulse)));
        }

        CosmeticShapes.end();
    }

    private static void holoVisor(float age)
    {
        CosmeticShapes.begin(false);
        CosmeticShapes.box(-0.29F, -0.31F, -0.27F, -0.26F, -0.25F, 0.12F, 0xFF2A3440);
        CosmeticShapes.box(0.26F, -0.31F, -0.27F, 0.29F, -0.25F, 0.12F, 0xFF2A3440);
        CosmeticShapes.end();
        CosmeticShapes.beginTranslucent();
        CosmeticShapes.box(-0.28F, -0.33F, -0.31F, 0.28F, -0.19F, -0.29F, 0x7020C8F0);
        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        float scan = -0.33F + ((age * 0.02F) % 1.0F) * 0.14F;
        CosmeticShapes.box(-0.28F, scan, -0.315F, 0.28F, scan + 0.012F, -0.29F, CosmeticShapes.alpha(0xFFC8FFFF, 220));
        CosmeticShapes.box(-0.28F, -0.335F, -0.315F, 0.28F, -0.325F, -0.29F, CosmeticShapes.alpha(CYAN, 200));
        CosmeticShapes.end();
    }

    private static void arcticFox()
    {
        CosmeticShapes.begin(false);
        ears(0xFFF4F7FA, 0xFFFFC7D8, 1.3F);

        for (int side = -1; side <= 1; side += 2)
        {
            float s = (float)side;
            CosmeticShapes.box(s * 0.19F - 0.032F, -0.78F, -0.045F, s * 0.19F + 0.032F, -0.7F, 0.035F, 0xFF9AA2AC);
        }

        CosmeticShapes.end();
    }

    private static void astralHat(float age)
    {
        int navy = 0xFF161A4A;
        CosmeticShapes.begin(false);
        CosmeticShapes.box(-0.42F, -0.54F, -0.42F, 0.42F, -0.5F, 0.42F, navy);
        CosmeticShapes.box(-0.26F, -0.6F, -0.26F, 0.26F, -0.54F, 0.26F, 0xFFC8962A);
        CosmeticShapes.box(-0.22F, -0.74F, -0.22F, 0.22F, -0.6F, 0.22F, navy);
        CosmeticShapes.box(-0.16F, -0.86F, -0.14F, 0.16F, -0.74F, 0.18F, navy);
        CosmeticShapes.box(-0.1F, -0.96F, -0.04F, 0.1F, -0.86F, 0.18F, navy);
        CosmeticShapes.box(-0.05F, -1.02F, 0.08F, 0.05F, -0.96F, 0.2F, navy);
        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        float[][] stars = {{-0.15F, -0.68F, -0.225F}, {0.1F, -0.8F, -0.145F}, {-0.05F, -0.9F, -0.045F}, {0.2F, -0.66F, -0.225F}, {-0.225F, -0.7F, 0.05F}, {0.225F, -0.78F, 0.08F}};

        for (int i = 0; i < stars.length; ++i)
        {
            float twinkle = 0.4F + 0.6F * Math.abs(MathHelper.sin(age * 0.09F + (float)i * 1.3F));
            CosmeticShapes.diamond(stars[i][0], stars[i][1], stars[i][2], 0.018F, 0.025F, CosmeticShapes.alpha(0xFFFFF0B0, (int)(255 * twinkle)));
        }

        CosmeticShapes.end();
    }

    private static void headphones(float age)
    {
        int shell = 0xFF1E1E26;
        CosmeticShapes.begin(false);
        CosmeticShapes.box(-0.27F, -0.56F, -0.04F, 0.27F, -0.52F, 0.04F, shell);
        CosmeticShapes.box(-0.3F, -0.52F, -0.04F, -0.26F, -0.32F, 0.04F, shell);
        CosmeticShapes.box(0.26F, -0.52F, -0.04F, 0.3F, -0.32F, 0.04F, shell);
        CosmeticShapes.box(-0.34F, -0.34F, -0.1F, -0.26F, -0.16F, 0.1F, shell);
        CosmeticShapes.box(0.26F, -0.34F, -0.1F, 0.34F, -0.16F, 0.1F, shell);
        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        float beat = (float)Math.pow(Math.abs(MathHelper.sin(age * 0.2F)), 4.0D);

        for (int side = -1; side <= 1; side += 2)
        {
            int c = CosmeticShapes.hue(age * 0.01F + (side > 0 ? 0.5F : 0.0F));
            float x = (float)side * 0.345F;
            CosmeticShapes.box(x - 0.006F, -0.31F, -0.07F, x + 0.006F, -0.19F, 0.07F, CosmeticShapes.alpha(c, (int)(140 + 115 * beat)));
            CosmeticShapes.box(x - 0.01F, -0.34F - beat * 0.03F, -0.1F - beat * 0.03F, x + 0.01F, -0.16F + beat * 0.03F, 0.1F + beat * 0.03F, CosmeticShapes.alpha(c, (int)(60 * beat)));
        }

        CosmeticShapes.end();
    }

    private static void laurel()
    {
        CosmeticShapes.begin(true);
        int leaf = 0xFFE2B33A;
        int dark = 0xFFB0821C;

        // leaves round the sides and back, open at the front
        for (int i = 0; i < 18; ++i)
        {
            float a = (float)Math.PI * (0.18F + 1.64F * (float)i / 17.0F) + (float)Math.PI / 2.0F;
            float x = MathHelper.cos(a) * 0.29F;
            float z = MathHelper.sin(a) * 0.29F;
            float y = -0.36F - (i % 2 == 0 ? 0.02F : -0.015F);
            CosmeticShapes.diamond(x, y, z, 0.035F, 0.05F, i % 2 == 0 ? leaf : dark);
        }

        CosmeticShapes.ring(-0.35F, 0.285F, 0.012F, dark);
        CosmeticShapes.end();
    }

    private static void riftShards(float age)
    {
        for (int pass = 0; pass < 2; ++pass)
        {
            if (pass == 0)
            {
                CosmeticShapes.beginTranslucent();
            }
            else
            {
                CosmeticShapes.beginAdditive();
            }

            shards(age, pass == 0);
            CosmeticShapes.end();
        }
    }

    private static void shards(float age, boolean body)
    {
        for (int i = 0; i < 5; ++i)
        {
            float speed = 1.0F + (float)i * 0.25F;
            float a = age * 0.06F * speed + (float)i * 1.25F;
            float r = 0.4F + (float)(i % 2) * 0.07F;
            float y = -0.3F - (float)i * 0.07F + MathHelper.sin(age * 0.1F + (float)i) * 0.03F;
            int c = i % 2 == 0 ? 0xFF8A30E8 : 0xFF18B8E0;

            if (body)
            {
                CosmeticShapes.diamond(MathHelper.cos(a) * r, y, MathHelper.sin(a) * r, 0.04F, 0.08F, CosmeticShapes.alpha(c, 225));
            }
            else
            {
                CosmeticShapes.diamond(MathHelper.cos(a) * r, y, MathHelper.sin(a) * r, 0.018F, 0.045F, 0xC0FFFFFF);
                CosmeticShapes.diamond(MathHelper.cos(a) * r, y, MathHelper.sin(a) * r, 0.065F, 0.12F, CosmeticShapes.alpha(c, 45));
            }
        }
    }

    private static void overlordCrown(float pulse)
    {
        int metal = 0xFF141218;
        int trim = 0xFFC79318;
        CosmeticShapes.begin(false);
        CosmeticShapes.box(-0.28F, -0.62F, -0.28F, 0.28F, -0.49F, 0.28F, metal);
        CosmeticShapes.box(-0.285F, -0.52F, -0.285F, 0.285F, -0.49F, 0.285F, trim);

        for (int i = 0; i < 4; ++i)
        {
            float o = -0.21F + (float)i * 0.14F;
            float tall = i == 1 || i == 2 ? 0.24F : 0.16F;
            CosmeticShapes.column(o, -0.25F, 0.035F, -0.62F - tall, -0.62F, metal);
            CosmeticShapes.column(o, 0.25F, 0.035F, -0.62F - tall * 0.7F, -0.62F, metal);
            CosmeticShapes.column(-0.25F, o, 0.035F, -0.62F - tall * 0.8F, -0.62F, metal);
            CosmeticShapes.column(0.25F, o, 0.035F, -0.62F - tall * 0.8F, -0.62F, metal);
        }

        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        int red = CosmeticShapes.alpha(0xFFFF2030, (int)(180 + 75 * pulse));
        CosmeticShapes.diamond(0.0F, -0.56F, -0.29F, 0.045F, 0.05F, red);
        CosmeticShapes.diamond(-0.15F, -0.56F, -0.29F, 0.03F, 0.035F, red);
        CosmeticShapes.diamond(0.15F, -0.56F, -0.29F, 0.03F, 0.035F, red);
        CosmeticShapes.diamond(0.0F, -0.56F, -0.29F, 0.09F, 0.1F, CosmeticShapes.alpha(0xFFFF2030, (int)(60 * pulse)));
        CosmeticShapes.end();
    }
}
