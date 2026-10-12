package net.sythix.modern.block;

import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Flowers and other small plants (cross-shaped). Placed on grass, dirt and similar.
 */
public class MPlant extends BlockBush
{
    private final AxisAlignedBB box;
    private final boolean sandy;

    public MPlant(double heightPixels, boolean growsOnSand)
    {
        super(Material.PLANTS);
        this.box = new AxisAlignedBB(0.2D, 0.0D, 0.2D, 0.8D, heightPixels / 16.0D, 0.8D);
        this.sandy = growsOnSand;
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.PLANT);
    }

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return this.box;
    }

    protected boolean canSustainBush(IBlockState state)
    {
        return super.canSustainBush(state) || state.getBlock() == Blocks.MYCELIUM || state.getBlock() == net.sythix.modern.ModernBlocks.get("moss_block") || state.getBlock() == net.sythix.modern.ModernBlocks.get("rooted_dirt") || state.getBlock() == net.sythix.modern.ModernBlocks.get("mud") || this.sandy && (state.getBlock() == Blocks.SAND || state.getBlock() == Blocks.GRAVEL || state.getBlock() == Blocks.CLAY || state.getBlock() == this);
    }
}
