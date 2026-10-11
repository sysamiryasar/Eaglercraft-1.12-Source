package net.sythix.client.module.hud;

import net.sythix.client.SythixClient;
import net.sythix.client.module.Category;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #8 Left/right clicks per second.
 */
public class CpsModule extends TextHudModule
{
    private final ModeSetting display;
    private final NumberSetting window;
    private final NumberSetting decimals;

    public CpsModule()
    {
        super("CPS Counter", "Shows your left and right clicks per second.", Category.PVP, true, 0.005D, 0.075D);
        this.display = this.add(new ModeSetting("Buttons", "Which mouse buttons to count.", "Left | Right", "Left | Right", "Left", "Right", "Combined"));
        this.window = this.add(new NumberSetting("Time Window", "Clicks are averaged over this long. 1s matches most clients.", 1.0D, 0.5D, 3.0D, 0.25D, "s"));
        this.decimals = this.add(new NumberSetting("Decimals", "Digits after the decimal point (useful with longer windows).", 0.0D, 0.0D, 1.0D, 1.0D));
    }

    protected String getLabel()
    {
        return "CPS";
    }

    private String fmt(float v)
    {
        return this.decimals.getInt() == 0 ? String.valueOf(Math.round(v)) : String.format("%.1f", v);
    }

    protected String getValue()
    {
        long ms = (long)(this.window.getFloat() * 1000.0F);
        float left = SythixClient.get().getClickCounter().getLeft(ms);
        float right = SythixClient.get().getClickCounter().getRight(ms);

        if (this.display.is("Left"))
        {
            return this.fmt(left);
        }
        else if (this.display.is("Right"))
        {
            return this.fmt(right);
        }
        else if (this.display.is("Combined"))
        {
            return this.fmt(left + right);
        }

        return this.fmt(left) + " | " + this.fmt(right);
    }
}
