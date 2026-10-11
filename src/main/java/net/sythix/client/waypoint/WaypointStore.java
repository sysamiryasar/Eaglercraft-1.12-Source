package net.sythix.client.waypoint;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.integrated.IntegratedServer;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Waypoints for every world and server, saved to sythix/waypoints.json.
 */
public final class WaypointStore
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, List<Waypoint>> WORLDS = new LinkedHashMap<String, List<Waypoint>>();
    private static boolean loaded;

    private WaypointStore()
    {
    }

    private static File file()
    {
        return new File(new File(Minecraft.getMinecraft().mcDataDir, "sythix"), "waypoints.json");
    }

    /** Identifies the current world: "server:<address>" or "world:<save folder>". */
    public static String currentKey()
    {
        Minecraft mc = Minecraft.getMinecraft();
        IntegratedServer server = mc.getIntegratedServer();

        if (server != null)
        {
            return "world:" + server.getFolderName();
        }

        ServerData data = mc.getCurrentServerData();
        return data != null ? "server:" + data.serverIP.toLowerCase() : "unknown";
    }

    public static synchronized List<Waypoint> current()
    {
        load();
        return WORLDS.computeIfAbsent(currentKey(), k -> new ArrayList<Waypoint>());
    }

    public static synchronized void load()
    {
        if (loaded)
        {
            return;
        }

        loaded = true;

        try
        {
            if (file().isFile())
            {
                JsonObject root = new JsonParser().parse(FileUtils.readFileToString(file(), StandardCharsets.UTF_8)).getAsJsonObject();

                for (Map.Entry<String, JsonElement> entry : root.entrySet())
                {
                    WORLDS.put(entry.getKey(), parseList(entry.getValue().getAsJsonArray()));
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.error("[Sythix] Could not read waypoints", e);
        }
    }

    public static List<Waypoint> parseList(JsonArray array)
    {
        List<Waypoint> list = new ArrayList<Waypoint>();

        for (JsonElement element : array)
        {
            try
            {
                list.add(Waypoint.fromJson(element.getAsJsonObject()));
            }
            catch (RuntimeException ignored)
            {
                // skip broken entries
            }
        }

        return list;
    }

    public static JsonArray toArray(List<Waypoint> list)
    {
        JsonArray array = new JsonArray();

        for (Waypoint w : list)
        {
            array.add(w.toJson());
        }

        return array;
    }

    public static synchronized void save()
    {
        JsonObject root = new JsonObject();

        for (Map.Entry<String, List<Waypoint>> entry : WORLDS.entrySet())
        {
            root.add(entry.getKey(), toArray(entry.getValue()));
        }

        try
        {
            FileUtils.writeStringToFile(file(), new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            LOGGER.error("[Sythix] Could not save waypoints", e);
        }
    }
}
