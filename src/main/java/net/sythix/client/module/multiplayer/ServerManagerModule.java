package net.sythix.client.module.multiplayer;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;

/**
 * #40 Favourites, folders, icons, notes and quick connect for servers.
 */
public class ServerManagerModule extends Module
{
    private final BooleanSetting listBadges;

    public ServerManagerModule()
    {
        super("Server Manager", "Favourite servers, folders, notes, icons and quick connect.", Category.MULTIPLAYER, true);
        this.add(new ButtonSetting("Server Manager", "Open the server manager.", "Open", () -> this.mc.displayGuiScreen(new GuiServerManager(this.mc.currentScreen))));
        this.listBadges = this.add(new BooleanSetting("Badges In Server List", "Show the star and folder in the normal Multiplayer list.", true));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiServerManager(parent);
    }

    /** Name shown in the vanilla multiplayer list (with star and folder). */
    public String decorateName(ServerData data)
    {
        if (!this.listBadges.isOn())
        {
            return data.serverName;
        }

        String folder = ServerMeta.folder(data.serverIP);
        return (ServerMeta.isFavorite(data.serverIP) ? "§e★ §r" : "") + data.serverName + (folder.isEmpty() ? "" : " §8[" + folder + "]");
    }
}
