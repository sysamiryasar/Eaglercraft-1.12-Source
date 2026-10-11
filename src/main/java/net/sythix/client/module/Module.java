package net.sythix.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.Setting;

/**
 * A client feature that can be switched on and off and has its own settings.
 */
public abstract class Module
{
    protected final Minecraft mc = Minecraft.getMinecraft();
    private final String name;
    private final String description;
    private final Category category;
    private final boolean defaultEnabled;
    private boolean enabled;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    protected final KeybindSetting toggleKey;

    protected Module(String name, String description, Category category, boolean defaultEnabled)
    {
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultEnabled = defaultEnabled;
        this.enabled = defaultEnabled;
        this.toggleKey = new KeybindSetting("Toggle Key", "Press this key in game to switch " + name + " on or off.", KeybindSetting.NONE);
    }

    /** Registers a setting so it shows up in menus, is saved and can be reset. */
    protected <S extends Setting<?>> S add(S setting)
    {
        this.settings.add(setting);
        return setting;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public Category getCategory()
    {
        return this.category;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        if (this.enabled != enabled)
        {
            this.enabled = enabled;

            if (enabled)
            {
                this.onEnable();
            }
            else
            {
                this.onDisable();
            }
        }
    }

    public void toggle()
    {
        this.setEnabled(!this.enabled);
    }

    protected void onEnable()
    {
    }

    protected void onDisable()
    {
    }

    /** All settings, with the toggle key last. */
    public List<Setting<?>> getSettings()
    {
        List<Setting<?>> list = new ArrayList<Setting<?>>(this.settings);
        list.add(this.toggleKey);
        return Collections.unmodifiableList(list);
    }

    public KeybindSetting getToggleKey()
    {
        return this.toggleKey;
    }

    public void resetToDefaults()
    {
        for (Setting<?> setting : this.getSettings())
        {
            setting.reset();
        }

        this.setEnabled(this.defaultEnabled);
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", this.enabled);
        JsonObject values = new JsonObject();

        for (Setting<?> setting : this.getSettings())
        {
            values.add(setting.getName(), setting.toJson());
        }

        json.add("settings", values);
        return json;
    }

    public void fromJson(JsonObject json)
    {
        if (json.has("enabled") && json.get("enabled").isJsonPrimitive())
        {
            this.setEnabled(json.get("enabled").getAsBoolean());
        }

        if (json.has("settings") && json.get("settings").isJsonObject())
        {
            JsonObject values = json.getAsJsonObject("settings");

            for (Setting<?> setting : this.getSettings())
            {
                JsonElement element = values.get(setting.getName());

                if (element != null)
                {
                    setting.fromJson(element);
                }
            }
        }
    }
}
