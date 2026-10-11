package net.sythix.modern.mob;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.sythix.modern.entity.EntityWindCharge;

/** Breeze: a jumpy wind spirit that shoots wind charges and shrugs off arrows. */
public class Breeze extends EntityMob implements IRangedAttackMob
{
    public Breeze(World worldIn)
    {
        super(worldIn);
        this.setSize(0.6F, 1.77F);
        this.experienceValue = 10;
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(2, new EntityAIAttackRanged(this, 1.0D, 40, 16.0F));
        this.tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.6D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.4D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24.0D);
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (this.world.isRemote)
        {
            this.world.spawnParticle(EnumParticleTypes.CLOUD, this.posX + (this.rand.nextDouble() - 0.5D) * 0.6D, this.posY + 0.2D, this.posZ + (this.rand.nextDouble() - 0.5D) * 0.6D, 0.0D, 0.02D, 0.0D);
        }
        else if (this.onGround && this.getAttackTarget() != null && this.rand.nextInt(50) == 0)
        {
            // breezes jump around their target
            this.motionY = 0.9D;
            this.motionX += (this.rand.nextDouble() - 0.5D) * 0.8D;
            this.motionZ += (this.rand.nextDouble() - 0.5D) * 0.8D;
        }

        if (this.motionY < 0.0D)
        {
            this.motionY *= 0.8D;
        }
    }

    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor)
    {
        EntityWindCharge charge = new EntityWindCharge(this.world, this);
        double dx = target.posX - this.posX;
        double dy = target.posY + (double)target.getEyeHeight() * 0.5D - charge.posY;
        double dz = target.posZ - this.posZ;
        charge.setThrowableHeading(dx, dy, dz, 1.2F, 2.0F);
        this.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.0F, 1.6F);
        this.world.spawnEntity(charge);
    }

    public void setSwingingArms(boolean swingingArms)
    {
    }

    /** Projectiles (except wind charges) are deflected. */
    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        if (source.isProjectile() && !(source.getImmediateSource() instanceof EntityWindCharge))
        {
            return false;
        }

        return source != DamageSource.FALL && super.attackEntityFrom(source, amount);
    }

    public void fall(float distance, float damageMultiplier)
    {
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && super.getCanSpawnHere();
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, "wind_charge", 1, 2, lootingModifier);
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_BLAZE_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_BLAZE_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_BLAZE_DEATH;
    }

    protected float getSoundPitch()
    {
        return 1.5F;
    }
}
