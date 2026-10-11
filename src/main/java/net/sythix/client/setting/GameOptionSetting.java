package net.sythix.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.GameSettings;

/**
 * A live view of one of Minecraft's (or OptiFine's) own options. Changing it changes the real game
 * option, which Minecraft saves in options.txt / optionsof.txt, so it is not stored by Sythix.
 */
public class GameOptionSetting extends Setting<String>
{
    private final GameSettings.Options option;

    public GameOptionSetting(String description, GameSettings.Options option)
    {
        super(nameOf(option), description, "");
        this.option = option;
    }

    private static String nameOf(GameSettings.Options option)
    {
        try
        {
            String full = Minecraft.getMinecraft().gameSettings.getKeyBinding(option);
            int colon = full.indexOf(':');
            return colon > 0 ? full.substring(0, colon).trim() : full;
        }
        catch (RuntimeException e)
        {
            return option.name();
        }
    }

    private static GameSettings gs()
    {
        return Minecraft.getMinecraft().gameSettings;
    }

    public GameSettings.Options getOption()
    {
        return this.option;
    }

    public boolean isSlider()
    {
        return this.option.getEnumFloat();
    }

    public double getPercent()
    {
        return (double)this.option.normalizeValue(gs().getOptionFloatValue(this.option));
    }

    public void setPercent(double percent)
    {
        gs().setOptionFloatValue(this.option, this.option.denormalizeValue((float)percent));
    }

    public void cycle()
    {
        gs().setOptionValue(this.option, 1);

        if (this.option == GameSettings.Options.GUI_SCALE)
        {
            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution res = new ScaledResolution(mc);

            if (mc.currentScreen != null)
            {
                mc.currentScreen.setWorldAndResolution(mc, res.getScaledWidth(), res.getScaledHeight());
            }
        }
    }

    /** The current value as Minecraft shows it, without the "Name:" prefix. */
    public String getDisplay()
    {
        String full = gs().getKeyBinding(this.option);
        int colon = full.indexOf(':');
        return colon >= 0 ? full.substring(colon + 1).trim() : full;
    }

    public String get()
    {
        return this.getDisplay();
    }

    public boolean isDefault()
    {
        return true;
    }

    public JsonElement toJson()
    {
        return JsonNull.INSTANCE;
    }

    public void fromJson(JsonElement json)
    {
    }
}
