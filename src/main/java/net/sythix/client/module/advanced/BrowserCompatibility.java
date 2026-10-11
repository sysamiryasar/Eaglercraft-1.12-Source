package net.sythix.client.module.advanced;

import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.SythixClient;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.ui.GuiTextPage;

/**
 * #64 Browser readiness. This source builds the desktop (Java) client; there is no browser
 * (WebGL) build in it yet. This module says so plainly and lists which of your settings would
 * cause problems in a browser, so a setup can be prepared for a future web build.
 */
public class BrowserCompatibility extends Module
{
    public BrowserCompatibility()
    {
        super("Browser Compatibility", "Platform info and checks for settings that would not work in a browser.", Category.ADVANCED, true);
        this.setCore();
        this.add(new InfoSetting("Platform", "What you are running now.", () -> "Desktop (Java " + System.getProperty("java.version") + ", LWJGL 2)"));
        this.add(new InfoSetting("Browser Build", "Whether a web version exists.", () -> "Not in this source yet (desktop only)"));
        this.add(new ButtonSetting("Readiness Check", "See which settings would need changing for a browser.", "Open", () -> this.mc.displayGuiScreen(this.createScreen(this.mc.currentScreen))));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiTextPage(parent, "Browser Readiness", this::report);
    }

    private String line(boolean ok, String text, String fix)
    {
        return (ok ? "§aOK§r   " : "§6CHANGE§r   ") + text + (ok ? "" : " - " + fix) + "\n";
    }

    private String report()
    {
        StringBuilder b = new StringBuilder();
        b.append("# Status\n");
        b.append(SythixClient.FULL_NAME).append(" is currently a desktop Java client. Browsers cannot run it directly; a web build needs the game compiled to JavaScript/WebAssembly with WebGL rendering and WebSocket networking, which this project does not include yet.\n\n");
        b.append("# Settings that matter in a browser\n");
        PluginFramework plugins = net.sythix.client.core.Hooks.on(PluginFramework.class);
        b.append(this.line(this.mc.gameSettings.renderDistanceChunks <= 8, "Render distance " + this.mc.gameSettings.renderDistanceChunks, "browsers usually manage 8 chunks or less"));
        b.append(this.line(!net.minecraft.src.Config.isShaders(), "Shader packs", "shader packs are desktop only"));
        b.append(this.line(plugins == null || !plugins.shouldLoad(), "Plugins", "plugin jars are desktop only"));
        b.append(this.line(this.mc.gameSettings.limitFramerate <= 60 || this.mc.gameSettings.enableVsync, "Frame rate limit " + this.mc.gameSettings.limitFramerate, "browsers are tied to the screen refresh (VSync)"));
        b.append(this.line(this.mc.getResourcePackRepository().getRepositoryEntries().size() <= 2, "Resource packs (" + this.mc.getResourcePackRepository().getRepositoryEntries().size() + " active)", "keep packs small - they are loaded into browser memory"));
        b.append("\n# Works the same everywhere\n");
        b.append("Module settings, HUD layouts, profiles, waypoints and keybinds are plain JSON in the sythix folder, so they can be moved to a future web build unchanged.\n");
        return b.toString();
    }
}
