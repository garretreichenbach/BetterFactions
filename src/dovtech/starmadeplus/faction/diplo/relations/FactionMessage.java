package dovtech.starmadeplus.faction.diplo.relations;

import dovtech.starmadeplus.BetterFactions;
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
        String fromPactName = BetterFactions.getInstance().getFactionPact(from).getName();
        String toPactName = BetterFactions.getInstance().getFactionPact(to).getName();
        switch(messageType) {
            case TEXT:
                this.subject = "No Title";
                this.message = "No Message";
            case PACT_INVITE:
                this.subject = "Invite to " + fromPactName + " from " + from.getName();
                this.message = from.getName() + " has invited your faction to their Pact " + fromPactName + ".";
            case JOIN_PACT_REQUEST:
                this.subject = "Request to join " + toPactName + " from " + from.getName();
                this.message = from.getName() + " has requested to join your Pact " + toPactName + ".";
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
        PACT_INVITE,
        JOIN_PACT_REQUEST,
        ALLY_REQUEST
    }
}
