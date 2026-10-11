package net.sythix.modern.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.sythix.modern.entity.EntityWindCharge;

/**
 * Wind charge: throw it to blast entities (and yourself) away without breaking blocks.
 */
public class MThrowable extends Item
{
    public MThrowable()
    {
        this.setMaxStackSize(64);
    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (!playerIn.capabilities.isCreativeMode)
        {
            stack.shrink(1);
        }

        worldIn.playSound((EntityPlayer)null, playerIn.posX, playerIn.posY, playerIn.posZ, SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F, 1.4F);
        playerIn.getCooldownTracker().setCooldown(this, 10);

        if (!worldIn.isRemote)
        {
            EntityWindCharge charge = new EntityWindCharge(worldIn, playerIn);
            charge.setHeadingFromThrower(playerIn, playerIn.rotationPitch, playerIn.rotationYaw, 0.0F, 1.5F, 1.0F);
            worldIn.spawnEntity(charge);
        }

        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }
}
