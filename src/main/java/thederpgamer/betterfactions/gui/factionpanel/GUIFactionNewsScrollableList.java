package thederpgamer.betterfactions.gui.factionpanel;

import org.hsqldb.lib.StringComparator;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.FactionState;
import org.schema.game.server.data.simulation.npc.news.NPCFactionNewsEvent;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.ControllerElement.FilterRowStyle;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIListFilterText;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITextOverlayTable;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.input.InputState;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

public class GUIFactionNewsScrollableList extends ScrollableTableList<NPCFactionNewsEvent> {

	public GUIFactionNewsScrollableList(InputState state, GUIElement p) {
		super(state, 100, 100, p);
		((GameClientState)getState()).getFactionManager().getNpcFactionNews().addObserver(this);
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		((GameClientState)getState()).getFactionManager().getNpcFactionNews().deleteObserver(this);
	}

	@Override
	public boolean isFiltered(NPCFactionNewsEvent e) {
		return super.isFiltered(e) ;
	}

	@Override
	public void initColumns() {
		final StringComparator c = new StringComparator();
		addColumn(Lng.str("Event"), 3f, new Comparator<NPCFactionNewsEvent>() {
			@Override
			public int compare(NPCFactionNewsEvent o1, NPCFactionNewsEvent o2) {
				return o1.getMessage((FactionState) getState()).compareTo(o2.getMessage((FactionState) getState()));
			}
		});
		addTextFilter(new GUIListFilterText<NPCFactionNewsEvent>() {

			@Override
			public boolean isOk(String input, NPCFactionNewsEvent listElement) {
				return listElement.getMessage((FactionState) getState()).toLowerCase(Locale.ENGLISH).contains(input.toLowerCase(Locale.ENGLISH));
			}
		}, Lng.str("SEARCH"), FilterRowStyle.FULL);
		activeSortColumnIndex = 0;
	}

	@Override
	protected Collection<NPCFactionNewsEvent> getElementList() {
		return ((GameClientState)getState()).getFactionManager().getNpcFactionNews().events;
	}


	@Override
	public void updateListEntries(GUIElementList mainList, Set<NPCFactionNewsEvent> collection) {
		mainList.deleteObservers();
		mainList.addObserver(this);
		final PlayerState player = ((GameClientState) getState()).getPlayer();
		int i = 0;
		for (final NPCFactionNewsEvent f : collection) {
			GUITextOverlayTable nameText = new GUITextOverlayTable(10, 10, getState());
			nameText.setTextSimple(new Object(){
				@Override
				public String toString() {
					return f.getMessage((FactionState)getState());
				}

			});
			GUIClippedRow nameAnchorP = new GUIClippedRow(getState());
			nameAnchorP.attach(nameText);
			nameText.getPos().y = 5;
			final NPCFactionNewsEventRow r = new NPCFactionNewsEventRow(getState(), f, nameAnchorP);
			r.onInit();
			mainList.addWithoutUpdate(r);
			i++;
		}
		mainList.updateDim();
	}



	private class NPCFactionNewsEventRow extends Row {

		public NPCFactionNewsEventRow(InputState state, NPCFactionNewsEvent f, GUIElement... elements) {
			super(state, f, elements);
			this.highlightSelect = true;
		}
	}
}