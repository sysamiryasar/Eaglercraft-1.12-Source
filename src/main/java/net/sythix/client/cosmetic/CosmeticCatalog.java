package net.sythix.client.cosmetic;

import java.util.ArrayList;
import java.util.List;

/**
 * Every Sythix cosmetic: its number, name, category and description. The wardrobe, the settings
 * and the renderers all use these names, so this is the one list to change when adding one.
 */
public final class CosmeticCatalog
{
    public static final String CAPES = "Capes";
    public static final String HEADWEAR = "Headwear";
    public static final String WINGS = "Wings";
    public static final String AURAS = "Auras";
    public static final String EXTRAS = "Extras";
    public static final String[] CATEGORIES = {CAPES, HEADWEAR, WINGS, AURAS, EXTRAS};
    public static final String NONE = "None";

    /** One cosmetic. */
    public static final class Item
    {
        public final int number;
        public final String name;
        public final String category;
        public final String description;

        Item(int number, String name, String category, String description)
        {
            this.number = number;
            this.name = name;
            this.category = category;
            this.description = description;
        }

        /** Cape id saved in the profile (sythix/profile.json). */
        public String capeId()
        {
            return "sythix:" + this.name.toLowerCase().replaceAll("[^a-z0-9]+", "_");
        }
    }

    private static final List<Item> ITEMS = new ArrayList<Item>();

    static
    {
        cape(1, "Void Rift", "Black cape with a glowing purple crack that pulses.");
        cape(2, "Galaxy Drift", "Animated stars and a swirling nebula.");
        cape(3, "Cyber Circuit", "Neon blue circuit lines that travel down the fabric.");
        cape(4, "Inferno", "Animated red-orange flames along the bottom.");
        cape(5, "Frostbite", "Icy blue crystalline patterns that shimmer.");
        cape(6, "Celestial", "White-and-gold cape with animated constellations.");
        cape(7, "Shadow Pulse", "Dark smoke drifting across the fabric.");
        cape(8, "Glitchwave", "RGB distortion and shifting pixel patterns.");
        cape(9, "Royal Eclipse", "Black and gold with a slowly rotating eclipse.");
        cape(10, "SYTHIX Genesis", "Signature purple energy flowing into the SYTHIX emblem.");

        head(11, "Void Headband", "Black band with a pulsing void gem and drifting tails.");
        head(12, "Cyber Band", "Tech band with a neon strip and a scanning light.");
        head(13, "Shadow Ninja", "Dark ninja band with a steel plate and long flowing tails.");
        head(14, "Rift Crown", "Purple rift shards floating in a slowly turning crown.");
        head(15, "Phantom Ears", "Glowing see-through purple ears that pulse.");
        head(16, "Solar Halo", "A golden halo with turning sun rays.");
        head(17, "Frost Horns", "Curved horns of shimmering ice.");
        head(18, "Infernal Horns", "Dark horns with tips that glow like embers.");
        head(19, "Holo Visor", "A see-through cyan visor with a moving scan line.");
        head(20, "Arctic Fox", "White fox ears with soft grey tips.");
        head(21, "Astral Hat", "A deep blue wizard hat with twinkling stars.");
        head(22, "Pulse Headphones", "Headphones with rings of light that pulse through colours.");
        head(23, "Champion Laurel", "A golden laurel wreath.");
        head(24, "Rift Shards", "Crystal shards orbiting your head.");
        head(25, "Overlord Crown", "A tall dark crown with glowing red gems.");

        wings(26, "Void Wings", "Fractured obsidian feathers with purple energy.");
        wings(27, "Celestial Wings", "White feathers with gold particles.");
        wings(28, "Cyber Wings", "Segmented mechanical wings with animated lights.");
        wings(29, "Frost Wings", "Translucent ice crystals that sparkle.");
        wings(30, "Infernal Wings", "Dark membranes with flowing lava veins.");
        wings(31, "Astral Wings", "See-through galaxy wings with drifting stars.");
        wings(32, "Phantom Wings", "Semi-transparent purple energy wings.");
        wings(33, "Dragon Wings", "Articulated scaled wings with a slow idle flap.");

        aura(34, "Void Aura", "A turning void ring at your feet and purple rift particles.");
        aura(35, "Thunder Core", "Crackling lightning arcs around your body.");
        aura(36, "Inferno Aura", "A ring of fire and flames spiralling up around you.");
        aura(37, "Absolute Zero", "Ice crystals around your feet and falling snow.");
        aura(38, "Divine Radiance", "Turning beams of golden light and rising sparks.");
        aura(39, "Glitch Entity", "RGB glitch blocks flickering around you.");
        aura(42, "Shadowborn", "Shadow tendrils and dark smoke trailing behind you.");
        aura(43, "Prism Bloom", "Rainbow petals blooming around your feet.");

        extra(44, "Weapon Trails", "Purple, cyan, red or gold trails when you swing.");
        extra(45, "Footstep Runes", "Glowing rune circles appear under your feet for a moment.");
        extra(46, "Rift Companion", "A tiny floating crystal that follows you.");
        extra(47, "Victory Burst", "A burst of light when you win a duel.");
        extra(48, "Afterimage", "Fading silhouettes follow you while you move.");
        extra(49, "Spawn Entrance", "A portal opens around you when you join a world.");
        extra(50, "Signature Emblem", "An animated SYTHIX badge above your head (text, colour and style can be changed).");
    }

    private CosmeticCatalog()
    {
    }

    private static void cape(int n, String name, String text)
    {
        ITEMS.add(new Item(n, name, CAPES, text));
    }

    private static void head(int n, String name, String text)
    {
        ITEMS.add(new Item(n, name, HEADWEAR, text));
    }

    private static void wings(int n, String name, String text)
    {
        ITEMS.add(new Item(n, name, WINGS, text));
    }

    private static void aura(int n, String name, String text)
    {
        ITEMS.add(new Item(n, name, AURAS, text));
    }

    private static void extra(int n, String name, String text)
    {
        ITEMS.add(new Item(n, name, EXTRAS, text));
    }

    public static List<Item> all()
    {
        return ITEMS;
    }

    public static List<Item> in(String category)
    {
        List<Item> list = new ArrayList<Item>();

        for (Item item : ITEMS)
        {
            if (item.category.equals(category))
            {
                list.add(item);
            }
        }

        return list;
    }

    /** "None" followed by every name in the category, for a setting's choices. */
    public static String[] choices(String category)
    {
        List<Item> items = in(category);
        String[] names = new String[items.size() + 1];
        names[0] = NONE;

        for (int i = 0; i < items.size(); ++i)
        {
            names[i + 1] = items.get(i).name;
        }

        return names;
    }

    /** The cape with this profile id, or null. */
    public static Item capeById(String id)
    {
        for (Item item : in(CAPES))
        {
            if (item.capeId().equals(id))
            {
                return item;
            }
        }

        return null;
    }
}
