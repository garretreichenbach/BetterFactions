package videogoose.betterfactions.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.game.client.controller.PlayerOkCancelInput;
import org.schema.game.client.controller.manager.ingame.InGameControlManager;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITextOverlayTable;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.common.language.Lng;
import org.schema.schine.input.InputState;
import org.schema.schine.input.KeyboardMappings;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.battle.BattleTracker;
import videogoose.betterfactions.net.BFPacket;

import java.util.Collection;
import java.util.Set;

/**
 * The admin profiler's battle list, one row per battle with pause, end, teleport and watch buttons.
 *
 * <p>The server sends every row about once a second while the tab is drawn. Rows are updated in place
 * and the list is only rebuilt when a battle starts or ends, so a button is not swapped out from under
 * the cursor every second.</p>
 */
public class BattleAdminList extends ScrollableTableList<BattleAdminList.Row> {

	public static final class Row {
		final int sectorId;
		String pos = "", status = "", load = "", age = "", sides = "";
		int entities;
		boolean paused;

		Row(int sectorId) {
			this.sectorId = sectorId;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Row r && r.sectorId == sectorId;
		}

		@Override
		public int hashCode() {
			return sectorId;
		}
	}

	/** Client: the header text and rows of the last report. Main thread only. */
	static volatile String header = "";
	private static Int2ObjectLinkedOpenHashMap<Row> rows = new Int2ObjectLinkedOpenHashMap<>();
	private static int version;
	private static long lastRequest;
	private int shownVersion = -1;
	/** Client: watching a battle from the free-roam camera (see {@code BattleTracker.watch}): 0 not, 1 soft-loaded, 2 held for load. */
	private static byte watching;
	private static long bannerShown;

	public BattleAdminList(InputState state, GUIElement p) {
		super(state, 100, 100, p);
	}

	/** Client: a report from the server. */
	public static void received(Tag tag) {
		Tag[] t = tag.getStruct();
		header = t[0].getString();
		Int2ObjectLinkedOpenHashMap<Row> fresh = new Int2ObjectLinkedOpenHashMap<>();
		for (int i = 1; i < t.length - 1; i++) {
			Tag[] e = t[i].getStruct();
			Row r = rows.get(e[0].getInt());
			if (r == null) {
				r = new Row(e[0].getInt());
			}
			r.pos = e[1].getString();
			r.status = e[2].getString();
			r.load = e[3].getString();
			r.entities = e[4].getInt();
			r.age = e[5].getString();
			r.sides = e[6].getString();
			r.paused = e[7].getByte() != 0;
			fresh.put(r.sectorId, r);
		}
		if (!fresh.keySet().equals(rows.keySet())) {
			version++;
		}
		rows = fresh;
	}

	/** Client, from the tab's draw: asks for a fresh report about once a second. */
	static void requestReport() {
		long now = System.currentTimeMillis();
		if (now - lastRequest > 1000) {
			lastRequest = now;
			BFPacket.toServer(BFPacket.Kind.BATTLES_REQUEST, new Tag(Tag.Type.BYTE, null, (byte) 0));
		}
	}

	/**
	 * Client: the server says this admin has arrived at a battle (1), the battle was held for load (2) or
	 * let go again (1), or they stopped watching (0). Swaps walking for the engine's free-roam camera
	 * and back.
	 */
	public static void setWatching(GameClientState state, byte to) {
		boolean was = watching != 0;
		watching = to;
		if (was != (to != 0)) {
			//the player controls stay on, so the nav menu (N), selecting what the camera looks at (F) and
			//the target panel all work; only the character's own movement is suspended, so WASD moves the
			//camera and not the body. Suspended rather than deactivated: the interaction manager
			//reactivates a character controller it finds off
			InGameControlManager ig = state.getGlobalGameControlManager().getIngameControlManager();
			ig.getPlayerGameControlManager().getPlayerIntercationManager().getPlayerCharacterManager().suspend(to != 0);
			ig.getFreeRoamController().setActive(to != 0);
		}
		state.getController().timeOutBigMessage(BANNER_SOFT);
		state.getController().timeOutBigMessage(BANNER_HELD);
		bannerShown = 0;
		banner(state);
	}

	private static final String BANNER_SOFT = "BF_WATCH_SOFT", BANNER_HELD = "BF_WATCH_HELD";

	/**
	 * Client, every frame of the free-roam camera: keeps the big banner up while watching. The engine's
	 * big messages fade after five seconds, so it is restarted every second.
	 */
	public static void banner(GameClientState state) {
		long now = System.currentTimeMillis();
		if (watching == 0 || now - bannerShown < 1000) {
			return;
		}
		bannerShown = now;
		String leave = Lng.str("Press %s to go back", KeyboardMappings.ENTER_SHIP.getKeyChar());
		if (watching == 2) {
			state.getController().showBigMessage(BANNER_HELD, Lng.str("BATTLE ON HOLD"),
					Lng.str("The server is busy; this fight resumes when it has time to spare. %s", leave), 0);
		}
	}

	public static boolean isWatching() {
		return watching != 0;
	}

	/** Client, from the free-roam camera: asks to stop watching and go back. */
	public static void stopWatching() {
		command(BattleTracker.CMD_WATCH, 0);
	}

	private static void command(byte cmd, Row r) {
		command(cmd, r.sectorId);
	}

	private static void command(byte cmd, int sectorId) {
		BFPacket.toServer(BFPacket.Kind.BATTLE_COMMAND, new Tag(Tag.Type.STRUCT, null, new Tag[]{
				new Tag(Tag.Type.BYTE, null, cmd), new Tag(Tag.Type.INT, null, sectorId), FinishTag.INST}));
		lastRequest = 0; //show the result on the next frame rather than up to a second later
	}

	private void confirmEnd(Row r) {
		new PlayerOkCancelInput("BF_BATTLE_END", getState(), 400, 200, "End battle at " + r.pos,
				"Report it as it stands and stop tracking it?\nIts sector is left alone for 30 minutes unless a player is there.") {
			@Override
			public void pressedOK() {
				command(BattleTracker.CMD_END, r);
				deactivate();
			}

			@Override
			public void onDeactivate() {
			}
		}.activate();
	}

	@Override
	public void draw() {
		requestReport();
		if (shownVersion != version) {
			shownVersion = version;
			flagDirty();
		}
		super.draw();
	}

	@Override
	public void initColumns() {
		addColumn("Sector", 1.2f, (a, b) -> a.pos.compareTo(b.pos));
		addColumn("State", 1.2f, (a, b) -> a.status.compareTo(b.status));
		addFixedWidthColumn("ms/tick", 64, (a, b) -> Double.compare(Double.parseDouble(a.load), Double.parseDouble(b.load)));
		addFixedWidthColumn("Ents", 44, (a, b) -> a.entities - b.entities);
		addColumn("Age", 1.6f, (a, b) -> a.age.compareTo(b.age));
		addColumn("Sides", 4.0f, (a, b) -> a.sides.compareTo(b.sides));
		addFixedWidthColumn("", 256, (a, b) -> 0);
		activeSortColumnIndex = 2;
	}

	@Override
	protected Collection<Row> getElementList() {
		return new ObjectArrayList<>(rows.values());
	}

	@Override
	public void updateListEntries(GUIElementList mainList, Set<Row> collection) {
		mainList.deleteObservers();
		mainList.addObserver(this);
		for (Row r : collection) {
			GUIAnchor actions = new GUIAnchor(getState(), 256, 24);
			GUIHorizontalButtonTablePane buttons = new GUIHorizontalButtonTablePane(getState(), 4, 1, actions);
			buttons.onInit();
			buttons.addButton(0, 0, new Object() {
				@Override
				public String toString() {
					return r.paused ? "RESUME" : "PAUSE";
				}
			}, GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, press(() -> command(BattleTracker.CMD_PAUSE, r)), null);
			buttons.addButton(1, 0, "END", GUIHorizontalArea.HButtonType.BUTTON_RED_MEDIUM, press(() -> confirmEnd(r)), null);
			buttons.addButton(2, 0, "GO TO", GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, press(() -> command(BattleTracker.CMD_TELEPORT, r)), null);
			buttons.addButton(3, 0, "WATCH", GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, press(() -> command(BattleTracker.CMD_WATCH, r)), null);
			actions.attach(buttons);

			BattleRow row = new BattleRow(getState(), r,
					cell(() -> r.pos), cell(() -> r.status), cell(() -> r.load), cell(() -> String.valueOf(r.entities)),
					cell(() -> r.age), cell(() -> r.sides), actions);
			row.onInit();
			mainList.addWithoutUpdate(row);
		}
		mainList.updateDim();
	}

	private GUIClippedRow cell(java.util.function.Supplier<String> value) {
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

	private GUICallback press(Runnable onPress) {
		return new GUICallback() {
			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if (event.pressedLeftMouse()) {
					onPress.run();
				}
			}

			@Override
			public boolean isOccluded() {
				return false;
			}
		};
	}

	private class BattleRow extends ScrollableTableList<Row>.Row {
		BattleRow(InputState state, BattleAdminList.Row r, GUIElement... elements) {
			super(state, r, elements);
			highlightSelect = true;
			highlightSelectSimple = true;
		}

		private SelectionMark mark;

		@Override
		public void onInit() {
			super.onInit();
			mark = new SelectionMark(this);
		}

		@Override
		public void draw() {
			mark.update(isSimpleSelected());
			super.draw();
		}
	}
}
