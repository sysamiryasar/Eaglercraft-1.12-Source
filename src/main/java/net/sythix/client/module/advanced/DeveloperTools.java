package net.sythix.client.module.advanced;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.SythixClient;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.module.ModuleManager;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.Setting;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.GuiTextPage;
import net.sythix.client.ui.SythixTheme;
import org.apache.commons.io.FileUtils;

/**
 * #62 Tools for testing and making modules: event timings, the game log, a module dump and
 * config reloading.
 */
public class DeveloperTools extends Module
{
    private final BooleanSetting timings;

    public DeveloperTools()
    {
        super("Developer Tools", "Module timings, log viewer, module dump and config reload.", Category.ADVANCED, false);
        this.timings = this.add(new BooleanSetting("Measure Timings", "Measure how long each module takes (slightly slower).", false));
        this.add(new ButtonSetting("Timings", "Show the slowest modules.", "Open", () -> this.mc.displayGuiScreen(new Timings(this.mc.currentScreen))));
        this.add(new ButtonSetting("Game Log", "Show the end of logs/latest.log.", "Open", () -> this.mc.displayGuiScreen(new GuiTextPage(this.mc.currentScreen, "logs/latest.log", this::logTail))));
        this.add(new ButtonSetting("Dump Modules", "Write every module and setting to sythix/dev/modules.txt.", "Dump", this::dump));
        this.add(new ButtonSetting("Reload Config", "Read sythix/config.json again (after editing it by hand).", "Reload", () -> {
            SythixClient.get().getModuleManager().resetAll();
            SythixClient.get().getConfigManager().load();
            Notifications.post("Config reloaded", "sythix/config.json", Notifications.Kind.SUCCESS);
        }));
        this.add(new ButtonSetting("Copy Config", "Copy the current config JSON to the clipboard.", "Copy", () -> GuiScreen.setClipboardString(SythixClient.get().getConfigManager().toJsonString())));
    }

    public void onTick()
    {
        ModuleManager.timing = this.timings.isOn();
    }

    protected void onDisable()
    {
        ModuleManager.timing = false;
    }

    private String logTail()
    {
        try
        {
            List<String> lines = FileUtils.readLines(new File(new File(this.mc.mcDataDir, "logs"), "latest.log"), StandardCharsets.UTF_8);
            return String.join("\n", lines.subList(Math.max(0, lines.size() - 200), lines.size()));
        }
        catch (Exception e)
        {
            return "Could not read the log: " + e.getMessage();
        }
    }

    private void dump()
    {
        StringBuilder b = new StringBuilder();

        for (Module m : SythixClient.get().getModuleManager().getModules())
        {
            b.append(m.getName()).append(" [").append(m.getCategory().getDisplayName()).append("] ").append(m.isEnabled() ? "ON" : "off").append(m.isCore() ? " core" : "").append(" - ").append(m.getDescription()).append('\n');

            for (Setting<?> s : m.getSettings())
            {
                b.append("    ").append(s.getName()).append(" = ").append(s.toJson()).append('\n');
            }
        }

        try
        {
            File file = new File(new File(new File(this.mc.mcDataDir, "sythix"), "dev"), "modules.txt");
            FileUtils.writeStringToFile(file, b.toString(), StandardCharsets.UTF_8);
            Notifications.post("Modules dumped", "sythix/dev/modules.txt", Notifications.Kind.SUCCESS);
        }
        catch (Exception e)
        {
            Notifications.post("Dump failed", String.valueOf(e.getMessage()), Notifications.Kind.ERROR);
        }
    }

    static class Timings extends GuiListScreen
    {
        Timings(GuiScreen parent)
        {
            super(parent, "Module Timings");
        }

        protected String header()
        {
            return ModuleManager.timing ? "Average time per event call, slowest first." : "Turn on Measure Timings to collect data.";
        }

        protected List<Row> rows()
        {
            List<Row> rows = new ArrayList<Row>();
            List<Map.Entry<Module, long[]>> all = new ArrayList<Map.Entry<Module, long[]>>(SythixClient.get().getModuleManager().getTimings().entrySet());
            all.sort((a, c) -> Long.compare(c.getValue()[0] / Math.max(1L, c.getValue()[1]), a.getValue()[0] / Math.max(1L, a.getValue()[1])));

            for (Map.Entry<Module, long[]> e : all)
            {
                long avg = e.getValue()[0] / Math.max(1L, e.getValue()[1]);
                rows.add(new Row(e.getKey().getName(), e.getKey().getName(), e.getValue()[1] + " calls, " + e.getValue()[0] / 1000000L + " ms total", String.format("%.1f us", avg / 1000.0D), avg > 100000L ? 0xFFFF4040 : SythixTheme.BLUE));
            }

            return rows;
        }

        protected String[] buttons()
        {
            return new String[] {"Clear", "Done"};
        }

        protected void press(int index)
        {
            if (index == 0)
            {
                SythixClient.get().getModuleManager().getTimings().clear();
            }
            else
            {
                this.mc.displayGuiScreen(this.parent);
            }
        }
    }
}
