#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

layout(std140) uniform CloudInfo {
    vec4 CloudColor;
    vec2 CloudUV;
    float CloudHeight;
    float CloudForecast;
    vec2 CloudOffsets;
};

in vec3 Position;
in vec2 UV0;

out vec2 texCoord0;
out vec4 vertexColor;
out float vertexHeight;

void main() {
	//float softness = clamp((((1.0 - UV0.x) * UV0.x * 12.0) * ((1.0 - UV0.y) * UV0.y * 12.0)), 0, 1);
	//float softness_x = abs(UV0.x - 0.5);
	//float softness_y = abs(UV0.y - 0.5);
	
	// Take the maximum of the two to create a square shape that increases towards the edges.
	//float softness = max(softness_x, softness_y) * 2.0;
	//softness = pow(softness, 8);
	vec3 pos = Position * 12.0;
	//pos.y -= softness * 64 * 12;
	pos.y += CloudHeight;
	gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
	
	texCoord0 = UV0;
	vertexColor = CloudColor;
	vertexHeight = CloudHeight;
}
