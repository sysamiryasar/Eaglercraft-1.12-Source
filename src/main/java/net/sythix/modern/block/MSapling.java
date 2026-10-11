package net.sythix.modern.block;

import java.util.Random;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sythix.modern.ModernBlocks;

/**
 * Saplings of the new trees. They grow by themselves or with bone meal.
 */
public class MSapling extends MPlant implements IGrowable
{
    private final String log;
    private final String leaves;
    private final int shape;

    /** shape: 0 = round (cherry, pale oak), 1 = tall with roots (mangrove). */
    public MSapling(String logName, String leavesName, int treeShape)
    {
        super(13.0D, false);
        this.log = logName;
        this.leaves = leavesName;
        this.shape = treeShape;
        net.minecraft.block.SythixBlockAccess.tickRandomly(this);
    }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        super.updateTick(worldIn, pos, state, rand);

        if (!worldIn.isRemote && worldIn.getLightFromNeighbors(pos.up()) >= 9 && rand.nextInt(7) == 0)
        {
            this.grow(worldIn, rand, pos, state);
        }
    }

    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient)
    {
        return true;
    }

    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state)
    {
        return worldIn.rand.nextFloat() < 0.45F;
    }

    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state)
    {
        ModernTrees.grow(worldIn, rand, pos, ModernBlocks.get(this.log), ModernBlocks.get(this.leaves), this.shape);
    }
}
