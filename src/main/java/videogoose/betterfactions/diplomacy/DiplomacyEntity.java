package videogoose.betterfactions.diplomacy;

import videogoose.betterfactions.DiplomacyManager;
import api.event.EventResult;
import videogoose.betterfactions.api.DiplomacyActionListener;
import videogoose.betterfactions.api.ExecuteDiplomacyReactionListener;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.schema.common.util.LogInterface;
import org.schema.common.util.StringTools;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.common.data.player.faction.PersonalEnemyMod;
import org.schema.game.server.data.FactionState;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.common.language.Translatable;
import org.schema.schine.network.server.ServerMessage;
import org.schema.schine.network.server.ServerStateInterface;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DiplomacyEntity implements LogInterface {
    /**
     * Cap on how lopsided a strength comparison is allowed to read, in doublings. Without it an
     * empire measuring itself against a one-member faction produces an opinion swing no amount of
     * diplomacy could ever offset.
     */
    private static final double STRENGTH_CLAMP = 6;
    private static final long TRUST_HOUR_MS = 60L * 60L * 1000L;
    /**
     * What breaking one treaty costs, in hours of holding one.
     */
    private static final float TRUST_BREAK_PENALTY = 24;
    private static final byte VERSION = 1;
    /**
     * Live actions, keyed by {@link DiplomacyAction#key()} so a type can carry a subject.
     */
    private final Object2ObjectOpenHashMap<String, DiplomacyAction> actions = new Object2ObjectOpenHashMap<String, DiplomacyAction>();
    private final FactionState state;
    private final DiplomacyManager mgr;
    private final Object2ObjectOpenHashMap<DiplomacyAction.DiplActionType, OpinionTurnModifier> dynamicMap = new Object2ObjectOpenHashMap<DiplomacyAction.DiplActionType, OpinionTurnModifier>() {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        public OpinionTurnModifier remove(Object k) {
            if (!(k == null || k instanceof DiplomacyAction.DiplActionType)) {
                throw new IllegalArgumentException();
            }
            return super.remove(k);
        }

        @Override
        public OpinionTurnModifier get(Object k) {
            if (!(k == null || k instanceof DiplomacyAction.DiplActionType)) {
                throw new IllegalArgumentException();
            }
            return super.get(k);
        }

        @Override
        public boolean containsKey(Object k) {
            if (!(k == null || k instanceof DiplomacyAction.DiplActionType)) {
                throw new IllegalArgumentException();
            }
            return super.containsKey(k);
        }
    };
    private final Object2ObjectOpenHashMap<DiplStatusType, OpinionStaticModifier> staticMap = new Object2ObjectOpenHashMap<DiplStatusType, OpinionStaticModifier>() {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        public OpinionStaticModifier remove(Object k) {
            if (!(k == null || k instanceof DiplStatusType)) {
                throw new IllegalArgumentException();
            }
            return super.remove(k);
        }

        @Override
        public OpinionStaticModifier get(Object k) {
            if (!(k == null || k instanceof DiplStatusType)) {
                throw new IllegalArgumentException();
            }
            return super.get(k);
        }

        @Override
        public boolean containsKey(Object k) {
            if (!(k == null || k instanceof DiplStatusType)) {
                throw new IllegalArgumentException();
            }
            return super.containsKey(k);
        }
    };
    private int points;
    private int pointCached;
    private int fid;
    private long dbId;
    private FactionDiplomacy dCache;
    private boolean changed;
    private final Int2BooleanOpenHashMap reactionsFired = new Int2BooleanOpenHashMap();

    public DiplomacyEntity(DiplomacyManager mgr, int fid, long dbId) {
        this(mgr, fid);
        this.dbId = dbId;
    }

    public DiplomacyEntity(DiplomacyManager mgr, int fid) {
        this.fid = fid;
        this.mgr = mgr;
        this.state = (FactionState) mgr.getState();
    }

    public boolean isFactionLoaded() {
        return state.getFactionManager().getFaction(fid) != null;
    }

    public int getRawPoints() {
        return points;
    }

    public void calculateStaticModifiers(long timeElapsed) {
        for (DiplStatusType s : DiplStatusType.values()) {
            if (getConfig().existsStatus(s)) {
                calculateStaticModifier(timeElapsed, staticMap, s);
            }
        }
        checkReactions();
        setChanged();
        setNTChanged();
    }

    private void checkReactions() {
        Faction owner = getFaction();
        if (owner == null || !owner.isNPC()) {
            //player factions keep a ledger for display; their options are never decided by it
            return;
        }
        List<DiplomacyReaction> reactions = getConfig().getDiplomacyReactions();
        for (DiplomacyReaction r : reactions) {
            if (r.isSatisfied(this)) {
                boolean executed = executeReaction(r);
                if (executed) {
                    reactionsFired.put(r.index, true);
                } else {
                    reactionsFired.remove(r.index);
                }
            } else {
                reactionsFired.remove(r.index);
            }
        }
    }

    public void applyDynamicModifiers(long timeElapsed) {
        int pointsBef = getPoints();
        for (OpinionTurnModifier e : dynamicMap.values()) {
            modPoints(e.pointsPerTurn);
        }
        setChanged();
        if (pointsBef != getPoints()) {
            onPointsChanged(pointsBef, getPoints());
        }
    }

    private void onPointsChanged(int pointsBef, int pointsNow) {
        //use for reaction based on points
        setNTChanged();
    }

    public boolean existsStatusModifier(DiplStatusType status) {
        OpinionStaticModifier mod = staticMap.get(status);
        return mod != null && mod.value != 0;
    }

    public long persistedStatusModifier(DiplStatusType status) {
        OpinionStaticModifier mod = staticMap.get(status);
        return mod == null ? -1 : mod.totalTimeApplied;
    }

    public long persistedActionModifier(DiplomacyAction.DiplActionType action) {
        OpinionTurnModifier mod = dynamicMap.get(action);
        return mod == null ? -1 : mod.totalElapsedTime;
    }

    private void calculateStaticModifier(long timeElapsed, Object2ObjectOpenHashMap<DiplStatusType, OpinionStaticModifier> map, DiplStatusType status) {
        float intensity = calculateStatus(status);
//		if(dbId >= Integer.MAX_VALUE){
//			assert(status != DiplStatusType.IN_WAR || intensity == 0):status.name()+": "+intensity;
//		}
        int value = getConfig().getDiplomacyValue(status);
        OpinionStaticModifier mod = map.get(status);
        if (intensity > 0) {
            if (mod == null) {
                mod = new OpinionStaticModifier();
                mod.type = status;
                mod.value = (int) Math.ceil(value * intensity);
                mod.totalTimeApplied = 0;
                map.put(status, mod);
            } else {
                mod.elapsedTimeInactive = 0;
                mod.totalTimeApplied += timeElapsed;
            }
        } else {
            if (mod != null) {
                mod.elapsedTimeInactive += timeElapsed;
                if (mod.type == DiplStatusType.NON_AGGRESSION || mod.elapsedTimeInactive > getConfig().getDiplomacyStaticTimeout(status)) {
                    map.remove(status);
                }
            }
        }
        if (mod != null) {
            log("Faction static modifier: " + mod.type.name() + "; Value: " + value, LogLevel.DEBUG);
        }
    }

    public float getStatusPoints(DiplStatusType status) {
        OpinionStaticModifier mod = staticMap.get(status);
        return mod == null ? -1 : mod.value;
    }

    public float calculateStatus(DiplStatusType status) {
        FactionRelation.RType rel = getDiplomacy().faction.getRelationshipWithFactionOrPlayer(dbId);
//		if(dbId >= Integer.MAX_VALUE){
//			System.err.println("CHECKING STATUS "+status.name()+": "+rel+" from "+getDiplomacy().faction.getIdFaction()+" --- "+dbId);
//		}
        switch (status) {
            case ALLIANCE:
                if (rel == FactionRelation.RType.FRIEND) {
                    return 1;
                } else {
                    return 0;
                }
            case ALLIANCE_WITH_ENEMY:
                if (isFaction()) {
                    List<Faction> enemies = getFaction().getEnemies();
                    int alliances = 0;
                    for (Faction f : enemies) {
                        if (f.getIdFaction() != dbId) {
                            FactionRelation.RType orel = f.getRelationshipWithFactionOrPlayer(dbId);
                            if (orel == FactionRelation.RType.FRIEND) {
                                alliances++;
                            }
                        }
                    }
                    return alliances;
                } else {
                    return 0;
                }
            case IN_WAR:
                if (rel == FactionRelation.RType.ENEMY) {
//				System.err.println("RETURNING 1::::: "+rel);
                    return 1;
                } else {
//				System.err.println("RETURNING 0::::: "+rel);
                    return 0;
                }
            case IN_WAR_WITH_ENEMY:
                List<Faction> enemies = getFaction().getEnemies();
                int wars = 0;
                for (Faction f : enemies) {
                    if ((f.isNPC() || f.isPlayerFaction()) && f.getIdFaction() != dbId) {
                        FactionRelation.RType orel = f.getRelationshipWithFactionOrPlayer(dbId);
                        if (orel == FactionRelation.RType.ENEMY) {
                            wars++;
                        }
                    }
                }
                return wars;
            case ALLIANCE_WITH_FRIENDS:
                if (isFaction()) {
                    List<Faction> friends = getFaction().getFriends();
                    int friendsWFriends = 0;
                    for (Faction f : friends) {
                        if ((f.isNPC() || f.isPlayerFaction()) && f.getIdFaction() != dbId) {
                            FactionRelation.RType orel = f.getRelationshipWithFactionOrPlayer(dbId);
                            if (orel == FactionRelation.RType.FRIEND) {
                                friendsWFriends++;
                            }
                        }
                    }
                    return friendsWFriends;
                } else {
                    return 0;
                }
            case IN_WAR_WITH_FRIENDS:
                List<Faction> friends = getFaction().getFriends();
                int warsWFriends = 0;
                for (Faction f : friends) {
                    if ((f.isNPC() || f.isPlayerFaction()) && f.getIdFaction() != dbId) {
                        FactionRelation.RType orel = f.getRelationshipWithFactionOrPlayer(dbId);
                        if (orel == FactionRelation.RType.ENEMY) {
                            warsWFriends++;
                        }
                    }
                }
                return warsWFriends;
            case NON_AGGRESSION:
                DiplomacyAction diplomacyAction = actions.get(DiplomacyAction.DiplActionType.ATTACK.name());
                if (diplomacyAction == null || diplomacyAction.counter < 1) {
                    return 1;
                } else {
                    return 0;
                }
            case FACTION_MEMBER_AT_WAR_WITH_US:
                return countMembersMatching(true);
            case FACTION_MEMBER_WE_DONT_LIKE:
                return countMembersMatching(false);
            case PIRATE:
                //used to return a flat 1 for everyone, so a config that enabled it applied the
                //"they are pirates" penalty to every faction in the game
                return isOfKind("PIRATE") ? 1 : 0;
            case POWER:
                return (float) getRelativeStrength();
            case TRUST:
                return calculateTrust();
            default:
                break;
        }
        return 0;
    }

    /**
     * Counts the members of the faction this entry is about that we hold something against
     * individually - either they are one of our personal enemies, or their own ledger entry with us
     * has gone negative. Meaningless for a player-scoped entry, which has no members.
     *
     * @param atWar true to count personal enemies, false to count members we merely dislike
     */
    private float countMembersMatching(boolean atWar) {
        if (!isFaction()) {
            return 0;
        }
        Faction other = state.getFactionManager().getFaction((int) dbId);
        Faction own = getFaction();
        if (other == null || own == null) {
            return 0;
        }
        if (atWar) {
            int count = 0;
            for (String member : other.getMembersUID().keySet()) {
                if (own.getPersonalEnemies().contains(member.toLowerCase(Locale.ENGLISH))) {
                    count++;
                }
            }
            return count;
        }
        if (!(state instanceof GameServerState server)) {
            //the per-player ledger entries only exist server-side
            return 0;
        }
        int count = 0;
        for (String member : other.getMembersUID().keySet()) {
            PlayerState p = server.getPlayerFromNameIgnoreCaseWOException(member.toLowerCase(Locale.ENGLISH));
            if (p == null) {
                continue;
            }
            DiplomacyEntity personal = getDiplomacy().entities.get(p.getDbId());
            if (personal != null && personal.getPoints() < 0) {
                count++;
            }
        }
        return count;
    }

    private void calculateDynamicModifier(long timeElapsed, Object2ObjectOpenHashMap<DiplomacyAction.DiplActionType, OpinionTurnModifier> dynamicMap2, DiplomacyAction.DiplActionType action) {
        int upperOrig = getConfig().getDiplomacyUpperLimit(action);
        int lowerOrig = getConfig().getDiplomacyLowerLimit(action);
        if ((upperOrig == lowerOrig) && lowerOrig == 0) {
            //no modification for this action
            return;
        }
        int valueMod = upperOrig > lowerOrig ? 1 : -1;
        int upper = valueMod > 0 ? upperOrig : lowerOrig;
        int lower = valueMod > 0 ? lowerOrig : upperOrig;
        long timeOut = getConfig().getDiplomacyTurnTimeout(action);
        if (Math.abs(upper - lower) > 0) {
            timeOut = 0;
        }
        int value = getConfig().getDiplomacyValue(action);
        int ac = getActionCount(action);
        OpinionTurnModifier mod = dynamicMap2.get(action);
        long delay = getConfig().getDiplomacyTurnEffectChangeDelay();
        if (ac > 0) {
            if (mod == null) {
                mod = new OpinionTurnModifier();
                mod.type = action;
                mod.pointsPerTurn = value;
                dynamicMap2.put(action, mod);
            } else {
                mod.elapsedTime += timeElapsed;
                mod.totalElapsedTime += timeElapsed;
                if (mod.elapsedTime > delay) {
                    int b = mod.pointsPerTurn;
                    int md = valueMod * getConfig().getDiplomacyExistingActionModifier(action);
                    mod.pointsPerTurn = Math.max(lower, Math.min(upper, mod.pointsPerTurn + md));
                    log("PERSISTING ACTION " + b + " -> " + mod.pointsPerTurn + " [" + lower + ", " + upper + "] mod: " + md, LogLevel.DEBUG);
                    mod.elapsedTime -= delay;
                } else {
                    log("PERSISTING ACTION not triggering yet " + mod.elapsedTime + "/" + delay + " -> " + mod.pointsPerTurn, LogLevel.DEBUG);
                }
            }
        } else {
            if (mod != null) {
                mod.elapsedTime += timeElapsed;
                mod.totalElapsedTime += timeElapsed;
                if (mod.elapsedTime > delay) {
                    int b = mod.pointsPerTurn;
                    int md = valueMod * getConfig().getDiplomacyNonExistingActionModifier(action);
                    mod.pointsPerTurn = Math.max(lower, Math.min(upper, mod.pointsPerTurn - md));
                    mod.elapsedTime -= delay;
                    log("NON PERSISTING ACTION " + b + " -> " + mod.pointsPerTurn + " [" + lower + ", " + upper + "] mod: " + md + ";", LogLevel.DEBUG);
                    /*
                     * if there is no upper/lower bound we use timeout
                     * else we remove if upper/lower bounds is reached for positive/negative effect
                     */
                    if ((timeOut <= 0 && ((valueMod > 0 && mod.pointsPerTurn == upperOrig) ||
                            (valueMod < 0 && mod.pointsPerTurn == lowerOrig))) ||
                            (timeOut > 0 && mod.totalElapsedTime > timeOut)) {
                        if (timeOut > 0 && mod.totalElapsedTime > timeOut) {
                            log("ACTION TIMEOUT! REMOVING; " + mod.totalElapsedTime + " / " + timeOut, LogLevel.DEBUG);
                        } else {
                            log("LIMIT REACHED! REMOVING; ppt " + mod.pointsPerTurn + " [" + lowerOrig + ", " + upperOrig + "]", LogLevel.DEBUG);
                        }
                        //limit reached -> Remove
                        dynamicMap2.remove(action);
                    }
                }
            }
        }
        checkReactions();
    }

    void calculateDiplomacyModifiersFromActions(long timeElapsed) {
        ObjectIterator<DiplomacyAction> iterator = actions.values().iterator();
        while (iterator.hasNext()) {
            DiplomacyAction a = iterator.next();
            if (getConfig().existsAction(a.type)) {
                DiplomacyReaction react = getConfig().getDiplomacyActionRequired(a.type);
                if (react != null) {
                    if (react.isSatisfied(this)) {
                        //dont enter this reaction into the executed map
                        //as it is triggered on action
                        executeReaction(react);
                    }
                } else {
                    calculateDynamicModifier(0, dynamicMap, a.type);
                }
            }
            log("ACTION TIMEOUT: " + a.type.name() + " " + a.timeDuration + " - " + timeElapsed + " = " + (a.timeDuration - timeElapsed), LogLevel.DEBUG);
            a.timeDuration -= timeElapsed;
            if (a.timeDuration <= 0) {
                iterator.remove();
            }
        }
        for (OpinionTurnModifier d : dynamicMap.values()) {
            if (getConfig().existsAction(d.type)) {
                calculateDynamicModifier(timeElapsed, dynamicMap, d.type);
            }
        }
        setNTChanged();
    }

    public int getActionCount(DiplomacyAction.DiplActionType type) {
        return getActionCount(type, null);
    }

    public int getActionCount(DiplomacyAction.DiplActionType type, String subject) {
        DiplomacyAction action = actions.get(DiplomacyAction.key(type, subject));
        return action == null ? 0 : action.counter;
    }

    public void diplomacyAction(DiplomacyAction.DiplActionType type) {
        diplomacyAction(type, null);
    }

    /**
     * @param subject what the action was about when the type alone is ambiguous (a treaty id), else null
     */
    public void diplomacyAction(DiplomacyAction.DiplActionType type, String subject) {
        String key = DiplomacyAction.key(type, subject);
        DiplomacyAction action = actions.get(key);
        if (action == null) {
            action = new DiplomacyAction();
            action.type = type;
            action.subject = subject;
            actions.put(key, action);
        }
        EventResult nPCDiplomacyActionResult = EventResult.CONTINUE;
        for (DiplomacyActionListener l : DiplomacyActionListener.TYPE.get()) {
            nPCDiplomacyActionResult = nPCDiplomacyActionResult.merge(l.onDiplomacyAction(this, actions, action, true));
            if (nPCDiplomacyActionResult.isConsumed()) break;
        }
        if (nPCDiplomacyActionResult.isCanceled()) {
            actions.remove(key);
            return;
        }
        log("Faction action modifier triggered: " + action.type.name(), LogLevel.DEBUG);
        if (isFaction() && (action.type == DiplomacyAction.DiplActionType.ATTACK || action.type == DiplomacyAction.DiplActionType.DECLARATION_OF_WAR)) {
            DiplomacyProposal offer = mgr.getRelationShipOffers().get(
                    DiplomacyProposal.getOfferCode(getFaction().getIdFaction(), (int) dbId));
            if (offer != null) {
                //remove offer since there was aggression
                DiplomacyProposal e = new DiplomacyProposal();
                e.set("npc", getFaction().getIdFaction(), (int) dbId,
                        offer.rel, "msg", true);
                e.serverAuthored = true;
                mgr.getRelationOffersToAdd().add(e);
                log("Faction revoked " + offer.getRelation().name() + " offer because of aggresion", LogLevel.NORMAL);
            }
        }
        //reset action and increase counter
        action.timeDuration = getConfig().getDiplomacyActionTimeout(type);
        action.counter++;
        setChanged();
    }

    @Override
    public void log(String string, LogLevel l) {
        getDiplomacy().log("[TO_" + dbId + "]" + string, l);
    }

    public boolean isFaction() {
        return dbId < Integer.MAX_VALUE;
    }

    private Faction getFaction() {
        return getDiplomacy().faction;
    }

    public DiplomacySettings getConfig() {
        return getDiplomacy().getConfig();
    }

    public void setChanged() {
        changed = true;
    }

    public FactionDiplomacy getDiplomacy() {
        if (dCache == null) {
            dCache = mgr.diplomacyOf(state.getFactionManager().getFaction(fid));
        }
        return dCache;
    }

    public void deserialize(DataInputStream stream, int updateSenderStateId,
                            boolean onServer) throws IOException {
        actions.clear();
        dynamicMap.clear();
        staticMap.clear();
        Tag t = Tag.deserializeNT(stream);
        fromTag(t);
    }

    public void fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        byte version = t[0].getByte();
        dbId = t[1].getLong();
        fid = t[2].getInt();
        points = t[3].getInt();
        pointCached = t[4].getInt();
        if (version < VERSION) {
            //v0 keyed its sub-maps by enum ordinal and nested each entry one level deeper.
            //The running score above is kept; the modifiers are not worth reading through a
            //vocabulary that has since shifted, and they rebuild from live relations within a turn.
            return;
        }
        fromActionsTag(t[5]);
        fromDynamicMapTag(t[6]);
        fromStaticMapTag(t[7]);
        fromReactionsFiredMap(t[8]);
    }

    private void fromActionsTag(Tag tag) {
        Tag[] t = tag.getStruct();
        if ((t.length - 1) > 300) {
            System.err.println("[SERVER][FACTION][NPC][DIPLOMACY] FID: " + fid + "; Actions: " + (t.length - 1));
        }
        for (int i = 0; i < t.length - 1; i++) {
            Tag[] m = t[i].getStruct();
            DiplomacyAction s = new DiplomacyAction();
            if (s.fromTag(m[0])) {
                actions.put(s.key(), s);
            }
        }
    }

    private void fromDynamicMapTag(Tag tag) {
        Tag[] t = tag.getStruct();
        if ((t.length - 1) > 300) {
            System.err.println("[SERVER][FACTION][NPC][DIPLOMACY] FID: " + fid + "; DynMap: " + (t.length - 1));
        }
        for (int i = 0; i < t.length - 1; i++) {
            Tag[] m = t[i].getStruct();
            OpinionTurnModifier s = new OpinionTurnModifier();
            if (s.fromTag(m[0])) {
                dynamicMap.put(s.type, s);
            }
        }
    }

    private void fromStaticMapTag(Tag tag) {
        Tag[] t = tag.getStruct();
        if ((t.length - 1) > 300) {
            System.err.println("[SERVER][FACTION][NPC][DIPLOMACY] FID: " + fid + "; StaticMap: " + (t.length - 1));
        }
        for (int i = 0; i < t.length - 1; i++) {
            Tag[] m = t[i].getStruct();
            OpinionStaticModifier s = new OpinionStaticModifier();
            if (s.fromTag(m[0])) {
                staticMap.put(s.type, s);
            }
        }
    }

    private void fromReactionsFiredMap(Tag tag) {
        Tag[] t = tag.getStruct();
        if ((t.length - 1) > 300) {
            System.err.println("[SERVER][FACTION][NPC][DIPLOMACY] FID: " + fid + "; ReActions: " + (t.length - 1));
        }
        for (int i = 0; i < t.length - 1; i++) {
            Tag[] m = t[i].getStruct();
            short key = m[0].getShort();
            boolean val = m[1].getByte() != 0;
            reactionsFired.put(key, val);
        }
    }

    public void serialize(DataOutput buffer, boolean onServer) throws IOException {
        Tag t = toTag();
        t.serializeNT(buffer);
    }

    public Tag toTag() {
        getPoints();
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, VERSION),
                new Tag(Tag.Type.LONG, null, dbId),
                new Tag(Tag.Type.INT, null, fid),
                new Tag(Tag.Type.INT, null, points),
                new Tag(Tag.Type.INT, null, pointCached),
                getActionsTag(),
                getDynamicMapTag(),
                getStaticMapTag(),
                getReactionsFiredMapTag(),
                FinishTag.INST
        });
    }

    public int getPoints() {
        if (!getFaction().isOnServer()) {
            //from NT
            return pointCached;
        }
        if (changed) {
            pointCached = recalcPoints();
            setNTChanged();
            changed = false;
        }
        return pointCached;
    }

    public void setPoints(int points) {
        this.points = Math.max(getConfig().getDiplomacyMinPoints(), Math.min(getConfig().getDiplomacyMaxPoints(), points));
    }

    private Tag getActionsTag() {
        Tag[] t = new Tag[actions.size() + 1];
        t[t.length - 1] = FinishTag.INST;
        int i = 0;
        for (DiplomacyAction a : actions.values()) {
            t[i] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    a.toTag(),
                    FinishTag.INST
            });
            i++;
        }
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    private Tag getDynamicMapTag() {
        Tag[] t = new Tag[dynamicMap.size() + 1];
        t[t.length - 1] = FinishTag.INST;
        int i = 0;
        for (OpinionTurnModifier m : dynamicMap.values()) {
            t[i] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    m.toTag(),
                    FinishTag.INST
            });
            i++;
        }
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    private Tag getStaticMapTag() {
        Tag[] t = new Tag[staticMap.size() + 1];
        t[t.length - 1] = FinishTag.INST;
        int i = 0;
        for (OpinionStaticModifier m : staticMap.values()) {
            t[i] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    m.toTag(),
                    FinishTag.INST
            });
            i++;
        }
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    private Tag getReactionsFiredMapTag() {
        Tag[] t = new Tag[reactionsFired.size() + 1];
        t[t.length - 1] = FinishTag.INST;
        int i = 0;
        for (Map.Entry<Integer, Boolean> e : reactionsFired.entrySet()) {
            t[i] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.SHORT, null, e.getKey().shortValue()),
                    new Tag(Tag.Type.BYTE, null, e.getValue().booleanValue() ? (byte) 1 : (byte) 0),
                    FinishTag.INST
            });
            i++;
        }
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    private int recalcPoints() {
        int p = points;
        for (OpinionStaticModifier e : staticMap.values()) {
            p += e.value;
        }
        return Math.max(getConfig().getDiplomacyMinPoints(), Math.min(getConfig().getDiplomacyMaxPoints(), p));
    }

    private void setNTChanged() {
        if (state instanceof ServerStateInterface) {
            getDiplomacy().ntChanged(dbId);
        }
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("ENTITY: " + dbId + "\n");
        b.append("Points: " + pointCached + " (Raw: " + points + ")\n");
        b.append("Actions: \n");
        for (DiplomacyAction a : actions.values()) {
            b.append("  " + a.type.name() + "x" + a.counter + "; Duration " + a.timeDuration + "\n");
        }
        b.append("Dynamic: \n");
        for (OpinionTurnModifier a : dynamicMap.values()) {
            b.append("  " + a.type.name() + "; PPT: " + a.pointsPerTurn + ";\n");
        }
        b.append("Static: \n");
        for (OpinionStaticModifier a : staticMap.values()) {
            b.append("  " + a.type.name() + "; MOD: " + a.value + ";\n");
        }
        return b.toString();
    }

    public Object2ObjectOpenHashMap<DiplomacyAction.DiplActionType, OpinionTurnModifier> getDynamicMap() {
        return dynamicMap;
    }

    public Object2ObjectOpenHashMap<DiplStatusType, OpinionStaticModifier> getStaticMap() {
        return staticMap;
    }

    public void triggerReactionManually(DiplomacyReaction r) {
        log("Triggering reaction manually", LogLevel.NORMAL);
        executeReaction(r);
    }

    private boolean executeReaction(DiplomacyReaction r) {
        EventResult nPCExecuteDiplomacyReactionResult = EventResult.CONTINUE;
        for (ExecuteDiplomacyReactionListener l : ExecuteDiplomacyReactionListener.TYPE.get()) {
            nPCExecuteDiplomacyReactionResult = nPCExecuteDiplomacyReactionResult.merge(l.onExecuteDiplomacyReaction(this, r, true));
            if (nPCExecuteDiplomacyReactionResult.isConsumed()) break;
        }
        if (nPCExecuteDiplomacyReactionResult.isCanceled()) return false;
        switch (r.reaction) {
            case PROPOSE_TREATY:
                return proposeTreaty(r);
            case WITHDRAW_PROPOSAL:
                return withdrawProposal(r);
            case BREAK_TREATY:
                return breakTreaty(r);
            case SEND_FACTION_MAIL:
                return sendFactionMail(r);
            case SEND_POPUP_MESSAGE:
                if (!reactionsFired.containsKey(r.index)) {
                    if (isSinglePlayer()) {
                        PlayerState playerState = ((GameServerState) state).getPlayerStatesByDbId().get(dbId);
                        if (playerState != null) {
                            playerState.sendServerMessagePlayerError(Lng.astr("Personal Diplomacy Message:\n%s", r.message));
                        }
                    } else {
                        if ((int) dbId > 0) {
                            Faction f = state.getFactionManager().getFaction((int) dbId);
                            if (f != null && r.message != null) {
                                f.broadcastMessage(Lng.astr("Diplomacy Message to your faction:\n%s", r.message), ServerMessage.MESSAGE_TYPE_ERROR, (GameServerState) state);
                            }
                        }
                    }
                }
                return true;
            default:
                break;
        }
        return false;
    }

    /**
     * Offers them a treaty, or plain peace when the reaction names none.
     *
     * <p>Single-player targets are a special case only for peace: a player who is not in a faction
     * has nothing to sign, so the equivalent gesture is dropping them as a personal enemy.</p>
     */
    private boolean proposeTreaty(DiplomacyReaction r) {
        if (isSinglePlayer()) {
            if (r.treatyId != null || getFaction().getRelationshipWithFactionOrPlayer(dbId) != FactionRelation.RType.ENEMY) {
                return false;
            }
            String player = ((GameServerState) state).getPlayerNameFromDbIdLowerCase(dbId);
            PersonalEnemyMod mod = new PersonalEnemyMod("npc", player, getFaction().getIdFaction(), false);
            if (state.getFactionManager().getToModPersonalEnemies().contains(mod)) {
                return false;
            }
            state.getFactionManager().getToModPersonalEnemies().add(mod);
            ((GameServerState) state).getServerPlayerMessager().send(
                    getFaction().getName(), player, Lng.str("Peace"),
                    r.message != null && !r.message.isEmpty() ? r.message
                            : Lng.str("Since you haven't been attacking for some time, we will no longer consider you an enemy!"));
            log("Faction offered peace to a lone player", LogLevel.NORMAL);
            return true;
        }
        if (!isFaction()) {
            return false;
        }
        TreatyType type = r.treatyId != null ? TreatyTypeRegistry.get(r.treatyId) : null;
        if (r.treatyId != null && type == null) {
            log("Reaction names a treaty type nothing registered: " + r.treatyId, LogLevel.ERROR);
            return false;
        }
        //nothing to offer if it is already in force, or already on the table
        if (type != null && mgr.hasTreaty(getFaction().getIdFaction(), (int) dbId, type.getId())) {
            return false;
        }
        if (type != null && type.getImplies() != null
                && getFaction().getRelationshipWithFactionOrPlayer(dbId) == type.getImplies()) {
            //already where this treaty would put us - a war we are already fighting, say - and a
            //declaration that repeats every turn is just spam
            return false;
        }
        if (findOwnProposal(r.treatyId) != null) {
            return false;
        }
        if (type == null && getFaction().getRelationshipWithFactionOrPlayer(dbId) != FactionRelation.RType.ENEMY) {
            //plain peace is only meaningful while there is something to end
            return false;
        }

        DiplomacyProposal offer = new DiplomacyProposal();
        byte rel = type != null && type.getImplies() != null ? type.getImplies().code : FactionRelation.RType.NEUTRAL.code;
        offer.set("ADMIN", getFaction().getIdFaction(), (int) dbId, r.treatyId, rel,
                r.message != null && !r.message.isEmpty() ? r.message
                        : Lng.str("Since you haven't been attacking for some time, we wanted to make peace"),
                DiplomacyProposal.Action.PROPOSE);
        offer.serverAuthored = true;
        mgr.getRelationOffersToAdd().add(offer);
        log("Faction offered " + (r.treatyId != null ? r.treatyId : "peace"), LogLevel.NORMAL);
        return true;
    }

    private boolean withdrawProposal(DiplomacyReaction r) {
        if (!isFaction()) {
            return false;
        }
        DiplomacyProposal standing = findOwnProposal(r.treatyId);
        if (standing == null) {
            return false;
        }
        DiplomacyProposal rej = new DiplomacyProposal();
        rej.set("ADMIN", standing.a, standing.b, standing.typeId, standing.rel,
                r.message != null && !r.message.isEmpty() ? r.message : "We revoke our offer!",
                DiplomacyProposal.Action.REVOKE);
        mgr.relationShipOfferServer(rej);
        log("Faction withdrew its " + (r.treatyId != null ? r.treatyId : "peace") + " offer", LogLevel.NORMAL);
        return true;
    }

    private boolean breakTreaty(DiplomacyReaction r) {
        if (!isFaction() || r.treatyId == null) {
            return false;
        }
        if (!mgr.hasTreaty(getFaction().getIdFaction(), (int) dbId, r.treatyId)) {
            return false;
        }
        DiplomacyProposal br = new DiplomacyProposal();
        TreatyType type = TreatyTypeRegistry.get(r.treatyId);
        byte rel = type != null && type.getImplies() != null ? type.getImplies().code : FactionRelation.RType.NEUTRAL.code;
        br.set("ADMIN", getFaction().getIdFaction(), (int) dbId, r.treatyId, rel,
                r.message != null && !r.message.isEmpty() ? r.message : "We are done with this arrangement.",
                DiplomacyProposal.Action.REVOKE);
        mgr.relationShipOfferServer(br);
        log("Faction broke " + r.treatyId, LogLevel.NORMAL);
        return true;
    }

    private boolean sendFactionMail(DiplomacyReaction r) {
        if (!isFaction() || reactionsFired.containsKey(r.index)) {
            return false;
        }
        Faction them = state.getFactionManager().getFaction((int) dbId);
        if (them == null || r.message == null) {
            return false;
        }
        mgr.sendServerFactionMail(
                r.name != null && !r.name.isEmpty() ? r.name : Lng.str("A message from %s", getFaction().getName()),
                r.message, them);
        return true;
    }

    /**
     * Our own standing offer of this treaty type to them, or null.
     */
    private DiplomacyProposal findOwnProposal(String treatyId) {
        DiplomacyProposal p = mgr.getRelationShipOffers()
                .get(DiplomacyProposal.getOfferCode(getFaction().getIdFaction(), (int) dbId));
        if (p == null) {
            return null;
        }
        return treatyId == null ? (p.typeId == null ? p : null) : (treatyId.equals(p.typeId) ? p : null);
    }

    /**
     * Whether this faction would agree to the given treaty if it were offered right now, and if not,
     * what it is holding out for.
     *
     * <p>This is the whole of NPC acceptance. It replaces the four {@code ACCEPT_*}/{@code REJECT_*}
     * reactions, which only ran on the diplomacy turn and had the treaty type baked into the Java
     * rather than the config - so an archetype could not be given a personality without editing the
     * engine. Because it is a plain query it also answers the GUI's "would they accept?" preview
     * without committing to anything.</p>
     *
     * @param typeId treaty type being offered, or null for plain peace
     * @param unmet  if non-null, filled with the conditions that were not met
     */
    public boolean wouldAccept(String typeId, List<String> unmet) {
        TreatyType offered = typeId != null ? TreatyTypeRegistry.get(typeId) : null;
        if (offered != null && offered.getKind() == TreatyType.Kind.DEMAND) {
            //An NPC faction never submits to anyone, whatever its archetype says. Two reasons, and
            //the second outlives the first: nothing yet implements what being a vassal or a tributary
            //would mean, so a signed one would be a record no behaviour honours - and an NPC faction
            //is run by the engine rather than by anyone who could be made to answer to a player, so
            //there is nobody there for the arrangement to bind.
            if (unmet != null) {
                unmet.add(Lng.str("they will not be made subject to anyone"));
            }
            return false;
        }
        DiplomacyConditionGroup rule = getConfig().getDiplomacyAcceptance(typeId);
        String refusal = getConfig().getDiplomacyRefusal(typeId);
        if (rule == null) {
            //An archetype opts in to each kind of agreement by writing a rule for it, so no rule is a
            //settled "we don't do that" rather than an oversight - and it is the most common refusal
            //a player will ever see. It used to answer with the phrase "no acceptance rule configured"
            //and the internal type id, which describes the server's config file rather than anything
            //happening in the game.
            if (unmet != null) {
                unmet.add(refusal != null ? refusal : declines(typeId));
            }
            return false;
        }
        if (rule.satisfied(this, unmet)) {
            return true;
        }
        if (refusal != null && unmet != null) {
            //the archetype speaks for itself, so the condition list it would have got is replaced
            //rather than added to: two explanations of the same "no" is worse than either
            unmet.clear();
            unmet.add(refusal);
        }
        return false;
    }

    /**
     * How a faction that simply does not make this kind of agreement says so.
     */
    private String declines(String typeId) {
        TreatyType type = typeId != null ? TreatyTypeRegistry.get(typeId) : null;
        if (type == null) {
            return Lng.str("they are not interested in peace with you");
        }
        switch (type.getKind()) {
            case DEMAND:
                return Lng.str("they do not submit to anyone");
            case DECLARATION:
                return Lng.str("that is not something they answer");
            default:
                return Lng.str("they do not enter into agreements of that kind");
        }
    }

    /**
     * Strength of the faction or player this entry is about, on an arbitrary but shared scale.
     */
    private double theirStrength() {
        if (isSinglePlayer()) {
            return FactionStrength.ofLonePlayer(getConfig());
        }
        Faction f = state.getFactionManager().getFaction((int) dbId);
        return f != null ? strengthOf(f) : 0;
    }

    /**
     * Sized up on this archetype's own weights; see {@link FactionStrength} for what goes into it and
     * what does not.
     */
    private double strengthOf(Faction f) {
        return FactionStrength.of(f, getConfig());
    }

    /**
     * How much stronger they are than us, counted in doublings: 0 is an equal, +1 twice our
     * strength, -1 half of it.
     *
     * <p>A ratio rather than a difference because the interesting question is never "how many more
     * systems", it is "can they take us" - and that does not scale linearly with size. The log keeps
     * the number small enough to be a sane opinion multiplier, and it is clamped so an empire
     * comparing itself to a one-man faction does not produce an opinion swing nothing can offset.</p>
     */
    public double getRelativeStrength() {
        Faction us = getFaction();
        if (us == null) {
            return 0;
        }
        double ours = Math.max(1, strengthOf(us));
        double theirs = Math.max(1, theirStrength());
        double doublings = Math.log(theirs / ours) / Math.log(2);
        return Math.max(-STRENGTH_CLAMP, Math.min(STRENGTH_CLAMP, doublings));
    }

    /**
     * How many factions we are at war with.
     */
    public int getOurWarCount() {
        Faction us = getFaction();
        return us != null ? us.getEnemies().size() : 0;
    }

    public boolean hasTreatyWithThem(String typeId) {
        return isFaction() && typeId != null
                && mgr.hasTreaty(getFaction().getIdFaction(), (int) dbId, typeId);
    }

    /**
     * How many treaties of this type our faction holds with anyone - for per-type caps.
     */
    public int countOurTreatiesOfType(String typeId) {
        Faction us = getFaction();
        if (us == null || typeId == null) {
            return 0;
        }
        int count = 0;
        for (FactionTreatySet set : mgr.getTreatyMap().values()) {
            for (FactionTreaty t : set.getTreaties()) {
                if (t.involves(us.getIdFaction()) && typeId.equals(t.typeId)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * @param kind PLAYER, NPC or PIRATE, as an acceptance rule's &lt;Argument&gt; spells it.
     */
    public boolean isOfKind(String kind) {
        if (kind == null) {
            return false;
        }
        String k = kind.trim().toUpperCase(Locale.ENGLISH);
        if ("PLAYER".equals(k)) {
            if (isSinglePlayer()) {
                return true;
            }
            Faction f = state.getFactionManager().getFaction((int) dbId);
            return f != null && f.isPlayerFaction();
        }
        if ("NPC".equals(k)) {
            Faction f = isSinglePlayer() ? null : state.getFactionManager().getFaction((int) dbId);
            return f != null && f.isNPC();
        }
        if ("PIRATE".equals(k)) {
            return !isSinglePlayer() && (int) dbId == FactionManager.PIRATES_ID;
        }
        return false;
    }

    /**
     * One point per hour of unbroken agreement, less what each broken one cost.
     *
     * <p>Time held is the only thing here that cannot be bought, which is the point: it is what
     * separates an alliance somebody has kept from one they signed this morning.</p>
     */
    private float calculateTrust() {
        if (!isFaction()) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long held = 0;
        for (FactionTreaty t : mgr.getTreaties(getFaction().getIdFaction(), (int) dbId).getTreaties()) {
            held += Math.max(0, now - t.signedAt);
        }
        float earned = held / (float) TRUST_HOUR_MS;
        return earned - getActionCount(DiplomacyAction.DiplActionType.TREATY_BROKEN) * TRUST_BREAK_PENALTY;
    }

    public boolean isSinglePlayer() {
        return dbId >= Integer.MAX_VALUE;
    }

    public long getDbId() {
        return dbId;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }

    public void modPoints(int pnts) {
        int prevPoints = getPoints();
        setPoints(points + pnts);
        pointCached = recalcPoints();
        setNTChanged();
        //System.err.println("POINT MANUALLY CHANGED for " + state.getFactionManager().getFactionName(fid) + ": " + pnts+"; "+prevPoints+" -> "+getPoints()+"; RAW: "+getRawPoints());
    }

    public enum DiplStatusType {
        IN_WAR(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("At war with them");
            }
        }),
        IN_WAR_WITH_ENEMY(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("At war with their enemies");
            }
        }),
        POWER(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("They see a difference in power");
            }
        }),
        PIRATE(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("They are pirates");
            }
        }),
        ALLIANCE(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("You are in an alliance");
            }
        }),
        ALLIANCE_WITH_ENEMY(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("You're in an alliance with their enemies");
            }
        }),
        NON_AGGRESSION(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("No agression");
            }
        }),
        ALLIANCE_WITH_FRIENDS(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("You're in an alliance with their friends");
            }
        }),
        IN_WAR_WITH_FRIENDS(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("You're in at war with their friends");
            }
        }),
        FACTION_MEMBER_AT_WAR_WITH_US(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("One or more of your faction members is at war with us");
            }
        }),
        FACTION_MEMBER_WE_DONT_LIKE(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("We don't like one or more of your faction members");
            }
        }),
        TRUST(new Translatable() {
            @Override
            public String getName(Enum en) {
                return Lng.str("We have kept agreements with each other");
            }
        }),
        ;
        private final Translatable description;

        DiplStatusType(Translatable description) {
            this.description = description;
        }

        public static String list() {
            return StringTools.listEnum(values());
        }

        public String getDescription() {
            return description.getName(this);
        }
    }
}
