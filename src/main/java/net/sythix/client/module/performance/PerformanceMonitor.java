package net.sythix.client.module.performance;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.StyledHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #6 FPS, frame-time graph, memory and other diagnostics.
 */
public class PerformanceMonitor extends StyledHudModule
{
    private static final int HISTORY = 120;
    private final float[] frameTimes = new float[HISTORY];
    private int frameIndex;
    private long lastFrame;

    private final BooleanSetting showFps;
    private final BooleanSetting showMinMax;
    private final BooleanSetting showGraph;
    private final NumberSetting graphHeight;
    private final ColorSetting graphColor;
    private final BooleanSetting showMemory;
    private final BooleanSetting showChunks;
    private final BooleanSetting showEntities;

    public PerformanceMonitor()
    {
        super("Performance Monitor", "FPS, frame-time graph, memory and render statistics.", Category.PERFORMANCE, true, 0.005D, 0.01D);
        this.showFps = this.add(new BooleanSetting("FPS", "Show frames per second.", true));
        this.showMinMax = this.add(new BooleanSetting("Frame Times", "Show average and worst frame time over the last 2 seconds.", false));
        this.showGraph = this.add(new BooleanSetting("Frame Graph", "Show a graph of recent frame times.", false));
        this.graphHeight = this.add(new NumberSetting("Graph Height", "Height of the graph.", 20.0D, 10.0D, 60.0D, 2.0D, "px")).visibleWhen(this.showGraph::isOn);
        this.graphColor = this.add(new ColorSetting("Graph Color", "Colour of the graph bars.", 0xFF6CC6FF, true)).visibleWhen(this.showGraph::isOn);
        this.showMemory = this.add(new BooleanSetting("Memory", "Show memory usage.", false));
        this.showChunks = this.add(new BooleanSetting("Chunk Updates", "Show chunk updates per second.", false));
        this.showEntities = this.add(new BooleanSetting("Entities", "Show how many entities are drawn.", false));
    }

    /** Called once per rendered frame. */
    public void onFrame()
    {
        long now = System.nanoTime();

        if (this.lastFrame != 0L)
        {
            this.frameTimes[this.frameIndex] = (float)(now - this.lastFrame) / 1000000.0F;
            this.frameIndex = (this.frameIndex + 1) % HISTORY;
        }

        this.lastFrame = now;
    }

    private List<String[]> lines()
    {
        List<String[]> lines = new ArrayList<String[]>();

        if (this.showFps.isOn())
        {
            lines.add(new String[] {"FPS", String.valueOf(Minecraft.getDebugFPS())});
        }

        if (this.showMinMax.isOn())
        {
            float total = 0.0F;
            float worst = 0.0F;

            for (float f : this.frameTimes)
            {
                total += f;
                worst = Math.max(worst, f);
            }

            lines.add(new String[] {"Frame", String.format("%.1f ms (max %.1f)", total / (float)HISTORY, worst)});
        }

        if (this.showMemory.isOn())
        {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            lines.add(new String[] {"Mem", used + "/" + rt.maxMemory() / 1048576L + " MB"});
        }

        if (this.showChunks.isOn())
        {
            lines.add(new String[] {"Chunks", String.valueOf(RenderChunk.renderChunksUpdated)});
        }

        if (this.showEntities.isOn())
        {
            lines.add(new String[] {"Entities", this.mc.world == null ? "0" : String.valueOf(this.mc.world.loadedEntityList.size())});
        }

        return lines;
    }

    public int getWidth()
    {
        int w = this.showGraph.isOn() ? 80 : 20;

        for (String[] l : this.lines())
        {
            w = Math.max(w, this.mc.fontRenderer.getStringWidth(l[0] + " " + l[1]));
        }

        return w + this.getPad() * 2;
    }

    public int getHeight()
    {
        int h = Math.max(0, this.lines().size() * 10 - 2);

        if (this.showGraph.isOn())
        {
            h += (h > 0 ? 3 : 0) + this.graphHeight.getInt();
        }

        return Math.max(8, h) + this.getPad() * 2;
    }

    protected void renderContent(boolean preview)
    {
        int width = this.getWidth();
        this.drawBox(width, this.getHeight());
        int y = this.getPad();

        for (String[] l : this.lines())
        {
            this.drawText(l[0] + " ", (float)this.getPad(), (float)y, this.labelColor.getColor());
            this.drawText(l[1], (float)(this.getPad() + this.mc.fontRenderer.getStringWidth(l[0] + " ")), (float)y, this.valueColor.getColor());
            y += 10;
        }

        if (this.showGraph.isOn())
        {
            if (y > this.getPad())
            {
                y += 1;
            }

            int gh = this.graphHeight.getInt();
            int gw = width - this.getPad() * 2;
            SythixTheme.drawRect((float)this.getPad(), (float)y, (float)(this.getPad() + gw), (float)(y + gh), 0x40000000);
            float barWidth = (float)gw / (float)HISTORY;

            for (int i = 0; i < HISTORY; ++i)
            {
                float ms = this.frameTimes[(this.frameIndex + i) % HISTORY];
                float h = Math.min((float)gh, ms / 50.0F * (float)gh);
                int color = ms > 33.0F ? 0xFFFF5555 : (ms > 17.0F ? 0xFFFFFF55 : this.graphColor.getColor((float)i / (float)HISTORY));
                float x = (float)this.getPad() + (float)i * barWidth;
                SythixTheme.drawRect(x, (float)(y + gh) - h, x + Math.max(0.5F, barWidth), (float)(y + gh), color);
            }

            // 60 fps reference line
            float ref = (float)(y + gh) - 16.7F / 50.0F * (float)gh;
            SythixTheme.drawRect((float)this.getPad(), ref, (float)(this.getPad() + gw), ref + 0.5F, 0x80FFFFFF);
        }
    }
}
