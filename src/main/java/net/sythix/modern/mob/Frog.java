package net.sythix.modern.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/**
 * Frog: hops around swamps and eats small slimes and magma cubes. A magma cube eaten by a frog
 * leaves a froglight behind. Breed frogs with slime balls.
 */
public class Frog extends EntityRabbit
{
    public Frog(World worldIn)
    {
        super(worldIn);
        this.setSize(0.5F, 0.5F);
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(1, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAIPanic(this, 2.0D));
        this.tasks.addTask(2, new EntityAIMate(this, 0.8D));
        this.tasks.addTask(3, new EntityAITempt(this, 1.0D, Items.SLIME_BALL, false));
        this.tasks.addTask(4, new EntityAIAttackMelee(this, 1.4D, true));
        this.tasks.addTask(6, new EntityAIWanderAvoidWater(this, 0.6D));
        this.tasks.addTask(11, new EntityAIWatchClosest(this, EntityPlayer.class, 10.0F));
        this.targetTasks.addTask(1, new EntityAINearestAttackableTarget<EntitySlime>(this, EntitySlime.class, 10, true, false, s -> s.getSlimeSize() <= 1));
    }

    public boolean attackEntityAsMob(Entity entityIn)
    {
        boolean hit = entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), 10.0F);

        if (hit && !entityIn.isEntityAlive())
        {
            this.playSound(SoundEvents.ENTITY_SLIME_SQUISH, 1.0F, 1.4F);

            if (entityIn instanceof EntityMagmaCube)
            {
                String[] lights = new String[] {"ochre_froglight", "verdant_froglight", "pearlescent_froglight"};
                MobUtil.drop(entityIn, lights[this.rand.nextInt(lights.length)], 1, 1, 0);
            }
            else
            {
                MobUtil.drop(entityIn, "slime_ball", 1, 1, 0);
            }
        }

        return hit;
    }

    public boolean isBreedingItem(ItemStack stack)
    {
        return stack.getItem() == Items.SLIME_BALL;
    }

    /** Frogs lay frogspawn; here the tadpoles hatch right away. */
    public Frog createChild(EntityAgeable ageable)
    {
        if (!this.world.isRemote)
        {
            for (int i = 0; i < 2 + this.rand.nextInt(3); ++i)
            {
                Tadpole t = new Tadpole(this.world);
                t.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rand.nextFloat() * 360.0F, 0.0F);
                this.world.spawnEntity(t);
            }
        }

        Frog baby = new Frog(this.world);
        baby.setGrowingAge(-24000);
        return baby;
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
        return SoundEvents.ENTITY_SLIME_JUMP;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_SLIME_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_SLIME_DEATH;
    }

    protected float getSoundPitch()
    {
        return 1.6F;
    }
}
