package net.sythix.modern.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Powder snow: you sink into it and slowly freeze, unless you wear leather boots (then you can
 * walk on it).
 */
public class MPowderSnow extends MBlock
{
    public MPowderSnow()
    {
        super(Material.CRAFTED_SNOW);
        this.collision(null);
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.SNOW);
    }

    private static boolean leatherBoots(@Nullable Entity entity)
    {
        return entity instanceof EntityLivingBase && ((EntityLivingBase)entity).getItemStackFromSlot(EntityEquipmentSlot.FEET).getItem() == Items.LEATHER_BOOTS;
    }

    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState)
    {
        if (leatherBoots(entityIn) && !entityIn.isSneaking())
        {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, FULL_BLOCK_AABB);
        }
    }

    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn)
    {
        if (leatherBoots(entityIn))
        {
            return;
        }

        entityIn.motionX *= 0.5D;
        entityIn.motionZ *= 0.5D;
        entityIn.motionY = Math.max(entityIn.motionY * 0.5D, -0.08D);
        entityIn.fallDistance = 0.0F;

        if (!worldIn.isRemote && entityIn instanceof EntityLivingBase && entityIn.ticksExisted % 40 == 0 && entityIn.getEntityBoundingBox().maxY < pos.getY() + 1.0D)
        {
            entityIn.attackEntityFrom(DamageSource.GENERIC, 1.0F);
        }
    }

    public boolean isOpaqueCube(IBlockState state)
    {
        return true;
    }

    public boolean isFullCube(IBlockState state)
    {
        return true;
    }
}
