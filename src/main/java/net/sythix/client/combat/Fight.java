package net.sythix.client.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * One fight as seen by the client: who it was against, how it ended, and what happened (hits,
 * damage, crystals, TNT minecarts, totem pops). Recorded by {@link CombatTracker}; used by Combat
 * Replay (review), Duel Analytics (history) and the Crystal Flow Analyzer.
 */
public final class Fight
{
    public static final String WIN = "Win";
    public static final String LOSS = "Loss";
    public static final String DRAW = "Draw";

    /** Something that happened during the fight; t is milliseconds since the fight started. */
    public static final class Event
    {
        public final long t;
        public final String kind;
        public final String text;

        public Event(long t, String kind, String text)
        {
            this.t = t;
            this.kind = kind;
            this.text = text;
        }
    }

    public long startEpoch;
    public long duration;
    public String opponent = "?";
    public boolean opponentIsPlayer;
    public String result = DRAW;
    public String server = "";
    public boolean training;

    public int hitsDealt;
    public int hitsTaken;
    public float damageDealt;
    public float damageTaken;
    public float dealtExplosion;
    public float dealtMelee;
    public float takenCrystal;
    public float takenCart;
    public float takenMelee;
    public float takenOther;

    public int crystalsPlaced;
    public int crystalsBroken;
    public int enemyCrystals;
    public long breakDelaySum;
    public int breakDelayCount;
    public long bestBreakDelay = -1L;
    public float bestCrystalRate;
    public long longestCrystalGap;
    public int obsidianPlaced;

    public int cartsPlaced;
    public int cartsDetonated;
    public int railsPlaced;
    public long railToCartSum;
    public int railToCartCount;

    public int popsSelf;
    public int popsOpponent;

    /** Replay this fight is in (file name inside the replays folder) and where it starts, or null / -1. */
    public String replayFile;
    public long replayOffset = -1L;

    public final List<Event> events = new ArrayList<Event>();
    /** {seconds, your health, opponent health} every half second. */
    public final List<float[]> health = new ArrayList<float[]>();

    public long avgBreakDelay()
    {
        return this.breakDelayCount == 0 ? -1L : this.breakDelaySum / (long)this.breakDelayCount;
    }

    public long avgRailToCart()
    {
        return this.railToCartCount == 0 ? -1L : this.railToCartSum / (long)this.railToCartCount;
    }

    /** Crystals placed per second over the whole fight. */
    public float crystalRate()
    {
        return this.duration <= 0L ? 0.0F : (float)this.crystalsPlaced * 1000.0F / (float)this.duration;
    }

    /** "Crystal", "Cart" or "Melee": what the fight was mostly fought with. */
    public String type()
    {
        int crystal = this.crystalsPlaced + this.crystalsBroken + this.enemyCrystals;
        int cart = (this.cartsPlaced + this.cartsDetonated) * 2;

        if (crystal == 0 && cart == 0)
        {
            return this.takenCrystal > this.takenCart && this.takenCrystal > 0.0F ? "Crystal" : (this.takenCart > 0.0F ? "Cart" : "Melee");
        }

        return cart > crystal ? "Cart" : "Crystal";
    }

    public boolean isWin()
    {
        return WIN.equals(this.result);
    }

    public boolean isLoss()
    {
        return LOSS.equals(this.result);
    }

    public JsonObject toJson(boolean details)
    {
        JsonObject o = new JsonObject();
        o.addProperty("start", Long.valueOf(this.startEpoch));
        o.addProperty("duration", Long.valueOf(this.duration));
        o.addProperty("opponent", this.opponent);
        o.addProperty("player", Boolean.valueOf(this.opponentIsPlayer));
        o.addProperty("result", this.result);
        o.addProperty("server", this.server);
        o.addProperty("training", Boolean.valueOf(this.training));
        o.addProperty("hitsDealt", Integer.valueOf(this.hitsDealt));
        o.addProperty("hitsTaken", Integer.valueOf(this.hitsTaken));
        o.addProperty("damageDealt", Float.valueOf(this.damageDealt));
        o.addProperty("damageTaken", Float.valueOf(this.damageTaken));
        o.addProperty("dealtExplosion", Float.valueOf(this.dealtExplosion));
        o.addProperty("dealtMelee", Float.valueOf(this.dealtMelee));
        o.addProperty("takenCrystal", Float.valueOf(this.takenCrystal));
        o.addProperty("takenCart", Float.valueOf(this.takenCart));
        o.addProperty("takenMelee", Float.valueOf(this.takenMelee));
        o.addProperty("takenOther", Float.valueOf(this.takenOther));
        o.addProperty("crystalsPlaced", Integer.valueOf(this.crystalsPlaced));
        o.addProperty("crystalsBroken", Integer.valueOf(this.crystalsBroken));
        o.addProperty("enemyCrystals", Integer.valueOf(this.enemyCrystals));
        o.addProperty("breakDelaySum", Long.valueOf(this.breakDelaySum));
        o.addProperty("breakDelayCount", Integer.valueOf(this.breakDelayCount));
        o.addProperty("bestBreakDelay", Long.valueOf(this.bestBreakDelay));
        o.addProperty("bestCrystalRate", Float.valueOf(this.bestCrystalRate));
        o.addProperty("longestCrystalGap", Long.valueOf(this.longestCrystalGap));
        o.addProperty("obsidianPlaced", Integer.valueOf(this.obsidianPlaced));
        o.addProperty("cartsPlaced", Integer.valueOf(this.cartsPlaced));
        o.addProperty("cartsDetonated", Integer.valueOf(this.cartsDetonated));
        o.addProperty("railsPlaced", Integer.valueOf(this.railsPlaced));
        o.addProperty("railToCartSum", Long.valueOf(this.railToCartSum));
        o.addProperty("railToCartCount", Integer.valueOf(this.railToCartCount));
        o.addProperty("popsSelf", Integer.valueOf(this.popsSelf));
        o.addProperty("popsOpponent", Integer.valueOf(this.popsOpponent));

        if (this.replayFile != null)
        {
            o.addProperty("replayFile", this.replayFile);
            o.addProperty("replayOffset", Long.valueOf(this.replayOffset));
        }

        if (details)
        {
            JsonArray events = new JsonArray();

            for (Event e : this.events)
            {
                JsonArray a = new JsonArray();
                a.add(Long.valueOf(e.t));
                a.add(e.kind);
                a.add(e.text);
                events.add(a);
            }

            o.add("events", events);
            JsonArray hp = new JsonArray();

            for (float[] h : this.health)
            {
                JsonArray a = new JsonArray();
                a.add(Float.valueOf(h[0]));
                a.add(Float.valueOf(h[1]));
                a.add(Float.valueOf(h[2]));
                hp.add(a);
            }

            o.add("health", hp);
        }

        return o;
    }

    public static Fight fromJson(JsonObject o)
    {
        Fight f = new Fight();
        f.startEpoch = l(o, "start");
        f.duration = l(o, "duration");
        f.opponent = s(o, "opponent", "?");
        f.opponentIsPlayer = b(o, "player");
        f.result = s(o, "result", DRAW);
        f.server = s(o, "server", "");
        f.training = b(o, "training");
        f.hitsDealt = (int)l(o, "hitsDealt");
        f.hitsTaken = (int)l(o, "hitsTaken");
        f.damageDealt = fl(o, "damageDealt");
        f.damageTaken = fl(o, "damageTaken");
        f.dealtExplosion = fl(o, "dealtExplosion");
        f.dealtMelee = fl(o, "dealtMelee");
        f.takenCrystal = fl(o, "takenCrystal");
        f.takenCart = fl(o, "takenCart");
        f.takenMelee = fl(o, "takenMelee");
        f.takenOther = fl(o, "takenOther");
        f.crystalsPlaced = (int)l(o, "crystalsPlaced");
        f.crystalsBroken = (int)l(o, "crystalsBroken");
        f.enemyCrystals = (int)l(o, "enemyCrystals");
        f.breakDelaySum = l(o, "breakDelaySum");
        f.breakDelayCount = (int)l(o, "breakDelayCount");
        f.bestBreakDelay = o.has("bestBreakDelay") ? l(o, "bestBreakDelay") : -1L;
        f.bestCrystalRate = fl(o, "bestCrystalRate");
        f.longestCrystalGap = l(o, "longestCrystalGap");
        f.obsidianPlaced = (int)l(o, "obsidianPlaced");
        f.cartsPlaced = (int)l(o, "cartsPlaced");
        f.cartsDetonated = (int)l(o, "cartsDetonated");
        f.railsPlaced = (int)l(o, "railsPlaced");
        f.railToCartSum = l(o, "railToCartSum");
        f.railToCartCount = (int)l(o, "railToCartCount");
        f.popsSelf = (int)l(o, "popsSelf");
        f.popsOpponent = (int)l(o, "popsOpponent");

        if (o.has("replayFile"))
        {
            f.replayFile = s(o, "replayFile", null);
            f.replayOffset = l(o, "replayOffset");
        }

        if (o.has("events") && o.get("events").isJsonArray())
        {
            for (JsonElement e : o.getAsJsonArray("events"))
            {
                JsonArray a = e.getAsJsonArray();
                f.events.add(new Event(a.get(0).getAsLong(), a.get(1).getAsString(), a.get(2).getAsString()));
            }
        }

        if (o.has("health") && o.get("health").isJsonArray())
        {
            for (JsonElement e : o.getAsJsonArray("health"))
            {
                JsonArray a = e.getAsJsonArray();
                f.health.add(new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()});
            }
        }

        return f;
    }

    private static long l(JsonObject o, String k)
    {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsLong() : 0L;
    }

    private static float fl(JsonObject o, String k)
    {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsFloat() : 0.0F;
    }

    private static boolean b(JsonObject o, String k)
    {
        return o.has(k) && o.get(k).isJsonPrimitive() && o.get(k).getAsBoolean();
    }

    private static String s(JsonObject o, String k, String def)
    {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : def;
    }

    /** "m:ss.s" for event times. */
    public static String clock(long ms)
    {
        return String.format("%d:%04.1f", ms / 60000L, (float)(ms % 60000L) / 1000.0F);
    }
}
