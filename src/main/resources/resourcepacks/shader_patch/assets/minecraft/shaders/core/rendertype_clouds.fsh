#version 150

uniform sampler2D Sampler0;

uniform vec2 uvOffset;
uniform vec4 cloudColor;

in vec2 texCoord0;
in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd) {
	if(vertexDistance <= fogStart) return inColor;

	float fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogEnd, vertexDistance) : 1.0;
	inColor.a = mix(inColor.a, 0, fogValue);
	return inColor;
}

void main() {
	vec4 color = texture(Sampler0, texCoord0 + uvOffset) * cloudColor;
	if(color.a < 0.01) discard;
	float d = length(texCoord0) * 512;
	fragColor = linear_fog(color, d, 128, 512);
}
