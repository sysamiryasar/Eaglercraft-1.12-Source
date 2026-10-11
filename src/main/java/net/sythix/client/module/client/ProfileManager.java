package net.sythix.client.module.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.config.ConfigProfiles;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.GuiTextPrompt;
import net.sythix.client.ui.SythixTheme;

/**
 * #52 Save your whole setup as named profiles (PvP, building, recording...) and switch, copy,
 * rename, share (clipboard) and delete them.
 */
public class ProfileManager extends Module
{
    public ProfileManager()
    {
        super("Profile Manager", "Named setting profiles: create, load, rename, duplicate, import and export.", Category.CLIENT, true);
        this.setCore();
        this.add(new ButtonSetting("Profiles", "Open the profile list.", "Open", () -> this.mc.displayGuiScreen(new Screen(this.mc.currentScreen))));
        this.add(new InfoSetting("Saved Profiles", "How many profiles you have.", () -> String.valueOf(ConfigProfiles.list().size())));
        this.add(new InfoSetting("Sharing", "How to share a profile.", () -> "Export copies JSON - contains no account data"));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new Screen(parent);
    }

    static class Screen extends GuiListScreen
    {
        Screen(GuiScreen parent)
        {
            super(parent, "Setting Profiles");
        }

        protected String header()
        {
            return "A profile stores every module setting and HUD position. Double-click to load.";
        }

        protected List<Row> rows()
        {
            List<Row> rows = new ArrayList<Row>();

            for (String name : ConfigProfiles.list())
            {
                rows.add(new Row(name, name, "sythix/profiles/" + name + ".json", "", SythixTheme.RED));
            }

            return rows;
        }

        protected String emptyText()
        {
            return "No profiles yet - press \"Save Current\".";
        }

        protected String[] buttons()
        {
            return new String[] {"Save Current", "Load", "Overwrite", "Rename", "Duplicate", "Delete", "Export", "Import", "Done"};
        }

        protected boolean enabled(int index)
        {
            return index == 0 || index >= 7 || this.selected != null;
        }

        protected void open(Object key)
        {
            this.press(1);
        }

        protected void press(int index)
        {
            String name = (String)this.selected;

            try
            {
                switch (index)
                {
                    case 0:
                        this.mc.displayGuiScreen(new GuiTextPrompt(this, "Save current settings", "Profile name (letters, numbers, spaces)", "", 32, text -> this.saveAs(text)));
                        break;
                    case 1:
                        ConfigProfiles.load(name);
                        this.status = "Loaded " + name + ".";
                        break;
                    case 2:
                        ConfigProfiles.save(name);
                        this.status = "Saved current settings into " + name + ".";
                        break;
                    case 3:
                        this.mc.displayGuiScreen(new GuiTextPrompt(this, "Rename profile", "New name for " + name, name, 32, text -> {
                            this.status = !ConfigProfiles.clean(text).isEmpty() && !ConfigProfiles.exists(text) && ConfigProfiles.rename(name, text) ? "Renamed." : "Could not rename (empty or name taken).";
                            this.selected = null;
                        }));
                        break;
                    case 4:
                        String copy = this.freeName(name + " copy");
                        ConfigProfiles.importText(copy, ConfigProfiles.read(name));
                        this.status = "Duplicated as " + copy + ".";
                        break;
                    case 5:
                        ConfigProfiles.delete(name);
                        this.selected = null;
                        this.status = "Deleted " + name + ".";
                        break;
                    case 6:
                        GuiScreen.setClipboardString(name == null ? net.sythix.client.SythixClient.get().getConfigManager().toJsonString() : ConfigProfiles.read(name));
                        this.status = (name == null ? "Current settings" : name) + " copied to the clipboard.";
                        break;
                    case 7:
                        String json = GuiScreen.getClipboardString();
                        String imported = this.freeName("Imported");
                        ConfigProfiles.importText(imported, json);
                        this.status = "Imported clipboard as " + imported + ".";
                        break;
                    default:
                        this.mc.displayGuiScreen(this.parent);
                }
            }
            catch (Exception e)
            {
                this.status = "Failed: " + (index == 7 ? "the clipboard does not contain a Sythix profile." : e.getMessage());
            }
        }

        private void saveAs(String text)
        {
            String clean = ConfigProfiles.clean(text);

            if (clean.isEmpty())
            {
                this.status = "Enter a name with letters or numbers.";
                return;
            }

            try
            {
                ConfigProfiles.save(clean);
                this.selected = clean;
                this.status = "Saved " + clean + ".";
            }
            catch (Exception e)
            {
                this.status = "Could not save: " + e.getMessage();
            }
        }

        private String freeName(String base)
        {
            String name = ConfigProfiles.clean(base);

            for (int i = 2; ConfigProfiles.exists(name); ++i)
            {
                name = ConfigProfiles.clean(base + " " + i);
            }

            return name;
        }
    }
}
