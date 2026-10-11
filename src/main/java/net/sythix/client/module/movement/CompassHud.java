package net.sythix.client.module.movement;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.StyledHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #20 Cardinal directions, facing angle and visual style.
 */
public class CompassHud extends StyledHudModule
{
    private static final String[] MARKS = new String[] {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private final ModeSetting style;
    private final NumberSetting stripWidth;
    private final BooleanSetting showDegrees;
    private final BooleanSetting showIntercardinal;
    private final ColorSetting needleColor;

    public CompassHud()
    {
        super("Compass HUD", "A compass strip or text showing which way you face.", Category.MOVEMENT, false, 0.5D, 0.0D);
        this.style = this.add(new ModeSetting("Style", "A sliding strip or a short text.", "Strip", "Strip", "Text"));
        this.stripWidth = this.add(new NumberSetting("Width", "Width of the compass strip.", 120.0D, 60.0D, 300.0D, 5.0D, "px"));
        this.showDegrees = this.add(new BooleanSetting("Show Degrees", "Show your exact facing angle.", true));
        this.showIntercardinal = this.add(new BooleanSetting("NE / SW Marks", "Show in-between directions.", true));
        this.needleColor = this.add(new ColorSetting("Needle Color", "Colour of the centre marker.", 0xFF6CC6FF, true));
    }

    private float yaw360()
    {
        Entity e = this.mc.getRenderViewEntity();
        float yaw = e == null ? 180.0F : MathHelper.wrapDegrees(e.rotationYaw);
        return yaw < 0.0F ? yaw + 360.0F : yaw;
    }

    private String textValue()
    {
        float yaw = this.yaw360();
        String mark = MARKS[Math.round(yaw / 45.0F) % 8];
        return this.showDegrees.isOn() ? mark + " " + Math.round(yaw) + "°" : mark;
    }

    public int getWidth()
    {
        if (this.style.is("Text"))
        {
            return this.mc.fontRenderer.getStringWidth(this.textValue()) + this.getPad() * 2;
        }

        return this.stripWidth.getInt();
    }

    public int getHeight()
    {
        if (this.style.is("Text"))
        {
            return 8 + this.getPad() * 2;
        }

        return (this.showDegrees.isOn() ? 22 : 12) + this.getPad();
    }

    protected void renderContent(boolean preview)
    {
        int width = this.getWidth();
        this.drawBox(width, this.getHeight());
        FontRenderer font = this.mc.fontRenderer;

        if (this.style.is("Text"))
        {
            this.drawText(this.textValue(), (float)this.getPad(), (float)this.getPad(), this.valueColor.getColor());
            return;
        }

        float yaw = this.yaw360();
        float pixelsPerDegree = 1.2F;
        float centre = (float)width / 2.0F;
        int top = this.getPad() / 2 + 2;

        for (int i = 0; i < 8; ++i)
        {
            String mark = MARKS[i];

            if (mark.length() > 1 && !this.showIntercardinal.isOn())
            {
                continue;
            }

            float x = centre + MathHelper.wrapDegrees((float)i * 45.0F - yaw) * pixelsPerDegree;
            float half = (float)font.getStringWidth(mark) / 2.0F;

            if (x - half < 2.0F || x + half > (float)width - 2.0F)
            {
                continue;
            }

            this.drawText(mark, x - half, (float)top, mark.length() == 1 ? this.valueColor.getColor() : this.labelColor.getColor());
        }

        for (int deg = 0; deg < 360; deg += 15)
        {
            if (deg % 45 != 0)
            {
                float x = centre + MathHelper.wrapDegrees((float)deg - yaw) * pixelsPerDegree;

                if (x >= 2.0F && x <= (float)width - 2.0F)
                {
                    SythixTheme.drawRect(x - 0.5F, (float)(top + 2), x + 0.5F, (float)(top + 6), 0x80FFFFFF);
                }
            }
        }

        SythixTheme.drawRect(centre - 0.5F, 0.0F, centre + 0.5F, (float)(top + 10), this.needleColor.getColor());

        if (this.showDegrees.isOn())
        {
            String deg = Math.round(yaw) + "°";
            this.drawText(deg, centre - (float)font.getStringWidth(deg) / 2.0F, (float)(top + 11), this.labelColor.getColor());
        }
    }
}
