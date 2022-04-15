package thederpgamer.betterfactions;

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
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.schine.resource.ResourceLoader;
import thederpgamer.betterfactions.data.old.faction.FactionDataOld;
import thederpgamer.betterfactions.data.old.faction.FactionRank;
import thederpgamer.betterfactions.gui.NewFactionPanel;
import thederpgamer.betterfactions.manager.ConfigManager;
import thederpgamer.betterfactions.manager.FactionManagerOld;
import thederpgamer.betterfactions.manager.ResourceManager;
import thederpgamer.betterfactions.manager.TradeGuildManager;
import thederpgamer.betterfactions.network.client.CreateNewFederationPacket;
import thederpgamer.betterfactions.network.client.ModifyFactionMessagePacket;
import thederpgamer.betterfactions.network.client.RequestDataPacket;
import thederpgamer.betterfactions.network.client.SendFactionMessagePacket;
import thederpgamer.betterfactions.network.server.SendDataPacket;
import thederpgamer.betterfactions.network.server.UpdateGUIsPacket;
import thederpgamer.betterfactions.utils.FactionNewsUtils;

import java.lang.reflect.Field;

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
    }

    @Override
    public void onResourceLoad(ResourceLoader resourceLoader) {
        ResourceManager.loadResources(resourceLoader);
    }

    private void registerListeners() {
        StarLoader.registerListener(PlayerGUICreateEvent.class, new Listener<PlayerGUICreateEvent>() {
            @Override
            public void onEvent(PlayerGUICreateEvent event) {
                FactionManagerOld.initializeFactions();
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
                FactionManagerOld.getFactionData(event.getFaction());
                FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionCreateNews(FactionManagerOld.getFactionData(event.getFaction()), event.getPlayer()));
                FactionNewsUtils.saveData();
                FactionDataOld factionData = FactionManagerOld.getFactionData(event.getFaction());
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
                FactionManagerOld.getFactionData(event.getFaction());
                FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionJoinNews(FactionManagerOld.getFactionData(event.getFaction()), event.getPlayer()));
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    FactionManagerOld.getFactionData(event.getFaction()).addMember(event.getPlayer().getName());
                    if(FactionManagerOld.getFactionData(event.getFaction()).getMembers().size() <= 1) {
                        FactionRank founderRank = new FactionRank("Founder", 4, "*");
                        FactionManagerOld.getFactionData(event.getFaction()).addRank(founderRank);
                        FactionManagerOld.getFactionData(event.getFaction()).getMembers().get(0).setRank(founderRank);
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
                        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionDisbandNews(FactionManagerOld.getFactionData(event.getFaction())));
                    } else {
                        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionLeaveNews(FactionManagerOld.getFactionData(event.getFaction()), event.getPlayer()));
                    }
                    FactionManagerOld.getFactionData(event.getFaction());
                }
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    FactionManagerOld.getFactionData(event.getFaction()).removeMember(event.getPlayer().getName());
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
        PacketUtil.registerPacket(RequestDataPacket.class);
        PacketUtil.registerPacket(SendDataPacket.class);
        PacketUtil.registerPacket(UpdateGUIsPacket.class);
        PacketUtil.registerPacket(CreateNewFederationPacket.class);
        PacketUtil.registerPacket(SendFactionMessagePacket.class);
        PacketUtil.registerPacket(ModifyFactionMessagePacket.class);
    }

    private void updateClientData() {

    }
}
