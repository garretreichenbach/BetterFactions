package videogoose.betterfactions.gui;

import org.schema.common.util.StringTools;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITextOverlayTable;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Supplier;

/**
 * The Mass column on the vanilla fleet menu's two lists, built here because a mixin cannot carry anonymous
 * classes of its own. The cells read live: vanilla only rebuilds a row when its fleet changes, not its mass.
 */
public final class FleetMassColumns {

	private FleetMassColumns() {
	}

	/** A fleet's ships in their own right: one docked in a carrier is in its carrier's mass already. */
	public static double mass(Fleet f) {
		double m = 0;
		for (FleetMember fm : f.getMembers()) {
			m += fm.getDockedToRootDbId() > 0 ? 0 : fm.getMass();
		}
		return m;
	}

	public static final int WIDTH = 110;
	public static final Comparator<Fleet> FLEET_ORDER = Comparator.comparingDouble(FleetMassColumns::mass);
	public static final Comparator<FleetMember> MEMBER_ORDER = Comparator.comparingDouble(FleetMember::getMass);

	public static String title() {
		return Lng.str("Mass");
	}

	public static GUIElement[] withFleetCell(ScrollableTableList<?> list, Fleet f, GUIElement[] cells) {
		return append(list, cells, () -> StringTools.massFormat(mass(f)));
	}

	public static GUIElement[] withMemberCell(ScrollableTableList<?> list, FleetMember m, GUIElement[] cells) {
		return append(list, cells, () -> StringTools.massFormat(m.getMass()));
	}

	private static GUIElement[] append(ScrollableTableList<?> list, GUIElement[] cells, Supplier<String> text) {
		GUITextOverlayTable t = new GUITextOverlayTable(10, 10, list.getState());
		t.setTextSimple(new Object() {
			@Override
			public String toString() {
				return text.get();
			}
		});
		t.setPos(4, 5, 0);
		ScrollableTableList<?>.GUIClippedRow c = list.new GUIClippedRow(list.getState());
		c.attach(t);
		GUIElement[] out = Arrays.copyOf(cells, cells.length + 1);
		out[cells.length] = c;
		return out;
	}
}
