#version 120
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying float ndotl;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform vec3 shadowLightPosition;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;
varying vec3 shadowPos;

const int shadowMapResolution = 2048;
const float shadowDistance = 100.0;
const float sunPathRotation = -25.0;

vec3 distort(vec3 p) {
    float f = length(p.xy) * 0.9 + 0.1;
    return vec3(p.xy / f, p.z * 0.25);
}

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
    vec4 sp = shadowProjection * (shadowModelView * pos);
    // push the sample point a little along the normal to avoid shadow acne
    sp.xyz = distort(sp.xyz);
    shadowPos = sp.xyz * 0.5 + 0.5;
    shadowPos.z -= 0.0006 + 0.0012 * (1.0 - clamp(abs(ndotl), 0.0, 1.0));
}
