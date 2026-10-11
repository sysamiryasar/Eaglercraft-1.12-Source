package net.sythix.client.module.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.sythix.client.module.Category;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * A HUD element made of one or more "label value" lines, such as "FPS 144".
 */
public abstract class TextHudModule extends StyledHudModule
{
    protected final ModeSetting labelMode;
    protected final ModeSetting brackets;
    protected final ModeSetting textAlign;
    protected final NumberSetting minWidth;

    protected TextHudModule(String name, String description, Category category, boolean defaultEnabled, double defaultX, double defaultY)
    {
        super(name, description, category, defaultEnabled, defaultX, defaultY);
        this.labelMode = this.add(1, new ModeSetting("Label", "Where labels are shown.", "Before", "Before", "After", "Hidden"));
        this.brackets = this.add(2, new ModeSetting("Brackets", "Surround each line with brackets.", "None", "None", "[ ]", "( )", "< >"));
        this.textAlign = this.add(3, new ModeSetting("Text Align", "Alignment of the text inside the box.", "Center", "Left", "Center", "Right"));
        this.minWidth = this.add(new NumberSetting("Min Width", "Keeps the box from changing size as values change. 0 = automatic.", 0.0D, 0.0D, 200.0D, 1.0D, "px"));
    }

    protected TextHudModule(String name, String description, boolean defaultEnabled, double defaultX, double defaultY)
    {
        this(name, description, Category.INTERFACE, defaultEnabled, defaultX, defaultY);
    }

    /** Short label for single-line elements, for example "FPS". */
    protected String getLabel()
    {
        return this.getName();
    }

    /** Value for single-line elements. */
    protected String getValue()
    {
        return "";
    }

    /** Colour of the value on the given line. */
    protected int getValueColor(int line)
    {
        return this.valueColor.getColor((float)line * 0.1F);
    }

    /** Lines as {label, value} pairs. Single-line elements use getLabel and getValue. */
    protected List<String[]> getLines()
    {
        return Collections.singletonList(new String[] {this.getLabel(), this.getValue()});
    }

    /** Splits a {label, value} pair into {open, label, value, label-after, close}. */
    private String[] parts(String[] line)
    {
        String open = "";
        String close = "";

        if (!this.brackets.is("None"))
        {
            open = this.brackets.get().substring(0, 1);
            close = this.brackets.get().substring(2, 3);
        }

        String label = line[0];
        String value = line[1];

        if (this.labelMode.is("Hidden") || label.isEmpty())
        {
            return new String[] {open, "", value, "", close};
        }
        else if (this.labelMode.is("After"))
        {
            return new String[] {open, "", value, " " + label, close};
        }

        return new String[] {open, label + (value.isEmpty() ? "" : " "), value, "", close};
    }

    private int lineWidth(String[] parts)
    {
        FontRenderer font = this.mc.fontRenderer;
        int width = 0;

        for (String part : parts)
        {
            width += font.getStringWidth(part);
        }

        return width;
    }

    private List<String[]> partLines()
    {
        List<String[]> result = new ArrayList<String[]>();

        for (String[] line : this.getLines())
        {
            result.add(this.parts(line));
        }

        return result;
    }

    public int getWidth()
    {
        int max = 0;

        for (String[] parts : this.partLines())
        {
            max = Math.max(max, this.lineWidth(parts));
        }

        return Math.max(this.minWidth.getInt(), max + this.getPad() * 2);
    }

    public int getHeight()
    {
        int lines = Math.max(1, this.getLines().size());
        return lines * 10 - 2 + this.getPad() * 2;
    }

    protected void renderContent(boolean preview)
    {
        int width = this.getWidth();
        this.drawBox(width, this.getHeight());
        FontRenderer font = this.mc.fontRenderer;
        List<String[]> lines = this.partLines();
        int labelArgb = this.labelColor.getColor();

        for (int i = 0; i < lines.size(); ++i)
        {
            String[] parts = lines.get(i);
            int textWidth = this.lineWidth(parts);
            float x;

            if (this.textAlign.is("Left"))
            {
                x = (float)this.getPad();
            }
            else if (this.textAlign.is("Right"))
            {
                x = (float)(width - this.getPad() - textWidth);
            }
            else
            {
                x = (float)(width - textWidth) / 2.0F;
            }

            float y = (float)(this.getPad() + i * 10) + (this.background.isOn() ? 0.5F : 0.0F);
            int[] colors = new int[] {labelArgb, labelArgb, this.getValueColor(i), labelArgb, labelArgb};

            for (int p = 0; p < parts.length; ++p)
            {
                if (!parts[p].isEmpty())
                {
                    this.drawText(parts[p], x, y, colors[p]);
                    x += (float)font.getStringWidth(parts[p]);
                }
            }
        }
    }
}
