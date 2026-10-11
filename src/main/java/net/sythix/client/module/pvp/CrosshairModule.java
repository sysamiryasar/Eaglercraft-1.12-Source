package net.sythix.client.module.pvp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #9 Crosshair: shape, size, gap, colour, outline and hit feedback.
 */
public class CrosshairModule extends Module
{
    private final ModeSetting shape;
    private final NumberSetting size;
    private final NumberSetting thickness;
    private final NumberSetting gap;
    private final ColorSetting color;
    private final BooleanSetting outline;
    private final ColorSetting outlineColor;
    private final BooleanSetting centerDot;
    private final NumberSetting dotSize;
    private final BooleanSetting targetHighlight;
    private final ColorSetting targetColor;
    private final BooleanSetting hitMarker;
    private final ColorSetting hitMarkerColor;
    private final NumberSetting hitMarkerTime;
    private final BooleanSetting dynamicGap;
    private final BooleanSetting thirdPerson;
    private final BooleanSetting invert;
    private float dynamicExtra;
    private long lastHit;

    public CrosshairModule()
    {
        super("Crosshair", "Design your crosshair: shape, size, gap, colour, outline and hit feedback.", Category.PVP, false);
        this.shape = this.add(new ModeSetting("Shape", "Crosshair style.", "Cross", "Cross", "Circle", "Square", "X", "Arrow", "Dot"));
        this.size = this.add(new NumberSetting("Size", "Length of each line (or radius).", 5.0D, 1.0D, 20.0D, 0.5D, "px"));
        this.thickness = this.add(new NumberSetting("Thickness", "Line thickness.", 1.0D, 0.5D, 5.0D, 0.5D, "px"));
        this.gap = this.add(new NumberSetting("Gap", "Empty space in the middle.", 2.0D, 0.0D, 15.0D, 0.5D, "px"));
        this.color = this.add(new ColorSetting("Color", "Crosshair colour.", 0xFFFFFFFF, true));
        this.outline = this.add(new BooleanSetting("Outline", "Dark border for visibility on bright backgrounds.", true));
        this.outlineColor = this.add(new ColorSetting("Outline Color", "Colour of the border.", 0xC0000000, true)).visibleWhen(this.outline::isOn);
        this.centerDot = this.add(new BooleanSetting("Center Dot", "Add a dot in the middle.", false));
        this.dotSize = this.add(new NumberSetting("Dot Size", "Size of the center dot.", 1.0D, 0.5D, 6.0D, 0.5D, "px")).visibleWhen(this.centerDot::isOn);
        this.targetHighlight = this.add(new BooleanSetting("Target Highlight", "Change colour while aiming at an entity.", true));
        this.targetColor = this.add(new ColorSetting("Target Color", "Colour while aiming at an entity.", 0xFFD81820, true)).visibleWhen(this.targetHighlight::isOn);
        this.hitMarker = this.add(new BooleanSetting("Hit Marker", "Flash an X when you hit something.", true));
        this.hitMarkerColor = this.add(new ColorSetting("Hit Marker Color", "Colour of the hit marker.", 0xFFFFFFFF, true)).visibleWhen(this.hitMarker::isOn);
        this.hitMarkerTime = this.add(new NumberSetting("Hit Marker Time", "How long the hit marker shows.", 250.0D, 100.0D, 1000.0D, 50.0D, "ms")).visibleWhen(this.hitMarker::isOn);
        this.dynamicGap = this.add(new BooleanSetting("Dynamic Gap", "Gap widens slightly while you move.", false));
        this.thirdPerson = this.add(new BooleanSetting("Third Person", "Also show the crosshair in third person.", false));
        this.invert = this.add(new BooleanSetting("Invert Colors", "Invert what is behind it, like the vanilla crosshair.", false));
    }

    public boolean showInThirdPerson()
    {
        return this.thirdPerson.isOn();
    }

    public void onAttack(Entity target)
    {
        if (target instanceof EntityLivingBase)
        {
            this.lastHit = Minecraft.getSystemTime();
        }
    }

    public void onTick()
    {
        Entity e = this.mc.getRenderViewEntity();
        float target = 0.0F;

        if (this.dynamicGap.isOn() && e != null)
        {
            double dx = e.posX - e.prevPosX;
            double dz = e.posZ - e.prevPosZ;
            target = (float)Math.min(4.0D, Math.sqrt(dx * dx + dz * dz) * 12.0D);
        }

        this.dynamicExtra += (target - this.dynamicExtra) * 0.5F;
    }

    /** Draws the crosshair centred on (cx, cy) in GUI coordinates. */
    public void draw(float cx, float cy, boolean aimingAtEntity)
    {
        int main = aimingAtEntity && this.targetHighlight.isOn() ? this.targetColor.getColor() : this.color.getColor();
        GlStateManager.enableBlend();

        if (this.invert.isOn())
        {
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }
        else
        {
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }

        boolean drawOutline = this.outline.isOn() && !this.invert.isOn();

        if (drawOutline)
        {
            this.drawShape(cx, cy, this.outlineColor.getColor(), 1.0F);
        }

        this.drawShape(cx, cy, main, 0.0F);

        if (this.centerDot.isOn())
        {
            float d = this.dotSize.getFloat() / 2.0F;

            if (drawOutline)
            {
                this.rect(cx - d - 1.0F, cy - d - 1.0F, cx + d + 1.0F, cy + d + 1.0F, this.outlineColor.getColor());
            }

            this.rect(cx - d, cy - d, cx + d, cy + d, main);
        }

        long since = Minecraft.getSystemTime() - this.lastHit;

        if (this.hitMarker.isOn() && since < (long)this.hitMarkerTime.getInt())
        {
            float fade = 1.0F - (float)since / this.hitMarkerTime.getFloat();
            int c = SythixTheme.withAlpha(this.hitMarkerColor.getColor(), (int)(255.0F * fade));
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 0.0F);
            GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F);
            float inner = this.gap.getFloat() + 2.0F;
            float outer = inner + 4.0F;
            this.rect(-0.5F, -outer, 0.5F, -inner, c);
            this.rect(-0.5F, inner, 0.5F, outer, c);
            this.rect(-outer, -0.5F, -inner, 0.5F, c);
            this.rect(inner, -0.5F, outer, 0.5F, c);
            GlStateManager.popMatrix();
        }

        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Rectangle that keeps the current blend mode. */
    private void rect(float left, float top, float right, float bottom, int color)
    {
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int a = (color >>> 24) & 255;
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos((double)left, (double)bottom, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos((double)right, (double)bottom, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos((double)right, (double)top, 0.0D).color(r, g, b, a).endVertex();
        buffer.pos((double)left, (double)top, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private void ring(float cx, float cy, float radius, float width, int color)
    {
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int a = (color >>> 24) & 255;
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        int segments = Math.max(16, (int)(radius * 6.0F));
        buffer.begin(5, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i <= segments; ++i)
        {
            double angle = Math.PI * 2.0D * (double)i / (double)segments;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double outer = (double)(radius + width / 2.0F);
            double inner = (double)Math.max(0.0F, radius - width / 2.0F);
            buffer.pos((double)cx + cos * outer, (double)cy + sin * outer, 0.0D).color(r, g, b, a).endVertex();
            buffer.pos((double)cx + cos * inner, (double)cy + sin * inner, 0.0D).color(r, g, b, a).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    /** grow is extra size on every side (used for the outline pass). */
    private void drawShape(float cx, float cy, int color, float grow)
    {
        float len = this.size.getFloat();
        float t = this.thickness.getFloat() / 2.0F + grow;
        float gap = this.gap.getFloat() + this.dynamicExtra;
        String s = this.shape.get();

        if (s.equals("Cross") || s.equals("X"))
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 0.0F);

            if (s.equals("X"))
            {
                GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F);
            }

            this.rect(-t, -gap - len - grow, t, -gap + grow, color);
            this.rect(-t, gap - grow, t, gap + len + grow, color);
            this.rect(-gap - len - grow, -t, -gap + grow, t, color);
            this.rect(gap - grow, -t, gap + len + grow, t, color);
            GlStateManager.popMatrix();
        }
        else if (s.equals("Circle"))
        {
            this.ring(cx, cy, gap + len / 2.0F, this.thickness.getFloat() + grow * 2.0F, color);
        }
        else if (s.equals("Square"))
        {
            float o = gap + len / 2.0F;
            this.rect(cx - o - t, cy - o - t, cx + o + t, cy - o + t, color);
            this.rect(cx - o - t, cy + o - t, cx + o + t, cy + o + t, color);
            this.rect(cx - o - t, cy - o + t, cx - o + t, cy + o - t, color);
            this.rect(cx + o - t, cy - o + t, cx + o + t, cy + o - t, color);
        }
        else if (s.equals("Arrow"))
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy + gap, 0.0F);
            GlStateManager.rotate(-135.0F, 0.0F, 0.0F, 1.0F);
            this.rect(-t, -t, len + grow, t, color);
            this.rect(-t, -t, t, len + grow, color);
            GlStateManager.popMatrix();
        }
        else
        {
            float d = len / 2.0F + grow;
            this.rect(cx - d, cy - d, cx + d, cy + d, color);
        }
    }

    public boolean hasPreview()
    {
        return true;
    }

    public void renderPreview(int x, int y, int width, int height)
    {
        int sw = this.mc.currentScreen == null ? width : this.mc.currentScreen.width;
        int sh = this.mc.currentScreen == null ? height : this.mc.currentScreen.height;
        SythixTheme.drawBackgroundRegion(sw, sh, x, y, x + width, y + height, -1, -1, 0.0F);
        SythixTheme.drawRect((float)x, (float)(y + height / 2), (float)(x + width), (float)(y + height), 0x60FFFFFF);
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x + (float)width / 2.0F, (float)y + (float)height / 2.0F, 0.0F);
        GlStateManager.scale(2.0F, 2.0F, 1.0F);
        this.draw(0.0F, 0.0F, false);
        GlStateManager.popMatrix();
        this.mc.fontRenderer.drawString("Preview (2x)", (float)(x + 3), (float)(y + 3), 0xA0FFFFFF, true);
    }
}
