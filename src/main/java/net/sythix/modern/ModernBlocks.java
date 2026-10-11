package net.sythix.modern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.SythixBlockAccess;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.SythixItemAccess;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.sythix.modern.block.*;

/**
 * Blocks from Minecraft 1.13 - 1.21.11 added to this 1.12 client (from the 1.21.11 reference
 * catalog). They use block IDs 500 and up, so the vanilla blocks keep their IDs and normal 1.12
 * servers still work. They exist in singleplayer and LAN worlds hosted by Sythix; a 1.12 server
 * never sends them.
 *
 * IDs are given in list order: only ever add new blocks at the END, or saved worlds would mix
 * blocks up.
 */
public final class ModernBlocks
{
    public static final int FIRST_ID = 500;
    private static final Map<String, Block> BY_NAME = new LinkedHashMap<String, Block>();
    private static final Set<Block> CLIMBABLE = new HashSet<Block>();
    private static int nextId = FIRST_ID;

    private ModernBlocks()
    {
    }

    public static Block get(String name)
    {
        return BY_NAME.get(name);
    }

    public static Map<String, Block> all()
    {
        return Collections.unmodifiableMap(BY_NAME);
    }

    /** Any item by registry name (vanilla or Sythix), or null. */
    public static Item item(String name)
    {
        return Item.REGISTRY.getObject(new ResourceLocation(name));
    }

    public static boolean isClimbable(Block block)
    {
        return CLIMBABLE.contains(block);
    }

    private static <T extends Block> T add(String name, T block, CreativeTabs tab, float hardness, float resistance, SoundType sound)
    {
        block.setUnlocalizedName("m." + name);
        block.setCreativeTab(tab);
        SythixBlockAccess.hardness(block, hardness);
        SythixBlockAccess.resistance(block, resistance);
        SythixBlockAccess.sound(block, sound);
        Block.REGISTRY.register(nextId++, new ResourceLocation(name), block);
        BY_NAME.put(name, block);
        return block;
    }

    private static MBlock cube(String name, Material m, CreativeTabs tab, float hardness, float resistance, SoundType sound)
    {
        return add(name, new MBlock(m), tab, hardness, resistance, sound);
    }

    private static MPillar pillar(String name, Material m, float hardness, SoundType sound)
    {
        return add(name, new MPillar(m, 16.0D), CreativeTabs.BUILDING_BLOCKS, hardness, hardness, sound);
    }

    private static MBlock light(MBlock block, int level)
    {
        SythixBlockAccess.light(block, level);
        return block;
    }

    private static MBlock ore(String name, boolean deep, String drop, int min, int max, int xpMin, int xpMax)
    {
        MBlock b = cube(name, Material.ROCK, CreativeTabs.BUILDING_BLOCKS, deep ? 4.5F : 3.0F, 3.0F, SoundType.STONE);
        b.drops(drop, min, max, true).xp(xpMin, xpMax);
        return b;
    }

    /** Called from Block.registerBlocks(). */
    public static void register()
    {
        CreativeTabs build = CreativeTabs.BUILDING_BLOCKS;
        CreativeTabs deco = CreativeTabs.DECORATIONS;
        CreativeTabs red = CreativeTabs.REDSTONE;

        // ---- Stone / geology ----
        pillar("deepslate", Material.ROCK, 3.0F, SoundType.STONE);
        cube("cobbled_deepslate", Material.ROCK, build, 3.5F, 6.0F, SoundType.STONE);
        cube("tuff", Material.ROCK, build, 1.5F, 6.0F, SoundType.STONE);
        cube("calcite", Material.ROCK, build, 0.75F, 0.75F, SoundType.STONE);
        cube("dripstone_block", Material.ROCK, build, 1.5F, 1.0F, SoundType.STONE);
        cube("smooth_basalt", Material.ROCK, build, 1.25F, 4.2F, SoundType.STONE);
        SythixBlockAccess.unbreakable(cube("reinforced_deepslate", Material.ROCK, build, 55.0F, 1200.0F, SoundType.STONE).noDrops());

        // ---- Soil ----
        cube("rooted_dirt", Material.GROUND, build, 0.5F, 0.5F, SoundType.GROUND);
        cube("mud", Material.GROUND, build, 0.5F, 0.5F, SoundType.SLIME).collision(new double[] {0, 0, 0, 16, 14, 16}).slows(0.9F);
        add("suspicious_sand", new MSuspicious(Material.SAND, "sand"), build, 0.25F, 0.25F, SoundType.SAND);
        add("suspicious_gravel", new MSuspicious(Material.GROUND, "gravel"), build, 0.25F, 0.25F, SoundType.GROUND);

        // ---- Masonry ----
        cube("mud_bricks", Material.ROCK, build, 1.5F, 3.0F, SoundType.STONE);
        cube("smooth_quartz", Material.ROCK, build, 2.0F, 6.0F, SoundType.STONE);

        // ---- Wood ----
        pillar("mangrove_log", Material.WOOD, 2.0F, SoundType.WOOD);
        pillar("cherry_log", Material.WOOD, 2.0F, SoundType.WOOD);
        pillar("pale_oak_log", Material.WOOD, 2.0F, SoundType.WOOD);
        pillar("stripped_oak_log", Material.WOOD, 2.0F, SoundType.WOOD);
        pillar("bamboo_block", Material.WOOD, 2.0F, SoundType.WOOD);
        cube("mangrove_planks", Material.WOOD, build, 2.0F, 3.0F, SoundType.WOOD);
        cube("cherry_planks", Material.WOOD, build, 2.0F, 3.0F, SoundType.WOOD);
        cube("pale_oak_planks", Material.WOOD, build, 2.0F, 3.0F, SoundType.WOOD);

        // ---- Leaves / moss ----
        add("mangrove_leaves", new MLeaves("mangrove_propagule"), deco, 0.2F, 0.2F, SoundType.PLANT);
        add("cherry_leaves", new MLeaves("cherry_sapling"), deco, 0.2F, 0.2F, SoundType.PLANT);
        add("pale_oak_leaves", new MLeaves("pale_oak_sapling"), deco, 0.2F, 0.2F, SoundType.PLANT);
        add("azalea_leaves", new MLeaves(null), deco, 0.2F, 0.2F, SoundType.PLANT);
        add("flowering_azalea_leaves", new MLeaves(null), deco, 0.2F, 0.2F, SoundType.PLANT);
        cube("moss_block", Material.GRASS, build, 0.1F, 0.1F, SoundType.PLANT);
        cube("moss_carpet", Material.CARPET, deco, 0.1F, 0.1F, SoundType.CLOTH).shape(0, 0, 0, 16, 1, 16);

        // ---- Flowers ----
        add("cornflower", new MPlant(11.0D, false), deco, 0.0F, 0.0F, SoundType.PLANT);
        add("lily_of_the_valley", new MPlant(10.0D, false), deco, 0.0F, 0.0F, SoundType.PLANT);
        add("torchflower", new MPlant(12.0D, false), deco, 0.0F, 0.0F, SoundType.PLANT);
        add("pitcher_plant", new MPlant(16.0D, false), deco, 0.0F, 0.0F, SoundType.PLANT);
        cube("pink_petals", Material.PLANTS, deco, 0.0F, 0.0F, SoundType.PLANT).shape(0, 0, 0, 16, 3, 16).collision(null).layer(BlockRenderLayer.CUTOUT);

        // ---- Cold ----
        cube("blue_ice", Material.PACKED_ICE, build, 2.8F, 2.8F, SoundType.GLASS).slippery(0.989F).noDrops();
        add("powder_snow", new MPowderSnow(), build, 0.25F, 0.25F, SoundType.SNOW);

        // ---- Ores ----
        ore("deepslate_coal_ore", true, "coal", 1, 1, 0, 2);
        ore("deepslate_iron_ore", true, "raw_iron", 1, 1, 0, 0);
        ore("copper_ore", false, "raw_copper", 2, 5, 0, 0);
        ore("deepslate_copper_ore", true, "raw_copper", 2, 5, 0, 0);
        ore("deepslate_gold_ore", true, "raw_gold", 1, 1, 0, 0);
        ore("deepslate_redstone_ore", true, "redstone", 4, 5, 1, 5);
        ore("deepslate_lapis_ore", true, "dye", 4, 9, 2, 5);
        ore("deepslate_diamond_ore", true, "diamond", 1, 1, 3, 7);
        ore("deepslate_emerald_ore", true, "emerald", 1, 1, 3, 7);
        ore("nether_gold_ore", false, "gold_nugget", 2, 6, 0, 1);
        cube("ancient_debris", Material.ROCK, build, 30.0F, 1200.0F, SoundType.METAL);

        // ---- Utility / workstations ----
        add("blast_furnace", new MFurnace(false), deco, 3.5F, 3.5F, SoundType.STONE);
        add("smoker", new MFurnace(true), deco, 3.5F, 3.5F, SoundType.STONE);
        add("stonecutter", new MFacing(Material.ROCK), deco, 3.5F, 3.5F, SoundType.STONE).shape(0, 0, 0, 16, 9, 16).layer(BlockRenderLayer.CUTOUT);
        cube("smithing_table", Material.WOOD, deco, 2.5F, 2.5F, SoundType.WOOD);
        add("grindstone", new MFacing(Material.ROCK), deco, 2.0F, 6.0F, SoundType.STONE).shape(2, 0, 2, 14, 16, 14);
        add("composter", new MComposter(), deco, 0.6F, 0.6F, SoundType.WOOD);
        add("loom", new MFacing(Material.WOOD), deco, 2.5F, 2.5F, SoundType.WOOD);
        cube("cartography_table", Material.WOOD, deco, 2.5F, 2.5F, SoundType.WOOD);
        cube("fletching_table", Material.WOOD, deco, 2.5F, 2.5F, SoundType.WOOD);
        add("barrel", new MBarrel(), deco, 2.5F, 2.5F, SoundType.WOOD);

        // ---- Redstone ----
        add("target", new MTarget(), red, 0.5F, 0.5F, SoundType.PLANT);

        // ---- Building ----
        add("oak_hanging_sign", new MFacing(Material.WOOD), deco, 1.0F, 1.0F, SoundType.WOOD).shape(1, 0, 7, 15, 10, 9).collision(null).layer(BlockRenderLayer.CUTOUT);
        CLIMBABLE.add(add("scaffolding", new MScaffolding(), deco, 0.0F, 0.0F, SoundType.WOOD));
        add("chain", new MPillar(Material.IRON, 3.0D), deco, 5.0F, 6.0F, SoundType.METAL);
        SythixBlockAccess.opacity(cube("tinted_glass", Material.GLASS, build, 0.3F, 0.3F, SoundType.GLASS).layer(BlockRenderLayer.TRANSLUCENT), 15);

        // ---- Light sources ----
        add("soul_torch", new MTorch(10), deco, 0.0F, 0.0F, SoundType.WOOD);
        light(cube("lantern", Material.IRON, deco, 3.5F, 3.5F, SoundType.METAL).shape(5, 0, 5, 11, 9, 11).layer(BlockRenderLayer.CUTOUT), 15);
        light(cube("soul_lantern", Material.IRON, deco, 3.5F, 3.5F, SoundType.METAL).shape(5, 0, 5, 11, 9, 11).layer(BlockRenderLayer.CUTOUT), 10);
        light(cube("campfire", Material.WOOD, deco, 2.0F, 2.0F, SoundType.WOOD).shape(0, 0, 0, 16, 7, 16).layer(BlockRenderLayer.CUTOUT).hurts(1.0F).drops("coal", 2, 2, false), 15);
        light(cube("soul_campfire", Material.WOOD, deco, 2.0F, 2.0F, SoundType.WOOD).shape(0, 0, 0, 16, 7, 16).layer(BlockRenderLayer.CUTOUT).hurts(2.0F).drops("soul_sand", 1, 1, false), 10);
        light(cube("shroomlight", Material.GRASS, build, 1.0F, 1.0F, SoundType.PLANT), 15);
        SythixBlockAccess.light(pillar("ochre_froglight", Material.GLASS, 0.3F, SoundType.GLASS), 15);
        SythixBlockAccess.light(pillar("verdant_froglight", Material.GLASS, 0.3F, SoundType.GLASS), 15);
        SythixBlockAccess.light(pillar("pearlescent_froglight", Material.GLASS, 0.3F, SoundType.GLASS), 15);

        // ---- Special / progression ----
        light(cube("trial_spawner", Material.IRON, deco, 50.0F, 50.0F, SoundType.METAL).layer(BlockRenderLayer.CUTOUT).noDrops(), 4);
        light(cube("ominous_trial_spawner", Material.IRON, deco, 50.0F, 50.0F, SoundType.METAL).layer(BlockRenderLayer.CUTOUT).noDrops(), 8);
        light(cube("vault", Material.IRON, deco, 50.0F, 50.0F, SoundType.METAL).layer(BlockRenderLayer.CUTOUT).noDrops(), 6);
        cube("sculk", Material.CLOTH, build, 0.2F, 0.2F, SoundType.SLIME).xp(1, 1);
        add("sculk_sensor", new MSensor(8), red, 1.5F, 1.5F, SoundType.SLIME);
        add("calibrated_sculk_sensor", new MSensor(16), red, 1.5F, 1.5F, SoundType.SLIME);
        cube("sculk_shrieker", Material.CLOTH, deco, 3.0F, 3.0F, SoundType.SLIME).shape(0, 0, 0, 16, 8, 16).layer(BlockRenderLayer.CUTOUT).xp(5, 5);
        cube("respawn_anchor", Material.ROCK, deco, 50.0F, 1200.0F, SoundType.STONE);
        cube("lodestone", Material.IRON, deco, 3.5F, 3.5F, SoundType.METAL);
        add("conduit", new MConduit(), deco, 3.0F, 3.0F, SoundType.GLASS);

        // ---- Ecology ----
        add("bee_nest", new MFacing(Material.WOOD), deco, 0.3F, 0.3F, SoundType.WOOD);
        add("beehive", new MFacing(Material.WOOD), deco, 0.6F, 0.6F, SoundType.WOOD);
        cube("honey_block", Material.CLAY, deco, 0.0F, 0.0F, SoundType.SLIME).collision(new double[] {1, 0, 1, 15, 15, 15}).layer(BlockRenderLayer.TRANSLUCENT).slows(0.4F).softLanding(0.2F);
        add("bamboo", new MBamboo(), deco, 1.0F, 1.0F, SoundType.WOOD);
        add("kelp", new MPlant(16.0D, true), deco, 0.0F, 0.0F, SoundType.PLANT);
        light(cube("sea_pickle", Material.PLANTS, deco, 0.0F, 0.0F, SoundType.SLIME).shape(6, 0, 6, 10, 6, 10).layer(BlockRenderLayer.CUTOUT), 6);
        cube("tube_coral_block", Material.ROCK, build, 1.5F, 6.0F, SoundType.STONE);
        cube("brain_coral_block", Material.ROCK, build, 1.5F, 6.0F, SoundType.STONE);
        cube("dead_tube_coral_block", Material.ROCK, build, 1.5F, 6.0F, SoundType.STONE);

        // ---- Amethyst ----
        cube("amethyst_block", Material.GLASS, build, 1.5F, 1.5F, SoundType.GLASS);
        add("budding_amethyst", new MBudding(), build, 1.5F, 1.5F, SoundType.GLASS);
        add("small_amethyst_bud", new MCrystal(3.0D, 1), deco, 1.5F, 1.5F, SoundType.GLASS).noDrops();
        add("medium_amethyst_bud", new MCrystal(4.0D, 2), deco, 1.5F, 1.5F, SoundType.GLASS).noDrops();
        add("large_amethyst_bud", new MCrystal(5.0D, 4), deco, 1.5F, 1.5F, SoundType.GLASS).noDrops();
        add("amethyst_cluster", new MCrystal(7.0D, 5), deco, 1.5F, 1.5F, SoundType.GLASS).drops("amethyst_shard", 4, 4, true);

        // ---- Copper ----
        add("copper_block", new MCopper("exposed_copper", null), build, 3.0F, 6.0F, SoundType.METAL);
        add("exposed_copper", new MCopper("weathered_copper", "copper_block"), build, 3.0F, 6.0F, SoundType.METAL);
        add("weathered_copper", new MCopper("oxidized_copper", "exposed_copper"), build, 3.0F, 6.0F, SoundType.METAL);
        add("oxidized_copper", new MCopper(null, "weathered_copper"), build, 3.0F, 6.0F, SoundType.METAL);
        add("waxed_copper_block", new MCopper(null, "copper_block"), build, 3.0F, 6.0F, SoundType.METAL);

        // ---- Saplings (from the Items sheet) ----
        add("mangrove_propagule", new MSapling("mangrove_log", "mangrove_leaves", 1), deco, 0.0F, 0.0F, SoundType.PLANT);
        add("cherry_sapling", new MSapling("cherry_log", "cherry_leaves", 0), deco, 0.0F, 0.0F, SoundType.PLANT);
        add("pale_oak_sapling", new MSapling("pale_oak_log", "pale_oak_leaves", 0), deco, 0.0F, 0.0F, SoundType.PLANT);

        TileEntity.registerSythix("sythix_modern_furnace", TileModernFurnace.class);
        TileEntity.registerSythix("sythix_barrel", TileBarrel.class);
    }

    /** Called from Item.registerItems(): item forms of the blocks. */
    public static void registerItems()
    {
        for (Block block : BY_NAME.values())
        {
            SythixItemAccess.registerItemBlock(block, new ItemBlock(block));
        }

        ModernItems.register();
    }

    /** Inventory models for the block items (client). */
    public static void registerItemModels(RenderItem renderItem)
    {
        for (Map.Entry<String, Block> e : BY_NAME.entrySet())
        {
            renderItem.getItemModelMesher().register(Item.getItemFromBlock(e.getValue()), 0, new ModelResourceLocation(e.getKey(), "inventory"));
        }

        ModernItems.registerModels(renderItem);
    }
}
