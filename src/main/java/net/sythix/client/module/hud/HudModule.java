package net.sythix.client.module.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.NumberSetting;

/**
 * A module drawn on the in-game HUD that can be moved and scaled in the HUD editor.
 *
 * The position is stored as a fraction (0..1) of the free space on screen, so elements stay
 * in the same relative place at any window size or GUI scale.
 */
public abstract class HudModule extends Module
{
    private final double defaultX;
    private final double defaultY;
    private double posX;
    private double posY;
    protected final NumberSetting scale;
    protected final NumberSetting layer;

    protected HudModule(String name, String description, boolean defaultEnabled, double defaultX, double defaultY)
    {
        this(name, description, Category.INTERFACE, defaultEnabled, defaultX, defaultY);
    }

    protected HudModule(String name, String description, Category category, boolean defaultEnabled, double defaultX, double defaultY)
    {
        super(name, description, category, defaultEnabled);
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.posX = defaultX;
        this.posY = defaultY;
        this.scale = this.add(new NumberSetting("Scale", "Size of the element. You can also scroll over it in the HUD editor.", 1.0D, 0.5D, 3.0D, 0.05D, "x"));
        this.layer = this.add(new NumberSetting("Layer", "Elements on higher layers are drawn on top of lower ones.", 0.0D, 0.0D, 10.0D, 1.0D));
    }

    /** Unscaled width of the element. */
    public abstract int getWidth();

    /** Unscaled height of the element. */
    public abstract int getHeight();

    /** Draws the element at (0, 0) in its own unscaled coordinates. preview is true in the HUD editor. */
    protected abstract void renderContent(boolean preview);

    /** Whether the element has something to show right now (in game). Hidden elements are skipped. */
    public boolean shouldRender()
    {
        return true;
    }

    public NumberSetting getScaleSetting()
    {
        return this.scale;
    }

    public int getLayer()
    {
        return this.layer.getInt();
    }

    public float getScale()
    {
        return this.scale.getFloat() * net.sythix.client.module.client.Accessibility.hudScale();
    }

    public float getScaledWidth()
    {
        return (float)this.getWidth() * this.getScale();
    }

    public float getScaledHeight()
    {
        return (float)this.getHeight() * this.getScale();
    }

    public float getScreenX(ScaledResolution res)
    {
        double x = net.sythix.client.module.advanced.Localization.mirrorHud() ? 1.0D - this.posX : this.posX;
        return (float)(x * Math.max(0.0F, (float)res.getScaledWidth() - this.getScaledWidth()));
    }

    public float getScreenY(ScaledResolution res)
    {
        return (float)(this.posY * Math.max(0.0F, (float)res.getScaledHeight() - this.getScaledHeight()));
    }

    /** Moves the element so its top-left corner is at (x, y), clamped to the screen. */
    public void setScreenPosition(float x, float y, ScaledResolution res)
    {
        float freeX = (float)res.getScaledWidth() - this.getScaledWidth();
        float freeY = (float)res.getScaledHeight() - this.getScaledHeight();
        this.posX = freeX <= 0.0F ? 0.0D : Math.max(0.0D, Math.min(1.0D, (double)(x / freeX)));

        if (net.sythix.client.module.advanced.Localization.mirrorHud())
        {
            this.posX = 1.0D - this.posX;
        }
        this.posY = freeY <= 0.0F ? 0.0D : Math.max(0.0D, Math.min(1.0D, (double)(y / freeY)));
    }

    public void render(ScaledResolution res, boolean preview)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translate(this.getScreenX(res), this.getScreenY(res), 0.0F);
        GlStateManager.scale(this.getScale(), this.getScale(), 1.0F);
        this.renderContent(preview);
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void resetPosition()
    {
        this.posX = this.defaultX;
        this.posY = this.defaultY;
    }

    public void resetToDefaults()
    {
        super.resetToDefaults();
        this.resetPosition();
    }

    public JsonObject toJson()
    {
        JsonObject json = super.toJson();
        JsonArray position = new JsonArray();
        position.add(Double.valueOf(this.posX));
        position.add(Double.valueOf(this.posY));
        json.add("position", position);
        return json;
    }

    public void fromJson(JsonObject json)
    {
        super.fromJson(json);

        if (json.has("position") && json.get("position").isJsonArray() && json.getAsJsonArray("position").size() == 2)
        {
            try
            {
                this.posX = Math.max(0.0D, Math.min(1.0D, json.getAsJsonArray("position").get(0).getAsDouble()));
                this.posY = Math.max(0.0D, Math.min(1.0D, json.getAsJsonArray("position").get(1).getAsDouble()));
            }
            catch (RuntimeException ignored)
            {
                this.resetPosition();
            }
        }
    }
}
