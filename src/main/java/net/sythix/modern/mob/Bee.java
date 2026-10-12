package net.sythix.modern.mob;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Bee: buzzes from flower to flower and makes nearby crops grow faster. Hit one and the whole swarm
 * gets angry; a bee that stings loses its stinger and dies soon after.
 */
public class Bee extends EntityAnimal
{
    private EntityLivingBase anger;
    private int angerTime;
    private boolean stung;
    private int dieTimer;
    private BlockPos flower;
    private int pollen;
    private double hx;
    private double hy;
    private double hz;

    public Bee(World worldIn)
    {
        super(worldIn);
        this.setSize(0.7F, 0.6F);
        this.setNoGravity(true);
    }

    protected void initEntityAI()
    {
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
    }

    protected void updateAITasks()
    {
        super.updateAITasks();

        if (this.stung && ++this.dieTimer > 60 + this.rand.nextInt(600))
        {
            this.attackEntityFrom(DamageSource.GENERIC, this.getHealth());
            return;
        }

        if (this.anger != null && (!this.anger.isEntityAlive() || --this.angerTime <= 0 || this.anger instanceof EntityPlayer && ((EntityPlayer)this.anger).capabilities.disableDamage))
        {
            this.anger = null;
        }

        if (this.anger != null && !this.stung)
        {
            MobUtil.flyTowards(this, this.anger.posX, this.anger.posY + this.anger.height * 0.5D, this.anger.posZ, 0.35D);

            if (this.getEntityBoundingBox().grow(0.3D).intersects(this.anger.getEntityBoundingBox()) && this.anger.attackEntityFrom(DamageSource.causeMobDamage(this), 2.0F))
            {
                if (this.world.getDifficulty() != EnumDifficulty.EASY && this.world.getDifficulty() != EnumDifficulty.PEACEFUL)
                {
                    this.anger.addPotionEffect(new PotionEffect(MobEffects.POISON, this.world.getDifficulty() == EnumDifficulty.HARD ? 360 : 200, 0));
                }

                this.stung = true;
                this.anger = null;
            }

            return;
        }

        if (this.isInLove() && this.mate())
        {
            return;
        }

        this.pollinate();
    }

    /** Flying bees cannot use the walking mate AI, so they find each other here. */
    private boolean mate()
    {
        List<Bee> list = this.world.<Bee>getEntitiesWithinAABB(Bee.class, this.getEntityBoundingBox().grow(8.0D), b -> b != this && b.isInLove());

        if (list.isEmpty())
        {
            return false;
        }

        Bee other = list.get(0);
        MobUtil.flyTowards(this, other.posX, other.posY, other.posZ, 0.2D);

        if (this.getDistanceSqToEntity(other) < 2.0D)
        {
            Bee baby = new Bee(this.world);
            baby.setGrowingAge(-24000);
            baby.setLocationAndAngles(this.posX, this.posY, this.posZ, 0.0F, 0.0F);
            this.world.spawnEntity(baby);
            this.setGrowingAge(6000);
            other.setGrowingAge(6000);
            this.resetInLove();
            other.resetInLove();
        }

        return true;
    }

    private boolean isFlower(BlockPos p)
    {
        Block b = this.world.getBlockState(p).getBlock();
        return b instanceof BlockFlower || b == MobUtil.blockOrNull("cornflower") || b == MobUtil.blockOrNull("lily_of_the_valley") || b == MobUtil.blockOrNull("torchflower") || b == MobUtil.blockOrNull("pink_petals");
    }

    /** Visits flowers, then spreads pollen over crops on the way. */
    private void pollinate()
    {
        if (this.flower != null && !this.isFlower(this.flower))
        {
            this.flower = null;
        }

        if (this.flower == null && this.ticksExisted % 40 == 0)
        {
            for (int i = 0; i < 20 && this.flower == null; ++i)
            {
                BlockPos p = new BlockPos(this.posX + this.rand.nextInt(17) - 8, this.posY + this.rand.nextInt(7) - 4, this.posZ + this.rand.nextInt(17) - 8);

                if (this.isFlower(p))
                {
                    this.flower = p;
                }
            }
        }

        if (this.flower != null && this.pollen < 200)
        {
            MobUtil.flyTowards(this, this.flower.getX() + 0.5D, this.flower.getY() + 0.7D, this.flower.getZ() + 0.5D, 0.2D);

            if (this.getDistanceSq(this.flower.getX() + 0.5D, this.flower.getY() + 0.7D, this.flower.getZ() + 0.5D) < 1.0D)
            {
                ++this.pollen;
            }

            return;
        }

        if (this.pollen > 0 && this.ticksExisted % 20 == 0)
        {
            BlockPos crop = new BlockPos(this.posX + this.rand.nextInt(5) - 2, this.posY - 1 - this.rand.nextInt(3), this.posZ + this.rand.nextInt(5) - 2);

            if (this.world.getBlockState(crop).getBlock() instanceof net.minecraft.block.BlockCrops && ((net.minecraft.block.BlockCrops)this.world.getBlockState(crop).getBlock()).canGrow(this.world, crop, this.world.getBlockState(crop), false))
            {
                ((net.minecraft.block.BlockCrops)this.world.getBlockState(crop).getBlock()).grow(this.world, this.rand, crop, this.world.getBlockState(crop));
                this.pollen -= 20;

                if (this.world instanceof WorldServer)
                {
                    ((WorldServer)this.world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, crop.getX() + 0.5D, crop.getY() + 0.5D, crop.getZ() + 0.5D, 4, 0.3D, 0.3D, 0.3D, 0.0D);
                }
            }
        }

        if (this.ticksExisted % 80 == 0 || this.getDistanceSq(this.hx, this.hy, this.hz) < 1.0D)
        {
            this.hx = this.posX + this.rand.nextInt(13) - 6;
            this.hy = this.world.getHeight(new BlockPos(this.hx, 0, this.posZ)).getY() + 1 + this.rand.nextInt(3);
            this.hz = this.posZ + this.rand.nextInt(13) - 6;
            this.pollen = Math.max(0, this.pollen - 10);
        }

        MobUtil.flyTowards(this, this.hx, this.hy, this.hz, 0.15D);
    }

    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        boolean hurt = super.attackEntityFrom(source, amount);

        if (hurt && !this.world.isRemote && source.getTrueSource() instanceof EntityLivingBase)
        {
            EntityLivingBase attacker = (EntityLivingBase)source.getTrueSource();
            List<Bee> swarm = this.world.<Bee>getEntitiesWithinAABB(Bee.class, this.getEntityBoundingBox().grow(16.0D));

            for (Bee b : swarm)
            {
                if (!b.stung)
                {
                    b.anger = attacker;
                    b.angerTime = 400 + this.rand.nextInt(400);
                }
            }
        }

        return hurt;
    }

    public void travel(float strafe, float vertical, float forward)
    {
        MobUtil.flyTravel(this);
    }

    public void fall(float distance, float damageMultiplier)
    {
    }

    public boolean isBreedingItem(ItemStack stack)
    {
        Item item = stack.getItem();
        return item == Item.getItemFromBlock(net.minecraft.init.Blocks.RED_FLOWER) || item == Item.getItemFromBlock(net.minecraft.init.Blocks.YELLOW_FLOWER);
    }

    public EntityAgeable createChild(EntityAgeable ageable)
    {
        return new Bee(this.world);
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && super.getCanSpawnHere();
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_PARROT_FLY;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_SILVERFISH_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_SILVERFISH_DEATH;
    }

    protected float getSoundPitch()
    {
        return 1.8F;
    }
}
