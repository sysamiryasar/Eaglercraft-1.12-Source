package net.sythix.client.module.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.StyledHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.TextSetting;

/**
 * #23 Counts pearls, arrows, blocks, food, potions and any item you choose.
 */
public class ItemCounters extends StyledHudModule
{
    private final BooleanSetting arrows;
    private final BooleanSetting pearls;
    private final BooleanSetting gapples;
    private final BooleanSetting blocks;
    private final BooleanSetting food;
    private final BooleanSetting potions;
    private final BooleanSetting totems;
    private final TextSetting custom;
    private final BooleanSetting hideZero;
    private final ModeSetting layout;

    public ItemCounters()
    {
        super("Item Counters", "Counts pearls, arrows, blocks, food, potions and a custom item.", Category.INVENTORY, false, 0.995D, 0.4D);
        this.arrows = this.add(new BooleanSetting("Arrows", "All arrow types.", true));
        this.pearls = this.add(new BooleanSetting("Ender Pearls", "Ender pearls.", true));
        this.gapples = this.add(new BooleanSetting("Golden Apples", "Golden apples.", true));
        this.blocks = this.add(new BooleanSetting("Blocks", "Placeable blocks.", false));
        this.food = this.add(new BooleanSetting("Food", "Any food.", false));
        this.potions = this.add(new BooleanSetting("Potions", "Drinkable, splash and lingering potions.", false));
        this.totems = this.add(new BooleanSetting("Totems", "Totems of undying.", false));
        this.custom = this.add(new TextSetting("Custom Item", "Item id to count, for example minecraft:tnt (empty = off).", "", 48));
        this.hideZero = this.add(new BooleanSetting("Hide Zero", "Hide items you have none of.", true));
        this.layout = this.add(new ModeSetting("Layout", "Stack vertically or in a row.", "Vertical", "Vertical", "Horizontal"));
    }

    private interface Match
    {
        boolean test(ItemStack stack);
    }

    private int count(Match match)
    {
        if (this.mc.player == null)
        {
            return 0;
        }

        int total = 0;

        for (ItemStack stack : this.mc.player.inventory.mainInventory)
        {
            if (!stack.isEmpty() && match.test(stack))
            {
                total += stack.getCount();
            }
        }

        for (ItemStack stack : this.mc.player.inventory.offHandInventory)
        {
            if (!stack.isEmpty() && match.test(stack))
            {
                total += stack.getCount();
            }
        }

        return total;
    }

    private List<Object[]> entries(boolean preview)
    {
        List<Object[]> list = new ArrayList<Object[]>();

        if (this.arrows.isOn())
        {
            list.add(new Object[] {new ItemStack(Items.ARROW), this.count(s -> s.getItem() == Items.ARROW || s.getItem() == Items.TIPPED_ARROW || s.getItem() == Items.SPECTRAL_ARROW)});
        }

        if (this.pearls.isOn())
        {
            list.add(new Object[] {new ItemStack(Items.ENDER_PEARL), this.count(s -> s.getItem() == Items.ENDER_PEARL)});
        }

        if (this.gapples.isOn())
        {
            list.add(new Object[] {new ItemStack(Items.GOLDEN_APPLE), this.count(s -> s.getItem() == Items.GOLDEN_APPLE)});
        }

        if (this.blocks.isOn())
        {
            list.add(new Object[] {new ItemStack(Blocks.COBBLESTONE), this.count(s -> s.getItem() instanceof ItemBlock)});
        }

        if (this.food.isOn())
        {
            list.add(new Object[] {new ItemStack(Items.COOKED_BEEF), this.count(s -> s.getItem() instanceof ItemFood)});
        }

        if (this.potions.isOn())
        {
            list.add(new Object[] {new ItemStack(Items.SPLASH_POTION), this.count(s -> s.getItem() == Items.POTIONITEM || s.getItem() == Items.SPLASH_POTION || s.getItem() == Items.LINGERING_POTION)});
        }

        if (this.totems.isOn())
        {
            list.add(new Object[] {new ItemStack(Items.TOTEM_OF_UNDYING), this.count(s -> s.getItem() == Items.TOTEM_OF_UNDYING)});
        }

        String id = this.custom.get().trim();

        if (!id.isEmpty())
        {
            Item item = Item.getByNameOrId(id);

            if (item != null)
            {
                list.add(new Object[] {new ItemStack(item), this.count(s -> s.getItem() == item)});
            }
        }

        if (this.hideZero.isOn() && !preview)
        {
            list.removeIf(e -> ((Integer)e[1]).intValue() == 0);
        }

        return list;
    }

    public boolean shouldRender()
    {
        return !this.entries(false).isEmpty();
    }

    private int entryWidth(Object[] e)
    {
        return 18 + this.mc.fontRenderer.getStringWidth(String.valueOf(e[1]));
    }

    public int getWidth()
    {
        int w = 0;

        for (Object[] e : this.entries(true))
        {
            w = this.layout.is("Horizontal") ? w + this.entryWidth(e) + 4 : Math.max(w, this.entryWidth(e));
        }

        return Math.max(18, w) + this.getPad() * 2;
    }

    public int getHeight()
    {
        int n = Math.max(1, this.entries(true).size());
        return (this.layout.is("Horizontal") ? 16 : n * 17 - 1) + this.getPad() * 2;
    }

    protected void renderContent(boolean preview)
    {
        this.drawBox(this.getWidth(), this.getHeight());
        int x = this.getPad();
        int y = this.getPad();

        for (Object[] e : this.entries(preview))
        {
            this.drawItem((ItemStack)e[0], x, y, false);
            this.drawText(String.valueOf(e[1]), (float)(x + 18), (float)(y + 4), this.valueColor.getColor());

            if (this.layout.is("Horizontal"))
            {
                x += this.entryWidth(e) + 4;
            }
            else
            {
                y += 17;
            }
        }
    }
}
