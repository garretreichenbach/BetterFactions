package thederpgamer.betterfactions.utils;

import api.common.GameCommon;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.controller.EntityNotFountException;
import org.schema.game.server.data.blueprintnw.BlueprintClassification;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
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
}
