package net.sythix.client.module.hud;

import net.minecraft.client.settings.GameSettings;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #33 Colours, fonts, panels, borders and visual style of the Sythix interface.
 */
public class InterfaceThemeModule extends Module
{
    private final BooleanSetting themed;
    private final ColorSetting primary;
    private final ColorSetting secondary;
    private final ColorSetting panel;
    private final ColorSetting text;
    private final NumberSetting opacity;
    private final BooleanSetting borders;

    public InterfaceThemeModule()
    {
        super("Interface Theme", "Colours, fonts, panels, borders and visual style.", Category.INTERFACE, true);
        this.setCore();
        this.themed = this.add(new BooleanSetting("Sythix Style", "Use the Sythix look for menus and buttons (off = vanilla).", true));
        this.primary = this.add(new ColorSetting("Primary Color", "Main accent colour (logo red by default).", 0xFFD81820, false));
        this.secondary = this.add(new ColorSetting("Secondary Color", "Second accent colour (blue by default).", 0xFF2F6BFF, false));
        this.panel = this.add(new ColorSetting("Panel Color", "Background colour of panels.", 0xFF16161B, false));
        this.text = this.add(new ColorSetting("Text Color", "Main text colour.", 0xFFE6E6EC, false));
        this.opacity = this.add(new NumberSetting("Panel Opacity", "How solid menu panels are.", 100.0D, 40.0D, 100.0D, 5.0D, "%"));
        this.borders = this.add(new BooleanSetting("Borders", "Outline buttons, cards and panels.", true));
        this.add(new GameOptionSetting("Use the Unicode font everywhere (helps some languages).", GameSettings.Options.FORCE_UNICODE_FONT));
        this.add(new GameOptionSetting("Allow resource packs to replace the font.", GameSettings.Options.CUSTOM_FONTS));
    }

    public void onTick()
    {
        this.apply();
    }

    /** Copies the chosen colours into the shared theme. */
    public void apply()
    {
        SythixTheme.enabled = this.themed.isOn();
        int p = this.primary.getArgb() | 0xFF000000;
        int s = this.secondary.getArgb() | 0xFF000000;
        SythixTheme.RED = p;
        SythixTheme.RED_DARK = SythixTheme.lerpColor(p, 0xFF000000, 0.35F);
        SythixTheme.BLUE = s;
        SythixTheme.LIGHT_BLUE = SythixTheme.lerpColor(s, 0xFFFFFFFF, 0.4F);
        SythixTheme.CHARCOAL = this.panel.getArgb() | 0xFF000000;
        SythixTheme.CHARCOAL_LIGHT = SythixTheme.lerpColor(SythixTheme.CHARCOAL, 0xFFFFFFFF, 0.05F);
        SythixTheme.BLACK = SythixTheme.lerpColor(SythixTheme.CHARCOAL, 0xFF000000, 0.5F);
        SythixTheme.TEXT = this.text.getArgb() | 0xFF000000;
        SythixTheme.panelOpacity = this.opacity.getFloat() / 100.0F;
        SythixTheme.borders = this.borders.isOn();
    }
}
