#version 120
uniform sampler2D gcolor;
uniform float viewWidth;
uniform float viewHeight;
uniform float rainStrength;
uniform int isEyeInWater;
varying vec2 texcoord;

vec3 glow(vec2 uv) {
    vec2 px = vec2(1.0 / viewWidth, 1.0 / viewHeight) * 3.0;
    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            float w = 1.0 / (1.0 + float(x * x + y * y));
            vec3 c = texture2D(gcolor, uv + vec2(x, y) * px).rgb;
            sum += max(c - 0.82, 0.0) * w;
            total += w;
        }
    }
    return sum / total;
}

void main() {
    vec3 c = texture2D(gcolor, texcoord).rgb;
    c += glow(texcoord) * 2.0;
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(luma), c, 1.22 - rainStrength * 0.25);          // saturation
    c = (c - 0.5) * 1.07 + 0.5;                                    // contrast
    c *= vec3(1.03, 1.0, 0.97);                                    // a touch warmer
    if (isEyeInWater == 1) c *= vec3(0.75, 0.95, 1.1);
    vec2 d = texcoord - 0.5;
    c *= 1.0 - dot(d, d) * 0.55;                                   // vignette
    gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
