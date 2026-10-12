package net.sythix.client.module.pvp;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.EnumHand;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #12 Position, scale and rotation of the items in your hands.
 */
public class ViewmodelModule extends Module
{
    private final NumberSetting mainX;
    private final NumberSetting mainY;
    private final NumberSetting mainZ;
    private final NumberSetting mainScale;
    private final NumberSetting mainRotX;
    private final NumberSetting mainRotY;
    private final NumberSetting mainRotZ;
    private final BooleanSetting separateOffhand;
    private final NumberSetting offX;
    private final NumberSetting offY;
    private final NumberSetting offZ;
    private final NumberSetting offScale;

    public ViewmodelModule()
    {
        super("Viewmodel", "Move, scale and rotate the items in your hands.", Category.PVP, false);
        this.add(new GameOptionSetting("Which hand is your main hand.", GameSettings.Options.MAIN_HAND));
        this.mainX = this.add(new NumberSetting("Main X", "Left/right offset of the main hand.", 0.0D, -1.0D, 1.0D, 0.02D));
        this.mainY = this.add(new NumberSetting("Main Y", "Up/down offset of the main hand.", 0.0D, -1.0D, 1.0D, 0.02D));
        this.mainZ = this.add(new NumberSetting("Main Z", "Forward/back offset of the main hand.", 0.0D, -1.0D, 1.0D, 0.02D));
        this.mainScale = this.add(new NumberSetting("Main Scale", "Size of the main hand item.", 1.0D, 0.3D, 1.5D, 0.05D, "x"));
        this.mainRotX = this.add(new NumberSetting("Main Pitch", "Tilt forward/back.", 0.0D, -90.0D, 90.0D, 1.0D, " deg"));
        this.mainRotY = this.add(new NumberSetting("Main Yaw", "Turn left/right.", 0.0D, -90.0D, 90.0D, 1.0D, " deg"));
        this.mainRotZ = this.add(new NumberSetting("Main Roll", "Roll sideways.", 0.0D, -90.0D, 90.0D, 1.0D, " deg"));
        this.separateOffhand = this.add(new BooleanSetting("Separate Off Hand", "Give the off hand its own position and size.", false));
        this.offX = this.add(new NumberSetting("Off X", "Left/right offset of the off hand.", 0.0D, -1.0D, 1.0D, 0.02D)).visibleWhen(this.separateOffhand::isOn);
        this.offY = this.add(new NumberSetting("Off Y", "Up/down offset of the off hand.", 0.0D, -1.0D, 1.0D, 0.02D)).visibleWhen(this.separateOffhand::isOn);
        this.offZ = this.add(new NumberSetting("Off Z", "Forward/back offset of the off hand.", 0.0D, -1.0D, 1.0D, 0.02D)).visibleWhen(this.separateOffhand::isOn);
        this.offScale = this.add(new NumberSetting("Off Scale", "Size of the off hand item.", 1.0D, 0.3D, 1.5D, 0.05D, "x")).visibleWhen(this.separateOffhand::isOn);
    }

    /** Applies the transform for one hand. sideSign is +1 for the right side of the screen, -1 for the left. */
    public void apply(EnumHand hand, float sideSign)
    {
        boolean off = hand == EnumHand.OFF_HAND && this.separateOffhand.isOn();
        float x = (off ? this.offX : this.mainX).getFloat();
        float y = (off ? this.offY : this.mainY).getFloat();
        float z = (off ? this.offZ : this.mainZ).getFloat();
        float scale = (off ? this.offScale : this.mainScale).getFloat();
        GlStateManager.translate(x * sideSign, y, z);

        if (!off)
        {
            GlStateManager.rotate(this.mainRotX.getFloat(), 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(this.mainRotY.getFloat() * sideSign, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(this.mainRotZ.getFloat() * sideSign, 0.0F, 0.0F, 1.0F);
        }

        GlStateManager.scale(scale, scale, scale);
    }
}
