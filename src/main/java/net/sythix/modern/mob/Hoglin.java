package net.sythix.modern.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/** Hoglin: a strong nether beast that throws its target into the air. */
public class Hoglin extends EntityMob
{
    public Hoglin(World worldIn)
    {
        super(worldIn);
        this.setSize(1.3965F, 1.4F);
        this.experienceValue = 5;
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(2, new EntityAIAttackMelee(this, 1.0D, true));
        this.tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.6D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        this.addTargets();
    }

    protected void addTargets()
    {
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
    }

    public boolean attackEntityAsMob(Entity entityIn)
    {
        boolean hit = super.attackEntityAsMob(entityIn);

        if (hit)
        {
            entityIn.motionY += 0.5D;
            entityIn.velocityChanged = true;
            this.playSound(SoundEvents.ENTITY_PIG_HURT, 1.0F, 0.5F);
        }

        return hit;
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && super.getCanSpawnHere();
    }

    /** Nether mobs ignore the light level. */
    protected boolean isValidLightLevel()
    {
        return this.world.provider.isNether() || super.isValidLightLevel();
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, this.isBurning() ? "cooked_porkchop" : "porkchop", 2, 4, lootingModifier);
        MobUtil.drop(this, "leather", 0, 1, lootingModifier);
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_PIG_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_PIG_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_PIG_DEATH;
    }

    protected float getSoundPitch()
    {
        return 0.5F + this.rand.nextFloat() * 0.1F;
    }
}
