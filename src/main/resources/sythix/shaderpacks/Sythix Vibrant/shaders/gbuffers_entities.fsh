#version 120
uniform sampler2D texture;
uniform sampler2D lightmap;
uniform int worldTime;
uniform float rainStrength;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying float ndotl;
uniform vec4 entityColor;

void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    color.rgb = mix(color.rgb, entityColor.rgb, entityColor.a);
    color *= texture2D(lightmap, lmcoord);
    float fog = clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0);
    color.rgb = mix(color.rgb, gl_Fog.color.rgb, fog);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}
