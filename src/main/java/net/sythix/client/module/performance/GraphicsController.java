package net.sythix.client.module.performance;

import net.minecraft.client.settings.GameSettings;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.GameOptionSetting;

/**
 * #4 Fog, clouds, weather, shadows and visual effects.
 */
public class GraphicsController extends Module
{
    public GraphicsController()
    {
        super("Graphics Controller", "Fog, clouds, weather, sky, shadows and other visual effects.", Category.PERFORMANCE, true);
        this.add(new GameOptionSetting("Fog quality, or Off.", GameSettings.Options.FOG_FANCY));
        this.add(new GameOptionSetting("How close the fog starts.", GameSettings.Options.FOG_START));
        this.add(new GameOptionSetting("Cloud quality, or Off.", GameSettings.Options.CLOUDS));
        this.add(new GameOptionSetting("Height of the clouds.", GameSettings.Options.CLOUD_HEIGHT));
        this.add(new GameOptionSetting("Rain and snow quality, or Off.", GameSettings.Options.RAIN));
        this.add(new GameOptionSetting("Draw the sky.", GameSettings.Options.SKY));
        this.add(new GameOptionSetting("Draw stars at night.", GameSettings.Options.STARS));
        this.add(new GameOptionSetting("Draw the sun and moon.", GameSettings.Options.SUN_MOON));
        this.add(new GameOptionSetting("Dark edges around the screen.", GameSettings.Options.VIGNETTE));
        this.add(new GameOptionSetting("Smooth lighting.", GameSettings.Options.AMBIENT_OCCLUSION));
        this.add(new GameOptionSetting("Smooth lighting strength.", GameSettings.Options.AO_LEVEL));
        this.add(new GameOptionSetting("Shadows under entities.", GameSettings.Options.ENTITY_SHADOWS));
        this.add(new GameOptionSetting("Grass on the sides of blocks.", GameSettings.Options.BETTER_GRASS));
        this.add(new GameOptionSetting("Leaves quality.", GameSettings.Options.TREES));
        this.add(new GameOptionSetting("Light from held torches (OptiFine).", GameSettings.Options.DYNAMIC_LIGHTS));
        this.add(new GameOptionSetting("Connected glass and other textures.", GameSettings.Options.CONNECTED_TEXTURES));
    }
}
