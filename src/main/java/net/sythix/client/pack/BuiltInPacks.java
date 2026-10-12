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
 * - Texture Pack (Texture Pack Manager) picks one of the CHOICES below, in this order:
 *   SMP/Red (Red Revolution), SMP/Blue (Fallen Angel), Cart (Cowarted Vanilla with the Harlu Cart
 *   Pack on top: clear rails, low fire, small particles) and Default (Cowarted Vanilla, Sythix's
 *   normal look). The packs sit just above Minecraft's own textures, so any resource pack you pick
 *   still goes on top of them.
 *
 * To add another built-in pack: put it in src/main/resources/sythix/texturepacks/FOLDER (made with
 * branding/texture_packs.py), add a Pack for it and a line to CHOICES.
 *
 * In the browser the packs' files are not in the game jar but in zips next to it (packs/NAME-N.zip,
 * listed in the sythix.packs property by CompileWebRuntime.sh), so a pack is only downloaded once it
 * is actually used instead of every pack at every first start.
 */
public final class BuiltInPacks
{
    public static final String SETTING = "Texture Pack";
    public static final String RED_CHOICE = "SMP/Red";
    public static final String BLUE_CHOICE = "SMP/Blue";
    public static final String CART_CHOICE = "Cart";
    public static final String DEFAULT_CHOICE = "Default";

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Pack COWARTED = new Pack("cowarted", "Cowarted Vanilla");
    private static final Pack FALLEN_ANGEL = new Pack("fallenangel", "Fallen Angel");
    private static final Pack RED_REVOLUTION = new Pack("redrevolution", "Red Revolution");
    private static final Pack HARLU = new Pack("harlu", "Harlu Cart Pack");

    /** The choices in menu order, each with its packs (bottom to top). */
    private static final Choice[] CHOICES = {
        new Choice(RED_CHOICE, "Red Revolution", RED_REVOLUTION),
        new Choice(BLUE_CHOICE, "Fallen Angel", FALLEN_ANGEL),
        new Choice(CART_CHOICE, "Cowarted Vanilla + Harlu Cart Pack", COWARTED, HARLU),
        new Choice(DEFAULT_CHOICE, "Cowarted Vanilla", COWARTED)
    };

    /** The chosen pack; null until read from the settings. */
    private static String choice;

    private BuiltInPacks()
    {
    }

    /** The names shown in Texture Pack Manager, in order. */
    public static String[] choiceNames()
    {
        String[] names = new String[CHOICES.length];

        for (int i = 0; i < CHOICES.length; ++i)
        {
            names[i] = CHOICES[i].name;
        }

        return names;
    }

    /** One line per choice for the setting's description, e.g. "SMP/Red: Red Revolution". */
    public static String describeChoices()
    {
        StringBuilder text = new StringBuilder();

        for (Choice c : CHOICES)
        {
            text.append(text.length() == 0 ? "" : ", ").append(c.name).append(" = ").append(c.description);
        }

        return text.toString();
    }

    private static Choice find(String name)
    {
        for (Choice c : CHOICES)
        {
            if (c.name.equals(name))
            {
                return c;
            }
        }

        return CHOICES[CHOICES.length - 1];
    }

    /**
     * Adds the chosen pack(s) right after Minecraft's own (called on every resource reload).
     * Packs the player adds still go on top of these.
     */
    public static void addTo(List<IResourcePack> packs)
    {
        int at = Math.min(1, packs.size());

        for (Pack pack : find(choice()).packs)
        {
            packs.add(at++, pack);
        }
    }

    public static String choice()
    {
        if (choice == null)
        {
            choice = migrate(savedSetting(SETTING), savedSetting("Client Pack"), savedSetting("Cart Pack"));
        }

        return choice;
    }

    /**
     * The saved choice, or the one matching older settings ("Client Pack" Default/Blue/Red plus the
     * separate "Cart Pack" switch) so nobody loses their pack after an update.
     */
    public static String migrate(JsonElement saved, JsonElement oldTheme, JsonElement oldCart)
    {
        if (saved != null && saved.isJsonPrimitive())
        {
            return find(saved.getAsString()).name;
        }

        String theme = oldTheme != null && oldTheme.isJsonPrimitive() ? oldTheme.getAsString() : "";

        if (theme.startsWith("Red"))
        {
            return RED_CHOICE;
        }

        if (theme.startsWith("Blue"))
        {
            return BLUE_CHOICE;
        }

        return oldCart != null && oldCart.isJsonPrimitive() && oldCart.getAsBoolean() ? CART_CHOICE : DEFAULT_CHOICE;
    }

    /** Follows the Texture Pack Manager setting and reloads textures once when it changed. */
    public static void sync(String newChoice)
    {
        boolean loaded = choice != null;
        boolean changed = find(choice()) != find(newChoice);
        choice = find(newChoice).name;
        Minecraft mc = Minecraft.getMinecraft();

        if (changed && loaded && mc.getTextureManager() != null)
        {
            mc.addScheduledTask(mc::refreshResources);
        }
    }

    /** A Texture Pack choice: a name and the packs it turns on. */
    private static final class Choice
    {
        final String name;
        final String description;
        final Pack[] packs;

        Choice(String name, String description, Pack... packs)
        {
            this.name = name;
            this.description = description;
            this.packs = packs;
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
