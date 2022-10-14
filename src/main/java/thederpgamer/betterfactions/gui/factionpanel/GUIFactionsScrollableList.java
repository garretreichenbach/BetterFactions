package thederpgamer.betterfactions.gui.factionpanel;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.hsqldb.lib.StringComparator;
import org.schema.game.client.controller.manager.ingame.shop.ShopControllerManager;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation.RType;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.ControllerElement.FilterRowStyle;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import java.util.*;

public class GUIFactionsScrollableList extends ScrollableTableList<Faction> {

	private BFFactionPanelNew panel;

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

	public RType getOwnRelationTo(Faction f) {
		return ((GameClientState) getState()).getFactionManager().getRelation
				(((GameClientState) getState()).getPlayerName(),
						((GameClientState) getState()).getPlayer().getFactionId(), f.getIdFaction());
	}

	@Override
	public void initColumns() {

		final StringComparator c = new StringComparator();

		addColumn(Lng.str("Name"), 3f, new Comparator<Faction>() {
			@Override
			public int compare(Faction o1, Faction o2) {
				return (o1.getName()).compareTo(o2.getName());
			}
		});
		addFixedWidthColumn(Lng.str("Home"), 140, new Comparator<Faction>() {
			@Override
			public int compare(Faction o1, Faction o2) {
				return o1.getHomeSector().compareTo(o2.getHomeSector());
			}
		});

		addTextFilter(new GUIListFilterText<Faction>() {

			@Override
			public boolean isOk(String input, Faction listElement) {
				return listElement.getName().toLowerCase(Locale.ENGLISH).contains(input.toLowerCase(Locale.ENGLISH));
			}
		}, Lng.str("SEARCH"), FilterRowStyle.LEFT);

		addDropdownFilter(new GUIListFilterDropdown<Faction, Integer>(new Integer[]{0, 1, 2, 3}) {

			@Override
			public boolean isOk(Integer input, Faction f) {
				switch(input) {
					case 0:
						return true;
					case 1:
						return getOwnRelationTo(f) == RType.NEUTRAL;
					case 2:
						return getOwnRelationTo(f) == RType.ENEMY;
					case 3:
						return getOwnRelationTo(f) == RType.FRIEND;
				}
				return true;
			}
		}, new CreateGUIElementInterface<Integer>() {
			@Override
			public GUIElement create(Integer o) {
				GUIAncor c = new GUIAncor(getState(), 10, 24);
				GUITextOverlayTableDropDown a = new GUITextOverlayTableDropDown(10, 10, getState());
				switch(o) {
					case 0:
						a.setTextSimple(Lng.str("ALL"));
						break;
					case 1:
						a.setTextSimple(Lng.str("NEUTRAL"));
						break;
					case 2:
						a.setTextSimple(Lng.str("WAR"));
						break;
					case 3:
						a.setTextSimple(Lng.str("ALLIES"));
						break;
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
		}, FilterRowStyle.RIGHT);

		activeSortColumnIndex = 0;
	}

	@Override
	protected Collection<Faction> getElementList() {
		List<Faction> d = new ObjectArrayList<Faction>();
		for(Faction f : ((GameClientState) getState()).getFactionManager().getFactionCollection()) {
			if(f.isNPC()) {
				d.add((Faction) f);
			}
		}

		return d;
	}


	@Override
	public void updateListEntries(GUIElementList mainList,
	                              Set<Faction> collection) {
		mainList.deleteObservers();
		mainList.addObserver(this);
		final PlayerState player = ((GameClientState) getState()).getPlayer();
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


	private class FactionRow extends Row {


		public FactionRow(InputState state, Faction f, GUIElement... elements) {
			super(state, f, elements);
			this.highlightSelect = true;
			this.highlightSelectSimple = true;
			this.setAllwaysOneSelected(true);
		}

		@Override
		public void clickedOnRow() {
			panel.onSelectFaction(f);
			super.clickedOnRow();
		}
	}
}