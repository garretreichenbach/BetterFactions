package videogoose.betterfactions.data.persistent.federation;

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
    public static final int ACCEPT = 3;
    public static final int DENY = 4;
    public static final int COUNTER = 5;

    public enum MessageCategory {ALL, GENERAL, ALLIANCE, FEDERATION, DIPLOMATIC, WAR, TRADE, READ, UNREAD}

    public enum MessageType {
        ALL(MessageCategory.ALL, "ALL"), UNREAD(MessageCategory.GENERAL, "UNREAD"), MESSAGE(MessageCategory.GENERAL, "MESSAGE"), REPLY(MessageCategory.GENERAL, "REPLY"),
        ALLIANCE_OFFER(MessageCategory.ALLIANCE, "ALLIANCE OFFER"), ALLIANCE_BREAK(MessageCategory.ALLIANCE, "ALLIANCE BREAK"),
        FEDERATION_INVITE(MessageCategory.FEDERATION, "FEDERATION INVITE"), FEDERATION_REQUEST(MessageCategory.FEDERATION, "FEDERATION JOIN REQUEST"),
        NON_AGGRESSION_PACT(MessageCategory.DIPLOMATIC, "NON-AGGRESSION PACT OFFER"), CANCEL_NON_AGGRESSION_PACT(MessageCategory.DIPLOMATIC, "CANCEL NON-AGGRESSION PACT"),
        GUARANTEE_INDEPENDENCE(MessageCategory.DIPLOMATIC, "GUARANTEE INDEPENDENCE"), CANCEL_GUARANTEE(MessageCategory.DIPLOMATIC, "CANCEL GUARANTEE"),
        DEMAND_CONCESSION(MessageCategory.WAR, "DEMAND CONCESSION"), DECLARE_WAR(MessageCategory.WAR, "WAR DECLARATION"), OFFER_PEACE(MessageCategory.WAR, "PEACE OFFER"),
        OFFER_TRADE(MessageCategory.TRADE, "TRADE OFFER"), CANCEL_TRADE(MessageCategory.TRADE, "TRADE CANCELLATION"),
        COUNTER_OFFER(MessageCategory.DIPLOMATIC, "COUNTER OFFER"),
        IMPROVE_RELATIONS(MessageCategory.DIPLOMATIC, "IMPROVING RELATIONS"), DECREASE_RELATIONS(MessageCategory.DIPLOMATIC, "WORSENING RELATIONS"),
        INSULT(MessageCategory.DIPLOMATIC, "INSULT"), SEND_GIFT(MessageCategory.DIPLOMATIC, "GIFT"),
        EMBARGO(MessageCategory.DIPLOMATIC, "EMBARGO"), CANCEL_EMBARGO(MessageCategory.DIPLOMATIC, "EMBARGO LIFTED"),
        CANCEL_GUARANTEE(MessageCategory.DIPLOMATIC, "GUARANTEE REVOKED"),
        BAN_DIPLOMATS(MessageCategory.DIPLOMATIC, "DIPLOMATS BANNED");

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
    public String counterButtonText;
    public MessageType originalType; // For counter-offers: tracks the original proposal type

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
        this.counterButtonText = "";
        switch (messageType) {
            case OFFER_PEACE, DEMAND_CONCESSION, COUNTER_OFFER -> {
                acceptButtonText = "ACCEPT";
                denyButtonText = "DENY";
                counterButtonText = "COUNTER";
            }
            case OFFER_TRADE, ALLIANCE_OFFER, NON_AGGRESSION_PACT -> {
                acceptButtonText = "ACCEPT";
                denyButtonText = "DENY";
            }
            case FEDERATION_INVITE, FEDERATION_REQUEST -> {
                acceptButtonText = "ACCEPT";
                denyButtonText = "DENY";
            }
            default -> {
                acceptButtonText = "";
                denyButtonText = "";
            }
        }
    }

    public Faction getSender() {
        return GameCommon.getGameState().getFactionManager().getFaction(fromId);
    }

    public Faction getRecipient() {
        return GameCommon.getGameState().getFactionManager().getFaction(toId);
    }
}
