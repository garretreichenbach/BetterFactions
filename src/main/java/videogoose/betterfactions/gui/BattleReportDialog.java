package videogoose.betterfactions.gui;

import org.schema.common.util.StringTools;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.battle.BattleReports;

import java.util.*;

/**
 * A battle report in full: our ships and stations in one table, everyone else's in the other, each as
 * last seen in the fight. The units are asked for when this opens; the list of reports does not carry them.
 */
public class BattleReportDialog extends PlayerInput {

	private final GUIInputPanel panel;
	private UnitList ours;
	private UnitList theirs;

	public BattleReportDialog(GameClientState state, BattleReports.Report report) {
		super(state);
		BattleReports.requestDetail(report);
		int own = state.getPlayer().getFactionId();
		panel = new GUIInputPanel("BF_BATTLE_REPORT", state, 900, 600, this,
				Lng.str("%s (%s)", report.name(), report.pos().toStringPure()), "") {
			@Override
			public void onInit() {
				super.onInit();
				GUIContentPane pane = ((GUIDialogWindow) background).getMainContentPane();
				pane.setTextBoxHeightLast(250);
				pane.addNewTextBox(10);
				ours = new UnitList(getState(), pane.getContent(0), report, u -> u.faction() == own);
				ours.onInit();
				pane.getContent(0).attach(ours);
				theirs = new UnitList(getState(), pane.getContent(1), report, u -> u.faction() != own);
				theirs.onInit();
				pane.getContent(1).attach(theirs);
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
		if (ours != null) {
			ours.cleanUp();
			theirs.cleanUp();
		}
	}

	@Override
	public void handleMouseEvent(MouseEvent e) {
	}

	private static class UnitList extends ScrollableTableList<BattleReports.Unit> {

		private final BattleReports.Report report;
		private final java.util.function.Predicate<BattleReports.Unit> side;
		/** The answer this was built from; it rebuilds when the server's lands. */
		private List<BattleReports.Unit> builtFrom;

		UnitList(InputState state, GUIElement p, BattleReports.Report report, java.util.function.Predicate<BattleReports.Unit> side) {
			super(state, 100, 100, p);
			this.report = report;
			this.side = side;
		}

		private String faction(int id) {
			for (BattleReports.Line l : report.sides()) {
				if (l.faction() == id) {
					return l.name();
				}
			}
			return Lng.str("Faction %s", id);
		}

		private static String type(byte t) {
			SimpleTransformableSendableObject.EntityType e = t >= 0 && t < SimpleTransformableSendableObject.EntityType.byDbId.length
					? SimpleTransformableSendableObject.EntityType.byDbId[t] : null;
			return e != null ? e.getName() : "?";
		}

		@Override
		public void initColumns() {
			addColumn(Lng.str("Name"), 2.4f, (a, b) -> a.name().compareToIgnoreCase(b.name()));
			addColumn(Lng.str("Faction"), 1.6f, (a, b) -> faction(a.faction()).compareToIgnoreCase(faction(b.faction())));
			addFixedWidthColumn(Lng.str("Type"), 90, (a, b) -> type(a.type()).compareTo(type(b.type())));
			addFixedWidthColumn(Lng.str("Mass"), 100, (a, b) -> Float.compare(b.mass(), a.mass()));
			addFixedWidthColumn(Lng.str("Health"), 70, (a, b) -> Float.compare(a.hp(), b.hp()));
			addFixedWidthColumn(Lng.str("Rating"), 70, (a, b) -> Double.compare(b.rating(), a.rating()));
			addFixedWidthColumn(Lng.str("Status"), 90, (a, b) -> Boolean.compare(b.lost(), a.lost()));
			activeSortColumnIndex = 3;
		}

		@Override
		public void draw() {
			if (BattleReports.detail(report) != builtFrom) {
				builtFrom = BattleReports.detail(report);
				flagDirty();
			}
			super.draw();
		}

		@Override
		protected Collection<BattleReports.Unit> getElementList() {
			List<BattleReports.Unit> all = BattleReports.detail(report);
			List<BattleReports.Unit> out = new ArrayList<>();
			if (all != null) {
				for (BattleReports.Unit u : all) {
					if (side.test(u)) {
						out.add(u);
					}
				}
			}
			return out;
		}

		@Override
		public void updateListEntries(GUIElementList mainList, Set<BattleReports.Unit> collection) {
			mainList.deleteObservers();
			mainList.addObserver(this);
			for (BattleReports.Unit u : collection) {
				Row r = new Row(getState(), u, cell(u.name()), cell(faction(u.faction())), cell(type(u.type())),
						cell(StringTools.massFormat(u.mass())), cell(u.lost() ? "-" : Math.round(u.hp() * 100) + "%"),
						cell(String.valueOf(Math.round(u.rating()))), cell(u.lost() ? Lng.str("Destroyed") : Lng.str("Survived"))) {
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
