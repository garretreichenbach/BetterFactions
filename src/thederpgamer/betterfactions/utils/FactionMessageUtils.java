package thederpgamer.betterfactions.utils;

import api.common.GameCommon;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.FactionRelation;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [10/18/2021]
 */
public class FactionMessageUtils {

    private static final String[][] messages = {
            { //Pirates
                "Sorry mate, we don't speak idiot!", //GENERAL
                "Piss off you stupid bastard!", //ALLIANCE
                "Hows about you join the federation of dead wankers instead? You'll fit right in soon enough!", //FEDERATION
                "Piss off you stupid bastard!", //DIPLOMATIC
                "Har har har! Now that's a funny one!", //WAR
                "I'll do you one better - give us all your credits and maybe we won't shoot you for a bit!" //TRADE
            },
            { //Traders
                "Apologies, but unless it's important we can't spare the time for this conversation.", //GENERAL
                "We appreciate the offer but we'd rather stay neutral for now...", //ALLIANCE
                "We appreciate the offer but we'd rather stay neutral for now...", //FEDERATION
                "We appreciate the offer but we'd rather stay neutral for now...", //DIPLOMATIC
                "You'll regret that!", //WAR
                "If you wish to trade, please visit one of our shop stations." //TRADE
            }
    };

    public static String getResponseMessage(FactionMessage.MessageType type, Faction to, Faction from) {
        FactionRelation.RType relation = GameCommon.getGameState().getFactionManager().getRelation(to.getIdFaction(), from.getIdFaction());
        if(to.getIdFaction() == FactionManager.PIRATES_ID) {
            return messages[0][type.category.ordinal() - 1];
        } else if(to.getIdFaction() == FactionManager.TRAIDING_GUILD_ID) {
            if(!type.equals(FactionMessage.MessageType.OFFER_PEACE)) return messages[1][type.category.ordinal() - 1];
            else return ""; //Todo
        }
        return "";
    }
}
