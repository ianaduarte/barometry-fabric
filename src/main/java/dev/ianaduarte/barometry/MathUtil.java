package dev.ianaduarte.barometry;

public class MathUtil {
	private MathUtil() {}
	
	public static float roundn(float f, float n) {
		return (float)Math.round(f / n) * n;
	}
	public static float gradient(float delta, float... values) {
		if(values.length == 0) throw new IllegalArgumentException("Gradient array cannot be empty.");
		if(delta <= 0) return values[0];
		if(delta >= 1) return values[values.length - 1];
		
		int index = (int) (delta * (values.length - 1));
		float t = delta * (values.length - 1) - index;
		return values[index] * (1 - t) + values[index + 1] * t;
	}
	public static double squareGradient(double u, double v, double coeficient) {
		return (1 - Math.min((((1 - u) * u * coeficient) * ((1 - v) * v * coeficient)), 1));
	}
}
