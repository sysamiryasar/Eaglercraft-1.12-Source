package net.sythix.modern;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeHills;
import net.minecraft.world.biome.BiomeJungle;
import net.minecraft.world.biome.BiomeSwamp;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.sythix.modern.block.MCrystal;
import net.sythix.modern.block.ModernTrees;

/**
 * Generates the 1.21 blocks in NEW chunks of singleplayer/LAN worlds: a deepslate layer with
 * deepslate ores and tuff, copper ore, amethyst geodes, cherry/pale oak/mangrove trees, bamboo,
 * new flowers, mud, dripstone, sculk, and nether gold ore and ancient debris. Runs after vanilla
 * population with its own random numbers, so vanilla terrain for a seed stays the same.
 */
public final class ModernWorldGen
{
    private ModernWorldGen()
    {
    }

    private static Block b(String name)
    {
        return ModernBlocks.get(name);
    }

    private static Random random(World world, int x, int z, int salt)
    {
        return new Random(world.getSeed() * 31L + (long)x * 341873128712L + (long)z * 132897987541L + (long)salt);
    }

    // ---- Overworld ----------------------------------------------------------------------------

    /** x, z = block coordinates of the chunk being populated (its corner). */
    public static void populateOverworld(World world, int x, int z, Biome biome)
    {
        if (!ModernContent.worldGen())
        {
            return;
        }

        Random r = random(world, x, z, 1);

        if (ModernContent.deepslate())
        {
            deepslateLayer(world, x, z, r);
        }

        // Features are placed in the vanilla population area (+8..+23) so no new chunks load
        int ox = x + 8;
        int oz = z + 8;
        ore(world, r, "copper_ore", 10, 8, 16, 96, ox, oz);
        ore(world, r, "tuff", 24, 2, 0, 16, ox, oz);
        ore(world, r, "dripstone_block", 20, 1, 16, 50, ox, oz);

        if (ModernContent.geodes() && r.nextInt(24) == 0)
        {
            geode(world, r, new BlockPos(x + 12 + r.nextInt(8), 8 + r.nextInt(24), z + 12 + r.nextInt(8)));
        }

        if (r.nextInt(10) == 0)
        {
            patch(world, r, "sculk", new BlockPos(ox + r.nextInt(16), 4 + r.nextInt(10), oz + r.nextInt(16)), 3);
        }

        if (ModernContent.plants())
        {
            biomeFeatures(world, r, ox, oz, biome);
        }
    }

    /** Stone below y 16 becomes deepslate (always below 8, more and more often going down). */
    private static void deepslateLayer(World world, int x, int z, Random r)
    {
        Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
        ExtendedBlockStorage section = chunk.getBlockStorageArray()[0];

        if (section == null)
        {
            return;
        }

        IBlockState deep = b("deepslate").getDefaultState();
        IBlockState tuff = b("tuff").getDefaultState();
        boolean changed = false;

        for (int dx = 0; dx < 16; ++dx)
        {
            for (int dz = 0; dz < 16; ++dz)
            {
                for (int y = 1; y < 16; ++y)
                {
                    if (y >= 8 && r.nextInt(8) < y - 7)
                    {
                        continue;
                    }

                    IBlockState s = section.get(dx, y, dz);
                    Block block = s.getBlock();
                    IBlockState to = null;

                    if (block == Blocks.STONE)
                    {
                        BlockStone.EnumType type = (BlockStone.EnumType)s.getValue(BlockStone.VARIANT);
                        to = type == BlockStone.EnumType.STONE ? deep : tuff;
                    }
                    else if (block == Blocks.COAL_ORE)
                    {
                        to = b("deepslate_coal_ore").getDefaultState();
                    }
                    else if (block == Blocks.IRON_ORE)
                    {
                        to = b("deepslate_iron_ore").getDefaultState();
                    }
                    else if (block == Blocks.GOLD_ORE)
                    {
                        to = b("deepslate_gold_ore").getDefaultState();
                    }
                    else if (block == Blocks.REDSTONE_ORE || block == Blocks.LIT_REDSTONE_ORE)
                    {
                        to = b("deepslate_redstone_ore").getDefaultState();
                    }
                    else if (block == Blocks.LAPIS_ORE)
                    {
                        to = b("deepslate_lapis_ore").getDefaultState();
                    }
                    else if (block == Blocks.DIAMOND_ORE)
                    {
                        to = b("deepslate_diamond_ore").getDefaultState();
                    }
                    else if (block == Blocks.EMERALD_ORE)
                    {
                        to = b("deepslate_emerald_ore").getDefaultState();
                    }
                    else if (block == b("copper_ore"))
                    {
                        to = b("deepslate_copper_ore").getDefaultState();
                    }

                    if (to != null)
                    {
                        section.set(dx, y, dz, to);
                        changed = true;
                    }
                }
            }
        }

        if (changed)
        {
            chunk.markDirty();
        }
    }

    private static void ore(World world, Random r, String block, int size, int count, int minY, int maxY, int ox, int oz)
    {
        Block ore = b(block);

        if (ore == null)
        {
            return;
        }

        WorldGenMinable gen = new WorldGenMinable(ore.getDefaultState(), size);

        for (int i = 0; i < count; ++i)
        {
            gen.generate(world, r, new BlockPos(ox + r.nextInt(16), minY + r.nextInt(Math.max(1, maxY - minY)), oz + r.nextInt(16)));
        }
    }

    /** A hollow amethyst geode: smooth basalt shell, calcite, amethyst lining with buds. */
    private static void geode(World world, Random r, BlockPos c)
    {
        IBlockState basalt = b("smooth_basalt").getDefaultState();
        IBlockState calcite = b("calcite").getDefaultState();
        IBlockState amethyst = b("amethyst_block").getDefaultState();
        IBlockState budding = b("budding_amethyst").getDefaultState();
        int radius = 4;

        for (int dx = -radius; dx <= radius; ++dx)
        {
            for (int dy = -radius; dy <= radius; ++dy)
            {
                for (int dz = -radius; dz <= radius; ++dz)
                {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    BlockPos p = c.add(dx, dy, dz);

                    if (d > radius + 0.3D || p.getY() < 2)
                    {
                        continue;
                    }

                    if (d > radius - 0.7D)
                    {
                        world.setBlockState(p, basalt, 2);
                    }
                    else if (d > radius - 1.5D)
                    {
                        world.setBlockState(p, calcite, 2);
                    }
                    else if (d > radius - 2.4D)
                    {
                        world.setBlockState(p, r.nextInt(12) == 0 ? budding : amethyst, 2);
                    }
                    else
                    {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }

        // Crystals on the inside walls
        String[] buds = new String[] {"small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud", "amethyst_cluster"};

        for (int i = 0; i < 14; ++i)
        {
            BlockPos p = c.add(r.nextInt(5) - 2, r.nextInt(5) - 2, r.nextInt(5) - 2);

            if (!world.isAirBlock(p))
            {
                continue;
            }

            for (EnumFacing f : EnumFacing.values())
            {
                if (world.getBlockState(p.offset(f.getOpposite())).getBlock() == b("amethyst_block") || world.getBlockState(p.offset(f.getOpposite())).getBlock() == b("budding_amethyst"))
                {
                    world.setBlockState(p, b(buds[r.nextInt(buds.length)]).getDefaultState().withProperty(MCrystal.FACING, f), 2);
                    break;
                }
            }
        }
    }

    /** A small blob of a block replacing stone. */
    private static void patch(World world, Random r, String block, BlockPos c, int radius)
    {
        IBlockState state = b(block).getDefaultState();

        for (int dx = -radius; dx <= radius; ++dx)
        {
            for (int dz = -radius; dz <= radius; ++dz)
            {
                for (int dy = -1; dy <= 1; ++dy)
                {
                    BlockPos p = c.add(dx, dy, dz);

                    if (dx * dx + dz * dz <= radius * radius && r.nextInt(3) > 0 && world.getBlockState(p).getBlock() == Blocks.STONE)
                    {
                        world.setBlockState(p, state, 2);
                    }
                }
            }
        }
    }

    /** Surface position (on top of the highest solid block). */
    private static BlockPos surface(World world, int x, int z)
    {
        return world.getHeight(new BlockPos(x, 0, z));
    }

    private static boolean grassy(World world, BlockPos ground)
    {
        Block below = world.getBlockState(ground.down()).getBlock();
        return below == Blocks.GRASS || below == Blocks.DIRT;
    }

    private static void flowers(World world, Random r, String flower, int ox, int oz, int tries)
    {
        Block f = b(flower);

        for (int i = 0; i < tries; ++i)
        {
            BlockPos p = surface(world, ox + r.nextInt(16), oz + r.nextInt(16));

            if (world.isAirBlock(p) && grassy(world, p))
            {
                world.setBlockState(p, f.getDefaultState(), 2);
            }
        }
    }

    private static void trees(World world, Random r, String log, String leaves, int shape, int ox, int oz, int count)
    {
        for (int i = 0; i < count; ++i)
        {
            BlockPos p = surface(world, ox + 2 + r.nextInt(12), oz + 2 + r.nextInt(12));

            if (grassy(world, p) || world.getBlockState(p.down()).getBlock() == b("mud"))
            {
                ModernTrees.grow(world, r, p, b(log), b(leaves), shape);
            }
        }
    }

    private static void biomeFeatures(World world, Random r, int ox, int oz, Biome biome)
    {
        if (biome == Biomes.PLAINS || biome == Biomes.MUTATED_PLAINS)
        {
            flowers(world, r, "cornflower", ox, oz, 4);

            if (r.nextInt(8) == 0)
            {
                flowers(world, r, "torchflower", ox, oz, 1);
            }
        }
        else if (biome == Biomes.FOREST || biome == Biomes.FOREST_HILLS || biome == Biomes.MUTATED_FOREST)
        {
            flowers(world, r, "lily_of_the_valley", ox, oz, 4);
            flowers(world, r, "cornflower", ox, oz, 2);
        }
        else if (biome == Biomes.BIRCH_FOREST || biome == Biomes.BIRCH_FOREST_HILLS)
        {
            flowers(world, r, "pink_petals", ox, oz, 6);
        }
        else if (biome == Biomes.ROOFED_FOREST || biome == Biomes.MUTATED_ROOFED_FOREST)
        {
            if (r.nextInt(3) == 0)
            {
                trees(world, r, "pale_oak_log", "pale_oak_leaves", 0, ox, oz, 1);
            }
        }
        else if (biome instanceof BiomeSwamp)
        {
            for (int i = 0; i < 6; ++i)
            {
                BlockPos p = surface(world, ox + r.nextInt(16), oz + r.nextInt(16)).down();

                if (world.getBlockState(p).getBlock() == Blocks.GRASS || world.getBlockState(p).getBlock() == Blocks.DIRT)
                {
                    world.setBlockState(p, b("mud").getDefaultState(), 2);
                }
            }

            if (r.nextBoolean())
            {
                trees(world, r, "mangrove_log", "mangrove_leaves", 1, ox, oz, 1);
            }
        }
        else if (biome instanceof BiomeJungle)
        {
            for (int i = 0; i < 4; ++i)
            {
                BlockPos p = surface(world, ox + r.nextInt(16), oz + r.nextInt(16));

                if (grassy(world, p))
                {
                    int h = 3 + r.nextInt(8);

                    for (int y = 0; y < h && world.isAirBlock(p.up(y)); ++y)
                    {
                        world.setBlockState(p.up(y), b("bamboo").getDefaultState(), 2);
                    }
                }
            }
        }
        else if (biome instanceof BiomeHills)
        {
            // Cherry groves grow on the mountains
            if (r.nextInt(3) == 0)
            {
                trees(world, r, "cherry_log", "cherry_leaves", 0, ox, oz, 1 + r.nextInt(2));
                flowers(world, r, "pink_petals", ox, oz, 6);
            }
        }
        else if (biome == Biomes.DESERT || biome == Biomes.DESERT_HILLS)
        {
            if (r.nextInt(6) == 0)
            {
                BlockPos p = surface(world, ox + r.nextInt(16), oz + r.nextInt(16)).down(2);

                if (world.getBlockState(p).getBlock() == Blocks.SAND)
                {
                    world.setBlockState(p, b("suspicious_sand").getDefaultState(), 2);
                }
            }
        }
    }

    // ---- Nether -------------------------------------------------------------------------------

    public static void populateNether(World world, int x, int z)
    {
        if (!ModernContent.worldGen())
        {
            return;
        }

        Random r = random(world, x, z, 2);
        int ox = x + 8;
        int oz = z + 8;
        WorldGenMinable gold = new WorldGenMinable(b("nether_gold_ore").getDefaultState(), 10, BlockMatcher.forBlock(Blocks.NETHERRACK));

        for (int i = 0; i < 10; ++i)
        {
            gold.generate(world, r, new BlockPos(ox + r.nextInt(16), 10 + r.nextInt(108), oz + r.nextInt(16)));
        }

        WorldGenMinable debris = new WorldGenMinable(b("ancient_debris").getDefaultState(), 2, BlockMatcher.forBlock(Blocks.NETHERRACK));

        for (int i = 0; i < 2; ++i)
        {
            debris.generate(world, r, new BlockPos(ox + r.nextInt(16), 8 + r.nextInt(16), oz + r.nextInt(16)));
        }
    }
}
