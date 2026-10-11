package net.sythix.modern.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sythix.modern.ModernBlocks;

/**
 * Budding amethyst: slowly grows amethyst buds on its free sides (small, medium, large, cluster).
 * It cannot be picked up, like in 1.21.
 */
public class MBudding extends MBlock
{
    private static final String[] STAGES = new String[] {"small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud", "amethyst_cluster"};

    public MBudding()
    {
        super(Material.GLASS);
        this.noDrops();
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.GLASS);
        net.minecraft.block.SythixBlockAccess.tickRandomly(this);
    }

    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random)
    {
        if (random.nextInt(5) != 0)
        {
            return;
        }

        EnumFacing side = EnumFacing.random(random);
        BlockPos target = pos.offset(side);
        IBlockState there = worldIn.getBlockState(target);
        Block next = null;

        if (worldIn.isAirBlock(target))
        {
            next = ModernBlocks.get(STAGES[0]);
        }
        else
        {
            for (int i = 0; i < STAGES.length - 1; ++i)
            {
                if (there.getBlock() == ModernBlocks.get(STAGES[i]) && there.getValue(MCrystal.FACING) == side)
                {
                    next = ModernBlocks.get(STAGES[i + 1]);
                }
            }
        }

        if (next != null)
        {
            worldIn.setBlockState(target, next.getDefaultState().withProperty(MCrystal.FACING, side));
        }
    }
}
