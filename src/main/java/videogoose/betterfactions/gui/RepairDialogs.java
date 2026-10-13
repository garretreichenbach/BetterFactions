package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerButtonTilesInput;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.inventory.inventorynew.SecondaryInventoryPanelNew;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.assets.FactionAssets;
import videogoose.betterfactions.net.BFPacket;
import videogoose.betterfactions.repair.FactionDesigns;

/**
 * The client's side of shipyard repairs: picking the yard a fleet goes to, picking the blueprint a ship is
 * rebuilt to, and watching a yard's queue.
 */
public final class RepairDialogs {

	/** The last queue the server sent: station UID, the yard's state, then one line per ship. */
	private static volatile String[] queue = new String[0];
	private static long queueAskedAt;

	private RepairDialogs() {
	}

	/** Which of our stations should this fleet repair at. Stations come from the holdings list the server sent. */
	public static void orderRepair(GameClientState state, Fleet fleet) {
		orderRepair(state, fleet, null, fleet.getName());
	}

	/** As above, but only the ship {@code shipUid} is repaired (null: the whole fleet). */
	public static void orderRepair(GameClientState state, Fleet fleet, String shipUid, String what) {
		FactionAssets assets = DiplomacyManager.client().getFactionAssets();
		if (assets == null) {
			DiplomacyManager.client().requestFactionAssets(true);
			state.getController().popupAlertTextMessage(Lng.str("Still asking the server for our stations - try again in a moment."));
			return;
		}
		PlayerButtonTilesInput a = tiles(state, Lng.str("Repair %s at which shipyard?", what));
		int n = 0;
		for (FactionAssets.Asset s : assets.assets) {
			if (s.type != SimpleTransformableSendableObject.EntityType.SPACE_STATION.dbTypeId) {
				continue;
			}
			n++;
			a.addTile(s.name, Lng.str("Sector %s. Each ship lines up, docks, and is rebuilt from its blueprint (Assets > Designs). Ships too large for the yard are skipped.", s.sector.toStringPure()),
					GUIHorizontalArea.HButtonColor.BLUE, click(a, () -> BFPacket.toServer(BFPacket.Kind.REPAIR_ORDER, new Tag(Tag.Type.STRUCT, null, new Tag[]{
							new Tag(Tag.Type.LONG, null, fleet.dbid), new Tag(Tag.Type.STRING, null, s.uid),
							new Tag(Tag.Type.STRING, null, shipUid == null ? "" : shipUid), FinishTag.INST}))), ALWAYS);
		}
		if (n == 0) {
			state.getController().popupAlertTextMessage(Lng.str("We have no stations to repair at."));
			return;
		}
		a.activate();
	}

	/** The blueprint a ship is rebuilt to: one from our catalog, or back to guessing from its name. */
	public static void pickBlueprint(GameClientState state, String shipUid, String shipName) {
		PlayerButtonTilesInput a = tiles(state, Lng.str("Blueprint for %s", shipName));
		a.addTile(Lng.str("Guess from name"), Lng.str("Use the catalog blueprint whose name best matches the ship's."),
				GUIHorizontalArea.HButtonColor.ORANGE, click(a, () -> assign(shipUid, "")), ALWAYS);
		for (CatalogPermission p : state.getPlayer().getCatalog().getAvailableCatalog()) {
			a.addTile(p.getUid(), Lng.str("%s, %s", p.type != null ? p.type.name() : "", p.ownerUID),
					GUIHorizontalArea.HButtonColor.BLUE, click(a, () -> assign(shipUid, p.getUid())), ALWAYS);
		}
		a.activate();
	}

	private static void assign(String shipUid, String blueprint) {
		BFPacket.toServer(BFPacket.Kind.DESIGN_SET, new Tag(Tag.Type.STRUCT, null, new Tag[]{
				new Tag(Tag.Type.STRING, null, shipUid), new Tag(Tag.Type.STRING, null, blueprint), FinishTag.INST}));
	}

	/** The line of ships at a station's shipyard, kept fresh while it is open. */
	public static void viewQueue(GameClientState state, String stationUid, String stationName) {
		queue = new String[0];
		queueAskedAt = 0;
		new PlayerGameOkCancelInput("BF_REPAIR_QUEUE", state, 560, 360, Lng.str("Repair queue: %s", stationName), new Object() {
			@Override
			public String toString() {
				String[] q = pollQueue(stationUid);
				if (q.length < 2 || !q[0].equals(stationUid)) {
					return Lng.str("Asking...");
				}
				StringBuilder b = new StringBuilder(q[1].isEmpty() ? "" : Lng.str("Shipyard: %s", q[1]) + "\n\n");
				if (q.length == 2) {
					b.append(Lng.str("No ships queued. Order a fleet here from Faction > Assets, or the fleet's Order menu."));
				}
				for (int i = 2; i < q.length; i++) {
					b.append(i - 1).append(". ").append(q[i]).append('\n');
				}
				return b.toString();
			}
		}) {
			@Override
			public void onDeactivate() {
			}

			@Override
			public void pressedOK() {
				deactivate();
			}
		}.activate();
	}

	/** Whether the server last said ships are queued for repair at this station; asks again every few seconds while called. */
	public static boolean queued(String stationUid) {
		long now = System.currentTimeMillis();
		if (now - queueAskedAt > 3000) {
			queueAskedAt = now;
			BFPacket.toServer(BFPacket.Kind.REPAIR_QUEUE_REQUEST, new Tag(Tag.Type.STRING, null, stationUid));
		}
		String[] q = queue;
		return q.length > 2 && q[0].equals(stationUid);
	}

	/** Asks the server for a yard's queue at most every 1.5s; returns the last one received. */
	private static String[] pollQueue(String stationUid) {
		long now = System.currentTimeMillis();
		if (now - queueAskedAt > 1500) {
			queueAskedAt = now;
			BFPacket.toServer(BFPacket.Kind.REPAIR_QUEUE_REQUEST, new Tag(Tag.Type.STRING, null, stationUid));
		}
		return queue;
	}

	public static void cancelQueue(GameClientState state, String stationUid, String stationName) {
		new PlayerGameOkCancelInput("CONFIRM", state, Lng.str("Cancel repairs"),
				Lng.str("Send every ship waiting at %s back to its fleet? A ship already inside the yard is finished first.", stationName)) {
			@Override
			public void onDeactivate() {
			}

			@Override
			public void pressedOK() {
				deactivate();
				BFPacket.toServer(BFPacket.Kind.REPAIR_CANCEL, new Tag(Tag.Type.STRING, null, stationUid));
			}
		}.activate();
	}

	public static void queueReceived(Tag tag) {
		Tag[] t = tag.getStruct();
		String[] q = new String[t.length - 1];
		for (int i = 0; i < q.length; i++) {
			q[i] = t[i].getString();
		}
		queue = q;
	}

	/** Client: asks for the faction's blueprint assignments, at most once every few seconds. */
	private static long designsAskedAt;

	public static void requestDesigns() {
		long now = System.currentTimeMillis();
		if (now - designsAskedAt > 5000) {
			designsAskedAt = now;
			BFPacket.toServer(BFPacket.Kind.DESIGNS_REQUEST, new Tag(Tag.Type.BYTE, null, (byte) 0));
		}
	}

	/** The blueprint a ship repairs to as this client sees it: assigned, else guessed; null if neither. */
	public static String blueprintOf(GameClientState state, String shipUid, String shipName) {
		//the server's rule (FactionDesigns.blueprintFor) with this player as the one ordering
		java.util.List<String> names = state.getPlayer().getCatalog().getAvailableCatalog().stream().map(CatalogPermission::getUid).toList();
		String a = FactionDesigns.client().get(shipUid);
		return a != null && names.contains(a) ? a : FactionDesigns.guess(shipName, names);
	}

	/** The fleet Order dialog's "Shipyard Repair" tile (FleetRepairOrderMixin). */
	public static void addRepairTile(PlayerButtonTilesInput a, Fleet fleet) {
		a.addTile(Lng.str("Shipyard Repair"), Lng.str("Send the fleet to one of our shipyards. Each ship lines up, docks and is rebuilt from its blueprint (Faction > Assets > Designs)."),
				GUIHorizontalArea.HButtonColor.BLUE, click(a, () -> orderRepair(GameClientState.instance, fleet)), new GUIActivationCallback() {
					@Override
					public boolean isVisible(InputState state) {
						return true;
					}

					@Override
					public boolean isActive(InputState state) {
						return fleet != null && !fleet.isEmpty();
					}
				});
	}

	/** The shipyard computer's "Repair Queue" and "Cancel Repairs" buttons (ShipyardQueueButtonMixin). */
	public static void addQueueButtons(GUIHorizontalButtonTablePane ac, SegmentController station, SecondaryInventoryPanelNew panel) {
		ac.addButton(2, 0, Lng.str("Repair Queue"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, button(panel, () ->
				viewQueue(GameClientState.instance, station.getUniqueIdentifier(), station.getRealName())), null);
		ac.addButton(2, 1, Lng.str("Cancel Repairs"), GUIHorizontalArea.HButtonType.BUTTON_RED_MEDIUM, button(panel, () ->
				cancelQueue(GameClientState.instance, station.getUniqueIdentifier(), station.getRealName())), new GUIActivationCallback() {
			@Override
			public boolean isVisible(InputState state) {
				return true;
			}

			@Override
			public boolean isActive(InputState state) {
				String[] q = pollQueue(station.getUniqueIdentifier());
				return q.length > 2 && q[0].equals(station.getUniqueIdentifier());
			}
		});
	}

	private static GUICallback button(SecondaryInventoryPanelNew panel, Runnable r) {
		return new GUICallback() {
			@Override
			public boolean isOccluded() {
				return !panel.isActive();
			}

			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if (event.pressedLeftMouse()) {
					r.run();
				}
			}
		};
	}

	private static PlayerButtonTilesInput tiles(GameClientState state, String title) {
		return new PlayerButtonTilesInput("BF_REPAIR_PICK", state, 800, 640, title, 260, 120) {
			@Override
			public void onDeactivate() {
			}
		};
	}

	private static GUICallback click(PlayerButtonTilesInput a, Runnable r) {
		return new GUICallback() {
			@Override
			public boolean isOccluded() {
				return !a.isActive();
			}

			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if (event.pressedLeftMouse()) {
					r.run();
					a.deactivate();
				}
			}
		};
	}

	private static final GUIActivationCallback ALWAYS = new GUIActivationCallback() {
		@Override
		public boolean isVisible(InputState state) {
			return true;
		}

		@Override
		public boolean isActive(InputState state) {
			return true;
		}
	};
}
