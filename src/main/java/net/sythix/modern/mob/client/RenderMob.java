package net.sythix.modern.mob.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.sythix.modern.mob.Armadillo;
import net.sythix.modern.mob.ModernFish;
import net.sythix.modern.mob.Pufferfish;

/**
 * Draws one of the new mobs with a model, a texture from textures/entity/sythix/ and a size.
 * Fish lie on their side on land, pufferfish grow when puffed and armadillos curl up.
 */
public class RenderMob<T extends EntityLiving> extends RenderLiving<T>
{
    private final ResourceLocation texture;
    private final float scale;

    public RenderMob(RenderManager manager, ModelBase model, String name, float scale, float shadow)
    {
        super(manager, model, shadow);
        this.texture = new ResourceLocation("textures/entity/sythix/" + name + ".png");
        this.scale = scale;
    }

    /** Also draws the item the mob holds, floating in front of it (used by the allay). */
    public RenderMob<T> withFloatingItem()
    {
        this.addLayer(new LayerRenderer<EntityLivingBase>()
        {
            public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
            {
                ItemStack stack = entity.getHeldItemMainhand();

                if (stack.isEmpty())
                {
                    return;
                }

                GlStateManager.pushMatrix();
                GlStateManager.translate(0.0F, 1.2F, -0.25F);
                GlStateManager.scale(0.4F, 0.4F, 0.4F);
                Minecraft.getMinecraft().getItemRenderer().renderItem(entity, stack, ItemCameraTransforms.TransformType.GROUND);
                GlStateManager.popMatrix();
            }

            public boolean shouldCombineTextures()
            {
                return false;
            }
        });
        return this;
    }

    protected ResourceLocation getEntityTexture(T entity)
    {
        return this.texture;
    }

    protected void preRenderCallback(T entity, float partialTickTime)
    {
        float s = this.scale;

        if (entity instanceof Pufferfish)
        {
            s *= ((Pufferfish)entity).puffScale(partialTickTime);
        }

        if (entity instanceof Armadillo && ((Armadillo)entity).isRolled())
        {
            GlStateManager.scale(s * 1.1F, s * 0.75F, s * 0.8F);
            return;
        }

        GlStateManager.scale(s, s, s);
    }

    protected void applyRotations(T entity, float ageInTicks, float rotationYaw, float partialTicks)
    {
        super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);

        if (entity instanceof ModernFish && !entity.isInWater())
        {
            GlStateManager.translate(0.1F, 0.1F, 0.0F);
            GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        }
    }
}
