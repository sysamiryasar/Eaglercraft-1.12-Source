package net.sythix.client.module.advanced;

import net.minecraft.client.Minecraft;
import net.sythix.client.core.Hooks;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.TextSetting;

/**
 * #63 Streamer mode and control over what sensitive information is shown on screen.
 */
public class PrivacyControls extends Module
{
    private final BooleanSetting streamerMode;
    private final KeybindSetting streamerKey;
    private final BooleanSetting hideName;
    private final TextSetting alias;
    private final BooleanSetting hideServer;
    private final BooleanSetting hideCoords;

    public PrivacyControls()
    {
        super("Privacy Controls", "Streamer mode: hide your name, the server address and coordinates.", Category.ADVANCED, true);
        this.streamerMode = this.add(new BooleanSetting("Streamer Mode", "Turn on all the hiding options below.", false));
        this.streamerKey = this.add(new KeybindSetting("Streamer Mode Key", "Switch streamer mode quickly.", 0));
        this.hideName = this.add(new BooleanSetting("Hide My Name", "Replace your name everywhere on screen while streaming.", true));
        this.alias = this.add(new TextSetting("Alias", "Name shown instead of yours.", "Player", 16));
        this.hideServer = this.add(new BooleanSetting("Hide Server Address", "Hide the server address in Sythix displays.", true));
        this.hideCoords = this.add(new BooleanSetting("Hide Coordinates", "Hide the Coordinates HUD while streaming.", true));
    }

    public void onKey(int keyCode)
    {
        if (keyCode != 0 && keyCode == this.streamerKey.getKey())
        {
            this.streamerMode.toggle();
            net.sythix.client.core.Notifications.post("Streamer mode", this.streamerMode.isOn() ? "On - sensitive info hidden." : "Off.", net.sythix.client.core.Notifications.Kind.INFO);
        }
    }

    private static PrivacyControls active()
    {
        PrivacyControls p = Hooks.on(PrivacyControls.class);
        return p != null && p.streamerMode.isOn() ? p : null;
    }

    public static boolean hideServer()
    {
        PrivacyControls p = active();
        return p != null && p.hideServer.isOn();
    }

    public static boolean hideCoords()
    {
        PrivacyControls p = active();
        return p != null && p.hideCoords.isOn();
    }

    /** Replaces your name in any text drawn on screen while streamer mode hides it. */
    public static String filterText(String text)
    {
        PrivacyControls p = active();

        if (p == null || !p.hideName.isOn() || text == null)
        {
            return text;
        }

        String name = Minecraft.getMinecraft().getSession().getUsername();
        return name.length() < 3 || !text.contains(name) ? text : text.replace(name, p.alias.get().isEmpty() ? "Player" : p.alias.get());
    }
}
