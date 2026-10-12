#!/usr/bin/env python3
"""
Writes Sythix's own shader packs (original work, OptiFine shader format, GLSL 120) to
src/main/resources/sythix/shaderpacks/. The client copies them into the game's shaderpacks
folder the first time the Shaders screen is opened.

  Sythix Vibrant - fast: colour grading, soft glow, vignette, waving plants, tinted water
  Sythix Shadows - everything in Vibrant plus sun and moon shadows

Run from the repository root:  python3 branding/shaderpacks.py
"""
import os

OUT = "src/main/resources/sythix/shaderpacks"
VERSION = "5"

# Legacy 1.12 block ids that sway in the wind (OptiFine passes them in mc_Entity.x).
PLANTS = [6, 31, 32, 37, 38, 59, 83, 106, 141, 142, 175, 207]
LEAVES = [18, 161]

HEADER = "#version 120\n"

SHADOW_COMMON = """
const int shadowMapResolution = 2048;
const float shadowDistance = 100.0;
const float sunPathRotation = -25.0;

vec3 distort(vec3 p) {
    float f = length(p.xy) * 0.9 + 0.1;
    return vec3(p.xy / f, p.z * 0.25);
}
"""

WAVE = """
uniform float frameTimeCounter;
uniform float rainStrength;
attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

vec3 wave(vec3 worldPos, vec2 uv) {
    float id = mc_Entity.x;
    bool plant = PLANT_TEST;
    bool leaf = LEAF_TEST;
    if (!(plant || leaf)) return vec3(0.0);
    if (plant && uv.y > mc_midTexCoord.y) return vec3(0.0);   // only the top of plants moves
    float t = frameTimeCounter * (1.6 + rainStrength);
    float s = (leaf ? 0.035 : 0.07) * (1.0 + rainStrength * 1.5);
    return vec3(sin(t + worldPos.x * 0.7 + worldPos.z * 0.3), 0.0, cos(t * 0.8 + worldPos.z * 0.7)) * s
         + vec3(0.0, sin(t * 1.3 + worldPos.x + worldPos.z) * s * 0.4, 0.0);
}
""".replace("PLANT_TEST", " || ".join(f"abs(id - {i}.0) < 0.5" for i in PLANTS)).replace(
    "LEAF_TEST", " || ".join(f"abs(id - {i}.0) < 0.5" for i in LEAVES))

# ---- lit geometry (terrain, entities, hand, water...) ---------------------------------------


def lit_vsh(shadows, waving, water=False):
    s = HEADER + "varying vec2 texcoord;\nvarying vec2 lmcoord;\nvarying vec4 glcolor;\nvarying float ndotl;\n"
    s += "uniform mat4 gbufferModelView;\nuniform mat4 gbufferModelViewInverse;\nuniform vec3 cameraPosition;\nuniform vec3 shadowLightPosition;\n"
    if shadows:
        s += "uniform mat4 shadowModelView;\nuniform mat4 shadowProjection;\nvarying vec3 shadowPos;\n" + SHADOW_COMMON
    if waving:
        s += WAVE
    if water:
        s += "uniform float frameTimeCounter;\nvarying float isWater;\nattribute vec4 mc_Entity;\n"
    s += """
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    vec4 pos = gbufferModelViewInverse * viewPos;
"""
    if waving:
        s += "    pos.xyz += wave(pos.xyz + cameraPosition, texcoord);\n"
    if water:
        s += """    isWater = (abs(mc_Entity.x - 8.0) < 0.5 || abs(mc_Entity.x - 9.0) < 0.5) ? 1.0 : 0.0;
    if (isWater > 0.5) {
        vec3 w = pos.xyz + cameraPosition;
        pos.y += (sin(frameTimeCounter * 1.5 + w.x * 0.6) + cos(frameTimeCounter * 1.1 + w.z * 0.5)) * 0.03 - 0.06;
    }
"""
    s += """    gl_Position = gl_ProjectionMatrix * gbufferModelView * pos;
    gl_FogFragCoord = length(viewPos.xyz);
    vec3 normal = normalize(gl_NormalMatrix * gl_Normal);
    ndotl = dot(normal, normalize(shadowLightPosition));
"""
    if shadows:
        s += """    vec4 sp = shadowProjection * (shadowModelView * pos);
    // push the sample point a little along the normal to avoid shadow acne
    sp.xyz = distort(sp.xyz);
    shadowPos = sp.xyz * 0.5 + 0.5;
    shadowPos.z -= 0.0006 + 0.0012 * (1.0 - clamp(abs(ndotl), 0.0, 1.0));
"""
    s += "}\n"
    return s


def lit_fsh(shadows, entity=False, water=False):
    s = HEADER + "uniform sampler2D texture;\nuniform sampler2D lightmap;\nuniform int worldTime;\nuniform float rainStrength;\n"
    s += "varying vec2 texcoord;\nvarying vec2 lmcoord;\nvarying vec4 glcolor;\nvarying float ndotl;\n"
    if entity:
        s += "uniform vec4 entityColor;\n"
    if water:
        s += "varying float isWater;\n"
    if shadows:
        s += "uniform sampler2D shadowtex0;\nvarying vec3 shadowPos;\n" + SHADOW_COMMON.split("vec3 distort")[0]
        s += """
float shadowAt(vec3 p) {
    if (p.x < 0.0 || p.y < 0.0 || p.x > 1.0 || p.y > 1.0 || p.z > 1.0) return 1.0;
    float texel = 1.0 / float(shadowMapResolution);
    float sum = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            sum += step(p.z, texture2D(shadowtex0, p.xy + vec2(x, y) * texel * 1.5).r);
        }
    }
    return sum / 9.0;
}
"""
    s += """
void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
"""
    if entity:
        s += "    color.rgb = mix(color.rgb, entityColor.rgb, entityColor.a);\n"
    if water:
        s += """    if (isWater > 0.5) {
        color.rgb = mix(color.rgb, vec3(0.10, 0.35, 0.55), 0.35);
        color.a = max(color.a, 0.72);
    }
"""
    if shadows:
        s += """    // sunlight that reaches this spot: shadow map x facing the light x how open the sky is
    float day = (worldTime < 12700 || worldTime > 23250) ? 1.0 : 0.25;
    float sun = clamp(ndotl * 1.4, 0.0, 1.0) * shadowAt(shadowPos);
    float sky = clamp((lmcoord.y - 0.5) * 2.2, 0.0, 1.0) * (1.0 - rainStrength * 0.7);
    color *= texture2D(lightmap, lmcoord);
    // shadowed spots get 60% of the light, sunlit spots a little extra warmth
    color.rgb *= mix(1.0, mix(0.6, 1.0, sun), sky * day);
    color.rgb *= mix(vec3(1.0), vec3(1.08, 1.04, 0.96), sun * sky * day);
"""
    else:
        s += "    color *= texture2D(lightmap, lmcoord);\n"
    s += """    float fog = clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0);
    color.rgb = mix(color.rgb, gl_Fog.color.rgb, fog);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}
"""
    return s


# ---- unlit geometry ------------------------------------------------------------------------

BASIC_VSH = HEADER + """varying vec2 lmcoord;
varying vec4 glcolor;
void main() {
    gl_Position = ftransform();
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    gl_FogFragCoord = length((gl_ModelViewMatrix * gl_Vertex).xyz);
}
"""
BASIC_FSH = HEADER + """uniform sampler2D lightmap;
varying vec2 lmcoord;
varying vec4 glcolor;
void main() {
    vec4 color = glcolor * texture2D(lightmap, lmcoord);
    float fog = clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0);
    color.rgb = mix(color.rgb, gl_Fog.color.rgb, fog);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}
"""
TEXTURED_VSH = HEADER + """varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
void main() {
    gl_Position = ftransform();
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    gl_FogFragCoord = length((gl_ModelViewMatrix * gl_Vertex).xyz);
}
"""
TEXTURED_FSH = HEADER + """uniform sampler2D texture;
uniform sampler2D lightmap;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor * texture2D(lightmap, lmcoord);
    float fog = clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0);
    color.rgb = mix(color.rgb, gl_Fog.color.rgb, fog);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}
"""
SKYBASIC_VSH = HEADER + """varying vec4 glcolor;
void main() {
    gl_Position = ftransform();
    glcolor = gl_Color;
}
"""
SKYBASIC_FSH = HEADER + """varying vec4 glcolor;
void main() {
/* DRAWBUFFERS:0 */
    gl_FragData[0] = glcolor;
}
"""
SKYTEX_VSH = HEADER + """varying vec2 texcoord;
varying vec4 glcolor;
void main() {
    gl_Position = ftransform();
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
}
"""
SKYTEX_FSH = HEADER + """uniform sampler2D texture;
varying vec2 texcoord;
varying vec4 glcolor;
void main() {
/* DRAWBUFFERS:0 */
    gl_FragData[0] = texture2D(texture, texcoord) * glcolor;
}
"""

# ---- shadow pass ---------------------------------------------------------------------------

SHADOW_VSH = HEADER + """uniform mat4 shadowModelView;
uniform mat4 shadowModelViewInverse;
uniform vec3 cameraPosition;
varying vec2 texcoord;
varying vec4 glcolor;
""" + SHADOW_COMMON + WAVE + """
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
    vec4 pos = shadowModelViewInverse * (gl_ModelViewMatrix * gl_Vertex);
    pos.xyz += wave(pos.xyz + cameraPosition, texcoord);
    vec4 p = gl_ProjectionMatrix * (shadowModelView * pos);
    p.xyz = distort(p.xyz);
    gl_Position = p;
}
"""
SHADOW_FSH = HEADER + """uniform sampler2D texture;
varying vec2 texcoord;
varying vec4 glcolor;
void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    if (color.a < 0.1) discard;
    gl_FragData[0] = color;
}
"""

# ---- final: colour grading -----------------------------------------------------------------

FINAL_VSH = HEADER + """varying vec2 texcoord;
void main() {
    gl_Position = ftransform();
    texcoord = gl_MultiTexCoord0.xy;
}
"""
FINAL_FSH = HEADER + """uniform sampler2D gcolor;
uniform float viewWidth;
uniform float viewHeight;
uniform float rainStrength;
uniform int isEyeInWater;
varying vec2 texcoord;

vec3 glow(vec2 uv) {
    vec2 px = vec2(1.0 / viewWidth, 1.0 / viewHeight) * 3.0;
    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            float w = 1.0 / (1.0 + float(x * x + y * y));
            vec3 c = texture2D(gcolor, uv + vec2(x, y) * px).rgb;
            sum += max(c - 0.82, 0.0) * w;
            total += w;
        }
    }
    return sum / total;
}

void main() {
    vec3 c = texture2D(gcolor, texcoord).rgb;
    c += glow(texcoord) * 2.0;
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(luma), c, 1.22 - rainStrength * 0.25);          // saturation
    c = (c - 0.5) * 1.07 + 0.5;                                    // contrast
    c *= vec3(1.03, 1.0, 0.97);                                    // a touch warmer
    if (isEyeInWater == 1) c *= vec3(0.75, 0.95, 1.1);
    vec2 d = texcoord - 0.5;
    c *= 1.0 - dot(d, d) * 0.55;                                   // vignette
    gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
"""


def write(pack, name, text):
    path = os.path.join(OUT, pack, "shaders", name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(text)
    return f"shaders/{name}"


def make(pack, shadows, description):
    files = []
    files.append(write(pack, "gbuffers_basic.vsh", BASIC_VSH))
    files.append(write(pack, "gbuffers_basic.fsh", BASIC_FSH))
    files.append(write(pack, "gbuffers_textured.vsh", TEXTURED_VSH))
    files.append(write(pack, "gbuffers_textured.fsh", TEXTURED_FSH))
    files.append(write(pack, "gbuffers_skybasic.vsh", SKYBASIC_VSH))
    files.append(write(pack, "gbuffers_skybasic.fsh", SKYBASIC_FSH))
    files.append(write(pack, "gbuffers_skytextured.vsh", SKYTEX_VSH))
    files.append(write(pack, "gbuffers_skytextured.fsh", SKYTEX_FSH))
    files.append(write(pack, "gbuffers_textured_lit.vsh", lit_vsh(shadows, False)))
    files.append(write(pack, "gbuffers_textured_lit.fsh", lit_fsh(shadows)))
    files.append(write(pack, "gbuffers_terrain.vsh", lit_vsh(shadows, True)))
    files.append(write(pack, "gbuffers_terrain.fsh", lit_fsh(shadows)))
    files.append(write(pack, "gbuffers_entities.vsh", lit_vsh(shadows, False)))
    files.append(write(pack, "gbuffers_entities.fsh", lit_fsh(shadows, entity=True)))
    files.append(write(pack, "gbuffers_water.vsh", lit_vsh(shadows, False, water=True)))
    files.append(write(pack, "gbuffers_water.fsh", lit_fsh(shadows, water=True)))
    files.append(write(pack, "final.vsh", FINAL_VSH))
    files.append(write(pack, "final.fsh", FINAL_FSH))
    if shadows:
        files.append(write(pack, "shadow.vsh", SHADOW_VSH))
        files.append(write(pack, "shadow.fsh", SHADOW_FSH))
    with open(os.path.join(OUT, pack, "readme.txt"), "w") as f:
        f.write(f"{pack} (version {VERSION})\n{description}\nOriginal shader pack made for Sythix Client. OptiFine shader format.\n")
    files.append("readme.txt")
    return files


if __name__ == "__main__":
    index = []
    for pack, shadows, desc in (("Sythix Vibrant", False, "Fast: colour grading, soft glow, vignette, waving plants and tinted water."),
                                ("Sythix Shadows", True, "Everything in Sythix Vibrant plus sun and moon shadows. Needs a stronger graphics card.")):
        for f in make(pack, shadows, desc):
            index.append(f"{pack}/{f}")
    with open(os.path.join(OUT, "index.txt"), "w") as f:
        f.write("version=" + VERSION + "\n" + "\n".join(index) + "\n")
    print("shader packs written:", len(index), "files")
