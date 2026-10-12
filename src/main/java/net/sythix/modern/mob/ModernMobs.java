package net.sythix.modern.mob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

/**
 * The mobs from Minecraft 1.13-1.21: registration (ids 402+, with spawn eggs), where they spawn
 * naturally, and the special spawners (phantoms, wardens, wandering traders, striders).
 * Like the other 1.21 content they work in singleplayer and LAN worlds.
 */
public final class ModernMobs
{
    private static final List<String> NAMES = new ArrayList<String>();
    private static final Map<UUID, Long> LAST_REST = new HashMap<UUID, Long>();
    private static final Map<UUID, int[]> SHRIEKS = new HashMap<UUID, int[]>();

    private ModernMobs()
    {
    }

    public static int count()
    {
        return NAMES.size();
    }

    public static List<String> names()
    {
        return Collections.unmodifiableList(NAMES);
    }

    /** Called from EntityList.init. */
    public static void register()
    {
        int id = 402;
        reg(id++, "drowned", Drowned.class, 0x8FF1D7, 0x799C65);
        reg(id++, "bogged", Bogged.class, 0x8A9A5B, 0x314D1B);
        reg(id++, "pillager", Pillager.class, 0x532F36, 0x959B9B);
        reg(id++, "ravager", Ravager.class, 0x757470, 0x5B5049);
        reg(id++, "hoglin", Hoglin.class, 0xC66E55, 0x5F6464);
        reg(id++, "zoglin", Zoglin.class, 0xC66E55, 0xE6E6E6);
        reg(id++, "piglin", Piglin.class, 0x995F40, 0xF9F3A4);
        reg(id++, "piglin_brute", PiglinBrute.class, 0x592A10, 0xF9F3A4);
        reg(id++, "phantom", Phantom.class, 0x43518A, 0x88FF00);
        reg(id++, "warden", Warden.class, 0x0F4649, 0x39D6E0);
        reg(id++, "breeze", Breeze.class, 0xAF94DF, 0x9166DF);
        reg(id++, "creaking", Creaking.class, 0x5F5F5F, 0xFC7812);
        reg(id++, "zombie_nautilus", ZombieNautilus.class, 0x4E6B5A, 0xC8B8A0);
        reg(id++, "cod", Cod.class, 0xC1A76A, 0xE5C48B);
        reg(id++, "salmon", Salmon.class, 0xA00F10, 0x0E8474);
        reg(id++, "tropical_fish", TropicalFish.class, 0xEF6915, 0xFFF9EF);
        reg(id++, "pufferfish", Pufferfish.class, 0xF6B201, 0x37C3F2);
        reg(id++, "tadpole", Tadpole.class, 0x6D533D, 0x160A00);
        reg(id++, "dolphin", Dolphin.class, 0x223B4D, 0xF9F9F9);
        reg(id++, "axolotl", Axolotl.class, 0xFBC1E3, 0xA62D74);
        reg(id++, "glow_squid", GlowSquid.class, 0x095656, 0x85F1BC);
        reg(id++, "turtle", Turtle.class, 0xE7E7E7, 0x00AFAF);
        reg(id++, "frog", Frog.class, 0xD07444, 0xFFC77C);
        reg(id++, "fox", Fox.class, 0xD5B69F, 0xCC6920);
        reg(id++, "panda", Panda.class, 0xE7E7E7, 0x1B1B22);
        reg(id++, "bee", Bee.class, 0xEDC343, 0x43241B);
        reg(id++, "goat", Goat.class, 0xA5947C, 0x55493E);
        reg(id++, "camel", Camel.class, 0xFCC369, 0xCB9337);
        reg(id++, "trader_llama", TraderLlama.class, 0xEAA430, 0x456296);
        reg(id++, "wandering_trader", WanderingTrader.class, 0x456296, 0xEAA430);
        reg(id++, "sniffer", Sniffer.class, 0x871E09, 0x22A787);
        reg(id++, "armadillo", Armadillo.class, 0xAD716D, 0x824848);
        reg(id++, "allay", Allay.class, 0x00DAFF, 0x00ADFF);
        reg(id++, "strider", Strider.class, 0x9C3436, 0x4D494D);
        reg(id++, "happy_ghast", HappyGhast.class, 0xF9F9F9, 0xBCBCBC);

        for (Class<?> c : new Class<?>[] {Drowned.class, ZombieNautilus.class, Cod.class, Salmon.class, TropicalFish.class, Pufferfish.class, Tadpole.class, Dolphin.class, Axolotl.class, GlowSquid.class})
        {
            EntitySpawnPlacementRegistry.setSythix(c, EntityLiving.SpawnPlacementType.IN_WATER);
        }

        for (Class<?> c : new Class<?>[] {Bogged.class, Pillager.class, Ravager.class, Hoglin.class, Zoglin.class, Piglin.class, PiglinBrute.class, Breeze.class, Creaking.class,
            Turtle.class, Frog.class, Fox.class, Panda.class, Bee.class, Goat.class, Camel.class, Armadillo.class})
        {
            EntitySpawnPlacementRegistry.setSythix(c, EntityLiving.SpawnPlacementType.ON_GROUND);
        }
    }

    private static void reg(int id, String name, Class <? extends EntityLiving > clazz, int primary, int secondary)
    {
        EntityList.registerSythix(id, name, clazz, "sythix." + name, primary, secondary);
        NAMES.add(name);
    }

    /** Called from Bootstrap after the biomes are registered: adds the mobs to the biome spawn lists. */
    public static void registerSpawns()
    {
        for (Biome biome : Biome.REGISTRY)
        {
            String n = Biome.REGISTRY.getNameForObject(biome).getResourcePath();
            boolean frozen = n.contains("frozen") || n.contains("cold") || n.contains("ice");

            if (n.contains("ocean"))
            {
                add(biome, EnumCreatureType.WATER_CREATURE, frozen ? Salmon.class : Cod.class, 15, 3, 6);
                add(biome, EnumCreatureType.WATER_CREATURE, GlowSquid.class, 4, 1, 2);
                add(biome, EnumCreatureType.MONSTER, Drowned.class, 5, 1, 2);

                if (!frozen)
                {
                    add(biome, EnumCreatureType.WATER_CREATURE, TropicalFish.class, 5, 3, 6);
                    add(biome, EnumCreatureType.WATER_CREATURE, Pufferfish.class, 3, 1, 3);
                    add(biome, EnumCreatureType.WATER_CREATURE, Dolphin.class, 2, 1, 2);
                }

                if (n.contains("deep"))
                {
                    add(biome, EnumCreatureType.MONSTER, ZombieNautilus.class, 1, 1, 1);
                }
            }
            else if (n.contains("river"))
            {
                add(biome, EnumCreatureType.WATER_CREATURE, Salmon.class, 10, 2, 4);
                add(biome, EnumCreatureType.WATER_CREATURE, Axolotl.class, 1, 1, 2);
                add(biome, EnumCreatureType.MONSTER, Drowned.class, 5, 1, 1);
            }
            else if (n.contains("swamp"))
            {
                add(biome, EnumCreatureType.MONSTER, Bogged.class, 30, 1, 2);
                add(biome, EnumCreatureType.CREATURE, Frog.class, 10, 2, 5);
                add(biome, EnumCreatureType.WATER_CREATURE, Axolotl.class, 2, 1, 2);
            }
            else if (n.equals("hell"))
            {
                add(biome, EnumCreatureType.MONSTER, Piglin.class, 40, 2, 4);
                add(biome, EnumCreatureType.MONSTER, Hoglin.class, 25, 2, 4);
                add(biome, EnumCreatureType.MONSTER, PiglinBrute.class, 2, 1, 1);
            }
            else if (n.contains("jungle"))
            {
                add(biome, EnumCreatureType.CREATURE, Panda.class, 2, 1, 2);
            }
            else if (n.contains("plains"))
            {
                add(biome, EnumCreatureType.CREATURE, Bee.class, 4, 2, 3);
                add(biome, EnumCreatureType.MONSTER, Pillager.class, 3, 1, 3);
                add(biome, EnumCreatureType.MONSTER, Ravager.class, 1, 1, 1);
            }
            else if (n.contains("roofed_forest"))
            {
                add(biome, EnumCreatureType.MONSTER, Creaking.class, 15, 1, 1);
            }
            else if (n.contains("forest") && !n.contains("birch"))
            {
                add(biome, EnumCreatureType.CREATURE, Bee.class, 2, 2, 3);
            }
            else if (n.contains("taiga"))
            {
                add(biome, EnumCreatureType.CREATURE, Fox.class, 8, 2, 4);
                add(biome, EnumCreatureType.MONSTER, Pillager.class, 2, 1, 2);
            }
            else if (n.contains("extreme_hills") || n.equals("ice_mountains"))
            {
                add(biome, EnumCreatureType.CREATURE, Goat.class, 5, 1, 3);
                add(biome, EnumCreatureType.MONSTER, Breeze.class, 3, 1, 1);
            }
            else if (n.equals("desert") || n.equals("mutated_desert"))
            {
                add(biome, EnumCreatureType.CREATURE, Camel.class, 1, 1, 1);
                add(biome, EnumCreatureType.MONSTER, Pillager.class, 2, 1, 2);
            }
            else if (n.contains("savanna"))
            {
                add(biome, EnumCreatureType.CREATURE, Armadillo.class, 6, 1, 2);
                add(biome, EnumCreatureType.MONSTER, Pillager.class, 2, 1, 2);
            }
            else if (n.contains("mesa"))
            {
                add(biome, EnumCreatureType.CREATURE, Armadillo.class, 6, 1, 2);
            }
            else if (n.equals("beaches"))
            {
                add(biome, EnumCreatureType.CREATURE, Turtle.class, 5, 2, 5);
            }
        }
    }

    private static void add(Biome biome, EnumCreatureType type, Class <? extends EntityLiving > clazz, int weight, int min, int max)
    {
        biome.getSpawnableList(type).add(new Biome.SpawnListEntry(clazz, weight, min, max));
    }

    /** Called every world tick (when mob spawning is on) for the special spawners. */
    public static void worldTick(WorldServer world)
    {
        if (!MobUtil.naturalSpawns() || world.playerEntities.isEmpty())
        {
            return;
        }

        long time = world.getTotalWorldTime();

        if (world.provider.isSurfaceWorld())
        {
            if (time % 20L == 0L)
            {
                shriekers(world);
            }

            phantoms(world, time);

            if (time % 24000L == 12000L && world.rand.nextInt(4) == 0)
            {
                wanderingTrader(world);
            }
        }
        else if (world.provider.isNether() && time % 200L == 0L)
        {
            striders(world);
        }
    }

    /** Stepping on sculk shriekers: the fourth warning summons a warden. */
    public static void shriekers(WorldServer world)
    {
        Block shrieker = MobUtil.blockOrNull("sculk_shrieker");

        for (EntityPlayer p : world.playerEntities)
        {
            int[] s = SHRIEKS.computeIfAbsent(p.getUniqueID(), k -> new int[2]);

            if (s[1] > 0)
            {
                --s[1];
            }

            if (shrieker == null || p.capabilities.isCreativeMode || p.isSpectator() || p.isSneaking() || s[1] > 0)
            {
                continue;
            }

            BlockPos feet = new BlockPos(p.posX, p.posY - 0.1D, p.posZ);

            if (world.getBlockState(feet).getBlock() != shrieker && world.getBlockState(feet.up()).getBlock() != shrieker)
            {
                continue;
            }

            s[1] = 10;
            ++s[0];
            world.playSound(null, feet, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.0F, 0.5F + s[0] * 0.1F);

            if (s[0] >= 4 && world.getDifficulty() != EnumDifficulty.PEACEFUL)
            {
                s[0] = 0;
                Warden warden = new Warden(world);

                for (int i = 0; i < 20; ++i)
                {
                    BlockPos at = feet.add(world.rand.nextInt(11) - 5, 0, world.rand.nextInt(11) - 5);

                    for (int dy = 4; dy > -6; --dy)
                    {
                        BlockPos q = at.up(dy);

                        if (world.getBlockState(q.down()).isTopSolid() && world.isAirBlock(q) && world.isAirBlock(q.up()) && world.isAirBlock(q.up(2)))
                        {
                            warden.setLocationAndAngles(q.getX() + 0.5D, q.getY(), q.getZ() + 0.5D, world.rand.nextFloat() * 360.0F, 0.0F);
                            world.spawnEntity(warden);
                            warden.setAttackTarget(p);
                            return;
                        }
                    }
                }
            }
        }
    }

    /** Phantoms come for players who have not slept for three in-game days. */
    private static void phantoms(WorldServer world, long time)
    {
        for (EntityPlayer p : world.playerEntities)
        {
            if (p.isPlayerSleeping())
            {
                LAST_REST.put(p.getUniqueID(), time);
            }
            else
            {
                LAST_REST.putIfAbsent(p.getUniqueID(), time);
            }
        }

        long day = world.getWorldTime() % 24000L;

        if (time % 1200L != 0L || day < 13000L || day > 23000L || world.getDifficulty() == EnumDifficulty.PEACEFUL)
        {
            return;
        }

        for (EntityPlayer p : world.playerEntities)
        {
            if (p.capabilities.isCreativeMode || p.isSpectator() || time - LAST_REST.get(p.getUniqueID()) < 72000L || !world.canSeeSky(new BlockPos(p)) || world.rand.nextInt(2) != 0)
            {
                continue;
            }

            int n = 1 + world.rand.nextInt(world.getDifficulty().getDifficultyId() + 1);

            for (int i = 0; i < n; ++i)
            {
                Phantom ph = new Phantom(world);
                ph.setLocationAndAngles(p.posX + world.rand.nextInt(11) - 5, p.posY + 20 + world.rand.nextInt(15), p.posZ + world.rand.nextInt(11) - 5, world.rand.nextFloat() * 360.0F, 0.0F);

                if (world.isAirBlock(new BlockPos(ph)))
                {
                    world.spawnEntity(ph);
                }
            }
        }
    }

    /** About once every four days a wandering trader with two llamas appears near a player. */
    private static void wanderingTrader(WorldServer world)
    {
        if (!world.getEntities(WanderingTrader.class, e -> e.isEntityAlive()).isEmpty())
        {
            return;
        }

        EntityPlayer p = world.playerEntities.get(world.rand.nextInt(world.playerEntities.size()));

        for (int i = 0; i < 10; ++i)
        {
            BlockPos column = new BlockPos(p.posX + world.rand.nextInt(33) - 16, 0, p.posZ + world.rand.nextInt(33) - 16);
            BlockPos at = world.getTopSolidOrLiquidBlock(column);

            if (!world.getBlockState(at.down()).isTopSolid() || world.getBlockState(at).getMaterial().isLiquid() || !world.isAirBlock(at.up()))
            {
                continue;
            }

            WanderingTrader trader = new WanderingTrader(world);
            trader.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, world.rand.nextFloat() * 360.0F, 0.0F);
            world.spawnEntity(trader);

            for (int l = 0; l < 2; ++l)
            {
                TraderLlama llama = new TraderLlama(world);
                llama.setLocationAndAngles(at.getX() + 0.5D + (l == 0 ? 2 : -2), at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
                llama.onInitialSpawn(world.getDifficultyForLocation(at), null);
                world.spawnEntity(llama);
                llama.setLeashedToEntity(trader, true);
            }

            return;
        }
    }

    /** Striders appear on the lava seas of the Nether. */
    private static void striders(WorldServer world)
    {
        for (EntityPlayer p : world.playerEntities)
        {
            if (world.getEntitiesWithinAABB(Strider.class, p.getEntityBoundingBox().grow(64.0D)).size() >= 4)
            {
                continue;
            }

            for (int i = 0; i < 8; ++i)
            {
                int x = (int)p.posX + world.rand.nextInt(65) - 32;
                int z = (int)p.posZ + world.rand.nextInt(65) - 32;

                if (Math.abs(x - p.posX) < 16 && Math.abs(z - p.posZ) < 16)
                {
                    continue;
                }

                for (int y = Math.min(120, (int)p.posY + 16); y > Math.max(4, (int)p.posY - 24); --y)
                {
                    BlockPos q = new BlockPos(x, y, z);

                    if (world.getBlockState(q).getMaterial() == Material.LAVA && world.isAirBlock(q.up()) && world.isAirBlock(q.up(2)))
                    {
                        Strider s = new Strider(world);
                        s.setLocationAndAngles(x + 0.5D, y + 1.0D, z + 0.5D, world.rand.nextFloat() * 360.0F, 0.0F);
                        world.spawnEntity(s);
                        return;
                    }
                }
            }
        }
    }
}
