package videogoose.betterfactions.civics;

import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionNewsCategory;
import org.schema.game.common.data.player.faction.FactionNewsPost;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.common.data.player.faction.FactionRoles;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.Perms;
import videogoose.betterfactions.net.BFPacket;

import java.util.HashMap;
import java.util.Map;

/**
 * Every faction's {@link FactionCivics}. One instance per side; the server's is saved by {@code BFStore}.
 *
 * <p>Clients get every faction's civics, not only their own: the names are public (other factions see
 * them on the diplomacy tab) and nothing else held here is secret yet. ponytail: when per-civic private
 * data arrives (directives, phase 4), send that part to members only.</p>
 */
public final class Civics {

    private static final Civics SERVER = new Civics();
    private static final Civics CLIENT = new Civics();
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private final Map<Integer, FactionCivics> byFaction = new HashMap<>();
    private int version;

    public static Civics server() {
        return SERVER;
    }

    public static Civics client() {
        return CLIENT;
    }

    /** Never null: a faction that has picked nothing gets an empty, unsaved record. */
    public synchronized FactionCivics of(int faction) {
        FactionCivics c = byFaction.get(faction);
        return c != null ? c : new FactionCivics();
    }

    /** Ids of the factions holding the civic {@code id}. */
    public synchronized java.util.List<Integer> factionsWith(String id) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        byFaction.forEach((f, c) -> {
            if (c.civics.contains(id)) {
                out.add(f);
            }
        });
        return out;
    }

    /** Bumped on every change, so a client list can tell when to redraw. */
    public synchronized int version() {
        return version;
    }

    public synchronized void clear() {
        byFaction.clear();
        version++;
    }

    public synchronized void onFactionDeleted(int faction) {
        byFaction.remove(faction);
        version++;
    }

    /**
     * Server: a member asks to put a civic in a slot. Everything is checked here; the chooser's greying
     * out is courtesy.
     */
    public void pick(PlayerState p, Tag tag) {
        Tag[] t = tag.getStruct();
        if (t.length < 2 || t[0].getType() != Tag.Type.INT || t[1].getType() != Tag.Type.STRING) {
            return;
        }
        int slot = t[0].getInt();
        String id = t[1].getString();
        short resource = t.length > 2 && t[2].getType() == Tag.Type.SHORT ? t[2].getShort() : 0;
        CivicType type = CivicRegistry.get(id);
        if (type != null && type.choosesResource() && !isResource(resource)) {
            reject(p, Lng.astr("Choose a resource for that civic."));
            return;
        }
        Faction f = GameServerState.instance.getFactionManager().getFaction(p.getFactionId());
        if (f == null) {
            reject(p, Lng.astr("You are not in a faction."));
            return;
        }
        if (!Perms.has(f, p.getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION)) {
            reject(p, Lng.astr("Your rank may not choose civics."));
            return;
        }
        long now = System.currentTimeMillis();
        int swapCost = Council.server().cost(f.getIdFaction(), Council.REFORM, BFConfig.civicSwapCost());
        int rushCost = BFConfig.civicRushCost();
        long cooldownMs = BFConfig.civicSwapCooldownDays() * DAY_MS;
        Tag update;
        String replaced;
        synchronized (this) {
            FactionCivics c = byFaction.computeIfAbsent(f.getIdFaction(), k -> new FactionCivics());
            String why = c.problem(slot, id, f.factionPoints, now, swapCost, rushCost, cooldownMs);
            if (why != null) {
                reject(p, new Object[]{why});
                return;
            }
            int cost = c.cost(slot, now, swapCost, rushCost, cooldownMs);
            if (cost > 0) {
                f.factionPoints -= cost;
                f.sendFactionPointUpdate(GameServerState.instance.getGameState());
            }
            replaced = c.at(slot);
            c.set(slot, id, resource, now);
            version++;
            update = entries(Map.of(f.getIdFaction(), c));
        }
        String name = CivicRegistry.nameOf(id);
        boolean doctrine = slot == FactionCivics.DOCTRINE;
        governanceNews(f, p.getName(), doctrine ? Lng.str("New fleet doctrine: %s", name) : Lng.str("New civic: %s", name),
                replaced == null ? Lng.str("%s chose %s.", p.getName(), name)
                        : Lng.str("%s replaced %s with %s.", p.getName(), CivicRegistry.nameOf(replaced), name));
        broadcast(update);
    }

    /**
     * Server: a member adopts or drops an institution. Free either way; what it costs is upkeep, charged
     * on the income turn ({@link FactionCivics#institutionUpkeep}).
     */
    public void setInstitution(PlayerState p, Tag tag) {
        Tag[] t = tag.getStruct();
        if (t.length < 2 || t[0].getType() != Tag.Type.STRING || t[1].getType() != Tag.Type.BYTE) {
            return;
        }
        String id = t[0].getString();
        boolean adopt = t[1].getByte() != 0;
        Faction f = GameServerState.instance.getFactionManager().getFaction(p.getFactionId());
        if (f == null) {
            reject(p, Lng.astr("You are not in a faction."));
            return;
        }
        if (!Perms.has(f, p.getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION)) {
            reject(p, Lng.astr("Your rank may not change institutions."));
            return;
        }
        CivicType type = CivicRegistry.get(id);
        if (adopt && (type == null || type.getKind() != CivicType.Kind.INSTITUTION)) {
            reject(p, Lng.astr("There is no such institution."));
            return;
        }
        Tag update;
        synchronized (this) {
            FactionCivics c = byFaction.computeIfAbsent(f.getIdFaction(), k -> new FactionCivics());
            if (adopt == c.institutions.contains(id)) {
                return;
            }
            if (adopt) {
                c.institutions.add(id);
            } else {
                c.institutions.remove(id);
            }
            version++;
            update = entries(Map.of(f.getIdFaction(), c));
        }
        if (!adopt && CivicRegistry.SENATE.equals(id)) {
            Senate.server().onDissolved(f.getIdFaction());
        }
        if (!adopt && CivicRegistry.COUNCIL.equals(id)) {
            Council.server().onDissolved(f.getIdFaction());
        }
        String name = CivicRegistry.nameOf(id);
        governanceNews(f, p.getName(), adopt ? Lng.str("The %s is founded", name) : Lng.str("The %s is dissolved", name),
                adopt ? Lng.str("%s adopted the %s. %s", p.getName(), name, type.getDescription())
                        : Lng.str("%s dissolved the %s.", p.getName(), name));
        broadcast(update);
    }

    /**
     * Server: who sits in the Senate and how long its votes stay open. Votes already open keep the
     * rules they opened under, except that seats are counted afresh at every tally.
     */
    public void setSenateRules(PlayerState p, Tag tag) {
        Tag[] t = tag.getStruct();
        if (t.length < 2 || t[0].getType() != Tag.Type.INT || t[1].getType() != Tag.Type.INT) {
            return;
        }
        Faction f = GameServerState.instance.getFactionManager().getFaction(p.getFactionId());
        if (f == null || !holds(f.getIdFaction(), CivicRegistry.SENATE)) {
            reject(p, Lng.astr("Your faction has no Senate."));
            return;
        }
        if (!Perms.has(f, p.getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION)) {
            reject(p, Lng.astr("Your rank may not change the Senate's rules."));
            return;
        }
        int seats = seatMask(t[0].getInt());
        int hours = t[1].getInt() <= 0 ? 0 : Math.min(t[1].getInt(), BFConfig.senateMaxVoteHours());
        Tag update;
        boolean veto;
        String oldName;
        String name;
        boolean rules;
        synchronized (this) {
            FactionCivics c = byFaction.computeIfAbsent(f.getIdFaction(), k -> new FactionCivics());
            veto = t.length > 2 && t[2].getType() == Tag.Type.BYTE ? t[2].getByte() != 0 : c.senateVeto;
            String sent = t.length > 3 && t[3].getType() == Tag.Type.STRING ? FactionCivics.cleanSenateName(t[3].getString()) : c.senateName;
            oldName = c.senateName();
            rules = c.senateSeats != seats || c.senateHours != hours || c.senateVeto != veto;
            if (!rules && c.senateName.equals(sent)) {
                return;
            }
            c.senateSeats = seats;
            c.senateHours = hours;
            c.senateVeto = veto;
            c.senateName = sent;
            name = c.senateName();
            version++;
            update = entries(Map.of(f.getIdFaction(), c));
        }
        if (!name.equals(oldName)) {
            governanceNews(f, p.getName(), Lng.str("The %s is renamed", oldName),
                    Lng.str("%s renamed the %s to the %s.", p.getName(), oldName, name));
        }
        if (rules) {
            governanceNews(f, p.getName(), Lng.str("The %s's rules are changed", name),
                    Lng.str("%s set who sits to %s, votes to stay open %s hours, and the leader's veto %s.", p.getName(),
                            seats == 0 ? Lng.str("every active member") : Lng.str("chosen ranks"),
                            hours > 0 ? hours : BFConfig.senateVoteHours(), veto ? Lng.str("on") : Lng.str("off")));
        }
        broadcast(update);
        //open votes are counted under the new seats at their next ballot or their close
        Senate.server().send(f);
    }

    /** A seat mask as sent, made valid: rank bits only, and the founder always sits unless everyone does. */
    static int seatMask(int mask) {
        mask &= (1 << FactionRoles.MAX_ROLES) - 1;
        return mask == 0 ? 0 : mask | (1 << FactionRoles.INDEX_ADMIN_ROLE);
    }

    /** Client: asks to change the Senate's rules. */
    public static void requestSenateRules(int seats, int hours, boolean veto, String name) {
        BFPacket.toServer(BFPacket.Kind.SENATE_RULES, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.INT, null, seats), new Tag(Tag.Type.INT, null, hours),
                new Tag(Tag.Type.BYTE, null, (byte) (veto ? 1 : 0)), new Tag(Tag.Type.STRING, null, name), FinishTag.INST}));
    }

    /** Client: asks to adopt or drop an institution. */
    public static void requestInstitution(String id, boolean adopt) {
        BFPacket.toServer(BFPacket.Kind.INSTITUTION_SET, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.STRING, null, id), new Tag(Tag.Type.BYTE, null, (byte) (adopt ? 1 : 0)), FinishTag.INST}));
    }

    public synchronized boolean holds(int faction, String id) {
        FactionCivics c = byFaction.get(faction);
        return c != null && c.institutions.contains(id);
    }

    /** Server: a post in the faction's own news feed about how it governs itself. */
    public static void governanceNews(Faction f, String author, String title, String body) {
        FactionNewsPost post = new FactionNewsPost();
        post.set(f.getIdFaction(), author, System.currentTimeMillis(), title, body, 0);
        //ponytail: the engine's categories have no governance; GENERAL until one is added there
        post.setCategory(FactionNewsCategory.GENERAL);
        GameServerState.instance.getFactionManager().addNewsPostServer(post);
    }

    private static void broadcast(Tag update) {
        for (PlayerState other : GameServerState.instance.getPlayerStatesByName().values()) {
            if (other.getClientChannel() != null) {
                BFPacket.toPlayer(other, BFPacket.Kind.CIVICS, update);
            }
        }
    }

    /** Whether {@code type} is one of the engine's raw resources, modded ones included. */
    public static boolean isResource(short type) {
        if (type <= 0) {
            return false;
        }
        for (short r : ElementKeyMap.resources) {
            if (r == type) {
                return true;
            }
        }
        return false;
    }

    private static void reject(PlayerState p, Object[] message) {
        p.sendServerMessage(new ServerMessage(message, ServerMessage.MESSAGE_TYPE_ERROR, p.getId()));
    }

    /** Client: sends a pick; the answer comes back as a {@link BFPacket.Kind#CIVICS} update. */
    public static void requestPick(int slot, String id, short resource) {
        BFPacket.toServer(BFPacket.Kind.CIVICS_PICK, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.INT, null, slot), new Tag(Tag.Type.STRING, null, id),
                new Tag(Tag.Type.SHORT, null, resource), FinishTag.INST}));
    }

    // ---- tags: a list of (faction id, civics); the whole store, or one faction's update

    public synchronized Tag toTag() {
        return entries(byFaction);
    }

    /** Merges, so the same call takes the whole store on join and one faction's update after. */
    public synchronized void fromTag(Tag tag) {
        for (Tag e : tag.getStruct()) {
            if (e.getType() == Tag.Type.STRUCT) {
                Tag[] pair = e.getStruct();
                byFaction.put(pair[0].getInt(), FactionCivics.fromTag(pair[1]));
            }
        }
        version++;
    }

    private static Tag entries(Map<Integer, FactionCivics> m) {
        Tag[] t = new Tag[m.size() + 1];
        int i = 0;
        for (Map.Entry<Integer, FactionCivics> e : m.entrySet()) {
            t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.INT, null, e.getKey()), e.getValue().toTag(), FinishTag.INST});
        }
        t[i] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }
}
