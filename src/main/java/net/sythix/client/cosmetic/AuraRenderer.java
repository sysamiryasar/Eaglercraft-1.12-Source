package net.sythix.client.cosmetic;

import java.util.Random;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * The eight Sythix auras: shapes drawn around the player in model space (y = 0 at the neck,
 * 1.5 at the feet, up is -Y) plus particles spawned in the world every tick.
 */
public final class AuraRenderer
{
    private static final float FEET = 1.48F;

    private AuraRenderer()
    {
    }

    /** Draws the aura's shapes. firstPerson leaves out the parts that would cover the view. */
    public static void draw(String aura, float age, boolean firstPerson)
    {
        if (aura == null || CosmeticCatalog.NONE.equals(aura))
        {
            return;
        }

        float pulse = 0.6F + 0.4F * MathHelper.sin(age * 0.12F);

        switch (aura)
        {
            case "Void Aura":
                voidAura(age, pulse, firstPerson);
                break;
            case "Thunder Core":
                if (!firstPerson)
                {
                    thunder(age, pulse);
                }

                break;
            case "Inferno Aura":
                inferno(age);
                break;
            case "Absolute Zero":
                absoluteZero(age, pulse);
                break;
            case "Divine Radiance":
                divine(age, pulse, firstPerson);
                break;
            case "Glitch Entity":
                if (!firstPerson)
                {
                    glitch(age);
                }

                break;
            case "Shadowborn":
                shadowborn(age, pulse);
                break;
            case "Prism Bloom":
                prism(age);
                break;
            default:
        }
    }

    private static void arcs(float y, float radius, float width, int segments, float turn, int color)
    {
        for (int i = 0; i < segments; ++i)
        {
            float a0 = turn + (float)i / (float)segments * (float)Math.PI * 2.0F;
            float a1 = a0 + (float)Math.PI * 2.0F / (float)segments * 0.6F;
            float ri = radius - width;
            CosmeticShapes.quad(new float[] {MathHelper.cos(a0) * ri, y, MathHelper.sin(a0) * ri}, new float[] {MathHelper.cos(a0) * radius, y, MathHelper.sin(a0) * radius}, new float[] {MathHelper.cos(a1) * radius, y, MathHelper.sin(a1) * radius}, new float[] {MathHelper.cos(a1) * ri, y, MathHelper.sin(a1) * ri}, color, color, color, color);
        }
    }

    private static void voidAura(float age, float pulse, boolean firstPerson)
    {
        CosmeticShapes.beginAdditive();
        CosmeticShapes.ring(FEET, 0.78F, 0.05F, CosmeticShapes.alpha(0xFF7A20E0, (int)(150 * pulse)));
        arcs(FEET - 0.01F, 0.68F, 0.06F, 6, age * 0.04F, CosmeticShapes.alpha(0xFFC070FF, 200));
        arcs(FEET - 0.01F, 0.55F, 0.03F, 4, -age * 0.06F, CosmeticShapes.alpha(0xFF9A3CFF, 160));

        if (!firstPerson)
        {
            for (int i = 0; i < 3; ++i)
            {
                float a = age * 0.05F + (float)i * 2.094F;
                CosmeticShapes.diamond(MathHelper.cos(a) * 0.62F, 0.55F + MathHelper.sin(age * 0.1F + (float)i) * 0.15F, MathHelper.sin(a) * 0.62F, 0.05F, 0.07F, CosmeticShapes.alpha(0xFF5A10B0, 220));
                CosmeticShapes.diamond(MathHelper.cos(a) * 0.62F, 0.55F + MathHelper.sin(age * 0.1F + (float)i) * 0.15F, MathHelper.sin(a) * 0.62F, 0.1F, 0.12F, CosmeticShapes.alpha(0xFFA040FF, 50));
            }
        }

        CosmeticShapes.end();
    }

    private static void thunder(float age, float pulse)
    {
        CosmeticShapes.beginAdditive();
        Random rand = new Random((long)(age / 2.0F) * 31L);

        for (int arc = 0; arc < 3; ++arc)
        {
            float a = rand.nextFloat() * (float)Math.PI * 2.0F;
            float x = MathHelper.cos(a) * 0.6F;
            float z = MathHelper.sin(a) * 0.6F;
            float y = FEET;
            float tx = (rand.nextFloat() - 0.5F) * 0.3F;
            float tz = (rand.nextFloat() - 0.5F) * 0.3F;
            float ty = 0.2F + rand.nextFloat() * 0.3F;
            int steps = 6;

            for (int i = 1; i <= steps; ++i)
            {
                float k = (float)i / (float)steps;
                float jitter = i == steps ? 0.0F : 0.12F;
                float nx = x + (tx - x) / (float)(steps - i + 1) + (rand.nextFloat() - 0.5F) * jitter;
                float nz = z + (tz - z) / (float)(steps - i + 1) + (rand.nextFloat() - 0.5F) * jitter;
                float ny = FEET + (ty - FEET) * k;
                CosmeticShapes.line(x, y, z, nx, ny, nz, 0.025F, 0xFFCFF8FF);
                CosmeticShapes.line(x, y, z, nx, ny, nz, 0.07F, CosmeticShapes.alpha(0xFF3AA8FF, 70));
                x = nx;
                y = ny;
                z = nz;
            }
        }

        CosmeticShapes.diamond(0.0F, 0.35F, 0.0F, 0.12F * pulse, 0.12F * pulse, CosmeticShapes.alpha(0xFF6AD0FF, 70));
        CosmeticShapes.ring(FEET, 0.65F, 0.03F, CosmeticShapes.alpha(0xFF6AD0FF, (int)(120 * pulse)));
        CosmeticShapes.end();
    }

    private static void inferno(float age)
    {
        CosmeticShapes.beginAdditive();
        CosmeticShapes.ring(FEET, 0.7F, 0.08F, CosmeticShapes.alpha(0xFFFF5A12, 170));

        for (int i = 0; i < 16; ++i)
        {
            float a = (float)i / 16.0F * (float)Math.PI * 2.0F + age * 0.01F;
            float flicker = 0.5F + 0.5F * MathHelper.sin(age * 0.5F + (float)i * 2.7F);
            float h = 0.12F + 0.22F * flicker;
            float x = MathHelper.cos(a) * 0.68F;
            float z = MathHelper.sin(a) * 0.68F;
            CosmeticShapes.line(x, FEET, z, x * 0.95F, FEET - h, z * 0.95F, 0.06F, CosmeticShapes.alpha(0xFFFF7A1A, (int)(120 + 100 * flicker)));
            CosmeticShapes.line(x, FEET, z, x * 0.97F, FEET - h * 0.5F, z * 0.97F, 0.03F, CosmeticShapes.alpha(0xFFFFE070, 200));
        }

        CosmeticShapes.end();
    }

    private static void absoluteZero(float age, float pulse)
    {
        CosmeticShapes.begin(true);
        GlStateManager.depthMask(false);

        for (int i = 0; i < 9; ++i)
        {
            float a = (float)i / 9.0F * (float)Math.PI * 2.0F;
            float tall = 0.12F + 0.1F * (float)((i * 7) % 3) / 2.0F;
            float r = 0.62F + (float)(i % 2) * 0.1F;
            CosmeticShapes.diamond(MathHelper.cos(a) * r, FEET - tall, MathHelper.sin(a) * r, 0.05F, tall, 0xA0BFEFFF);
        }

        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        CosmeticShapes.ring(FEET, 0.8F, 0.25F, CosmeticShapes.alpha(0xFFDDF6FF, (int)(50 * pulse)));

        for (int i = 0; i < 9; ++i)
        {
            float sparkle = (float)Math.pow(Math.abs(MathHelper.sin(age * 0.07F + (float)i * 1.3F)), 8.0D);
            float a = (float)i / 9.0F * (float)Math.PI * 2.0F;
            CosmeticShapes.diamond(MathHelper.cos(a) * 0.62F, FEET - 0.2F, MathHelper.sin(a) * 0.62F, 0.03F * sparkle, 0.04F * sparkle, CosmeticShapes.alpha(0xFFFFFFFF, (int)(255 * sparkle)));
        }

        CosmeticShapes.end();
    }

    private static void divine(float age, float pulse, boolean firstPerson)
    {
        CosmeticShapes.beginAdditive();
        CosmeticShapes.ring(FEET, 0.85F, 0.06F, CosmeticShapes.alpha(0xFFFFD25A, 200));
        CosmeticShapes.ring(FEET, 0.95F, 0.3F, CosmeticShapes.alpha(0xFFFFE8A0, (int)(40 * pulse)));

        if (!firstPerson)
        {
            for (int i = 0; i < 6; ++i)
            {
                float a = (float)i / 6.0F * (float)Math.PI * 2.0F + age * 0.015F;
                float x = MathHelper.cos(a) * 0.8F;
                float z = MathHelper.sin(a) * 0.8F;
                float w = 0.06F;
                float tx = -MathHelper.sin(a) * w;
                float tz = MathHelper.cos(a) * w;
                int bottom = CosmeticShapes.alpha(0xFFFFE08A, (int)(110 * pulse + 40));
                int top = CosmeticShapes.alpha(0xFFFFE08A, 1);
                CosmeticShapes.quad(new float[] {x - tx, FEET, z - tz}, new float[] {x + tx, FEET, z + tz}, new float[] {x + tx, -0.9F, z + tz}, new float[] {x - tx, -0.9F, z - tz}, bottom, bottom, top, top);
            }
        }

        CosmeticShapes.end();
    }

    private static void glitch(float age)
    {
        CosmeticShapes.beginAdditive();
        Random rand = new Random((long)(age / 1.5F) * 977L);
        int[] colors = {0xFFFF2A3A, 0xFF2AFF6A, 0xFF2A7AFF, 0xFFFFFFFF};

        for (int i = 0; i < 12; ++i)
        {
            float size = 0.04F + rand.nextFloat() * 0.1F;
            float x = (rand.nextFloat() - 0.5F) * 1.1F;
            float y = -0.4F + rand.nextFloat() * 1.85F;
            float z = (rand.nextFloat() - 0.5F) * 0.9F;
            CosmeticShapes.box(x - size, y - size * 0.5F, z - size, x + size, y + size * 0.5F, z + size, CosmeticShapes.alpha(colors[rand.nextInt(colors.length)], 90 + rand.nextInt(120)));
        }

        // a couple of thin horizontal tear lines across the body
        for (int i = 0; i < 2; ++i)
        {
            float y = -0.3F + rand.nextFloat() * 1.7F;
            float off = (rand.nextFloat() - 0.5F) * 0.3F;
            CosmeticShapes.box(-0.45F + off, y, -0.2F, 0.45F + off, y + 0.015F, 0.2F, CosmeticShapes.alpha(colors[i], 120));
        }

        CosmeticShapes.end();
    }

    private static void shadowborn(float age, float pulse)
    {
        CosmeticShapes.begin(false);
        GlStateManager.disableCull();

        // dark tendrils curling up from the ground around you
        for (int t = 0; t < 6; ++t)
        {
            float a = (float)t / 6.0F * (float)Math.PI * 2.0F;
            float r = 0.6F;
            float x = MathHelper.cos(a) * r;
            float z = MathHelper.sin(a) * r;
            float y = FEET;

            for (int seg = 0; seg < 6; ++seg)
            {
                float curl = age * 0.06F + (float)t + (float)seg * 0.6F;
                float nx = x * 0.9F + MathHelper.cos(curl) * 0.05F;
                float nz = z * 0.9F + MathHelper.sin(curl) * 0.05F;
                float ny = y - 0.09F;
                CosmeticShapes.line(x, y, z, nx, ny, nz, 0.05F - (float)seg * 0.007F, 0xE0120814);
                x = nx;
                y = ny;
                z = nz;
            }
        }

        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        CosmeticShapes.ring(FEET, 0.75F, 0.05F, CosmeticShapes.alpha(0xFF6A2BB8, (int)(110 * pulse)));
        CosmeticShapes.end();
    }

    private static void prism(float age)
    {
        float bloom = 0.85F + 0.15F * MathHelper.sin(age * 0.06F);

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

            for (int ring = 0; ring < 2; ++ring)
            {
                int petals = ring == 0 ? 8 : 6;
                float reach = (ring == 0 ? 1.0F : 0.68F) * bloom;
                float turn = age * (ring == 0 ? 0.01F : -0.015F);
                // a cupped flower: petals rise towards their tips
                float base = FEET - 0.05F - (float)ring * 0.03F;
                float lift = 0.22F * bloom;

                for (int i = 0; i < petals; ++i)
                {
                    float a = turn + (float)i / (float)petals * (float)Math.PI * 2.0F;
                    float side = 0.22F;
                    float hue = age * 0.004F + (float)i / (float)petals;
                    int c = CosmeticShapes.alpha(CosmeticShapes.hue(hue), pass == 0 ? 170 : 90);
                    int tip = CosmeticShapes.alpha(CosmeticShapes.hue(hue + 0.12F), pass == 0 ? 110 : 140);
                    float r0 = 0.28F;
                    CosmeticShapes.quad(new float[] {MathHelper.cos(a) * r0, base, MathHelper.sin(a) * r0}, new float[] {MathHelper.cos(a - side) * reach * 0.65F, base - lift * 0.5F, MathHelper.sin(a - side) * reach * 0.65F}, new float[] {MathHelper.cos(a) * reach, base - lift, MathHelper.sin(a) * reach}, new float[] {MathHelper.cos(a + side) * reach * 0.65F, base - lift * 0.5F, MathHelper.sin(a + side) * reach * 0.65F}, c, c, tip, c);
                }
            }

            CosmeticShapes.end();
        }
    }

    /** Particles for the aura, around a player in the world (called once per tick). */
    public static void particles(String aura, EntityPlayer p, World world, int amount, boolean moving)
    {
        Random rand = p.getRNG();
        double x = p.posX;
        double y = p.posY;
        double z = p.posZ;

        switch (aura)
        {
            case "Void Aura":
                for (int i = 0; i < amount; ++i)
                {
                    world.spawnParticle(EnumParticleTypes.PORTAL, x + (rand.nextDouble() - 0.5D) * 1.4D, y + rand.nextDouble() * 1.8D, z + (rand.nextDouble() - 0.5D) * 1.4D, (rand.nextDouble() - 0.5D) * 0.6D, -rand.nextDouble() * 0.4D, (rand.nextDouble() - 0.5D) * 0.6D);
                }

                break;
            case "Thunder Core":
                if (rand.nextInt(3) == 0)
                {
                    world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x + (rand.nextDouble() - 0.5D), y + 0.3D + rand.nextDouble() * 1.4D, z + (rand.nextDouble() - 0.5D), 0.0D, 0.02D, 0.0D);
                }

                break;
            case "Inferno Aura":
                for (int i = 0; i < Math.max(1, amount / 2); ++i)
                {
                    double a = (double)(p.ticksExisted * 0.35F) + (double)i * Math.PI;
                    double h = (double)((p.ticksExisted + i * 10) % 20) / 10.0D;
                    world.spawnParticle(EnumParticleTypes.FLAME, x + Math.cos(a + h * 3.0D) * 0.6D, y + h, z + Math.sin(a + h * 3.0D) * 0.6D, 0.0D, 0.01D, 0.0D);
                }

                break;
            case "Absolute Zero":
                for (int i = 0; i < Math.max(1, amount / 2); ++i)
                {
                    world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, x + (rand.nextDouble() - 0.5D) * 1.6D, y + 1.6D + rand.nextDouble() * 0.6D, z + (rand.nextDouble() - 0.5D) * 1.6D, 0.0D, -0.03D, 0.0D);
                }

                break;
            case "Divine Radiance":
                if (p.ticksExisted % 2 == 0)
                {
                    double a = rand.nextDouble() * Math.PI * 2.0D;
                    world.spawnParticle(EnumParticleTypes.END_ROD, x + Math.cos(a) * 0.8D, y + 0.1D, z + Math.sin(a) * 0.8D, 0.0D, 0.06D, 0.0D);
                }

                break;
            case "Glitch Entity":
                for (int i = 0; i < Math.max(1, amount / 2); ++i)
                {
                    int c = rand.nextInt(3);
                    world.spawnParticle(EnumParticleTypes.REDSTONE, x + (rand.nextDouble() - 0.5D) * 1.2D, y + rand.nextDouble() * 2.0D, z + (rand.nextDouble() - 0.5D) * 1.2D, c == 0 ? 1.0D : 0.001D, c == 1 ? 1.0D : 0.0D, c == 2 ? 1.0D : 0.0D);
                }

                break;
            case "Shadowborn":
                if (moving || p.ticksExisted % 3 == 0)
                {
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x + (rand.nextDouble() - 0.5D) * 0.8D, y + rand.nextDouble() * 1.2D, z + (rand.nextDouble() - 0.5D) * 0.8D, 0.0D, 0.0D, 0.0D);
                }

                break;
            case "Prism Bloom":
                if (p.ticksExisted % 2 == 0)
                {
                    int c = CosmeticShapes.hue((float)p.ticksExisted * 0.02F + rand.nextFloat() * 0.3F);
                    double a = rand.nextDouble() * Math.PI * 2.0D;
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, x + Math.cos(a) * 0.7D, y + 0.2D + rand.nextDouble() * 0.4D, z + Math.sin(a) * 0.7D, (double)((c >> 16) & 255) / 255.0D, (double)((c >> 8) & 255) / 255.0D, (double)(c & 255) / 255.0D);
                }

                break;
            default:
        }
    }
}
