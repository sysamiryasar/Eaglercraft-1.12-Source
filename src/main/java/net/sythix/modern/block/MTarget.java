package net.sythix.modern.block;

import java.util.Random;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Target: gives a redstone signal for a second when hit by an arrow or other projectile.
 */
public class MTarget extends MBlock
{
    public static final PropertyInteger POWER = PropertyInteger.create("power", 0, 15);

    public MTarget()
    {
        super(Material.GRASS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(POWER, Integer.valueOf(0)));
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.PLANT);
    }

    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn)
    {
        if (!worldIn.isRemote && entityIn instanceof IProjectile && ((Integer)state.getValue(POWER)).intValue() == 0)
        {
            worldIn.setBlockState(pos, state.withProperty(POWER, Integer.valueOf(15)), 3);
            worldIn.notifyNeighborsOfStateChange(pos, this, false);
            worldIn.scheduleUpdate(pos, this, 20);
        }
    }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (((Integer)state.getValue(POWER)).intValue() > 0)
        {
            worldIn.setBlockState(pos, state.withProperty(POWER, Integer.valueOf(0)), 3);
            worldIn.notifyNeighborsOfStateChange(pos, this, false);
        }
    }

    public boolean canProvidePower(IBlockState state)
    {
        return true;
    }

    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        return ((Integer)blockState.getValue(POWER)).intValue();
    }

    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(POWER, Integer.valueOf(meta & 15));
    }

    public int getMetaFromState(IBlockState state)
    {
        return ((Integer)state.getValue(POWER)).intValue();
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {POWER});
    }
}
