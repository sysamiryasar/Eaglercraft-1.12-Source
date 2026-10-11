package net.sythix.modern.mob;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/** Tadpole: a baby frog that grows into a frog after 20 minutes (faster when fed slime balls). */
public class Tadpole extends ModernFish
{
    private int age;

    public Tadpole(World worldIn)
    {
        super(worldIn);
        this.setSize(0.4F, 0.3F);
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (!this.world.isRemote && ++this.age >= 24000)
        {
            this.growUp();
        }
    }

    private void growUp()
    {
        Frog frog = new Frog(this.world);
        frog.copyLocationAndAnglesFrom(this);
        frog.onInitialSpawn(this.world.getDifficultyForLocation(this.getPosition()), null);
        this.world.removeEntity(this);
        this.world.spawnEntity(frog);
    }

    protected boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);

        if (held.getItem() == Items.SLIME_BALL)
        {
            if (!player.capabilities.isCreativeMode)
            {
                held.shrink(1);
            }

            this.age += 2400;
            return true;
        }

        return super.processInteract(player, hand);
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("Age", this.age);
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.age = compound.getInteger("Age");
    }

    protected boolean canDespawn()
    {
        return false;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
    }
}
