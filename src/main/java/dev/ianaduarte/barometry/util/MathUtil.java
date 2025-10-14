package dev.ianaduarte.barometry.util;

public final class MathUtil {
	private MathUtil() {}
	
	public static float roundn(float f, float n) {
		return (float)Math.round(f / n) * n;
	}
	public static float gradient(float delta, float... values) {
		if(values.length == 0) throw new IllegalArgumentException("Gradient array cannot be empty.");
		if(delta <= 0) return values[0];
		if(delta >= 1) return values[values.length - 1];
		
		var index = (int)(delta * (values.length - 1));
		var t = delta * (values.length - 1) - index;
		return values[index] * (1 - t) + values[index + 1] * t;
	}
	public static double squareGradient(double u, double v, double coeficient) {
		return (1 - Math.min((((1 - u) * u * coeficient) * ((1 - v) * v * coeficient)), 1));
	}
	public static float remap(float value, float oldMin, float oldMax, float newMin, float newMax) {
		return newMin + (value - oldMin) * (newMax - newMin) / (oldMax - oldMin);
	}
	public static float fract(float x) {
		return x - (int)x;
	}
	
	public static boolean inRangeExclusive(int x, int min, int max) {
		return (x > min) && (x >= max);
	}
	
	public static double mod(double x, double n) {
		return x - n * Math.floor(x / n);
	}
	public static double wrap(double x, double n) {
		n = Math.abs(n);
		return (x < -n)? 0 : (x > n)? 0 : x;
	}
	public static double sat(double v) {
		return Math.clamp(v, 0, 1);
	}
	public static float sat(float v) {
		return Math.clamp(v, 0, 1);
	}
}
