package net.sythix.modern.mob;

import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Phantom: a flying undead that circles high above players who have not slept for three nights and
 * swoops down to bite. It burns in sunlight.
 */
public class Phantom extends EntityFlying implements IMob
{
    private double angle;
    private int swoop = 100;
    private boolean diving;
    private BlockPos anchor;

    public Phantom(World worldIn)
    {
        super(worldIn);
        this.setSize(0.9F, 0.5F);
        this.experienceValue = 5;
        this.setNoGravity(true);
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0D);
    }

    public void onUpdate()
    {
        super.onUpdate();

        if (!this.world.isRemote && this.world.getDifficulty() == EnumDifficulty.PEACEFUL)
        {
            this.setDead();
        }
    }

    public void onLivingUpdate()
    {
        if (!this.world.isRemote && this.world.isDaytime() && this.world.canSeeSky(new BlockPos(this.posX, this.posY + 0.5D, this.posZ)) && this.getBrightness() > 0.5F)
        {
            this.setFire(8);
        }

        super.onLivingUpdate();
    }

    protected void updateAITasks()
    {
        EntityLivingBase target = this.getAttackTarget();

        if (target == null || !target.isEntityAlive() || target instanceof EntityPlayer && (((EntityPlayer)target).capabilities.disableDamage || ((EntityPlayer)target).isSpectator()))
        {
            EntityPlayer p = this.world.getNearestAttackablePlayer(this, 64.0D, 64.0D);
            this.setAttackTarget(p);
            target = p;
        }

        if (this.anchor == null)
        {
            this.anchor = new BlockPos(this);
        }

        this.angle += 0.05D;

        if (target == null)
        {
            this.diving = false;
            MobUtil.flyTowards(this, this.anchor.getX() + Math.cos(this.angle) * 12.0D, this.anchor.getY(), this.anchor.getZ() + Math.sin(this.angle) * 12.0D, 0.4D);
            return;
        }

        if (this.diving)
        {
            MobUtil.flyTowards(this, target.posX, target.posY + target.height * 0.5D, target.posZ, 0.9D);

            if (this.getEntityBoundingBox().grow(0.2D).intersects(target.getEntityBoundingBox()))
            {
                target.attackEntityFrom(DamageSource.causeMobDamage(this), 4.0F + this.world.getDifficulty().getDifficultyId());
                this.diving = false;
                this.swoop = 80 + this.rand.nextInt(120);
            }
            else if (this.isCollidedHorizontally || this.hurtTime > 0 || this.swoop-- < -60)
            {
                this.diving = false;
                this.swoop = 80 + this.rand.nextInt(120);
            }
        }
        else
        {
            MobUtil.flyTowards(this, target.posX + Math.cos(this.angle) * 10.0D, target.posY + 18.0D, target.posZ + Math.sin(this.angle) * 10.0D, 0.6D);

            if (--this.swoop <= 0)
            {
                this.diving = true;
                this.playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.0F, 2.0F);
            }
        }
    }

    public void travel(float strafe, float vertical, float forward)
    {
        MobUtil.flyTravel(this);
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && this.world.getDifficulty() != EnumDifficulty.PEACEFUL;
    }

    protected boolean canDespawn()
    {
        return true;
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        if (wasRecentlyHit)
        {
            MobUtil.drop(this, "phantom_membrane", 0, 1, lootingModifier);
        }
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_BAT_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_BAT_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_BAT_DEATH;
    }

    protected float getSoundPitch()
    {
        return 0.6F;
    }

    public void onDeath(DamageSource cause)
    {
        super.onDeath(cause);

        if (this.world instanceof WorldServer)
        {
            ((WorldServer)this.world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX, this.posY, this.posZ, 10, 0.4D, 0.2D, 0.4D, 0.0D);
        }
    }

    public float getEyeHeight()
    {
        return this.height * 0.35F;
    }

    public int getVerticalFaceSpeed()
    {
        return MathHelper.floor(10.0F);
    }
}
