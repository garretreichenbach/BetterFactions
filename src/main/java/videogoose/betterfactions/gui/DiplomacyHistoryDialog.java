package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITextOverlayTable;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.graphicsengine.forms.gui.newgui.config.GuiDateFormats;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.DiplomacyManager.HistoryEntry;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

/**
 * Everything that has passed between two factions, newest first: offers, demands and declarations,
 * how they were answered, and the treaties and wars that came of them and ended.
 */
public class DiplomacyHistoryDialog extends PlayerInput {

	private final GUIInputPanel panel;
	private HistoryList list;

	public DiplomacyHistoryDialog(GameClientState state, int side, Faction other) {
		super(state);
		panel = new GUIInputPanel("BF_DIPLOMACY_HISTORY", state, 900, 600, this, Lng.str("History with %s", other.getName()), "") {
			@Override
			public void onInit() {
				super.onInit();
				GUIContentPane pane = ((GUIDialogWindow) background).getMainContentPane();
				list = new HistoryList(getState(), pane.getContent(0), side, other.getIdFaction());
				list.onInit();
				pane.getContent(0).attach(list);
			}
		};
		panel.setCancelButton(false);
	}

	@Override
	public GUIElement getInputPanel() {
		return panel;
	}

	@Override
	public void onDeactivate() {
		if (list != null) {
			list.cleanUp();
		}
	}

	@Override
	public void handleMouseEvent(MouseEvent e) {
	}

	private static class HistoryList extends ScrollableTableList<HistoryEntry> {

		private final int side, other;

		HistoryList(InputState state, GUIElement p, int side, int other) {
			super(state, 100, 100, p);
			this.side = side;
			this.other = other;
			DiplomacyManager.client().addObserver(this);
		}

		@Override
		public void cleanUp() {
			super.cleanUp();
			DiplomacyManager.client().deleteObserver(this);
		}

		@Override
		public void initColumns() {
			addFixedWidthColumn(Lng.str("When"), 150, (a, b) -> Long.compare(b.time(), a.time()));
			addColumn(Lng.str("Event"), 5.0f, (a, b) -> a.text().compareToIgnoreCase(b.text()));
			activeSortColumnIndex = 0;
		}

		@Override
		protected Collection<HistoryEntry> getElementList() {
			return DiplomacyManager.client().getHistory(side, other);
		}

		@Override
		public void updateListEntries(GUIElementList mainList, Set<HistoryEntry> collection) {
			mainList.deleteObservers();
			mainList.addObserver(this);
			for (HistoryEntry e : collection) {
				Row r = new Row(getState(), e, cell(GuiDateFormats.factionNewsTime.format(new Date(e.time()))), cell(e.text())) {
					{
						highlightSelect = true;
					}
				};
				r.onInit();
				mainList.addWithoutUpdate(r);
			}
			mainList.updateDim();
		}

		private GUIClippedRow cell(String value) {
			GUITextOverlayTable t = new GUITextOverlayTable(10, 10, getState());
			t.setTextSimple(value);
			t.getPos().y = 5;
			GUIClippedRow c = new GUIClippedRow(getState());
			c.attach(t);
			return c;
		}
	}
}
