package net.sythix.modern.mob;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/** Panda: a lazy jungle bear that loves bamboo. Babies sometimes sneeze out a slime ball. */
public class Panda extends ModernAnimal
{
    public Panda(World worldIn)
    {
        super(worldIn);
        this.setSize(1.3F, 1.25F);
    }

    protected double maxHealth()
    {
        return 20.0D;
    }

    protected double speed()
    {
        return 0.15D;
    }

    protected String food()
    {
        return "bamboo";
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (!this.world.isRemote && this.isChild() && this.rand.nextInt(6000) == 0)
        {
            this.playSound(SoundEvents.ENTITY_POLAR_BEAR_BABY_AMBIENT, 1.0F, 1.6F);
            MobUtil.drop(this, "slime_ball", 1, 1, 0);
        }
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, "bamboo", 0, 2, lootingModifier);
    }

    protected SoundEvent getAmbientSound()
    {
        return this.isChild() ? SoundEvents.ENTITY_POLAR_BEAR_BABY_AMBIENT : SoundEvents.ENTITY_POLAR_BEAR_AMBIENT;
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
        return 1.3F;
    }
}
