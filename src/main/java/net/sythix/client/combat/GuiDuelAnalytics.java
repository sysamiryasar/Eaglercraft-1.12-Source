package net.sythix.client.combat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.sythix.client.module.recording.DuelAnalytics;
import net.sythix.client.ui.SythixTheme;
import org.lwjgl.opengl.GL11;

/**
 * Duel Analytics: totals, trends over your recent duels, records against opponents and personal
 * bests, filtered by fight type (crystal, cart or melee).
 */
public class GuiDuelAnalytics extends GuiScreen
{
    private static final String[] FILTERS = {"All", "Crystal", "Cart", "Melee"};
    private static final int GREEN = 0xFF4CD964;

    private final GuiScreen parent;
    private final DuelAnalytics module;
    private int filter;
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GuiDuelAnalytics(GuiScreen parent, DuelAnalytics module)
    {
        this.parent = parent;
        this.module = module;
    }

    public void initGui()
    {
        this.pw = Math.min(this.width - 12, 560);
        this.ph = Math.min(this.height - 12, 320);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
    }

    private List<Fight> filtered()
    {
        List<Fight> list = new ArrayList<Fight>();

        for (Fight f : this.module.duels())
        {
            if (this.filter == 0 || FILTERS[this.filter].equals(f.type()))
            {
                list.add(f);
            }
        }

        return list;
    }

    private int[] filterBox(int i)
    {
        int w = 46;
        int x = this.px + this.pw - 8 - (FILTERS.length - i) * (w + 3) + 3;
        return new int[] {x, this.py + 5, w, 13};
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        SythixTheme.drawBackground(this.width, this.height, -1, -1, this.mc.world == null ? 0.45F : 0.35F);
        SythixTheme.drawGlass((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), 8.0F, 1.0F);
        this.fontRenderer.drawString("Duel Analytics", (float)(this.px + 8), (float)(this.py + 8), 0xFFFFFFFF, true);

        for (int i = 0; i < FILTERS.length; ++i)
        {
            int[] b = this.filterBox(i);
            boolean sel = i == this.filter;
            boolean hover = mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];
            SythixTheme.drawRounded((float)b[0], (float)b[1], (float)(b[0] + b[2]), (float)(b[1] + b[3]), 6.0F, sel ? SythixTheme.BLUE : (hover ? 0x40FFFFFF : 0x20FFFFFF));
            this.fontRenderer.drawString(FILTERS[i], (float)(b[0] + b[2] / 2 - this.fontRenderer.getStringWidth(FILTERS[i]) / 2), (float)(b[1] + 3), sel ? 0xFF000000 : SythixTheme.TEXT, false);
        }

        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + 21), (float)(this.px + this.pw), 1.0F);
        List<Fight> list = this.filtered();
        int top = this.py + 27;

        if (list.isEmpty())
        {
            String none = "No duels yet" + (this.filter == 0 ? "" : " of this type") + ". Fight someone and they will show up here.";
            this.fontRenderer.drawString(none, (float)(this.px + this.pw / 2 - this.fontRenderer.getStringWidth(none) / 2), (float)(top + 60), SythixTheme.TEXT_MUTED, false);
            String hint = this.module.includeTraining() ? "" : "Training Lab and mob fights are hidden (Include Training setting).";
            this.fontRenderer.drawString(hint, (float)(this.px + this.pw / 2 - this.fontRenderer.getStringWidth(hint) / 2), (float)(top + 74), SythixTheme.TEXT_MUTED, false);
            this.drawDone(mouseX, mouseY);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        // cards
        String[][] cards = this.cards(list);
        int cw = (this.pw - 16 - (cards.length - 1) * 4) / cards.length;

        for (int i = 0; i < cards.length; ++i)
        {
            int cx = this.px + 8 + i * (cw + 4);
            SythixTheme.drawRounded((float)cx, (float)top, (float)(cx + cw), (float)(top + 30), 4.0F, 0x30FFFFFF);
            this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(cards[i][0], cw - 8), (float)(cx + 4), (float)(top + 4), SythixTheme.TEXT_MUTED, false);
            this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(cards[i][1], cw - 8), (float)(cx + 4), (float)(top + 16), 0xFFFFFFFF, true);
        }

        // charts
        int chartTop = top + 36;
        int chartH = Math.max(60, (this.ph - 36 - 36 - 26) / 2);
        int half = (this.pw - 20) / 2;
        this.chart(this.px + 8, chartTop, half, chartH, "Win rate (last 10 duels)", winRate(list), 100.0F, "%", GREEN);
        String[] metric = this.metric();
        this.chart(this.px + 12 + half, chartTop, half, chartH, metric[0], this.metricValues(list), 0.0F, metric[1], SythixTheme.BLUE);

        // opponents and bests
        int listTop = chartTop + chartH + 8;
        this.fontRenderer.drawString("Opponents", (float)(this.px + 8), (float)listTop, SythixTheme.BLUE, true);
        int y = listTop + 11;
        int bottom = this.py + this.ph - 26;

        for (Map.Entry<String, int[]> e : opponents(list))
        {
            if (y + 9 > bottom)
            {
                break;
            }

            int[] r = e.getValue();
            this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(e.getKey(), half - 90), (float)(this.px + 8), (float)y, SythixTheme.TEXT, false);
            String rec = r[0] + "W " + r[1] + "L  (" + r[2] + ")";
            this.fontRenderer.drawString(rec, (float)(this.px + 8 + half - this.fontRenderer.getStringWidth(rec)), (float)y, r[0] >= r[1] ? GREEN : 0xFFFF8080, false);
            y += 10;
        }

        this.fontRenderer.drawString("Personal bests", (float)(this.px + 12 + half), (float)listTop, SythixTheme.BLUE, true);
        y = listTop + 11;

        for (String[] best : bests(list))
        {
            if (y + 9 > bottom)
            {
                break;
            }

            this.fontRenderer.drawString(best[0], (float)(this.px + 12 + half), (float)y, SythixTheme.TEXT_MUTED, false);
            this.fontRenderer.drawString(best[1], (float)(this.px + 12 + 2 * half - this.fontRenderer.getStringWidth(best[1])), (float)y, 0xFFFFFFFF, false);
            y += 10;
        }

        this.drawDone(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int[] doneBox()
    {
        return new int[] {this.px + this.pw - 118, this.py + this.ph - 21, 110, 16};
    }

    private void drawDone(int mouseX, int mouseY)
    {
        int[] b = this.doneBox();
        boolean hover = mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];
        SythixTheme.drawButton((float)b[0], (float)b[1], (float)b[2], (float)b[3], true, hover ? 1.0F : 0.0F);
        this.fontRenderer.drawString("Done", (float)(b[0] + b[2] / 2 - this.fontRenderer.getStringWidth("Done") / 2), (float)(b[1] + 4), SythixTheme.getButtonTextColor(true, hover ? 1.0F : 0.0F), false);
        String trend = this.trend();

        if (!trend.isEmpty())
        {
            this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(trend, this.pw - 140), (float)(this.px + 8), (float)(b[1] + 4), SythixTheme.LIGHT_BLUE, false);
        }
    }

    private String[][] cards(List<Fight> list)
    {
        int wins = 0;
        int losses = 0;
        long time = 0L;
        float dealt = 0.0F;
        float taken = 0.0F;
        int pops = 0;

        for (Fight f : list)
        {
            wins += f.isWin() ? 1 : 0;
            losses += f.isLoss() ? 1 : 0;
            time += f.duration;
            dealt += f.damageDealt;
            taken += f.damageTaken;
            pops += f.popsOpponent;
        }

        int n = list.size();
        int decided = wins + losses;
        int s = DuelAnalytics.streak(list);
        List<String[]> cards = new ArrayList<String[]>();
        cards.add(new String[] {"Duels", n + " (" + wins + "-" + losses + ")"});
        cards.add(new String[] {"Win rate", decided == 0 ? "--" : wins * 100 / decided + "%"});
        cards.add(new String[] {"Streak", s == 0 ? "--" : (s > 0 ? s + " W" : -s + " L")});
        cards.add(new String[] {"Avg time", Fight.clock(time / (long)n)});
        cards.add(new String[] {"Avg dmg", String.format("%.0f / %.0f", dealt / (float)n, taken / (float)n)});
        cards.add(new String[] {"Pops/duel", String.format("%.1f", (float)pops / (float)n)});
        return cards.toArray(new String[0][]);
    }

    /** {chart title, unit} for the right chart, depending on the filter. */
    private String[] metric()
    {
        switch (FILTERS[this.filter])
        {
            case "Crystal":
                return new String[] {"Crystals per second", "/s"};
            case "Cart":
                return new String[] {"Rail to cart time", " ms"};
            default:
                return new String[] {"Damage dealt minus taken", ""};
        }
    }

    private List<Float> metricValues(List<Fight> list)
    {
        List<Float> values = new ArrayList<Float>();

        for (Fight f : list)
        {
            switch (FILTERS[this.filter])
            {
                case "Crystal":
                    values.add(Float.valueOf(f.crystalRate()));
                    break;
                case "Cart":
                    if (f.avgRailToCart() >= 0L)
                    {
                        values.add(Float.valueOf((float)f.avgRailToCart()));
                    }

                    break;
                default:
                    values.add(Float.valueOf(f.damageDealt - f.damageTaken));
            }
        }

        return values;
    }

    private static List<Float> winRate(List<Fight> list)
    {
        List<Float> values = new ArrayList<Float>();

        for (int i = 0; i < list.size(); ++i)
        {
            int w = 0;
            int d = 0;

            for (int j = Math.max(0, i - 9); j <= i; ++j)
            {
                w += list.get(j).isWin() ? 1 : 0;
                d += list.get(j).isWin() || list.get(j).isLoss() ? 1 : 0;
            }

            values.add(Float.valueOf(d == 0 ? 0.0F : (float)w * 100.0F / (float)d));
        }

        return values;
    }

    /** "Your crystal speed is up 12% compared to earlier duels." from the right chart's values. */
    private String trend()
    {
        List<Float> v = this.metricValues(this.filtered());

        if (v.size() < 6)
        {
            return v.isEmpty() ? "" : "Play a few more duels to see your trend.";
        }

        int half = Math.min(10, v.size() / 2);
        float older = 0.0F;
        float recent = 0.0F;

        for (int i = v.size() - 2 * half; i < v.size() - half; ++i)
        {
            older += v.get(i).floatValue();
        }

        for (int i = v.size() - half; i < v.size(); ++i)
        {
            recent += v.get(i).floatValue();
        }

        older /= (float)half;
        recent /= (float)half;
        String name = this.metric()[0].toLowerCase();
        boolean lowerIsBetter = "Cart".equals(FILTERS[this.filter]);

        if (Math.abs(older) < 0.01F)
        {
            return String.format("Recent %s: %.1f%s.", name, recent, this.metric()[1]);
        }

        float change = (recent - older) / Math.abs(older) * 100.0F;
        boolean better = lowerIsBetter ? change < 0.0F : change > 0.0F;
        return String.format("Last %d duels vs the %d before: %s %s by %.0f%%.", half, half, name, better ? "improved" : "dropped", Math.abs(change));
    }

    private static List<Map.Entry<String, int[]>> opponents(List<Fight> list)
    {
        Map<String, int[]> map = new LinkedHashMap<String, int[]>();

        for (Fight f : list)
        {
            int[] r = map.computeIfAbsent(f.opponent, k -> new int[3]);
            r[0] += f.isWin() ? 1 : 0;
            r[1] += f.isLoss() ? 1 : 0;
            ++r[2];
        }

        List<Map.Entry<String, int[]>> entries = new ArrayList<Map.Entry<String, int[]>>(map.entrySet());
        entries.sort((a, b) -> b.getValue()[2] - a.getValue()[2]);
        return entries;
    }

    private static List<String[]> bests(List<Fight> list)
    {
        long fastestWin = -1L;
        float bestRate = 0.0F;
        long bestDelay = -1L;
        long bestRail = -1L;
        int mostPops = 0;
        float mostDealt = 0.0F;
        int bestStreak = 0;
        int run = 0;

        for (Fight f : list)
        {
            if (f.isWin() && (fastestWin < 0L || f.duration < fastestWin))
            {
                fastestWin = f.duration;
            }

            bestRate = Math.max(bestRate, f.bestCrystalRate);

            if (f.breakDelayCount >= 3 && (bestDelay < 0L || f.avgBreakDelay() < bestDelay))
            {
                bestDelay = f.avgBreakDelay();
            }

            if (f.railToCartCount >= 2 && (bestRail < 0L || f.avgRailToCart() < bestRail))
            {
                bestRail = f.avgRailToCart();
            }

            mostPops = Math.max(mostPops, f.popsOpponent);
            mostDealt = Math.max(mostDealt, f.damageDealt);
            run = f.isWin() ? run + 1 : (f.isLoss() ? 0 : run);
            bestStreak = Math.max(bestStreak, run);
        }

        List<String[]> b = new ArrayList<String[]>();
        b.add(new String[] {"Fastest win", fastestWin < 0L ? "--" : Fight.clock(fastestWin)});
        b.add(new String[] {"Longest win streak", String.valueOf(bestStreak)});
        b.add(new String[] {"Best crystal speed", bestRate <= 0.0F ? "--" : String.format("%.1f/s", bestRate)});
        b.add(new String[] {"Best break delay (avg)", bestDelay < 0L ? "--" : bestDelay + " ms"});
        b.add(new String[] {"Best rail to cart (avg)", bestRail < 0L ? "--" : bestRail + " ms"});
        b.add(new String[] {"Most pops forced", String.valueOf(mostPops)});
        b.add(new String[] {"Most damage in a duel", String.format("%.0f", mostDealt)});
        return b;
    }

    private void chart(int x, int y, int w, int h, String title, List<Float> values, float fixedMax, String unit, int color)
    {
        SythixTheme.drawRounded((float)x, (float)y, (float)(x + w), (float)(y + h), 4.0F, 0x30000000);
        this.fontRenderer.drawString(title, (float)(x + 4), (float)(y + 3), SythixTheme.TEXT_MUTED, false);

        // last 40 duels
        List<Float> v = values.subList(Math.max(0, values.size() - 40), values.size());

        if (v.size() < 2)
        {
            this.fontRenderer.drawString("Needs 2+ duels", (float)(x + w / 2 - 34), (float)(y + h / 2), SythixTheme.TEXT_MUTED, false);
            return;
        }

        float min = 0.0F;
        float max = fixedMax;

        for (Float f : v)
        {
            min = Math.min(min, f.floatValue());
            max = Math.max(max, f.floatValue());
        }

        if (max - min < 0.001F)
        {
            max = min + 1.0F;
        }

        String last = String.format(Math.abs(v.get(v.size() - 1).floatValue()) >= 10.0F ? "%.0f%s" : "%.1f%s", v.get(v.size() - 1), unit);
        this.fontRenderer.drawString(last, (float)(x + w - 4 - this.fontRenderer.getStringWidth(last)), (float)(y + 3), color, false);
        int gx = x + 4;
        int gy = y + 14;
        int gw = w - 8;
        int gh = h - 18;

        if (min < 0.0F)
        {
            float zero = (float)gy + (float)gh * (max / (max - min));
            SythixTheme.drawRect((float)gx, zero, (float)(gx + gw), zero + 0.5F, 0x40FFFFFF);
        }

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2.0F);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < v.size(); ++i)
        {
            double px = (double)gx + (double)gw * (double)i / (double)(v.size() - 1);
            double py = (double)gy + (double)gh * (double)((max - v.get(i).floatValue()) / (max - min));
            buf.pos(px, py, 0.0D).color(color >> 16 & 255, color >> 8 & 255, color & 255, 255).endVertex();
        }

        tess.draw();
        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (int i = 0; i < FILTERS.length; ++i)
        {
            int[] b = this.filterBox(i);

            if (mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3])
            {
                this.filter = i;
                return;
            }
        }

        int[] d = this.doneBox();

        if (mouseX >= d[0] && mouseX < d[0] + d[2] && mouseY >= d[1] && mouseY < d[1] + d[3])
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1)
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
