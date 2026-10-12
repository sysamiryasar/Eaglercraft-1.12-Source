package net.sythix.client.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Texture packs that ship inside the client (src/main/resources/sythix/texturepacks, built with
 * branding/texture_packs.py):
 *
 * - Client Pack (Texture Pack Manager): Cowarted Vanilla is Sythix's default look; Blue (Fallen
 *   Angel) or Red (Red Revolution) replace it while chosen. It sits just above Minecraft's own
 *   textures, so any resource pack you pick still goes on top of it.
 * - Harlu Cart Pack is for cart PvP (clear rails, low fire, small particles) and is switched on in
 *   Texture Pack Manager (Cart Pack) or by the Cart PvP layout in Smart HUD Layout.
 *
 * In the browser the packs' files are not in the game jar but in zips next to it (packs/NAME-N.zip,
 * listed in the sythix.packs property by CompileWebRuntime.sh), so a pack is only downloaded once it
 * is actually used instead of every pack at every first start.
 */
public final class BuiltInPacks
{
    public static final String CART_SETTING = "Cart Pack";
    public static final String THEME_SETTING = "Client Pack";
    public static final String DEFAULT_THEME = "Default (Cowarted Vanilla)";
    public static final String BLUE_THEME = "Blue (Fallen Angel)";
    public static final String RED_THEME = "Red (Red Revolution)";
    public static final String[] THEMES = {DEFAULT_THEME, BLUE_THEME, RED_THEME};

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Pack DEFAULT = new Pack("cowarted", "Cowarted Vanilla");
    private static final Pack BLUE = new Pack("fallenangel", "Fallen Angel");
    private static final Pack RED = new Pack("redrevolution", "Red Revolution");
    private static final Pack CART = new Pack("harlu", "Harlu Cart Pack");

    /** The chosen client pack and whether the cart pack is on; null until read from the settings. */
    private static String theme;
    private static Boolean cartPack;

    private BuiltInPacks()
    {
    }

    /**
     * Adds the active built-in packs right after Minecraft's own (called on every resource reload):
     * one client pack (Cowarted Vanilla unless Blue or Red is chosen) and the cart pack on top of it
     * when that is switched on. Packs the player adds still go on top of these.
     */
    public static void addTo(List<IResourcePack> packs)
    {
        int at = Math.min(1, packs.size());
        packs.add(at, themePack(theme()));

        if (cartPackOn())
        {
            packs.add(at + 1, CART);
        }
    }

    private static Pack themePack(String name)
    {
        return BLUE_THEME.equals(name) ? BLUE : (RED_THEME.equals(name) ? RED : DEFAULT);
    }

    public static String theme()
    {
        if (theme == null)
        {
            JsonElement saved = savedSetting(THEME_SETTING);
            theme = saved != null && saved.isJsonPrimitive() ? saved.getAsString() : DEFAULT_THEME;
        }

        return theme;
    }

    public static boolean cartPackOn()
    {
        if (cartPack == null)
        {
            JsonElement saved = savedSetting(CART_SETTING);
            cartPack = Boolean.valueOf(saved != null && saved.isJsonPrimitive() && saved.getAsBoolean());
        }

        return cartPack.booleanValue();
    }

    /** Follows the Texture Pack Manager settings and reloads textures once when anything changed. */
    public static void sync(String newTheme, boolean newCart)
    {
        boolean loaded = theme != null && cartPack != null;
        boolean changed = !themePack(theme()).equals(themePack(newTheme)) || cartPackOn() != newCart;
        theme = newTheme;
        cartPack = Boolean.valueOf(newCart);
        Minecraft mc = Minecraft.getMinecraft();

        if (changed && loaded && mc.getTextureManager() != null)
        {
            mc.addScheduledTask(mc::refreshResources);
        }
    }

    /** The first resource load happens before Sythix's settings are loaded: read them from config.json. */
    private static JsonElement savedSetting(String name)
    {
        try
        {
            File config = new File(new File(Minecraft.getMinecraft().mcDataDir, "sythix"), "config.json");

            if (config.isFile())
            {
                JsonObject root = new JsonParser().parse(FileUtils.readFileToString(config, StandardCharsets.UTF_8)).getAsJsonObject();
                return root.getAsJsonObject("modules").getAsJsonObject("Texture Pack Manager").getAsJsonObject("settings").get(name);
            }
        }
        catch (Exception ignored)
        {
            // no saved setting yet
        }

        return null;
    }

    /** A pack read from the client jar, with a file index so lookups never search the jar. */
    private static final class Pack implements IResourcePack
    {
        private final String root;
        private final String name;
        private final String folder;
        private Set<String> files;
        private List<ZipFile> zips;

        Pack(String folder, String name)
        {
            this.folder = folder;
            this.root = "/sythix/texturepacks/" + folder + "/";
            this.name = name;
        }

        /** Browser only: this pack's zips, opened (and so downloaded) the first time a file is read. */
        private List<ZipFile> zips()
        {
            if (this.zips == null)
            {
                List<ZipFile> list = new ArrayList<ZipFile>();
                String dir = System.getProperty("sythix.packdir");

                for (String zip : System.getProperty("sythix.packs", "").split(","))
                {
                    if (zip.startsWith(this.folder + "-"))
                    {
                        try
                        {
                            list.add(new ZipFile(new File(dir, zip)));
                        }
                        catch (IOException e)
                        {
                            LOGGER.warn("[Sythix] Could not open built-in pack file " + zip + ": " + e);
                        }
                    }
                }

                this.zips = list;
            }

            return this.zips;
        }

        private InputStream open(String path) throws IOException
        {
            if (System.getProperty("sythix.packdir") == null)
            {
                return BuiltInPacks.class.getResourceAsStream(this.root + path);
            }

            for (ZipFile zip : this.zips())
            {
                ZipEntry entry = zip.getEntry(path);

                if (entry != null)
                {
                    return zip.getInputStream(entry);
                }
            }

            return null;
        }

        private Set<String> files()
        {
            if (this.files == null)
            {
                Set<String> set = new HashSet<String>();

                try (InputStream in = BuiltInPacks.class.getResourceAsStream(this.root + "index.txt"))
                {
                    if (in != null)
                    {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                        String line;

                        while ((line = reader.readLine()) != null)
                        {
                            if (!line.isEmpty())
                            {
                                set.add(line);
                            }
                        }
                    }
                }
                catch (IOException ignored)
                {
                    // an empty pack changes nothing
                }

                this.files = set;
            }

            return this.files;
        }

        private static String path(ResourceLocation location)
        {
            return "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
        }

        public InputStream getInputStream(ResourceLocation location) throws IOException
        {
            InputStream in = this.resourceExists(location) ? this.open(path(location)) : null;

            if (in == null)
            {
                throw new FileNotFoundException(location.toString());
            }

            return in;
        }

        public boolean resourceExists(ResourceLocation location)
        {
            return this.files().contains(path(location));
        }

        public Set<String> getResourceDomains()
        {
            return Collections.singleton("minecraft");
        }

        public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException
        {
            return null;
        }

        public BufferedImage getPackImage() throws IOException
        {
            InputStream in = BuiltInPacks.class.getResourceAsStream(this.root + "pack.png");

            if (in == null)
            {
                throw new FileNotFoundException(this.name + " pack.png");
            }

            try
            {
                return ImageIO.read(in);
            }
            finally
            {
                in.close();
            }
        }

        public String getPackName()
        {
            return this.name;
        }
    }
}
