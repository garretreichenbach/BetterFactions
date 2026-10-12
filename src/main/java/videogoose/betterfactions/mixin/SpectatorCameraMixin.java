package videogoose.betterfactions.mixin;

import org.schema.game.client.controller.manager.freemode.FreeRoamController;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.input.Keyboard;
import org.schema.schine.input.KeyboardMappings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.gui.BattleAdminList;

/**
 * The free-roam camera while an admin watches a battle: a banner says it is soft-loaded (or held for
 * load), the exit-ship key takes them back, it holds still while a menu (the nav list, say) is open,
 * and it moves at battle scale (the engine's 5 and 50 m/s are for looking around a spawn point).
 */
@Mixin(value = FreeRoamController.class, remap = false)
public abstract class SpectatorCameraMixin {

	private static boolean betterFactions$exitWasDown;

	//polled: the free-roam controller has no key handler of its own to hook
	@Inject(method = "update", at = @At("HEAD"), cancellable = true)
	private void betterFactions$leaveBattle(Timer timer, CallbackInfo ci) {
		GameClientState state = GameClientState.instance;
		if (BattleAdminList.isWatching() && (state.getGlobalGameControlManager().getIngameControlManager().isAnyMenuOrChatActive()
				|| !state.getController().getPlayerInputs().isEmpty())) {
			//a menu or chat is open: the camera holds still and leaves the mouse to it, and an R typed
			//there is not a request to leave
			betterFactions$exitWasDown = true;
			ci.cancel();
			return;
		}
		boolean down = Keyboard.isKeyDown(KeyboardMappings.ENTER_SHIP.getMapping());
		if (down && !betterFactions$exitWasDown && BattleAdminList.isWatching()) {
			BattleAdminList.stopWatching();
		}
		betterFactions$exitWasDown = down;
		if (BattleAdminList.isWatching()) {
			BattleAdminList.banner(GameClientState.instance);
		}
	}

	@ModifyConstant(method = "update", constant = @Constant(floatValue = 5f))
	private float betterFactions$watchSpeed(float speed) {
		return BattleAdminList.isWatching() ? 40f : speed;
	}

	@ModifyConstant(method = "update", constant = @Constant(floatValue = 50f))
	private float betterFactions$watchFastSpeed(float speed) {
		return BattleAdminList.isWatching() ? 400f : speed;
	}
}
