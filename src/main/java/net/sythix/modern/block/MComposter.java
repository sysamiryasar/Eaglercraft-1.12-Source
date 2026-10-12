package net.sythix.modern.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Composter: put plants, seeds and food in; when it is full you get bone meal.
 */
public class MComposter extends MBlock
{
    public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 8);

    public MComposter()
    {
        super(Material.WOOD);
        this.shape(0, 0, 0, 16, 16, 16);
        this.layer(net.minecraft.util.BlockRenderLayer.CUTOUT);
        this.setDefaultState(this.blockState.getBaseState().withProperty(LEVEL, Integer.valueOf(0)));
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.WOOD);
    }

    /** Chance (0-1) that one item adds a layer. */
    private static float chance(ItemStack stack)
    {
        Item item = stack.getItem();

        if (item instanceof ItemSeeds || item == Items.WHEAT_SEEDS || item == Items.MELON_SEEDS || item == Items.PUMPKIN_SEEDS || item == Items.BEETROOT_SEEDS)
        {
            return 0.3F;
        }

        if (item instanceof ItemBlock)
        {
            Block block = ((ItemBlock)item).getBlock();

            if (block instanceof BlockLeaves || block instanceof MLeaves || block instanceof BlockBush)
            {
                return 0.5F;
            }
        }

        if (item instanceof ItemFood || item == Items.WHEAT || item == Items.REEDS)
        {
            return 0.65F;
        }

        return 0.0F;
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        int level = ((Integer)state.getValue(LEVEL)).intValue();
        ItemStack held = playerIn.getHeldItem(hand);

        if (level == 8)
        {
            if (!worldIn.isRemote)
            {
                EntityItem drop = new EntityItem(worldIn, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, new ItemStack(Items.DYE, 1, 15));
                worldIn.spawnEntity(drop);
                worldIn.setBlockState(pos, state.withProperty(LEVEL, Integer.valueOf(0)), 3);
            }

            return true;
        }

        float c = chance(held);

        if (c <= 0.0F)
        {
            return false;
        }

        if (!worldIn.isRemote)
        {
            if (!playerIn.capabilities.isCreativeMode)
            {
                held.shrink(1);
            }

            if (worldIn.rand.nextFloat() < c)
            {
                worldIn.setBlockState(pos, state.withProperty(LEVEL, Integer.valueOf(Math.min(8, level + 1))), 3);
            }
        }
        else
        {
            for (int i = 0; i < 5; ++i)
            {
                worldIn.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, pos.getX() + worldIn.rand.nextDouble(), pos.getY() + 1.0D, pos.getZ() + worldIn.rand.nextDouble(), 0.0D, 0.0D, 0.0D);
            }
        }

        return true;
    }

    public boolean hasComparatorInputOverride(IBlockState state)
    {
        return true;
    }

    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        return ((Integer)blockState.getValue(LEVEL)).intValue();
    }

    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(LEVEL, Integer.valueOf(Math.min(8, meta)));
    }

    public int getMetaFromState(IBlockState state)
    {
        return ((Integer)state.getValue(LEVEL)).intValue();
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {LEVEL});
    }
}
