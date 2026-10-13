package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.controller.elements.ShipyardManagerContainerInterface;
import org.schema.game.common.controller.elements.shipyard.ShipyardCollectionManager;
import org.schema.game.common.controller.elements.shipyard.orders.states.ShipyardState;
import org.schema.game.network.objects.valueUpdate.ShipyardBlockGoalValueUpdate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.repair.RepairComponents;
import videogoose.betterfactions.repair.ShipyardRepairs;

/**
 * The shipyard's "Inputs Needed" list during a repair: the goal the server sends is converted to components, which
 * is what a repair takes ({@link ShipyardRepairComponentsMixin}), so the vanilla list shows them unchanged. The
 * client cannot tell a repair from any other build, so this is done where the goal is sent. While the yard has no
 * build on, the list is what the ships queued for repair there will need ({@link ShipyardInputsButtonMixin}).
 */
@Mixin(value = ShipyardBlockGoalValueUpdate.class, remap = false)
public abstract class ShipyardRepairGoalMixin {

	@Shadow
	private ElementCountMap blocksFrom;
	@Shadow
	private ElementCountMap blocksto;

	@Inject(method = "setServer(Lorg/schema/game/common/controller/elements/ManagerContainer;J)V", at = @At("TAIL"))
	private void betterFactions$asComponents(ManagerContainer<?> o, long parameter, CallbackInfo ci) {
		ShipyardCollectionManager v = ((ShipyardManagerContainerInterface) o).getShipyard().getElementManager().getCollectionManagersMap().get(parameter);
		if (v == null || blocksFrom == null) {
			return;
		}
		ShipyardState st = v.getCurrentStateServer();
		if (st == null || !st.hasBlockGoal()) {
			ElementCountMap[] queued = ShipyardRepairs.get().queuedGoal(v);
			if (queued != null) {
				blocksFrom = queued[0];
				blocksto = queued[1];
			}
		} else if (RepairComponents.repairing(v.getServerEntityState())) {
			blocksFrom = RepairComponents.components(blocksFrom);
			blocksto = RepairComponents.components(blocksto);
		}
	}
}
