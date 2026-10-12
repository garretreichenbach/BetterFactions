package videogoose.betterfactions.diplomacy;

import videogoose.betterfactions.DiplomacyManager;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import org.schema.common.util.LogInterface;
import org.schema.game.common.data.SendableGameState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.server.data.FactionState;
import org.schema.game.server.data.GameServerState;
import videogoose.betterfactions.diplomacy.DiplomacyAction.DiplActionType;
import org.schema.schine.resource.tag.Tag;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag.Type;

public class FactionDiplomacy extends Observable implements LogInterface {


    private static final long CHANGE_MOD_DURATION = 11000;
    public final Faction faction;
    final LongOpenHashSet ntChanged = new LongOpenHashSet();
    public Long2ObjectOpenHashMap<DiplomacyEntity> entities = new Long2ObjectOpenHashMap<DiplomacyEntity>();
    private LongOpenHashSet changedEnts = new LongOpenHashSet();
    private boolean first = true;
    private List<TimedExecution> execs = new ObjectArrayList<TimedExecution>();

    private final DiplomacyManager manager;

    public FactionDiplomacy(Faction faction, DiplomacyManager manager) {
        super();
        this.faction = faction;
        this.manager = manager;
    }

    public void initialize() {
        if (faction.isOnServer()) {
            Collection<Faction> m = ((FactionState) faction.getState()).getFactionManager().getFactionCollection();
            for (Faction f : m) {
                if (f != faction) {
                    onAddedFaction(f);
                }
            }

            for (PlayerState p : GameServerState.instance.getPlayerStatesByName().values()) {
                onPlayerJoined(p);
            }
        }

        execs.add(new TimedExecution() {
            @Override
            public DiplomacyExecType getType() {
                return DiplomacyExecType.STATUS_CALC;
            }

            @Override
            public long getDelay() {
                return getConfig().getDiplomacyStatusCheckDelay();
            }

            @Override
            public void execute() {
                log("Diplomacy Status Check", LogLevel.NORMAL);
                for (DiplomacyEntity e : entities.values()) {
                    e.calculateStaticModifiers(getDelay());
                }
            }
        });
        execs.add(new TimedExecution() {
            @Override
            public DiplomacyExecType getType() {
                return DiplomacyExecType.DIPL_APPLY;
            }

            @Override
            public long getDelay() {
                return getConfig().getDiplomacyTurnEffectDelay();
            }

            @Override
            public void execute() {
                log("Diplomacy Turn apply", LogLevel.NORMAL);
                for (DiplomacyEntity e : entities.values()) {
                    e.applyDynamicModifiers(getDelay());
                }
            }
        });
        execs.add(new TimedExecution() {
            @Override
            public DiplomacyExecType getType() {
                return DiplomacyExecType.DIPL_CHANGE_CHECK;
            }

            @Override
            public long getDelay() {
                return getConfig().getDiplomacyTurnEffectChangeDelay();
            }

            @Override
            public void execute() {
                log("Calculate Diplomacy Turn change " + getDelay(), LogLevel.NORMAL);
                for (DiplomacyEntity e : entities.values()) {
                    e.calculateDiplomacyModifiersFromActions(getDelay());
                }
            }
        });
        execs.add(new TimedExecution() {
            @Override
            public DiplomacyExecType getType() {
                return DiplomacyExecType.DIPL_ON_ACTION;
            }

            @Override
            public long getDelay() {
                return CHANGE_MOD_DURATION;
            }

            @Override
            public void execute() {
                for (long l : changedEnts) {
                    //only done when change is triggered
                    DiplomacyEntity npcDiplomacyEntity = entities.get(l);
                    if (npcDiplomacyEntity != null) {
                        npcDiplomacyEntity.calculateDiplomacyModifiersFromActions(0);
                        npcDiplomacyEntity.calculateStaticModifiers(0);
                    }
                }
                changedEnts.clear();
            }
        });

    }

    public DiplomacySettings getConfig() {
        return DiplomacySettings.of(faction);
    }

    /**
     * The ledger entry for one faction or player, created on first mention.
     */
    public DiplomacyEntity getEntity(long otherDbId) {
        DiplomacyEntity e = entities.get(otherDbId);
        if (e == null) {
            e = new DiplomacyEntity(manager, faction.getIdFaction(), otherDbId);
            e.setPoints(getConfig().getDiplomacyStartPoints());
            entities.put(otherDbId, e);
        }
        return e;
    }

    public void diplomacyAction(DiplActionType type, long otherDbId) {
        diplomacyAction(type, otherDbId, null);
    }

    /**
     * @param subject what the action was about when its type alone is ambiguous - a treaty id
     */
    public void diplomacyAction(DiplActionType type, long otherDbId, String subject) {
        DiplomacyEntity e = getEntity(otherDbId);
        e.diplomacyAction(type, subject);
        changedEnts.add(e.getDbId());
    }

    public void trigger(DiplomacyExecType type) {
        for (TimedExecution c : execs) {
            if (c.getType() == type) {
                c.forceTrigger();
                break;
            }
        }
    }

    public void update(long time) {
        if (first) {
            initialize();
            first = false;

        }
        for (TimedExecution exe : execs) {
            exe.update(time);
        }
    }

    @Override
    public void log(String string, LogLevel l) {
        if (l != LogLevel.FINE) {
            System.err.println("[FACTION][" + faction.getName() + "][DIPLOMACY]" + string);
        }
    }

    public Tag toTag() {
        Tag[] t = new Tag[entities.size() + 1];
        t[t.length - 1] = FinishTag.INST;
        int i = 0;
        for (DiplomacyEntity e : entities.values()) {
            t[i] = e.toTag();
            i++;
        }

        return new Tag(Type.STRUCT, null, new Tag[]{
                new Tag(Type.BYTE, null, (byte) 0),
                new Tag(Type.STRUCT, null, t),
                FinishTag.INST
        });
    }

    public void fromTag(Tag tag) {
        Tag[] t = tag.getStruct();
        byte version = t[0].getByte();
        Tag[] ents = t[1].getStruct();

        System.err.println("[SERVER][FACTION][NPC] laoding diplomacy from tag. Entries; " + ents.length);
        for (int i = 0; i < ents.length - 1; i++) {
            DiplomacyEntity e = new DiplomacyEntity(manager, faction.getIdFaction());
            e.fromTag(ents[i]);
            entities.put(e.getDbId(), e);
        }
    }

    public void checkDiplomacySending(boolean force) {
        for (long k : force ? entities.keySet() : ntChanged) {
            DiplomacyEntity e = entities.get(k);
            if (e != null) {
                manager.sendOpinion(faction, e);
            }
        }
        ntChanged.clear();
    }

    public String printFor(PlayerState player) {
        StringBuilder b = new StringBuilder();
        DiplomacyEntity npcDiplomacyEntity = entities.get(player.getDbId());
        if (npcDiplomacyEntity != null) {
            b.append("PLAYER DIPLOMACY: \n");
            b.append(npcDiplomacyEntity).append("\n");
        }
        npcDiplomacyEntity = entities.get(player.getFactionId());
        if (npcDiplomacyEntity != null) {
            b.append("PLAYER FACTION DIPLOMACY: \n");
            b.append(npcDiplomacyEntity).append("\n");
        }
        if (b.isEmpty()) {
            b.append("No Values yet for ").append(player.getName());
        }
        return b.toString();
    }

    public void onAddedFaction(Faction f) {
        DiplomacyEntity e = entities.get(f.getIdFaction());
        if (e == null) {
            e = new DiplomacyEntity(manager, faction.getIdFaction(), (long) f.getIdFaction());
            e.setPoints(getConfig().getDiplomacyStartPoints());
            entities.put(f.getIdFaction(), e);
        }
        ntChanged(f.getIdFaction());

    }

    public void onPlayerJoined(PlayerState p) {
        DiplomacyEntity e = entities.get(p.getDbId());
        if (e == null) {
            e = new DiplomacyEntity(manager, faction.getIdFaction(), p.getDbId());
            e.setPoints(getConfig().getDiplomacyStartPoints());
            entities.put(p.getDbId(), e);
            e.calculateStaticModifiers(0);
            e.applyDynamicModifiers(0);
        }
        ntChanged(p.getDbId());
    }

    public void onDeletedFaction(Faction f) {
        entities.remove(f.getIdFaction());
    }

    public void ntChanged(long dbId) {
        ntChanged.add(dbId);
        manager.markDiplomacyChanged(this);
    }

    public void onClientChanged() {
        setChanged();
        notifyObservers();
    }

    public enum DiplomacyExecType {
        STATUS_CALC,
        DIPL_APPLY,
        DIPL_CHANGE_CHECK,
        DIPL_ON_ACTION,
    }

    private abstract class TimedExecution {
        long lastT = -1;
        long timeElapsed;

        public abstract long getDelay();

        public void forceTrigger() {
            timeElapsed = getDelay() + 1;
            log("Forced Trigger " + getType().name(), LogLevel.NORMAL);
        }

        public abstract void execute();

        public abstract DiplomacyExecType getType();

        public void update(long time) {
            if (lastT < 0) {
                lastT = time;
            }
            timeElapsed += (time - lastT);

            if (timeElapsed > getDelay()) {
                log("Executing " + getType().name(), LogLevel.NORMAL);
                execute();
                timeElapsed -= getDelay();
            }


            lastT = time;
        }
    }
}
