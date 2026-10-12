package net.sythix.client.module.multiplayer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.TextSetting;
import org.apache.commons.io.FileUtils;

/**
 * #45 Message copying, saved history, keyword highlights and chat shortcuts.
 */
public class ChatUtilities extends Module
{
    private final BooleanSetting copy;
    private final BooleanSetting saveHistory;
    private final TextSetting keywords;
    private final ColorSetting keywordColor;
    private final TextSetting replacements;
    private final KeybindSetting quickKey1;
    private final TextSetting quickText1;
    private final KeybindSetting quickKey2;
    private final TextSetting quickText2;
    private boolean historyLoaded;
    private boolean historyDirty;

    public ChatUtilities()
    {
        super("Chat Utilities", "Ctrl+click to copy messages, saved chat history, keyword highlights and shortcuts.", Category.MULTIPLAYER, true);
        this.copy = this.add(new BooleanSetting("Ctrl+Click Copy", "Hold Ctrl and click a chat message to copy it.", true));
        this.saveHistory = this.add(new BooleanSetting("Save Sent History", "Keep the messages you sent (arrow keys) after restarting.", true));
        this.keywords = this.add(new TextSetting("Highlight Words", "Comma-separated words to highlight in chat.", "", 200));
        this.keywordColor = this.add(new ColorSetting("Highlight Color", "Background of highlighted messages.", 0x502F6BFF, true));
        this.replacements = this.add(new TextSetting("Shortcuts", "Text replacements when you send, e.g. gg=good game; brb=be right back", "", 300));
        this.quickKey1 = this.add(new KeybindSetting("Quick Message 1 Key", "Sends Quick Message 1.", 0));
        this.quickText1 = this.add(new TextSetting("Quick Message 1", "Message or command sent by the key above.", "", 100));
        this.quickKey2 = this.add(new KeybindSetting("Quick Message 2 Key", "Sends Quick Message 2.", 0));
        this.quickText2 = this.add(new TextSetting("Quick Message 2", "Message or command sent by the key above.", "", 100));
    }

    private File historyFile()
    {
        return new File(new File(this.mc.mcDataDir, "sythix"), "chat-history.txt");
    }

    public void onTick()
    {
        if (this.historyDirty)
        {
            this.historyDirty = false;
            this.writeHistory();
        }

        if (!this.historyLoaded && this.mc.ingameGUI != null)
        {
            this.historyLoaded = true;

            if (this.saveHistory.isOn() && this.historyFile().isFile())
            {
                try
                {
                    List<String> sent = this.mc.ingameGUI.getChatGUI().getSentMessages();

                    if (sent.isEmpty())
                    {
                        sent.addAll(FileUtils.readLines(this.historyFile(), StandardCharsets.UTF_8));
                    }
                }
                catch (Exception ignored)
                {
                    // history is optional
                }
            }
        }
    }

    public void onSent()
    {
        this.historyDirty = true;
    }

    private void writeHistory()
    {
        if (!this.saveHistory.isOn() || this.mc.ingameGUI == null)
        {
            return;
        }

        try
        {
            List<String> sent = this.mc.ingameGUI.getChatGUI().getSentMessages();
            List<String> last = sent.subList(Math.max(0, sent.size() - 100), sent.size());
            FileUtils.writeLines(this.historyFile(), "UTF-8", last);
        }
        catch (Exception ignored)
        {
            // history is optional
        }
    }

    public void onKey(int keyCode)
    {
        if (keyCode == 0 || this.mc.player == null)
        {
            return;
        }

        if (keyCode == this.quickKey1.getKey() && !this.quickText1.get().trim().isEmpty())
        {
            this.mc.player.sendChatMessage(this.applyShortcuts(this.quickText1.get().trim()));
        }
        else if (keyCode == this.quickKey2.getKey() && !this.quickText2.get().trim().isEmpty())
        {
            this.mc.player.sendChatMessage(this.applyShortcuts(this.quickText2.get().trim()));
        }
    }

    /** Replaces whole-word shortcuts ("gg=good game; brb=be right back"). */
    public String applyShortcuts(String message)
    {
        String result = message;

        for (String pair : this.replacements.get().split(";"))
        {
            int eq = pair.indexOf('=');

            if (eq > 0)
            {
                String from = pair.substring(0, eq).trim();
                String to = pair.substring(eq + 1).trim();

                if (!from.isEmpty())
                {
                    result = result.replaceAll("(?<![A-Za-z0-9_])" + java.util.regex.Pattern.quote(from) + "(?![A-Za-z0-9_])", java.util.regex.Matcher.quoteReplacement(to));
                }
            }
        }

        return result.length() > 256 ? result.substring(0, 256) : result;
    }

    /** Highlight colour for a message (0 = none). */
    public int highlight(ITextComponent message)
    {
        String text = TextFormatting.getTextWithoutFormattingCodes(message.getUnformattedText()).toLowerCase(Locale.ROOT);

        for (String word : this.keywords.get().split(","))
        {
            String w = word.trim().toLowerCase(Locale.ROOT);

            if (!w.isEmpty() && text.contains(w))
            {
                return this.keywordColor.getColor();
            }
        }

        return 0;
    }

    public boolean copyEnabled()
    {
        return this.copy.isOn();
    }

    public void copy(ITextComponent message)
    {
        String text = TextFormatting.getTextWithoutFormattingCodes(message.getUnformattedText());
        GuiScreen.setClipboardString(text);
        Notifications.post("Message copied", text, Notifications.Kind.SUCCESS);
    }
}
