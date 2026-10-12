package net.sythix.client.module.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatAllowedCharacters;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #25 Inventory search, highlighting and sorting. Sorting clicks items like you would, so it is
 * only available in singleplayer, where it cannot break any server's rules.
 */
public class InventoryEnhancements extends Module
{
    private final BooleanSetting search;
    private final ColorSetting dimColor;
    private final BooleanSetting sortButton;
    private final ModeSetting sortBy;
    private String query = "";
    private boolean focused;
    private GuiScreen lastScreen;

    public InventoryEnhancements()
    {
        super("Inventory Enhancements", "Search bar and highlighting in inventories, plus sorting in singleplayer.", Category.INVENTORY, true);
        this.search = this.add(new BooleanSetting("Search Bar", "A search box above inventories and chests.", true));
        this.dimColor = this.add(new ColorSetting("Dim Color", "Overlay on items that do not match the search.", 0xB0000000, true)).visibleWhen(this.search::isOn);
        this.sortButton = this.add(new BooleanSetting("Sort Button", "Sort button next to inventories (singleplayer only).", true));
        this.sortBy = this.add(new ModeSetting("Sort By", "How items are ordered.", "Type", "Type", "Name", "Count")).visibleWhen(this.sortButton::isOn);
    }

    private boolean canSort(GuiContainer gui)
    {
        return this.sortButton.isOn() && !(gui instanceof GuiContainerCreative) && this.mc.isIntegratedServerRunning() && this.mc.getIntegratedServer() != null && !this.mc.getIntegratedServer().getPublic();
    }

    private void reset(GuiContainer gui)
    {
        if (gui != this.lastScreen)
        {
            this.lastScreen = gui;
            this.query = "";
            this.focused = false;
        }
    }

    private boolean matches(ItemStack stack)
    {
        if (this.query.isEmpty())
        {
            return true;
        }

        String q = this.query.toLowerCase(Locale.ROOT);

        if (stack.getDisplayName().toLowerCase(Locale.ROOT).contains(q))
        {
            return true;
        }

        for (String line : stack.getTooltip(this.mc.player, net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL))
        {
            if (line.toLowerCase(Locale.ROOT).contains(q))
            {
                return true;
            }
        }

        return false;
    }

    private int[] searchBox(GuiContainer gui)
    {
        int w = Math.min(gui.getXSizeSythix(), 120);
        return new int[] {gui.getGuiLeftSythix() + gui.getXSizeSythix() - w, gui.getGuiTopSythix() - 14, w, 12};
    }

    private int[] sortBox(GuiContainer gui)
    {
        return new int[] {gui.getGuiLeftSythix(), gui.getGuiTopSythix() - 14, 30, 12};
    }

    /** Draws the search box, dimming and sort button over a container screen. */
    public void draw(GuiContainer gui, int mouseX, int mouseY)
    {
        if (gui instanceof GuiContainerCreative)
        {
            return;
        }

        this.reset(gui);

        if (this.search.isOn() && !this.query.isEmpty())
        {
            for (Slot slot : gui.inventorySlots.inventorySlots)
            {
                if (slot.getHasStack() && !this.matches(slot.getStack()))
                {
                    int x = gui.getGuiLeftSythix() + slot.xPos;
                    int y = gui.getGuiTopSythix() + slot.yPos;
                    SythixTheme.drawRect((float)x, (float)y, (float)(x + 16), (float)(y + 16), this.dimColor.getColor());
                }
            }
        }

        if (this.search.isOn())
        {
            int[] b = this.searchBox(gui);
            SythixTheme.drawRect((float)b[0], (float)b[1], (float)(b[0] + b[2]), (float)(b[1] + b[3]), 0xE00F0F13);
            SythixTheme.drawOutline((float)b[0], (float)b[1], (float)(b[0] + b[2]), (float)(b[1] + b[3]), this.focused ? SythixTheme.RED : SythixTheme.BORDER);
            String text = this.query.isEmpty() && !this.focused ? "Search..." : this.query + (this.focused && (System.currentTimeMillis() / 500L) % 2L == 0L ? "_" : "");
            this.mc.fontRenderer.drawString(this.mc.fontRenderer.trimStringToWidth(text, b[2] - 6, true), (float)(b[0] + 3), (float)(b[1] + 2), this.query.isEmpty() && !this.focused ? SythixTheme.TEXT_DISABLED : 0xFFFFFFFF, false);
        }

        if (this.canSort(gui))
        {
            int[] b = this.sortBox(gui);
            boolean hover = mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];
            SythixTheme.drawButton((float)b[0], (float)b[1], (float)b[2], (float)b[3], true, hover ? 1.0F : 0.0F);
            this.mc.fontRenderer.drawString("Sort", (float)(b[0] + 4), (float)(b[1] + 2), 0xFFFFFFFF, false);
        }
    }

    /** Returns true if the click was used by the search box or sort button. */
    public boolean click(GuiContainer gui, int mouseX, int mouseY, int button)
    {
        if (gui instanceof GuiContainerCreative)
        {
            return false;
        }

        this.reset(gui);

        if (this.search.isOn())
        {
            int[] b = this.searchBox(gui);
            boolean inside = mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];

            if (inside)
            {
                this.focused = true;

                if (button == 1)
                {
                    this.query = "";
                }

                return true;
            }

            this.focused = false;
        }

        if (this.canSort(gui))
        {
            int[] b = this.sortBox(gui);

            if (mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3])
            {
                this.sort(gui);
                return true;
            }
        }

        return false;
    }

    /** Returns true if the key was typed into the search box. */
    public boolean key(GuiContainer gui, char typedChar, int keyCode)
    {
        if (!this.focused || !this.search.isOn())
        {
            return false;
        }

        if (keyCode == 1 || keyCode == 28)
        {
            this.focused = false;
        }
        else if (keyCode == 14)
        {
            this.query = this.query.isEmpty() ? "" : this.query.substring(0, this.query.length() - 1);
        }
        else if (ChatAllowedCharacters.isAllowedCharacter(typedChar) && this.query.length() < 30)
        {
            this.query = this.query + typedChar;
        }

        return true;
    }

    private void sort(GuiContainer gui)
    {
        Container container = gui.inventorySlots;
        boolean playerInventory = gui instanceof GuiInventory;
        List<Slot> slots = new ArrayList<Slot>();

        for (Slot slot : container.inventorySlots)
        {
            boolean isPlayer = slot.inventory instanceof InventoryPlayer;

            if (playerInventory ? isPlayer && slot.getSlotIndexSythix() >= 9 && slot.getSlotIndexSythix() < 36 : !isPlayer)
            {
                slots.add(slot);
            }
        }

        // Work out the target order on a copy, comparing item kinds only (never stacks of the same kind)
        List<ItemStack> current = new ArrayList<ItemStack>();

        for (Slot slot : slots)
        {
            current.add(slot.getStack().copy());
        }

        List<ItemStack> sorted = new ArrayList<ItemStack>(current);
        Comparator<ItemStack> byType = Comparator.comparingInt((ItemStack s) -> s.isEmpty() ? Integer.MAX_VALUE : Item.getIdFromItem(s.getItem())).thenComparingInt(ItemStack::getMetadata).thenComparing(s -> s.getDisplayName());

        if (this.sortBy.is("Name"))
        {
            sorted.sort(Comparator.comparing((ItemStack s) -> s.isEmpty() ? "￿" : s.getDisplayName().toLowerCase(Locale.ROOT)));
        }
        else if (this.sortBy.is("Count"))
        {
            sorted.sort(Comparator.comparingInt((ItemStack s) -> s.isEmpty() ? Integer.MAX_VALUE : -s.getCount()).thenComparing(byType));
        }
        else
        {
            sorted.sort(byType);
        }

        int clicks = 0;

        for (int i = 0; i < slots.size(); ++i)
        {
            ItemStack want = sorted.get(i);

            if (sameKind(current.get(i), want))
            {
                continue;
            }

            int j = -1;

            for (int k = i + 1; k < slots.size(); ++k)
            {
                if (sameKind(current.get(k), want))
                {
                    j = k;
                    break;
                }
            }

            if (j < 0)
            {
                continue;
            }

            // Swap slots i and j by picking up j, placing on i, and putting the old i into j
            this.click(container, slots.get(j));
            this.click(container, slots.get(i));
            this.click(container, slots.get(j));
            ItemStack tmp = current.get(i);
            current.set(i, current.get(j));
            current.set(j, tmp);
            clicks += 3;
        }

        Notifications.post("Inventory sorted", clicks == 0 ? "Already in order." : "Sorted by " + this.sortBy.get().toLowerCase(Locale.ROOT) + ".", Notifications.Kind.SUCCESS);
    }

    private static boolean sameKind(ItemStack a, ItemStack b)
    {
        if (a.isEmpty() || b.isEmpty())
        {
            return a.isEmpty() && b.isEmpty();
        }

        return ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    private void click(Container container, Slot slot)
    {
        this.mc.playerController.windowClick(container.windowId, slot.slotNumber, 0, ClickType.PICKUP, this.mc.player);
    }
}
