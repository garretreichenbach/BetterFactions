package videogoose.betterfactions.fleet;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.controller.PlayerButtonTilesInput;
import org.schema.game.client.controller.manager.ingame.map.MapControllerManager;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.RadialMenu;
import org.schema.game.client.view.gui.RadialMenuDialog;
import org.schema.game.common.controller.trade.TradeActive;
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
 * Server: fleets put to work on the trade routes. A fleet ordered to <b>escort trade</b> waits at the sector it
 * was ordered from, then shadows the nearest delivery its faction is party to, defending whatever sector the
 * convoy's flagship is in, start to finish; then the next. A fleet ordered to <b>raid trade routes</b> is given
 * sectors, and patrols the systems holding them in turn, on a loop. It attacks the nearest delivery in any of
 * those systems that is neither its faction's nor a friend's, following the convoy until it is gone or out of them.
 *
 * <p>Deliveries are real fleets (the engine's {@code TradeActive}): a faction's own, or a Trading Guild fleet.
 * The engine has no order to follow another fleet, so the order is re-issued as the convoy moves, as
 * {@link FleetIntercepts} does. Any other order given to the fleet ends it.</p>
 */
public final class TradeFleetOrders {

	private static TradeFleetOrders instance = new TradeFleetOrders();
	private static final long CHECK_MS = 2000;
	private static final long GRACE_MS = 5000;
	public static final int MAX_ROUTE = 16;

	public enum Kind {ESCORT, RAID}

	private static final class Post {
		final Kind kind;
		/** An escort: where it waits between convoys. */
		final Vector3i home;
		/** A raid: the sectors it was given; it hunts in the systems holding them and patrols them in turn. */
		final List<Vector3i> route = new ArrayList<>();
		/** A raid: the factions whose deliveries it hits, or none for any that are not ours or a friend's. */
		final java.util.Set<Integer> targets = new java.util.HashSet<>();
		/** A raid: it also hits fleets mining in its systems, under the same rules as deliveries. */
		boolean miners;
		/** A raid: blocks seized, and their list value, since last reported to the fleet's owner; and whose. */
		int seized;
		long seizedValue;
		String seizedFrom;
		/** A raid: what it took from its current victims since their last news of it, by block, and from whom. */
		final it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap stolen = new it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap();
		long stolenValue;
		final java.util.Set<Integer> victims = new java.util.HashSet<>();
		String victimConvoy;
		Vector3i victimAt;
		long lastHitAt;
		int leg;
		/** The convoy fleet it is on, or -1. */
		long convoy = -1;
		FleetCommandTypes sentType;
		Vector3i sentTo;
		long sentAt;

		Post(Kind kind, Vector3i home) {
			this.kind = kind;
			this.home = home;
		}
	}

	private final Map<Long, Post> posts = new HashMap<>();
	private long lastCheck;
	/** Sectors where a raided convoy's ship overheated lately, to the raiding fleet: loose cargo there is fair game for a while. */
	private final Map<Vector3i, long[]> wreckSites = new HashMap<>();
	private static final long SALVAGE_MS = 3 * 60_000L;

	public static TradeFleetOrders get() {
		return instance;
	}

	public static void reset() {
		instance = new TradeFleetOrders();
	}

	/** Server: a player gave the fleet an order of their own (FleetCommandReceiveMixin); this one is over. */
	public void cancel(Fleet fleet) {
		if (posts.remove(fleet.dbid) != null) {
			FleetLabels.refresh(fleet);
		}
	}

	/** World tests (BattleWorldTests): puts the fleet on a raid of the route's systems, no player needed. */
	public void raidForTest(Fleet fleet, List<Vector3i> route, java.util.Set<Integer> targets, boolean miners) {
		Post post = new Post(Kind.RAID, new Vector3i(route.get(0)));
		for (Vector3i s : route) {
			post.route.add(new Vector3i(s));
		}
		post.targets.addAll(targets);
		post.miners = miners;
		posts.put(fleet.dbid, post);
		send(fleet, post, FleetCommandTypes.FLEET_DEFEND, post.route.get(0), System.currentTimeMillis());
		FleetLabels.refresh(fleet);
	}

	/** The fleet a raiding fleet is on (a convoy, or miners), or -1. */
	public long preyOf(long fleetDbId) {
		Post post = posts.get(fleetDbId);
		return post != null && post.kind == Kind.RAID ? post.convoy : -1;
	}

	/**
	 * Server: whether two fleets are a raider and the fleet it is on, either way round. They fight as enemies
	 * (RaidHostilityMixin, BattleTracker) though their factions need not be at war: a raid needs no war.
	 */
	public boolean raiding(long a, long b) {
		return a >= 0 && b >= 0 && (preyOf(a) == b || preyOf(b) == a);
	}

	/**
	 * Server: whether the fleet is on a raid against the faction: its prey, and the escorts and defenders who come
	 * for the raider, are its enemies. The raider's own faction is not at war with them for it.
	 */
	public boolean raidingFaction(Fleet raider, int faction) {
		long prey = preyOf(raider.dbid);
		Fleet f = prey < 0 ? null : GameServerState.instance.getFleetManager().getByFleetDbId(prey);
		return f != null && faction != 0 && factionOf(f) == faction;
	}

	/** Server, for /raid_status: each raid's prey and the fleets in its systems, with why each is or isn't one it takes. */
	public List<String> explain(GameServerState state) {
		List<String> out = new ArrayList<>();
		FactionManager fm = state.getFactionManager();
		LongOpenHashSet inTrade = new LongOpenHashSet();
		for (TradeActive t : state.getGameState().getTradeManager().getTradeActiveMap().getTradeList()) {
			inTrade.add(t.getFleetId());
		}
		for (Map.Entry<Long, Post> e : posts.entrySet()) {
			Post post = e.getValue();
			Fleet raider = state.getFleetManager().getByFleetDbId(e.getKey());
			if (post.kind != Kind.RAID || raider == null) {
				continue;
			}
			FleetCommand c = raider.getCurrentCommand();
			out.add(raider.getName() + " (faction " + factionOf(raider) + "): prey " + post.convoy + ", miners " + post.miners + ", targets " + post.targets
					+ ", sent " + post.sentType + " " + post.sentTo + ", current " + (c == null ? "none" : c.getCommandType() + " " + java.util.Arrays.toString(c.getArgs())));
			for (Fleet f : new ArrayList<>(state.getFleetManager().fleetCache.values())) {
				FleetMember flag = f.getFlagShip();
				if (f == raider || flag == null || !inRoute(post.route, flag.getSector())) {
					continue;
				}
				FleetCommand fc = f.getCurrentCommand();
				boolean mining = fc != null && fc.getCommandType() == FleetCommandTypes.MINE_IN_SECTOR
						|| f.isNPCFleet() && f.getNpcType() == org.schema.game.server.data.simulation.npc.geo.NPCSystemFleetManager.FleetType.MINING;
				int fac = factionOf(f);
				out.add("  " + f.getName() + " at " + flag.getSector().toStringPure() + ", faction " + fac + ", npc " + f.isNPCFleet() + "/" + f.isNPCFleetGeneral()
						+ (f.isAnyNPCFleet() ? " " + f.getNpcType() : "") + ", command " + (fc == null ? "none" : fc.getCommandType())
						+ ", trading " + inTrade.contains(f.dbid) + ", mining " + mining + ", raidable " + raidable(factionOf(raider), fac, fac, fac, fm::isFriend, post.targets));
			}
		}
		if (out.isEmpty()) {
			out.add("No fleets are raiding.");
		}
		return out;
	}

	/** Server: whether a fleet of one faction is raiding a fleet of the other, either way round. */
	public boolean raidingFactions(int a, int b) {
		for (Map.Entry<Long, Post> e : posts.entrySet()) {
			Post post = e.getValue();
			if (post.kind != Kind.RAID || post.convoy < 0) {
				continue;
			}
			Fleet raider = GameServerState.instance.getFleetManager().getByFleetDbId(e.getKey());
			Fleet prey = GameServerState.instance.getFleetManager().getByFleetDbId(post.convoy);
			if (raider != null && prey != null && (factionOf(raider) == a && factionOf(prey) == b
					|| factionOf(raider) == b && factionOf(prey) == a)) {
				return true;
			}
		}
		return false;
	}

	/** A fleet's faction. An NPC faction's fleet knows its own: the engine reads it off the flagship, 0 while that is unloaded. */
	public static int factionOf(Fleet f) {
		return f.isAnyNPCFleet() ? f.getNpcFaction() : f.getFactionId();
	}

	public boolean anyRaids() {
		return !posts.isEmpty();
	}

	public String labelFor(long fleetDbId) {
		Post post = posts.get(fleetDbId);
		if (post == null) {
			return null;
		}
		return post.kind == Kind.ESCORT ? (post.convoy >= 0 ? "ESCORTING CONVOY" : "AWAITING CONVOY") : (post.convoy >= 0 ? "RAIDING CONVOY" : "RAIDING TRADE ROUTES");
	}

	// ---- rules, kept pure for TradeFleetOrders_test

	/**
	 * Whether a raid may hit a delivery. With targets chosen: one of them is on it, and the raider's own
	 * faction is not; no war needed. With none: nobody on it is the raider's faction or a friend of it, and the
	 * Trading Guild is not buying or selling (it may still fly someone else's goods).
	 */
	static boolean raidable(int raider, int from, int to, int convoyFaction, java.util.function.BiPredicate<Integer, Integer> friends,
							java.util.Set<Integer> targets) {
		if (targets.isEmpty() && (from == FactionManager.TRAIDING_GUILD_ID || to == FactionManager.TRAIDING_GUILD_ID)) {
			return false;
		}
		boolean named = false;
		for (int f : new int[]{from, to, convoyFaction}) {
			if (f == raider || targets.isEmpty() && f != 0 && friends.test(raider, f)) {
				return false;
			}
			named |= targets.contains(f);
		}
		return targets.isEmpty() || named;
	}

	/** Whether a sector is in one of the route's systems. */
	static boolean inRoute(List<Vector3i> route, Vector3i sector) {
		Vector3i system = VoidSystem.getContainingSystem(sector, new Vector3i());
		Vector3i tmp = new Vector3i();
		for (Vector3i r : route) {
			if (system.equals(VoidSystem.getContainingSystem(r, tmp))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sectors as typed: groups of three whole numbers, any separators between them ({@code 10, 20, 111; 30 4 5}).
	 * A trailing group of fewer than three is ignored.
	 */
	public static List<Vector3i> parseSectors(String s) {
		List<Integer> n = new ArrayList<>();
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\d+").matcher(s == null ? "" : s);
		while (m.find()) {
			n.add(Integer.parseInt(m.group()));
		}
		List<Vector3i> out = new ArrayList<>();
		for (int i = 0; i + 2 < n.size(); i += 3) {
			out.add(new Vector3i(n.get(i), n.get(i + 1), n.get(i + 2)));
		}
		return out;
	}

	/** Whether an escort covers a delivery: its faction is buying or selling. */
	static boolean escortable(int escort, int from, int to) {
		return escort != 0 && (from == escort || to == escort);
	}

	// ---- server

	/**
	 * @param sectors a raid's: the systems holding them are hunted, in this order
	 * @param append  a raid already on goes on, with these systems added to its route
	 */
	public void order(GameServerState state, PlayerState p, long fleetDbId, Kind kind, List<Vector3i> sectors, boolean append, java.util.Set<Integer> targets,
					  boolean miners) {
		Fleet fleet = state.getFleetManager().getByFleetDbId(fleetDbId);
		if (fleet == null || fleet.isEmpty() || !fleet.canAccess(p.getName()) || factionOf(fleet) == 0) {
			p.sendServerMessagePlayerInfo(new Object[]{Lng.str("You can't order that fleet; it must be a faction's.")});
			return;
		}
		if (kind == Kind.RAID && sectors.isEmpty()) {
			p.sendServerMessagePlayerInfo(new Object[]{Lng.str("A raid needs at least one sector.")});
			return;
		}
		Post post = posts.get(fleetDbId);
		boolean adding = append && post != null && post.kind == Kind.RAID && kind == Kind.RAID;
		if (!adding) {
			//an escort waits where it is
			post = new Post(kind, kind == Kind.ESCORT ? new Vector3i(fleet.getFlagShip().getSector()) : new Vector3i(sectors.get(0)));
		}
		for (Vector3i s : sectors) {
			if (kind == Kind.RAID && post.route.size() < MAX_ROUTE && !inRoute(post.route, s)) {
				post.route.add(new Vector3i(s));
			}
		}
		if (!adding && kind == Kind.RAID) {
			targets.remove(factionOf(fleet));
			post.targets.addAll(targets);
			post.miners = miners;
		}
		if (!adding) {
			posts.put(fleetDbId, post);
			send(fleet, post, FleetCommandTypes.FLEET_DEFEND, kind == Kind.ESCORT ? post.home : post.route.get(0), System.currentTimeMillis());
		}
		FleetLabels.refresh(fleet);
		p.sendServerMessagePlayerInfo(new Object[]{kind == Kind.ESCORT
				? Lng.str("%s will escort our faction's trade deliveries, one at a time, waiting at %s between them.", fleet.getName(), post.home.toStringPure())
				: (post.targets.isEmpty() ? Lng.str("%s raids trade deliveries that are not ours or our friends' in %s systems, patrolling them in turn.", fleet.getName(), post.route.size())
						: Lng.str("%s raids the trade deliveries of %s chosen factions in %s systems, patrolling them in turn.", fleet.getName(), post.targets.size(), post.route.size()))});
	}

	public void update(GameServerState state, long now) {
		if (now - lastCheck < CHECK_MS || posts.isEmpty()) {
			return;
		}
		lastCheck = now;
		sweepWreckSites(state, now);
		FactionManager fm = state.getFactionManager();
		List<TradeActive> trades = new ArrayList<>(state.getGameState().getTradeManager().getTradeActiveMap().getTradeList());
		LongOpenHashSet inTrade = new LongOpenHashSet();
		for (TradeActive t : trades) {
			if (t.getFleetId() >= 0) {
				inTrade.add(t.getFleetId());
			}
		}
		LongOpenHashSet mining = new LongOpenHashSet();
		for (Fleet f : new ArrayList<>(state.getFleetManager().fleetCache.values())) {
			FleetCommand c = f.getCurrentCommand();
			//an NPC faction's miners move between sectors and mine in turn: count them throughout
			if (c != null && c.getCommandType() == FleetCommandTypes.MINE_IN_SECTOR
					|| f.isNPCFleet() && f.getNpcType() == org.schema.game.server.data.simulation.npc.geo.NPCSystemFleetManager.FleetType.MINING) {
				mining.add(f.dbid);
			}
		}
		Iterator<Map.Entry<Long, Post>> it = posts.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Post> e = it.next();
			Post post = e.getValue();
			Fleet fleet = state.getFleetManager().getByFleetDbId(e.getKey());
			FleetMember flag = fleet == null ? null : fleet.getFlagShip();
			if (flag == null || overridden(fleet.getCurrentCommand(), post, now)) {
				it.remove();
				if (fleet != null) {
					//the raid is over: its victims hear of it now rather than never
					post.lastHitAt = 0;
					newsForVictims(state, fleet, post, now);
					FleetLabels.refresh(fleet);
				}
				continue;
			}
			newsForVictims(state, fleet, post, now);
			Vector3i convoyAt = convoySector(state, post, inTrade, mining);
			if (convoyAt == null) {
				post.convoy = pick(state, fm, fleet, post, trades, mining, flag.getSector());
				convoyAt = convoySector(state, post, inTrade, mining);
			}
			if (convoyAt != null) {
				send(fleet, post, post.kind == Kind.ESCORT ? FleetCommandTypes.FLEET_DEFEND : FleetCommandTypes.FLEET_ATTACK, convoyAt, now);
			} else if (post.kind == Kind.RAID) {
				//patrol: on to the next system once in this one's
				Vector3i leg = post.route.get(post.leg % post.route.size());
				if (inRoute(List.of(leg), flag.getSector())) {
					post.leg = (post.leg + 1) % post.route.size();
					leg = post.route.get(post.leg);
				}
				send(fleet, post, FleetCommandTypes.FLEET_DEFEND, leg, now);
			} else {
				send(fleet, post, FleetCommandTypes.FLEET_DEFEND, post.home, now);
			}
			FleetLabels.refresh(fleet);
			report(state, fleet, post);
		}
	}

	/** Tells the raiding fleet's owner, if on, what it has seized since last time. */
	private static void report(GameServerState state, Fleet fleet, Post post) {
		if (post.seized <= 0) {
			return;
		}
		PlayerState owner = state.getPlayerFromNameIgnoreCaseWOException(fleet.getOwner());
		if (owner != null) {
			owner.sendServerMessagePlayerInfo(new Object[]{Lng.str("%s seized %s blocks of cargo from %s, worth about %s credits.", fleet.getName(),
					String.format("%,d", post.seized), post.seizedFrom == null ? Lng.str("its prey") : post.seizedFrom, String.format("%,d", post.seizedValue))});
		}
		post.seized = 0;
		post.seizedValue = 0;
	}

	/** Where the convoy (or miners) the post is on is now, or null (and the post let go of it) once it is out of trade or gone. */
	private static Vector3i convoySector(GameServerState state, Post post, LongOpenHashSet inTrade, LongOpenHashSet mining) {
		if (post.convoy < 0) {
			return null;
		}
		Fleet convoy = state.getFleetManager().getByFleetDbId(post.convoy);
		FleetMember flag = convoy == null ? null : convoy.getFlagShip();
		if (flag == null || !inTrade.contains(post.convoy) && !(post.miners && mining.contains(post.convoy))) {
			post.convoy = -1;
			return null;
		}
		//a raid lets a convoy go once it has run out of the hunting ground
		if (post.kind == Kind.RAID && !inRoute(post.route, flag.getSector())) {
			post.convoy = -1;
			return null;
		}
		return flag.getSector();
	}

	/** The nearest delivery this post may take on: for an escort, one no other escort has. -1 if none. */
	private long pick(GameServerState state, FactionManager fm, Fleet fleet, Post post, List<TradeActive> trades, LongOpenHashSet mining, Vector3i from) {
		int mine = factionOf(fleet);
		LongOpenHashSet escorted = new LongOpenHashSet();
		for (Post other : posts.values()) {
			if (other != post && other.kind == Kind.ESCORT && other.convoy >= 0) {
				escorted.add(other.convoy);
			}
		}
		long best = -1;
		long bestDist = Long.MAX_VALUE;
		for (TradeActive t : trades) {
			Fleet convoy = t.getFleetId() < 0 ? null : state.getFleetManager().getByFleetDbId(t.getFleetId());
			FleetMember flag = convoy == null ? null : convoy.getFlagShip();
			if (flag == null || convoy == fleet) {
				continue;
			}
			boolean ok = post.kind == Kind.ESCORT
					? escortable(mine, t.getFromFactionId(), t.getToFactionId()) && !escorted.contains(convoy.dbid)
					: raidable(mine, t.getFromFactionId(), t.getToFactionId(), factionOf(convoy), fm::isFriend, post.targets)
					&& inRoute(post.route, flag.getSector());
			if (!ok) {
				continue;
			}
			long d = Vector3i.getDistanceSquared(flag.getSector(), from);
			if (d < bestDist) {
				bestDist = d;
				best = convoy.dbid;
			}
		}
		if (post.kind == Kind.RAID && post.miners) {
			//a mining fleet is its own faction's trade: buyer, seller and carrier at once
			for (long id : mining) {
				Fleet miners = state.getFleetManager().getByFleetDbId(id);
				FleetMember flag = miners == null ? null : miners.getFlagShip();
				int f = miners == null ? 0 : factionOf(miners);
				if (flag == null || miners == fleet || !raidable(mine, f, f, f, fm::isFriend, post.targets) || !inRoute(post.route, flag.getSector())) {
					continue;
				}
				long d = Vector3i.getDistanceSquared(flag.getSector(), from);
				if (d < bestDist) {
					bestDist = d;
					best = id;
				}
			}
		}
		return best;
	}

	// ---- raids: what the raiders take

	/**
	 * Server: a ship overheated. If it flies in a convoy a raid is on, the raiders' loaded ships in its sector
	 * take its cargo, as much as their cargo space holds: from its holds, or for a Trading Guild convoy whose
	 * goods ride on paper, its share of the delivery. The sector's loose cargo is theirs to sweep for a while.
	 */
	public void onOverheat(GameServerState state, org.schema.game.common.controller.SegmentController sc) {
		if (posts.isEmpty() || sc.getDbId() < 0) {
			return;
		}
		for (Map.Entry<Long, Post> e : posts.entrySet()) {
			Post post = e.getValue();
			if (post.kind != Kind.RAID || post.convoy < 0) {
				continue;
			}
			Fleet convoy = state.getFleetManager().getByFleetDbId(post.convoy);
			if (convoy == null || convoy.getMembers().stream().noneMatch(m -> m.entityDbId == sc.getDbId())) {
				continue;
			}
			Fleet raiders = state.getFleetManager().getByFleetDbId(e.getKey());
			//its victims hear of it once the raid goes quiet (newsForVictims), whether or not anything could be taken
			post.victims.addAll(hurt(state, convoy));
			if (raiders != null) {
				post.victims.remove(factionOf(raiders));
			}
			post.victimConvoy = convoy.getName();
			post.victimAt = sc.getSector(new Vector3i());
			post.lastHitAt = System.currentTimeMillis();
			List<org.schema.game.common.data.player.inventory.Inventory> holds = holds(raiders, sc.getSectorId());
			if (holds.isEmpty()) {
				return;
			}
			post.seizedFrom = convoy.getName();
			int taken = 0;
			if (sc instanceof org.schema.game.common.data.ManagedSegmentController<?> m) {
				for (var inv : new ArrayList<>(m.getManagerContainer().getInventories().inventoriesList)) {
					taken += takeAll(inv, holds, post);
				}
			}
			if (convoy.isNPCFleet()) {
				taken += takeShare(state, convoy, holds, post);
			}
			Vector3i at = sc.getSector(new Vector3i());
			if (at != null) {
				wreckSites.put(at, new long[]{e.getKey(), System.currentTimeMillis()});
				videogoose.betterfactions.trade.RouteSafety.server().onRaid(at);
			}
			if (raiders != null) {
				raided(state, factionOf(raiders), convoy);
			}
			if (taken > 0) {
				FleetLabels.refresh(raiders);
			}
			return;
		}
	}

	/**
	 * Everyone a raid hurt that it is not at war with gains a casus belli against the raider, ready at once, as
	 * Raiders' victims do; and the raider's name suffers for the attack. The Trading Guild and pirates hold no claims.
	 */
	private static void raided(GameServerState state, int raider, Fleet convoy) {
		java.util.Set<Integer> hurt = hurt(state, convoy);
		videogoose.betterfactions.DiplomacyManager dm = videogoose.betterfactions.DiplomacyManager.server();
		org.schema.game.common.data.player.faction.Faction raiderFaction = state.getFactionManager().getFaction(raider);
		long now = System.currentTimeMillis();
		for (int id : hurt) {
			org.schema.game.common.data.player.faction.Faction victim = state.getFactionManager().getFaction(id);
			if (victim == null || raiderFaction == null || id == raider || id == FactionManager.PIRATES_ID || id == FactionManager.TRAIDING_GUILD_ID
					|| dm.hasTreaty(raider, id, videogoose.betterfactions.diplomacy.TreatyTypeRegistry.WAR)) {
				continue;
			}
			dm.getReputation().attacked(raider, id, now);
			//vanilla's ATTACK only reaches the faction whose ship was shot, and not the buyer or seller of the cargo
			dm.diplomacyAction(videogoose.betterfactions.diplomacy.DiplomacyAction.DiplActionType.RAIDED, id, raider);
			var ledger = dm.diplomacyOf(victim).getEntity(raider);
			long lasts = videogoose.betterfactions.BFConfig.claimLastsMs();
			boolean had = ledger.claimReadyIn(lasts) == 0;
			if (ledger.fabricateClaim(0, lasts) && !had) {
				dm.sendServerFactionMail(Lng.str("[FACTION] Trade raided by %s", raiderFaction.getName()),
						Lng.str("%s raided a trade delivery of ours without a war. We hold a casus belli against them for %s hours.",
								raiderFaction.getName(), lasts / (60L * 60L * 1000L)), victim);
			}
		}
	}

	/** The factions a raid on the convoy hurts: its own, and the buyer's and seller's of what it carries. */
	private static java.util.Set<Integer> hurt(GameServerState state, Fleet convoy) {
		java.util.Set<Integer> hurt = new java.util.HashSet<>();
		hurt.add(factionOf(convoy));
		for (TradeActive t : state.getGameState().getTradeManager().getTradeActiveMap().getTradeList()) {
			if (t.getFleetId() == convoy.dbid) {
				hurt.add(t.getFromFactionId());
				hurt.add(t.getToFactionId());
			}
		}
		hurt.remove(0);
		return hurt;
	}

	/** The raiding fleet's loaded ships' inventories in the sector, where loot can go. */
	private static List<org.schema.game.common.data.player.inventory.Inventory> holds(Fleet raiders, int sectorId) {
		List<org.schema.game.common.data.player.inventory.Inventory> out = new ArrayList<>();
		if (raiders == null) {
			return out;
		}
		for (FleetMember m : raiders.getMembers()) {
			if (m.getLoaded() instanceof org.schema.game.common.data.ManagedSegmentController<?> ship && m.getLoaded().getSectorId() == sectorId) {
				out.addAll(ship.getManagerContainer().getInventories().inventoriesList);
			}
		}
		return out;
	}

	/** Moves every block in {@code from} into {@code to} that fits. @return blocks moved */
	private static int takeAll(org.schema.game.common.data.player.inventory.Inventory from, List<org.schema.game.common.data.player.inventory.Inventory> to, Post post) {
		int moved = 0;
		it.unimi.dsi.fastutil.ints.IntOpenHashSet changed = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
		for (short type : org.schema.game.common.data.element.ElementKeyMap.keySet) {
			int have = from.getOverallQuantity(type);
			if (have <= 0) {
				continue;
			}
			int n = put(type, have, to, post);
			if (n > 0) {
				from.decreaseBatch(type, n, changed);
				moved += n;
			}
		}
		if (!changed.isEmpty()) {
			from.sendInventoryModification(changed);
		}
		return moved;
	}

	/** A Trading Guild convoy's goods are carried on paper: each ship lost gives up its share of what is left. */
	private static int takeShare(GameServerState state, Fleet convoy, List<org.schema.game.common.data.player.inventory.Inventory> to, Post post) {
		for (TradeActive t : state.getGameState().getTradeManager().getTradeActiveMap().getTradeList()) {
			if (t.getFleetId() != convoy.dbid) {
				continue;
			}
			int ships = Math.max(1, convoy.getMembers().size());
			int moved = 0;
			for (short type : org.schema.game.common.data.element.ElementKeyMap.keySet) {
				int left = t.getBlocks().get(type);
				int share = left / ships;
				if (share <= 0) {
					continue;
				}
				int n = put(type, share, to, post);
				if (n > 0) {
					t.getBlocks().put(type, left - n);
					moved += n;
				}
			}
			return moved;
		}
		return 0;
	}

	/** Puts up to {@code count} of a block into the holds, as much as fits, on the raid's tally. @return how many went in */
	private static int put(short type, int count, List<org.schema.game.common.data.player.inventory.Inventory> to, Post post) {
		int moved = 0;
		for (var inv : to) {
			int n = count - moved;
			while (n > 0 && !inv.canPutIn(type, n)) {
				n /= 2;
			}
			if (n <= 0) {
				continue;
			}
			inv.sendInventoryModification(inv.incExistingOrNextFreeSlot(type, n));
			moved += n;
			if (moved >= count) {
				break;
			}
		}
		tally(post, type, moved);
		return moved;
	}

	/**
	 * Server: a block is about to drop into a soft-loaded battle sector, where nothing is left floating
	 * (SoftLootMixin). If the sector is a raid's, on a convoy there or where one lost a ship, the raiders' holds
	 * there take what fits; the rest is lost as before.
	 */
	public void catchDrop(org.schema.game.common.data.world.Sector sector, short type, int count) {
		if (posts.isEmpty() || type <= 0 || count <= 0) {
			return;
		}
		Vector3i at = sector.pos;
		long[] site = wreckSites.get(at);
		for (Map.Entry<Long, Post> e : posts.entrySet()) {
			Post post = e.getValue();
			if (post.kind != Kind.RAID || site == null && post.convoy < 0 || site != null && site[0] != e.getKey()) {
				continue;
			}
			List<org.schema.game.common.data.player.inventory.Inventory> holds = holds(GameServerState.instance.getFleetManager().getByFleetDbId(e.getKey()), sector.getId());
			if (!holds.isEmpty()) {
				put(type, count, holds, post);
				return;
			}
		}
	}

	/** Loose blocks floating where a raided convoy lost ships go to the raiders' holds there while they fit. */
	private void sweepWreckSites(GameServerState state, long now) {
		Iterator<Map.Entry<Vector3i, long[]>> it = wreckSites.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Vector3i, long[]> e = it.next();
			if (now - e.getValue()[1] > SALVAGE_MS) {
				it.remove();
				continue;
			}
			org.schema.game.common.data.world.Sector sector = state.getUniverse().getSectorWithoutLoading(e.getKey());
			Fleet raiders = state.getFleetManager().getByFleetDbId(e.getValue()[0]);
			if (sector == null || raiders == null) {
				continue;
			}
			List<org.schema.game.common.data.player.inventory.Inventory> holds = holds(raiders, sector.getId());
			if (holds.isEmpty()) {
				continue;
			}
			for (org.schema.game.common.data.player.inventory.FreeItem item : new ArrayList<>(sector.getItems().values())) {
				//whole stacks of blocks only: a stack that does not fit stays where it floats
				if (item.getType() <= 0 || item.getCount() <= 0) {
					continue;
				}
				org.schema.game.common.data.player.inventory.Inventory room = null;
				for (var inv : holds) {
					if (inv.canPutIn(item.getType(), item.getCount())) {
						room = inv;
						break;
					}
				}
				if (room != null) {
					room.sendInventoryModification(room.incExistingOrNextFreeSlot(item.getType(), item.getCount()));
					tally(posts.get(e.getValue()[0]), item.getType(), item.getCount());
					sector.getRemoteSector().removeItem(item.getId());
				}
			}
		}
	}

	private static void tally(Post post, short type, int n) {
		if (post != null && n > 0) {
			long value = n * org.schema.game.common.data.element.ElementKeyMap.getInfo(type).getPrice(false);
			post.seized += n;
			post.seizedValue += value;
			post.stolen.put(type, post.stolen.get(type) + n);
			post.stolenValue += value;
		}
	}

	/** Quiet this long after the raid's last hit and its victims hear of it, loot swept up since included. */
	private static final long RAID_NEWS_QUIET_MS = 60_000L;

	/** Tells the raid's victims, and the raider's own faction, in their faction news, that they were raided and what was taken. */
	private static void newsForVictims(GameServerState state, Fleet raiders, Post post, long now) {
		if (post.victims.isEmpty() || now - post.lastHitAt < RAID_NEWS_QUIET_MS) {
			return;
		}
		String raider = factionOf(raiders) == 0 ? raiders.getName() : state.getFactionManager().getFactionName(factionOf(raiders));
		StringBuilder body = new StringBuilder(Lng.str("%s raided the trade delivery %s at %s.", raider, post.victimConvoy, post.victimAt == null ? "?" : post.victimAt.toStringPure()));
		if (post.stolen.isEmpty()) {
			body.append('\n').append(Lng.str("Nothing was taken."));
		} else {
			body.append('\n').append(Lng.str("Stolen, worth about %s credits:", String.format("%,d", post.stolenValue)));
			List<it.unimi.dsi.fastutil.shorts.Short2IntMap.Entry> taken = new ArrayList<>(post.stolen.short2IntEntrySet());
			taken.sort((a, b) -> Integer.compare(b.getIntValue(), a.getIntValue()));
			for (int i = 0; i < taken.size(); i++) {
				if (i == 15) {
					body.append('\n').append(Lng.str("...and %s more kinds of block.", taken.size() - i));
					break;
				}
				var t = taken.get(i);
				body.append("\n  ").append(String.format("%,d", t.getIntValue())).append(" x ")
						.append(org.schema.game.common.data.element.ElementKeyMap.getInfo(t.getShortKey()).getName());
			}
		}
		for (int id : post.victims) {
			org.schema.game.common.data.player.faction.FactionNewsPost news = new org.schema.game.common.data.player.faction.FactionNewsPost();
			news.set(id, raider, now, Lng.str("Trade Raided"), body.toString(), 0);
			news.setCategory(org.schema.game.common.data.player.faction.FactionNewsCategory.WAR);
			state.getFactionManager().addNewsPostServer(news);
		}
		if (factionOf(raiders) != 0) {
			org.schema.game.common.data.player.faction.FactionNewsPost news = new org.schema.game.common.data.player.faction.FactionNewsPost();
			news.set(factionOf(raiders), raiders.getName(), now, Lng.str("Raid Report"), body.toString(), 0);
			news.setCategory(org.schema.game.common.data.player.faction.FactionNewsCategory.WAR);
			state.getFactionManager().addNewsPostServer(news);
		}
		post.victims.clear();
		post.stolen.clear();
		post.stolenValue = 0;
	}

	private static boolean overridden(FleetCommand c, Post post, long now) {
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
		fleet.getFleetManager().executeCommand(new FleetCommand(type, fleet, new Vector3i(to)));
	}

	public Tag toTag() {
		List<Tag> ts = new ArrayList<>();
		for (Map.Entry<Long, Post> e : posts.entrySet()) {
			Tag[] route = new Tag[e.getValue().route.size() + 1];
			for (int i = 0; i < e.getValue().route.size(); i++) {
				route[i] = new Tag(Tag.Type.VECTOR3i, null, new Vector3i(e.getValue().route.get(i)));
			}
			route[route.length - 1] = FinishTag.INST;
			ts.add(new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.LONG, null, e.getKey()),
					new Tag(Tag.Type.BYTE, null, (byte) e.getValue().kind.ordinal()), new Tag(Tag.Type.VECTOR3i, null, new Vector3i(e.getValue().home)),
					new Tag(Tag.Type.STRUCT, null, route), ints(e.getValue().targets), new Tag(Tag.Type.BYTE, null, (byte) (e.getValue().miners ? 1 : 0)), FinishTag.INST}));
		}
		ts.add(FinishTag.INST);
		return new Tag(Tag.Type.STRUCT, null, ts.toArray(new Tag[0]));
	}

	/** The convoys and last orders are not saved: the first update finds a convoy again. */
	public void fromTag(Tag tag) {
		posts.clear();
		for (Tag e : tag.getStruct()) {
			if (e.getType() == Tag.Type.STRUCT) {
				Tag[] t = e.getStruct();
				int k = t[1].getByte();
				Post post = new Post(k >= 0 && k < Kind.values().length ? Kind.values()[k] : Kind.ESCORT, t[2].getVector3i());
				if (t.length > 3 && t[3].getType() == Tag.Type.STRUCT) {
					for (Tag r : t[3].getStruct()) {
						if (r.getType() == Tag.Type.VECTOR3i) {
							post.route.add(r.getVector3i());
						}
					}
				}
				if (t.length > 4 && t[4].getType() == Tag.Type.STRUCT) {
					post.targets.addAll(ints(t[4]));
				}
				post.miners = t.length > 5 && t[5].getType() == Tag.Type.BYTE && t[5].getByte() != 0;
				//saved before routes: the one sector it prowled
				if (post.kind == Kind.RAID && post.route.isEmpty()) {
					post.route.add(new Vector3i(post.home));
				}
				posts.put(t[0].getLong(), post);
			}
		}
	}

	static Tag ints(java.util.Collection<Integer> v) {
		Tag[] t = new Tag[v.size() + 1];
		int i = 0;
		for (int x : v) {
			t[i++] = new Tag(Tag.Type.INT, null, x);
		}
		t[i] = FinishTag.INST;
		return new Tag(Tag.Type.STRUCT, null, t);
	}

	static java.util.Set<Integer> ints(Tag tag) {
		java.util.Set<Integer> out = new java.util.HashSet<>();
		for (Tag t : tag.getStruct()) {
			if (t.getType() == Tag.Type.INT) {
				out.add(t.getInt());
			}
		}
		return out;
	}

	// ---- client

	/** Client: send a fleet raiding the systems holding {@code sectors}, at {@code targets}' deliveries (none: anyone's not ours or a friend's). */
	public static void requestRaid(Fleet fleet, List<Vector3i> sectors, java.util.Collection<Integer> targets, boolean miners) {
		orderToServer(fleet, Kind.RAID, sectors, false, targets, miners);
	}

	private static void orderToServer(Fleet fleet, Kind kind, List<Vector3i> sectors, boolean append) {
		orderToServer(fleet, kind, sectors, append, List.of(), false);
	}

	private static void orderToServer(Fleet fleet, Kind kind, List<Vector3i> sectors, boolean append, java.util.Collection<Integer> targets, boolean miners) {
		Tag[] route = new Tag[sectors.size() + 1];
		for (int i = 0; i < sectors.size(); i++) {
			route[i] = new Tag(Tag.Type.VECTOR3i, null, new Vector3i(sectors.get(i)));
		}
		route[route.length - 1] = FinishTag.INST;
		BFPacket.toServer(BFPacket.Kind.TRADE_FLEET_ORDER, new Tag(Tag.Type.STRUCT, null, new Tag[]{
				new Tag(Tag.Type.LONG, null, fleet.dbid), new Tag(Tag.Type.BYTE, null, (byte) kind.ordinal()),
				new Tag(Tag.Type.STRUCT, null, route), new Tag(Tag.Type.BYTE, null, (byte) (append ? 1 : 0)), ints(targets), new Tag(Tag.Type.BYTE, null, (byte) (miners ? 1 : 0)), FinishTag.INST}));
	}

	/** Server: the order packet. */
	public static void received(GameServerState state, PlayerState p, Tag tag) {
		Tag[] t = tag.getStruct();
		int k = t[1].getByte();
		if (t.length < 4 || k < 0 || k >= Kind.values().length || t[2].getType() != Tag.Type.STRUCT) {
			return;
		}
		List<Vector3i> sectors = new ArrayList<>();
		for (Tag r : t[2].getStruct()) {
			if (r.getType() == Tag.Type.VECTOR3i && sectors.size() < MAX_ROUTE) {
				sectors.add(r.getVector3i());
			}
		}
		java.util.Set<Integer> targets = t.length > 4 && t[4].getType() == Tag.Type.STRUCT ? ints(t[4]) : new java.util.HashSet<>();
		boolean miners = t.length > 5 && t[5].getType() == Tag.Type.BYTE && t[5].getByte() != 0;
		get().order(state, p, t[0].getLong(), Kind.values()[k], sectors, t[3].getByte() != 0, targets, miners);
	}

	/** Client: the fleet Order dialog's "Escort Trade" and "Raid Trade Routes" tiles (FleetRepairOrderMixin). */
	public static void addTiles(PlayerButtonTilesInput a, Fleet fleet) {
		tile(a, fleet, Lng.str("Escort Trade"), Lng.str("Escort our faction's trade deliveries, start to finish, one at a time, waiting here between them. Any other order ends it."),
				() -> orderToServer(fleet, Kind.ESCORT, List.of(), false));
		tile(a, fleet, Lng.str("Raid Trade Routes"), Lng.str("Patrol the systems holding the sectors you give, in turn, attacking trade deliveries in them that are not ours or our friends'. Any other order ends it."),
				() -> new videogoose.betterfactions.gui.RaidOrderDialog(GameClientState.instance, fleet).activate());
	}

	private static void tile(PlayerButtonTilesInput a, Fleet fleet, String name, String description, Runnable onPress) {
		a.addTile(name, description, GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
			@Override
			public boolean isOccluded() {
				return !a.isActive();
			}

			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if (event.pressedLeftMouse()) {
					a.deactivate();
					onPress.run();
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

	/** Client: the map radial's "Raid Trade" item: the clicked sector's system joins each selected fleet's raid, or starts one (MapInterceptMixin). */
	public static void addRadialItem(RadialMenu menu, RadialMenuDialog dialog, Vector3i sector) {
		menu.addItem(Lng.str("Raid Trade"), new GUICallback() {
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
							orderToServer(fleet, Kind.RAID, List.of(sector), true);
						}
					}
					MapControllerManager.resetFleets();
					dialog.deactivate();
				}
			}
		}, new GUIActivationCallback() {
			@Override
			public boolean isVisible(InputState state) {
				return true;
			}

			@Override
			public boolean isActive(InputState state) {
				return true;
			}
		});
	}
}
