package net.sythix.client.module.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.sythix.client.SythixClient;
import net.sythix.client.config.ConfigMigration;
import net.sythix.client.config.ConfigProfiles;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.SythixTheme;

/**
 * #53 Saving, automatic backups, restoring, resetting and config migration.
 */
public class SettingsManager extends Module
{
    private final BooleanSetting autoBackup;
    private final NumberSetting keep;
    private final NumberSetting autoSave;
    private boolean backedUp;
    private int ticks;
    private long resetArmed;

    public SettingsManager()
    {
        super("Settings Manager", "Save, back up, restore and reset all Sythix settings.", Category.CLIENT, true);
        this.setCore();
        this.add(new InfoSetting("Config File", "Where your settings live.", () -> "sythix/config.json (format v" + ConfigMigration.CURRENT + ")"));
        this.add(new ButtonSetting("Save Now", "Write all settings to disk now.", "Save", () -> {
            SythixClient.get().save();
            Notifications.post("Settings saved", "sythix/config.json", Notifications.Kind.SUCCESS);
        }));
        this.add(new ButtonSetting("Backups", "Restore or delete backups.", "Open", () -> this.mc.displayGuiScreen(new Screen(this.mc.currentScreen, this))));
        this.add(new ButtonSetting("Back Up Now", "Make a backup of the current settings.", "Back Up", () -> this.backupNow(true)));
        this.autoBackup = this.add(new BooleanSetting("Backup On Start", "Make a backup each time the game starts.", true));
        this.keep = this.add(new NumberSetting("Keep Backups", "Older backups are deleted automatically.", 10.0D, 1.0D, 50.0D, 1.0D));
        this.autoSave = this.add(new NumberSetting("Auto Save", "Also save every few minutes (0 = only when menus close / on exit).", 5.0D, 0.0D, 30.0D, 1.0D, " min"));
        this.add(new ButtonSetting("Open Folder", "Open the sythix folder in your file manager.", "Open", () -> OpenGlHelper.openFile(new File(this.mc.mcDataDir, "sythix"))));
        this.add(new ButtonSetting("Reset Everything", "Reset every module to its defaults (a backup is made first). Press twice.", "Reset", this::resetAll));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new Screen(parent, this);
    }

    public void onTick()
    {
        if (!this.backedUp)
        {
            this.backedUp = true;

            if (this.autoBackup.isOn())
            {
                this.backupNow(false);
            }
        }

        int every = this.autoSave.getInt() * 60 * 20;

        if (every > 0 && ++this.ticks >= every)
        {
            this.ticks = 0;
            SythixClient.get().save();
        }
    }

    void backupNow(boolean notify)
    {
        try
        {
            String name = ConfigProfiles.backup();
            ConfigProfiles.prune(this.keep.getInt());

            if (notify)
            {
                Notifications.post("Backup made", name, Notifications.Kind.SUCCESS);
            }
        }
        catch (Exception e)
        {
            Notifications.post("Backup failed", String.valueOf(e.getMessage()), Notifications.Kind.ERROR);
        }
    }

    private void resetAll()
    {
        long now = System.currentTimeMillis();

        if (now - this.resetArmed > 4000L)
        {
            this.resetArmed = now;
            Notifications.post("Reset everything?", "Press Reset again within 4 seconds to confirm.", Notifications.Kind.WARNING);
            return;
        }

        this.resetArmed = 0L;
        this.backupNow(false);
        SythixClient.get().getModuleManager().resetAll();
        SythixClient.get().save();
        Notifications.post("Settings reset", "A backup of your old settings was made first.", Notifications.Kind.SUCCESS);
    }

    static class Screen extends GuiListScreen
    {
        private final SettingsManager manager;

        Screen(GuiScreen parent, SettingsManager manager)
        {
            super(parent, "Settings Backups");
            this.manager = manager;
        }

        protected String header()
        {
            return "Backups are full copies of your settings. Restoring replaces the current settings.";
        }

        protected List<Row> rows()
        {
            List<Row> rows = new ArrayList<Row>();
            List<String> all = ConfigProfiles.backups();

            for (int i = all.size() - 1; i >= 0; --i)
            {
                String n = all.get(i);
                String pretty = n.length() == 15 ? n.substring(0, 4) + "-" + n.substring(4, 6) + "-" + n.substring(6, 8) + " " + n.substring(9, 11) + ":" + n.substring(11, 13) + ":" + n.substring(13, 15) : n;
                rows.add(new Row(n, pretty, "sythix/backups/" + n + ".json", "", SythixTheme.BLUE));
            }

            return rows;
        }

        protected String emptyText()
        {
            return "No backups yet.";
        }

        protected String[] buttons()
        {
            return new String[] {"Back Up Now", "Restore", "Delete", "Prune", "Done"};
        }

        protected boolean enabled(int index)
        {
            return index != 1 && index != 2 || this.selected != null;
        }

        protected void press(int index)
        {
            String name = (String)this.selected;

            try
            {
                switch (index)
                {
                    case 0:
                        this.manager.backupNow(false);
                        this.status = "Backup made.";
                        break;
                    case 1:
                        // Keep a safety copy of what is replaced
                        this.manager.backupNow(false);
                        ConfigProfiles.restoreBackup(name);
                        this.status = "Restored " + name + " (your previous settings were backed up).";
                        break;
                    case 2:
                        new File(new File(new File(this.mc.mcDataDir, "sythix"), "backups"), name + ".json").delete();
                        this.selected = null;
                        this.status = "Deleted.";
                        break;
                    case 3:
                        ConfigProfiles.prune(this.manager.keep.getInt());
                        this.status = "Kept the newest " + this.manager.keep.getInt() + ".";
                        break;
                    default:
                        this.mc.displayGuiScreen(this.parent);
                }
            }
            catch (Exception e)
            {
                this.status = "Failed: " + e.getMessage();
            }
        }
    }
}
