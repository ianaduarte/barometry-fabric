package dev.ianaduarte.barometry.weather;

import dev.ianaduarte.barometry.util.MathUtil;
import it.unimi.dsi.fastutil.longs.LongDoubleMutablePair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.storage.LevelData;
import org.joml.Vector2d;

import java.util.*;

public class WeatherSystem {
	private final RandomSource random;
	private final LevelData levelData;
	
	private final ImprovedNoise coverageNoise;
	private final PerlinNoise   rainNoise;
	private final PerlinNoise   thunderNoise;
	
	private final Map<Vector2d, LongDoubleMutablePair>[] noiseCaches;
	
	public WeatherSystem(ServerLevel level) {
		this.random = RandomSource.create(level.getSeed());
		this.levelData = level.getLevelData();
		this.coverageNoise = new ImprovedNoise(this.random);
		this.rainNoise = PerlinNoise.create(this.random, List.of(-2, 0));
		this.thunderNoise = PerlinNoise.create(this.random, List.of(-3, 0));
		this.noiseCaches = new Map[]{
			new HashMap<>(),
			new HashMap<>(),
			new HashMap<>(),
		};
	}
	public float getRainLevel(double x, double z) {
		var coverageNoise = this.getCoverageNoise(x, z);
		
		return this.getRainNoise(x, z) * coverageNoise;
	}
	public float getThunderLevel(double x, double z) {
		var coverageNoise = this.getCoverageNoise(x, z);
		
		return this.getThunderNoise(x, z) * coverageNoise;
	}
	public CoverageType getCoverageType(double x, double y) {
		double noise = this.getCoverageNoise(x, y);
		return CoverageType.VALUES[(int)(noise * (CoverageType.VALUES.length - 1))];
	}
	
	private double getNoise(int index, double x, double y, double t) {
		Vector2d coord = new Vector2d(x, y);
		LongDoubleMutablePair cachedNoise = this.noiseCaches[index].get(coord);
		if(cachedNoise != null && cachedNoise.leftLong() == this.levelData.getGameTime()) return cachedNoise.rightDouble();
		
		double noise = switch(index) {
			case 0 -> {
				//double n0 = this.coverageNoise.noise(coord.x * 0.0075, coord.y * 0.0075, t * 0.25);
				double n1 = this.coverageNoise.noise(coord.x * 0.0075, coord.y * 0.0075, t * 0.5);
				double n2 = this.coverageNoise.noise(coord.x * 0.0312, coord.y * 0.0312, t * 0.75);
				double n3 = this.coverageNoise.noise(coord.x * 0.0468, coord.y * 0.0468, t);
				//n3 = MathUtil.sat((n3 + 1) * 0.5);
				//n3 = (n3 * n3);
				//n1 = ((n1 + n2) + 1) * 0.5;
				//yield Math.sqrt(MathUtil.sat(n1 * n3));
				//n0 = (n0 * 0.5) + 0.5;
				//n0 = n0 * n0;
				//n2 = (n2 + n0) * 0.5;
				n1 = (Math.clamp(n1, 0.5, 0.7) - 0.5) / 0.2;
				n2 = (n1 * n2);
				n2 = (n2 + n3) * 0.5;
				
				yield (n2 * 0.5) + 0.5;
			}
			case 1 -> this.rainNoise    .getValue(coord.x * 0.125, coord.y * 0.125, t);
			case 2 -> this.thunderNoise .getValue(coord.x * 0.250, coord.y * 0.250, t);
			
			//unreachable because `noiseCaches` access would have already thrown an exception
			default -> throw new IllegalStateException("unreachable");
		};
		
		if(cachedNoise == null) this.noiseCaches[index].put(coord, new LongDoubleMutablePair(this.levelData.getGameTime(), noise));
		else cachedNoise.left(this.levelData.getGameTime()).right(noise);
		return noise;
	}
	
	public float getCoverageNoise(double x, double y) {
		double noise = this.getNoise(0, x, y, levelData.getGameTime() * 2.5e-3f);
		return (Math.clamp((float)noise, 0.4f, 0.60f) - 0.4f) * 5;
	}
	public float getRainNoise(double x, double y) {
		double noise = this.getNoise(1, x, y, levelData.getGameTime() * 6.25e-3);
		return Math.clamp((float)noise, 0f, .25f) * 4;
	}
	public float getThunderNoise(double x, double y) {
		double noise = this.getNoise(2, x, y, levelData.getGameTime() * 5e-3f);
		return (Math.clamp((float)noise, 0.30f, 0.31f) - 0.3f) * 100;
	}
	
	private static void packData(CoverageType coverageType, Humidity humidity) {
	
	}
}
