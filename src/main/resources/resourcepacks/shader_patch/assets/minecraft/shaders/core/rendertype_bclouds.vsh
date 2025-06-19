#version 150

#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ModelOffset;

uniform vec4 ColorModulator;

out vec2 texCoord0;
out float vertexDistance;
out vec4 vertexColor;

void main() {
    vec3 pos = Position * 12 + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    texCoord0 = UV0;
    vertexDistance = length(Position.xz);
    vertexColor = ColorModulator;
}
