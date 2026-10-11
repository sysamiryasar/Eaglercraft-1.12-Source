package net.sythix.client.module.hud;

import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.setting.TextSetting;

/**
 * #34 Main menu background, logo, buttons and server shortcuts.
 */
public class MainMenuModule extends Module
{
    private final ModeSetting background;
    private final BooleanSetting parallax;
    private final NumberSetting logoSize;
    private final BooleanSetting logoAnimation;
    private final ModeSetting buttonLayout;
    private final BooleanSetting footer;
    private final TextSetting[] shortcutNames = new TextSetting[3];
    private final TextSetting[] shortcutAddresses = new TextSetting[3];

    public MainMenuModule()
    {
        super("Main Menu", "Background, logo, button layout and quick-join server shortcuts.", Category.INTERFACE, true);
        this.setCore();
        this.background = this.add(new ModeSetting("Background", "Main menu background.", "Sythix", "Sythix", "Dark", "Gradient"));
        this.parallax = this.add(new BooleanSetting("Parallax", "Background moves slightly with the mouse.", true));
        this.logoSize = this.add(new NumberSetting("Logo Size", "Size of the logo.", 100.0D, 50.0D, 130.0D, 5.0D, "%"));
        this.logoAnimation = this.add(new BooleanSetting("Logo Animation", "Gently float the logo.", true));
        this.buttonLayout = this.add(new ModeSetting("Buttons", "Where the menu buttons are.", "Center", "Center", "Left"));
        this.footer = this.add(new BooleanSetting("Footer", "Show the version bar at the bottom.", true));

        for (int i = 0; i < 3; ++i)
        {
            this.shortcutNames[i] = this.add(new TextSetting("Shortcut " + (i + 1) + " Name", "Button label for a quick-join server (empty = hidden).", "", 20));
            this.shortcutAddresses[i] = this.add(new TextSetting("Shortcut " + (i + 1) + " Address", "Server address, for example play.example.net.", "", 64));
        }
    }

    public String getBackground()
    {
        return this.background.get();
    }

    public boolean parallax()
    {
        return this.parallax.isOn();
    }

    public float logoScale()
    {
        return this.logoSize.getFloat() / 100.0F;
    }

    public boolean logoAnimation()
    {
        return this.logoAnimation.isOn();
    }

    public boolean leftButtons()
    {
        return this.buttonLayout.is("Left");
    }

    public boolean footer()
    {
        return this.footer.isOn();
    }

    /** {name, address} for each configured shortcut. */
    public java.util.List<String[]> shortcuts()
    {
        java.util.List<String[]> list = new java.util.ArrayList<String[]>();

        for (int i = 0; i < 3; ++i)
        {
            String name = this.shortcutNames[i].get().trim();
            String address = this.shortcutAddresses[i].get().trim();

            if (!name.isEmpty() && !address.isEmpty())
            {
                list.add(new String[] {name, address});
            }
        }

        return list;
    }
}
