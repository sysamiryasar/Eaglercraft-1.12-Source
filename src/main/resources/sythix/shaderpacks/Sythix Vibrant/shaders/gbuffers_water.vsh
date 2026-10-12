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
varying vec3 waterPos;
attribute vec4 mc_Entity;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    vec4 pos = gbufferModelViewInverse * viewPos;
    isWater = (abs(mc_Entity.x - 8.0) < 0.5 || abs(mc_Entity.x - 9.0) < 0.5) ? 1.0 : 0.0;
    waterPos = pos.xyz + cameraPosition;
    if (isWater > 0.5) {
        // rolling waves: two crossing swells plus a small fast chop
        vec3 w = waterPos;
        float t = frameTimeCounter;
        pos.y += sin(t * 1.4 + w.x * 0.55 + w.z * 0.2) * 0.045
               + cos(t * 1.1 + w.z * 0.5 - w.x * 0.15) * 0.035
               + sin(t * 3.2 + (w.x + w.z) * 1.7) * 0.012 - 0.09;
    }
    gl_Position = gl_ProjectionMatrix * gbufferModelView * pos;
    gl_FogFragCoord = length(viewPos.xyz);
    vec3 normal = normalize(gl_NormalMatrix * gl_Normal);
    ndotl = dot(normal, normalize(shadowLightPosition));
}
