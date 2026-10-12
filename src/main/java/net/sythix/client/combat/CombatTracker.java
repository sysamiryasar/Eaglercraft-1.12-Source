package net.sythix.client.combat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityMinecartTNT;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.sythix.client.replay.ReplayPlayer;
import net.sythix.client.replay.ReplayRecorder;

/**
 * Watches the game for fights, end crystals, TNT minecarts and totem pops. It only reads what the
 * client already receives (entities, health, explosions, totem effects) and never sends anything,
 * so everything built on it is display and statistics only.
 *
 * Runs on the client thread: tick() every client tick, the on... methods from small hooks in
 * Minecraft's code (attacking, right-clicking a block, explosions, totem pops).
 */
public final class CombatTracker
{
    private static final double SCAN_RANGE = 24.0D;
    private static final double FIGHT_RANGE = 12.0D;

    /** A crystal or TNT minecart the tracker has seen. */
    private static final class Tracked
    {
        final boolean crystal;
        final long spawned;
        final Vec3d pos;
        boolean mine;
        boolean lit;
        long attacked;

        Tracked(boolean crystal, long spawned, Vec3d pos)
        {
            this.crystal = crystal;
            this.spawned = spawned;
            this.pos = pos;
        }
    }

    /** Live counters since the last reset (shown by the Crystal Flow Analyzer). */
    public static final class Session
    {
        public long started = Minecraft.getSystemTime();
        public int crystalsPlaced;
        public int crystalsBroken;
        public int obsidianPlaced;
        public int cartsPlaced;
        public int cartsLit;
        public int cartsDetonated;
        public int railsPlaced;
        public int popsSelf;
        public int popsOthers;
        public float bestCrystalRate;
        public final Deque<Long> breakDelays = new ArrayDeque<Long>();
        public final Deque<Long> railToCart = new ArrayDeque<Long>();
    }

    private static final List<Consumer<Fight>> LISTENERS = new ArrayList<Consumer<Fight>>();
    private static final Map<Integer, Tracked> TRACKED = new HashMap<Integer, Tracked>();
    private static final Deque<Long> PLACE_TIMES = new ArrayDeque<Long>();
    private static final Deque<Long> BREAK_TIMES = new ArrayDeque<Long>();
    private static final Deque<double[]> EXPLOSIONS = new ArrayDeque<double[]>();
    private static Session session = new Session();

    /** Milliseconds without combat before a fight ends (set by Combat Replay). */
    public static long fightTimeout = 8000L;
    /** Whether fights against mobs count outside the Training Lab. */
    public static boolean mobFights;
    /** Whether Combat Replay adds markers to a replay that is being recorded. */
    public static boolean replayMarkers = true;

    private static Fight fight;
    private static EntityLivingBase opponent;
    private static long fightStart;
    private static long lastActivity;
    private static long lastCrystalPlace;
    private static float lastHp = -1.0F;
    private static float lastOppHp = -1.0F;
    private static int lastHurtTime;
    private static int ticks;
    private static Object lastWorld;
    private static long pendingCrystal;
    private static BlockPos pendingCrystalPos;
    private static long pendingCart;
    private static BlockPos pendingCartPos;
    private static long lastRail;
    private static long lastMelee;

    private CombatTracker()
    {
    }

    /** Called with every finished fight. */
    public static void addListener(Consumer<Fight> listener)
    {
        LISTENERS.add(listener);
    }

    public static Session session()
    {
        return session;
    }

    public static void resetSession()
    {
        session = new Session();
        PLACE_TIMES.clear();
        BREAK_TIMES.clear();
    }

    /** The fight in progress, or null. */
    public static Fight current()
    {
        return fight;
    }

    public static EntityLivingBase currentOpponent()
    {
        return opponent;
    }

    /** Crystals you placed per second over the last 3 seconds. */
    public static float placeRate()
    {
        return rate(PLACE_TIMES);
    }

    /** Crystals you broke per second over the last 3 seconds. */
    public static float breakRate()
    {
        return rate(BREAK_TIMES);
    }

    private static float rate(Deque<Long> times)
    {
        long now = Minecraft.getSystemTime();

        while (!times.isEmpty() && now - times.peekFirst().longValue() > 3000L)
        {
            times.pollFirst();
        }

        return (float)times.size() / 3.0F;
    }

    private static Minecraft mc()
    {
        return Minecraft.getMinecraft();
    }

    private static boolean active()
    {
        Minecraft mc = mc();
        return mc.player != null && mc.world != null && !ReplayPlayer.isActive();
    }

    // ---- Hooks ----------------------------------------------------------------------------

    /** The player right-clicked a block with this item (before the game sends it). */
    public static void onUseOnBlock(ItemStack stack, BlockPos pos)
    {
        if (!active() || stack == null || stack.isEmpty())
        {
            return;
        }

        long now = Minecraft.getSystemTime();
        Item item = stack.getItem();
        Block clicked = mc().world.getBlockState(pos).getBlock();

        if (item == Items.END_CRYSTAL && (clicked == Blocks.OBSIDIAN || clicked == Blocks.BEDROCK))
        {
            pendingCrystal = now;
            pendingCrystalPos = pos.up();
        }
        else if (item == Items.TNT_MINECART && clicked instanceof BlockRailBase)
        {
            pendingCart = now;
            pendingCartPos = pos;
        }
        else if (Block.getBlockFromItem(item) instanceof BlockRailBase)
        {
            lastRail = now;
            ++session.railsPlaced;

            if (fight != null)
            {
                ++fight.railsPlaced;
            }
        }
        else if (Block.getBlockFromItem(item) == Blocks.OBSIDIAN)
        {
            ++session.obsidianPlaced;

            if (fight != null)
            {
                ++fight.obsidianPlaced;
            }
        }
    }

    /** The player attacked an entity. */
    public static void onAttack(Entity target)
    {
        if (!active() || target == null)
        {
            return;
        }

        long now = Minecraft.getSystemTime();

        if (target instanceof EntityEnderCrystal)
        {
            Tracked t = TRACKED.get(Integer.valueOf(target.getEntityId()));

            if (t != null && t.attacked == 0L)
            {
                t.attacked = now;
            }

            return;
        }

        if (!(target instanceof EntityLivingBase) || !countsAsOpponent((EntityLivingBase)target))
        {
            return;
        }

        lastMelee = now;
        ensureFight((EntityLivingBase)target, now);

        if (fight != null && target == opponent)
        {
            ++fight.hitsDealt;
            lastActivity = now;
            event("hit", "You hit " + opponent.getName());
        }
    }

    /** An explosion happened (from the server's explosion packet). */
    public static void onExplosion(double x, double y, double z, float strength)
    {
        if (!active())
        {
            return;
        }

        EXPLOSIONS.addLast(new double[] {x, y, z, (double)strength, (double)Minecraft.getSystemTime()});

        while (EXPLOSIONS.size() > 32)
        {
            EXPLOSIONS.pollFirst();
        }
    }

    /** An entity used a totem of undying. */
    public static void onTotemPop(Entity entity)
    {
        if (!active() || entity == null)
        {
            return;
        }

        Minecraft mc = mc();
        long now = Minecraft.getSystemTime();

        if (entity == mc.player)
        {
            ++session.popsSelf;

            if (fight != null)
            {
                ++fight.popsSelf;
                lastActivity = now;
                event("pop", "You popped a totem");
            }

            return;
        }

        if (!(entity instanceof EntityLivingBase))
        {
            return;
        }

        ++session.popsOthers;

        if (fight == null && entity instanceof EntityPlayer && mc.player.getDistanceToEntity(entity) < FIGHT_RANGE && now - Math.max(lastCrystalPlace, lastMelee) < 3000L)
        {
            ensureFight((EntityLivingBase)entity, now);
        }

        if (fight != null && entity == opponent)
        {
            ++fight.popsOpponent;
            lastActivity = now;
            event("pop-them", opponent.getName() + " popped a totem");
        }
    }

    // ---- Tick -------------------------------------------------------------------------------

    public static void tick()
    {
        Minecraft mc = mc();

        if (mc.world != lastWorld)
        {
            lastWorld = mc.world;
            endFight(Fight.DRAW, "Left the world");
            TRACKED.clear();
            EXPLOSIONS.clear();
            lastHp = -1.0F;
        }

        if (!active())
        {
            return;
        }

        long now = Minecraft.getSystemTime();
        ++ticks;
        scanEntities(now);
        trackOwnHealth(now);

        if (fight != null)
        {
            trackOpponent(now);
        }

        if (fight != null && now - lastActivity > fightTimeout)
        {
            endFight(Fight.DRAW, "No combat for " + fightTimeout / 1000L + "s");
        }

        if (fight != null && ticks % 10 == 0)
        {
            fight.health.add(new float[] {(float)(now - fightStart) / 1000.0F, hp(mc.player), opponent == null ? 0.0F : hp(opponent)});
        }
    }

    private static void scanEntities(long now)
    {
        Minecraft mc = mc();
        Map<Integer, Entity> seen = new HashMap<Integer, Entity>();

        for (Entity e : mc.world.loadedEntityList)
        {
            if ((e instanceof EntityEnderCrystal || e instanceof EntityMinecartTNT) && !e.isDead && mc.player.getDistanceSqToEntity(e) < SCAN_RANGE * SCAN_RANGE)
            {
                seen.put(Integer.valueOf(e.getEntityId()), e);
            }
        }

        // new crystals and carts
        for (Entity e : seen.values())
        {
            Integer id = Integer.valueOf(e.getEntityId());
            Tracked t = TRACKED.get(id);

            if (t == null)
            {
                boolean crystal = e instanceof EntityEnderCrystal;
                t = new Tracked(crystal, now, e.getPositionVector());
                TRACKED.put(id, t);
                newObject(t, e, now);
            }

            if (!t.crystal && !t.lit && ((EntityMinecartTNT)e).isIgnited())
            {
                t.lit = true;

                if (t.mine)
                {
                    ++session.cartsLit;
                    event("cart", "Your TNT minecart was lit");
                }
            }
        }

        // crystals and carts that are gone
        for (Iterator<Map.Entry<Integer, Tracked>> it = TRACKED.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry<Integer, Tracked> entry = it.next();

            if (!seen.containsKey(entry.getKey()))
            {
                gone(entry.getValue(), now);
                it.remove();
            }
        }
    }

    private static void newObject(Tracked t, Entity e, long now)
    {
        Minecraft mc = mc();

        if (t.crystal)
        {
            t.mine = pendingCrystalPos != null && now - pendingCrystal < 1000L && e.getDistanceSq(pendingCrystalPos) < 4.0D;

            if (t.mine)
            {
                pendingCrystalPos = null;
                ++session.crystalsPlaced;
                PLACE_TIMES.addLast(Long.valueOf(now));
                float rate = placeRate();
                session.bestCrystalRate = Math.max(session.bestCrystalRate, rate);
                ensureFight(nearestOpponent(e.getPositionVector()), now);

                if (fight != null)
                {
                    if (fight.crystalsPlaced > 0)
                    {
                        fight.longestCrystalGap = Math.max(fight.longestCrystalGap, now - lastCrystalPlace);
                    }

                    ++fight.crystalsPlaced;
                    fight.bestCrystalRate = Math.max(fight.bestCrystalRate, rate);
                    lastActivity = now;
                    event("crystal", "You placed a crystal");
                }

                lastCrystalPlace = now;
            }
            else if (fight != null && opponent != null && e.getDistanceToEntity(opponent) < 6.0F)
            {
                ++fight.enemyCrystals;
                lastActivity = now;
                event("enemy", opponent.getName() + " placed a crystal");
            }
        }
        else
        {
            t.mine = pendingCartPos != null && now - pendingCart < 1000L && e.getDistanceSq(pendingCartPos) < 4.0D;

            if (t.mine)
            {
                pendingCartPos = null;
                ++session.cartsPlaced;
                long railDelay = now - lastRail;

                if (railDelay < 3000L)
                {
                    push(session.railToCart, railDelay, 10);
                }

                ensureFight(nearestOpponent(e.getPositionVector()), now);

                if (fight != null)
                {
                    ++fight.cartsPlaced;

                    if (railDelay < 3000L)
                    {
                        fight.railToCartSum += railDelay;
                        ++fight.railToCartCount;
                    }

                    lastActivity = now;
                    event("cart", "You placed a TNT minecart" + (railDelay < 3000L ? " (" + railDelay + " ms after the rail)" : ""));
                }
            }
        }
    }

    private static void gone(Tracked t, long now)
    {
        if (t.crystal)
        {
            if (t.attacked > 0L && now - t.attacked < 1500L)
            {
                ++session.crystalsBroken;
                BREAK_TIMES.addLast(Long.valueOf(now));
                long delay = t.mine ? t.attacked - t.spawned : -1L;

                if (delay >= 0L)
                {
                    push(session.breakDelays, delay, 20);
                }

                if (fight != null)
                {
                    ++fight.crystalsBroken;

                    if (delay >= 0L)
                    {
                        fight.breakDelaySum += delay;
                        ++fight.breakDelayCount;
                        fight.bestBreakDelay = fight.bestBreakDelay < 0L ? delay : Math.min(fight.bestBreakDelay, delay);
                    }

                    lastActivity = now;
                    event("break", "You broke a crystal" + (delay >= 0L ? " (" + delay + " ms after placing)" : ""));
                }
            }
        }
        else if (t.mine)
        {
            ++session.cartsDetonated;

            if (fight != null)
            {
                ++fight.cartsDetonated;
                lastActivity = now;
                event("cart", "Your TNT minecart went off");
            }
        }
    }

    private static void trackOwnHealth(long now)
    {
        Minecraft mc = mc();
        float hp = hp(mc.player);
        int hurt = mc.player.hurtTime;
        boolean newHit = hurt > lastHurtTime;
        lastHurtTime = hurt;

        if (lastHp >= 0.0F && hp < lastHp - 0.01F)
        {
            float dmg = lastHp - hp;
            String source = damageSource(mc.player.getPositionVector(), now);

            if (source.equals("Melee") || source.equals("Crystal") || source.equals("Cart"))
            {
                ensureFight(nearestOpponent(mc.player.getPositionVector()), now);
            }

            if (fight != null)
            {
                fight.damageTaken += dmg;
                lastActivity = now;

                if (source.equals("Crystal"))
                {
                    fight.takenCrystal += dmg;
                }
                else if (source.equals("Cart"))
                {
                    fight.takenCart += dmg;
                }
                else if (source.equals("Melee"))
                {
                    fight.takenMelee += dmg;

                    if (newHit)
                    {
                        ++fight.hitsTaken;
                    }
                }
                else
                {
                    fight.takenOther += dmg;
                }

                event("hurt", String.format("You took %.1f damage (%s)", dmg, source.toLowerCase()));
            }
        }

        lastHp = hp;

        if (fight != null && mc.player.getHealth() <= 0.0F)
        {
            endFight(Fight.LOSS, "You died");
        }
    }

    private static void trackOpponent(long now)
    {
        Minecraft mc = mc();

        if (opponent == null)
        {
            return;
        }

        boolean present = mc.world.getEntityByID(opponent.getEntityId()) == opponent;
        float hp = hp(opponent);

        if (lastOppHp >= 0.0F && hp < lastOppHp - 0.01F)
        {
            float dmg = lastOppHp - hp;
            fight.damageDealt += dmg;
            lastActivity = now;
            boolean explosion = recentExplosion(opponent.getPositionVector(), now) != null;

            if (now - lastMelee < 400L && !explosion)
            {
                fight.dealtMelee += dmg;
            }
            else if (explosion)
            {
                fight.dealtExplosion += dmg;
            }

            event("dealt", String.format("%s took %.1f damage%s", opponent.getName(), dmg, explosion ? " (explosion)" : ""));
        }

        lastOppHp = hp;

        if (opponent.getHealth() <= 0.0F || (opponent.isDead && opponent.deathTime > 0))
        {
            endFight(Fight.WIN, opponent.getName() + " died");
        }
        else if (!present || opponent.isDead)
        {
            endFight(Fight.DRAW, opponent.getName() + " left");
        }
        else if (mc.player.getDistanceToEntity(opponent) > 48.0F)
        {
            endFight(Fight.DRAW, opponent.getName() + " got away");
        }
    }

    // ---- Fight lifecycle --------------------------------------------------------------------

    private static boolean countsAsOpponent(EntityLivingBase e)
    {
        if (e == null || e == mc().player || e instanceof EntityArmorStand)
        {
            return false;
        }

        return e instanceof EntityPlayer || mobFights || TrainingWorld.isActive();
    }

    private static EntityLivingBase nearestOpponent(Vec3d near)
    {
        Minecraft mc = mc();

        if (fight != null)
        {
            return opponent;
        }

        EntityLivingBase best = null;
        double bestDist = FIGHT_RANGE * FIGHT_RANGE;

        for (Entity e : mc.world.loadedEntityList)
        {
            if (e instanceof EntityLivingBase && !e.isDead && countsAsOpponent((EntityLivingBase)e))
            {
                double d = e.getDistanceSq(near.x, near.y, near.z);

                // players first: a mob only counts when no player is close
                if (e instanceof EntityPlayer)
                {
                    d *= 0.25D;
                }

                if (d < bestDist)
                {
                    bestDist = d;
                    best = (EntityLivingBase)e;
                }
            }
        }

        return best;
    }

    private static void ensureFight(EntityLivingBase target, long now)
    {
        if (fight != null || target == null || !countsAsOpponent(target))
        {
            return;
        }

        fight = new Fight();
        opponent = target;
        fightStart = now;
        lastActivity = now;
        lastOppHp = hp(target);
        fight.startEpoch = System.currentTimeMillis();
        fight.opponent = target.getName();
        fight.opponentIsPlayer = target instanceof EntityPlayer;
        fight.training = TrainingWorld.isActive();
        fight.server = net.sythix.client.waypoint.WaypointStore.currentKey();

        if (ReplayRecorder.isRecording())
        {
            java.io.File file = ReplayRecorder.current();

            if (file != null)
            {
                fight.replayFile = file.getName();
                fight.replayOffset = ReplayRecorder.elapsed();

                if (replayMarkers)
                {
                    ReplayRecorder.marker();
                }
            }
        }

        event("start", "Fight started against " + target.getName());
    }

    private static void endFight(String result, String why)
    {
        if (fight == null)
        {
            return;
        }

        Fight f = fight;
        long now = Minecraft.getSystemTime();
        f.duration = Math.max(0L, (WIN.equals(result) || Fight.LOSS.equals(result) ? now : lastActivity) - fightStart);
        f.result = result;
        event("end", why);
        fight = null;
        opponent = null;
        lastOppHp = -1.0F;

        // a single stray hit or explosion is not a fight
        if (f.hitsDealt + f.hitsTaken + f.crystalsPlaced + f.cartsPlaced + f.popsSelf + f.popsOpponent < 2 && f.damageDealt + f.damageTaken < 4.0F)
        {
            return;
        }

        if (replayMarkers && f.replayFile != null && ReplayRecorder.isRecording())
        {
            ReplayRecorder.marker();
        }

        for (Consumer<Fight> l : LISTENERS)
        {
            try
            {
                l.accept(f);
            }
            catch (RuntimeException e)
            {
                net.sythix.client.core.ModuleErrors.report(null, "fight listener", e);
            }
        }
    }

    private static final String WIN = Fight.WIN;

    /** Ends the current fight now (for example from a key). */
    public static void endNow()
    {
        endFight(Fight.DRAW, "Ended by you");
    }

    // ---- Helpers ----------------------------------------------------------------------------

    private static void event(String kind, String text)
    {
        if (fight != null)
        {
            fight.events.add(new Fight.Event(Minecraft.getSystemTime() - fightStart, kind, text));

            if (fight.events.size() > 2000)
            {
                fight.events.remove(1);
            }
        }
    }

    private static float hp(EntityLivingBase e)
    {
        return e.getHealth() + e.getAbsorptionAmount();
    }

    private static void push(Deque<Long> deque, long value, int max)
    {
        deque.addLast(Long.valueOf(value));

        while (deque.size() > max)
        {
            deque.pollFirst();
        }
    }

    private static double[] recentExplosion(Vec3d at, long now)
    {
        for (Iterator<double[]> it = EXPLOSIONS.descendingIterator(); it.hasNext();)
        {
            double[] x = it.next();

            if (now - (long)x[4] > 400L)
            {
                break;
            }

            if (at.squareDistanceTo(x[0], x[1], x[2]) < 144.0D)
            {
                return x;
            }
        }

        return null;
    }

    /** What most likely hurt you: Crystal, Cart, Melee or Other. */
    private static String damageSource(Vec3d at, long now)
    {
        double[] x = recentExplosion(at, now);

        if (x != null)
        {
            return x[3] >= 5.5D ? "Crystal" : "Cart";
        }

        Minecraft mc = mc();

        for (Entity e : mc.world.playerEntities)
        {
            if (e != mc.player && mc.player.getDistanceToEntity(e) < 6.0F)
            {
                return "Melee";
            }
        }

        if (opponent != null && mc.player.getDistanceToEntity(opponent) < 5.0F)
        {
            return "Melee";
        }

        return "Other";
    }
}
