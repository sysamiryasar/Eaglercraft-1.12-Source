package net.sythix.modern.mob;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.INpc;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;

/**
 * Wandering trader: appears now and then near players with two trader llamas and sells plants and
 * blocks for emeralds. Leaves after about two in-game days.
 */
public class WanderingTrader extends EntityCreature implements IMerchant, INpc
{
    /** {item, how many you get, emerald price}. */
    private static final Object[][] OFFERS = new Object[][] {
        {"cherry_sapling", 1, 5}, {"pale_oak_sapling", 1, 5}, {"mangrove_propagule", 1, 5}, {"cornflower", 1, 1}, {"lily_of_the_valley", 1, 1},
        {"torchflower_seeds", 1, 3}, {"pitcher_pod", 1, 3}, {"pink_petals", 2, 1}, {"moss_block", 2, 1}, {"bamboo", 4, 1}, {"kelp", 4, 1},
        {"sea_pickle", 2, 1}, {"glow_berries", 4, 1}, {"sweet_berries", 4, 1}, {"blue_ice", 1, 6}, {"mud", 4, 1}, {"tuff", 8, 1},
        {"slime_ball", 1, 4}, {"nautilus_shell", 1, 5}, {"packed_ice", 1, 3}, {"vine", 1, 1}, {"cactus", 1, 3}, {"pumpkin", 1, 1}};

    private MerchantRecipeList offers;
    private EntityPlayer customer;
    private int despawnDelay = 48000;

    public WanderingTrader(World worldIn)
    {
        super(worldIn);
        this.setSize(0.6F, 1.95F);
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAIAvoidEntity<EntityZombie>(this, EntityZombie.class, 8.0F, 0.6D, 0.6D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<AbstractIllager>(this, AbstractIllager.class, 12.0F, 0.6D, 0.6D));
        this.tasks.addTask(1, new EntityAIPanic(this, 0.6D));
        this.tasks.addTask(8, new EntityAIWanderAvoidWater(this, 0.35D));
        this.tasks.addTask(9, new EntityAIWatchClosest(this, EntityPlayer.class, 3.0F, 1.0F));
        this.tasks.addTask(10, new EntityAILookIdle(this));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
    }

    protected boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);

        if (held.getItem() == Items.SPAWN_EGG || held.getItem() == Items.NAME_TAG || !this.isEntityAlive() || this.customer != null)
        {
            return super.processInteract(player, hand);
        }

        if (!this.world.isRemote && !this.getRecipes(player).isEmpty())
        {
            this.setCustomer(player);
            player.displayVillagerTradeGui(this);
        }

        return true;
    }

    protected void updateAITasks()
    {
        super.updateAITasks();

        if (this.customer != null)
        {
            this.getNavigator().clearPathEntity();
            this.getLookHelper().setLookPositionWithEntity(this.customer, 30.0F, 30.0F);
        }

        if (--this.despawnDelay <= 0 && this.customer == null)
        {
            this.setDead();
        }
    }

    public void setDespawnDelay(int ticks)
    {
        this.despawnDelay = ticks;
    }

    public void setCustomer(EntityPlayer player)
    {
        this.customer = player;
    }

    public EntityPlayer getCustomer()
    {
        return this.customer;
    }

    public MerchantRecipeList getRecipes(EntityPlayer player)
    {
        if (this.offers == null)
        {
            this.offers = new MerchantRecipeList();
            java.util.List<Object[]> pool = new java.util.ArrayList<Object[]>(java.util.Arrays.asList(OFFERS));
            java.util.Collections.shuffle(pool, this.rand);

            for (Object[] o : pool)
            {
                Item item = MobUtil.item((String)o[0]);

                if (item != null)
                {
                    this.offers.add(new MerchantRecipe(new ItemStack(Items.EMERALD, (Integer)o[2]), ItemStack.EMPTY, new ItemStack(item, (Integer)o[1]), 0, 8));
                }

                if (this.offers.size() >= 6)
                {
                    break;
                }
            }
        }

        return this.offers;
    }

    public void setRecipes(MerchantRecipeList recipeList)
    {
    }

    public void useRecipe(MerchantRecipe recipe)
    {
        recipe.incrementToolUses();
        this.livingSoundTime = -this.getTalkInterval();
        this.playSound(SoundEvents.ENTITY_VILLAGER_YES, this.getSoundVolume(), this.getSoundPitch());
        this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY + 0.5D, this.posZ, 1 + this.rand.nextInt(3)));
    }

    public void verifySellingItem(ItemStack stack)
    {
        if (!this.world.isRemote && this.livingSoundTime > -this.getTalkInterval() + 20)
        {
            this.livingSoundTime = -this.getTalkInterval();
            this.playSound(stack.isEmpty() ? SoundEvents.ENTITY_VILLAGER_NO : SoundEvents.ENTITY_VILLAGER_YES, this.getSoundVolume(), this.getSoundPitch());
        }
    }

    public World getWorld()
    {
        return this.world;
    }

    public BlockPos getPos()
    {
        return new BlockPos(this);
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("DespawnDelay", this.despawnDelay);

        if (this.offers != null)
        {
            compound.setTag("Offers", this.offers.getRecipiesAsTags());
        }
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);

        if (compound.hasKey("DespawnDelay"))
        {
            this.despawnDelay = compound.getInteger("DespawnDelay");
        }

        if (compound.hasKey("Offers"))
        {
            this.offers = new MerchantRecipeList(compound.getCompoundTag("Offers"));
        }
    }

    protected boolean canDespawn()
    {
        return false;
    }

    public boolean getCanSpawnHere()
    {
        return false;
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected SoundEvent getAmbientSound()
    {
        return this.customer != null ? SoundEvents.ENTITY_VILLAGER_TRADING : SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }
}
