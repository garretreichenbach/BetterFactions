package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.fleet.TradeFleetOrders;

/**
 * Server: a raiding fleet's ships and the convoy's (or miners') it is on are enemies, both ways, whatever their
 * factions' relation: a raid needs no war, but the engine's ship AI only picks and fires at {@code isEnemy}
 * targets, so without this a raider that caught its prey just sat beside it. Docked ships count as their root's
 * fleet, so turrets on either side fight too: their target search and fire checks use this same overload.
 */
@Mixin(value = FactionManager.class, remap = false)
public abstract class RaidHostilityMixin {

	@Inject(method = "isEnemy(Lorg/schema/game/common/data/world/SimpleTransformableSendableObject;Lorg/schema/game/common/data/world/SimpleTransformableSendableObject;)Z",
			at = @At("RETURN"), cancellable = true)
	private void betterFactions$raid(SimpleTransformableSendableObject<?> a, SimpleTransformableSendableObject<?> b, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() || !a.isOnServer() || !TradeFleetOrders.get().anyRaids()
				|| !(a instanceof SegmentController sa) || !(b instanceof SegmentController sb)) {
			return;
		}
		Fleet fa = sa.railController.getRoot().getFleet();
		Fleet fb = fa == null ? null : sb.railController.getRoot().getFleet();
		if (fb != null && TradeFleetOrders.get().raiding(fa.dbid, fb.dbid)) {
			cir.setReturnValue(true);
		}
	}
}
