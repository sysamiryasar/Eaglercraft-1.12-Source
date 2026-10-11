package net.sythix.modern.mob;

import net.minecraft.block.material.Material;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Base of the new swimming mobs: they swim to random spots in the water, flee from nearby players
 * and flop around on land.
 */
public abstract class ModernFish extends EntityWaterMob
{
    private double tx;
    private double ty;
    private double tz;
    private int timer;

    public ModernFish(World worldIn)
    {
        super(worldIn);
        this.setSize(0.5F, 0.4F);
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(this.maxHealth());
    }

    protected double maxHealth()
    {
        return 3.0D;
    }

    /** Blocks per tick when swimming. */
    protected double swimSpeed()
    {
        return 0.12D;
    }

    /** Whether the fish swims away from players. */
    protected boolean shy()
    {
        return true;
    }

    protected void updateAITasks()
    {
        super.updateAITasks();

        if (!this.isInWater())
        {
            if (this.onGround && this.rand.nextInt(12) == 0)
            {
                this.motionY = 0.35D;
                this.motionX += (this.rand.nextDouble() - 0.5D) * 0.3D;
                this.motionZ += (this.rand.nextDouble() - 0.5D) * 0.3D;
                this.playSound(SoundEvents.ENTITY_GUARDIAN_FLOP, 0.5F, 1.6F);
            }

            return;
        }

        if (this.swimTowardsTarget())
        {
            return;
        }

        EntityPlayer player = this.shy() ? this.world.getClosestPlayerToEntity(this, 4.0D) : null;

        if (player != null && !player.isSpectator())
        {
            this.tx = this.posX + (this.posX - player.posX) * 2.0D;
            this.ty = this.posY;
            this.tz = this.posZ + (this.posZ - player.posZ) * 2.0D;
            this.timer = 20;
        }
        else if (--this.timer <= 0 || this.getDistanceSq(this.tx, this.ty, this.tz) < 1.0D)
        {
            this.timer = 60 + this.rand.nextInt(80);

            for (int i = 0; i < 10; ++i)
            {
                BlockPos p = new BlockPos(this.posX + this.rand.nextInt(13) - 6, this.posY + this.rand.nextInt(7) - 3, this.posZ + this.rand.nextInt(13) - 6);

                if (this.world.getBlockState(p).getMaterial() == Material.WATER)
                {
                    this.tx = p.getX() + 0.5D;
                    this.ty = p.getY() + 0.5D;
                    this.tz = p.getZ() + 0.5D;
                    break;
                }
            }
        }

        if (this.timer > 0 && (this.tx != 0.0D || this.tz != 0.0D))
        {
            MobUtil.flyTowards(this, this.tx, this.ty, this.tz, this.swimSpeed() * (player != null ? 1.8D : 1.0D));
        }
    }

    /** Lets a subclass chase something instead of wandering; returns true when it did. */
    protected boolean swimTowardsTarget()
    {
        return false;
    }

    public void travel(float strafe, float vertical, float forward)
    {
        if (this.isInWater())
        {
            MobUtil.flyTravel(this);
        }
        else
        {
            super.travel(0.0F, 0.0F, 0.0F);
        }
    }

    public boolean isPushedByWater()
    {
        return false;
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && this.posY > 40.0D && this.posY < (double)this.world.getSeaLevel() && this.world.getBlockState(new BlockPos(this)).getMaterial() == Material.WATER && super.getCanSpawnHere();
    }

    public int getMaxSpawnedInChunk()
    {
        return 6;
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected SoundEvent getAmbientSound()
    {
        return this.isInWater() ? null : SoundEvents.ENTITY_GUARDIAN_FLOP;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_GENERIC_SWIM;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_GENERIC_SWIM;
    }

    protected float getSoundVolume()
    {
        return 0.4F;
    }
}
