package net.sythix.client.module.inventory;

import net.minecraft.item.ItemStack;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.StyledHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #22 Shows your inventory on screen.
 */
public class InventoryHud extends StyledHudModule
{
    private final ModeSetting visibility;
    private final BooleanSetting showHotbar;
    private final BooleanSetting slotBackgrounds;
    private final ColorSetting slotColor;
    private final BooleanSetting showCounts;
    private final BooleanSetting hideEmptyRows;

    public InventoryHud()
    {
        super("Inventory HUD", "Shows your inventory on screen.", Category.INVENTORY, false, 0.5D, 0.02D);
        this.visibility = this.add(new ModeSetting("Show", "When the inventory is shown.", "Always", "Always", "While Sneaking", "While Holding Tab"));
        this.showHotbar = this.add(new BooleanSetting("Include Hotbar", "Also show the hotbar row.", false));
        this.slotBackgrounds = this.add(new BooleanSetting("Slot Backgrounds", "Draw a box for each slot.", true));
        this.slotColor = this.add(new ColorSetting("Slot Color", "Colour of slot boxes.", 0x40FFFFFF, true)).visibleWhen(this.slotBackgrounds::isOn);
        this.showCounts = this.add(new BooleanSetting("Stack Counts", "Show stack sizes and durability bars.", true));
        this.hideEmptyRows = this.add(new BooleanSetting("Hide Empty Rows", "Skip rows with nothing in them.", false));
    }

    private int[] rows()
    {
        int[] all = this.showHotbar.isOn() ? new int[] {1, 2, 3, 0} : new int[] {1, 2, 3};

        if (!this.hideEmptyRows.isOn() || this.mc.player == null)
        {
            return all;
        }

        int count = 0;
        int[] kept = new int[all.length];

        for (int row : all)
        {
            for (int c = 0; c < 9; ++c)
            {
                if (!this.mc.player.inventory.mainInventory.get(row * 9 + c).isEmpty())
                {
                    kept[count++] = row;
                    break;
                }
            }
        }

        int[] result = new int[Math.max(1, count)];
        System.arraycopy(kept, 0, result, 0, count);
        return count == 0 ? new int[] {1} : result;
    }

    public boolean shouldRender()
    {
        if (this.mc.player == null)
        {
            return false;
        }

        if (this.visibility.is("While Sneaking"))
        {
            return this.mc.player.isSneaking();
        }

        return !this.visibility.is("While Holding Tab") || this.mc.gameSettings.keyBindPlayerList.isKeyDown();
    }

    public int getWidth()
    {
        return 9 * 18 + this.getPad() * 2;
    }

    public int getHeight()
    {
        return this.rows().length * 18 + this.getPad() * 2;
    }

    protected void renderContent(boolean preview)
    {
        this.drawBox(this.getWidth(), this.getHeight());
        int y = this.getPad();

        for (int row : this.rows())
        {
            for (int c = 0; c < 9; ++c)
            {
                int x = this.getPad() + c * 18;

                if (this.slotBackgrounds.isOn())
                {
                    SythixTheme.drawRect((float)(x + 1), (float)(y + 1), (float)(x + 17), (float)(y + 17), this.slotColor.getColor());
                }

                if (this.mc.player != null)
                {
                    ItemStack stack = this.mc.player.inventory.mainInventory.get(row * 9 + c);

                    if (!stack.isEmpty())
                    {
                        this.drawItem(stack, x + 1, y + 1, this.showCounts.isOn());
                    }
                }
            }

            y += 18;
        }
    }
}
