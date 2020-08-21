package dovtech.betterfactions.faction.diplo.relations;

import dovtech.betterfactions.BetterFactions;
import org.schema.game.common.data.player.faction.Faction;

public class FactionMessage {

    private String subject;
    private String message;
    private Faction from;
    private Faction to;
    private MessageType messageType;

    public FactionMessage(Faction from, Faction to, MessageType messageType) {
        this.from = from;
        this.to = to;
        this.messageType = messageType;
        String fromAllianceName = BetterFactions.getInstance().getFactionAlliance(from).getName();
        String toAllianceName = BetterFactions.getInstance().getFactionAlliance(to).getName();
        switch(messageType) {
            case TEXT:
                this.subject = "No Title";
                this.message = "No Message";
            case ALLIANCE_INVITE:
                this.subject = "Invite to " + fromAllianceName + " from " + from.getName();
                this.message = from.getName() + " has invited your faction to their Alliance " + fromAllianceName + ".";
            case JOIN_ALLIANCE_INVITE:
                this.subject = "Request to join " + toAllianceName + " from " + from.getName();
                this.message = from.getName() + " has requested to join your Alliance " + toAllianceName + ".";
            case ALLY_REQUEST:
                this.subject = "Ally request from " + from.getName();
                this.message = from.getName() + " has requested an alliance with your faction " + to.getName();
        }
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Faction getFrom() {
        return from;
    }

    public Faction getTo() {
        return to;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public enum MessageType {
        TEXT,
        ALLIANCE_INVITE,
        JOIN_ALLIANCE_INVITE,
        ALLY_REQUEST
    }
}
