package thederpgamer.betterfactions.data.diplomacy;

import api.mod.config.FileConfiguration;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
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
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import org.schema.schine.resource.tag.Tag.Type;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.diplomacy.action.FactionDiplomacyAction;
import thederpgamer.betterfactions.manager.ConfigManager;
import thederpgamer.betterfactions.manager.FactionDiplomacyManager;
import thederpgamer.betterfactions.manager.NetworkManager;

import java.io.IOException;
import java.util.*;

public class FactionDiplomacy extends Observable implements LogInterface {

	private static final long CHANGE_MOD_DURATION = 11000;
	public Long2ObjectOpenHashMap<FactionDiplomacyEntity> entities = new Long2ObjectOpenHashMap<>();
	private LongOpenHashSet changedEnts = new LongOpenHashSet();

	public final Faction faction;
	final LongOpenHashSet ntChanged = new LongOpenHashSet();
	private boolean first = true;

	public FactionDiplomacy(Faction faction) {
		super();
		this.faction = faction;
	}

	public void initialize() {
		if(faction == null) return;
		if(faction.isOnServer()) {
			Collection<Faction> m = ((FactionState) faction.getState()).getFactionManager().getFactionCollection();
			for(Faction f : m) if(f != faction) onAddedFaction(f);
			for(PlayerState p : ((GameServerState) faction.getState()).getPlayerStatesByName().values()) onPlayerJoined(p);
		}

		execs.add(new TimedExecution() {
			@Override
			public NPCDipleExecType getType() {
				return NPCDipleExecType.STATUS_CALC;
			}

			@Override
			public long getDelay() {
				return getConfig().getLong("diplomacy-status-calc-delay");
			}

			@Override
			public void execute() {
				log("Diplomacy Status Check", LogLevel.NORMAL);
				for(FactionDiplomacyEntity e : entities.values()) {
					e.calculateStaticModifiers(getDelay());
				}
			}
		});
		execs.add(new TimedExecution() {
			@Override
			public NPCDipleExecType getType() {
				return NPCDipleExecType.DIPL_APPLY;
			}

			@Override
			public long getDelay() {
				return getConfig().getLong("diplomacy-apply-delay");
			}

			@Override
			public void execute() {
				log("Diplomacy Turn apply", LogLevel.NORMAL);
				for(FactionDiplomacyEntity e : entities.values()) {
					e.applyDynamicModifiers(getDelay());
				}
			}
		});
		execs.add(new TimedExecution() {
			@Override
			public NPCDipleExecType getType() {
				return NPCDipleExecType.DIPL_CHANGE_CHECK;
			}

			@Override
			public long getDelay() {
				return getConfig().getLong("diplomacy-change-check-delay");
			}

			@Override
			public void execute() {
				log("Calculate Diplomacy Turn change " + getDelay(), LogLevel.NORMAL);
				for(FactionDiplomacyEntity e : entities.values()) {
					e.calculateDiplomacyModifiersFromActions(getDelay());
				}
			}
		});
		execs.add(new TimedExecution() {
			@Override
			public NPCDipleExecType getType() {
				return NPCDipleExecType.DIPL_ON_ACTION;
			}

			@Override
			public long getDelay() {
				return CHANGE_MOD_DURATION;
			}

			@Override
			public void execute() {
				for(long l : changedEnts) {
					//only done when change is triggered
					FactionDiplomacyEntity FactionDiplomacyEntity = entities.get(l);
					if(FactionDiplomacyEntity != null) {
						FactionDiplomacyEntity.calculateDiplomacyModifiersFromActions(0);
						FactionDiplomacyEntity.calculateStaticModifiers(0);
					}
				}
				changedEnts.clear();
			}
		});
	}

	public FileConfiguration getConfig() {
		return ConfigManager.getDiplomacyConfig();
	}

	public void diplomacyAction(FactionDiplomacyAction.DiploActionType type, long otherDbId) {
		FactionDiplomacyEntity e = entities.get(otherDbId);
		if(e == null) {
			e = new FactionDiplomacyEntity((FactionState) faction.getState(), faction.getIdFaction(), otherDbId);
			e.setPoints(getConfig().getInt("diplomacy-start-points"));
			entities.put(otherDbId, e);
		}
		e.diplomacyAction(type);
		changedEnts.add(e.getDbId());
		ntChanged.add(e.getDbId());
		FactionDiplomacyManager.diplomacyChanged.add(this);
	}

	public void trigger(NPCDipleExecType type) {
		for(TimedExecution c : execs){
			if(c.getType() == type){
				c.forceTrigger();
				break;
			}
		}
	}

	public void fromNetwork(PacketReadBuffer packetReadBuffer) throws IOException {
		entities.clear();
		int size = packetReadBuffer.readInt();
		for(int i = 0; i < size; i++) {
			FactionDiplomacyEntity e = new FactionDiplomacyEntity((FactionState) faction.getState(), faction.getIdFaction(), packetReadBuffer.readLong());
			e.fromNetwork(packetReadBuffer);
			entities.put(e.getDbId(), e);
		}
	}

	public void toNetwork(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeInt(entities.size());
		for(FactionDiplomacyEntity e : entities.values()) {
			packetWriteBuffer.writeLong(e.getDbId());
			e.toNetwork(packetWriteBuffer);
		}
	}

	public enum NPCDipleExecType{
		STATUS_CALC,
		DIPL_APPLY,
		DIPL_CHANGE_CHECK,
		DIPL_ON_ACTION,
	}

	private List<TimedExecution> execs = new ObjectArrayList<TimedExecution>();


	public void update(long time) {
		if(first){
			initialize();
			assert(entities.size() > 0);
			first = false;

		}
		for(TimedExecution exe : execs){
			exe.update(time);
		}
	}

	private abstract class TimedExecution{
		long lastT = -1;
		long timeElapsed;
		public abstract long getDelay();
		public void forceTrigger() {
			timeElapsed = getDelay()+1;
			log("Forced Trigger "+getType().name(), LogLevel.NORMAL);
		}
		public abstract void execute();
		public abstract NPCDipleExecType getType();
		public void update(long time){
			if(lastT < 0) lastT = time;
			timeElapsed += (time - lastT);
			if(timeElapsed > getDelay()){
				log("Executing "+getType().name(), LogLevel.NORMAL);
				execute();
				timeElapsed -= getDelay();
			}
			lastT = time;
		}
	}

	@Override
	public void log(String string, LogLevel l) {
		BetterFactions.log.info("[DIPLOMACY]" + string);
	}

	public Tag toTag() {
		Tag[] t = new Tag[entities.size()+1];
		t[t.length-1] = FinishTag.INST;
		int i = 0;
		for(FactionDiplomacyEntity e : entities.values()) {
			t[i] = e.toTag();
			i++;
		}
		return new Tag(Type.STRUCT, null, new Tag[]{
				new Tag(Type.BYTE, null, (byte)0),
				new Tag(Type.STRUCT, null, t),
				FinishTag.INST
		});
	}

	public void fromTag(Tag tag) {
		Tag[] t = tag.getStruct();
		byte version = t[0].getByte();
		Tag[] ents = t[1].getStruct();
		for(int i = 0; i < ents.length-1; i++){
			FactionDiplomacyEntity e = new FactionDiplomacyEntity((FactionState) faction.getState(), faction.getIdFaction());
			e.fromTag(ents[i]);
			entities.put(e.getDbId(), e);
		}
	}

	private void sendFor(SendableGameState g, FactionDiplomacyEntity e){
		if(e.isSinglePlayer()) {
			PlayerState playerState = ((GameServerState)g.getState()).getPlayerStatesByDbId().get(e.getDbId());
			if(playerState != null && playerState.getClientChannel() != null) {
				log("SENDING NT PLAYER DIPLOMACY TO "+playerState+": "+e, LogLevel.DEBUG);
				NetworkManager.sendToPlayer(playerState, this);
			}
		} else {
			Faction f = ((FactionState)g.getState()).getFactionManager().getFaction((int) e.getDbId());
			if(f != null && f.isPlayerFaction()) {
				Map<String, FactionPermission> membersUID = f.getMembersUID();
				for(String mem : membersUID.keySet()) {
					PlayerState playerState = ((GameServerState)g.getState()).getPlayerFromNameIgnoreCaseWOException(mem.toLowerCase(Locale.ENGLISH));
					if(playerState != null && playerState.getClientChannel() != null) {
						log("SENDING NT FACTION DIPLOMACY TO "+playerState+": "+e, LogLevel.DEBUG);
						NetworkManager.sendToPlayer(playerState, this);
					}
				}
			}
		}
	}

	public void checkFactionSending(SendableGameState g, boolean force) {
		if(force) for(FactionDiplomacyEntity e : entities.values())sendFor(g, e);
		else {
			for(long k : ntChanged) {
				FactionDiplomacyEntity e = entities.get(k);
				if(e != null) sendFor(g, e);
			}
			ntChanged.clear();
		}
	}

	public String printFor(PlayerState player) {
		StringBuffer b = new StringBuffer();
		FactionDiplomacyEntity FactionDiplomacyEntity = entities.get(player.getDbId());
		if(FactionDiplomacyEntity != null){
			b.append("PLAYER DIPLOMACY: \n");
			b.append(FactionDiplomacyEntity+"\n");
		}
		FactionDiplomacyEntity = entities.get(player.getFactionId());
		if(FactionDiplomacyEntity != null){
			b.append("PLAYER FACTION DIPLOMACY: \n");
			b.append(FactionDiplomacyEntity+"\n");
		}
		if(b.length() == 0) b.append("No Values yet for "+player.getName());
		return b.toString();
	}

	public void onAddedFaction(Faction f) {
		FactionDiplomacyEntity e = entities.get(f.getIdFaction());
		if(e == null) {
			e = new FactionDiplomacyEntity((FactionState) faction.getState(), faction.getIdFaction(), (long)f.getIdFaction());
			e.setPoints(getConfig().getInt("diplomacy-start-points"));
			entities.put(f.getIdFaction(), e);
		}
		ntChanged(f.getIdFaction());
	}

	public void onPlayerJoined(PlayerState p) {
		FactionDiplomacyEntity e = entities.get(p.getDbId());
		if(e == null) {
			e = new FactionDiplomacyEntity((FactionState) faction.getState(), faction.getIdFaction(), p.getDbId());
			e.setPoints(getConfig().getInt("diplomacy-start-points"));
			entities.put(p.getDbId(), e);
			e.calculateStaticModifiers(0);
			e.applyDynamicModifiers(0);
		}
		ntChanged(p.getDbId());
		((FactionState) faction.getState()).getFactionManager().needsSendAll.add(p);
	}

	public void onDeletedFaction(Faction f) {
		entities.remove(f.getIdFaction());
	}

	public void ntChanged(long dbId) {
		ntChanged.add(dbId);
		FactionDiplomacyManager.diplomacyChanged.add(this);
	}

	public void onClientChanged() {
		setChanged();
		notifyObservers();
	}
}