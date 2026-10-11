package net.sythix.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.sythix.client.ui.ColorUtil;

/**
 * An ARGB colour with an optional animated "chroma" (rainbow) mode.
 * The stored value is the ARGB colour; chroma is a separate flag.
 */
public class ColorSetting extends Setting<Integer>
{
    private final boolean defaultChroma;
    private boolean chroma;
    private final boolean allowAlpha;

    public ColorSetting(String name, String description, int defaultArgb, boolean allowAlpha)
    {
        super(name, description, Integer.valueOf(defaultArgb));
        this.allowAlpha = allowAlpha;
        this.defaultChroma = false;
    }

    public boolean allowsAlpha()
    {
        return this.allowAlpha;
    }

    public boolean isChroma()
    {
        return this.chroma;
    }

    public void setChroma(boolean chroma)
    {
        this.chroma = chroma;
    }

    public int getArgb()
    {
        return this.value.intValue();
    }

    /** Colour to draw with right now; offset shifts the rainbow phase (for example by x position). */
    public int getColor(float offset)
    {
        if (!this.chroma)
        {
            return this.getArgb();
        }

        float hue = ((float)(Minecraft.getSystemTime() % 4000L) / 4000.0F + offset) % 1.0F;
        return (this.getArgb() & 0xFF000000) | (ColorUtil.hsbToRgb(hue, 0.75F, 1.0F));
    }

    public int getColor()
    {
        return this.getColor(0.0F);
    }

    public void reset()
    {
        super.reset();
        this.chroma = this.defaultChroma;
    }

    public boolean isDefault()
    {
        return super.isDefault() && this.chroma == this.defaultChroma;
    }

    public JsonElement toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("argb", String.format("#%08X", this.getArgb()));
        json.addProperty("chroma", this.chroma);
        return json;
    }

    public void fromJson(JsonElement json)
    {
        if (json != null && json.isJsonObject())
        {
            JsonObject obj = json.getAsJsonObject();

            try
            {
                if (obj.has("argb"))
                {
                    this.set(Integer.valueOf((int)Long.parseLong(obj.get("argb").getAsString().replace("#", ""), 16)));
                }

                if (obj.has("chroma"))
                {
                    this.chroma = obj.get("chroma").getAsBoolean();
                }
            }
            catch (RuntimeException ignored)
            {
                // keep current value on malformed input
            }
        }
    }
}
