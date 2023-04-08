package thederpgamer.betterfactions.manager;

import api.listener.Listener;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.mod.StarLoader;
import org.schema.game.client.view.gui.PlayerPanel;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.gui.factionpanel.BFFactionPanelNew;

import java.lang.reflect.Field;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class EventManager {

	public static void registerEvents(BetterFactions betterFactions) {
		StarLoader.registerListener(PlayerGUICreateEvent.class, new Listener<PlayerGUICreateEvent>() {
			@Override
			public void onEvent(PlayerGUICreateEvent event) {
				try {
					PlayerPanel playerPanel = event.getPlayerPanel();
					Field factionPanelNewField = playerPanel.getClass().getDeclaredField("factionPanelNew");
					factionPanelNewField.setAccessible(true);
					if(!(factionPanelNewField.get(playerPanel) instanceof BFFactionPanelNew)) {
						BFFactionPanelNew factionPanelNew = new BFFactionPanelNew(playerPanel.getState());
						factionPanelNew.onInit();
						factionPanelNewField.set(playerPanel, factionPanelNew);
					}
				} catch(NoSuchFieldException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}, betterFactions);
	}
}
