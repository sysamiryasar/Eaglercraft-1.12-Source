package net.sythix.modern.block;

import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.sythix.modern.ModernBlocks;

/**
 * Leaves of the new trees. They do not decay; breaking them sometimes drops a sapling.
 */
public class MLeaves extends MBlock
{
    private final String sapling;

    public MLeaves(String saplingName)
    {
        super(Material.LEAVES);
        this.sapling = saplingName;
        this.layer(BlockRenderLayer.CUTOUT_MIPPED);
        net.minecraft.block.SythixBlockAccess.opacity(this, 1);
    }

    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        return !blockAccess.getBlockState(pos.offset(side)).isOpaqueCube() || blockAccess.getBlockState(pos.offset(side)).getBlock() == this;
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        Item sap = this.sapling == null ? null : ModernBlocks.item(this.sapling);
        return sap != null && rand.nextInt(20) == 0 ? sap : net.minecraft.init.Items.AIR;
    }

    public int quantityDropped(Random random)
    {
        return 1;
    }

    public int quantityDroppedWithBonus(int fortune, Random random)
    {
        return 1;
    }
}
