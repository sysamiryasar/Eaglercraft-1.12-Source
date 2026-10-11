package net.sythix.client.module.movement;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.TextHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #19 XYZ coordinates with formatting options and a copy shortcut.
 */
public class CoordinatesHud extends TextHudModule
{
    private final ModeSetting layout;
    private final NumberSetting decimals;
    private final BooleanSetting showFacing;
    private final BooleanSetting showOtherDimension;
    private final KeybindSetting copyKey;
    private final ModeSetting copyFormat;

    public CoordinatesHud()
    {
        super("Coordinates HUD", "Your X, Y and Z position, facing direction and a copy shortcut.", Category.MOVEMENT, true, 0.005D, 0.6D);
        this.layout = this.add(new ModeSetting("Layout", "One line or one line per axis.", "Vertical", "Vertical", "Horizontal"));
        this.decimals = this.add(new NumberSetting("Decimals", "Digits after the decimal point.", 0.0D, 0.0D, 3.0D, 1.0D));
        this.showFacing = this.add(new BooleanSetting("Show Facing", "Show the direction you face, for example \"North (-Z)\".", true));
        this.showOtherDimension = this.add(new BooleanSetting("Nether Coords", "Also show the matching Nether/Overworld position.", false));
        this.copyKey = this.add(new KeybindSetting("Copy Key", "Copy your coordinates to the clipboard.", 0));
        this.copyFormat = this.add(new ModeSetting("Copy Format", "Text that is copied.", "X Y Z", "X Y Z", "X, Y, Z", "/tp X Y Z"));
    }

    private String fmt(double v)
    {
        return String.format("%." + this.decimals.getInt() + "f", v);
    }

    public void onKey(int keyCode)
    {
        Entity e = this.mc.getRenderViewEntity();

        if (keyCode != 0 && keyCode == this.copyKey.getKey() && e != null)
        {
            String x = String.valueOf((int)Math.floor(e.posX));
            String y = String.valueOf((int)Math.floor(e.getEntityBoundingBox().minY));
            String z = String.valueOf((int)Math.floor(e.posZ));
            String text = this.copyFormat.is("X, Y, Z") ? x + ", " + y + ", " + z : (this.copyFormat.is("/tp X Y Z") ? "/tp " + x + " " + y + " " + z : x + " " + y + " " + z);
            GuiScreen.setClipboardString(text);
            Notifications.post("Coordinates copied", text, Notifications.Kind.SUCCESS);
        }
    }

    public boolean shouldRender()
    {
        return !net.sythix.client.module.advanced.PrivacyControls.hideCoords();
    }

    protected List<String[]> getLines()
    {
        Entity e = this.mc.getRenderViewEntity();
        double x = e == null ? 0.0D : e.posX;
        double y = e == null ? 64.0D : e.getEntityBoundingBox().minY;
        double z = e == null ? 0.0D : e.posZ;
        List<String[]> lines = new ArrayList<String[]>();

        if (this.layout.is("Horizontal"))
        {
            lines.add(new String[] {"XYZ", this.fmt(x) + ", " + this.fmt(y) + ", " + this.fmt(z)});
        }
        else
        {
            lines.add(new String[] {"X", this.fmt(x)});
            lines.add(new String[] {"Y", this.fmt(y)});
            lines.add(new String[] {"Z", this.fmt(z)});
        }

        if (this.showFacing.isOn())
        {
            EnumFacing facing = e == null ? EnumFacing.NORTH : e.getHorizontalFacing();
            String axis = facing.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? "+" : "-";
            String name = facing.getName();
            lines.add(new String[] {"Facing", Character.toUpperCase(name.charAt(0)) + name.substring(1) + " (" + axis + facing.getAxis().getName().toUpperCase() + ")"});
        }

        if (this.showOtherDimension.isOn())
        {
            boolean nether = this.mc.world != null && this.mc.world.provider.isNether();
            double f = nether ? 8.0D : 0.125D;
            lines.add(new String[] {nether ? "Overworld" : "Nether", this.fmt(x * f) + ", " + this.fmt(z * f)});
        }

        return lines;
    }
}
