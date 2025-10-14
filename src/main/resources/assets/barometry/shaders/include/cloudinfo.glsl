#version 150

layout(std140) uniform CloudInfo {
    vec4 CloudColor;
    vec2 CloudUVOffset;
    vec2 CloudPos;
    float CloudLayerOffset;
    float CloudHeight;
    int CloudLayerDiameter;
    int CloudLayerCount;
};
uniform sampler2D CloudForecast;
uniform sampler2D CloudCoverage;