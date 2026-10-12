package net.sythix.modern.block;

import java.util.Random;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Amethyst buds and clusters: glowing crystals that grow out of the block they are attached to.
 */
public class MCrystal extends MBlock
{
    public static final PropertyDirection FACING = BlockDirectional.FACING;
    private final double size;

    public MCrystal(double sizePixels, int lightLevel)
    {
        super(Material.GLASS);
        this.size = sizePixels;
        this.layer(BlockRenderLayer.CUTOUT);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.GLASS);
        net.minecraft.block.SythixBlockAccess.light(this, lightLevel);
    }

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        double a = 3.0D / 16.0D;
        double b = 13.0D / 16.0D;
        double h = this.size / 16.0D;

        switch ((EnumFacing)state.getValue(FACING))
        {
            case DOWN:
                return new AxisAlignedBB(a, 1.0D - h, a, b, 1.0D, b);
            case NORTH:
                return new AxisAlignedBB(a, a, 1.0D - h, b, b, 1.0D);
            case SOUTH:
                return new AxisAlignedBB(a, a, 0.0D, b, b, h);
            case WEST:
                return new AxisAlignedBB(1.0D - h, a, a, 1.0D, b, b);
            case EAST:
                return new AxisAlignedBB(0.0D, a, a, h, b, b);
            default:
                return new AxisAlignedBB(a, 0.0D, a, b, h, b);
        }
    }

    public net.minecraft.util.math.AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos)
    {
        return this.getBoundingBox(blockState, worldIn, pos);
    }

    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        return this.getDefaultState().withProperty(FACING, facing);
    }

    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getFront(meta & 7));
    }

    public int getMetaFromState(IBlockState state)
    {
        return ((EnumFacing)state.getValue(FACING)).getIndex();
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {FACING});
    }
}
