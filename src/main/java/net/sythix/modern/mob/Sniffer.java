package net.sythix.modern.mob;

import net.minecraft.block.material.Material;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Sniffer: a huge ancient animal that sniffs out and digs up torchflower seeds and pitcher pods. */
public class Sniffer extends ModernAnimal
{
    private int digTimer = 2400;

    public Sniffer(World worldIn)
    {
        super(worldIn);
        this.setSize(1.9F, 1.75F);
    }

    protected double maxHealth()
    {
        return 14.0D;
    }

    protected double speed()
    {
        return 0.1D;
    }

    protected String food()
    {
        return "torchflower_seeds";
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (this.world.isRemote || this.isChild())
        {
            return;
        }

        if (--this.digTimer <= 0)
        {
            Material ground = this.world.getBlockState(this.getPosition().down()).getMaterial();

            if (ground == Material.GRASS || ground == Material.GROUND)
            {
                this.getNavigator().clearPathEntity();
                MobUtil.drop(this, this.rand.nextBoolean() ? "torchflower_seeds" : "pitcher_pod", 1, 1, 0);
                this.playSound(SoundEvents.BLOCK_GRAVEL_BREAK, 1.0F, 0.8F);

                if (this.world instanceof WorldServer)
                {
                    ((WorldServer)this.world).spawnParticle(EnumParticleTypes.BLOCK_CRACK, this.posX, this.posY, this.posZ, 30, 0.6D, 0.1D, 0.6D, 0.0D, net.minecraft.block.Block.getStateId(this.world.getBlockState(this.getPosition().down())));
                }
            }

            this.digTimer = 2400 + this.rand.nextInt(3600);
        }
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("DigTimer", this.digTimer);
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.digTimer = compound.getInteger("DigTimer");
    }

    /** Sniffers do not spawn naturally (in 1.21 they hatch from eggs found by archaeology). */
    public boolean getCanSpawnHere()
    {
        return false;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_COW_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_COW_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_COW_DEATH;
    }

    protected float getSoundPitch()
    {
        return 0.5F;
    }
}
