package net.sythix.modern.mob;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Ravager: a huge beast that charges and roars, knocking back everything around it. */
public class Ravager extends EntityMob
{
    private int roarCooldown;

    public Ravager(World worldIn)
    {
        super(worldIn);
        this.setSize(1.95F, 2.2F);
        this.stepHeight = 1.0F;
        this.experienceValue = 20;
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(4, new EntityAIAttackMelee(this, 1.0D, true));
        this.tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.4D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        this.tasks.addTask(10, new EntityAILookIdle(this));
        this.targetTasks.addTask(2, new EntityAIHurtByTarget(this, true, AbstractIllager.class));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
        this.targetTasks.addTask(4, new EntityAINearestAttackableTarget<EntityVillager>(this, EntityVillager.class, true));
        this.targetTasks.addTask(4, new EntityAINearestAttackableTarget<EntityIronGolem>(this, EntityIronGolem.class, false));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.75D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(12.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
    }

    protected void updateAITasks()
    {
        super.updateAITasks();

        if (this.roarCooldown > 0)
        {
            --this.roarCooldown;
        }
        else if (this.getAttackTarget() != null && this.getDistanceSqToEntity(this.getAttackTarget()) < 16.0D && this.rand.nextInt(60) == 0)
        {
            this.roar();
        }
    }

    /** Knocks back and hurts everything nearby except illagers. */
    public void roar()
    {
        this.roarCooldown = 200;
        this.playSound(SoundEvents.ENTITY_POLAR_BEAR_WARNING, 2.0F, 0.6F);
        List<EntityLivingBase> list = this.world.<EntityLivingBase>getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(4.0D), e -> e != this && !(e instanceof AbstractIllager) && !(e instanceof Ravager));

        for (EntityLivingBase e : list)
        {
            e.attackEntityFrom(DamageSource.causeMobDamage(this), 6.0F);
            double dx = e.posX - this.posX;
            double dz = e.posZ - this.posZ;
            double d = Math.max(0.01D, Math.sqrt(dx * dx + dz * dz));
            e.addVelocity(dx / d * 1.5D, 0.3D, dz / d * 1.5D);
            e.velocityChanged = true;
        }

        if (this.world instanceof WorldServer)
        {
            ((WorldServer)this.world).spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, this.posX, this.posY + 1.0D, this.posZ, 20, 2.0D, 0.5D, 2.0D, 0.0D);
        }
    }

    public boolean attackEntityAsMob(Entity entityIn)
    {
        boolean hit = super.attackEntityAsMob(entityIn);

        if (hit)
        {
            entityIn.motionY += 0.3D;
        }

        return hit;
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && this.world.canSeeSky(new net.minecraft.util.math.BlockPos(this)) && super.getCanSpawnHere();
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, "saddle", 1, 1, 0);
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_POLAR_BEAR_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_POLAR_BEAR_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_POLAR_BEAR_DEATH;
    }

    protected float getSoundPitch()
    {
        return 0.6F;
    }
}
