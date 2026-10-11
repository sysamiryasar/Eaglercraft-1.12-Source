package net.sythix.modern.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.sythix.modern.entity.EntityThrownTrident;

/**
 * Trident: a strong melee weapon you can also throw (hold right-click, then let go).
 */
public class MTrident extends MWeapon
{
    public MTrident()
    {
        super(250, 8.0D, -2.9D);
    }

    public EnumAction getItemUseAction(ItemStack stack)
    {
        return EnumAction.BOW;
    }

    public int getMaxItemUseDuration(ItemStack stack)
    {
        return 72000;
    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        playerIn.setActiveHand(handIn);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
    }

    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft)
    {
        if (!(entityLiving instanceof EntityPlayer) || this.getMaxItemUseDuration(stack) - timeLeft < 10)
        {
            return;
        }

        EntityPlayer player = (EntityPlayer)entityLiving;

        if (!worldIn.isRemote)
        {
            stack.damageItem(1, player);
            EntityThrownTrident trident = new EntityThrownTrident(worldIn, player, stack.copy(), !player.capabilities.isCreativeMode);
            trident.setHeadingFromThrower(player, player.rotationPitch, player.rotationYaw, 0.0F, 2.5F, 1.0F);
            worldIn.spawnEntity(trident);

            if (!player.capabilities.isCreativeMode)
            {
                player.inventory.deleteStack(stack);
            }
        }

        worldIn.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.6F);
    }
}
