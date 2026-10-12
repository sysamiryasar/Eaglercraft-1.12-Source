package net.sythix.modern.mob;

import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/** Zombie nautilus: an undead sea creature that attacks swimmers with its beam. */
public class ZombieNautilus extends EntityGuardian
{
    public ZombieNautilus(World worldIn)
    {
        super(worldIn);
        this.experienceValue = 5;
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
    }

    public EnumCreatureAttribute getCreatureAttribute()
    {
        return EnumCreatureAttribute.UNDEAD;
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
        MobUtil.drop(this, "rotten_flesh", 0, 2, lootingModifier);

        if (wasRecentlyHit && this.rand.nextInt(4) == 0)
        {
            MobUtil.drop(this, "nautilus_shell", 1, 1, 0);
        }
    }
}
