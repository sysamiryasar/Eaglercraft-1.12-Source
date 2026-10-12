#version 120
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying float ndotl;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform vec3 shadowLightPosition;

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
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    vec4 pos = gbufferModelViewInverse * viewPos;
    pos.xyz += wave(pos.xyz + cameraPosition, texcoord);
    gl_Position = gl_ProjectionMatrix * gbufferModelView * pos;
    gl_FogFragCoord = length(viewPos.xyz);
    vec3 normal = normalize(gl_NormalMatrix * gl_Normal);
    ndotl = dot(normal, normalize(shadowLightPosition));
}
