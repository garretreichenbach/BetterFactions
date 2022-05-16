package thederpgamer.betterfactions.data.faction;

import api.common.GameClient;
import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.forms.Sprite;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.federation.Federation;
import thederpgamer.betterfactions.data.old.federation.FactionMessage;
import thederpgamer.betterfactions.manager.ResourceManager;
import thederpgamer.betterfactions.manager.data.FactionDataManager;
import thederpgamer.betterfactions.manager.data.FactionMemberManager;
import thederpgamer.betterfactions.manager.data.FactionMessageManager;
import thederpgamer.betterfactions.manager.data.FederationManager;

import java.awt.geom.RectangularShape;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class FactionData implements SerializationInterface {

	public static final int FP = 0;
	public static final int INFLUENCE = 1;
	public static final int TERRITORY = 2;
	public static final int ECONOMIC = 3;
	public static final int MILITARY = 4;
	public static final int DIPLOMATIC = 5;

	private int id;
	private String name;
	private final int[] score = new int[6];
	private final ArrayList<String> members = new ArrayList<>();
	private String logo = "default-logo";
	private final ArrayList<Integer> inbox = new ArrayList<>();

	public FactionData(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public FactionData(Faction faction) {
		this.id = faction.getIdFaction();
		this.name = faction.getName();
		this.score[0] = (int) faction.factionPoints;
		this.members.addAll(faction.getMembersUID().keySet());
	}

	public FactionData(PacketReadBuffer readBuffer) throws IOException {
		deserialize(readBuffer);
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Federation getFederation() {
		return FederationManager.instance.getFederation(this);
	}

	public void setFederation(Federation federation) {
		federation.addMember(this);
	}

	@Override
	public boolean equals(SerializationInterface data) {
		return data instanceof FactionData && data.getId() == getId() && data.getName().equals(getName());
	}

	@Override
	public void deserialize(PacketReadBuffer readBuffer) throws IOException {
		id = readBuffer.readInt();
		name = readBuffer.readString();
		for(int i = 0; i < score.length; i ++) score[i] = readBuffer.readInt();
		int size = readBuffer.readInt();
		for(int i = 0; i < size; i ++) members.add(readBuffer.readString());
	}

	@Override
	public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeInt(id);
		writeBuffer.writeString(name);
		for(int i : score) writeBuffer.writeInt(i);
		writeBuffer.writeInt(members.size());
		for(String member : members) writeBuffer.writeString(member);
	}

	public FactionMember getMember(PlayerState playerState) {
		return FactionMemberManager.instance.getMember(this, playerState);
	}

	public FactionMember getMember(String name) {
		return FactionMemberManager.instance.getMember(this, name);
	}

	public ArrayList<FactionMember> getMembers() {
		ArrayList<FactionMember> list = new ArrayList<>();
		for(String name : members) list.add(FactionMemberManager.instance.getMember(this, name));
		return list;
	}

	public void addMember(String name) {
		members.add(name);
	}

	public void removeMember(String name) {
		members.remove(name);
	}

	public Faction getFaction() {
		return GameCommon.getGameState().getFactionManager().getFaction(id);
	}

	public ArrayList<FactionRank> getAllRanks() {
		ArrayList<FactionRank> ranks = new ArrayList<>();
		for(FactionMember member : getMembers()) {
			if(!ranks.contains(member.getRank())) ranks.add(member.getRank());
		}
		return ranks;
	}

	public void setFactionLogo(String logo) {
		this.logo = logo;
	}

	public Sprite getFactionLogo() {
		return ResourceManager.getSprite(logo);
	}

	public ArrayList<? extends FactionMessage> getInbox() {
		ArrayList<FactionMessage> messages = new ArrayList<>();
		for(int i : inbox) {
			if(FactionMessageManager.instance.getCache().asMap().containsKey(i)) messages.add(FactionMessageManager.instance.getCache().asMap().get(i));
		}
		return messages;
 	}

	public void addMessage(FactionMessage factionMessage) {
		inbox.add(factionMessage.getId());
		FactionMessageManager.instance.saveData(factionMessage);
		FactionMessageManager.instance.sendDataToClients(factionMessage);
	}

	public void removeMessage(FactionMessage message) {
		inbox.remove(message.getId());
		FactionMessageManager.instance.getCache().asMap().remove(message.getId());
		FactionMessageManager.instance.sendAllDataToClients();
	}
}