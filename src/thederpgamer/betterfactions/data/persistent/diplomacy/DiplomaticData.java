package thederpgamer.betterfactions.data.persistent.diplomacy;

import thederpgamer.betterfactions.data.persistent.PersistentData;
import thederpgamer.betterfactions.manager.NetworkSyncManager;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/16/2021]
 */
public class DiplomaticData implements PersistentData {

    private boolean needsUpdate = true;

    @Override
    public int getDataType() {
        return NetworkSyncManager.DIPLOMATIC_DATA;
    }

    @Override
    public int getDataId() {
        return 0; //Todo
    }

    @Override
    public boolean needsUpdate() {
        return needsUpdate;
    }

    @Override
    public void queueUpdate(boolean update) {
        needsUpdate = update;
    }
}
