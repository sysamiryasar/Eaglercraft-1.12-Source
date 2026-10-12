package net.sythix.modern.mob;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.sythix.modern.ModernEffects;

/** Dolphin: a playful swimmer that follows players in the water and gives them Dolphin's Grace. */
public class Dolphin extends ModernFish
{
    private EntityPlayer friend;

    public Dolphin(World worldIn)
    {
        super(worldIn);
        this.setSize(0.9F, 0.6F);
    }

    protected double maxHealth()
    {
        return 10.0D;
    }

    protected double swimSpeed()
    {
        return 0.3D;
    }

    protected boolean shy()
    {
        return false;
    }

    protected boolean swimTowardsTarget()
    {
        if (this.friend == null || !this.friend.isEntityAlive() || !this.friend.isInWater() || this.getDistanceSqToEntity(this.friend) > 400.0D)
        {
            this.friend = null;

            for (EntityPlayer p : this.world.playerEntities)
            {
                if (p.isInWater() && !p.isSpectator() && this.getDistanceSqToEntity(p) < 100.0D)
                {
                    this.friend = p;
                    break;
                }
            }
        }

        if (this.friend == null)
        {
            return false;
        }

        if (this.getDistanceSqToEntity(this.friend) > 9.0D)
        {
            MobUtil.flyTowards(this, this.friend.posX, this.friend.posY + 0.5D, this.friend.posZ, this.swimSpeed() * 1.3D);
        }

        if (this.ticksExisted % 20 == 0)
        {
            Potion grace = ModernEffects.effect("dolphins_grace");

            if (grace != null)
            {
                this.friend.addPotionEffect(new PotionEffect(grace, 100, 0, true, true));
            }
        }

        return true;
    }

    /** Feed a dolphin raw fish and it shows you some treasure (a nautilus shell or heart of the sea). */
    protected boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);

        if (held.getItem() == Items.FISH && !this.world.isRemote)
        {
            if (!player.capabilities.isCreativeMode)
            {
                held.shrink(1);
            }

            this.playSound(SoundEvents.ENTITY_PLAYER_SPLASH, 1.0F, 1.5F);

            if (this.rand.nextInt(4) == 0)
            {
                MobUtil.drop(this, this.rand.nextInt(10) == 0 ? "heart_of_the_sea" : "nautilus_shell", 1, 1, 0);
            }

            return true;
        }

        return super.processInteract(player, hand);
    }

    protected boolean canDespawn()
    {
        return false;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        this.entityDropItem(new ItemStack(Items.FISH, this.rand.nextInt(2 + lootingModifier), 0), 0.0F);
    }
}
