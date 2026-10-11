package net.sythix.modern;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBoat;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.PotionEffect;
import net.sythix.modern.item.*;

/**
 * Items from Minecraft 1.13 - 1.21.11 (IDs 3000 and up). Only add new items at the END.
 */
public final class ModernItems
{
    public static final int FIRST_ID = 3000;
    private static final Map<String, Item> BY_NAME = new LinkedHashMap<String, Item>();
    private static int nextId = FIRST_ID;

    private ModernItems()
    {
    }

    public static Item get(String name)
    {
        return BY_NAME.get(name);
    }

    public static Map<String, Item> all()
    {
        return Collections.unmodifiableMap(BY_NAME);
    }

    static <T extends Item> T add(String name, T item, CreativeTabs tab)
    {
        item.setUnlocalizedName("m." + name);
        item.setCreativeTab(tab);
        Item.REGISTRY.register(nextId++, new ResourceLocation(name), item);
        BY_NAME.put(name, item);
        return item;
    }

    private static Item plain(String name, CreativeTabs tab)
    {
        return add(name, new Item(), tab);
    }

    static void register()
    {
        CreativeTabs mat = CreativeTabs.MATERIALS;

        // ---- Materials ----
        plain("raw_iron", mat);
        plain("raw_gold", mat);
        plain("raw_copper", mat);
        plain("copper_ingot", mat);
        plain("amethyst_shard", mat);
        plain("netherite_scrap", mat);
        plain("netherite_ingot", mat);
        plain("echo_shard", mat);
        plain("disc_fragment_5", mat);
        plain("phantom_membrane", CreativeTabs.BREWING);
        plain("nautilus_shell", mat);
        plain("heart_of_the_sea", mat);
        plain("pottery_shard_archer", mat);
        plain("pottery_shard_prize", mat);
        plain("pottery_shard_arms_up", mat);
        plain("pottery_shard_skull", mat);

        // ---- Netherite tools and armour ----
        CreativeTabs tools = CreativeTabs.TOOLS;
        CreativeTabs combat = CreativeTabs.COMBAT;
        add("netherite_sword", new ItemSword(Item.ToolMaterial.NETHERITE), combat);
        add("netherite_pickaxe", new ItemPickaxe(Item.ToolMaterial.NETHERITE) {}, tools);
        add("netherite_axe", new ItemAxe(Item.ToolMaterial.NETHERITE) {}, tools);
        add("netherite_shovel", new ItemSpade(Item.ToolMaterial.NETHERITE), tools);
        add("netherite_hoe", new ItemHoe(Item.ToolMaterial.NETHERITE), tools);
        add("netherite_helmet", new ItemArmor(ItemArmor.ArmorMaterial.NETHERITE, 3, EntityEquipmentSlot.HEAD), combat);
        add("netherite_chestplate", new ItemArmor(ItemArmor.ArmorMaterial.NETHERITE, 3, EntityEquipmentSlot.CHEST), combat);
        add("netherite_leggings", new ItemArmor(ItemArmor.ArmorMaterial.NETHERITE, 3, EntityEquipmentSlot.LEGS), combat);
        add("netherite_boots", new ItemArmor(ItemArmor.ArmorMaterial.NETHERITE, 3, EntityEquipmentSlot.FEET), combat);
        add("turtle_helmet", new ItemArmor(ItemArmor.ArmorMaterial.TURTLE, 0, EntityEquipmentSlot.HEAD), combat);

        // ---- Weapons and equipment ----
        add("mace", new MWeapon(500, 5.0D, -3.4D), combat);
        add("spear", new MWeapon(250, 4.0D, -2.8D), combat);
        add("trident", new MTrident(), combat);
        add("crossbow", new MCrossbow(), combat);
        add("wind_charge", new MThrowable(), combat);
        add("brush", new MBrush(), tools);
        add("spyglass", new MSpyglass(), tools);
        add("recovery_compass", new MCompass(), tools);
        add("warped_fungus_on_a_stick", new MWeapon(100, 0.0D, 0.0D), tools);
        add("wolf_armor", new Item().setMaxStackSize(1), combat);

        // ---- Food ----
        CreativeTabs food = CreativeTabs.FOOD;
        add("sweet_berries", new MFood(2, 0.1F, MFood.NORMAL), food);
        add("glow_berries", new MFood(2, 0.1F, MFood.NORMAL), food);
        add("dried_kelp", new MFood(1, 0.3F, MFood.FAST), food);
        add("suspicious_stew", new MFood(6, 0.6F, MFood.STEW), food);
        add("honey_bottle", new MFood(6, 0.1F, MFood.HONEY), food);
        add("ominous_bottle", new MFood(0, 0.0F, MFood.OMINOUS), CreativeTabs.BREWING);

        // ---- Planting and buckets ----
        add("torchflower_seeds", new MPlacer("torchflower", false), mat);
        add("pitcher_pod", new MPlacer("pitcher_plant", false), mat);
        add("powder_snow_bucket", new MPlacer("powder_snow", true), CreativeTabs.MISC);

        // ---- Boats ----
        add("mangrove_boat", new ItemBoat(EntityBoat.Type.MANGROVE), CreativeTabs.TRANSPORTATION);
        add("cherry_boat", new ItemBoat(EntityBoat.Type.CHERRY), CreativeTabs.TRANSPORTATION);
        add("pale_oak_boat", new ItemBoat(EntityBoat.Type.PALE_OAK), CreativeTabs.TRANSPORTATION);

        // ---- Music discs (the music comes from a converted 1.21 pack) ----
        for (String disc : DISCS)
        {
            ResourceLocation id = new ResourceLocation("record." + disc);
            SoundEvent sound = new SoundEvent(id);
            SoundEvent.REGISTRY.register(1000 + BY_NAME.size(), id, sound);
            add("music_disc_" + disc, new MRecord(disc, sound), CreativeTabs.MISC);
        }
    }

    public static final String[] DISCS = new String[] {"otherside", "5", "pigstep", "relic", "creator", "creator_music_box", "precipice"};

    /** Extra melee damage: the mace hits harder the further you fell (and stops your fall). */
    public static float bonusDamage(EntityPlayer player, Entity target)
    {
        Item mace = BY_NAME.get("mace");

        if (mace == null || player.getHeldItemMainhand().getItem() != mace || player.fallDistance <= 1.5F)
        {
            return 0.0F;
        }

        float fall = player.fallDistance;
        float bonus = Math.min(fall, 3.0F) * 4.0F + Math.max(0.0F, Math.min(fall - 3.0F, 5.0F)) * 2.0F + Math.max(0.0F, fall - 8.0F);
        player.fallDistance = 0.0F;

        if (!player.world.isRemote)
        {
            // Smash: knock nearby mobs back
            for (EntityLivingBase e : player.world.getEntitiesWithinAABB(EntityLivingBase.class, target.getEntityBoundingBox().grow(3.5D)))
            {
                if (e != player && e != target)
                {
                    e.knockBack(player, 0.7F, player.posX - e.posX, player.posZ - e.posZ);
                }
            }

            player.world.playSound(null, target.posX, target.posY, target.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6F, 1.8F);
        }

        return bonus;
    }

    /** Every player tick: turtle shell water breathing, and remembering where you died. */
    public static void playerTick(EntityPlayer player)
    {
        if (!player.world.isRemote)
        {
            Item turtle = BY_NAME.get("turtle_helmet");

            if (turtle != null && player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem() == turtle && !player.isInsideOfMaterial(Material.WATER))
            {
                player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 200, 0, false, false));
            }
        }
        else if (player.getHealth() <= 0.0F && player.deathTime == 1)
        {
            MCompass.lastDeath = new net.minecraft.util.math.BlockPos(player);
            MCompass.lastDeathDimension = player.dimension;
        }
    }

    static void registerModels(RenderItem renderItem)
    {
        for (Map.Entry<String, Item> e : BY_NAME.entrySet())
        {
            renderItem.getItemModelMesher().register(e.getValue(), 0, new ModelResourceLocation(e.getKey(), "inventory"));
        }
    }
}
