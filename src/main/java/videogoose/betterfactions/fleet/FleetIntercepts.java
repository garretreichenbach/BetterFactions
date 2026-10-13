package videogoose.betterfactions.fleet;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.controller.PlayerButtonTilesInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.controller.manager.ingame.map.MapControllerManager;
import org.schema.game.client.view.gui.PlayerSectorInput;
import org.schema.game.client.view.gui.RadialMenu;
import org.schema.game.client.view.gui.RadialMenuDialog;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetCommandTypes;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.network.objects.remote.FleetCommand;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.input.InputState;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.net.BFPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Server: fleets posted to a sector to intercept. A posted fleet defends its sector, and when an enemy fleet's
 * flagship is anywhere in the post's system it attacks the nearest one's sector, following it while it stays in
 * the system, then goes back to defending its post.
 *
 * <p>Any other order given to the fleet (by a player, or a battle's retreat) ends the post.</p>
 */
public final class FleetIntercepts {

	private static FleetIntercepts instance = new FleetIntercepts();
	private static final long CHECK_MS = 2000;
	/** Fleet commands apply a tick or more after they are sent: an order this fresh is not yet a player's override. */
	private static final long GRACE_MS = 5000;
	/** What a posted fleet's order reads in the fleet list and on the map (FleetLabels): holding its post, and chasing. */
	static final String HOLDING = "Intercepting", ENGAGING = "Intercepting: engaging";

	private static final class Post {
		final Vector3i sector;
		FleetCommandTypes sentType;
		Vector3i sentTo;
		long sentAt;

		Post(Vector3i sector) {
			this.sector = sector;
		}
	}

	private final Map<Long, Post> posts = new HashMap<>();
	private long lastCheck;

	public static FleetIntercepts get() {
		return instance;
	}

	public static void reset() {
		instance = new FleetIntercepts();
	}

	public String labelFor(long fleetDbId) {
		Post post = posts.get(fleetDbId);
		return post == null ? null : post.sentType == FleetCommandTypes.FLEET_ATTACK ? ENGAGING : HOLDING;
	}

	public void order(GameServerState state, PlayerState p, long fleetDbId, Vector3i sector) {
		Fleet fleet = state.getFleetManager().getByFleetDbId(fleetDbId);
		if (fleet == null || fleet.isEmpty() || !fleet.canAccess(p.getName())) {
			p.sendServerMessagePlayerInfo(new Object[]{Lng.str("You can't order that fleet.")});
			return;
		}
		Post post = new Post(new Vector3i(sector));
		posts.put(fleetDbId, post);
		send(fleet, post, FleetCommandTypes.FLEET_DEFEND, post.sector, System.currentTimeMillis());
		p.sendServerMessagePlayerInfo(new Object[]{Lng.str("%s will hold %s and intercept enemy fleets in its system.", fleet.getName(), sector.toStringPure())});
	}

	public void update(GameServerState state, long now) {
		if (now - lastCheck < CHECK_MS || posts.isEmpty()) {
			return;
		}
		lastCheck = now;
		FactionManager fm = state.getFactionManager();
		Iterator<Map.Entry<Long, Post>> it = posts.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Post> e = it.next();
			Post post = e.getValue();
			Fleet fleet = state.getFleetManager().getByFleetDbId(e.getKey());
			FleetMember flag = fleet == null ? null : fleet.getFlagShip();
			if (flag == null || overridden(fleet.getCurrentCommand(), post, now)) {
				it.remove();
				if (fleet != null) {
					FleetLabels.refresh(fleet);
				}
				continue;
			}
			Vector3i target = nearestEnemy(state, fm, fleet.getFactionId(), post.sector, flag.getSector());
			if (target != null) {
				send(fleet, post, FleetCommandTypes.FLEET_ATTACK, target, now);
			} else {
				send(fleet, post, FleetCommandTypes.FLEET_DEFEND, post.sector, now);
			}
			FleetLabels.refresh(fleet);
		}
	}

	/** The fleet's order is no longer the one this last gave it. */
	static boolean overridden(FleetCommand c, Post post, long now) {
		if (post.sentType == null || now - post.sentAt < GRACE_MS) {
			return false;
		}
		return c == null || c.getCommandType() != post.sentType || c.getArgs().length == 0 || !post.sentTo.equals(c.getArgs()[0]);
	}

	private static void send(Fleet fleet, Post post, FleetCommandTypes type, Vector3i to, long now) {
		if (type == post.sentType && to.equals(post.sentTo)) {
			return;
		}
		post.sentType = type;
		post.sentTo = new Vector3i(to);
		post.sentAt = now;
		fleet.sendFleetCommand(type, new Vector3i(to));
	}

	/** Sector of the enemy fleet flagship in {@code post}'s system closest to {@code from}, or null. */
	private static Vector3i nearestEnemy(GameServerState state, FactionManager fm, int faction, Vector3i post, Vector3i from) {
		if (faction == 0) {
			return null;
		}
		Vector3i system = VoidSystem.getContainingSystem(post, new Vector3i());
		Vector3i tmp = new Vector3i();
		Vector3i best = null;
		long bestDist = Long.MAX_VALUE;
		for (Fleet other : state.getFleetManager().fleetCache.values()) {
			FleetMember flag = other.getFlagShip();
			if (flag == null || other.getFactionId() == 0 || !fm.isEnemy(faction, other.getFactionId())) {
				continue;
			}
			Vector3i at = flag.getSector();
			if (!system.equals(VoidSystem.getContainingSystem(at, tmp))) {
				continue;
			}
			long d = Vector3i.getDistanceSquared(at, from);
			if (d < bestDist) {
				bestDist = d;
				best = at;
			}
		}
		return best;
	}

	public Tag toTag() {
		List<Tag> ts = new ArrayList<>();
		for (Map.Entry<Long, Post> e : posts.entrySet()) {
			ts.add(new Tag(Tag.Type.LONG, null, e.getKey()));
			ts.add(new Tag(Tag.Type.VECTOR3i, null, new Vector3i(e.getValue().sector)));
		}
		ts.add(FinishTag.INST);
		return new Tag(Tag.Type.STRUCT, null, ts.toArray(new Tag[0]));
	}

	/** Re-sends each post's defend order on the first update, since what was sent last is not saved. */
	public void fromTag(Tag tag) {
		posts.clear();
		Tag[] t = tag.getStruct();
		for (int i = 0; i + 1 < t.length && t[i].getType() == Tag.Type.LONG; i += 2) {
			posts.put(t[i].getLong(), new Post(t[i + 1].getVector3i()));
		}
	}

	/** Client: the fleet Order dialog's "Intercept" tile (FleetRepairOrderMixin). */
	public static void addTile(PlayerButtonTilesInput a, Fleet fleet) {
		a.addTile(Lng.str("Intercept"), Lng.str("Hold a sector and attack any enemy fleet that enters its system, returning to the sector after. Any other order ends it."),
				GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
					@Override
					public boolean isOccluded() {
						return !a.isActive();
					}

					@Override
					public void callback(GUIElement callingGuiElement, MouseEvent event) {
						if (event.pressedLeftMouse()) {
							a.deactivate();
							askSector(fleet);
						}
					}
				}, new GUIActivationCallback() {
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

	/** Client: the map radial's "Intercept" item, posting every selected fleet to the clicked sector (MapInterceptMixin). */
	public static void addRadialItem(RadialMenu menu, RadialMenuDialog dialog, Vector3i sector) {
		menu.addItem(Lng.str("Intercept"), new GUICallback() {
			@Override
			public boolean isOccluded() {
				return false;
			}

			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if (event.pressedLeftMouse()) {
					GameClientState state = GameClientState.instance;
					for (FleetMember.FleetMemberMapIndication m : MapControllerManager.selectedFleets) {
						Fleet fleet = m.getMember().getFleetByOwner(state.getPlayerName());
						if (fleet != null) {
							orderToServer(fleet, sector);
						}
					}
					MapControllerManager.resetFleets();
					dialog.deactivate();
				}
			}
		}, ALWAYS);
	}

	private static void orderToServer(Fleet fleet, Vector3i sector) {
		BFPacket.toServer(BFPacket.Kind.INTERCEPT_ORDER, new Tag(Tag.Type.STRUCT, null, new Tag[]{
				new Tag(Tag.Type.LONG, null, fleet.dbid), new Tag(Tag.Type.VECTOR3i, null, new Vector3i(sector)), FinishTag.INST}));
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

	private static void askSector(Fleet fleet) {
		new PlayerSectorInput(GameClientState.instance, Lng.str("Intercept from which sector?"), Lng.str("Enter the sector to hold (e.g. 10, 20, 111 [or 10 20 111][or 10.20.100])"), "") {
			@Override
			public void handleEnteredEmpty() {
				deactivate();
			}

			@Override
			public void handleEntered(Vector3i p) {
				orderToServer(fleet, p);
			}

			@Override
			public Object getSelectCoordinateButtonText() {
				return Lng.str("USE");
			}
		}.activate();
	}
}
