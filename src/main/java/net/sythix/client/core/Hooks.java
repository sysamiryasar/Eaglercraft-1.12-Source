package net.sythix.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.sythix.client.SythixClient;
import net.sythix.client.module.Module;
import net.sythix.client.module.ModuleManager;
import net.sythix.client.module.hud.HudModule;
import net.sythix.client.module.multiplayer.ConnectionMonitor;
import net.sythix.client.module.multiplayer.FriendsManager;
import net.sythix.client.module.multiplayer.ServerManagerModule;
import net.sythix.client.module.advanced.PrivacyControls;
import net.minecraft.client.multiplayer.ServerData;
import net.sythix.client.module.visuals.BlockVisuals;
import net.sythix.client.module.visuals.CosmeticsModule;
import net.sythix.client.module.visuals.EnvironmentVisuals;
import net.sythix.client.module.visuals.SoundController;
import net.sythix.client.cosmetic.CosmeticTextures;
import net.sythix.client.cosmetic.ProfileStore;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.util.ResourceLocation;
import net.sythix.client.module.hud.ChatModule;
import net.sythix.client.module.hud.NotificationsModule;
import net.sythix.client.module.hud.ScoreboardModule;
import net.sythix.client.module.hud.TabListModule;
import net.sythix.client.module.multiplayer.ChatUtilities;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.client.renderer.GlStateManager;
import net.sythix.client.module.inventory.InventoryEnhancements;
import net.sythix.client.module.inventory.ItemInformation;
import net.sythix.client.module.movement.CameraTools;
import net.sythix.client.module.performance.AnimationController;
import net.sythix.client.module.performance.ParticleController;
import net.sythix.client.module.performance.PerformanceMonitor;
import net.sythix.client.module.performance.PerformanceOptimizer;
import net.sythix.client.module.pvp.CombatAnimations;
import net.sythix.client.module.pvp.CrosshairModule;
import net.sythix.client.module.pvp.MovementControls;
import net.sythix.client.module.pvp.PotionHud;
import net.sythix.client.module.pvp.ViewmodelModule;
import net.sythix.client.ui.GuiHudEditor;
import net.sythix.client.ui.SythixTheme;
import net.sythix.client.ui.GuiModMenu;
import net.sythix.client.module.recording.RecordingTools;
import net.sythix.client.module.recording.ScreenshotManager;

/**
 * Entry points called from Minecraft's own code. Every method is safe to call before the client
 * has started and never lets a module error escape into the game.
 */
public final class Hooks
{
    private static WorldClient lastWorld;

    private Hooks()
    {
    }

    private static ModuleManager modules()
    {
        SythixClient client = SythixClient.get();
        return client == null ? null : client.getModuleManager();
    }

    /** The module if the client is running and the module is enabled, else null. */
    public static <T extends Module> T on(Class<T> type)
    {
        ModuleManager m = modules();
        return m == null ? null : m.getEnabled(type);
    }

    private static void fail(Class<?> type, String where, Throwable t)
    {
        ModuleManager m = modules();
        Module module = m == null ? null : m.get(type.asSubclass(Module.class));
        ModuleErrors.report(module, where, t);
    }

    // ---- Input -------------------------------------------------------------------------------

    /** Key pressed in game with no menu open. */
    public static void onKeyPressed(int keyCode)
    {
        ModuleManager m = modules();

        if (m == null)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (keyCode == mc.gameSettings.keyBindSythixMenu.getKeyCode())
        {
            mc.displayGuiScreen(new GuiModMenu(null));
            return;
        }

        m.handleToggleKeys(keyCode);
        m.forEachEnabled("key", module -> module.onKey(keyCode));
    }

    public static void onKeyReleased(int keyCode)
    {
        ModuleManager m = modules();

        if (m != null)
        {
            m.handleToggleRelease(keyCode);
            m.forEachEnabled("key release", module -> module.onKeyRelease(keyCode));
        }
    }

    /** Mouse button pressed in game with no menu open. */
    public static void onMouseClick(int button)
    {
        SythixClient client = SythixClient.get();

        if (client == null)
        {
            return;
        }

        client.getClickCounter().onClick(button);
        client.getModuleManager().forEachEnabled("mouse", module -> module.onMouseClick(button));
    }

    // ---- Game events ------------------------------------------------------------------------

    /** Start of every client tick. */
    public static void onTick()
    {
        SelfTest.tick();
        ModuleManager m = modules();

        if (m == null)
        {
            return;
        }

        WorldClient world = Minecraft.getMinecraft().world;

        if (world != null && world != lastWorld)
        {
            m.forEachEnabled("world join", Module::onWorldJoin);
        }

        lastWorld = world;
        m.forEachEnabled("tick", Module::onTick);
    }

    /** The player attacked an entity. */
    public static void onAttack(Entity target)
    {
        ModuleManager m = modules();

        if (m != null && target != null)
        {
            m.forEachEnabled("attack", module -> module.onAttack(target));
        }
    }

    /** A chat message arrived from the server. */
    public static void onServerChat(ITextComponent message)
    {
        ModuleManager m = modules();

        if (m != null && message != null)
        {
            m.forEachEnabled("chat", module -> module.onChat(message));
        }
    }

    /** After the world (3D) has been drawn. */
    public static void onRenderWorld(float partialTicks)
    {
        ModuleManager m = modules();

        if (m != null)
        {
            m.forEachEnabled("world render", module -> module.onRenderWorld(partialTicks));
        }
    }

    // ---- HUD ----------------------------------------------------------------------------------

    /** Draws HUD modules and notifications. Called by the in-game overlay every frame. */
    public static void renderHud(ScaledResolution res)
    {
        ModuleManager m = modules();

        if (m == null)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        PerformanceMonitor monitor = m.get(PerformanceMonitor.class);

        if (monitor != null)
        {
            monitor.onFrame();
        }

        if (mc.currentScreen instanceof GuiHudEditor || mc.gameSettings.showDebugInfo)
        {
            return;
        }

        if (net.sythix.client.module.performance.Benchmark.isRunning())
        {
            String text = "Benchmark " + Math.round(net.sythix.client.module.performance.Benchmark.progress() * 100.0F) + "% - FPS " + Minecraft.getDebugFPS();
            mc.fontRenderer.drawString(text, (float)(res.getScaledWidth() / 2 - mc.fontRenderer.getStringWidth(text) / 2), 4.0F, SythixTheme.LIGHT_BLUE, true);
            return;
        }

        RecordingTools recording = on(RecordingTools.class);
        boolean recordingClean = recording != null && recording.hidesHud();

        for (HudModule module : m.getHudModules())
        {
            if (module.isEnabled() && (!recordingClean || module == recording))
            {
                try
                {
                    if (module.shouldRender())
                    {
                        module.render(res, false);
                    }
                }
                catch (Throwable t)
                {
                    ModuleErrors.report(module, "HUD render", t);
                }
            }
        }
    }

    /** Draws the Sythix crosshair; returns true when it replaced the vanilla one. */
    public static boolean renderCrosshair(int screenWidth, int screenHeight)
    {
        CrosshairModule crosshair = on(CrosshairModule.class);

        if (crosshair == null)
        {
            return false;
        }

        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            crosshair.draw((float)screenWidth / 2.0F, (float)screenHeight / 2.0F, mc.pointedEntity != null);
            return true;
        }
        catch (Throwable t)
        {
            fail(CrosshairModule.class, "crosshair", t);
            return false;
        }
    }

    /** True if the crosshair should also be drawn in third person. */
    public static boolean crosshairInThirdPerson()
    {
        try
        {
            CrosshairModule crosshair = on(CrosshairModule.class);
            return crosshair != null && crosshair.showInThirdPerson();
        }
        catch (Throwable t)
        {
            fail(CrosshairModule.class, "crosshairInThirdPerson", t);
            CrosshairModule crosshair = (CrosshairModule)null;
            return crosshair != null && crosshair.showInThirdPerson();
        }
    }

    public static boolean hideVanillaPotionIcons()
    {
        try
        {
            PotionHud hud = on(PotionHud.class);
            return hud != null && hud.hideVanillaOverlay();
        }
        catch (Throwable t)
        {
            fail(PotionHud.class, "hideVanillaPotionIcons", t);
            PotionHud hud = (PotionHud)null;
            return hud != null && hud.hideVanillaOverlay();
        }
    }

    // ---- Performance --------------------------------------------------------------------------

    public static int limitFramerate(int vanilla)
    {
        try
        {
            RecordingTools recording = on(RecordingTools.class);

            if (recording != null && recording.fpsLock() > 0)
            {
                return recording.fpsLock();
            }

            PerformanceOptimizer optimizer = on(PerformanceOptimizer.class);

            if (optimizer == null)
            {
                return vanilla;
            }

            Minecraft mc = Minecraft.getMinecraft();
            int limit = optimizer.getFramerateLimit(org.lwjgl.opengl.Display.isActive(), mc.world != null && mc.currentScreen != null);
            return limit > 0 ? Math.min(vanilla, limit) : vanilla;
        }
        catch (Throwable t)
        {
            fail(RecordingTools.class, "limitFramerate", t);
            RecordingTools recording = (RecordingTools)null;

            if (recording != null && recording.fpsLock() > 0)
            {
                return recording.fpsLock();
            }

            PerformanceOptimizer optimizer = (PerformanceOptimizer)null;

            if (optimizer == null)
            {
                return vanilla;
            }

            Minecraft mc = Minecraft.getMinecraft();
            int limit = optimizer.getFramerateLimit(org.lwjgl.opengl.Display.isActive(), mc.world != null && mc.currentScreen != null);
            return limit > 0 ? Math.min(vanilla, limit) : vanilla;
        }
    }

    public static boolean shouldRenderEntity(Entity entity)
    {
        try
        {
            PerformanceOptimizer optimizer = on(PerformanceOptimizer.class);

            if (optimizer == null || optimizer.getEntityDistance() <= 0 || entity instanceof EntityPlayer)
            {
                return true;
            }

            Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
            int max = optimizer.getEntityDistance();
            return viewer == null || viewer.getDistanceSqToEntity(entity) <= (double)(max * max);
        }
        catch (Throwable t)
        {
            fail(PerformanceOptimizer.class, "shouldRenderEntity", t);
            PerformanceOptimizer optimizer = (PerformanceOptimizer)null;

            if (optimizer == null || optimizer.getEntityDistance() <= 0 || entity instanceof EntityPlayer)
            {
                return true;
            }

            Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
            int max = optimizer.getEntityDistance();
            return viewer == null || viewer.getDistanceSqToEntity(entity) <= (double)(max * max);
        }
    }

    public static boolean allowParticle(Particle particle)
    {
        try
        {
            ParticleController controller = on(ParticleController.class);
            return controller == null || controller.allow(particle);
        }
        catch (Throwable t)
        {
            fail(ParticleController.class, "allowParticle", t);
            ParticleController controller = (ParticleController)null;
            return controller == null || controller.allow(particle);
        }
    }

    public static boolean itemSpin()
    {
        try
        {
            AnimationController controller = on(AnimationController.class);
            return controller == null || controller.itemSpin();
        }
        catch (Throwable t)
        {
            fail(AnimationController.class, "itemSpin", t);
            AnimationController controller = (AnimationController)null;
            return controller == null || controller.itemSpin();
        }
    }

    public static boolean itemBob()
    {
        try
        {
            AnimationController controller = on(AnimationController.class);
            return controller == null || controller.itemBob();
        }
        catch (Throwable t)
        {
            fail(AnimationController.class, "itemBob", t);
            AnimationController controller = (AnimationController)null;
            return controller == null || controller.itemBob();
        }
    }

    // ---- First person and camera --------------------------------------------------------------

    /** Arm swing duration for the local player (vanilla value passed in). */
    public static int swingDuration(Entity entity, int vanilla)
    {
        try
        {
            CombatAnimations anim = on(CombatAnimations.class);

            if (anim == null || entity != Minecraft.getMinecraft().player)
            {
                return vanilla;
            }

            return Math.max(1, Math.round((float)vanilla / anim.getSwingSpeed()));
        }
        catch (Throwable t)
        {
            fail(CombatAnimations.class, "swingDuration", t);
            CombatAnimations anim = (CombatAnimations)null;

            if (anim == null || entity != Minecraft.getMinecraft().player)
            {
                return vanilla;
            }

            return Math.max(1, Math.round((float)vanilla / anim.getSwingSpeed()));
        }
    }

    public static boolean classicBlocking(ItemStack mainHand)
    {
        try
        {
            CombatAnimations anim = on(CombatAnimations.class);
            return anim != null && anim.isClassicBlocking(mainHand);
        }
        catch (Throwable t)
        {
            fail(CombatAnimations.class, "classicBlocking", t);
            CombatAnimations anim = (CombatAnimations)null;
            return anim != null && anim.isClassicBlocking(mainHand);
        }
    }

    public static boolean swingWhileUsing()
    {
        try
        {
            CombatAnimations anim = on(CombatAnimations.class);
            return anim != null && anim.swingWhileUsing();
        }
        catch (Throwable t)
        {
            fail(CombatAnimations.class, "swingWhileUsing", t);
            CombatAnimations anim = (CombatAnimations)null;
            return anim != null && anim.swingWhileUsing();
        }
    }

    public static boolean noEquipBounce()
    {
        try
        {
            CombatAnimations anim = on(CombatAnimations.class);
            return anim != null && anim.noEquipBounce();
        }
        catch (Throwable t)
        {
            fail(CombatAnimations.class, "noEquipBounce", t);
            CombatAnimations anim = (CombatAnimations)null;
            return anim != null && anim.noEquipBounce();
        }
    }

    /** Applies Viewmodel offsets for one hand. */
    public static void applyViewmodel(EnumHand hand, float sideSign)
    {
        try
        {
            ViewmodelModule viewmodel = on(ViewmodelModule.class);

            if (viewmodel != null)
            {
                viewmodel.apply(hand, sideSign);
            }
        }
        catch (Throwable t)
        {
            fail(ViewmodelModule.class, "applyViewmodel", t);
            ViewmodelModule viewmodel = (ViewmodelModule)null;

            if (viewmodel != null)
            {
                viewmodel.apply(hand, sideSign);
            }
        }
    }

    public static boolean staticFov()
    {
        try
        {
            MovementControls controls = on(MovementControls.class);
            return controls != null && controls.staticFov();
        }
        catch (Throwable t)
        {
            fail(MovementControls.class, "staticFov", t);
            MovementControls controls = (MovementControls)null;
            return controls != null && controls.staticFov();
        }
    }

    public static float bobbingAmount()
    {
        try
        {
            MovementControls controls = on(MovementControls.class);
            return controls == null ? 1.0F : controls.bobbingAmount();
        }
        catch (Throwable t)
        {
            fail(MovementControls.class, "bobbingAmount", t);
            MovementControls controls = (MovementControls)null;
            return controls == null ? 1.0F : controls.bobbingAmount();
        }
    }

    // ---- Camera Tools ---------------------------------------------------------------------------

    /** Returns the zoomed FOV, or -1 when Camera Tools is off (vanilla/OptiFine zoom then applies). */
    public static float zoomFov(float fov, boolean zoomKeyDown)
    {
        CameraTools tools = on(CameraTools.class);

        if (tools == null)
        {
            return -1.0F;
        }

        try
        {
            return tools.zoomFov(fov, zoomKeyDown);
        }
        catch (Throwable t)
        {
            fail(CameraTools.class, "zoom", t);
            return -1.0F;
        }
    }

    /** Lets Camera Tools use the mouse wheel while zooming. Returns true when consumed. */
    public static boolean consumeScroll(int wheel)
    {
        try
        {
            CameraTools tools = on(CameraTools.class);
            return tools != null && tools.onScroll(wheel);
        }
        catch (Throwable t)
        {
            fail(CameraTools.class, "consumeScroll", t);
            CameraTools tools = (CameraTools)null;
            return tools != null && tools.onScroll(wheel);
        }
    }

    /** Freelook: returns true when mouse movement went to the free camera instead of the player. */
    public static boolean freelookTurn(float yaw, float pitch)
    {
        try
        {
            CameraTools tools = on(CameraTools.class);
            return tools != null && tools.turn(yaw, pitch);
        }
        catch (Throwable t)
        {
            fail(CameraTools.class, "freelookTurn", t);
            CameraTools tools = (CameraTools)null;
            return tools != null && tools.turn(yaw, pitch);
        }
    }

    public static void preCamera(Entity entity)
    {
        try
        {
            CameraTools tools = on(CameraTools.class);

            if (tools != null)
            {
                tools.preCamera(entity);
            }
        }
        catch (Throwable t)
        {
            fail(CameraTools.class, "preCamera", t);
            CameraTools tools = (CameraTools)null;

            if (tools != null)
            {
                tools.preCamera(entity);
            }
        }
    }

    public static void postCamera(Entity entity)
    {
        try
        {
            CameraTools tools = on(CameraTools.class);

            if (tools != null)
            {
                tools.postCamera(entity);
            }
        }
        catch (Throwable t)
        {
            fail(CameraTools.class, "postCamera", t);
            CameraTools tools = (CameraTools)null;

            if (tools != null)
            {
                tools.postCamera(entity);
            }
        }
    }

    public static float thirdPersonDistance()
    {
        try
        {
            CameraTools tools = on(CameraTools.class);
            return tools == null ? 4.0F : tools.getThirdPersonDistance();
        }
        catch (Throwable t)
        {
            fail(CameraTools.class, "thirdPersonDistance", t);
            CameraTools tools = (CameraTools)null;
            return tools == null ? 4.0F : tools.getThirdPersonDistance();
        }
    }

    public static boolean skipFrontView()
    {
        try
        {
            CameraTools tools = on(CameraTools.class);
            return tools != null && tools.skipFrontView();
        }
        catch (Throwable t)
        {
            fail(CameraTools.class, "skipFrontView", t);
            CameraTools tools = (CameraTools)null;
            return tools != null && tools.skipFrontView();
        }
    }

    // ---- Inventory and items -------------------------------------------------------------------

    public static void modifyTooltip(ItemStack stack, java.util.List<String> lines)
    {
        ItemInformation info = on(ItemInformation.class);

        if (info != null)
        {
            try
            {
                info.modifyTooltip(stack, lines);
            }
            catch (Throwable t)
            {
                fail(ItemInformation.class, "tooltip", t);
            }
        }
    }

    public static int heldNameTicks(int vanilla)
    {
        try
        {
            ItemInformation info = on(ItemInformation.class);
            return info == null ? vanilla : info.getHeldNameTicks();
        }
        catch (Throwable t)
        {
            fail(ItemInformation.class, "heldNameTicks", t);
            ItemInformation info = (ItemInformation)null;
            return info == null ? vanilla : info.getHeldNameTicks();
        }
    }

    public static int itemGlint(int vanilla)
    {
        try
        {
            ItemInformation info = on(ItemInformation.class);
            return info == null ? vanilla : info.itemGlint(vanilla);
        }
        catch (Throwable t)
        {
            fail(ItemInformation.class, "itemGlint", t);
            ItemInformation info = (ItemInformation)null;
            return info == null ? vanilla : info.itemGlint(vanilla);
        }
    }

    public static float[] armorGlint()
    {
        try
        {
            ItemInformation info = on(ItemInformation.class);
            return info == null ? null : info.armorGlint();
        }
        catch (Throwable t)
        {
            fail(ItemInformation.class, "armorGlint", t);
            ItemInformation info = (ItemInformation)null;
            return info == null ? null : info.armorGlint();
        }
    }

    public static void drawContainerOverlay(net.minecraft.client.gui.inventory.GuiContainer gui, int mouseX, int mouseY)
    {
        InventoryEnhancements enh = on(InventoryEnhancements.class);

        if (enh != null)
        {
            try
            {
                net.minecraft.client.renderer.GlStateManager.disableLighting();
                net.minecraft.client.renderer.GlStateManager.disableDepth();
                enh.draw(gui, mouseX, mouseY);
            }
            catch (Throwable t)
            {
                fail(InventoryEnhancements.class, "inventory overlay", t);
            }
        }
    }

    public static boolean containerClick(net.minecraft.client.gui.inventory.GuiContainer gui, int mouseX, int mouseY, int button)
    {
        InventoryEnhancements enh = on(InventoryEnhancements.class);

        try
        {
            return enh != null && enh.click(gui, mouseX, mouseY, button);
        }
        catch (Throwable t)
        {
            fail(InventoryEnhancements.class, "inventory click", t);
            return false;
        }
    }

    public static boolean containerKey(net.minecraft.client.gui.inventory.GuiContainer gui, char typedChar, int keyCode)
    {
        try
        {
            InventoryEnhancements enh = on(InventoryEnhancements.class);
            return enh != null && enh.key(gui, typedChar, keyCode);
        }
        catch (Throwable t)
        {
            fail(InventoryEnhancements.class, "containerKey", t);
            InventoryEnhancements enh = (InventoryEnhancements)null;
            return enh != null && enh.key(gui, typedChar, keyCode);
        }
    }

    // ---- HUD and interface ---------------------------------------------------------------------

    /** Returns true when the Scoreboard module draws the sidebar instead of vanilla. */
    public static boolean captureScoreboard(ScoreObjective objective)
    {
        try
        {
            ScoreboardModule scoreboard = on(ScoreboardModule.class);
            return scoreboard != null && scoreboard.capture(objective);
        }
        catch (Throwable t)
        {
            fail(ScoreboardModule.class, "captureScoreboard", t);
            ScoreboardModule scoreboard = (ScoreboardModule)null;
            return scoreboard != null && scoreboard.capture(objective);
        }
    }

    public static void renderTabList(GuiPlayerTabOverlay overlay, int width, Scoreboard scoreboard, ScoreObjective objective)
    {
        try
        {
            TabListModule tab = on(TabListModule.class);
            float scale = tab == null ? 1.0F : tab.getScale();

            if (scale == 1.0F)
            {
                overlay.renderPlayerlist(width, scoreboard, objective);
                return;
            }

            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, 1.0F);
            overlay.renderPlayerlist((int)((float)width / scale), scoreboard, objective);
            GlStateManager.popMatrix();
        }
        catch (Throwable t)
        {
            fail(TabListModule.class, "renderTabList", t);
            TabListModule tab = (TabListModule)null;
            float scale = tab == null ? 1.0F : tab.getScale();

            if (scale == 1.0F)
            {
                overlay.renderPlayerlist(width, scoreboard, objective);
                return;
            }

            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, 1.0F);
            overlay.renderPlayerlist((int)((float)width / scale), scoreboard, objective);
            GlStateManager.popMatrix();
        }
    }

    public static int tabBackground(int vanilla)
    {
        try
        {
            TabListModule tab = on(TabListModule.class);
            return tab == null ? vanilla : tab.background(vanilla);
        }
        catch (Throwable t)
        {
            fail(TabListModule.class, "tabBackground", t);
            TabListModule tab = (TabListModule)null;
            return tab == null ? vanilla : tab.background(vanilla);
        }
    }

    public static int tabRow(int vanilla, String playerName)
    {
        try
        {
            FriendsManager friends = on(FriendsManager.class);

            if (friends != null && friends.highlightTab() && friends.isFriend(playerName))
            {
                return SythixTheme.withAlpha(friends.getColor(), 0x50);
            }

            TabListModule tab = on(TabListModule.class);
            return tab == null ? vanilla : tab.rowColor(playerName);
        }
        catch (Throwable t)
        {
            fail(FriendsManager.class, "tabRow", t);
            FriendsManager friends = (FriendsManager)null;

            if (friends != null && friends.highlightTab() && friends.isFriend(playerName))
            {
                return SythixTheme.withAlpha(friends.getColor(), 0x50);
            }

            TabListModule tab = (TabListModule)null;
            return tab == null ? vanilla : tab.rowColor(playerName);
        }
    }

    public static boolean tabHeads(boolean vanilla)
    {
        try
        {
            TabListModule tab = on(TabListModule.class);
            return tab == null ? vanilla : tab.showHeads(vanilla);
        }
        catch (Throwable t)
        {
            fail(TabListModule.class, "tabHeads", t);
            TabListModule tab = (TabListModule)null;
            return tab == null ? vanilla : tab.showHeads(vanilla);
        }
    }

    public static boolean tabHeader()
    {
        try
        {
            TabListModule tab = on(TabListModule.class);
            return tab == null || tab.showHeader();
        }
        catch (Throwable t)
        {
            fail(TabListModule.class, "tabHeader", t);
            TabListModule tab = (TabListModule)null;
            return tab == null || tab.showHeader();
        }
    }

    public static boolean tabFooter()
    {
        try
        {
            TabListModule tab = on(TabListModule.class);
            return tab == null || tab.showFooter();
        }
        catch (Throwable t)
        {
            fail(TabListModule.class, "tabFooter", t);
            TabListModule tab = (TabListModule)null;
            return tab == null || tab.showFooter();
        }
    }

    /** Draws ping numbers; returns true when the vanilla ping bars should be skipped. */
    public static boolean tabPing(int width, int x, int y, NetworkPlayerInfo info)
    {
        try
        {
            TabListModule tab = on(TabListModule.class);

            if (tab == null)
            {
                return false;
            }

            if (tab.showNumbers())
            {
                tab.drawPingNumber(width, x, y, info, tab.showBars());
            }

            return !tab.showBars();
        }
        catch (Throwable t)
        {
            fail(TabListModule.class, "tabPing", t);
            TabListModule tab = (TabListModule)null;

            if (tab == null)
            {
                return false;
            }

            if (tab.showNumbers())
            {
                tab.drawPingNumber(width, x, y, info, tab.showBars());
            }

            return !tab.showBars();
        }
    }

    /** Filters, stacks and timestamps an incoming chat line. Returns null to hide it. */
    public static ITextComponent incomingChat(GuiNewChat chat, ITextComponent message, int[] chatLineId)
    {
        ChatModule module = on(ChatModule.class);

        if (module == null)
        {
            return message;
        }

        try
        {
            return module.incoming(chat, message, chatLineId);
        }
        catch (Throwable t)
        {
            fail(ChatModule.class, "chat", t);
            return message;
        }
    }

    public static void tagChatLines(ITextComponent message, java.util.List<ITextComponent> lines)
    {
        try
        {
            ChatModule module = on(ChatModule.class);

            if (module != null)
            {
                ChatUtilities utilities = on(ChatUtilities.class);
                module.tagLines(message, lines, utilities == null ? 0 : utilities.highlight(message));
            }
        }
        catch (Throwable t)
        {
            fail(ChatModule.class, "tagChatLines", t);
            ChatModule module = (ChatModule)null;

            if (module != null)
            {
                ChatUtilities utilities = (ChatUtilities)null;
                module.tagLines(message, lines, utilities == null ? 0 : utilities.highlight(message));
            }
        }
    }

    public static boolean chatTabAllows(ITextComponent message)
    {
        try
        {
            ChatModule module = on(ChatModule.class);
            return module == null || module.tabAllows(message);
        }
        catch (Throwable t)
        {
            fail(ChatModule.class, "chatTabAllows", t);
            ChatModule module = (ChatModule)null;
            return module == null || module.tabAllows(message);
        }
    }

    public static int chatHistory(int vanilla)
    {
        try
        {
            ChatModule module = on(ChatModule.class);
            return module == null ? vanilla : module.getHistory();
        }
        catch (Throwable t)
        {
            fail(ChatModule.class, "chatHistory", t);
            ChatModule module = (ChatModule)null;
            return module == null ? vanilla : module.getHistory();
        }
    }

    public static int chatBackground(int alpha, ITextComponent line)
    {
        try
        {
            ChatModule module = on(ChatModule.class);
            return module == null ? alpha << 24 : module.background(alpha, line);
        }
        catch (Throwable t)
        {
            fail(ChatModule.class, "chatBackground", t);
            ChatModule module = (ChatModule)null;
            return module == null ? alpha << 24 : module.background(alpha, line);
        }
    }

    public static boolean chatShadow()
    {
        try
        {
            ChatModule module = on(ChatModule.class);
            return module == null || module.textShadow();
        }
        catch (Throwable t)
        {
            fail(ChatModule.class, "chatShadow", t);
            ChatModule module = (ChatModule)null;
            return module == null || module.textShadow();
        }
    }

    public static void onChatSent()
    {
        try
        {
            ChatUtilities utilities = on(ChatUtilities.class);

            if (utilities != null)
            {
                utilities.onSent();
            }
        }
        catch (Throwable t)
        {
            fail(ChatUtilities.class, "onChatSent", t);
            ChatUtilities utilities = (ChatUtilities)null;

            if (utilities != null)
            {
                utilities.onSent();
            }
        }
    }

    public static String outgoingChat(String message)
    {
        try
        {
            ChatUtilities utilities = on(ChatUtilities.class);
            return utilities == null || message.startsWith("/") ? message : utilities.applyShortcuts(message);
        }
        catch (Throwable t)
        {
            fail(ChatUtilities.class, "outgoingChat", t);
            ChatUtilities utilities = (ChatUtilities)null;
            return utilities == null || message.startsWith("/") ? message : utilities.applyShortcuts(message);
        }
    }

    private static final int TAB_W = 48;

    /** Draws chat tabs above the chat input while chat is open. */
    public static void drawChatTabs(int width, int height, int mouseX, int mouseY)
    {
        try
        {
            ChatModule module = on(ChatModule.class);

            if (module == null || !module.tabsEnabled())
            {
                return;
            }

            Minecraft mc = Minecraft.getMinecraft();
            int y = height - 26;

            for (int i = 0; i < ChatModule.TABS.length; ++i)
            {
                int x = 2 + i * (TAB_W + 2);
                boolean active = ChatModule.TABS[i].equals(ChatModule.getActiveTab());
                boolean hover = mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < y + 11;
                net.sythix.client.ui.SythixTheme.drawRect((float)x, (float)y, (float)(x + TAB_W), (float)(y + 11), active ? 0xC0D81820 : (hover ? 0x90303038 : 0x90000000));
                String label = ChatModule.TABS[i];
                mc.fontRenderer.drawString(label, (float)(x + TAB_W / 2 - mc.fontRenderer.getStringWidth(label) / 2), (float)(y + 2), active ? 0xFFFFFFFF : 0xFFB0B0B8, false);
            }
        }
        catch (Throwable t)
        {
            fail(ChatModule.class, "drawChatTabs", t);
            ChatModule module = (ChatModule)null;

            if (module == null || !module.tabsEnabled())
            {
                return;
            }

            Minecraft mc = Minecraft.getMinecraft();
            int y = height - 26;

            for (int i = 0; i < ChatModule.TABS.length; ++i)
            {
                int x = 2 + i * (TAB_W + 2);
                boolean active = ChatModule.TABS[i].equals(ChatModule.getActiveTab());
                boolean hover = mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < y + 11;
                net.sythix.client.ui.SythixTheme.drawRect((float)x, (float)y, (float)(x + TAB_W), (float)(y + 11), active ? 0xC0D81820 : (hover ? 0x90303038 : 0x90000000));
                String label = ChatModule.TABS[i];
                mc.fontRenderer.drawString(label, (float)(x + TAB_W / 2 - mc.fontRenderer.getStringWidth(label) / 2), (float)(y + 2), active ? 0xFFFFFFFF : 0xFFB0B0B8, false);
            }
        }
    }

    /** Handles chat tab clicks and Ctrl+click copy. Returns true when the click was used. */
    public static boolean chatScreenClick(int width, int height, int mouseX, int mouseY, int button)
    {
        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            ChatModule module = on(ChatModule.class);

            if (module != null && module.tabsEnabled() && button == 0 && mouseY >= height - 26 && mouseY < height - 15)
            {
                int index = (mouseX - 2) / (TAB_W + 2);

                if (mouseX >= 2 && index >= 0 && index < ChatModule.TABS.length)
                {
                    ChatModule.setActiveTab(ChatModule.TABS[index]);
                    mc.ingameGUI.getChatGUI().refreshChat();
                    return true;
                }
            }

            ChatUtilities utilities = on(ChatUtilities.class);

            if (utilities != null && utilities.copyEnabled() && button == 0 && net.minecraft.client.gui.GuiScreen.isCtrlKeyDown())
            {
                ChatLine line = mc.ingameGUI.getChatGUI().getLineAtSythix(org.lwjgl.input.Mouse.getX(), org.lwjgl.input.Mouse.getY());

                if (line != null)
                {
                    ITextComponent message = module == null ? null : module.messageForLine(line.getChatComponent());
                    utilities.copy(message != null ? message : line.getChatComponent());
                    return true;
                }
            }

            return false;
        }
        catch (Throwable t)
        {
            fail(ChatModule.class, "chatScreenClick", t);
            Minecraft mc = Minecraft.getMinecraft();
            ChatModule module = (ChatModule)null;

            if (module != null && module.tabsEnabled() && button == 0 && mouseY >= height - 26 && mouseY < height - 15)
            {
                int index = (mouseX - 2) / (TAB_W + 2);

                if (mouseX >= 2 && index >= 0 && index < ChatModule.TABS.length)
                {
                    ChatModule.setActiveTab(ChatModule.TABS[index]);
                    mc.ingameGUI.getChatGUI().refreshChat();
                    return true;
                }
            }

            ChatUtilities utilities = (ChatUtilities)null;

            if (utilities != null && utilities.copyEnabled() && button == 0 && net.minecraft.client.gui.GuiScreen.isCtrlKeyDown())
            {
                ChatLine line = mc.ingameGUI.getChatGUI().getLineAtSythix(org.lwjgl.input.Mouse.getX(), org.lwjgl.input.Mouse.getY());

                if (line != null)
                {
                    ITextComponent message = module == null ? null : module.messageForLine(line.getChatComponent());
                    utilities.copy(message != null ? message : line.getChatComponent());
                    return true;
                }
            }

            return false;
        }
    }

    /** Draws toast notifications above everything. */
    public static void renderNotifications(ScaledResolution res)
    {
        NotificationsModule module = on(NotificationsModule.class);

        if (module != null)
        {
            try
            {
                GlStateManager.disableDepth();
                module.render(res);
                GlStateManager.enableDepth();
            }
            catch (Throwable t)
            {
                fail(NotificationsModule.class, "notifications", t);
            }
        }
    }

    /** The render distance written to options.txt (adaptive mode never saves its own value). */
    public static int savedRenderDistance(int current)
    {
        try
        {
            PerformanceOptimizer optimizer = on(PerformanceOptimizer.class);
            return optimizer == null ? current : optimizer.savedRenderDistance(current);
        }
        catch (Throwable t)
        {
            return current;
        }
    }

    /** Once per rendered frame, after everything (including notifications) is drawn. */
    public static void onFrame()
    {
        try
        {
            net.sythix.client.module.performance.Benchmark.onFrame();
        }
        catch (Throwable t)
        {
            fail(PerformanceOptimizer.class, "benchmark", t);
        }
    }

    // ---- Visuals and cosmetics -----------------------------------------------------------------

    private static final ResourceLocation ALEX = new ResourceLocation("textures/entity/alex.png");

    /** Your own skin from the Profile screen, or null to use the normal one. */
    public static ResourceLocation localSkin(AbstractClientPlayer player)
    {
        if (!(player instanceof EntityPlayerSP) || SythixClient.get() == null)
        {
            return null;
        }

        try
        {
            ResourceLocation custom = CosmeticTextures.skin(ProfileStore.skin);

            if (custom != null)
            {
                return custom;
            }

            return ProfileStore.slim ? ALEX : null;
        }
        catch (Throwable t)
        {
            fail(CosmeticsModule.class, "skin", t);
            return null;
        }
    }

    public static String localSkinType(AbstractClientPlayer player)
    {
        if (!(player instanceof EntityPlayerSP) || SythixClient.get() == null)
        {
            return null;
        }

        return ProfileStore.slim ? "slim" : (ProfileStore.skin.isEmpty() ? null : "default");
    }

    public static ResourceLocation localCape(AbstractClientPlayer player)
    {
        CosmeticsModule cosmetics = on(CosmeticsModule.class);

        if (cosmetics == null || !cosmetics.showCape() || !(player instanceof EntityPlayerSP))
        {
            return null;
        }

        try
        {
            return CosmeticTextures.cape(ProfileStore.cape);
        }
        catch (Throwable t)
        {
            fail(CosmeticsModule.class, "cape", t);
            return null;
        }
    }

    public static void applyEmote(ModelBiped model, Entity entity, float ageInTicks)
    {
        try
        {
            CosmeticsModule cosmetics = on(CosmeticsModule.class);

            if (cosmetics != null && entity != null)
            {
                cosmetics.applyEmote(model, entity, ageInTicks);
            }
        }
        catch (Throwable t)
        {
            fail(CosmeticsModule.class, "applyEmote", t);
            CosmeticsModule cosmetics = (CosmeticsModule)null;

            if (cosmetics != null && entity != null)
            {
                cosmetics.applyEmote(model, entity, ageInTicks);
            }
        }
    }

    public static boolean allowSound(ISound sound)
    {
        SoundController controller = on(SoundController.class);

        try
        {
            return controller == null || sound == null || controller.allow(sound);
        }
        catch (Throwable t)
        {
            return true;
        }
    }

    public static long worldTime()
    {
        try
        {
            EnvironmentVisuals env = on(EnvironmentVisuals.class);
            return env == null ? -1L : env.worldTime();
        }
        catch (Throwable t)
        {
            fail(EnvironmentVisuals.class, "worldTime", t);
            EnvironmentVisuals env = (EnvironmentVisuals)null;
            return env == null ? -1L : env.worldTime();
        }
    }

    public static float rainStrength()
    {
        try
        {
            EnvironmentVisuals env = on(EnvironmentVisuals.class);
            return env == null ? -1.0F : env.rain();
        }
        catch (Throwable t)
        {
            fail(EnvironmentVisuals.class, "rainStrength", t);
            EnvironmentVisuals env = (EnvironmentVisuals)null;
            return env == null ? -1.0F : env.rain();
        }
    }

    public static float thunderStrength()
    {
        try
        {
            EnvironmentVisuals env = on(EnvironmentVisuals.class);
            return env == null ? -1.0F : env.thunder();
        }
        catch (Throwable t)
        {
            fail(EnvironmentVisuals.class, "thunderStrength", t);
            EnvironmentVisuals env = (EnvironmentVisuals)null;
            return env == null ? -1.0F : env.thunder();
        }
    }

    public static float gamma(float vanilla)
    {
        try
        {
            EnvironmentVisuals env = on(EnvironmentVisuals.class);
            float g = env == null ? -1.0F : env.gamma();
            return g >= 0.0F ? g : vanilla;
        }
        catch (Throwable t)
        {
            fail(EnvironmentVisuals.class, "gamma", t);
            EnvironmentVisuals env = (EnvironmentVisuals)null;
            float g = env == null ? -1.0F : env.gamma();
            return g >= 0.0F ? g : vanilla;
        }
    }

    public static float outlineWidth(float vanilla)
    {
        try
        {
            BlockVisuals visuals = on(BlockVisuals.class);
            return visuals == null ? vanilla : visuals.getWidth();
        }
        catch (Throwable t)
        {
            fail(BlockVisuals.class, "outlineWidth", t);
            BlockVisuals visuals = (BlockVisuals)null;
            return visuals == null ? vanilla : visuals.getWidth();
        }
    }

    public static float[] outlineColor()
    {
        try
        {
            BlockVisuals visuals = on(BlockVisuals.class);
            return visuals == null ? null : visuals.outline();
        }
        catch (Throwable t)
        {
            fail(BlockVisuals.class, "outlineColor", t);
            BlockVisuals visuals = (BlockVisuals)null;
            return visuals == null ? null : visuals.outline();
        }
    }

    public static float[] outlineFill()
    {
        try
        {
            BlockVisuals visuals = on(BlockVisuals.class);
            return visuals == null ? null : visuals.fillColor();
        }
        catch (Throwable t)
        {
            fail(BlockVisuals.class, "outlineFill", t);
            BlockVisuals visuals = (BlockVisuals)null;
            return visuals == null ? null : visuals.fillColor();
        }
    }

    // ---- Multiplayer ----------------------------------------------------------------------------

    public static String serverName(ServerData data)
    {
        try
        {
            ServerManagerModule manager = on(ServerManagerModule.class);
            return manager == null ? data.serverName : manager.decorateName(data);
        }
        catch (Throwable t)
        {
            fail(ServerManagerModule.class, "serverName", t);
            ServerManagerModule manager = (ServerManagerModule)null;
            return manager == null ? data.serverName : manager.decorateName(data);
        }
    }

    public static String nametag(Entity entity, String text)
    {
        try
        {
            FriendsManager friends = on(FriendsManager.class);

            if (friends != null && friends.highlightNames() && entity instanceof EntityPlayer && friends.isFriend(entity.getName()))
            {
                return "\u00a7b\u2605 \u00a7r" + text;
            }

            return text;
        }
        catch (Throwable t)
        {
            fail(FriendsManager.class, "nametag", t);
            FriendsManager friends = (FriendsManager)null;

            if (friends != null && friends.highlightNames() && entity instanceof EntityPlayer && friends.isFriend(entity.getName()))
            {
                return "\u00a7b\u2605 \u00a7r" + text;
            }

            return text;
        }
    }

    public static void onPacket()
    {
        try
        {
            ConnectionMonitor monitor = on(ConnectionMonitor.class);

            if (monitor != null)
            {
                monitor.onPacket();
            }
        }
        catch (Throwable t)
        {
            fail(ConnectionMonitor.class, "onPacket", t);
            ConnectionMonitor monitor = (ConnectionMonitor)null;

            if (monitor != null)
            {
                monitor.onPacket();
            }
        }
    }

    /** Streamer mode: replaces your name in text drawn on screen. */
    public static String filterText(String text)
    {
        return SythixClient.get() == null ? text : PrivacyControls.filterText(text);
    }

    // ---- Recording ---------------------------------------------------------------------------

    /** A custom screenshot file, or null for the vanilla name. */
    public static java.io.File screenshotFile(java.io.File dir)
    {
        ScreenshotManager manager = on(ScreenshotManager.class);

        try
        {
            return manager == null ? null : manager.nextFile(dir);
        }
        catch (Throwable t)
        {
            fail(ScreenshotManager.class, "screenshot name", t);
            return null;
        }
    }

    public static void onScreenshotSaved(java.io.File file)
    {
        ScreenshotManager manager = on(ScreenshotManager.class);

        try
        {
            if (manager != null)
            {
                manager.onSaved(file);
            }
        }
        catch (Throwable t)
        {
            fail(ScreenshotManager.class, "screenshot saved", t);
        }
    }

    public static boolean hideChat()
    {
        try
        {
            RecordingTools recording = on(RecordingTools.class);
            return recording != null && recording.hidesChat();
        }
        catch (Throwable t)
        {
            fail(RecordingTools.class, "hideChat", t);
            RecordingTools recording = (RecordingTools)null;
            return recording != null && recording.hidesChat();
        }
    }
}
