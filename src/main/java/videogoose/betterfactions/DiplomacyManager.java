package videogoose.betterfactions;

import org.schema.common.util.StringTools;
import api.network.packets.PacketUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.schema.common.LogUtil;
import org.schema.common.util.LogInterface;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.FactionNewsCategory;
import org.schema.game.common.data.player.faction.FactionNewsPost;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.server.data.Galaxy;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.network.objects.ChatMessage;
import org.schema.game.server.data.FactionState;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.simulation.npc.NPCFaction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.StateInterface;
import org.schema.schine.network.server.ServerMessage;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.assets.FactionAssets;
import videogoose.betterfactions.battle.BattleReports;
import videogoose.betterfactions.battle.BattleTracker;
import videogoose.betterfactions.gui.BattleAdminList;
import videogoose.betterfactions.gui.RepairDialogs;
import videogoose.betterfactions.repair.FactionDesigns;
import videogoose.betterfactions.fleet.FleetIntercepts;
import videogoose.betterfactions.repair.ShipyardRepairs;
import videogoose.betterfactions.diplomacy.CombatRatings;
import videogoose.betterfactions.diplomacy.NpcRatingBackfill;
import videogoose.betterfactions.diplomacy.DiplomacyAction;
import videogoose.betterfactions.diplomacy.DiplomacyEntity;
import videogoose.betterfactions.diplomacy.DiplomacyProposal;
import videogoose.betterfactions.diplomacy.FactionDiplomacy;
import videogoose.betterfactions.diplomacy.FactionStrength;
import videogoose.betterfactions.diplomacy.FactionTreaty;
import videogoose.betterfactions.diplomacy.FactionTreatyChange;
import videogoose.betterfactions.diplomacy.FactionTreatySet;
import videogoose.betterfactions.diplomacy.Federation;
import videogoose.betterfactions.diplomacy.SettlementPricing;
import videogoose.betterfactions.diplomacy.SettlementTerm;
import videogoose.betterfactions.diplomacy.FederationCommand;
import videogoose.betterfactions.diplomacy.TreatyNames;
import videogoose.betterfactions.diplomacy.TreatyType;
import videogoose.betterfactions.diplomacy.TreatyTypeRegistry;
import videogoose.betterfactions.emblem.FactionEmblem;
import videogoose.betterfactions.emblem.FactionEmblemCache;
import videogoose.betterfactions.net.BFPacket;
import videogoose.betterfactions.net.BFPacket.Kind;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

/**
 * Everything the mod knows about diplomacy on one side of the wire: treaties, open proposals,
 * federations, opinion ledgers and emblem hashes.
 *
 * <p>One per side rather than one per process, because singleplayer runs a server and a client in the
 * same JVM and each keeps its own copy, exactly as each keeps its own {@link FactionManager}. The
 * server's copy is the authority and is saved with the world; the client's is a mirror fed by
 * {@link BFPacket}s and is what the GUI observes.</p>
 */
public final class DiplomacyManager extends Observable {

	private static final FactionTreatySet EMPTY_TREATIES = new FactionTreatySet();
	/**
	 * The merge is an SQL round trip per pair, so shared vision refreshes on a timer, not per tick.
	 */
	private static final long INTEL_SHARING_SYNC_MS = 60000;

	private static DiplomacyManager server;
	private static DiplomacyManager client;

	private final boolean onServer;
	/**
	 * Treaties in force, keyed by the unordered pair code {@link FactionRelation#getCode} uses. A pair
	 * can hold several at once, and an asymmetric one keeps its direction inside the record rather
	 * than in the key - so this stays one map with one lookup.
	 */
	private final Long2ObjectOpenHashMap<FactionTreatySet> treaties = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectOpenHashMap<DiplomacyProposal> relationShipOffers = new Long2ObjectOpenHashMap<>();
	private final ObjectArrayList<DiplomacyProposal> relationOffersToAdd = new ObjectArrayList<>();
	private final ObjectArrayList<DiplomacyProposal> toAddProposalResponses = new ObjectArrayList<>();
	/**
	 * Every bloc on this server, by id. Small - a server has a handful of federations, not a handful
	 * per faction - which is why {@link #getFederationOf} is allowed to scan it.
	 */
	private final Int2ObjectOpenHashMap<Federation> federations = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectOpenHashMap<FactionDiplomacy> ledgers = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectOpenHashMap<String> emblems = new Int2ObjectOpenHashMap<>();
	private final ObjectOpenHashSet<FactionDiplomacy> diplomacyChanged = new ObjectOpenHashSet<>();
	/**
	 * Players who have joined but have not been sent the world's diplomacy yet. Sent once their
	 * client channel exists, because a mod packet to a player still logging in has nowhere to go.
	 */
	private final ObjectOpenHashSet<PlayerState> needsSnapshot = new ObjectOpenHashSet<>();
	private int nextFederationId = 1;
	private long lastIntelSharingSync;
	private DiplomacyProposal queryResult;
	private final CombatRatings combatRatings = new CombatRatings();
	private long lastMilitarySweep;
	/** A rated entity was deleted since the totals last went out. */
	private boolean militaryDirty;
	private boolean npcRatingsBackfilled;
	private FactionAssets factionAssets;
	private boolean factionAssetsPending;

	private DiplomacyManager(boolean onServer) {
		this.onServer = onServer;
	}

	public static synchronized DiplomacyManager server() {
		if (server == null) {
			server = new DiplomacyManager(true);
		}
		return server;
	}

	public static synchronized DiplomacyManager client() {
		if (client == null) {
			client = new DiplomacyManager(false);
		}
		return client;
	}

	public static DiplomacyManager of(StateInterface state) {
		return state instanceof GameServerState ? server() : client();
	}

	public static DiplomacyManager of(boolean isServer) {
		return isServer ? server() : client();
	}

	/**
	 * Drops the client mirror, for a disconnect: the next server has its own treaties.
	 */
	public static synchronized void resetClient() {
		client = null;
		FactionEmblemCache.reset();
	}

	/**
	 * Drops the server state, for a world being loaded: the next world has its own.
	 */
	public static synchronized void resetServer() {
		server = null;
	}

	public boolean isOnServer() {
		return onServer;
	}

	public StateInterface getState() {
		return state();
	}

	private StateInterface state() {
		return onServer ? GameServerState.instance : GameClientState.instance;
	}

	private FactionManager fm() {
		return ((FactionState) state()).getFactionManager();
	}

	private Faction getFaction(int id) {
		return fm().getFaction(id);
	}

	private boolean existsFaction(int id) {
		return fm().existsFaction(id);
	}

	private String getFactionName(int id) {
		return fm().getFactionName(id);
	}

	private FactionRelation.RType getRelation(int a, int b) {
		return fm().getRelation(a, b);
	}

	private boolean isEnemy(int a, int b) {
		return fm().isEnemy(a, b);
	}

	private void changed() {
		setChanged();
		notifyObservers();
	}

	// ------------------------------------------------------------------
	// Relation override (the engine's relation query hook lands here)
	// ------------------------------------------------------------------

	/**
	 * What the treaties between the pair say about hostility, or null if they say nothing and the
	 * engine's own relation table stands. This is on the combat targeting path, so it is one map
	 * lookup and a cached field read.
	 */
	public FactionRelation.RType derivedRelation(int a, int b) {
		FactionTreatySet set = treaties.get(FactionRelation.getCode(a, b));
		return set != null ? set.getDerivedRelation() : null;
	}

	// ------------------------------------------------------------------
	// Opinion ledgers and emblems, which used to be fields on Faction
	// ------------------------------------------------------------------

	/**
	 * Offers waiting on an answer from this faction - the inbox.
	 */
	public List<DiplomacyProposal> getOffersTo(int factionId) {
		List<DiplomacyProposal> out = new ObjectArrayList<>();
		synchronized (relationShipOffers) {
			for (DiplomacyProposal p : relationShipOffers.values()) {
				if (p.b == factionId) {
					out.add(p);
				}
			}
		}
		return out;
	}

	public List<DiplomacyProposal> getRelationOffersToAdd() {
		return relationOffersToAdd;
	}

	public CombatRatings getCombatRatings() {
		return combatRatings;
	}

	public FactionDiplomacy diplomacyOf(Faction f) {
		synchronized (ledgers) {
			FactionDiplomacy d = ledgers.get(f.getIdFaction());
			if (d == null) {
				d = new FactionDiplomacy(f, this);
				ledgers.put(f.getIdFaction(), d);
			}
			return d;
		}
	}

	public void markDiplomacyChanged(FactionDiplomacy d) {
		diplomacyChanged.add(d);
	}

	public String emblemOf(Faction f) {
		return f == null ? "" : emblemOf(f.getIdFaction());
	}

	public String emblemOf(int factionId) {
		String h = emblems.get(factionId);
		return h != null ? h : "";
	}

	private void setEmblem(Faction f, String hash, String by) {
		if (hash.isEmpty()) {
			emblems.remove(f.getIdFaction());
		} else {
			emblems.put(f.getIdFaction(), hash);
		}
		LogUtil.log().fine("[EMBLEM] " + f + " set emblem '" + hash + "' by " + by);
		broadcast(Kind.EMBLEM_HASH, emblemTag(f.getIdFaction(), hash));
		changed();
	}

	private static Tag emblemTag(int factionId, String hash) {
		return new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.INT, null, factionId), new Tag(Tag.Type.STRING, null, hash), FinishTag.INST});
	}

	// ------------------------------------------------------------------
	// Wire
	// ------------------------------------------------------------------

	private void broadcast(Kind kind, Tag tag) {
		if (!onServer) {
			return;
		}
		for (PlayerState p : GameServerState.instance.getPlayerStatesByName().values()) {
			if (!needsSnapshot.contains(p) && p.getClientChannel() != null) {
				BFPacket.toPlayer(p, kind, tag);
			}
		}
	}

	/**
	 * Server: one request from a client. The sender is whoever the network layer says sent the
	 * packet; nothing in the payload names an initiator that is believed.
	 */
	public void receive(PlayerState sender, Kind kind, Tag tag) {
		switch (kind) {
			case PROPOSAL -> {
				DiplomacyProposal p = new DiplomacyProposal();
				p.fromTagStructure(tag);
				p.setInitiator(sender.getName());
				p.serverAuthored = false;
				p.against = 0;
				if (p.action == DiplomacyProposal.Action.QUERY_RESULT) {
					return;
				}
				if (p.isResponse()) {
					toAddProposalResponses.add(p);
					handleFactionOffersAccept();
				} else {
					relationOffersToAdd.add(p);
					handleRelationShipOffers();
				}
			}
			case FEDERATION_COMMAND -> {
				FederationCommand c = new FederationCommand();
				if (c.fromTag(tag)) {
					handleFederationCommand(c, sender);
				}
			}
			case EMBLEM_UPLOAD -> uploadEmblem(sender, (byte[]) tag.getValue());
			case EMBLEM_CLEAR -> {
				Faction f = editableFaction(sender);
				if (f != null) {
					setEmblem(f, "", sender.getName());
				}
			}
			case EMBLEM_REQUEST -> sendEmblemData(sender, (String) tag.getValue());
			case ASSETS_REQUEST -> answerAssets(sender);
			case BATTLES_REQUEST -> {
				GameServerState st = (GameServerState) state();
				if (st.isAdmin(sender.getName())) {
					BFPacket.toPlayer(sender, Kind.BATTLES, BattleTracker.get().adminTag(st, System.currentTimeMillis()));
				}
			}
			case BATTLE_REPORTS_REQUEST -> {
				//anyone in a faction reads its battle news, so anyone in it reads the reports
				if (sender.getFactionId() != 0 && BattleReports.mayAnswer(sender.getName(), System.currentTimeMillis())) {
					BFPacket.toPlayer(sender, Kind.BATTLE_REPORTS, BattleTracker.get().archive().tagFor(sender.getFactionId()));
				}
			}
			case BATTLE_DETAIL_REQUEST -> {
				if (sender.getFactionId() != 0 && BattleReports.mayAnswer("detail:" + sender.getName(), System.currentTimeMillis())) {
					Tag detail = BattleTracker.get().archive().detailFor(sender.getFactionId(), tag);
					if (detail != null) {
						BFPacket.toPlayer(sender, Kind.BATTLE_DETAIL, detail);
					}
				}
			}
			case REPAIR_ORDER -> {
				Tag[] t = tag.getStruct();
				FactionAssets a = BetterFactions.getInstance().getAssetIndex().get(sender.getFactionId());
				//the station must be one the server itself lists as the sender's faction's
				FactionAssets.Asset station = a == null ? null : a.find(t[1].getString());
				if (station != null) {
					ShipyardRepairs.get().order((GameServerState) state(), sender, t[0].getLong(), station.uid, station.name, station.sector);
				}
			}
			case REPAIR_CANCEL -> ShipyardRepairs.get().cancel(sender, tag.getString());
			case INTERCEPT_ORDER -> {
				Tag[] t = tag.getStruct();
				FleetIntercepts.get().order((GameServerState) state(), sender, t[0].getLong(), t[1].getVector3i());
			}
			case REPAIR_QUEUE_REQUEST -> {
				if (sender.getFactionId() != 0 && BattleReports.mayAnswer("queue:" + sender.getName(), System.currentTimeMillis())) {
					BFPacket.toPlayer(sender, Kind.REPAIR_QUEUE, ShipyardRepairs.get().queueTag(tag.getString(), sender.getFactionId()));
				}
			}
			case DESIGNS_REQUEST -> {
				if (sender.getFactionId() != 0 && BattleReports.mayAnswer("designs:" + sender.getName(), System.currentTimeMillis())) {
					BFPacket.toPlayer(sender, Kind.DESIGNS, FactionDesigns.server().tagFor(sender.getFactionId()));
				}
			}
			case DESIGN_SET -> {
				//only ever writes into the sender's own faction's map, so whatever ship it names harms nobody else
				Tag[] t = tag.getStruct();
				//and only to a blueprint the sender may use: the picker lists no others, so anything else is a forged packet
				String bp = t[1].getString();
				boolean allowed = bp.isEmpty() || sender.getCatalog().getAvailableCatalog().stream().anyMatch(p -> bp.equals(p.getUid()));
				if (sender.getFactionId() != 0 && allowed) {
					FactionDesigns.server().assign(sender.getFactionId(), t[0].getString(), t[1].getString());
					BFPacket.toPlayer(sender, Kind.DESIGNS, FactionDesigns.server().tagFor(sender.getFactionId()));
				}
			}
			case BATTLE_COMMAND -> {
				GameServerState st = (GameServerState) state();
				if (st.isAdmin(sender.getName())) {
					Tag[] t = tag.getStruct();
					BattleTracker.get().adminCommand(st, sender, t[0].getByte(), t[1].getInt());
				}
			}
			default -> LogUtil.log().warning("[BetterFactions] client sent a server-to-client packet: " + kind);
		}
	}

	/**
	 * Client: the server telling this client what changed.
	 */
	public void receive(Kind kind, Tag tag) {
		switch (kind) {
			case SNAPSHOT -> fromTag(tag, false);
			case TREATY -> {
				FactionTreatyChange c = new FactionTreatyChange();
				if (c.fromTag(tag)) {
					applyTreatyChange(c);
				}
			}
			case OFFER -> {
				DiplomacyProposal p = new DiplomacyProposal();
				p.fromTagStructure(tag);
				if (p.action == DiplomacyProposal.Action.QUERY_RESULT) {
					queryResult = p;
					setChanged();
					notifyObservers("DIPLOMACY_QUERY");
				} else if (p.isResponse()) {
					toAddProposalResponses.add(p);
					handleFactionOffersAccept();
				} else {
					relationOffersToAdd.add(p);
					handleRelationShipOffers();
				}
			}
			case FEDERATION -> {
				Federation f = new Federation();
				f.fromTag(tag);
				applyFederation(f);
			}
			case OPINION -> {
				Tag[] t = tag.getStruct();
				Faction owner = getFaction(t[0].getInt());
				if (owner != null) {
					FactionDiplomacy d = diplomacyOf(owner);
					DiplomacyEntity e = new DiplomacyEntity(this, owner.getIdFaction());
					e.fromTag(t[1]);
					d.entities.put(e.getDbId(), e);
					d.onClientChanged();
				}
				changed();
			}
			case EMBLEM_HASH -> {
				Tag[] t = tag.getStruct();
				int id = t[0].getInt();
				String hash = t[1].getString();
				if (hash.isEmpty()) {
					emblems.remove(id);
				} else {
					emblems.put(id, hash);
				}
				changed();
			}
			case BATTLES -> BattleAdminList.received(tag);
			case BATTLE_REPORTS -> BattleReports.received(tag);
			case BATTLE_DETAIL -> BattleReports.detailReceived(tag);
			case BATTLE_WATCH -> BattleAdminList.setWatching(GameClientState.instance, tag.getByte());
			case REPAIR_QUEUE -> RepairDialogs.queueReceived(tag);
			case DESIGNS -> FactionDesigns.received(tag);
			case MILITARY -> {
				combatRatings.fromTotalsTag(tag);
				changed();
			}
			case EMBLEM_DATA -> {
				Tag[] t = tag.getStruct();
				FactionEmblemCache.received(GameClientState.instance, t[0].getString(), (byte[]) t[1].getValue());
			}
			case ASSETS -> {
				FactionAssets a = new FactionAssets();
				try {
					a.deserialize(new java.io.DataInputStream(new java.io.ByteArrayInputStream((byte[]) tag.getValue())));
				} catch (IOException e) {
					LogUtil.log().warning("[BetterFactions] bad holdings answer: " + e);
					return;
				}
				factionAssets = a;
				factionAssetsPending = false;
				setChanged();
				notifyObservers("FACTION_ASSETS");
			}
			default -> LogUtil.log().warning("[BetterFactions] server sent a client-to-server packet: " + kind);
		}
	}

	// ------------------------------------------------------------------
	// Ticks
	// ------------------------------------------------------------------

	/**
	 * Server tick: ledgers, treaty expiry, shared vision, lapsed votes, and the first sync to anyone
	 * who has just joined.
	 */
	public void updateServer(long now) {
		//the ledgers queue what their reactions decide rather than acting mid-iteration
		handleRelationShipOffers();
		handleFactionOffersAccept();
		lapseCallsToArms(System.currentTimeMillis());
		for (Faction f : fm().getFactionCollection()) {
			diplomacyOf(f).update(now);
		}
		long wall = System.currentTimeMillis();
		expireTreaties(wall);
		syncIntelSharing(wall);
		rateMilitary(wall);
		if (militaryDirty) {
			publishMilitary(false);
		}
		checkFederationVotes();

		if (!needsSnapshot.isEmpty()) {
			Tag snapshot = null;
			for (PlayerState p : new ObjectArrayList<>(needsSnapshot)) {
				if (p.getClientChannel() == null) {
					continue;
				}
				if (snapshot == null) {
					snapshot = toTag(true);
				}
				needsSnapshot.remove(p);
				BFPacket.toPlayer(p, Kind.SNAPSHOT, snapshot);
				sendAllFederationsTo(p);
				for (Faction f : fm().getFactionCollection()) {
					diplomacyOf(f).onPlayerJoined(p);
					diplomacyOf(f).checkDiplomacySending(true);
				}
			}
		}

		for (FactionDiplomacy d : diplomacyChanged) {
			d.checkDiplomacySending(false);
		}
		diplomacyChanged.clear();
	}

	public void onPlayerJoined(PlayerState p) {
		needsSnapshot.add(p);
	}

	public void onPlayerLeft(PlayerState p) {
		needsSnapshot.remove(p);
	}

	public void onFactionDeleted(Faction f) {
		FactionDesigns.server().onFactionDeleted(f.getIdFaction());
		removeTreatiesOf(f.getIdFaction());
		removeFederationsOf(f.getIdFaction());
		emblems.remove(f.getIdFaction());
		synchronized (ledgers) {
			ledgers.remove(f.getIdFaction());
			for (FactionDiplomacy d : ledgers.values()) {
				d.onDeletedFaction(f);
			}
		}
		synchronized (relationShipOffers) {
			relationShipOffers.values().removeIf(o -> o.a == f.getIdFaction() || o.b == f.getIdFaction());
		}
		combatRatings.removeFaction(f.getIdFaction());
		changed();
	}

	/**
	 * Sends one ledger entry - one faction's opinion of somebody - to the clients that somebody
	 * covers: the player, or every online member of the faction.
	 */
	public void sendOpinion(Faction owner, DiplomacyEntity e) {
		if (!onServer) {
			return;
		}
		Tag tag = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.INT, null, owner.getIdFaction()), e.toTag(), FinishTag.INST});
		GameServerState s = GameServerState.instance;
		if (e.isSinglePlayer()) {
			PlayerState p = s.getPlayerStatesByDbId().get(e.getDbId());
			if (p != null && p.getClientChannel() != null) {
				BFPacket.toPlayer(p, Kind.OPINION, tag);
			}
		} else {
			Faction f = getFaction((int) e.getDbId());
			if (f != null && f.isPlayerFaction()) {
				for (String mem : f.getMembersUID().keySet()) {
					PlayerState p = s.getPlayerFromNameIgnoreCaseWOException(mem);
					if (p != null && p.getClientChannel() != null) {
						BFPacket.toPlayer(p, Kind.OPINION, tag);
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------
	// Persistence and snapshot
	// ------------------------------------------------------------------

	/**
	 * @param forClient leaves out what a client has no business holding: the opinion ledgers (sent
	 *                  per recipient) and federations (redacted per recipient)
	 */
	public Tag toTag(boolean forClient) {
		Tag[] offers;
		synchronized (relationShipOffers) {
			offers = new Tag[relationShipOffers.size() + 1];
			int i = 0;
			for (DiplomacyProposal p : relationShipOffers.values()) {
				offers[i++] = p.toTagStructure();
			}
			offers[offers.length - 1] = FinishTag.INST;
		}
		Tag[] emb = new Tag[emblems.size() + 1];
		int i = 0;
		for (Int2ObjectMap.Entry<String> e : emblems.int2ObjectEntrySet()) {
			emb[i++] = emblemTag(e.getIntKey(), e.getValue());
		}
		emb[emb.length - 1] = FinishTag.INST;

		Tag ledgerTag;
		if (forClient) {
			ledgerTag = new Tag(Tag.Type.STRUCT, null, new Tag[]{FinishTag.INST});
		} else {
			synchronized (ledgers) {
				Tag[] l = new Tag[ledgers.size() + 1];
				int j = 0;
				for (Int2ObjectMap.Entry<FactionDiplomacy> e : ledgers.int2ObjectEntrySet()) {
					l[j++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.INT, null, e.getIntKey()), e.getValue().toTag(), FinishTag.INST});
				}
				l[l.length - 1] = FinishTag.INST;
				ledgerTag = new Tag(Tag.Type.STRUCT, null, l);
			}
		}
		return new Tag(Tag.Type.STRUCT, null, new Tag[]{
				new Tag(Tag.Type.BYTE, null, (byte) 0),
				toTreatyTag(),
				new Tag(Tag.Type.STRUCT, null, offers),
				forClient ? new Tag(Tag.Type.STRUCT, null, new Tag[]{FinishTag.INST}) : toFederationTag(),
				new Tag(Tag.Type.STRUCT, null, emb),
				ledgerTag,
				forClient ? combatRatings.totalsTag() : combatRatings.toTag(),
				FinishTag.INST});
	}

	public void fromTag(Tag tag, boolean withLedgers) {
		Tag[] t = tag.getStruct();
		synchronized (treaties) {
			treaties.clear();
		}
		fromTreatyTag(t[1]);
		synchronized (relationShipOffers) {
			relationShipOffers.clear();
		}
		fromRelationshipOfferTag(t[2]);
		fromFederationTag(t[3]);
		emblems.clear();
		Tag[] emb = t[4].getStruct();
		for (int i = 0; i < emb.length - 1; i++) {
			Tag[] e = emb[i].getStruct();
			emblems.put(e[0].getInt(), e[1].getString());
		}
		//saves from before ships were rated end after the ledgers
		if (t.length > 7 && t[6].getType() != Tag.Type.FINISH) {
			if (withLedgers) {
				combatRatings.fromTag(t[6]);
			} else {
				combatRatings.fromTotalsTag(t[6]);
			}
		}
		if (withLedgers) {
			Tag[] l = t[5].getStruct();
			for (int i = 0; i < l.length - 1; i++) {
				Tag[] e = l[i].getStruct();
				Faction f = getFaction(e[0].getInt());
				if (f != null) {
					diplomacyOf(f).fromTag(e[1]);
				}
			}
		}
		changed();
	}

	// ------------------------------------------------------------------
	// Emblems and holdings
	// ------------------------------------------------------------------

	/**
	 * Three gates, all here and none in the GUI: the server setting, the uploader's rank, and
	 * {@link FactionEmblem#store} on the bytes themselves.
	 */
	private void uploadEmblem(PlayerState sender, byte[] bytes) {
		if (!BFConfig.customEmblemsEnabled()) {
			reject(sender, Lng.astr("Custom faction emblems are\nturned off on this server."));
			return;
		}
		Faction f = editableFaction(sender);
		if (f == null) {
			return;
		}
		try {
			setEmblem(f, FactionEmblem.store(bytes), sender.getName());
		} catch (IOException e) {
			reject(sender, Lng.astr("Emblem refused:\n%s", e.getMessage()));
		}
	}

	private Faction editableFaction(PlayerState p) {
		Faction f = getFaction(p.getFactionId());
		if (f == null) {
			reject(p, Lng.astr("You are not in a faction."));
			return null;
		}
		FactionPermission perm = f.getMembersUID().get(p.getName());
		if (perm == null || !perm.hasRelationshipPermission(f)) {
			reject(p, Lng.astr("ERROR\nYou can't change this setting!\n-> permission denied!"));
			return null;
		}
		return f;
	}

	private void sendEmblemData(PlayerState p, String hash) {
		if (!FactionEmblem.isPlainHash(hash) || !emblems.containsValue(hash)) {
			return;
		}
		try {
			byte[] bytes = java.nio.file.Files.readAllBytes(FactionEmblem.serverFile(hash).toPath());
			BFPacket.toPlayer(p, Kind.EMBLEM_DATA, new Tag(Tag.Type.STRUCT, null, new Tag[]{
					new Tag(Tag.Type.STRING, null, hash), new Tag(Tag.Type.BYTE_ARRAY, null, bytes), FinishTag.INST}));
		} catch (IOException e) {
			LogUtil.log().warning("[EMBLEM] could not serve " + hash + ": " + e);
		}
	}

	private void reject(PlayerState p, Object[] message) {
		p.sendServerMessage(new ServerMessage(message, ServerMessage.MESSAGE_TYPE_ERROR, p.getId()));
	}

	public void requestEmblemUpload(byte[] png) {
		BFPacket.toServer(Kind.EMBLEM_UPLOAD, new Tag(Tag.Type.BYTE_ARRAY, null, png));
	}

	public void requestEmblemClear() {
		BFPacket.toServer(Kind.EMBLEM_CLEAR, new Tag(Tag.Type.BYTE, null, (byte) 0));
	}

	public void requestEmblem(String hash) {
		BFPacket.toServer(Kind.EMBLEM_REQUEST, new Tag(Tag.Type.STRING, null, hash));
	}

	private long lastAssetAnswer;

	/**
	 * Answers a client's request for its own faction's holdings. The faction is the sender's, read
	 * here rather than taken off the wire, so there is nothing in the request to forge.
	 */
	private void answerAssets(PlayerState p) {
		Faction f = getFaction(p.getFactionId());
		if (f == null || !Perms.has(f, f.getMembersUID().get(p.getName()), Perms.ASSETS_VIEW)) {
			return;
		}
		//a request is one byte and an answer can be hundreds of rows, so an unthrottled reply is an
		//amplifier pointed at the server's own uplink
		long now = System.currentTimeMillis();
		if (now - lastAssetAnswer < 250) {
			return;
		}
		lastAssetAnswer = now;
		FactionAssets a = BetterFactions.getInstance().getAssetIndex().get(f.getIdFaction());
		if (a != null) {
			//routes move on in seconds, so they are read on every answer rather than cached with the holdings
			videogoose.betterfactions.assets.FleetRoutes.addTo(a);
			java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
			try {
				a.serialize(new java.io.DataOutputStream(bytes));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			BFPacket.toPlayer(p, Kind.ASSETS, new Tag(Tag.Type.BYTE_ARRAY, null, bytes.toByteArray()));
		}
	}

	public FactionAssets getFactionAssets() {
		return factionAssets;
	}

	public boolean isFactionAssetsPending() {
		return factionAssetsPending;
	}

	private long factionAssetsRequestedAt;

	/**
	 * Client: asks what this client's faction holds. Throttled here because a list that refreshes on
	 * every draw is the easy way to put a database query on the server tick sixty times a second.
	 *
	 * @param force ask even if the last answer is still young - the refresh button
	 * @return whether a request went out
	 */
	public boolean requestFactionAssets(boolean force) {
		long now = System.currentTimeMillis();
		long age = now - factionAssetsRequestedAt;
		if (force ? age < 2000 : (factionAssets != null && age < 30000) || (factionAssetsPending && age < 10000)) {
			return false;
		}
		factionAssetsRequestedAt = now;
		factionAssetsPending = true;
		BFPacket.toServer(Kind.ASSETS_REQUEST, new Tag(Tag.Type.BYTE, null, (byte) 0));
		return true;
	}

	/**
	 * Client: sends a picked image. The size is checked here only to save a pointless upload; the
	 * server checks it, and everything else, again.
	 */
	public void uploadEmblemFile(java.io.File file) throws IOException {
		if (file.length() > FactionEmblem.MAX_UPLOAD_BYTES) {
			throw new IOException(Lng.str("That image is too large (limit %s KB).", FactionEmblem.MAX_UPLOAD_BYTES / 1024));
		}
		requestEmblemUpload(java.nio.file.Files.readAllBytes(file.toPath()));
	}

	public void sendFederationCommand(FederationCommand c) {
		BFPacket.toServer(Kind.FEDERATION_COMMAND, c.toTag());
	}

	// ------------------------------------------------------------------
	// Ported from the engine branch's FactionManager
	// ------------------------------------------------------------------

	/**
	 * The acceptance rule's unmet conditions, as one line a player can read.
	 */
	private static String joinReasons(List<String> unmet) {
		StringBuilder b = new StringBuilder();
		for (String s : unmet) {
			if (b.length() > 0) {
				b.append("; ");
			}
			b.append(s);
		}
		return b.toString();
	}

    /**
     * Keeps intel_sharing treaties doing what they say: while one holds, each side's discovered map
     * keeps flowing to the other.
     *
     * <p>Fog of war could only be handed over as a one-off before this - a member with the right
     * permission pushed their faction's map across once, and nothing kept it current, so there was no
     * way to express a standing arrangement. Throttled because the merge goes through the visibility
     * table, which is SQL.</p>
     */
    private void syncIntelSharing(long now) {
        if (now - lastIntelSharingSync < INTEL_SHARING_SYNC_MS) {
            return;
        }
        lastIntelSharingSync = now;
        List<FactionTreaty> shared = null;
        synchronized (treaties) {
            for (FactionTreatySet set : treaties.values()) {
                for (FactionTreaty t : set.getTreaties()) {
                    if (TreatyTypeRegistry.INTEL_SHARING.equals(t.typeId)) {
                        if (shared == null) {
                            shared = new ObjectArrayList<FactionTreaty>();
                        }
                        shared.add(t);
                    }
                }
            }
        }
        if (shared == null) {
            return;
        }
        for (FactionTreaty t : shared) {
            Faction a = getFaction(t.from);
            Faction b = getFaction(t.to);
            if (a != null && b != null) {
                //both ways: the treaty is symmetric, so neither side is only a donor
                a.shareFogOfWar(b);
                b.shareFogOfWar(a);
            }
        }
    }

    /**
     * A deleted faction leaves no treaties behind, the same way it leaves no relations.
     */
    private void removeTreatiesOf(int factionId) {
        synchronized (treaties) {
            LongIterator it = treaties.keySet().iterator();
            while (it.hasNext()) {
                FactionTreatySet set = treaties.get(it.nextLong());
                boolean touched = false;
				for (FactionTreaty t : new ObjectArrayList<>(set.getTreaties())) {
                    if (t.involves(factionId)) {
                        set.remove(t);
                        touched = true;
                    }
                }
                if (touched && set.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    private static final long MILITARY_SWEEP_MS = 60000;
    private static final int[] RATED_TYPES = {
            SimpleTransformableSendableObject.EntityType.SHIP.dbTypeId,
            SimpleTransformableSendableObject.EntityType.SPACE_STATION.dbTypeId};

    /**
     * Server: rates every loaded faction-owned ship and station, then drops rows for what each faction
     * no longer owns. Loaded entities are rated as they stand, so a refit shows up within a sweep.
     */
    private void rateMilitary(long now) {
        if (now - lastMilitarySweep < MILITARY_SWEEP_MS) {
            return;
        }
        lastMilitarySweep = now;
        GameServerState s = (GameServerState) state();
        LongOpenHashSet loaded = new LongOpenHashSet();
        for (SegmentController sc : s.getSegmentControllersByName().values()) {
            if (sc.getFactionId() != 0 && sc.getDbId() > 0 && (sc instanceof Ship || sc instanceof SpaceStation) && sc.isFullyLoaded()) {
                combatRatings.put(sc.getDbId(), sc.getFactionId(), CombatRatings.rate(sc));
                loaded.add(sc.getDbId());
            }
        }
        //once per world, after the loaded pass so anything loaded keeps its real rating; by the first
        //sweep, a minute in, the NPC factions have their systems
        if (!npcRatingsBackfilled) {
            npcRatingsBackfilled = true;
            int n = NpcRatingBackfill.run(s.getFactionManager(), combatRatings);
            if (n > 0) {
                System.err.println("[BetterFactions] rated " + n + " never-loaded NPC entities from their blueprints");
            }
        }
        //ponytail: one database query per rated faction per sweep; round-robin them if that ever
        //shows up in a tick profile
        java.sql.Connection db = ((videogoose.betterfactions.mixin.TableAccessor) s.getDatabaseIndex().getTableManager().getEntityTable()).betterFactions$connection();
        for (int f : combatRatings.ratedFactions()) {
            //not EntityTable.getByFaction: it skips docked entities, which would drop every turret's
            //rating once it unloads, and a hull whose guns are all on turrets rates 0 by itself
            try (java.sql.Statement st = db.createStatement();
                 java.sql.ResultSet r = st.executeQuery("SELECT ID FROM ENTITIES WHERE FACTION = " + f + " AND TYPE IN ("
                         + RATED_TYPES[0] + "," + RATED_TYPES[1] + ");")) {
                //loaded counts as owned: a ship built since the last save has no row yet
                LongOpenHashSet owned = new LongOpenHashSet(loaded);
                while (r.next()) {
                    owned.add(r.getLong(1));
                }
                combatRatings.retain(f, owned);
            } catch (java.sql.SQLException e) {
                System.err.println("[SERVER][FACTION] combat rating query failed for faction " + f + ": " + e);
            }
        }
        boolean changed = false;
        for (Faction f : s.getFactionManager().getFactionCollection()) {
            if (f instanceof NPCFaction npc && npc.structure != null) {
                changed |= combatRatings.setNpcSystems(f.getIdFaction(), npc.structure.getTotalSystems());
            }
        }
        publishMilitary(changed);
    }

    /**
     * Server: a ship or station removed for good - destroyed, despawned or deleted - stops counting
     * now rather than at the next sweep. The totals go out on the next tick, so a battle that kills
     * twenty ships in one tick sends them once.
     */
    public void onEntityDeleted(SegmentController sc) {
        if (sc.getDbId() > 0 && combatRatings.remove(sc.getDbId())) {
            militaryDirty = true;
        }
    }

    private void publishMilitary(boolean changed) {
        militaryDirty = false;
        if (combatRatings.recomputeTotals() | changed) {
            broadcast(Kind.MILITARY, combatRatings.totalsTag());
            changed();
        }
    }

    private void expireTreaties(long now) {
        List<FactionTreaty> expired = null;
        synchronized (treaties) {
            LongIterator it = treaties.keySet().iterator();
            while (it.hasNext()) {
                FactionTreatySet set = treaties.get(it.nextLong());
                for (FactionTreaty t : set.getTreaties()) {
                    if (t.isExpired(now)) {
                        if (expired == null) {
							expired = new ObjectArrayList<>();
                        }
                        expired.add(t);
                    }
                }
                set.removeExpired(now);
                if (set.isEmpty()) {
                    it.remove();
                }
            }
        }
        if (expired != null) {
            changed();
            for (FactionTreaty t : expired) {
                sendTreatyChange(t, true);
                diplomacyAction(DiplomacyAction.DiplActionType.TREATY_EXPIRED, t.from, t.to, t.typeId);
                diplomacyAction(DiplomacyAction.DiplActionType.TREATY_EXPIRED, t.to, t.from, t.typeId);
            }
        }
    }

    private void sendTreatyChange(FactionTreaty treaty, boolean removed) {
        if (onServer) {
			broadcast(Kind.TREATY, FactionTreatyChange.of(treaty, removed).toTag());
        }
    }

    /**
     * Applies a treaty change without re-broadcasting it - the receiving side of the wire.
     */
    private void applyTreatyChange(FactionTreatyChange change) {
        FactionTreaty treaty = change.treaty;
        long code = FactionRelation.getCode(treaty.from, treaty.to);
        synchronized (treaties) {
            if (change.removed) {
                FactionTreatySet set = treaties.get(code);
                if (set != null) {
                    set.removeByType(treaty.typeId);
                    if (set.isEmpty()) {
                        treaties.remove(code);
                    }
                }
            } else {
                FactionTreatySet set = treaties.get(code);
                if (set == null) {
                    set = new FactionTreatySet();
                    treaties.put(code, set);
                }
                set.removeByType(treaty.typeId);
                set.add(treaty);
            }
        }
        changed();
    }

    private void fromTreatyTag(Tag t) {
        Tag[] pairs = (Tag[]) t.getValue();
        for (int i = 0; i < pairs.length - 1; i++) {
            Tag[] pair = pairs[i].getStruct();
            long code = pair[0].getLong();
            FactionTreatySet set = new FactionTreatySet();
            set.fromTag(pair[1]);
            if (!set.isEmpty()) {
                treaties.put(code, set);
            }
        }
    }

    private Tag toTreatyTag() {
        synchronized (treaties) {
            Tag[] pairs = new Tag[treaties.size() + 1];
            int i = 0;
            for (Long2ObjectMap.Entry<FactionTreatySet> e : treaties.long2ObjectEntrySet()) {
				pairs[i] = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.LONG, null, e.getLongKey()), e.getValue().toTag(), FinishTag.INST});
                i++;
            }
            pairs[pairs.length - 1] = FinishTag.INST;
            return new Tag(Tag.Type.STRUCT, null, pairs);
		}
	}

	private void fromRelationshipOfferTag(Tag t) {
		Tag[] relOfferTags = (Tag[]) t.getValue();
        for (int i = 0; i < relOfferTags.length - 1; i++) {
			DiplomacyProposal relationOffer = new DiplomacyProposal();
			relationOffer.fromTagStructure(relOfferTags[i]);
			if ((relationOffer.a > 0 && !existsFaction(relationOffer.a)) || (relationOffer.b > 0 && !existsFaction(relationOffer.b))) {
				System.err.println("[SERVER][FACTION][WARNING] not adding faction relation offer for non existent faction: " + relationOffer);
			} else {
				System.err.println("[SERVER][FACION][TAG] loaded relation offer " + relationOffer);
				relationShipOffers.put(relationOffer.getCode(), relationOffer);
			}
		}
	}

    public void diplomacyAction(DiplomacyAction.DiplActionType type, int toFaction, long otherDbId) {
        diplomacyAction(type, toFaction, otherDbId, null);
    }

    /**
     * Records something that happened in a faction's opinion ledger.
     *
     * @param subject what the action was about when its type alone is ambiguous - a treaty id
     */
    public void diplomacyAction(DiplomacyAction.DiplActionType type, int toFaction, long otherDbId, String subject) {
        Faction faction = getFaction(toFaction);
        //used to be NPC-only, which left the ledger every faction now keeps empty for player ones
		if (faction != null) {
			diplomacyOf(faction).diplomacyAction(type, otherDbId, subject);
		}
	}

    /**
     * @return the treaties in force between the pair, empty (never null) if there are none.
     */
    public FactionTreatySet getTreaties(int a, int b) {
        FactionTreatySet set = treaties.get(FactionRelation.getCode(a, b));
        return set != null ? set : EMPTY_TREATIES;
    }

    public boolean hasTreaty(int a, int b, String typeId) {
        return getTreaties(a, b).has(typeId);
    }

    /**
     * What a trade agreement is worth at the counter: the unit price a member of {@code buyerFaction}
     * pays at (or is paid by) a shop of {@code shopFaction}. Unchanged without an agreement.
     */
    public int shopPrice(int shopFaction, int buyerFaction, int price, boolean playerBuys) {
        if (shopFaction == 0 || buyerFaction == 0 || shopFaction == buyerFaction
                || !hasTreaty(shopFaction, buyerFaction, TreatyTypeRegistry.TRADE_AGREEMENT)) {
            return price;
        }
        return tradePrice(price, BFConfig.tradeDiscountPercent(), playerBuys);
    }

    public static int tradePrice(int price, int percent, boolean playerBuys) {
        if (price <= 0 || percent <= 0) {
            return price;
        }
        long p = playerBuys ? (long) price * (100 - percent) / 100 : (long) price * (100 + percent) / 100;
        //a 1-credit item still costs something, and a sale never pays out more than an int holds
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, p));
    }

    public Long2ObjectOpenHashMap<FactionTreatySet> getTreatyMap() {
        return treaties;
    }

    public Federation getFederation(int id) {
        synchronized (federations) {
            return federations.get(id);
        }
    }

    /**
     * The bloc this faction belongs to, or null. A faction is in at most one, which is what makes
     * this answerable at all - and what keeps the synthetic membership treaties from needing to know
     * which bloc they came from.
     *
     * <p>A scan rather than an index: blocs are counted in tens and this is called from menus, not
     * from a tick.</p>
     */
    public Federation getFederationOf(int factionId) {
        if (factionId == 0) {
            return null;
        }
        synchronized (federations) {
            for (Federation f : federations.values()) {
                if (f.isMember(factionId)) {
                    return f;
                }
            }
        }
        return null;
    }

    public List<Federation> getFederations() {
        synchronized (federations) {
            return new ObjectArrayList<Federation>(federations.values());
        }
    }

    /**
     * Blocs that have asked this faction to join and are still waiting for an answer.
     */
    public List<Federation> getFederationInvitesFor(int factionId) {
        List<Federation> out = new ObjectArrayList<Federation>();
        if (factionId == 0) {
            return out;
        }
        synchronized (federations) {
            for (Federation f : federations.values()) {
                if (f.isInvited(factionId)) {
                    out.add(f);
                }
            }
        }
        return out;
    }

    /**
     * Applies one request from a client.
     *
     * <p>Every gate is here and none of it is in the GUI. The asker is the channel the command
     * arrived on, never a name in the payload, so the only questions left are whether their rank may
     * speak for their faction ({@link FactionPermission#hasFederationVotePermission}) and whether
     * their faction is entitled to the thing being asked for.</p>
     */
    public void handleFederationCommand(FederationCommand c, PlayerState sender) {
        if (!onServer || c == null || c.kind == null || sender == null) {
            return;
        }
        Faction own = getFaction(sender.getFactionId());
        if (own == null) {
            refuseFederation(sender, Lng.astr("You are not in a faction."));
            return;
        }
        FactionPermission perm = own.getMembersUID().get(sender.getName());
        if (perm == null || !Perms.has(own, perm, Perms.FEDERATION_VOTE)) {
            refuseFederation(sender, Lng.astr("ERROR\nYour rank cannot speak for your\nfaction in a federation."));
            return;
        }
        int me = own.getIdFaction();
        Federation mine = getFederationOf(me);

        switch (c.kind) {
            case CREATE: {
                if (mine != null) {
                    refuseFederation(sender, Lng.astr("Your faction is already in a federation."));
                    return;
                }
                String name = c.text == null ? "" : c.text.trim();
                if (name.isEmpty()) {
                    refuseFederation(sender, Lng.astr("A federation needs a name."));
                    return;
                }
                Federation f;
                synchronized (federations) {
                    f = new Federation(nextFederationId++, name, me, System.currentTimeMillis());
                    federations.put(f.id, f);
                }
                federationNews(f, Lng.str("%s founded the federation %s.", own.getName(), f.name));
                sendFederation(f);
                break;
            }
            case ACCEPT_INVITE: {
                Federation f = getFederation(c.federationId);
                if (f == null || !f.isInvited(me)) {
                    refuseFederation(sender, Lng.astr("That invitation is no longer open."));
                    return;
                }
                if (mine != null) {
                    refuseFederation(sender, Lng.astr("Your faction is already in a federation."));
                    return;
                }
                f.invites.rem(me);
                joinFederation(f, me);
                break;
            }
            case DECLINE_INVITE: {
                Federation f = getFederation(c.federationId);
                if (f != null && f.invites.rem(me)) {
                    federationNews(f, Lng.str("%s declined the invitation.", own.getName()));
                    sendFederation(f);
                }
                break;
            }
            case LEAVE: {
                if (mine == null) {
                    return;
                }
                federationNews(mine, Lng.str("%s left the federation.", own.getName()));
                leaveFederation(mine, me);
                break;
            }
            case CAST: {
                Federation.FederationVote v = mine == null ? null : mine.vote(c.target);
                if (v == null || !mine.mayVote(v, me)) {
                    refuseFederation(sender, Lng.astr("That vote is already decided."));
                    return;
                }
                v.ballots.put(me, c.value != 0 ? (byte) 1 : (byte) 0);
                resolveVotes(mine, System.currentTimeMillis());
                sendFederation(mine);
                break;
            }
            default:
                handleLeaderCommand(c, sender, own, mine, me);
                break;
        }
    }

    /**
     * The half of the vocabulary that only the leader may use - or, for admission and expulsion, the
     * members when the charter says so.
     */
    private void handleLeaderCommand(FederationCommand c, PlayerState sender, Faction own, Federation mine, int me) {
        if (mine == null) {
            refuseFederation(sender, Lng.astr("Your faction is not in a federation."));
            return;
        }
        boolean leads = mine.leader == me;
        switch (c.kind) {
            case INVITE: {
                if (c.target == 0 || getFaction(c.target) == null) {
                    return;
                }
                if (mine.isMember(c.target) || mine.isInvited(c.target)) {
                    return;
                }
                if (getFederationOf(c.target) != null) {
                    refuseFederation(sender, Lng.astr("They are already in a federation."));
                    return;
                }
                if (mine.hasCharterFlag(Federation.CHARTER_VOTE_ADMISSION)) {
                    openVote(mine, sender, Federation.FederationVote.Kind.ADMIT, c.target, me, 0, "");
                    return;
                }
                if (!leads) {
                    refuseFederation(sender, Lng.astr("Only the federation's leader can invite."));
                    return;
                }
                inviteToFederation(mine, c.target);
                break;
            }
            case EXPEL: {
                if (!mine.isMember(c.target) || c.target == mine.leader) {
                    refuseFederation(sender, Lng.astr("That faction cannot be expelled."));
                    return;
                }
                if (mine.hasCharterFlag(Federation.CHARTER_VOTE_EXPULSION)) {
                    openVote(mine, sender, Federation.FederationVote.Kind.EXPEL, c.target, me, 0, "");
                    return;
                }
                if (!leads) {
                    refuseFederation(sender, Lng.astr("Only the federation's leader can expel."));
                    return;
                }
                expelFromFederation(mine, c.target);
                break;
            }
            case CANCEL_INVITE: {
                if (leads && mine.invites.rem(c.target)) {
                    sendFederation(mine);
                }
                break;
            }
            case RENAME: {
                String name = c.text == null ? "" : c.text.trim();
                if (leads && !name.isEmpty()) {
                    mine.name = name;
                    sendFederation(mine);
                }
                break;
            }
            case SET_CHARTER: {
                String text = c.text == null ? "" : c.text;
                long flags = Federation.sanitize(c.value);
                if (mine.hasCharterFlag(Federation.CHARTER_VOTE_CHARTER)) {
                    openVote(mine, sender, Federation.FederationVote.Kind.CHARTER, 0, me, flags, text);
                    return;
                }
                if (!leads) {
                    refuseFederation(sender, Lng.astr("Only the federation's chair can\nrewrite the charter."));
                    return;
                }
                setCharter(mine, text, flags);
                federationNews(mine, Lng.str("%s rewrote the federation's charter.", own.getName()));
                sendFederation(mine);
                break;
            }
            case DECLARE_WAR: {
                if (getFaction(c.target) == null || mine.isMember(c.target)) {
                    refuseFederation(sender, Lng.astr("The federation cannot declare\nwar on that faction."));
                    return;
                }
                if (mine.hasCharterFlag(Federation.CHARTER_VOTE_WAR)) {
                    openVote(mine, sender, Federation.FederationVote.Kind.WAR, c.target, me, 0, "");
                    return;
                }
                if (!leads) {
                    refuseFederation(sender, Lng.astr("Only the federation's chair can\ndeclare war for it."));
                    return;
                }
                declareFederationWar(mine, c.target, null);
                sendFederation(mine);
                break;
            }
            case TRANSFER_LEAD: {
                if (!leads || !mine.isMember(c.target)) {
                    return;
                }
                setChair(mine, c.target);
                sendFederation(mine);
                break;
            }
            case DISSOLVE: {
                if (!leads) {
                    refuseFederation(sender, Lng.astr("Only the federation's leader can\ndissolve the federation."));
                    return;
                }
                federationNews(mine, Lng.str("The federation was dissolved."));
                dissolveFederation(mine);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Puts a decision to the members, with the asker's own ballot already on it - somebody who
     * proposes an admission has plainly voted for it, and making them click again is ceremony.
     */
    private void openVote(Federation f, PlayerState sender, Federation.FederationVote.Kind kind, int subject, int by,
                          long value, String text) {
        if (f.voteOn(kind, subject) != null) {
            refuseFederation(sender, Lng.astr("The federation is already\nvoting on that."));
            return;
        }
        long now = System.currentTimeMillis();
        Federation.FederationVote v = new Federation.FederationVote(f.nextVoteId++, kind, subject, now);
        v.value = value;
        v.text = text;
        v.ballots.put(by, (byte) 1);
        f.votes.add(v);
        federationNews(f, Lng.str("A vote is open: %s.", describeVote(v)));
        resolveVotes(f, now);
        sendFederation(f);
    }

    /**
     * What a vote is about, in a line. Shared by the news and the vote list.
     */
    public String describeVote(Federation.FederationVote v) {
        Faction s = getFaction(v.subject);
        String who = s != null ? s.getName() : String.valueOf(v.subject);
        return switch (v.kind) {
            case ADMIT -> Lng.str("Admit %s", who);
            case EXPEL -> Lng.str("Expel %s", who);
            case WAR -> Lng.str("Declare war on %s", who);
            case CHARTER -> Lng.str("Amend the charter");
        };
    }

    /**
     * What one member's ballot counts for under this bloc's charter: one each, or their power. Power
     * is floored at one so a faction that holds nothing still has a voice.
     */
    public java.util.function.IntToDoubleFunction voteWeight(Federation f) {
        if (!f.hasCharterFlag(Federation.CHARTER_WEIGHT_BY_POWER)) {
            return id -> 1.0;
        }
        return id -> {
            Faction m = getFaction(id);
            return m == null ? 1.0 : Math.max(1.0, FactionStrength.of(m));
        };
    }

    /**
     * Counts every open vote and carries out the ones that are settled. Callers broadcast after;
     * carrying a vote out may already have, which costs one redundant send.
     *
     * <p>The window in {@link Federation#VOTE_WINDOW_MS} only ever bounds the case where somebody
     * never turns up - see {@link Federation#tally}.</p>
     */
    private void resolveVotes(Federation f, long now) {
        java.util.function.IntToDoubleFunction weight = voteWeight(f);
        for (Federation.FederationVote v : new ObjectArrayList<Federation.FederationVote>(f.votes)) {
            //carrying one out can expel a member or dissolve the bloc, which settles the others
            if (f.isDissolved() || !f.votes.contains(v)) {
                continue;
            }
            Federation.Outcome o = f.tally(v, weight, now);
            if (o == Federation.Outcome.OPEN) {
                continue;
            }
            f.votes.remove(v);
            if (o == Federation.Outcome.LOST) {
                federationNews(f, Lng.str("The vote did not carry: %s.", describeVote(v)));
                continue;
            }
            federationNews(f, Lng.str("The vote carried: %s.", describeVote(v)));
            switch (v.kind) {
                case ADMIT -> {
                    if (getFederationOf(v.subject) == null && getFaction(v.subject) != null) {
                        inviteToFederation(f, v.subject);
                    }
                }
                case EXPEL -> {
                    if (f.isMember(v.subject) && v.subject != f.leader) {
                        expelFromFederation(f, v.subject);
                    }
                }
                case WAR -> declareFederationWar(f, v.subject, v);
                case CHARTER -> setCharter(f, v.text, v.value);
            }
        }
    }

    /**
     * Takes the bloc to war. Every member the decision binds signs its own war against the target,
     * so the bloc fights through the same treaties as anyone else and each member can make its own
     * peace afterwards.
     *
     * @param v the vote that carried it, or null for the chair's own call, which binds everyone
     */
    private void declareFederationWar(Federation f, int target, Federation.FederationVote v) {
        Faction t = getFaction(target);
        if (t == null || f.isMember(target)) {
            return;
        }
        for (int i = 0; i < f.members.size(); i++) {
            int m = f.members.getInt(i);
            if ((v == null || f.binds(v, m)) && !hasTreaty(m, target, TreatyTypeRegistry.WAR)) {
                signTreatyServer(m, target, TreatyTypeRegistry.WAR, 0, Lng.str("Declared by %s", f.name));
            }
        }
        federationNews(f, Lng.str("The federation declared war on %s.", t.getName()));
        sendServerFactionMail(Lng.str("[FEDERATION] %s declared war!", f.name),
                Lng.str("The federation %s declared war on your faction.", f.name), t);
    }

    private void setCharter(Federation f, String text, long flags) {
        flags = Federation.sanitize(flags);
        Federation probe = new Federation();
        probe.charterFlags = flags;
        if (probe.rotationDays() != f.rotationDays()) {
            //a new term length starts a fresh term rather than ending the current one retroactively
            f.chairSince = System.currentTimeMillis();
        }
        f.charter = text == null ? "" : text;
        f.charterFlags = flags;
    }

    private void setChair(Federation f, int factionId) {
        f.leader = factionId;
        f.chairSince = System.currentTimeMillis();
        Faction to = getFaction(factionId);
        federationNews(f, Lng.str("%s now chairs the federation.", to != null ? to.getName() : String.valueOf(factionId)));
    }

    private void inviteToFederation(Federation f, int factionId) {
        if (!f.isInvited(factionId)) {
            f.invites.add(factionId);
        }
        Faction to = getFaction(factionId);
        if (to != null) {
            sendServerFactionMail(Lng.str("[FEDERATION] %s", f.name),
                    Lng.str("%s has invited your faction to join.\nAnswer on the federation tab.", f.name), to);
        }
        sendFederation(f);
    }

    /**
     * Admits a faction, and writes the membership treaties that are what actually allies them.
     */
    private void joinFederation(Federation f, int factionId) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < f.members.size(); i++) {
            int m = f.members.getInt(i);
            if (m != factionId) {
                addTreaty(new FactionTreaty(m, factionId, TreatyTypeRegistry.FEDERATION, now));
            }
        }
        f.members.add(factionId);
        Faction joined = getFaction(factionId);
        federationNews(f, Lng.str("%s joined the federation.", joined != null ? joined.getName() : String.valueOf(factionId)));
        sendFederation(f);
    }

    /**
     * Takes a faction back out, tearing up the membership treaties with it. The leader leaving hands
     * the bloc to whoever has been in it longest, because a bloc with no leader can never admit,
     * expel or dissolve and would sit there forever.
     */
    private void leaveFederation(Federation f, int factionId) {
        for (int i = 0; i < f.members.size(); i++) {
            int m = f.members.getInt(i);
            if (m != factionId) {
                removeTreaty(m, factionId, TreatyTypeRegistry.FEDERATION);
            }
        }
        f.members.rem(factionId);
        //a vote on expelling somebody who has gone is moot; their ballots elsewhere no longer count
        f.votes.removeIf(v -> v.kind == Federation.FederationVote.Kind.EXPEL && v.subject == factionId);
        for (Federation.FederationVote v : f.votes) {
            v.ballots.remove(factionId);
        }
        if (f.members.isEmpty()) {
            dissolveFederation(f);
            return;
        }
        if (f.leader == factionId) {
            setChair(f, f.members.getInt(0));
        }
        resolveVotes(f, System.currentTimeMillis());
        sendFederation(f);
    }

    private void expelFromFederation(Federation f, int factionId) {
        Faction gone = getFaction(factionId);
        federationNews(f, Lng.str("%s was expelled from the federation.", gone != null ? gone.getName() : String.valueOf(factionId)));
        if (gone != null) {
            sendServerFactionMail(Lng.str("[FEDERATION] %s", f.name),
                    Lng.str("Your faction was expelled from %s.", f.name), gone);
        }
        leaveFederation(f, factionId);
    }

    /**
     * Winds a bloc up. Every membership treaty goes, and the record is broadcast with no members -
     * which is what "dissolved" means on the wire and on load alike.
     */
    private void dissolveFederation(Federation f) {
        for (int i = 0; i < f.members.size(); i++) {
            for (int j = i + 1; j < f.members.size(); j++) {
                removeTreaty(f.members.getInt(i), f.members.getInt(j), TreatyTypeRegistry.FEDERATION);
            }
        }
        f.members.clear();
        f.invites.clear();
        f.votes.clear();
        synchronized (federations) {
            federations.remove(f.id);
        }
        sendFederation(f);
    }

    /**
     * A faction that no longer exists cannot be in a bloc, the same way it leaves no treaties behind.
     */
    /**
     * Winds a bloc up on the server's own authority - no leader, no vote.
     *
     * @return false if there is no such bloc
     */
    public boolean dissolveFederationServer(int id) {
        Federation f = getFederation(id);
        if (f == null) {
            return false;
        }
        federationNews(f, Lng.str("The federation was dissolved by an administrator."));
        dissolveFederation(f);
        return true;
    }

    private void removeFederationsOf(int factionId) {
        for (Federation f : getFederations()) {
            if (f.isMember(factionId)) {
                leaveFederation(f, factionId);
            } else if (f.invites.rem(factionId)) {
                sendFederation(f);
            }
        }
    }

    /**
     * Lapses any vote nobody finished answering and passes a rotating chair on when its term is up.
     * Cheap enough to run on the faction tick: there are tens of blocs and a few votes each.
     */
    private void checkFederationVotes() {
        if (!onServer) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Federation f : getFederations()) {
            boolean changed = false;
            int days = f.rotationDays();
            if (days > 0 && f.members.size() > 1 && now - f.chairSince >= days * Federation.DAY_MS) {
                int at = f.members.indexOf(f.leader);
                setChair(f, f.members.getInt((at + 1) % f.members.size()));
                changed = true;
            }
            for (Federation.FederationVote v : f.votes) {
                if (now >= v.closes) {
                    resolveVotes(f, now);
                    changed = true;
                    break;
                }
            }
            if (changed && !f.isDissolved()) {
                sendFederation(f);
            }
        }
    }

    private void federationNews(Federation f, String message) {
        for (int i = 0; i < f.members.size(); i++) {
            Faction m = getFaction(f.members.getInt(i));
            if (m != null) {
                System.err.println("[SERVER][FACTION][" + m.getName() + "]" + "[FEDERATION][" + f.name + "] " + message);
            }
        }
    }

    private void refuseFederation(PlayerState p, Object[] message) {
        p.sendServerMessage(new ServerMessage(message, ServerMessage.MESSAGE_TYPE_ERROR, p.getId()));
    }

    /**
     * Pushes a bloc to every client online, redacted for each of them.
     *
     * <p>One send per recipient rather than one broadcast, which is the price of keeping the vote
     * private - and a small one: this runs when somebody joins, leaves or votes, and the loop is over
     * players online rather than anything per-tick.</p>
     */
    private void sendFederation(Federation f) {
        changed();
        if (!onServer) {
            return;
        }
        for (PlayerState p : ((GameServerState) state()).getPlayerStatesByName().values()) {
            sendFederationTo(p, f);
        }
    }

    private void sendFederationTo(PlayerState p, Federation f) {
        if (p == null || p.getClientChannel() == null) {
            return;
        }
        BFPacket.toPlayer(p, Kind.FEDERATION, f.copyFor(p.getFactionId()).toTag());
    }

    /**
     * Everything this player is allowed to know about every bloc, for a client that has just
     * connected. There is no broadcast to pick it up from any more, so somebody has to say it once.
     */
    public void sendAllFederationsTo(PlayerState p) {
        if (!onServer) {
            return;
        }
        for (Federation f : getFederations()) {
            sendFederationTo(p, f);
        }
    }

    /**
     * The receiving side of the wire. A bloc with no members has been dissolved, which is how that
     * travels without a removed flag on the record.
     *
     * <p>Replaces wholesale, with no merge: the server redacted this copy for the client it sent it
     * to, so whatever arrived is the complete picture for this client and the one before it is not.</p>
     */
    public void applyFederation(Federation f) {
        synchronized (federations) {
            if (f.isDissolved()) {
                federations.remove(f.id);
            } else {
                federations.put(f.id, f);
            }
        }
        changed();
    }

    private Tag toFederationTag() {
        synchronized (federations) {
            Tag[] t = new Tag[federations.size() + 1];
            int i = 0;
            for (Federation f : federations.values()) {
                t[i++] = f.toTag();
            }
            t[t.length - 1] = FinishTag.INST;
            return new Tag(Tag.Type.STRUCT, null, t);
        }
    }

    private void fromFederationTag(Tag tag) {
        Tag[] t = tag.getStruct();
        int highest = 0;
        for (int i = 0; i < t.length - 1; i++) {
            Federation f = new Federation();
            f.fromTag(t[i]);
            if (!f.isDissolved()) {
                federations.put(f.id, f);
                highest = Math.max(highest, f.id);
            }
        }
        nextFederationId = highest + 1;
    }

    /**
     * Puts a treaty in force. Signing one of a type the pair already holds replaces it, so that
     * renewing an expiring pact does not leave two of them stacked.
     *
     * @return the treaty now in force
     */
    public FactionTreaty addTreaty(FactionTreaty treaty) {
        long code = FactionRelation.getCode(treaty.from, treaty.to);
        FactionTreatySet set;
        synchronized (treaties) {
            set = treaties.get(code);
            if (set == null) {
                set = new FactionTreatySet();
                treaties.put(code, set);
            }
            set.removeByType(treaty.typeId);
            set.add(treaty);
        }
        changed();
        sendTreatyChange(treaty, false);
        return treaty;
    }

    /**
     * @return the treaty that was in force, or null if the pair did not hold one of that type.
     */
    public FactionTreaty removeTreaty(int a, int b, String typeId) {
        long code = FactionRelation.getCode(a, b);
        FactionTreaty removed;
        synchronized (treaties) {
            FactionTreatySet set = treaties.get(code);
            if (set == null) {
                return null;
            }
            removed = set.removeByType(typeId);
            if (set.isEmpty()) {
                treaties.remove(code);
            }
        }
        if (removed != null) {
			changed();
			sendTreatyChange(removed, true);
		}
		return removed;
	}

	/**
	 * @return the relationShipOffers
	 */
	public Long2ObjectOpenHashMap<DiplomacyProposal> getRelationShipOffers() {
		return relationShipOffers;
	}

    /**
     * Queues a proposal the engine authored itself - an auto-declared war, an NPC reaction, an
     * admin command. Marked so the permission gate in the drain lets it through: it did not come
     * from a client, so there is no member to check it against.
	 */
	public void relationShipOfferServer(DiplomacyProposal a) {
		a.serverAuthored = true;
		synchronized (relationOffersToAdd) {
			relationOffersToAdd.add(a);
		}
	}

    public void sendRelationshipAccept(String initiator, DiplomacyProposal offer, boolean accept) {
        sendProposal(DiplomacyProposal.respondTo(offer, initiator, accept));
    }

    /**
     * Answers an offer with different terms. Settles the offer being answered and puts the new terms
     * on the table in its place.
     *
     * @param typeId    treaty to counter with, or null for plain peace
     * @param expiresAt when the countered treaty would lapse, or 0 for no term
     */
	public void sendCounterOffer(String initiator, DiplomacyProposal offer, String typeId, long expiresAt,
								 String message, String name) {
		sendCounterOffer(initiator, offer, typeId, expiresAt, message, name, List.of());
    }

	public void sendCounterOffer(String initiator, DiplomacyProposal offer, String typeId, long expiresAt,
								 String message, String name, List<SettlementTerm> terms) {
		sendProposal(DiplomacyProposal.counterTo(offer, initiator, typeId, expiresAt, message, name, terms));
    }

    /**
     * Asks whether a treaty would be accepted, so the GUI can say so before anything is offered.
     * The answer arrives asynchronously; observers are notified with {@code "DIPLOMACY_QUERY"} and
     * read it from {@link #getQueryResult()}.
     */
    public void sendAcceptanceQuery(String initiator, int from, int to, String typeId) {
        sendAcceptanceQuery(initiator, from, to, typeId, List.of());
    }

    public void sendAcceptanceQuery(String initiator, int from, int to, String typeId, List<SettlementTerm> terms) {
        sendProposal(DiplomacyProposal.query(initiator, from, to, typeId, terms));
    }

    /**
     * @return the last acceptance answer received, or null if none has arrived this session.
     */
    public DiplomacyProposal getQueryResult() {
        return queryResult;
    }

    /**
     * Proposes a specific treaty type, rather than one of the three legacy relations.
     */
	public void sendTreatyProposal(String initiator, int from, int to, String typeId, String message, DiplomacyProposal.Action action) {
        sendTreatyProposal(initiator, from, to, typeId, message, action, 0L);
    }

    /**
     * @param typeId    treaty to propose, or null for white peace - which signs nothing and instead
     *                  drops whatever was forcing the pair apart
     * @param expiresAt when the treaty would lapse, or 0 for one with no term
     */
	public void sendTreatyProposal(String initiator, int from, int to, String typeId, String message, DiplomacyProposal.Action action, long expiresAt) {
		sendTreatyProposal(initiator, from, to, typeId, message, action, expiresAt, null);
	}

	/**
	 * @param name what the proposer wants the agreement called, or null/blank to let the server name
	 *             it from the treaty type and the two signatories
	 */
	public void sendTreatyProposal(String initiator, int from, int to, String typeId, String message,
								   DiplomacyProposal.Action action, long expiresAt, String name) {
		sendTreatyProposal(initiator, from, to, typeId, message, action, expiresAt, name, List.of());
	}

	/**
	 * @param terms what changes hands at signing as well; see {@link SettlementTerm}
	 */
	public void sendTreatyProposal(String initiator, int from, int to, String typeId, String message,
								   DiplomacyProposal.Action action, long expiresAt, String name,
								   List<SettlementTerm> terms) {
        byte rel = FactionRelation.RType.NEUTRAL.code;
        if (typeId != null) {
            TreatyType type = TreatyTypeRegistry.get(typeId);
            if (type == null) {
                throw new IllegalArgumentException("no such treaty type: " + typeId);
            }
            if (type.getImplies() != null) {
                rel = type.getImplies().code;
            }
        }
        DiplomacyProposal offer = new DiplomacyProposal();
        offer.set(initiator, from, to, typeId, rel, message, action, expiresAt);
		offer.name = name == null ? "" : name;
		offer.terms.addAll(terms);
        sendProposal(offer);
    }

    private void sendProposal(DiplomacyProposal p) {
		//Todo: the answer to a query reaches every client, which ignores the ones that are not its own.
		//Per-recipient delivery is one toPlayer away if it ever matters.
		if (onServer) {
			broadcast(Kind.OFFER, p.toTagStructure());
		} else {
			BFPacket.toServer(Kind.PROPOSAL, p.toTagStructure());
		}
	}

	/**
	 * Tears up a treaty that is in force and lets everything downstream of it catch up: the ledger
	 * records who broke it, and a treaty that was forcing a relation leaves the pair neutral.
	 */
	private void breakTreatyInForce(int a, int b, TreatyType type) {
		removeTreaty(a, b, type.getId());
		diplomacyAction(DiplomacyAction.DiplActionType.TREATY_BROKEN, b, a, type.getId());
		if (type.getImplies() != null) {
			//whatever the treaty was forcing is gone, so the pair falls back to plain neutrality
			queueRelation(a, b, FactionRelation.RType.NEUTRAL.code);
		}
		if (onServer && type.getImplies() == FactionRelation.RType.FRIEND) {
			diplomacyAction(DiplomacyAction.DiplActionType.ALLIANCE_CANCEL, b, a);
		}
	}

	/**
	 * Signs a treaty on the server's own authority, as an admin command does, with no offer and no
	 * acceptance.
	 *
	 * <p>It goes through the same apply a negotiated proposal ends at, so the relation, the opinion
	 * ledger, the faction news and the client sync all see exactly what they would see from one that
	 * was agreed - which is the point: an admin-set agreement has to be the same object a player can
	 * then break, renew or counter, not a relation written behind the treaty model's back.</p>
	 *
	 * @param typeId    a registered treaty type
	 * @param expiresAt when it lapses, or 0 for no term
	 * @return false if the type is not registered
	 */
	public boolean signTreatyServer(int a, int b, String typeId, long expiresAt, String clause) {
		if (TreatyTypeRegistry.get(typeId) == null) {
			return false;
		}
		DiplomacyProposal signed = new DiplomacyProposal();
		signed.set("ADMIN", a, b, typeId, FactionRelation.RType.NEUTRAL.code, clause == null ? "" : clause, DiplomacyProposal.Action.ACCEPT, expiresAt);
		signed.serverAuthored = true;
		//an admin setting a relation is not an act of aggression anyone is pledged to answer
		signed.noCallToArms = true;
		applyAgreedProposal(signed);
		return true;
	}

	/**
	 * Tears up a treaty on the server's own authority, ignoring whether its type allows a signatory
	 * to walk away from it - an admin is not a signatory.
	 *
	 * @return false if the pair did not hold one of that type
	 */
	public boolean breakTreatyServer(int a, int b, String typeId) {
		TreatyType type = TreatyTypeRegistry.get(typeId);
		if (type == null || !hasTreaty(a, b, typeId)) {
			return false;
		}
		breakTreatyInForce(a, b, type);
		return true;
    }

    /**
     * Drops every treaty between the pair that forces a relation, leaving the rest in place.
     */
    private void clearImplyingTreaties(int a, int b) {
        List<String> forcing = null;
        for (FactionTreaty t : getTreaties(a, b).getTreaties()) {
            TreatyType type = t.getType();
            if (type != null && type.getImplies() != null) {
                if (forcing == null) {
					forcing = new ObjectArrayList<String>();
				}
				forcing.add(t.typeId);
			}
		}
		if (forcing != null) {
			for (String typeId : forcing) {
				removeTreaty(a, b, typeId);
			}
		}
	}

    private void handleFactionOffersAccept() {
        if (toAddProposalResponses.isEmpty()) {
            return;
        }
        int accepted = 0;
        changed();
        synchronized (toAddProposalResponses) {
            while (!toAddProposalResponses.isEmpty()) {
                DiplomacyProposal response = toAddProposalResponses.remove(0);
                DiplomacyProposal offer = relationShipOffers.get(response.getCode());
                if (offer == null) {
                    //System.err.println("[FACTION][OFFERACCEPT] relation ship offer not found " + response.getCode());
                    continue;
                }
                if (!onServer) {
                    //the server already decided; the client is only being told to drop the row
                    relationShipOffers.remove(response.getCode());
                    continue;
                }
                //the side that was asked is the side that answers, so the permission that matters
                //belongs to the offer's target - not to whoever happened to send the packet
                Faction target = getFaction(offer.b);
                //a counter is also a proposal, so it is checked against the terms it asks for as well
                //as the ones it turns down
				if (!response.serverAuthored && (!mayNegotiate(target, response.getInitiator(), offer.typeId) || (response.action == DiplomacyProposal.Action.COUNTER && (!mayNegotiate(target, response.getInitiator(), response.typeId) || termsProblem(target, response) != null)))) {
                    //the offer itself stands: a refused answer must not consume it
                    denyDiplomacy(response.getInitiator(), Lng.astr("Cannot answer this offer:\nFaction permission denied!"));
                    continue;
                }
                if (offer.against != 0) {
                    if (response.action == DiplomacyProposal.Action.COUNTER) {
                        denyDiplomacy(response.getInitiator(), Lng.astr("A call to arms is answered yes or no."));
                        continue;
                    }
                    relationShipOffers.remove(response.getCode());
                    answerCallToArms(offer, response.action == DiplomacyProposal.Action.ACCEPT);
                    sendRelationshipAccept(response.getInitiator(), offer, response.action == DiplomacyProposal.Action.ACCEPT);
                    accepted++;
                    continue;
                }
                relationShipOffers.remove(response.getCode());
                boolean accept = response.action == DiplomacyProposal.Action.ACCEPT;
                String unsignable = accept ? settlementBlocker(offer) : null;
                if (unsignable != null) {
                    //the world moved since it was offered; signing half of it is worse than none
                    accept = false;
                    diplomacyAction(DiplomacyAction.DiplActionType.TREATY_REFUSED, offer.a, offer.b, offer.typeId);
                    for (int side : new int[]{offer.a, offer.b}) {
                        Faction f = getFaction(side);
                        if (f != null) {
                            sendServerFactionMail(Lng.str("[FACTION] Settlement could not be signed"),
                                    Lng.str("The settlement between %s and %s fell through: %s", getFactionName(offer.a), getFactionName(offer.b), unsignable), f);
                        }
                    }
                } else if (accept) {
					LogUtil.log().fine("[FACTION] Relationship Offer accepted: " + offer + " between " + getFactionName(offer.a) + " AND " + getFactionName(offer.b));
                    applyAgreedProposal(offer);
                    applyTerms(offer);
                } else if (response.action == DiplomacyProposal.Action.COUNTER) {
                    //a counter settles the offer it answers and starts a new one the other way, which
                    //the original proposer can accept, refuse, or counter again
                    diplomacyAction(DiplomacyAction.DiplActionType.TREATY_REFUSED, offer.a, offer.b, offer.typeId);
                    DiplomacyProposal back = response.asCounterProposal();
                    synchronized (relationOffersToAdd) {
                        relationOffersToAdd.add(back);
                    }
                } else {
                    diplomacyAction(DiplomacyAction.DiplActionType.TREATY_REFUSED, offer.a, offer.b, offer.typeId);
				}
				//deligate to remove offer from clients
                sendRelationshipAccept(response.getInitiator(), offer, accept);
                accepted++;
            }
        }
        if (accepted > 10) {
            System.err.println("[FACTION] more than 10 relationship offers in this update: " + accepted);
        }
    }

    /**
     * What the war between the two is being fought for, or empty when there is no war or it named
     * nothing. Works on clients too: the goals ride the treaty.
     */
    public List<SettlementTerm> warGoals(int a, int b) {
        FactionTreaty war = getTreaties(a, b).get(TreatyTypeRegistry.WAR);
        return war != null ? war.goals : List.of();
    }

    /**
     * The faction holding {@code vassal} in vassalage, or 0 if it answers to nobody.
     */
    public int overlordOf(int vassal) {
        synchronized (treaties) {
            for (FactionTreatySet set : treaties.values()) {
                for (FactionTreaty t : set.getTreaties()) {
                    if (t.to == vassal && TreatyTypeRegistry.VASSALAGE.equals(t.typeId)) {
                        return t.from;
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Server, once per engine income turn: every tributary pays its overlord a share of what it
     * earned that turn. Never more than it holds, so tribute alone cannot put a faction in debt.
     */
    public void payTribute() {
        int percent = BFConfig.tributePercent();
        if (percent <= 0) {
            return;
        }
        List<FactionTreaty> owed = new ObjectArrayList<FactionTreaty>();
        synchronized (treaties) {
            for (FactionTreatySet set : treaties.values()) {
                for (FactionTreaty t : set.getTreaties()) {
                    if (TreatyTypeRegistry.TRIBUTARY.equals(t.typeId)) {
                        owed.add(t);
                    }
                }
            }
        }
        for (FactionTreaty t : owed) {
            Faction lord = getFaction(t.from);
            Faction payer = getFaction(t.to);
            if (lord == null || payer == null) {
                continue;
            }
            float amount = tributeOwed(payer.lastPointsFromOnline + payer.lastPointsFromOffline, payer.factionPoints, percent);
            if (amount <= 0) {
                continue;
            }
            payer.factionPoints -= amount;
            lord.factionPoints += amount;
            payer.sendFactionPointUpdate(((GameServerState) state()).getGameState());
            lord.sendFactionPointUpdate(((GameServerState) state()).getGameState());
        }
    }

    /**
     * {@code percent} of what was earned this turn, capped at what is held; never negative.
     */
    public static float tributeOwed(float earned, float held, int percent) {
        return Math.max(0, Math.min(earned * percent / 100f, held));
    }

    /**
     * What is wrong with a proposal's terms as offered, or null. Run on the server for every offer and
     * counter before it is staged, so a term nobody may offer never reaches the other side's inbox.
     */
    private String termsProblem(Faction proposer, DiplomacyProposal p) {
        TreatyType offered = p.typeId != null ? TreatyTypeRegistry.get(p.typeId) : null;
        if (offered != null && offered.needsAcceptance() && getRelation(p.a, p.b) == FactionRelation.RType.ENEMY) {
            //at war the only thing on the table is how it ends; anything else rides the peace as a term
            return Lng.str("You are at war. Offer peace, with %s as one of its terms.", offered.getDisplayName());
        }
        if (offered != null && offered.getImplies() == FactionRelation.RType.ENEMY && !p.serverAuthored) {
            int lord = overlordOf(proposer.getIdFaction());
            if (lord != 0) {
                return Lng.str("A vassal of %s cannot declare war on its own.", getFactionName(lord));
            }
        }
        if (p.terms.isEmpty()) {
            return null;
        }
        if (p.terms.size() > DiplomacyProposal.MAX_TERMS) {
            return Lng.str("At most %s terms.", DiplomacyProposal.MAX_TERMS);
        }
        TreatyType type = p.typeId != null ? TreatyTypeRegistry.get(p.typeId) : null;
        if (type != null && !type.needsAcceptance() && type.getImplies() != FactionRelation.RType.ENEMY) {
            //terms on a war declaration are its war goals; any other declaration has nobody to settle with
            return Lng.str("%s takes no terms.", type.getDisplayName());
        }
        String bad = SettlementTerm.problem(p.terms, p.a, p.b);
        if (bad != null) {
            return bad;
        }
        if (type != null && type.getImplies() == FactionRelation.RType.ENEMY) {
            //war goals are demands; the declarer conceding something is a peace offer, not a war
            for (SettlementTerm t : p.terms) {
                if (t.giver == proposer.getIdFaction()) {
                    return Lng.str("War goals can only be demands on %s.", getFactionName(p.b));
                }
            }
        }
        if (type != null && type.getImplies() == FactionRelation.RType.ENEMY && !p.serverAuthored) {
            List<String> unmet = new ObjectArrayList<String>();
            if (!diplomacyOf(proposer).getEntity(p.b).hasCasusBelli(p.terms, unmet)) {
                return Lng.str("No casus belli: %s", joinReasons(unmet));
            }
        }
        if (!p.serverAuthored) {
            for (SettlementTerm t : p.terms) {
                if (t.kind == SettlementTerm.Kind.SIGN_TREATY && !mayNegotiate(proposer, p.getInitiator(), t.typeId)) {
                    return Lng.str("Faction permission denied!");
                }
            }
        }
        return null;
    }

    /**
     * Why a settlement cannot be signed right now, or null. Checked in full before anything applies,
     * which is what makes {@link #applyTerms} all or nothing.
     */
    private String settlementBlocker(DiplomacyProposal offer) {
        String bad = SettlementTerm.problem(offer.terms, offer.a, offer.b);
        if (bad != null) {
            return bad;
        }
        Map<Integer, Float> owed = new HashMap<>();
        for (SettlementTerm t : offer.terms) {
            if (t.kind == SettlementTerm.Kind.TRANSFER_FACTION_POINTS) {
                owed.merge(t.giver, t.amount, Float::sum);
            } else if (t.kind == SettlementTerm.Kind.BREAK_TREATY) {
                int with = t.breakWith(offer.a, offer.b);
                TreatyType type = TreatyTypeRegistry.get(t.typeId);
                if (!hasTreaty(t.giver, with, t.typeId)) {
                    return Lng.str("%s no longer holds that treaty with %s.", getFactionName(t.giver), getFactionName(with));
                }
                //breaking it with the other signatory is agreed; with a third party it is not
                if (with != t.receiver(offer.a, offer.b) && !type.isBreakable()) {
                    return Lng.str("%s cannot be broken unilaterally.", type.getDisplayName());
                }
            }
        }
        for (Map.Entry<Integer, Float> e : owed.entrySet()) {
            Faction f = getFaction(e.getKey());
            if (f == null || f.factionPoints < e.getValue()) {
                return Lng.str("%s cannot pay %s faction points.", getFactionName(e.getKey()), Math.round(e.getValue()));
            }
        }
        return null;
    }

    /**
     * Applies a settlement's terms. Call only after {@link #settlementBlocker} said yes, and after the
     * main agreement, so a peace that clears forcing treaties cannot clear an alliance a term just signed.
     */
    private void applyTerms(DiplomacyProposal offer) {
        for (SettlementTerm t : offer.terms) {
            int receiver = t.receiver(offer.a, offer.b);
            switch (t.kind) {
                case TRANSFER_FACTION_POINTS: {
                    Faction g = getFaction(t.giver);
                    Faction r = getFaction(receiver);
                    g.factionPoints -= t.amount;
                    r.factionPoints += t.amount;
                    g.sendFactionPointUpdate(((GameServerState) state()).getGameState());
                    r.sendFactionPointUpdate(((GameServerState) state()).getGameState());
                    break;
                }
                case BREAK_TREATY: {
                    TreatyType type = TreatyTypeRegistry.get(t.typeId);
                    int with = t.breakWith(offer.a, offer.b);
                    if (with == receiver) {
                        //dropped by agreement, so nobody is owed a grudge for it
                        removeTreaty(t.giver, with, t.typeId);
                        if (type.getImplies() != null) {
                            queueRelation(t.giver, with, FactionRelation.RType.NEUTRAL.code);
                        }
                    } else {
                        breakTreatyInForce(t.giver, with, type);
                    }
                    break;
                }
                case SIGN_TREATY:
                    //receiver first: a demand reads "a demands of b", and the giver is the one bound
                    signTreatyServer(receiver, t.giver, t.typeId, 0L, "");
                    break;
            }
        }
    }

    /**
     * Debug only, never saved: NPC factions an admin set to accept every demand made of them.
     */
    private final IntOpenHashSet debugYielding = new IntOpenHashSet();

    /**
     * Admin debug toggle; see {@code /faction_npc_yield}. Only NPC factions take it.
     *
     * @return false if the faction is not an NPC faction
     */
    public boolean setDebugYielding(Faction npc, boolean on) {
        if (!npc.isNPC()) {
            return false;
        }
        if (on) {
            debugYielding.add(npc.getIdFaction());
        } else {
            debugYielding.remove(npc.getIdFaction());
        }
        return true;
    }

    public boolean isDebugYielding(int factionId) {
        return debugYielding.contains(factionId);
    }

    /**
     * Whether the offer asks something of {@code target}: a subjugating treaty, or a term it gives.
     */
    private static boolean isDemandOn(DiplomacyProposal offer, int target) {
        TreatyType type = offer.typeId != null ? TreatyTypeRegistry.get(offer.typeId) : null;
        if (type != null && type.getKind() == TreatyType.Kind.DEMAND) {
            return true;
        }
        for (SettlementTerm t : offer.terms) {
            if (t.giver == target) {
                return true;
            }
        }
        return false;
    }

    /**
     * An NPC's verdict on an offer to it, with the terms it would sign instead when it would not.
     */
    private record NpcVerdict(boolean accept, List<SettlementTerm> counter) {
    }

    /**
     * Prices the offer's terms against what the agreement is worth to the NPC; see
     * {@link SettlementPricing}. Fills {@code unmet} with why not.
     */
    private NpcVerdict judgeAsNPC(DiplomacyProposal offer, Faction npc, List<String> unmet) {
        if (debugYielding.contains(npc.getIdFaction()) && isDemandOn(offer, npc.getIdFaction())) {
            return new NpcVerdict(true, null);
        }
        DiplomacyEntity e = diplomacyOf(npc).getEntity(offer.a);
        List<String> ruleUnmet = new ObjectArrayList<>();
        boolean rule = e.wouldAccept(offer.typeId, ruleUnmet);
        boolean peace = offer.typeId == null && getRelation(offer.a, offer.b) == FactionRelation.RType.ENEMY;
        double treatyValue = e.getConfig().getSettlementTreatyValue();
        double need = SettlementPricing.need(peace, rule, e.getRelativeStrength(), e.getConfig().getSettlementPeaceValue());
        double net = SettlementPricing.net(offer.terms, offer.b, offer.a, offer.b, treatyValue);
        if (net >= need) {
            return new NpcVerdict(true, null);
        }
        List<SettlementTerm> counter = SettlementPricing.counter(offer.terms, offer.b, offer.a, offer.b, need, treatyValue);
        if (Double.isInfinite(need) || net == Double.NEGATIVE_INFINITY) {
            unmet.addAll(ruleUnmet);
            if (net == Double.NEGATIVE_INFINITY) {
                unmet.add(Lng.str("They will not be made subject to anyone."));
            }
        } else if (!rule) {
            unmet.add(Lng.str("They want %s faction points' worth more to make peace.", Math.round(need - net)));
        } else {
            unmet.add(Lng.str("They will not give up that much: %s faction points' worth too far.", Math.round(need - net)));
        }
        if (counter != null) {
            unmet.add(Lng.str("They would settle for:\n%s", SettlementTerm.describeAll(counter, this::getFactionName, offer.a, offer.b, Lng.str("Nothing changing hands."))));
        }
        return new NpcVerdict(false, counter);
    }

    /**
     * Server: a faction was attacked. {@code attackerDbId} is a faction id, or a player's db id when a
     * player's ship did it, as the engine reports it.
     *
     * <p>Only an attack outside a declared war calls the victim's pact partners: one between neutrals,
     * or under the engine's own auto-war, which leaves no war treaty behind. A declared war calls them
     * once, when it is declared.</p>
     */
    public void onAttacked(int victim, long attackerDbId) {
        int aggressor;
        if (attackerDbId < Integer.MAX_VALUE) {
            aggressor = (int) attackerDbId;
        } else {
            PlayerState player = ((GameServerState) state()).getPlayerStatesByDbId().get(attackerDbId);
            if (player == null) {
                return;
            }
            aggressor = player.getFactionId();
        }
        if (aggressor == 0 || aggressor == victim || getFaction(aggressor) == null) {
            return;
        }
        if (hasTreaty(victim, aggressor, TreatyTypeRegistry.WAR)) {
            //a declared war called its pacts in when it was declared; counting its fighting again
            //would also call the aggressor's partners against anyone who answered that call
            return;
        }
        callToArms(victim, aggressor);
    }

    /**
     * When the same pair is called again within this window, it is the same attack still being
     * reported, and the declaration it caused may not have applied yet.
     */
    private static final long CALL_TO_ARMS_DEBOUNCE_MS = 5000;
    private final Long2LongOpenHashMap lastCallToArms = new Long2LongOpenHashMap();

    /** How long a partner has to answer a call to arms before silence counts as refusing it. */
    private static final long CALL_TO_ARMS_ANSWER_MS = 15 * 60000;

    /**
     * Calls every defensive pact partner of the victim to war on the aggressor.
     *
     * <p>A player faction is asked, through its inbox, and may refuse: refusing (or not answering in
     * time) is recorded against it in the victim's ledger as a broken treaty. An NPC partner simply
     * honours the pact. A partner that is itself allied or pact-bound to the aggressor is not asked
     * at all, and is told why, rather than being made to choose between two promises.</p>
     */
    private void callToArms(int victim, int aggressor) {
        long now = System.currentTimeMillis();
        for (FactionTreatySet set : treaties.values()) {
            for (FactionTreaty t : set.getTreaties()) {
                if (!TreatyTypeRegistry.DEFENSIVE_PACT.equals(t.typeId) || !t.involves(victim)) {
                    continue;
                }
                int partner = t.other(victim);
                Faction p = getFaction(partner);
                if (p == null || partner == aggressor || getRelation(partner, aggressor) == FactionRelation.RType.ENEMY) {
                    continue;
                }
                long key = DiplomacyProposal.getOfferCode(partner, aggressor);
                if (now - lastCallToArms.get(key) < CALL_TO_ARMS_DEBOUNCE_MS) {
                    continue;
                }
                lastCallToArms.put(key, now);
                if (getRelation(partner, aggressor) == FactionRelation.RType.FRIEND || hasTreaty(partner, aggressor, TreatyTypeRegistry.DEFENSIVE_PACT)) {
                    sendServerFactionMail(Lng.str("[FACTION] Call to arms ignored"),
                            Lng.str("%s was attacked by %s, but your own pact with %s keeps you out of it.",
                                    getFactionName(victim), getFactionName(aggressor), getFactionName(aggressor)), p);
                    continue;
                }
                if (p.isNPC()) {
                    joinWar(victim, partner, aggressor);
                    continue;
                }
                //ponytail: keyed on the victim->partner pair like any offer, so it replaces a pending
                //offer the victim made the partner. Give calls their own map if that ever bites.
                DiplomacyProposal call = new DiplomacyProposal();
                call.set(getFactionName(victim), victim, partner, TreatyTypeRegistry.WAR, FactionRelation.RType.ENEMY.code,
                        Lng.str("%s was attacked by %s and calls on your defensive pact. Refusing breaks faith with the pact.",
                                getFactionName(victim), getFactionName(aggressor)),
                        DiplomacyProposal.Action.PROPOSE, now + CALL_TO_ARMS_ANSWER_MS);
                call.against = aggressor;
                call.serverAuthored = true;
                relationShipOffers.put(call.getCode(), call);
                sendProposal(call);
                sendServerFactionMail(Lng.str("[FACTION] Call to arms"),
                        Lng.str("%s was attacked by %s and calls on your defensive pact.\nAnswer in your diplomacy inbox within %s, or it counts as refusing.",
                                getFactionName(victim), getFactionName(aggressor), StringTools.formatTimeFromMS(CALL_TO_ARMS_ANSWER_MS)), p);
            }
        }
    }

    private void joinWar(int victim, int partner, int aggressor) {
        DiplomacyProposal war = new DiplomacyProposal();
        war.set(getFactionName(partner), partner, aggressor, TreatyTypeRegistry.WAR, FactionRelation.RType.ENEMY.code,
                Lng.str("Honouring our defensive pact with %s.", getFactionName(victim)), DiplomacyProposal.Action.PROPOSE);
        war.serverAuthored = true;
        war.noCallToArms = true;
        synchronized (relationOffersToAdd) {
            relationOffersToAdd.add(war);
        }
    }

    /**
     * A partner's answer to a call to arms, or its silence. Moot - neither war nor penalty - once the
     * victim and the aggressor are no longer fighting, or the pact is gone.
     */
    private void answerCallToArms(DiplomacyProposal call, boolean join) {
        int victim = call.a;
        int partner = call.b;
        int aggressor = call.against;
        Faction v = getFaction(victim);
        if (v == null || getFaction(partner) == null || getFaction(aggressor) == null
                || getRelation(victim, aggressor) != FactionRelation.RType.ENEMY
                || !hasTreaty(victim, partner, TreatyTypeRegistry.DEFENSIVE_PACT)) {
            return;
        }
        if (join) {
            if (getRelation(partner, aggressor) != FactionRelation.RType.ENEMY) {
                joinWar(victim, partner, aggressor);
            }
            sendServerFactionMail(Lng.str("[FACTION] Call to arms answered"),
                    Lng.str("%s joins the war against %s.", getFactionName(partner), getFactionName(aggressor)), v);
        } else {
            diplomacyAction(DiplomacyAction.DiplActionType.TREATY_BROKEN, victim, partner, TreatyTypeRegistry.DEFENSIVE_PACT);
            sendServerFactionMail(Lng.str("[FACTION] Call to arms refused"),
                    Lng.str("%s did not answer your call to arms against %s.", getFactionName(partner), getFactionName(aggressor)), v);
        }
    }

    /** Server: calls nobody answered in time are refused on the partner's behalf. */
    private void lapseCallsToArms(long now) {
        List<DiplomacyProposal> lapsed = new ObjectArrayList<>();
        synchronized (relationShipOffers) {
            for (DiplomacyProposal o : relationShipOffers.values()) {
                if (o.against != 0 && o.expiresAt < now) {
                    lapsed.add(o);
                }
            }
        }
        for (DiplomacyProposal o : lapsed) {
            DiplomacyProposal no = DiplomacyProposal.respondTo(o, getFactionName(o.b), false);
            no.serverAuthored = true;
            synchronized (toAddProposalResponses) {
                toAddProposalResponses.add(no);
            }
        }
    }

    /**
     * Puts an agreed proposal into force. A proposal that named a treaty type signs one; a bare
     * relation change - white peace - signs nothing and instead drops whatever was forcing the pair
     * apart. Either way the legacy relation is written too, so the three-value answer the rest of
     * the engine reads keeps up, and the existing listeners and rule triggers still fire.
     */
    private void applyAgreedProposal(DiplomacyProposal offer) {
        TreatyType type = offer.typeId != null ? TreatyTypeRegistry.get(offer.typeId) : null;
        if (type != null) {
            FactionTreaty treaty = new FactionTreaty(offer.a, offer.b, type.getId(), System.currentTimeMillis());
            treaty.clause = offer.getMessage();
            treaty.expiresAt = offer.treatyExpiry();
			treaty.name = nameFor(offer, type, treaty.signedAt);
            if (type.getImplies() == FactionRelation.RType.ENEMY) {
                //a war is declared, never agreed, so its terms are what it is for rather than anything
                //that applies now; they come back as the starting point of every peace offer
                treaty.goals.addAll(offer.terms);
            }
            //re-signing a type already held replaces it, which is what renegotiating an existing
            //treaty amounts to: same parties, new term
            addTreaty(treaty);
        } else {
            clearImplyingTreaties(offer.a, offer.b);
        }

        if (type != null) {
            //both directions: the ledger entry is "what they did to us", so each side records it
            diplomacyAction(DiplomacyAction.DiplActionType.TREATY_SIGNED, offer.a, offer.b, type.getId());
            diplomacyAction(DiplomacyAction.DiplActionType.TREATY_SIGNED, offer.b, offer.a, type.getId());
        }

        FactionRelation.RType implied = type != null ? type.getImplies() : FactionRelation.RType.NEUTRAL;
        if (implied == null) {
            //a trade pact or shared vision changes what is permitted, not who shoots
            return;
        }
        if (implied == FactionRelation.RType.ENEMY) {
            fm().getNpcFactionNews().war(offer.a, getFactionName(offer.b));
            if (onServer && !offer.noCallToArms) {
                callToArms(offer.b, offer.a);
            }
        } else if (implied == FactionRelation.RType.FRIEND) {
            fm().getNpcFactionNews().ally(offer.a, getFactionName(offer.b));
        } else {
            fm().getNpcFactionNews().peace(offer.a, getFactionName(offer.b));
        }
        queueRelation(offer.a, offer.b, implied.code);
    }

	/**
	 * What the agreement being signed is called: what the proposer asked for if they asked for
	 * anything, and otherwise a name built from the signatories and their home systems.
	 *
	 * <p>The name is only ever decided here, on the server, at the moment of signing. A client
	 * cannot build the generated one for a preview without knowing what the galaxy calls a system it
	 * may never have visited, and a name that drifted between the two signatories would defeat the
	 * point of having one - so the propose dialog offers to name it and says nothing about what the
	 * default will be.</p>
	 *
	 * <p>{@code ceded} is deliberately left unset until phase 5's settlement terms exist to say what
	 * changed hands. Until then the templates that name a concession never come up at all, which is
	 * why {@link TreatyNames} filters those templates out rather than filling them from a home
	 * system: a faction does not concede its capital, so naming a concession after one would be a
	 * lie the players would read on every row.</p>
	 */
	private String nameFor(DiplomacyProposal offer, TreatyType type, long signedAt) {
		String asked = TreatyNames.sanitize(offer.name);
		if (asked != null) {
			return asked;
		}
		return TreatyNames.defaultName(type, signedAt, new TreatyNames.Tokens()
				.from(getFactionName(offer.a))
				.to(getFactionName(offer.b))
				.homes(homeSystemName(offer.a), homeSystemName(offer.b)));
	}

	/**
	 * The name of the star system a faction's home base sits in, or null if it has no home base, the
	 * lookup is not available (this runs on clients too) or anything about the galaxy is unusable.
	 * Never loads a sector: the galaxy's names are generated from the system seed and cached.
	 */
	private String homeSystemName(int factionId) {
		Faction f = getFaction(factionId);
		if (f == null || !onServer || f.getHomeSector().equals(0, 0, 0)) {
			return null;
		}
		return systemName((GameServerState) state(), f.getHomeSector());
	}

	/** Server: the name of the star system {@code sector} is in, or null. Never loads a sector. */
	public static String systemName(GameServerState state, Vector3i sector) {
		try {
			Vector3i system = VoidSystem.getContainingSystem(sector, new Vector3i());
			Galaxy galaxy = state.getUniverse().getGalaxyFromSystemPos(system);
			if (galaxy == null) {
				return null;
			}
			//the galaxy's name table is indexed in galaxy-local coordinates, not absolute ones
			String name = galaxy.getName(Galaxy.getLocalCoordinatesFromSystem(system, new Vector3i()));
			return name == null || name.isEmpty() ? null : name;
		} catch (Exception e) {
			System.err.println("[BetterFactions] could not name the system of " + sector + ": " + e);
			return null;
		}
	}

    private void queueRelation(int a, int b, byte rel) {
        if (onServer) {
            fm().setRelationServer(a, b, rel);
        }
    }

    /**
     * Whether a client request may set this faction's foreign policy.
     *
     * <p>War and the three legacy relations sit behind RELATIONSHIPS_EDIT. Any other treaty also
     * accepts DIPLOMACY_TREATIES, so a faction can let someone sign a trade pact without also
     * letting them start a war. NPC factions get no bypass here: their own ledger decides their
     * policy, and anything it decides is authored server-side and never reaches this check.</p>
     */
    private boolean mayNegotiate(Faction f, String initiator, String typeId) {
        if (f == null) {
            return false;
        }
        if (!(state() instanceof GameServerState s)) {
            return true;
        }
        if (initiator != null && s.isAdmin(initiator)) {
            return true;
        }
        FactionPermission p = initiator != null ? f.getMembersUID().get(initiator) : null;
        if (p == null) {
            return false;
        }
        if (p.hasRelationshipPermission(f)) {
            return true;
        }
        boolean hostility = typeId == null || TreatyTypeRegistry.WAR.equals(typeId);
        return !hostility && Perms.has(f, p, Perms.TREATIES);
	}

	/**
	 * Tells a player why their request was refused, if they are online to hear it. A refusal is
	 * always logged by its caller, so a name nobody is using is not itself an error here.
	 */
	private void denyToPlayer(String name, Object[] message) {
		if (name == null || !(state() instanceof GameServerState)) {
            return;
		}
		try {
			((GameServerState) state()).getPlayerFromName(name).sendServerMessagePlayerError(message);
        } catch (PlayerNotFountException e) {
		}
	}

	private void denyDiplomacy(String initiator, Object[] message) {
		System.err.println("[SERVER][FACTION] diplomacy request denied for " + initiator);
		denyToPlayer(initiator, message);
    }

    public void handleRelationShipOffers() {
        if (relationOffersToAdd.isEmpty()) {
            return;
        }
        synchronized (relationOffersToAdd) {
            while (!relationOffersToAdd.isEmpty()) {
                changed();
                DiplomacyProposal fk = relationOffersToAdd.remove(0);

                if (fk.a == fk.b) {
                    System.err.println("[FACTIONMANAGER] dropped diplomacy proposal to self: " + fk);
                    continue;
                }
                Faction from = getFaction(fk.a);
                Faction to = getFaction(fk.b);
                if (from == null || to == null) {
                    continue;
                }
                //The gate this pipeline never had. Everything below acts on behalf of `from`, and
                //the only checks on that used to live in the client's GUI - so a crafted packet
                //could declare war, or break an alliance, for any faction in the game.
				if (onServer && !fk.serverAuthored && !mayNegotiate(from, fk.getInitiator(), fk.typeId)) {
                    denyDiplomacy(fk.getInitiator(), Lng.astr("Cannot change relations:\nFaction permission denied!"));
                    continue;
                }
                String badTerms = onServer && fk.action != DiplomacyProposal.Action.REVOKE ? termsProblem(from, fk) : null;
                if (badTerms != null) {
                    denyDiplomacy(fk.getInitiator(), Lng.astr("Cannot offer that settlement:\n%s", badTerms));
                    continue;
                }

                TreatyType type = fk.typeId != null ? TreatyTypeRegistry.get(fk.typeId) : null;
                if (fk.action == DiplomacyProposal.Action.QUERY) {
                    //deliberately after the permission gate above: if you may not negotiate it, you
                    //may not find out whether it would have worked either
                    if (onServer) {
                        answerQuery(fk, to);
                    }
                    continue;
                }
                if (fk.action == DiplomacyProposal.Action.REVOKE) {
                    revokeProposal(fk, from, to, type);
                } else {
                    sendProposalToTarget(fk, from, to, type);
                }
            }
        }
    }

    /**
     * Withdraws a proposal that is still pending, or walks away from one already in force.
     */
    private void revokeProposal(DiplomacyProposal fk, Faction from, Faction to, TreatyType type) {
        boolean inForce = type != null && hasTreaty(from.getIdFaction(), to.getIdFaction(), type.getId());
        if (inForce && !type.isBreakable()) {
            denyDiplomacy(fk.getInitiator(), Lng.astr("%s cannot be broken unilaterally!", type.getDisplayName()));
            return;
        }
		if (inForce) {
			breakTreatyInForce(from.getIdFaction(), to.getIdFaction(), type);
		} else if (fk.rel == FactionRelation.RType.FRIEND.code && getRelation(from.getIdFaction(), to.getIdFaction()) == FactionRelation.RType.FRIEND) {
            //an alliance held in the relation table rather than as a treaty
            System.err.println("[FACTIONMANAGER] revoked alliance " + fk + " on " + state());
            queueRelation(from.getIdFaction(), to.getIdFaction(), FactionRelation.RType.NEUTRAL.code);
            if (onServer) {
                diplomacyAction(DiplomacyAction.DiplActionType.ALLIANCE_CANCEL, to.getIdFaction(), from.getIdFaction());
            }
        } else {
            System.err.println("[FACTIONMANAGER] remove relationship offer " + fk + " on " + state());
            relationShipOffers.remove(fk.getCode());
        }

        if (onServer) {
            sendProposal(fk);
        }
    }

    /**
     * Stages a proposal for the other side to answer - unless its type does not need an answer, in
     * which case it is already done. War is the one that matters: you do not ask permission to
     * declare it.
     */
    private void sendProposalToTarget(DiplomacyProposal fk, Faction from, Faction to, TreatyType type) {
        if (onServer) {
            announceProposal(fk, from, to, type);
        }
        if (type != null && !type.needsAcceptance() && fk.against == 0) {
            if (onServer) {
                applyAgreedProposal(fk);
            }
            return;
        }
        relationShipOffers.put(fk.getCode(), fk);
        if (onServer) {
            sendProposal(fk);
            if (to.isNPC()) {
                answerAsNPC(fk, to);
            }
        }
    }

    /**
     * The news post, faction mail and opinion hit a proposal generates when it is first made.
     */
    private void announceProposal(DiplomacyProposal fk, Faction from, Faction to, TreatyType type) {
        String typeId = type != null ? type.getId() : null;

        if (TreatyTypeRegistry.WAR.equals(typeId)) {
            FactionNewsPost warDeclaration = new FactionNewsPost();
            warDeclaration.set(to.getIdFaction(), from.getName(), System.currentTimeMillis(), "Declaration of War", from.getName() + " declared war!\n" + fk.getMessage(), 0);
			warDeclaration.setCategory(FactionNewsCategory.WAR);

            String goals = SettlementTerm.describeAll(fk.terms, this::getFactionName, fk.a, fk.b, Lng.str("No war goals stated."));
            sendServerFactionMail(Lng.str("[FACTION] %s declared war!", from.getName()), Lng.str("%s declared war on your faction!\n%s\nWar goals:\n%s", from.getName(), fk.getMessage(), goals), to);
            sendServerFactionMail(Lng.str("[FACTION] Your faction declared war!"), Lng.str("Your faction %s\ndeclared war on %s!\n", from.getName(), to.getName()), from);

            diplomacyAction(DiplomacyAction.DiplActionType.DECLARATION_OF_WAR, to.getIdFaction(), from.getIdFaction());
            fm().getNpcFactionNews().war(from.getIdFaction(), to.getName());

            fm().addNewsPostServer(warDeclaration);
            return;
        }

        String title;
        String body;
        DiplomacyAction.DiplActionType action;
        if (TreatyTypeRegistry.ALLIANCE.equals(typeId)) {
            title = "Alliance Offer";
            body = from.getName() + " offered an Alliance!\n" + fk.getMessage();
            action = DiplomacyAction.DiplActionType.ALLIANCE_REQUEST;
        } else if (typeId == null) {
            title = "Peace Offer";
            body = from.getName() + " offered a peace treaty!\n" + fk.getMessage();
            action = DiplomacyAction.DiplActionType.PEACE_OFFER;
        } else {
            title = type.getDisplayName();
            body = from.getName() + " offered a " + type.getDisplayName() + "!\n" + fk.getMessage();
            //the opinion vocabulary still names only the three legacy relations, so a pact of any
            //other kind is announced but moves no points
            action = null;
        }

        FactionNewsPost post = new FactionNewsPost();
        post.set(to.getIdFaction(), from.getName(), System.currentTimeMillis(), title, body, 0);
		post.setCategory(FactionNewsCategory.DIPLOMACY);
        if (action != null) {
            diplomacyAction(action, to.getIdFaction(), from.getIdFaction());
        }
        diplomacyAction(DiplomacyAction.DiplActionType.TREATY_PROPOSED, to.getIdFaction(), from.getIdFaction(), typeId);
        fm().addNewsPostServer(post);
    }

    /**
     * Answers "would you accept this?" without putting anything on the table.
     *
     * <p>Only an NPC faction has an answer to give; a player faction decides for itself, and saying
     * so is more use than silence, which the asking panel cannot tell from a lost packet.</p>
     */
    private void answerQuery(DiplomacyProposal q, Faction to) {
        boolean accept = false;
        String reasons;
        TreatyType type = q.typeId != null ? TreatyTypeRegistry.get(q.typeId) : null;
        Faction from = getFaction(q.a);
        if (type != null && type.getImplies() == FactionRelation.RType.ENEMY && from != null) {
            //nobody is asked about a war; the question is whether the declarer's goals are justified
            List<String> unmet = new ObjectArrayList<String>();
            accept = diplomacyOf(from).getEntity(q.b).hasCasusBelli(q.terms, unmet);
            reasons = joinReasons(unmet);
        } else if (to.isNPC()) {
            List<String> unmet = new ObjectArrayList<String>();
            accept = judgeAsNPC(q, to, unmet).accept();
            reasons = joinReasons(unmet);
        } else {
            reasons = Lng.str("They decide for themselves.");
        }
        //Todo: the answer rides the same broadcast buffer every other proposal does, so every
        //client sees it and ignores the ones that are not theirs. Per-client delivery would need a
        //channel of its own; revisit if diplomacy traffic ever shows up in a bandwidth profile.
        sendProposal(DiplomacyProposal.queryResult(q, accept, reasons));
    }

    private void answerAsNPC(DiplomacyProposal offer, Faction to) {
        List<String> unmet = new ObjectArrayList<String>();
        NpcVerdict verdict = judgeAsNPC(offer, to, unmet);
        boolean accept = verdict.accept();
        Faction proposer = getFaction(offer.a);
        if (!accept && verdict.counter() != null && proposer != null && !proposer.isNPC()) {
            //haggling: a player gets the terms it would sign, back the other way, to accept or argue
            //with. Never to another NPC, or two of them would counter each other every tick.
            DiplomacyProposal counter = DiplomacyProposal.counterTo(offer, to.getName(), offer.typeId, offer.expiresAt,
                    Lng.str("%s will settle on these terms.", to.getName()), offer.name, verdict.counter());
            counter.serverAuthored = true;
            sendServerFactionMail(Lng.str("[FACTION] %s countered", to.getName()),
                    Lng.str("%s will not sign as offered, but has sent terms back.\n%s", to.getName(), joinReasons(unmet)), proposer);
            synchronized (toAddProposalResponses) {
                toAddProposalResponses.add(counter);
            }
            return;
        }
        if (!accept) {
            diplomacyAction(DiplomacyAction.DiplActionType.TREATY_REFUSED, offer.a, offer.b, offer.typeId);
			System.err.println("[SERVER][FACTION] " + to.getName() + " refuses " + (offer.typeId != null ? offer.typeId : "peace") + " from " + getFactionName(offer.a) + ": " + unmet);
            //the reasons used to go to stderr and nowhere else, which left the player with a
            //refusal and no idea what would have changed it
            Faction from = getFaction(offer.a);
            if (from != null) {
				sendServerFactionMail(Lng.str("[FACTION] %s refused", to.getName()), Lng.str("%s refused your offer.\n%s", to.getName(), joinReasons(unmet)), from);
			}
		}
		DiplomacyProposal answer = DiplomacyProposal.respondTo(offer, to.getName(), accept);
		answer.serverAuthored = true;
		synchronized (toAddProposalResponses) {
			toAddProposalResponses.add(answer);
		}
	}

	/**
	 * Takes a faction's emblem away and says who did it.
	 *
	 * <p>Moderation is part of the upload feature rather than a follow-up to it: an upload path with no
	 * way to remove what came up it is the thing that becomes somebody's weekend. The removal is
	 * announced to the faction and printed to the server log rather than applied quietly, so a faction
	 * that keeps re-uploading is visible in the tooling an admin already reads instead of being an
	 * invisible game of whack-a-mole.</p>
	 *
	 * <p>The stored image goes too, unless another faction is wearing the same one - the store is keyed
	 * by content, so two factions that picked the same picture share a file. Clients that already
	 * fetched it keep their copy on disk; nothing draws it once no faction points at it, and a fresh
	 * client can no longer fetch it at all.</p>
	 */
	public void clearEmblem(Faction f, String by) {
		String hash = emblemOf(f);
		if (hash == null || hash.isEmpty()) {
			return;
		}
		setEmblem(f, "", by);
		if (!emblems.containsValue(hash)) {
			FactionEmblem.serverFile(hash).delete();
		}
		System.err.println("[SERVER][FACTION][" + f.getName() + "]" + "[EMBLEM] removed by " + by + " (was " + hash + ")");
		sendServerFactionMail(Lng.str("[FACTION] Emblem removed"),
				Lng.str("An administrator removed your faction's emblem.\nRemoved by: %s", by), f);
	}

	public void sendServerFactionMail(String topic, String message, Faction to) {
		assert (state() instanceof GameServerState);
		GameServerState state = ((GameServerState) state());
		for (String name : to.getMembersUID().keySet()) {
			state.getServerPlayerMessager().send("[FACTION]", name, topic, message);
		}
	}

}
