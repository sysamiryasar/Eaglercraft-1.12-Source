package net.sythix.client.cosmetic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.sythix.client.SythixClient;
import net.sythix.client.module.visuals.CosmeticsModule;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.Setting;
import net.sythix.client.ui.SythixTheme;
import net.sythix.client.ui.component.SettingsPanel;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The Wardrobe: every Sythix cosmetic in tabs (Capes, Headwear, Wings, Auras, Extras) plus an
 * Emotes tab, with a live 3D preview. Hovering a card previews it; clicking equips it (clicking an
 * equipped one takes it off). Everything is saved straight away.
 */
public class GuiCosmetics extends GuiScreen
{
    private static final String EMOTES = "Emotes";
    private static final String[] TABS = {CosmeticCatalog.CAPES, CosmeticCatalog.HEADWEAR, CosmeticCatalog.WINGS, CosmeticCatalog.AURAS, CosmeticCatalog.EXTRAS, EMOTES};
    private static String tab = CosmeticCatalog.CAPES;
    private final GuiScreen parent;
    private final SettingsPanel options = new SettingsPanel();
    private float yaw = 200.0F;
    private boolean dragging;
    private int lastMouseX;
    private int scroll;
    private String focusedExtra;
    private int wheelSlot = -1;
    private String previewEmote;
    private long previewEmoteStart;
    private int px;
    private int py;
    private int pw;
    private int ph;
    private int gridX;
    private int gridY;
    private int gridW;
    private int gridH;
    private int previewX;
    private int previewW;
    private int previewBottom;
    private int footerY;

    public GuiCosmetics(GuiScreen parent)
    {
        this.parent = parent;
    }

    /** Opens on a tab (Capes, Headwear, Wings, Auras, Extras or Emotes). */
    public GuiCosmetics(GuiScreen parent, String startTab)
    {
        this.parent = parent;

        for (String t : TABS)
        {
            if (t.equals(startTab))
            {
                tab = t;
            }
        }
    }

    private static CosmeticsModule cosmetics()
    {
        return SythixClient.get().getModuleManager().get(CosmeticsModule.class);
    }

    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        // header, a row of tabs, then cards (left) and the preview with its options (right)
        this.pw = Math.min(this.width - 8, 560);
        this.ph = Math.min(this.height - 8, 330);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
        this.footerY = this.py + this.ph - 22;
        this.previewW = Math.max(120, this.pw * 36 / 100);
        this.previewX = this.px + this.pw - this.previewW - 6;
        this.gridX = this.px + 6;
        this.gridY = this.py + 42;
        this.gridW = this.previewX - this.gridX - 6;
        this.gridH = this.footerY - 4 - this.gridY - (EMOTES.equals(tab) ? 46 : 0);
        this.refreshOptions();
    }

    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
        SythixClient.get().save();
    }

    private void refreshOptions()
    {
        CosmeticsModule c = cosmetics();
        List<Setting<?>> list = new ArrayList<Setting<?>>();

        if (c != null)
        {
            if (EMOTES.equals(tab))
            {
                list.addAll(c.emoteSettings());
            }
            else if (CosmeticCatalog.EXTRAS.equals(tab))
            {
                if (this.focusedExtra != null)
                {
                    list.addAll(c.optionsFor(this.focusedExtra));
                }

                list.add(new ButtonSetting("Try Victory Burst", "Play the victory burst now (in a world).", "Play", c::victoryBurst));
                list.add(new ButtonSetting("Try Spawn Entrance", "Play the spawn portal now (in a world).", "Play", c::spawnPortal));
            }
            else
            {
                list.addAll(c.optionsFor(tab));
            }
        }

        // the options take what they need under the preview (at most about four rows)
        int needed = 0;

        for (Setting<?> setting : list)
        {
            needed += setting instanceof net.sythix.client.setting.NumberSetting ? 24 : 16;
        }

        int optionsH = Math.min(needed, Math.max(32, (this.footerY - 4 - this.gridY) / 2 - 8));
        this.previewBottom = this.footerY - 4 - optionsH - (optionsH > 0 ? 4 : 0);
        this.options.setBounds(this.previewX, this.previewBottom + 4, this.previewW, optionsH);
        this.options.setSettings(list);
    }

    // ---- what each card is ---------------------------------------------------------------------

    private List<String> cards()
    {
        List<String> names = new ArrayList<String>();

        if (EMOTES.equals(tab))
        {
            for (String e : Emotes.ALL)
            {
                names.add(e);
            }
        }
        else
        {
            for (CosmeticCatalog.Item item : CosmeticCatalog.in(tab))
            {
                names.add(item.name);
            }
        }

        return names;
    }

    private CosmeticCatalog.Item item(String name)
    {
        for (CosmeticCatalog.Item item : CosmeticCatalog.all())
        {
            if (item.name.equals(name))
            {
                return item;
            }
        }

        return null;
    }

    private boolean equipped(String name)
    {
        CosmeticsModule c = cosmetics();

        if (c == null)
        {
            return false;
        }

        if (CosmeticCatalog.CAPES.equals(tab))
        {
            CosmeticCatalog.Item item = this.item(name);
            return item != null && item.capeId().equals(ProfileStore.cape);
        }

        if (CosmeticCatalog.EXTRAS.equals(tab))
        {
            return c.extra(name).isOn();
        }

        if (EMOTES.equals(tab))
        {
            for (int i = 0; i < 8; ++i)
            {
                if (c.wheelSlot(i).equals(name))
                {
                    return true;
                }
            }

            return false;
        }

        ModeSetting slot = c.slot(tab);
        return slot != null && slot.is(name);
    }

    private void click(String name)
    {
        CosmeticsModule c = cosmetics();

        if (c == null)
        {
            return;
        }

        if (EMOTES.equals(tab))
        {
            this.previewEmote = name;
            this.previewEmoteStart = Minecraft.getSystemTime();

            if (this.wheelSlot >= 0)
            {
                c.setWheelSlot(this.wheelSlot, name);
                this.wheelSlot = -1;
            }

            return;
        }

        if (CosmeticCatalog.CAPES.equals(tab))
        {
            CosmeticCatalog.Item item = this.item(name);
            ProfileStore.cape = item.capeId().equals(ProfileStore.cape) ? "none" : item.capeId();
            ProfileStore.save();
            return;
        }

        if (CosmeticCatalog.EXTRAS.equals(tab))
        {
            c.extra(name).set(Boolean.valueOf(!c.extra(name).isOn()));
            this.focusedExtra = name;
            this.refreshOptions();
            return;
        }

        ModeSetting slot = c.slot(tab);
        slot.set(slot.is(name) ? CosmeticCatalog.NONE : name);
    }

    /** The look to preview: what is equipped, plus the hovered card. */
    private PlayerPreview.Look look(String hovered)
    {
        PlayerPreview.Look look = PlayerPreview.Look.current(cosmetics());

        if (hovered != null)
        {
            switch (tab)
            {
                case CosmeticCatalog.CAPES:
                    look.cape = this.item(hovered).capeId();
                    look.showCape = true;
                    break;
                case CosmeticCatalog.HEADWEAR:
                    look.headwear = hovered;
                    break;
                case CosmeticCatalog.WINGS:
                    look.wings = hovered;
                    break;
                case CosmeticCatalog.AURAS:
                    look.aura = hovered;
                    break;
                case CosmeticCatalog.EXTRAS:
                    look.companion |= "Rift Companion".equals(hovered);
                    look.emblem |= "Signature Emblem".equals(hovered);
                    break;
                default:
            }
        }

        if (this.previewEmote != null && Minecraft.getSystemTime() - this.previewEmoteStart < 4000L)
        {
            look.emote = this.previewEmote;
            look.emoteStart = this.previewEmoteStart;
        }

        return look;
    }

    // ---- drawing -----------------------------------------------------------------------------------

    private int columns()
    {
        return Math.max(1, this.gridW / 110);
    }

    private int cardW()
    {
        return (this.gridW - (this.columns() - 1) * 4) / this.columns();
    }

    private static final int CARD_H = 20;

    private int maxScroll()
    {
        int rows = (this.cards().size() + this.columns() - 1) / this.columns();
        return Math.max(0, rows * (CARD_H + 4) - 4 - this.gridH);
    }

    /** Index of the card under the mouse, or -1. */
    private int cardAt(int mouseX, int mouseY)
    {
        if (mouseX < this.gridX || mouseX >= this.gridX + this.gridW || mouseY < this.gridY || mouseY >= this.gridY + this.gridH)
        {
            return -1;
        }

        int col = (mouseX - this.gridX) / (this.cardW() + 4);
        int row = (mouseY - this.gridY + this.scroll) / (CARD_H + 4);
        int inX = (mouseX - this.gridX) % (this.cardW() + 4);
        int inY = (mouseY - this.gridY + this.scroll) % (CARD_H + 4);
        int index = row * this.columns() + col;
        return col < this.columns() && inX < this.cardW() && inY < CARD_H && index < this.cards().size() ? index : -1;
    }

    /** {x, y, width} of tab i: a row under the title, as wide as the text needs. */
    private int[] tabBounds(int i)
    {
        int x = this.px + 6;

        for (int j = 0; j < i; ++j)
        {
            x += this.fontRenderer.getStringWidth(TABS[j]) + 14 + 3;
        }

        return new int[] {x, this.py + 19, this.fontRenderer.getStringWidth(TABS[i]) + 14};
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        if (this.mc.world == null)
        {
            SythixTheme.drawBackground(this.width, this.height, mouseX, mouseY, 0.45F);
        }
        else
        {
            SythixTheme.drawBackground(this.width, this.height, -1, -1, 0.35F);
        }

        SythixTheme.drawGlass((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), 8.0F, 1.0F);
        this.fontRenderer.drawString("Wardrobe", (float)(this.px + 8), (float)(this.py + 6), 0xFFFFFFFF, true);
        String note = "Shown on your screen only";
        this.fontRenderer.drawString(note, (float)(this.px + this.pw - 8 - this.fontRenderer.getStringWidth(note)), (float)(this.py + 6), SythixTheme.TEXT_DISABLED, false);

        for (int i = 0; i < TABS.length; ++i)
        {
            int[] t = this.tabBounds(i);
            boolean active = TABS[i].equals(tab);
            boolean hover = mouseX >= t[0] && mouseX < t[0] + t[2] && mouseY >= t[1] && mouseY < t[1] + 15;
            SythixTheme.drawButton((float)t[0], (float)t[1], (float)t[2], 15.0F, true, active ? 1.0F : (hover ? 0.6F : 0.0F));

            if (active)
            {
                SythixTheme.drawRect((float)t[0] + 3.0F, (float)(t[1] + 13), (float)(t[0] + t[2] - 3), (float)(t[1] + 15), SythixTheme.RED);
            }

            this.fontRenderer.drawString(TABS[i], (float)(t[0] + t[2] / 2 - this.fontRenderer.getStringWidth(TABS[i]) / 2), (float)(t[1] + 4), active ? 0xFFFFFFFF : SythixTheme.TEXT, false);
        }

        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + 38), (float)(this.px + this.pw), 0.6F);

        String hovered = this.drawCards(mouseX, mouseY);

        if (EMOTES.equals(tab))
        {
            this.drawWheelSlots(mouseX, mouseY);
        }

        // preview, with the shown cosmetic's name over it
        int previewTop = this.gridY;
        SythixTheme.drawRect((float)this.previewX, (float)previewTop, (float)(this.previewX + this.previewW), (float)this.previewBottom, 0x40000000);

        if (this.dragging)
        {
            this.yaw += (float)(mouseX - this.lastMouseX) * 1.5F;
            this.lastMouseX = mouseX;
        }

        float scale = Math.min((float)(this.previewBottom - previewTop - 24) / 2.6F, (float)this.previewW / 2.2F);

        try
        {
            SythixTheme.beginClip(this.previewX, previewTop, this.previewX + this.previewW, this.previewBottom);
            GlStateManager.pushMatrix();
            PlayerPreview.draw(this.previewX + this.previewW / 2, previewTop + 24 + (int)(scale * 0.55F), scale, this.yaw, this.look(EMOTES.equals(tab) ? null : hovered));
            GlStateManager.popMatrix();
        }
        catch (Throwable t)
        {
            net.sythix.client.core.ModuleErrors.report(cosmetics(), "wardrobe preview", t);
        }
        finally
        {
            SythixTheme.endClip();
        }

        String shown = hovered != null ? hovered : (EMOTES.equals(tab) ? this.previewEmote : null);
        CosmeticCatalog.Item item = shown == null ? null : this.item(shown);
        String title = item != null ? String.format("%02d  %s", item.number, item.name) : (shown != null ? shown : "Drag to turn");
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(title, this.previewW - 6), (float)(this.previewX + 3), (float)(previewTop + 3), 0xFFFFFFFF, true);
        this.options.render(mouseX, mouseY);

        // footer: what the hovered thing does (or a hint), then Done
        Setting<?> hoveredSetting = this.options.getHoveredSetting();
        String text = hoveredSetting != null ? hoveredSetting.getDescription() : (item != null ? item.description : (EMOTES.equals(tab) ? (this.wheelSlot >= 0 ? "Now click an emote for slot " + (this.wheelSlot + 1) + "." : "Click an emote to preview it. Click a wheel slot, then an emote, to change the wheel.") : "Hover a cosmetic to try it on, click to wear it, click again to take it off."));
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(text, this.pw - 84), (float)(this.px + 8), (float)(this.footerY + 7), SythixTheme.TEXT_MUTED, false);

        int dx = this.px + this.pw - 66;
        int dy = this.footerY + 2;
        boolean doneHover = mouseX >= dx && mouseX < dx + 60 && mouseY >= dy && mouseY < dy + 18;
        SythixTheme.drawButton((float)dx, (float)dy, 60.0F, 18.0F, true, doneHover ? 1.0F : 0.0F);
        this.fontRenderer.drawString("Done", (float)(dx + 30 - this.fontRenderer.getStringWidth("Done") / 2), (float)(dy + 5), SythixTheme.TEXT, false);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** Draws the cards and returns the hovered one's name. */
    private String drawCards(int mouseX, int mouseY)
    {
        List<String> cards = this.cards();
        int hoveredIndex = this.cardAt(mouseX, mouseY);
        int cw = this.cardW();
        this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
        SythixTheme.beginClip(this.gridX, this.gridY, this.gridX + this.gridW, this.gridY + this.gridH);

        for (int i = 0; i < cards.size(); ++i)
        {
            int cx = this.gridX + (i % this.columns()) * (cw + 4);
            int cy = this.gridY + (i / this.columns()) * (CARD_H + 4) - this.scroll;

            if (cy + CARD_H < this.gridY || cy > this.gridY + this.gridH)
            {
                continue;
            }

            String name = cards.get(i);
            boolean on = this.equipped(name);
            boolean hover = i == hoveredIndex;
            SythixTheme.drawRounded((float)cx, (float)cy, (float)(cx + cw), (float)(cy + CARD_H), 4.0F, hover ? 0x34FFFFFF : 0x1CFFFFFF);

            if (on)
            {
                SythixTheme.drawRoundedOutline((float)cx, (float)cy, (float)(cx + cw), (float)(cy + CARD_H), 4.0F, 1.0F, SythixTheme.RED);
            }

            CosmeticCatalog.Item item = this.item(name);
            int textX = cx + 6;

            if (item != null)
            {
                String number = String.format("%02d", item.number);
                this.fontRenderer.drawString(number, (float)textX, (float)(cy + 6), on ? 0xFFFF7A3D : SythixTheme.TEXT_DISABLED, false);
                textX += this.fontRenderer.getStringWidth("00") + 5;
            }

            int room = cx + cw - textX - (on ? 10 : 4);
            this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(name, room), (float)textX, (float)(cy + 6), hover || on ? 0xFFFFFFFF : SythixTheme.TEXT, false);

            if (on)
            {
                // a small dot: worn (or on the emote wheel)
                SythixTheme.drawRounded((float)(cx + cw - 9), (float)(cy + 8), (float)(cx + cw - 5), (float)(cy + 12), 2.0F, 0xFFFF7A3D);
            }
        }

        SythixTheme.endClip();
        return hoveredIndex >= 0 ? cards.get(hoveredIndex) : null;
    }

    /** The emote wheel's eight slots under the emote cards. */
    private void drawWheelSlots(int mouseX, int mouseY)
    {
        CosmeticsModule c = cosmetics();

        if (c == null)
        {
            return;
        }

        int top = this.gridY + this.gridH + 4;
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth("Emote wheel - hold " + Keyboard.getKeyName(c.wheelKey()) + " in game", this.gridW), (float)this.gridX, (float)top, SythixTheme.TEXT_MUTED, false);
        int w = this.gridW / 4 - 3;

        for (int i = 0; i < 8; ++i)
        {
            int x = this.gridX + (i % 4) * (this.gridW / 4);
            int y = top + 10 + (i / 4) * 17;
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 17;
            SythixTheme.drawButton((float)x, (float)y, (float)w, 15.0F, true, this.wheelSlot == i ? 1.0F : (hover ? 0.6F : 0.0F));

            if (this.wheelSlot == i)
            {
                SythixTheme.drawRoundedOutline((float)x, (float)y, (float)(x + w), (float)(y + 15), 3.0F, 1.0F, SythixTheme.RED);
            }

            String label = this.fontRenderer.trimStringToWidth((i + 1) + " " + c.wheelSlot(i), w - 6);
            this.fontRenderer.drawString(label, (float)(x + 4), (float)(y + 4), SythixTheme.TEXT, false);
        }
    }

    // ---- input ---------------------------------------------------------------------------------------

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (int i = 0; i < TABS.length; ++i)
        {
            int[] t = this.tabBounds(i);

            if (mouseX >= t[0] && mouseX < t[0] + t[2] && mouseY >= t[1] && mouseY < t[1] + 15)
            {
                tab = TABS[i];
                this.scroll = 0;
                this.focusedExtra = null;
                this.wheelSlot = -1;
                this.initGui();
                return;
            }
        }

        int dx = this.px + this.pw - 66;
        int dy = this.footerY + 2;

        if (mouseX >= dx && mouseX < dx + 60 && mouseY >= dy && mouseY < dy + 18)
        {
            this.mc.displayGuiScreen(this.parent);
            return;
        }

        int card = this.cardAt(mouseX, mouseY);

        if (card >= 0)
        {
            this.click(this.cards().get(card));
            return;
        }

        if (EMOTES.equals(tab))
        {
            int w = this.gridW / 4 - 3;
            int top = this.gridY + this.gridH + 4;

            for (int i = 0; i < 8; ++i)
            {
                int x = this.gridX + (i % 4) * (this.gridW / 4);
                int y = top + 10 + (i / 4) * 17;

                if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 15)
                {
                    this.wheelSlot = this.wheelSlot == i ? -1 : i;
                    return;
                }
            }
        }

        if (mouseX >= this.previewX && mouseX < this.previewX + this.previewW && mouseY >= this.gridY && mouseY < this.previewBottom)
        {
            this.dragging = true;
            this.lastMouseX = mouseX;
            return;
        }

        this.options.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
        this.dragging = false;
        this.options.mouseReleased();
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0)
        {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;

            if (mouseX >= this.gridX && mouseX < this.gridX + this.gridW)
            {
                this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll - Integer.signum(wheel) * 20));
            }
            else
            {
                this.options.scrollBy(wheel);
            }
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.options.keyTyped(typedChar, keyCode))
        {
            return;
        }

        if (keyCode == 1)
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
