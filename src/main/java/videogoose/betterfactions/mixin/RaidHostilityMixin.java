package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.fleet.TradeFleetOrders;

/**
 * Server: a raiding fleet's ships and every ship of the faction whose convoy (or miners) it is on are enemies, both
 * ways, whatever their factions' relation, so escorts and defenders fight the raider and it fights them back, while
 * the rest of the raider's faction stays out of it. Stations, and what is docked to them, are left out: a raid hits
 * ships, not bases. A raid needs no war, but the engine's ship AI only picks and fires at {@code isEnemy}
 * targets, so without this a raider that caught its prey just sat beside it. Docked ships count as their root's
 * fleet, so turrets on either side fight too: their target search and fire checks use this same overload.
 */
@Mixin(value = FactionManager.class, remap = false)
public abstract class RaidHostilityMixin {

	@Inject(method = "isEnemy(Lorg/schema/game/common/data/world/SimpleTransformableSendableObject;Lorg/schema/game/common/data/world/SimpleTransformableSendableObject;)Z",
			at = @At("RETURN"), cancellable = true)
	private void betterFactions$raid(SimpleTransformableSendableObject<?> a, SimpleTransformableSendableObject<?> b, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() || !a.isOnServer() || !TradeFleetOrders.get().anyRaids()
				|| !(a instanceof SegmentController sa) || !(b instanceof SegmentController sb)
				|| sa.railController.getRoot() instanceof SpaceStation || sb.railController.getRoot() instanceof SpaceStation) {
			return;
		}
		Fleet fa = sa.railController.getRoot().getFleet();
		Fleet fb = sb.railController.getRoot().getFleet();
		if (fa != null && TradeFleetOrders.get().raidingFaction(fa, sb.getFactionId())
				|| fb != null && TradeFleetOrders.get().raidingFaction(fb, sa.getFactionId())) {
			cir.setReturnValue(true);
		}
	}
}
