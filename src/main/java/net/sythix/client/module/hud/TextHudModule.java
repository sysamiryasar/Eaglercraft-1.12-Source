package net.sythix.client.module.hud;

import net.minecraft.client.gui.FontRenderer;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * A HUD element that shows a label and a value, such as "FPS 144", with shared styling options.
 */
public abstract class TextHudModule extends HudModule
{
    protected final ModeSetting labelMode;
    protected final ModeSetting brackets;
    protected final ColorSetting labelColor;
    protected final ColorSetting valueColor;
    protected final BooleanSetting textShadow;
    protected final BooleanSetting background;
    protected final ColorSetting backgroundColor;
    protected final BooleanSetting accentLine;
    protected final NumberSetting padding;
    protected final NumberSetting minWidth;

    protected TextHudModule(String name, String description, boolean defaultEnabled, double defaultX, double defaultY)
    {
        super(name, description, defaultEnabled, defaultX, defaultY);
        this.labelMode = this.add(new ModeSetting("Label", "Where the label (for example \"FPS\") is shown.", "Before", "Before", "After", "Hidden"));
        this.brackets = this.add(new ModeSetting("Brackets", "Surround the text with brackets.", "None", "None", "[ ]", "( )", "< >"));
        this.labelColor = this.add(new ColorSetting("Label Color", "Colour of the label and brackets.", SythixTheme.RED | 0xFF000000, false));
        this.valueColor = this.add(new ColorSetting("Value Color", "Colour of the number.", 0xFFFFFFFF, false));
        this.textShadow = this.add(new BooleanSetting("Text Shadow", "Draw a shadow behind the text.", true));
        this.background = this.add(new BooleanSetting("Background", "Draw a box behind the text.", true));
        this.backgroundColor = this.add(new ColorSetting("Background Color", "Colour and transparency of the box.", 0xA016161B, true)).visibleWhen(this.background::isOn);
        this.accentLine = this.add(new BooleanSetting("Accent Line", "Red-to-blue line along the bottom of the box.", true)).visibleWhen(this.background::isOn);
        this.padding = this.add(new NumberSetting("Padding", "Space between the text and the edge of the box.", 4.0D, 0.0D, 10.0D, 1.0D, "px")).visibleWhen(this.background::isOn);
        this.minWidth = this.add(new NumberSetting("Min Width", "Keeps the box from changing size as the number changes. 0 = automatic.", 0.0D, 0.0D, 150.0D, 1.0D, "px"));
    }

    /** Short label, for example "FPS". */
    protected abstract String getLabel();

    /** Current value text. */
    protected abstract String getValue();

    /** Colour for the value; subclasses can override (for example ping colours). */
    protected int getValueColor()
    {
        return this.valueColor.getColor();
    }

    private String[] getParts()
    {
        String open = "";
        String close = "";

        if (!this.brackets.is("None"))
        {
            open = this.brackets.get().substring(0, 1);
            close = this.brackets.get().substring(2, 3);
        }

        String label = this.getLabel();
        String value = this.getValue();

        if (this.labelMode.is("Hidden"))
        {
            return new String[] {open, "", value, "", close};
        }
        else if (this.labelMode.is("After"))
        {
            return new String[] {open, "", value, " " + label, close};
        }
        else
        {
            return new String[] {open, label + " ", value, "", close};
        }
    }

    private int getTextWidth()
    {
        FontRenderer font = this.mc.fontRenderer;
        int width = 0;

        for (String part : this.getParts())
        {
            width += font.getStringWidth(part);
        }

        return width;
    }

    private int getPad()
    {
        return this.background.isOn() ? this.padding.getInt() : 0;
    }

    public int getWidth()
    {
        return Math.max(this.minWidth.getInt(), this.getTextWidth() + this.getPad() * 2);
    }

    public int getHeight()
    {
        return 8 + this.getPad() * 2;
    }

    protected void renderContent(boolean preview)
    {
        int width = this.getWidth();
        int height = this.getHeight();

        if (this.background.isOn())
        {
            SythixTheme.drawRect(0.0F, 0.0F, (float)width, (float)height, this.backgroundColor.getColor());

            if (this.accentLine.isOn())
            {
                SythixTheme.drawAccentLine(0.0F, (float)(height - 1), (float)width, 1.0F);
            }
        }

        FontRenderer font = this.mc.fontRenderer;
        String[] parts = this.getParts();
        float x = (float)(width - this.getTextWidth()) / 2.0F;
        float y = (float)this.getPad() + (this.background.isOn() ? 0.5F : 0.0F);
        int labelArgb = this.labelColor.getColor();
        int[] colors = new int[] {labelArgb, labelArgb, this.getValueColor(), labelArgb, labelArgb};

        for (int i = 0; i < parts.length; ++i)
        {
            if (!parts[i].isEmpty())
            {
                font.drawString(parts[i], x, y, colors[i] | 0xFF000000, this.textShadow.isOn());
                x += (float)font.getStringWidth(parts[i]);
            }
        }
    }
}
