package videogoose.betterfactions.mixin;

import org.schema.game.client.data.PlayerControllable;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.common.data.world.StellarSystem;
import org.schema.game.server.data.GameServerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.ClaimAges;

import java.io.IOException;

/**
 * Server: salvaging in your own system pays the owner bonus only in full once the system has been held for
 * {@code mining_bonus_ramp_hours}; before that it climbs from the unowned bonus. Every salvage path and
 * fleet mining go through here (ships via SegmentController's override, which calls this first). The
 * miner's faction is worked out the way vanilla does: the pilot's, else the entity's.
 */
@Mixin(value = SimpleTransformableSendableObject.class, remap = false)
public abstract class MiningBonusRampMixin {

	@Inject(method = "getMiningBonus", at = @At("RETURN"), cancellable = true)
	private void betterFactions$ramp(SimpleTransformableSendableObject<?> in, CallbackInfoReturnable<Integer> cir) {
		SimpleTransformableSendableObject<?> self = (SimpleTransformableSendableObject<?>) (Object) this;
		if (!self.isOnServer()) {
			return;
		}
		int faction = self instanceof PlayerControllable pc && !pc.getAttachedPlayers().isEmpty()
				? pc.getAttachedPlayers().get(0).getFactionId() : self.getFactionId();
		Sector sector = GameServerState.instance.getUniverse().getSector(in.getSectorId());
		if (faction == 0 || sector == null) {
			return;
		}
		try {
			StellarSystem sys = GameServerState.instance.getUniverse().getStellarSystemFromSecPos(sector.pos);
			if (sys.getOwnerFaction() == faction) {
				float scale = ClaimAges.ownerBonusScale(ClaimAges.get().maturity(sys.getPos(), System.currentTimeMillis()));
				cir.setReturnValue(Math.round(cir.getReturnValueI() * scale));
			}
		} catch (IOException ignored) {
		}
	}
}
