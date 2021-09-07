package thederpgamer.betterfactions.utils;

import api.common.GameClient;
import api.common.GameCommon;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.manager.LogManager;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/07/2021]
 */
public class DataUtils {

    public static String getResourcesPath() {
        return BetterFactions.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
    }

    public static String getWorldDataPath() {
        try {
            String universeName = GameCommon.getUniqueContextId();
            if(!universeName.contains(":")) return getResourcesPath() + "/data/" + universeName;
            else throw new IllegalStateException("Clients cannot be here!");
        } catch(Exception exception) {
            LogManager.logException("Client " + GameClient.getClientPlayerState().getName() + " attempted to illegally access server data.", exception);
        }
        return null;
    }
}
