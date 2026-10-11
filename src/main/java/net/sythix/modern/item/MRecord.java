package net.sythix.modern.item;

import net.minecraft.item.ItemRecord;
import net.minecraft.util.SoundEvent;

/**
 * Music discs added after 1.12. Their music plays when a converted 1.21 pack (or a resource pack)
 * provides the sound file (assets/minecraft/sounds/records/NAME.ogg).
 */
public class MRecord extends ItemRecord
{
    public MRecord(String name, SoundEvent sound)
    {
        super(name, sound);
    }
}
