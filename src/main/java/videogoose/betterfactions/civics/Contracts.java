package videogoose.betterfactions.civics;

import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.net.BFPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contracts: work posted with its reward held in escrow, and taken by a corporation whose line of business
 * fits. Only corporations and NPC factions post; only corporations take, so a player works a contract through
 * a corporation of their faction. Everything is decided here, on the server: clients only ask.
 *
 * <p>The concepts come from the old Contracts2 mod (difficulty-free for now, aggression bounties, escorts
 * later); none of its code does. See {@code docs/design/CORPORATIONS_PLAN.md}, "Contracts".</p>
 *
 * <p>Public: the board is the same for everyone, so every contract goes to every player.</p>
 */
public final class Contracts {

    private static final Contracts SERVER = new Contracts();
    private static final Contracts CLIENT = new Contracts();

    public static Contracts server() {
        return SERVER;
    }

    public static Contracts client() {
        return CLIENT;
    }

    static final long DAY_MS = 24L * 60L * 60L * 1000L;
    static final long HOUR_MS = 60L * 60L * 1000L;
    private static final long CHECK_MS = 30_000L;
    public static final int MAX_POSTED = 5;
    public static final long MIN_REWARD = 100_000L;
    public static final int MIN_DAYS = 1;
    public static final int MAX_DAYS = 7;
    /** Kills an NPC faction remembers, each fading after this long. */
    static final long AGGRESSION_MEMORY_MS = 6 * HOUR_MS;

    public enum Type {
        //appended only: saves keep the ordinal
        BOUNTY, ESCORT, HAUL, SUPPLY, MINE, BUILD, SALVAGE;

        public String title() {
            return switch (this) {
                case BOUNTY -> Lng.str("Bounty");
                case ESCORT -> Lng.str("Escort");
                case HAUL -> Lng.str("Haul");
                case SUPPLY -> Lng.str("Supply");
                case MINE -> Lng.str("Mining");
                case BUILD -> Lng.str("Build");
                case SALVAGE -> Lng.str("Salvage");
            };
        }

        /** The line of business that may take it. */
        public Corporations.Type takenBy() {
            return switch (this) {
                case BOUNTY, ESCORT -> Corporations.Type.MERCENARIES;
                case HAUL -> Corporations.Type.LOGISTICS;
                case SUPPLY -> Corporations.Type.CONSUMER_GOODS;
                case MINE -> Corporations.Type.RESOURCES;
                case BUILD -> Corporations.Type.SHIPYARDS;
                case SALVAGE -> Corporations.Type.SCRAPPERS;
            };
        }
    }

    public static final class Contract {
        public long id;
        public Type type = Type.BOUNTY;
        public int posterFaction;
        /** The posting corporation's id, or 0 for an NPC faction posting as itself. */
        public long posterCorp;
        public String posterName = "";
        /** A bounty's mark: a player's name. */
        public String target = "";
        public long reward;
        public long posted;
        public long deadline;
        public int takerFaction;
        public long takerCorp;
        public String takerName = "";
        /** Haul and Supply: what, how many, and how many have arrived. */
        public short item;
        public int count;
        public int delivered;
        /** Where the goods go: a station of the poster's. */
        public Corporations.Holding destination;
        /** Haul: where they are picked up, another station of the poster's, and how many have been. */
        public Corporations.Holding origin;
        public int pickedUp;
        /** Haul: held from the taker's treasury against the poster's goods; theirs again when the job is done. */
        public long collateral;

        public boolean goods() {
            return type == Type.HAUL || type == Type.SUPPLY;
        }

        public boolean taken() {
            return takerCorp != 0;
        }

        public boolean npc() {
            return posterCorp == 0;
        }

        Tag toTag() {
            return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.LONG, null, id),
                    new Tag(Tag.Type.BYTE, null, (byte) type.ordinal()),
                    new Tag(Tag.Type.INT, null, posterFaction),
                    new Tag(Tag.Type.LONG, null, posterCorp),
                    new Tag(Tag.Type.STRING, null, posterName),
                    new Tag(Tag.Type.STRING, null, target),
                    new Tag(Tag.Type.LONG, null, reward),
                    new Tag(Tag.Type.LONG, null, posted),
                    new Tag(Tag.Type.LONG, null, deadline),
                    new Tag(Tag.Type.INT, null, takerFaction),
                    new Tag(Tag.Type.LONG, null, takerCorp),
                    new Tag(Tag.Type.STRING, null, takerName),
                    new Tag(Tag.Type.SHORT, null, item),
                    new Tag(Tag.Type.INT, null, count),
                    new Tag(Tag.Type.INT, null, delivered),
                    destination != null ? destination.toTag() : new Tag(Tag.Type.STRUCT, null, new Tag[]{FinishTag.INST}),
                    origin != null ? origin.toTag() : new Tag(Tag.Type.STRUCT, null, new Tag[]{FinishTag.INST}),
                    new Tag(Tag.Type.INT, null, pickedUp),
                    new Tag(Tag.Type.LONG, null, collateral),
                    FinishTag.INST});
        }

        static Contract fromTag(Tag tag) {
            Tag[] t = tag.getStruct();
            Contract c = new Contract();
            c.id = t[0].getLong();
            int o = t[1].getByte();
            c.type = o >= 0 && o < Type.values().length ? Type.values()[o] : Type.BOUNTY;
            c.posterFaction = t[2].getInt();
            c.posterCorp = t[3].getLong();
            c.posterName = t[4].getString();
            c.target = t[5].getString();
            c.reward = t[6].getLong();
            c.posted = t[7].getLong();
            c.deadline = t[8].getLong();
            c.takerFaction = t[9].getInt();
            c.takerCorp = t[10].getLong();
            c.takerName = t[11].getString();
            //saved before goods contracts
            if (t.length > 18 && t[12].getType() == Tag.Type.SHORT) {
                c.item = t[12].getShort();
                c.count = t[13].getInt();
                c.delivered = t[14].getInt();
                c.destination = Corporations.Holding.fromTag(t[15]);
                c.origin = Corporations.Holding.fromTag(t[16]);
                c.pickedUp = t[17].getInt();
                c.collateral = t[18].getLong();
            }
            return c;
        }
    }

    private final Map<Long, Contract> contracts = new LinkedHashMap<>();
    private long nextId = 1;
    /** Server: each player's recent kills of each NPC faction's ships, "faction:player" to their times. Not saved. */
    private final Map<String, List<Long>> aggression = new HashMap<>();
    private int version;
    private long nextCheck;

    // ---- rules, kept pure for Contracts_test

    /** What an expired or cancelled contract hands back: all of it before anyone took it on, less the posting fee after. */
    static long refund(long reward, int feePercent) {
        return reward - reward * feePercent / 100;
    }

    /** An NPC faction's bounty after its {@code n}th: each one on the same pilot pays more. */
    static long npcBounty(long base, int n) {
        return base * Math.max(1, n);
    }

    /** Kills still remembered at {@code now}, the faded ones dropped. */
    static int remembered(List<Long> kills, long now) {
        kills.removeIf(at -> now - at > AGGRESSION_MEMORY_MS);
        return kills.size();
    }

    public static final int MAX_GOODS = 1_000_000;

    /** A Haul's collateral: what the goods are worth at the engine's list price. */
    static long collateral(long unitPrice, int count) {
        return Math.max(0, unitPrice) * Math.max(0, count);
    }

    /** Who gets a Haul's collateral when it ends unfinished: the poster if goods were picked up and not all delivered, else the taker. */
    static boolean collateralToPoster(Contract c) {
        return c.pickedUp > c.delivered;
    }

    /** Whether a corporation may take a contract: the right line of business, not its own faction's, not on itself. */
    static boolean mayTake(Contract c, Corporations.Type type, int faction, int targetFaction) {
        return !c.taken() && c.type.takenBy() == type && faction != c.posterFaction && faction != targetFaction;
    }

    // ---- shared

    public synchronized List<Contract> all() {
        return new ArrayList<>(contracts.values());
    }

    public synchronized int version() {
        return version;
    }

    // ---- client

    public synchronized void received(Tag tag) {
        contracts.clear();
        for (Tag e : tag.getStruct()) {
            if (e.getType() == Tag.Type.STRUCT) {
                Contract c = Contract.fromTag(e);
                contracts.put(c.id, c);
            }
        }
        version++;
    }

    /** Client: post a bounty from a corporation (its index in our faction), on a player. */
    public static void requestBounty(int corp, String target, long reward, int days) {
        send(0, corp, 0, target, reward, days);
    }

    /** Client: post a Haul or Supply job from a corporation of ours: {@code count} of {@code item} to {@code destination}, from {@code origin} for a Haul. */
    public static void requestGoods(int corp, Type type, short item, int count, long reward, int days, String destination, String origin) {
        BFPacket.toServer(BFPacket.Kind.CONTRACT_ACTION, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) 6), new Tag(Tag.Type.INT, null, corp), new Tag(Tag.Type.LONG, null, type.ordinal()),
                new Tag(Tag.Type.STRING, null, ""), new Tag(Tag.Type.LONG, null, reward), new Tag(Tag.Type.INT, null, days),
                new Tag(Tag.Type.SHORT, null, item), new Tag(Tag.Type.INT, null, count),
                new Tag(Tag.Type.STRING, null, destination), new Tag(Tag.Type.STRING, null, origin == null ? "" : origin), FinishTag.INST}));
    }

    /** Client: hand over goods for a contract our faction holds, at its destination (4), or load a Haul's at its origin (5). */
    public static void requestGoodsMove(int op, long id) {
        send(op, -1, id, "", 0, 0);
    }

    /** Client: a corporation of ours (by index) takes a contract. */
    public static void requestTake(int corp, long id) {
        send(1, corp, id, "", 0, 0);
    }

    /** Client: give a contract back to the board (2), or withdraw one we posted that nobody has taken (3). */
    public static void request(int op, int corp, long id) {
        send(op, corp, id, "", 0, 0);
    }

    private static void send(int op, int corp, long id, String text, long reward, int days) {
        BFPacket.toServer(BFPacket.Kind.CONTRACT_ACTION, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) op), new Tag(Tag.Type.INT, null, corp), new Tag(Tag.Type.LONG, null, id),
                new Tag(Tag.Type.STRING, null, text == null ? "" : text), new Tag(Tag.Type.LONG, null, reward),
                new Tag(Tag.Type.INT, null, days), FinishTag.INST}));
    }

    // ---- server: the clock

    /** Server: contracts past their deadline lapse, and the poster gets back what it is owed. */
    public void update(long now) {
        if (now < nextCheck) {
            return;
        }
        nextCheck = now + CHECK_MS;
        List<Contract> lapsed = new ArrayList<>();
        synchronized (this) {
            contracts.values().removeIf(c -> {
                if (now >= c.deadline) {
                    lapsed.add(c);
                    return true;
                }
                return false;
            });
            aggression.values().removeIf(k -> remembered(k, now) == 0);
            if (!lapsed.isEmpty()) {
                version++;
            }
        }
        for (Contract c : lapsed) {
            settleCollateral(c);
            //nobody earned it, so the poster has it back, less the fee for having posted it
            if (!c.npc()) {
                Corporations.server().credit(c.posterFaction, c.posterCorp, refund(c.reward, BFConfig.contractFeePercent()), false);
            }
            news(c.posterFaction, c.posterName, Lng.str("Contract lapsed: %s", describe(c)),
                    c.taken() ? Lng.str("%s did not finish it in time. The reward, less the posting fee, returns to %s.", c.takerName, c.posterName)
                            : Lng.str("Nobody took it in time. The reward, less the posting fee, returns to %s.", c.posterName));
            if (c.taken()) {
                news(c.takerFaction, c.takerName, Lng.str("Contract lapsed: %s", describe(c)), Lng.str("%s ran out of time on it.", c.takerName));
            }
        }
        if (!lapsed.isEmpty()) {
            broadcast();
        }
    }

    // ---- server: hooks

    /** Server: {@code killer} killed {@code victim}, of another faction. Pays any bounty on the victim a corporation of the killer's faction holds. */
    public void onPlayerKilled(PlayerState killer, PlayerState victim) {
        List<Contract> met = new ArrayList<>();
        synchronized (this) {
            contracts.values().removeIf(c -> {
                if (c.type == Type.BOUNTY && c.taken() && c.takerFaction == killer.getFactionId() && c.target.equalsIgnoreCase(victim.getName())) {
                    met.add(c);
                    return true;
                }
                return false;
            });
            if (!met.isEmpty()) {
                version++;
            }
        }
        for (Contract c : met) {
            Corporations.server().credit(c.takerFaction, c.takerCorp, c.reward, true);
            news(c.takerFaction, killer.getName(), Lng.str("Bounty collected: %s", c.target),
                    Lng.str("%s killed %s. %s collects %s credits from %s.", killer.getName(), c.target, c.takerName, c.reward, c.posterName));
            news(c.posterFaction, c.posterName, Lng.str("Bounty collected: %s", c.target),
                    Lng.str("%s of %s killed %s.", killer.getName(), c.takerName, c.target));
        }
        if (!met.isEmpty()) {
            broadcast();
        }
    }

    /**
     * Server: {@code killer} destroyed a ship of {@code faction}. An NPC faction (the Trading Guild among them)
     * remembers; enough kills in a while and it puts a bounty on the pilot.
     */
    public void onNpcShipDestroyed(int faction, PlayerState killer) {
        Faction f = GameServerState.instance.getFactionManager().getFaction(faction);
        if (f == null || !f.isNPC() || faction == FactionManager.PIRATES_ID || killer.getFactionId() == faction) {
            return;
        }
        long now = System.currentTimeMillis();
        Contract posted = null;
        synchronized (this) {
            List<Long> kills = aggression.computeIfAbsent(faction + ":" + killer.getName().toLowerCase(java.util.Locale.ROOT), k -> new ArrayList<>());
            kills.add(now);
            if (remembered(kills, now) < BFConfig.npcBountyKills()) {
                return;
            }
            kills.clear();
            int prior = (int) contracts.values().stream().filter(c -> c.npc() && c.posterFaction == faction && c.target.equalsIgnoreCase(killer.getName())).count();
            Contract c = new Contract();
            c.id = nextId++;
            c.type = Type.BOUNTY;
            c.posterFaction = faction;
            c.posterName = f.getName();
            c.target = killer.getName();
            //todo: NPC money lives in their homebase shop, unloaded most of the time; their bounties are minted.
            //Debit the homebase when it is loaded if NPC rewards turn out to flood the economy.
            c.reward = npcBounty(BFConfig.npcBountyReward(), prior + 1);
            c.posted = now;
            c.deadline = now + 3 * DAY_MS;
            contracts.put(c.id, c);
            version++;
            posted = c;
        }
        Contract c = posted;
        killer.sendServerMessage(new ServerMessage(new Object[]{Lng.str("%s has put a bounty of %s credits on you.", c.posterName, c.reward)},
                ServerMessage.MESSAGE_TYPE_WARNING, killer.getId()));
        news(killer.getFactionId(), c.posterName, Lng.str("A bounty on %s", c.target),
                Lng.str("%s will pay %s credits to the corporation whose people kill %s.", c.posterName, c.reward, c.target));
        broadcast();
    }

    /** Server: a corporation is gone. Its posted contracts go (their escrow with its treasury), its taken ones back to the board. */
    public void onCorporationGone(int faction, long corp) {
        boolean changed = false;
        synchronized (this) {
            for (Contract c : new ArrayList<>(contracts.values())) {
                if (c.posterFaction == faction && c.posterCorp == corp) {
                    contracts.remove(c.id);
                    changed = true;
                } else if (c.takerFaction == faction && c.takerCorp == corp) {
                    settleCollateral(c);
                    release(c);
                    changed = true;
                }
            }
            if (changed) {
                version++;
            }
        }
        if (changed) {
            broadcast();
        }
    }

    /** Server: the faction is gone, or dropped the institution: as {@link #onCorporationGone} for each of its corporations. */
    public void onFactionGone(int faction) {
        boolean changed = false;
        synchronized (this) {
            for (Contract c : new ArrayList<>(contracts.values())) {
                if (c.posterFaction == faction && !c.npc()) {
                    contracts.remove(c.id);
                    changed = true;
                } else if (c.takerFaction == faction) {
                    settleCollateral(c);
                    release(c);
                    changed = true;
                }
            }
            if (changed) {
                version++;
            }
        }
        if (changed) {
            broadcast();
        }
    }

    private static void release(Contract c) {
        c.takerFaction = 0;
        c.takerCorp = 0;
        c.takerName = "";
        c.collateral = 0;
        c.pickedUp = c.delivered;
    }

    /**
     * A Haul ending unfinished for its taker: its collateral goes to the poster if goods it picked up never
     * arrived, else back to the taker. Lost with the taker if the taker is gone.
     */
    private static void settleCollateral(Contract c) {
        if (c.collateral <= 0 || !c.taken()) {
            return;
        }
        if (collateralToPoster(c)) {
            Corporations.server().credit(c.posterFaction, c.posterCorp, c.collateral, false);
        } else {
            Corporations.server().credit(c.takerFaction, c.takerCorp, c.collateral, false);
        }
        c.collateral = 0;
    }

    // ---- server: what members do

    public void action(PlayerState p, Tag tag) {
        Tag[] t = tag.getStruct();
        if (t.length < 6 || t[0].getType() != Tag.Type.BYTE || t[1].getType() != Tag.Type.INT || t[2].getType() != Tag.Type.LONG) {
            return;
        }
        Faction f = GameServerState.instance.getFactionManager().getFaction(p.getFactionId());
        if (f == null || !Civics.server().holds(f.getIdFaction(), CivicRegistry.CORPORATIONS)) {
            reject(p, Lng.str("Contracts are posted and taken through corporations, and your faction has none."));
            return;
        }
        int op = t[0].getByte();
        if (op == 4 || op == 5) {
            moveGoods(p, f, op == 4, t[2].getLong());
            return;
        }
        Corporations.Corp corp = Corporations.server().runBy(p, f, t[1].getInt());
        if (corp == null) {
            reject(p, Lng.str("Only a corporation's CEO or your faction's officers may act for it."));
            return;
        }
        long id = t[2].getLong();
        int me = f.getIdFaction();
        long now = System.currentTimeMillis();
        switch (op) {
            case 0 -> postBounty(p, f, corp, t[3].getString(), t[4].getLong(), t[5].getInt(), now);
            case 6 -> {
                if (t.length < 10 || t[6].getType() != Tag.Type.SHORT) {
                    return;
                }
                postGoods(p, f, corp, (int) id == Type.HAUL.ordinal() ? Type.HAUL : Type.SUPPLY, t[6].getShort(), t[7].getInt(), t[4].getLong(),
                        t[8].getString(), t[9].getString(), t[5].getInt(), now);
            }
            case 1 -> {
                Contract c;
                synchronized (this) {
                    c = contracts.get(id);
                    PlayerState mark = c == null || c.target.isEmpty() ? null : GameServerState.instance.getPlayerStatesByName().get(c.target);
                    int targetFaction = mark != null ? mark.getFactionId() : 0;
                    if (c == null || !mayTake(c, corp.type, me, targetFaction)) {
                        reject(p, c == null ? Lng.str("That contract is gone.")
                                : Lng.str("Only a %s corporation of another faction may take a %s contract, and only while nobody has.", c.type.takenBy().title(), c.type.title()));
                        return;
                    }
                    if (c.type == Type.HAUL) {
                        long held = collateral(org.schema.game.common.data.element.ElementKeyMap.getInfo(c.item).getPrice(false), c.count - c.delivered);
                        if (!Corporations.server().debit(me, corp.id, held)) {
                            reject(p, Lng.str("%s must hold %s credits against the goods it hauls, and its treasury cannot cover it.", corp.name, held));
                            return;
                        }
                        c.collateral = held;
                    }
                    c.takerFaction = me;
                    c.takerCorp = corp.id;
                    c.takerName = corp.name;
                    version++;
                }
                news(me, p.getName(), Lng.str("%s takes a contract: %s", corp.name, describe(c)),
                        Lng.str("%s credits from %s, if it is done in %s.", c.reward, c.posterName, videogoose.betterfactions.gui.DiplomacyTreatyList.roughly(c.deadline - now)));
                news(c.posterFaction, c.posterName, Lng.str("Contract taken: %s", describe(c)), Lng.str("%s of %s has taken it on.", corp.name, f.getName()));
            }
            case 2 -> {
                Contract c;
                synchronized (this) {
                    c = contracts.get(id);
                    if (c == null || c.takerFaction != me || c.takerCorp != corp.id) {
                        return;
                    }
                    settleCollateral(c);
                    release(c);
                    version++;
                }
                news(me, p.getName(), Lng.str("%s gives up a contract: %s", corp.name, describe(c)), Lng.str("It is back on the board."));
            }
            case 3 -> {
                Contract c;
                synchronized (this) {
                    c = contracts.get(id);
                    if (c == null || c.posterFaction != me || c.posterCorp != corp.id) {
                        return;
                    }
                    if (c.taken()) {
                        reject(p, Lng.str("%s has taken it on; it can no longer be withdrawn.", c.takerName));
                        return;
                    }
                    contracts.remove(id);
                    version++;
                }
                Corporations.server().credit(me, corp.id, c.reward, false);
                news(me, p.getName(), Lng.str("%s withdraws a contract: %s", corp.name, describe(c)), Lng.str("The reward returns to its treasury."));
            }
            default -> {
                return;
            }
        }
        broadcast();
    }

    private void postBounty(PlayerState p, Faction f, Corporations.Corp corp, String target, long reward, int days, long now) {
        PlayerState mark = GameServerState.instance.getPlayerStatesByName().get(target);
        if (mark == null) {
            //the name as typed may differ in case
            for (PlayerState o : GameServerState.instance.getPlayerStatesByName().values()) {
                if (o.getName().equalsIgnoreCase(target)) {
                    mark = o;
                }
            }
        }
        if (mark == null || mark.getFactionId() == f.getIdFaction()) {
            reject(p, Lng.str("A bounty needs a player online, of another faction."));
            return;
        }
        if (reward < MIN_REWARD) {
            reject(p, Lng.str("A bounty pays at least %s credits.", MIN_REWARD));
            return;
        }
        Contract c = new Contract();
        synchronized (this) {
            long mine = contracts.values().stream().filter(o -> o.posterFaction == f.getIdFaction() && o.posterCorp == corp.id).count();
            if (mine >= MAX_POSTED) {
                reject(p, Lng.str("%s has %s contracts posted, all it may.", corp.name, MAX_POSTED));
                return;
            }
            //escrow first: no contract stands that is not paid for
            if (!Corporations.server().debit(f.getIdFaction(), corp.id, reward)) {
                reject(p, Lng.str("%s's treasury cannot cover %s credits.", corp.name, reward));
                return;
            }
            c.id = nextId++;
            c.type = Type.BOUNTY;
            c.posterFaction = f.getIdFaction();
            c.posterCorp = corp.id;
            c.posterName = corp.name;
            c.target = mark.getName();
            c.reward = reward;
            c.posted = now;
            c.deadline = now + Math.max(MIN_DAYS, Math.min(MAX_DAYS, days)) * DAY_MS;
            contracts.put(c.id, c);
            version++;
        }
        news(f.getIdFaction(), p.getName(), Lng.str("%s posts a bounty on %s", corp.name, c.target),
                Lng.str("%s credits, held from its treasury, for the corporation whose people kill %s.", c.reward, c.target));
        news(mark.getFactionId(), c.posterName, Lng.str("A bounty on %s", c.target), Lng.str("%s will pay %s credits for %s's death.", c.posterName, c.reward, c.target));
    }

    private void postGoods(PlayerState p, Faction f, Corporations.Corp corp, Type type, short item, int count, long reward,
                           String destinationUid, String originUid, int days, long now) {
        int me = f.getIdFaction();
        Corporations.Holding destination = Corporations.ownStation(me, destinationUid);
        Corporations.Holding origin = type == Type.HAUL ? Corporations.ownStation(me, originUid) : null;
        if (!org.schema.game.common.data.element.ElementKeyMap.isValidType(item) || count < 1 || count > MAX_GOODS) {
            reject(p, Lng.str("A job asks for between 1 and %s of a real block.", MAX_GOODS));
            return;
        }
        if (destination == null || type == Type.HAUL && (origin == null || origin.uid.equals(destination.uid))) {
            reject(p, type == Type.HAUL ? Lng.str("A haul runs between two different stations of ours.") : Lng.str("Supplies go to a station of ours."));
            return;
        }
        if (reward < MIN_REWARD) {
            reject(p, Lng.str("A job pays at least %s credits.", MIN_REWARD));
            return;
        }
        Contract c = new Contract();
        synchronized (this) {
            long mine = contracts.values().stream().filter(o -> o.posterFaction == me && o.posterCorp == corp.id).count();
            if (mine >= MAX_POSTED) {
                reject(p, Lng.str("%s has %s contracts posted, all it may.", corp.name, MAX_POSTED));
                return;
            }
            if (!Corporations.server().debit(me, corp.id, reward)) {
                reject(p, Lng.str("%s's treasury cannot cover %s credits.", corp.name, reward));
                return;
            }
            c.id = nextId++;
            c.type = type;
            c.posterFaction = me;
            c.posterCorp = corp.id;
            c.posterName = corp.name;
            c.item = item;
            c.count = count;
            c.destination = destination;
            c.origin = origin;
            c.reward = reward;
            c.posted = now;
            c.deadline = now + Math.max(MIN_DAYS, Math.min(MAX_DAYS, days)) * DAY_MS;
            contracts.put(c.id, c);
            version++;
        }
        news(me, p.getName(), Lng.str("%s posts a job: %s", corp.name, describe(c)),
                Lng.str("%s credits, held from its treasury, for the corporation that does it.", c.reward));
    }

    /**
     * A member of the taker's faction, at the station, hands over goods ({@code deliver}) from their own
     * inventory and the ship they are in, or loads a Haul's goods into them. Partial moves count.
     */
    private void moveGoods(PlayerState p, Faction f, boolean deliver, long id) {
        Contract c;
        synchronized (this) {
            c = contracts.get(id);
        }
        if (c == null || !c.goods() || c.takerFaction != f.getIdFaction()) {
            reject(p, Lng.str("Your faction has not taken that job on."));
            return;
        }
        if (!deliver && c.type != Type.HAUL) {
            return;
        }
        Corporations.Holding at = deliver ? c.destination : c.origin;
        org.schema.game.common.controller.SegmentController station = GameServerState.instance.getSegmentControllersByName()
                .get(org.schema.game.common.data.world.SimpleTransformableSendableObject.EntityType.SPACE_STATION.dbPrefix + at.uid);
        if (station == null || !p.getCurrentSector().equals(at.sector) || !(station instanceof org.schema.game.common.data.ManagedSegmentController<?> m)) {
            reject(p, Lng.str("Be at %s, in sector %s, to %s.", at.name, at.sector, deliver ? Lng.str("deliver") : Lng.str("load the goods")));
            return;
        }
        List<org.schema.game.common.data.player.inventory.Inventory> stationInv = new ArrayList<>(m.getManagerContainer().getInventories().inventoriesList);
        List<org.schema.game.common.data.player.inventory.Inventory> carried = new ArrayList<>();
        carried.add(p.getInventory());
        if (p.getFirstControlledTransformableWOExc() instanceof org.schema.game.common.data.ManagedSegmentController<?> ship && ship != station) {
            carried.addAll(ship.getManagerContainer().getInventories().inventoriesList);
        }
        int moved;
        synchronized (this) {
            if (contracts.get(id) != c) {
                return;
            }
            int want = deliver ? c.count - c.delivered : c.count - c.pickedUp;
            moved = move(c.item, want, deliver ? carried : stationInv, deliver ? stationInv : carried);
            if (deliver) {
                c.delivered += moved;
            } else {
                c.pickedUp += moved;
            }
            if (moved > 0) {
                version++;
            }
        }
        String what = org.schema.game.common.data.element.ElementKeyMap.getInfo(c.item).getName();
        if (moved == 0) {
            reject(p, deliver ? Lng.str("You carry no %s, or %s has no room for it.", what, at.name)
                    : Lng.str("%s has no %s left, or you have no room for it.", at.name, what));
            return;
        }
        p.sendServerMessage(new ServerMessage(new Object[]{deliver ? Lng.str("Delivered %s %s to %s (%s of %s).", moved, what, at.name, c.delivered, c.count)
                : Lng.str("Loaded %s %s at %s (%s of %s).", moved, what, at.name, c.pickedUp, c.count)}, ServerMessage.MESSAGE_TYPE_INFO, p.getId()));
        if (deliver && c.delivered >= c.count) {
            synchronized (this) {
                if (contracts.remove(id) == null) {
                    return;
                }
                version++;
            }
            Corporations.server().credit(c.takerFaction, c.takerCorp, c.reward, true);
            if (c.collateral > 0) {
                Corporations.server().credit(c.takerFaction, c.takerCorp, c.collateral, false);
            }
            news(c.takerFaction, p.getName(), Lng.str("Job done: %s", describe(c)), Lng.str("%s collects %s credits from %s.", c.takerName, c.reward, c.posterName));
            news(c.posterFaction, c.posterName, Lng.str("Job done: %s", describe(c)), Lng.str("%s delivered everything to %s.", c.takerName, at.name));
        }
        broadcast();
    }

    /** Moves up to {@code want} of {@code item} out of {@code from} into {@code to}, as much as there is and as fits. @return how many */
    private static int move(short item, int want, List<org.schema.game.common.data.player.inventory.Inventory> from,
                            List<org.schema.game.common.data.player.inventory.Inventory> to) {
        int have = 0;
        for (var inv : from) {
            have += inv.getOverallQuantity(item);
        }
        int left = Math.min(want, have);
        int moved = 0;
        for (var inv : to) {
            int n = left - moved;
            //the most this one takes, halving down from what is left
            while (n > 0 && !inv.canPutIn(item, n)) {
                n /= 2;
            }
            if (n <= 0) {
                continue;
            }
            int slot = inv.incExistingOrNextFreeSlot(item, n);
            inv.sendInventoryModification(slot);
            moved += n;
            if (moved >= left) {
                break;
            }
        }
        int take = moved;
        for (var inv : from) {
            int n = Math.min(take, inv.getOverallQuantity(item));
            if (n > 0) {
                it.unimi.dsi.fastutil.ints.IntOpenHashSet changed = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
                inv.decreaseBatch(item, n, changed);
                inv.sendInventoryModification(changed);
                take -= n;
            }
            if (take <= 0) {
                break;
            }
        }
        return moved;
    }

    public static String describe(Contract c) {
        String what = c.goods() && org.schema.game.common.data.element.ElementKeyMap.isValidType(c.item)
                ? org.schema.game.common.data.element.ElementKeyMap.getInfo(c.item).getName() : "";
        return switch (c.type) {
            case BOUNTY -> Lng.str("kill %s", c.target);
            case HAUL -> Lng.str("haul %s %s from %s to %s (%s delivered)", c.count, what, c.origin != null ? c.origin.name : "?",
                    c.destination != null ? c.destination.name : "?", c.delivered);
            case SUPPLY -> Lng.str("supply %s %s to %s (%s delivered)", c.count, what, c.destination != null ? c.destination.name : "?", c.delivered);
            default -> c.type.title();
        };
    }

    private static void news(int faction, String author, String title, String body) {
        Faction f = faction > 0 ? GameServerState.instance.getFactionManager().getFaction(faction) : null;
        if (f != null) {
            Civics.governanceNews(f, author, title, body);
        }
    }

    private static void reject(PlayerState p, String why) {
        p.sendServerMessage(new ServerMessage(new Object[]{why}, ServerMessage.MESSAGE_TYPE_ERROR, p.getId()));
    }

    // ---- sync: the whole board to everyone

    public void broadcast() {
        Tag tag = boardTag();
        for (PlayerState p : GameServerState.instance.getPlayerStatesByName().values()) {
            if (p.getClientChannel() != null) {
                BFPacket.toPlayer(p, BFPacket.Kind.CONTRACTS, tag);
            }
        }
    }

    public void sendTo(PlayerState p) {
        BFPacket.toPlayer(p, BFPacket.Kind.CONTRACTS, boardTag());
    }

    private synchronized Tag boardTag() {
        Tag[] t = new Tag[contracts.size() + 1];
        int i = 0;
        for (Contract c : contracts.values()) {
            t[i++] = c.toTag();
        }
        t[i] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    // ---- saved

    public synchronized Tag toTag() {
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.LONG, null, nextId), boardTag(), FinishTag.INST});
    }

    public synchronized void fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        nextId = t[0].getLong();
        received(t[1]);
    }

    public synchronized void clear() {
        contracts.clear();
        aggression.clear();
        nextId = 1;
        version++;
    }
}
