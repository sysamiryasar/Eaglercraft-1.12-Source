package net.sythix.client.module.recording;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.combat.CombatTracker;
import net.sythix.client.combat.Fight;
import net.sythix.client.combat.GuiFightReview;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.SythixTheme;
import org.apache.commons.io.FileUtils;

/**
 * Post-fight review: every crystal, cart or melee fight is summarised (damage, hits, crystal speed,
 * break delay, cart timing, totem pops), with a health graph, a timeline of what happened, tips,
 * and a button that opens the replay at the start of the fight when Replay System was recording.
 */
public class CombatReplay extends Module
{
    private final ModeSetting afterFight;
    private final NumberSetting timeout;
    private final BooleanSetting markers;
    private final BooleanSetting mobFights;
    private final NumberSetting keep;
    private final KeybindSetting reviewKey;
    private final List<Fight> fights = new ArrayList<Fight>();
    private boolean loaded;
    private Fight pendingOpen;

    public CombatReplay()
    {
        super("Combat Replay", "Review each fight afterwards: damage, crystals, carts, pops, a health graph, a timeline and the replay.", Category.RECORDING, true);
        this.add(new ButtonSetting("Fight Reviews", "Your recent fights.", "Open", () -> this.mc.displayGuiScreen(new GuiFights(this.mc.currentScreen))));
        this.afterFight = this.add(new ModeSetting("After A Fight", "What happens when a fight ends. Open Review waits until you are out of combat and no menu is open.", "Notify", "Notify", "Open Review", "Nothing"));
        this.timeout = this.add(new NumberSetting("Fight Ends After", "A fight ends after this long without combat (or when someone dies).", 8.0D, 3.0D, 30.0D, 1.0D, "s"));
        this.markers = this.add(new BooleanSetting("Replay Markers", "Mark the start and end of each fight in the replay (when Replay System is recording).", true));
        this.mobFights = this.add(new BooleanSetting("Count Mob Fights", "Also review fights against mobs (training dummies in the Training Lab always count).", false));
        this.keep = this.add(new NumberSetting("Keep Fights", "How many fight reviews are kept.", 30.0D, 5.0D, 100.0D, 5.0D));
        this.reviewKey = this.add(new KeybindSetting("Last Fight Key", "Open the review of your last fight.", 0));
        CombatTracker.addListener(this::onFight);
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiFights(parent);
    }

    public void onTick()
    {
        CombatTracker.fightTimeout = (long)(this.timeout.getFloat() * 1000.0F);
        CombatTracker.replayMarkers = this.markers.isOn();
        CombatTracker.mobFights = this.mobFights.isOn();

        if (this.pendingOpen != null && this.mc.currentScreen == null && CombatTracker.current() == null && this.mc.player != null && this.mc.player.getHealth() > 0.0F)
        {
            this.mc.displayGuiScreen(new GuiFightReview(null, this.pendingOpen));
            this.pendingOpen = null;
        }
    }

    public void onKey(int keyCode)
    {
        if (keyCode != 0 && keyCode == this.reviewKey.getKey())
        {
            this.load();

            if (this.fights.isEmpty())
            {
                Notifications.post("Combat Replay", "No fights recorded yet.", Notifications.Kind.INFO);
            }
            else
            {
                this.mc.displayGuiScreen(new GuiFightReview(null, this.fights.get(this.fights.size() - 1)));
            }
        }
    }

    private void onFight(Fight fight)
    {
        if (!this.isEnabled())
        {
            return;
        }

        this.load();
        this.fights.add(fight);

        while (this.fights.size() > this.keep.getInt())
        {
            this.fights.remove(0);
        }

        this.save();

        if (this.afterFight.is("Notify"))
        {
            String key = this.reviewKey.getKey() == 0 ? "open it in Combat Replay" : "press " + this.reviewKey.getKeyName() + " to review";
            Notifications.post(fight.result + " vs " + fight.opponent, String.format("%s, dealt %.0f, took %.0f - %s", Fight.clock(fight.duration), fight.damageDealt, fight.damageTaken, key), fight.isWin() ? Notifications.Kind.SUCCESS : (fight.isLoss() ? Notifications.Kind.ERROR : Notifications.Kind.INFO));
        }
        else if (this.afterFight.is("Open Review"))
        {
            this.pendingOpen = fight;
        }
    }

    public List<Fight> fights()
    {
        this.load();
        return this.fights;
    }

    private File file()
    {
        return new File(new File(this.mc.mcDataDir, "sythix"), "fight-reviews.json");
    }

    private void load()
    {
        if (this.loaded)
        {
            return;
        }

        this.loaded = true;

        try
        {
            if (this.file().isFile())
            {
                for (JsonElement e : new JsonParser().parse(FileUtils.readFileToString(this.file(), StandardCharsets.UTF_8)).getAsJsonArray())
                {
                    this.fights.add(Fight.fromJson(e.getAsJsonObject()));
                }
            }
        }
        catch (Exception ignored)
        {
            // start with an empty list
        }
    }

    private void save()
    {
        JsonArray array = new JsonArray();

        for (Fight f : this.fights)
        {
            array.add(f.toJson(true));
        }

        try
        {
            FileUtils.writeStringToFile(this.file(), array.toString(), StandardCharsets.UTF_8);
        }
        catch (Exception ignored)
        {
            // best effort
        }
    }

    /** The list of recent fights. */
    public class GuiFights extends GuiListScreen
    {
        public GuiFights(GuiScreen parent)
        {
            super(parent, "Fight Reviews");
        }

        protected String header()
        {
            int wins = 0;
            int losses = 0;

            for (Fight f : CombatReplay.this.fights())
            {
                wins += f.isWin() ? 1 : 0;
                losses += f.isLoss() ? 1 : 0;
            }

            int n = CombatReplay.this.fights.size();
            return n + (n == 1 ? " fight: " : " fights: ") + wins + " won, " + losses + " lost. Double-click a fight to review it.";
        }

        protected List<Row> rows()
        {
            List<Row> rows = new ArrayList<Row>();
            SimpleDateFormat fmt = net.sythix.client.module.advanced.Localization.dateTime(false);
            List<Fight> list = CombatReplay.this.fights();

            for (int i = list.size() - 1; i >= 0; --i)
            {
                Fight f = list.get(i);
                String title = f.result + " vs " + f.opponent + "  (" + f.type() + (f.training ? ", training" : "") + ")";
                String sub = String.format("%s  -  dealt %.0f, took %.0f, %d crystal%s, %d cart%s, pops %d/%d%s", Fight.clock(f.duration), f.damageDealt, f.damageTaken, f.crystalsPlaced, f.crystalsPlaced == 1 ? "" : "s", f.cartsPlaced, f.cartsPlaced == 1 ? "" : "s", f.popsSelf, f.popsOpponent, f.replayFile != null ? "  -  replay" : "");
                rows.add(new Row(f, title, sub, fmt.format(new Date(f.startEpoch)), f.isWin() ? 0xFF4CD964 : (f.isLoss() ? SythixTheme.RED : SythixTheme.TEXT_MUTED)));
            }

            return rows;
        }

        protected String emptyText()
        {
            return "No fights yet. Fight someone (or try the CPvP Training Lab).";
        }

        protected String[] buttons()
        {
            return new String[] {"Review", "Watch Replay", "Delete", "Done"};
        }

        protected boolean enabled(int index)
        {
            Fight f = this.selected instanceof Fight ? (Fight)this.selected : null;

            if (index == 0 || index == 2)
            {
                return f != null;
            }

            if (index == 1)
            {
                return f != null && GuiFightReview.replayAvailable(f);
            }

            return true;
        }

        protected void open(Object key)
        {
            this.mc.displayGuiScreen(new GuiFightReview(this, (Fight)key));
        }

        protected void press(int index)
        {
            Fight f = this.selected instanceof Fight ? (Fight)this.selected : null;

            switch (index)
            {
                case 0:
                    this.open(f);
                    break;
                case 1:
                    GuiFightReview.watch(f);
                    break;
                case 2:
                    CombatReplay.this.fights.remove(f);
                    CombatReplay.this.save();
                    this.selected = null;
                    break;
                default:
                    this.mc.displayGuiScreen(this.parent);
            }
        }
    }
}
