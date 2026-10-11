package net.sythix.modern.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Creaking: a tree creature of the pale forests that only moves while nobody is looking at it.
 * It crumbles at sunrise.
 */
public class Creaking extends EntityMob
{
    private boolean watched;

    public Creaking(World worldIn)
    {
        super(worldIn);
        this.setSize(0.9F, 2.7F);
        this.experienceValue = 0;
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(2, new EntityAIAttackMelee(this, 1.0D, true));
        this.tasks.addTask(7, new EntityAIWanderAvoidWater(this, 0.6D));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, false));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.4D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
    }

    /** Whether a survival player within 32 blocks is looking at this creaking. */
    private boolean isWatched()
    {
        for (EntityPlayer p : this.world.playerEntities)
        {
            if (p.isSpectator() || p.capabilities.isCreativeMode || p.getDistanceSqToEntity(this) > 1024.0D)
            {
                continue;
            }

            Vec3d look = p.getLook(1.0F).normalize();
            Vec3d to = new Vec3d(this.posX - p.posX, this.posY + this.height * 0.5D - (p.posY + p.getEyeHeight()), this.posZ - p.posZ);
            double d = to.lengthVector();

            if (look.dotProduct(to.normalize()) > 1.0D - 0.6D / d && p.canEntityBeSeen(this))
            {
                return true;
            }
        }

        return false;
    }

    protected void updateAITasks()
    {
        this.watched = this.isWatched();

        if (this.watched)
        {
            this.getNavigator().clearPathEntity();
            this.motionX = 0.0D;
            this.motionZ = 0.0D;
            return;
        }

        super.updateAITasks();

        if (this.world.isDaytime() && this.world.canSeeSky(new BlockPos(this)) && this.ticksExisted % 20 == 0)
        {
            if (this.world instanceof WorldServer)
            {
                ((WorldServer)this.world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX, this.posY + 1.0D, this.posZ, 20, 0.3D, 0.8D, 0.3D, 0.0D);
            }

            this.playSound(SoundEvents.BLOCK_WOOD_BREAK, 1.0F, 0.6F);
            this.setDead();
        }
    }

    protected boolean isMovementBlocked()
    {
        return this.watched || super.isMovementBlocked();
    }

    public boolean attackEntityAsMob(Entity entityIn)
    {
        return !this.watched && super.attackEntityAsMob(entityIn);
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && !this.world.isDaytime() && super.getCanSpawnHere();
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, "pale_oak_log", 0, 1, 0);
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.BLOCK_WOOD_STEP;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.BLOCK_WOOD_HIT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.BLOCK_WOOD_BREAK;
    }

    protected float getSoundPitch()
    {
        return 0.5F;
    }
}
