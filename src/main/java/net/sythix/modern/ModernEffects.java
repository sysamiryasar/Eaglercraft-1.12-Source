package net.sythix.modern;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.potion.PotionType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sythix.modern.entity.EntityWindCharge;

/**
 * Status effects (IDs 28+), enchantments (IDs 80+), potions and brewing from Minecraft
 * 1.13 - 1.21, and what they do.
 */
public final class ModernEffects
{
    private static final Map<String, Potion> EFFECTS = new LinkedHashMap<String, Potion>();
    private static final Map<String, Enchantment> ENCHANTS = new LinkedHashMap<String, Enchantment>();

    private ModernEffects()
    {
    }

    public static Potion effect(String name)
    {
        return EFFECTS.get(name);
    }

    public static Enchantment enchant(String name)
    {
        return ENCHANTS.get(name);
    }

    public static Map<String, Potion> effects()
    {
        return EFFECTS;
    }

    public static Map<String, Enchantment> enchantments()
    {
        return ENCHANTS;
    }

    // ---- Registration ---------------------------------------------------------------------------

    static class ModernPotion extends Potion
    {
        ModernPotion(boolean bad, int color)
        {
            super(bad, color);
        }
    }

    private static void potion(int id, String name, boolean bad, int color)
    {
        Potion p = new ModernPotion(bad, color).setPotionName("effect.m." + name);

        if (!bad)
        {
            p.setBeneficial();
        }

        Potion.REGISTRY.register(id, new ResourceLocation(name), p);
        EFFECTS.put(name, p);
    }

    /** Called from Potion.registerPotions(). */
    public static void registerPotions()
    {
        potion(28, "slow_falling", false, 0xF7F8E0);
        potion(29, "conduit_power", false, 0x1DC2D1);
        potion(30, "dolphins_grace", false, 0x88A3BE);
        potion(31, "bad_omen", true, 0x0B6138);
        potion(32, "hero_of_the_village", false, 0x44FF44);
        potion(33, "darkness", true, 0x292721);
        potion(34, "trial_omen", true, 0x16A6A6);
        potion(35, "raid_omen", true, 0xDE4058);
        potion(36, "wind_charged", true, 0xBDC9FF);
        potion(37, "weaving", true, 0x78695A);
        potion(38, "oozing", true, 0x99FFA3);
        potion(39, "infested", true, 0x8C9B8C);
    }

    static class ModernEnchantment extends Enchantment
    {
        private final int maxLevel;
        private final boolean treasure;

        ModernEnchantment(Rarity rarity, EnumEnchantmentType type, EntityEquipmentSlot[] slots, int max, boolean isTreasure)
        {
            super(rarity, type, slots);
            this.maxLevel = max;
            this.treasure = isTreasure;
        }

        public int getMaxLevel()
        {
            return this.maxLevel;
        }

        public int getMinEnchantability(int level)
        {
            return 1 + (level - 1) * 10;
        }

        public int getMaxEnchantability(int level)
        {
            return this.getMinEnchantability(level) + 30;
        }

        public boolean isTreasureEnchantment()
        {
            return this.treasure;
        }
    }

    private static void enchant(int id, String name, Enchantment.Rarity rarity, EnumEnchantmentType type, EntityEquipmentSlot slot, int max, boolean treasure)
    {
        Enchantment e = new ModernEnchantment(rarity, type, new EntityEquipmentSlot[] {slot}, max, treasure).setName("m." + name);
        Enchantment.REGISTRY.register(id, new ResourceLocation(name), e);
        ENCHANTS.put(name, e);
    }

    /** Called from Enchantment.registerEnchantments(). */
    public static void registerEnchantments()
    {
        EntityEquipmentSlot hand = EntityEquipmentSlot.MAINHAND;
        enchant(80, "swift_sneak", Enchantment.Rarity.VERY_RARE, EnumEnchantmentType.ARMOR_LEGS, EntityEquipmentSlot.LEGS, 3, true);
        enchant(81, "soul_speed", Enchantment.Rarity.VERY_RARE, EnumEnchantmentType.ARMOR_FEET, EntityEquipmentSlot.FEET, 3, true);
        enchant(82, "density", Enchantment.Rarity.UNCOMMON, EnumEnchantmentType.MACE, hand, 5, false);
        enchant(83, "breach", Enchantment.Rarity.RARE, EnumEnchantmentType.MACE, hand, 4, false);
        enchant(84, "wind_burst", Enchantment.Rarity.RARE, EnumEnchantmentType.MACE, hand, 3, true);
        enchant(85, "multishot", Enchantment.Rarity.RARE, EnumEnchantmentType.CROSSBOW, hand, 1, false);
        enchant(86, "piercing", Enchantment.Rarity.COMMON, EnumEnchantmentType.CROSSBOW, hand, 4, false);
        enchant(87, "quick_charge", Enchantment.Rarity.UNCOMMON, EnumEnchantmentType.CROSSBOW, hand, 3, false);
        enchant(88, "impaling", Enchantment.Rarity.RARE, EnumEnchantmentType.TRIDENT, hand, 5, false);
        enchant(89, "loyalty", Enchantment.Rarity.UNCOMMON, EnumEnchantmentType.TRIDENT, hand, 3, false);
        enchant(90, "riptide", Enchantment.Rarity.RARE, EnumEnchantmentType.TRIDENT, hand, 3, false);
        enchant(91, "channeling", Enchantment.Rarity.VERY_RARE, EnumEnchantmentType.TRIDENT, hand, 1, false);
        // 1.21.11: the spear's jab pushes you forward
        enchant(92, "lunge", Enchantment.Rarity.RARE, EnumEnchantmentType.SPEAR, hand, 3, false);
    }

    private static void potionType(String name, Potion effect, int duration, int amplifier)
    {
        PotionType.registerSythix(name, new PotionType(name.replace("long_", "").replace("strong_", ""), new PotionEffect(effect, duration, amplifier)));
    }

    /** Called from PotionType.registerPotionTypes(). */
    public static void registerPotionTypes()
    {
        potionType("slow_falling", EFFECTS.get("slow_falling"), 1800, 0);
        potionType("long_slow_falling", EFFECTS.get("slow_falling"), 4800, 0);
        potionType("wind_charged", EFFECTS.get("wind_charged"), 3600, 0);
        potionType("weaving", EFFECTS.get("weaving"), 3600, 0);
        potionType("oozing", EFFECTS.get("oozing"), 3600, 0);
        potionType("infested", EFFECTS.get("infested"), 3600, 0);
    }

    private static PotionType type(String name)
    {
        return PotionType.getPotionTypeForName(name);
    }

    /** Called from PotionHelper.init(). */
    public static void registerBrewing()
    {
        Item membrane = ModernBlocks.item("phantom_membrane");

        if (membrane != null)
        {
            PotionHelper.addMixSythix(PotionTypes.AWKWARD, membrane, type("slow_falling"));
            PotionHelper.addMixSythix(type("slow_falling"), Items.REDSTONE, type("long_slow_falling"));
        }

        Item windCharge = ModernBlocks.item("wind_charge");

        if (windCharge != null)
        {
            PotionHelper.addMixSythix(PotionTypes.AWKWARD, windCharge, type("wind_charged"));
        }

        PotionHelper.addMixSythix(PotionTypes.AWKWARD, Item.getItemFromBlock(Blocks.WEB), type("weaving"));
        PotionHelper.addMixSythix(PotionTypes.AWKWARD, Item.getItemFromBlock(Blocks.SLIME_BLOCK), type("oozing"));
        PotionHelper.addMixSythix(PotionTypes.AWKWARD, Item.getItemFromBlock(Blocks.STONE), type("infested"));
    }

    // ---- What the effects do ----------------------------------------------------------------------

    private static boolean has(EntityLivingBase e, String name)
    {
        Potion p = EFFECTS.get(name);
        return p != null && e.isPotionActive(p);
    }

    /** Every tick for every living entity (client and server). */
    public static void livingTick(EntityLivingBase e)
    {
        if (EFFECTS.isEmpty() || e.getActivePotionMap().isEmpty())
        {
            return;
        }

        if (has(e, "slow_falling") && e.motionY < -0.07D)
        {
            // Fall gently and take no fall damage
            e.motionY = -0.07D;
            e.fallDistance = 0.0F;
        }

        if (has(e, "dolphins_grace") && e.isInWater())
        {
            e.motionX *= 1.12D;
            e.motionZ *= 1.12D;
        }

        if (has(e, "conduit_power") && e.isInsideOfMaterial(Material.WATER))
        {
            e.setAir(300);
        }
    }

    /** When a living entity dies (server). */
    public static void onDeath(EntityLivingBase e)
    {
        World world = e.world;

        if (world.isRemote || EFFECTS.isEmpty())
        {
            return;
        }

        if (has(e, "wind_charged"))
        {
            new EntityWindCharge(world, e.posX, e.posY + 0.5D, e.posZ).burstNow();
        }

        if (has(e, "weaving"))
        {
            for (int i = 0; i < 3; ++i)
            {
                BlockPos p = new BlockPos(e).add(world.rand.nextInt(3) - 1, 0, world.rand.nextInt(3) - 1);

                if (world.isAirBlock(p))
                {
                    world.setBlockState(p, Blocks.WEB.getDefaultState());
                }
            }
        }

        if (has(e, "oozing"))
        {
            for (int i = 0; i < 2; ++i)
            {
                EntitySlime slime = new EntitySlime(world);
                slime.setLocationAndAngles(e.posX, e.posY, e.posZ, world.rand.nextFloat() * 360.0F, 0.0F);
                world.spawnEntity(slime);
            }
        }
    }

    /** When a living entity is hurt (server). Returns true to cancel the damage (never). */
    public static boolean onHurt(EntityLivingBase e, DamageSource source, float amount)
    {
        if (!EFFECTS.isEmpty() && has(e, "infested") && e.world.rand.nextFloat() < 0.1F)
        {
            for (int i = 0; i < 1 + e.world.rand.nextInt(2); ++i)
            {
                EntitySilverfish fish = new EntitySilverfish(e.world);
                fish.setLocationAndAngles(e.posX, e.posY, e.posZ, e.world.rand.nextFloat() * 360.0F, 0.0F);
                e.world.spawnEntity(fish);
            }
        }

        return false;
    }

    /** Sneaking speed multiplier for the local player (Swift Sneak). */
    public static double sneakSpeed()
    {
        Enchantment swift = ENCHANTS.get("swift_sneak");
        EntityPlayer p = Minecraft.getMinecraft().player;

        if (swift == null || p == null)
        {
            return 0.3D;
        }

        int level = EnchantmentHelper.getMaxEnchantmentLevel(swift, p);
        return Math.min(1.0D, 0.3D + 0.15D * (double)level);
    }

    /** Movement multiplier on soul sand (0.4 normally; Soul Speed makes it faster). */
    public static double soulSpeed(Entity entity)
    {
        Enchantment soul = ENCHANTS.get("soul_speed");

        if (soul == null || !(entity instanceof EntityLivingBase))
        {
            return 0.4D;
        }

        int level = EnchantmentHelper.getMaxEnchantmentLevel(soul, (EntityLivingBase)entity);
        return level <= 0 ? 0.4D : 1.0D + 0.1D * (double)level;
    }

    /** Level of a Sythix enchantment on a stack. */
    public static int level(String name, net.minecraft.item.ItemStack stack)
    {
        Enchantment e = ENCHANTS.get(name);
        return e == null ? 0 : EnchantmentHelper.getEnchantmentLevel(e, stack);
    }

    /** Darkness: how dark the screen is right now (0-1), pulsing. */
    public static float darkness()
    {
        EntityPlayer p = Minecraft.getMinecraft().player;

        if (p == null || EFFECTS.isEmpty() || !has(p, "darkness"))
        {
            return 0.0F;
        }

        return 0.55F + 0.35F * (float)Math.sin((double)p.ticksExisted * 0.15D);
    }
}
