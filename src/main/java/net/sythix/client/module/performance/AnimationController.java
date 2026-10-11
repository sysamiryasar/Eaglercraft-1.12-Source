package net.sythix.client.module.performance;

import net.minecraft.client.settings.GameSettings;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.GameOptionSetting;

/**
 * #2 Water, lava, fire, terrain, item and entity animations.
 */
public class AnimationController extends Module
{
    private final BooleanSetting itemSpin;
    private final BooleanSetting itemBob;

    public AnimationController()
    {
        super("Animation Controller", "Turn water, lava, fire, terrain, item and entity animations on or off.", Category.PERFORMANCE, true);
        this.add(new GameOptionSetting("Animated water texture.", GameSettings.Options.ANIMATED_WATER));
        this.add(new GameOptionSetting("Animated lava texture.", GameSettings.Options.ANIMATED_LAVA));
        this.add(new GameOptionSetting("Animated fire texture.", GameSettings.Options.ANIMATED_FIRE));
        this.add(new GameOptionSetting("Animated nether portal texture.", GameSettings.Options.ANIMATED_PORTAL));
        this.add(new GameOptionSetting("Animated terrain textures (all other animated blocks).", GameSettings.Options.ANIMATED_TERRAIN));
        this.add(new GameOptionSetting("All animated textures, including items.", GameSettings.Options.ANIMATED_TEXTURES));
        this.add(new GameOptionSetting("Redstone dust particles.", GameSettings.Options.ANIMATED_REDSTONE));
        this.add(new GameOptionSetting("Explosion animation.", GameSettings.Options.ANIMATED_EXPLOSION));
        this.add(new GameOptionSetting("Torch and furnace flames.", GameSettings.Options.ANIMATED_FLAME));
        this.add(new GameOptionSetting("Smoke particles.", GameSettings.Options.ANIMATED_SMOKE));
        this.itemSpin = this.add(new BooleanSetting("Dropped Item Spin", "Dropped items rotate on the ground.", true));
        this.itemBob = this.add(new BooleanSetting("Dropped Item Bob", "Dropped items float up and down.", true));
    }

    public boolean itemSpin()
    {
        return this.itemSpin.isOn();
    }

    public boolean itemBob()
    {
        return this.itemBob.isOn();
    }
}
