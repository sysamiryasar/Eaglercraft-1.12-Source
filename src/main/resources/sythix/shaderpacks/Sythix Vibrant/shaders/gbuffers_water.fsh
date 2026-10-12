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

void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    if (isWater > 0.5) {
        color.rgb = mix(color.rgb, vec3(0.10, 0.35, 0.55), 0.35);
        color.a = max(color.a, 0.72);
    }
    color *= texture2D(lightmap, lmcoord);
    float fog = clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0);
    color.rgb = mix(color.rgb, gl_Fog.color.rgb, fog);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}
