package thederpgamer.betterfactions.utils;

import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.data.FactionData;
import java.util.HashMap;

/**
 * FactionUtils.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionUtils {

    private static HashMap<Faction, FactionData> factionInfo = new HashMap<>();

    public static FactionData getFactionInfo(Faction faction) {
        if(factionInfo.containsKey(faction)) {
            return factionInfo.get(faction);
        } else {
            return null;
        }
    }
}
