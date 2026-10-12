package net.sythix.client.module.pvp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.sythix.client.combat.CombatTracker;
import net.sythix.client.combat.TrainingWorld;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.TextHudModule;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import org.apache.commons.io.FileUtils;

/**
 * A singleplayer practice world for crystal and cart PvP with timed drills and personal bests:
 * Crystal Speed, Cart Speed, Totem Reaction and Dummy Duel. Works only in its own Training Lab
 * world, so it can never help in a real match.
 */
public class TrainingLab extends TextHudModule
{
    private static final String[] DRILLS = {"Crystal Speed", "Cart Speed", "Totem Reaction", "Dummy Duel"};
    private static final int TOTEM_ROUNDS = 5;

    private final ModeSetting drill;
    private final NumberSetting length;
    private final ModeSetting gameMode;
    private final KeybindSetting startKey;
    private final Random random = new Random();
    private JsonObject bests;

    private enum State
    {
        IDLE, WAITING, COUNTDOWN, RUNNING
    }

    private State state = State.IDLE;
    private String running;
    private long stateStart;
    private int lastCount;
    private int startBroken;
    private int startPlaced;
    private int startCarts;
    private int startDetonated;
    private String lastResult = "";
    // totem reaction
    private int round;
    private long signalAt;
    /** When the round began; the server empties the offhand then, so a totem there after 0.7 s was swapped too early. */
    private long roundStart;
    private boolean signalled;
    private final List<Long> reactions = new ArrayList<Long>();
    private String lastGameMode;

    public TrainingLab()
    {
        super("CPvP Training Lab", "A practice world for crystal and cart PvP with timed drills, dummies and personal bests.", Category.PVP, false, 0.5D, 0.18D);
        this.add(new ButtonSetting("Training Lab", "Opens the Sythix Training Lab world (leaves the world or server you are in). Your own worlds are never changed.", "Open", TrainingWorld::open));
        this.drill = this.add(new ModeSetting("Drill", "Crystal Speed: place and break as many crystals as you can. Cart Speed: rail, cart, light. Totem Reaction: put a totem in your offhand when told. Dummy Duel: take down every dummy.", "Crystal Speed", DRILLS));
        this.length = this.add(new NumberSetting("Drill Length", "How long Crystal Speed and Cart Speed last.", 30.0D, 10.0D, 120.0D, 5.0D, "s"));
        this.startKey = this.add(new KeybindSetting("Start Drill Key", "Start (or stop) the selected drill in the Training Lab.", 0));
        this.add(new ButtonSetting("Start Drill", "Starts the selected drill when you close this menu.", "Start", () -> {
            if (this.state == State.IDLE)
            {
                this.state = State.WAITING;
            }
        }));
        this.gameMode = this.add(new ModeSetting("Lab Game Mode", "Creative: no damage, endless items. Survival: you take damage (keep inventory is on).", "Creative", "Creative", "Survival"));
        this.add(new ButtonSetting("Refill Kit", "Gives the crystal and cart PvP kit again.", "Refill", TrainingWorld::refillKit));
        this.add(new ButtonSetting("Reset Arena", "Repairs the floor and brings the training dummies back.", "Reset", TrainingWorld::resetArena));
        this.add(new InfoSetting("Personal Bests", "Your best score in each drill.", this::bestsText));
        this.add(new ButtonSetting("Clear Bests", "Forget your personal bests.", "Clear", () -> {
            this.bests = new JsonObject();
            this.saveBests();
        }));
    }

    public boolean shouldRender()
    {
        return TrainingWorld.isActive();
    }

    public void onWorldJoin()
    {
        this.state = State.IDLE;

        if (TrainingWorld.isActive())
        {
            TrainingWorld.setUp(this.gameMode.is("Creative"));
            this.lastGameMode = this.gameMode.get();
            Notifications.post("Training Lab", "Pick a drill in the CPvP Training Lab settings, then start it" + (this.startKey.getKey() == 0 ? "." : " with " + this.startKey.getKeyName() + "."), Notifications.Kind.INFO);
        }
    }

    public void onKey(int keyCode)
    {
        if (keyCode == 0 || keyCode != this.startKey.getKey())
        {
            return;
        }

        if (this.state == State.IDLE)
        {
            this.state = State.WAITING;
        }
        else
        {
            this.finish(true);
        }
    }

    public void onTick()
    {
        if (!TrainingWorld.isActive() || this.mc.player == null)
        {
            if (this.state != State.IDLE && this.state != State.WAITING)
            {
                this.state = State.IDLE;
            }

            if (this.state == State.WAITING && this.mc.currentScreen == null && this.mc.world != null)
            {
                Notifications.post("Training Lab", "Drills only run in the Training Lab world. Open it from the CPvP Training Lab settings.", Notifications.Kind.INFO);
                this.state = State.IDLE;
            }

            return;
        }

        if (this.lastGameMode != null && !this.lastGameMode.equals(this.gameMode.get()))
        {
            this.lastGameMode = this.gameMode.get();
            TrainingWorld.setGameMode(this.gameMode.is("Creative"));
        }

        long now = Minecraft.getSystemTime();

        switch (this.state)
        {
            case WAITING:
                if (this.mc.currentScreen == null)
                {
                    this.begin(now);
                }

                break;
            case COUNTDOWN:
                int left = 3 - (int)((now - this.stateStart) / 1000L);

                if (left != this.lastCount)
                {
                    this.lastCount = left;

                    if (left > 0)
                    {
                        this.title(String.valueOf(left), this.running);
                        this.sound(1.0F);
                    }
                }

                if (now - this.stateStart >= 3000L)
                {
                    this.go(now);
                }

                break;
            case RUNNING:
                this.runDrill(now);
                break;
            default:
                break;
        }
    }

    private void begin(long now)
    {
        this.running = this.drill.get();
        this.state = State.COUNTDOWN;
        this.stateStart = now;
        this.lastCount = -1;

        if ("Dummy Duel".equals(this.running))
        {
            TrainingWorld.resetArena();
        }
        else if ("Totem Reaction".equals(this.running))
        {
            TrainingWorld.prepareTotemRound();
        }
    }

    private void go(long now)
    {
        CombatTracker.Session s = CombatTracker.session();
        this.startBroken = s.crystalsBroken;
        this.startPlaced = s.crystalsPlaced;
        this.startCarts = s.cartsPlaced;
        this.startDetonated = s.cartsDetonated;
        this.state = State.RUNNING;
        this.stateStart = now;
        this.round = 0;
        this.reactions.clear();

        if ("Totem Reaction".equals(this.running))
        {
            this.nextRound(now);
            this.title("Get ready", "Swap a totem to your offhand when you see SWAP");
        }
        else
        {
            this.title("GO!", this.running);
            this.sound(2.0F);
        }
    }

    private void nextRound(long now)
    {
        this.signalled = false;
        this.roundStart = now;
        this.signalAt = now + 1500L + (long)this.random.nextInt(2500);
        TrainingWorld.prepareTotemRound();
    }

    private void runDrill(long now)
    {
        long elapsed = now - this.stateStart;

        switch (this.running)
        {
            case "Crystal Speed":
            case "Cart Speed":
                if (elapsed >= (long)(this.length.getFloat() * 1000.0F))
                {
                    this.finish(false);
                }

                break;
            case "Totem Reaction":
                boolean totem = this.mc.player.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING;

                if (!this.signalled && now >= this.signalAt)
                {
                    this.signalled = true;
                    this.title("SWAP!", "");
                    this.sound(2.0F);
                }
                else if (this.signalled && totem && now - this.signalAt > 50L)
                {
                    this.reactions.add(Long.valueOf(now - this.signalAt));
                    this.title(String.valueOf(now - this.signalAt) + " ms", "Round " + (this.round + 1) + " of " + TOTEM_ROUNDS);

                    if (++this.round >= TOTEM_ROUNDS)
                    {
                        this.finish(false);
                    }
                    else
                    {
                        this.nextRound(now);
                    }
                }
                else if (!this.signalled && totem && now - this.roundStart > 700L)
                {
                    // swapped before the signal: the round starts again
                    this.title("Too early", "Wait for SWAP");
                    this.nextRound(now);
                }

                break;
            case "Dummy Duel":
                if (elapsed > 1500L && this.dummiesLeft() == 0)
                {
                    this.finish(false);
                }
                else if (elapsed > 300000L)
                {
                    this.finish(true);
                }

                break;
            default:
                this.finish(true);
        }
    }

    private int dummiesLeft()
    {
        int n = 0;

        for (Entity e : this.mc.world.loadedEntityList)
        {
            if (e instanceof EntityZombie && !e.isDead && ((EntityZombie)e).getHealth() > 0.0F && "Training Dummy".equals(e.getCustomNameTag()))
            {
                ++n;
            }
        }

        return n;
    }

    /** Ends the drill; stopped = ended early by the player (no score). */
    private void finish(boolean stopped)
    {
        if (this.state != State.RUNNING || stopped)
        {
            this.state = State.IDLE;
            this.lastResult = "Stopped";
            this.title("Stopped", "");
            return;
        }

        long elapsed = Minecraft.getSystemTime() - this.stateStart;
        CombatTracker.Session s = CombatTracker.session();
        float seconds = (float)elapsed / 1000.0F;
        float score;
        boolean higherIsBetter = true;
        String text;

        switch (this.running)
        {
            case "Crystal Speed":
                int broken = s.crystalsBroken - this.startBroken;
                score = (float)broken / seconds;
                text = String.format("%.2f crystals/s (%d placed, %d broken)", score, s.crystalsPlaced - this.startPlaced, broken);
                break;
            case "Cart Speed":
                int carts = s.cartsPlaced - this.startCarts;
                score = (float)carts * 60.0F / seconds;
                text = String.format("%.1f carts/min (%d placed, %d went off)", score, carts, s.cartsDetonated - this.startDetonated);
                break;
            case "Totem Reaction":
                long sum = 0L;

                for (Long r : this.reactions)
                {
                    sum += r.longValue();
                }

                score = (float)(sum / (long)Math.max(1, this.reactions.size()));
                higherIsBetter = false;
                text = String.format("%.0f ms average over %d swaps", score, this.reactions.size());
                break;
            default:
                score = seconds;
                higherIsBetter = false;
                text = String.format("All dummies down in %.1f s", score);
        }

        boolean best = this.record(this.running, score, higherIsBetter);
        this.lastResult = text + (best ? "  - new best!" : "");
        this.state = State.IDLE;
        this.title(best ? "New best!" : "Done", text);
        this.sound(best ? 2.0F : 1.5F);
        Notifications.post(this.running + (best ? " - new best!" : ""), text, best ? Notifications.Kind.SUCCESS : Notifications.Kind.INFO);
    }

    private void title(String title, String sub)
    {
        this.mc.ingameGUI.displayTitle(null, null, 2, 20, 6);
        this.mc.ingameGUI.displayTitle(null, sub, -1, -1, -1);
        this.mc.ingameGUI.displayTitle(title, null, -1, -1, -1);
    }

    private void sound(float pitch)
    {
        this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_NOTE_PLING, pitch));
    }

    // ---- Personal bests --------------------------------------------------------------------

    private File bestsFile()
    {
        return new File(new File(this.mc.mcDataDir, "sythix"), "training-lab.json");
    }

    private JsonObject bests()
    {
        if (this.bests == null)
        {
            this.bests = new JsonObject();

            try
            {
                if (this.bestsFile().isFile())
                {
                    this.bests = new JsonParser().parse(FileUtils.readFileToString(this.bestsFile(), StandardCharsets.UTF_8)).getAsJsonObject();
                }
            }
            catch (Exception ignored)
            {
                // start without bests
            }
        }

        return this.bests;
    }

    private void saveBests()
    {
        try
        {
            FileUtils.writeStringToFile(this.bestsFile(), this.bests().toString(), StandardCharsets.UTF_8);
        }
        catch (Exception ignored)
        {
            // best effort
        }
    }

    private boolean record(String drill, float score, boolean higherIsBetter)
    {
        JsonObject b = this.bests();
        boolean best = !b.has(drill) || (higherIsBetter ? score > b.get(drill).getAsFloat() : score < b.get(drill).getAsFloat());

        if (best && score > 0.0F)
        {
            b.addProperty(drill, Float.valueOf(score));
            this.saveBests();
            return true;
        }

        return false;
    }

    private String best(String drill)
    {
        JsonObject b = this.bests();

        if (!b.has(drill))
        {
            return "--";
        }

        float v = b.get(drill).getAsFloat();

        switch (drill)
        {
            case "Crystal Speed":
                return String.format("%.2f/s", v);
            case "Cart Speed":
                return String.format("%.1f/min", v);
            case "Totem Reaction":
                return String.format("%.0f ms", v);
            default:
                return String.format("%.1f s", v);
        }
    }

    private String bestsText()
    {
        StringBuilder sb = new StringBuilder();

        for (String d : DRILLS)
        {
            if (sb.length() > 0)
            {
                sb.append(", ");
            }

            sb.append(d.split(" ")[0]).append(' ').append(this.best(d));
        }

        return sb.toString();
    }

    // ---- HUD ----------------------------------------------------------------------------------

    protected List<String[]> getLines()
    {
        List<String[]> lines = new ArrayList<String[]>();
        long now = Minecraft.getSystemTime();
        String d = this.state == State.IDLE || this.state == State.WAITING ? this.drill.get() : this.running;
        lines.add(new String[] {"Drill", d});

        if (this.state == State.RUNNING)
        {
            CombatTracker.Session s = CombatTracker.session();
            float seconds = Math.max(0.001F, (float)(now - this.stateStart) / 1000.0F);

            switch (d)
            {
                case "Crystal Speed":
                    lines.add(new String[] {"Time", String.format("%.1fs left", Math.max(0.0F, this.length.getFloat() - seconds))});
                    lines.add(new String[] {"Broken", String.format("%d (%.2f/s)", s.crystalsBroken - this.startBroken, (float)(s.crystalsBroken - this.startBroken) / seconds)});
                    break;
                case "Cart Speed":
                    lines.add(new String[] {"Time", String.format("%.1fs left", Math.max(0.0F, this.length.getFloat() - seconds))});
                    lines.add(new String[] {"Carts", String.valueOf(s.cartsPlaced - this.startCarts)});
                    break;
                case "Totem Reaction":
                    lines.add(new String[] {"Round", (this.round + 1) + " / " + TOTEM_ROUNDS});
                    break;
                default:
                    lines.add(new String[] {"Time", String.format("%.1fs", seconds)});
                    lines.add(new String[] {"Dummies", String.valueOf(this.dummiesLeft())});
            }
        }
        else if (this.state == State.COUNTDOWN)
        {
            lines.add(new String[] {"Starting", "in " + Math.max(1L, 3L - (now - this.stateStart) / 1000L)});
        }
        else
        {
            lines.add(new String[] {"Start", this.startKey.getKey() == 0 ? "in the module settings" : "press " + this.startKey.getKeyName()});

            if (!this.lastResult.isEmpty())
            {
                lines.add(new String[] {"Last", this.lastResult});
            }
        }

        lines.add(new String[] {"Best", this.best(d)});
        return lines;
    }
}
