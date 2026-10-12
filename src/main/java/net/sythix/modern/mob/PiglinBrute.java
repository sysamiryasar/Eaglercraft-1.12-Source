package net.sythix.modern.mob;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

/** Piglin brute: a bastion guard with a golden axe. It never barters and attacks every player. */
public class PiglinBrute extends Piglin
{
    public PiglinBrute(World worldIn)
    {
        super(worldIn);
        this.experienceValue = 20;
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(50.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(7.0D);
    }

    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata)
    {
        livingdata = super.onInitialSpawn(difficulty, livingdata);
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        return livingdata;
    }

    protected boolean ignores(EntityPlayer player)
    {
        return false;
    }

    protected boolean canBarter()
    {
        return false;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        if (wasRecentlyHit && this.rand.nextInt(12) == 0)
        {
            MobUtil.drop(this, "golden_axe", 1, 1, 0);
        }
    }
}
