package net.sythix.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A number shown as a slider, clamped to [min, max] and rounded to step.
 */
public class NumberSetting extends Setting<Double>
{
    private final double min;
    private final double max;
    private final double step;
    private final String unit;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step, String unit)
    {
        super(name, description, Double.valueOf(defaultValue));
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
    }

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step)
    {
        this(name, description, defaultValue, min, max, step, "");
    }

    public void set(Double newValue)
    {
        double v = Math.max(this.min, Math.min(this.max, newValue.doubleValue()));

        if (this.step > 0.0D)
        {
            v = this.min + Math.round((v - this.min) / this.step) * this.step;
            v = Math.max(this.min, Math.min(this.max, v));
        }

        super.set(Double.valueOf(v));
    }

    public double getMin()
    {
        return this.min;
    }

    public double getMax()
    {
        return this.max;
    }

    public float getFloat()
    {
        return this.value.floatValue();
    }

    public int getInt()
    {
        return (int)Math.round(this.value.doubleValue());
    }

    /** Position of the value along the slider, 0..1. */
    public double getPercent()
    {
        return this.max == this.min ? 0.0D : (this.value.doubleValue() - this.min) / (this.max - this.min);
    }

    public void setPercent(double percent)
    {
        this.set(Double.valueOf(this.min + (this.max - this.min) * Math.max(0.0D, Math.min(1.0D, percent))));
    }

    public String getDisplayValue()
    {
        double v = this.value.doubleValue();
        String text = this.step >= 1.0D ? String.valueOf((long)Math.round(v)) : (this.step >= 0.1D ? String.format("%.1f", v) : String.format("%.2f", v));
        return text + this.unit;
    }

    public JsonElement toJson()
    {
        return new JsonPrimitive(this.value);
    }

    public void fromJson(JsonElement json)
    {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber())
        {
            this.set(Double.valueOf(json.getAsDouble()));
        }
    }
}
