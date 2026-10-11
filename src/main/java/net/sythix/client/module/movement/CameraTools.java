package net.sythix.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.src.Config;
import net.minecraft.util.math.MathHelper;
import net.sythix.client.input.InputUtil;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #17 Zoom, freelook and perspective controls. Freelook only moves your camera, never where your
 * player looks, but some servers still forbid it - it is off until you turn it on.
 */
public class CameraTools extends Module
{
    private final NumberSetting zoomLevel;
    private final BooleanSetting scrollZoom;
    private final BooleanSetting smoothZoom;
    private final BooleanSetting cinematic;
    private final BooleanSetting freelookEnabled;
    private final KeybindSetting freelookKey;
    private final ModeSetting freelookMode;
    private final BooleanSetting invertFreelook;
    private final BooleanSetting skipFrontView;
    private final NumberSetting thirdPersonDistance;

    private float zoomAmount = 1.0F;
    private float scrollLevel;
    private long lastFrame;
    private boolean zoomedLastFrame;
    private boolean setSmoothCamera;

    private boolean freelooking;
    private boolean freelookToggled;
    private float cameraYaw;
    private float cameraPitch;
    private int previousPerspective;
    private float savedYaw;
    private float savedPitch;
    private float savedPrevYaw;
    private float savedPrevPitch;

    public CameraTools()
    {
        super("Camera Tools", "Zoom, freelook and perspective controls.", Category.MOVEMENT, true);
        this.zoomLevel = this.add(new NumberSetting("Zoom Level", "How far the zoom key zooms in (zoom key is set in Controls).", 4.0D, 2.0D, 10.0D, 0.5D, "x"));
        this.scrollZoom = this.add(new BooleanSetting("Scroll To Zoom", "Use the mouse wheel to zoom further while zooming.", true));
        this.smoothZoom = this.add(new BooleanSetting("Smooth Zoom", "Animate zooming in and out.", true));
        this.cinematic = this.add(new BooleanSetting("Cinematic Camera", "Smooth mouse movement while zoomed (like OptiFine).", true));
        this.freelookEnabled = this.add(new BooleanSetting("Freelook", "Hold a key to look around your player without turning. Check the server rules first.", false));
        this.freelookKey = this.add(new KeybindSetting("Freelook Key", "Key for freelook.", 56)).visibleWhen(this.freelookEnabled::isOn);
        this.freelookMode = this.add(new ModeSetting("Freelook Mode", "Hold the key, or press it to switch.", "Hold", "Hold", "Toggle")).visibleWhen(this.freelookEnabled::isOn);
        this.invertFreelook = this.add(new BooleanSetting("Invert Freelook Y", "Invert up/down while freelooking.", false)).visibleWhen(this.freelookEnabled::isOn);
        this.skipFrontView = this.add(new BooleanSetting("Skip Front View", "F5 only switches between first person and back view.", false));
        this.thirdPersonDistance = this.add(new NumberSetting("Third Person Distance", "How far the camera is behind you in third person.", 4.0D, 1.0D, 12.0D, 0.5D, " blocks"));
    }

    // ---- Zoom ------------------------------------------------------------------------------

    /** Returns the zoomed FOV for the given base FOV. zoomKeyDown is whether the zoom key is held. */
    public float zoomFov(float fov, boolean zoomKeyDown)
    {
        Minecraft mc = this.mc;
        long now = Minecraft.getSystemTime();
        float seconds = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1000.0F);
        this.lastFrame = now;

        if (zoomKeyDown && !this.zoomedLastFrame)
        {
            this.scrollLevel = 0.0F;
            Config.zoomMode = true;
            mc.renderGlobal.displayListEntitiesDirty = true;

            if (this.cinematic.isOn() && !mc.gameSettings.smoothCamera)
            {
                mc.gameSettings.smoothCamera = true;
                this.setSmoothCamera = true;
            }
        }
        else if (!zoomKeyDown && this.zoomedLastFrame)
        {
            Config.zoomMode = false;
            mc.renderGlobal.displayListEntitiesDirty = true;

            if (this.setSmoothCamera)
            {
                mc.gameSettings.smoothCamera = false;
                this.setSmoothCamera = false;
            }
        }

        this.zoomedLastFrame = zoomKeyDown;
        float target = zoomKeyDown ? Math.max(1.0F, this.zoomLevel.getFloat() * (float)Math.pow(1.25D, (double)this.scrollLevel)) : 1.0F;

        if (this.smoothZoom.isOn() && !net.sythix.client.ui.SythixTheme.reducedMotion)
        {
            this.zoomAmount += (target - this.zoomAmount) * Math.min(1.0F, seconds * 12.0F);
        }
        else
        {
            this.zoomAmount = target;
        }

        return fov / Math.max(1.0F, this.zoomAmount);
    }

    /** Uses the mouse wheel for zoom while zooming. Returns true when the scroll was used. */
    public boolean onScroll(int wheel)
    {
        if (!this.zoomedLastFrame || !this.scrollZoom.isOn())
        {
            return false;
        }

        this.scrollLevel = MathHelper.clamp(this.scrollLevel + (wheel > 0 ? 1.0F : -1.0F), -3.0F, 8.0F);
        return true;
    }

    // ---- Perspective ------------------------------------------------------------------------

    public float getThirdPersonDistance()
    {
        return this.thirdPersonDistance.getFloat();
    }

    public boolean skipFrontView()
    {
        return this.skipFrontView.isOn();
    }

    // ---- Freelook ---------------------------------------------------------------------------

    public void onKey(int keyCode)
    {
        if (this.freelookEnabled.isOn() && keyCode == this.freelookKey.getKey() && this.freelookMode.is("Toggle"))
        {
            this.freelookToggled = !this.freelookToggled;
        }
    }

    public void onTick()
    {
        boolean want = this.freelookEnabled.isOn() && this.mc.currentScreen == null && this.mc.player != null && (this.freelookMode.is("Toggle") ? this.freelookToggled : InputUtil.isKeyDown(this.freelookKey.getKey()));

        if (want && !this.freelooking)
        {
            this.freelooking = true;
            this.cameraYaw = this.mc.player.rotationYaw;
            this.cameraPitch = this.mc.player.rotationPitch;
            this.previousPerspective = this.mc.gameSettings.thirdPersonView;

            if (this.previousPerspective == 0)
            {
                this.mc.gameSettings.thirdPersonView = 1;
            }
        }
        else if (!want && this.freelooking)
        {
            this.stopFreelook();
        }
    }

    private void stopFreelook()
    {
        this.freelooking = false;
        this.freelookToggled = false;
        this.mc.gameSettings.thirdPersonView = this.previousPerspective;
    }

    protected void onDisable()
    {
        if (this.freelooking)
        {
            this.stopFreelook();
        }
    }

    /** Takes mouse movement for the freelook camera. Returns true when the player should not turn. */
    public boolean turn(float yaw, float pitch)
    {
        if (!this.freelooking)
        {
            return false;
        }

        this.cameraYaw += yaw * 0.15F;
        this.cameraPitch = MathHelper.clamp(this.cameraPitch + (this.invertFreelook.isOn() ? pitch : -pitch) * 0.15F, -90.0F, 90.0F);
        return true;
    }

    /** Before the camera is positioned: temporarily point the entity where the camera looks. */
    public void preCamera(Entity entity)
    {
        if (!this.freelooking || entity != this.mc.player)
        {
            return;
        }

        this.savedYaw = entity.rotationYaw;
        this.savedPitch = entity.rotationPitch;
        this.savedPrevYaw = entity.prevRotationYaw;
        this.savedPrevPitch = entity.prevRotationPitch;
        entity.rotationYaw = this.cameraYaw;
        entity.prevRotationYaw = this.cameraYaw;
        entity.rotationPitch = this.cameraPitch;
        entity.prevRotationPitch = this.cameraPitch;
    }

    /** After the camera is positioned: put the entity's real rotation back. */
    public void postCamera(Entity entity)
    {
        if (!this.freelooking || entity != this.mc.player)
        {
            return;
        }

        entity.rotationYaw = this.savedYaw;
        entity.rotationPitch = this.savedPitch;
        entity.prevRotationYaw = this.savedPrevYaw;
        entity.prevRotationPitch = this.savedPrevPitch;
    }
}
