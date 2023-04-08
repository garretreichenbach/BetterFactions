package thederpgamer.betterfactions.data.persistent;

/**
 * Interface for persistent data.
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/15/2021]
 */
public interface PersistentData {

    int getDataType();
    int getDataId();
    boolean needsUpdate();
    void queueUpdate(boolean update);
}
