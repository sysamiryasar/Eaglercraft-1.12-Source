package net.sythix.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/**
 * A clickable action in a settings list (for example "Open Waypoint Manager"). Not saved.
 */
public class ButtonSetting extends Setting<Boolean>
{
    private final Runnable action;
    private final String buttonText;

    public ButtonSetting(String name, String description, String buttonText, Runnable action)
    {
        super(name, description, Boolean.FALSE);
        this.buttonText = buttonText;
        this.action = action;
    }

    public String getButtonText()
    {
        return this.buttonText;
    }

    public void press()
    {
        this.action.run();
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
