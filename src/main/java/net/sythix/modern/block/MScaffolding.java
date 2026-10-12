package net.sythix.modern.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Scaffolding: climb it like a ladder, stand on top, sneak to go down through it.
 */
public class MScaffolding extends MBlock
{
    private static final AxisAlignedBB TOP = new AxisAlignedBB(0.0D, 14.0D / 16.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    public MScaffolding()
    {
        super(Material.WOOD);
        this.layer(BlockRenderLayer.CUTOUT);
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.WOOD);
    }

    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState)
    {
        // Solid only from above, and not while sneaking (to climb down)
        if (entityIn != null && !entityIn.isSneaking() && entityIn.getEntityBoundingBox().minY >= pos.getY() + 1.0D - 1.0E-4D)
        {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, TOP);
        }
    }

    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    public boolean isFullCube(IBlockState state)
    {
        return false;
    }
}
