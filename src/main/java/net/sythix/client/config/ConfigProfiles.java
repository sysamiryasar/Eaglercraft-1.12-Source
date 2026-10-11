package net.sythix.client.config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.sythix.client.SythixClient;
import org.apache.commons.io.FileUtils;

/**
 * Named setting profiles (sythix/profiles/*.json) and automatic backups (sythix/backups).
 */
public final class ConfigProfiles
{
    private ConfigProfiles()
    {
    }

    private static File dir(String name)
    {
        File d = new File(new File(Minecraft.getMinecraft().mcDataDir, "sythix"), name);
        d.mkdirs();
        return d;
    }

    public static String clean(String name)
    {
        String s = name.trim().replaceAll("[^A-Za-z0-9 _.-]", "");
        return s.length() > 32 ? s.substring(0, 32) : s;
    }

    public static List<String> list()
    {
        List<String> names = new ArrayList<String>();
        File[] files = dir("profiles").listFiles((d, n) -> n.endsWith(".json"));

        if (files != null)
        {
            Arrays.sort(files);

            for (File f : files)
            {
                names.add(f.getName().substring(0, f.getName().length() - 5));
            }
        }

        return names;
    }

    public static boolean exists(String name)
    {
        return new File(dir("profiles"), clean(name) + ".json").isFile();
    }

    /** Saves the current settings as a profile. */
    public static void save(String name) throws Exception
    {
        FileUtils.writeStringToFile(new File(dir("profiles"), clean(name) + ".json"), SythixClient.get().getConfigManager().toJsonString(), StandardCharsets.UTF_8);
    }

    public static String read(String name) throws Exception
    {
        return FileUtils.readFileToString(new File(dir("profiles"), clean(name) + ".json"), StandardCharsets.UTF_8);
    }

    /** Loads a profile into the running client and saves it as the current config. */
    public static void load(String name) throws Exception
    {
        SythixClient.get().getModuleManager().resetAll();
        SythixClient.get().getConfigManager().apply(read(name));
        SythixClient.get().save();
    }

    public static void delete(String name)
    {
        new File(dir("profiles"), clean(name) + ".json").delete();
    }

    public static boolean rename(String from, String to)
    {
        return new File(dir("profiles"), clean(from) + ".json").renameTo(new File(dir("profiles"), clean(to) + ".json"));
    }

    public static void importText(String name, String json) throws Exception
    {
        new com.google.gson.JsonParser().parse(json).getAsJsonObject();
        FileUtils.writeStringToFile(new File(dir("profiles"), clean(name) + ".json"), json, StandardCharsets.UTF_8);
    }

    // ---- Backups --------------------------------------------------------------------------------

    public static String backup() throws Exception
    {
        String name = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        FileUtils.writeStringToFile(new File(dir("backups"), name + ".json"), SythixClient.get().getConfigManager().toJsonString(), StandardCharsets.UTF_8);
        return name;
    }

    public static List<String> backups()
    {
        List<String> names = new ArrayList<String>();
        File[] files = dir("backups").listFiles((d, n) -> n.endsWith(".json"));

        if (files != null)
        {
            Arrays.sort(files);

            for (File f : files)
            {
                names.add(f.getName().substring(0, f.getName().length() - 5));
            }
        }

        return names;
    }

    /** Deletes the oldest backups so at most keep remain. */
    public static void prune(int keep)
    {
        List<String> all = backups();

        for (int i = 0; i < all.size() - keep; ++i)
        {
            new File(dir("backups"), all.get(i) + ".json").delete();
        }
    }

    public static void restoreBackup(String name) throws Exception
    {
        String json = FileUtils.readFileToString(new File(dir("backups"), name + ".json"), StandardCharsets.UTF_8);
        SythixClient.get().getModuleManager().resetAll();
        SythixClient.get().getConfigManager().apply(json);
        SythixClient.get().save();
    }
}
