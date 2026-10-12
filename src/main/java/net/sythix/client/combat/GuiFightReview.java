package net.sythix.client.combat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.sythix.client.replay.ReplayFormat;
import net.sythix.client.replay.ReplayPlayer;
import net.sythix.client.replay.ReplayRecorder;
import net.sythix.client.ui.SythixTheme;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * The review of one fight: result, key numbers, a health graph, tips and a timeline, plus a
 * button that opens the replay a few seconds before the fight started.
 */
public class GuiFightReview extends GuiScreen
{
    private static final int GREEN = 0xFF4CD964;
    private static final int GOLD = 0xFFFFC94D;
    /** Your line in the health graph: a fixed blue, so it never matches the opponent's red whatever the theme. */
    private static final int YOU = 0xFF4DA6FF;

    private final GuiScreen parent;
    private final Fight fight;
    private final List<String> tips;
    private float scroll;
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GuiFightReview(GuiScreen parent, Fight fight)
    {
        this.parent = parent;
        this.fight = fight;
        this.tips = tips(fight);
    }

    public static boolean replayAvailable(Fight f)
    {
        return f != null && f.replayFile != null && new File(ReplayFormat.folder(), f.replayFile).isFile();
    }

    /** Opens the replay three seconds before the fight started. */
    public static void watch(Fight f)
    {
        if (!replayAvailable(f))
        {
            return;
        }

        File file = new File(ReplayFormat.folder(), f.replayFile);
        File recording = ReplayRecorder.current();

        if (recording != null && recording.getName().equals(file.getName()))
        {
            // this session is still being recorded: finish the file first
            ReplayRecorder.stop();
        }

        ReplayPlayer.play(file, Math.max(1L, f.replayOffset - 3000L));
    }

    public void initGui()
    {
        this.pw = Math.min(this.width - 12, 540);
        this.ph = Math.min(this.height - 12, 320);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
    }

    private int resultColor()
    {
        return this.fight.isWin() ? GREEN : (this.fight.isLoss() ? SythixTheme.RED : SythixTheme.TEXT_MUTED);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        SythixTheme.drawBackground(this.width, this.height, -1, -1, this.mc.world == null ? 0.45F : 0.35F);
        SythixTheme.drawGlass((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), 8.0F, 1.0F);
        Fight f = this.fight;

        // title
        String result = f.result.toUpperCase();
        this.fontRenderer.drawString(result, (float)(this.px + 8), (float)(this.py + 8), this.resultColor(), true);
        int rw = this.fontRenderer.getStringWidth(result);
        this.fontRenderer.drawString("vs " + f.opponent + "  -  " + f.type() + " fight" + (f.training ? " (training)" : ""), (float)(this.px + 14 + rw), (float)(this.py + 8), 0xFFFFFFFF, true);
        String when = net.sythix.client.module.advanced.Localization.dateTime(false).format(new Date(f.startEpoch)) + "   " + Fight.clock(f.duration);
        this.fontRenderer.drawString(when, (float)(this.px + this.pw - 8 - this.fontRenderer.getStringWidth(when)), (float)(this.py + 8), SythixTheme.TEXT_MUTED, false);
        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + 21), (float)(this.px + this.pw), 1.0F);

        int top = this.py + 27;
        int bottom = this.py + this.ph - 26;
        int leftW = this.pw * 50 / 100;
        int lx = this.px + 8;
        int rx = this.px + 8 + leftW + 8;
        int rwid = this.px + this.pw - 8 - rx;

        // stat cards
        String[][] stats = this.stats();
        int cw = (leftW - 4) / 2;
        int ch = 22;

        for (int i = 0; i < stats.length; ++i)
        {
            int cx = lx + (i % 2) * (cw + 4);
            int cy = top + (i / 2) * (ch + 3);
            SythixTheme.drawRounded((float)cx, (float)cy, (float)(cx + cw), (float)(cy + ch), 4.0F, 0x30FFFFFF);
            this.fontRenderer.drawString(stats[i][0], (float)(cx + 5), (float)(cy + 3), SythixTheme.TEXT_MUTED, false);
            this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(stats[i][1], cw - 10), (float)(cx + 5), (float)(cy + 12), 0xFFFFFFFF, false);
        }

        // tips
        int ty = top + ((stats.length + 1) / 2) * (ch + 3) + 4;
        this.fontRenderer.drawString("Tips", (float)lx, (float)ty, SythixTheme.BLUE, true);
        ty += 11;

        for (String tip : this.tips)
        {
            for (String line : this.fontRenderer.listFormattedStringToWidth("- " + tip, leftW))
            {
                if (ty + 9 > bottom)
                {
                    break;
                }

                this.fontRenderer.drawString(line, (float)lx, (float)ty, SythixTheme.TEXT, false);
                ty += 10;
            }
        }

        // health graph
        int gh = Math.min(84, (bottom - top) / 2);
        this.drawGraph(rx, top, rwid, gh);

        // timeline
        int listTop = top + gh + 16;
        this.fontRenderer.drawString("Timeline", (float)rx, (float)(listTop - 11), SythixTheme.BLUE, true);
        List<Fight.Event> events = f.events;
        int rowH = 10;
        int max = Math.max(0, events.size() * rowH - (bottom - listTop));
        this.scroll = Math.max(0.0F, Math.min((float)max, this.scroll));
        SythixTheme.beginClip(rx, listTop, rx + rwid, bottom);
        int y = listTop - Math.round(this.scroll);

        for (Fight.Event e : events)
        {
            if (y + rowH > listTop && y < bottom)
            {
                this.fontRenderer.drawString(Fight.clock(e.t), (float)rx, (float)y, SythixTheme.TEXT_MUTED, false);
                this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(e.text, rwid - 40), (float)(rx + 38), (float)y, color(e.kind), false);
            }

            y += rowH;
        }

        SythixTheme.endClip();

        // buttons
        boolean replay = replayAvailable(f);
        this.button(0, "Watch Replay", replay, mouseX, mouseY);
        this.button(1, "Back", true, mouseX, mouseY);

        if (!replay)
        {
            String hint = f.replayFile == null ? "Turn on Record Replays in Replay System to watch fights again." : "The replay file was deleted.";
            this.fontRenderer.drawString(hint, (float)(this.px + 8), (float)(this.py + this.ph - 31), SythixTheme.TEXT_MUTED, false);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int[] buttonBox(int i)
    {
        int bw = 110;
        int bx = this.px + this.pw - 8 - (2 - i) * (bw + 4) + 4;
        return new int[] {bx, this.py + this.ph - 21, bw, 16};
    }

    private void button(int i, String label, boolean enabled, int mouseX, int mouseY)
    {
        int[] b = this.buttonBox(i);
        boolean hover = enabled && mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];
        SythixTheme.drawButton((float)b[0], (float)b[1], (float)b[2], (float)b[3], enabled, hover ? 1.0F : 0.0F);
        this.fontRenderer.drawString(label, (float)(b[0] + b[2] / 2 - this.fontRenderer.getStringWidth(label) / 2), (float)(b[1] + 4), SythixTheme.getButtonTextColor(enabled, hover ? 1.0F : 0.0F), false);
    }

    private static int color(String kind)
    {
        switch (kind)
        {
            case "hit":
            case "dealt":
            case "break":
            case "crystal":
                return 0xFFB8F0C0;
            case "hurt":
            case "enemy":
                return 0xFFFFB0B0;
            case "pop":
            case "pop-them":
                return GOLD;
            case "cart":
                return 0xFFFFD0A0;
            default:
                return SythixTheme.TEXT;
        }
    }

    private String[][] stats()
    {
        Fight f = this.fight;
        List<String[]> s = new ArrayList<String[]>();
        s.add(new String[] {"Damage dealt", String.format("%.1f", f.damageDealt)});
        s.add(new String[] {"Damage taken", String.format("%.1f", f.damageTaken)});
        s.add(new String[] {"Hits", f.hitsDealt + " dealt, " + f.hitsTaken + " taken"});
        s.add(new String[] {"Totem pops", "you " + f.popsSelf + ", them " + f.popsOpponent});

        if (f.crystalsPlaced + f.crystalsBroken + f.enemyCrystals > 0 || !"Cart".equals(f.type()))
        {
            s.add(new String[] {"Crystals", f.crystalsPlaced + " put, " + f.crystalsBroken + " hit"});
            s.add(new String[] {"Crystal speed", String.format("%.1f/s, best %.1f", f.crystalRate(), f.bestCrystalRate)});
            long d = f.avgBreakDelay();
            s.add(new String[] {"Break delay", d < 0L ? "--" : d + " ms, best " + f.bestBreakDelay});
        }

        if (f.cartsPlaced + f.railsPlaced > 0)
        {
            s.add(new String[] {"TNT carts", f.cartsPlaced + " put, " + f.cartsDetonated + " off"});
            long r = f.avgRailToCart();
            s.add(new String[] {"Rail > cart", r < 0L ? "--" : r + " ms"});
        }

        return s.toArray(new String[0][]);
    }

    /** Short, factual tips from the fight's numbers. */
    static List<String> tips(Fight f)
    {
        List<String> tips = new ArrayList<String>();

        if (f.crystalsPlaced > 0)
        {
            long d = f.avgBreakDelay();

            if (d > 200L)
            {
                tips.add("Your crystals waited " + d + " ms on average before you hit them. Breaking faster (under 150 ms) keeps the pressure on.");
            }
            else if (d >= 0L)
            {
                tips.add("Quick breaking: " + d + " ms on average after placing.");
            }

            if (f.longestCrystalGap > 1500L)
            {
                tips.add(String.format("Longest pause between your crystals: %.1f s. Keep obsidian and crystals in easy hotbar slots to keep the flow going.", (float)f.longestCrystalGap / 1000.0F));
            }

            if (f.crystalsPlaced > f.crystalsBroken + 3)
            {
                tips.add((f.crystalsPlaced - f.crystalsBroken) + " of your crystals were never hit by you.");
            }
        }

        if (f.cartsPlaced > 0)
        {
            long r = f.avgRailToCart();

            if (r > 400L)
            {
                tips.add("Rail to cart took " + r + " ms on average; under 300 ms makes carts much harder to dodge.");
            }

            if (f.cartsDetonated < f.cartsPlaced)
            {
                tips.add((f.cartsPlaced - f.cartsDetonated) + " of your " + f.cartsPlaced + " carts did not go off while you were nearby.");
            }
        }

        float taken = f.takenCrystal + f.takenCart + f.takenMelee + f.takenOther;

        if (taken > 0.0F)
        {
            String source = "melee";
            float most = f.takenMelee;

            if (f.takenCrystal > most)
            {
                source = "crystals";
                most = f.takenCrystal;
            }

            if (f.takenCart > most)
            {
                source = "TNT minecarts";
                most = f.takenCart;
            }

            tips.add(String.format("Most of your damage came from %s (%d%%).", source, Math.round(most * 100.0F / taken)));
        }

        if (f.popsOpponent > 0 || f.popsSelf > 0)
        {
            tips.add("You forced " + f.popsOpponent + " totem pop" + (f.popsOpponent == 1 ? "" : "s") + " and used " + f.popsSelf + " yourself.");
        }

        if (f.opponentIsPlayer && f.damageDealt == 0.0F && f.hitsDealt + f.crystalsBroken > 0)
        {
            tips.add("The server did not show the opponent's health, so damage dealt could not be measured.");
        }

        if (tips.isEmpty())
        {
            tips.add("Short fight - not much to analyse.");
        }

        return tips;
    }

    private void drawGraph(int x, int y, int w, int h)
    {
        SythixTheme.drawRounded((float)x, (float)y, (float)(x + w), (float)(y + h), 4.0F, 0x30000000);
        this.fontRenderer.drawString("Health", (float)(x + 4), (float)(y + 3), SythixTheme.TEXT_MUTED, false);
        String legend = "you";
        this.fontRenderer.drawString(legend, (float)(x + w - 60), (float)(y + 3), YOU, false);
        this.fontRenderer.drawString(this.fight.opponent.length() > 8 ? "them" : this.fight.opponent, (float)(x + w - 36), (float)(y + 3), 0xFFFF6060, false);
        List<float[]> hp = this.fight.health;

        if (hp.size() < 2)
        {
            this.fontRenderer.drawString("Not enough data", (float)(x + w / 2 - 38), (float)(y + h / 2), SythixTheme.TEXT_MUTED, false);
            return;
        }

        float maxT = Math.max(1.0F, hp.get(hp.size() - 1)[0]);
        float maxH = 20.0F;

        for (float[] p : hp)
        {
            maxH = Math.max(maxH, Math.max(p[1], p[2]));
        }

        int gx = x + 4;
        int gy = y + 14;
        int gw = w - 8;
        int gh = h - 18;
        this.line(hp, 2, gx, gy, gw, gh, maxT, maxH, 0xFFFF6060);
        this.line(hp, 1, gx, gy, gw, gh, maxT, maxH, YOU);

        // totem pops as gold ticks along the bottom
        for (Fight.Event e : this.fight.events)
        {
            if (e.kind.startsWith("pop"))
            {
                float ex = (float)gx + (float)gw * Math.min(1.0F, (float)e.t / 1000.0F / maxT);
                SythixTheme.drawRect(ex - 0.5F, (float)(gy + gh - 4), ex + 0.5F, (float)(gy + gh), e.kind.equals("pop") ? GOLD : 0xFFFFE9A8);
            }
        }
    }

    private void line(List<float[]> points, int index, int x, int y, int w, int h, float maxT, float maxH, int color)
    {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2.0F);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;

        for (float[] p : points)
        {
            double px = (double)x + (double)w * (double)(p[0] / maxT);
            double py = (double)y + (double)h * (1.0D - (double)(p[index] / maxH));
            buf.pos(px, py, 0.0D).color(r, g, b, 255).endVertex();
        }

        tess.draw();
        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (int i = 0; i < 2; ++i)
        {
            int[] b = this.buttonBox(i);

            if (mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3])
            {
                if (i == 0 && replayAvailable(this.fight))
                {
                    watch(this.fight);
                }
                else if (i == 1)
                {
                    this.mc.displayGuiScreen(this.parent);
                }
            }
        }
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0)
        {
            this.scroll -= (float)wheel * 0.25F;
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
