package net.sythix.client.module.performance;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.module.advanced.Localization;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.SythixTheme;

/**
 * Benchmark history: best result highlighted, change against the previous run.
 */
public class GuiBenchmarks extends GuiListScreen
{
    public GuiBenchmarks(GuiScreen parent)
    {
        super(parent, "Benchmark Results");
    }

    protected String header()
    {
        return "Change one setting, run again and compare. 1% low = how bad the worst stutters are.";
    }

    protected List<Row> rows()
    {
        List<JsonObject> all = Benchmark.load();
        double best = 0.0D;

        for (JsonObject o : all)
        {
            best = Math.max(best, o.get("avgFps").getAsDouble());
        }

        List<Row> rows = new ArrayList<Row>();

        for (int i = all.size() - 1; i >= 0; --i)
        {
            JsonObject o = all.get(i);
            double fps = o.get("avgFps").getAsDouble();
            String change = "";

            if (i > 0)
            {
                double prev = all.get(i - 1).get("avgFps").getAsDouble();
                change = String.format("  %+.0f%%", (fps / Math.max(0.1D, prev) - 1.0D) * 100.0D);
            }

            String when = Localization.dateTime(false).format(new java.util.Date(o.get("time").getAsLong()));
            rows.add(new Row(Integer.valueOf(i), String.format("%.0f FPS  (1%% low %.0f)%s", fps, o.get("lowFps").getAsDouble(), change), when + "  " + o.get("settings").getAsString(), String.format("%.1f ms", o.get("avgMs").getAsDouble()), fps >= best ? 0xFF40D060 : SythixTheme.BLUE));
        }

        return rows;
    }

    protected String emptyText()
    {
        return "No results yet - join a world and press Run Benchmark.";
    }

    protected String[] buttons()
    {
        return new String[] {"Run Now", "Delete", "Clear All", "Done"};
    }

    protected boolean enabled(int index)
    {
        return index != 1 || this.selected != null;
    }

    protected void press(int index)
    {
        PerformanceOptimizer optimizer = net.sythix.client.SythixClient.get().getModuleManager().get(PerformanceOptimizer.class);

        switch (index)
        {
            case 0:
                for (net.sythix.client.setting.Setting<?> s : optimizer.getSettings())
                {
                    if (s.getName().equals("Run Benchmark"))
                    {
                        ((net.sythix.client.setting.ButtonSetting)s).press();
                    }
                }

                break;
            case 1:
                List<JsonObject> all = Benchmark.load();
                int i = ((Integer)this.selected).intValue();

                if (i < all.size())
                {
                    all.remove(i);
                    Benchmark.save(all);
                }

                this.selected = null;
                break;
            case 2:
                Benchmark.save(new ArrayList<JsonObject>());
                this.status = "History cleared.";
                break;
            default:
                this.mc.displayGuiScreen(this.parent);
        }
    }
}
