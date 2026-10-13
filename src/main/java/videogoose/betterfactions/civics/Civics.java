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
        int cost = BFConfig.civicSwapCost();
        Tag update;
        synchronized (this) {
            FactionCivics c = byFaction.computeIfAbsent(f.getIdFaction(), k -> new FactionCivics());
            String why = c.problem(slot, id, f.factionPoints, now, cost, BFConfig.civicSwapCooldownDays() * DAY_MS);
            if (why != null) {
                reject(p, new Object[]{why});
                return;
            }
            if (c.isSwap(slot) && cost > 0) {
                f.factionPoints -= cost;
                f.sendFactionPointUpdate(GameServerState.instance.getGameState());
            }
            c.set(slot, id, now);
            version++;
            update = entries(Map.of(f.getIdFaction(), c));
        }
        for (PlayerState other : GameServerState.instance.getPlayerStatesByName().values()) {
            if (other.getClientChannel() != null) {
                BFPacket.toPlayer(other, BFPacket.Kind.CIVICS, update);
            }
        }
    }

    private static void reject(PlayerState p, Object[] message) {
        p.sendServerMessage(new ServerMessage(message, ServerMessage.MESSAGE_TYPE_ERROR, p.getId()));
    }

    /** Client: sends a pick; the answer comes back as a {@link BFPacket.Kind#CIVICS} update. */
    public static void requestPick(int slot, String id) {
        BFPacket.toServer(BFPacket.Kind.CIVICS_PICK, new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.INT, null, slot), new Tag(Tag.Type.STRING, null, id), FinishTag.INST}));
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
