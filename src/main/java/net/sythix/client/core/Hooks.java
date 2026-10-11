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
import net.sythix.client.ui.GuiModMenu;

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

        for (HudModule module : m.getHudModules())
        {
            if (module.isEnabled())
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
        CrosshairModule crosshair = on(CrosshairModule.class);
        return crosshair != null && crosshair.showInThirdPerson();
    }

    public static boolean hideVanillaPotionIcons()
    {
        PotionHud hud = on(PotionHud.class);
        return hud != null && hud.hideVanillaOverlay();
    }

    // ---- Performance --------------------------------------------------------------------------

    public static int limitFramerate(int vanilla)
    {
        PerformanceOptimizer optimizer = on(PerformanceOptimizer.class);

        if (optimizer == null)
        {
            return vanilla;
        }

        Minecraft mc = Minecraft.getMinecraft();
        int limit = optimizer.getFramerateLimit(org.lwjgl.opengl.Display.isActive(), mc.world != null && mc.currentScreen != null);
        return limit > 0 ? Math.min(vanilla, limit) : vanilla;
    }

    public static boolean shouldRenderEntity(Entity entity)
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

    public static boolean allowParticle(Particle particle)
    {
        ParticleController controller = on(ParticleController.class);
        return controller == null || controller.allow(particle);
    }

    public static boolean itemSpin()
    {
        AnimationController controller = on(AnimationController.class);
        return controller == null || controller.itemSpin();
    }

    public static boolean itemBob()
    {
        AnimationController controller = on(AnimationController.class);
        return controller == null || controller.itemBob();
    }

    // ---- First person and camera --------------------------------------------------------------

    /** Arm swing duration for the local player (vanilla value passed in). */
    public static int swingDuration(Entity entity, int vanilla)
    {
        CombatAnimations anim = on(CombatAnimations.class);

        if (anim == null || entity != Minecraft.getMinecraft().player)
        {
            return vanilla;
        }

        return Math.max(1, Math.round((float)vanilla / anim.getSwingSpeed()));
    }

    public static boolean classicBlocking(ItemStack mainHand)
    {
        CombatAnimations anim = on(CombatAnimations.class);
        return anim != null && anim.isClassicBlocking(mainHand);
    }

    public static boolean swingWhileUsing()
    {
        CombatAnimations anim = on(CombatAnimations.class);
        return anim != null && anim.swingWhileUsing();
    }

    public static boolean noEquipBounce()
    {
        CombatAnimations anim = on(CombatAnimations.class);
        return anim != null && anim.noEquipBounce();
    }

    /** Applies Viewmodel offsets for one hand. */
    public static void applyViewmodel(EnumHand hand, float sideSign)
    {
        ViewmodelModule viewmodel = on(ViewmodelModule.class);

        if (viewmodel != null)
        {
            viewmodel.apply(hand, sideSign);
        }
    }

    public static boolean staticFov()
    {
        MovementControls controls = on(MovementControls.class);
        return controls != null && controls.staticFov();
    }

    public static float bobbingAmount()
    {
        MovementControls controls = on(MovementControls.class);
        return controls == null ? 1.0F : controls.bobbingAmount();
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
        CameraTools tools = on(CameraTools.class);
        return tools != null && tools.onScroll(wheel);
    }

    /** Freelook: returns true when mouse movement went to the free camera instead of the player. */
    public static boolean freelookTurn(float yaw, float pitch)
    {
        CameraTools tools = on(CameraTools.class);
        return tools != null && tools.turn(yaw, pitch);
    }

    public static void preCamera(Entity entity)
    {
        CameraTools tools = on(CameraTools.class);

        if (tools != null)
        {
            tools.preCamera(entity);
        }
    }

    public static void postCamera(Entity entity)
    {
        CameraTools tools = on(CameraTools.class);

        if (tools != null)
        {
            tools.postCamera(entity);
        }
    }

    public static float thirdPersonDistance()
    {
        CameraTools tools = on(CameraTools.class);
        return tools == null ? 4.0F : tools.getThirdPersonDistance();
    }

    public static boolean skipFrontView()
    {
        CameraTools tools = on(CameraTools.class);
        return tools != null && tools.skipFrontView();
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
        ItemInformation info = on(ItemInformation.class);
        return info == null ? vanilla : info.getHeldNameTicks();
    }

    public static int itemGlint(int vanilla)
    {
        ItemInformation info = on(ItemInformation.class);
        return info == null ? vanilla : info.itemGlint(vanilla);
    }

    public static float[] armorGlint()
    {
        ItemInformation info = on(ItemInformation.class);
        return info == null ? null : info.armorGlint();
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
        InventoryEnhancements enh = on(InventoryEnhancements.class);
        return enh != null && enh.key(gui, typedChar, keyCode);
    }
}
