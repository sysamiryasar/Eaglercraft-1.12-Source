package net.sythix.modern.mob;

import java.util.List;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/**
 * Goat: a mountain animal that jumps high, ignores short falls and now and then rams a nearby mob
 * or player. Milk it with a bucket; a ram that misses can knock off a goat horn.
 */
public class Goat extends EntityCow
{
    private int ramCooldown = 600;
    private EntityLivingBase ramTarget;
    private int ramTicks;

    public Goat(World worldIn)
    {
        super(worldIn);
        this.setSize(0.9F, 1.3F);
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
    }

    protected float getJumpUpwardsMotion()
    {
        return 0.6F;
    }

    public void fall(float distance, float damageMultiplier)
    {
        super.fall(distance - 10.0F, damageMultiplier);
    }

    protected void updateAITasks()
    {
        super.updateAITasks();

        if (this.isChild())
        {
            return;
        }

        if (this.ramTarget != null)
        {
            this.getNavigator().tryMoveToEntityLiving(this.ramTarget, 2.0D);

            if (this.getEntityBoundingBox().grow(0.4D).intersects(this.ramTarget.getEntityBoundingBox()))
            {
                this.ramTarget.attackEntityFrom(DamageSource.causeMobDamage(this), 2.0F);
                this.ramTarget.knockBack(this, 2.0F, this.posX - this.ramTarget.posX, this.posZ - this.ramTarget.posZ);
                this.playSound(SoundEvents.ENTITY_SHEEP_HURT, 1.0F, 0.6F);
                this.ramTarget = null;
            }
            else if (++this.ramTicks > 60 || this.isCollidedHorizontally)
            {
                if (this.isCollidedHorizontally && this.rand.nextInt(4) == 0)
                {
                    MobUtil.drop(this, "goat_horn", 1, 1, 0);
                }

                this.ramTarget = null;
            }

            return;
        }

        if (--this.ramCooldown <= 0)
        {
            this.ramCooldown = 600 + this.rand.nextInt(5400);
            List<EntityLivingBase> list = this.world.<EntityLivingBase>getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(10.0D, 3.0D, 10.0D),
                e -> e != this && !(e instanceof Goat) && !(e instanceof net.minecraft.entity.item.EntityArmorStand) && !(e instanceof EntityPlayer && (((EntityPlayer)e).capabilities.isCreativeMode || ((EntityPlayer)e).isSpectator())));

            if (!list.isEmpty())
            {
                this.ramTarget = list.get(this.rand.nextInt(list.size()));
                this.ramTicks = 0;
                this.playSound(SoundEvents.ENTITY_SHEEP_AMBIENT, 1.0F, 0.6F);
            }
        }
    }

    public Goat createChild(EntityAgeable ageable)
    {
        return new Goat(this.world);
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
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_SHEEP_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_SHEEP_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_SHEEP_DEATH;
    }

    protected float getSoundPitch()
    {
        return 0.8F;
    }
}
