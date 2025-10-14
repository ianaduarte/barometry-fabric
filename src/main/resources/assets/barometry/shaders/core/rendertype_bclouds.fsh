#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <barometry:math.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <barometry:cloudinfo.glsl>

//uniform sampler2D Sampler0;
//uniform sampler2D Sampler1;
//
in vec2 texCoord0;
in vec4 vertexColor;
//in float vertexHeight;
//
out vec4 fragColor;
//

vec4 permute(vec4 x){return mod(((x*34.0)+1.0)*x, 289.0);}
vec4 taylorInvSqrt(vec4 r){return 1.79284291400159 - 0.85373472095314 * r;}
vec3 fade(vec3 t) {return t*t*t*(t*(t*6.0-15.0)+10.0);}

float cnoise(vec3 P){
	vec3 Pi0 = floor(P); // Integer part for indexing
	vec3 Pi1 = Pi0 + vec3(1.0); // Integer part + 1
	Pi0 = mod(Pi0, 289.0);
	Pi1 = mod(Pi1, 289.0);
	vec3 Pf0 = fract(P); // Fractional part for interpolation
	vec3 Pf1 = Pf0 - vec3(1.0); // Fractional part - 1.0
	vec4 ix = vec4(Pi0.x, Pi1.x, Pi0.x, Pi1.x);
	vec4 iy = vec4(Pi0.yy, Pi1.yy);
	vec4 iz0 = Pi0.zzzz;
	vec4 iz1 = Pi1.zzzz;
	
	vec4 ixy = permute(permute(ix) + iy);
	vec4 ixy0 = permute(ixy + iz0);
	vec4 ixy1 = permute(ixy + iz1);
	
	vec4 gx0 = ixy0 / 7.0;
	vec4 gy0 = fract(floor(gx0) / 7.0) - 0.5;
	gx0 = fract(gx0);
	vec4 gz0 = vec4(0.5) - abs(gx0) - abs(gy0);
	vec4 sz0 = step(gz0, vec4(0.0));
	gx0 -= sz0 * (step(0.0, gx0) - 0.5);
	gy0 -= sz0 * (step(0.0, gy0) - 0.5);
	
	vec4 gx1 = ixy1 / 7.0;
	vec4 gy1 = fract(floor(gx1) / 7.0) - 0.5;
	gx1 = fract(gx1);
	vec4 gz1 = vec4(0.5) - abs(gx1) - abs(gy1);
	vec4 sz1 = step(gz1, vec4(0.0));
	gx1 -= sz1 * (step(0.0, gx1) - 0.5);
	gy1 -= sz1 * (step(0.0, gy1) - 0.5);
	
	vec3 g000 = vec3(gx0.x,gy0.x,gz0.x);
	vec3 g100 = vec3(gx0.y,gy0.y,gz0.y);
	vec3 g010 = vec3(gx0.z,gy0.z,gz0.z);
	vec3 g110 = vec3(gx0.w,gy0.w,gz0.w);
	vec3 g001 = vec3(gx1.x,gy1.x,gz1.x);
	vec3 g101 = vec3(gx1.y,gy1.y,gz1.y);
	vec3 g011 = vec3(gx1.z,gy1.z,gz1.z);
	vec3 g111 = vec3(gx1.w,gy1.w,gz1.w);
	
	vec4 norm0 = taylorInvSqrt(vec4(dot(g000, g000), dot(g010, g010), dot(g100, g100), dot(g110, g110)));
	g000 *= norm0.x;
	g010 *= norm0.y;
	g100 *= norm0.z;
	g110 *= norm0.w;
	vec4 norm1 = taylorInvSqrt(vec4(dot(g001, g001), dot(g011, g011), dot(g101, g101), dot(g111, g111)));
	g001 *= norm1.x;
	g011 *= norm1.y;
	g101 *= norm1.z;
	g111 *= norm1.w;
	
	float n000 = dot(g000, Pf0);
	float n100 = dot(g100, vec3(Pf1.x, Pf0.yz));
	float n010 = dot(g010, vec3(Pf0.x, Pf1.y, Pf0.z));
	float n110 = dot(g110, vec3(Pf1.xy, Pf0.z));
	float n001 = dot(g001, vec3(Pf0.xy, Pf1.z));
	float n101 = dot(g101, vec3(Pf1.x, Pf0.y, Pf1.z));
	float n011 = dot(g011, vec3(Pf0.x, Pf1.yz));
	float n111 = dot(g111, Pf1);
	
	vec3 fade_xyz = fade(Pf0);
	vec4 n_z = mix(vec4(n000, n100, n010, n110), vec4(n001, n101, n011, n111), fade_xyz.z);
	vec2 n_yz = mix(n_z.xy, n_z.zw, fade_xyz.y);
	float n_xyz = mix(n_yz.x, n_yz.y, fade_xyz.x);
	return 2.2 * n_xyz;
}
float fbm(vec2 x, float t) {
	float v = 0.0;
	float a = 0.5;
	vec2 shift = vec2(100);
	// Rotate to reduce axial bias
	mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.50));
	for (int i = 0; i < 6; ++i) {
		v += a * cnoise(vec3(x, t));
		x = rot * x * 2.0 + shift;
		a *= 0.5;
	}
	return v;
}
float noise(vec2 p, float t) {
	return sat((fbm(p * 16, t) + 1) * 0.5);
}
vec2 floorin(vec2 v, float n) {
	float f = 1 / n;
	return vec2(
		floor(v.x * n) * f,
		floor(v.y * n) * f
	);
}


//float steppedBlendY(vec2 texCoordBase, float offset, float step) {
//	float factor = offset / step;
//
//	return mix(
//		fbm(floorn(texCoordBase + vec2(0, floor(factor) * step), 1.0 / 256)),
//		fbm(floorn(texCoordBase + vec2(0, ceil (factor) * step), 1.0 / 256)),
//		fract(offset * step)
//	);
//}

void main() {
	//float sg1 = 1 - pow(square_gradient(texCoord0.x, texCoord0.y, 4.5), 1.75);
//
	//float fc = mix(0.201, 0.48, sat(texture(CloudForecast, texCoord0, 0).r));
//
    //float scaledModOffset = CloudOffsets.y * 256.0;
    //float cm = steppedBlendY(texCoord0 + vec2(CloudLayerOffset, 0), CloudLayerOffset * 0.5, 256);
	//float c1 = sat((cnoise(texCoord0 + vec2(CloudLayerOffset, 0)) * cm) + cm * 0.0625);
//
	//vec2 offset = vec2(CloudLayerOffset, 0);
	//vec2 offset = vec2(0);
	//vec2 uv = floorn(texCoord0 + offset, 1.0 / 256);
	//float cm = steppedBlendY(uv, CloudLayerOffset * 0.5, 256);
	//float col3 = sat((noise(uv) * cm) + cm * 0.625);
	//float col3 = sat(
	//	spline_remap(
	//		mix(c1, 0, pow(sg1, 2)),
	//		mix(0.20, 0.08, sg1), 1.0,
	//		mix(fc, 0.4, sg1), 0.0
	//	)
	//);
	//vec4 color = vec4(1, 1, 1, col3) * vertexColor;
	//vec4 color = vec4(texture(CloudForecast, texCoord0, 0).r) * vertexColor;
	//color.a = 1;
	//float gradient_factor = square_gradient(texCoord0.x, texCoord0.y, 5.0);
	vec2 uv = texCoord0 + vec2(CloudLayerOffset * 0.2, 0);
	uv = floorin(uv, 256) + CloudPos / 256;
	
	float coverage = texture(CloudCoverage, uv).r;
	float col3 = noise(uv, CloudLayerOffset * 0.05) * 0.5;
	col3 = spline_remap(
		col3,
		0.09, 1.0,
		mix(0.17, 0.30, coverage), 0.0
	);
	
	
	
	vec4 color = vertexColor;
	color.a *= col3;
	//color.a *= gradient_factor;
	
	float distance_gradient = 1 - square_gradient(texCoord0.x, texCoord0.y, 4.8);
	color.a *= 1.0 - linear_fog_value(distance_gradient * 1024, 0.0, FogCloudsEnd);

	if(color.a < 0.01) discard;
	fragColor = color;
}
