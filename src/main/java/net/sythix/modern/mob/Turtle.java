package net.sythix.modern.mob;

import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/** Turtle: a slow beach animal that swims well. Babies drop a turtle scute when they grow up. */
public class Turtle extends ModernAnimal
{
    public Turtle(World worldIn)
    {
        super(worldIn);
        this.setSize(1.2F, 0.4F);
    }

    protected double maxHealth()
    {
        return 30.0D;
    }

    protected double speed()
    {
        return 0.12D;
    }

    protected String food()
    {
        return "kelp";
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (this.isInWater() && !this.world.isRemote)
        {
            // turtles are fast swimmers
            this.motionX *= 1.15D;
            this.motionZ *= 1.15D;
        }
    }

    public boolean canBreatheUnderwater()
    {
        return true;
    }

    protected void onGrowingAdult()
    {
        super.onGrowingAdult();

        if (!this.world.isRemote)
        {
            MobUtil.drop(this, "turtle_scute", 1, 1, 0);
        }
    }

    public boolean getCanSpawnHere()
    {
        return this.canSpawnOn(Blocks.SAND);
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, "seagrass", 0, 2, lootingModifier);
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_GENERIC_SWIM;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_GENERIC_SWIM;
    }
}
