package net.sythix.client.replay;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.module.advanced.Localization;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.GuiTextPrompt;
import net.sythix.client.ui.SythixTheme;
import org.apache.commons.io.FileUtils;

/**
 * Your replays: watch, rename, delete.
 */
public class GuiReplays extends GuiListScreen
{
    private List<Row> cached;

    public GuiReplays(GuiScreen parent)
    {
        super(parent, "Replays");
    }

    protected String header()
    {
        return ReplayRecorder.isRecording() ? "Recording this session now (" + ReplayFormat.time(ReplayRecorder.elapsed()) + "). Double-click a replay to watch it." : "Double-click a replay to watch it. Turn on Record Replays in Replay System to record.";
    }

    protected List<Row> rows()
    {
        if (this.cached == null)
        {
            this.cached = new ArrayList<Row>();
            File[] files = ReplayFormat.folder().listFiles((d, n) -> n.endsWith(ReplayFormat.EXTENSION));

            if (files != null)
            {
                Arrays.sort(files);

                for (int i = files.length - 1; i >= 0; --i)
                {
                    File f = files[i];

                    if (f.equals(ReplayRecorder.current()) && ReplayRecorder.isRecording())
                    {
                        continue;
                    }

                    JsonObject s = ReplayFormat.summary(f);
                    String title = s.has("title") ? s.get("title").getAsString() : f.getName().replace(ReplayFormat.EXTENSION, "");
                    String server = s.has("server") ? s.get("server").getAsString() : "?";
                    String when = s.has("started") ? Localization.dateTime(false).format(new java.util.Date(s.get("started").getAsLong())) : "";
                    String markers = s.has("markers") && s.getAsJsonArray("markers").size() > 0 ? ", " + s.getAsJsonArray("markers").size() + " markers" : "";
                    this.cached.add(new Row(f, title, when + "  " + server + "  " + FileUtils.byteCountToDisplaySize(f.length()) + markers, s.has("duration") ? ReplayFormat.time(s.get("duration").getAsLong()) : "?", SythixTheme.RED));
                }
            }
        }

        return this.cached;
    }

    protected String emptyText()
    {
        return "No replays yet.";
    }

    protected String[] buttons()
    {
        return new String[] {"Watch", "Rename", "Delete", "Open Folder", "Done"};
    }

    protected boolean enabled(int index)
    {
        return index >= 3 || this.selected != null;
    }

    protected void open(Object key)
    {
        ReplayPlayer.play((File)key);
    }

    protected void press(int index)
    {
        File f = (File)this.selected;

        switch (index)
        {
            case 0:
                ReplayPlayer.play(f);
                break;
            case 1:
                JsonObject s = ReplayFormat.summary(f);
                this.mc.displayGuiScreen(new GuiTextPrompt(this, "Rename Replay", "A name for this replay.", s.has("title") ? s.get("title").getAsString() : "", 60, text -> {
                    s.addProperty("title", text.trim());

                    try
                    {
                        FileUtils.writeStringToFile(ReplayFormat.summaryFile(f), s.toString(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                    catch (Exception e)
                    {
                        this.status = "Could not rename: " + e.getMessage();
                    }

                    this.cached = null;
                }));
                break;
            case 2:
                ReplayFormat.summaryFile(f).delete();
                f.delete();
                this.selected = null;
                this.cached = null;
                break;
            case 3:
                net.minecraft.client.renderer.OpenGlHelper.openFile(ReplayFormat.folder());
                break;
            default:
                this.mc.displayGuiScreen(this.parent);
        }
    }
}
