#version 120
uniform mat4 shadowModelView;
uniform mat4 shadowModelViewInverse;
uniform vec3 cameraPosition;
varying vec2 texcoord;
varying vec4 glcolor;

const int shadowMapResolution = 2048;
const float shadowDistance = 100.0;
const float sunPathRotation = -25.0;

vec3 distort(vec3 p) {
    float f = length(p.xy) * 0.9 + 0.1;
    return vec3(p.xy / f, p.z * 0.25);
}

uniform float frameTimeCounter;
uniform float rainStrength;
attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

vec3 wave(vec3 worldPos, vec2 uv) {
    float id = mc_Entity.x;
    bool plant = abs(id - 6.0) < 0.5 || abs(id - 31.0) < 0.5 || abs(id - 32.0) < 0.5 || abs(id - 37.0) < 0.5 || abs(id - 38.0) < 0.5 || abs(id - 59.0) < 0.5 || abs(id - 83.0) < 0.5 || abs(id - 106.0) < 0.5 || abs(id - 141.0) < 0.5 || abs(id - 142.0) < 0.5 || abs(id - 175.0) < 0.5 || abs(id - 207.0) < 0.5;
    bool leaf = abs(id - 18.0) < 0.5 || abs(id - 161.0) < 0.5;
    if (!(plant || leaf)) return vec3(0.0);
    if (plant && uv.y > mc_midTexCoord.y) return vec3(0.0);   // only the top of plants moves
    float t = frameTimeCounter * (1.6 + rainStrength);
    float s = (leaf ? 0.035 : 0.07) * (1.0 + rainStrength * 1.5);
    return vec3(sin(t + worldPos.x * 0.7 + worldPos.z * 0.3), 0.0, cos(t * 0.8 + worldPos.z * 0.7)) * s
         + vec3(0.0, sin(t * 1.3 + worldPos.x + worldPos.z) * s * 0.4, 0.0);
}

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
    vec4 pos = shadowModelViewInverse * (gl_ModelViewMatrix * gl_Vertex);
    pos.xyz += wave(pos.xyz + cameraPosition, texcoord);
    vec4 p = gl_ProjectionMatrix * (shadowModelView * pos);
    p.xyz = distort(p.xyz);
    gl_Position = p;
}
