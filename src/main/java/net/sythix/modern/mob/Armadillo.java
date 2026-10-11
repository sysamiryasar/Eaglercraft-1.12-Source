package net.sythix.modern.mob;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/**
 * Armadillo: rolls into a ball when a sprinting player comes close or it gets hurt (taking half
 * damage while rolled), and sheds a scute now and then. Brushing it gives a scute too.
 */
public class Armadillo extends ModernAnimal
{
    private static final DataParameter<Boolean> ROLLED = EntityDataManager.<Boolean>createKey(Armadillo.class, DataSerializers.BOOLEAN);
    private int calm;
    private int scuteTimer = 6000;

    public Armadillo(World worldIn)
    {
        super(worldIn);
        this.setSize(0.7F, 0.65F);
    }

    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(ROLLED, Boolean.valueOf(false));
    }

    public boolean isRolled()
    {
        return ((Boolean)this.dataManager.get(ROLLED)).booleanValue();
    }

    private void setRolled(boolean rolled)
    {
        if (rolled != this.isRolled())
        {
            this.dataManager.set(ROLLED, Boolean.valueOf(rolled));
            this.playSound(SoundEvents.ENTITY_SILVERFISH_STEP, 1.0F, rolled ? 0.6F : 0.9F);
        }
    }

    protected double maxHealth()
    {
        return 12.0D;
    }

    protected double speed()
    {
        return 0.14D;
    }

    protected String food()
    {
        return "spider_eye";
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (this.world.isRemote)
        {
            return;
        }

        EntityPlayer p = this.world.getClosestPlayerToEntity(this, 7.0D);
        boolean scared = p != null && !p.isSpectator() && (p.isSprinting() || p.isRiding()) || this.hurtTime > 0;

        if (scared)
        {
            this.calm = 0;
            this.setRolled(true);
        }
        else if (this.isRolled() && ++this.calm > 60)
        {
            this.setRolled(false);
        }

        if (!this.isChild() && --this.scuteTimer <= 0)
        {
            MobUtil.drop(this, "armadillo_scute", 1, 1, 0);
            this.scuteTimer = 6000 + this.rand.nextInt(6000);
        }
    }

    protected boolean isMovementBlocked()
    {
        return this.isRolled() || super.isMovementBlocked();
    }

    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        return super.attackEntityFrom(source, this.isRolled() ? amount * 0.5F : amount);
    }

    public boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);
        Item brush = MobUtil.item("brush");

        if (brush != null && held.getItem() == brush && !this.isChild())
        {
            if (!this.world.isRemote)
            {
                MobUtil.drop(this, "armadillo_scute", 1, 1, 0);
                held.damageItem(16, player);
            }

            return true;
        }

        return super.processInteract(player, hand);
    }

    public boolean getCanSpawnHere()
    {
        return this.canSpawnOn(Blocks.GRASS, Blocks.SAND, Blocks.HARDENED_CLAY, Blocks.STAINED_HARDENED_CLAY);
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("ScuteTime", this.scuteTimer);
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.scuteTimer = compound.getInteger("ScuteTime");
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_SILVERFISH_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_SILVERFISH_DEATH;
    }
}
