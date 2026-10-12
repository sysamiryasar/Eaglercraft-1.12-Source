package net.sythix.client.module.recording;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.combat.CombatTracker;
import net.sythix.client.combat.Fight;
import net.sythix.client.combat.GuiDuelAnalytics;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.TextHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.NumberSetting;
import org.apache.commons.io.FileUtils;

/**
 * Keeps every duel (crystal, cart and melee) and shows your progress over time: win rate, damage,
 * crystal speed and break delay trends, records against each opponent and personal bests.
 */
public class DuelAnalytics extends TextHudModule
{
    private final BooleanSetting showHud;
    private final BooleanSetting includeTraining;
    private final BooleanSetting showRecord;
    private final BooleanSetting showStreak;
    private final BooleanSetting showTrend;
    private final NumberSetting keep;
    private final List<Fight> duels = new ArrayList<Fight>();
    private boolean loaded;

    public DuelAnalytics()
    {
        super("Duel Analytics", "Your duel history and progress over time: win rate, damage, crystal speed and records.", Category.RECORDING, true, 0.995D, 0.86D);
        this.add(new ButtonSetting("Analytics", "Charts, records and personal bests.", "Open", () -> this.mc.displayGuiScreen(new GuiDuelAnalytics(this.mc.currentScreen, this))));
        this.showHud = this.add(new BooleanSetting("Show HUD", "Show your record on screen. Duels are saved either way while this module is on.", false));
        this.includeTraining = this.add(new BooleanSetting("Include Training", "Count fights in the CPvP Training Lab and against mobs.", false));
        this.showRecord = this.add(new BooleanSetting("Record", "Wins and losses (HUD).", true));
        this.showStreak = this.add(new BooleanSetting("Streak", "Your current win or loss streak (HUD).", true));
        this.showTrend = this.add(new BooleanSetting("Last 10", "Win rate of your last 10 duels (HUD).", false));
        this.keep = this.add(new NumberSetting("Keep Duels", "How many duels are kept in the history.", 500.0D, 50.0D, 5000.0D, 50.0D));
        this.add(new ButtonSetting("Clear History", "Delete all saved duels.", "Clear", () -> {
            this.load();
            this.duels.clear();
            this.save();
        }));
        CombatTracker.addListener(this::onFight);
    }

    public boolean shouldRender()
    {
        return this.showHud.isOn();
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiDuelAnalytics(parent, this);
    }

    public boolean includeTraining()
    {
        return this.includeTraining.isOn();
    }

    private void onFight(Fight fight)
    {
        if (!this.isEnabled())
        {
            return;
        }

        this.load();
        this.duels.add(fight);

        while (this.duels.size() > this.keep.getInt())
        {
            this.duels.remove(0);
        }

        this.save();
    }

    /** All saved duels, oldest first (filtered by the Include Training setting). */
    public List<Fight> duels()
    {
        this.load();
        List<Fight> list = new ArrayList<Fight>();

        for (Fight f : this.duels)
        {
            if (this.includeTraining.isOn() || (f.opponentIsPlayer && !f.training))
            {
                list.add(f);
            }
        }

        return list;
    }

    private File file()
    {
        return new File(new File(this.mc.mcDataDir, "sythix"), "duel-history.json");
    }

    private void load()
    {
        if (this.loaded)
        {
            return;
        }

        this.loaded = true;

        try
        {
            if (this.file().isFile())
            {
                for (JsonElement e : new JsonParser().parse(FileUtils.readFileToString(this.file(), StandardCharsets.UTF_8)).getAsJsonArray())
                {
                    this.duels.add(Fight.fromJson(e.getAsJsonObject()));
                }
            }
        }
        catch (Exception ignored)
        {
            // start empty
        }
    }

    private void save()
    {
        JsonArray array = new JsonArray();

        for (Fight f : this.duels)
        {
            array.add(f.toJson(false));
        }

        try
        {
            FileUtils.writeStringToFile(this.file(), array.toString(), StandardCharsets.UTF_8);
        }
        catch (Exception ignored)
        {
            // best effort
        }
    }

    /** Current streak: positive for wins in a row, negative for losses. */
    public static int streak(List<Fight> list)
    {
        int s = 0;

        for (int i = list.size() - 1; i >= 0; --i)
        {
            Fight f = list.get(i);

            if (f.isWin())
            {
                if (s < 0)
                {
                    break;
                }

                ++s;
            }
            else if (f.isLoss())
            {
                if (s > 0)
                {
                    break;
                }

                --s;
            }
        }

        return s;
    }

    protected List<String[]> getLines()
    {
        List<Fight> list = this.duels();
        List<String[]> lines = new ArrayList<String[]>();
        int wins = 0;
        int losses = 0;

        for (Fight f : list)
        {
            wins += f.isWin() ? 1 : 0;
            losses += f.isLoss() ? 1 : 0;
        }

        if (this.showRecord.isOn())
        {
            int decided = wins + losses;
            lines.add(new String[] {"Duels", wins + "W " + losses + "L" + (decided == 0 ? "" : " (" + wins * 100 / decided + "%)")});
        }

        if (this.showStreak.isOn())
        {
            int s = streak(list);
            lines.add(new String[] {"Streak", s == 0 ? "--" : (s > 0 ? "W" + s : "L" + -s)});
        }

        if (this.showTrend.isOn())
        {
            int w = 0;
            int d = 0;

            for (int i = Math.max(0, list.size() - 10); i < list.size(); ++i)
            {
                w += list.get(i).isWin() ? 1 : 0;
                d += list.get(i).isWin() || list.get(i).isLoss() ? 1 : 0;
            }

            lines.add(new String[] {"Last 10", d == 0 ? "--" : w * 100 / d + "% won"});
        }

        if (lines.isEmpty())
        {
            lines.add(new String[] {"Duels", String.valueOf(list.size())});
        }

        return lines;
    }
}
