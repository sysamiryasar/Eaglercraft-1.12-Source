package net.sythix.modern.block;

import java.util.Random;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sythix.modern.ModernBlocks;

/**
 * Copper blocks slowly oxidise (copper -> exposed -> weathered -> oxidized). Right-click with an
 * axe to scrape one stage off. Waxed copper never changes.
 */
public class MCopper extends MBlock
{
    private final String next;
    private final String previous;

    public MCopper(String nextStage, String previousStage)
    {
        super(Material.IRON);
        this.next = nextStage;
        this.previous = previousStage;
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.METAL);

        if (nextStage != null)
        {
            net.minecraft.block.SythixBlockAccess.tickRandomly(this);
        }
    }

    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random)
    {
        if (this.next != null && random.nextInt(40) == 0)
        {
            worldIn.setBlockState(pos, ModernBlocks.get(this.next).getDefaultState());
        }
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        ItemStack held = playerIn.getHeldItem(hand);

        if (this.previous == null || !(held.getItem() instanceof ItemAxe))
        {
            return false;
        }

        if (!worldIn.isRemote)
        {
            worldIn.setBlockState(pos, ModernBlocks.get(this.previous).getDefaultState());
            held.damageItem(1, playerIn);
        }
        else
        {
            for (int i = 0; i < 6; ++i)
            {
                worldIn.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, pos.getX() + worldIn.rand.nextDouble(), pos.getY() + worldIn.rand.nextDouble(), pos.getZ() + worldIn.rand.nextDouble(), 0.0D, 0.0D, 0.0D);
            }
        }

        return true;
    }
}
