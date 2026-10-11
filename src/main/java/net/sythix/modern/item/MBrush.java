package net.sythix.modern.item;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sythix.modern.ModernBlocks;
import net.sythix.modern.block.MSuspicious;

/**
 * Brush: right-click suspicious sand or gravel a few times to dig out an old item.
 */
public class MBrush extends Item
{
    private static final String[] LOOT = new String[] {"pottery_shard_archer", "pottery_shard_prize", "pottery_shard_arms_up", "pottery_shard_skull", "emerald", "gold_nugget", "iron_nugget", "coal", "diamond", "wheat_seeds"};

    public MBrush()
    {
        this.setMaxStackSize(1);
        this.setMaxDamage(64);
    }

    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        IBlockState state = worldIn.getBlockState(pos);

        if (!(state.getBlock() instanceof MSuspicious))
        {
            return EnumActionResult.PASS;
        }

        if (worldIn.isRemote)
        {
            for (int i = 0; i < 6; ++i)
            {
                worldIn.spawnParticle(EnumParticleTypes.BLOCK_DUST, pos.getX() + 0.5D + facing.getFrontOffsetX() * 0.55D, pos.getY() + 0.5D + facing.getFrontOffsetY() * 0.55D, pos.getZ() + 0.5D + facing.getFrontOffsetZ() * 0.55D, 0.0D, 0.05D, 0.0D, net.minecraft.block.Block.getStateId(state));
            }

            return EnumActionResult.SUCCESS;
        }

        // About one in four brushes uncovers the item
        if (worldIn.rand.nextInt(4) == 0)
        {
            String name = LOOT[worldIn.rand.nextInt(worldIn.rand.nextInt(5) == 0 ? LOOT.length : 4)];
            Item loot = ModernBlocks.item(name);

            if (loot != null)
            {
                BlockPos out = pos.offset(facing);
                worldIn.spawnEntity(new EntityItem(worldIn, out.getX() + 0.5D, out.getY() + 0.3D, out.getZ() + 0.5D, new ItemStack(loot)));
            }

            String plain = ((MSuspicious)state.getBlock()).getPlainBlock();
            worldIn.setBlockState(pos, plain.equals("gravel") ? Blocks.GRAVEL.getDefaultState() : Blocks.SAND.getDefaultState());
        }

        player.getHeldItem(hand).damageItem(1, player);
        return EnumActionResult.SUCCESS;
    }
}
