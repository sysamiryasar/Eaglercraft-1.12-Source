#version 120
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying float ndotl;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform vec3 shadowLightPosition;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    vec4 pos = gbufferModelViewInverse * viewPos;
    gl_Position = gl_ProjectionMatrix * gbufferModelView * pos;
    gl_FogFragCoord = length(viewPos.xyz);
    vec3 normal = normalize(gl_NormalMatrix * gl_Normal);
    ndotl = dot(normal, normalize(shadowLightPosition));
}
