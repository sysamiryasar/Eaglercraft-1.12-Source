package net.sythix.modern.mob;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/** Trader llama: a llama that travels with a wandering trader and leaves when the trader does. */
public class TraderLlama extends EntityLlama
{
    private int despawnDelay = 47400;

    public TraderLlama(World worldIn)
    {
        super(worldIn);
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (!this.world.isRemote && !this.isTame() && !(this.getLeashedToEntity() instanceof WanderingTrader) && --this.despawnDelay <= 0)
        {
            this.setDead();
        }
    }

    public void setDespawnDelay(int ticks)
    {
        this.despawnDelay = ticks;
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("DespawnDelay", this.despawnDelay);
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);

        if (compound.hasKey("DespawnDelay"))
        {
            this.despawnDelay = compound.getInteger("DespawnDelay");
        }
    }

    public EntityLlama createChild(EntityAgeable ageable)
    {
        return new EntityLlama(this.world);
    }
}
