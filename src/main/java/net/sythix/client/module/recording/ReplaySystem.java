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
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.replay.ReplayFormat;
import net.sythix.client.replay.ReplayPlayer;
import net.sythix.client.replay.ReplayRecorder;
import net.sythix.client.replay.GuiReplays;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.GuiTextPrompt;
import net.sythix.client.ui.SythixTheme;
import org.apache.commons.io.FileUtils;

/**
 * #48 Replays: records your sessions (what the server sent, plus your own movement) and plays
 * them back with a free camera, pause, speed and seeking. Bookmarks mark moments on the timeline.
 */
public class ReplaySystem extends Module
{
    private final KeybindSetting bookmarkKey;
    private final ModeSetting recordMode;
    private final NumberSetting keep;
    private final KeybindSetting pauseKey;
    private final KeybindSetting backKey;
    private final KeybindSetting forwardKey;
    private final KeybindSetting slowerKey;
    private final KeybindSetting fasterKey;
    private final KeybindSetting markerKey;
    private final List<JsonObject> bookmarks = new ArrayList<JsonObject>();
    private long sessionStart = System.currentTimeMillis();
    private boolean loaded;

    public ReplaySystem()
    {
        super("Replay System", "Record your games and watch them again with a free camera, pause, speed and seeking.", Category.RECORDING, true);
        this.add(new ButtonSetting("Replays", "Watch, rename and delete your replays.", "Open", () -> this.mc.displayGuiScreen(new GuiReplays(this.mc.currentScreen))));
        this.recordMode = this.add(new ModeSetting("Record Replays", "Which sessions are recorded automatically (from the moment you join).", "Off", "Off", "Everything", "Multiplayer", "Singleplayer"));
        this.add(new InfoSetting("Recording Now", "Whether this session is being recorded.", () -> ReplayRecorder.isRecording() ? "Yes - " + ReplayFormat.time(ReplayRecorder.elapsed()) : "No"));
        this.keep = this.add(new NumberSetting("Keep Replays", "Older replays are deleted when there are more than this (0 = keep all).", 20.0D, 0.0D, 200.0D, 1.0D));
        this.bookmarkKey = this.add(new KeybindSetting("Bookmark Key", "Mark the current moment (also adds a marker to the replay).", 0));
        this.add(new ButtonSetting("Timeline", "See, rename and delete your bookmarks.", "Open", () -> this.mc.displayGuiScreen(new GuiTimeline(this.mc.currentScreen))));
        this.pauseKey = this.add(new KeybindSetting("Replay: Pause Key", "Pause or play while watching a replay.", 25));
        this.backKey = this.add(new KeybindSetting("Replay: Back 10s Key", "Jump 10 seconds back.", 203));
        this.forwardKey = this.add(new KeybindSetting("Replay: Forward 10s Key", "Jump 10 seconds forward.", 205));
        this.slowerKey = this.add(new KeybindSetting("Replay: Slower Key", "Slow the replay down.", 26));
        this.fasterKey = this.add(new KeybindSetting("Replay: Faster Key", "Speed the replay up.", 27));
        this.markerKey = this.add(new KeybindSetting("Replay: Next Marker Key", "Jump to the next marker.", 50));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiReplays(parent);
    }

    /** Whether a session that is starting now should be recorded. */
    public boolean shouldRecord(boolean singleplayer)
    {
        switch (this.recordMode.get())
        {
            case "Everything":
                return true;
            case "Multiplayer":
                return !singleplayer;
            case "Singleplayer":
                return singleplayer;
            default:
                return false;
        }
    }

    public void onTick()
    {
        if (!ReplayRecorder.isRecording() && this.mc.world == null && !this.pruned)
        {
            this.pruned = true;
            this.prune();
        }
        else if (this.mc.world != null)
        {
            this.pruned = false;
        }
    }

    private boolean pruned;

    /** Deletes the oldest replays beyond the Keep Replays limit. */
    private void prune()
    {
        int limit = this.keep.getInt();
        File[] files = ReplayFormat.folder().listFiles((d, n) -> n.endsWith(ReplayFormat.EXTENSION));

        if (limit <= 0 || files == null || files.length <= limit)
        {
            return;
        }

        java.util.Arrays.sort(files);

        for (int i = 0; i < files.length - limit; ++i)
        {
            ReplayFormat.summaryFile(files[i]).delete();
            files[i].delete();
        }
    }

    public String keysHint()
    {
        return key(this.pauseKey) + " pause  " + key(this.backKey) + "/" + key(this.forwardKey) + " 10s  " + key(this.slowerKey) + "/" + key(this.fasterKey) + " speed  " + key(this.markerKey) + " marker  Esc: leave";
    }

    private static String key(KeybindSetting k)
    {
        switch (k.getKeyName())
        {
            case "LBRACKET":
                return "[";
            case "RBRACKET":
                return "]";
            case "LEFT":
                return "Left";
            case "RIGHT":
                return "Right";
            default:
                return k.getKeyName();
        }
    }

    /** Replay controls; returns true if the key was used. */
    private boolean replayKey(int keyCode)
    {
        if (!ReplayPlayer.isActive() || keyCode == 0)
        {
            return false;
        }

        if (keyCode == this.pauseKey.getKey())
        {
            ReplayPlayer.togglePause();
        }
        else if (keyCode == this.backKey.getKey())
        {
            ReplayPlayer.seek(ReplayPlayer.getTime() - 10000L);
        }
        else if (keyCode == this.forwardKey.getKey())
        {
            ReplayPlayer.seek(ReplayPlayer.getTime() + 10000L);
        }
        else if (keyCode == this.slowerKey.getKey())
        {
            ReplayPlayer.slower();
        }
        else if (keyCode == this.fasterKey.getKey())
        {
            ReplayPlayer.faster();
        }
        else if (keyCode == this.markerKey.getKey())
        {
            ReplayPlayer.nextMarker();
        }
        else
        {
            return false;
        }

        return true;
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
        if (this.replayKey(keyCode))
        {
            return;
        }

        if (keyCode != 0 && keyCode == this.bookmarkKey.getKey() && this.mc.player != null)
        {
            ReplayRecorder.marker();
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
            return "Bookmarks from all sessions. In recorded sessions they also appear as markers on the replay timeline.";
        }

        protected List<Row> rows()
        {
            List<Row> rows = new ArrayList<Row>();
            SimpleDateFormat fmt = net.sythix.client.module.advanced.Localization.dateTime(true);

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
