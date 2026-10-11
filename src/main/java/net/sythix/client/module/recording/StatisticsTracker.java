package net.sythix.client.module.recording;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.TextHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.TextSetting;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.SythixTheme;
import org.apache.commons.io.FileUtils;

/**
 * #49 Session statistics: kills, deaths, K/D, streaks and match history. Kills are read from chat
 * messages (patterns are editable to match your server).
 */
public class StatisticsTracker extends TextHudModule
{
    private final TextSetting killPattern;
    private final TextSetting matchEndPattern;
    private final BooleanSetting showKd;
    private final BooleanSetting showStreak;
    private final BooleanSetting showTime;
    private long sessionStart;
    private int kills;
    private int deaths;
    private int streak;
    private int bestStreak;
    private int matches;
    private boolean wasDead;
    private final List<JsonObject> history = new ArrayList<JsonObject>();
    private boolean loaded;

    public StatisticsTracker()
    {
        super("Statistics Tracker", "Session kills, deaths, K/D, streaks and match history.", Category.RECORDING, false, 0.995D, 0.62D);
        this.killPattern = this.add(new TextSetting("Kill Pattern", "Regex for kill messages; {me} is your name.", "(?i)(?:was |)(?:slain|killed|shot|knocked|blown up|pummeled|thrown off)[^.]* by {me}\\b", 160));
        this.matchEndPattern = this.add(new TextSetting("Match End Pattern", "Regex for the end of a match.", "(?i)(won the game|game over|winner:|you (?:won|lost))", 120));
        this.showKd = this.add(new BooleanSetting("K/D", "Show the kill/death ratio.", true));
        this.showStreak = this.add(new BooleanSetting("Streak", "Show your kill streak.", true));
        this.showTime = this.add(new BooleanSetting("Session Time", "Show how long you have played.", false));
        this.add(new ButtonSetting("Match History", "Your saved match results.", "Open", () -> this.mc.displayGuiScreen(new GuiHistory(this.mc.currentScreen))));
        this.add(new ButtonSetting("Reset Session", "Start counting again from zero.", "Reset", this::resetSession));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiHistory(parent);
    }

    private void resetSession()
    {
        this.sessionStart = Minecraft.getSystemTime();
        this.kills = 0;
        this.deaths = 0;
        this.streak = 0;
        this.bestStreak = 0;
        this.matches = 0;
    }

    public void onTick()
    {
        if (this.sessionStart == 0L)
        {
            this.sessionStart = Minecraft.getSystemTime();
        }

        if (this.mc.player == null)
        {
            this.wasDead = false;
            return;
        }

        boolean dead = this.mc.player.getHealth() <= 0.0F;

        if (dead && !this.wasDead)
        {
            ++this.deaths;
            this.streak = 0;
        }

        this.wasDead = dead;
    }

    public void onChat(ITextComponent message)
    {
        String text = TextFormatting.getTextWithoutFormattingCodes(message.getUnformattedText());
        String me = Pattern.quote(this.mc.getSession().getUsername());

        try
        {
            if (Pattern.compile(this.killPattern.get().replace("{me}", me)).matcher(text).find() && !text.startsWith(this.mc.getSession().getUsername() + " "))
            {
                ++this.kills;
                ++this.streak;
                this.bestStreak = Math.max(this.bestStreak, this.streak);
            }

            if (Pattern.compile(this.matchEndPattern.get()).matcher(text).find())
            {
                ++this.matches;
                this.recordMatch(text);
            }
        }
        catch (RuntimeException ignored)
        {
            // invalid pattern
        }
    }

    private File file()
    {
        return new File(new File(this.mc.mcDataDir, "sythix"), "match-history.json");
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
                    this.history.add(e.getAsJsonObject());
                }
            }
        }
        catch (Exception ignored)
        {
            // start empty
        }
    }

    private void recordMatch(String result)
    {
        this.load();
        JsonObject o = new JsonObject();
        o.addProperty("time", Long.valueOf(System.currentTimeMillis()));
        o.addProperty("server", net.sythix.client.waypoint.WaypointStore.currentKey());
        o.addProperty("result", result.length() > 60 ? result.substring(0, 60) : result);
        o.addProperty("kills", Integer.valueOf(this.kills));
        o.addProperty("deaths", Integer.valueOf(this.deaths));
        this.history.add(o);

        while (this.history.size() > 50)
        {
            this.history.remove(0);
        }

        JsonArray array = new JsonArray();

        for (JsonObject h : this.history)
        {
            array.add(h);
        }

        try
        {
            FileUtils.writeStringToFile(this.file(), new GsonBuilder().setPrettyPrinting().create().toJson(array), StandardCharsets.UTF_8);
        }
        catch (Exception ignored)
        {
            // best effort
        }
    }

    protected List<String[]> getLines()
    {
        List<String[]> lines = new ArrayList<String[]>();
        lines.add(new String[] {"Kills", String.valueOf(this.kills)});
        lines.add(new String[] {"Deaths", String.valueOf(this.deaths)});

        if (this.showKd.isOn())
        {
            lines.add(new String[] {"K/D", String.format("%.2f", this.deaths == 0 ? (double)this.kills : (double)this.kills / (double)this.deaths)});
        }

        if (this.showStreak.isOn())
        {
            lines.add(new String[] {"Streak", this.streak + " (best " + this.bestStreak + ")"});
        }

        if (this.showTime.isOn())
        {
            long ms = this.sessionStart == 0L ? 0L : Minecraft.getSystemTime() - this.sessionStart;
            lines.add(new String[] {"Session", String.format("%d:%02d", ms / 3600000L, ms / 60000L % 60L)});
        }

        return lines;
    }

    /** Match history list. */
    public class GuiHistory extends GuiListScreen
    {
        public GuiHistory(GuiScreen parent)
        {
            super(parent, "Match History");
            StatisticsTracker.this.load();
        }

        protected String header()
        {
            return "This session: " + StatisticsTracker.this.kills + " kills, " + StatisticsTracker.this.deaths + " deaths, " + StatisticsTracker.this.matches + " matches.";
        }

        protected List<Row> rows()
        {
            List<Row> rows = new ArrayList<Row>();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            for (int i = StatisticsTracker.this.history.size() - 1; i >= 0; --i)
            {
                JsonObject o = StatisticsTracker.this.history.get(i);
                rows.add(new Row(Integer.valueOf(i), o.get("result").getAsString(), fmt.format(new Date(o.get("time").getAsLong())) + "  " + o.get("server").getAsString(), o.get("kills").getAsInt() + "K / " + o.get("deaths").getAsInt() + "D", SythixTheme.BLUE));
            }

            return rows;
        }

        protected String emptyText()
        {
            return "No matches recorded yet.";
        }

        protected String[] buttons()
        {
            return new String[] {"Done"};
        }

        protected void press(int index)
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }
}
