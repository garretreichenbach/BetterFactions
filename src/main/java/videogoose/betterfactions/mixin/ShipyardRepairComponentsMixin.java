package videogoose.betterfactions.mixin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.controller.elements.shipyard.orders.ShipyardEntityState;
import org.schema.game.common.controller.elements.shipyard.orders.states.Constructing;
import org.schema.game.common.data.player.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.repair.RepairComponents;

/**
 * A shipyard repair builds the new ship from components rather than blocks: what the yard counts as on hand, what
 * it takes, and what it pulls in from its connected chests. Other orders are left alone. The other half, breaking
 * the old ship down into components, is {@link ShipyardRepairRefundMixin}.
 */
@Mixin(value = Constructing.class, remap = false)
public abstract class ShipyardRepairComponentsMixin {

	@Shadow
	private long lastInventoryFilterStep;

	@Unique
	private ShipyardEntityState betterFactions$state() {
		return ((Constructing) (Object) this).getEntityState();
	}

	@Redirect(method = "onUpdate()Z", at = @At(value = "INVOKE",
			target = "Lorg/schema/game/common/data/player/inventory/Inventory;getOverallQuantity(S)I"))
	private int betterFactions$onHand(Inventory inv, short type) {
		return RepairComponents.repairing(betterFactions$state()) ? RepairComponents.makeable(inv, type) : inv.getOverallQuantity(type);
	}

	@Redirect(method = "onUpdate()Z", at = @At(value = "INVOKE",
			target = "Lorg/schema/game/common/data/player/inventory/Inventory;decreaseBatch(SILit/unimi/dsi/fastutil/ints/IntOpenHashSet;)V"))
	private void betterFactions$take(Inventory inv, short type, int count, IntOpenHashSet mod) {
		if (RepairComponents.repairing(betterFactions$state())) {
			RepairComponents.take(inv, type, count, mod);
		} else {
			inv.decreaseBatch(type, count, mod);
		}
	}

	@Inject(method = "pullResources()V", at = @At("HEAD"), cancellable = true)
	private void betterFactions$pullComponents(CallbackInfo ci) {
		ShipyardEntityState s = betterFactions$state();
		if (!RepairComponents.repairing(s)) {
			return;
		}
		ci.cancel();
		//same cadence as the vanilla pull
		long step = s.getShipyardCollectionManager().getSegmentController().getState().getController().getServerRunningTime() / ManagerContainer.TIME_STEP_STASH_PULL;
		if (step > lastInventoryFilterStep) {
			lastInventoryFilterStep = step;
			RepairComponents.pull(s);
		}
	}
}
