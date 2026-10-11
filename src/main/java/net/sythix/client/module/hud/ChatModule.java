package net.sythix.client.module.hud;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.setting.TextSetting;

/**
 * #30 Chat timestamps, filters, tabs, opacity, mentions, duplicate stacking and history length.
 */
public class ChatModule extends Module
{
    public static final String[] TABS = new String[] {"All", "Players", "Server", "Mentions"};
    private static String activeTab = "All";

    private final BooleanSetting timestamps;
    private final ModeSetting timeFormat;
    private final TextSetting filterWords;
    private final ModeSetting filterMode;
    private final BooleanSetting tabs;
    private final ColorSetting background;
    private final BooleanSetting textShadow;
    private final BooleanSetting mentions;
    private final TextSetting extraMentions;
    private final ColorSetting mentionColor;
    private final BooleanSetting mentionSound;
    private final BooleanSetting compact;
    private final NumberSetting history;

    /** Original message for each drawn line, and its highlight colour (0 = none). */
    // Identity maps with weak keys: vanilla Style.hashCode() throws for styles without a colour,
    // so chat components must never be hashed by value
    private final Map<ITextComponent, ITextComponent> lineToMessage = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private final Map<ITextComponent, Integer> lineHighlight = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private String lastText = "";
    private int lastCount;
    private int lastId;
    private int nextId = 0x5A000000;

    public ChatModule()
    {
        super("Chat", "Timestamps, filters, tabs, opacity, mentions and stacking of repeated messages.", Category.INTERFACE, true);
        this.timestamps = this.add(new BooleanSetting("Timestamps", "Show the time before each message.", false));
        this.timeFormat = this.add(new ModeSetting("Time Format", "Format of timestamps.", "24h", "24h", "12h", "24h + seconds")).visibleWhen(this.timestamps::isOn);
        this.filterWords = this.add(new TextSetting("Filter Words", "Comma-separated words; matching messages are hidden or greyed.", "", 200));
        this.filterMode = this.add(new ModeSetting("Filter Mode", "What happens to filtered messages.", "Hide", "Hide", "Gray Out"));
        this.tabs = this.add(new BooleanSetting("Chat Tabs", "Tabs above the chat box: All, Players, Server, Mentions.", true));
        this.background = this.add(new ColorSetting("Background", "Chat background colour (alpha follows Chat Opacity).", 0xFF000000, false));
        this.textShadow = this.add(new BooleanSetting("Text Shadow", "Shadow behind chat text.", true));
        this.mentions = this.add(new BooleanSetting("Mentions", "Highlight messages that contain your name.", true));
        this.extraMentions = this.add(new TextSetting("Mention Words", "Other comma-separated words that count as mentions.", "", 120)).visibleWhen(this.mentions::isOn);
        this.mentionColor = this.add(new ColorSetting("Mention Color", "Background of mentions.", 0x60D81820, true)).visibleWhen(this.mentions::isOn);
        this.mentionSound = this.add(new BooleanSetting("Mention Sound", "Play a sound when you are mentioned.", true)).visibleWhen(this.mentions::isOn);
        this.compact = this.add(new BooleanSetting("Stack Repeats", "Show repeated messages once with a counter (x3).", true));
        this.history = this.add(new NumberSetting("History", "How many messages chat keeps.", 100.0D, 100.0D, 1000.0D, 50.0D, " lines"));
    }

    public static String getActiveTab()
    {
        return activeTab;
    }

    public static void setActiveTab(String tab)
    {
        activeTab = tab;
    }

    public boolean tabsEnabled()
    {
        return this.tabs.isOn();
    }

    public int getHistory()
    {
        return this.history.getInt();
    }

    public boolean textShadow()
    {
        return this.textShadow.isOn();
    }

    private boolean containsAny(String text, String commaList)
    {
        String lower = text.toLowerCase(Locale.ROOT);

        for (String word : commaList.split(","))
        {
            String w = word.trim().toLowerCase(Locale.ROOT);

            if (!w.isEmpty() && lower.contains(w))
            {
                return true;
            }
        }

        return false;
    }

    public boolean isMention(String text)
    {
        if (!this.mentions.isOn())
        {
            return false;
        }

        String name = this.mc.getSession() == null ? "" : this.mc.getSession().getUsername();
        String lower = text.toLowerCase(Locale.ROOT);

        if (!name.isEmpty() && lower.contains(name.toLowerCase(Locale.ROOT)) && !lower.startsWith("<" + name.toLowerCase(Locale.ROOT) + ">"))
        {
            return true;
        }

        return this.containsAny(text, this.extraMentions.get());
    }

    private static String category(String text)
    {
        return text.startsWith("<") || text.matches("^\\[?[^\\s]{0,24}\\]? ?[A-Za-z0-9_]{3,16}: .*") ? "Players" : "Server";
    }

    /** Whether a message belongs in the active tab. */
    public boolean tabAllows(ITextComponent message)
    {
        if (!this.tabs.isOn() || activeTab.equals("All"))
        {
            return true;
        }

        String text = TextFormatting.getTextWithoutFormattingCodes(message.getUnformattedText());

        if (activeTab.equals("Mentions"))
        {
            return this.isMention(text);
        }

        return category(text).equals(activeTab);
    }

    /**
     * Processes a message before it is added to chat. Returns the message to show, or null to
     * drop it. May remove the previous identical message (stacking).
     */
    public ITextComponent incoming(GuiNewChat chat, ITextComponent message, int[] chatLineId)
    {
        String text = TextFormatting.getTextWithoutFormattingCodes(message.getUnformattedText());

        if (!this.filterWords.get().trim().isEmpty() && this.containsAny(text, this.filterWords.get()))
        {
            if (this.filterMode.is("Hide"))
            {
                return null;
            }

            message = new TextComponentString(TextFormatting.DARK_GRAY + text);
        }

        ITextComponent result = message;

        if (this.compact.isOn() && chatLineId[0] == 0)
        {
            if (text.equals(this.lastText) && this.lastId != 0)
            {
                ++this.lastCount;
                chat.deleteChatLine(this.lastId);
                result = message.createCopy().appendSibling(new TextComponentString(TextFormatting.GRAY + " (x" + this.lastCount + ")"));
            }
            else
            {
                this.lastCount = 1;
                this.lastText = text;
            }

            this.lastId = this.nextId++;
            chatLineId[0] = this.lastId;
        }

        if (this.timestamps.isOn())
        {
            String pattern = this.timeFormat.is("12h") ? "h:mm a" : (this.timeFormat.is("24h + seconds") ? "HH:mm:ss" : "HH:mm");
            result = new TextComponentString(TextFormatting.DARK_GRAY + "[" + new SimpleDateFormat(pattern).format(new Date()) + "] " + TextFormatting.RESET).appendSibling(result);
        }

        if (this.isMention(text) && this.mentionSound.isOn() && !text.startsWith("<" + this.mc.getSession().getUsername() + ">"))
        {
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_NOTE_PLING, 1.4F));
        }

        return result;
    }

    /** Remembers which message each drawn line belongs to (for copying and highlighting). */
    public void tagLines(ITextComponent message, List<ITextComponent> lines, int highlight)
    {
        String text = TextFormatting.getTextWithoutFormattingCodes(message.getUnformattedText());
        int color = this.isMention(text) ? this.mentionColor.getColor() : highlight;

        for (ITextComponent line : lines)
        {
            this.lineToMessage.put(line, message);

            if (color != 0)
            {
                this.lineHighlight.put(line, Integer.valueOf(color));
            }
        }
    }

    public ITextComponent messageForLine(ITextComponent line)
    {
        return this.lineToMessage.get(line);
    }

    /** Background colour for a chat line. alpha is vanilla's fade-adjusted background alpha. */
    public int background(int alpha, ITextComponent line)
    {
        Integer highlight = this.lineHighlight.get(line);

        if (highlight != null)
        {
            int a = Math.min(alpha * 2, (highlight.intValue() >>> 24) & 255);
            return (a << 24) | (highlight.intValue() & 0xFFFFFF);
        }

        return (alpha << 24) | (this.background.getArgb() & 0xFFFFFF);
    }
}
