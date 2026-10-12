package net.sythix.client.module.advanced;

import java.text.SimpleDateFormat;
import net.minecraft.client.gui.GuiLanguage;
import net.sythix.client.core.Hooks;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.setting.ModeSetting;

/**
 * #65 Language, date, time and number formats, and a mirrored (right-to-left) HUD layout.
 */
public class Localization extends Module
{
    private final ModeSetting dateFormat;
    private final ModeSetting timeFormat;
    private final ModeSetting decimal;
    private final BooleanSetting mirrorHud;

    public Localization()
    {
        super("Localization", "Language, date/time and number formats, and right-to-left HUD layout.", Category.ADVANCED, true);
        this.setCore();
        this.add(new InfoSetting("Game Language", "Language of Minecraft's own text.", () -> this.mc.getLanguageManager().getCurrentLanguage().toString()));
        this.add(new ButtonSetting("Change Language", "Open Minecraft's language list.", "Open", () -> this.mc.displayGuiScreen(new GuiLanguage(this.mc.currentScreen, this.mc.gameSettings, this.mc.getLanguageManager()))));
        this.dateFormat = this.add(new ModeSetting("Date Format", "How dates are shown in Sythix lists.", "YYYY-MM-DD", "YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY", "DD.MM.YYYY"));
        this.timeFormat = this.add(new ModeSetting("Time Format", "24-hour or 12-hour clock in Sythix lists.", "24h", "24h", "12h"));
        this.decimal = this.add(new ModeSetting("Decimal Separator", "Character before decimals (coordinates and other numbers).", "Point (1.5)", "Point (1.5)", "Comma (1,5)"));
        this.mirrorHud = this.add(new BooleanSetting("Right-To-Left HUD", "Mirror HUD positions left/right (for right-to-left languages).", false));
        this.add(new InfoSetting("Sythix Text", "Language of Sythix menus.", () -> "English (translations welcome)"));
    }

    private static Localization active()
    {
        return Hooks.on(Localization.class);
    }

    /** Date and time pattern for lists, e.g. "yyyy-MM-dd HH:mm". */
    public static SimpleDateFormat dateTime(boolean seconds)
    {
        Localization l = active();
        String date = "yyyy-MM-dd";
        String time = seconds ? "HH:mm:ss" : "HH:mm";

        if (l != null)
        {
            date = l.dateFormat.is("DD/MM/YYYY") ? "dd/MM/yyyy" : (l.dateFormat.is("MM/DD/YYYY") ? "MM/dd/yyyy" : (l.dateFormat.is("DD.MM.YYYY") ? "dd.MM.yyyy" : date));
            time = l.timeFormat.is("12h") ? (seconds ? "h:mm:ss a" : "h:mm a") : time;
        }

        return new SimpleDateFormat(date + " " + time);
    }

    /** Time-only pattern. */
    public static SimpleDateFormat time()
    {
        Localization l = active();
        return new SimpleDateFormat(l != null && l.timeFormat.is("12h") ? "h:mm a" : "HH:mm");
    }

    /** A number with a fixed count of decimals, using the chosen separator. */
    public static String number(double value, int decimals)
    {
        String s = String.format(java.util.Locale.ROOT, "%." + Math.max(0, decimals) + "f", value);
        Localization l = active();
        return l != null && l.decimal.is("Comma (1,5)") ? s.replace('.', ',') : s;
    }

    public static boolean mirrorHud()
    {
        Localization l = active();
        return l != null && l.mirrorHud.isOn();
    }
}
