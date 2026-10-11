package net.sythix.client.module.multiplayer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.GuiTextPrompt;
import net.sythix.client.ui.SythixTheme;

/**
 * Server Manager: favourites, folders, icons, notes and quick connect for your saved servers.
 */
public class GuiServerManager extends GuiListScreen
{
    private static String folderFilter = "All";
    private static final Map<String, ResourceLocation> ICONS = new HashMap<String, ResourceLocation>();
    private ServerList servers;

    public GuiServerManager(GuiScreen parent)
    {
        super(parent, "Server Manager");
    }

    public void initGui()
    {
        super.initGui();
        this.servers = new ServerList(this.mc);
        this.servers.loadServerList();
    }

    private List<String> folders()
    {
        Set<String> set = new LinkedHashSet<String>();
        set.add("All");
        set.add("Favourites");

        for (int i = 0; i < this.servers.countServers(); ++i)
        {
            String f = ServerMeta.folder(this.servers.getServerData(i).serverIP);

            if (!f.isEmpty())
            {
                set.add(f);
            }
        }

        return new ArrayList<String>(set);
    }

    private ResourceLocation icon(ServerData data)
    {
        String b64 = data.getBase64EncodedIconData();

        if (b64 == null)
        {
            return null;
        }

        if (!ICONS.containsKey(data.serverIP))
        {
            ResourceLocation location = null;

            try
            {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(b64)));

                if (image != null)
                {
                    location = this.mc.getTextureManager().getDynamicTextureLocation("sythix_server", new DynamicTexture(image));
                }
            }
            catch (Exception ignored)
            {
                location = null;
            }

            ICONS.put(data.serverIP, location);
        }

        return ICONS.get(data.serverIP);
    }

    protected String header()
    {
        return "Folder: " + folderFilter + "   |   Last server: " + (this.mc.gameSettings.lastServer.isEmpty() ? "none" : this.mc.gameSettings.lastServer);
    }

    protected List<Row> rows()
    {
        List<Row> favourites = new ArrayList<Row>();
        List<Row> others = new ArrayList<Row>();

        for (int i = 0; i < this.servers.countServers(); ++i)
        {
            ServerData data = this.servers.getServerData(i);
            String folder = ServerMeta.folder(data.serverIP);
            boolean fav = ServerMeta.isFavorite(data.serverIP);

            if (!folderFilter.equals("All") && !(folderFilter.equals("Favourites") && fav) && !folder.equals(folderFilter))
            {
                continue;
            }

            String note = ServerMeta.note(data.serverIP);
            Row row = new Row(data.serverIP, (fav ? "★ " : "") + data.serverName, data.serverIP + (note.isEmpty() ? "" : "  -  " + note), folder.isEmpty() ? null : "[" + folder + "]", fav ? 0xFFFFD24A : SythixTheme.BORDER);
            row.icon = this.icon(data);
            (fav ? favourites : others).add(row);
        }

        favourites.addAll(others);
        return favourites;
    }

    protected String emptyText()
    {
        return "No saved servers. Add some in Multiplayer, or use Quick Connect.";
    }

    protected String[] buttons()
    {
        return new String[] {"Join", "★ Favourite", "Folder", "Note", "Folder: " + folderFilter, "Quick Connect", "Rejoin Last", "Done"};
    }

    protected boolean enabled(int index)
    {
        return index >= 4 || this.selected != null;
    }

    protected void open(Object key)
    {
        this.join(this.find((String)key));
    }

    private ServerData find(String ip)
    {
        for (int i = 0; i < this.servers.countServers(); ++i)
        {
            if (this.servers.getServerData(i).serverIP.equals(ip))
            {
                return this.servers.getServerData(i);
            }
        }

        return null;
    }

    private void join(ServerData data)
    {
        if (data != null)
        {
            this.mc.displayGuiScreen(new GuiConnecting(this, this.mc, data));
        }
    }

    protected void press(int index)
    {
        String ip = (String)this.selected;

        switch (index)
        {
            case 0:
                this.join(this.find(ip));
                break;
            case 1:
                ServerMeta.setFavorite(ip, !ServerMeta.isFavorite(ip));
                break;
            case 2:
                this.mc.displayGuiScreen(new GuiTextPrompt(this, "Folder", "Group this server (for example \"PvP\"). Empty = no folder.", ServerMeta.folder(ip), 24, text -> ServerMeta.setFolder(ip, text)));
                break;
            case 3:
                this.mc.displayGuiScreen(new GuiTextPrompt(this, "Note", "A short note about this server.", ServerMeta.note(ip), 80, text -> ServerMeta.setNote(ip, text)));
                break;
            case 4:
                List<String> folders = this.folders();
                folderFilter = folders.get((folders.indexOf(folderFilter) + 1) % folders.size());
                break;
            case 5:
                this.mc.displayGuiScreen(new GuiTextPrompt(this, "Quick Connect", "Server address to join (not saved).", "", 128, text -> {
                    if (!text.trim().isEmpty())
                    {
                        this.join(new ServerData("Quick Connect", text.trim(), false));
                    }
                }));
                break;
            case 6:
                if (this.mc.gameSettings.lastServer.isEmpty())
                {
                    this.status = "You have not joined a server yet.";
                }
                else
                {
                    this.join(new ServerData("Last Server", this.mc.gameSettings.lastServer, false));
                }

                break;
            default:
                this.mc.displayGuiScreen(this.parent);
        }
    }
}
