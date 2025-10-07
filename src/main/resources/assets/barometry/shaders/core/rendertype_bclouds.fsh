#version 150

#moj_import <minecraft:fog.glsl>

layout(std140) uniform CloudInfo {
	vec4 CloudColor;
	vec2 CloudUV;
	float CloudHeight;
	float CloudForecast;
	vec2 CloudOffsets;
};
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord0;
in vec4 vertexColor;
in float vertexHeight;

out vec4 fragColor;


float spline_remap(float t, float x1, float y1, float x2, float y2) {
	if(x1 == x2 || x1 == 0.0 || x2 == 0.0 || x1 == 1.0 || x2 == 1.0) {
		if (t <= x1) return t * (y1 / x1);
		if (t <= x2) return y1 + (t - x1) * (y2 - y1) / (x2 - x1);
		return y2 + (t - x2) * (1.0 - y2) / (1.0 - x2);
	}

	float den = (x1 * (x1 - 1.0) * x2 * (x2 - 1.0) * (x1 - x2));
	if(abs(den) < 0.00001) {
		if (t <= x1) return t * (y1 / x1);
		if (t <= x2) return y1 + (t - x1) * (y2 - y1) / (x2 - x1);
		return y2 + (t - x2) * (1.0 - y2) / (1.0 - x2);
	}

	float L1 = t * (t - x2) * (t - 1.0) / (x1 * (x1 - x2) * (x1 - 1.0));
	float L2 = t * (t - x1) * (t - 1.0) / (x2 * (x2 - x1) * (x2 - 1.0));
	float L3 = t * (t - x1) * (t - x2) / ((1.0 - x1) * (1.0 - x2));

	return y1 * L1 + y2 * L2 + L3;
}
float floorn(float x, float n) {
    return floor(x / n) * n;
}
float ceiln(float x, float n) {
    return (floor(x / n) + 1) * n;
}
float square_gradient(float u, float v, float sq) {
    return clamp(((1.0 - u) * u * sq) * ((1.0 - v) * v * sq), 0, 1);
}
void main() {
	float sg1 = 1 - pow(square_gradient(texCoord0.x, texCoord0.y, 4.5), 1.75);

	float fc = mix(0.201, 0.48, clamp(CloudForecast, 0, 1));

    float scaledModOffset = CloudOffsets.y * 256.0;
    float cm = mix(
        texture(Sampler1, texCoord0 + CloudUV + vec2(CloudOffsets.x, floorn(CloudOffsets.y, 1 / 256f))).r,
        texture(Sampler1, texCoord0 + CloudUV + vec2(CloudOffsets.x, ceiln(CloudOffsets.y, 1 / 256f))).r,
        fract(scaledModOffset)
    );
	float c1 = (texture(Sampler0, texCoord0 + CloudUV + vec2(CloudOffsets.x, 0)).r * cm) + cm * 0.0625;

	float col3 = clamp(
		spline_remap(
			mix(c1, 0, pow(sg1, 2)),
			mix(0.20, 0.08, sg1), 1.0,
			mix(fc, 0.4, sg1), 0.0
		),
		0.0,
		1.0
	);
	vec4 color = vec4(1, 1, 1, col3) * vertexColor;
	if(color.a < 0.01) discard;

	float sg2 = 1 - square_gradient(texCoord0.x, texCoord0.y, 4.8);
	float dst = sg2 * 1024;
	color.a *= 1.0 - linear_fog_value(dst, 0.0, FogCloudsEnd);
	fragColor = color;
}
