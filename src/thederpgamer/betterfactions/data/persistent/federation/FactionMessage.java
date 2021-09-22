package thederpgamer.betterfactions.data.persistent.federation;

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

    public static final int GENERAL = 0;
    public static final int ALLIANCE = 1;
    public static final int FEDERATION = 2;
    public static final int DIPLOMATIC = 3;
    public static final int WAR = 4;
    public static final int TRADE = 5;

    public enum MessageType {
        ALL(GENERAL, "ALL"), UNREAD(GENERAL, "UNREAD"), MESSAGE(GENERAL, "MESSAGE"), REPLY(GENERAL, "REPLY"),
        ALLIANCE_OFFER(ALLIANCE, "ALLIANCE OFFER"), ALLIANCE_BREAK(ALLIANCE, "ALLIANCE BREAK"),
        FEDERATION_INVITE(FEDERATION, "FEDERATION INVITE"), FEDERATION_REQUEST(FEDERATION, "FEDERATION JOIN REQUEST"),
        NON_AGGRESSION_PACT(DIPLOMATIC, "NON-AGGRESSION PACT OFFER"), CANCEL_NON_AGGRESSION_PACT(DIPLOMATIC, "CANCEL NON-AGGRESSION PACT"),
        GUARANTEE_INDEPENDENCE(DIPLOMATIC, "GUARANTEE INDEPENDENCE"), CANCEL_GUARANTEE(DIPLOMATIC, "CANCEL GUARANTEE"),
        DEMAND_CONCESSION(WAR, "DEMAND CONCESSION"), DECLARE_WAR(WAR, "WAR DECLARATION"), OFFER_PEACE(WAR, "PEACE OFFER"),
        OFFER_TRADE(TRADE, "TRADE OFFER"), CANCEL_TRADE(TRADE, "TRADE CANCELLATION");

        public int category;
        public String display;

        MessageType(int category, String display) {
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
    }

    public Faction getSender() {
        return GameCommon.getGameState().getFactionManager().getFaction(fromId);
    }

    public Faction getRecipient() {
        return GameCommon.getGameState().getFactionManager().getFaction(toId);
    }
}
