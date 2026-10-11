package net.sythix.client.module.visuals;

import net.minecraft.client.settings.GameSettings;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #35 Brightness, time of day, weather appearance and fog - all only on your screen.
 */
public class EnvironmentVisuals extends Module
{
    private final BooleanSetting fullbright;
    private final NumberSetting brightness;
    private final ModeSetting time;
    private final NumberSetting customTime;
    private final ModeSetting weather;

    public EnvironmentVisuals()
    {
        super("Environment Visuals", "Brightness, time of day, weather and fog (only on your screen).", Category.VISUALS, false);
        this.fullbright = this.add(new BooleanSetting("Fullbright", "See clearly in dark places.", false));
        this.brightness = this.add(new NumberSetting("Brightness", "How bright Fullbright makes things.", 10.0D, 1.0D, 15.0D, 1.0D)).visibleWhen(this.fullbright::isOn);
        this.time = this.add(new ModeSetting("Time", "Time of day you see (the server's time is unchanged).", "Server", "Server", "Day", "Sunset", "Night", "Midnight", "Custom"));
        this.customTime = this.add(new NumberSetting("Custom Time", "Time of day for Custom (0 = sunrise, 6000 = noon).", 6000.0D, 0.0D, 23999.0D, 250.0D)).visibleWhen(() -> this.time.is("Custom"));
        this.weather = this.add(new ModeSetting("Weather", "Weather you see.", "Server", "Server", "Clear", "Rain", "Thunder"));
        this.add(new GameOptionSetting("Fog quality, or Off.", GameSettings.Options.FOG_FANCY));
        this.add(new GameOptionSetting("How close the fog starts.", GameSettings.Options.FOG_START));
    }

    /** Gamma to use instead of the brightness setting, or -1 for vanilla. */
    public float gamma()
    {
        return this.fullbright.isOn() ? this.brightness.getFloat() : -1.0F;
    }

    /** World time to display, or -1 to use the server's. */
    public long worldTime()
    {
        switch (this.time.get())
        {
            case "Day":
                return 6000L;
            case "Sunset":
                return 12500L;
            case "Night":
                return 14500L;
            case "Midnight":
                return 18000L;
            case "Custom":
                return (long)this.customTime.getInt();
            default:
                return -1L;
        }
    }

    /** Rain strength to display, or -1 to use the server's. */
    public float rain()
    {
        return this.weather.is("Server") ? -1.0F : (this.weather.is("Clear") ? 0.0F : 1.0F);
    }

    public float thunder()
    {
        return this.weather.is("Server") ? -1.0F : (this.weather.is("Thunder") ? 1.0F : 0.0F);
    }
}
