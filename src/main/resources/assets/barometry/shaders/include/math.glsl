#version 150

float remap(float x, float omin, float omax, float nmin, float nmax) {
	return nmin + (x - omin) * (nmax - nmin) / (omax - omin);
}
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

float sat(float x) {
    return clamp(x, 0, 1);
}

float square_gradient(float u, float v, float sq) {
    return clamp(((1.0 - u) * u * sq) * ((1.0 - v) * v * sq), 0, 1);
}
float hypot(float x, float y) {
	return sqrt(x * x + y * y);
}

vec3 hsv2rgb(vec3 c) {
	vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
	vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
	return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}