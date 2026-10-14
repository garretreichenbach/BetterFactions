package videogoose.betterfactions;

import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetManager;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.network.objects.Sendable;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Root ships whose overheat timer ran out stay as wrecks instead of vanishing: ownerless, still
 * overheating (so anyone may board and reboot one), with everything docked still on them. A wreck
 * nobody reboots is removed when its second timer, {@code wreck_lifetime_hours}, runs out.
 *
 * <p>Turrets knocked off a ship during the fight are removed when it wrecks, wherever they drifted.</p>
 */
public final class Wrecks {

    private static Wrecks instance = new Wrecks();

    private final Set<String> wrecks = new HashSet<>();
    /** Turret UID to the UID of the root it was knocked off. todo: not saved; a restart forgets them. */
    private final Map<String, String> knockedOff = new HashMap<>();

    public static Wrecks get() {
        return instance;
    }

    public static void reset() {
        instance = new Wrecks();
    }

    public synchronized boolean isWreck(String uid) {
        return wrecks.contains(uid);
    }

    /**
     * Server: {@code c}'s overheat timer ran out. Makes it a wreck and restarts its timer for the
     * wreck's lifetime.
     *
     * @return false when the engine should destroy it as usual: not a root ship, wrecks are off, or
     * it already was a wreck and its lifetime is over
     */
    public boolean tryWreck(SegmentController c, long now) {
        long lifetimeMs = BFConfig.wreckLifetimeHours() * 60L * 60L * 1000L;
        if (!(c instanceof Ship ship) || !c.railController.isRoot() || c.isVirtualBlueprint() || lifetimeMs <= 0) {
            return false;
        }
        synchronized (this) {
            if (wrecks.remove(c.getUniqueIdentifier())) {
                return false;
            }
            wrecks.add(c.getUniqueIdentifier());
        }
        //as the engine does when the timer runs out
        for (int i = 0; i < ship.getAttachedPlayers().size(); i++) {
            ship.getAttachedPlayers().get(i).handleServerHealthAndCheckAliveOnServer(0, c);
        }
        leaveFleet(c);
        //docked entities without their own faction block go ownerless with it
        c.railController.resetFactionForEntitiesWithoutFactionBlock(c.getFactionId());
        removeKnockedOff(c.getUniqueIdentifier());
        c.setCoreTimerDuration(now - c.getCoreTimerStarted() + lifetimeMs);
        return true;
    }

    /** Server: a wreck was rebooted; it is a ship again. */
    public synchronized void onRebooted(String uid) {
        wrecks.remove(uid);
    }

    public synchronized void onTurretKnockedOff(String turretUid, String rootUid) {
        knockedOff.put(turretUid, rootUid);
    }

    /** Forgets and returns the turrets knocked off {@code rootUid}. */
    synchronized List<String> takeKnockedOff(String rootUid) {
        List<String> out = new ArrayList<>();
        for (Iterator<Map.Entry<String, String>> it = knockedOff.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> e = it.next();
            if (e.getValue().equals(rootUid)) {
                out.add(e.getKey());
                it.remove();
            }
        }
        return out;
    }

    private void removeKnockedOff(String rootUid) {
        for (String uid : takeKnockedOff(rootUid)) {
            Sendable s = GameServerState.instance.getLocalAndRemoteObjectContainer().getUidObjectMap().get(uid);
            //todo: only loaded ones; a turret that drifted into an unloaded sector stays
            if (s instanceof SegmentController t && !t.railController.isDocked()
                    && !(t instanceof Ship ts && !ts.getAttachedPlayers().isEmpty())) {
                t.railController.destroyDockedRecursive();
                t.markForPermanentDelete(true);
                t.setMarkedForDeleteVolatile(true);
            }
        }
    }

    private static void leaveFleet(SegmentController c) {
        FleetManager fm = GameServerState.instance.getFleetManager();
        Fleet f = fm.getByEntity(c);
        if (f == null) {
            return;
        }
        for (FleetMember m : f.getMembers()) {
            if (c.getUniqueIdentifier().equals(m.UID)) {
                fm.requestFleetMemberRemove(f, m);
                return;
            }
        }
    }

    public synchronized Tag toTag() {
        Tag[] t = new Tag[wrecks.size() + 1];
        int i = 0;
        for (String uid : wrecks) {
            t[i++] = new Tag(Tag.Type.STRING, null, uid);
        }
        t[i] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    public synchronized void fromTag(Tag tag) {
        for (Tag t : tag.getStruct()) {
            if (t.getType() == Tag.Type.STRING) {
                wrecks.add(t.getString());
            }
        }
    }
}
