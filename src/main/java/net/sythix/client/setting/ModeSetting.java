package net.sythix.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * One choice out of a fixed list, cycled with clicks.
 */
public class ModeSetting extends Setting<String>
{
    private final String[] modes;

    public ModeSetting(String name, String description, String defaultValue, String... modes)
    {
        super(name, description, defaultValue);
        this.modes = modes;
    }

    public String[] getModes()
    {
        return this.modes;
    }

    public boolean is(String mode)
    {
        return this.value.equals(mode);
    }

    public int getIndex()
    {
        for (int i = 0; i < this.modes.length; ++i)
        {
            if (this.modes[i].equals(this.value))
            {
                return i;
            }
        }

        return 0;
    }

    public void cycle(int direction)
    {
        int i = (this.getIndex() + direction) % this.modes.length;
        this.set(this.modes[i < 0 ? i + this.modes.length : i]);
    }

    public JsonElement toJson()
    {
        return new JsonPrimitive(this.value);
    }

    public void fromJson(JsonElement json)
    {
        if (json != null && json.isJsonPrimitive())
        {
            String s = json.getAsString();

            for (String mode : this.modes)
            {
                if (mode.equals(s))
                {
                    this.set(mode);
                }
            }
        }
    }
}
