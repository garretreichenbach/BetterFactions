package videogoose.betterfactions.data.persistent.diplomacy;

import videogoose.betterfactions.data.persistent.PersistentData;
import videogoose.betterfactions.manager.NetworkSyncManager;


public class DiplomaticDataOld implements PersistentData {

    private transient boolean needsUpdate = true;

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
