package net.sythix.client.module.multiplayer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.TextSetting;

/**
 * #43 Party shortcuts and invite notifications. Commands are sent like you typed them, so set
 * them to match your server's party plugin.
 */
public class PartyTools extends Module
{
    private final TextSetting inviteCommand;
    private final TextSetting acceptCommand;
    private final TextSetting warpCommand;
    private final TextSetting leaveCommand;
    private final TextSetting invitePattern;
    private final KeybindSetting inviteKey;
    private final KeybindSetting acceptKey;
    private final KeybindSetting warpKey;
    private final KeybindSetting leaveKey;
    private String lastInviter;

    public PartyTools()
    {
        super("Party Tools", "Hotkeys for party commands and alerts for party invites.", Category.MULTIPLAYER, false);
        this.inviteCommand = this.add(new TextSetting("Invite Command", "{name} is replaced by the player you look at.", "/party invite {name}", 80));
        this.acceptCommand = this.add(new TextSetting("Accept Command", "{name} is replaced by whoever invited you last.", "/party accept {name}", 80));
        this.warpCommand = this.add(new TextSetting("Warp Command", "Command that brings your party to you.", "/party warp", 80));
        this.leaveCommand = this.add(new TextSetting("Leave Command", "Command that leaves the party.", "/party leave", 80));
        this.invitePattern = this.add(new TextSetting("Invite Detection", "Regular expression for invite messages; group 1 is the inviter's name.", "(?i)([A-Za-z0-9_]{3,16}) (?:has )?invited you to (?:join )?(?:their|a|the) party", 160));
        this.inviteKey = this.add(new KeybindSetting("Invite Looked-at Key", "Invite the player you are looking at.", 0));
        this.acceptKey = this.add(new KeybindSetting("Accept Invite Key", "Accept the last party invite.", 0));
        this.warpKey = this.add(new KeybindSetting("Warp Key", "Send the warp command.", 0));
        this.leaveKey = this.add(new KeybindSetting("Leave Key", "Send the leave command.", 0));
    }

    private void send(String command)
    {
        if (this.mc.player != null && !command.trim().isEmpty())
        {
            this.mc.player.sendChatMessage(command.trim());
        }
    }

    public void onKey(int keyCode)
    {
        if (keyCode == 0)
        {
            return;
        }

        if (keyCode == this.inviteKey.getKey())
        {
            if (this.mc.pointedEntity instanceof EntityPlayer)
            {
                this.send(this.inviteCommand.get().replace("{name}", this.mc.pointedEntity.getName()));
            }
            else
            {
                Notifications.post("No player", "Look at a player to invite them.", Notifications.Kind.WARNING);
            }
        }
        else if (keyCode == this.acceptKey.getKey())
        {
            if (this.lastInviter == null)
            {
                Notifications.post("No invite", "Nobody has invited you yet.", Notifications.Kind.WARNING);
            }
            else
            {
                this.send(this.acceptCommand.get().replace("{name}", this.lastInviter));
            }
        }
        else if (keyCode == this.warpKey.getKey())
        {
            this.send(this.warpCommand.get());
        }
        else if (keyCode == this.leaveKey.getKey())
        {
            this.send(this.leaveCommand.get());
        }
    }

    public void onChat(ITextComponent message)
    {
        try
        {
            Matcher m = Pattern.compile(this.invitePattern.get()).matcher(TextFormatting.getTextWithoutFormattingCodes(message.getUnformattedText()));

            if (m.find() && m.groupCount() >= 1)
            {
                this.lastInviter = m.group(1);
                String key = this.acceptKey.getKey() == 0 ? "the accept key (set it in Party Tools)" : this.acceptKey.getKeyName();
                Notifications.post("Party invite", this.lastInviter + " invited you. Press " + key + " to accept.", Notifications.Kind.SOCIAL);
            }
        }
        catch (RuntimeException ignored)
        {
            // invalid pattern: ignore
        }
    }
}
