package net.sythix.modern.item;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sythix.modern.ModernBlocks;

/**
 * Items that place a block: seeds that plant a flower, and the powder snow bucket.
 */
public class MPlacer extends Item
{
    private final String block;
    private final boolean bucket;

    public MPlacer(String blockName, boolean isBucket)
    {
        this.block = blockName;
        this.bucket = isBucket;
        this.setMaxStackSize(isBucket ? 1 : 64);
    }

    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        Block b = ModernBlocks.get(this.block);
        BlockPos target = pos.offset(facing);
        IBlockState below = worldIn.getBlockState(target.down());

        if (b == null || !worldIn.getBlockState(target).getBlock().isReplaceable(worldIn, target))
        {
            return EnumActionResult.FAIL;
        }

        if (!this.bucket && facing != EnumFacing.UP || !this.bucket && below.getBlock() != Blocks.GRASS && below.getBlock() != Blocks.DIRT && below.getBlock() != Blocks.FARMLAND)
        {
            return EnumActionResult.FAIL;
        }

        if (!worldIn.isRemote)
        {
            worldIn.setBlockState(target, b.getDefaultState());
            ItemStack stack = player.getHeldItem(hand);

            if (!player.capabilities.isCreativeMode)
            {
                if (this.bucket)
                {
                    player.setHeldItem(hand, new ItemStack(Items.BUCKET));
                }
                else
                {
                    stack.shrink(1);
                }
            }
        }

        return EnumActionResult.SUCCESS;
    }
}
