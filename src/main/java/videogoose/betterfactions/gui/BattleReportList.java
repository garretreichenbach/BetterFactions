package videogoose.betterfactions.gui;

import org.schema.game.client.data.GameClientState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.config.GuiDateFormats;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.battle.BattleReports;

import javax.vecmath.Vector4f;
import java.util.*;

/**
 * The faction's battle reports, newest first, under Assets. A row is the verdict and the losses on each
 * side; clicking it opens the full report with a way to go there. The faction news only carries the
 * verdict and points here.
 */
public class BattleReportList extends ScrollableTableList<BattleReports.Report> {

	private static final Vector4f WON = new Vector4f(0.03f, 0.76f, 0.33f, 1.0f);
	private static final Vector4f LOST = new Vector4f(0.85f, 0.25f, 0.22f, 1.0f);
	/** Lighter than a held or lost field: nobody was driven off, one side just bled less. */
	private static final Vector4f BETTER = new Vector4f(0.45f, 0.82f, 0.55f, 1.0f);
	private static final Vector4f WORSE = new Vector4f(0.88f, 0.55f, 0.50f, 1.0f);
	private static final Vector4f DRAWN = new Vector4f(0.44f, 0.48f, 0.52f, 1.0f);

	private final GUIElement parent;
	/** The answer version this list was built from; a reply landing after the first draw rebuilds it. */
	private int builtFrom = -1;

	public BattleReportList(InputState state, GUIElement p) {
		super(state, 100, 100, p);
		this.parent = p;
	}

	@Override
	public GameClientState getState() {
		return (GameClientState) super.getState();
	}

	private int own() {
		return getState().getPlayer().getFactionId();
	}

	@Override
	public void initColumns() {
		addColumn(Lng.str("When"), 1.6f, (a, b) -> Long.compare(b.time(), a.time()));
		addColumn(Lng.str("Against"), 2.4f, (a, b) -> a.opponents(own()).compareToIgnoreCase(b.opponents(own())));
		addColumn(Lng.str("Result"), 2.4f, (a, b) -> a.verdict().compareToIgnoreCase(b.verdict()));
		addFixedWidthColumn(Lng.str("Losses"), 56, (a, b) -> a.lostBy(own()) - b.lostBy(own()));
		addFixedWidthColumn(Lng.str("Kills"), 56, (a, b) -> a.lostByOthers(own()) - b.lostByOthers(own()));
		addFixedWidthColumn(Lng.str("Sector"), 110, Comparator.comparing(BattleReports.Report::pos));

		addButton(new GUICallback() {
			@Override
			public boolean isOccluded() {
				return !isActive();
			}

			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if (event.pressedLeftMouse()) {
					BattleReports.request(true);
				}
			}
		}, new Object() {
			@Override
			public String toString() {
				return BattleReports.received() == null ? Lng.str("ASKING...") : Lng.str("REFRESH");
			}
		}, GUIHorizontalArea.HButtonType.BUTTON_GREY_MEDIUM, ControllerElement.FilterRowStyle.FULL, ControllerElement.FilterPos.BOTTOM);
		activeSortColumnIndex = 0;
	}

	@Override
	public void draw() {
		BattleReports.request(false);
		if (BattleReports.version() != builtFrom) {
			builtFrom = BattleReports.version();
			flagDirty();
		}
		super.draw();
	}

	@Override
	protected Collection<BattleReports.Report> getElementList() {
		List<BattleReports.Report> r = BattleReports.received();
		return r == null ? Collections.emptyList() : r;
	}

	@Override
	public void updateListEntries(GUIElementList mainList, Set<BattleReports.Report> collection) {
		mainList.deleteObservers();
		mainList.addObserver(this);
		int own = own();
		for (BattleReports.Report rep : collection) {
			ReportRow r = new ReportRow(getState(), rep,
					cell(GuiDateFormats.factionNewsTime.format(new Date(rep.time()))), cell(rep.opponents(own)), cell(rep.verdict()),
					cell(String.valueOf(rep.lostBy(own))), cell(String.valueOf(rep.lostByOthers(own))), cell(rep.pos().toStringPure()));
			GUIAnchor expanded = expandedFor(rep);
			r.expanded = new GUIElementList(getState());
			r.expanded.add(new GUIListElement(expanded, expanded, getState()));
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

	/** The full report, sized to its text, and a waypoint to where it was fought. */
	private GUIAnchor expandedFor(BattleReports.Report rep) {
		String text = rep.body() + (rep.soft() ? "\n" + Lng.str("Fought with no player near; simplified.") : "");
		GUITextOverlayTableInnerDescription body = new GUITextOverlayTableInnerDescription(10, 10, getState());
		body.setTextSimple(text);
		body.onInit();
		body.updateTextSize();
		int textHeight = Math.max(16, body.getTextHeight()) + 8;
		GUIAnchor c = new GUIAnchor(getState(), 100, textHeight + 30) {
			@Override
			public void draw() {
				setWidth(parent.getWidth());
				super.draw();
			}
		};
		body.setPos(4, 2, 0);

		GUITextButton nav = new GUITextButton(getState(), 90, 24, GUITextButton.ColorPalette.OK, Lng.str("NAV TO"), new GUICallback() {
			@Override
			public boolean isOccluded() {
				return !isActive();
			}

			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if (event.pressedLeftMouse()) {
					getState().getController().getClientGameData().setWaypoint(rep.pos());
				}
			}
		});
		nav.setPos(4, textHeight, 0);
		c.attach(body);
		c.attach(nav);
		return c;
	}

	private class ReportRow extends ScrollableTableList<BattleReports.Report>.Row {
		private final BattleReports.Report report;

		ReportRow(InputState state, BattleReports.Report r, GUIElement... elements) {
			super(state, r, elements);
			report = r;
			highlightSelect = true;
		}

		/** Green for a field we held, red for one someone else did, lighter for coming off better or worse, grey for even. */
		@Override
		protected Vector4f getAccentColor() {
			if (report.winner() == Integer.MIN_VALUE) {
				return report.favoured() == Integer.MIN_VALUE ? DRAWN : report.favoured() == own() ? BETTER : WORSE;
			}
			return report.winner() == own() ? WON : LOST;
		}
	}
}
