package net.sythix.client.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Converts a resource pack made for Minecraft 1.13 to 1.21 into one that 1.12 can load.
 *
 * Modern versions renamed most block and item textures (for example "oak_planks" was
 * "planks_oak") and moved them from textures/block and textures/item to textures/blocks and
 * textures/items. This converter renames every texture that also exists in 1.12, so blocks, items,
 * mobs and the interface get the pack's modern look. Textures for things that do not exist in
 * 1.12, and textures whose layout changed (their image shape differs from the 1.12 one), are left
 * out so nothing shows up broken.
 */
public final class ModernPackConverter
{
    /** Result of a conversion. */
    public static final class Result
    {
        public File output;
        public int converted;
        public int skippedNew;
        public int skippedLayout;
        public final List<String> notes = new ArrayList<String>();
    }

    private static final String[] COLORS = new String[] {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    private static final String[] WOODS = new String[] {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
    private static final Map<String, String> BLOCKS = new HashMap<String, String>();
    private static final Map<String, String> ITEMS = new HashMap<String, String>();
    private static final Map<String, String> OTHER = new HashMap<String, String>();

    /**
     * Sythix's own 1.21 blocks drawn with hand-made models: the real 1.21 textures for these use a
     * different layout, so the generated ones are kept.
     */
    private static final Set<String> CUSTOM_LAYOUT = new HashSet<String>(Arrays.asList("lantern", "soul_lantern", "chain", "campfire_log", "campfire_fire", "soul_campfire_fire",
        "stonecutter_saw", "stonecutter_top", "stonecutter_side", "composter_top", "composter_side", "composter_bottom", "composter_compost", "composter_ready", "sculk_sensor_tendril",
        "sculk_sensor_top", "sculk_sensor_side", "scaffolding_top", "scaffolding_side", "bamboo_stalk", "sea_pickle", "conduit", "grindstone_side", "grindstone_pivot",
        "trial_spawner_side", "trial_spawner_top", "ominous_trial_spawner_side", "ominous_trial_spawner_top", "vault_side", "vault_top", "pink_petals", "kelp", "honey_block_side", "honey_block_top"));

    /** Paths whose image layout changed after 1.12 although the file name stayed the same. */
    private static final String[] CHANGED_LAYOUT = new String[] {"textures/entity/chest/", "textures/entity/villager/", "textures/entity/zombie_villager/", "textures/entity/bed/", "textures/entity/cat/", "textures/entity/illager/", "textures/entity/horse/", "textures/entity/llama/", "textures/entity/cow/", "textures/entity/pig/", "textures/entity/chicken", "textures/entity/sheep/", "textures/entity/wolf/", "textures/gui/container/creative_inventory/", "textures/gui/widgets.png", "textures/gui/icons.png", "textures/gui/container/inventory.png", "textures/gui/advancements/"};

    static
    {
        // ---- Blocks: families -----------------------------------------------------------------
        for (String c : COLORS)
        {
            String old = c.equals("light_gray") ? "silver" : c;
            BLOCKS.put(c + "_wool", "wool_colored_" + old);
            BLOCKS.put(c + "_stained_glass", "glass_" + old);
            BLOCKS.put(c + "_stained_glass_pane_top", "glass_pane_top_" + old);
            BLOCKS.put(c + "_terracotta", "hardened_clay_stained_" + old);
            BLOCKS.put(c + "_glazed_terracotta", "glazed_terracotta_" + old);
            BLOCKS.put(c + "_concrete", "concrete_" + old);
            BLOCKS.put(c + "_concrete_powder", "concrete_powder_" + old);
            BLOCKS.put(c + "_shulker_box", "shulker_top_" + old);
            ITEMS.put(c + "_dye", "dye_powder_" + old);
        }

        for (String w : WOODS)
        {
            String old = w.equals("dark_oak") ? "big_oak" : w;
            BLOCKS.put(w + "_planks", "planks_" + old);
            BLOCKS.put(w + "_log", "log_" + old);
            BLOCKS.put(w + "_log_top", "log_" + old + "_top");
            BLOCKS.put(w + "_leaves", "leaves_" + old);
            BLOCKS.put(w + "_sapling", "sapling_" + (w.equals("dark_oak") ? "roofed_oak" : w));
            String door = w.equals("oak") ? "wood" : w;
            BLOCKS.put(w + "_door_top", "door_" + door + "_upper");
            BLOCKS.put(w + "_door_bottom", "door_" + door + "_lower");
            ITEMS.put(w + "_door", "door_" + door);
        }

        String[][] blocks = new String[][] {
            {"grass_block_top", "grass_top"}, {"grass_block_side", "grass_side"}, {"grass_block_side_overlay", "grass_side_overlay"}, {"grass_block_snow", "grass_side_snowed"},
            {"podzol_side", "dirt_podzol_side"}, {"podzol_top", "dirt_podzol_top"},
            {"stone_bricks", "stonebrick"}, {"mossy_stone_bricks", "stonebrick_mossy"}, {"cracked_stone_bricks", "stonebrick_cracked"}, {"chiseled_stone_bricks", "stonebrick_carved"},
            {"mossy_cobblestone", "cobblestone_mossy"}, {"andesite", "stone_andesite"}, {"polished_andesite", "stone_andesite_smooth"}, {"diorite", "stone_diorite"},
            {"polished_diorite", "stone_diorite_smooth"}, {"granite", "stone_granite"}, {"polished_granite", "stone_granite_smooth"}, {"terracotta", "hardened_clay"},
            {"packed_ice", "ice_packed"}, {"spawner", "mob_spawner"}, {"note_block", "noteblock"}, {"sugar_cane", "reeds"}, {"lily_pad", "waterlily"}, {"cobweb", "web"},
            {"grass", "tallgrass"}, {"short_grass", "tallgrass"}, {"dead_bush", "deadbush"}, {"dandelion", "flower_dandelion"}, {"poppy", "flower_rose"},
            {"blue_orchid", "flower_blue_orchid"}, {"allium", "flower_allium"}, {"azure_bluet", "flower_houstonia"}, {"red_tulip", "flower_tulip_red"},
            {"orange_tulip", "flower_tulip_orange"}, {"white_tulip", "flower_tulip_white"}, {"pink_tulip", "flower_tulip_pink"}, {"oxeye_daisy", "flower_oxeye_daisy"},
            {"brown_mushroom", "mushroom_brown"}, {"red_mushroom", "mushroom_red"}, {"brown_mushroom_block", "mushroom_block_skin_brown"}, {"red_mushroom_block", "mushroom_block_skin_red"},
            {"mushroom_stem", "mushroom_block_skin_stem"}, {"sunflower_front", "double_plant_sunflower_front"}, {"sunflower_back", "double_plant_sunflower_back"},
            {"sunflower_top", "double_plant_sunflower_top"}, {"sunflower_bottom", "double_plant_sunflower_bottom"}, {"lilac_top", "double_plant_syringa_top"},
            {"lilac_bottom", "double_plant_syringa_bottom"}, {"peony_top", "double_plant_paeonia_top"}, {"peony_bottom", "double_plant_paeonia_bottom"},
            {"rose_bush_top", "double_plant_rose_top"}, {"rose_bush_bottom", "double_plant_rose_bottom"}, {"tall_grass_top", "double_plant_grass_top"},
            {"tall_grass_bottom", "double_plant_grass_bottom"}, {"large_fern_top", "double_plant_fern_top"}, {"large_fern_bottom", "double_plant_fern_bottom"},
            {"iron_door_top", "door_iron_upper"}, {"iron_door_bottom", "door_iron_lower"}, {"oak_trapdoor", "trapdoor"},
            {"nether_bricks", "nether_brick"}, {"red_nether_bricks", "red_nether_brick"}, {"bricks", "brick"}, {"end_stone_bricks", "end_bricks"},
            {"prismarine", "prismarine_rough"}, {"dark_prismarine", "prismarine_dark"}, {"sandstone", "sandstone_normal"}, {"cut_sandstone", "sandstone_smooth"},
            {"chiseled_sandstone", "sandstone_carved"}, {"red_sandstone", "red_sandstone_normal"}, {"cut_red_sandstone", "red_sandstone_smooth"},
            {"chiseled_red_sandstone", "red_sandstone_carved"}, {"smooth_stone", "stone_slab_top"}, {"smooth_stone_slab_side", "stone_slab_side"},
            {"chiseled_quartz_block", "quartz_block_chiseled"}, {"chiseled_quartz_block_top", "quartz_block_chiseled_top"}, {"quartz_pillar", "quartz_block_lines"},
            {"quartz_pillar_top", "quartz_block_lines_top"}, {"carved_pumpkin", "pumpkin_face_off"}, {"jack_o_lantern", "pumpkin_face_on"},
            {"attached_melon_stem", "melon_stem_connected"}, {"melon_stem", "melon_stem_disconnected"}, {"attached_pumpkin_stem", "pumpkin_stem_connected"},
            {"pumpkin_stem", "pumpkin_stem_disconnected"}, {"magma_block", "magma"}, {"slime_block", "slime"}, {"nether_portal", "portal"}, {"torch", "torch_on"},
            {"redstone_torch", "redstone_torch_on"}, {"redstone_lamp", "redstone_lamp_off"}, {"comparator", "comparator_off"}, {"repeater", "repeater_off"},
            {"rail", "rail_normal"}, {"rail_corner", "rail_normal_turned"}, {"powered_rail", "rail_golden"}, {"powered_rail_on", "rail_golden_powered"},
            {"detector_rail", "rail_detector"}, {"detector_rail_on", "rail_detector_powered"}, {"activator_rail", "rail_activator"}, {"activator_rail_on", "rail_activator_powered"},
            {"furnace_front", "furnace_front_off"}, {"dispenser_front", "dispenser_front_horizontal"}, {"dropper_front", "dropper_front_horizontal"},
            {"piston_top", "piston_top_normal"}, {"end_portal_frame_top", "endframe_top"}, {"end_portal_frame_side", "endframe_side"}, {"end_portal_frame_eye", "endframe_eye"},
            {"farmland", "farmland_dry"}, {"farmland_moist", "farmland_wet"}, {"dirt_path_top", "grass_path_top"}, {"dirt_path_side", "grass_path_side"},
            {"wet_sponge", "sponge_wet"}, {"tripwire", "trip_wire"}, {"tripwire_hook", "trip_wire_source"}, {"observer_back_on", "observer_back_lit"},
            {"fire_0", "fire_layer_0"}, {"fire_1", "fire_layer_1"}, {"anvil", "anvil_base"}, {"anvil_top", "anvil_top_damaged_0"},
            {"chipped_anvil_top", "anvil_top_damaged_1"}, {"damaged_anvil_top", "anvil_top_damaged_2"}, {"item_frame", "itemframe_background"},
            {"sticky_piston_top", "piston_top_sticky"}, {"daylight_detector_inverted_top", "daylight_detector_inverted_top"}
        };

        for (String[] b : blocks)
        {
            BLOCKS.put(b[0], b[1]);
        }

        String[][] items = new String[][] {
            {"golden_apple", "apple_golden"}, {"enchanted_golden_apple", "apple_golden"}, {"cooked_beef", "beef_cooked"}, {"beef", "beef_raw"},
            {"enchanted_book", "book_enchanted"}, {"book", "book_normal"}, {"writable_book", "book_writable"}, {"written_book", "book_written"},
            {"bow", "bow_standby"}, {"bucket", "bucket_empty"}, {"lava_bucket", "bucket_lava"}, {"milk_bucket", "bucket_milk"}, {"water_bucket", "bucket_water"},
            {"golden_carrot", "carrot_golden"}, {"cooked_chicken", "chicken_cooked"}, {"chicken", "chicken_raw"}, {"popped_chorus_fruit", "chorus_fruit_popped"},
            {"ink_sac", "dye_powder_black"}, {"cocoa_beans", "dye_powder_brown"}, {"lapis_lazuli", "dye_powder_blue"}, {"bone_meal", "dye_powder_white"},
            {"rose_red", "dye_powder_red"}, {"dandelion_yellow", "dye_powder_yellow"}, {"cactus_green", "dye_powder_green"},
            {"iron_door", "door_iron"}, {"fire_charge", "fireball"}, {"firework_rocket", "fireworks"}, {"firework_star", "fireworks_charge"},
            {"firework_star_overlay", "fireworks_charge_overlay"}, {"tropical_fish", "fish_clownfish_raw"}, {"cooked_cod", "fish_cod_cooked"}, {"cod", "fish_cod_raw"},
            {"pufferfish", "fish_pufferfish_raw"}, {"cooked_salmon", "fish_salmon_cooked"}, {"salmon", "fish_salmon_raw"}, {"fishing_rod", "fishing_rod_uncast"},
            {"glistering_melon_slice", "melon_speckled"}, {"melon_slice", "melon"}, {"map", "map_empty"}, {"filled_map", "map_filled"},
            {"filled_map_markings", "map_filled_markings"}, {"chest_minecart", "minecart_chest"}, {"command_block_minecart", "minecart_command_block"},
            {"furnace_minecart", "minecart_furnace"}, {"hopper_minecart", "minecart_hopper"}, {"minecart", "minecart_normal"}, {"tnt_minecart", "minecart_tnt"},
            {"cooked_mutton", "mutton_cooked"}, {"mutton", "mutton_raw"}, {"nether_brick", "netherbrick"}, {"porkchop", "porkchop_raw"},
            {"cooked_porkchop", "porkchop_cooked"}, {"baked_potato", "potato_baked"}, {"poisonous_potato", "potato_poisonous"}, {"potion", "potion_bottle_drinkable"},
            {"glass_bottle", "potion_bottle_empty"}, {"lingering_potion", "potion_bottle_lingering"}, {"splash_potion", "potion_bottle_splash"},
            {"cooked_rabbit", "rabbit_cooked"}, {"rabbit", "rabbit_raw"}, {"redstone", "redstone_dust"}, {"sugar_cane", "reeds"}, {"melon_seeds", "seeds_melon"},
            {"pumpkin_seeds", "seeds_pumpkin"}, {"wheat_seeds", "seeds_wheat"}, {"oak_sign", "sign"}, {"slime_ball", "slimeball"},
            {"fermented_spider_eye", "spider_eye_fermented"}, {"totem_of_undying", "totem"}, {"armor_stand", "wooden_armorstand"}, {"leather_horse_armor", "leather_horse_armor"},
            {"golden_horse_armor", "gold_horse_armor"}, {"experience_bottle", "experience_bottle"}, {"oak_boat", "oak_boat"}, {"dark_oak_boat", "dark_oak_boat"},
            {"music_disc_11", "record_11"}, {"music_disc_13", "record_13"}, {"music_disc_blocks", "record_blocks"}, {"music_disc_cat", "record_cat"},
            {"music_disc_chirp", "record_chirp"}, {"music_disc_far", "record_far"}, {"music_disc_mall", "record_mall"}, {"music_disc_mellohi", "record_mellohi"},
            {"music_disc_stal", "record_stal"}, {"music_disc_strad", "record_strad"}, {"music_disc_wait", "record_wait"}, {"music_disc_ward", "record_ward"}
        };

        for (String[] i : items)
        {
            ITEMS.put(i[0], i[1]);
        }

        for (String tool : new String[] {"axe", "hoe", "pickaxe", "shovel", "sword"})
        {
            ITEMS.put("golden_" + tool, "gold_" + tool);
            ITEMS.put("wooden_" + tool, "wood_" + tool);
        }

        for (String armor : new String[] {"boots", "chestplate", "helmet", "leggings"})
        {
            ITEMS.put("golden_" + armor, "gold_" + armor);
        }

        // ---- Other files that moved -------------------------------------------------------------
        OTHER.put("textures/misc/enchanted_glint_item.png", "textures/misc/enchanted_item_glint.png");
        OTHER.put("textures/misc/enchanted_glint_entity.png", "textures/misc/enchanted_item_glint.png");
        OTHER.put("textures/entity/experience_orb.png", "textures/entity/experience_orb.png");
        OTHER.put("textures/entity/player/wide/steve.png", "textures/entity/steve.png");
        OTHER.put("textures/entity/player/slim/alex.png", "textures/entity/alex.png");
        OTHER.put("textures/entity/end_crystal/end_crystal.png", "textures/entity/endercrystal/endercrystal.png");
        OTHER.put("textures/entity/end_crystal/end_crystal_beam.png", "textures/entity/endercrystal/endercrystal_beam.png");
        OTHER.put("textures/entity/zombified_piglin.png", "textures/entity/zombie_pigman.png");
        OTHER.put("textures/entity/piglin/zombified_piglin.png", "textures/entity/zombie_pigman.png");
    }

    private ModernPackConverter()
    {
    }

    /** Pack format from pack.mcmeta, or -1 if unknown. */
    public static int packFormat(File pack)
    {
        try
        {
            String meta = read(pack, "pack.mcmeta");
            return meta == null ? -1 : new JsonParser().parse(meta).getAsJsonObject().getAsJsonObject("pack").get("pack_format").getAsInt();
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    /** True if the pack looks like it was made for 1.13 or newer. */
    public static boolean isModern(File pack)
    {
        int format = packFormat(pack);
        return format > 3 || (format < 0 && hasEntry(pack, "assets/minecraft/textures/block/"));
    }

    private static String read(File pack, String path) throws Exception
    {
        if (pack.isDirectory())
        {
            File f = new File(pack, path);
            return f.isFile() ? FileUtils.readFileToString(f, StandardCharsets.UTF_8) : null;
        }

        try (ZipFile zip = new ZipFile(pack))
        {
            ZipEntry e = zip.getEntry(path);
            return e == null ? null : IOUtils.toString(zip.getInputStream(e), StandardCharsets.UTF_8);
        }
    }

    private static boolean hasEntry(File pack, String prefix)
    {
        try
        {
            for (String name : entries(pack).keySet())
            {
                if (name.startsWith(prefix))
                {
                    return true;
                }
            }
        }
        catch (Exception ignored)
        {
        }

        return false;
    }

    /** Every file in the pack: path -> bytes. */
    private static Map<String, byte[]> entries(File pack) throws Exception
    {
        Map<String, byte[]> map = new LinkedHashMap<String, byte[]>();

        if (pack.isDirectory())
        {
            for (File f : FileUtils.listFiles(pack, null, true))
            {
                String rel = pack.toURI().relativize(f.toURI()).getPath();
                map.put(rel, FileUtils.readFileToByteArray(f));
            }
        }
        else
        {
            try (ZipFile zip = new ZipFile(pack))
            {
                Enumeration<? extends ZipEntry> e = zip.entries();

                while (e.hasMoreElements())
                {
                    ZipEntry entry = e.nextElement();

                    String name = entry.getName().replace('\\', '/');

                    // a Minecraft .jar also holds thousands of .class files: only resources matter
                    if (!entry.isDirectory() && (name.startsWith("assets/") || name.startsWith("pack.")))
                    {
                        map.put(name, IOUtils.toByteArray(zip.getInputStream(entry)));
                    }
                }
            }
        }

        return map;
    }

    // ---- The 1.12 texture list -------------------------------------------------------------------

    private static Set<String> legacy;

    /** Every texture path in the game's own 1.12 assets ("textures/blocks/stone.png"...). */
    private static synchronized Set<String> legacyTextures()
    {
        if (legacy != null)
        {
            return legacy;
        }

        legacy = new HashSet<String>();

        try
        {
            URL location = ModernPackConverter.class.getProtectionDomain().getCodeSource().getLocation();
            File source = new File(location.toURI());

            if (source.isDirectory())
            {
                File textures = new File(source, "assets/minecraft/textures");

                if (textures.isDirectory())
                {
                    for (File f : FileUtils.listFiles(textures, null, true))
                    {
                        legacy.add("textures/" + textures.toURI().relativize(f.toURI()).getPath());
                    }
                }
            }
            else
            {
                try (ZipInputStream in = new ZipInputStream(new java.io.FileInputStream(source)))
                {
                    ZipEntry e;

                    while ((e = in.getNextEntry()) != null)
                    {
                        if (e.getName().startsWith("assets/minecraft/textures/") && !e.isDirectory())
                        {
                            legacy.add(e.getName().substring("assets/minecraft/".length()));
                        }
                    }
                }
            }
        }
        catch (Exception ignored)
        {
            // Falls back to checking single files with exists()
        }

        return legacy;
    }

    private static boolean exists(String texturePath)
    {
        Set<String> all = legacyTextures();
        return all.isEmpty() ? ModernPackConverter.class.getResource("/assets/minecraft/" + texturePath) != null : all.contains(texturePath);
    }

    private static String tokens(String name)
    {
        String[] parts = name.replace("silver", "light_gray").replace("big_oak", "dark_oak").replace("roofed_oak", "dark_oak").split("_");
        Arrays.sort(parts);
        return String.join("_", parts);
    }

    /** 1.12 name for a modern block/item texture name in dir ("blocks" or "items"), or null. */
    static String legacyName(String dir, String name)
    {
        // Animation frames: "wheat_stage3" -> "wheat_stage_3"
        String fixed = name.replaceAll("_stage(\\d+)$", "_stage_$1");
        Map<String, String> table = dir.equals("blocks") ? BLOCKS : ITEMS;

        if (table.containsKey(name) && exists("textures/" + dir + "/" + table.get(name) + ".png"))
        {
            return table.get(name);
        }

        if (exists("textures/" + dir + "/" + fixed + ".png"))
        {
            return fixed;
        }

        // Same words in a different order ("oak_planks" / "planks_oak")
        String key = tokens(fixed);

        for (String path : legacyTextures())
        {
            if (path.startsWith("textures/" + dir + "/") && path.endsWith(".png"))
            {
                String legacyName = path.substring(dir.length() + 10, path.length() - 4);

                if (legacyName.indexOf('/') < 0 && tokens(legacyName).equals(key))
                {
                    return legacyName;
                }
            }
        }

        return null;
    }

    /** 1.12 path ("textures/...") for a modern asset path below assets/minecraft/, or null. */
    static String legacyPath(String path)
    {
        boolean meta = path.endsWith(".png.mcmeta");
        String png = meta ? path.substring(0, path.length() - 7) : path;
        String result = null;

        if (OTHER.containsKey(png))
        {
            result = OTHER.get(png);
        }
        else if (png.startsWith("textures/entity/equipment/humanoid/") || png.startsWith("textures/entity/equipment/humanoid_leggings/"))
        {
            // 1.21.2+ armour: equipment/humanoid/NAME.png = layer 1, humanoid_leggings = layer 2
            boolean legs = png.startsWith("textures/entity/equipment/humanoid_leggings/");
            String material = png.substring(png.lastIndexOf('/') + 1, png.length() - 4);
            material = material.equals("chainmail") ? "chainmail" : material;
            String target = "textures/models/armor/" + material + "_layer_" + (legs ? 2 : 1) + ".png";
            result = exists(target) ? target : null;
        }
        else if (png.startsWith("textures/entity/boat/") && png.indexOf('/', 21) < 0 && !png.contains("boat_"))
        {
            String wood = png.substring(21, png.length() - 4);
            String target = "textures/entity/boat/boat_" + (wood.equals("dark_oak") ? "darkoak" : wood) + ".png";
            result = exists(target) ? target : null;
        }
        else if (png.startsWith("textures/block/") || png.startsWith("textures/item/"))
        {
            boolean block = png.startsWith("textures/block/");
            String dir = block ? "blocks" : "items";
            String name = png.substring(block ? 15 : 14, png.length() - 4);

            if (name.indexOf('/') < 0)
            {
                String old = legacyName(dir, name);
                result = old == null ? null : "textures/" + dir + "/" + old + ".png";
            }
        }
        else if (png.startsWith("textures/") && exists(png))
        {
            result = png;
        }

        return result == null ? null : (meta ? result + ".mcmeta" : result);
    }

    private static boolean changedLayout(String legacyPath)
    {
        for (String prefix : CHANGED_LAYOUT)
        {
            if (legacyPath.startsWith(prefix))
            {
                return true;
            }
        }

        return false;
    }

    /** Same width:height ratio as the 1.12 texture (animation strips may be taller). */
    private static boolean sameShape(String legacyPath, byte[] png)
    {
        try (InputStream in = ModernPackConverter.class.getResourceAsStream("/assets/minecraft/" + legacyPath))
        {
            if (in == null)
            {
                return true;
            }

            BufferedImage a = ImageIO.read(in);
            BufferedImage b = ImageIO.read(new ByteArrayInputStream(png));

            if (a == null || b == null)
            {
                return false;
            }

            boolean blockOrItem = legacyPath.startsWith("textures/blocks/") || legacyPath.startsWith("textures/items/");

            if (blockOrItem)
            {
                // Square textures, or vertical animation strips of square frames
                return b.getHeight() % b.getWidth() == 0;
            }

            return a.getWidth() * b.getHeight() == a.getHeight() * b.getWidth();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /** Converts pack into resourcepacks/"name (1.12).zip". */
    public static Result convert(File pack, File resourcePackDir) throws Exception
    {
        Result result = new Result();
        Map<String, byte[]> files = entries(pack);
        String base = pack.getName().replaceAll("\\.(zip|jar)$", "");

        if (pack.getName().endsWith(".jar"))
        {
            base = "Minecraft " + base + " textures";
        }
        result.output = new File(resourcePackDir, base + " (1.12).zip");
        Set<String> written = new HashSet<String>();

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(result.output)))
        {
            for (Map.Entry<String, byte[]> e : files.entrySet())
            {
                String name = e.getKey();

                if (!name.startsWith("assets/minecraft/textures/"))
                {
                    continue;
                }

                String modern = name.substring("assets/minecraft/".length());
                String target = legacyPath(modern);

                if (target == null)
                {
                    if (modern.endsWith(".png"))
                    {
                        ++result.skippedNew;
                    }

                    continue;
                }

                boolean png = target.endsWith(".png");
                String fileBase = target.substring(target.lastIndexOf('/') + 1).replace(".png.mcmeta", "").replace(".png", "");

                if (CUSTOM_LAYOUT.contains(fileBase) && target.startsWith("textures/blocks/"))
                {
                    continue;
                }

                if (changedLayout(target) || png && !sameShape(target, e.getValue()))
                {
                    if (png)
                    {
                        ++result.skippedLayout;
                    }

                    continue;
                }

                if (written.add(target))
                {
                    out.putNextEntry(new ZipEntry("assets/minecraft/" + target));
                    out.write(e.getValue());
                    out.closeEntry();

                    if (png)
                    {
                        ++result.converted;
                    }
                }
            }

            // Fonts, sounds and languages keep their paths
            for (Map.Entry<String, byte[]> e : files.entrySet())
            {
                String name = e.getKey();

                if ((name.startsWith("assets/minecraft/sounds/") && name.endsWith(".ogg")) && written.add(name))
                {
                    out.putNextEntry(new ZipEntry(name));
                    out.write(e.getValue());
                    out.closeEntry();
                }
            }

            if (files.containsKey("pack.png"))
            {
                out.putNextEntry(new ZipEntry("pack.png"));
                out.write(files.get("pack.png"));
                out.closeEntry();
            }

            JsonObject root = new JsonObject();
            JsonObject packInfo = new JsonObject();
            packInfo.addProperty("pack_format", Integer.valueOf(3));
            packInfo.addProperty("description", base + " - converted for 1.12 by Sythix");
            root.add("pack", packInfo);
            out.putNextEntry(new ZipEntry("pack.mcmeta"));
            out.write(root.toString().getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        if (result.converted == 0)
        {
            result.output.delete();
            result.notes.add("No textures could be converted - is this a Minecraft 1.13+ resource pack?");
        }

        result.notes.add(result.converted + " textures converted, " + result.skippedNew + " left out (blocks/items/mobs that 1.12 does not have), " + result.skippedLayout + " left out (layout changed since 1.12).");
        return result;
    }
}
