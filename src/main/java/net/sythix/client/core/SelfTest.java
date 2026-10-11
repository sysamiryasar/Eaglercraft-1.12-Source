package net.sythix.client.core;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.sythix.client.SythixClient;
import net.sythix.client.cosmetic.GuiProfile;
import net.sythix.client.module.Module;
import net.sythix.client.module.ModuleManager;
import net.sythix.client.module.hud.HudModule;
import net.sythix.client.module.recording.RecordingTools;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.setting.Setting;
import net.sythix.client.setting.TextSetting;
import net.sythix.client.ui.GuiHudEditor;
import net.sythix.client.ui.GuiModMenu;
import net.sythix.client.ui.GuiSythixSettings;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Automatic test run for developers. Only active when the game is started with
 * -Dsythix.selftest=true; it then exercises every module, setting and screen on the main menu
 * and in a flat creative test world, takes screenshots, writes sythix/selftest.txt and quits.
 */
public final class SelfTest
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean ACTIVE = System.getProperty("sythix.selftest") != null;

    /** Buttons that open external programs, need the network or would undo the test itself. */
    private static final Set<String> SKIP_BUTTONS = new HashSet<String>(Arrays.asList("Open Folder", "Plugin Folder", "Reset Everything", "Enter Safe Mode", "Exit And Restore", "Check Now", "Reload Config", "Cape Folder", "Skin Folder", "Clear Server Pack Cache"));

    private static final class Step
    {
        final String name;
        final int wait;
        final Runnable action;

        Step(String name, int wait, Runnable action)
        {
            this.name = name;
            this.wait = wait;
            this.action = action;
        }
    }

    private static final List<Step> STEPS = new ArrayList<Step>();
    private static final List<String> RESULTS = new ArrayList<String>();
    private static int failures;
    private static int index;
    private static int countdown = 60;
    private static boolean built;

    private SelfTest()
    {
    }

    public static void tick()
    {
        if (!ACTIVE || SythixClient.get() == null)
        {
            return;
        }

        if (!built)
        {
            built = true;
            build();
        }

        if (countdown > 0)
        {
            --countdown;
            return;
        }

        if (index >= STEPS.size())
        {
            return;
        }

        Step step = STEPS.get(index++);
        countdown = step.wait;
        LOGGER.info("[SelfTest] step {}", step.name);

        try
        {
            step.action.run();
        }
        catch (Throwable t)
        {
            fail(step.name, t);
        }
    }

    private static void pass(String what)
    {
        RESULTS.add("PASS " + what);
    }

    private static void fail(String what, Throwable t)
    {
        ++failures;
        RESULTS.add("FAIL " + what + ": " + t);
        LOGGER.error("[SelfTest] FAIL {}", what, t);
    }

    private static Minecraft mc()
    {
        return Minecraft.getMinecraft();
    }

    private static ModuleManager modules()
    {
        return SythixClient.get().getModuleManager();
    }

    private static void add(String name, int wait, Runnable action)
    {
        STEPS.add(new Step(name, wait, action));
    }

    private static void show(String name, java.util.function.Supplier<GuiScreen> screen)
    {
        add("open " + name, 15, () -> mc().displayGuiScreen(screen.get()));
        add("screenshot " + name, 2, () -> shot(name));
    }

    private static void shot(String name)
    {
        ScreenShotHelper.saveScreenshot(mc().mcDataDir, "selftest-" + name + ".png", mc().displayWidth, mc().displayHeight, mc().getFramebuffer());
    }

    private static void build()
    {
        if ("pack".equals(System.getProperty("sythix.selftest")))
        {
            add("modern pack converter", 1, SelfTest::modernPack);
            add("report", 1, SelfTest::report);
            add("quit", 1, () -> mc().shutdown());
            return;
        }

        if ("anim".equals(System.getProperty("sythix.selftest")))
        {
            add("slow animation", 1, () -> set(net.sythix.client.module.performance.AnimationController.class, "Menu Animation Time", "600"));
            add("open", 4, () -> mc().displayGuiScreen(new GuiModMenu(null)));
            add("screenshot anim-mid", 20, () -> shot("anim-mid"));
            add("screenshot anim-end", 1, () -> shot("anim-end"));
            add("quit", 1, () -> mc().shutdown());
            return;
        }

        if ("cosmetics".equals(System.getProperty("sythix.selftest")))
        {
            add("equip", 1, SelfTest::equipCosmetics);
            show("cosmetics-profile", () -> new GuiProfile(null));
            add("quit", 1, () -> mc().shutdown());
            return;
        }

        if ("profile".equals(System.getProperty("sythix.selftest")))
        {
            show("profile", () -> new GuiProfile(null));
            add("quit", 1, () -> mc().shutdown());
            return;
        }

        add("config round trip", 1, SelfTest::configRoundTrip);
        add("modern pack converter", 1, SelfTest::modernPack);
        add("settings on menu", 1, () -> exerciseSettings(true));
        add("module screens on menu", 1, SelfTest::drawModuleScreens);
        add("reset", 5, () -> modules().resetAll());
        add("safe mode", 1, SelfTest::safeMode);
        show("main-menu", () -> new net.minecraft.client.gui.GuiMainMenu());
        show("mod-menu", () -> new GuiModMenu(null));
        show("sythix-settings", () -> new GuiSythixSettings(null));
        show("profile", () -> new GuiProfile(null));
        show("options", () -> new GuiOptions(null, mc().gameSettings));
        show("multiplayer", () -> new GuiMultiplayer(null));
        show("help", () -> new net.sythix.client.module.client.HelpCenter().createScreen(null));
        show("keybinds", () -> modules().get(net.sythix.client.module.client.KeybindManager.class).createScreen(null));
        show("diagnostics", () -> modules().get(net.sythix.client.module.advanced.Diagnostics.class).createScreen(null));
        add("create world", 1, () -> {
            mc().displayGuiScreen(null);
            File old = new File(new File(mc().mcDataDir, "saves"), "sythix-selftest");

            try
            {
                FileUtils.deleteDirectory(old);
            }
            catch (Exception ignored)
            {
            }

            mc().launchIntegratedServer("sythix-selftest", "Sythix Self Test", new WorldSettings(12345L, GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
        });
        add("wait for world", 1, SelfTest::waitForWorld);
        add("enable all modules", 100, () -> {
            for (Module m : modules().getModules())
            {
                m.setEnabled(true);
            }

            pass("all modules enabled in world");
        });
        add("screenshot in-game-hud", 2, () -> shot("in-game-hud"));
        add("events", 20, SelfTest::events);
        add("settings in world", 40, () -> exerciseSettings(false));
        add("hud previews", 1, SelfTest::hudPreviews);
        add("reset in world", 1, () -> {
            modules().resetAll();

            for (Module m : modules().getModules())
            {
                m.setEnabled(true);
            }
        });
        add("module screens in world", 1, SelfTest::drawModuleScreens);
        add("third person", 40, () -> mc().gameSettings.thirdPersonView = 2);
        add("screenshot third-person", 2, () -> shot("third-person"));
        add("third person back", 40, () -> mc().gameSettings.thirdPersonView = 1);
        add("screenshot third-person-back", 2, () -> shot("third-person-back"));
        add("first person", 5, () -> mc().gameSettings.thirdPersonView = 0);
        add("tab list", 10, () -> KeyBinding.setKeyBindState(mc().gameSettings.keyBindPlayerList.getKeyCode(), true));
        add("screenshot tab-list", 2, () -> shot("tab-list"));
        add("tab list off", 2, () -> KeyBinding.setKeyBindState(mc().gameSettings.keyBindPlayerList.getKeyCode(), false));
        show("inventory", () -> new GuiInventory(mc().player));
        show("chat", () -> new GuiChat("hello"));
        show("hud-editor", () -> new GuiHudEditor(null));
        show("mod-menu-ingame", () -> new GuiModMenu(null));
        add("close", 5, () -> mc().displayGuiScreen(null));
        add("recording mode", 20, () -> {
            for (Setting<?> s : modules().get(RecordingTools.class).getSettings())
            {
                if (s instanceof ButtonSetting && s.getName().equals("Recording Mode"))
                {
                    ((ButtonSetting)s).press();
                }
            }
        });
        add("screenshot recording", 60, () -> shot("recording"));
        add("stop recording", 60, () -> press(modules().get(RecordingTools.class), "Recording Mode"));
        add("check video", 1, SelfTest::checkVideo);
        add("normal screenshot", 5, () -> mc().ingameGUI.getChatGUI().printChatMessage(ScreenShotHelper.saveScreenshot(mc().mcDataDir, mc().displayWidth, mc().displayHeight, mc().getFramebuffer())));
        add("report", 1, SelfTest::report);
        add("leave world", 40, () -> {
            mc().loadWorld(null);
            mc().displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
        });
        add("final report", 20, () -> {
            pass("left the world and returned to the main menu");
            report();
        });
        add("quit", 1, () -> mc().shutdown());
    }

    private static int waitTicks;

    private static void waitForWorld()
    {
        if (mc().world == null || mc().player == null)
        {
            if (++waitTicks > 1200)
            {
                fail("world did not load in 60 seconds", new IllegalStateException());
                return;
            }

            // Run this step again next tick
            --index;
            return;
        }

        pass("world loaded");
    }

    private static byte[] png(int w, int h, int color) throws Exception
    {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; ++y)
        {
            for (int x = 0; x < w; ++x)
            {
                img.setRGB(x, y, color);
            }
        }

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /** Builds a fake 1.21-style pack, converts it and checks the result. */
    private static void modernPack()
    {
        try
        {
            File dir = new File(mc().mcDataDir, "sythix/selftest-packs");
            FileUtils.deleteDirectory(dir);
            dir.mkdirs();
            File pack = new File(dir, "Modern Test.zip");
            String[][] files = new String[][] {
                {"textures/block/oak_planks.png", "16x16"}, {"textures/block/wheat_stage3.png", "16x16"}, {"textures/block/grass_block_top.png", "16x16"},
                {"textures/block/red_wool.png", "16x16"}, {"textures/block/light_gray_concrete.png", "16x16"}, {"textures/block/stone.png", "16x48"},
                {"textures/block/cherry_planks.png", "16x16"}, {"textures/item/golden_sword.png", "16x16"}, {"textures/item/netherite_sword.png", "16x16"},
                {"textures/entity/creeper/creeper.png", "64x32"}, {"textures/entity/chest/normal.png", "64x64"}, {"textures/block/spruce_log_top.png", "32x32"}};

            try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(pack)))
            {
                out.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));
                out.write("{\"pack\":{\"pack_format\":34,\"description\":\"test\"}}".getBytes(StandardCharsets.UTF_8));

                for (String[] f : files)
                {
                    String[] size = f[1].split("x");
                    out.putNextEntry(new java.util.zip.ZipEntry("assets/minecraft/" + f[0]));
                    out.write(png(Integer.parseInt(size[0]), Integer.parseInt(size[1]), 0xFF20C040));
                }

                out.putNextEntry(new java.util.zip.ZipEntry("assets/minecraft/textures/block/stone.png.mcmeta"));
                out.write("{\"animation\":{}}".getBytes(StandardCharsets.UTF_8));
            }

            if (!net.sythix.client.pack.ModernPackConverter.isModern(pack))
            {
                fail("modern pack detection", new IllegalStateException("not detected"));
                return;
            }

            net.sythix.client.pack.ModernPackConverter.Result r = net.sythix.client.pack.ModernPackConverter.convert(pack, dir);
            Set<String> names = new HashSet<String>();

            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(r.output))
            {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> e = zip.entries();

                while (e.hasMoreElements())
                {
                    names.add(e.nextElement().getName());
                }
            }

            String[] expected = new String[] {"blocks/planks_oak.png", "blocks/wheat_stage_3.png", "blocks/grass_top.png", "blocks/wool_colored_red.png", "blocks/concrete_silver.png",
                "blocks/stone.png", "blocks/stone.png.mcmeta", "items/gold_sword.png", "entity/creeper/creeper.png", "blocks/log_spruce_top.png"};
            List<String> missing = new ArrayList<String>();

            for (String x : expected)
            {
                if (!names.contains("assets/minecraft/textures/" + x))
                {
                    missing.add(x);
                }
            }

            boolean noChest = !names.contains("assets/minecraft/textures/entity/chest/normal.png");

            if (missing.isEmpty() && noChest && r.converted == 9 && r.skippedNew == 2 && r.skippedLayout == 1 && names.contains("pack.mcmeta"))
            {
                pass("modern pack converter (" + String.join(" ", r.notes) + ")");
            }
            else
            {
                fail("modern pack converter", new IllegalStateException("missing " + missing + ", chest skipped " + noChest + ", " + r.converted + "/" + r.skippedNew + "/" + r.skippedLayout + " " + names));
            }
        }
        catch (Throwable t)
        {
            fail("modern pack converter", t);
        }
    }

    /** Opens the newest recording and checks it is a valid MJPEG AVI. */
    private static void checkVideo()
    {
        try
        {
            File file = net.sythix.client.module.recording.VideoRecorder.getOutput();

            if (file == null || !file.isFile())
            {
                fail("video recorder", new IllegalStateException("no file"));
                return;
            }

            byte[] data = FileUtils.readFileToByteArray(file);
            java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            String riff = new String(data, 0, 4, StandardCharsets.US_ASCII) + new String(data, 8, 4, StandardCharsets.US_ASCII);
            int frames = b.getInt(48);
            int width = b.getInt(64);
            int height = b.getInt(68);
            int movi = new String(data, StandardCharsets.ISO_8859_1).indexOf("movi00dc");
            int size = b.getInt(movi + 8);
            java.awt.image.BufferedImage first = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data, movi + 12, size));
            boolean idx = new String(data, StandardCharsets.ISO_8859_1).contains("idx1");

            if (riff.equals("RIFFAVI ") && frames >= 30 && first != null && first.getWidth() == width && first.getHeight() == height && idx && b.getInt(4) == data.length - 8)
            {
                pass("video recorder: " + file.getName() + ", " + frames + " frames " + width + "x" + height + ", " + data.length / 1024 + " KB");
            }
            else
            {
                fail("video recorder", new IllegalStateException(riff + " frames=" + frames + " first=" + first + " idx=" + idx + " size=" + b.getInt(4) + "/" + (data.length - 8)));
            }
        }
        catch (Throwable t)
        {
            fail("video recorder", t);
        }
    }

    private static void configRoundTrip()
    {
        String before = SythixClient.get().getConfigManager().toJsonString();
        modules().resetAll();
        SythixClient.get().getConfigManager().apply(before);
        String after = SythixClient.get().getConfigManager().toJsonString();

        if (before.equals(after))
        {
            pass("config save/load round trip");
        }
        else
        {
            fail("config round trip changed the data", new IllegalStateException(diff(before, after)));
        }
    }

    /** Changes every setting, checks it survives a JSON round trip, presses safe buttons. */
    private static void exerciseSettings(boolean pressButtons)
    {
        int count = 0;

        for (Module m : modules().getModules())
        {
            try
            {
                m.setEnabled(true);

                for (Setting<?> s : m.getSettings())
                {
                    change(s);

                    if (pressButtons && s instanceof ButtonSetting && !SKIP_BUTTONS.contains(s.getName()))
                    {
                        ((ButtonSetting)s).press();
                        mc().displayGuiScreen(null);
                    }

                    ++count;
                }

                com.google.gson.JsonObject json = m.toJson();
                m.resetToDefaults();
                m.fromJson(json);

                if (!json.toString().equals(m.toJson().toString()))
                {
                    fail(m.getName() + " settings round trip", new IllegalStateException(json + " != " + m.toJson()));
                }

                m.toggle();
                m.toggle();
            }
            catch (Throwable t)
            {
                fail(m.getName() + " settings", t);
            }
        }

        pass(count + " settings changed and saved/loaded (" + (pressButtons ? "menu" : "world") + ")");
    }

    private static void change(Setting<?> s)
    {
        if (s instanceof BooleanSetting)
        {
            ((BooleanSetting)s).set(Boolean.valueOf(!((BooleanSetting)s).get().booleanValue()));
        }
        else if (s instanceof NumberSetting)
        {
            NumberSetting n = (NumberSetting)s;
            n.set(Double.valueOf(n.getMin() + (n.getMax() - n.getMin()) * 0.75D));
        }
        else if (s instanceof ModeSetting)
        {
            ((ModeSetting)s).cycle(1);
        }
        else if (s instanceof ColorSetting)
        {
            ((ColorSetting)s).set(Integer.valueOf(0xC02F6BFF));
        }
        else if (s instanceof TextSetting && !s.getName().contains("URL"))
        {
            ((TextSetting)s).set("Test");
        }
    }

    private static void drawModuleScreens()
    {
        int count = 0;
        int w = new ScaledResolution(mc()).getScaledWidth();
        int h = new ScaledResolution(mc()).getScaledHeight();

        for (Module m : modules().getModules())
        {
            try
            {
                GuiScreen screen = m.createScreen(null);

                if (screen != null)
                {
                    screen.setWorldAndResolution(mc(), w, h);
                    screen.drawScreen(w / 2, h / 2, 0.0F);
                    ++count;
                }

                if (m.hasPreview())
                {
                    m.renderPreview(10, 10, 160, 90);
                }
            }
            catch (Throwable t)
            {
                fail(m.getName() + " screen", t);
            }
        }

        // Also the mod menu settings view for every module
        GuiModMenu menu = new GuiModMenu(null);
        menu.setWorldAndResolution(mc(), w, h);

        for (int i = 0; i < 3; ++i)
        {
            menu.drawScreen(w / 2, h / 2, 0.0F);
        }

        pass(count + " module screens drawn");
    }

    private static void safeMode()
    {
        Module safe = modules().get(net.sythix.client.module.advanced.SafeMode.class);
        String before = SythixClient.get().getConfigManager().toJsonString();
        press(safe, "Enter Safe Mode");
        int on = 0;

        for (Module m : modules().getModules())
        {
            if (!m.isCore() && m.isEnabled())
            {
                ++on;
            }
        }

        press(safe, "Exit And Restore");
        String after = SythixClient.get().getConfigManager().toJsonString();

        if (on == 0 && before.equals(after))
        {
            pass("safe mode switches modules off and restores them");
        }
        else
        {
            fail("safe mode", new IllegalStateException(on + " modules still on, differences: " + diff(before, after)));
        }
    }

    /** Which module settings differ between two config JSON texts. */
    private static String diff(String a, String b)
    {
        com.google.gson.JsonObject x = new com.google.gson.JsonParser().parse(a).getAsJsonObject().getAsJsonObject("modules");
        com.google.gson.JsonObject y = new com.google.gson.JsonParser().parse(b).getAsJsonObject().getAsJsonObject("modules");
        StringBuilder out = new StringBuilder();

        for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : x.entrySet())
        {
            com.google.gson.JsonElement other = y.get(e.getKey());

            if (other == null || !e.getValue().equals(other))
            {
                out.append(e.getKey()).append(": ").append(e.getValue()).append(" -> ").append(other).append("; ");
            }
        }

        return out.length() == 0 ? "(only outside modules)" : out.toString();
    }

    /** Sets one setting from a JSON value, e.g. set(CosmeticsModule.class, "Hat", "\"Crown\""). */
    private static void set(Class<? extends Module> type, String name, String json)
    {
        for (Setting<?> s : modules().get(type).getSettings())
        {
            if (s.getName().equals(name))
            {
                s.fromJson(new com.google.gson.JsonParser().parse(json));
            }
        }
    }

    private static void equipCosmetics()
    {
        Class<net.sythix.client.module.visuals.CosmeticsModule> c = net.sythix.client.module.visuals.CosmeticsModule.class;
        modules().get(c).setEnabled(true);
        set(c, "Hat", "\"Crown\"");
        set(c, "Hat Color", "{\"argb\":\"#FFFFD020\"}");
        set(c, "Shoulder Buddy", "\"Slime\"");
        set(c, "Wings", "true");
        set(c, "Wing Style", "\"Angel\"");
        set(c, "Wing Color", "{\"argb\":\"#E0FFFFFF\"}");
        set(c, "Aura", "\"Hearts\"");
        net.sythix.client.cosmetic.ProfileStore.cape = "anim:chroma";
        pass("cosmetics equipped");
    }

    private static void press(Module m, String name)
    {
        for (Setting<?> s : m.getSettings())
        {
            if (s instanceof ButtonSetting && s.getName().equals(name))
            {
                ((ButtonSetting)s).press();
            }
        }
    }

    private static void events()
    {
        Hooks.onServerChat(new TextComponentString("<Steve> hello " + mc().getSession().getUsername()));
        Hooks.onServerChat(new TextComponentString("Steve was slain by " + mc().getSession().getUsername()));
        Hooks.onAttack(mc().player);
        Hooks.onMouseClick(0);
        Hooks.onMouseClick(1);
        Hooks.onKeyPressed(17);
        Hooks.onKeyReleased(17);
        Hooks.onRenderWorld(0.0F);
        mc().ingameGUI.getChatGUI().printChatMessage(new TextComponentString("Sythix self test chat line"));
        pass("events dispatched");
    }

    private static void hudPreviews()
    {
        ScaledResolution res = new ScaledResolution(mc());

        for (HudModule m : modules().getHudModules())
        {
            try
            {
                m.render(res, true);
                m.render(res, false);
            }
            catch (Throwable t)
            {
                fail(m.getName() + " HUD", t);
            }
        }

        pass(modules().getHudModules().size() + " HUD elements drawn");
    }

    private static void report()
    {
        List<String> lines = new ArrayList<String>(RESULTS);
        int total = failures;

        for (String e : ModuleErrors.getReports())
        {
            ++total;
            lines.add("FAIL module error: " + e);
        }

        lines.add(0, (total == 0 ? "ALL PASSED" : total + " FAILURES") + " - " + modules().getModules().size() + " modules");
        String text = String.join("\n", lines) + "\n";
        LOGGER.info("[SelfTest] result\n{}", text);

        try
        {
            FileUtils.writeStringToFile(new File(new File(mc().mcDataDir, "sythix"), "selftest.txt"), text, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            LOGGER.error("[SelfTest] could not write report", e);
        }
    }
}
