package net.sythix.modern.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Simple generators for the new trees (cherry, pale oak, mangrove), used by saplings and world
 * generation.
 */
public final class ModernTrees
{
    private ModernTrees()
    {
    }

    private static boolean free(World world, BlockPos pos)
    {
        IBlockState s = world.getBlockState(pos);
        return s.getMaterial() == Material.AIR || s.getMaterial() == Material.LEAVES || s.getMaterial() == Material.PLANTS || s.getMaterial() == Material.VINE;
    }

    /** Grows a tree at pos (replacing the sapling); returns false if there is no room. */
    public static boolean grow(World world, Random rand, BlockPos pos, Block log, Block leaves, int shape)
    {
        if (log == null || leaves == null)
        {
            return false;
        }

        int height = shape == 1 ? 6 + rand.nextInt(4) : 4 + rand.nextInt(3);

        for (int y = 1; y <= height + 1; ++y)
        {
            if (!free(world, pos.up(y)))
            {
                return false;
            }
        }

        IBlockState logState = log.getDefaultState();
        IBlockState leafState = leaves.getDefaultState();
        world.setBlockState(pos, logState, 2);

        for (int y = 1; y < height; ++y)
        {
            world.setBlockState(pos.up(y), logState, 2);
        }

        if (shape == 1)
        {
            // Mangrove: arching roots around the trunk
            for (EnumFacing f : EnumFacing.Plane.HORIZONTAL)
            {
                if (rand.nextBoolean())
                {
                    BlockPos root = pos.offset(f).up(1);

                    if (free(world, root))
                    {
                        world.setBlockState(root, logState.withProperty(BlockRotatedPillar.AXIS, f.getAxis()), 2);
                    }

                    BlockPos foot = pos.offset(f, 2);

                    if (free(world, foot))
                    {
                        world.setBlockState(foot, logState, 2);
                    }
                }
            }
        }

        // A wide, slightly flat canopy
        BlockPos top = pos.up(height);
        int radius = shape == 0 ? 3 : 2;

        for (int dy = -2; dy <= 1; ++dy)
        {
            int r = dy == 1 ? radius - 1 : (dy == -2 ? radius - 1 : radius);

            for (int dx = -r; dx <= r; ++dx)
            {
                for (int dz = -r; dz <= r; ++dz)
                {
                    if (dx * dx + dz * dz <= r * r + 1 && !(Math.abs(dx) == r && Math.abs(dz) == r && rand.nextBoolean()))
                    {
                        BlockPos p = top.add(dx, dy, dz);

                        if (world.getBlockState(p).getMaterial() == Material.AIR)
                        {
                            world.setBlockState(p, leafState, 2);
                        }
                    }
                }
            }
        }

        world.setBlockState(top, logState, 2);
        return true;
    }
}
