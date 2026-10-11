package net.sythix.client.module.hud;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.sythix.client.module.Category;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #28 Scoreboard scale, position, opacity and formatting. Drag it in the HUD editor.
 */
public class ScoreboardModule extends HudModule
{
    private final BooleanSetting hide;
    private final BooleanSetting hideNumbers;
    private final BooleanSetting textShadow;
    private final ColorSetting backgroundColor;
    private final ColorSetting titleBackground;
    private final BooleanSetting accentLine;
    private final NumberSetting lineSpacing;
    private ScoreObjective objective;

    public ScoreboardModule()
    {
        super("Scoreboard", "Move, resize and restyle the sidebar scoreboard.", Category.INTERFACE, true, 1.0D, 0.5D);
        this.hide = this.add(new BooleanSetting("Hide Scoreboard", "Do not show the sidebar at all.", false));
        this.hideNumbers = this.add(new BooleanSetting("Hide Numbers", "Hide the red score numbers.", true));
        this.textShadow = this.add(new BooleanSetting("Text Shadow", "Draw a shadow behind text.", false));
        this.backgroundColor = this.add(new ColorSetting("Background", "Colour of the scoreboard background.", 0x5016161B, true));
        this.titleBackground = this.add(new ColorSetting("Title Background", "Colour behind the title.", 0x7016161B, true));
        this.accentLine = this.add(new BooleanSetting("Accent Line", "Red-to-blue line under the title.", true));
        this.lineSpacing = this.add(new NumberSetting("Line Spacing", "Space between lines.", 9.0D, 8.0D, 14.0D, 1.0D, "px"));
    }

    /** Called by the overlay with the objective to show; returns true when Sythix draws it instead. */
    public boolean capture(ScoreObjective objective)
    {
        this.objective = objective;
        return true;
    }

    public boolean shouldRender()
    {
        return this.objective != null && !this.hide.isOn();
    }

    private List<String[]> rows(boolean preview)
    {
        List<String[]> rows = new ArrayList<String[]>();

        if (this.objective == null)
        {
            if (preview)
            {
                rows.add(new String[] {"§7Players: §f8", "3"});
                rows.add(new String[] {"§7Kills: §a12", "2"});
                rows.add(new String[] {"§cplay.example.net", "1"});
            }

            return rows;
        }

        Scoreboard scoreboard = this.objective.getScoreboard();
        Collection<Score> collection = scoreboard.getSortedScores(this.objective);
        List<Score> list = Lists.newArrayList(Iterables.filter(collection, s -> s.getPlayerName() != null && !s.getPlayerName().startsWith("#")));

        if (list.size() > 15)
        {
            list = Lists.newArrayList(Iterables.skip(list, list.size() - 15));
        }

        for (int i = list.size() - 1; i >= 0; --i)
        {
            Score score = list.get(i);
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            rows.add(new String[] {ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()), String.valueOf(score.getScorePoints())});
        }

        return rows;
    }

    private String title()
    {
        return this.objective == null ? "§c§lSYTHIX" : this.objective.getDisplayName();
    }

    public int getWidth()
    {
        FontRenderer font = this.mc.fontRenderer;
        int w = font.getStringWidth(this.title());

        for (String[] row : this.rows(true))
        {
            w = Math.max(w, font.getStringWidth(row[0]) + (this.hideNumbers.isOn() ? 0 : 6 + font.getStringWidth(row[1])));
        }

        return w + 6;
    }

    public int getHeight()
    {
        return 11 + this.rows(true).size() * this.lineSpacing.getInt() + 2;
    }

    protected void renderContent(boolean preview)
    {
        FontRenderer font = this.mc.fontRenderer;
        int width = this.getWidth();
        int height = this.getHeight();
        SythixTheme.drawRect(0.0F, 0.0F, (float)width, 10.0F, this.titleBackground.getColor());
        SythixTheme.drawRect(0.0F, 10.0F, (float)width, (float)height, this.backgroundColor.getColor());

        if (this.accentLine.isOn())
        {
            SythixTheme.drawAccentLine(0.0F, 10.0F, (float)width, 1.0F);
        }

        String title = this.title();
        font.drawString(title, (float)(width - font.getStringWidth(title)) / 2.0F, 1.0F, 0xFFFFFFFF, this.textShadow.isOn());
        int y = 12;

        for (String[] row : this.rows(preview))
        {
            font.drawString(row[0], 3.0F, (float)y, 0xFFFFFFFF, this.textShadow.isOn());

            if (!this.hideNumbers.isOn())
            {
                String score = "§c" + row[1];
                font.drawString(score, (float)(width - 3 - font.getStringWidth(score)), (float)y, 0xFFFFFFFF, this.textShadow.isOn());
            }

            y += this.lineSpacing.getInt();
        }

        if (!preview)
        {
            this.objective = null;
        }
    }
}
