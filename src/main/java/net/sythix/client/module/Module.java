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
    private boolean favorite;

    /** Core modules are part of the client itself and cannot be switched off. */
    private boolean core;

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

    /** Registers a setting at a specific position in the menu order. */
    protected <S extends Setting<?>> S add(int index, S setting)
    {
        this.settings.add(Math.min(index, this.settings.size()), setting);
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
        return this.enabled || this.core;
    }

    /** Marks this module as always on (used by client infrastructure modules). */
    protected void setCore()
    {
        this.core = true;
        this.enabled = true;
    }

    public boolean isCore()
    {
        return this.core;
    }

    public boolean isFavorite()
    {
        return this.favorite;
    }

    public void setFavorite(boolean favorite)
    {
        this.favorite = favorite;
    }

    /** A screen this module opens from the mod menu (for example the waypoint manager), or null. */
    public net.minecraft.client.gui.GuiScreen createScreen(net.minecraft.client.gui.GuiScreen parent)
    {
        return null;
    }

    public void setEnabled(boolean enabled)
    {
        if (this.core)
        {
            return;
        }

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

    /** Called every client tick (20 times a second) while enabled. */
    public void onTick()
    {
    }

    /** Called when the player attacks an entity, while enabled. */
    public void onAttack(net.minecraft.entity.Entity target)
    {
    }

    /** Called for every key press with no menu open, while enabled. */
    public void onKey(int keyCode)
    {
    }

    /** Called for each mouse button press with no menu open (0 = left, 1 = right), while enabled. */
    public void onMouseClick(int button)
    {
    }

    /** Called when a key is released with no menu open, while enabled. */
    public void onKeyRelease(int keyCode)
    {
    }

    /** Called for each chat message received from the server, while enabled. */
    public void onChat(net.minecraft.util.text.ITextComponent message)
    {
    }

    /** Called when the player joins a world or server, while enabled. */
    public void onWorldJoin()
    {
    }

    /** Called every frame after the world is drawn (3D), while enabled. */
    public void onRenderWorld(float partialTicks)
    {
    }

    /** True if this module draws a live preview in the mod menu (see renderPreview). */
    public boolean hasPreview()
    {
        return false;
    }

    /** Draws a preview inside the given box in the mod menu. */
    public void renderPreview(int x, int y, int width, int height)
    {
    }

    /** All settings, with the toggle key last (core modules have no toggle key). */
    public List<Setting<?>> getSettings()
    {
        List<Setting<?>> list = new ArrayList<Setting<?>>(this.settings);

        if (!this.core)
        {
            list.add(this.toggleKey);
        }

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
        json.addProperty("favorite", this.favorite);
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

        if (json.has("favorite") && json.get("favorite").isJsonPrimitive())
        {
            this.favorite = json.get("favorite").getAsBoolean();
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
