package net.sythix.client.module.movement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.waypoint.GuiWaypoints;
import net.sythix.client.waypoint.Waypoint;
import net.sythix.client.waypoint.WaypointStore;

/**
 * #21 Waypoints: saved locations, death markers, labels, colours, distance, visibility and
 * import/export, managed in the waypoint manager screen.
 */
public class WaypointsModule extends Module
{
    private final KeybindSetting managerKey;
    private final KeybindSetting quickAddKey;
    private final BooleanSetting beams;
    private final NumberSetting beamOpacity;
    private final BooleanSetting labels;
    private final BooleanSetting showDistance;
    private final NumberSetting labelScale;
    private final NumberSetting maxDistance;
    private final BooleanSetting deathMarkers;
    private final NumberSetting keepDeaths;
    private final ColorSetting deathColor;
    private boolean wasDead;

    public WaypointsModule()
    {
        super("Waypoints", "Saved locations with beams, labels, distance and automatic death markers.", Category.MOVEMENT, true);
        this.add(new ButtonSetting("Waypoint Manager", "Add, edit, remove, organise, import and export waypoints.", "Open", () -> this.mc.displayGuiScreen(new GuiWaypoints(this.mc.currentScreen))));
        this.managerKey = this.add(new KeybindSetting("Manager Key", "Open the waypoint manager in game.", 0));
        this.quickAddKey = this.add(new KeybindSetting("Quick Add Key", "Save a waypoint where you stand without opening a menu.", 0));
        this.beams = this.add(new BooleanSetting("Beams", "Draw a coloured beam at each waypoint.", true));
        this.beamOpacity = this.add(new NumberSetting("Beam Opacity", "How solid the beams are.", 35.0D, 5.0D, 100.0D, 5.0D, "%")).visibleWhen(this.beams::isOn);
        this.labels = this.add(new BooleanSetting("Labels", "Show the waypoint name.", true));
        this.showDistance = this.add(new BooleanSetting("Distance", "Show how far away each waypoint is.", true));
        this.labelScale = this.add(new NumberSetting("Label Size", "Size of the labels.", 1.0D, 0.5D, 2.5D, 0.1D, "x"));
        this.maxDistance = this.add(new NumberSetting("Max Distance", "Hide waypoints further than this. 0 = no limit.", 0.0D, 0.0D, 10000.0D, 100.0D, " blocks"));
        this.deathMarkers = this.add(new BooleanSetting("Death Markers", "Save a waypoint where you die.", true));
        this.keepDeaths = this.add(new NumberSetting("Keep Deaths", "How many death markers to keep per world.", 3.0D, 1.0D, 20.0D, 1.0D)).visibleWhen(this.deathMarkers::isOn);
        this.deathColor = this.add(new ColorSetting("Death Color", "Colour of death markers.", 0xFF8A8A8A, false)).visibleWhen(this.deathMarkers::isOn);
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiWaypoints(parent);
    }

    private int dimension()
    {
        return this.mc.world == null ? 0 : this.mc.world.provider.getDimensionType().getId();
    }

    public void onKey(int keyCode)
    {
        if (keyCode == 0 || this.mc.player == null)
        {
            return;
        }

        if (keyCode == this.managerKey.getKey())
        {
            this.mc.displayGuiScreen(new GuiWaypoints(null));
        }
        else if (keyCode == this.quickAddKey.getKey())
        {
            List<Waypoint> list = WaypointStore.current();
            Waypoint w = new Waypoint("Waypoint " + (list.size() + 1), (int)Math.floor(this.mc.player.posX), (int)Math.floor(this.mc.player.posY), (int)Math.floor(this.mc.player.posZ), this.dimension(), 0x2F6BFF);
            list.add(w);
            WaypointStore.save();
            Notifications.post("Waypoint added", w.name + " at " + w.x + ", " + w.y + ", " + w.z, Notifications.Kind.SUCCESS);
        }
    }

    public void onTick()
    {
        if (this.mc.player == null)
        {
            this.wasDead = false;
            return;
        }

        boolean dead = this.mc.player.getHealth() <= 0.0F || this.mc.player.isDead;

        if (dead && !this.wasDead && this.deathMarkers.isOn())
        {
            List<Waypoint> list = WaypointStore.current();
            Waypoint w = new Waypoint("Death " + net.sythix.client.module.advanced.Localization.time().format(new Date()), (int)Math.floor(this.mc.player.posX), (int)Math.floor(this.mc.player.posY), (int)Math.floor(this.mc.player.posZ), this.dimension(), this.deathColor.getArgb() & 0xFFFFFF);
            w.death = true;
            list.add(w);
            List<Waypoint> deaths = new ArrayList<Waypoint>();

            for (Waypoint other : list)
            {
                if (other.death)
                {
                    deaths.add(other);
                }
            }

            while (deaths.size() > this.keepDeaths.getInt())
            {
                list.remove(deaths.remove(0));
            }

            WaypointStore.save();
            Notifications.post("Death marker saved", w.x + ", " + w.y + ", " + w.z, Notifications.Kind.INFO);
        }

        this.wasDead = dead;
    }

    public void onRenderWorld(float partialTicks)
    {
        Entity viewer = this.mc.getRenderViewEntity();

        if (viewer == null || this.mc.world == null)
        {
            return;
        }

        RenderManager rm = this.mc.getRenderManager();
        int dim = this.dimension();

        for (Waypoint w : WaypointStore.current())
        {
            if (!w.visible || w.dimension != dim)
            {
                continue;
            }

            double dx = (double)w.x + 0.5D - rm.viewerPosX;
            double dy = (double)w.y - rm.viewerPosY;
            double dz = (double)w.z + 0.5D - rm.viewerPosZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (this.maxDistance.getInt() > 0 && distance > (double)this.maxDistance.getInt())
            {
                continue;
            }

            if (this.beams.isOn())
            {
                this.drawBeam(dx, dz, w.color, this.beamOpacity.getFloat() / 100.0F, rm.viewerPosY);
            }

            if (this.labels.isOn() || this.showDistance.isOn())
            {
                String text = (this.labels.isOn() ? w.name : "") + (this.showDistance.isOn() ? (this.labels.isOn() ? " " : "") + "(" + Math.round(distance) + "m)" : "");
                // Keep far labels readable by drawing them at most 24 blocks away, in the same direction
                double scaleDist = Math.min(distance, 24.0D);
                double f = distance < 0.001D ? 0.0D : scaleDist / distance;
                this.drawLabel(text, dx * f, (dy + 1.5D) * f, dz * f, scaleDist, w.color);
            }
        }
    }

    private void drawBeam(double x, double z, int color, float alpha, double viewerY)
    {
        float r = (float)((color >> 16) & 255) / 255.0F;
        float g = (float)((color >> 8) & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        double bottom = -viewerY;
        double top = 256.0D - viewerY;
        double w = 0.15D;
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        double[][] faces = new double[][] {{-w, -w, w, -w}, {w, -w, w, w}, {w, w, -w, w}, {-w, w, -w, -w}};

        for (double[] face : faces)
        {
            buffer.pos(x + face[0], bottom, z + face[1]).color(r, g, b, alpha).endVertex();
            buffer.pos(x + face[2], bottom, z + face[3]).color(r, g, b, alpha).endVertex();
            buffer.pos(x + face[2], top, z + face[3]).color(r, g, b, alpha).endVertex();
            buffer.pos(x + face[0], top, z + face[1]).color(r, g, b, alpha).endVertex();
        }

        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawLabel(String text, double x, double y, double z, double distance, int color)
    {
        RenderManager rm = this.mc.getRenderManager();
        FontRenderer font = this.mc.fontRenderer;
        float scale = (float)(0.02D + distance * 0.0025D) * this.labelScale.getFloat();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float)(this.mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        int half = font.getStringWidth(text) / 2;
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos((double)(-half - 2), -2.0D, 0.0D).color(0.05F, 0.05F, 0.07F, 0.6F).endVertex();
        buffer.pos((double)(-half - 2), 9.0D, 0.0D).color(0.05F, 0.05F, 0.07F, 0.6F).endVertex();
        buffer.pos((double)(half + 2), 9.0D, 0.0D).color(0.05F, 0.05F, 0.07F, 0.6F).endVertex();
        buffer.pos((double)(half + 2), -2.0D, 0.0D).color(0.05F, 0.05F, 0.07F, 0.6F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        font.drawString(text, -half, 0, 0xFF000000 | color);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
