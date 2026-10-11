package net.sythix.client.module.multiplayer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.GuiTextPrompt;
import net.sythix.client.ui.SythixTheme;
import org.apache.commons.io.FileUtils;

/**
 * #42 A local friends list with notes, online status (from the server's player list),
 * highlighting and join/leave notifications.
 */
public class FriendsManager extends Module
{
    /** Friend name (lower case) to {display name, note}. */
    private final Map<String, String[]> friends = new LinkedHashMap<String, String[]>();
    private final BooleanSetting highlightTab;
    private final BooleanSetting highlightNames;
    private final ColorSetting color;
    private final BooleanSetting notify;
    private Set<String> lastOnline = new HashSet<String>();
    private boolean loaded;

    public FriendsManager()
    {
        super("Friends Manager", "Local friends list with notes, online status and join alerts.", Category.MULTIPLAYER, true);
        this.add(new ButtonSetting("Friends", "Add, remove and write notes about friends.", "Open", () -> this.mc.displayGuiScreen(new GuiFriends(this.mc.currentScreen))));
        this.highlightTab = this.add(new BooleanSetting("Highlight In Tab", "Colour friends' rows in the player list.", true));
        this.highlightNames = this.add(new BooleanSetting("Highlight Nametags", "Colour friends' nametags above their heads.", true));
        this.color = this.add(new ColorSetting("Friend Color", "Colour used for friends.", 0xFF55FFFF, false));
        this.notify = this.add(new BooleanSetting("Join/Leave Alerts", "Notify you when a friend joins or leaves your server.", true));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiFriends(parent);
    }

    private File file()
    {
        return new File(new File(this.mc.mcDataDir, "sythix"), "friends.json");
    }

    private void load()
    {
        if (this.loaded)
        {
            return;
        }

        this.loaded = true;

        try
        {
            if (this.file().isFile())
            {
                for (JsonElement e : new JsonParser().parse(FileUtils.readFileToString(this.file(), StandardCharsets.UTF_8)).getAsJsonArray())
                {
                    JsonObject o = e.getAsJsonObject();
                    String name = o.get("name").getAsString();
                    this.friends.put(name.toLowerCase(Locale.ROOT), new String[] {name, o.has("note") ? o.get("note").getAsString() : ""});
                }
            }
        }
        catch (Exception ignored)
        {
            // start with an empty list
        }
    }

    private void saveFriends()
    {
        JsonArray array = new JsonArray();

        for (String[] f : this.friends.values())
        {
            JsonObject o = new JsonObject();
            o.addProperty("name", f[0]);
            o.addProperty("note", f[1]);
            array.add(o);
        }

        try
        {
            FileUtils.writeStringToFile(this.file(), new GsonBuilder().setPrettyPrinting().create().toJson(array), StandardCharsets.UTF_8);
        }
        catch (Exception ignored)
        {
            // best effort
        }
    }

    public boolean isFriend(String name)
    {
        this.load();
        return name != null && this.friends.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public int getColor()
    {
        return this.color.getColor();
    }

    public boolean highlightTab()
    {
        return this.highlightTab.isOn();
    }

    public boolean highlightNames()
    {
        return this.highlightNames.isOn();
    }

    private Set<String> onlineNames()
    {
        Set<String> names = new HashSet<String>();

        if (this.mc.getConnection() != null)
        {
            for (NetworkPlayerInfo info : this.mc.getConnection().getPlayerInfoMap())
            {
                names.add(info.getGameProfile().getName().toLowerCase(Locale.ROOT));
            }
        }

        return names;
    }

    public void onTick()
    {
        this.load();

        if (this.mc.world == null || this.mc.player == null || this.mc.player.ticksExisted % 20 != 0)
        {
            return;
        }

        Set<String> online = this.onlineNames();

        if (this.notify.isOn() && !this.lastOnline.isEmpty())
        {
            for (String key : this.friends.keySet())
            {
                if (online.contains(key) && !this.lastOnline.contains(key))
                {
                    Notifications.post("Friend joined", this.friends.get(key)[0] + " is online.", Notifications.Kind.SOCIAL);
                }
                else if (!online.contains(key) && this.lastOnline.contains(key))
                {
                    Notifications.post("Friend left", this.friends.get(key)[0] + " went offline.", Notifications.Kind.SOCIAL);
                }
            }
        }

        this.lastOnline = online;
    }

    /** The friends list screen. */
    public class GuiFriends extends GuiListScreen
    {
        public GuiFriends(GuiScreen parent)
        {
            super(parent, "Friends");
            FriendsManager.this.load();
        }

        protected String header()
        {
            return FriendsManager.this.friends.size() + " friends. Online status comes from this server's player list.";
        }

        protected List<Row> rows()
        {
            Set<String> online = FriendsManager.this.onlineNames();
            List<Row> rows = new ArrayList<Row>();

            for (Map.Entry<String, String[]> e : FriendsManager.this.friends.entrySet())
            {
                boolean on = online.contains(e.getKey());
                rows.add(new Row(e.getKey(), e.getValue()[0], e.getValue()[1], on ? "● Online" : "Offline", on ? 0xFF55FF55 : SythixTheme.BORDER));
            }

            return rows;
        }

        protected String emptyText()
        {
            return "No friends yet - press Add.";
        }

        protected String[] buttons()
        {
            return new String[] {"Add", "Note", "Remove", "Done"};
        }

        protected boolean enabled(int index)
        {
            return index == 0 || index == 3 || this.selected != null;
        }

        protected void press(int index)
        {
            String key = (String)this.selected;

            if (index == 0)
            {
                this.mc.displayGuiScreen(new GuiTextPrompt(this, "Add Friend", "Their Minecraft name.", "", 16, text -> {
                    String name = text.trim();

                    if (name.matches("[A-Za-z0-9_]{1,16}"))
                    {
                        FriendsManager.this.friends.put(name.toLowerCase(Locale.ROOT), new String[] {name, ""});
                        FriendsManager.this.saveFriends();
                    }
                }));
            }
            else if (index == 1 && key != null)
            {
                this.mc.displayGuiScreen(new GuiTextPrompt(this, "Note", "A note about " + FriendsManager.this.friends.get(key)[0] + ".", FriendsManager.this.friends.get(key)[1], 80, text -> {
                    FriendsManager.this.friends.get(key)[1] = text.trim();
                    FriendsManager.this.saveFriends();
                }));
            }
            else if (index == 2 && key != null)
            {
                FriendsManager.this.friends.remove(key);
                FriendsManager.this.saveFriends();
                this.selected = null;
            }
            else if (index == 3)
            {
                this.mc.displayGuiScreen(this.parent);
            }
        }
    }
}
