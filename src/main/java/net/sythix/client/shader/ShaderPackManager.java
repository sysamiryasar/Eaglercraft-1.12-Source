package net.sythix.client.shader;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import shadersmod.client.Shaders;

/**
 * Shader packs: installs Sythix's own packs into the game's shaderpacks folder, lists the
 * installed packs, imports new ones and switches between them (OptiFine's shader engine does the
 * rendering).
 */
public final class ShaderPackManager
{
    public static final String OFF = "OFF";
    public static final String INTERNAL = "(internal)";

    private ShaderPackManager()
    {
    }

    public static File folder()
    {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "shaderpacks");
        dir.mkdirs();
        return dir;
    }

    /** Copies the bundled packs (Sythix Vibrant, Sythix Shadows) when missing or outdated. */
    public static void installBundled()
    {
        try (InputStream in = ShaderPackManager.class.getResourceAsStream("/sythix/shaderpacks/index.txt"))
        {
            if (in == null)
            {
                return;
            }

            List<String> lines = IOUtils.readLines(in, StandardCharsets.UTF_8);
            String version = lines.get(0).substring("version=".length());
            File dir = folder();

            for (String line : lines.subList(1, lines.size()))
            {
                if (line.trim().isEmpty())
                {
                    continue;
                }

                String pack = line.substring(0, line.indexOf('/'));
                File marker = new File(new File(dir, pack), ".sythix-version");

                if (marker.isFile() && version.equals(FileUtils.readFileToString(marker, StandardCharsets.UTF_8).trim()))
                {
                    continue;
                }

                try (InputStream file = ShaderPackManager.class.getResourceAsStream("/sythix/shaderpacks/" + line))
                {
                    if (file != null)
                    {
                        FileUtils.copyInputStreamToFile(file, new File(dir, line));
                    }
                }
            }

            for (String line : lines.subList(1, lines.size()))
            {
                if (!line.trim().isEmpty())
                {
                    FileUtils.writeStringToFile(new File(new File(dir, line.substring(0, line.indexOf('/'))), ".sythix-version"), version, StandardCharsets.UTF_8);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /** OFF, the internal pack, then every pack in the folder. */
    public static List<String> packs()
    {
        return new ArrayList<String>(Shaders.listOfShadersSythix());
    }

    public static String current()
    {
        return Shaders.currentShaderSythix();
    }

    public static boolean enabled()
    {
        return !OFF.equals(current());
    }

    /** Switches to a pack (or OFF) and saves the choice. */
    public static void select(String name)
    {
        Minecraft.getMinecraft().gameSettings.saveOptions();
        Shaders.setShaderPack(name);
        Shaders.uninit();
        Shaders.storeConfig();
    }

    /**
     * Copies a shader pack (.zip or a folder containing "shaders") into the shaderpacks folder.
     * Returns the pack's name, or throws with a reason a player can read.
     */
    public static String importPack(File source) throws Exception
    {
        if (!source.exists())
        {
            throw new IllegalArgumentException("File not found: " + source);
        }

        boolean zip = source.isFile() && source.getName().toLowerCase().endsWith(".zip");
        boolean folder = source.isDirectory() && new File(source, "shaders").isDirectory();

        if (!zip && !folder)
        {
            throw new IllegalArgumentException("Not a shader pack: pick a .zip or a folder that contains a \"shaders\" folder");
        }

        File target = new File(folder(), source.getName());

        if (zip)
        {
            FileUtils.copyFile(source, target);
        }
        else
        {
            FileUtils.copyDirectory(source, target);
        }

        return source.getName();
    }

    public static String displayName(String pack)
    {
        if (OFF.equals(pack))
        {
            return "Off";
        }

        if (INTERNAL.equals(pack))
        {
            return "OptiFine Internal";
        }

        return pack.endsWith(".zip") ? pack.substring(0, pack.length() - 4) : pack;
    }

    public static String description(String pack)
    {
        if (OFF.equals(pack))
        {
            return "Normal Minecraft graphics. Fastest.";
        }

        if (INTERNAL.equals(pack))
        {
            return "OptiFine's simple built-in shaders.";
        }

        if (pack.equals("Sythix Vibrant"))
        {
            return "Sythix: colour grading, glow, waving plants. Fast.";
        }

        if (pack.equals("Sythix Shadows"))
        {
            return "Sythix: Vibrant plus sun and moon shadows.";
        }

        return pack.endsWith(".zip") ? "Shader pack (.zip)" : "Shader pack (folder)";
    }
}
