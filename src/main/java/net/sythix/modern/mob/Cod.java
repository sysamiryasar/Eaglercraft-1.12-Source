package net.sythix.modern.mob;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Cod: a common fish. */
public class Cod extends ModernFish
{
    public Cod(World worldIn)
    {
        super(worldIn);
        this.setSize(0.5F, 0.3F);
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        this.entityDropItem(new ItemStack(this.isBurning() ? Items.COOKED_FISH : Items.FISH, 1, 0), 0.0F);

        if (this.rand.nextInt(20) == 0)
        {
            this.entityDropItem(new ItemStack(Items.DYE, 1, 15), 0.0F);
        }
    }
}
