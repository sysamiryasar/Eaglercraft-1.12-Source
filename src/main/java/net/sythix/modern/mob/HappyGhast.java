package net.sythix.modern.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Happy ghast: a gentle flying mob. Put a harness (here: a saddle) on it and up to four players can
 * ride; the first one steers by looking and walking forwards.
 */
public class HappyGhast extends EntityFlying implements IAnimals
{
    private static final DataParameter<Boolean> HARNESS = EntityDataManager.<Boolean>createKey(HappyGhast.class, DataSerializers.BOOLEAN);
    private double wx;
    private double wy;
    private double wz;

    public HappyGhast(World worldIn)
    {
        super(worldIn);
        this.setSize(4.0F, 4.0F);
        this.setNoGravity(true);
    }

    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(HARNESS, Boolean.valueOf(false));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
    }

    public boolean hasHarness()
    {
        return ((Boolean)this.dataManager.get(HARNESS)).booleanValue();
    }

    protected boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);

        if (!this.hasHarness() && held.getItem() == Items.SADDLE)
        {
            if (!this.world.isRemote)
            {
                this.dataManager.set(HARNESS, Boolean.valueOf(true));

                if (!player.capabilities.isCreativeMode)
                {
                    held.shrink(1);
                }

                this.playSound(SoundEvents.ENTITY_HORSE_SADDLE, 1.0F, 1.0F);
            }

            return true;
        }

        if (this.hasHarness() && !player.isSneaking() && this.getPassengers().size() < 4)
        {
            if (!this.world.isRemote)
            {
                player.startRiding(this);
            }

            return true;
        }

        return super.processInteract(player, hand);
    }

    protected boolean canFitPassenger(Entity passenger)
    {
        return this.getPassengers().size() < 4;
    }

    public Entity getControllingPassenger()
    {
        return this.getPassengers().isEmpty() ? null : (Entity)this.getPassengers().get(0);
    }

    public boolean canBeSteered()
    {
        return this.getControllingPassenger() instanceof EntityPlayer;
    }

    public void updatePassenger(Entity passenger)
    {
        if (this.isPassenger(passenger))
        {
            int i = this.getPassengers().indexOf(passenger);
            double ox = (i % 2 == 0 ? -0.7D : 0.7D);
            double oz = (i < 2 ? 0.7D : -0.7D);
            float yaw = this.renderYawOffset * 0.017453292F;
            double x = ox * MathHelper.cos(yaw) - oz * MathHelper.sin(yaw);
            double z = ox * MathHelper.sin(yaw) + oz * MathHelper.cos(yaw);
            passenger.setPosition(this.posX + x, this.posY + this.getMountedYOffset() + passenger.getYOffset(), this.posZ + z);
        }
    }

    public double getMountedYOffset()
    {
        return (double)this.height;
    }

    protected void updateAITasks()
    {
        super.updateAITasks();

        if (this.isBeingRidden())
        {
            return;
        }

        EntityPlayer friend = this.world.getClosestPlayerToEntity(this, 16.0D);

        if (friend != null && friend.getHeldItemMainhand().getItem() == Items.SNOWBALL)
        {
            // like other happy ghasts, it follows players holding snowballs
            MobUtil.flyTowards(this, friend.posX, friend.posY + 3.0D, friend.posZ, 0.08D);
            return;
        }

        if (this.ticksExisted % 100 == 0 || this.getDistanceSq(this.wx, this.wy, this.wz) < 4.0D)
        {
            this.wx = this.posX + this.rand.nextInt(17) - 8;
            this.wy = Math.min(this.posY + this.rand.nextInt(5) - 2, this.world.getHeight(new net.minecraft.util.math.BlockPos(this.wx, 0, this.posZ)).getY() + 8);
            this.wz = this.posZ + this.rand.nextInt(17) - 8;
        }

        MobUtil.flyTowards(this, this.wx, this.wy, this.wz, 0.05D);
    }

    public void travel(float strafe, float vertical, float forward)
    {
        Entity rider = this.getControllingPassenger();

        if (rider instanceof EntityPlayer && this.canBeSteered())
        {
            EntityPlayer p = (EntityPlayer)rider;
            this.rotationYaw = p.rotationYaw;
            this.prevRotationYaw = this.rotationYaw;
            this.renderYawOffset = this.rotationYaw;
            this.rotationYawHead = this.rotationYaw;

            if (this.canPassengerSteer())
            {
                float f = p.moveForward;
                float s = p.moveStrafing * 0.5F;
                double speed = 0.18D;
                float yaw = this.rotationYaw * 0.017453292F;
                float pitch = p.rotationPitch * 0.017453292F;
                double mx = (-MathHelper.sin(yaw) * f + MathHelper.cos(yaw) * s) * MathHelper.cos(pitch);
                double mz = (MathHelper.cos(yaw) * f + MathHelper.sin(yaw) * s) * MathHelper.cos(pitch);
                double my = -MathHelper.sin(pitch) * f;
                this.motionX += (mx * speed - this.motionX) * 0.2D;
                this.motionY += (my * speed - this.motionY) * 0.2D;
                this.motionZ += (mz * speed - this.motionZ) * 0.2D;
                MobUtil.flyTravel(this);
            }
            else
            {
                this.motionX = 0.0D;
                this.motionY = 0.0D;
                this.motionZ = 0.0D;
            }

            return;
        }

        MobUtil.flyTravel(this);
    }

    public void fall(float distance, float damageMultiplier)
    {
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setBoolean("Harness", this.hasHarness());
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.dataManager.set(HARNESS, Boolean.valueOf(compound.getBoolean("Harness")));
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        if (this.hasHarness())
        {
            MobUtil.drop(this, "saddle", 1, 1, 0);
        }
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected boolean canDespawn()
    {
        return false;
    }

    public boolean getCanSpawnHere()
    {
        return false;
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_GHAST_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_GHAST_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_GHAST_DEATH;
    }

    protected float getSoundPitch()
    {
        return 1.5F;
    }

    protected float getSoundVolume()
    {
        return 0.5F;
    }
}
