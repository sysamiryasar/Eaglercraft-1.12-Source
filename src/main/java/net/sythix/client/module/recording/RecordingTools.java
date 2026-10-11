package net.sythix.client.module.recording;

import net.minecraft.client.Minecraft;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.StyledHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #47 Gets the game ready for recording with your own recorder (OBS, ShadowPlay...): a REC
 * indicator, a clean view without HUD/chat, and a steady frame rate. Sythix does not record video.
 */
public class RecordingTools extends StyledHudModule
{
    private final KeybindSetting toggleKey;
    private final BooleanSetting hideHud;
    private final BooleanSetting hideChat;
    private final NumberSetting fpsLock;
    private final BooleanSetting showIndicator;
    private boolean recording;
    private long started;

    public RecordingTools()
    {
        super("Recording Tools", "Recording mode: REC indicator, clean view and steady FPS for your own recorder.", Category.RECORDING, false, 0.995D, 0.01D);
        this.add(new InfoSetting("Note", "What this module does.", () -> "Works with OBS etc. - Sythix itself does not record"));
        this.toggleKey = this.add(new KeybindSetting("Recording Mode Key", "Start or stop recording mode.", 0));
        this.hideHud = this.add(new BooleanSetting("Hide HUD", "Hide Sythix HUD elements while recording.", true));
        this.hideChat = this.add(new BooleanSetting("Hide Chat", "Hide chat while recording.", false));
        this.fpsLock = this.add(new NumberSetting("FPS Lock", "Frame rate while recording (match your recorder). 0 = no lock.", 60.0D, 0.0D, 240.0D, 10.0D, " fps"));
        this.showIndicator = this.add(new BooleanSetting("REC Indicator", "Show a REC dot and timer (it is hidden from screenshots only if you hide it).", true));
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
        Notifications.post("Recording mode", this.recording ? "On - start your recorder now." : "Off.", Notifications.Kind.INFO);
    }

    protected void onDisable()
    {
        this.recording = false;
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
