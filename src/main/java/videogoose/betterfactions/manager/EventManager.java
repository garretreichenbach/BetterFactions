package videogoose.betterfactions.manager;

import api.listener.Listener;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.mod.StarLoader;
import org.schema.game.client.view.gui.PlayerPanel;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.gui.factionpanel.BFFactionPanelNew;
import videogoose.betterfactions.mixin.PlayerPanelAccessor;

/**
 * Handles mod event registration.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class EventManager {

	public static void registerEvents(BetterFactions betterFactions) {
		StarLoader.registerListener(PlayerGUICreateEvent.class, (Listener<PlayerGUICreateEvent>) event -> {
			PlayerPanel playerPanel = event.getPlayerPanel();
			PlayerPanelAccessor accessor = (PlayerPanelAccessor) playerPanel;
			if (!(accessor.getFactionPanelNew() instanceof BFFactionPanelNew)) {
				BFFactionPanelNew factionPanelNew = new BFFactionPanelNew(playerPanel.getState());
				factionPanelNew.onInit();
				accessor.setFactionPanelNew(factionPanelNew);
			}
		}, betterFactions);
	}
}
