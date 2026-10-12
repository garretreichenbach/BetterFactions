package videogoose.betterfactions.mixin;

import org.schema.schine.graphicsengine.core.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The tick's elapsed time, so an entity updated one tick in N can be given N ticks' worth. */
@Mixin(value = Timer.class, remap = false)
public interface TimerAccessor {

	@Accessor("delta")
	double betterFactions$delta();

	@Accessor("delta")
	void betterFactions$setDelta(double delta);
}
