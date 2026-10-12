package net.sythix.modern.mob;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.sythix.modern.ModernEffects;

/**
 * Warden: a blind guardian of the deep. It notices players who move without sneaking, spreads
 * Darkness, hits very hard and uses a sonic boom on targets it cannot reach. It digs back into the
 * ground after a minute without a target. Summoned by stepping on sculk shriekers.
 */
public class Warden extends EntityMob
{
    private int boomCooldown = 100;
    private int calm;
    private int unreachable;

    public Warden(World worldIn)
    {
        super(worldIn);
        this.setSize(0.9F, 2.9F);
        this.experienceValue = 5;
        this.isImmuneToFire = true;
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(2, new EntityAIAttackMelee(this, 1.0D, true));
        this.tasks.addTask(7, new EntityAIWanderAvoidWater(this, 0.5D));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, 10, false, false, p -> this.notices(p)));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(500.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(30.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24.0D);
    }

    /** The warden is blind: it finds players by the vibrations of their movement. */
    private boolean notices(EntityPlayer p)
    {
        if (p.isSneaking() || p.capabilities.disableDamage)
        {
            return false;
        }

        double moved = Math.abs(p.posX - p.prevPosX) + Math.abs(p.posY - p.prevPosY) + Math.abs(p.posZ - p.prevPosZ);
        return moved > 0.01D || p.isSwingInProgress;
    }

    protected void updateAITasks()
    {
        super.updateAITasks();

        if (this.ticksExisted % 120 == 0)
        {
            Potion darkness = ModernEffects.effect("darkness");
            List<EntityPlayer> players = this.world.<EntityPlayer>getEntitiesWithinAABB(EntityPlayer.class, this.getEntityBoundingBox().grow(20.0D));

            for (EntityPlayer p : players)
            {
                if (darkness != null && !p.capabilities.isCreativeMode)
                {
                    p.addPotionEffect(new PotionEffect(darkness, 260, 0, false, false));
                }
            }

            this.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT, 2.0F, 0.5F);
        }

        EntityLivingBase target = this.getAttackTarget();

        if (target == null || !target.isEntityAlive())
        {
            if (++this.calm > 1200)
            {
                this.digAway();
            }

            return;
        }

        this.calm = 0;

        if (this.boomCooldown > 0)
        {
            --this.boomCooldown;
        }

        double dist = this.getDistanceSqToEntity(target);
        boolean stuck = this.getNavigator().noPath() || Math.abs(target.posY - this.posY) > 3.0D;
        this.unreachable = dist > 9.0D && stuck ? this.unreachable + 1 : 0;

        if (this.boomCooldown <= 0 && dist < 225.0D && dist > 9.0D && (this.unreachable > 60 || this.rand.nextInt(200) == 0) && this.canEntityBeSeen(target))
        {
            this.sonicBoom(target);
        }
    }

    /** A sound blast that goes through armour and shields. */
    private void sonicBoom(EntityLivingBase target)
    {
        this.boomCooldown = 100;
        this.unreachable = 0;
        target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), 10.0F);
        double dx = target.posX - this.posX;
        double dz = target.posZ - this.posZ;
        double d = Math.max(0.01D, Math.sqrt(dx * dx + dz * dz));
        target.addVelocity(dx / d * 2.5D, 0.5D, dz / d * 2.5D);
        target.velocityChanged = true;
        this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3.0F, 1.6F);

        if (this.world instanceof WorldServer)
        {
            for (int i = 1; i < 16; ++i)
            {
                double f = i / 16.0D;
                ((WorldServer)this.world).spawnParticle(EnumParticleTypes.SPELL_MOB, this.posX + (target.posX - this.posX) * f, this.posY + 1.6D + (target.posY - this.posY) * f, this.posZ + (target.posZ - this.posZ) * f, 3, 0.2D, 0.2D, 0.2D, 0.0D);
            }
        }
    }

    private void digAway()
    {
        if (this.world instanceof WorldServer)
        {
            ((WorldServer)this.world).spawnParticle(EnumParticleTypes.BLOCK_CRACK, this.posX, this.posY, this.posZ, 60, 0.5D, 0.2D, 0.5D, 0.0D, net.minecraft.block.Block.getStateId(this.world.getBlockState(this.getPosition().down())));
        }

        this.setDead();
    }

    public boolean attackEntityAsMob(Entity entityIn)
    {
        boolean hit = super.attackEntityAsMob(entityIn);

        if (hit && entityIn instanceof EntityPlayer)
        {
            ((EntityPlayer)entityIn).disableShield(true);
        }

        return hit;
    }

    protected boolean canDespawn()
    {
        return false;
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("Calm", this.calm);
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.calm = compound.getInteger("Calm");
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, "sculk", 1, 3, 0);
        MobUtil.drop(this, "echo_shard", 0, 1, 0);
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_IRONGOLEM_HURT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_IRONGOLEM_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_IRONGOLEM_DEATH;
    }

    protected float getSoundPitch()
    {
        return 0.4F;
    }
}
