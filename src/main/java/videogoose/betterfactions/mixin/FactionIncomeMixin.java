package videogoose.betterfactions.mixin;

import org.schema.game.common.data.player.faction.config.FactionConfig;
import org.schema.game.common.data.player.faction.config.FactionPointIncomeConfig;
import org.schema.schine.network.StateInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Raises faction point income per member: 300 online, 80 offline. Inactive members already earn nothing, since
 * vanilla only pays offline income to {@code isActiveMember}. Set after the XML loads, which would otherwise win.
 */
@Mixin(value = FactionConfig.class, remap = false)
public abstract class FactionIncomeMixin {

	@Inject(method = "load(Lorg/schema/schine/network/StateInterface;Ljava/lang/String;)V", at = @At("RETURN"))
	private static void betterFactions$income(StateInterface state, String path, CallbackInfo ci) {
		FactionPointIncomeConfig.FACTION_POINTS_PER_ONLINE_MEMBER = 300;
		FactionPointIncomeConfig.FACTION_POINTS_PER_MEMBER = 80;
	}
}
