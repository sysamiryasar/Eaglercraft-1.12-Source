package net.sythix.client.module.advanced;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.core.ModuleErrors;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.ui.GuiListScreen;
import org.apache.commons.io.FileUtils;

/**
 * #58 Module errors: what failed, turning modules back on, and exporting reports. A module that
 * throws is switched off so the game keeps running.
 */
public class ErrorHandler extends Module
{
    private final BooleanSetting autoDisable;
    private final BooleanSetting notify;

    public ErrorHandler()
    {
        super("Error Handler", "Catches module errors, switches broken modules off and lists what happened.", Category.ADVANCED, true);
        this.setCore();
        this.add(new ButtonSetting("Error Log", "See every error from this session.", "Open", () -> this.mc.displayGuiScreen(new Screen(this.mc.currentScreen))));
        this.add(new InfoSetting("Errors", "Errors this session.", () -> ModuleErrors.getReports().size() + " (" + ModuleErrors.getDisabled().size() + " modules switched off)"));
        this.autoDisable = this.add(new BooleanSetting("Switch Off Broken Modules", "Recommended. When off, a failing module keeps running (and keeps failing).", true));
        this.notify = this.add(new BooleanSetting("Notify", "Show a notification when a module is switched off.", true));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new Screen(parent);
    }

    public void onTick()
    {
        ModuleErrors.autoDisable = this.autoDisable.isOn();
        ModuleErrors.notify = this.notify.isOn();
    }

    static class Screen extends GuiListScreen
    {
        Screen(GuiScreen parent)
        {
            super(parent, "Error Handler");
        }

        protected String header()
        {
            List<Module> off = ModuleErrors.getDisabled();
            StringBuilder b = new StringBuilder(off.isEmpty() ? "No modules were switched off." : "Switched off: ");

            for (int i = 0; i < off.size(); ++i)
            {
                b.append(i > 0 ? ", " : "").append(off.get(i).getName());
            }

            return b.toString();
        }

        protected int rowHeight()
        {
            return 12;
        }

        protected List<Row> rows()
        {
            List<Row> rows = new ArrayList<Row>();
            List<String> reports = ModuleErrors.getReports();

            for (int i = reports.size() - 1; i >= 0; --i)
            {
                rows.add(new Row(Integer.valueOf(i), reports.get(i), "", "", 0xFFFF4040));
            }

            return rows;
        }

        protected String emptyText()
        {
            return "No errors - everything is running fine.";
        }

        protected String[] buttons()
        {
            return new String[] {"Turn Modules Back On", "Copy All", "Export", "Clear", "Done"};
        }

        protected void press(int index)
        {
            String all = String.join("\n", ModuleErrors.getReports());

            switch (index)
            {
                case 0:
                    this.status = ModuleErrors.reenableAll() + " modules switched back on.";
                    break;
                case 1:
                    GuiScreen.setClipboardString(all);
                    this.status = "Copied.";
                    break;
                case 2:
                    try
                    {
                        File file = new File(new File(this.mc.mcDataDir, "sythix"), "errors.txt");
                        FileUtils.writeStringToFile(file, all + "\n\n(see logs/latest.log for full stack traces)\n", StandardCharsets.UTF_8);
                        this.status = "Saved to sythix/errors.txt";
                    }
                    catch (Exception e)
                    {
                        this.status = "Failed: " + e.getMessage();
                    }

                    break;
                case 3:
                    ModuleErrors.clear();
                    this.status = "Cleared.";
                    break;
                default:
                    this.mc.displayGuiScreen(this.parent);
            }
        }
    }
}
