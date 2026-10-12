package net.sythix.client.cosmetic;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

/**
 * A small companion sitting on the left shoulder, in layer space (shoulder top at y = 0, like
 * the vanilla parrot).
 */
public final class BuddyRenderer
{
    public static final String[] BUDDIES = new String[] {"None", "Slime", "Cube", "Ghost"};

    private BuddyRenderer()
    {
    }

    public static void draw(String buddy, int color, float age, boolean sneaking)
    {
        if (buddy == null || buddy.equals("None"))
        {
            return;
        }

        CosmeticShapes.begin(false);
        GlStateManager.translate(0.4F, sneaking ? 0.2F : 0.0F, 0.0F);
        float bob = buddy.equals("Ghost") ? -0.15F + MathHelper.sin(age * 0.12F) * 0.05F : -Math.abs(MathHelper.sin(age * 0.15F)) * 0.06F;
        GlStateManager.translate(0.0F, bob, 0.0F);
        GlStateManager.rotate(MathHelper.sin(age * 0.05F) * 10.0F, 0.0F, 1.0F, 0.0F);

        switch (buddy)
        {
            case "Slime":
                // Inner core first, then the see-through outer jelly
                CosmeticShapes.box(-0.08F, -0.2F, -0.08F, 0.08F, -0.05F, 0.08F, HatRenderer.darker(color, 0.6F) | 0xFF000000);
                eyes(-0.14F);
                CosmeticShapes.box(-0.14F, -0.27F, -0.14F, 0.14F, 0.0F, 0.14F, (color & 0xFFFFFF) | 0x90000000);
                break;
            case "Cube":
                CosmeticShapes.box(-0.12F, -0.24F, -0.12F, 0.12F, 0.0F, 0.12F, color | 0xFF000000);
                eyes(-0.125F);
                break;
            case "Ghost":
                CosmeticShapes.box(-0.11F, -0.26F, -0.11F, 0.11F, -0.04F, 0.11F, (color & 0xFFFFFF) | 0xC0000000);
                CosmeticShapes.box(-0.11F, -0.04F, -0.11F, -0.04F, 0.02F, 0.11F, (color & 0xFFFFFF) | 0xC0000000);
                CosmeticShapes.box(0.04F, -0.04F, -0.11F, 0.11F, 0.02F, 0.11F, (color & 0xFFFFFF) | 0xC0000000);
                eyes(-0.115F);
                break;
            default:
        }

        CosmeticShapes.end();
    }

    private static void eyes(float front)
    {
        CosmeticShapes.box(-0.07F, -0.17F, front - 0.01F, -0.03F, -0.12F, front, 0xFF101014);
        CosmeticShapes.box(0.03F, -0.17F, front - 0.01F, 0.07F, -0.12F, front, 0xFF101014);
    }
}
