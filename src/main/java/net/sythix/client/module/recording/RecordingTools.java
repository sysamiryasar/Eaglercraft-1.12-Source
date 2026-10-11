package net.sythix.client.module.recording;

import net.minecraft.client.Minecraft;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.StyledHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #47 Recording mode: the built-in video recorder (MJPEG .avi in the recordings folder, no
 * sound), or a clean view for an external recorder like OBS, with a REC indicator, hidden HUD or
 * chat, and a steady frame rate.
 */
public class RecordingTools extends StyledHudModule
{
    private final KeybindSetting toggleKey;
    private final BooleanSetting hideHud;
    private final BooleanSetting hideChat;
    private final NumberSetting fpsLock;
    private final BooleanSetting showIndicator;
    private final BooleanSetting builtIn;
    private final NumberSetting videoFps;
    private final ModeSetting videoSize;
    private final NumberSetting videoQuality;
    private boolean recording;
    private long started;

    public RecordingTools()
    {
        super("Recording Tools", "Record video with the built-in recorder, or prepare a clean view for OBS.", Category.RECORDING, false, 0.995D, 0.01D);
        this.builtIn = this.add(new BooleanSetting("Built-in Recorder", "Save a video file while recording mode is on (video only - use OBS if you also need sound).", true));
        this.videoFps = this.add(new NumberSetting("Video FPS", "Frames per second in the video.", 30.0D, 10.0D, 60.0D, 5.0D, " fps")).visibleWhen(this.builtIn::isOn);
        this.videoSize = this.add(new ModeSetting("Video Size", "Full window size, or half size (smaller files, less lag).", "Full", "Full", "Half")).visibleWhen(this.builtIn::isOn);
        this.videoQuality = this.add(new NumberSetting("Video Quality", "Higher = sharper but bigger files.", 80.0D, 30.0D, 100.0D, 5.0D, "%")).visibleWhen(this.builtIn::isOn);
        this.add(new ButtonSetting("Recordings Folder", "Open the folder with your videos.", "Open", () -> {
            java.io.File dir = new java.io.File(this.mc.mcDataDir, "recordings");
            dir.mkdirs();
            net.minecraft.client.renderer.OpenGlHelper.openFile(dir);
        }));
        this.toggleKey = this.add(new KeybindSetting("Recording Mode Key", "Start or stop recording mode.", 0));
        this.hideHud = this.add(new BooleanSetting("Hide HUD", "Hide Sythix HUD elements while recording.", true));
        this.hideChat = this.add(new BooleanSetting("Hide Chat", "Hide chat while recording.", false));
        this.fpsLock = this.add(new NumberSetting("FPS Lock", "Frame rate while recording (match your recorder). 0 = no lock.", 60.0D, 0.0D, 240.0D, 10.0D, " fps"));
        this.showIndicator = this.add(new BooleanSetting("REC Indicator", "Show a REC dot and timer on screen. It appears in videos too; the window title also shows REC.", false));
        this.add(new net.sythix.client.setting.ButtonSetting("Recording Mode", "Start or stop recording mode now.", "Toggle", this::toggleRecording));
    }

    public void onKey(int keyCode)
    {
        if (keyCode != 0 && keyCode == this.toggleKey.getKey())
        {
            this.toggleRecording();
        }
    }

    private void toggleRecording()
    {
        this.recording = !this.recording;
        this.started = Minecraft.getSystemTime();

        if (this.builtIn.isOn())
        {
            if (this.recording)
            {
                VideoRecorder.start(this.videoFps.getInt(), this.videoSize.is("Half") ? 0.5F : 1.0F, this.videoQuality.getFloat() / 100.0F);
            }
            else
            {
                VideoRecorder.stop();
            }
        }
        else
        {
            Notifications.post("Recording mode", this.recording ? "On - start your recorder now." : "Off.", Notifications.Kind.INFO);
        }

        // The window title shows recording, so the REC dot can stay out of the video
        org.lwjgl.opengl.Display.setTitle(this.recording ? "\u25CF REC - " + net.sythix.client.SythixClient.getWindowTitle() : net.sythix.client.SythixClient.getWindowTitle());
    }

    protected void onDisable()
    {
        if (this.recording)
        {
            this.toggleRecording();
        }
    }

    public void onTick()
    {
        // The recorder stopped by itself (error): leave recording mode too
        if (this.recording && this.builtIn.isOn() && !VideoRecorder.isRecording() && Minecraft.getSystemTime() - this.started > 1000L)
        {
            this.recording = false;
            org.lwjgl.opengl.Display.setTitle(net.sythix.client.SythixClient.getWindowTitle());
        }
    }

    public boolean isRecording()
    {
        return this.recording;
    }

    public boolean hidesHud()
    {
        return this.recording && this.hideHud.isOn();
    }

    public boolean hidesChat()
    {
        return this.recording && this.hideChat.isOn();
    }

    public int fpsLock()
    {
        return this.recording ? this.fpsLock.getInt() : 0;
    }

    public boolean shouldRender()
    {
        return this.recording && this.showIndicator.isOn();
    }

    public int getWidth()
    {
        return 50 + this.getPad() * 2;
    }

    public int getHeight()
    {
        return 8 + this.getPad() * 2;
    }

    protected void renderContent(boolean preview)
    {
        this.drawBox(this.getWidth(), this.getHeight());
        long ms = this.recording ? Minecraft.getSystemTime() - this.started : 0L;
        boolean dot = (Minecraft.getSystemTime() / 500L) % 2L == 0L || preview;

        if (dot)
        {
            net.sythix.client.ui.SythixTheme.drawRect((float)this.getPad(), (float)(this.getPad() + 1), (float)(this.getPad() + 6), (float)(this.getPad() + 7), 0xFFFF2020);
        }

        this.drawText(String.format("REC %d:%02d", ms / 60000L, ms / 1000L % 60L), (float)(this.getPad() + 9), (float)this.getPad(), this.valueColor.getColor());
    }
}
