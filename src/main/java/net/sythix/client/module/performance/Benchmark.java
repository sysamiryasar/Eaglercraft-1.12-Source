package net.sythix.client.module.performance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.sythix.client.core.Notifications;
import org.apache.commons.io.FileUtils;

/**
 * Measures real frame rates so settings can be compared: average FPS, 1% low FPS and frame
 * times over a fixed time. Results are kept in sythix/benchmarks.json.
 */
public final class Benchmark
{
    private static final List<Long> FRAMES = new ArrayList<Long>();
    private static long start;
    private static long last;
    private static long duration;
    private static boolean spin;
    private static boolean running;
    private static float startYaw;
    private static String label = "";

    private Benchmark()
    {
    }

    public static boolean isRunning()
    {
        return running;
    }

    /** Progress 0..1 while running. */
    public static float progress()
    {
        return running ? Math.min(1.0F, (float)(System.nanoTime() - start) / (float)duration) : 0.0F;
    }

    public static void start(int seconds, boolean spinCamera, String name)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world == null || mc.player == null)
        {
            Notifications.post("Benchmark", "Join a world first - the benchmark measures in-game FPS.", Notifications.Kind.WARNING);
            return;
        }

        FRAMES.clear();
        // Wait one second before measuring so menus closing do not count
        start = System.nanoTime() + 1000000000L;
        last = 0L;
        duration = (long)seconds * 1000000000L;
        // Turning the camera is only done in singleplayer, where nobody else sees it
        spin = spinCamera && mc.isSingleplayer();
        startYaw = mc.player.rotationYaw;
        label = name;
        running = true;
        mc.displayGuiScreen(null);
        Notifications.post("Benchmark started", seconds + " seconds - don't touch anything.", Notifications.Kind.INFO);
    }

    public static void cancel()
    {
        running = false;
    }

    public static void onFrame()
    {
        if (!running)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world == null || mc.player == null)
        {
            running = false;
            Notifications.post("Benchmark stopped", "You left the world.", Notifications.Kind.WARNING);
            return;
        }

        long now = System.nanoTime();

        if (now < start)
        {
            return;
        }

        if (spin)
        {
            float t = (float)(now - start) / (float)duration;
            mc.player.rotationYaw = startYaw + t * 360.0F;
            mc.player.prevRotationYaw = mc.player.rotationYaw;
        }

        if (last != 0L)
        {
            FRAMES.add(Long.valueOf(now - last));
        }

        last = now;

        if (now - start >= duration)
        {
            running = false;
            finish(mc);
        }
    }

    private static void finish(Minecraft mc)
    {
        if (FRAMES.size() < 10)
        {
            Notifications.post("Benchmark failed", "Too few frames were drawn.", Notifications.Kind.ERROR);
            return;
        }

        long[] times = new long[FRAMES.size()];
        long total = 0L;

        for (int i = 0; i < times.length; ++i)
        {
            times[i] = FRAMES.get(i).longValue();
            total += times[i];
        }

        Arrays.sort(times);
        double avgMs = (double)total / (double)times.length / 1000000.0D;
        // 1% low = average FPS of the slowest 1% of frames
        int worstCount = Math.max(1, times.length / 100);
        long worstTotal = 0L;

        for (int i = times.length - worstCount; i < times.length; ++i)
        {
            worstTotal += times[i];
        }

        double lowMs = (double)worstTotal / (double)worstCount / 1000000.0D;
        GameSettings gs = mc.gameSettings;
        JsonObject result = new JsonObject();
        result.addProperty("time", Long.valueOf(System.currentTimeMillis()));
        result.addProperty("label", label);
        result.addProperty("avgFps", Double.valueOf(Math.round(10000.0D / avgMs) / 10.0D));
        result.addProperty("lowFps", Double.valueOf(Math.round(10000.0D / lowMs) / 10.0D));
        result.addProperty("avgMs", Double.valueOf(Math.round(avgMs * 100.0D) / 100.0D));
        result.addProperty("maxMs", Double.valueOf(Math.round((double)times[times.length - 1] / 10000.0D) / 100.0D));
        result.addProperty("frames", Integer.valueOf(times.length));
        result.addProperty("settings", "RD " + gs.renderDistanceChunks + ", " + (gs.fancyGraphics ? "Fancy" : "Fast") + ", limit " + gs.limitFramerate + (gs.enableVsync ? ", vsync" : "") + (gs.ofFastRender ? ", fast render" : ""));
        List<JsonObject> all = load();
        all.add(result);

        while (all.size() > 50)
        {
            all.remove(0);
        }

        save(all);
        JsonObject previous = all.size() > 1 ? all.get(all.size() - 2) : null;
        String compare = previous == null ? "" : String.format(" (%+.0f%% vs last)", (result.get("avgFps").getAsDouble() / Math.max(0.1D, previous.get("avgFps").getAsDouble()) - 1.0D) * 100.0D);
        Notifications.post("Benchmark done", result.get("avgFps").getAsString() + " FPS avg, " + result.get("lowFps").getAsString() + " 1% low" + compare, Notifications.Kind.SUCCESS);
    }

    private static File file()
    {
        return new File(new File(Minecraft.getMinecraft().mcDataDir, "sythix"), "benchmarks.json");
    }

    public static List<JsonObject> load()
    {
        List<JsonObject> list = new ArrayList<JsonObject>();

        try
        {
            if (file().isFile())
            {
                JsonArray array = new JsonParser().parse(FileUtils.readFileToString(file(), StandardCharsets.UTF_8)).getAsJsonArray();

                for (int i = 0; i < array.size(); ++i)
                {
                    list.add(array.get(i).getAsJsonObject());
                }
            }
        }
        catch (Exception ignored)
        {
            // A broken file just starts a new history
        }

        return list;
    }

    public static void save(List<JsonObject> list)
    {
        JsonArray array = new JsonArray();

        for (JsonObject o : list)
        {
            array.add(o);
        }

        try
        {
            FileUtils.writeStringToFile(file(), array.toString(), StandardCharsets.UTF_8);
        }
        catch (Exception ignored)
        {
        }
    }
}
