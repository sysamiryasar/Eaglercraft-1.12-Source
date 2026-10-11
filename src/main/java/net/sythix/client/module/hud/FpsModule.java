package net.sythix.client.module.hud;

import net.minecraft.client.Minecraft;

public class FpsModule extends TextHudModule
{
    public FpsModule()
    {
        super("FPS", "Shows your frames per second.", true, 0.005D, 0.01D);
    }

    protected String getLabel()
    {
        return "FPS";
    }

    protected String getValue()
    {
        return String.valueOf(Minecraft.getDebugFPS());
    }
}
