package net.sythix.client.cosmetic;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

/**
 * Hats, drawn in head space (after bipedHead.postRender): the head is the box from y = -0.5
 * (top) to 0, x and z from -0.25 to 0.25. Up is -Y.
 */
public final class HatRenderer
{
    public static final String[] HATS = new String[] {"None", "Top Hat", "Crown", "Halo", "Horns", "Cat Ears", "Witch Hat", "Cap"};

    private HatRenderer()
    {
    }

    public static void draw(String hat, int color, float age)
    {
        if (hat == null || hat.equals("None"))
        {
            return;
        }

        boolean glow = hat.equals("Halo");
        CosmeticShapes.begin(glow);
        int dark = darker(color, 0.55F);

        switch (hat)
        {
            case "Top Hat":
                CosmeticShapes.box(-0.36F, -0.54F, -0.36F, 0.36F, -0.5F, 0.36F, color);
                CosmeticShapes.box(-0.23F, -0.62F, -0.23F, 0.23F, -0.54F, 0.23F, 0xFFD81820);
                CosmeticShapes.box(-0.22F, -0.98F, -0.22F, 0.22F, -0.62F, 0.22F, color);
                break;
            case "Crown":
                CosmeticShapes.box(-0.27F, -0.64F, -0.27F, 0.27F, -0.49F, 0.27F, color);

                for (int i = 0; i < 3; ++i)
                {
                    float o = -0.2F + (float)i * 0.2F;
                    CosmeticShapes.column(o, -0.24F, 0.035F, -0.76F, -0.64F, color);
                    CosmeticShapes.column(o, 0.24F, 0.035F, -0.76F, -0.64F, color);
                    CosmeticShapes.column(-0.24F, o, 0.035F, -0.76F, -0.64F, color);
                    CosmeticShapes.column(0.24F, o, 0.035F, -0.76F, -0.64F, color);
                }

                // Jewels on the front
                CosmeticShapes.box(-0.05F, -0.6F, -0.285F, 0.05F, -0.53F, -0.27F, 0xFFD81820);
                CosmeticShapes.box(-0.17F, -0.59F, -0.285F, -0.11F, -0.54F, -0.27F, 0xFF2F6BFF);
                CosmeticShapes.box(0.11F, -0.59F, -0.285F, 0.17F, -0.54F, -0.27F, 0xFF2F6BFF);
                break;
            case "Halo":
                GlStateManager.translate(0.0F, MathHelper.sin(age * 0.1F) * 0.03F, 0.0F);
                CosmeticShapes.ring(-0.72F, 0.3F, 0.06F, color);
                break;
            case "Horns":
                for (int side = -1; side <= 1; side += 2)
                {
                    float s = (float)side;
                    CosmeticShapes.box(s * 0.18F - 0.05F, -0.58F, -0.12F, s * 0.18F + 0.05F, -0.48F, -0.02F, color);
                    CosmeticShapes.box(s * 0.25F - 0.04F, -0.68F, -0.12F, s * 0.25F + 0.04F, -0.58F, -0.04F, color);
                    CosmeticShapes.box(s * 0.3F - 0.03F, -0.78F, -0.11F, s * 0.3F + 0.03F, -0.68F, -0.05F, dark);
                    CosmeticShapes.box(s * 0.31F - 0.02F, -0.86F, -0.1F, s * 0.31F + 0.02F, -0.78F, -0.06F, dark);
                }

                break;
            case "Cat Ears":
                for (int side = -1; side <= 1; side += 2)
                {
                    float s = (float)side;
                    CosmeticShapes.box(s * 0.17F - 0.08F, -0.58F, -0.05F, s * 0.17F + 0.08F, -0.49F, 0.02F, color);
                    CosmeticShapes.box(s * 0.17F - 0.055F, -0.65F, -0.05F, s * 0.17F + 0.055F, -0.58F, 0.02F, color);
                    CosmeticShapes.box(s * 0.17F - 0.03F, -0.7F, -0.05F, s * 0.17F + 0.03F, -0.65F, 0.02F, color);
                    // Pink inner ear
                    CosmeticShapes.box(s * 0.17F - 0.04F, -0.62F, -0.055F, s * 0.17F + 0.04F, -0.51F, -0.05F, 0xFFFF9EC8);
                }

                break;
            case "Witch Hat":
                CosmeticShapes.box(-0.42F, -0.54F, -0.42F, 0.42F, -0.5F, 0.42F, color);
                CosmeticShapes.box(-0.25F, -0.62F, -0.25F, 0.25F, -0.54F, 0.25F, 0xFFD81820);
                CosmeticShapes.box(-0.2F, -0.74F, -0.2F, 0.2F, -0.62F, 0.2F, color);
                CosmeticShapes.box(-0.15F, -0.86F, -0.13F, 0.15F, -0.74F, 0.17F, color);
                CosmeticShapes.box(-0.1F, -0.96F, -0.05F, 0.1F, -0.86F, 0.15F, color);
                CosmeticShapes.box(-0.05F, -1.03F, 0.04F, 0.05F, -0.96F, 0.14F, color);
                break;
            case "Cap":
                CosmeticShapes.box(-0.27F, -0.6F, -0.27F, 0.27F, -0.45F, 0.27F, color);
                CosmeticShapes.box(-0.25F, -0.48F, -0.48F, 0.25F, -0.45F, -0.27F, dark);
                break;
            default:
        }

        CosmeticShapes.end();
    }

    static int darker(int color, float f)
    {
        int r = (int)((float)((color >> 16) & 255) * f);
        int g = (int)((float)((color >> 8) & 255) * f);
        int b = (int)((float)(color & 255) * f);
        return (color & 0xFF000000) | r << 16 | g << 8 | b;
    }
}
