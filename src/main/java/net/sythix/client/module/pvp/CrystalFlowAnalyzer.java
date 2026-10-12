package net.sythix.client.module.pvp;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.sythix.client.combat.CombatTracker;
import net.sythix.client.combat.Fight;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.TextHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ModeSetting;

/**
 * Live crystal PvP and cart PvP statistics: how fast you place and break crystals, how quickly
 * you break them after placing, totem pops, and TNT minecart flow (rail to cart time, carts lit
 * and set off). Display only: it counts what you do and never does anything for you.
 */
public class CrystalFlowAnalyzer extends TextHudModule
{
    private final ModeSetting mode;
    private final BooleanSetting showSpeed;
    private final BooleanSetting showDelay;
    private final BooleanSetting showTotals;
    private final BooleanSetting showPops;
    private final BooleanSetting showSupplies;
    private final BooleanSetting showFight;
    private long lastCartUse;

    public CrystalFlowAnalyzer()
    {
        super("Crystal Flow Analyzer", "Live crystal and cart PvP stats: crystals per second, break delay, pops and cart timing.", Category.PVP, false, 0.995D, 0.35D);
        this.mode = this.add(new ModeSetting("Show", "Which kind of PvP to show. Auto adds the cart lines once you use TNT minecarts.", "Auto", "Auto", "Crystal", "Cart", "Both"));
        this.showSpeed = this.add(new BooleanSetting("Speed", "Crystals placed and broken per second (last 3 seconds) and your best.", true));
        this.showDelay = this.add(new BooleanSetting("Break Delay", "Average time between placing a crystal and hitting it (last 20).", true));
        this.showTotals = this.add(new BooleanSetting("Totals", "Crystals placed and broken, obsidian and rails placed this session.", false));
        this.showPops = this.add(new BooleanSetting("Totem Pops", "Totems you and others popped this session.", true));
        this.showSupplies = this.add(new BooleanSetting("Supplies", "Crystals, carts and totems left in your inventory.", true));
        this.showFight = this.add(new BooleanSetting("Current Fight", "Damage dealt and taken in the fight you are in.", true));
        this.add(new ButtonSetting("Reset Session", "Start counting from zero.", "Reset", CombatTracker::resetSession));
    }

    private boolean cartLines()
    {
        if (this.mode.is("Cart") || this.mode.is("Both"))
        {
            return true;
        }

        return this.mode.is("Auto") && (CombatTracker.session().cartsPlaced > 0 || CombatTracker.session().railsPlaced > 0) && Minecraft.getSystemTime() - this.lastCartUse < 60000L;
    }

    private boolean crystalLines()
    {
        return !this.mode.is("Cart");
    }

    public void onTick()
    {
        CombatTracker.Session s = CombatTracker.session();

        if (s.cartsPlaced + s.railsPlaced != this.cartUses)
        {
            this.cartUses = s.cartsPlaced + s.railsPlaced;
            this.lastCartUse = Minecraft.getSystemTime();
        }
    }

    private int cartUses;

    protected List<String[]> getLines()
    {
        CombatTracker.Session s = CombatTracker.session();
        List<String[]> lines = new ArrayList<String[]>();

        if (this.crystalLines())
        {
            if (this.showSpeed.isOn())
            {
                lines.add(new String[] {"Crystals/s", String.format("%.1f place  %.1f break", CombatTracker.placeRate(), CombatTracker.breakRate())});
                lines.add(new String[] {"Best", String.format("%.1f /s", s.bestCrystalRate)});
            }

            if (this.showDelay.isOn())
            {
                long avg = average(s.breakDelays);
                lines.add(new String[] {"Break Delay", avg < 0L ? "--" : avg + " ms"});
            }

            if (this.showTotals.isOn())
            {
                lines.add(new String[] {"Placed/Broken", s.crystalsPlaced + " / " + s.crystalsBroken});
                lines.add(new String[] {"Obsidian", String.valueOf(s.obsidianPlaced)});
            }
        }

        if (this.cartLines())
        {
            long rail = average(s.railToCart);
            lines.add(new String[] {"Carts", s.cartsPlaced + " placed, " + s.cartsLit + " lit, " + s.cartsDetonated + " off"});
            lines.add(new String[] {"Rail > Cart", rail < 0L ? "--" : rail + " ms"});

            if (this.showTotals.isOn())
            {
                lines.add(new String[] {"Rails", String.valueOf(s.railsPlaced)});
            }
        }

        if (this.showPops.isOn())
        {
            lines.add(new String[] {"Pops", "you " + s.popsSelf + "  them " + s.popsOthers});
        }

        if (this.showSupplies.isOn() && this.mc.player != null)
        {
            String supplies = (this.crystalLines() ? count(Items.END_CRYSTAL) + " crystals  " : "") + (this.cartLines() ? count(Items.TNT_MINECART) + " carts  " : "") + count(Items.TOTEM_OF_UNDYING) + " totems";
            lines.add(new String[] {"Left", supplies});
        }

        Fight fight = CombatTracker.current();

        if (this.showFight.isOn() && fight != null)
        {
            lines.add(new String[] {"vs " + fight.opponent, String.format("dealt %.0f  taken %.0f", fight.damageDealt, fight.damageTaken)});
        }

        return lines;
    }

    private int count(Item item)
    {
        int n = 0;

        for (ItemStack stack : this.mc.player.inventory.mainInventory)
        {
            if (stack.getItem() == item)
            {
                n += stack.getCount();
            }
        }

        for (ItemStack stack : this.mc.player.inventory.offHandInventory)
        {
            if (stack.getItem() == item)
            {
                n += stack.getCount();
            }
        }

        return n;
    }

    private static long average(Deque<Long> values)
    {
        if (values.isEmpty())
        {
            return -1L;
        }

        long sum = 0L;

        for (Long v : values)
        {
            sum += v.longValue();
        }

        return sum / (long)values.size();
    }
}
