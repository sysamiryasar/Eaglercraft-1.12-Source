package net.sythix.modern.mob;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Salmon: a river fish that swims upstream. */
public class Salmon extends ModernFish
{
    public Salmon(World worldIn)
    {
        super(worldIn);
        this.setSize(0.7F, 0.4F);
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        this.entityDropItem(new ItemStack(this.isBurning() ? Items.COOKED_FISH : Items.FISH, 1, 1), 0.0F);

        if (this.rand.nextInt(20) == 0)
        {
            this.entityDropItem(new ItemStack(Items.DYE, 1, 15), 0.0F);
        }
    }
}
