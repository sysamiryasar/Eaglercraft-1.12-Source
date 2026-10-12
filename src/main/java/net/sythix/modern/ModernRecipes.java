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

    public static void addCrafting()
    {
        String[] parts = new String[] {"sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots"};

        for (String part : parts)
        {
            Item from = ModernBlocks.item("diamond_" + part);
            Item to = ModernBlocks.item("netherite_" + part);

            if (from != null && to != null)
            {
                net.minecraft.item.crafting.CraftingManager.register("sythix_netherite_" + part, new NetheriteUpgrade(from, to));
            }
        }
    }

    /** Diamond gear + netherite ingot = netherite gear, keeping enchantments, name and damage. */
    static class NetheriteUpgrade implements net.minecraft.item.crafting.IRecipe
    {
        private final Item from;
        private final Item to;

        NetheriteUpgrade(Item fromItem, Item toItem)
        {
            this.from = fromItem;
            this.to = toItem;
        }

        private ItemStack find(net.minecraft.inventory.InventoryCrafting inv, boolean gear)
        {
            ItemStack found = ItemStack.EMPTY;
            int count = 0;

            for (int i = 0; i < inv.getSizeInventory(); ++i)
            {
                ItemStack s = inv.getStackInSlot(i);

                if (!s.isEmpty())
                {
                    ++count;

                    if (gear ? s.getItem() == this.from : s.getItem() == ModernBlocks.item("netherite_ingot"))
                    {
                        found = s;
                    }
                }
            }

            return count == 2 ? found : ItemStack.EMPTY;
        }

        public boolean matches(net.minecraft.inventory.InventoryCrafting inv, net.minecraft.world.World worldIn)
        {
            return !this.find(inv, true).isEmpty() && !this.find(inv, false).isEmpty();
        }

        public ItemStack getCraftingResult(net.minecraft.inventory.InventoryCrafting inv)
        {
            ItemStack gear = this.find(inv, true);
            ItemStack out = new ItemStack(this.to);

            if (gear.hasTagCompound())
            {
                out.setTagCompound(gear.getTagCompound().copy());
            }

            out.setItemDamage(Math.min(gear.getItemDamage(), out.getMaxDamage() - 1));
            return out;
        }

        public boolean canFit(int width, int height)
        {
            return width * height >= 2;
        }

        public ItemStack getRecipeOutput()
        {
            return new ItemStack(this.to);
        }

        public net.minecraft.util.NonNullList<ItemStack> getRemainingItems(net.minecraft.inventory.InventoryCrafting inv)
        {
            return net.minecraft.util.NonNullList.<ItemStack>withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        }

        public boolean isHidden()
        {
            return true;
        }
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
