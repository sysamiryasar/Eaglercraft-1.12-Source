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
varying float isWater;
attribute vec4 mc_Entity;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    vec4 pos = gbufferModelViewInverse * viewPos;
    isWater = (abs(mc_Entity.x - 8.0) < 0.5 || abs(mc_Entity.x - 9.0) < 0.5) ? 1.0 : 0.0;
    if (isWater > 0.5) {
        vec3 w = pos.xyz + cameraPosition;
        pos.y += (sin(frameTimeCounter * 1.5 + w.x * 0.6) + cos(frameTimeCounter * 1.1 + w.z * 0.5)) * 0.03 - 0.06;
    }
    gl_Position = gl_ProjectionMatrix * gbufferModelView * pos;
    gl_FogFragCoord = length(viewPos.xyz);
    vec3 normal = normalize(gl_NormalMatrix * gl_Normal);
    ndotl = dot(normal, normalize(shadowLightPosition));
}
