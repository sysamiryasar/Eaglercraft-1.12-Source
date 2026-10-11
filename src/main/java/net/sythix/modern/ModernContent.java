package net.sythix.modern;

import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.core.Hooks;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;

/**
 * Settings and information for the blocks, items, effects and world generation backported from
 * Minecraft 1.13 - 1.21.11. They work in singleplayer and LAN worlds; normal 1.12 servers do not
 * have them.
 */
public class ModernContent extends Module
{
    private final BooleanSetting worldGen;
    private final BooleanSetting deepslate;
    private final BooleanSetting geodes;
    private final BooleanSetting plants;
    private final BooleanSetting mobs;

    public ModernContent()
    {
        super("1.21 Content", "Blocks, items, mobs, effects and world generation from Minecraft 1.13-1.21 (singleplayer and LAN).", Category.ADVANCED, true);
        this.setCore();
        this.add(new InfoSetting("Added", "What has been added.", () -> ModernBlocks.all().size() + " blocks, " + ModernItems.all().size() + " items, " + ModernEffects.effects().size() + " effects, " + ModernEffects.enchantments().size() + " enchantments, " + net.sythix.modern.mob.ModernMobs.count() + " mobs"));
        this.add(new InfoSetting("Where", "Where the new content works.", () -> "Singleplayer and LAN worlds (1.12 servers do not send it)"));
        this.worldGen = this.add(new BooleanSetting("New World Generation", "Generate the new blocks in newly explored chunks of singleplayer worlds.", true));
        this.deepslate = this.add(new BooleanSetting("Deepslate Layer", "Stone near the bottom of the world becomes deepslate, with deepslate ores and tuff.", true)).visibleWhen(this.worldGen::isOn);
        this.geodes = this.add(new BooleanSetting("Amethyst Geodes", "Rare hollow geodes lined with amethyst underground.", true)).visibleWhen(this.worldGen::isOn);
        this.plants = this.add(new BooleanSetting("New Trees and Plants", "Cherry trees on mountains, pale oaks in dark forests, mangroves and mud in swamps, bamboo in jungles, new flowers.", true)).visibleWhen(this.worldGen::isOn);
        this.mobs = this.add(new BooleanSetting("New Mobs", "Let the new mobs spawn naturally in singleplayer worlds (spawn eggs always work).", true));
        this.add(new ButtonSetting("Real 1.21 Textures", "Use the real textures from a 1.21 pack or Minecraft jar you own.", "Open", () -> this.mc.displayGuiScreen(new net.sythix.client.pack.GuiModernPacks(this.mc.currentScreen))));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new net.sythix.client.pack.GuiModernPacks(parent);
    }

    private static ModernContent instance()
    {
        net.sythix.client.SythixClient client = net.sythix.client.SythixClient.get();
        return client == null ? null : client.getModuleManager().get(ModernContent.class);
    }

    public static boolean worldGen()
    {
        ModernContent m = instance();
        return m == null || m.worldGen.isOn();
    }

    public static boolean deepslate()
    {
        ModernContent m = instance();
        return m == null || m.deepslate.isOn();
    }

    public static boolean geodes()
    {
        ModernContent m = instance();
        return m == null || m.geodes.isOn();
    }

    public static boolean mobs()
    {
        ModernContent m = instance();
        return m == null || m.mobs.isOn();
    }

    public static boolean plants()
    {
        ModernContent m = instance();
        return m == null || m.plants.isOn();
    }
}
