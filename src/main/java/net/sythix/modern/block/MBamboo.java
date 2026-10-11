package net.sythix.modern.block;

import java.util.Random;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Bamboo: a thin stalk that grows up to 12 blocks tall on grass, dirt or sand.
 */
public class MBamboo extends MBlock
{
    public MBamboo()
    {
        super(Material.WOOD);
        this.shape(6.5, 0, 6.5, 9.5, 16, 9.5);
        this.layer(net.minecraft.util.BlockRenderLayer.CUTOUT);
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.WOOD);
        net.minecraft.block.SythixBlockAccess.tickRandomly(this);
    }

    public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        IBlockState below = worldIn.getBlockState(pos.down());
        return super.canPlaceBlockAt(worldIn, pos) && (below.getBlock() == this || below.getBlock() == Blocks.GRASS || below.getBlock() == Blocks.DIRT || below.getBlock() == Blocks.SAND || below.getBlock() == Blocks.GRAVEL);
    }

    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, net.minecraft.block.Block blockIn, BlockPos fromPos)
    {
        if (!this.canPlaceBlockAt(worldIn, pos) && worldIn.getBlockState(pos.down()).getBlock() != this)
        {
            worldIn.destroyBlock(pos, true);
        }
    }

    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random)
    {
        if (random.nextInt(3) != 0 || !worldIn.isAirBlock(pos.up()))
        {
            return;
        }

        int height = 1;

        while (height < 12 && worldIn.getBlockState(pos.down(height)).getBlock() == this)
        {
            ++height;
        }

        if (height < 12)
        {
            worldIn.setBlockState(pos.up(), this.getDefaultState());
        }
    }
}
