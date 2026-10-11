package net.sythix.client.module.performance;

import java.io.File;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.settings.GameSettings;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.sythix.client.setting.InfoSetting;
import org.apache.commons.io.FileUtils;

/**
 * #5 Resource packs, texture loading and cache controls.
 */
public class ResourceManagerModule extends Module
{
    public ResourceManagerModule()
    {
        super("Resource Manager", "Resource packs, texture loading and cache controls.", Category.PERFORMANCE, true);
        this.add(new InfoSetting("Active Packs", "Resource packs currently in use.", () -> String.valueOf(this.mc.getResourcePackRepository().getRepositoryEntries().size())));
        this.add(new ButtonSetting("Resource Packs", "Open the resource pack screen.", "Open", () -> this.mc.displayGuiScreen(new GuiScreenResourcePacks(this.mc.currentScreen))));
        this.add(new ButtonSetting("Reload Resources", "Reload all textures, models and sounds (like F3+T).", "Reload", () -> {
            this.mc.refreshResources();
            Notifications.post("Resources reloaded", "Textures, models and sounds were reloaded.", Notifications.Kind.SUCCESS);
        }));
        this.add(new GameOptionSetting("Mipmap levels: smoother distant textures, uses more memory.", GameSettings.Options.MIPMAP_LEVELS));
        this.add(new GameOptionSetting("Anisotropic filtering for textures at an angle.", GameSettings.Options.AF_LEVEL));
        this.add(new GameOptionSetting("Custom sky textures from resource packs.", GameSettings.Options.CUSTOM_SKY));
        this.add(new GameOptionSetting("Custom item textures from resource packs.", GameSettings.Options.CUSTOM_ITEMS));
        this.add(new InfoSetting("Server Pack Cache", "Space used by downloaded server resource packs.", () -> FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(this.serverPackDir()))));
        this.add(new ButtonSetting("Clear Server Pack Cache", "Delete downloaded server resource packs (they download again when needed).", "Clear", this::clearServerPacks));
    }

    private File serverPackDir()
    {
        File dir = new File(this.mc.mcDataDir, "server-resource-packs");

        if (!dir.isDirectory())
        {
            dir.mkdirs();
        }

        return dir;
    }

    private void clearServerPacks()
    {
        try
        {
            FileUtils.cleanDirectory(this.serverPackDir());
            Notifications.post("Cache cleared", "Server resource pack cache is empty.", Notifications.Kind.SUCCESS);
        }
        catch (Exception e)
        {
            Notifications.post("Could not clear cache", String.valueOf(e.getMessage()), Notifications.Kind.ERROR);
        }
    }
}
