package net.sythix.modern;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

/**
 * Items from Minecraft 1.13 - 1.21.11 (IDs 3000 and up). Only add new items at the END.
 */
public final class ModernItems
{
    public static final int FIRST_ID = 3000;
    private static final Map<String, Item> BY_NAME = new LinkedHashMap<String, Item>();
    private static int nextId = FIRST_ID;

    private ModernItems()
    {
    }

    public static Item get(String name)
    {
        return BY_NAME.get(name);
    }

    public static Map<String, Item> all()
    {
        return Collections.unmodifiableMap(BY_NAME);
    }

    static <T extends Item> T add(String name, T item, CreativeTabs tab)
    {
        item.setUnlocalizedName("m." + name);
        item.setCreativeTab(tab);
        Item.REGISTRY.register(nextId++, new ResourceLocation(name), item);
        BY_NAME.put(name, item);
        return item;
    }

    private static Item plain(String name, CreativeTabs tab)
    {
        return add(name, new Item(), tab);
    }

    static void register()
    {
        CreativeTabs mat = CreativeTabs.MATERIALS;

        // ---- Materials ----
        plain("raw_iron", mat);
        plain("raw_gold", mat);
        plain("raw_copper", mat);
        plain("copper_ingot", mat);
        plain("amethyst_shard", mat);
        plain("netherite_scrap", mat);
        plain("netherite_ingot", mat);
        plain("echo_shard", mat);
        plain("disc_fragment_5", mat);
        plain("phantom_membrane", CreativeTabs.BREWING);
        plain("nautilus_shell", mat);
        plain("heart_of_the_sea", mat);
        plain("pottery_shard_archer", mat);
        plain("pottery_shard_prize", mat);
        plain("pottery_shard_arms_up", mat);
        plain("pottery_shard_skull", mat);
    }

    static void registerModels(RenderItem renderItem)
    {
        for (Map.Entry<String, Item> e : BY_NAME.entrySet())
        {
            renderItem.getItemModelMesher().register(e.getValue(), 0, new ModelResourceLocation(e.getKey(), "inventory"));
        }
    }
}
