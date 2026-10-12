package net.sythix.modern.mob;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/** Pufferfish: puffs up when a player comes close and poisons anything that touches it. */
public class Pufferfish extends ModernFish
{
    private static final DataParameter<Integer> PUFF = EntityDataManager.<Integer>createKey(Pufferfish.class, DataSerializers.VARINT);
    private int calm;
    private float prevPuff;
    private float puff;

    public Pufferfish(World worldIn)
    {
        super(worldIn);
        this.setSize(0.7F, 0.7F);
    }

    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(PUFF, Integer.valueOf(0));
    }

    public int getPuff()
    {
        return ((Integer)this.dataManager.get(PUFF)).intValue();
    }

    /** Size for drawing: 1 when deflated, up to about 1.7 when fully puffed. */
    public float puffScale(float partialTicks)
    {
        return 1.0F + 0.35F * (this.prevPuff + (this.puff - this.prevPuff) * partialTicks);
    }

    protected boolean shy()
    {
        return false;
    }

    public void onUpdate()
    {
        super.onUpdate();
        this.prevPuff = this.puff;
        this.puff += (this.getPuff() - this.puff) * 0.25F;

        if (this.world.isRemote)
        {
            return;
        }

        EntityPlayer near = this.world.getClosestPlayerToEntity(this, 2.5D);
        int p = this.getPuff();

        if (near != null && !near.capabilities.isCreativeMode && !near.isSpectator())
        {
            this.calm = 0;

            if (p < 2 && this.ticksExisted % 10 == 0)
            {
                this.dataManager.set(PUFF, Integer.valueOf(p + 1));
                this.playSound(SoundEvents.ENTITY_SLIME_SQUISH, 0.6F, 1.6F);
            }
        }
        else if (p > 0 && ++this.calm > 60)
        {
            this.calm = 0;
            this.dataManager.set(PUFF, Integer.valueOf(p - 1));
        }
    }

    public void onCollideWithPlayer(EntityPlayer player)
    {
        int p = this.getPuff();

        if (!this.world.isRemote && p > 0 && !player.capabilities.isCreativeMode && player.attackEntityFrom(DamageSource.causeMobDamage(this), (float)(1 + p)))
        {
            player.addPotionEffect(new PotionEffect(MobEffects.POISON, 60 * p, 0));
        }
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        this.entityDropItem(new ItemStack(Items.FISH, 1, 3), 0.0F);
    }
}
