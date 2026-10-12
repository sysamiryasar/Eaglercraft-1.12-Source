package net.sythix.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Free text typed by the player (for example an Auto GG message).
 */
public class TextSetting extends Setting<String>
{
    private final int maxLength;

    public TextSetting(String name, String description, String defaultValue, int maxLength)
    {
        super(name, description, defaultValue);
        this.maxLength = maxLength;
    }

    public int getMaxLength()
    {
        return this.maxLength;
    }

    public void set(String newValue)
    {
        String v = newValue == null ? "" : newValue;
        super.set(v.length() > this.maxLength ? v.substring(0, this.maxLength) : v);
    }

    public JsonElement toJson()
    {
        return new JsonPrimitive(this.get());
    }

    public void fromJson(JsonElement json)
    {
        if (json != null && json.isJsonPrimitive())
        {
            this.set(json.getAsString());
        }
    }
}
