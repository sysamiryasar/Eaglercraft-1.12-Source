package net.sythix.modern.mob;

import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * Axolotl: hunts fish, drowned and guardians under water. A player nearby when it wins a fight
 * gets Regeneration.
 */
public class Axolotl extends ModernFish
{
    private EntityLivingBase prey;
    private int attackCooldown;

    public Axolotl(World worldIn)
    {
        super(worldIn);
        this.setSize(0.75F, 0.42F);
    }

    protected double maxHealth()
    {
        return 14.0D;
    }

    protected double swimSpeed()
    {
        return 0.16D;
    }

    protected boolean shy()
    {
        return false;
    }

    private static boolean isPrey(EntityLivingBase e)
    {
        return e instanceof Cod || e instanceof Salmon || e instanceof TropicalFish || e instanceof Pufferfish || e instanceof Drowned || e instanceof EntityGuardian;
    }

    protected boolean swimTowardsTarget()
    {
        if (this.attackCooldown > 0)
        {
            --this.attackCooldown;
        }

        if (this.prey == null || !this.prey.isEntityAlive() || this.getDistanceSqToEntity(this.prey) > 256.0D)
        {
            this.prey = null;

            if (this.ticksExisted % 20 == 0)
            {
                List<EntityLivingBase> list = this.world.<EntityLivingBase>getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(8.0D), e -> isPrey(e) && e.isInWater());
                this.prey = list.isEmpty() ? null : list.get(0);
            }
        }

        if (this.prey == null)
        {
            return false;
        }

        MobUtil.flyTowards(this, this.prey.posX, this.prey.posY + this.prey.height * 0.5D, this.prey.posZ, this.swimSpeed() * 1.4D);

        if (this.attackCooldown <= 0 && this.getEntityBoundingBox().grow(0.5D).intersects(this.prey.getEntityBoundingBox()))
        {
            this.attackCooldown = 20;
            this.prey.attackEntityFrom(DamageSource.causeMobDamage(this), 2.0F);

            if (!this.prey.isEntityAlive())
            {
                EntityPlayer p = this.world.getClosestPlayerToEntity(this, 20.0D);

                if (p != null)
                {
                    p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 100, 0));
                }
            }
        }

        return true;
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && this.posY < (double)this.world.getSeaLevel() && super.getCanSpawnHere();
    }

    protected boolean canDespawn()
    {
        return false;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
    }
}
