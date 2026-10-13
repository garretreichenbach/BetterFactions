package videogoose.betterfactions.gui;

import org.schema.common.util.StringTools;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.repair.FactionDesigns;

import java.util.*;

/**
 * Faction Designs, under Assets: every ship in the fleets we can see, and the blueprint a shipyard repair
 * rebuilds it to. A ship nobody assigned shows the guess from its name, which is what the server will use too.
 */
public class FactionDesignsList extends ScrollableTableList<FactionDesignsList.Ship> {

	/** Equality is everything the row shows, so a changed assignment rebuilds it. */
	public record Ship(String uid, String name, long fleetDbId, String fleet, String blueprint, double mass) {
	}

	private final GUIElement parent;
	private int builtFrom = -1;

	public FactionDesignsList(InputState state, GUIElement p) {
		super(state, 100, 100, p);
		this.parent = p;
		getState().getFleetManager().addObserver(this);
		setGroupProvider(new RowGroupProvider<>() {
			@Override
			public String getGroupName(Ship s) {
				return s.fleet();
			}

			@Override
			public int getGroupSortIndex(Ship s) {
				return 0;
			}
		});
	}

	@Override
	public void cleanUp() {
		getState().getFleetManager().deleteObserver(this);
		super.cleanUp();
	}

	@Override
	public GameClientState getState() {
		return (GameClientState) super.getState();
	}

	@Override
	public void initColumns() {
		addColumn(Lng.str("Ship"), 2.2f, (a, b) -> a.name().compareToIgnoreCase(b.name()));
		addFixedWidthColumn(Lng.str("Mass"), 110, (a, b) -> Double.compare(a.mass(), b.mass()));
		addColumn(Lng.str("Blueprint"), 2.2f, (a, b) -> label(a).compareToIgnoreCase(label(b)));
		activeSortColumnIndex = 0;
	}

	private static String label(Ship s) {
		return s.blueprint() == null ? Lng.str("none - set one") : s.blueprint();
	}

	@Override
	public void draw() {
		RepairDialogs.requestDesigns();
		if (FactionDesigns.clientVersion() != builtFrom) {
			builtFrom = FactionDesigns.clientVersion();
			flagDirty();
		}
		super.draw();
	}

	@Override
	protected Collection<Ship> getElementList() {
		List<Ship> out = new ArrayList<>();
		for (Fleet f : getState().getFleetManager().getAvailableFleetsClient()) {
			for (FleetMember m : f.getMembers()) {
				if (m.getDockedToRootDbId() > 0) {
					continue;
				}
				out.add(new Ship(m.UID, m.name, f.dbid, f.getName(), RepairDialogs.blueprintOf(getState(), m.UID, m.name), m.getMass()));
			}
		}
		return out;
	}

	@Override
	public void updateListEntries(GUIElementList mainList, Set<Ship> collection) {
		mainList.deleteObservers();
		mainList.addObserver(this);
		for (Ship s : collection) {
			Row r = new Row(getState(), s, cell(s.name()), cell(StringTools.massFormat(s.mass())), cell(label(s))) {
			};
			GUIAnchor expanded = expandedFor(s);
			r.expanded = new GUIElementList(getState());
			r.expanded.add(new GUIListElement(expanded, expanded, getState()));
			r.onInit();
			mainList.addWithoutUpdate(r);
		}
		mainList.updateDim();
	}

	private GUIAnchor expandedFor(Ship s) {
		GUIAnchor c = new GUIAnchor(getState(), 100, 32) {
			@Override
			public void draw() {
				setWidth(parent.getWidth());
				super.draw();
			}
		};
		GUITextButton set = new GUITextButton(getState(), 130, 24, GUITextButton.ColorPalette.OK, Lng.str("SET BLUEPRINT"), new GUICallback() {
			@Override
			public boolean isOccluded() {
				return !isActive();
			}

			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if (event.pressedLeftMouse()) {
					RepairDialogs.pickBlueprint(getState(), s.uid(), s.name());
				}
			}
		});
		set.setPos(4, 4, 0);
		c.attach(set);
		GUITextButton repair = new GUITextButton(getState(), 130, 24, GUITextButton.ColorPalette.OK, Lng.str("REPAIR SHIP"), new GUICallback() {
			@Override
			public boolean isOccluded() {
				return !isActive();
			}

			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if (event.pressedLeftMouse()) {
					for (Fleet f : getState().getFleetManager().getAvailableFleetsClient()) {
						if (f.dbid == s.fleetDbId()) {
							RepairDialogs.orderRepair(getState(), f, s.uid(), s.name());
						}
					}
				}
			}
		});
		repair.setPos(140, 4, 0);
		c.attach(repair);
		return c;
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
