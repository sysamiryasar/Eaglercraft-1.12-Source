package net.sythix.client.module.visuals;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.GuiTexturePacks;

/**
 * #39 Volume options, sound toggles, hit sounds and custom sound packs (resource packs).
 */
public class SoundController extends Module
{
    private final BooleanSetting muteUnfocused;
    private final BooleanSetting rain;
    private final BooleanSetting caveAmbience;
    private final BooleanSetting explosions;
    private final BooleanSetting music;
    private final ModeSetting hitSound;
    private final NumberSetting hitVolume;

    public SoundController()
    {
        super("Sound Controller", "Volumes, sound toggles, hit sounds and sound packs.", Category.VISUALS, true);

        for (SoundCategory category : SoundCategory.values())
        {
            this.add(new NumberSetting(categoryName(category) + " Volume", "Volume for " + categoryName(category).toLowerCase() + " sounds.", 100.0D, 0.0D, 100.0D, 5.0D, "%")
            {
                public Double get()
                {
                    return Double.valueOf((double)Math.round(SoundController.this.mc.gameSettings.getSoundLevel(category) * 100.0F));
                }

                public void set(Double value)
                {
                    if (SoundController.this.mc != null && SoundController.this.mc.gameSettings != null)
                    {
                        SoundController.this.mc.gameSettings.setSoundLevel(category, (float)Math.max(0.0D, Math.min(100.0D, value.doubleValue())) / 100.0F);
                    }
                }

                public com.google.gson.JsonElement toJson()
                {
                    return com.google.gson.JsonNull.INSTANCE;
                }

                public boolean isDefault()
                {
                    return true;
                }
            });
        }

        this.muteUnfocused = this.add(new BooleanSetting("Mute In Background", "Silence the game when the window is not focused.", false));
        this.rain = this.add(new BooleanSetting("Rain Sounds", "Rain and thunder sounds.", true));
        this.caveAmbience = this.add(new BooleanSetting("Cave Ambience", "Spooky cave sounds.", true));
        this.explosions = this.add(new BooleanSetting("Explosions", "Explosion sounds.", true));
        this.music = this.add(new BooleanSetting("Music", "Background music.", true));
        this.hitSound = this.add(new ModeSetting("Hit Sound", "Extra sound when you hit something (only you hear it).", "Off", "Off", "Ding", "Click", "Pling", "Bell"));
        this.hitVolume = this.add(new NumberSetting("Hit Sound Volume", "Volume of the hit sound.", 60.0D, 5.0D, 100.0D, 5.0D, "%")).visibleWhen(() -> !this.hitSound.is("Off"));
        this.add(new ButtonSetting("Sound Packs", "Sound packs are resource packs; manage them here.", "Open", () -> this.mc.displayGuiScreen(new GuiTexturePacks(this.mc.currentScreen))));
    }

    private static String categoryName(SoundCategory category)
    {
        String n = category.getName();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private boolean muted;

    public void onTick()
    {
        boolean shouldMute = this.muteUnfocused.isOn() && !org.lwjgl.opengl.Display.isActive();

        if (shouldMute != this.muted)
        {
            this.muted = shouldMute;
            // Only the sound engine is changed; the saved volume setting stays the same
            this.mc.getSoundHandler().setSoundLevel(SoundCategory.MASTER, shouldMute ? 0.0F : this.mc.gameSettings.getSoundLevel(SoundCategory.MASTER));
        }
    }

    protected void onDisable()
    {
        if (this.muted)
        {
            this.muted = false;
            this.mc.getSoundHandler().setSoundLevel(SoundCategory.MASTER, this.mc.gameSettings.getSoundLevel(SoundCategory.MASTER));
        }
    }

    /** False when a sound should be silenced. */
    public boolean allow(ISound sound)
    {
        String path = sound.getSoundLocation().getResourcePath();

        if (!this.rain.isOn() && path.startsWith("weather."))
        {
            return false;
        }

        if (!this.caveAmbience.isOn() && path.startsWith("ambient.cave"))
        {
            return false;
        }

        if (!this.explosions.isOn() && path.contains("explode"))
        {
            return false;
        }

        return this.music.isOn() || !path.startsWith("music.");
    }

    public void onAttack(Entity target)
    {
        if (this.hitSound.is("Off") || !(target instanceof EntityLivingBase))
        {
            return;
        }

        SoundEvent event = this.hitSound.is("Ding") ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : (this.hitSound.is("Click") ? SoundEvents.BLOCK_NOTE_HAT : (this.hitSound.is("Bell") ? SoundEvents.BLOCK_NOTE_BELL : SoundEvents.BLOCK_NOTE_PLING));
        this.mc.getSoundHandler().playSound(new PositionedSoundRecord(event.getSoundName(), SoundCategory.MASTER, this.hitVolume.getFloat() / 100.0F, 1.0F, false, 0, ISound.AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
    }
}
