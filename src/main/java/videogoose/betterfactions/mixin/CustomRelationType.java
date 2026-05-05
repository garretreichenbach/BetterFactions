package videogoose.betterfactions.mixin;

/**
 * Compile-time constants for custom FactionRelation.RType values
 * that are injected at runtime via ReflectionUtils.
 */
public final class CustomRelationType {
	public static final byte NON_AGGRESSION = 3;
	public static final byte FEDERATION = 4;

	private CustomRelationType() {}
}
