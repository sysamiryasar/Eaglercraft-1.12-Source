package net.sythix.client.setting;

import com.google.gson.JsonElement;
import java.util.function.BooleanSupplier;

/**
 * A single user-adjustable value with a default, a description and JSON (de)serialisation.
 */
public abstract class Setting<T>
{
    private final String name;
    private final String description;
    private final T defaultValue;
    protected T value;

    /** Optional rule that hides this setting in menus (for example "only when background is on"). */
    private BooleanSupplier visibility = () -> true;

    protected Setting(String name, String description, T defaultValue)
    {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public T get()
    {
        return this.value;
    }

    public void set(T newValue)
    {
        this.value = newValue;
    }

    public T getDefault()
    {
        return this.defaultValue;
    }

    public void reset()
    {
        this.set(this.defaultValue);
    }

    public boolean isDefault()
    {
        return this.get().equals(this.defaultValue);
    }

    public boolean isVisible()
    {
        return this.visibility.getAsBoolean();
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier rule)
    {
        this.visibility = rule;
        return (S)this;
    }

    public abstract JsonElement toJson();

    /** Reads a saved value; invalid or missing data leaves the current value unchanged. */
    public abstract void fromJson(JsonElement json);
}
