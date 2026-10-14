package videogoose.betterfactions.civics;

import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.Perms;
import videogoose.betterfactions.diplomacy.FactionTreaty;
import videogoose.betterfactions.diplomacy.TreatyType;
import videogoose.betterfactions.diplomacy.TreatyTypeRegistry;
import videogoose.betterfactions.net.BFPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntFunction;

/**
 * The Council institution: a ring of councilors, each with an agenda of their own, set against real
 * factions and paid in faction points. AI councilors keep a favour that agendas met raise and agendas
 * failed or refused lower; at none they resign and someone new takes the seat. With a Senate, the faction
 * can vote members onto the Council, to a new seat or in an AI councilor's place; a member's seat chooses
 * its own agenda.
 *
 * <p>Progress counts on its own, from treaties signed and broken and from ships lost in battle. Who the
 * Council is, and how it speaks, comes from the faction's civics ({@link CouncilLore}).</p>
 *
 * <p>Private: each faction's Council goes to its own members only.</p>
 */
public final class Council {

    private static final Council SERVER = new Council();
    private static final Council CLIENT = new Council();

    public static Council server() {
        return SERVER;
    }

    public static Council client() {
        return CLIENT;
    }

    public static final int MAX_SEATS = 7;
    static final int START_FAVOUR = 50;
    static final int MAX_FAVOUR = 100;
    static final int FAVOUR_MET = 15;
    static final int FAVOUR_FAILED = 15;
    static final int FAVOUR_REFUSED = 20;
    static final int FAVOUR_CLASHED = 10;
    static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long CHECK_MS = 30_000L;
    private static final String ACT = "council";

    public enum Objective {
        //appended only: saves keep the ordinal
        DESTROY_SHIPS, DECLARE_WAR, SIGN_TREATY, BREAK_TREATY, KEEP_PEACE, STAY_SOLVENT, SANCTION, CLAIM_SYSTEMS;

        /** Met by not breaking it until the deadline, rather than by counting up. */
        public boolean timed() {
            return this == KEEP_PEACE || this == STAY_SOLVENT;
        }
    }

    /**
     * What each role's seat asks for: war, peace, intrigue, calm, treasury, glory, diplomacy, expansion,
     * reform, zeal. A Council starts with the first three; the rest are added.
     */
    static final Objective[][] POOLS = {
            {Objective.DESTROY_SHIPS, Objective.SANCTION, Objective.DECLARE_WAR},
            {Objective.SIGN_TREATY},
            {Objective.BREAK_TREATY, Objective.SANCTION, Objective.DECLARE_WAR},
            {Objective.KEEP_PEACE},
            {Objective.STAY_SOLVENT},
            {Objective.DESTROY_SHIPS},
            {Objective.SIGN_TREATY, Objective.KEEP_PEACE},
            {Objective.CLAIM_SYSTEMS},
            {Objective.KEEP_PEACE, Objective.STAY_SOLVENT},
            {Objective.SANCTION, Objective.DESTROY_SHIPS},
    };

    /** How many roles there are. */
    public static final int ROLES = POOLS.length;

    /** Treaties an agenda may ask to be broken: ones a signatory can walk out of. */
    private static final String[] BREAKABLE = {TreatyTypeRegistry.NON_AGGRESSION, TreatyTypeRegistry.TRADE_AGREEMENT,
            TreatyTypeRegistry.DEFENSIVE_PACT, TreatyTypeRegistry.INTEL_SHARING, TreatyTypeRegistry.ALLIANCE};

    public static final class Agenda {
        public Objective objective = Objective.KEEP_PEACE;
        public int target;
        public String typeId = "";
        public int count = 1;
        public int done;
        public long issued;
        /** For an option a member has not chosen yet, how long it runs once chosen. */
        public long deadline;
        public int reward;
        public int penalty;
        public String backedBy = "";

        boolean hostile() {
            return objective == Objective.DESTROY_SHIPS || objective == Objective.DECLARE_WAR || objective == Objective.BREAK_TREATY
                    || objective == Objective.SANCTION;
        }

        boolean breaksPeace() {
            return objective == Objective.DECLARE_WAR || objective == Objective.BREAK_TREATY;
        }

        public int refuseCost() {
            return penalty * 2 / 5;
        }

        /** How far along: ships destroyed, or for a timed one whole days held. */
        public int progress(long now) {
            return objective.timed() ? (int) Math.min(count, Math.max(0, (now - issued) / DAY_MS)) : Math.min(done, count);
        }

        Tag toTag() {
            return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.BYTE, null, (byte) objective.ordinal()),
                    new Tag(Tag.Type.INT, null, target),
                    new Tag(Tag.Type.STRING, null, typeId),
                    new Tag(Tag.Type.INT, null, count),
                    new Tag(Tag.Type.INT, null, done),
                    new Tag(Tag.Type.LONG, null, issued),
                    new Tag(Tag.Type.LONG, null, deadline),
                    new Tag(Tag.Type.INT, null, reward),
                    new Tag(Tag.Type.INT, null, penalty),
                    new Tag(Tag.Type.STRING, null, backedBy),
                    FinishTag.INST});
        }

        static Agenda fromTag(Tag tag) {
            Tag[] t = tag.getStruct();
            if (t.length < 10 || t[0].getType() != Tag.Type.BYTE) {
                return null;
            }
            Agenda a = new Agenda();
            int o = t[0].getByte();
            a.objective = o >= 0 && o < Objective.values().length ? Objective.values()[o] : Objective.KEEP_PEACE;
            a.target = t[1].getInt();
            a.typeId = t[2].getString();
            a.count = t[3].getInt();
            a.done = t[4].getInt();
            a.issued = t[5].getLong();
            a.deadline = t[6].getLong();
            a.reward = t[7].getInt();
            a.penalty = t[8].getInt();
            a.backedBy = t[9].getString();
            return a;
        }
    }

    public static final class Seat {
        /** Which ring position's pool it asks from, and so its title. */
        public int archetype;
        public String name = "";
        /** The member holding it, or blank for an AI councilor. */
        public String member = "";
        public int favour = START_FAVOUR;
        public Agenda agenda;
        /** When an empty seat next asks for something. */
        public long nextAt;
        /** A member's seat: agendas to choose from. */
        public final List<Agenda> options = new ArrayList<>();

        public boolean held() {
            return !member.isEmpty();
        }

        Tag toTag() {
            Tag[] o = new Tag[options.size() + 1];
            for (int i = 0; i < options.size(); i++) {
                o[i] = options.get(i).toTag();
            }
            o[options.size()] = FinishTag.INST;
            return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.INT, null, archetype),
                    new Tag(Tag.Type.STRING, null, name),
                    new Tag(Tag.Type.STRING, null, member),
                    new Tag(Tag.Type.INT, null, favour),
                    agenda != null ? agenda.toTag() : new Tag(Tag.Type.STRUCT, null, new Tag[]{FinishTag.INST}),
                    new Tag(Tag.Type.LONG, null, nextAt),
                    new Tag(Tag.Type.STRUCT, null, o),
                    FinishTag.INST});
        }

        static Seat fromTag(Tag tag) {
            Tag[] t = tag.getStruct();
            Seat s = new Seat();
            s.archetype = t[0].getInt();
            s.name = t[1].getString();
            s.member = t[2].getString();
            s.favour = t[3].getInt();
            s.agenda = Agenda.fromTag(t[4]);
            s.nextAt = t[5].getLong();
            for (Tag e : t[6].getStruct()) {
                if (e.getType() == Tag.Type.STRUCT) {
                    Agenda a = Agenda.fromTag(e);
                    if (a != null) {
                        s.options.add(a);
                    }
                }
            }
            return s;
        }
    }

    public static final class State {
        public final List<Seat> seats = new ArrayList<>();
        public int fulfilled;
        /** What the faction calls it, or blank for its civics' name ({@link CouncilLore#title}). */
        public String name = "";
        /** Candidates who would take an AI councilor's place; a new pool each day. */
        public final List<Seat> pool = new ArrayList<>();
        /** The day ({@code now / DAY_MS}) the pool was drawn. */
        public long poolDay = -1;
        /** When the faction last had no active member, or 0: while set, the Council stands still. */
        public long suspendedAt;
        /** The style the faction chose ({@link CouncilLore.Flavour} name), or blank for its first shaping civic's. */
        public String style = "";

        public boolean suspended() {
            return suspendedAt > 0;
        }

        /**
         * Picks up where it stopped: every deadline and wait moves on by the time away, so nothing comes
         * due the moment the faction returns.
         */
        void resume(long now) {
            long away = Math.max(0, now - suspendedAt);
            for (Seat s : seats) {
                s.nextAt += away;
                if (s.agenda != null) {
                    s.agenda.issued += away;
                    s.agenda.deadline += away;
                }
            }
            suspendedAt = 0;
        }

        public String title(CouncilLore.Flavour flavour) {
            return name.isEmpty() ? CouncilLore.title(flavour) : name;
        }

        Tag toTag() {
            Tag[] s = new Tag[seats.size() + 1];
            for (int i = 0; i < seats.size(); i++) {
                s[i] = seats.get(i).toTag();
            }
            s[seats.size()] = FinishTag.INST;
            Tag[] c = new Tag[pool.size() + 1];
            for (int i = 0; i < pool.size(); i++) {
                c[i] = pool.get(i).toTag();
            }
            c[pool.size()] = FinishTag.INST;
            return new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.INT, null, fulfilled), new Tag(Tag.Type.STRUCT, null, s),
                    new Tag(Tag.Type.STRING, null, name), new Tag(Tag.Type.STRUCT, null, c), new Tag(Tag.Type.LONG, null, poolDay),
                    new Tag(Tag.Type.LONG, null, suspendedAt), new Tag(Tag.Type.STRING, null, style), FinishTag.INST});
        }

        static State fromTag(Tag tag) {
            Tag[] t = tag.getStruct();
            State st = new State();
            st.fulfilled = t[0].getInt();
            for (Tag e : t[1].getStruct()) {
                if (e.getType() == Tag.Type.STRUCT) {
                    st.seats.add(Seat.fromTag(e));
                }
            }
            st.name = t.length > 2 && t[2].getType() == Tag.Type.STRING ? t[2].getString() : "";
            if (t.length > 4 && t[3].getType() == Tag.Type.STRUCT && t[4].getType() == Tag.Type.LONG) {
                for (Tag e : t[3].getStruct()) {
                    if (e.getType() == Tag.Type.STRUCT) {
                        st.pool.add(Seat.fromTag(e));
                    }
                }
                st.poolDay = t[4].getLong();
                st.suspendedAt = t.length > 5 && t[5].getType() == Tag.Type.LONG ? t[5].getLong() : 0;
                st.style = t.length > 6 && t[6].getType() == Tag.Type.STRING ? t[6].getString() : "";
            }
            return st;
        }

        /** The seat a member holds, or null. */
        public Seat heldBy(String player) {
            for (Seat s : seats) {
                if (s.held() && s.member.equalsIgnoreCase(player)) {
                    return s;
                }
            }
            return null;
        }
    }

    private enum Settle {MET, FAILED, REFUSED, CLASHED}

    private final Map<Integer, State> byFaction = new HashMap<>();
    private int version;
    private long nextCheck;

    // ---- rules, kept pure for Council_test

    /** Seats every Council starts with (war, peace, intrigue); more are added by the faction, each at upkeep. */
    public static final int BASE_SEATS = 3;

    /** Roles not yet on the ring, which a new seat can take. */
    public static List<Integer> openRoles(State st) {
        List<Integer> out = new ArrayList<>();
        for (int r = 0; r < ROLES; r++) {
            int role = r;
            if (st.seats.stream().noneMatch(s -> s.archetype == role)) {
                out.add(r);
            }
        }
        return out;
    }

    /** Whether meeting one agenda breaks the other: war on a faction the other wants peace with, or any act of war against keeping the peace. */
    public static boolean clash(Agenda a, Agenda b) {
        if (a == null || b == null) {
            return false;
        }
        return a.hostile() && b.objective == Objective.SIGN_TREATY && a.target == b.target
                || b.hostile() && a.objective == Objective.SIGN_TREATY && a.target == b.target
                || a.objective == Objective.KEEP_PEACE && b.breaksPeace()
                || b.objective == Objective.KEEP_PEACE && a.breaksPeace();
    }

    /** The other seat whose agenda clashes with this one's, or null. */
    public static Seat clashWith(State st, Seat seat) {
        for (Seat o : st.seats) {
            if (o != seat && clash(seat.agenda, o.agenda)) {
                return o;
            }
        }
        return null;
    }

    /** What an agenda asks, in a sentence. */
    public static String describe(Agenda a, IntFunction<String> factionName) {
        String who = factionName.apply(a.target);
        TreatyType type = TreatyTypeRegistry.get(a.typeId);
        String treaty = type != null ? type.getDisplayName() : Lng.str("treaty");
        return switch (a.objective) {
            case DESTROY_SHIPS -> Lng.str("Destroy %s ships of %s.", a.count, who);
            case DECLARE_WAR -> Lng.str("Declare war on %s.", who);
            case SIGN_TREATY -> Lng.str("Sign a %s with %s.", treaty, who);
            case BREAK_TREATY -> Lng.str("Break the %s with %s.", treaty, who);
            case SANCTION -> Lng.str("Impose an %s on %s.", treaty, who);
            case KEEP_PEACE -> Lng.str("Declare no war and break no treaty for %s days.", a.count);
            case STAY_SOLVENT -> Lng.str("Stay out of debt for %s days.", a.count);
            case CLAIM_SYSTEMS -> Lng.str("Claim %s new systems.", a.count);
        };
    }

    /** The same, short enough for a ring node. */
    public static String brief(Agenda a, IntFunction<String> factionName) {
        String who = factionName.apply(a.target);
        return switch (a.objective) {
            case DESTROY_SHIPS -> Lng.str("Destroy %s %s ships", a.count, who);
            case DECLARE_WAR -> Lng.str("War on %s", who);
            case SIGN_TREATY -> Lng.str("A treaty with %s", who);
            case BREAK_TREATY -> Lng.str("Break with %s", who);
            case SANCTION -> Lng.str("Sanction %s", who);
            case KEEP_PEACE -> Lng.str("Keep the peace %s days", a.count);
            case STAY_SOLVENT -> Lng.str("No debt for %s days", a.count);
            case CLAIM_SYSTEMS -> Lng.str("Claim %s systems", a.count);
        };
    }

    // ---- perks: a seat's, while its councilor is devoted or a member holds it

    public static final int WAR = 0, PEACE = 1, INTRIGUE = 2, CALM = 3, TREASURY = 4, GLORY = 5, DIPLOMACY = 6,
            EXPANSION = 7, REFORM = 8, ZEAL = 9;
    static final int DEVOTED = 75;
    /** Each archetype's perk, in percent: off a cost or an upkeep, or onto station income for {@link #CALM}. */
    private static final int[] PERK_PERCENT = {25, 25, 50, 10, 15, 10, 25, 25, 25, 50};

    public static boolean devoted(Seat s) {
        return s.held() || s.favour >= DEVOTED;
    }

    public static String perk(int archetype) {
        int p = PERK_PERCENT[Math.floorMod(archetype, PERK_PERCENT.length)];
        return switch (Math.floorMod(archetype, PERK_PERCENT.length)) {
            case WAR -> Lng.str("declaring war costs %s%% fewer faction points.", p);
            case PEACE -> Lng.str("proposing a treaty costs %s%% fewer faction points.", p);
            case INTRIGUE -> Lng.str("breaking a treaty costs %s%% fewer faction points.", p);
            case CALM -> Lng.str("our stations earn %s%% more faction points.", p);
            case TREASURY -> Lng.str("institution upkeep is %s%% lower.", p);
            case GLORY -> Lng.str("fleet upkeep is %s%% lower.", p);
            case DIPLOMACY -> Lng.str("gifts to other factions cost %s%% fewer faction points.", p);
            case EXPANSION -> Lng.str("fabricating a claim costs %s%% fewer faction points.", p);
            case REFORM -> Lng.str("changing a civic or the fleet doctrine costs %s%% fewer faction points.", p);
            default -> Lng.str("the extra cost of a war without just cause is %s%% lower.", p);
        };
    }

    /** Whether any seat of this archetype gives the faction its perk now. */
    public synchronized boolean perk(int faction, int archetype) {
        State st = byFaction.get(faction);
        if (st != null) {
            for (Seat s : st.seats) {
                if (s.archetype == archetype && devoted(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Client: a cost the own faction pays, after its perk. */
    public static int ownCost(int archetype, int base) {
        org.schema.game.client.data.GameClientState s = org.schema.game.client.data.GameClientState.instance;
        return s == null || s.getPlayer() == null ? base : CLIENT.cost(s.getPlayer().getFactionId(), archetype, base);
    }

    /** A cost after the archetype's discount, if the faction has its perk. */
    public int cost(int faction, int archetype, int base) {
        return perk(faction, archetype) ? base * (100 - PERK_PERCENT[archetype]) / 100 : base;
    }

    /** An upkeep or income after the archetype's perk, if the faction has it. */
    public float scale(int faction, int archetype, float v) {
        if (!perk(faction, archetype)) {
            return v;
        }
        int p = PERK_PERCENT[archetype];
        return archetype == CALM ? v * (100 + p) / 100f : v * (100 - p) / 100f;
    }

    /** Seats added past the first three; each costs one step of institution upkeep. */
    public synchronized int extraSeats(int faction) {
        State st = byFaction.get(faction);
        return st == null ? 0 : Math.max(0, st.seats.size() - BASE_SEATS);
    }

    /** What the faction pays for its institutions an income turn: the ladder, extra Council seats, the Treasury perk. */
    public float institutionUpkeep(int faction, FactionCivics c, int base) {
        float up = FactionCivics.institutionUpkeep(c.institutions.size(), base) + (float) extraSeats(faction) * base;
        return scale(faction, TREASURY, up);
    }

    // ---- client

    public synchronized State of(int faction) {
        return byFaction.get(faction);
    }

    public synchronized int version() {
        return version;
    }

    /** Client: the own faction's Council, replacing what was held. */
    public synchronized void received(Tag tag) {
        Tag[] t = tag.getStruct();
        byFaction.clear();
        if (t.length > 1 && t[1].getType() == Tag.Type.STRUCT && t[1].getStruct().length > 1) {
            byFaction.put(t[0].getInt(), State.fromTag(t[1]));
        }
        version++;
    }

    /** Client: back (0) or refuse (1) a seat's agenda, or choose a member seat's agenda (2; option -1 for none). */
    public static void requestAction(int op, int seat, int option) {
        BFPacket.toServer(BFPacket.Kind.COUNCIL_ACTION, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) op), new Tag(Tag.Type.INT, null, seat), new Tag(Tag.Type.INT, null, option), FinishTag.INST}));
    }

    /** Client: rename the Council (blank for its civics' name), or the AI councilor in {@code seat}. */
    public static void requestRename(int seat, String name) {
        BFPacket.toServer(BFPacket.Kind.COUNCIL_ACTION, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) (seat < 0 ? 3 : 4)), new Tag(Tag.Type.INT, null, seat), new Tag(Tag.Type.INT, null, 0),
                new Tag(Tag.Type.STRING, null, name == null ? "" : name), FinishTag.INST}));
    }

    /** Client: add a seat in a role not yet on the ring, taken by a new NPC councilor. */
    public static void requestAddSeat(int role) {
        requestAction(7, -1, role);
    }

    /** Client: give the Council a style its civics allow ({@link CouncilLore.Flavour} name, blank for the default). */
    public static void requestStyle(String style) {
        BFPacket.toServer(BFPacket.Kind.COUNCIL_ACTION, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) 6), new Tag(Tag.Type.INT, null, -1), new Tag(Tag.Type.INT, null, 0),
                new Tag(Tag.Type.STRING, null, style), FinishTag.INST}));
    }

    /** Client: put the pool's candidate in place of the AI councilor in {@code seat}. */
    public static void requestReplace(int seat, int candidate) {
        requestAction(5, seat, candidate);
    }

    /** Client: put to the Senate appointing a member to a new seat (0) or in a seat's place (1), or removing one (2). */
    public static void requestPropose(int op, int seat, String player, String reason) {
        BFPacket.toServer(BFPacket.Kind.COUNCIL_PROPOSE, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) op), new Tag(Tag.Type.INT, null, seat),
                new Tag(Tag.Type.STRING, null, player == null ? "" : player), new Tag(Tag.Type.STRING, null, reason == null ? "" : reason), FinishTag.INST}));
    }

    // ---- server: the clock

    /** Server: seats filled, agendas issued and settled at their deadlines. Throttled; call every tick. */
    public void update(long now) {
        if (now < nextCheck) {
            return;
        }
        nextCheck = now + CHECK_MS;
        List<Runnable> after = new ArrayList<>();
        List<Faction> dirty = new ArrayList<>();
        for (Faction f : GameServerState.instance.getFactionManager().getFactionCollection()) {
            if (f.isNPC() || !Civics.server().holds(f.getIdFaction(), CivicRegistry.COUNCIL)) {
                continue;
            }
            int earned = Civics.server().of(f.getIdFaction()).slotsEarned;
            boolean active = hasActiveMember(f);
            synchronized (this) {
                State st = byFaction.computeIfAbsent(f.getIdFaction(), k -> new State());
                //a faction nobody plays is not asked anything, and owes nothing for the time away
                if (!active) {
                    if (!st.suspended()) {
                        st.suspendedAt = now;
                        version++;
                        dirty.add(f);
                    }
                    continue;
                }
                boolean resumed = st.suspended();
                if (resumed) {
                    st.resume(now);
                }
                if (tick(f, st, earned, now, after) | resumed) {
                    version++;
                    dirty.add(f);
                }
            }
        }
        after.forEach(Runnable::run);
        dirty.forEach(this::send);
    }

    /** @return whether anything changed */
    private boolean tick(Faction f, State st, int earned, long now, List<Runnable> after) {
        boolean changed = false;
        if (!CouncilLore.allowed(Civics.server().of(f.getIdFaction()), st.style)) {
            //a saved style a later version no longer knows falls back too
            String gone = java.util.Arrays.stream(CouncilLore.Flavour.values()).filter(v -> v.name().equals(st.style))
                    .findFirst().map(CouncilLore::title).orElse(CouncilLore.title(CouncilLore.Flavour.COUNCIL));
            st.style = "";
            String now2 = st.title(CouncilLore.of(Civics.server().of(f.getIdFaction()), ""));
            after.add(() -> Civics.governanceNews(f, now2, Lng.str("%s is no more", gone),
                    Lng.str("Without the civic that shaped it, the Council takes the style of %s. Its councilors keep their seats.", now2)));
            changed = true;
        }
        if (st.poolDay != now / DAY_MS) {
            st.poolDay = now / DAY_MS;
            st.pool.clear();
            st.pool.addAll(drawPool(now, flavour(f)));
            changed = true;
        }
        while (st.seats.size() < BASE_SEATS) {
            st.seats.add(newSeat(st.seats.size(), now, flavour(f)));
            changed = true;
        }
        for (Seat seat : new ArrayList<>(st.seats)) {
            if (seat.held() && !activeMember(f, seat.member)) {
                String gone = seat.member;
                toAi(seat, now, flavour(f));
                after.add(() -> Civics.governanceNews(f, title(f), Lng.str("%s leaves the Council", gone),
                        Lng.str("%s is no longer an active member. %s takes the seat of %s.", gone, seat.name, seatTitle(f, seat))));
                changed = true;
            }
            Agenda a = seat.agenda;
            if (a != null) {
                if (a.objective == Objective.STAY_SOLVENT && f.factionPoints < 0) {
                    settle(f, st, seat, Settle.FAILED, "", now, after);
                    changed = true;
                } else if (inPlace(f.getIdFaction(), a)) {
                    settle(f, st, seat, Settle.MET, "", now, after);
                    changed = true;
                } else if (now >= a.deadline) {
                    settle(f, st, seat, a.objective.timed() ? Settle.MET : Settle.FAILED, "", now, after);
                    changed = true;
                }
            } else if (now >= seat.nextAt) {
                if (!seat.held()) {
                    Agenda made = generate(f, seat.archetype, now);
                    seat.agenda = made;
                    after.add(() -> Civics.governanceNews(f, title(f), Lng.str("%s asks: %s", who(f, seat), describe(made, Council::serverName)),
                            Lng.str("\"%s\"\n\nMeet it for %s faction points; fail it and pay %s.", quote(f, made), made.reward, made.penalty)));
                    changed = true;
                } else if (seat.options.isEmpty()) {
                    for (int i = 0; i < 3; i++) {
                        Agenda o = generate(f, seat.archetype + i, now);
                        o.deadline -= o.issued;
                        o.issued = 0;
                        seat.options.add(o);
                    }
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static Seat newSeat(int archetype, long now, CouncilLore.Flavour flavour) {
        Seat s = new Seat();
        s.archetype = archetype;
        s.name = CouncilLore.name(flavour);
        //the first agendas come in over the first day, not all at once
        s.nextAt = now + ThreadLocalRandom.current().nextLong(DAY_MS / 4);
        return s;
    }

    static final int POOL_SIZE = 4;

    /** A day's candidates: any seat's role, each with their own starting favour. */
    static List<Seat> drawPool(long now, CouncilLore.Flavour flavour) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<Seat> out = new ArrayList<>();
        for (int i = 0; i < POOL_SIZE; i++) {
            Seat s = newSeat(r.nextInt(POOLS.length), now, flavour);
            s.favour = 35 + r.nextInt(36);
            out.add(s);
        }
        return out;
    }

    private static void toAi(Seat seat, long now, CouncilLore.Flavour flavour) {
        seat.member = "";
        seat.name = CouncilLore.name(flavour);
        seat.favour = START_FAVOUR;
        seat.agenda = null;
        seat.options.clear();
        seat.nextAt = now;
    }

    /**
     * Ends a seat's agenda and pays for it. Meeting one drops every agenda it clashed with; an AI councilor
     * whose favour runs out resigns.
     */
    private void settle(Faction f, State st, Seat seat, Settle how, String by, long now, List<Runnable> after) {
        Agenda a = seat.agenda;
        if (a == null) {
            return;
        }
        seat.agenda = null;
        seat.options.clear();
        seat.nextAt = now + DAY_MS + ThreadLocalRandom.current().nextLong(DAY_MS);
        int points = switch (how) {
            case MET -> a.reward;
            case FAILED -> -a.penalty;
            case REFUSED -> -a.refuseCost();
            case CLASHED -> 0;
        };
        if (!seat.held()) {
            seat.favour = Math.max(0, Math.min(MAX_FAVOUR, seat.favour + switch (how) {
                case MET -> FAVOUR_MET;
                case FAILED -> -FAVOUR_FAILED;
                case REFUSED -> -FAVOUR_REFUSED;
                case CLASHED -> -FAVOUR_CLASHED;
            }));
        }
        String what = describe(a, Council::serverName);
        String who = who(f, seat);
        after.add(() -> {
            if (points != 0) {
                f.factionPoints += points;
                f.sendFactionPointUpdate(GameServerState.instance.getGameState());
            }
            switch (how) {
                case MET -> Civics.governanceNews(f, title(f), Lng.str("Agenda met: %s", what),
                        Lng.str("%s is pleased. %s faction points.", who, "+" + points));
                case FAILED -> Civics.governanceNews(f, title(f), Lng.str("Agenda failed: %s", what),
                        Lng.str("%s is displeased. %s faction points.", who, points));
                case REFUSED -> Civics.governanceNews(f, by, Lng.str("Agenda refused: %s", what),
                        Lng.str("%s refused %s's agenda. %s faction points.", by, who, points));
                case CLASHED -> Civics.governanceNews(f, title(f), Lng.str("Agenda dropped: %s", what),
                        Lng.str("Another councilor's agenda was met in its place. %s is displeased.", who));
            }
        });
        if (how == Settle.MET) {
            st.fulfilled++;
            for (Seat o : st.seats) {
                if (o != seat && clash(a, o.agenda)) {
                    settle(f, st, o, Settle.CLASHED, "", now, after);
                }
            }
        }
        if (!seat.held() && seat.favour <= 0) {
            String old = seat.name;
            seat.name = CouncilLore.name(flavour(f));
            seat.favour = START_FAVOUR;
            after.add(() -> Civics.governanceNews(f, title(f), Lng.str("%s resigns from the Council", old),
                    Lng.str("Out of patience with us, %s gave up the seat of %s. %s takes it.", old, seatTitle(f, seat), seat.name)));
        }
    }

    // ---- server: agendas

    private static Agenda generate(Faction f, int archetype, long now) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<Objective> pool = new ArrayList<>(List.of(POOLS[Math.floorMod(archetype, POOLS.length)]));
        Collections.shuffle(pool, r);
        for (Objective o : pool) {
            Agenda a = make(f, o, now, r);
            if (a != null) {
                return a;
            }
        }
        //nothing it wants is possible right now: it asks for patience instead
        Agenda a = make(f, f.factionPoints >= 0 && r.nextBoolean() ? Objective.STAY_SOLVENT : Objective.KEEP_PEACE, now, r);
        return a != null ? a : make(f, Objective.KEEP_PEACE, now, r);
    }

    private static Agenda make(Faction f, Objective o, long now, ThreadLocalRandom r) {
        DiplomacyManager dm = DiplomacyManager.server();
        int me = f.getIdFaction();
        List<Integer> at = new ArrayList<>();
        List<String> types = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (Faction other : GameServerState.instance.getFactionManager().getFactionCollection()) {
            int id = other.getIdFaction();
            //real polities only: not the shop owner, the pirates or the engine's fauna factions
            if (id == me || id == 0 || id == FactionManager.TRAIDING_GUILD_ID || id == FactionManager.PIRATES_ID || !other.isShowInHub()) {
                continue;
            }
            boolean war = dm.hasTreaty(me, id, TreatyTypeRegistry.WAR);
            int opinion = standing(dm, other, me);
            String type = targetType(o, opinion, war, t -> dm.hasTreaty(me, id, t), dm.getTreaties(me, id).getTreaties().isEmpty());
            if (type != null) {
                at.add(id);
                types.add(type);
                weights.add(weight(o, opinion));
            }
            if (o == Objective.BREAK_TREATY && opinion < BREAK_BELOW) {
                for (FactionTreaty t : dm.getTreaties(me, id).getTreaties()) {
                    for (String b : BREAKABLE) {
                        if (b.equals(t.typeId)) {
                            at.add(id);
                            types.add(b);
                            weights.add(weight(o, opinion));
                        }
                    }
                }
            }
        }
        Agenda a = new Agenda();
        a.objective = o;
        a.issued = now;
        if (o == Objective.CLAIM_SYSTEMS) {
            a.count = 1 + r.nextInt(2);
            a.deadline = now + (3 + r.nextInt(3)) * DAY_MS;
        } else if (o.timed()) {
            if (o == Objective.STAY_SOLVENT && f.factionPoints < 0) {
                return null;
            }
            a.count = 3;
            a.deadline = now + a.count * DAY_MS;
        } else {
            if (at.isEmpty()) {
                return null;
            }
            int i = pick(weights, r.nextInt(Math.max(1, weights.stream().mapToInt(Integer::intValue).sum())));
            a.target = at.get(i);
            a.typeId = types.get(i);
            a.count = o == Objective.DESTROY_SHIPS ? 2 + r.nextInt(3) : 1;
            a.deadline = now + (3 + r.nextInt(3)) * DAY_MS;
        }
        a.reward = switch (o) {
            case DESTROY_SHIPS -> 120 * a.count;
            case DECLARE_WAR, BREAK_TREATY -> 300;
            case SIGN_TREATY -> 250;
            case SANCTION -> 200;
            case KEEP_PEACE -> 200;
            case STAY_SOLVENT -> 150;
            case CLAIM_SYSTEMS -> 200 * a.count;
        };
        a.penalty = a.reward * 3 / 5;
        return a;
    }

    /**
     * Whether what the agenda asks for already stands, however it came about: a sanction or treaty in
     * force, a war under way, a treaty gone. Agendas are only issued for what does not stand yet, so this
     * catches what the hooks miss (a renewal, a sanction set some other way) without paying for nothing.
     */
    private static boolean inPlace(int me, Agenda a) {
        DiplomacyManager dm = DiplomacyManager.server();
        return switch (a.objective) {
            case DECLARE_WAR -> dm.hasTreaty(me, a.target, TreatyTypeRegistry.WAR);
            case SIGN_TREATY -> dm.hasTreaty(me, a.target, a.typeId);
            case BREAK_TREATY -> !dm.hasTreaty(me, a.target, a.typeId);
            case SANCTION -> {
                //ours on them, not theirs on us
                FactionTreaty t = dm.getTreaties(me, a.target).get(a.typeId);
                yield t != null && t.from == me;
            }
            default -> false;
        };
    }

    /** Their opinion of us, on the NPC rules' scale (war at -500, hostile at -200). */
    static final int WAR_BELOW = -500;
    static final int SANCTION_BELOW = -200;
    static final int BREAK_BELOW = 0;

    private static int standing(DiplomacyManager dm, Faction other, int me) {
        videogoose.betterfactions.diplomacy.DiplomacyEntity e = dm.diplomacyOf(other).entities.get((long) me);
        return e != null ? e.getPoints() : 0;
    }

    /**
     * Whether a faction is a target for {@code o}, and with which treaty ("" for none): relations decide.
     * Councilors nudge rather than force: war only when relations are very bad, sanctions when poor,
     * treaties when fair. A treaty to break is found by the caller. Null for no.
     */
    static String targetType(Objective o, int opinion, boolean war, java.util.function.Predicate<String> holds, boolean noTreaties) {
        return switch (o) {
            case DESTROY_SHIPS -> war ? "" : null;
            case DECLARE_WAR -> !war && opinion <= WAR_BELOW && noTreaties ? "" : null;
            case SANCTION -> war || opinion > SANCTION_BELOW ? null
                    : !holds.test(TreatyTypeRegistry.EMBARGO) ? TreatyTypeRegistry.EMBARGO
                    : !holds.test(TreatyTypeRegistry.TARIFF) ? TreatyTypeRegistry.TARIFF : null;
            case SIGN_TREATY -> war || opinion < 0 || holds.test(TreatyTypeRegistry.EMBARGO) ? null
                    : !holds.test(TreatyTypeRegistry.NON_AGGRESSION) ? TreatyTypeRegistry.NON_AGGRESSION
                    : !holds.test(TreatyTypeRegistry.TRADE_AGREEMENT) ? TreatyTypeRegistry.TRADE_AGREEMENT : null;
            default -> null;
        };
    }

    /** How likely a faction is chosen: the further relations are past the line, the likelier. */
    static int weight(Objective o, int opinion) {
        return 1 + switch (o) {
            case DECLARE_WAR -> WAR_BELOW - opinion;
            case SANCTION -> SANCTION_BELOW - opinion;
            case BREAK_TREATY -> BREAK_BELOW - opinion;
            case SIGN_TREATY -> opinion;
            default -> 0;
        } / 50;
    }

    /** The index {@code roll} lands on, walking the weights. */
    static int pick(List<Integer> weights, int roll) {
        for (int i = 0; i < weights.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return i;
            }
        }
        return weights.size() - 1;
    }

    // ---- server: progress, from the hooks

    /** Server: a treaty came into force; {@code a} offered or declared it. */
    public void onTreaty(int a, int b, String typeId) {
        boolean war = TreatyTypeRegistry.WAR.equals(typeId);
        boolean sanction = TreatyTypeRegistry.EMBARGO.equals(typeId) || TreatyTypeRegistry.TARIFF.equals(typeId);
        progress(a, ag -> war ? (ag.objective == Objective.DECLARE_WAR && ag.target == b ? Settle.MET : ag.objective == Objective.KEEP_PEACE ? Settle.FAILED : null)
                : sanction ? (ag.objective == Objective.SANCTION && ag.target == b && ag.typeId.equals(typeId) ? Settle.MET : null)
                : ag.objective == Objective.SIGN_TREATY && ag.target == b && (ag.typeId.isEmpty() || ag.typeId.equals(typeId)) ? Settle.MET : null);
        if (!war && !sanction) {
            progress(b, ag -> ag.objective == Objective.SIGN_TREATY && ag.target == a && (ag.typeId.isEmpty() || ag.typeId.equals(typeId)) ? Settle.MET : null);
        }
    }

    /** Server: the faction claimed a system that was not its own. */
    public void onClaim(int faction) {
        progress(faction, ag -> {
            if (ag.objective == Objective.CLAIM_SYSTEMS) {
                ag.done++;
                return ag.done >= ag.count ? Settle.MET : null;
            }
            return null;
        });
    }

    /** Server: {@code breaker} walked out of a treaty with {@code other}. */
    public void onBroken(int breaker, int other, String typeId) {
        progress(breaker, ag -> ag.objective == Objective.BREAK_TREATY && ag.target == other && ag.typeId.equals(typeId) ? Settle.MET
                : ag.objective == Objective.KEEP_PEACE ? Settle.FAILED : null);
    }

    /** Server: a ship of {@code lost} died in a battle; every other side counts it. */
    public void onShipLost(int[] sides, int lost) {
        for (int side : sides) {
            if (side != lost && side != 0) {
                progress(side, ag -> {
                    if (ag.objective == Objective.DESTROY_SHIPS && ag.target == lost) {
                        //counted even when it does not settle it: progress() sends the new count
                        ag.done++;
                        return ag.done >= ag.count ? Settle.MET : null;
                    }
                    return null;
                });
            }
        }
    }

    private void progress(int faction, java.util.function.Function<Agenda, Settle> check) {
        Faction f = GameServerState.instance.getFactionManager().getFaction(faction);
        if (f == null) {
            return;
        }
        List<Runnable> after = new ArrayList<>();
        boolean changed = false;
        synchronized (this) {
            State st = byFaction.get(faction);
            if (st == null || st.suspended()) {
                return;
            }
            long now = System.currentTimeMillis();
            for (Seat seat : new ArrayList<>(st.seats)) {
                if (seat.agenda == null) {
                    continue;
                }
                int before = seat.agenda.done;
                Settle how = check.apply(seat.agenda);
                if (how != null) {
                    settle(f, st, seat, how, "", now, after);
                    changed = true;
                } else if (seat.agenda.done != before) {
                    changed = true;
                }
            }
            if (changed) {
                version++;
            }
        }
        after.forEach(Runnable::run);
        if (changed) {
            send(f);
        }
    }

    // ---- server: what members do

    /** Server: a member backs or refuses an agenda, or chooses their seat's. */
    public void action(PlayerState p, Tag tag) {
        Tag[] t = tag.getStruct();
        if (t.length < 3 || t[0].getType() != Tag.Type.BYTE || t[1].getType() != Tag.Type.INT || t[2].getType() != Tag.Type.INT) {
            return;
        }
        Faction f = GameServerState.instance.getFactionManager().getFaction(p.getFactionId());
        if (f == null) {
            return;
        }
        int op = t[0].getByte();
        if (op == 1 && !Perms.has(f, p.getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION)) {
            reject(p, Lng.str("Your rank may not refuse the Council."));
            return;
        }
        if ((op == 3 || op == 4) && !Perms.has(f, p.getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION)) {
            reject(p, Lng.str("Your rank may not rename the Council."));
            return;
        }
        if (op == 7 && !Perms.has(f, p.getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION)) {
            reject(p, Lng.str("Your rank may not add seats to the Council."));
            return;
        }
        if (op == 6 && !Perms.has(f, p.getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION)) {
            reject(p, Lng.str("Your rank may not change the Council's style."));
            return;
        }
        if (op == 5 && !Perms.has(f, p.getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION)) {
            reject(p, Lng.str("Your rank may not change the Council's councilors."));
            return;
        }
        String newName = t.length > 3 && t[3].getType() == Tag.Type.STRING ? FactionCivics.cleanSenateName(t[3].getString()) : "";
        List<Runnable> after = new ArrayList<>();
        long now = System.currentTimeMillis();
        synchronized (this) {
            State st = byFaction.get(f.getIdFaction());
            int i = t[1].getInt();
            //renaming the Council is the one action that is about no seat
            Seat seat = st == null || i < 0 || i >= st.seats.size() ? null : st.seats.get(i);
            if (st == null || seat == null && op != 3 && op != 6 && op != 7) {
                return;
            }
            switch (op) {
                case 3 -> {
                    if (st.name.equals(newName)) {
                        return;
                    }
                    CouncilLore.Flavour flavour = CouncilLore.of(Civics.server().of(f.getIdFaction()), st.style);
                    String old = st.title(flavour);
                    st.name = newName;
                    String renamed = st.title(flavour);
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s is renamed", old),
                            Lng.str("%s renamed %s to %s.", p.getName(), old, renamed)));
                }
                case 7 -> {
                    int role = t[2].getInt();
                    if (st.seats.size() >= MAX_SEATS || !openRoles(st).contains(role)) {
                        reject(p, Lng.str("That role is taken, or the Council has no room for another seat."));
                        return;
                    }
                    Seat added = newSeat(role, now, flavour(f));
                    st.seats.add(added);
                    String title = seatTitle(f, added);
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("A new seat: %s", title),
                            Lng.str("%s added a seat to the Council. %s takes it as %s. It costs %s faction points of upkeep an income turn.",
                                    p.getName(), added.name, title, videogoose.betterfactions.BFConfig.institutionUpkeep())));
                }
                case 6 -> {
                    String chosen = t.length > 3 && t[3].getType() == Tag.Type.STRING ? t[3].getString() : "";
                    FactionCivics civics = Civics.server().of(f.getIdFaction());
                    if (chosen.equals(st.style) || !CouncilLore.allowed(civics, chosen)) {
                        return;
                    }
                    String old = st.title(CouncilLore.of(civics, st.style));
                    st.style = chosen;
                    CouncilLore.Flavour now2 = CouncilLore.of(civics, chosen);
                    String renamed = st.title(now2);
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s takes a new style", old),
                            Lng.str("%s gave the Council the style of %s, shaped by %s. Its councilors keep their names.", p.getName(), renamed, CouncilLore.shapedBy(now2))));
                }
                case 5 -> {
                    int k = t[2].getInt();
                    if (seat.held() || k < 0 || k >= st.pool.size()) {
                        reject(p, Lng.str("Only an AI councilor can be replaced from the pool."));
                        return;
                    }
                    Seat c = st.pool.remove(k);
                    String old = seat.name;
                    //an agenda left behind is paid for as refused, so dismissing is no way out of one
                    int cost = seat.agenda != null ? seat.agenda.refuseCost() : 0;
                    seat.archetype = c.archetype;
                    seat.name = c.name;
                    seat.favour = c.favour;
                    seat.agenda = null;
                    seat.options.clear();
                    seat.nextAt = c.nextAt;
                    String title = seatTitle(f, seat);
                    after.add(() -> {
                        if (cost > 0) {
                            f.factionPoints -= cost;
                            f.sendFactionPointUpdate(GameServerState.instance.getGameState());
                        }
                        Civics.governanceNews(f, p.getName(), Lng.str("%s joins the Council", c.name),
                                cost > 0 ? Lng.str("%s dismissed %s and seated %s as %s. %s's unfinished agenda cost %s faction points.", p.getName(), old, c.name, title, old, cost)
                                        : Lng.str("%s dismissed %s and seated %s as %s.", p.getName(), old, c.name, title));
                    });
                }
                case 4 -> {
                    if (seat.held()) {
                        reject(p, Lng.str("A member's seat carries their own name."));
                        return;
                    }
                    if (newName.isEmpty() || newName.equals(seat.name)) {
                        return;
                    }
                    String old = seat.name;
                    seat.name = newName;
                    String title = seatTitle(f, seat);
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s is now %s", old, newName),
                            Lng.str("%s renamed the %s, %s, to %s.", p.getName(), title, old, newName)));
                }
                case 0 -> {
                    if (seat.agenda == null || !seat.agenda.backedBy.isEmpty()) {
                        return;
                    }
                    seat.agenda.backedBy = p.getName();
                    String what = describe(seat.agenda, Council::serverName);
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("Backed: %s", what),
                            Lng.str("%s means to meet %s's agenda.", p.getName(), who(f, seat))));
                }
                case 1 -> settle(f, st, seat, Settle.REFUSED, p.getName(), now, after);
                case 2 -> {
                    if (!seat.held() || !seat.member.equalsIgnoreCase(p.getName()) || seat.agenda != null) {
                        reject(p, Lng.str("Only the member holding this seat may choose its agenda, and only while it has none."));
                        return;
                    }
                    int k = t[2].getInt();
                    if (k < 0 || k >= seat.options.size()) {
                        return;
                    }
                    Agenda a = seat.options.get(k);
                    a.issued = now;
                    a.deadline += now;
                    seat.agenda = a;
                    seat.options.clear();
                    String what = describe(a, Council::serverName);
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s sets an agenda: %s", p.getName(), what),
                            Lng.str("Meet it for %s faction points; fail it and pay %s.", a.reward, a.penalty)));
                }
                default -> {
                    return;
                }
            }
            version++;
        }
        after.forEach(Runnable::run);
        send(f);
    }

    /** Server: a member asks the Senate to appoint a member to the Council, or to remove one. */
    public void propose(PlayerState p, Tag tag) {
        Tag[] t = tag.getStruct();
        if (t.length < 4 || t[0].getType() != Tag.Type.BYTE || t[1].getType() != Tag.Type.INT || t[2].getType() != Tag.Type.STRING) {
            return;
        }
        Faction f = GameServerState.instance.getFactionManager().getFaction(p.getFactionId());
        if (f == null || !activeMember(f, p.getName())) {
            return;
        }
        if (!Civics.server().holds(f.getIdFaction(), CivicRegistry.SENATE)) {
            reject(p, Lng.str("Members join the Council by Senate vote, and your faction has no Senate."));
            return;
        }
        int op = t[0].getByte();
        int i = t[1].getInt();
        String player = t[2].getString();
        String why;
        String description;
        synchronized (this) {
            State st = byFaction.get(f.getIdFaction());
            why = st == null ? Lng.str("Your faction has no Council.") : problem(f, st, op, i, player);
            description = why != null ? "" : switch (op) {
                case 0 -> Lng.str("Appoint %s to a new seat on the Council, as %s", player, CouncilLore.seat(flavour(f), i));
                case 1 -> Lng.str("Appoint %s to the Council in place of %s", player, st.seats.get(i).name);
                default -> Lng.str("Remove %s from the Council", st.seats.get(i).member);
            };
        }
        if (why != null) {
            reject(p, why);
            return;
        }
        Tag act = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.STRING, null, ACT),
                new Tag(Tag.Type.BYTE, null, (byte) op), new Tag(Tag.Type.INT, null, i), new Tag(Tag.Type.STRING, null, player), FinishTag.INST});
        Senate.server().open(f, p.getName(), description, t[3].getType() == Tag.Type.STRING ? t[3].getString() : "", false, act,
                v -> carry(f.getIdFaction(), v.act));
    }

    /** Why the appointment cannot be made, or null. Checked when proposed, and again when carried. */
    private static String problem(Faction f, State st, int op, int i, String player) {
        switch (op) {
            case 0, 1 -> {
                if (!activeMember(f, player)) {
                    return Lng.str("%s is not an active member.", player);
                }
                if (st.heldBy(player) != null) {
                    return Lng.str("%s already holds a seat.", player);
                }
                if (op == 0 && st.seats.size() >= MAX_SEATS) {
                    return Lng.str("The Council has no room for another seat.");
                }
                if (op == 0 && !openRoles(st).contains(i)) {
                    return Lng.str("That role already has a seat.");
                }
                if (op == 1 && (i < 0 || i >= st.seats.size() || st.seats.get(i).held())) {
                    return Lng.str("That seat is not held by a councilor who can be replaced.");
                }
                return null;
            }
            case 2 -> {
                return i < 0 || i >= st.seats.size() || !st.seats.get(i).held() ? Lng.str("No member holds that seat.") : null;
            }
            default -> {
                return Lng.str("Unknown request.");
            }
        }
    }

    /** Whether a Senate vote's act is a Council appointment rather than a diplomatic act. */
    public static boolean isAct(Tag act) {
        Tag[] t = act == null ? null : act.getStruct();
        return t != null && t.length > 0 && t[0].getType() == Tag.Type.STRING && ACT.equals(t[0].getString());
    }

    /** Server: a Senate vote on the Council passed. */
    public void carry(int faction, Tag act) {
        Tag[] t = act.getStruct();
        Faction f = GameServerState.instance.getFactionManager().getFaction(faction);
        if (f == null || t.length < 4) {
            return;
        }
        int op = t[1].getByte();
        int i = t[2].getInt();
        String player = t[3].getString();
        long now = System.currentTimeMillis();
        String news;
        synchronized (this) {
            State st = byFaction.get(faction);
            String why = st == null ? Lng.str("the faction has no Council") : problem(f, st, op, i, player);
            if (why != null) {
                news = null;
                Civics.governanceNews(f, title(f), Lng.str("Could not be carried out: a Council appointment"), why);
            } else if (op == 2) {
                Seat seat = st.seats.get(i);
                String gone = seat.member;
                toAi(seat, now, flavour(f));
                news = Lng.str("%s leaves the Council by Senate vote. %s takes the seat of %s.", gone, seat.name, seatTitle(f, seat));
            } else {
                Seat seat = op == 0 ? newSeat(i, now, flavour(f)) : st.seats.get(i);
                String replaced = op == 1 ? seat.name : null;
                if (op == 0) {
                    st.seats.add(seat);
                }
                seat.member = player;
                seat.name = player;
                seat.agenda = null;
                seat.options.clear();
                seat.nextAt = now;
                news = replaced == null ? Lng.str("%s takes a new seat on the Council, as %s.", player, seatTitle(f, seat))
                        : Lng.str("%s takes the seat of %s from %s.", player, seatTitle(f, seat), replaced);
            }
            version++;
        }
        if (news != null) {
            Civics.governanceNews(f, title(f), Lng.str("The Council changes"), news);
        }
        nextCheck = 0;
        update(now);
        send(f);
    }

    /** Debugging, from /council_next: every empty seat of the faction asks now. */
    public void hurry(int faction) {
        synchronized (this) {
            State st = byFaction.get(faction);
            if (st != null) {
                for (Seat s : st.seats) {
                    s.nextAt = 0;
                }
            }
        }
        nextCheck = 0;
        update(System.currentTimeMillis());
    }

    /** Server: the faction dropped its Council or is gone. */
    public void onDissolved(int faction) {
        synchronized (this) {
            byFaction.remove(faction);
            version++;
        }
        Faction f = GameServerState.instance.getFactionManager().getFaction(faction);
        if (f != null) {
            send(f);
        }
    }

    // ---- server: words

    private String title(Faction f) {
        State st;
        synchronized (this) {
            st = byFaction.get(f.getIdFaction());
        }
        CouncilLore.Flavour flavour = CouncilLore.of(Civics.server().of(f.getIdFaction()), st != null ? st.style : "");
        return st != null ? st.title(flavour) : CouncilLore.title(flavour);
    }

    private CouncilLore.Flavour flavour(Faction f) {
        State st;
        synchronized (this) {
            st = byFaction.get(f.getIdFaction());
        }
        return CouncilLore.of(Civics.server().of(f.getIdFaction()), st != null ? st.style : "");
    }

    private String seatTitle(Faction f, Seat seat) {
        return CouncilLore.seat(flavour(f), seat.archetype);
    }

    private String who(Faction f, Seat seat) {
        return seat.held() ? Lng.str("%s (%s)", seat.name, seatTitle(f, seat)) : Lng.str("%s, %s", seatTitle(f, seat), seat.name);
    }

    private String quote(Faction f, Agenda a) {
        return CouncilLore.quote(flavour(f), a.objective, serverName(a.target));
    }

    private static String serverName(int id) {
        Faction f = GameServerState.instance.getFactionManager().getFaction(id);
        return f != null ? f.getName() : "?";
    }

    private static boolean hasActiveMember(Faction f) {
        for (FactionPermission p : f.getMembersUID().values()) {
            if (p.isActiveMember()) {
                return true;
            }
        }
        return false;
    }

    static boolean activeMember(Faction f, String player) {
        for (Map.Entry<String, FactionPermission> e : f.getMembersUID().entrySet()) {
            if (e.getKey().equalsIgnoreCase(player)) {
                return e.getValue().isActiveMember();
            }
        }
        return false;
    }

    private static void reject(PlayerState p, String why) {
        p.sendServerMessage(new ServerMessage(new Object[]{why}, ServerMessage.MESSAGE_TYPE_ERROR, p.getId()));
    }

    // ---- sync: each faction's Council to its own members

    public void send(Faction f) {
        for (PlayerState p : GameServerState.instance.getPlayerStatesByName().values()) {
            if (p.getFactionId() == f.getIdFaction() && p.getClientChannel() != null) {
                sendTo(p);
            }
        }
    }

    public void sendTo(PlayerState p) {
        Tag tag;
        synchronized (this) {
            State st = byFaction.get(p.getFactionId());
            tag = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.INT, null, p.getFactionId()),
                    st != null ? st.toTag() : new Tag(Tag.Type.STRUCT, null, new Tag[]{FinishTag.INST}), FinishTag.INST});
        }
        BFPacket.toPlayer(p, BFPacket.Kind.COUNCIL, tag);
    }

    // ---- saved

    public synchronized Tag toTag() {
        Tag[] t = new Tag[byFaction.size() + 1];
        int i = 0;
        for (Map.Entry<Integer, State> e : byFaction.entrySet()) {
            t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.INT, null, e.getKey()), e.getValue().toTag(), FinishTag.INST});
        }
        t[i] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    public synchronized void fromTag(Tag tag) {
        byFaction.clear();
        for (Tag e : tag.getStruct()) {
            if (e.getType() == Tag.Type.STRUCT) {
                byFaction.put(e.getStruct()[0].getInt(), State.fromTag(e.getStruct()[1]));
            }
        }
        version++;
    }

    public synchronized void clear() {
        byFaction.clear();
        version++;
    }
}
