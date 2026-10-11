package net.sythix.client.module.recording;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.GuiTextPrompt;
import net.sythix.client.ui.SythixTheme;
import org.apache.commons.io.FileUtils;

/**
 * #48 Session timeline with bookmarks. Video playback is NOT implemented - bookmarks mark moments
 * (time, place, note) so you can find them later in your own recordings.
 */
public class ReplaySystem extends Module
{
    private final KeybindSetting bookmarkKey;
    private final List<JsonObject> bookmarks = new ArrayList<JsonObject>();
    private long sessionStart = System.currentTimeMillis();
    private boolean loaded;

    public ReplaySystem()
    {
        super("Replay System", "Session timeline and bookmarks. Video replay playback is not implemented.", Category.RECORDING, true);
        this.add(new InfoSetting("Status", "What is and is not available.", () -> "Bookmarks + timeline (no video playback)"));
        this.bookmarkKey = this.add(new KeybindSetting("Bookmark Key", "Mark the current moment.", 0));
        this.add(new ButtonSetting("Timeline", "See, rename and delete your bookmarks.", "Open", () -> this.mc.displayGuiScreen(new GuiTimeline(this.mc.currentScreen))));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiTimeline(parent);
    }

    private File file()
    {
        return new File(new File(this.mc.mcDataDir, "sythix"), "bookmarks.json");
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
                    this.bookmarks.add(e.getAsJsonObject());
                }
            }
        }
        catch (Exception ignored)
        {
            // start empty
        }
    }

    private void saveBookmarks()
    {
        JsonArray array = new JsonArray();

        for (JsonObject o : this.bookmarks)
        {
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

    public void onKey(int keyCode)
    {
        if (keyCode != 0 && keyCode == this.bookmarkKey.getKey() && this.mc.player != null)
        {
            this.load();
            JsonObject o = new JsonObject();
            long now = System.currentTimeMillis();
            o.addProperty("time", Long.valueOf(now));
            o.addProperty("session", Long.valueOf(now - this.sessionStart));
            o.addProperty("note", "Bookmark " + (this.bookmarks.size() + 1));
            o.addProperty("where", (int)this.mc.player.posX + ", " + (int)this.mc.player.posY + ", " + (int)this.mc.player.posZ);
            o.addProperty("world", net.sythix.client.waypoint.WaypointStore.currentKey());
            this.bookmarks.add(o);
            this.saveBookmarks();
            Notifications.post("Bookmarked", "Moment saved to the timeline.", Notifications.Kind.SUCCESS);
        }
    }

    /** Bookmark list. */
    public class GuiTimeline extends GuiListScreen
    {
        public GuiTimeline(GuiScreen parent)
        {
            super(parent, "Timeline & Bookmarks");
            ReplaySystem.this.load();
        }

        protected String header()
        {
            return "Video playback is not implemented. Use bookmarks to find moments in your own recordings.";
        }

        protected List<Row> rows()
        {
            List<Row> rows = new ArrayList<Row>();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (int i = ReplaySystem.this.bookmarks.size() - 1; i >= 0; --i)
            {
                JsonObject o = ReplaySystem.this.bookmarks.get(i);
                long session = o.get("session").getAsLong();
                String when = fmt.format(new Date(o.get("time").getAsLong()));
                rows.add(new Row(Integer.valueOf(i), o.get("note").getAsString(), when + "  @ " + o.get("where").getAsString() + "  (" + o.get("world").getAsString() + ")", String.format("+%d:%02d", session / 60000L, session / 1000L % 60L), SythixTheme.RED));
            }

            return rows;
        }

        protected String emptyText()
        {
            return "No bookmarks yet - set a Bookmark Key and press it in game.";
        }

        protected String[] buttons()
        {
            return new String[] {"Rename", "Delete", "Clear All", "Done"};
        }

        protected boolean enabled(int index)
        {
            return index >= 2 || this.selected != null;
        }

        protected void press(int index)
        {
            if (index == 0 && this.selected != null)
            {
                JsonObject o = ReplaySystem.this.bookmarks.get(((Integer)this.selected).intValue());
                this.mc.displayGuiScreen(new GuiTextPrompt(this, "Rename Bookmark", "A short description of this moment.", o.get("note").getAsString(), 60, text -> {
                    o.addProperty("note", text.trim());
                    ReplaySystem.this.saveBookmarks();
                }));
            }
            else if (index == 1 && this.selected != null)
            {
                ReplaySystem.this.bookmarks.remove(((Integer)this.selected).intValue());
                ReplaySystem.this.saveBookmarks();
                this.selected = null;
            }
            else if (index == 2)
            {
                ReplaySystem.this.bookmarks.clear();
                ReplaySystem.this.saveBookmarks();
                this.selected = null;
            }
            else if (index == 3)
            {
                this.mc.displayGuiScreen(this.parent);
            }
        }
    }
}
