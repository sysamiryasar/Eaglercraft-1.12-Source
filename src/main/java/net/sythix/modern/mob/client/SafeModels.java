package net.sythix.modern.mob.client;

import net.minecraft.client.model.ModelIronGolem;
import net.minecraft.client.model.ModelPolarBear;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/**
 * 1.12 models that read data from one specific mob class, changed so other mobs can use them.
 */
public final class SafeModels
{
    private SafeModels()
    {
    }

    /** The polar bear's body without its standing-up animation. */
    public static class Bear extends ModelPolarBear
    {
        public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn)
        {
            this.head.rotateAngleX = headPitch * 0.017453292F;
            this.head.rotateAngleY = netHeadYaw * 0.017453292F;
            this.body.rotateAngleX = ((float)Math.PI / 2F);
            this.leg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.leg2.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
            this.leg3.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
            this.leg4.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        }
    }

    /** The iron golem's body; the arms swing when walking and when the mob attacks. */
    public static class Golem extends ModelIronGolem
    {
        public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTickTime)
        {
            float swing = entity.getSwingProgress(partialTickTime);

            if (swing > 0.0F)
            {
                this.ironGolemRightArm.rotateAngleX = -2.0F + 1.5F * MathHelper.sin(swing * (float)Math.PI);
                this.ironGolemLeftArm.rotateAngleX = this.ironGolemRightArm.rotateAngleX;
            }
            else
            {
                this.ironGolemRightArm.rotateAngleX = (-0.2F + 1.5F * triangle(limbSwing, 13.0F)) * limbSwingAmount;
                this.ironGolemLeftArm.rotateAngleX = (-0.2F - 1.5F * triangle(limbSwing, 13.0F)) * limbSwingAmount;
            }
        }

        private static float triangle(float x, float period)
        {
            return (Math.abs(x % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
        }
    }
}
