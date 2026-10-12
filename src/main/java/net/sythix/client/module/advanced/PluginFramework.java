package net.sythix.client.module.advanced;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.plugin.PluginLoader;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.SythixTheme;

/**
 * #61 Optional plugins (desktop only): extra modules from .jar files in sythix/plugins. Off by
 * default; plugins can run any code, so only install ones you trust.
 */
public class PluginFramework extends Module
{
    private final BooleanSetting loadPlugins;

    public PluginFramework()
    {
        super("Plugin Framework", "Load extra modules from plugin jars (desktop only, off by default).", Category.ADVANCED, true);
        this.setCore();
        this.loadPlugins = this.add(new BooleanSetting("Load Plugins", "Load jars from sythix/plugins when the game starts (restart needed). Only use plugins you trust.", false));
        this.add(new InfoSetting("Loaded", "Plugins running now.", () -> PluginLoader.loaded().size() + " loaded, " + PluginLoader.failed().size() + " failed"));
        this.add(new ButtonSetting("Plugins", "See which plugins are loaded.", "Open", () -> this.mc.displayGuiScreen(new Screen(this.mc.currentScreen))));
        this.add(new ButtonSetting("Plugin Folder", "Open sythix/plugins.", "Open", () -> OpenGlHelper.openFile(PluginLoader.folder(this.mc.mcDataDir))));
        this.add(new InfoSetting("For Developers", "How to make a plugin.", () -> "Implement net.sythix.client.plugin.SythixPlugin"));
    }

    public boolean shouldLoad()
    {
        return this.loadPlugins.isOn();
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new Screen(parent);
    }

    static class Screen extends GuiListScreen
    {
        Screen(GuiScreen parent)
        {
            super(parent, "Plugins");
        }

        protected String header()
        {
            return "Put plugin .jar files in sythix/plugins and turn on Load Plugins, then restart.";
        }

        protected List<Row> rows()
        {
            List<Row> rows = new ArrayList<Row>();

            for (String p : PluginLoader.loaded())
            {
                rows.add(new Row(p, p, "Loaded", "OK", SythixTheme.BLUE));
            }

            for (String p : PluginLoader.failed())
            {
                rows.add(new Row(p, p, "Failed to load", "!", 0xFFFF4040));
            }

            return rows;
        }

        protected String emptyText()
        {
            return "No plugins loaded.";
        }

        protected String[] buttons()
        {
            return new String[] {"Open Folder", "Done"};
        }

        protected void press(int index)
        {
            if (index == 0)
            {
                OpenGlHelper.openFile(PluginLoader.folder(this.mc.mcDataDir));
            }
            else
            {
                this.mc.displayGuiScreen(this.parent);
            }
        }
    }
}
