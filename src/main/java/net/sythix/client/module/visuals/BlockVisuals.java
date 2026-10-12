package net.sythix.client.module.visuals;

import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #36 Block selection outline and highlight.
 */
public class BlockVisuals extends Module
{
    private final ColorSetting outlineColor;
    private final NumberSetting width;
    private final BooleanSetting fill;
    private final ColorSetting fillColor;
    private final BooleanSetting pulse;

    public BlockVisuals()
    {
        super("Block Visuals", "Colour, thickness and fill of the outline on the block you look at.", Category.VISUALS, false);
        this.outlineColor = this.add(new ColorSetting("Outline Color", "Colour of the block outline.", 0xCCD81820, true));
        this.width = this.add(new NumberSetting("Outline Width", "Thickness of the outline.", 2.0D, 1.0D, 8.0D, 0.5D, "px"));
        this.fill = this.add(new BooleanSetting("Fill", "Tint the faces of the block.", true));
        this.fillColor = this.add(new ColorSetting("Fill Color", "Colour of the tint.", 0x302F6BFF, true)).visibleWhen(this.fill::isOn);
        this.pulse = this.add(new BooleanSetting("Pulse", "Gently pulse the highlight.", false));
    }

    private float pulseFactor()
    {
        if (!this.pulse.isOn())
        {
            return 1.0F;
        }

        return 0.65F + 0.35F * (float)Math.sin((double)(System.currentTimeMillis() % 1500L) / 1500.0D * Math.PI * 2.0D);
    }

    public float getWidth()
    {
        return this.width.getFloat();
    }

    /** Outline colour as {r, g, b, a}. */
    public float[] outline()
    {
        return rgba(this.outlineColor.getColor(), this.pulseFactor());
    }

    /** Fill colour as {r, g, b, a}, or null when fill is off. */
    public float[] fillColor()
    {
        return this.fill.isOn() ? rgba(this.fillColor.getColor(), this.pulseFactor()) : null;
    }

    private static float[] rgba(int c, float alphaFactor)
    {
        return new float[] {(float)((c >> 16) & 255) / 255.0F, (float)((c >> 8) & 255) / 255.0F, (float)(c & 255) / 255.0F, (float)((c >>> 24) & 255) / 255.0F * alphaFactor};
    }
}
