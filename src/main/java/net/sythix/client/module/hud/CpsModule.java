package net.sythix.client.module.hud;

import net.sythix.client.SythixClient;
import net.sythix.client.setting.ModeSetting;

public class CpsModule extends TextHudModule
{
    private final ModeSetting display;

    public CpsModule()
    {
        super("CPS", "Shows your clicks per second.", true, 0.005D, 0.075D);
        this.display = this.add(new ModeSetting("Buttons", "Which mouse buttons to count.", "Left | Right", "Left | Right", "Left", "Right", "Combined"));
    }

    protected String getLabel()
    {
        return "CPS";
    }

    protected String getValue()
    {
        int left = SythixClient.get().getClickCounter().getLeftCps();
        int right = SythixClient.get().getClickCounter().getRightCps();

        if (this.display.is("Left"))
        {
            return String.valueOf(left);
        }
        else if (this.display.is("Right"))
        {
            return String.valueOf(right);
        }
        else if (this.display.is("Combined"))
        {
            return String.valueOf(left + right);
        }

        return left + " | " + right;
    }
}
