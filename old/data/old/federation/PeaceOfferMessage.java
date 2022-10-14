package thederpgamer.betterfactions.data.old.federation;

import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.data.old.diplomacy.peace.PeaceOfferData;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/15/2021]
 */
public class PeaceOfferMessage extends FactionMessage {

    public PeaceOfferData peaceOfferData;

    public PeaceOfferMessage(Faction from, Faction to, String title, PeaceOfferData peaceOfferData) {
        super(from, to, title, "");
        this.messageType = MessageType.OFFER_PEACE;
        this.peaceOfferData = peaceOfferData;
    }
}
