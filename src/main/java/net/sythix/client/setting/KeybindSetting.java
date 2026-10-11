package net.sythix.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.settings.GameSettings;

/**
 * A key code (LWJGL keyboard codes; 0 = none).
 */
public class KeybindSetting extends Setting<Integer>
{
    public static final int NONE = 0;

    public KeybindSetting(String name, String description, int defaultKey)
    {
        super(name, description, Integer.valueOf(defaultKey));
    }

    public int getKey()
    {
        return this.get().intValue();
    }

    public String getKeyName()
    {
        return this.getKey() == NONE ? "None" : GameSettings.getKeyDisplayString(this.getKey());
    }

    public JsonElement toJson()
    {
        return new JsonPrimitive(this.get());
    }

    public void fromJson(JsonElement json)
    {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber())
        {
            this.set(Integer.valueOf(json.getAsInt()));
        }
    }
}
