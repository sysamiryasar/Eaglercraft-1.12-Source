package net.sythix.client.replay;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;

/**
 * Replay files (replays/NAME.sythixreplay, gzip):
 * header "SYTHIX-REPLAY", version, metadata JSON; then entries of
 * [int time ms][int id][int length][bytes]. id >= 0 is a server packet (PLAY state, clientbound),
 * negative ids are Sythix records: SELF = your own movement each tick, EQUIPMENT, MARKER.
 * NAME.json next to it holds the summary (duration, server, markers) for the replay list.
 */
public final class ReplayFormat
{
    public static final String MAGIC = "SYTHIX-REPLAY";
    public static final int VERSION = 1;
    public static final int SELF = -1;
    public static final int EQUIPMENT = -2;
    public static final int MARKER = -3;
    public static final String EXTENSION = ".sythixreplay";

    private ReplayFormat()
    {
    }

    public static File folder()
    {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "replays");
        dir.mkdirs();
        return dir;
    }

    public static File summaryFile(File replay)
    {
        return new File(replay.getParentFile(), replay.getName().replace(EXTENSION, ".json"));
    }

    public static JsonObject summary(File replay)
    {
        try
        {
            File f = summaryFile(replay);
            return f.isFile() ? new JsonParser().parse(FileUtils.readFileToString(f, StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
        }
        catch (Exception e)
        {
            return new JsonObject();
        }
    }

    public static String time(long ms)
    {
        long s = Math.max(0L, ms) / 1000L;
        return s >= 3600L ? String.format("%d:%02d:%02d", s / 3600L, s / 60L % 60L, s % 60L) : String.format("%d:%02d", s / 60L, s % 60L);
    }
}
