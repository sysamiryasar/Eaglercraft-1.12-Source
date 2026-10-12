package net.sythix.client.module.performance;

import java.util.Random;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #3 Particle types, density, visibility and effects.
 */
public class ParticleController extends Module
{
    private final Random random = new Random();
    private final NumberSetting density;
    private final NumberSetting hitMultiplier;
    private final BooleanSetting alwaysSharpness;

    public ParticleController()
    {
        super("Particle Controller", "Particle amount, density, types and hit effects.", Category.PERFORMANCE, true);
        this.add(new GameOptionSetting("Overall particle amount (All, Decreased, Minimal).", GameSettings.Options.PARTICLES));
        this.density = this.add(new NumberSetting("Density", "Percentage of particles that are actually spawned.", 100.0D, 0.0D, 100.0D, 5.0D, "%"));
        this.add(new GameOptionSetting("Particles in the void near bedrock.", GameSettings.Options.VOID_PARTICLES));
        this.add(new GameOptionSetting("Water particles.", GameSettings.Options.WATER_PARTICLES));
        this.add(new GameOptionSetting("Rain splashes on the ground.", GameSettings.Options.RAIN_SPLASH));
        this.add(new GameOptionSetting("Nether portal particles.", GameSettings.Options.PORTAL_PARTICLES));
        this.add(new GameOptionSetting("Potion swirl particles.", GameSettings.Options.POTION_PARTICLES));
        this.add(new GameOptionSetting("Firework particles.", GameSettings.Options.FIREWORK_PARTICLES));
        this.add(new GameOptionSetting("Water and lava dripping from ceilings.", GameSettings.Options.DRIPPING_WATER_LAVA));
        this.hitMultiplier = this.add(new NumberSetting("Hit Particles", "Extra crit particles when you hit something (visual only). 0 = vanilla.", 0.0D, 0.0D, 10.0D, 1.0D, "x"));
        this.alwaysSharpness = this.add(new BooleanSetting("Always Sharpness", "Show enchant sparkles on every hit (visual only).", false));
    }

    /** False when this particle should be skipped because of the density setting. */
    public boolean allow(Particle particle)
    {
        int d = this.density.getInt();
        return d >= 100 || this.random.nextInt(100) < d;
    }

    public void onAttack(Entity target)
    {
        if (!(target instanceof EntityLivingBase))
        {
            return;
        }

        for (int i = 0; i < this.hitMultiplier.getInt(); ++i)
        {
            this.mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.CRIT);
        }

        if (this.alwaysSharpness.isOn())
        {
            this.mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.CRIT_MAGIC);
        }
    }
}
