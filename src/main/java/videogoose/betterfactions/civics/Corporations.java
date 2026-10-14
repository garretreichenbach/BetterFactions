package videogoose.betterfactions.civics;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.database.DatabaseEntry;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.Perms;
import videogoose.betterfactions.assets.FactionAssets;
import videogoose.betterfactions.net.BFPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Corporations institution: companies the faction charters to run its trade. Each has a line of
 * business, a CEO (an NPC, or a member the Senate votes in), the faction's stations it runs, and an HQ.
 * A cut of every sale at its stations' shops goes from the shop to its treasury; each income turn it pays
 * a share of that turn's revenue to the faction as faction points.
 *
 * <p>Phase 1 of {@code docs/design/CORPORATIONS_PLAN.md}: own stations only. Franchises, contracts and
 * market share come later; until then the type names the line of business and its HQ bonus.</p>
 *
 * <p>Private: each faction's corporations go to its own members only.</p>
 */
public final class Corporations {

    private static final Corporations SERVER = new Corporations();
    private static final Corporations CLIENT = new Corporations();

    public static Corporations server() {
        return SERVER;
    }

    public static Corporations client() {
        return CLIENT;
    }

    public static final int MAX_CORPS = 6;
    public static final int MAX_HOLDINGS = 8;
    public static final int MAX_NAME = FactionCivics.MAX_SENATE_NAME;
    public static final int MAX_DESCRIPTION = 200;
    static final int HQ_RADIUS_SYSTEMS = 3;
    static final int HQ_BONUS_PERCENT = 10;
    static final long DAY_MS = 24L * 60L * 60L * 1000L;
    /** An HQ just seated or moved gives nothing for this long. */
    public static final long HQ_SETTLE_MS = 2 * DAY_MS;
    static final int CHAINS_DIVIDEND_PERCENT = 15;
    static final int START_RATE = 30;
    private static final long CHECK_MS = 30_000L;
    private static final String ACT = "corporation";

    public enum Type {
        //appended only: saves keep the ordinal
        LOGISTICS, CONSUMER_GOODS, RESOURCES, SHIPYARDS, SCRAPPERS, MERCENARIES;

        public String title() {
            return switch (this) {
                case LOGISTICS -> Lng.str("Logistics");
                case CONSUMER_GOODS -> Lng.str("Consumer Goods");
                case RESOURCES -> Lng.str("Resources");
                case SHIPYARDS -> Lng.str("Shipyards");
                case SCRAPPERS -> Lng.str("Scrappers");
                case MERCENARIES -> Lng.str("Mercenaries");
            };
        }

        /** The last word of a new corporation's name. */
        String suffix() {
            return switch (this) {
                case LOGISTICS -> "Logistics";
                case CONSUMER_GOODS -> "Goods";
                case RESOURCES -> "Mining";
                case SHIPYARDS -> "Shipworks";
                case SCRAPPERS -> "Salvage";
                case MERCENARIES -> "Security";
            };
        }

        public String description() {
            return switch (this) {
                case LOGISTICS -> Lng.str("Moves goods. Will earn delivery fees on trade-network orders and take haul contracts.");
                case CONSUMER_GOODS -> Lng.str("General retail: whatever its shops sell that no other line claims. Factions whose space hosts its shops will think better of us.");
                case RESOURCES -> Lng.str("Ores and raw materials. Will take mining quotas.");
                case SHIPYARDS -> Lng.str("Ships and ship parts, built at its stations' shipyards. Will take build orders.");
                case SCRAPPERS -> Lng.str("Salvage, and buying wrecks. Will take salvage jobs.");
                case MERCENARIES -> Lng.str("Fleets for hire. Will earn lease fees and take escort and bounty work.");
            };
        }

        /** What its HQ gives on top of the shared bonus; worded for later, when the systems behind it exist. */
        public String hqBonus() {
            return switch (this) {
                case LOGISTICS -> Lng.str("Orders shipped from the HQ will deliver faster.");
                case CONSUMER_GOODS -> Lng.str("Franchises near the HQ will cost less.");
                case RESOURCES -> Lng.str("Ore refined at the HQ will yield more.");
                case SHIPYARDS -> Lng.str("Shipyards at the HQ will build cheaper.");
                case SCRAPPERS -> Lng.str("Salvage sold at the HQ will pay more.");
                case MERCENARIES -> Lng.str("Leased fleets will refit at the HQ for free.");
            };
        }
    }

    /** One of the faction's stations. Stations do not move, so the sector is kept with it. */
    public static final class Holding {
        public String uid = "";
        public String name = "";
        public Vector3i sector = new Vector3i();

        public Holding() {
        }

        public Holding(String uid, String name, Vector3i sector) {
            this.uid = key(uid);
            this.name = name;
            this.sector = new Vector3i(sector);
        }

        Tag toTag() {
            return new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.STRING, null, uid), new Tag(Tag.Type.STRING, null, name),
                    new Tag(Tag.Type.VECTOR3i, null, sector), FinishTag.INST});
        }

        static Holding fromTag(Tag tag) {
            Tag[] t = tag.getStruct();
            if (t.length < 3 || t[0].getType() != Tag.Type.STRING) {
                return null;
            }
            Holding h = new Holding();
            h.uid = t[0].getString();
            h.name = t[1].getString();
            h.sector = t[2].getVector3i();
            return h;
        }
    }

    public static final class Corp {
        public String name = "";
        public Type type = Type.CONSUMER_GOODS;
        /** The member who runs it, or blank for its NPC CEO. */
        public String ceo = "";
        public String npcCeo = "";
        public long treasury;
        /** Percent of each turn's revenue paid to the faction. */
        public int rate = START_RATE;
        public final List<Holding> holdings = new ArrayList<>();
        public Holding hq;
        public long hqSince;
        /** This income turn's revenue so far, and the last whole turn's. */
        public long revenue;
        public long lastRevenue;
        public int lastDividend;
        /** What the faction says it does, or blank for its type's own words. */
        public String description = "";

        public String describe() {
            return description.isEmpty() ? type.description() : description;
        }

        public boolean memberCeo() {
            return !ceo.isEmpty();
        }

        public String ceoName() {
            return memberCeo() ? ceo : npcCeo;
        }

        public boolean hqActive(long now) {
            return hq != null && now - hqSince >= HQ_SETTLE_MS;
        }

        public Holding holding(String uid) {
            String k = key(uid);
            for (Holding h : holdings) {
                if (h.uid.equals(k)) {
                    return h;
                }
            }
            return null;
        }

        Tag toTag() {
            Tag[] h = new Tag[holdings.size() + 1];
            for (int i = 0; i < holdings.size(); i++) {
                h[i] = holdings.get(i).toTag();
            }
            h[holdings.size()] = FinishTag.INST;
            return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.STRING, null, name),
                    new Tag(Tag.Type.BYTE, null, (byte) type.ordinal()),
                    new Tag(Tag.Type.STRING, null, ceo),
                    new Tag(Tag.Type.STRING, null, npcCeo),
                    new Tag(Tag.Type.LONG, null, treasury),
                    new Tag(Tag.Type.INT, null, rate),
                    new Tag(Tag.Type.STRUCT, null, h),
                    hq != null ? hq.toTag() : new Tag(Tag.Type.STRUCT, null, new Tag[]{FinishTag.INST}),
                    new Tag(Tag.Type.LONG, null, hqSince),
                    new Tag(Tag.Type.LONG, null, revenue),
                    new Tag(Tag.Type.LONG, null, lastRevenue),
                    new Tag(Tag.Type.INT, null, lastDividend),
                    new Tag(Tag.Type.STRING, null, description),
                    FinishTag.INST});
        }

        static Corp fromTag(Tag tag) {
            Tag[] t = tag.getStruct();
            Corp c = new Corp();
            c.name = t[0].getString();
            int o = t[1].getByte();
            c.type = o >= 0 && o < Type.values().length ? Type.values()[o] : Type.CONSUMER_GOODS;
            c.ceo = t[2].getString();
            c.npcCeo = t[3].getString();
            c.treasury = t[4].getLong();
            c.rate = t[5].getInt();
            for (Tag e : t[6].getStruct()) {
                if (e.getType() == Tag.Type.STRUCT) {
                    Holding h = Holding.fromTag(e);
                    if (h != null) {
                        c.holdings.add(h);
                    }
                }
            }
            c.hq = Holding.fromTag(t[7]);
            c.hqSince = t[8].getLong();
            c.revenue = t[9].getLong();
            c.lastRevenue = t[10].getLong();
            c.lastDividend = t[11].getInt();
            //saved before descriptions could be written
            c.description = t.length > 12 && t[12].getType() == Tag.Type.STRING ? t[12].getString() : "";
            return c;
        }
    }

    public static final class State {
        public final List<Corp> corps = new ArrayList<>();
        /** Client only: the faction's stations and planets, to assign or seat an HQ at. Not saved. */
        public final List<Holding> stations = new ArrayList<>();

        /** The corporation running a station, or null. */
        public Corp runs(String uid) {
            for (Corp c : corps) {
                if (c.holding(uid) != null) {
                    return c;
                }
            }
            return null;
        }

        Tag toTag() {
            Tag[] c = new Tag[corps.size() + 1];
            for (int i = 0; i < corps.size(); i++) {
                c[i] = corps.get(i).toTag();
            }
            c[corps.size()] = FinishTag.INST;
            return new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.STRUCT, null, c), FinishTag.INST});
        }

        static State fromTag(Tag tag) {
            State st = new State();
            for (Tag e : tag.getStruct()[0].getStruct()) {
                if (e.getType() == Tag.Type.STRUCT) {
                    st.corps.add(Corp.fromTag(e));
                }
            }
            return st;
        }
    }

    private final Map<Integer, State> byFaction = new HashMap<>();
    private int version;
    private long nextCheck;
    /** Factions whose corporations earned since they were last sent. */
    private final java.util.Set<Integer> earned = new java.util.HashSet<>();

    // ---- rules, kept pure for Corporations_test

    /** Station uids with and without the engine's file prefix name the same station. */
    static String key(String uid) {
        return uid == null ? "" : DatabaseEntry.removePrefixWOException(uid);
    }

    public static int freeCharters(boolean chains) {
        return chains ? 2 : 1;
    }

    /** Corporations past the free charters; each costs one step of institution upkeep. */
    public static int extraCharters(int corps, boolean chains) {
        return Math.max(0, corps - freeCharters(chains));
    }

    /** A description as players may write one: one line, trimmed, at most {@link #MAX_DESCRIPTION} long. */
    static String cleanDescription(String s) {
        s = s == null ? "" : s.replaceAll("\\s+", " ").strip();
        return s.length() > MAX_DESCRIPTION ? s.substring(0, MAX_DESCRIPTION).strip() : s;
    }

    /** Whether two sectors are within the HQ's reach, counted in whole systems each way. */
    public static boolean near(Vector3i a, Vector3i b) {
        return Math.abs(Math.floorDiv(a.x, VoidSystem.SYSTEM_SIZE) - Math.floorDiv(b.x, VoidSystem.SYSTEM_SIZE)) <= HQ_RADIUS_SYSTEMS
                && Math.abs(Math.floorDiv(a.y, VoidSystem.SYSTEM_SIZE) - Math.floorDiv(b.y, VoidSystem.SYSTEM_SIZE)) <= HQ_RADIUS_SYSTEMS
                && Math.abs(Math.floorDiv(a.z, VoidSystem.SYSTEM_SIZE) - Math.floorDiv(b.z, VoidSystem.SYSTEM_SIZE)) <= HQ_RADIUS_SYSTEMS;
    }

    /** What a sale at a holding earns its corporation: the cut, taken from the shop, plus the HQ's bonus on top. */
    static long[] earn(long sale, int cutPercent, boolean nearHq) {
        long cut = sale * cutPercent / 100;
        return new long[]{cut, nearHq ? cut * HQ_BONUS_PERCENT / 100 : 0};
    }

    /**
     * A turn's dividend: {@code rate} percent of the revenue, out of the treasury, at {@code creditsPerFp},
     * Corporate Chains adding its share, capped. @return {credits paid out, faction points}
     */
    static long[] dividend(long revenue, int rate, long treasury, int creditsPerFp, int capFp, boolean chains) {
        int mult = 100 + (chains ? CHAINS_DIVIDEND_PERCENT : 0);
        long pay = Math.min(treasury, Math.max(0, revenue) * rate / 100);
        long fp = pay / Math.max(1, creditsPerFp) * mult / 100;
        if (capFp >= 0 && fp > capFp) {
            fp = capFp;
            pay = Math.min(pay, (long) capFp * 100 / mult * Math.max(1, creditsPerFp));
        }
        return new long[]{pay, fp};
    }

    /** The corporation whose HQ is in this sector, any faction's, other than {@code except}; or null. */
    synchronized Corp hqIn(Vector3i sector, Corp except) {
        for (State st : byFaction.values()) {
            for (Corp c : st.corps) {
                if (c != except && c.hq != null && c.hq.sector.equals(sector)) {
                    return c;
                }
            }
        }
        return null;
    }

    private static boolean chains(int faction, Civics civics) {
        return civics.of(faction).civics.contains(CivicRegistry.CORPORATE_CHAINS);
    }

    // ---- shared

    public synchronized State of(int faction) {
        return byFaction.get(faction);
    }

    public synchronized int version() {
        return version;
    }

    /** Charters past the free ones, which the faction pays upkeep for. */
    public int extraCharters(int faction) {
        boolean chains = chains(faction, this == CLIENT ? Civics.client() : Civics.server());
        synchronized (this) {
            State st = byFaction.get(faction);
            return st == null ? 0 : extraCharters(st.corps.size(), chains);
        }
    }

    // ---- client

    public synchronized void received(Tag tag) {
        Tag[] t = tag.getStruct();
        byFaction.clear();
        if (t.length > 2 && t[1].getType() == Tag.Type.STRUCT && t[1].getStruct().length > 1) {
            State st = State.fromTag(t[1]);
            for (Tag e : t[2].getStruct()) {
                if (e.getType() == Tag.Type.STRUCT) {
                    Holding h = Holding.fromTag(e);
                    if (h != null) {
                        st.stations.add(h);
                    }
                }
            }
            byFaction.put(t[0].getInt(), st);
        }
        version++;
    }

    /**
     * Client. {@code op}: 0 charter (value: type), 1 dissolve, 2 rename (text), 3 dividend rate (value),
     * 4 assign a station (text: uid), 5 release one, 6 seat the HQ (text: uid), 7 appoint a CEO (text: member,
     * blank for a new NPC; by Senate vote when the faction has one), 8 describe (text, blank for the type's own words), 9 rename its NPC CEO (text).
     */
    public static void request(int op, int corp, int value, String text) {
        BFPacket.toServer(BFPacket.Kind.CORP_ACTION, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) op), new Tag(Tag.Type.INT, null, corp), new Tag(Tag.Type.INT, null, value),
                new Tag(Tag.Type.STRING, null, text == null ? "" : text), new Tag(Tag.Type.STRING, null, ""), FinishTag.INST}));
    }

    /** Client: put a CEO appointment to the Senate, with a reason. */
    public static void requestAppoint(int corp, String player, String reason) {
        BFPacket.toServer(BFPacket.Kind.CORP_ACTION, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) 7), new Tag(Tag.Type.INT, null, corp), new Tag(Tag.Type.INT, null, 0),
                new Tag(Tag.Type.STRING, null, player), new Tag(Tag.Type.STRING, null, reason == null ? "" : reason), FinishTag.INST}));
    }

    // ---- server: the clock

    /** Server: holdings and HQs checked against what the faction still owns, CEOs against who is still active. */
    public void update(long now) {
        if (now < nextCheck) {
            return;
        }
        nextCheck = now + CHECK_MS;
        List<Runnable> after = new ArrayList<>();
        List<Faction> dirty = new ArrayList<>();
        for (Faction f : GameServerState.instance.getFactionManager().getFactionCollection()) {
            int id = f.getIdFaction();
            if (f.isNPC() || !Civics.server().holds(id, CivicRegistry.CORPORATIONS)) {
                continue;
            }
            FactionAssets assets = assets(id);
            synchronized (this) {
                State st = byFaction.get(id);
                boolean changed = earned.remove(id);
                if (st != null && tick(f, st, assets, after)) {
                    version++;
                    changed = true;
                }
                if (changed) {
                    dirty.add(f);
                }
            }
        }
        after.forEach(Runnable::run);
        dirty.forEach(this::send);
    }

    private boolean tick(Faction f, State st, FactionAssets assets, List<Runnable> after) {
        boolean changed = false;
        for (Corp c : st.corps) {
            if (c.memberCeo() && !Council.activeMember(f, c.ceo)) {
                String gone = c.ceo;
                c.ceo = "";
                after.add(() -> Civics.governanceNews(f, c.name, Lng.str("%s steps down", gone),
                        Lng.str("%s is no longer an active member. %s runs %s again.", gone, c.npcCeo, c.name)));
                changed = true;
            }
            //an unanswered or cut-short asset query is no proof a station is gone
            if (assets == null || assets.truncated) {
                continue;
            }
            for (Holding h : new ArrayList<>(c.holdings)) {
                if (station(assets, h.uid) == null) {
                    c.holdings.remove(h);
                    after.add(() -> Civics.governanceNews(f, c.name, Lng.str("%s loses %s", c.name, h.name),
                            Lng.str("%s is no longer ours, so %s no longer runs it.", h.name, c.name)));
                    changed = true;
                }
            }
            if (c.hq != null && station(assets, c.hq.uid) == null) {
                String gone = c.hq.name;
                c.hq = null;
                after.add(() -> Civics.governanceNews(f, c.name, Lng.str("%s loses its HQ", c.name),
                        Lng.str("%s is no longer ours. %s has no HQ until a new one is seated.", gone, c.name)));
                changed = true;
            }
        }
        return changed;
    }

    private static FactionAssets assets(int faction) {
        BetterFactions bf = BetterFactions.getInstance();
        return bf == null || bf.getAssetIndex() == null ? null : bf.getAssetIndex().get(faction);
    }

    /** A station or planet the faction owns, by uid. */
    private static FactionAssets.Asset station(FactionAssets assets, String uid) {
        String k = key(uid);
        for (FactionAssets.Asset a : assets.assets) {
            if (holdable(a.type) && key(a.uid).equals(k)) {
                return a;
            }
        }
        return null;
    }

    static boolean holdable(int type) {
        return type == SimpleTransformableSendableObject.EntityType.SPACE_STATION.dbTypeId || FactionAssets.isPlanet(type);
    }

    // ---- server: money

    /**
     * Server: a player paid {@code credits} into a shop on a station or planet of {@code faction}, named by
     * its uid ({@link FactionAssets#planetUid} for a planet). @return the cut its
     * corporation takes from the shop, or 0 if no corporation runs it
     */
    public long onSale(int faction, String stationUid, long credits) {
        if (credits <= 0) {
            return 0;
        }
        synchronized (this) {
            State st = byFaction.get(faction);
            Corp c = st == null ? null : st.runs(stationUid);
            if (c == null) {
                return 0;
            }
            Holding h = c.holding(stationUid);
            long[] e = earn(credits, BFConfig.corporationCutPercent(), c.hqActive(System.currentTimeMillis()) && near(h.sector, c.hq.sector));
            c.treasury += e[0] + e[1];
            c.revenue += e[0] + e[1];
            earned.add(faction);
            return e[0];
        }
    }

    /** Server, each income turn: every corporation pays its dividend. @return the faction points paid in all */
    public float payDividends(int faction) {
        boolean chains = chains(faction, Civics.server());
        long fp = 0;
        synchronized (this) {
            State st = byFaction.get(faction);
            if (st == null || !Civics.server().holds(faction, CivicRegistry.CORPORATIONS)) {
                return 0;
            }
            for (Corp c : st.corps) {
                long[] d = dividend(c.revenue, c.rate, c.treasury, BFConfig.corporationCreditsPerPoint(), BFConfig.corporationDividendCap(), chains);
                c.treasury -= d[0];
                c.lastRevenue = c.revenue;
                c.revenue = 0;
                c.lastDividend = (int) d[1];
                fp += d[1];
            }
            version++;
            earned.add(faction);
        }
        return fp;
    }

    // ---- server: what members do

    public void action(PlayerState p, Tag tag) {
        Tag[] t = tag.getStruct();
        if (t.length < 4 || t[0].getType() != Tag.Type.BYTE || t[1].getType() != Tag.Type.INT || t[3].getType() != Tag.Type.STRING) {
            return;
        }
        Faction f = GameServerState.instance.getFactionManager().getFaction(p.getFactionId());
        if (f == null || !Civics.server().holds(f.getIdFaction(), CivicRegistry.CORPORATIONS)) {
            return;
        }
        int op = t[0].getByte();
        int i = t[1].getInt();
        int value = t[2].getInt();
        String text = t[3].getString();
        String reason = t.length > 4 && t[4].getType() == Tag.Type.STRING ? t[4].getString() : "";
        boolean editor = Perms.has(f, p.getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION);
        int id = f.getIdFaction();
        FactionAssets assets = op == 4 || op == 6 ? assets(id) : null;
        List<Runnable> after = new ArrayList<>();
        long now = System.currentTimeMillis();
        synchronized (this) {
            State st = byFaction.computeIfAbsent(id, k -> new State());
            Corp c = op == 0 || i < 0 || i >= st.corps.size() ? null : st.corps.get(i);
            if (op != 0 && c == null) {
                return;
            }
            boolean runs = editor || c != null && c.ceo.equalsIgnoreCase(p.getName());
            switch (op) {
                case 0 -> {
                    if (!editor) {
                        reject(p, Lng.str("Your rank may not charter corporations."));
                        return;
                    }
                    if (st.corps.size() >= MAX_CORPS || value < 0 || value >= Type.values().length) {
                        reject(p, Lng.str("The faction has chartered all the corporations it can."));
                        return;
                    }
                    Corp n = new Corp();
                    n.type = Type.values()[value];
                    n.npcCeo = CouncilLore.name(CouncilLore.Flavour.BOARD);
                    n.name = CouncilLore.name(CouncilLore.Flavour.BOARD).split(" ")[0] + " " + n.type.suffix();
                    st.corps.add(n);
                    boolean extra = extraCharters(st.corps.size(), chains(id, Civics.server())) > 0;
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s is chartered", n.name),
                            extra ? Lng.str("%s chartered %s, a %s corporation run by %s. It costs %s faction points of upkeep an income turn.",
                                    p.getName(), n.name, n.type.title(), n.npcCeo, BFConfig.institutionUpkeep())
                                    : Lng.str("%s chartered %s, a %s corporation run by %s.", p.getName(), n.name, n.type.title(), n.npcCeo)));
                }
                case 1 -> {
                    if (!editor) {
                        reject(p, Lng.str("Your rank may not dissolve corporations."));
                        return;
                    }
                    st.corps.remove(c);
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s is dissolved", c.name),
                            Lng.str("%s dissolved %s. Its stations are the faction's own again.", p.getName(), c.name)));
                }
                case 2 -> {
                    String name = FactionCivics.cleanSenateName(text);
                    if (!runs) {
                        reject(p, Lng.str("Only its CEO or your faction's officers may rename it."));
                        return;
                    }
                    if (name.isEmpty() || name.equals(c.name)) {
                        return;
                    }
                    String old = c.name;
                    c.name = name;
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s is now %s", old, name),
                            Lng.str("%s renamed %s to %s.", p.getName(), old, name)));
                }
                case 9 -> {
                    String name = FactionCivics.cleanSenateName(text);
                    if (!runs) {
                        reject(p, Lng.str("Only its CEO or your faction's officers may rename its NPC CEO."));
                        return;
                    }
                    if (name.isEmpty() || name.equals(c.npcCeo)) {
                        return;
                    }
                    String old = c.npcCeo;
                    c.npcCeo = name;
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s is now %s", old, name),
                            Lng.str("%s renamed %s's NPC CEO, %s, to %s.", p.getName(), c.name, old, name)));
                }
                case 8 -> {
                    String d = cleanDescription(text);
                    if (!runs) {
                        reject(p, Lng.str("Only its CEO or your faction's officers may describe it."));
                        return;
                    }
                    if (d.equals(c.description)) {
                        return;
                    }
                    c.description = d;
                }
                case 3 -> {
                    if (!runs) {
                        reject(p, Lng.str("Only its CEO or your faction's officers may set its dividend."));
                        return;
                    }
                    c.rate = Math.max(0, Math.min(100, value));
                }
                case 4 -> {
                    FactionAssets.Asset a = assets == null ? null : station(assets, text);
                    if (!runs) {
                        reject(p, Lng.str("Only its CEO or your faction's officers may give it stations."));
                        return;
                    }
                    if (a == null || st.runs(text) != null || c.holdings.size() >= MAX_HOLDINGS) {
                        reject(p, Lng.str("That is not ours, another corporation runs it, or this one runs all it can."));
                        return;
                    }
                    Holding h = new Holding(a.uid, a.name, a.sector);
                    c.holdings.add(h);
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s takes over %s", c.name, h.name),
                            Lng.str("%s now runs the shops on %s and takes %s%% of their sales.", c.name, h.name, BFConfig.corporationCutPercent())));
                }
                case 5 -> {
                    Holding h = c.holding(text);
                    if (!runs) {
                        reject(p, Lng.str("Only its CEO or your faction's officers may release its stations."));
                        return;
                    }
                    if (h == null) {
                        return;
                    }
                    c.holdings.remove(h);
                    //its HQ is one of the stations it runs, so it goes with it
                    if (c.hq != null && c.hq.uid.equals(h.uid)) {
                        c.hq = null;
                        after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s gives up its HQ", c.name),
                                Lng.str("%s released %s, and with it the HQ of %s.", p.getName(), h.name, c.name)));
                    }
                }
                case 6 -> {
                    FactionAssets.Asset a = assets == null ? null : station(assets, text);
                    if (!runs) {
                        reject(p, Lng.str("Only its CEO or your faction's officers may seat its HQ."));
                        return;
                    }
                    if (a == null) {
                        reject(p, Lng.str("That station or planet is not ours."));
                        return;
                    }
                    if (c.hq != null && c.hq.uid.equals(key(a.uid))) {
                        return;
                    }
                    Corp there = hqIn(a.sector, c);
                    if (there != null) {
                        reject(p, Lng.str("Sector %s already holds the HQ of %s. Only one corporate HQ fits in a sector.", a.sector, there.name));
                        return;
                    }
                    //the HQ is one of the stations it runs, taken on if it is not yet
                    Corp other = st.runs(a.uid);
                    if (other != null && other != c) {
                        reject(p, Lng.str("%s runs %s. It must release it before %s can seat its HQ there.", other.name, a.name, c.name));
                        return;
                    }
                    boolean taken = other == null;
                    if (taken && c.holdings.size() >= MAX_HOLDINGS) {
                        reject(p, Lng.str("%s runs all it can. Release one first: its HQ is one of them.", c.name));
                        return;
                    }
                    if (taken) {
                        c.holdings.add(new Holding(a.uid, a.name, a.sector));
                    }
                    c.hq = new Holding(a.uid, a.name, a.sector);
                    c.hqSince = now;
                    after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s seats its HQ at %s", c.name, a.name),
                            taken ? Lng.str("%s now runs %s too. Its HQ gives nothing for the first %s days while it settles in.", c.name, a.name, HQ_SETTLE_MS / DAY_MS)
                                    : Lng.str("Its HQ gives nothing for the first %s days while it settles in.", HQ_SETTLE_MS / DAY_MS)));
                }
                case 7 -> {
                    if (text.isEmpty()) {
                        if (!editor || !c.memberCeo()) {
                            reject(p, Lng.str("Only your faction's officers may hand a corporation back to an NPC CEO."));
                            return;
                        }
                        String gone = c.ceo;
                        c.ceo = "";
                        after.add(() -> Civics.governanceNews(f, p.getName(), Lng.str("%s steps down", gone),
                                Lng.str("%s removed %s as CEO of %s. %s runs it again.", p.getName(), gone, c.name, c.npcCeo)));
                    } else {
                        String why = problem(f, st, i, text);
                        if (why != null) {
                            reject(p, why);
                            return;
                        }
                        if (Civics.server().holds(id, CivicRegistry.SENATE)) {
                            if (!Council.activeMember(f, p.getName())) {
                                return;
                            }
                            Tag act = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.STRING, null, ACT),
                                    new Tag(Tag.Type.INT, null, i), new Tag(Tag.Type.STRING, null, c.name), new Tag(Tag.Type.STRING, null, text), FinishTag.INST});
                            String description = Lng.str("Appoint %s CEO of %s", text, c.name);
                            //nothing changes until the vote carries
                            after.add(() -> Senate.server().open(f, p.getName(), description, reason, false, act, v -> carry(id, v.act)));
                        } else if (!editor) {
                            reject(p, Lng.str("Your rank may not appoint a CEO."));
                            return;
                        } else {
                            appoint(f, c, text, after);
                        }
                    }
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

    /** Why the member cannot be made CEO, or null. Checked when proposed, and again when carried. */
    private static String problem(Faction f, State st, int i, String player) {
        if (i < 0 || i >= st.corps.size()) {
            return Lng.str("That corporation is gone.");
        }
        if (!Council.activeMember(f, player)) {
            return Lng.str("%s is not an active member.", player);
        }
        for (Corp c : st.corps) {
            if (c.ceo.equalsIgnoreCase(player)) {
                return Lng.str("%s already runs %s.", player, c.name);
            }
        }
        return null;
    }

    private static void appoint(Faction f, Corp c, String player, List<Runnable> after) {
        c.ceo = player;
        after.add(() -> Civics.governanceNews(f, c.name, Lng.str("%s is CEO of %s", player, c.name),
                Lng.str("%s takes over from %s. They set its dividend and give it stations.", player, c.npcCeo)));
    }

    /** Whether a Senate vote's act is a CEO appointment. */
    public static boolean isAct(Tag act) {
        Tag[] t = act == null ? null : act.getStruct();
        return t != null && t.length > 0 && t[0].getType() == Tag.Type.STRING && ACT.equals(t[0].getString());
    }

    /** Server: a Senate vote to appoint a CEO passed. */
    public void carry(int faction, Tag act) {
        Tag[] t = act.getStruct();
        Faction f = GameServerState.instance.getFactionManager().getFaction(faction);
        if (f == null || t.length < 4) {
            return;
        }
        int i = t[1].getInt();
        String corpName = t[2].getString();
        String player = t[3].getString();
        List<Runnable> after = new ArrayList<>();
        synchronized (this) {
            State st = byFaction.get(faction);
            //the corporation may have been dissolved, or another taken its place, while the vote ran
            String why = st == null || i < 0 || i >= st.corps.size() || !st.corps.get(i).name.equals(corpName)
                    ? Lng.str("%s is gone.", corpName) : problem(f, st, i, player);
            if (why != null) {
                after.add(() -> Civics.governanceNews(f, corpName, Lng.str("Could not be carried out: a CEO appointment"), why));
            } else {
                appoint(f, st.corps.get(i), player, after);
                version++;
            }
        }
        after.forEach(Runnable::run);
        send(f);
    }

    /** Server: the faction dropped the institution or is gone. Treasuries go with it. */
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

    private static void reject(PlayerState p, String why) {
        p.sendServerMessage(new ServerMessage(new Object[]{why}, ServerMessage.MESSAGE_TYPE_ERROR, p.getId()));
    }

    // ---- sync: each faction's corporations to its own members, with its stations to choose from

    public void send(Faction f) {
        for (PlayerState p : GameServerState.instance.getPlayerStatesByName().values()) {
            if (p.getFactionId() == f.getIdFaction() && p.getClientChannel() != null) {
                sendTo(p);
            }
        }
    }

    public void sendTo(PlayerState p) {
        int id = p.getFactionId();
        FactionAssets assets = id == 0 ? null : assets(id);
        List<Tag> stations = new ArrayList<>();
        if (assets != null) {
            for (FactionAssets.Asset a : assets.assets) {
                if (holdable(a.type)) {
                    stations.add(new Holding(a.uid, a.name, a.sector).toTag());
                }
            }
        }
        stations.add(FinishTag.INST);
        Tag tag;
        synchronized (this) {
            State st = byFaction.get(id);
            tag = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.INT, null, id),
                    st != null ? st.toTag() : new Tag(Tag.Type.STRUCT, null, new Tag[]{FinishTag.INST}),
                    new Tag(Tag.Type.STRUCT, null, stations.toArray(new Tag[0])), FinishTag.INST});
        }
        BFPacket.toPlayer(p, BFPacket.Kind.CORPORATIONS, tag);
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
        earned.clear();
        version++;
    }
}
