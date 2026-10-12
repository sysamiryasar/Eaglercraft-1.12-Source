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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;

/**
 * Texture packs that ship inside the client (src/main/resources/sythix/texturepacks, built with
 * branding/texture_packs.py):
 *
 * - Cowarted Vanilla is Sythix's default look and is always on. It sits just above Minecraft's own
 *   textures, so any resource pack you pick still goes on top of it.
 * - Harlu Cart Pack is for cart PvP (clear rails, low fire, small particles) and is switched on in
 *   Texture Pack Manager (Cart Pack) or by the Cart PvP layout in Smart HUD Layout.
 */
public final class BuiltInPacks
{
    public static final String CART_SETTING = "Cart Pack";
    private static final Pack DEFAULT = new Pack("cowarted", "Cowarted Vanilla");
    private static final Pack CART = new Pack("harlu", "Harlu Cart Pack");

    /** Whether the cart pack is on; null until read from the settings. */
    private static Boolean cartPack;

    private BuiltInPacks()
    {
    }

    /** Adds the active built-in packs right after Minecraft's own (called on every resource reload). */
    public static void addTo(List<IResourcePack> packs)
    {
        int at = Math.min(1, packs.size());
        packs.add(at, DEFAULT);

        if (cartPackOn())
        {
            packs.add(at + 1, CART);
        }
    }

    public static boolean cartPackOn()
    {
        if (cartPack == null)
        {
            cartPack = Boolean.valueOf(savedCartSetting());
        }

        return cartPack.booleanValue();
    }

    /** Switches the cart pack on or off and reloads textures if that changed it. */
    public static void setCartPack(boolean on)
    {
        if (cartPack != null && cartPack.booleanValue() == on)
        {
            return;
        }

        boolean reload = cartPack != null;
        cartPack = Boolean.valueOf(on);
        Minecraft mc = Minecraft.getMinecraft();

        if (reload && mc.getTextureManager() != null)
        {
            mc.addScheduledTask(mc::refreshResources);
        }
    }

    /** The first resource load happens before Sythix's settings are loaded: read the flag from config.json. */
    private static boolean savedCartSetting()
    {
        try
        {
            File config = new File(new File(Minecraft.getMinecraft().mcDataDir, "sythix"), "config.json");

            if (config.isFile())
            {
                JsonObject root = new JsonParser().parse(FileUtils.readFileToString(config, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonElement value = root.getAsJsonObject("modules").getAsJsonObject("Texture Pack Manager").getAsJsonObject("settings").get(CART_SETTING);
                return value != null && value.getAsBoolean();
            }
        }
        catch (Exception ignored)
        {
            // no saved setting yet
        }

        return false;
    }

    /** A pack read from the client jar, with a file index so lookups never search the jar. */
    private static final class Pack implements IResourcePack
    {
        private final String root;
        private final String name;
        private Set<String> files;

        Pack(String folder, String name)
        {
            this.root = "/sythix/texturepacks/" + folder + "/";
            this.name = name;
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
            InputStream in = this.resourceExists(location) ? BuiltInPacks.class.getResourceAsStream(this.root + path(location)) : null;

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
