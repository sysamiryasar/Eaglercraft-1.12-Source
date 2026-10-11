package net.sythix.modern.mob.client;

import java.util.Map;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBlaze;
import net.minecraft.client.model.ModelCow;
import net.minecraft.client.model.ModelEnderman;
import net.minecraft.client.model.ModelGhast;
import net.minecraft.client.model.ModelIllager;
import net.minecraft.client.model.ModelPig;
import net.minecraft.client.model.ModelRabbit;
import net.minecraft.client.model.ModelVillager;
import net.minecraft.client.model.ModelWolf;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderAbstractHorse;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderGuardian;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderLlama;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSkeleton;
import net.minecraft.client.renderer.entity.RenderSquid;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.sythix.modern.mob.*;

/**
 * How each new mob is drawn. Mobs with a similar 1.12 mob reuse its model with their own
 * texture; the rest use simple box models (ModelBoxes).
 */
public final class MobRenderers
{
    private MobRenderers()
    {
    }

    private static ResourceLocation tex(String name)
    {
        return new ResourceLocation("textures/entity/sythix/" + name + ".png");
    }

    public static void register(RenderManager rm, Map<Class, Render> map)
    {
        map.put(Drowned.class, biped(rm, new ModelZombie(), "drowned"));
        map.put(Piglin.class, biped(rm, new ModelZombie(), "piglin"));
        map.put(PiglinBrute.class, biped(rm, new ModelZombie(), "piglin_brute"));
        map.put(Bogged.class, new RenderSkeleton(rm)
        {
            protected ResourceLocation getEntityTexture(AbstractSkeleton entity)
            {
                return tex("bogged");
            }
        });
        map.put(Pillager.class, new RenderPillager(rm));
        map.put(ZombieNautilus.class, new RenderGuardian(rm)
        {
            protected ResourceLocation getEntityTexture(EntityGuardian entity)
            {
                return tex("zombie_nautilus");
            }
        });
        map.put(GlowSquid.class, new RenderSquid(rm)
        {
            protected ResourceLocation getEntityTexture(EntitySquid entity)
            {
                return tex("glow_squid");
            }
        });
        map.put(TraderLlama.class, new RenderLlama(rm)
        {
            protected ResourceLocation getEntityTexture(EntityLlama entity)
            {
                return tex("trader_llama");
            }
        });
        map.put(Camel.class, new RenderAbstractHorse(rm, 1.25F)
        {
            protected ResourceLocation getEntityTexture(AbstractHorse entity)
            {
                return tex("camel");
            }
        });

        map.put(Ravager.class, new RenderMob<Ravager>(rm, new SafeModels.Bear(), "ravager", 1.45F, 1.2F));
        map.put(Hoglin.class, new RenderMob<Hoglin>(rm, new ModelPig(), "hoglin", 1.55F, 0.9F));
        map.put(Zoglin.class, new RenderMob<Zoglin>(rm, new ModelPig(), "zoglin", 1.55F, 0.9F));
        map.put(Warden.class, new RenderMob<Warden>(rm, new SafeModels.Golem(), "warden", 1.07F, 0.9F));
        map.put(Breeze.class, new RenderMob<Breeze>(rm, new ModelBlaze(), "breeze", 0.95F, 0.5F));
        map.put(Creaking.class, new RenderMob<Creaking>(rm, new ModelEnderman(0.0F), "creaking", 0.93F, 0.5F));
        map.put(Frog.class, new RenderMob<Frog>(rm, new ModelRabbit(), "frog", 0.9F, 0.3F));
        map.put(Fox.class, new RenderMob<Fox>(rm, new ModelWolf(), "fox", 0.85F, 0.4F));
        map.put(Panda.class, new RenderMob<Panda>(rm, new SafeModels.Bear(), "panda", 1.0F, 0.9F));
        map.put(Sniffer.class, new RenderMob<Sniffer>(rm, new SafeModels.Bear(), "sniffer", 1.35F, 1.2F));
        map.put(Goat.class, new RenderMob<Goat>(rm, new ModelCow(), "goat", 0.9F, 0.6F));
        map.put(Armadillo.class, new RenderMob<Armadillo>(rm, new ModelPig(), "armadillo", 0.7F, 0.4F));
        map.put(WanderingTrader.class, new RenderMob<WanderingTrader>(rm, new ModelVillager(0.0F), "wandering_trader", 0.9375F, 0.5F));
        map.put(HappyGhast.class, new RenderMob<HappyGhast>(rm, new ModelGhast(), "happy_ghast", 4.5F, 1.5F));

        for (Object[] o : new Object[][] {{Cod.class, "cod", 1.0F}, {Salmon.class, "salmon", 1.0F}, {TropicalFish.class, "tropical_fish", 1.0F}, {Pufferfish.class, "pufferfish", 1.0F},
            {Tadpole.class, "tadpole", 1.0F}, {Dolphin.class, "dolphin", 1.0F}, {Axolotl.class, "axolotl", 1.0F}, {Turtle.class, "turtle", 1.0F}, {Bee.class, "bee", 1.0F},
            {Strider.class, "strider", 1.0F}, {Phantom.class, "phantom", 1.0F}, {Allay.class, "allay", 1.0F}})
        {
            String name = (String)o[1];
            RenderMob<EntityLiving> r = new RenderMob<EntityLiving>(rm, new ModelBoxes(name), name, (Float)o[2], 0.4F);

            if (o[0] == Allay.class)
            {
                r.withFloatingItem();
            }

            map.put((Class)o[0], r);
        }
    }

    private static <T extends EntityLiving> RenderBiped<T> biped(RenderManager rm, ModelBiped model, String name)
    {
        RenderBiped<T> r = new RenderBiped<T>(rm, model, 0.5F)
        {
            protected ResourceLocation getEntityTexture(T entity)
            {
                return tex(name);
            }
        };
        r.addLayer(new LayerBipedArmor(r));
        return r;
    }

    /** Pillagers: the illager model, holding the crossbow up while attacking. */
    static class RenderPillager extends RenderLiving<Pillager>
    {
        RenderPillager(RenderManager rm)
        {
            super(rm, new ModelIllager(0.0F, 0.0F, 64, 64), 0.5F);
            this.addLayer(new LayerHeldItem(this)
            {
                public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
                {
                    if (((AbstractIllager)entity).getArmPose() != AbstractIllager.IllagerArmPose.CROSSED)
                    {
                        super.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
                    }
                }

                protected void translateToHand(EnumHandSide side)
                {
                    ((ModelIllager)this.livingEntityRenderer.getMainModel()).getArm(side).postRender(0.0625F);
                }
            });
        }

        protected ResourceLocation getEntityTexture(Pillager entity)
        {
            return tex("pillager");
        }

        protected void preRenderCallback(Pillager entity, float partialTickTime)
        {
            net.minecraft.client.renderer.GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
        }
    }
}
