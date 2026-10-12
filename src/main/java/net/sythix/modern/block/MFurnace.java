package net.sythix.modern.block;

import java.util.Random;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Blast furnace / smoker block. Uses the furnace screen and logic (see TileModernFurnace).
 */
public class MFurnace extends BlockContainer
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool LIT = PropertyBool.create("lit");
    private final boolean smoker;

    public MFurnace(boolean isSmoker)
    {
        super(Material.ROCK);
        this.smoker = isSmoker;
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(LIT, Boolean.FALSE));
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.STONE);
    }

    /** Called by the furnace logic when it starts or stops burning. */
    public static void setActive(boolean active, World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof MFurnace && ((Boolean)state.getValue(LIT)).booleanValue() != active)
        {
            TileEntity te = world.getTileEntity(pos);
            world.setBlockState(pos, state.withProperty(LIT, Boolean.valueOf(active)), 3);

            if (te != null)
            {
                te.validate();
                world.setTileEntity(pos, te);
            }
        }
    }

    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileModernFurnace(this.smoker);
    }

    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.MODEL;
    }

    public int getLightValue(IBlockState state)
    {
        return ((Boolean)state.getValue(LIT)).booleanValue() ? 13 : 0;
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (!worldIn.isRemote)
        {
            TileEntity te = worldIn.getTileEntity(pos);

            if (te instanceof TileModernFurnace)
            {
                playerIn.displayGUIChest((TileModernFurnace)te);
            }
        }

        return true;
    }

    public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
    {
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileModernFurnace)
        {
            InventoryHelper.dropInventoryItems(worldIn, pos, (TileModernFurnace)te);
        }

        super.breakBlock(worldIn, pos, state);
    }

    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand)
    {
        if (((Boolean)stateIn.getValue(LIT)).booleanValue())
        {
            EnumFacing f = (EnumFacing)stateIn.getValue(FACING);
            double x = pos.getX() + 0.5D + f.getFrontOffsetX() * 0.52D;
            double z = pos.getZ() + 0.5D + f.getFrontOffsetZ() * 0.52D;
            worldIn.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, pos.getY() + 0.4D, z, 0.0D, 0.0D, 0.0D);
            worldIn.spawnParticle(EnumParticleTypes.FLAME, x, pos.getY() + 0.4D, z, 0.0D, 0.0D, 0.0D);
        }
    }

    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3)).withProperty(LIT, Boolean.valueOf((meta & 4) != 0));
    }

    public int getMetaFromState(IBlockState state)
    {
        return ((EnumFacing)state.getValue(FACING)).getHorizontalIndex() | (((Boolean)state.getValue(LIT)).booleanValue() ? 4 : 0);
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {FACING, LIT});
    }

    public boolean hasComparatorInputOverride(IBlockState state)
    {
        return true;
    }

    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
    {
        return Container.calcRedstone(worldIn.getTileEntity(pos));
    }
}
