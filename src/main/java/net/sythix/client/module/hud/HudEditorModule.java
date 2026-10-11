package net.sythix.client.module.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.SythixClient;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.GuiHudEditor;

/**
 * #27 HUD editor options: snapping, grid, guides, layering and saved layouts.
 */
public class HudEditorModule extends Module
{
    private final NumberSetting snapDistance;
    private final NumberSetting gridSize;
    private final BooleanSetting guides;
    private final BooleanSetting names;
    private final ModeSetting slot;
    private final JsonObject layouts = new JsonObject();

    public HudEditorModule()
    {
        super("HUD Editor", "Drag, resize, align, layer, show/hide and save HUD layouts.", Category.INTERFACE, true);
        this.setCore();
        this.add(new ButtonSetting("Open Editor", "Move and resize HUD elements (or press Right Shift in game).", "Open", () -> this.mc.displayGuiScreen(new GuiHudEditor(this.mc.currentScreen))));
        this.snapDistance = this.add(new NumberSetting("Snap Distance", "How close an element must be before it snaps. 0 = no snapping.", 4.0D, 0.0D, 12.0D, 1.0D, "px"));
        this.gridSize = this.add(new NumberSetting("Grid", "Snap positions to a grid. 0 = off.", 0.0D, 0.0D, 20.0D, 1.0D, "px"));
        this.guides = this.add(new BooleanSetting("Centre Guides", "Show faint centre lines in the editor.", true));
        this.names = this.add(new BooleanSetting("Element Names", "Show names above elements in the editor.", false));
        this.slot = this.add(new ModeSetting("Layout Slot", "Which saved layout the buttons below use.", "Layout 1", "Layout 1", "Layout 2", "Layout 3"));
        this.add(new ButtonSetting("Save Layout", "Save positions, sizes and which elements are on into the chosen slot.", "Save", this::saveLayout));
        this.add(new ButtonSetting("Load Layout", "Restore the layout saved in the chosen slot.", "Load", this::loadLayout));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiHudEditor(parent);
    }

    public int getSnapDistance()
    {
        return this.snapDistance.getInt();
    }

    public int getGridSize()
    {
        return this.gridSize.getInt();
    }

    public boolean showGuides()
    {
        return this.guides.isOn();
    }

    public boolean showNames()
    {
        return this.names.isOn();
    }

    private void saveLayout()
    {
        JsonObject layout = new JsonObject();

        for (HudModule module : SythixClient.get().getModuleManager().getHudModules())
        {
            JsonObject entry = module.toJson();
            entry.remove("favorite");
            layout.add(module.getName(), entry);
        }

        this.layouts.add(this.slot.get(), layout);
        SythixClient.get().save();
        Notifications.post("Layout saved", "HUD layout stored in " + this.slot.get() + ".", Notifications.Kind.SUCCESS);
    }

    private void loadLayout()
    {
        JsonElement element = this.layouts.get(this.slot.get());

        if (element == null || !element.isJsonObject())
        {
            Notifications.post("Nothing saved", this.slot.get() + " is empty. Save a layout first.", Notifications.Kind.WARNING);
            return;
        }

        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet())
        {
            Module module = SythixClient.get().getModuleManager().getByName(entry.getKey());

            if (module instanceof HudModule && entry.getValue().isJsonObject())
            {
                module.fromJson(entry.getValue().getAsJsonObject());
            }
        }

        SythixClient.get().save();
        Notifications.post("Layout loaded", this.slot.get() + " is now active.", Notifications.Kind.SUCCESS);
    }

    public JsonObject toJson()
    {
        JsonObject json = super.toJson();
        json.add("layouts", this.layouts);
        return json;
    }

    public void fromJson(JsonObject json)
    {
        super.fromJson(json);

        if (json.has("layouts") && json.get("layouts").isJsonObject())
        {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("layouts").entrySet())
            {
                this.layouts.add(entry.getKey(), entry.getValue());
            }
        }
    }

}
