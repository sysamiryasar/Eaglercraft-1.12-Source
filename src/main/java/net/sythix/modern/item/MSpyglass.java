package net.sythix.modern.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/**
 * Spyglass: hold right-click to zoom in a long way (see Hooks.zoomFov).
 */
public class MSpyglass extends Item
{
    public MSpyglass()
    {
        this.setMaxStackSize(1);
    }

    public int getMaxItemUseDuration(ItemStack stack)
    {
        return 1200;
    }

    public EnumAction getItemUseAction(ItemStack stack)
    {
        return EnumAction.NONE;
    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        playerIn.setActiveHand(handIn);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
    }

    /** True while someone is looking through a spyglass. */
    public static boolean isLooking(EntityPlayer player)
    {
        return player != null && player.isHandActive() && player.getActiveItemStack().getItem() instanceof MSpyglass;
    }
}
