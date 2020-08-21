package dovtech.betterfactions;

import api.DebugFile;
import api.common.GameClient;
import api.config.BlockConfig;
import api.listener.Listener;
import api.listener.events.SegmentControllerOverheatEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.gui.ControlManagerActivateEvent;
import api.listener.events.player.PlayerDeathEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import dovtech.betterfactions.controller.NewFactionControlManager;
import dovtech.betterfactions.faction.FactionStats;
import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import dovtech.betterfactions.faction.diplo.alliance.coalition.Coalition;
import dovtech.betterfactions.faction.diplo.relations.FactionRelations;
import dovtech.betterfactions.gui.faction.NewFactionPanel;
import org.newdawn.slick.Image;
import org.schema.game.client.controller.manager.ingame.PlayerGameControlManager;
import org.schema.game.client.controller.manager.ingame.faction.FactionControlManager;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class BetterFactions extends StarMod {

    static BetterFactions inst;

    public BetterFactions() {
        inst = this;
    }

    //Resources
    private String resourcesPath;

    //Server
    private File moddataFolder;
    private File StarMadePlusDataFolder;
    private File factionStatsFolder;
    private File factionAlliancesFolder;
    private File imageFolder;
    private HashMap<Faction, FactionStats> factionStats;
    private HashMap<Faction, Coalition> coalitions;
    private HashMap<Faction, ArrayList<FactionRelations>> relations;
    private HashMap<String, Alliance> factionAlliances;
    private HashMap<Faction, Image> factionLogos;
    private HashMap<Alliance, Image> allianceLogos;
    public Image defaultLogo;

    //Config
    private FileConfiguration config;
    private String[] defaultConfig = {
            "debug-mode: false",
            "guardian-faction-enable: true",
            "guardian-faction-id: -1"
    };

    //Config Settings
    private boolean debugMode;
    private boolean guardianFactionEnabled;
    private int guardianFactionID;

    public static void main(String[] args) {

    }

    @Override
    public void onGameStart() {
        inst = this;
        setModName("BetterFactions");
        setModAuthor("Dovtech");
        setModVersion("0.1.15");
        setModDescription("Improves faction interaction and diplomacy.");

        resourcesPath = this.getClass().getResource("").getPath();

        moddataFolder = new File("moddata");
        if(!moddataFolder.exists()) moddataFolder.mkdir();
        StarMadePlusDataFolder = new File("moddata/BetterFactions");
        if(!StarMadePlusDataFolder.exists()) StarMadePlusDataFolder.mkdir();

        factionStatsFolder = new File("moddata/BetterFactions/factions");
        factionAlliancesFolder = new File("moddata/BetterFactions/alliances");
        if(!(factionStatsFolder.exists())) factionStatsFolder.mkdir();
        if(!(factionAlliancesFolder.exists())) factionAlliancesFolder.mkdir();

        imageFolder = new File("moddata/BetterFactions/images");
        if(!(imageFolder.exists())) imageFolder.mkdir();

        factionLogos = new HashMap<>();
        allianceLogos = new HashMap<>();

        //Todo: Only load moddata if in singleplayer or mod is on server

        initConfig();
        if(GameClient.getClientState() != null) {
            try {
                loadFactionStats();
                loadFactionAlliances();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        registerListeners();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        DebugFile.log("Enabled", this);

        this.factionStats = new HashMap<>();
        this.coalitions = new HashMap<>();
        this.relations = new HashMap<>();
        this.factionAlliances = new HashMap<>();
    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {

    }

    private void registerListeners() {

        StarLoader.registerListener(ControlManagerActivateEvent.class, new Listener<ControlManagerActivateEvent>() {
            @Override
            public void onEvent(ControlManagerActivateEvent event) {

                if(event.isActive()) {
                    //NewFactionPanel
                    PlayerPanel playerPanel = GameClientState.instance.getWorldDrawer().getGuiDrawer().getPlayerPanel();
                    try {
                        Field playerPanelField = PlayerPanel.class.getDeclaredField("factionPanelNew");
                        playerPanelField.setAccessible(true);
                        FactionPanelNew factionPanelNew = (FactionPanelNew) playerPanelField.get(playerPanel);
                        if(!(factionPanelNew instanceof NewFactionPanel)) {
                            GameClientState state = playerPanel.getState();
                            playerPanelField.set(playerPanel, new NewFactionPanel(state));
                            if(debugMode) DebugFile.log("[DEBUG]: Swapped out FactionPanelNew", getMod());
                        }
                    } catch (NoSuchFieldException | IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }


                if(event.isActive() && event.getControlManager() instanceof PlayerGameControlManager) {
                    try {
                        PlayerGameControlManager playerGameControlManager = (PlayerGameControlManager) event.getControlManager();
                        if(debugMode) DebugFile.log("[DEBUG]: PlayerGameControlManager activated", getMod());

                        //NewFactionControlManager
                        Field factionControlManagerField = PlayerGameControlManager.class.getDeclaredField("factionControlManager");
                        factionControlManagerField.setAccessible(true);
                        FactionControlManager factionControlManager = (FactionControlManager) factionControlManagerField.get(playerGameControlManager);
                        if(!(factionControlManager instanceof NewFactionControlManager)) {
                            //factionControlManagerField.set(playerGameControlManager, new NewFactionControlManager(GameClientState.instance));
                            if(debugMode)DebugFile.log("[DEBUG]: Swapped out FactionControlManager", getMod());
                        }
                    } catch(NoSuchFieldException | IllegalAccessException | NullPointerException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        StarLoader.registerListener(FactionRelationChangeEvent.class, new Listener<FactionRelationChangeEvent>() {
            @Override
            public void onEvent(FactionRelationChangeEvent event) {
                try {
                    Faction from = event.getFrom();
                    Faction to = event.getTo();
                    FactionStats fromStats = getFactionStats(from);
                    FactionStats toStats = getFactionStats(to);
                    fromStats.setEnemyCount(from.getEnemies().size());
                    toStats.setEnemyCount(to.getEnemies().size());
                    fromStats.setAllyCount(from.getFriends().size() - 1);
                    toStats.setAllyCount(to.getFriends().size() - 1);
                    fromStats.setCurrentTerritoryCount(from.clientLastTurnSytemsCount); //I'm not sure if this is the actual count of controlled systems
                    toStats.setCurrentTerritoryCount(to.clientLastTurnSytemsCount); //I'm not sure if this is the actual count of controlled systems

                    if(event.getNewRelation().equals(FactionRelation.RType.ENEMY)) {
                        fromStats.setAttackingWars(fromStats.getAttackingWars() + 1);
                        fromStats.setAggressionScore(calcAggressionScore(fromStats));
                        if(fromStats.getAggressionScore() >= 30) {
                            if(calcCoalitionChance(fromStats) && guardianFactionEnabled) {

                                //Coalition coalition = new Coalition(from, GameServerState.instance.getFactionManager().getFaction(guardianFactionID));


                                //fromStats.setCoalitionsAgainst(fromStats.getCoalitionsAgainst() + 1);
                                //toStats.setCoalitionsParticipated(toStats.getCoalitionsParticipated() + 1);
                                //coalitions.put(from, coalition);
                            }
                        }
                        toStats.setDefendingWars(toStats.getDefendingWars() + 1);
                    }

                    factionStats.remove(from);
                    factionStats.remove(to);
                    factionStats.put(from, fromStats);
                    factionStats.put(to, toStats);
                    saveFactionStats(fromStats);
                    saveFactionStats(toStats);
                } catch(IOException exception) {
                    exception.printStackTrace();
                }
            }
        });

        StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
            @Override
            public void onEvent(PlayerDeathEvent event) {
                Faction from = GameServerState.instance.getFactionManager().getFaction(event.getDamager().getFactionId());
                Faction to = GameServerState.instance.getFactionManager().getFaction(event.getPlayer().getFactionId());
                try {
                    FactionStats fromStats = getFactionStats(from);
                    FactionStats toStats = getFactionStats(to);
                    fromStats.setEnemyCount(from.getEnemies().size());
                    toStats.setEnemyCount(to.getEnemies().size());
                    fromStats.setAllyCount(from.getFriends().size() - 1);
                    toStats.setAllyCount(to.getFriends().size() - 1);
                    fromStats.setCurrentTerritoryCount(from.clientLastTurnSytemsCount); //I'm not sure if this is the actual count of controlled systems
                    toStats.setCurrentTerritoryCount(to.clientLastTurnSytemsCount); //I'm not sure if this is the actual count of controlled systems

                    fromStats.setPlayersKilled(fromStats.getPlayersKilled() + 1);
                    toStats.setPlayersLost(toStats.getPlayersLost() + 1);

                    factionStats.remove(from);
                    factionStats.remove(to);
                    factionStats.put(from, fromStats);
                    factionStats.put(to, toStats);
                    saveFactionStats(fromStats);
                    saveFactionStats(toStats);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        StarLoader.registerListener(SegmentControllerOverheatEvent.class, new Listener<SegmentControllerOverheatEvent>() {
            @Override
            public void onEvent(SegmentControllerOverheatEvent event) {
                Faction from = GameServerState.instance.getFactionManager().getFaction(event.getLastDamager().getFactionId());
                Faction to = GameServerState.instance.getFactionManager().getFaction(event.getEntity().getFactionId());
                try {
                    FactionStats fromStats = getFactionStats(from);
                    FactionStats toStats = getFactionStats(to);
                    fromStats.setEnemyCount(from.getEnemies().size());
                    toStats.setEnemyCount(to.getEnemies().size());
                    fromStats.setAllyCount(from.getFriends().size() - 1);
                    toStats.setAllyCount(to.getFriends().size() - 1);
                    fromStats.setCurrentTerritoryCount(from.clientLastTurnSytemsCount); //I'm not sure if this is the actual count of controlled systems
                    toStats.setCurrentTerritoryCount(to.clientLastTurnSytemsCount); //I'm not sure if this is the actual count of controlled systems

                    if(event.getEntity().getType().equals(SimpleTransformableSendableObject.EntityType.SPACE_STATION)) {
                        fromStats.setStationsDestroyed(fromStats.getStationsDestroyed() + 1);
                        toStats.setStationsLost(toStats.getStationsLost() + 1);
                    } else if(event.getEntity().getType().equals(SimpleTransformableSendableObject.EntityType.SHIP)) {
                        fromStats.setShipsDestroyed(fromStats.getShipsDestroyed() + 1);
                        toStats.setShipsLost(toStats.getShipsLost() + 1);
                    }

                    factionStats.remove(from);
                    factionStats.remove(to);
                    factionStats.put(from, fromStats);
                    factionStats.put(to, toStats);
                    saveFactionStats(fromStats);
                    saveFactionStats(toStats);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        DebugFile.log("Registered Listeners!", this);
    }

    private void initConfig() {
        this.config = getConfig("config");
        this.config.saveDefault(defaultConfig);

        this.debugMode = Boolean.parseBoolean(this.config.getString("debug-mode"));
        this.guardianFactionEnabled = Boolean.parseBoolean(this.config.getString("guardian-faction-enable"));
        this.guardianFactionID = this.config.getInt("guardian-faction-id");
    }

    private void loadFactionStats() throws FileNotFoundException {
        if(Objects.requireNonNull(factionStatsFolder.listFiles()).length > 0) {
            for(File factionFile : Objects.requireNonNull(factionStatsFolder.listFiles())) {
                String factionID = factionFile.getName().substring(0, factionFile.getName().indexOf(".smdat") - 1);
                Faction faction = GameServerState.instance.getFactionManager().getFaction(Integer.parseInt(factionID));
                Scanner scan = new Scanner(factionFile);
                int[] stats = new int[23];
                for(int i = 0; i < stats.length; i ++) {
                    stats[i] = Integer.parseInt(scan.nextLine());
                }
                FactionStats fStats = new FactionStats(stats);
                this.factionStats.put(faction, fStats);
            }
        } else {
            DebugFile.log("Faction Stats folder is empty, ignoring for now...", this);
        }
    }

    private void loadFactionAlliances() throws FileNotFoundException {
        if(Objects.requireNonNull(factionAlliancesFolder.listFiles()).length > 0) {
            for(File allianceFile : Objects.requireNonNull(factionAlliancesFolder.listFiles())) {
                /*
                String allianceID = allianceFile.getName().substring(0, allianceFile.getName().indexOf(".smdat") - 1);
                Scanner scan = new Scanner(allianceFile);
                String allianceName = scan.nextLine();
                AllianceGovernmentType governmentType = AllianceGovernmentType.valueOf(scan.nextLine());
                int logoUploaderID = scan.nextInt();
                FactionLogo logo = new FactionLogo(logoUploaderID, allianceID, scan.nextLine());
                ArrayList<Faction> members = new ArrayList<>();
                while(scan.hasNextInt()) {
                    members.add(GameServerState.instance.getFactionManager().getFaction(scan.nextInt()));
                }
                Alliance alliance = new Alliance(allianceName, governmentType);
                alliance.setMembers(members);
                alliance.setLogo(logo);
                alliance.setAllianceID(allianceID);
                this.factionAlliances.put(allianceID, alliance);
                 */
            }
        }
    }

    public static BetterFactions getInstance() {
        return inst;
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public Map<Faction, FactionStats> getAllFactionStats() {
        return factionStats;
    }

    public FactionStats getFactionStats(Faction faction) throws IOException {
        File factionFile = new File(factionStatsFolder.getPath() + "/" + faction.getIdFaction() + ".smdat");
        if(!factionFile.exists()) {
            factionFile.createNewFile();
            int[] stats = new int[] {
                    faction.getIdFaction(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, faction.getFriends().size() - 1, faction.getEnemies().size(), 0, 0, 0
            };
            FactionStats fStats = new FactionStats(stats);
            factionStats.put(faction, fStats);
            return fStats;
        }

        return factionStats.get(faction);
    }

    public void saveFactionStats(FactionStats fStats) throws IOException {
        int factionID = fStats.getFactionID();
        File factionFile = new File(factionStatsFolder.getPath() + "/" + factionID + ".smdat");
        if(factionFile.exists()) {
            factionFile.delete();
        }
        FileWriter output = new FileWriter(factionFile.getPath());
        for(int i = 0; i < fStats.getStats().length; i ++) {
            output.write(fStats.getStats()[i] + "\n");
        }
        output.close();
    }

    public ArrayList<FactionRelations> getRelations(Faction faction) {
        return relations.get(faction);
    }

    public FactionRelations getRelationsTo(Faction from, Faction to) {
        ArrayList<FactionRelations> relList = this.relations.get(from);
        for(FactionRelations rel : relList) {
            if(rel.getTo().equals(to)) {
                return rel;
            }
        }

        return new FactionRelations(from, to);
    }

    public String getResourcesPath() {
        return resourcesPath;
    }

    public HashMap<String, Alliance> getFactionAlliances() {
        return factionAlliances;
    }

    public Alliance getFactionAlliance(Faction faction) {
        for(Alliance alliance : this.factionAlliances.values()) {
            if(alliance.getMembers().contains(faction)) return alliance;
        }

        return null;
    }

    private int calcAggressionScore(FactionStats fStats) { //Todo:Redo this so that you loose aggression score over time
        int newAggressionScore = fStats.getAggressionScore();
        if(fStats.getAttackingWars() <= 5) {
            newAggressionScore += 5;
        } else if(fStats.getAttackingWars() <= 10) {
            newAggressionScore += 12;
        } else if(fStats.getAttackingWars() <= 15) {
            newAggressionScore += 20;
        } else if(fStats.getAttackingWars() > 15) {
            newAggressionScore += 30;
        }
        return newAggressionScore;
    }

    private boolean calcCoalitionChance(FactionStats fStats) {
        return false;
    }

    public HashMap<Faction, Image> getFactionLogos() {
        return factionLogos;
    }

    public HashMap<Alliance, Image> getAllianceLogos() {
        return allianceLogos;
    }
}
