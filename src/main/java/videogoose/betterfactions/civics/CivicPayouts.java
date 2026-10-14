package videogoose.betterfactions.civics;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.common.data.player.faction.FactionRoles;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.server.data.GameServerState;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.diplomacy.FactionTreaty;
import videogoose.betterfactions.diplomacy.FactionTreatySet;
import videogoose.betterfactions.diplomacy.TreatyTypeRegistry;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What civics pay out for. Most payouts are data, "on event X, if filters Y hold, pay Z faction points,
 * capped per day" ({@link Payout}), declared on the {@link CivicType} or in {@code civics.xml}; a
 * {@link CivicHandler} covers what data cannot say.
 *
 * <p>Every {@link CivicEvent} is about one faction, "us" ({@link CivicEvent#faction}); its payouts are
 * that faction's. Handlers hear an event when either side of it holds their civic.</p>
 */
public final class CivicPayouts {

    public static final long HOUR_MS = 60L * 60L * 1000L;
    public static final long DAY_MS = 24L * HOUR_MS;

    /** Something a civic can pay out for. */
    public enum Event {
        /** We killed a player. {@code player} is our killer, {@code victim} theirs. */
        PLAYER_KILLED,
        /** One of ours was killed by {@code other}'s player. {@code player} is our dead. */
        PLAYER_LOST,
        /** We destroyed a ship or station of {@code other}, at {@code sector}. */
        SHIP_DESTROYED,
        /** We claimed a system {@code other} held, or nobody did. */
        SYSTEM_CLAIMED,
        /** A system of ours went to {@code other}, or to nobody. */
        SYSTEM_LOST,
        /** We signed a treaty with {@code other}; {@code detail} is its type id. Wars and sanctions are not treaties here. */
        TREATY_SIGNED,
        /** A treaty with {@code other} ran its full term. */
        TREATY_KEPT,
        /** We walked out of a treaty with {@code other}. */
        TREATY_BROKEN,
        /** We declared war on {@code other}. */
        WAR_DECLARED,
        /** We joined our pact partner's war against {@code other}. */
        CALL_TO_ARMS_ANSWERED,
        /** We refused, or let lapse, a call to arms against {@code other}. */
        CALL_TO_ARMS_REFUSED,
        /** Once a day, for every faction. */
        DAILY
    }

    /** A condition on an event. A payout's filters must all hold. */
    public enum Filter {
        /** We are at war with {@code other}. */
        VICTIM_AT_WAR,
        /** {@code other} is a faction we are neither at war with nor hostile to by default, such as pirates. */
        VICTIM_NOT_AT_WAR,
        /** It happened in a system we own. */
        IN_OUR_TERRITORY,
        /** {@code player} holds our founder rank. */
        BY_LEADER,
        /** We are at war with {@code other} and hold a casus belli for the war's goals. */
        WITH_CASUS_BELLI,
        /** The victim joined their faction over a day ago and has not paid us out in the last hour. */
        FAIR_KILL,
        /** We hold no treaty beyond non-aggression; wars and sanctions do not count. */
        ONLY_NON_AGGRESSION
    }

    /** On {@code event}, if every filter holds, pay {@code fp} (negative to charge), at most {@code dailyCap} a day (0: no cap). */
    public record Payout(Event event, Set<Filter> where, int fp, int dailyCap) {
    }

    public record CivicEvent(Event type, int faction, int other, String player, String victim, Vector3i sector, String detail) {
        public static CivicEvent of(Event type, int faction, int other) {
            return new CivicEvent(type, faction, other, null, null, null, null);
        }
    }

    /** Code for what a civic's data cannot express. Hears every event either side of which holds the civic. */
    public interface CivicHandler {
        /** @param holder the faction holding the civic: {@code ev.faction()} or {@code ev.other()} */
        void on(CivicEvent ev, int holder, CivicPayouts pay);
    }

    private static final CivicPayouts SERVER = new CivicPayouts();

    /** Player name to when they joined their faction; since the server started. ponytail: lost on restart, so a restart makes everyone a veteran. */
    private final Map<String, Long> joined = new HashMap<>();
    /** Killer faction and victim name to when the kill last paid. */
    private final Map<String, Long> lastKill = new HashMap<>();

    public static CivicPayouts server() {
        return SERVER;
    }

    public synchronized void onJoinedFaction(String player, long now) {
        joined.put(player, now);
    }

    /**
     * Server: pays every payout {@code ev} earns its faction, then runs the handlers of both sides.
     */
    public void fire(CivicEvent ev) {
        long now = System.currentTimeMillis();
        FactionCivics c = Civics.server().of(ev.faction());
        for (String id : c.civics) {
            CivicType t = CivicRegistry.get(id);
            if (t == null) {
                continue;
            }
            List<Payout> payouts = t.getPayouts();
            for (int i = 0; i < payouts.size(); i++) {
                Payout p = payouts.get(i);
                if (p.event() == ev.type() && passes(ev, p.where(), now)) {
                    pay(ev.faction(), id + "#" + i, p.fp(), p.dailyCap());
                }
            }
        }
        for (int holder : new int[]{ev.faction(), ev.other()}) {
            if (holder == 0 || holder == ev.faction() && holder == ev.other()) {
                continue;
            }
            for (String id : Civics.server().of(holder).civics) {
                CivicType t = CivicRegistry.get(id);
                if (t != null && t.getHandler() != null) {
                    t.getHandler().on(ev, holder, this);
                }
            }
        }
        if (ev.type() == Event.PLAYER_KILLED && ev.victim() != null) {
            synchronized (this) {
                lastKill.put(ev.faction() + ":" + ev.victim(), now);
            }
        }
    }

    /** Server: credits {@code fp} under {@code key}'s daily cap; see {@link Civics#credit}. */
    public void pay(int faction, String key, int fp, int dailyCap) {
        Civics.server().credit(faction, key, fp, dailyCap, System.currentTimeMillis());
    }

    /** Whether every filter holds for {@code ev}. */
    public boolean passes(CivicEvent ev, Set<Filter> where, long now) {
        for (Filter f : where) {
            if (!holds(f, ev, now)) {
                return false;
            }
        }
        return true;
    }

    private boolean holds(Filter f, CivicEvent ev, long now) {
        DiplomacyManager dm = DiplomacyManager.server();
        int us = ev.faction();
        int them = ev.other();
        return switch (f) {
            case VICTIM_AT_WAR -> them != 0 && dm.hasTreaty(us, them, TreatyTypeRegistry.WAR);
            case VICTIM_NOT_AT_WAR -> them != 0 && them != us && !dm.hasTreaty(us, them, TreatyTypeRegistry.WAR)
                    && GameServerState.instance.getFactionManager().getRelation(us, them) != FactionRelation.RType.ENEMY;
            case IN_OUR_TERRITORY -> ev.sector() != null
                    && dm.systemOwner(VoidSystem.getPosFromSector(ev.sector(), new Vector3i())) == us;
            case BY_LEADER -> isLeader(us, ev.player());
            case WITH_CASUS_BELLI -> {
                Faction mine = GameServerState.instance.getFactionManager().getFaction(us);
                yield mine != null && them != 0 && dm.hasTreaty(us, them, TreatyTypeRegistry.WAR)
                        && dm.diplomacyOf(mine).getEntity(them).hasCasusBelli(dm.warGoals(us, them), null);
            }
            case FAIR_KILL -> fairKill(us, ev.victim(), now);
            case ONLY_NON_AGGRESSION -> onlyNonAggression(dm, us);
        };
    }

    synchronized boolean fairKill(int killerFaction, String victim, long now) {
        if (victim == null) {
            return false;
        }
        Long since = joined.get(victim);
        Long last = lastKill.get(killerFaction + ":" + victim);
        return (since == null || now - since >= DAY_MS) && (last == null || now - last >= HOUR_MS);
    }

    static boolean isLeader(int faction, String player) {
        Faction f = player == null ? null : GameServerState.instance.getFactionManager().getFaction(faction);
        FactionPermission p = f == null ? null : f.getMembersUID().get(player);
        return p != null && p.role == FactionRoles.INDEX_ADMIN_ROLE;
    }

    private static boolean onlyNonAggression(DiplomacyManager dm, int us) {
        synchronized (dm.getTreatyMap()) {
            for (FactionTreatySet set : dm.getTreatyMap().values()) {
                for (FactionTreaty t : set.getTreaties()) {
                    if (t.involves(us) && !TreatyTypeRegistry.NON_AGGRESSION.equals(t.typeId) && !TreatyTypeRegistry.WAR.equals(t.typeId)
                            && (t.getType() == null || !t.getType().isSanction())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // ---- handlers

    /** Holy Leader: whoever kills a holy leader gains a little, capped. The rest is its payouts. */
    static void holyLeader(CivicEvent ev, int holder, CivicPayouts pay) {
        if (ev.type() == Event.PLAYER_LOST && holder == ev.faction() && ev.other() != 0 && isLeader(holder, ev.player())) {
            pay.pay(ev.other(), CivicRegistry.HOLY_LEADER + ":slayer", 50, 150);
        }
    }

    /** Raiders: every faction we raid gains a casus belli against us, ready at once. */
    static void raiders(CivicEvent ev, int holder, CivicPayouts pay) {
        if (ev.type() != Event.SHIP_DESTROYED || holder != ev.faction() || !pay.passes(ev, EnumSet.of(Filter.VICTIM_NOT_AT_WAR), 0)) {
            return;
        }
        DiplomacyManager dm = DiplomacyManager.server();
        Faction victim = GameServerState.instance.getFactionManager().getFaction(ev.other());
        Faction raider = GameServerState.instance.getFactionManager().getFaction(holder);
        if (victim == null || raider == null) {
            return;
        }
        var ledger = dm.diplomacyOf(victim).getEntity(holder);
        long lasts = videogoose.betterfactions.BFConfig.claimLastsMs();
        boolean had = ledger.claimReadyIn(lasts) == 0;
        if (ledger.fabricateClaim(0, lasts) && !had) {
            dm.sendServerFactionMail(org.schema.schine.common.language.Lng.str("[FACTION] Raided by %s", raider.getName()),
                    org.schema.schine.common.language.Lng.str("%s destroyed one of our ships without a war. We hold a casus belli against them for %s hours.",
                            raider.getName(), lasts / HOUR_MS), victim);
        }
    }

    // ---- civics.xml

    /**
     * Applies a {@code civics.xml}: a {@code <Civic>} naming a registered id replaces its payouts; one
     * naming a new id registers a civic. Ids without a namespace go in {@code custom}. A broken civic is
     * skipped and reported; the rest still apply.
     *
     * @return one line per civic skipped
     */
    public static List<String> apply(String xml) {
        List<String> skipped = new ArrayList<>();
        CivicRegistry.resetData();
        if (xml == null || xml.isBlank()) {
            return skipped;
        }
        NodeList civics;
        try {
            civics = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).getDocumentElement().getElementsByTagName("Civic");
        } catch (Exception e) {
            skipped.add("civics.xml: " + e.getMessage());
            return skipped;
        }
        for (int i = 0; i < civics.getLength(); i++) {
            Element e = (Element) civics.item(i);
            String id = e.getAttribute("id").trim();
            try {
                applyOne(e, id.indexOf(':') >= 0 ? id : "custom:" + id);
            } catch (RuntimeException ex) {
                skipped.add(id + ": " + ex.getMessage());
            }
        }
        return skipped;
    }

    private static void applyOne(Element e, String id) {
        List<Payout> payouts = new ArrayList<>();
        for (Element p : children(e, "Payout")) {
            Set<Filter> where = EnumSet.noneOf(Filter.class);
            for (String w : p.getAttribute("where").split(",")) {
                if (!w.isBlank()) {
                    where.add(Filter.valueOf(w.trim().toUpperCase(Locale.ENGLISH)));
                }
            }
            String cap = p.getAttribute("dailyCap");
            payouts.add(new Payout(Event.valueOf(p.getAttribute("event").trim().toUpperCase(Locale.ENGLISH)), where,
                    Integer.parseInt(p.getAttribute("fp").trim()), cap.isBlank() ? 0 : Integer.parseInt(cap.trim())));
        }
        if (CivicRegistry.get(id) != null) {
            CivicRegistry.overridePayouts(id, payouts);
            return;
        }
        int colon = id.indexOf(':');
        CivicType.Builder b = new CivicType.Builder(id.substring(colon + 1));
        String name = e.getAttribute("name").trim();
        b.displayName(name.isEmpty() ? id.substring(colon + 1) : name);
        for (Element d : children(e, "Description")) {
            b.description(d.getTextContent().trim());
        }
        for (Element x : children(e, "Excludes")) {
            for (String ex : x.getTextContent().split(",")) {
                if (!ex.isBlank()) {
                    b.excludes(ex.trim());
                }
            }
        }
        for (Element r : children(e, "Requires")) {
            for (Element c : children(r, "Condition")) {
                String type = text(c, "Type").toUpperCase(Locale.ENGLISH);
                if (type.equals("HOLDS_CIVIC")) {
                    b.requires(text(c, "Argument"));
                } else {
                    String v = text(c, "Value");
                    b.unlock(CivicType.Unlock.Rule.valueOf(type), v.isEmpty() ? 0 : (int) Double.parseDouble(v));
                }
            }
        }
        for (Payout p : payouts) {
            b.payout(p.event(), p.fp(), p.dailyCap(), p.where().toArray(new Filter[0]));
        }
        CivicRegistry.registerData(id.substring(0, colon), b);
    }

    private static List<Element> children(Element e, String tag) {
        List<Element> out = new ArrayList<>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element c && c.getTagName().equalsIgnoreCase(tag)) {
                out.add(c);
            }
        }
        return out;
    }

    private static String text(Element e, String tag) {
        List<Element> c = children(e, tag);
        return c.isEmpty() ? "" : c.get(0).getTextContent().trim();
    }
}
