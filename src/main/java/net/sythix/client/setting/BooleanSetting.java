package net.sythix.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean>
{
    public BooleanSetting(String name, String description, boolean defaultValue)
    {
        super(name, description, Boolean.valueOf(defaultValue));
    }

    public boolean isOn()
    {
        return this.value.booleanValue();
    }

    public void toggle()
    {
        this.set(Boolean.valueOf(!this.isOn()));
    }

    public JsonElement toJson()
    {
        return new JsonPrimitive(this.value);
    }

    public void fromJson(JsonElement json)
    {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean())
        {
            this.set(Boolean.valueOf(json.getAsBoolean()));
        }
    }
}
