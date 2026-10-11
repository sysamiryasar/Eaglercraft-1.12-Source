package net.sythix.modern.mob.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * A simple box model for the new mobs that have no similar 1.12 model (fish, bee, turtle...).
 * The boxes come from assets/minecraft/sythix/mob_models.json, which branding/modern_assets.py
 * writes together with the matching placeholder textures.
 *
 * Each part has a role that decides how it moves: head (looks around), tail / tailx (sways
 * sideways / up and down), leg0-leg3 (walks like a four-legged mob), legl / legr (walks like a
 * two-legged mob), wingl / wingr (flaps), static.
 */
public class ModelBoxes extends ModelBase
{
    private static JsonObject models;

    private final List<ModelRenderer> roots = new ArrayList<ModelRenderer>();
    private final List<ModelRenderer> parts = new ArrayList<ModelRenderer>();
    private final List<String> roles = new ArrayList<String>();
    private final List<float[]> rest = new ArrayList<float[]>();

    public ModelBoxes(String name)
    {
        JsonObject model = load().getAsJsonObject(name);
        JsonArray tex = model.getAsJsonArray("tex");
        this.textureWidth = tex.get(0).getAsInt();
        this.textureHeight = tex.get(1).getAsInt();
        Map<String, ModelRenderer> byName = new HashMap<String, ModelRenderer>();

        for (JsonElement e : model.getAsJsonArray("parts"))
        {
            JsonObject p = e.getAsJsonObject();
            float[] uv = floats(p.getAsJsonArray("uv"));
            float[] box = floats(p.getAsJsonArray("box"));
            float[] pivot = floats(p.getAsJsonArray("pivot"));
            ModelRenderer r = new ModelRenderer(this, (int)uv[0], (int)uv[1]);
            r.addBox(box[0], box[1], box[2], (int)box[3], (int)box[4], (int)box[5]);
            r.setRotationPoint(pivot[0], pivot[1], pivot[2]);

            if (p.has("rot"))
            {
                float[] rot = floats(p.getAsJsonArray("rot"));
                r.rotateAngleX = rot[0] * 0.017453292F;
                r.rotateAngleY = rot[1] * 0.017453292F;
                r.rotateAngleZ = rot[2] * 0.017453292F;
            }

            String parent = p.has("parent") ? p.get("parent").getAsString() : null;

            if (parent != null && byName.containsKey(parent))
            {
                byName.get(parent).addChild(r);
            }
            else
            {
                this.roots.add(r);
            }

            if (p.has("name"))
            {
                byName.put(p.get("name").getAsString(), r);
            }

            this.parts.add(r);
            this.roles.add(p.has("role") ? p.get("role").getAsString() : "static");
            this.rest.add(new float[] {r.rotateAngleX, r.rotateAngleY, r.rotateAngleZ});
        }
    }

    private static float[] floats(JsonArray a)
    {
        float[] f = new float[a.size()];

        for (int i = 0; i < f.length; ++i)
        {
            f[i] = a.get(i).getAsFloat();
        }

        return f;
    }

    private static synchronized JsonObject load()
    {
        if (models == null)
        {
            try (Reader r = new InputStreamReader(ModelBoxes.class.getResourceAsStream("/assets/minecraft/sythix/mob_models.json"), StandardCharsets.UTF_8))
            {
                models = new JsonParser().parse(r).getAsJsonObject();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Sythix mob models are missing", e);
            }
        }

        return models;
    }

    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);

        if (this.isChild)
        {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
        }

        for (ModelRenderer r : this.roots)
        {
            r.render(scale);
        }

        if (this.isChild)
        {
            GlStateManager.popMatrix();
        }
    }

    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn)
    {
        float walk = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        float sway = MathHelper.sin(ageInTicks * 0.45F) * (0.25F + limbSwingAmount);
        float flap = MathHelper.cos(ageInTicks * 1.6F) * 0.6F;

        for (int i = 0; i < this.parts.size(); ++i)
        {
            ModelRenderer r = this.parts.get(i);
            float[] base = this.rest.get(i);
            r.rotateAngleX = base[0];
            r.rotateAngleY = base[1];
            r.rotateAngleZ = base[2];
            String role = this.roles.get(i);

            if ("head".equals(role))
            {
                r.rotateAngleX += headPitch * 0.017453292F;
                r.rotateAngleY += netHeadYaw * 0.017453292F;
            }
            else if ("tail".equals(role))
            {
                r.rotateAngleY += sway;
            }
            else if ("tailx".equals(role))
            {
                r.rotateAngleX += sway;
            }
            else if ("leg0".equals(role) || "leg3".equals(role) || "legl".equals(role))
            {
                r.rotateAngleX += walk;
            }
            else if ("leg1".equals(role) || "leg2".equals(role) || "legr".equals(role))
            {
                r.rotateAngleX -= walk;
            }
            else if ("wingl".equals(role))
            {
                r.rotateAngleZ += flap;
            }
            else if ("wingr".equals(role))
            {
                r.rotateAngleZ -= flap;
            }
        }
    }
}
