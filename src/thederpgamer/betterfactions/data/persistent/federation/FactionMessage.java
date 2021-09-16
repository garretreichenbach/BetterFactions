package thederpgamer.betterfactions.data.persistent.federation;

import api.common.GameCommon;
import org.schema.game.common.data.player.faction.Faction;
import java.io.Serializable;
import java.util.Date;

/**
 * FactionMessage.java
 * <Description>
 * ==================================================
 * Created 02/09/2021
 * @author TheDerpGamer
 */
public class FactionMessage implements Serializable {

    public enum MessageType {NONE, FEDERATION_INVITE, FEDERATION_REQUEST}

    public int fromId;
    public int toId;
    public String title;
    public String message;
    public MessageType messageType;
    public String date;

    public FactionMessage(Faction from, Faction to, String title, String message) {
        this(from, to, title, message, MessageType.NONE);
    }

    public FactionMessage(Faction from, Faction to, String title, String message, MessageType messageType) {
        fromId = from.getIdFaction();
        toId = to.getIdFaction();
        date = new Date().toString();
        this.title = title;
        this.message = message;
        this.messageType = messageType;
    }

    public Faction getSender() {
        return GameCommon.getGameState().getFactionManager().getFaction(fromId);
    }

    public Faction getRecipient() {
        return GameCommon.getGameState().getFactionManager().getFaction(toId);
    }
}
