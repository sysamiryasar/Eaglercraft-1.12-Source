package net.sythix.modern.mob;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.world.World;

/** Zoglin: a zombified hoglin that attacks almost every living thing. */
public class Zoglin extends Hoglin
{
    public Zoglin(World worldIn)
    {
        super(worldIn);
    }

    protected void addTargets()
    {
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityLivingBase>(this, EntityLivingBase.class, 10, true, false,
            e -> !(e instanceof Zoglin) && !(e instanceof EntityCreeper) && !(e instanceof EntityArmorStand)));
    }

    public EnumCreatureAttribute getCreatureAttribute()
    {
        return EnumCreatureAttribute.UNDEAD;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, "rotten_flesh", 1, 3, lootingModifier);
    }
}
