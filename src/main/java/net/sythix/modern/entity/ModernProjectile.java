package net.sythix.modern.entity;

/**
 * Projectiles added by Sythix. The number is the object type sent to the client when the
 * projectile appears (vanilla uses 1-91).
 */
public interface ModernProjectile
{
    int TRIDENT = 120;
    int WIND_CHARGE = 121;

    int getSpawnType();
}
