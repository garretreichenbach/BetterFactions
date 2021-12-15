package thederpgamer.betterfactions.data.persistent.faction;

import api.common.GameCommon;
import api.common.GameServer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.schine.network.objects.Sendable;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/14/2021]
 */
public class FactionEntityData {

    public enum EntityType {
        ALL
    }

    public String name;
    public EntityType entityType;
    private Vector3i sector;
    private float status;

    public boolean isValid() {
        return getEntity() != null;
    }

    public SegmentController getEntity() {
        if(GameCommon.isDedicatedServer()) {
            for(Sendable sendable : GameCommon.getGameState().getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
                if(sendable instanceof SegmentController && GameCommon.getGameObject(sendable.getId()) == sendable && ((SegmentController) sendable).getRealName().equals(name)) return (SegmentController) sendable;
            }
        } else return GameServer.getServerState().getSegmentControllersByName().get(name);
        return null;
    }

    public Vector3i getSector() {
        update();
        return sector;
    }

    public float getStatus() {
        update();
        return status;
    }

    private void update() {
        SegmentController entity = getEntity();
        if(entity != null) {
            sector = entity.getSector(new Vector3i());
            status = (float) entity.getHpController().getHpPercent();
        }
    }
}
