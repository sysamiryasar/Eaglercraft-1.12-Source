package net.sythix.modern.block;

import java.util.Random;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Conduit: players in water within 16 blocks can breathe underwater and see better
 * (Conduit Power). In 1.12 it works without the prismarine frame.
 */
public class MConduit extends MBlock
{
    public MConduit()
    {
        super(Material.GLASS);
        this.shape(5, 5, 5, 11, 11, 11);
        this.layer(net.minecraft.util.BlockRenderLayer.CUTOUT);
        net.minecraft.block.SythixBlockAccess.light(this, 15);
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.GLASS);
    }

    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state)
    {
        worldIn.scheduleUpdate(pos, this, 40);
    }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (worldIn.isRemote)
        {
            return;
        }

        for (EntityPlayer p : worldIn.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos).grow(16.0D)))
        {
            if (p.isInWater())
            {
                p.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 260, 0, true, true));
                p.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 260, 0, true, true));
                p.addPotionEffect(new PotionEffect(MobEffects.HASTE, 260, 0, true, true));
            }
        }

        worldIn.scheduleUpdate(pos, this, 40);
    }
}
