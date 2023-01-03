package thederpgamer.betterfactions.data.news;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.network.objects.NetworkGameState;
import org.schema.game.network.objects.remote.RemoteNPCFactionNews;
import org.schema.game.server.data.simulation.npc.news.*;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.Observable;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class FactionNews extends Observable {

	public enum FactionNewsEventType {
		GROWN(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventGrown();
			}
		}),
		WAR_GOAL(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventWarGoal();
			}
		}),
		WAR(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventWar();
			}
		}),
		PEACE(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventPeace();
			}
		}),
		ALLIES(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventAlly();
			}
		}),
		FEDERATION_CREATED(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventFederationCreated();
			}
		}),
		FEDERATION_JOIN(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventFederationJoin();
			}
		}),
		FEDERATION_LEAVE(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventFederationLeave();
			}
		}),
		FEDERATION_DISBAND(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventFederationDisband();
			}
		}),
		NON_AGGRESSION_PACT(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventNonAggressionPact();
			}
		}),
		TRADING(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventTrading();
			}
		}),
		LOST_STATION(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventLostStation();
			}
		}),
		LOST_TERRITORY(new FactionNews.FactionNewsEventType.CC() {
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventLostSystem();
			}
		}),
		BATTLE(new FactionNews.FactionNewsEventType.CC(){
			@Override
			public FactionNewsEvent inst() {
				return new FactionNewsEventBattle();
			}
		});

		private interface CC {
			FactionNewsEvent inst();
		}

		private final FactionNews.FactionNewsEventType.CC c;

		public FactionNewsEvent instance() {
			FactionNewsEvent inst = c.inst();
			if(!inst.getType().equals(this)) throw new IllegalArgumentException("Wrong instance: "+inst.getType()+"; ;; "+this);
			return inst;
		}

		FactionNewsEventType(FactionNews.FactionNewsEventType.CC c) {
			this.c = c;
		}
	}
	private final FactionManager man;

	public FactionNews(FactionManager man){
		this.man = man;
	}

	public void grown(int factionId, Vector3i system){
		NPCFactionNewsEventGrown c = new NPCFactionNewsEventGrown();
		c.factionId = factionId;
		c.time = System.currentTimeMillis();

		c.system.set(system);
		addEvent(c);
	}

	public void lostSystem(int factionId, Vector3i system){
		NPCFactionNewsEventLostSystem c = new NPCFactionNewsEventLostSystem();
		c.factionId = factionId;
		c.time = System.currentTimeMillis();

		c.system.set(system);
		addEvent(c);
	}

	public void trading(int factionId, Vector3i from, Vector3i to){
		NPCFactionNewsEventTrading c = new NPCFactionNewsEventTrading();
		c.factionId = factionId;
		c.time = System.currentTimeMillis();
		assert(from != null);
		assert(to != null);
		c.from.set(from);
		c.to.set(to);
		addEvent(c);
	}

	public void war(int factionId, String otherEnt){
		NPCFactionNewsEventWar c = new NPCFactionNewsEventWar();
		c.factionId = factionId;
		c.time = System.currentTimeMillis();

		c.otherEnt = otherEnt;
		addEvent(c);
	}

	public void peace(int factionId, String otherEnt){
		NPCFactionNewsEventNeutral c = new NPCFactionNewsEventNeutral();
		c.factionId = factionId;
		c.time = System.currentTimeMillis();

		c.otherEnt = otherEnt;
		addEvent(c);
	}

	public void ally(int factionId, String otherEnt){
		NPCFactionNewsEventAlly c = new NPCFactionNewsEventAlly();
		c.factionId = factionId;
		c.time = System.currentTimeMillis();

		c.otherEnt = otherEnt;
		addEvent(c);
	}

	public void lostStation(int factionId, String otherEnt) {
		NPCFactionNewsEventLostStation c = new NPCFactionNewsEventLostStation();
		c.factionId = factionId;
		c.time = System.currentTimeMillis();

		c.otherEnt = otherEnt;
		addEvent(c);
	}

	private void addEvent(NPCFactionNewsEvent c) {
		events.add(0, c);

		while(events.size() > 100){
			events.remove(events.size()-1);
		}
		if(man.isOnServer()){
			send(c);
		}

		setChanged();
		notifyObservers();
	}


	private void send(NPCFactionNewsEvent c) {
		man.getGameState().getNetworkObject().npcFactionNewsBuffer.add(new RemoteNPCFactionNews(c, man.getGameState().getNetworkObject()));
	}


	public final ObjectArrayList<NPCFactionNewsEvent> events = new ObjectArrayList<NPCFactionNewsEvent>();
	private final ObjectArrayList<NPCFactionNewsEvent>  toAddClient = new ObjectArrayList<NPCFactionNewsEvent> ();



	public void fromTag(Tag tag){
		Tag[] t = tag.getStruct();

		byte version = t[0].getByte();
		Tag[] v = t[1].getStruct();

		for(int i = 0; i < v.length-1; i++){
			events.add((NPCFactionNewsEvent) v[i].getValue());
		}
	}
	public Tag toTag(){
		Tag[] t = new Tag[events.size()+1];
		t[t.length-1] = FinishTag.INST;

		for(int i = 0; i < events.size(); i++){
			t[i] = new Tag(Tag.Type.SERIALIZABLE, null, events.get(i));
		}

		return new Tag(Tag.Type.STRUCT, null, new Tag[]{
				new Tag(Tag.Type.BYTE, null, (byte)0),
				new Tag(Tag.Type.STRUCT, null, t),
				FinishTag.INST});

	}
	public void updateFromNetworkObject(NetworkGameState networkObject) {
		ObjectArrayList<RemoteNPCFactionNews> r = networkObject.npcFactionNewsBuffer.getReceiveBuffer();
		for(RemoteNPCFactionNews remoteNPCFactionNews : r) toAddClient.add(remoteNPCFactionNews.get());
	}

	public void updateLocal(Timer t) {
		for(NPCFactionNewsEvent npcFactionNewsEvent : toAddClient) addEvent(npcFactionNewsEvent);
		toAddClient.clear();
	}

	public void initFromNetworkObject(NetworkGameState networkObject) {
		ObjectArrayList<RemoteNPCFactionNews> r = networkObject.npcFactionNewsBuffer.getReceiveBuffer();
		for(RemoteNPCFactionNews remoteNPCFactionNews : r) toAddClient.add(remoteNPCFactionNews.get());
	}

	public void updateToFullNetworkObject(NetworkGameState networkObject) {
		for(int i = events.size() - 1; i >= 0; i --){
			NPCFactionNewsEvent e = events.get(i);
			send(e);
		}
	}
}
