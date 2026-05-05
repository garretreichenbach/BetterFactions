package videogoose.betterfactions.gui.factionpanel;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.game.client.controller.manager.ingame.shop.ShopControllerManager;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import java.util.*;

public class GUIFactionsScrollableList extends ScrollableTableList<Faction> {
	private final BFFactionPanelNew panel;

	public GUIFactionsScrollableList(InputState state, GUIElement p, BFFactionPanelNew factionPanelNew) {
		super(state, 100, 100, p);
		this.panel = factionPanelNew;
		((GameClientState) state).getFactionManager().addObserver(this);
	}

	/* (non-Javadoc)
	 * @see org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList#cleanUp()
	 */
	@Override
	public void cleanUp() {
		super.cleanUp();
		((GameClientState) getState()).getFactionManager().deleteObserver(this);
	}

	@Override
	public boolean isFiltered(Faction e) {
		return super.isFiltered(e);
	}

	public ShopControllerManager getShopControlManager() {
		return ((GameClientState) getState()).getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getShopControlManager();
	}

	public FactionRelation.RType getOwnRelationTo(Faction f) {
		return ((GameClientState) getState()).getFactionManager().getRelation(((GameClientState) getState()).getPlayerName(), ((GameClientState) getState()).getPlayer().getFactionId(), f.getIdFaction());
	}

	@Override
	public void initColumns() {
		addColumn(Lng.str("Name"), 3f, (o1, o2) -> (o1.getName()).compareTo(o2.getName()));
		addFixedWidthColumn(Lng.str("Home"), 140, (o1, o2) -> o1.getHomeSector().compareTo(o2.getHomeSector()));
		addTextFilter(new GUIListFilterText<Faction>() {
			@Override
			public boolean isOk(String input, Faction listElement) {
				return listElement.getName().toLowerCase(Locale.ENGLISH).contains(input.toLowerCase(Locale.ENGLISH));
			}
		}, Lng.str("SEARCH"), ControllerElement.FilterRowStyle.LEFT);
		addDropdownFilter(new GUIListFilterDropdown<Faction, Integer>(0, 1, 2, 3) {
			@Override
			public boolean isOk(Integer input, Faction f) {
				return switch(input) {
					case 0 -> true;
					case 1 -> getOwnRelationTo(f) == FactionRelation.RType.NEUTRAL;
					case 2 -> getOwnRelationTo(f) == FactionRelation.RType.ENEMY;
					case 3 -> getOwnRelationTo(f) == FactionRelation.RType.FRIEND;
					default -> true;
				};
			}
		}, new CreateGUIElementInterface<Integer>() {
			@Override
			public GUIElement create(Integer o) {
				GUIAncor c = new GUIAncor(getState(), 10, 24);
				GUITextOverlayTableDropDown a = new GUITextOverlayTableDropDown(10, 10, getState());
				switch(o) {
					case 0 -> a.setTextSimple(Lng.str("ALL"));
					case 1 -> a.setTextSimple(Lng.str("NEUTRAL"));
					case 2 -> a.setTextSimple(Lng.str("WAR"));
					case 3 -> a.setTextSimple(Lng.str("ALLIES"));
					default -> {}
				}
				a.setPos(4, 4, 0);
				c.setUserPointer(o);
				c.attach(a);
				return c;
			}

			@Override
			public GUIElement createNeutral() {
				return null; // default is all
			}
		}, ControllerElement.FilterRowStyle.RIGHT);
		activeSortColumnIndex = 0;
	}

	@Override
	protected Collection<Faction> getElementList() {
		List<Faction> d = new ObjectArrayList<Faction>();
		for(Faction f : ((GameClientState) getState()).getFactionManager().getFactionCollection()) {
			if((f.isNPC() || f.getIdFaction() > 0) && f.getIdFaction() != ((GameClientState) getState()).getPlayer().getFactionId()) d.add(f);
		}
		return d;
	}

	@Override
	public void updateListEntries(GUIElementList mainList, Set<Faction> collection) {
		mainList.deleteObservers();
		mainList.addObserver(this);
		PlayerState player = ((GameClientState) getState()).getPlayer();
		int i = 0;
		for(final Faction f : collection) {
			GUITextOverlayTable nameText = new GUITextOverlayTable(10, 10, getState());
			GUITextOverlayTable systemText = new GUITextOverlayTable(10, 10, getState());
			nameText.setTextSimple(new Object() {
				@Override
				public String toString() {
					return f.getName();
				}
			});
			systemText.setTextSimple(new Object() {
				@Override
				public String toString() {
					return f.getHomeSector().toStringPure();
				}
			});
			GUIClippedRow nameAnchorP = new GUIClippedRow(getState());
			nameAnchorP.attach(nameText);
			GUIClippedRow sysAnchorP = new GUIClippedRow(getState());
			sysAnchorP.attach(systemText);
			nameText.getPos().y = 5;
			systemText.getPos().y = 5;
			final FactionRow r = new FactionRow(getState(), f, nameAnchorP, sysAnchorP);
			r.onInit();
			mainList.addWithoutUpdate(r);
			i++;
		}
		mainList.updateDim();
	}

	private class FactionRow extends ScrollableTableList.Row {
		public FactionRow(InputState state, Faction f, GUIElement... elements) {
			super(state, f, elements);
			this.highlightSelect = true;
			this.highlightSelectSimple = true;
			this.setAllwaysOneSelected(true);
		}

		@Override
		public void clickedOnRow() {
			panel.onSelectFaction((Faction) f);
			super.clickedOnRow();
		}
	}
}