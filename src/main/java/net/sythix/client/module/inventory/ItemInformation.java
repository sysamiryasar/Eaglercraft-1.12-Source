package net.sythix.client.module.inventory;

import java.util.List;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #24 Durability, enchantment and item details in tooltips, plus held-item name time and
 * enchantment glint colour.
 */
public class ItemInformation extends Module
{
    private final BooleanSetting durability;
    private final BooleanSetting numericLevels;
    private final BooleanSetting itemId;
    private final BooleanSetting repairCost;
    private final BooleanSetting stackTotal;
    private final NumberSetting heldNameTime;
    private final BooleanSetting customGlint;
    private final ColorSetting glintColor;
    private final ColorSetting armorGlintColor;

    public ItemInformation()
    {
        super("Item Information", "Durability, enchantments and item details in tooltips, plus glint colour.", Category.INVENTORY, true);
        this.durability = this.add(new BooleanSetting("Durability Line", "Add \"Durability: 120 / 250\" to tooltips.", true));
        this.numericLevels = this.add(new BooleanSetting("Number Levels", "Show enchantment levels as numbers (Sharpness 5).", false));
        this.itemId = this.add(new BooleanSetting("Item ID", "Show the item id, for example minecraft:diamond_sword.", false));
        this.repairCost = this.add(new BooleanSetting("Repair Cost", "Show the anvil repair cost.", false));
        this.stackTotal = this.add(new BooleanSetting("Inventory Total", "Show how many of the item you carry in total.", false));
        this.heldNameTime = this.add(new NumberSetting("Held Name Time", "How long the item name shows when you switch items.", 2.0D, 0.0D, 6.0D, 0.5D, "s"));
        this.customGlint = this.add(new BooleanSetting("Custom Glint", "Change the colour of the enchantment shine.", false));
        this.glintColor = this.add(new ColorSetting("Item Glint", "Glint colour on items.", 0xFF8040CC, false)).visibleWhen(this.customGlint::isOn);
        this.armorGlintColor = this.add(new ColorSetting("Armor Glint", "Glint colour on worn armor.", 0xFF6130A0, false)).visibleWhen(this.customGlint::isOn);
    }

    public void modifyTooltip(ItemStack stack, List<String> lines)
    {
        if (this.numericLevels.isOn())
        {
            for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet())
            {
                String roman = entry.getKey().getTranslatedName(entry.getValue().intValue());

                for (int i = 0; i < lines.size(); ++i)
                {
                    if (TextFormatting.getTextWithoutFormattingCodes(lines.get(i)).equals(TextFormatting.getTextWithoutFormattingCodes(roman)) && entry.getKey().getMaxLevel() > 1)
                    {
                        String color = lines.get(i).length() > 1 && lines.get(i).charAt(0) == '§' ? lines.get(i).substring(0, 2) : "";
                        lines.set(i, color + net.minecraft.client.resources.I18n.format(entry.getKey().getName()) + " " + entry.getValue());
                    }
                }
            }
        }

        if (this.durability.isOn() && stack.isItemStackDamageable())
        {
            int left = stack.getMaxDamage() - stack.getItemDamage();
            lines.add(TextFormatting.GRAY + "Durability: " + (left * 4 < stack.getMaxDamage() ? TextFormatting.RED : TextFormatting.GREEN) + left + TextFormatting.GRAY + " / " + stack.getMaxDamage());
        }

        if (this.repairCost.isOn() && stack.getRepairCost() > 0)
        {
            lines.add(TextFormatting.GRAY + "Repair cost: " + stack.getRepairCost());
        }

        if (this.stackTotal.isOn() && this.mc.player != null)
        {
            int total = 0;

            for (ItemStack s : this.mc.player.inventory.mainInventory)
            {
                if (ItemStack.areItemsEqualIgnoreDurability(s, stack))
                {
                    total += s.getCount();
                }
            }

            lines.add(TextFormatting.GRAY + "In inventory: " + total);
        }

        if (this.itemId.isOn() && net.minecraft.item.Item.REGISTRY.getNameForObject(stack.getItem()) != null)
        {
            lines.add(TextFormatting.DARK_GRAY + net.minecraft.item.Item.REGISTRY.getNameForObject(stack.getItem()).toString());
        }
    }

    public int getHeldNameTicks()
    {
        return Math.round(this.heldNameTime.getFloat() * 20.0F);
    }

    /** Item glint colour (ARGB), or the vanilla value when custom glint is off. */
    public int itemGlint(int vanilla)
    {
        return this.customGlint.isOn() ? this.glintColor.getColor() : vanilla;
    }

    /** Armor glint colour as {r, g, b}, or null for vanilla. */
    public float[] armorGlint()
    {
        if (!this.customGlint.isOn())
        {
            return null;
        }

        int c = this.armorGlintColor.getColor();
        return new float[] {(float)((c >> 16) & 255) / 255.0F, (float)((c >> 8) & 255) / 255.0F, (float)(c & 255) / 255.0F};
    }
}
