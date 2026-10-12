#version 120
uniform sampler2D texture;
uniform sampler2D lightmap;
uniform int worldTime;
uniform float rainStrength;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying float ndotl;
varying float isWater;
varying vec3 waterPos;
uniform float frameTimeCounter;

void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    if (isWater > 0.5) {
        // moving ripples drifting across the surface, with bright crests that catch the light
        vec2 w = waterPos.xz;
        float t = frameTimeCounter;
        float r = sin(w.x * 1.9 + t * 2.1) * cos(w.y * 1.6 - t * 1.7)
                + sin((w.x - w.y) * 1.1 + t * 1.3) * 0.7
                + sin((w.x + w.y) * 3.3 - t * 3.0) * 0.3;
        float crest = smoothstep(1.1, 1.8, r);
        color.rgb = mix(color.rgb, vec3(0.10, 0.35, 0.55), 0.35);
        color.rgb *= 0.88 + 0.12 * r;
        color.rgb += vec3(0.55, 0.65, 0.7) * crest * 0.35;
        color.a = max(color.a, 0.72);
    }
    color *= texture2D(lightmap, lmcoord);
    float fog = clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0);
    color.rgb = mix(color.rgb, gl_Fog.color.rgb, fog);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}
