package net.sythix.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import java.util.function.Supplier;

/**
 * A read-only line of text in a settings list (status, version, diagnostics). Not saved.
 */
public class InfoSetting extends Setting<String>
{
    private final Supplier<String> text;

    public InfoSetting(String name, String description, Supplier<String> text)
    {
        super(name, description, "");
        this.text = text;
    }

    public String getText()
    {
        try
        {
            return this.text.get();
        }
        catch (RuntimeException e)
        {
            return "n/a";
        }
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
