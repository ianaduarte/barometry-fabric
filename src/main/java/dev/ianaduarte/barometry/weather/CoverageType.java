package dev.ianaduarte.barometry.weather;

public enum CoverageType {
	CLEAR    (false, false),
	FEW      (false, false),
	SCATTERED(true , false),
	BROKEN   (true , true ),
	OVERCAST (true , true ),
	OBSCURED (true , true );
	
	public static final CoverageType[] VALUES = values();
	
	public final boolean canRain;
	public final boolean canThunder;
	
	CoverageType(boolean canRain, boolean canThunder) {
		this.canRain = canRain;
		this.canThunder = canThunder;
	}
}
