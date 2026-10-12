package net.sythix.client.module.multiplayer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.sythix.client.module.Category;
import net.sythix.client.module.advanced.PrivacyControls;
import net.sythix.client.module.hud.StyledHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #41 Ping, connection status, latency history and simple diagnostics.
 */
public class ConnectionMonitor extends StyledHudModule
{
    private static final int SAMPLES = 60;
    private final int[] history = new int[SAMPLES];
    private int index;
    private int count;
    private long lastSample;
    private long joined;
    private int packetsThisSecond;
    private int packetsPerSecond;
    private long packetSecond;

    private final BooleanSetting showPing;
    private final BooleanSetting showStats;
    private final BooleanSetting showServer;
    private final BooleanSetting showTime;
    private final BooleanSetting showPackets;
    private final BooleanSetting showGraph;
    private final BooleanSetting pingColors;
    private final ColorSetting graphColor;

    public ConnectionMonitor()
    {
        super("Connection Monitor", "Ping, latency history graph, connection time and packet rate.", Category.MULTIPLAYER, true, 0.005D, 0.14D);
        this.showPing = this.add(new BooleanSetting("Ping", "Your current ping as reported by the server.", true));
        this.showStats = this.add(new BooleanSetting("Average / Jitter", "Average ping and how much it varies.", false));
        this.showServer = this.add(new BooleanSetting("Server", "Address of the server.", false));
        this.showTime = this.add(new BooleanSetting("Connected Time", "How long you have been connected.", false));
        this.showPackets = this.add(new BooleanSetting("Packets/s", "Packets received per second.", false));
        this.showGraph = this.add(new BooleanSetting("Latency Graph", "Graph of the last minute of ping.", false));
        this.pingColors = this.add(new BooleanSetting("Ping Colors", "Green, yellow or red by latency.", true));
        this.graphColor = this.add(new ColorSetting("Graph Color", "Colour of the graph.", 0xFF6CC6FF, true)).visibleWhen(this.showGraph::isOn);
    }

    public void onWorldJoin()
    {
        this.joined = Minecraft.getSystemTime();
        this.count = 0;
    }

    /** Called for every packet received from the server (network thread). */
    public void onPacket()
    {
        ++this.packetsThisSecond;
    }

    private int ping()
    {
        if (this.mc.player == null || this.mc.getConnection() == null)
        {
            return 0;
        }

        NetworkPlayerInfo info = this.mc.getConnection().getPlayerInfo(this.mc.player.getUniqueID());
        return info == null ? 0 : info.getResponseTime();
    }

    public void onTick()
    {
        long now = Minecraft.getSystemTime();

        if (now - this.packetSecond >= 1000L)
        {
            this.packetsPerSecond = this.packetsThisSecond;
            this.packetsThisSecond = 0;
            this.packetSecond = now;
        }

        if (now - this.lastSample >= 1000L && this.mc.world != null)
        {
            this.lastSample = now;
            this.history[this.index] = this.ping();
            this.index = (this.index + 1) % SAMPLES;
            this.count = Math.min(SAMPLES, this.count + 1);
        }
    }

    private static int pingColor(int ping)
    {
        return ping < 80 ? 0xFF55FF55 : (ping < 150 ? 0xFFFFFF55 : (ping < 250 ? 0xFFFFAA00 : 0xFFFF5555));
    }

    private List<String[]> lines()
    {
        List<String[]> lines = new ArrayList<String[]>();

        if (this.showPing.isOn())
        {
            lines.add(new String[] {"Ping", this.mc.isSingleplayer() ? "0 ms (local)" : this.ping() + " ms"});
        }

        if (this.showStats.isOn())
        {
            int n = Math.max(1, this.count);
            double sum = 0.0D;

            for (int i = 0; i < this.count; ++i)
            {
                sum += (double)this.history[i];
            }

            double avg = sum / (double)n;
            double var = 0.0D;

            for (int i = 0; i < this.count; ++i)
            {
                var += ((double)this.history[i] - avg) * ((double)this.history[i] - avg);
            }

            lines.add(new String[] {"Avg", String.format("%.0f ms, jitter %.0f", avg, Math.sqrt(var / (double)n))});
        }

        if (this.showServer.isOn())
        {
            ServerData data = this.mc.getCurrentServerData();
            String address = this.mc.isSingleplayer() ? "Singleplayer" : (data == null ? "-" : data.serverIP);
            lines.add(new String[] {"Server", PrivacyControls.hideServer() ? "Hidden" : address});
        }

        if (this.showTime.isOn())
        {
            long ms = this.joined == 0L ? 0L : Minecraft.getSystemTime() - this.joined;
            lines.add(new String[] {"Online", String.format("%d:%02d:%02d", ms / 3600000L, ms / 60000L % 60L, ms / 1000L % 60L)});
        }

        if (this.showPackets.isOn())
        {
            lines.add(new String[] {"Packets", this.packetsPerSecond + "/s"});
        }

        return lines;
    }

    public int getWidth()
    {
        int w = this.showGraph.isOn() ? 90 : 30;

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
            h += (h > 0 ? 3 : 0) + 22;
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
            int color = l[0].equals("Ping") && this.pingColors.isOn() ? pingColor(this.ping()) : this.valueColor.getColor();
            this.drawText(l[1], (float)(this.getPad() + this.mc.fontRenderer.getStringWidth(l[0] + " ")), (float)y, color);
            y += 10;
        }

        if (this.showGraph.isOn())
        {
            if (y > this.getPad())
            {
                y += 1;
            }

            int gw = width - this.getPad() * 2;
            SythixTheme.drawRect((float)this.getPad(), (float)y, (float)(this.getPad() + gw), (float)(y + 22), 0x40000000);
            int max = 50;

            for (int i = 0; i < this.count; ++i)
            {
                max = Math.max(max, this.history[i]);
            }

            float bw = (float)gw / (float)SAMPLES;

            for (int i = 0; i < SAMPLES; ++i)
            {
                int sample = this.history[(this.index + i) % SAMPLES];
                float h = (float)sample / (float)max * 22.0F;
                float x = (float)this.getPad() + (float)i * bw;
                SythixTheme.drawRect(x, (float)(y + 22) - h, x + Math.max(0.5F, bw), (float)(y + 22), this.pingColors.isOn() ? pingColor(sample) : this.graphColor.getColor());
            }
        }
    }
}
