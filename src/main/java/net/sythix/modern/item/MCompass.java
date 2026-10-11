package net.sythix.modern.item;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Recovery compass: points to the place where you last died (it spins when you have not died in
 * this dimension).
 */
public class MCompass extends Item
{
    /** Last death of the local player: x, z, dimension. Set on the client. */
    public static BlockPos lastDeath;
    public static int lastDeathDimension;

    public MCompass()
    {
        this.setMaxStackSize(1);
        this.addPropertyOverride(new ResourceLocation("angle"), new IItemPropertyGetter()
        {
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn)
            {
                Entity e = entityIn != null ? entityIn : Minecraft.getMinecraft().player;

                if (e == null || e.world == null)
                {
                    return 0.0F;
                }

                if (lastDeath == null || e.dimension != lastDeathDimension)
                {
                    return (float)(Minecraft.getSystemTime() % 1000L) / 1000.0F;
                }

                double wanted = Math.atan2(lastDeath.getZ() + 0.5D - e.posZ, lastDeath.getX() + 0.5D - e.posX) / (Math.PI * 2.0D);
                double facing = MathHelper.positiveModulo((double)e.rotationYaw / 360.0D, 1.0D);
                return (float)MathHelper.positiveModulo(0.5D - (facing - 0.25D - wanted), 1.0D);
            }
        });
    }
}
