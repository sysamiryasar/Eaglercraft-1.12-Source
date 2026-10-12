package net.sythix.modern.block;

import java.util.List;
import java.util.Random;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Sculk sensor: gives a redstone signal when something moves nearby (sneaking does not count).
 */
public class MSensor extends MBlock
{
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    private final int range;

    public MSensor(int rangeBlocks)
    {
        super(Material.CLOTH);
        this.range = rangeBlocks;
        this.shape(0, 0, 0, 16, 8, 16);
        this.layer(net.minecraft.util.BlockRenderLayer.CUTOUT);
        this.setDefaultState(this.blockState.getBaseState().withProperty(POWERED, Boolean.FALSE));
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.SLIME);
        net.minecraft.block.SythixBlockAccess.light(this, 1);
    }

    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state)
    {
        worldIn.scheduleUpdate(pos, this, 5);
    }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (worldIn.isRemote)
        {
            return;
        }

        boolean powered = ((Boolean)state.getValue(POWERED)).booleanValue();
        boolean heard = false;
        List<EntityLivingBase> near = worldIn.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(pos).grow((double)this.range));

        for (EntityLivingBase e : near)
        {
            if (!e.isSneaking() && (Math.abs(e.posX - e.prevPosX) + Math.abs(e.posY - e.prevPosY) + Math.abs(e.posZ - e.prevPosZ) > 0.05D || e.swingProgress > 0.0F))
            {
                heard = true;
            }
        }

        if (heard != powered)
        {
            worldIn.setBlockState(pos, state.withProperty(POWERED, Boolean.valueOf(heard)), 3);
            worldIn.notifyNeighborsOfStateChange(pos, this, false);
            worldIn.notifyNeighborsOfStateChange(pos.down(), this, false);
        }

        // A detected vibration keeps the signal for 2 seconds
        worldIn.scheduleUpdate(pos, this, heard ? 40 : 5);
    }

    public boolean canProvidePower(IBlockState state)
    {
        return true;
    }

    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        return ((Boolean)blockState.getValue(POWERED)).booleanValue() ? 15 : 0;
    }

    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        return side == EnumFacing.UP ? this.getWeakPower(blockState, blockAccess, pos, side) : 0;
    }

    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(POWERED, Boolean.valueOf(meta == 1));
    }

    public int getMetaFromState(IBlockState state)
    {
        return ((Boolean)state.getValue(POWERED)).booleanValue() ? 1 : 0;
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {POWERED});
    }
}
