package videogoose.betterfactions.mixin;

import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.ntstats.GUINetworkStatsPanelNew;
import org.schema.schine.graphicsengine.forms.gui.GUIScrollableTextPanel;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITabbedContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.gui.BattleAdminList;
import videogoose.betterfactions.gui.BattlesProfilerText;

/**
 * A BATTLES tab in the profiler, for admins only: every battle, whether it is soft-loaded, what it
 * costs the server per tick, and buttons to pause, end or go to it. The server builds the report; the
 * tab asks for it about once a second while it is drawn. The server checks admin rights again on every
 * request and command.
 */
@Mixin(value = GUINetworkStatsPanelNew.class, remap = false)
public abstract class ProfilerBattlesTabMixin {

	/**
	 * At the end of the last tab the panel adds, taking the tab set from its argument: the panel's own
	 * {@code tabs} field is only assigned after every tab is built, so it is still null here.
	 */
	@Inject(method = "addDrawnTab(Lorg/schema/schine/graphicsengine/forms/gui/newgui/GUITabbedContent;)V", at = @At("TAIL"))
	private void betterFactions$addBattlesTab(GUITabbedContent tabs, CallbackInfo ci) {
		GameClientState state = GameClientState.instance;
		if (state == null || state.getPlayer() == null || !state.getPlayer().isAdmin()) {
			return;
		}
		GUIContentPane pane = tabs.addTab("BATTLES");
		pane.setTextBoxHeightLast(120);
		pane.addNewTextBox(10);
		GUIScrollableTextPanel scroll = new GUIScrollableTextPanel(10, 10, pane.getContent(0), state);
		scroll.setContent(new BattlesProfilerText(state));
		pane.getContent(0).attach(scroll);
		BattleAdminList list = new BattleAdminList(state, pane.getContent(1));
		list.onInit();
		pane.getContent(1).attach(list);
	}
}
