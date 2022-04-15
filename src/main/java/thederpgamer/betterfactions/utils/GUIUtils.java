package thederpgamer.betterfactions.utils;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerThrustManagerInput;
import org.schema.game.client.controller.manager.AbstractControlManager;
import org.schema.game.client.controller.manager.AiConfigurationManager;
import org.schema.game.client.controller.manager.ingame.InventoryControllerManager;
import org.schema.game.client.controller.manager.ingame.PlayerGameControlManager;
import org.schema.game.client.controller.manager.ingame.catalog.CatalogControlManager;
import org.schema.game.client.controller.manager.ingame.faction.FactionControlManager;
import org.schema.game.client.controller.manager.ingame.navigation.NavigationControllerManager;
import org.schema.game.client.controller.manager.ingame.ship.FleetControlManager;
import org.schema.game.client.controller.manager.ingame.ship.ThrustManager;
import org.schema.game.client.controller.manager.ingame.ship.WeaponAssignControllerManager;
import org.schema.game.client.controller.manager.ingame.shop.ShopControllerManager;
import org.schema.game.client.controller.manager.ingame.structurecontrol.StructureControllerManager;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.game.client.view.gui.ai.newai.AIPanelNew;
import org.schema.game.client.view.gui.catalog.newcatalog.CatalogPanelNew;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.game.client.view.gui.fleet.FleetPanel;
import org.schema.game.client.view.gui.inventory.inventorynew.InventoryPanelNew;
import org.schema.game.client.view.gui.navigation.navigationnew.NavigationPanelNew;
import org.schema.game.client.view.gui.shop.shopnew.ShopPanelNew;
import org.schema.game.client.view.gui.structurecontrol.structurenew.StructurePanelNew;
import org.schema.game.client.view.gui.thrustmanagement.ThrustManagementPanelNew;
import org.schema.game.client.view.gui.weapon.WeaponPanelNew;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;
import javax.vecmath.Vector2f;
import java.awt.*;
import java.lang.reflect.Field;

/**
 * GUIUtils.java
 * <Description>
 * ==================================================
 * Created 02/03/2021
 * @author TheDerpGamer
 */
public class GUIUtils {

    public static Vector2f[] getCorners(GUIElement guiElement) {
        Vector2f[] corners = new Vector2f[5];
        Vector2f currentPos = new Vector2f(guiElement.getPos().x, guiElement.getPos().y);
        corners[0] = new Vector2f(currentPos.x, currentPos.y);
        corners[1] = new Vector2f(currentPos.x + guiElement.getWidth(), currentPos.y);
        corners[2] = new Vector2f(currentPos.x, currentPos.y + guiElement.getHeight());
        corners[3] = new Vector2f(currentPos.x + guiElement.getWidth(), currentPos.y + guiElement.getHeight());
        corners[4] = new Vector2f((corners[1].x - corners[0].x) / 2, (corners[2].y - corners[0].y) / 2);
        return corners;
    }

    public static int getQuadrant(float x, float y) {
        if(x < 0 && y < 0) {
            return 1;
        } else if(x > 0 && y < 0) {
            return 2;
        } else if(x < 0 && y > 0) {
            return 3;
        } else if(x > 0 && y > 0) {
            return 4;
        } else {
            return 0;
        }
    }

    public static void adaptElementToScreen(GUIElement element) {
        Vector2f screenRes = getScreenRes();
        element.getPos().x *= screenRes.x;
        element.getPos().y *= screenRes.y;
        element.getScale().x *= screenRes.x;
        element.getScale().y *= screenRes.y;
    }

    public static Vector2f getScreenRes() {
        Vector2f screenRes = new Vector2f();
        GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        screenRes.x = graphicsDevice.getDisplayMode().getWidth();
        screenRes.y = graphicsDevice.getDisplayMode().getHeight();
        return screenRes;
    }

    public static GUIMainWindow getCurrentActiveWindow() {
        try {
            if (getPlayerGameControlManager().isAnyMenuActive()) {
                if (getPlayerGameControlManager().getInventoryControlManager().isActive()) {
                    return ((InventoryPanelNew) getControlManagerInterface(getPlayerGameControlManager().getInventoryControlManager())).inventoryPanel;
                } else if (getPlayerGameControlManager().getCatalogControlManager().isActive()) {
                    return ((CatalogPanelNew) getControlManagerInterface(getPlayerGameControlManager().getCatalogControlManager())).catalogPanel;
                } else if (getPlayerGameControlManager().getFleetControlManager().isActive()) {
                    return ((FleetPanel) getControlManagerInterface(getPlayerGameControlManager().getFleetControlManager())).fleetPanel;
                } else if (getPlayerGameControlManager().getFactionControlManager().isActive()) {
                    return ((FactionPanelNew) getControlManagerInterface(getPlayerGameControlManager().getFactionControlManager())).factionPanel;
                } else if (getPlayerGameControlManager().getShopControlManager().isActive()) {
                    return ((ShopPanelNew) getControlManagerInterface(getPlayerGameControlManager().getShopControlManager())).shopPanel;
                } else if (getPlayerGameControlManager().getWeaponControlManager().isActive()) {
                    return ((WeaponPanelNew) getControlManagerInterface(getPlayerGameControlManager().getWeaponControlManager())).weaponPanel;
                } else if (getPlayerGameControlManager().getNavigationControlManager().isActive()) {
                    return ((NavigationPanelNew) getControlManagerInterface(getPlayerGameControlManager().getNavigationControlManager())).navigationPanel;
                } else if (getPlayerGameControlManager().getMapControlManager().isActive()) {
                    return ((AIPanelNew) getControlManagerInterface(getPlayerGameControlManager().getAiConfigurationManager())).aiPanel;
                } else if (getPlayerGameControlManager().getStructureControlManager().isActive()) {
                    return ((StructurePanelNew) getControlManagerInterface(getPlayerGameControlManager().getStructureControlManager())).structurePanel;
                } else if (getPlayerGameControlManager().getThrustManager().isActive()) {
                    PlayerThrustManagerInput thrustManager = (PlayerThrustManagerInput) getControlManagerInterface(getPlayerGameControlManager().getThrustManager());
                    Field field = thrustManager.getClass().getDeclaredField("panel");
                    field.setAccessible(true);
                    return (ThrustManagementPanelNew) field.get(thrustManager);
                }
            }
        } catch (IllegalAccessException | NullPointerException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static GUIActiveInterface getControlManagerInterface(AbstractControlManager controlManager) throws NoSuchFieldException, IllegalAccessException {
        Field menuField = null;
        if (controlManager instanceof CatalogControlManager) {
            menuField = getPlayerPanel().getClass().getDeclaredField("catalogPanelNew");
        } else if (controlManager instanceof InventoryControllerManager) {
            menuField = getPlayerPanel().getClass().getDeclaredField("inventoryPanelNew");
        } else if (controlManager instanceof FleetControlManager) {
            menuField = getPlayerPanel().getClass().getDeclaredField("fleetPanel");
        } else if (controlManager instanceof FactionControlManager) {
            menuField = getPlayerPanel().getClass().getDeclaredField("factionPanelNew");
        } else if (controlManager instanceof ShopControllerManager) {
            menuField = getPlayerPanel().getClass().getDeclaredField("shopPanelNew");
        } else if (controlManager instanceof WeaponAssignControllerManager) {
            menuField = getPlayerPanel().getClass().getDeclaredField("weaponManagerPanelNew");
        } else if (controlManager instanceof NavigationControllerManager) {
            menuField = getPlayerPanel().getClass().getDeclaredField("navigationPanelNew");
        } else if (controlManager instanceof AiConfigurationManager) {
            menuField = getPlayerPanel().getClass().getDeclaredField("aiPanelNew");
        } else if (controlManager instanceof StructureControllerManager) {
            menuField = getPlayerPanel().getClass().getDeclaredField("structurePanelNew");
        } else if (controlManager instanceof ThrustManager) {
            menuField = getPlayerGameControlManager().getThrustManager().getClass().getDeclaredField("gameMenu");
        }

        if (menuField != null) {
            menuField.setAccessible(true);
            return (GUIActiveInterface) menuField.get(getPlayerPanel());
        } else {
            return null;
        }
    }

    public static PlayerGameControlManager getPlayerGameControlManager() {
        return GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager();
    }

    public static PlayerPanel getPlayerPanel() {
        return GameClient.getClientState().getWorldDrawer().getGuiDrawer().getPlayerPanel();
    }
}
