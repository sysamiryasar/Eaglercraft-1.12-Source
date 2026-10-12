package net.sythix.client.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

/**
 * The CPvP Training Lab world: a separate superflat world ("Sythix Training Lab") with an obsidian
 * arena, armoured training dummies and a crystal and cart PvP kit. Your own worlds are never
 * changed; everything here only works in this singleplayer world.
 */
public final class TrainingWorld
{
    public static final String FOLDER = "sythix-training-lab";
    public static final String NAME = "Sythix Training Lab";
    public static final String DUMMY_TAG = "sythix_dummy";
    private static final int RADIUS = 10;

    private TrainingWorld()
    {
    }

    /** True while you are in the Training Lab world. */
    public static boolean isActive()
    {
        Minecraft mc = Minecraft.getMinecraft();
        IntegratedServer server = mc.getIntegratedServer();
        return mc.isIntegratedServerRunning() && server != null && FOLDER.equals(server.getFolderName());
    }

    /** Opens the lab (creating it the first time). Leaves the current world first. */
    public static void open()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world != null)
        {
            mc.world.sendQuittingDisconnectingPacket();
            mc.loadWorld(null);
        }

        WorldSettings settings = null;

        if (mc.getSaveLoader().getWorldInfo(FOLDER) == null)
        {
            settings = new WorldSettings(20241012L, GameType.CREATIVE, false, false, WorldType.FLAT);
            settings.enableCommands();
            settings.setGeneratorOptions("3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;");
        }

        mc.launchIntegratedServer(FOLDER, NAME, settings);
    }

    /** Runs code on the lab's server thread with your server-side player (does nothing outside the lab). */
    private static void onServer(ServerTask task)
    {
        Minecraft mc = Minecraft.getMinecraft();
        IntegratedServer server = mc.getIntegratedServer();

        if (!isActive() || server == null || mc.player == null)
        {
            return;
        }

        final java.util.UUID id = mc.player.getUniqueID();
        server.addScheduledTask(() -> {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);

            if (player != null)
            {
                task.run(server.getWorld(0), player);
            }
        });
    }

    private interface ServerTask
    {
        void run(WorldServer world, EntityPlayerMP player);
    }

    /** Builds the arena if it is not there yet (called when you join the lab). */
    public static void setUp(boolean creative)
    {
        onServer((world, player) -> {
            world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            world.getGameRules().setOrCreateGameRule("keepInventory", "true");
            world.getGameRules().setOrCreateGameRule("doWeatherCycle", "false");
            world.setWorldTime(6000L);
            BlockPos o = origin(world);

            if (world.getBlockState(o.down()).getBlock() != Blocks.OBSIDIAN || dummies(world).isEmpty())
            {
                build(world, o);
                giveKit(player);
                player.setPositionAndUpdate((double)o.getX() + 0.5D, (double)o.getY(), (double)o.getZ() - 4.5D);
            }

            player.setGameType(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        });
    }

    /** Repairs the floor and brings the dummies back. */
    public static void resetArena()
    {
        onServer((world, player) -> {
            BlockPos o = origin(world);
            build(world, o);
        });
    }

    public static void refillKit()
    {
        onServer((world, player) -> giveKit(player));
    }

    public static void setGameMode(boolean creative)
    {
        onServer((world, player) -> player.setGameType(creative ? GameType.CREATIVE : GameType.SURVIVAL));
    }

    /** Empties your offhand and puts a totem in hotbar slot 9 (Totem Reaction drill). */
    public static void prepareTotemRound()
    {
        onServer((world, player) -> {
            player.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
            player.inventory.setInventorySlotContents(8, new ItemStack(Items.TOTEM_OF_UNDYING));
            player.inventoryContainer.detectAndSendChanges();
        });
    }

    private static BlockPos origin(WorldServer world)
    {
        BlockPos spawn = world.getSpawnPoint();
        return new BlockPos(spawn.getX(), 4, spawn.getZ());
    }

    private static List<Entity> dummies(WorldServer world)
    {
        List<Entity> list = new ArrayList<Entity>();

        for (Entity e : world.loadedEntityList)
        {
            if (e.getTags().contains(DUMMY_TAG) && !e.isDead)
            {
                list.add(e);
            }
        }

        return list;
    }

    private static void build(WorldServer world, BlockPos o)
    {
        for (Entity e : dummies(world))
        {
            e.setDead();
        }

        for (int x = -RADIUS; x <= RADIUS; ++x)
        {
            for (int z = -RADIUS; z <= RADIUS; ++z)
            {
                boolean edge = Math.abs(x) == RADIUS || Math.abs(z) == RADIUS;
                world.setBlockState(o.add(x, -2, z), Blocks.BEDROCK.getDefaultState());
                world.setBlockState(o.add(x, -1, z), edge ? Blocks.QUARTZ_BLOCK.getDefaultState() : Blocks.OBSIDIAN.getDefaultState());

                for (int y = 0; y < 6; ++y)
                {
                    world.setBlockToAir(o.add(x, y, z));
                }
            }
        }

        // a rail lane for cart practice, along the left side
        for (int z = -6; z <= 8; ++z)
        {
            world.setBlockState(o.add(-7, 0, z), Blocks.RAIL.getDefaultState());
        }

        spawnDummy(world, o.add(0, 0, 6));
        spawnDummy(world, o.add(-4, 0, 7));
        spawnDummy(world, o.add(4, 0, 7));
        spawnDummy(world, o.add(-7, 0, 8));
    }

    private static void spawnDummy(WorldServer world, BlockPos pos)
    {
        EntityZombie dummy = new EntityZombie(world);
        dummy.setLocationAndAngles((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D, 180.0F, 0.0F);
        dummy.setNoAI(true);
        dummy.setSilent(true);
        dummy.enablePersistence();
        dummy.setCustomNameTag("Training Dummy");
        dummy.setAlwaysRenderNameTag(true);
        dummy.addTag(DUMMY_TAG);
        dummy.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0D);
        dummy.setHealth(40.0F);
        dummy.setItemStackToSlot(EntityEquipmentSlot.HEAD, armor(Items.DIAMOND_HELMET));
        dummy.setItemStackToSlot(EntityEquipmentSlot.CHEST, armor(Items.DIAMOND_CHESTPLATE));
        dummy.setItemStackToSlot(EntityEquipmentSlot.LEGS, armor(Items.DIAMOND_LEGGINGS));
        dummy.setItemStackToSlot(EntityEquipmentSlot.FEET, armor(Items.DIAMOND_BOOTS));
        // like a real crystal PvP opponent, each dummy has one totem
        dummy.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));

        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values())
        {
            dummy.setDropChance(slot, 0.0F);
        }

        world.spawnEntity(dummy);
    }

    private static ItemStack armor(Item item)
    {
        ItemStack stack = new ItemStack(item);
        stack.addEnchantment(item == Items.DIAMOND_LEGGINGS ? Enchantments.BLAST_PROTECTION : Enchantments.PROTECTION, 4);
        stack.addEnchantment(Enchantments.UNBREAKING, 3);
        return stack;
    }

    private static ItemStack enchanted(Item item, Enchantment a, int la, Enchantment b, int lb)
    {
        ItemStack stack = new ItemStack(item);
        stack.addEnchantment(a, la);

        if (b != null)
        {
            stack.addEnchantment(b, lb);
        }

        return stack;
    }

    private static void giveKit(EntityPlayerMP player)
    {
        player.inventory.clear();
        player.inventory.setInventorySlotContents(0, enchanted(Items.DIAMOND_SWORD, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3));
        player.inventory.setInventorySlotContents(1, new ItemStack(Items.END_CRYSTAL, 64));
        player.inventory.setInventorySlotContents(2, new ItemStack(Blocks.OBSIDIAN, 64));
        player.inventory.setInventorySlotContents(3, new ItemStack(Blocks.RAIL, 64));
        player.inventory.setInventorySlotContents(4, new ItemStack(Items.TNT_MINECART));
        player.inventory.setInventorySlotContents(5, new ItemStack(Items.TNT_MINECART));
        player.inventory.setInventorySlotContents(6, enchanted(Items.BOW, Enchantments.FLAME, 1, Enchantments.INFINITY, 1));
        player.inventory.setInventorySlotContents(7, new ItemStack(Items.FLINT_AND_STEEL));
        player.inventory.setInventorySlotContents(8, new ItemStack(Items.TOTEM_OF_UNDYING));

        for (int i = 9; i < 15; ++i)
        {
            player.inventory.setInventorySlotContents(i, new ItemStack(Items.END_CRYSTAL, 64));
        }

        for (int i = 15; i < 22; ++i)
        {
            player.inventory.setInventorySlotContents(i, new ItemStack(Items.TNT_MINECART));
        }

        for (int i = 22; i < 30; ++i)
        {
            player.inventory.setInventorySlotContents(i, new ItemStack(Items.TOTEM_OF_UNDYING));
        }

        player.inventory.setInventorySlotContents(30, new ItemStack(Items.GOLDEN_APPLE, 64, 1));
        player.inventory.setInventorySlotContents(31, new ItemStack(Items.ARROW));
        player.inventory.setInventorySlotContents(32, new ItemStack(Blocks.OBSIDIAN, 64));
        player.inventory.setInventorySlotContents(33, new ItemStack(Blocks.RAIL, 64));
        player.setItemStackToSlot(EntityEquipmentSlot.HEAD, armor(Items.DIAMOND_HELMET));
        player.setItemStackToSlot(EntityEquipmentSlot.CHEST, armor(Items.DIAMOND_CHESTPLATE));
        player.setItemStackToSlot(EntityEquipmentSlot.LEGS, armor(Items.DIAMOND_LEGGINGS));
        player.setItemStackToSlot(EntityEquipmentSlot.FEET, armor(Items.DIAMOND_BOOTS));
        player.setHeldItem(EnumHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        player.setHealth(player.getMaxHealth());
        player.getFoodStats().setFoodLevel(20);
        player.inventoryContainer.detectAndSendChanges();
    }
}
