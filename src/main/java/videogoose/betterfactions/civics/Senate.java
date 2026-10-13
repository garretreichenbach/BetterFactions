package videogoose.betterfactions.civics;

import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.net.BFPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Senate votes: a faction holding the Senate institution decides its diplomatic acts by vote. The act
 * waits here as the proposal or answer it was sent as, and is sent again, as from its proposer, once
 * the vote carries; everything it is checked for then is checked again.
 *
 * <p>Every active member is a senator, the proposer voting for. A vote carries the moment more than half
 * the senators are for it, fails the moment that can no longer happen, and otherwise closes after
 * {@code senate_vote_hours} on the ballots cast: more for than against carries. A faction with one
 * active member decides at once.</p>
 *
 * <p>Votes go to the faction's own members only: they are the faction's undeclared intentions.</p>
 */
public final class Senate {

    private static final Senate SERVER = new Senate();
    private static final Senate CLIENT = new Senate();

    public static Senate server() {
        return SERVER;
    }

    public static Senate client() {
        return CLIENT;
    }

    public static final class Vote {
        public int id;
        public int faction;
        public String proposer = "";
        /** What is being decided, in a line, written when it opened. */
        public String description = "";
        /** True for an answer to another faction's offer, false for a proposal or revocation of our own. */
        public boolean response;
        /** The proposal or answer, as it arrived. Server only; clients get an empty struct. */
        public Tag act;
        public long opened;
        public long closes;
        /** Lower-case player name to ballot: 1 for, 0 against. */
        public final Map<String, Byte> ballots = new HashMap<>();
        /** Senators when last counted, for clients to show how many have not voted. */
        public int senators;
        /** The proposer's case for it, or empty. */
        public String reason = "";
        /** Non-zero once it has passed under a leader's veto: it waits for the leader until then, and takes effect. */
        public long holdUntil;

        public boolean held() {
            return holdUntil > 0;
        }

        public int count(boolean yes) {
            int n = 0;
            for (byte b : ballots.values()) {
                if ((b != 0) == yes) {
                    n++;
                }
            }
            return n;
        }

        Tag toTag(boolean withAct) {
            Tag[] b = new Tag[ballots.size() + 1];
            int i = 0;
            for (Map.Entry<String, Byte> e : ballots.entrySet()) {
                b[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                        new Tag(Tag.Type.STRING, null, e.getKey()), new Tag(Tag.Type.BYTE, null, e.getValue()), FinishTag.INST});
            }
            b[i] = FinishTag.INST;
            return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.INT, null, id),
                    new Tag(Tag.Type.INT, null, faction),
                    new Tag(Tag.Type.STRING, null, proposer),
                    new Tag(Tag.Type.STRING, null, description),
                    new Tag(Tag.Type.BYTE, null, (byte) (response ? 1 : 0)),
                    withAct && act != null ? act : new Tag(Tag.Type.STRUCT, null, new Tag[]{FinishTag.INST}),
                    new Tag(Tag.Type.LONG, null, opened),
                    new Tag(Tag.Type.LONG, null, closes),
                    new Tag(Tag.Type.STRUCT, null, b),
                    new Tag(Tag.Type.INT, null, senators),
                    new Tag(Tag.Type.STRING, null, reason),
                    new Tag(Tag.Type.LONG, null, holdUntil),
                    FinishTag.INST});
        }

        static Vote fromTag(Tag tag) {
            Tag[] t = tag.getStruct();
            Vote v = new Vote();
            v.id = t[0].getInt();
            v.faction = t[1].getInt();
            v.proposer = t[2].getString();
            v.description = t[3].getString();
            v.response = t[4].getByte() != 0;
            v.act = t[5];
            v.opened = t[6].getLong();
            v.closes = t[7].getLong();
            for (Tag e : t[8].getStruct()) {
                if (e.getType() == Tag.Type.STRUCT) {
                    v.ballots.put(e.getStruct()[0].getString(), e.getStruct()[1].getByte());
                }
            }
            v.senators = t[9].getInt();
            //saved before votes carried a reason
            v.reason = t.length > 10 && t[10].getType() == Tag.Type.STRING ? t[10].getString() : "";
            v.holdUntil = t.length > 11 && t[11].getType() == Tag.Type.LONG ? t[11].getLong() : 0L;
            return v;
        }
    }

    public enum Outcome {OPEN, CARRIED, LOST}

    /** However late a vote passes, the leader gets at least this long to veto it. */
    static final long MIN_HOLD_MS = 60L * 60L * 1000L;
    /** {@link Vote#holdUntil} once the leader has assented: carried out at the next count, paused or not. */
    static final long ASSENTED = 1L;

    /** Whether the player leads the faction: holds its founder rank. */
    public static boolean leads(Faction f, String player) {
        for (Map.Entry<String, FactionPermission> e : f.getMembersUID().entrySet()) {
            if (e.getKey().equalsIgnoreCase(player)) {
                return e.getValue().isFounder(f);
            }
        }
        return false;
    }

    /**
     * Debugging, from /senate_pause: nothing takes effect, so a faction of one can see its votes. A vote
     * that passes is held as if awaiting the leader, the founder's own included, so assent and veto can be
     * tried; one that fails stays open. Assenting still carries it out. Not saved: a restart unpauses.
     * Unpaused, each vote is settled at its next ballot or its close.
     */
    public static volatile boolean paused;

    private final List<Vote> votes = new ArrayList<>();
    private int nextId = 1;
    private int version;

    /**
     * How a vote stands. {@code senators} counts everyone who may vote, whether or not they have.
     */
    public static Outcome tally(int yes, int no, int senators, boolean closed) {
        if (yes * 2 > senators) {
            return Outcome.CARRIED;
        }
        if ((senators - no) * 2 <= senators) {
            return Outcome.LOST;
        }
        if (closed) {
            return yes > no ? Outcome.CARRIED : Outcome.LOST;
        }
        return Outcome.OPEN;
    }

    /** Whether this faction's diplomatic acts wait on a vote. */
    public static boolean gates(Faction f) {
        return f != null && !f.isNPC() && Civics.server().holds(f.getIdFaction(), CivicRegistry.SENATE);
    }

    /** Lower-case names of the faction's active members: its senators. */
    static List<String> senators(Faction f) {
        return senators(f, Civics.server().of(f.getIdFaction()));
    }

    /** The faction's active members whose rank sits, under {@code rules}. */
    static List<String> senators(Faction f, FactionCivics rules) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, FactionPermission> e : f.getMembersUID().entrySet()) {
            if (e.getValue().isActiveMember() && rules.seats(e.getValue().role)) {
                out.add(e.getKey().toLowerCase(Locale.ENGLISH));
            }
        }
        return out;
    }

    /** Client: how many sit; never fewer than one, so a vote can always be decided. */
    public static int seats(Faction f) {
        return Math.max(1, senators(f, Civics.client().of(f.getIdFaction())).size());
    }

    /** Client: whether the player sits in the faction's Senate. */
    public static boolean sits(Faction f, String player) {
        return player != null && senators(f, Civics.client().of(f.getIdFaction())).contains(player.toLowerCase(Locale.ENGLISH));
    }

    /** Client: the open votes of a faction, oldest first. */
    public synchronized List<Vote> of(int faction) {
        List<Vote> out = new ArrayList<>();
        for (Vote v : votes) {
            if (v.faction == faction) {
                out.add(v);
            }
        }
        return out;
    }

    public synchronized int version() {
        return version;
    }

    /**
     * Server: puts an act to the vote. Runs {@code carry} once it carries, perhaps right away.
     *
     * @param act the proposal or answer, as a tag to rebuild it from
     */
    public void open(Faction f, String proposer, String description, String reason, boolean response, Tag act, Consumer<Vote> carry) {
        Vote v = new Vote();
        synchronized (this) {
            v.id = nextId++;
            v.faction = f.getIdFaction();
            v.proposer = proposer;
            v.description = description;
            v.reason = reason == null ? "" : reason.trim();
            if (v.reason.length() > videogoose.betterfactions.diplomacy.DiplomacyProposal.MAX_JUSTIFICATION) {
                v.reason = v.reason.substring(0, videogoose.betterfactions.diplomacy.DiplomacyProposal.MAX_JUSTIFICATION);
            }
            v.response = response;
            v.act = act;
            v.opened = System.currentTimeMillis();
            v.closes = v.opened + Civics.server().of(f.getIdFaction()).voteHours(BFConfig.senateVoteHours(), BFConfig.senateMaxVoteHours()) * 60L * 60L * 1000L;
            //any member may propose; only a senator's proposal counts as their vote for it
            if (senators(f).contains(proposer.toLowerCase(Locale.ENGLISH))) {
                v.ballots.put(proposer.toLowerCase(Locale.ENGLISH), (byte) 1);
            }
            votes.add(v);
        }
        resolve(f, carry);
        //a motion that carried at once (a faction of one) is reported once, as passed
        boolean open;
        synchronized (this) {
            open = votes.contains(v);
        }
        if (open) {
            news(f, Lng.str("The Senate is voting: %s (proposed by %s).", description, proposer));
            Civics.governanceNews(f, proposer, Lng.str("Proposed to the Senate: %s", description),
                    v.reason.isEmpty() ? Lng.str("%s put it to a vote.", proposer) : Lng.str("%s put it to a vote:\n\"%s\"", proposer, v.reason));
        }
    }

    /** Server: a senator's ballot. */
    public void cast(PlayerState p, Tag tag, java.util.function.IntFunction<Consumer<Vote>> carrier) {
        Tag[] t = tag.getStruct();
        if (t.length < 2 || t[0].getType() != Tag.Type.INT || t[1].getType() != Tag.Type.BYTE) {
            return;
        }
        Faction f = GameServerState.instance.getFactionManager().getFaction(p.getFactionId());
        if (f == null) {
            return;
        }
        String me = p.getName().toLowerCase(Locale.ENGLISH);
        if (!senators(f).contains(me)) {
            p.sendServerMessage(new ServerMessage(Lng.astr("Only active members sit in the Senate."), ServerMessage.MESSAGE_TYPE_ERROR, p.getId()));
            return;
        }
        synchronized (this) {
            Vote v = byId(t[0].getInt());
            //once it has passed, it is the leader's to decide, not the senators'
            if (v == null || v.faction != f.getIdFaction() || v.held()) {
                return;
            }
            v.ballots.put(me, t[1].getByte() != 0 ? (byte) 1 : (byte) 0);
        }
        resolve(f, carrier.apply(f.getIdFaction()));
    }

    /**
     * Server: counts the faction's open votes and settles the decided ones, then tells its members.
     */
    public void resolve(Faction f, Consumer<Vote> carry) {
        List<String> senators = senators(f);
        long now = System.currentTimeMillis();
        List<Vote> carried = new ArrayList<>();
        List<Vote> lost = new ArrayList<>();
        List<Vote> held = new ArrayList<>();
        boolean veto = Civics.server().of(f.getIdFaction()).senateVeto;
        synchronized (this) {
            for (Iterator<Vote> it = votes.iterator(); it.hasNext(); ) {
                Vote v = it.next();
                if (v.faction != f.getIdFaction()) {
                    continue;
                }
                if (v.held()) {
                    //the leader assented or let it stand, or the veto was turned off while it waited; a hold
                    //only the pause made (no veto, or the founder's own) ends with the pause
                    if (v.holdUntil == ASSENTED || !paused && (!veto || leads(f, v.proposer) || now >= v.holdUntil)) {
                        it.remove();
                        carried.add(v);
                    }
                    continue;
                }
                //ballots of those who left or went inactive no longer count
                v.ballots.keySet().retainAll(senators);
                //the proposer always sits on their own motion, so a faction of one inactive member can still act
                int seats = Math.max(1, senators.size());
                v.senators = seats;
                Outcome o = tally(v.count(true), v.count(false), seats, now >= v.closes);
                if (o == Outcome.LOST && paused) {
                    //frozen: lost when unpaused, unless ballots change first
                    continue;
                }
                if (o == Outcome.CARRIED && (paused || veto && !leads(f, v.proposer))) {
                    v.holdUntil = Math.max(v.closes, now + MIN_HOLD_MS);
                    held.add(v);
                } else if (o != Outcome.OPEN) {
                    it.remove();
                    (o == Outcome.CARRIED ? carried : lost).add(v);
                }
            }
            version++;
        }
        for (Vote v : held) {
            news(f, Lng.str("The Senate passed %s. It waits for the leader to assent or veto.", v.description));
            Civics.governanceNews(f, Lng.str("The Senate"), Lng.str("Awaiting the leader: %s", v.description),
                    Lng.str("%s for, %s against, of %s senators. It takes effect unless the leader vetoes it first.", v.count(true), v.count(false), v.senators));
        }
        for (Vote v : lost) {
            news(f, Lng.str("The Senate voted down: %s.", v.description));
            Civics.governanceNews(f, Lng.str("The Senate"), Lng.str("Voted down: %s", v.description),
                    Lng.str("%s for, %s against, of %s senators. Proposed by %s.", v.count(true), v.count(false), v.senators, v.proposer));
        }
        for (Vote v : carried) {
            news(f, Lng.str("The Senate approved: %s.", v.description));
            Civics.governanceNews(f, Lng.str("The Senate"), Lng.str("Passed: %s", v.description),
                    Lng.str("%s for, %s against, of %s senators. Proposed by %s.", v.count(true), v.count(false), v.senators, v.proposer));
            carry.accept(v);
        }
        send(f);
    }

    /** Server: closes the votes whose window ran out. Called from the diplomacy update. */
    public void update(java.util.function.IntFunction<Consumer<Vote>> carrier) {
        long now = System.currentTimeMillis();
        List<Integer> due = new ArrayList<>();
        synchronized (this) {
            for (Vote v : votes) {
                if (now >= (v.held() ? v.holdUntil : v.closes) && !due.contains(v.faction)) {
                    due.add(v.faction);
                }
            }
        }
        for (int id : due) {
            Faction f = GameServerState.instance.getFactionManager().getFaction(id);
            if (f == null) {
                onDissolved(id);
            } else {
                resolve(f, carrier.apply(id));
            }
        }
    }

    /** Server: the leader assents to a vote waiting on them, or vetoes it. */
    public void lead(PlayerState p, Tag tag, java.util.function.IntFunction<Consumer<Vote>> carrier) {
        Tag[] t = tag.getStruct();
        if (t.length < 2 || t[0].getType() != Tag.Type.INT || t[1].getType() != Tag.Type.BYTE) {
            return;
        }
        Faction f = GameServerState.instance.getFactionManager().getFaction(p.getFactionId());
        if (f == null || !leads(f, p.getName())) {
            p.sendServerMessage(new ServerMessage(Lng.astr("Only the faction's leader may assent or veto."), ServerMessage.MESSAGE_TYPE_ERROR, p.getId()));
            return;
        }
        boolean assent = t[1].getByte() != 0;
        Vote v;
        synchronized (this) {
            v = byId(t[0].getInt());
            if (v == null || v.faction != f.getIdFaction() || !v.held()) {
                return;
            }
            if (assent) {
                v.holdUntil = ASSENTED;
            } else {
                votes.remove(v);
                version++;
            }
        }
        if (assent) {
            resolve(f, carrier.apply(f.getIdFaction()));
            return;
        }
        news(f, Lng.str("%s vetoed: %s.", p.getName(), v.description));
        Civics.governanceNews(f, p.getName(), Lng.str("Vetoed: %s", v.description),
                Lng.str("%s struck it down after the Senate passed it %s for, %s against. Proposed by %s.", p.getName(), v.count(true), v.count(false), v.proposer));
        send(f);
    }

    /** Client: the leader's answer to a vote waiting on them. */
    public static void requestLead(int voteId, boolean assent) {
        BFPacket.toServer(BFPacket.Kind.SENATE_LEADER, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.INT, null, voteId), new Tag(Tag.Type.BYTE, null, (byte) (assent ? 1 : 0)), FinishTag.INST}));
    }

    /** Server: the faction dropped its Senate or is gone; what was waiting on it is dropped too. */
    public void onDissolved(int faction) {
        synchronized (this) {
            votes.removeIf(v -> v.faction == faction);
            version++;
        }
        Faction f = GameServerState.instance.getFactionManager().getFaction(faction);
        if (f != null) {
            send(f);
        }
    }

    private Vote byId(int id) {
        for (Vote v : votes) {
            if (v.id == id) {
                return v;
            }
        }
        return null;
    }

    /** Server: the faction's open votes to its online members. */
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
            List<Vote> mine = new ArrayList<>();
            for (Vote v : votes) {
                if (v.faction == p.getFactionId()) {
                    mine.add(v);
                }
            }
            tag = list(mine, false);
        }
        BFPacket.toPlayer(p, BFPacket.Kind.SENATE, tag);
    }

    /** Client: the own faction's open votes, replacing what was held. */
    public synchronized void received(Tag tag) {
        votes.clear();
        for (Tag e : tag.getStruct()) {
            if (e.getType() == Tag.Type.STRUCT) {
                votes.add(Vote.fromTag(e));
            }
        }
        version++;
    }

    /** Client: a ballot. */
    public static void requestCast(int voteId, boolean yes) {
        BFPacket.toServer(BFPacket.Kind.SENATE_BALLOT, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.INT, null, voteId), new Tag(Tag.Type.BYTE, null, (byte) (yes ? 1 : 0)), FinishTag.INST}));
    }

    private static void news(Faction f, String text) {
        f.broadcastMessage(new Object[]{text}, ServerMessage.MESSAGE_TYPE_INFO, GameServerState.instance);
    }

    // ---- saved: every open vote, acts included, so a restart does not lose what was waiting

    public synchronized Tag toTag() {
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.INT, null, nextId), list(votes, true), FinishTag.INST});
    }

    public synchronized void fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        nextId = t[0].getInt();
        votes.clear();
        for (Tag e : t[1].getStruct()) {
            if (e.getType() == Tag.Type.STRUCT) {
                votes.add(Vote.fromTag(e));
            }
        }
        version++;
    }

    public synchronized void clear() {
        votes.clear();
        nextId = 1;
        version++;
    }

    private static Tag list(List<Vote> vs, boolean withActs) {
        Tag[] t = new Tag[vs.size() + 1];
        for (int i = 0; i < vs.size(); i++) {
            t[i] = vs.get(i).toTag(withActs);
        }
        t[vs.size()] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }
}
