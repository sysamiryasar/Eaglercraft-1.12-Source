package net.sythix.modern;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

/**
 * Furnace recipes for the extra 1.21 content (crafting recipes are JSON files in
 * assets/minecraft/recipes/sythix_*.json).
 */
public final class ModernRecipes
{
    private ModernRecipes()
    {
    }

    private static void smelt(FurnaceRecipes r, String input, ItemStack output, float xp)
    {
        Item in = ModernBlocks.item(input);

        if (in != null && !output.isEmpty())
        {
            r.addSmelting(in, output, xp);
        }
    }

    private static ItemStack stack(String name)
    {
        Item item = ModernBlocks.item(name);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static void addSmelting(FurnaceRecipes r)
    {
        smelt(r, "raw_iron", new ItemStack(Items.IRON_INGOT), 0.7F);
        smelt(r, "raw_gold", new ItemStack(Items.GOLD_INGOT), 1.0F);
        smelt(r, "raw_copper", stack("copper_ingot"), 0.7F);
        smelt(r, "copper_ore", stack("copper_ingot"), 0.7F);
        smelt(r, "deepslate_copper_ore", stack("copper_ingot"), 0.7F);
        smelt(r, "deepslate_iron_ore", new ItemStack(Items.IRON_INGOT), 0.7F);
        smelt(r, "deepslate_gold_ore", new ItemStack(Items.GOLD_INGOT), 1.0F);
        smelt(r, "deepslate_coal_ore", new ItemStack(Items.COAL), 0.1F);
        smelt(r, "deepslate_diamond_ore", new ItemStack(Items.DIAMOND), 1.0F);
        smelt(r, "deepslate_emerald_ore", new ItemStack(Items.EMERALD), 1.0F);
        smelt(r, "deepslate_redstone_ore", new ItemStack(Items.REDSTONE), 0.7F);
        smelt(r, "deepslate_lapis_ore", new ItemStack(Items.DYE, 1, 4), 0.2F);
        smelt(r, "nether_gold_ore", new ItemStack(Items.GOLD_INGOT), 1.0F);
        smelt(r, "ancient_debris", stack("netherite_scrap"), 2.0F);
        smelt(r, "cobbled_deepslate", stack("deepslate"), 0.1F);
        smelt(r, "mangrove_log", new ItemStack(Items.COAL, 1, 1), 0.15F);
        smelt(r, "cherry_log", new ItemStack(Items.COAL, 1, 1), 0.15F);
        smelt(r, "pale_oak_log", new ItemStack(Items.COAL, 1, 1), 0.15F);
        smelt(r, "sculk", ItemStack.EMPTY, 0.0F);
        r.addSmeltingRecipe(new ItemStack(Blocks.QUARTZ_BLOCK, 1, 0), stack("smooth_quartz"), 0.1F);
        smelt(r, "kelp", stack("dried_kelp"), 0.1F);
    }
}
