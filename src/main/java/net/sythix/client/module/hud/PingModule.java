package net.sythix.client.module.hud;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.sythix.client.setting.BooleanSetting;

public class PingModule extends TextHudModule
{
    private final BooleanSetting showUnit;
    private final BooleanSetting dynamicColor;

    public PingModule()
    {
        super("Ping", "Shows your latency to the server, as reported by the server.", true, 0.005D, 0.14D);
        this.showUnit = this.add(new BooleanSetting("Show \"ms\"", "Add \"ms\" after the number.", true));
        this.dynamicColor = this.add(new BooleanSetting("Ping Colors", "Colour the number green, yellow or red depending on latency.", true));
    }

    protected String getLabel()
    {
        return "Ping";
    }

    private int getPing()
    {
        if (this.mc.player == null || this.mc.getConnection() == null)
        {
            return 0;
        }

        NetworkPlayerInfo info = this.mc.getConnection().getPlayerInfo(this.mc.player.getUniqueID());
        return info == null ? 0 : info.getResponseTime();
    }

    protected String getValue()
    {
        return this.getPing() + (this.showUnit.isOn() ? " ms" : "");
    }

    protected int getValueColor()
    {
        if (!this.dynamicColor.isOn())
        {
            return super.getValueColor();
        }

        int ping = this.getPing();
        return ping < 80 ? 0xFF55FF55 : (ping < 150 ? 0xFFFFFF55 : (ping < 250 ? 0xFFFFAA00 : 0xFFFF5555));
    }
}
