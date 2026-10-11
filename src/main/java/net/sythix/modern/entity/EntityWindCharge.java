package net.sythix.modern.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * A wind charge: bursts on impact and pushes nearby entities (including the thrower) away, without
 * breaking blocks. Players use it to jump very high.
 */
public class EntityWindCharge extends net.minecraft.entity.projectile.EntityThrowable implements ModernProjectile
{
    public EntityWindCharge(World worldIn)
    {
        super(worldIn);
    }

    public EntityWindCharge(World worldIn, double x, double y, double z)
    {
        super(worldIn, x, y, z);
    }

    public EntityWindCharge(World worldIn, EntityLivingBase thrower)
    {
        super(worldIn, thrower);
    }

    public int getSpawnType()
    {
        return WIND_CHARGE;
    }

    protected float getGravityVelocity()
    {
        return 0.0F;
    }

    public void onUpdate()
    {
        super.onUpdate();

        // Bursts by itself after 5 seconds in the air
        if (!this.world.isRemote && this.ticksExisted > 100)
        {
            this.burst(null);
        }
    }

    protected void onImpact(RayTraceResult result)
    {
        if (!this.world.isRemote)
        {
            this.burst(result.entityHit);
        }
    }

    /** Bursts right here (used by the Wind Charged effect when a mob dies). */
    public void burstNow()
    {
        this.burst(null);
    }

    private void burst(Entity direct)
    {
        if (direct != null)
        {
            direct.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 1.0F);
        }

        for (Entity e : this.world.getEntitiesWithinAABBExcludingEntity(this, new AxisAlignedBB(this.getPosition()).grow(2.5D)))
        {
            double dx = e.posX - this.posX;
            double dy = e.posY + e.getEyeHeight() * 0.5D - this.posY;
            double dz = e.posZ - this.posZ;
            double dist = Math.max(0.5D, Math.sqrt(dx * dx + dy * dy + dz * dz));
            double power = (1.0D - Math.min(1.0D, dist / 3.0D)) * 1.6D;
            e.motionX += dx / dist * power;
            e.motionY += Math.max(0.4D, dy / dist) * power;
            e.motionZ += dz / dist * power;
            e.velocityChanged = true;
            e.fallDistance = 0.0F;
        }

        this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 0.5F, 1.6F);

        if (this.world instanceof WorldServer)
        {
            ((WorldServer)this.world).spawnParticle(EnumParticleTypes.CLOUD, this.posX, this.posY, this.posZ, 20, 0.6D, 0.6D, 0.6D, 0.05D);
        }

        this.setDead();
    }
}
