package net.sythix.modern.mob;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

/** Pillager: an illager that patrols and shoots with a crossbow. */
public class Pillager extends AbstractIllager implements IRangedAttackMob
{
    public Pillager(World worldIn)
    {
        super(worldIn);
        this.setSize(0.6F, 1.95F);
    }

    protected void initEntityAI()
    {
        super.initEntityAI();
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIAvoidEntity<EntityWolf>(this, EntityWolf.class, 6.0F, 1.0D, 1.2D));
        this.tasks.addTask(3, new EntityAIAttackRanged(this, 1.0D, 30, 15.0F));
        this.tasks.addTask(8, new EntityAIWanderAvoidWater(this, 0.6D));
        this.tasks.addTask(9, new EntityAIWatchClosest(this, EntityPlayer.class, 15.0F, 1.0F));
        this.tasks.addTask(10, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, AbstractIllager.class));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityVillager>(this, EntityVillager.class, false));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityIronGolem>(this, EntityIronGolem.class, true));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(24.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(5.0D);
    }

    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata)
    {
        livingdata = super.onInitialSpawn(difficulty, livingdata);
        Item crossbow = MobUtil.item("crossbow");
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(crossbow != null ? crossbow : net.minecraft.init.Items.BOW));
        return livingdata;
    }

    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor)
    {
        EntityTippedArrow arrow = new EntityTippedArrow(this.world, this);
        double dx = target.posX - this.posX;
        double dy = target.getEntityBoundingBox().minY + (double)(target.height / 3.0F) - arrow.posY;
        double dz = target.posZ - this.posZ;
        double d = (double)MathHelper.sqrt(dx * dx + dz * dz);
        arrow.setDamage(3.0D + this.world.getDifficulty().getDifficultyId() * 0.5D);
        arrow.setThrowableHeading(dx, dy + d * 0.2D, dz, 1.9F, (float)(14 - this.world.getDifficulty().getDifficultyId() * 4));
        this.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 1.2F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        this.world.spawnEntity(arrow);
    }

    public void setSwingingArms(boolean swingingArms)
    {
        this.setAggressive(1, swingingArms);
    }

    public boolean isCharging()
    {
        return this.isAggressive(1);
    }

    public AbstractIllager.IllagerArmPose getArmPose()
    {
        return this.isAggressive(1) || this.getAttackTarget() != null ? AbstractIllager.IllagerArmPose.BOW_AND_ARROW : AbstractIllager.IllagerArmPose.CROSSED;
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
        MobUtil.drop(this, "arrow", 0, 2, lootingModifier);

        if (wasRecentlyHit && this.rand.nextInt(12) == 0)
        {
            MobUtil.drop(this, "crossbow", 1, 1, 0);
        }

        if (this.rand.nextInt(3) == 0)
        {
            MobUtil.drop(this, "emerald", 1, 1, lootingModifier);
        }
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.VINDICATION_ILLAGER_AMBIENT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.VINDICATION_ILLAGER_DEATH;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_VINDICATION_ILLAGER_HURT;
    }
}
