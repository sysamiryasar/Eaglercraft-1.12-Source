package net.sythix.modern.mob;

import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/** Bogged: a mossy swamp skeleton whose arrows poison. */
public class Bogged extends EntitySkeleton
{
    public Bogged(World worldIn)
    {
        super(worldIn);
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MAX_HEALTH).setBaseValue(16.0D);
    }

    protected EntityArrow getArrow(float distanceFactor)
    {
        EntityArrow arrow = super.getArrow(distanceFactor);

        if (arrow instanceof EntityTippedArrow)
        {
            ((EntityTippedArrow)arrow).addEffect(new PotionEffect(MobEffects.POISON, 80));
        }

        return arrow;
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
        MobUtil.drop(this, "bone", 0, 2, lootingModifier);
        MobUtil.drop(this, "arrow", 0, 2, lootingModifier);
        MobUtil.drop(this, this.rand.nextBoolean() ? "red_mushroom" : "brown_mushroom", 0, 1, 0);
    }
}
