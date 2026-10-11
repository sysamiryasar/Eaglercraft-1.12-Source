package net.sythix.client.cosmetic;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.apache.commons.io.FileUtils;

/**
 * The player's profile: name, skin, skin model and cape. Saved to sythix/profile.json.
 */
public final class ProfileStore
{
    public static String name = "";
    /** Skin file name in sythix/skins, or "" for the default skin. */
    public static String skin = "";
    public static boolean slim;
    /** Built-in cape id, "custom:<file>" for a file in sythix/capes, or "none". */
    public static String cape = "sythix";

    private static File gameDir = new File(".");

    private ProfileStore()
    {
    }

    private static File file()
    {
        return new File(new File(gameDir, "sythix"), "profile.json");
    }

    public static File skinsDir()
    {
        File dir = new File(new File(gameDir, "sythix"), "skins");
        dir.mkdirs();
        return dir;
    }

    public static File capesDir()
    {
        File dir = new File(new File(gameDir, "sythix"), "capes");
        dir.mkdirs();
        return dir;
    }

    /** True for names Minecraft servers accept: 3-16 letters, digits or underscores. */
    public static boolean isValidName(String candidate)
    {
        return candidate != null && candidate.matches("[A-Za-z0-9_]{3,16}");
    }

    /** Loads the profile; used before the game window opens so the saved name is used. */
    public static void load(File dir)
    {
        gameDir = dir;

        try
        {
            if (file().isFile())
            {
                JsonObject o = new JsonParser().parse(FileUtils.readFileToString(file(), StandardCharsets.UTF_8)).getAsJsonObject();
                name = o.has("name") ? o.get("name").getAsString() : "";
                skin = o.has("skin") ? o.get("skin").getAsString() : "";
                slim = o.has("slim") && o.get("slim").getAsBoolean();
                cape = o.has("cape") ? o.get("cape").getAsString() : "sythix";
            }
        }
        catch (Exception ignored)
        {
            // fall back to defaults
        }

        if (!isValidName(name))
        {
            // First launch: pick a name and remember it so it stays the same
            name = "Player" + (100 + new Random().nextInt(900));
            save();
        }
    }

    public static void save()
    {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("skin", skin);
        o.addProperty("slim", Boolean.valueOf(slim));
        o.addProperty("cape", cape);

        try
        {
            FileUtils.writeStringToFile(file(), new GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
        }
        catch (Exception ignored)
        {
            // saving the profile is best effort
        }
    }
}
