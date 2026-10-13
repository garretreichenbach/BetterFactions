package videogoose.betterfactions.gui;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.input.InputState;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITextOverlayTable;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.battle.BattleReports;
import videogoose.betterfactions.diplomacy.FactionTreaty;
import videogoose.betterfactions.net.BFPacket;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A war's battles, for anyone in the faction: the fights against the enemy going on right now above,
 * the reports of those fought since the war began below. The players' side of the admin profiler's
 * BATTLES tab, without the server load or the controls.
 */
public class WarBattlesDialog extends PlayerInput {

	/** One battle going on now; updated in place, keyed by sector, so its button is not rebuilt every answer. */
	static final class Live {
		final Vector3i pos;
		String sides = "", age = "";
		int lost, killed;
		/** Adjacent sectors fought over as part of it. */
		int more;

		Live(Vector3i pos) {
			this.pos = pos;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Live l && l.pos.equals(pos);
		}

		@Override
		public int hashCode() {
			return pos.hashCode();
		}
	}

	/** Client: the last answer, and a counter that moves when a battle starts or ends. Main thread only. */
	private static Object2ObjectLinkedOpenHashMap<Vector3i, Live> live = new Object2ObjectLinkedOpenHashMap<>();
	private static int version;
	private static long requestedAt;

	private final GUIInputPanel panel;
	private BattleReportList reports;
	private LiveList liveList;

	public WarBattlesDialog(GameClientState state, FactionTreaty war) {
		super(state);
		int enemy = war.other(state.getPlayer().getFactionId());
		live = new Object2ObjectLinkedOpenHashMap<>();
		version++;
		requestedAt = 0;
		BattleReports.request(true);
		panel = new GUIInputPanel("BF_WAR_BATTLES", state, 900, 600, this, Lng.str("Battles of %s", war.name), "") {
			@Override
			public void onInit() {
				super.onInit();
				GUIContentPane pane = ((GUIDialogWindow) background).getMainContentPane();
				pane.setTextBoxHeightLast(140);
				pane.addNewTextBox(10);
				liveList = new LiveList(getState(), pane.getContent(0), enemy);
				liveList.onInit();
				pane.getContent(0).attach(liveList);
				reports = new BattleReportList(getState(), pane.getContent(1), r -> r.involves(enemy) && r.time() >= war.signedAt);
				reports.onInit();
				pane.getContent(1).attach(reports);
			}
		};
		panel.setCancelButton(false);
	}

	/** Client: asks for the live battles against {@code enemy} every two seconds while the dialog is drawn. */
	private static void request(int enemy) {
		long now = System.currentTimeMillis();
		if (now - requestedAt > 2000) {
			requestedAt = now;
			BFPacket.toServer(BFPacket.Kind.LIVE_BATTLES_REQUEST, new Tag(Tag.Type.INT, null, enemy));
		}
	}

	/** Client: the server's answer; see {@code BattleTracker.liveTag}. */
	public static void received(Tag tag) {
		Tag[] t = tag.getStruct();
		Object2ObjectLinkedOpenHashMap<Vector3i, Live> fresh = new Object2ObjectLinkedOpenHashMap<>();
		for (int i = 0; i < t.length - 1; i++) {
			Tag[] e = t[i].getStruct();
			Vector3i pos = (Vector3i) e[0].getValue();
			Live l = live.get(pos);
			if (l == null) {
				l = new Live(pos);
			}
			l.sides = e[1].getString();
			l.age = e[2].getString();
			l.lost = e[3].getInt();
			l.killed = e[4].getInt();
			l.more = e.length > 6 ? e[5].getInt() : 0;
			fresh.put(pos, l);
		}
		if (!fresh.keySet().equals(live.keySet())) {
			version++;
		}
		live = fresh;
	}

	@Override
	public GUIElement getInputPanel() {
		return panel;
	}

	@Override
	public void onDeactivate() {
		if (reports != null) {
			reports.cleanUp();
			liveList.cleanUp();
		}
	}

	@Override
	public void handleMouseEvent(MouseEvent e) {
	}

	private static class LiveList extends ScrollableTableList<Live> {

		private final int enemy;
		private int shownVersion = -1;

		LiveList(InputState state, GUIElement p, int enemy) {
			super(state, 100, 100, p);
			this.enemy = enemy;
		}

		@Override
		public void initColumns() {
			addColumn(Lng.str("Sector"), 1.2f, (a, b) -> a.pos.toString().compareTo(b.pos.toString()));
			addColumn(Lng.str("Sides"), 4.0f, (a, b) -> a.sides.compareTo(b.sides));
			addFixedWidthColumn(Lng.str("Age"), 60, (a, b) -> a.age.compareTo(b.age));
			addFixedWidthColumn(Lng.str("Losses"), 56, (a, b) -> a.lost - b.lost);
			addFixedWidthColumn(Lng.str("Kills"), 56, (a, b) -> a.killed - b.killed);
		}

		@Override
		public void draw() {
			request(enemy);
			if (shownVersion != version) {
				shownVersion = version;
				flagDirty();
			}
			super.draw();
		}

		@Override
		protected Collection<Live> getElementList() {
			return new ObjectArrayList<>(live.values());
		}

		@Override
		public void updateListEntries(GUIElementList mainList, Set<Live> collection) {
			mainList.deleteObservers();
			mainList.addObserver(this);
			for (Live l : collection) {
				GUITextButton nav = new GUITextButton(getState(), 90, 24, GUITextButton.ColorPalette.OK, Lng.str("NAV TO"), new GUICallback() {
					@Override
					public boolean isOccluded() {
						return !isActive();
					}

					@Override
					public void callback(GUIElement callingGuiElement, MouseEvent event) {
						if (event.pressedLeftMouse()) {
							((GameClientState) getState()).getController().getClientGameData().setWaypoint(l.pos);
						}
					}
				});
				//clicking the row opens it, like a report's below
				GUIAnchor actions = new GUIAnchor(getState(), 100, 30);
				nav.setPos(4, 3, 0);
				actions.attach(nav);
				Row r = new Row(getState(), l, cell(() -> l.pos.toStringPure() + (l.more > 0 ? " +" + l.more : "")), cell(() -> l.sides), cell(() -> l.age),
						cell(() -> String.valueOf(l.lost)), cell(() -> String.valueOf(l.killed))) {
					{
						highlightSelect = true;
					}
				};
				r.expanded = new GUIElementList(getState());
				r.expanded.add(new GUIListElement(actions, actions, getState()));
				r.onInit();
				mainList.addWithoutUpdate(r);
			}
			mainList.updateDim();
		}

		private GUIClippedRow cell(Supplier<String> value) {
			GUITextOverlayTable t = new GUITextOverlayTable(10, 10, getState());
			t.setTextSimple(new Object() {
				@Override
				public String toString() {
					return value.get();
				}
			});
			t.getPos().y = 5;
			GUIClippedRow c = new GUIClippedRow(getState());
			c.attach(t);
			return c;
		}
	}
}
