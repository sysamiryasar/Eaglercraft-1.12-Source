package net.sythix.client.cosmetic;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Capes (the animated Sythix capes from CapeArt, or your own PNGs) and custom skins as textures.
 */
public final class CosmeticTextures
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, ResourceLocation> CACHE = new HashMap<String, ResourceLocation>();
    private CosmeticTextures()
    {
    }

    /** {id, display name} of every cape the player can choose. */
    public static List<String[]> capes()
    {
        List<String[]> list = new ArrayList<String[]>();
        list.add(new String[] {"none", "No Cape"});

        for (CosmeticCatalog.Item cape : CosmeticCatalog.in(CosmeticCatalog.CAPES))
        {
            list.add(new String[] {cape.capeId(), cape.name});
        }

        File[] files = ProfileStore.capesDir().listFiles((dir, n) -> n.toLowerCase().endsWith(".png"));

        if (files != null)
        {
            java.util.Arrays.sort(files);

            for (File f : files)
            {
                list.add(new String[] {"custom:" + f.getName(), f.getName().substring(0, f.getName().length() - 4) + " (custom)"});
            }
        }

        return list;
    }

    /** Skin file names in sythix/skins. */
    public static List<String> skins()
    {
        List<String> list = new ArrayList<String>();
        File[] files = ProfileStore.skinsDir().listFiles((dir, n) -> n.toLowerCase().endsWith(".png"));

        if (files != null)
        {
            java.util.Arrays.sort(files);

            for (File f : files)
            {
                list.add(f.getName());
            }
        }

        return list;
    }

    public static void clearCache()
    {
        for (ResourceLocation location : CACHE.values())
        {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(location);
        }

        CACHE.clear();
    }

    /** Texture for a cape id, or null for "none" or a missing file. */
    public static ResourceLocation cape(String id)
    {
        if (id == null || id.equals("none"))
        {
            return null;
        }

        if (CapeArt.isArt(id))
        {
            return CapeArt.texture(id);
        }

        if (CACHE.containsKey("cape:" + id))
        {
            return CACHE.get("cape:" + id);
        }

        BufferedImage image = null;

        if (id.startsWith("custom:"))
        {
            image = read(new File(ProfileStore.capesDir(), id.substring(7)));

            if (image != null && image.getWidth() * 2 != image.getHeight() * 4 && image.getWidth() != image.getHeight() * 2)
            {
                LOGGER.warn("[Sythix] Cape {} should be 64x32 (or a 2:1 multiple)", id);
            }
        }

        ResourceLocation location = image == null ? null : Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("sythix_cape", new DynamicTexture(image));
        CACHE.put("cape:" + id, location);
        return location;
    }

    /** Texture for a skin file in sythix/skins, or null. Old 64x32 skins are converted. */
    public static ResourceLocation skin(String file)
    {
        if (file == null || file.isEmpty())
        {
            return null;
        }

        if (CACHE.containsKey("skin:" + file))
        {
            return CACHE.get("skin:" + file);
        }

        BufferedImage image = read(new File(ProfileStore.skinsDir(), file));
        ResourceLocation location = null;

        if (image != null && image.getWidth() == 64 && (image.getHeight() == 64 || image.getHeight() == 32))
        {
            BufferedImage converted = new ImageBufferDownload().parseUserSkin(image);
            location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("sythix_skin", new DynamicTexture(converted));
        }
        else if (image != null)
        {
            LOGGER.warn("[Sythix] Skin {} must be 64x64 or 64x32, it is {}x{}", file, image.getWidth(), image.getHeight());
        }

        CACHE.put("skin:" + file, location);
        return location;
    }

    private static BufferedImage read(File file)
    {
        try
        {
            return file.isFile() ? ImageIO.read(file) : null;
        }
        catch (Exception e)
        {
            LOGGER.warn("[Sythix] Could not read {}", file, e);
            return null;
        }
    }
}
