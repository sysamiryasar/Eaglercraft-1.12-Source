package net.sythix.client.module.hud;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #29 Player list layout, scale, colours and visibility.
 */
public class TabListModule extends Module
{
    private final NumberSetting scale;
    private final ModeSetting pingStyle;
    private final BooleanSetting showHeads;
    private final BooleanSetting showHeader;
    private final BooleanSetting showFooter;
    private final ColorSetting background;
    private final ColorSetting rowColor;
    private final BooleanSetting highlightSelf;
    private final ColorSetting selfColor;

    public TabListModule()
    {
        super("Tab List", "Scale, ping numbers, colours and what the player list shows.", Category.INTERFACE, false);
        this.scale = this.add(new NumberSetting("Scale", "Size of the player list.", 1.0D, 0.5D, 1.5D, 0.05D, "x"));
        this.pingStyle = this.add(new ModeSetting("Ping", "Show ping as bars, numbers, both, or not at all.", "Numbers", "Bars", "Numbers", "Both", "Hidden"));
        this.showHeads = this.add(new BooleanSetting("Player Heads", "Show player face icons.", true));
        this.showHeader = this.add(new BooleanSetting("Header", "Show the server's header text.", true));
        this.showFooter = this.add(new BooleanSetting("Footer", "Show the server's footer text.", true));
        this.background = this.add(new ColorSetting("Background", "Colour behind the whole list.", 0x80000000, true));
        this.rowColor = this.add(new ColorSetting("Row Color", "Colour behind each name.", 0x20FFFFFF, true));
        this.highlightSelf = this.add(new BooleanSetting("Highlight Me", "Give your own row a different colour.", true));
        this.selfColor = this.add(new ColorSetting("My Row Color", "Colour of your own row.", 0x40D81820, true)).visibleWhen(this.highlightSelf::isOn);
    }

    public float getScale()
    {
        return this.scale.getFloat();
    }

    public int background(int vanilla)
    {
        return this.background.getColor();
    }

    public int rowColor(String playerName)
    {
        if (this.highlightSelf.isOn() && this.mc.player != null && this.mc.player.getName().equals(playerName))
        {
            return this.selfColor.getColor();
        }

        return this.rowColor.getColor();
    }

    public boolean showHeads(boolean vanilla)
    {
        return vanilla && this.showHeads.isOn();
    }

    public boolean showHeader()
    {
        return this.showHeader.isOn();
    }

    public boolean showFooter()
    {
        return this.showFooter.isOn();
    }

    public boolean showBars()
    {
        return this.pingStyle.is("Bars") || this.pingStyle.is("Both");
    }

    public boolean showNumbers()
    {
        return this.pingStyle.is("Numbers") || this.pingStyle.is("Both");
    }

    /** Draws the ping number at the right edge of a row. */
    public void drawPingNumber(int width, int x, int y, NetworkPlayerInfo info, boolean barsToo)
    {
        int ping = info.getResponseTime();
        String text = ping < 0 ? "?" : String.valueOf(ping);
        int color = ping < 0 ? 0xFFAAAAAA : (ping < 80 ? 0xFF55FF55 : (ping < 150 ? 0xFFFFFF55 : (ping < 300 ? 0xFFFFAA00 : 0xFFFF5555)));
        int right = x + width - (barsToo ? 12 : 1);
        this.mc.fontRenderer.drawStringWithShadow(text, (float)(right - this.mc.fontRenderer.getStringWidth(text)), (float)y, color);
    }
}
