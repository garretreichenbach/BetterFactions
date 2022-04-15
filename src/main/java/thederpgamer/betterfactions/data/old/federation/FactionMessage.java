package thederpgamer.betterfactions.data.old.federation;

import api.common.GameCommon;
import org.apache.commons.lang3.text.WordUtils;
import org.schema.game.common.data.player.faction.Faction;

/**
 * Faction diplomatic message persistent data.
 *
 * @version 1.0 - [02/09/2021]
 * @author TheDerpGamer
 */
public class FactionMessage {

    public static final int MARK_READ = 0;
    public static final int MARK_UNREAD = 1;
    public static final int DELETE = 2;

    public enum MessageCategory {ALL, GENERAL, ALLIANCE, FEDERATION, DIPLOMATIC, WAR, TRADE, READ, UNREAD}

    public enum MessageType {
        ALL(MessageCategory.ALL, "ALL"), UNREAD(MessageCategory.GENERAL, "UNREAD"), MESSAGE(MessageCategory.GENERAL, "MESSAGE"), REPLY(MessageCategory.GENERAL, "REPLY"),
        ALLIANCE_OFFER(MessageCategory.ALLIANCE, "ALLIANCE OFFER"), ALLIANCE_BREAK(MessageCategory.ALLIANCE, "ALLIANCE BREAK"),
        FEDERATION_INVITE(MessageCategory.FEDERATION, "FEDERATION INVITE"), FEDERATION_REQUEST(MessageCategory.FEDERATION, "FEDERATION JOIN REQUEST"),
        NON_AGGRESSION_PACT(MessageCategory.DIPLOMATIC, "NON-AGGRESSION PACT OFFER"), CANCEL_NON_AGGRESSION_PACT(MessageCategory.DIPLOMATIC, "CANCEL NON-AGGRESSION PACT"),
        GUARANTEE_INDEPENDENCE(MessageCategory.DIPLOMATIC, "GUARANTEE INDEPENDENCE"), CANCEL_GUARANTEE(MessageCategory.DIPLOMATIC, "CANCEL GUARANTEE"),
        DEMAND_CONCESSION(MessageCategory.WAR, "DEMAND CONCESSION"), DECLARE_WAR(MessageCategory.WAR, "WAR DECLARATION"), OFFER_PEACE(MessageCategory.WAR, "PEACE OFFER"),
        OFFER_TRADE(MessageCategory.TRADE, "TRADE OFFER"), CANCEL_TRADE(MessageCategory.TRADE, "TRADE CANCELLATION");

        public MessageCategory category;
        public String display;

        MessageType(MessageCategory category, String display) {
            this.category = category;
            this.display = display;
        }

        public String getDisplayFormatted() {
            return WordUtils.capitalize(display.toLowerCase());
        }
    }

    public int fromId;
    public int toId;
    public String title;
    public String message;
    public MessageType messageType;
    public long date;
    public boolean read;
    public String acceptButtonText;
    public String denyButtonText;

    public FactionMessage(Faction from, Faction to, String title, String message) {
        this(from, to, title, message, MessageType.MESSAGE);
    }

    public FactionMessage(Faction from, Faction to, String title, String message, MessageType messageType) {
        this.fromId = from.getIdFaction();
        this.toId = to.getIdFaction();
        this.title = title;
        this.message = message;
        this.messageType = messageType;
        this.date = System.currentTimeMillis();
        switch(messageType) {
            case OFFER_PEACE:
            case OFFER_TRADE:
            case ALLIANCE_OFFER:
            case DEMAND_CONCESSION:
            case FEDERATION_INVITE:
            case FEDERATION_REQUEST:
            case NON_AGGRESSION_PACT:
                acceptButtonText = "ACCEPT";
                denyButtonText = "DENY";
                break;
            default:
                acceptButtonText = "";
                denyButtonText = "";
                break;
        }
    }

    public Faction getSender() {
        return GameCommon.getGameState().getFactionManager().getFaction(fromId);
    }

    public Faction getRecipient() {
        return GameCommon.getGameState().getFactionManager().getFaction(toId);
    }
}
