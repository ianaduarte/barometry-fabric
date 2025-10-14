#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <barometry:math.glsl>
#moj_import <barometry:cloudinfo.glsl>

in vec3 Position;
in vec2 UV0;
in int Layer;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
	vec3 pos = Position;
	int id = gl_InstanceID;
	
	//forecast texture has 1px padding all around to account for movement
	int layer_diameter = CloudLayerDiameter - 2;
	float tile_factor = 1.0 / layer_diameter;
	float half_width = layer_diameter / 2;
	
	vec2 instance_pos = vec2(
		float(id / layer_diameter),
		float(id % layer_diameter)
	);
	pos.xz += instance_pos - vec2(half_width);
	
	vec2 normalized_pos = (instance_pos + UV0) * tile_factor;
	float gradient_factor = 1 - square_gradient(normalized_pos.x, normalized_pos.y, 4.1);
	pos.y -= gradient_factor * 16;
	
	pos *= 16.0;
	pos.y += CloudHeight;
	
	////UVS MUST ACCOUNT FOR THE PADDING!!!! ITS ONE PIXEL!!! NOT ONE TEXEL!!!
	////pixel -> pixel on the actual image (0, CloudLayerWidth]
	////texel -> pixel without padding (0, CloudLayerWidth-2]
	float uv_factor = 1.0 / (CloudLayerDiameter);
	vec2 tile_pos = (instance_pos) * uv_factor + vec2(uv_factor);
	
	////how far from the center of the current chunk the camera is
	vec2 frac_pos = CloudUVOffset * 0.5;
	//texCoord0 = UV0 * uv_factor + tile_pos + frac_pos;
	
	gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
	texCoord0 = (UV0 + frac_pos) * uv_factor + tile_pos;
	vertexColor = CloudColor;
}