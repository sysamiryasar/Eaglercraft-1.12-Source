package net.sythix.client.module.multiplayer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;

/**
 * Favourites, folders and notes for servers, keyed by address (sythix/servers.json).
 */
public final class ServerMeta
{
    private static JsonObject data;

    private ServerMeta()
    {
    }

    private static File file()
    {
        return new File(new File(Minecraft.getMinecraft().mcDataDir, "sythix"), "servers.json");
    }

    private static JsonObject root()
    {
        if (data == null)
        {
            try
            {
                data = file().isFile() ? new JsonParser().parse(FileUtils.readFileToString(file(), StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
            }
            catch (Exception e)
            {
                data = new JsonObject();
            }
        }

        return data;
    }

    private static JsonObject entry(String address)
    {
        String key = address.toLowerCase(Locale.ROOT).trim();

        if (!root().has(key))
        {
            root().add(key, new JsonObject());
        }

        return root().getAsJsonObject(key);
    }

    public static boolean isFavorite(String address)
    {
        JsonObject e = entry(address);
        return e.has("favorite") && e.get("favorite").getAsBoolean();
    }

    public static void setFavorite(String address, boolean value)
    {
        entry(address).addProperty("favorite", Boolean.valueOf(value));
        save();
    }

    public static String folder(String address)
    {
        JsonObject e = entry(address);
        return e.has("folder") ? e.get("folder").getAsString() : "";
    }

    public static String note(String address)
    {
        JsonObject e = entry(address);
        return e.has("note") ? e.get("note").getAsString() : "";
    }

    public static void setFolder(String address, String folder)
    {
        entry(address).addProperty("folder", folder.trim());
        save();
    }

    public static void setNote(String address, String note)
    {
        entry(address).addProperty("note", note.trim());
        save();
    }

    public static void save()
    {
        try
        {
            FileUtils.writeStringToFile(file(), new GsonBuilder().setPrettyPrinting().create().toJson(root()), StandardCharsets.UTF_8);
        }
        catch (Exception ignored)
        {
            // best effort
        }
    }
}
