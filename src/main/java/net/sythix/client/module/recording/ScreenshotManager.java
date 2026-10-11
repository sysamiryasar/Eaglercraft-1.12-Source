package net.sythix.client.module.recording;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.TextSetting;

/**
 * #46 Screenshot file names, capture notifications and a screenshot gallery.
 */
public class ScreenshotManager extends Module
{
    private final TextSetting pattern;
    private final BooleanSetting notify;
    private final BooleanSetting copyPath;

    public ScreenshotManager()
    {
        super("Screenshot Manager", "Custom file names, capture notifications and a screenshot gallery.", Category.RECORDING, true);
        this.add(new ButtonSetting("Gallery", "Browse, preview and delete your screenshots.", "Open", () -> this.mc.displayGuiScreen(new GuiScreenshots(this.mc.currentScreen))));
        this.pattern = this.add(new TextSetting("File Name", "Tokens: {date} {time} {server} {world} {name}", "{date}_{time}", 80));
        this.notify = this.add(new BooleanSetting("Notify On Capture", "Show a notification when a screenshot is saved.", true));
        this.copyPath = this.add(new BooleanSetting("Copy Path", "Copy the new file's path to the clipboard.", false));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiScreenshots(parent);
    }

    /** A new file name in the screenshot folder, following the pattern. */
    public File nextFile(File dir)
    {
        Date now = new Date();
        ServerData data = this.mc.getCurrentServerData();
        String server = this.mc.isSingleplayer() ? "singleplayer" : (data == null ? "menu" : data.serverIP);
        String world = this.mc.getIntegratedServer() != null ? this.mc.getIntegratedServer().getFolderName() : server;
        String base = this.pattern.get().trim().isEmpty() ? "{date}_{time}" : this.pattern.get();
        base = base.replace("{date}", new SimpleDateFormat("yyyy-MM-dd").format(now)).replace("{time}", new SimpleDateFormat("HH.mm.ss").format(now)).replace("{server}", server).replace("{world}", world).replace("{name}", this.mc.getSession().getUsername());
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_");
        File file = new File(dir, base + ".png");

        for (int i = 2; file.exists(); ++i)
        {
            file = new File(dir, base + "_" + i + ".png");
        }

        return file;
    }

    public void onSaved(File file)
    {
        if (this.copyPath.isOn())
        {
            GuiScreen.setClipboardString(file.getAbsolutePath());
        }

        if (this.notify.isOn())
        {
            Notifications.post("Screenshot saved", file.getName(), Notifications.Kind.SUCCESS);
        }
    }
}
