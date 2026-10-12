package net.sythix.client.waypoint;

import com.google.gson.JsonObject;

/**
 * One saved location.
 */
public class Waypoint
{
    public String name;
    public int x;
    public int y;
    public int z;
    /** Dimension id: 0 overworld, -1 nether, 1 end. */
    public int dimension;
    public int color;
    public boolean visible = true;
    public String group = "";
    public boolean death;
    public long created;

    public Waypoint(String name, int x, int y, int z, int dimension, int color)
    {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.color = color;
        this.created = System.currentTimeMillis();
    }

    public JsonObject toJson()
    {
        JsonObject o = new JsonObject();
        o.addProperty("name", this.name);
        o.addProperty("x", Integer.valueOf(this.x));
        o.addProperty("y", Integer.valueOf(this.y));
        o.addProperty("z", Integer.valueOf(this.z));
        o.addProperty("dimension", Integer.valueOf(this.dimension));
        o.addProperty("color", String.format("#%06X", this.color & 0xFFFFFF));
        o.addProperty("visible", Boolean.valueOf(this.visible));
        o.addProperty("group", this.group);
        o.addProperty("death", Boolean.valueOf(this.death));
        o.addProperty("created", Long.valueOf(this.created));
        return o;
    }

    public static Waypoint fromJson(JsonObject o)
    {
        Waypoint w = new Waypoint(o.has("name") ? o.get("name").getAsString() : "Waypoint", o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt(), o.has("dimension") ? o.get("dimension").getAsInt() : 0, 0xD81820);

        if (o.has("color"))
        {
            try
            {
                w.color = Integer.parseInt(o.get("color").getAsString().replace("#", ""), 16);
            }
            catch (NumberFormatException ignored)
            {
                // keep default colour
            }
        }

        w.visible = !o.has("visible") || o.get("visible").getAsBoolean();
        w.group = o.has("group") ? o.get("group").getAsString() : "";
        w.death = o.has("death") && o.get("death").getAsBoolean();
        w.created = o.has("created") ? o.get("created").getAsLong() : System.currentTimeMillis();
        return w;
    }
}
