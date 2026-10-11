package net.sythix.client.module.client;

import net.minecraft.client.settings.GameSettings;
import net.sythix.client.core.Hooks;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #55 Readable text, high contrast, reduced motion and colour-blind friendly accents.
 */
public class Accessibility extends Module
{
    private final NumberSetting hudScale;
    private final BooleanSetting highContrast;
    private final BooleanSetting reducedMotion;
    private final ModeSetting colorBlind;

    public Accessibility()
    {
        super("Accessibility", "Bigger HUD text, high contrast, reduced motion and colour-blind accents.", Category.CLIENT, true);
        this.setCore();
        this.hudScale = this.add(new NumberSetting("HUD Size", "Makes every Sythix HUD element bigger or smaller.", 1.0D, 0.75D, 2.0D, 0.05D, "x"));
        this.highContrast = this.add(new BooleanSetting("High Contrast", "Solid dark boxes and shadowed text on every HUD element.", false));
        this.reducedMotion = this.add(new BooleanSetting("Reduced Motion", "No sliding or fading animations in Sythix menus.", false));
        this.colorBlind = this.add(new ModeSetting("Colour-blind Accents", "Swap red/blue accents for colours that are easier to tell apart.", "Off", "Off", "Red-Green", "Blue-Yellow", "Monochrome"));
        this.add(new GameOptionSetting("Size of all menus and the vanilla HUD.", GameSettings.Options.GUI_SCALE));
        this.add(new GameOptionSetting("Use the Unicode font everywhere.", GameSettings.Options.FORCE_UNICODE_FONT));
        this.add(new GameOptionSetting("Show sound captions on screen.", GameSettings.Options.SHOW_SUBTITLES));
        this.add(new GameOptionSetting("Read chat and system messages aloud.", GameSettings.Options.NARRATOR));
        this.add(new GameOptionSetting("How the attack cooldown is shown.", GameSettings.Options.ATTACK_INDICATOR));
    }

    /** Runs after Interface Theme (registered later), so its colours win when a mode is set. */
    public void onTick()
    {
        SythixTheme.reducedMotion = this.reducedMotion.isOn();

        switch (this.colorBlind.get())
        {
            case "Red-Green":
                SythixTheme.RED = 0xFFFF9F1C;
                SythixTheme.RED_DARK = 0xFFB86B00;
                SythixTheme.BLUE = 0xFF1E64FF;
                SythixTheme.LIGHT_BLUE = 0xFF7FB2FF;
                break;
            case "Blue-Yellow":
                SythixTheme.RED = 0xFFE0115F;
                SythixTheme.RED_DARK = 0xFF9A0B41;
                SythixTheme.BLUE = 0xFF00B3A4;
                SythixTheme.LIGHT_BLUE = 0xFF66DDD3;
                break;
            case "Monochrome":
                SythixTheme.RED = 0xFFFFFFFF;
                SythixTheme.RED_DARK = 0xFF9A9AA6;
                SythixTheme.BLUE = 0xFFB0B0B8;
                SythixTheme.LIGHT_BLUE = 0xFFDADAE0;
                break;
            default:
        }
    }

    private static Accessibility active()
    {
        return Hooks.on(Accessibility.class);
    }

    public static float hudScale()
    {
        Accessibility a = active();
        return a == null ? 1.0F : a.hudScale.getFloat();
    }

    public static boolean highContrast()
    {
        Accessibility a = active();
        return a != null && a.highContrast.isOn();
    }
}
