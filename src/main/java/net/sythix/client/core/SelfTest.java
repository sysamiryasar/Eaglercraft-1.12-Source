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
        if ("modern".equals(System.getProperty("sythix.selftest")))
        {
            modernSteps();
            add("report", 1, SelfTest::report);
            add("quit", 1, () -> mc().shutdown());
            return;
        }

        if ("replay".equals(System.getProperty("sythix.selftest")))
        {
            replaySteps();
            add("report", 1, SelfTest::report);
            add("quit", 1, () -> mc().shutdown());
            return;
        }

        if ("worldgen".equals(System.getProperty("sythix.selftest")))
        {
            worldgenSteps();
            add("report", 1, SelfTest::report);
            add("quit", 1, () -> mc().shutdown());
            return;
        }

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
        add("benchmark", 1, () -> {
            net.sythix.client.module.performance.Benchmark.start(10, true, "selftest");
            benchCount = net.sythix.client.module.performance.Benchmark.load().size();
        });
        add("benchmark running", 100, () -> shot("benchmark"));
        add("benchmark wait", 140, () -> {});
        add("check benchmark", 1, () -> {
            List<com.google.gson.JsonObject> all = net.sythix.client.module.performance.Benchmark.load();
            com.google.gson.JsonObject last = all.isEmpty() ? null : all.get(all.size() - 1);

            if (!net.sythix.client.module.performance.Benchmark.isRunning() && last != null && all.size() > benchCount - 1 && last.get("avgFps").getAsDouble() > 0.0D && last.get("lowFps").getAsDouble() <= last.get("avgFps").getAsDouble())
            {
                pass("benchmark: " + last.get("avgFps").getAsString() + " FPS avg, " + last.get("lowFps").getAsString() + " 1% low, " + last.get("frames").getAsString() + " frames");
            }
            else
            {
                fail("benchmark", new IllegalStateException("running=" + net.sythix.client.module.performance.Benchmark.isRunning() + " results=" + all));
            }
        });
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
        add("replay tests", 1, () -> {});
        replaySteps();
        add("final report", 20, () -> {
            pass("left the world and returned to the main menu");
            report();
        });
        add("quit", 1, () -> mc().shutdown());
    }

    private static File replayFile;
    private static int benchCount;
    private static int replayStage;

    /** Records a short singleplayer session, then plays it back and checks it. */
    private static void replaySteps()
    {
        add("enable replay recording", 1, () -> {
            modules().get(net.sythix.client.module.recording.ReplaySystem.class).setEnabled(true);
            set(net.sythix.client.module.recording.ReplaySystem.class, "Record Replays", "\"Everything\"");
        });
        add("create replay world", 1, () -> {
            File old = new File(new File(mc().mcDataDir, "saves"), "sythix-replaytest");

            try
            {
                FileUtils.deleteDirectory(old);
            }
            catch (Exception ignored)
            {
            }

            mc().launchIntegratedServer("sythix-replaytest", "Sythix Replay Test", new WorldSettings(777L, GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
        });
        add("wait for replay world", 1, SelfTest::waitForWorld);
        add("check recording", 40, () -> {
            if (net.sythix.client.replay.ReplayRecorder.isRecording())
            {
                pass("replay recording started on join");
            }
            else
            {
                fail("replay recording", new IllegalStateException("not recording after join"));
            }
        });
        add("summon pig", 20, () -> mc().player.sendChatMessage("/summon pig ~3 ~ ~"));
        add("chat", 20, () -> mc().player.sendChatMessage("hello replay"));
        add("marker", 1, net.sythix.client.replay.ReplayRecorder::marker);

        for (int i = 0; i < 6; ++i)
        {
            final int step = i;
            add("move " + i, 20, () -> {
                mc().player.setPositionAndRotation(mc().player.posX + 1.0D, mc().player.posY, mc().player.posZ, (float)(step * 30), 0.0F);
                mc().player.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
            });
        }

        add("screenshot replay-recorded", 2, () -> shot("replay-recorded"));
        add("leave replay world", 60, () -> {
            mc().loadWorld(null);
            mc().displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
        });
        add("check replay file", 1, () -> {
            replayFile = net.sythix.client.replay.ReplayRecorder.current();
            com.google.gson.JsonObject summary = replayFile == null ? new com.google.gson.JsonObject() : net.sythix.client.replay.ReplayFormat.summary(replayFile);

            if (!net.sythix.client.replay.ReplayRecorder.isRecording() && replayFile != null && replayFile.isFile() && summary.has("duration") && summary.get("duration").getAsLong() > 5000L && summary.getAsJsonArray("markers").size() == 1)
            {
                pass("replay saved: " + replayFile.getName() + ", " + summary.get("duration").getAsLong() + " ms, " + replayFile.length() / 1024 + " KB");
            }
            else
            {
                fail("replay file", new IllegalStateException(replayFile + " " + summary));
            }

            set(net.sythix.client.module.recording.ReplaySystem.class, "Record Replays", "\"Off\"");
        });
        add("play replay", 1, () -> net.sythix.client.replay.ReplayPlayer.play(replayFile));
        add("wait for replay", 1, SelfTest::waitForWorld);
        add("watch", 200, () -> {});
        add("screenshot replay-playing", 2, () -> shot("replay-playing"));
        add("check playback", 1, () -> {
            boolean spectator = mc().playerController != null && mc().playerController.isSpectator();
            boolean avatar = mc().world.getEntityByID(-7351) != null;
            boolean pig = false;

            for (net.minecraft.entity.Entity e : mc().world.loadedEntityList)
            {
                pig |= e instanceof net.minecraft.entity.passive.EntityPig;
            }

            long t = net.sythix.client.replay.ReplayPlayer.getTime();

            if (net.sythix.client.replay.ReplayPlayer.isActive() && spectator && avatar && pig && t > 5000L)
            {
                pass("replay playback: world, recorded player, pig, spectator camera, time " + t + " ms");
            }
            else
            {
                fail("replay playback", new IllegalStateException("active=" + net.sythix.client.replay.ReplayPlayer.isActive() + " spectator=" + spectator + " avatar=" + avatar + " pig=" + pig + " time=" + t));
            }
        });
        add("seek back", 1, () -> net.sythix.client.replay.ReplayPlayer.seek(2000L));
        add("wait seek", 60, () -> {});
        add("check seek", 1, () -> {
            long t = net.sythix.client.replay.ReplayPlayer.getTime();

            if (mc().world != null && net.sythix.client.replay.ReplayPlayer.isActive() && t >= 2000L && t < 6000L)
            {
                pass("replay seek back: time " + t + " ms");
            }
            else
            {
                fail("replay seek", new IllegalStateException("world=" + mc().world + " time=" + t));
            }
        });
        add("pause", 40, net.sythix.client.replay.ReplayPlayer::togglePause);
        add("check pause", 1, () -> {
            long a = net.sythix.client.replay.ReplayPlayer.getTime();
            replayStage = (int)a;
        });
        add("still paused", 1, () -> {
            if (net.sythix.client.replay.ReplayPlayer.getTime() == replayStage)
            {
                pass("replay pause");
            }
            else
            {
                fail("replay pause", new IllegalStateException("time moved"));
            }
        });
        add("exit replay", 40, net.sythix.client.replay.ReplayPlayer::exit);
        add("check exit", 1, () -> {
            if (mc().world == null && !net.sythix.client.replay.ReplayPlayer.isActive())
            {
                pass("left the replay");
            }
            else
            {
                fail("replay exit", new IllegalStateException("still in replay"));
            }
        });
    }

    private static BlockPosHolder modernOrigin = new BlockPosHolder();
    private static net.minecraft.entity.Entity[] modernEntities;
    private static net.minecraft.entity.Entity modernFloater;
    private static int slimesBefore;

    private static final class BlockPosHolder
    {
        net.minecraft.util.math.BlockPos pos;
    }

    /** Checks the extra 1.21 blocks and items: models, recipes, placing, saving. */
    private static void modernSteps()
    {
        add("modern models", 1, () -> {
            net.minecraft.client.renderer.block.model.ModelManager models = mc().getRenderItem().getItemModelMesher().getModelManager();
            List<String> missing = new ArrayList<String>();
            int states = 0;

            for (java.util.Map.Entry<String, net.minecraft.block.Block> e : net.sythix.modern.ModernBlocks.all().entrySet())
            {
                for (net.minecraft.block.state.IBlockState st : e.getValue().getBlockState().getValidStates())
                {
                    ++states;

                    if (e.getValue().getDefaultState().getRenderType() == net.minecraft.util.EnumBlockRenderType.MODEL && mc().getBlockRendererDispatcher().getModelForState(st) == models.getMissingModel())
                    {
                        missing.add(e.getKey() + "[" + st + "]");
                        break;
                    }
                }

                if (mc().getRenderItem().getItemModelMesher().getItemModel(new net.minecraft.item.ItemStack(e.getValue())) == models.getMissingModel())
                {
                    missing.add(e.getKey() + " (item)");
                }
            }

            for (java.util.Map.Entry<String, net.minecraft.item.Item> e : net.sythix.modern.ModernItems.all().entrySet())
            {
                if (mc().getRenderItem().getItemModelMesher().getItemModel(new net.minecraft.item.ItemStack(e.getValue())) == models.getMissingModel())
                {
                    missing.add(e.getKey() + " (item)");
                }
            }

            int recipes = 0;

            for (net.minecraft.util.ResourceLocation key : net.minecraft.item.crafting.CraftingManager.REGISTRY.getKeys())
            {
                if (key.getResourcePath().startsWith("sythix_"))
                {
                    ++recipes;
                }
            }

            if (missing.isEmpty() && recipes > 30)
            {
                pass("modern content: " + net.sythix.modern.ModernBlocks.all().size() + " blocks (" + states + " states), " + net.sythix.modern.ModernItems.all().size() + " items, all have models; " + recipes + " recipes");
            }
            else
            {
                fail("modern models/recipes", new IllegalStateException("recipes=" + recipes + " missing=" + missing));
            }
        });
        add("create modern world", 1, () -> {
            try
            {
                FileUtils.deleteDirectory(new File(new File(mc().mcDataDir, "saves"), "sythix-moderntest"));
            }
            catch (Exception ignored)
            {
            }

            mc().launchIntegratedServer("sythix-moderntest", "Sythix Modern Test", new WorldSettings(4242L, GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
        });
        add("wait for modern world", 1, SelfTest::waitForWorld);
        add("place modern blocks", 40, () -> onServer(() -> {
            net.minecraft.world.World world = mc().getIntegratedServer().getWorld(0);
            net.minecraft.util.math.BlockPos base = new net.minecraft.util.math.BlockPos(mc().player).add(2, 0, -6);
            modernOrigin.pos = base;
            int i = 0;

            for (net.minecraft.block.Block b : net.sythix.modern.ModernBlocks.all().values())
            {
                net.minecraft.util.math.BlockPos p = base.add((i % 12) * 2, 0, (i / 12) * 2);
                world.setBlockState(p.down(), net.minecraft.init.Blocks.GRASS.getDefaultState());
                world.setBlockState(p, b.getDefaultState());
                ++i;
            }

            net.minecraft.entity.player.EntityPlayerMP owner = mc().getIntegratedServer().getPlayerList().getPlayers().get(0);
            owner.connection.setPlayerLocation(base.getX() + 11.0D, base.getY() + 7.0D, base.getZ() - 6.0D, 0.0F, 45.0F);
            pass(i + " modern blocks placed");
        }));
        add("fly", 1, () -> mc().player.capabilities.isFlying = true);
        add("screenshot modern-blocks", 2, () -> shot("modern-blocks"));
        add("modern items", 1, () -> onServer(() -> {
            net.minecraft.world.WorldServer world = mc().getIntegratedServer().getWorld(0);
            net.minecraft.entity.player.EntityPlayerMP sp = mc().getIntegratedServer().getPlayerList().getPlayers().get(0);
            List<String> problems = new ArrayList<String>();

            // Netherite upgrade keeps enchantments
            net.minecraft.inventory.InventoryCrafting grid = new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container()
            {
                public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn)
                {
                    return true;
                }
            }, 3, 3);
            net.minecraft.item.ItemStack sword = new net.minecraft.item.ItemStack(net.minecraft.init.Items.DIAMOND_SWORD);
            sword.addEnchantment(net.minecraft.init.Enchantments.SHARPNESS, 3);
            grid.setInventorySlotContents(0, sword);
            grid.setInventorySlotContents(1, new net.minecraft.item.ItemStack(net.sythix.modern.ModernItems.get("netherite_ingot")));
            net.minecraft.item.ItemStack out = net.minecraft.item.crafting.CraftingManager.findMatchingResult(grid, world);

            if (out.getItem() != net.sythix.modern.ModernItems.get("netherite_sword") || net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.SHARPNESS, out) != 3)
            {
                problems.add("netherite upgrade gave " + out);
            }

            // Thrown trident and wind charge
            net.sythix.modern.entity.EntityThrownTrident trident = new net.sythix.modern.entity.EntityThrownTrident(world, sp, new net.minecraft.item.ItemStack(net.sythix.modern.ModernItems.get("trident")), true);
            trident.setHeadingFromThrower(sp, 30.0F, sp.rotationYaw, 0.0F, 1.5F, 0.0F);
            world.spawnEntity(trident);
            net.sythix.modern.entity.EntityWindCharge charge = new net.sythix.modern.entity.EntityWindCharge(world, sp);
            charge.setHeadingFromThrower(sp, 80.0F, sp.rotationYaw, 0.0F, 1.0F, 0.0F);
            world.spawnEntity(charge);
            modernEntities = new net.minecraft.entity.Entity[] {trident, charge};

            // A mangrove boat and netherite armour on the player
            net.minecraft.entity.item.EntityBoat boat = new net.minecraft.entity.item.EntityBoat(world, sp.posX + 2.0D, sp.posY, sp.posZ + 3.0D);
            boat.setBoatType(net.minecraft.entity.item.EntityBoat.Type.MANGROVE);
            world.spawnEntity(boat);
            sp.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.CHEST, new net.minecraft.item.ItemStack(net.sythix.modern.ModernItems.get("netherite_chestplate")));
            sp.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD, new net.minecraft.item.ItemStack(net.sythix.modern.ModernItems.get("turtle_helmet")));
            sp.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.LEGS, new net.minecraft.item.ItemStack(net.sythix.modern.ModernItems.get("netherite_leggings")));
            sp.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, new net.minecraft.item.ItemStack(net.sythix.modern.ModernItems.get("mace")));

            if (problems.isEmpty())
            {
                pass("netherite upgrade keeps enchantments; trident, wind charge and mangrove boat spawned");
            }
            else
            {
                fail("modern items", new IllegalStateException(problems.toString()));
            }
        }));
        add("modern effects", 1, () -> onServer(() -> {
            net.minecraft.world.WorldServer world = mc().getIntegratedServer().getWorld(0);
            net.minecraft.entity.player.EntityPlayerMP sp = mc().getIntegratedServer().getPlayerList().getPlayers().get(0);
            List<String> problems = new ArrayList<String>();

            if (net.sythix.modern.ModernEffects.effects().size() != 12 || net.sythix.modern.ModernEffects.enchantments().size() != 12)
            {
                problems.add("effects=" + net.sythix.modern.ModernEffects.effects().size() + " enchantments=" + net.sythix.modern.ModernEffects.enchantments().size());
            }

            // Brewing: awkward potion + phantom membrane = slow falling
            net.minecraft.item.ItemStack awkward = net.minecraft.potion.PotionUtils.addPotionToItemStack(new net.minecraft.item.ItemStack(net.minecraft.init.Items.POTIONITEM), net.minecraft.init.PotionTypes.AWKWARD);
            net.minecraft.item.ItemStack brewed = net.minecraft.potion.PotionHelper.doReaction(new net.minecraft.item.ItemStack(net.sythix.modern.ModernItems.get("phantom_membrane")), awkward);

            if (net.minecraft.potion.PotionUtils.getPotionFromItem(brewed) != net.minecraft.potion.PotionType.getPotionTypeForName("slow_falling"))
            {
                problems.add("brewing gave " + net.minecraft.potion.PotionUtils.getPotionFromItem(brewed).getNamePrefixed(""));
            }

            // A zombie with Slow Falling high in the air, and one with Oozing that dies
            net.minecraft.entity.monster.EntityZombie floater = new net.minecraft.entity.monster.EntityZombie(world);
            floater.setLocationAndAngles(sp.posX + 4.0D, sp.posY + 20.0D, sp.posZ, 0.0F, 0.0F);
            floater.addPotionEffect(new net.minecraft.potion.PotionEffect(net.sythix.modern.ModernEffects.effect("slow_falling"), 600));
            world.spawnEntity(floater);
            net.minecraft.entity.monster.EntityZombie oozer = new net.minecraft.entity.monster.EntityZombie(world);
            oozer.setLocationAndAngles(sp.posX - 4.0D, sp.posY, sp.posZ, 0.0F, 0.0F);
            oozer.addPotionEffect(new net.minecraft.potion.PotionEffect(net.sythix.modern.ModernEffects.effect("oozing"), 600));
            world.spawnEntity(oozer);
            slimesBefore = world.getEntities(net.minecraft.entity.monster.EntitySlime.class, x -> true).size();
            oozer.attackEntityFrom(net.minecraft.util.DamageSource.OUT_OF_WORLD, 1000.0F);
            modernFloater = floater;

            if (problems.isEmpty())
            {
                pass("12 effects, 12 enchantments; brewing slow falling works");
            }
            else
            {
                fail("modern effects", new IllegalStateException(problems.toString()));
            }
        }));
        add("wait projectiles", 120, () -> {});
        add("check effects", 1, () -> onServer(() -> {
            net.minecraft.world.WorldServer world = mc().getIntegratedServer().getWorld(0);
            int slimes = world.getEntities(net.minecraft.entity.monster.EntitySlime.class, x -> true).size();
            double perTick = modernFloater.prevPosY - modernFloater.posY;
            boolean slow = perTick < 0.08D && !modernFloater.onGround;

            if (slimes >= slimesBefore + 2 && slow)
            {
                pass("Oozing spawned slimes on death; Slow Falling zombie floats down (" + String.format("%.3f", perTick) + " blocks/tick)");
            }
            else
            {
                fail("effects behaviour", new IllegalStateException("slimes " + slimesBefore + "->" + slimes + " floater falls " + perTick + "/tick, onGround=" + modernFloater.onGround));
            }
        }));
        add("check projectiles", 1, () -> {
            boolean landed = modernEntities[0].isDead && modernEntities[1].isDead;

            if (landed)
            {
                pass("trident landed and the wind charge burst");
            }
            else
            {
                fail("projectiles", new IllegalStateException("trident dead=" + modernEntities[0].isDead + " wind dead=" + modernEntities[1].isDead));
            }

            mc().gameSettings.thirdPersonView = 2;
            net.minecraft.item.ItemStack chest = mc().player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.CHEST);

            if (chest.getItem() == net.sythix.modern.ModernItems.get("netherite_chestplate"))
            {
                pass("client player wears the netherite chestplate");
            }
            else
            {
                fail("armour on client", new IllegalStateException("chest=" + chest));
            }
        });
        add("screenshot modern-armor", 40, () -> shot("modern-armor"));
        add("first person again", 2, () -> mc().gameSettings.thirdPersonView = 0);
        add("check placed", 1, () -> {
            net.minecraft.world.World world = mc().world;
            List<String> wrong = new ArrayList<String>();
            int i = 0;

            for (java.util.Map.Entry<String, net.minecraft.block.Block> e : net.sythix.modern.ModernBlocks.all().entrySet())
            {
                net.minecraft.util.math.BlockPos p = modernOrigin.pos.add((i % 12) * 2, 0, (i / 12) * 2);
                net.minecraft.block.Block there = world.getBlockState(p).getBlock();

                // Plants on sand-only rules or falling blocks may legitimately change
                if (there != e.getValue() && !(e.getValue() instanceof net.minecraft.block.BlockFalling) && !(e.getValue() instanceof net.sythix.modern.block.MCopper) && !(e.getValue() instanceof net.sythix.modern.block.MSapling))
                {
                    wrong.add(e.getKey() + "=" + net.minecraft.block.Block.REGISTRY.getNameForObject(there));
                }

                ++i;
            }

            if (wrong.isEmpty())
            {
                pass("client received every modern block from the integrated server");
            }
            else
            {
                fail("modern blocks on client", new IllegalStateException(wrong.toString()));
            }
        });
        add("leave modern world", 60, () -> {
            mc().loadWorld(null);
            mc().displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
        });
        add("reopen modern world", 1, () -> mc().launchIntegratedServer("sythix-moderntest", "Sythix Modern Test", null));
        add("wait for reopened world", 1, SelfTest::waitForWorld);
        add("wait chunks", 60, () -> {});
        add("check saved", 1, () -> {
            net.minecraft.world.World world = mc().getIntegratedServer().getWorld(0);
            int ok = 0;
            int i = 0;
            List<String> wrong = new ArrayList<String>();

            for (java.util.Map.Entry<String, net.minecraft.block.Block> e : net.sythix.modern.ModernBlocks.all().entrySet())
            {
                net.minecraft.util.math.BlockPos p = modernOrigin.pos.add((i % 12) * 2, 0, (i / 12) * 2);

                if (world.getBlockState(p).getBlock() == e.getValue() || e.getValue() instanceof net.minecraft.block.BlockFalling || e.getValue() instanceof net.sythix.modern.block.MCopper || e.getValue() instanceof net.sythix.modern.block.MSapling)
                {
                    ++ok;
                }
                else
                {
                    wrong.add(e.getKey());
                }

                ++i;
            }

            if (wrong.isEmpty())
            {
                pass("all " + ok + " modern blocks were saved and loaded again");
            }
            else
            {
                fail("modern blocks after reload", new IllegalStateException(wrong.toString()));
            }
        });
        add("leave reopened world", 40, () -> {
            mc().loadWorld(null);
            mc().displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
        });
    }

    /** New default world: checks that the 1.21 blocks are generated. */
    private static void worldgenSteps()
    {
        add("create generated world", 1, () -> {
            try
            {
                FileUtils.deleteDirectory(new File(new File(mc().mcDataDir, "saves"), "sythix-worldgen"));
            }
            catch (Exception ignored)
            {
            }

            mc().launchIntegratedServer("sythix-worldgen", "Sythix Worldgen Test", new WorldSettings(20261011L, GameType.CREATIVE, true, false, WorldType.DEFAULT).enableCommands());
        });
        add("wait for generated world", 1, SelfTest::waitForWorld);
        add("let chunks generate", 200, () -> {});
        add("count generated blocks", 1, () -> onServer(() -> {
            net.minecraft.world.WorldServer world = mc().getIntegratedServer().getWorld(0);
            net.minecraft.util.math.BlockPos spawn = world.getSpawnPoint();
            java.util.Map<String, Integer> counts = new java.util.TreeMap<String, Integer>();
            int chunks = 0;
            long started = System.nanoTime();

            for (int cx = -3; cx <= 3; ++cx)
            {
                for (int cz = -3; cz <= 3; ++cz)
                {
                    net.minecraft.world.chunk.Chunk chunk = world.getChunkFromChunkCoords((spawn.getX() >> 4) + cx, (spawn.getZ() >> 4) + cz);

                    if (!chunk.isTerrainPopulated())
                    {
                        continue;
                    }

                    ++chunks;

                    for (int x = 0; x < 16; ++x)
                    {
                        for (int z = 0; z < 16; ++z)
                        {
                            for (int y = 1; y < 100; ++y)
                            {
                                net.minecraft.block.Block b = chunk.getBlockState(x, y, z).getBlock();
                                net.minecraft.util.ResourceLocation id = net.minecraft.block.Block.REGISTRY.getNameForObject(b);

                                if (net.sythix.modern.ModernBlocks.get(id.getResourcePath()) == b)
                                {
                                    counts.merge(id.getResourcePath(), Integer.valueOf(1), Integer::sum);
                                }
                            }
                        }
                    }
                }
            }

            boolean ok = chunks > 10 && counts.getOrDefault("deepslate", 0) > 1000 && counts.getOrDefault("copper_ore", 0) + counts.getOrDefault("deepslate_copper_ore", 0) > 10 && counts.getOrDefault("tuff", 0) > 10;

            if (ok)
            {
                pass("generated in " + chunks + " chunks: " + counts);
            }
            else
            {
                fail("world generation", new IllegalStateException(chunks + " chunks: " + counts));
            }
        }));
        add("look down", 1, () -> {
            mc().player.rotationPitch = 30.0F;
        });
        add("screenshot worldgen", 20, () -> shot("worldgen"));
        add("leave generated world", 40, () -> {
            mc().loadWorld(null);
            mc().displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
        });
    }

    private static int waitTicks;

    private static void waitForWorld()
    {
        if (mc().world == null || mc().player == null || mc().currentScreen instanceof net.minecraft.client.gui.GuiDownloadTerrain)
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

        waitTicks = 0;
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

    /** Runs a test action on the integrated server thread and waits for it. */
    private static void onServer(Runnable action)
    {
        try
        {
            com.google.common.util.concurrent.Futures.getUnchecked(mc().getIntegratedServer().addScheduledTask(action));
        }
        catch (Throwable t)
        {
            fail("server task", t);
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
