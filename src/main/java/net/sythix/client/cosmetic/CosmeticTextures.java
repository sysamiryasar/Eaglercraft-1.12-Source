package net.sythix.client.cosmetic;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
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
 * Built-in and custom capes, and custom skins, turned into textures.
 */
public final class CosmeticTextures
{
    private static final Logger LOGGER = LogManager.getLogger();
    /** Built-in capes: id, display name, top colour, bottom colour, emblem. */
    private static final Object[][] BUILT_IN = new Object[][] {
        {"sythix", "Sythix", Integer.valueOf(0x22222A), Integer.valueOf(0x0B0B0E), Boolean.TRUE},
        {"crimson", "Crimson", Integer.valueOf(0xD81820), Integer.valueOf(0x5A0408), Boolean.TRUE},
        {"ocean", "Ocean", Integer.valueOf(0x6CC6FF), Integer.valueOf(0x1B3FA0), Boolean.FALSE},
        {"midnight", "Midnight", Integer.valueOf(0x101018), Integer.valueOf(0x000000), Boolean.FALSE},
        {"split", "Red & Blue", Integer.valueOf(0xD81820), Integer.valueOf(0x2F6BFF), Boolean.TRUE}
    };
    private static final Map<String, ResourceLocation> CACHE = new HashMap<String, ResourceLocation>();

    private CosmeticTextures()
    {
    }

    /** {id, display name} of every cape the player can choose. */
    public static List<String[]> capes()
    {
        List<String[]> list = new ArrayList<String[]>();
        list.add(new String[] {"none", "No Cape"});

        for (Object[] cape : BUILT_IN)
        {
            list.add(new String[] {(String)cape[0], (String)cape[1]});
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
        else
        {
            for (Object[] cape : BUILT_IN)
            {
                if (cape[0].equals(id))
                {
                    image = drawCape(((Integer)cape[2]).intValue(), ((Integer)cape[3]).intValue(), ((Boolean)cape[4]).booleanValue());
                }
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

    /** Draws a 64x32 cape: vertical gradient, accent edges and (optionally) the Sythix emblem. */
    private static BufferedImage drawCape(int top, int bottom, boolean emblem)
    {
        BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        BufferedImage icon = null;

        if (emblem)
        {
            try (InputStream in = CosmeticTextures.class.getResourceAsStream("/assets/minecraft/textures/gui/sythix/icon_16.png"))
            {
                icon = in == null ? null : ImageIO.read(in);
            }
            catch (Exception ignored)
            {
                icon = null;
            }
        }

        // Front (x 1-10) and back (x 12-21), rows 1-16; edges (x 0, 11, 22; rows 0, 17)
        for (int y = 0; y < 18; ++y)
        {
            float t = Math.max(0.0F, Math.min(1.0F, (float)(y - 1) / 15.0F));
            int color = 0xFF000000 | lerp(top, bottom, t);

            for (int x = 0; x < 23; ++x)
            {
                boolean edge = y == 0 || y == 17 || x == 0 || x == 11 || x == 22;
                img.setRGB(x, y, edge ? 0xFF000000 | lerp(color, 0xD81820, 0.6F) : color);
            }
        }

        // Accent stripe near the bottom of both faces
        for (int x = 1; x < 22; ++x)
        {
            if (x != 11)
            {
                img.setRGB(x, 14, 0xFF000000 | lerp(0xD81820, 0x2F6BFF, (float)(x % 11) / 10.0F));
            }
        }

        if (icon != null)
        {
            // 8x8 emblem centred on both faces
            for (int face = 0; face < 2; ++face)
            {
                int ox = (face == 0 ? 1 : 12) + 1;
                int oy = 3;

                for (int y = 0; y < 8; ++y)
                {
                    for (int x = 0; x < 8; ++x)
                    {
                        int argb = icon.getRGB(x * icon.getWidth() / 8, y * icon.getHeight() / 8);

                        if (((argb >>> 24) & 255) > 128)
                        {
                            img.setRGB(ox + x, oy + y, argb | 0xFF000000);
                        }
                    }
                }
            }
        }

        return img;
    }

    private static int lerp(int a, int b, float t)
    {
        int r = (int)(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int)(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int)((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }
}
