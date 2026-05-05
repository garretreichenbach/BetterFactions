package videogoose.betterfactions.gui.factionpanel;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.FactionState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.ControllerElement.FilterRowStyle;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.data.diplomacy.FactionDiplomacyEntity;
import videogoose.betterfactions.data.diplomacy.modifier.FactionDiplomacyMod;
import videogoose.betterfactions.manager.FactionDiplomacyManager;

import java.util.*;

public class GUIFactionDiploScrollableList extends ScrollableTableList<FactionDiplomacyMod> {

	private long diplEntityId;
	private Faction toFaction;

	public GUIFactionDiploScrollableList(InputState state, long diplEntityId, Faction toFaction, GUIElement p) {
		super(state, 100, 100, p);
		this.toFaction = toFaction;
		this.diplEntityId = diplEntityId;
		((GameClientState)state).getFactionManager().addObserver(this);
		FactionDiplomacyManager.getDiplomacy(toFaction.getIdFaction()).addObserver(this);
	}

	/* (non-Javadoc)
	 * @see org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList#cleanUp()
	 */
	@Override
	public void cleanUp() {
		super.cleanUp();
		((GameClientState) getState()).getFactionManager().deleteObserver(this);
		FactionDiplomacyManager.getDiplomacy(toFaction.getIdFaction()).deleteObserver(this);
	}

	@Override
	public boolean isFiltered(FactionDiplomacyMod e) {
		return super.isFiltered(e);
	}

	@Override
	public void initColumns() {

		addColumn(Lng.str("Modifier"), 3f, (o1, o2) -> (o1.getName()).compareTo(o2.getName()));
		addFixedWidthColumn(Lng.str("Value"), 140, (o1, o2) -> o1.getValue() - o2.getValue());

		addTextFilter(new GUIListFilterText<FactionDiplomacyMod>() {

			@Override
			public boolean isOk(String input, FactionDiplomacyMod listElement) {
				return listElement.getName().toLowerCase(Locale.ENGLISH).contains(input.toLowerCase(Locale.ENGLISH));
			}
		}, Lng.str("SEARCH"), FilterRowStyle.LEFT);

		addDropdownFilter(new GUIListFilterDropdown<FactionDiplomacyMod, Integer>(new Integer[]{0, 1, 2}) {

			@Override
			public boolean isOk(Integer input, FactionDiplomacyMod f) {
				return switch(input) {
					case 0 -> true;
					case 1 -> !f.isStatic();
					case 2 -> f.isStatic();
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
					case 1 -> a.setTextSimple(Lng.str("TURN MODIFIERS"));
					case 2 -> a.setTextSimple(Lng.str("STATIC MODIFIERS"));
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
		}, FilterRowStyle.RIGHT);

		activeSortColumnIndex = 0;
	}

	@Override
	protected Collection<FactionDiplomacyMod> getElementList() {
		List<FactionDiplomacyMod> d = new ObjectArrayList<>();
		FactionDiplomacyEntity ent = FactionDiplomacyManager.getDiplomacy(toFaction.getIdFaction()).entities.get(diplEntityId);
		if(ent == null) {
			ent = new FactionDiplomacyEntity((FactionState) toFaction.getState(), (int) diplEntityId);
			FactionDiplomacyManager.getDiplomacy(toFaction.getIdFaction()).entities.put(diplEntityId, ent);
		}
		d.addAll(ent.getDynamicMap().values());
		d.addAll(ent.getStaticMap().values());
		return d;
	}

	@Override
	public void updateListEntries(GUIElementList mainList, Set<FactionDiplomacyMod> collection) {
		mainList.deleteObservers();
		mainList.addObserver(this);
		final PlayerState player = ((GameClientState) getState()).getPlayer();
		int i = 0;
		for(final FactionDiplomacyMod f : collection) {
			GUITextOverlayTable nameText = new GUITextOverlayTable(10, 10, getState());
			GUITextOverlayTable valueText = new GUITextOverlayTable(10, 10, getState());

			nameText.setTextSimple(new Object() {
				@Override
				public String toString() {
					return f.getName();
				}

			});
			valueText.setTextSimple(new Object() {
				@Override
				public String toString() {
					return f.getValue() + (f.isStatic() ? "" : " / turn");
				}
			});


			GUIClippedRow nameAnchorP = new GUIClippedRow(getState());
			nameAnchorP.attach(nameText);

			GUIClippedRow sysAnchorP = new GUIClippedRow(getState());

			sysAnchorP.attach(valueText);

			nameText.getPos().y = 5;
			valueText.getPos().y = 5;

			final FactionDiplomacyModRow r = new FactionDiplomacyModRow(getState(), f, nameAnchorP, sysAnchorP);

			r.onInit();
			mainList.addWithoutUpdate(r);
			i++;
		}
		mainList.updateDim();
	}


	private class FactionDiplomacyModRow extends Row {

		public FactionDiplomacyModRow(InputState state, FactionDiplomacyMod f, GUIElement... elements) {
			super(state, f, elements);
			this.highlightSelect = true;
		}
	}
}
