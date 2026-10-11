package net.sythix.modern.block;

import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Blocks with an axis: logs, deepslate, bamboo blocks, chains.
 */
public class MPillar extends BlockRotatedPillar
{
    /** Width in pixels for thin pillars (chains), 16 = full block. */
    private final double width;

    public MPillar(Material material, double widthPixels)
    {
        super(material);
        this.width = widthPixels;
    }

    private boolean thin()
    {
        return this.width < 16.0D;
    }

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        if (!this.thin())
        {
            return FULL_BLOCK_AABB;
        }

        double a = (8.0D - this.width / 2.0D) / 16.0D;
        double b = (8.0D + this.width / 2.0D) / 16.0D;

        switch ((EnumFacing.Axis)state.getValue(AXIS))
        {
            case X:
                return new AxisAlignedBB(0.0D, a, a, 1.0D, b, b);
            case Z:
                return new AxisAlignedBB(a, a, 0.0D, b, b, 1.0D);
            default:
                return new AxisAlignedBB(a, 0.0D, a, b, 1.0D, b);
        }
    }

    public boolean isOpaqueCube(IBlockState state)
    {
        return !this.thin();
    }

    public boolean isFullCube(IBlockState state)
    {
        return !this.thin();
    }

    public BlockRenderLayer getBlockLayer()
    {
        return this.thin() ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID;
    }

    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face)
    {
        return this.thin() ? BlockFaceShape.UNDEFINED : BlockFaceShape.SOLID;
    }
}
