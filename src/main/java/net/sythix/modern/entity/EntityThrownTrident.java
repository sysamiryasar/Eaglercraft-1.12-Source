package net.sythix.modern.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.sythix.modern.ModernItems;

/**
 * A thrown trident: deals 8 damage and drops back as an item where it lands.
 */
public class EntityThrownTrident extends net.minecraft.entity.projectile.EntityThrowable implements ModernProjectile
{
    private ItemStack stack = ItemStack.EMPTY;
    private boolean dropItem = true;

    public EntityThrownTrident(World worldIn)
    {
        super(worldIn);
    }

    public EntityThrownTrident(World worldIn, double x, double y, double z)
    {
        super(worldIn, x, y, z);
    }

    public EntityThrownTrident(World worldIn, EntityLivingBase thrower, ItemStack tridentStack, boolean drop)
    {
        super(worldIn, thrower);
        this.stack = tridentStack;
        this.dropItem = drop;
    }

    public int getSpawnType()
    {
        return TRIDENT;
    }

    protected float getGravityVelocity()
    {
        return 0.02F;
    }

    protected void onImpact(RayTraceResult result)
    {
        if (this.world.isRemote)
        {
            return;
        }

        if (result.entityHit != null && result.entityHit != this.getThrower())
        {
            result.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 8.0F);
            this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1.0F, 0.6F);
        }

        if (this.dropItem)
        {
            ItemStack drop = this.stack.isEmpty() ? new ItemStack(ModernItems.get("trident")) : this.stack;
            this.world.spawnEntity(new EntityItem(this.world, this.posX, this.posY, this.posZ, drop));
        }

        this.setDead();
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setTag("Trident", this.stack.writeToNBT(new NBTTagCompound()));
        compound.setBoolean("DropItem", this.dropItem);
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.stack = new ItemStack(compound.getCompoundTag("Trident"));
        this.dropItem = compound.getBoolean("DropItem");
    }
}
