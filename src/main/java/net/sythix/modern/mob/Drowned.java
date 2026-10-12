package net.sythix.modern.mob;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.entity.monster.EntityZombie;

/** Drowned: an underwater zombie. Some carry a trident or a nautilus shell. */
public class Drowned extends EntityZombie
{
    public Drowned(World worldIn)
    {
        super(worldIn);
    }

    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata)
    {
        livingdata = super.onInitialSpawn(difficulty, livingdata);
        float r = this.rand.nextFloat();
        Item trident = MobUtil.item("trident");
        Item shell = MobUtil.item("nautilus_shell");

        if (r < 0.06F && trident != null)
        {
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(trident));
        }
        else if (r < 0.09F && shell != null)
        {
            this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(shell));
        }

        return livingdata;
    }

    /** Drowned breathe under water and swim towards their target. */
    public boolean canBreatheUnderwater()
    {
        return true;
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (!this.world.isRemote && this.isInWater())
        {
            EntityLivingBase target = this.getAttackTarget();

            if (target != null)
            {
                double dy = target.posY - this.posY;
                this.motionY += Math.signum(dy) * 0.02D;
            }
            else if (this.rand.nextInt(40) == 0)
            {
                this.motionY -= 0.02D;
            }
        }
    }

    protected boolean shouldBurnInDay()
    {
        return !this.isInWater();
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && this.rand.nextInt(3) == 0 && super.getCanSpawnHere();
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, "rotten_flesh", 0, 2, lootingModifier);

        if (wasRecentlyHit && this.rand.nextInt(9) == 0)
        {
            MobUtil.drop(this, "copper_ingot", 1, 1, 0);
        }
    }

    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        return source != DamageSource.DROWN && super.attackEntityFrom(source, amount);
    }
}
