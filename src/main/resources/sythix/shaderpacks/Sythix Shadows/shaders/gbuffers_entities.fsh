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
uniform sampler2D shadowtex0;
varying vec3 shadowPos;

const int shadowMapResolution = 2048;
const float shadowDistance = 100.0;
const float sunPathRotation = -25.0;


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

void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    color.rgb = mix(color.rgb, entityColor.rgb, entityColor.a);
    // sunlight that reaches this spot: shadow map x facing the light x how open the sky is
    float day = (worldTime < 12700 || worldTime > 23250) ? 1.0 : 0.25;
    float sun = clamp(ndotl * 1.4, 0.0, 1.0) * shadowAt(shadowPos);
    float sky = clamp((lmcoord.y - 0.5) * 2.2, 0.0, 1.0) * (1.0 - rainStrength * 0.7);
    color *= texture2D(lightmap, lmcoord);
    // shadowed spots get 60% of the light, sunlit spots a little extra warmth
    color.rgb *= mix(1.0, mix(0.6, 1.0, sun), sky * day);
    color.rgb *= mix(vec3(1.0), vec3(1.08, 1.04, 0.96), sun * sky * day);
    float fog = clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0);
    color.rgb = mix(color.rgb, gl_Fog.color.rgb, fog);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}
