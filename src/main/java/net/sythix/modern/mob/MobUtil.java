package net.sythix.modern.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MoverType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.sythix.modern.ModernBlocks;
import net.sythix.modern.ModernContent;

/** Small helpers shared by the new mobs. */
public final class MobUtil
{
    private MobUtil()
    {
    }

    /** Any item by registry name (vanilla or Sythix), or null. */
    public static Item item(String name)
    {
        return ModernBlocks.item(name);
    }

    /** A Sythix or vanilla block by registry name, or null. */
    public static net.minecraft.block.Block blockOrNull(String name)
    {
        net.minecraft.block.Block b = ModernBlocks.get(name);
        return b != null ? b : net.minecraft.block.Block.getBlockFromName(name);
    }

    /** Drops between min and max of an item (plus up to one more per looting level). */
    public static void drop(Entity e, String name, int min, int max, int looting)
    {
        Item item = item(name);

        if (item == null)
        {
            return;
        }

        int count = min + (max > min ? e.world.rand.nextInt(max - min + 1) : 0) + (looting > 0 ? e.world.rand.nextInt(looting + 1) : 0);

        if (count > 0)
        {
            e.entityDropItem(new ItemStack(item, count), 0.0F);
        }
    }

    /** Whether the new mobs may spawn naturally (the "New Mobs" setting of the 1.21 Content module). */
    public static boolean naturalSpawns()
    {
        return ModernContent.mobs();
    }

    /** Steers a flying mob towards a point, like bats and vexes do. */
    public static void flyTowards(EntityLiving e, double x, double y, double z, double speed)
    {
        double dx = x - e.posX;
        double dy = y - e.posY;
        double dz = z - e.posZ;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (d < 0.05D)
        {
            return;
        }

        e.motionX += (dx / d * speed - e.motionX) * 0.15D;
        e.motionY += (dy / d * speed - e.motionY) * 0.15D;
        e.motionZ += (dz / d * speed - e.motionZ) * 0.15D;
        float yaw = (float)(MathHelper.atan2(e.motionZ, e.motionX) * (180D / Math.PI)) - 90.0F;
        e.rotationYaw += MathHelper.wrapDegrees(yaw - e.rotationYaw) * 0.3F;
        e.renderYawOffset = e.rotationYaw;
    }

    /** Moves a flying mob by its motion with air drag, and animates its limbs. */
    public static void flyTravel(EntityLiving e)
    {
        e.move(MoverType.SELF, e.motionX, e.motionY, e.motionZ);
        e.motionX *= 0.91D;
        e.motionY *= 0.91D;
        e.motionZ *= 0.91D;
        e.prevLimbSwingAmount = e.limbSwingAmount;
        double dx = e.posX - e.prevPosX;
        double dz = e.posZ - e.prevPosZ;
        float f = Math.min(1.0F, MathHelper.sqrt(dx * dx + dz * dz) * 4.0F);
        e.limbSwingAmount += (f - e.limbSwingAmount) * 0.4F;
        e.limbSwing += e.limbSwingAmount;
    }
}
