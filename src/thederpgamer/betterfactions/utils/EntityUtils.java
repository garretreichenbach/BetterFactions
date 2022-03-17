package thederpgamer.betterfactions.utils;

import api.common.GameCommon;
import api.common.GameServer;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.controller.EntityNotFountException;
import org.schema.game.server.data.blueprintnw.BlueprintClassification;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.schine.network.objects.Sendable;

import java.util.ArrayList;

/**
 * EntityUtils
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/01/2021
 */
public class EntityUtils {

    public static BlueprintClassification getEntityClassification(ManagedUsableSegmentController<?> entity) {
        if(entity.isLoadByBlueprint()) {
            try {
                BlueprintEntry entry = BluePrintController.active.getBlueprint(entity.blueprintIdentifier);
                if(entry.getClassification() != null) return entry.getClassification();
            } catch (EntityNotFountException e) {
                e.printStackTrace();
            }
        }
        return BlueprintClassification.NONE;
    }

    public static ArrayList<CatalogPermission> getServerCatalog() {
        return new ArrayList<>(GameCommon.getGameState().getCatalogManager().getCatalog());
    }

    public static SegmentController getEntity(String name) {
        if(GameCommon.isDedicatedServer()) {
            for(Sendable sendable : GameCommon.getGameState().getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
                if(sendable instanceof SegmentController && GameCommon.getGameObject(sendable.getId()) == sendable && ((SegmentController) sendable).getRealName().equals(name)) return (SegmentController) sendable;
            }
        } else {
            for(SegmentController segmentController : GameServer.getServerState().getSegmentControllersByName().values()) {
                if(segmentController.getRealName().equals(name)) return segmentController;
            }
        }
        return null;
    }
}
