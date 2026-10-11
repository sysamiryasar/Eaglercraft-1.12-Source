package net.sythix.modern.mob;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Tropical fish: a colourful reef fish. */
public class TropicalFish extends ModernFish
{
    public TropicalFish(World worldIn)
    {
        super(worldIn);
        this.setSize(0.5F, 0.4F);
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        this.entityDropItem(new ItemStack(Items.FISH, 1, 2), 0.0F);

        if (this.rand.nextInt(20) == 0)
        {
            this.entityDropItem(new ItemStack(Items.DYE, 1, 15), 0.0F);
        }
    }
}
