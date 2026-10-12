package net.minecraft.item;

import net.minecraft.block.Block;

/**
 * Lets Sythix code register item forms of the extra 1.21 blocks.
 */
public final class SythixItemAccess
{
    private SythixItemAccess()
    {
    }

    public static void registerItemBlock(Block block, Item item)
    {
        Item.registerItemBlock(block, item);
    }
}
