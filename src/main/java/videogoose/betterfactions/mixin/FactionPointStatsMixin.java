package videogoose.betterfactions.mixin;

import org.schema.game.client.view.gui.faction.newfaction.FactionPointRevenueScrollableListNew;
import org.schema.game.client.view.gui.faction.newfaction.FactionPointStat;
import org.schema.game.common.data.player.faction.Faction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.gui.ModPointStat;

import java.util.List;

/** Client: the mod's faction point lines in the engine's Faction Point Statistics list. */
@Mixin(value = FactionPointRevenueScrollableListNew.class, remap = false)
public abstract class FactionPointStatsMixin {

	@Shadow
	@Final
	private List<FactionPointStat> stats;
	@Shadow
	private Faction faction;

	@Inject(method = "createStats", at = @At("TAIL"))
	private void betterFactions$modStats(CallbackInfo ci) {
		ModPointStat.addTo(stats, faction);
	}
}
