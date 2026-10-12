package net.sythix.client.cosmetic;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.util.math.MathHelper;

/**
 * The emotes: animated poses for the player model. pose() runs after the normal walking pose, so
 * it decides where the head, body, arms and legs are while an emote plays.
 */
public final class Emotes
{
    public static final String[] ALL = {"Wave", "Salute", "Bow", "Cheer", "Clap", "Point", "Facepalm", "Shrug", "Think", "Flex", "Dab", "T-Pose", "Floss", "Dance", "Victory", "Heart"};
    /** The emote wheel's default eight. */
    public static final String[] WHEEL_DEFAULT = {"Wave", "Salute", "Cheer", "Clap", "Dance", "Floss", "Victory", "Bow"};

    private Emotes()
    {
    }

    /** Applies an emote's pose for this moment (age in ticks). */
    public static void pose(ModelBiped m, String emote, float age)
    {
        float t = age * 0.3F;
        // start from a neutral standing pose so walking doesn't mix in
        neutral(m);

        switch (emote)
        {
            case "Wave":
                m.bipedRightArm.rotateAngleX = (float)Math.PI;
                m.bipedRightArm.rotateAngleZ = 0.4F + MathHelper.sin(t) * 0.4F;
                m.bipedHead.rotateAngleZ = MathHelper.sin(t) * 0.06F;
                break;
            case "Salute":
                m.bipedRightArm.rotateAngleX = -2.2F;
                m.bipedRightArm.rotateAngleY = -0.6F;
                m.bipedRightArm.rotateAngleZ = 0.3F;
                m.bipedHead.rotateAngleX = -0.1F;
                break;
            case "Bow":
            {
                float bow = Math.max(0.0F, MathHelper.sin(Math.min(t * 0.5F, (float)Math.PI))) * 0.6F;
                body(m, bow, 0.0F);
                m.bipedHead.rotateAngleX = bow * 0.6F;
                m.bipedRightArm.rotateAngleX = -bow * 0.3F;
                m.bipedLeftArm.rotateAngleX = -bow * 0.3F;
                m.bipedRightArm.rotateAngleZ = 0.1F;
                m.bipedLeftArm.rotateAngleZ = -0.1F;
                break;
            }
            case "Cheer":
                m.bipedRightArm.rotateAngleX = (float)Math.PI + MathHelper.sin(t) * 0.3F;
                m.bipedLeftArm.rotateAngleX = (float)Math.PI - MathHelper.sin(t) * 0.3F;
                m.bipedRightArm.rotateAngleZ = 0.3F;
                m.bipedLeftArm.rotateAngleZ = -0.3F;
                m.bipedHead.rotateAngleX = -0.25F;
                hop(m, t);
                break;
            case "Clap":
                m.bipedRightArm.rotateAngleX = -1.2F;
                m.bipedLeftArm.rotateAngleX = -1.2F;
                m.bipedRightArm.rotateAngleY = -0.35F - MathHelper.abs(MathHelper.sin(t * 1.5F)) * 0.35F;
                m.bipedLeftArm.rotateAngleY = 0.35F + MathHelper.abs(MathHelper.sin(t * 1.5F)) * 0.35F;
                break;
            case "Point":
                m.bipedRightArm.rotateAngleX = -(float)Math.PI / 2.0F;
                m.bipedRightArm.rotateAngleY = -0.1F;
                m.bipedHead.rotateAngleY = -0.1F;
                break;
            case "Facepalm":
                m.bipedRightArm.rotateAngleX = -2.4F;
                m.bipedRightArm.rotateAngleY = -0.55F;
                m.bipedHead.rotateAngleX = 0.45F;
                m.bipedHead.rotateAngleY = MathHelper.sin(t * 0.7F) * 0.25F;
                break;
            case "Shrug":
            {
                float up = Math.abs(MathHelper.sin(t * 0.5F));
                m.bipedRightArm.rotateAngleX = -0.6F;
                m.bipedLeftArm.rotateAngleX = -0.6F;
                m.bipedRightArm.rotateAngleZ = 0.6F + up * 0.3F;
                m.bipedLeftArm.rotateAngleZ = -0.6F - up * 0.3F;
                m.bipedRightArm.rotationPointY = 2.0F - up;
                m.bipedLeftArm.rotationPointY = 2.0F - up;
                m.bipedHead.rotateAngleZ = 0.15F;
                break;
            }
            case "Think":
                m.bipedRightArm.rotateAngleX = -2.0F;
                m.bipedRightArm.rotateAngleY = -0.9F;
                m.bipedLeftArm.rotateAngleX = -0.9F;
                m.bipedLeftArm.rotateAngleY = 0.6F;
                m.bipedHead.rotateAngleX = -0.2F;
                m.bipedHead.rotateAngleY = 0.3F + MathHelper.sin(t * 0.3F) * 0.1F;
                break;
            case "Flex":
            {
                float squeeze = Math.abs(MathHelper.sin(t * 0.6F)) * 0.3F;
                m.bipedRightArm.rotateAngleZ = 1.5F;
                m.bipedLeftArm.rotateAngleZ = -1.5F;
                m.bipedRightArm.rotateAngleX = -1.4F - squeeze;
                m.bipedLeftArm.rotateAngleX = -1.4F - squeeze;
                m.bipedRightArm.rotateAngleY = -1.2F;
                m.bipedLeftArm.rotateAngleY = 1.2F;
                break;
            }
            case "Dab":
                m.bipedRightArm.rotateAngleX = -2.3F;
                m.bipedRightArm.rotateAngleZ = -0.6F;
                m.bipedLeftArm.rotateAngleX = -1.4F;
                m.bipedLeftArm.rotateAngleY = 0.8F;
                m.bipedHead.rotateAngleX = 0.6F;
                m.bipedHead.rotateAngleY = -0.5F;
                break;
            case "T-Pose":
                m.bipedRightArm.rotateAngleZ = (float)Math.PI / 2.0F;
                m.bipedLeftArm.rotateAngleZ = -(float)Math.PI / 2.0F;
                break;
            case "Floss":
            {
                float swing = MathHelper.sin(t * 1.6F);
                float hips = MathHelper.sin(t * 1.6F + (float)Math.PI / 2.0F);
                m.bipedRightArm.rotateAngleZ = 0.35F + swing * 0.3F;
                m.bipedLeftArm.rotateAngleZ = -0.35F + swing * 0.3F;
                m.bipedRightArm.rotateAngleX = swing > 0.0F ? -0.5F : 0.5F;
                m.bipedLeftArm.rotateAngleX = swing > 0.0F ? -0.5F : 0.5F;
                m.bipedRightLeg.rotateAngleZ = hips * 0.12F;
                m.bipedLeftLeg.rotateAngleZ = hips * 0.12F;
                m.bipedBody.rotateAngleZ = -hips * 0.08F;
                break;
            }
            case "Dance":
            {
                float beat = MathHelper.sin(t * 1.2F);
                body(m, 0.0F, beat * 0.35F);
                m.bipedRightArm.rotateAngleX = -1.6F + beat * 0.8F;
                m.bipedLeftArm.rotateAngleX = -1.6F - beat * 0.8F;
                m.bipedRightArm.rotateAngleZ = 0.4F;
                m.bipedLeftArm.rotateAngleZ = -0.4F;
                m.bipedRightLeg.rotateAngleX = Math.max(0.0F, beat) * -0.6F;
                m.bipedLeftLeg.rotateAngleX = Math.max(0.0F, -beat) * -0.6F;
                m.bipedHead.rotateAngleZ = beat * 0.15F;
                break;
            }
            case "Victory":
            {
                float pump = MathHelper.abs(MathHelper.sin(t * 0.9F));
                m.bipedRightArm.rotateAngleX = (float)Math.PI - 0.2F - pump * 0.4F;
                m.bipedRightArm.rotateAngleZ = 0.15F;
                m.bipedLeftArm.rotateAngleX = -0.3F;
                m.bipedLeftArm.rotateAngleZ = -0.2F;
                m.bipedHead.rotateAngleX = -0.3F;
                hop(m, t * 0.9F);
                break;
            }
            default:
                // Heart: hands together above the head
                m.bipedRightArm.rotateAngleX = (float)Math.PI - 0.15F;
                m.bipedLeftArm.rotateAngleX = (float)Math.PI - 0.15F;
                m.bipedRightArm.rotateAngleZ = -0.45F;
                m.bipedLeftArm.rotateAngleZ = 0.45F;
                m.bipedHead.rotateAngleZ = MathHelper.sin(t * 0.5F) * 0.12F;
        }
    }

    private static void neutral(ModelBiped m)
    {
        m.bipedHead.rotateAngleZ = 0.0F;
        m.bipedBody.rotateAngleX = 0.0F;
        m.bipedBody.rotateAngleY = 0.0F;
        m.bipedBody.rotateAngleZ = 0.0F;
        m.bipedRightArm.rotateAngleX = 0.0F;
        m.bipedRightArm.rotateAngleY = 0.0F;
        m.bipedRightArm.rotateAngleZ = 0.06F;
        m.bipedLeftArm.rotateAngleX = 0.0F;
        m.bipedLeftArm.rotateAngleY = 0.0F;
        m.bipedLeftArm.rotateAngleZ = -0.06F;
        m.bipedRightLeg.rotateAngleX = 0.0F;
        m.bipedRightLeg.rotateAngleY = 0.0F;
        m.bipedRightLeg.rotateAngleZ = 0.0F;
        m.bipedLeftLeg.rotateAngleX = 0.0F;
        m.bipedLeftLeg.rotateAngleY = 0.0F;
        m.bipedLeftLeg.rotateAngleZ = 0.0F;
        m.bipedRightArm.rotationPointX = -5.0F;
        m.bipedRightArm.rotationPointY = 2.0F;
        m.bipedRightArm.rotationPointZ = 0.0F;
        m.bipedLeftArm.rotationPointX = 5.0F;
        m.bipedLeftArm.rotationPointY = 2.0F;
        m.bipedLeftArm.rotationPointZ = 0.0F;
        m.bipedHead.rotationPointY = 0.0F;
        m.bipedHead.rotationPointZ = 0.0F;
        m.bipedRightLeg.rotationPointY = 12.0F;
        m.bipedLeftLeg.rotationPointY = 12.0F;
        m.bipedRightLeg.rotationPointZ = 0.1F;
        m.bipedLeftLeg.rotationPointZ = 0.1F;
    }

    /** Leans (forward) and twists the body, moving the head and arms with it. */
    private static void body(ModelBiped m, float lean, float twist)
    {
        m.bipedBody.rotateAngleX = lean;
        m.bipedBody.rotateAngleY = twist;
        // shoulders follow the twist (like the vanilla attack swing)
        m.bipedRightArm.rotationPointZ = MathHelper.sin(twist) * 5.0F;
        m.bipedRightArm.rotationPointX = -MathHelper.cos(twist) * 5.0F;
        m.bipedLeftArm.rotationPointZ = -MathHelper.sin(twist) * 5.0F;
        m.bipedLeftArm.rotationPointX = MathHelper.cos(twist) * 5.0F;
        m.bipedRightArm.rotateAngleY += twist;
        m.bipedLeftArm.rotateAngleY += twist;
        // leaning forward moves the head and shoulders forward and down
        float forward = MathHelper.sin(lean) * 12.0F;
        float down = (1.0F - MathHelper.cos(lean)) * 12.0F;
        m.bipedHead.rotationPointZ = -forward;
        m.bipedHead.rotationPointY = down;
        m.bipedRightArm.rotationPointZ -= forward * 0.85F;
        m.bipedLeftArm.rotationPointZ -= forward * 0.85F;
        m.bipedRightArm.rotationPointY = 2.0F + down * 0.85F;
        m.bipedLeftArm.rotationPointY = 2.0F + down * 0.85F;
        m.bipedHead.rotateAngleY += twist;
    }

    /** Small bounce on the toes. */
    private static void hop(ModelBiped m, float t)
    {
        float bounce = Math.abs(MathHelper.sin(t)) * 0.12F;
        m.bipedRightLeg.rotateAngleX = -bounce;
        m.bipedLeftLeg.rotateAngleX = -bounce;
    }
}
