package thederpgamer.betterfactions;

import api.common.GameServer;
import api.listener.Listener;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.listener.events.player.PlayerDeathEvent;
import api.listener.events.player.PlayerJoinFactionEvent;
import api.listener.events.player.PlayerLeaveFactionEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.resource.ResourceLoader;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.data.serializeable.FactionEntityData;
import thederpgamer.betterfactions.data.persistent.faction.FactionRank;
import thederpgamer.betterfactions.gui.NewFactionPanel;
import thederpgamer.betterfactions.manager.*;
import thederpgamer.betterfactions.network.client.CreateNewFederationPacket;
import thederpgamer.betterfactions.network.client.ModifyFactionMessagePacket;
import thederpgamer.betterfactions.network.client.SendFactionMessagePacket;
import thederpgamer.betterfactions.network.server.ServerSyncDataPacket;
import thederpgamer.betterfactions.network.server.UpdateClientCachePacket;
import thederpgamer.betterfactions.network.server.UpdateGUIsPacket;
import thederpgamer.betterfactions.utils.FactionNewsUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * BetterFactions mod main class.
 *
 * @author TheDerpGamer
 * @version 1.0 - [01/30/2021]
 */
public class BetterFactions extends StarMod {

    //Instance
    public BetterFactions() {

    }
    public static void main(String[] args) {

    }
    private static BetterFactions inst;
    public static BetterFactions getInstance() {
        return inst;
    }

    //Data
    public NewFactionPanel newFactionPanel;

    @Override
    public void onEnable() {
        inst = this;
        ConfigManager.initialize(this);
        registerListeners();
        registerPackets();
        startRunners();
    }

    @Override
    public void onResourceLoad(ResourceLoader resourceLoader) {
        ResourceManager.loadResources(resourceLoader);
    }

    private void registerListeners() {
        StarLoader.registerListener(PlayerGUICreateEvent.class, new Listener<PlayerGUICreateEvent>() {
            @Override
            public void onEvent(PlayerGUICreateEvent event) {
                FactionManager.initializeFactions();
                try {
                    PlayerPanel playerPanel = event.getPlayerPanel();
                    Field factionPanelNewField = playerPanel.getClass().getDeclaredField("factionPanelNew");
                    factionPanelNewField.setAccessible(true);
                    if(!(factionPanelNewField.get(playerPanel) instanceof NewFactionPanel)) {
                        (newFactionPanel = new NewFactionPanel(playerPanel.getState())).onInit();
                        factionPanelNewField.set(playerPanel, newFactionPanel);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }, this);

        StarLoader.registerListener(FactionCreateEvent.class, new Listener<FactionCreateEvent>() {
            @Override
            public void onEvent(FactionCreateEvent event) {
                FactionManager.getFactionData(event.getFaction());
                FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionCreateNews(FactionManager.getFactionData(event.getFaction()), event.getPlayer()));
                FactionNewsUtils.saveData();
                FactionData factionData = FactionManager.getFactionData(event.getFaction());
                FactionRank founderRank = new FactionRank("Founder", 4, "*");
                factionData.addRank(founderRank);
                factionData.addMember(event.getPlayer().getName());
                factionData.getMember(event.getPlayer().getName()).setRank(founderRank);
                PersistentObjectUtil.save(getSkeleton());
                updateClientData();
            }
        }, this);

        StarLoader.registerListener(PlayerJoinFactionEvent.class, new Listener<PlayerJoinFactionEvent>() {
            @Override
            public void onEvent(PlayerJoinFactionEvent event) {
                FactionManager.getFactionData(event.getFaction());
                FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionJoinNews(FactionManager.getFactionData(event.getFaction()), event.getPlayer()));
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    FactionManager.getFactionData(event.getFaction()).addMember(event.getPlayer().getName());
                    if(FactionManager.getFactionData(event.getFaction()).getMembers().size() <= 1) {
                        FactionRank founderRank = new FactionRank("Founder", 4, "*");
                        FactionManager.getFactionData(event.getFaction()).addRank(founderRank);
                        FactionManager.getFactionData(event.getFaction()).getMembers().get(0).setRank(founderRank);
                    }
                }
                PersistentObjectUtil.save(getSkeleton());
                updateClientData();
            }
        }, this);

        StarLoader.registerListener(PlayerLeaveFactionEvent.class, new Listener<PlayerLeaveFactionEvent>() {
            @Override
            public void onEvent(PlayerLeaveFactionEvent event) {
                if(event.getFaction() != null) {
                    if(event.getFaction().getMembersUID().keySet().size() <= 1) {
                        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionDisbandNews(FactionManager.getFactionData(event.getFaction())));
                    } else {
                        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionLeaveNews(FactionManager.getFactionData(event.getFaction()), event.getPlayer()));
                    }
                    FactionManager.getFactionData(event.getFaction());
                }
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    FactionManager.getFactionData(event.getFaction()).removeMember(event.getPlayer().getName());
                    PersistentObjectUtil.save(getSkeleton());
                    updateClientData();
                    newFactionPanel.recreateTabs();
                } else {
                    PersistentObjectUtil.save(getSkeleton());
                    updateClientData();
                }
            }
        }, this);

        StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
            @Override
            public void onEvent(PlayerDeathEvent event) {
                TradeGuildManager.handleAggressionEvent(event);
            }
        }, this);
    }

    private void registerPackets() {
        PacketUtil.registerPacket(UpdateClientCachePacket.class);
        PacketUtil.registerPacket(ServerSyncDataPacket.class);
        PacketUtil.registerPacket(UpdateGUIsPacket.class);
        PacketUtil.registerPacket(CreateNewFederationPacket.class);
        PacketUtil.registerPacket(SendFactionMessagePacket.class);
        PacketUtil.registerPacket(ModifyFactionMessagePacket.class);
    }

    private void startRunners() {
        if(NetworkSyncManager.onServer()) {
            new StarRunnable() {
                @Override
                public void run() {
                    try {
                        updateClientData();
                    } catch(Exception exception) {
                        LogManager.logException("Encountered an exception while trying to sync client data!", exception);
                    }
                }
            }.runTimer(this, 500);
        }
    }

    public void updateClientData() {
        for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
            ArrayList<FactionEntityData> assetList = new ArrayList<>(); //Todo: This is probably really slow
            for(SegmentController segmentController : GameServer.getServerState().getSegmentControllersByName().values()) {
                if(segmentController.getFactionId() == playerState.getFactionId() && playerState.getFactionId() > 0) assetList.add(new FactionEntityData(segmentController));
            }
            PacketUtil.sendPacket(playerState, new UpdateClientCachePacket(assetList));
        }

        //Todo: Rework below to match new networking system
        if(NetworkSyncManager.onServer()) NetworkSyncManager.sendServerModifications();
        else newFactionPanel.updateTabs();
    }
}
