package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.controller.elements.shipyard.orders.states.Deconstructing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import videogoose.betterfactions.repair.RepairComponents;
import videogoose.betterfactions.repair.ShipyardRepairs;

/**
 * A ship deconstructed for a repair goes into the yard as the components its blocks are made from, which is what
 * the rebuild then takes ({@link ShipyardRepairComponentsMixin}). A plain deconstruct order still gives blocks.
 */
@Mixin(value = Deconstructing.class, remap = false)
public abstract class ShipyardRepairRefundMixin {

	@ModifyArg(method = "onUpdate()Z", index = 0, at = @At(value = "INVOKE",
			target = "Lorg/schema/game/common/controller/elements/shipyard/orders/ShipyardEntityState;putInInventoryAndConnected(Lorg/schema/game/common/controller/ElementCountMap;Z)V"))
	private ElementCountMap betterFactions$asComponents(ElementCountMap blocks) {
		if (!RepairComponents.repairing(((Deconstructing) (Object) this).getEntityState())) {
			return blocks;
		}
		//a free repair refunds nothing either, or toggling it would be a way to print components
		return ShipyardRepairs.free ? new ElementCountMap() : RepairComponents.components(blocks);
	}
}
