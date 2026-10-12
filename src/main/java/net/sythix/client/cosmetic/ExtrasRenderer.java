package net.sythix.client.cosmetic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sythix.client.ui.SythixTheme;

/**
 * The Sythix extras. Some are drawn on the model (the Rift Companion and the Afterimage); the
 * rest happen in the world and are remembered here until they fade: weapon trails, footstep runes,
 * the victory burst, the spawn portal and the signature emblem above your head.
 */
public final class ExtrasRenderer
{
    public static final String[] TRAIL_COLORS = {"Purple", "Cyan", "Red", "Gold"};
    public static final String[] EMBLEM_STYLES = {"Spin", "Pulse", "Float"};

    private static final long TRAIL_MS = 380L;
    private static final long RUNE_MS = 2600L;
    private static final long BURST_MS = 1700L;
    private static final long PORTAL_MS = 1900L;

    private static final List<double[]> TRAIL = new ArrayList<double[]>();
    private static final List<double[]> RUNES = new ArrayList<double[]>();
    private static final List<double[]> BURSTS = new ArrayList<double[]>();
    private static final List<double[]> PORTALS = new ArrayList<double[]>();
    /** Afterimage: past {x, y, z, body yaw, time} of the local player. */
    private static final List<double[]> GHOSTS = new ArrayList<double[]>();

    private ExtrasRenderer()
    {
    }

    public static int trailColor(String name)
    {
        switch (name)
        {
            case "Cyan":
                return 0xFF20E0FF;
            case "Red":
                return 0xFFFF2A3A;
            case "Gold":
                return 0xFFFFC63A;
            default:
                return 0xFFB050FF;
        }
    }

    private static long now()
    {
        return Minecraft.getSystemTime();
    }

    // ---- recording -----------------------------------------------------------------------------

    /** Adds the point the swing has reached (world position), while a swing is in progress. */
    public static void addTrailPoint(Vec3d point)
    {
        TRAIL.add(new double[] {point.x, point.y, point.z, (double)now()});
    }

    /** Ends the current trail (the next swing starts a new one). */
    public static void breakTrail()
    {
        if (!TRAIL.isEmpty() && TRAIL.get(TRAIL.size() - 1).length == 4)
        {
            TRAIL.add(new double[] {0.0D, 0.0D, 0.0D, (double)now(), 1.0D});
        }
    }

    public static void addRune(double x, double y, double z, int glyph, float turn)
    {
        RUNES.add(new double[] {x, y, z, (double)now(), (double)glyph, (double)turn});
    }

    public static void addBurst(World world, double x, double y, double z)
    {
        BURSTS.add(new double[] {x, y, z, (double)now()});
        Random rand = new Random();

        for (int i = 0; i < 40; ++i)
        {
            double a = rand.nextDouble() * Math.PI * 2.0D;
            double up = rand.nextDouble();
            world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x, y + 1.0D, z, Math.cos(a) * 0.25D * (1.0D - up * 0.5D), 0.1D + up * 0.25D, Math.sin(a) * 0.25D * (1.0D - up * 0.5D));
        }

        for (int i = 0; i < 20; ++i)
        {
            world.spawnParticle(EnumParticleTypes.END_ROD, x + (rand.nextDouble() - 0.5D) * 0.6D, y + 0.2D, z + (rand.nextDouble() - 0.5D) * 0.6D, 0.0D, 0.15D + rand.nextDouble() * 0.15D, 0.0D);
        }
    }

    public static void addPortal(World world, double x, double y, double z)
    {
        PORTALS.add(new double[] {x, y, z, (double)now()});
        Random rand = new Random();

        for (int i = 0; i < 60; ++i)
        {
            double a = rand.nextDouble() * Math.PI * 2.0D;
            world.spawnParticle(EnumParticleTypes.PORTAL, x + Math.cos(a) * 1.2D, y + rand.nextDouble() * 2.0D, z + Math.sin(a) * 1.2D, -Math.cos(a) * 0.6D, 0.0D, -Math.sin(a) * 0.6D);
        }
    }

    public static void recordGhost(AbstractClientPlayer p)
    {
        double speed = Math.abs(p.posX - p.prevPosX) + Math.abs(p.posZ - p.prevPosZ) + Math.abs(p.posY - p.prevPosY) * 0.5D;

        if (speed > 0.08D)
        {
            GHOSTS.add(new double[] {p.posX, p.posY, p.posZ, (double)p.renderYawOffset, (double)now()});
        }

        while (GHOSTS.size() > 12)
        {
            GHOSTS.remove(0);
        }
    }

    public static void clear()
    {
        TRAIL.clear();
        RUNES.clear();
        BURSTS.clear();
        PORTALS.clear();
        GHOSTS.clear();
    }

    // ---- model space (on the player) ------------------------------------------------------------

    /** The Rift Companion: a small crystal floating by your left shoulder. */
    public static void companion(float age)
    {
        float bob = MathHelper.sin(age * 0.09F) * 0.06F;
        CosmeticShapes.begin(true);
        GlStateManager.translate(0.78F, -0.42F + bob, 0.1F);
        GlStateManager.rotate(age * 3.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(1.4F, 1.4F, 1.4F);
        CosmeticShapes.diamond(0.0F, 0.0F, 0.0F, 0.09F, 0.15F, 0xFF6A2AC0);
        CosmeticShapes.end();
        CosmeticShapes.beginAdditive();
        GlStateManager.translate(0.78F, -0.42F + bob, 0.1F);
        GlStateManager.scale(1.4F, 1.4F, 1.4F);
        float pulse = 0.6F + 0.4F * MathHelper.sin(age * 0.2F);
        CosmeticShapes.diamond(0.0F, 0.0F, 0.0F, 0.05F, 0.09F, CosmeticShapes.alpha(0xFFE0B0FF, (int)(255 * pulse)));
        CosmeticShapes.diamond(0.0F, 0.0F, 0.0F, 0.15F, 0.22F, CosmeticShapes.alpha(0xFF9A3CFF, (int)(50 * pulse)));

        for (int i = 0; i < 3; ++i)
        {
            float a = age * 0.12F + (float)i * 2.094F;
            CosmeticShapes.diamond(MathHelper.cos(a) * 0.17F, MathHelper.sin(a * 1.3F) * 0.05F, MathHelper.sin(a) * 0.17F, 0.012F, 0.018F, 0xFF7FE6FF);
        }

        CosmeticShapes.end();
    }

    /**
     * Afterimage: see-through copies of the model where you were a moment ago. Called in model
     * space while the player is drawn; 'x, y, z, yaw' are where the player is drawn this frame.
     */
    public static void afterimage(ModelPlayer model, double x, double y, double z, float yaw)
    {
        long time = now();
        Iterator<double[]> it = GHOSTS.iterator();

        while (it.hasNext())
        {
            if (time - (long)it.next()[4] > 450L)
            {
                it.remove();
            }
        }

        if (GHOSTS.isEmpty())
        {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);

        for (int i = 0; i < GHOSTS.size(); i += 3)
        {
            double[] g = GHOSTS.get(i);
            float life = 1.0F - (float)(time - (long)g[4]) / 450.0F;

            if (life <= 0.0F || (Math.abs(g[0] - x) + Math.abs(g[2] - z) + Math.abs(g[1] - y)) < 0.25D)
            {
                continue;
            }

            GlStateManager.pushMatrix();
            // from this frame's model space back to the world, over to the old spot, and back in
            GlStateManager.translate(0.0F, 1.501F, 0.0F);
            GlStateManager.scale(1.0F / 0.9375F, 1.0F / 0.9375F, 1.0F / 0.9375F);
            GlStateManager.scale(-1.0F, -1.0F, 1.0F);
            GlStateManager.rotate(-(180.0F - yaw), 0.0F, 1.0F, 0.0F);
            GlStateManager.translate((float)(g[0] - x), (float)(g[1] - y), (float)(g[2] - z));
            GlStateManager.rotate(180.0F - (float)g[3], 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(-1.0F, -1.0F, 1.0F);
            GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
            GlStateManager.translate(0.0F, -1.501F, 0.0F);
            GlStateManager.color(0.62F, 0.3F, 1.0F, 0.3F * life);
            float s = 0.0625F;
            model.bipedHead.render(s);
            model.bipedBody.render(s);
            model.bipedRightArm.render(s);
            model.bipedLeftArm.render(s);
            model.bipedRightLeg.render(s);
            model.bipedLeftLeg.render(s);
            GlStateManager.popMatrix();
        }

        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    /**
     * The signature emblem in a screen-like frame (x right, y down, 1 unit = 1/40 block, centred
     * above the head): the Sythix logo with a glow and your text under it.
     */
    public static void emblem(String text, int color, String style, float age)
    {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        // full brightness, so the text keeps its colour at night and in shade
        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        if ("Float".equals(style))
        {
            GlStateManager.translate(0.0F, MathHelper.sin(age * 0.08F) * 4.0F, 0.0F);
        }

        float scale = "Pulse".equals(style) ? 1.0F + 0.08F * MathHelper.sin(age * 0.15F) : 1.0F;
        GlStateManager.scale(scale, scale, scale);

        // glow behind the logo
        CosmeticShapes.beginAdditive();
        GlStateManager.translate(0.0F, -18.0F, 0.5F);
        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
        float glow = "Pulse".equals(style) ? 0.6F + 0.4F * MathHelper.sin(age * 0.15F) : 0.75F;

        for (int i = 0; i < 4; ++i)
        {
            CosmeticShapes.ring(0.0F, 18.0F - (float)i * 4.0F, 5.0F, CosmeticShapes.alpha(color, (int)((18 + i * 12) * glow)));
        }

        CosmeticShapes.end();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, -18.0F, 0.0F);

        if ("Spin".equals(style))
        {
            GlStateManager.rotate(age * 4.0F, 0.0F, 1.0F, 0.0F);
        }

        GlStateManager.enableTexture2D();
        SythixTheme.drawTexture(SythixTheme.LOGO, -16.0F, -16.0F, 32.0F, 32.0F, 1.0F);
        GlStateManager.popMatrix();

        String label = text == null || text.trim().isEmpty() ? "SYTHIX" : text.trim();
        GlStateManager.enableBlend();
        font.drawString(label, (float)(-font.getStringWidth(label) / 2), 1.0F, 0xFF000000 | (color & 0xFFFFFF), true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    // ---- world space (after the world is drawn; positions relative to the camera) ---------------

    /** Draws everything that lives in the world: trails, runes, bursts, portals. */
    public static void renderWorld(RenderManager rm, int trailColor)
    {
        long time = now();
        double vx = rm.viewerPosX;
        double vy = rm.viewerPosY;
        double vz = rm.viewerPosZ;
        prune(TRAIL, TRAIL_MS, 3);
        prune(RUNES, RUNE_MS, 3);
        prune(BURSTS, BURST_MS, 3);
        prune(PORTALS, PORTAL_MS, 3);

        if (TRAIL.isEmpty() && RUNES.isEmpty() && BURSTS.isEmpty() && PORTALS.isEmpty())
        {
            return;
        }

        CosmeticShapes.beginAdditive();
        GlStateManager.translate(-vx, -vy, -vz);

        // weapon trail: a ribbon through the recorded points, fading at its old end
        for (int i = 1; i < TRAIL.size(); ++i)
        {
            double[] a = TRAIL.get(i - 1);
            double[] b = TRAIL.get(i);

            if (a.length > 4 || b.length > 4)
            {
                continue;
            }

            float la = 1.0F - (float)(time - (long)a[3]) / (float)TRAIL_MS;
            float lb = 1.0F - (float)(time - (long)b[3]) / (float)TRAIL_MS;
            float wa = 0.09F * la;
            float wb = 0.09F * lb;
            CosmeticShapes.quad(new float[] {(float)a[0], (float)a[1] - wa, (float)a[2]}, new float[] {(float)a[0], (float)a[1] + wa, (float)a[2]}, new float[] {(float)b[0], (float)b[1] + wb, (float)b[2]}, new float[] {(float)b[0], (float)b[1] - wb, (float)b[2]}, CosmeticShapes.alpha(trailColor, (int)(200 * la)), CosmeticShapes.alpha(0xFFFFFFFF, (int)(160 * la)), CosmeticShapes.alpha(0xFFFFFFFF, (int)(160 * lb)), CosmeticShapes.alpha(trailColor, (int)(200 * lb)));
        }

        for (double[] r : RUNES)
        {
            float life = (float)(time - (long)r[3]) / (float)RUNE_MS;
            float fade = Math.min(1.0F, Math.min(life * 6.0F, (1.0F - life) * 2.5F));
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)r[0], (float)r[1] + 0.02F, (float)r[2]);
            GlStateManager.rotate((float)r[5] + life * 40.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(1.0F, -1.0F, 1.0F);
            rune((int)r[4], fade);
            GlStateManager.popMatrix();
        }

        for (double[] b : BURSTS)
        {
            float life = (float)(time - (long)b[3]) / (float)BURST_MS;
            float fade = 1.0F - life;
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)b[0], (float)b[1] + 0.05F, (float)b[2]);
            GlStateManager.scale(1.0F, -1.0F, 1.0F);
            CosmeticShapes.ring(0.0F, 0.4F + life * 3.2F, 0.18F * fade + 0.02F, CosmeticShapes.alpha(0xFFFFD25A, (int)(220 * fade)));
            CosmeticShapes.ring(0.0F, 0.2F + life * 2.0F, 0.08F, CosmeticShapes.alpha(0xFFFFFFFF, (int)(160 * fade)));

            for (int i = 0; i < 8; ++i)
            {
                float a = (float)i / 8.0F * (float)Math.PI * 2.0F;
                float r = 0.3F + life * 1.6F;
                CosmeticShapes.line(MathHelper.cos(a) * r, 0.0F, MathHelper.sin(a) * r, MathHelper.cos(a) * r * 1.1F, -(0.6F + life * 2.5F), MathHelper.sin(a) * r * 1.1F, 0.05F, CosmeticShapes.alpha(0xFFFFE08A, (int)(150 * fade)));
            }

            GlStateManager.popMatrix();
        }

        for (double[] p : PORTALS)
        {
            float life = (float)(time - (long)p[3]) / (float)PORTAL_MS;
            float fade = Math.min(1.0F, (1.0F - life) * 3.0F);
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)p[0], (float)p[1], (float)p[2]);
            GlStateManager.scale(1.0F, -1.0F, 1.0F);

            for (int ring = 0; ring < 4; ++ring)
            {
                float k = ((life * 1.5F) + (float)ring * 0.25F) % 1.0F;
                float radius = 1.3F * (1.0F - k) + 0.3F;
                CosmeticShapes.ring(-k * 2.1F, radius, 0.06F, CosmeticShapes.alpha(ring % 2 == 0 ? 0xFFB050FF : 0xFF40E0FF, (int)(220 * fade * (1.0F - k * 0.5F))));
            }

            CosmeticShapes.ring(0.0F, 1.2F, 0.6F * fade, CosmeticShapes.alpha(0xFF7A20E0, (int)(90 * fade)));
            GlStateManager.popMatrix();
        }

        CosmeticShapes.end();
    }

    private static void prune(List<double[]> list, long life, int timeIndex)
    {
        long time = now();
        Iterator<double[]> it = list.iterator();

        while (it.hasNext())
        {
            if (time - (long)it.next()[timeIndex] > life)
            {
                it.remove();
            }
        }
    }

    /** A rune circle on the ground (in a frame where up is -Y), one of five symbols. */
    private static void rune(int glyph, float fade)
    {
        int c = CosmeticShapes.alpha(0xFFB060FF, (int)(230 * fade));
        int soft = CosmeticShapes.alpha(0xFF7A20E0, (int)(90 * fade));
        CosmeticShapes.ring(0.0F, 0.4F, 0.03F, c);
        CosmeticShapes.ring(0.0F, 0.3F, 0.015F, c);
        CosmeticShapes.ring(0.0F, 0.42F, 0.12F, soft);
        float[][] pts = new float[6][];

        for (int i = 0; i < 6; ++i)
        {
            float a = (float)i / 6.0F * (float)Math.PI * 2.0F;
            pts[i] = new float[] {MathHelper.cos(a) * 0.28F, MathHelper.sin(a) * 0.28F};
        }

        int[][][] symbols = {
            {{0, 2}, {2, 4}, {4, 0}},
            {{0, 3}, {1, 4}, {2, 5}},
            {{0, 2}, {2, 4}, {4, 0}, {1, 3}, {3, 5}, {5, 1}},
            {{0, 3}, {1, 2}, {4, 5}},
            {{0, 1}, {1, 3}, {3, 4}, {4, 0}}
        };

        for (int[] line : symbols[Math.abs(glyph) % symbols.length])
        {
            CosmeticShapes.line(pts[line[0]][0], 0.0F, pts[line[0]][1], pts[line[1]][0], 0.0F, pts[line[1]][1], 0.025F, c);
        }

        CosmeticShapes.diamond(0.0F, -0.04F, 0.0F, 0.05F, 0.04F, c);
    }
}
