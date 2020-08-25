package dovtech.betterfactions;

import api.DebugFile;
import api.common.GameClient;
import api.common.GameServer;
import api.config.BlockConfig;
import api.element.block.Blocks;
import api.element.inventory.ItemStack;
import api.entity.StarPlayer;
import api.listener.Listener;
import api.listener.events.gui.MainWindowTabAddEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import dovtech.betterfactions.contracts.Contract;
import dovtech.betterfactions.contracts.target.*;
import dovtech.betterfactions.faction.BetterFaction;
import dovtech.betterfactions.faction.BetterPlayer;
import dovtech.betterfactions.faction.FactionStats;
import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import dovtech.betterfactions.faction.diplo.alliance.coalition.Coalition;
import dovtech.betterfactions.faction.diplo.relations.FactionRelations;
import dovtech.betterfactions.faction.government.AllianceGovernmentType;
import dovtech.betterfactions.faction.war.FactionWar;
import dovtech.betterfactions.faction.war.WarGoal;
import dovtech.betterfactions.faction.war.WarParticipant;
import dovtech.betterfactions.gui.contracts.ContractsScrollableList;
import dovtech.betterfactions.gui.faction.alliance.*;
import org.newdawn.slick.Image;
import org.schema.game.client.controller.PlayerGameTextInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUITextInputPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.TextInput;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.texture.Texture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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
    private HashMap<BetterFaction, FactionStats> factionStats;
    private HashMap<BetterFaction, Coalition> coalitions;
    private HashMap<BetterFaction, ArrayList<FactionRelations>> relations;
    private HashMap<String, Alliance> factionAlliances;
    private HashMap<BetterFaction, Image> factionLogos;
    private HashMap<Alliance, Image> allianceLogos;
    private ArrayList<Contract> contracts;
    private ArrayList<FactionWar> wars;
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
        setModVersion("0.1.20");
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

        wars = new ArrayList<>();
        factionLogos = new HashMap<>();
        allianceLogos = new HashMap<>();

        //Todo: Only load moddata if in singleplayer or mod is on server

        initConfig();
        /*
        if(GameClient.getClientState() != null) {
            try {
                loadFactionStats();
                loadFactionAlliances();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
         */
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
        this.contracts = new ArrayList<>();
    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {

    }

    private void registerListeners() {


        StarLoader.registerListener(MainWindowTabAddEvent.class, new Listener<MainWindowTabAddEvent>() {
            @Override
            public void onEvent(MainWindowTabAddEvent event) {
                if(event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_SHOP_SHOPNEW_SHOPPANELNEW_2)) {
                    testFunction();
                    GUIContentPane contractsTab = event.createTab("CONTRACTS");
                    contractsTab.setName("CONTRACTS");
                    contractsTab.setTextBoxHeightLast(300);

                    contractsTab.addDivider(300);
                    /* Faction/Contractor Logo
                    contractsTab.addNewTextBox(0, 150);
                    Sprite contractorLogo = new Sprite(new Texture(0, 0, resourcesPath + "/gui/logo/trading-guild-logo.png")); //Default Contractor
                    GUIOverlay logoOverlay = new GUIOverlay();
                    contractsTab.getContent(0, 0).attach(logoOverlay);
                     */

                    GUITextOverlay contractorDescOverlay = new GUITextOverlay(250, 300, contractsTab.getState());
                    contractorDescOverlay.onInit();
                    contractorDescOverlay.setTextSimple("Placeholder Text");
                    contractsTab.getContent(0, 0).attach(contractorDescOverlay);

                    contractsTab.addNewTextBox(0, 85);

                    ContractsScrollableList contractsScrollableList = new ContractsScrollableList(contractsTab.getState(), 500, 300, contractsTab.getContent(1, 0));
                    contractsScrollableList.onInit();
                    contractsTab.getContent(1, 0).attach(contractsScrollableList);



                } else if(event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_NEWFACTION_FACTIONPANELNEW_9)) {
                    final GUIContentPane allianceTab = event.createTab("ALLIANCE");
                    allianceTab.setName("ALLIANCE");
                    allianceTab.setTextBoxHeightLast(300);

                    allianceTab.addDivider(700);

                    allianceTab.addNewTextBox(0, 100);

                    AllianceInfoBox allianceInfoBox = new AllianceInfoBox(allianceTab.getState(), 130, 100);
                    allianceInfoBox.onInit();
                    allianceTab.getContent(0, 0).attach(allianceInfoBox);

                    allianceTab.addNewTextBox(1, 60);

                    if(getFactionAlliance(GameClient.getClientState().getFaction()) != null) {
                        AllianceFleetsList allianceFleetsList = new AllianceFleetsList(allianceTab.getState(), allianceTab.getContent(0, 1));
                        allianceFleetsList.onInit();
                        allianceTab.getContent(0, 1).attach(allianceFleetsList);

                        AllianceNewsBox allianceNewsBox = new AllianceNewsBox(allianceTab.getState(), allianceTab.getContent(1, 1));
                        allianceTab.getContent(1, 1).attach(allianceNewsBox);

                    } else {
                        if(GameClient.getClientState().getFaction() != null) {
                            GUITextButton createAllianceButton = new GUITextButton(allianceTab.getState(), 80, 30, "Create Alliance", new GUICallback() {
                                @Override
                                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                    if(mouseEvent.pressedLeftMouse()) {
                                        CreateAlliancePanel createAlliancePanel = new CreateAlliancePanel(allianceTab.getState());
                                        createAlliancePanel.draw();
                                    }
                                }

                                @Override
                                public boolean isOccluded() {
                                    return false;
                                }
                            });
                            createAllianceButton.onInit();
                            allianceTab.getContent(0, 1).attach(createAllianceButton);
                        } else {
                            GUITextOverlay noAllianceOverlay = new GUITextOverlay(70, 70, allianceTab.getState());
                            noAllianceOverlay.setTextSimple("No Alliance");
                            allianceTab.getContent(0, 1).attach(noAllianceOverlay);
                        }

                        GUITextOverlay noNewsOverlay = new GUITextOverlay(70, 70, allianceTab.getState());
                        noNewsOverlay.setTextSimple("No News");
                        allianceTab.getContent(1, 1).attach(noNewsOverlay);
                    }


                    AlliancesScrollableList alliancesScrollableList = new AlliancesScrollableList(allianceTab.getState(), 150, 100, allianceTab.getContent(1, 0));
                    alliancesScrollableList.onInit();
                    allianceTab.getContent(1, 0).attach(alliancesScrollableList);

                    GUIContentPane warsTab = event.createTab("WARS");
                    warsTab.setName("WARS");
                    warsTab.setTextBoxHeightLast(300);
                    allianceInfoBox.draw();

                } else if(event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_NEWFACTION_FACTIONPANELNEW_2)) {
                    event.getPane().setName("FACTION DIPLOMACY");
                } else if(event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_NEWFACTION_FACTIONPANELNEW_9)) {
                    event.getPane().setName("FACTION LIST");
                }
            }
        });


        /*
        StarLoader.registerListener(ControlManagerActivateEvent.class, new Listener<ControlManagerActivateEvent>() {
            @Override
            public void onEvent(ControlManagerActivateEvent event) {
                if(event.isActive()) {
                    PlayerPanel playerPanel = GameClientState.instance.getWorldDrawer().getGuiDrawer().getPlayerPanel();
                    if(playerPanel != null && playerPanel.isActive()) {
                        try {
                            Field playerPanelField = PlayerPanel.class.getDeclaredField("factionPanelNew");
                            playerPanelField.setAccessible(true);
                            FactionPanelNew factionPanelNew = (FactionPanelNew) playerPanelField.get(playerPanel);

                            if (GameClientState.instance.getFaction() != null) {
                                if (!(factionPanelNew instanceof NewFactionPanel)) {
                                    GameClientState state = playerPanel.getState();
                                    playerPanelField.set(playerPanel, new NewFactionPanel(state));
                                    if (debugMode) DebugFile.log("[DEBUG]: Swapped out Faction Panel", getMod());
                                }
                            } else {
                                if (!(factionPanelNew instanceof NoFactionPanel)) {
                                    GameClientState state = playerPanel.getState();
                                    playerPanelField.set(playerPanel, new NoFactionPanel(state));
                                }
                            }
                        } catch (NoSuchFieldException | IllegalAccessException ex) {
                            ex.printStackTrace();
                        }
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
         */

        /*
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
         */

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
                Faction internalFaction = GameServer.getServerState().getFactionManager().getFaction(Integer.parseInt(factionID));
                BetterFaction faction = new BetterFaction(internalFaction);
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

    public Map<BetterFaction, FactionStats> getAllFactionStats() {
        return factionStats;
    }

    public FactionStats getFactionStats(BetterFaction faction) throws IOException {
        File factionFile = new File(factionStatsFolder.getPath() + "/" + faction.getInternalFaction().getIdFaction() + ".smdat");
        if(!factionFile.exists()) {
            factionFile.createNewFile();
            int[] stats = new int[] {
                    faction.getInternalFaction().getIdFaction(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, faction.getAllies().size() - 1, faction.getEnemies().size(), 0, 0, 0
            };
            FactionStats fStats = new FactionStats(stats);
            factionStats.put(faction, fStats);
            return fStats;
        }

        return factionStats.get(faction);
    }

    /*
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
     */

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

    public ArrayList<Contract> getContracts() {
        return contracts;
    }

    public HashMap<BetterFaction, Image> getFactionLogos() {
        return factionLogos;
    }

    public HashMap<Alliance, Image> getAllianceLogos() {
        return allianceLogos;
    }

    public void testFunction() {
        //Testing Purposes only

        Random random = new Random();
        for(int i = 0; i < 10; i ++) {
            Contract.ContractType randomContractType = Contract.ContractType.BOUNTY;
            int randomTypeInt = random.nextInt(5 - 1) + 1;
            switch(randomTypeInt) {
                case 1:
                    randomContractType = Contract.ContractType.BOUNTY;
                    break;
                case 2:
                    randomContractType = Contract.ContractType.MINING;
                    break;
                case 3:
                    randomContractType = Contract.ContractType.PRODUCTION;
                    break;
                case 4:
                    randomContractType = Contract.ContractType.CARGO_ESCORT;
                    break;
            }
            int randomReward = (random.nextInt(1000 - 100) + 100) * 1000;

            ContractTarget contractTarget = new PlayerTarget();
            contractTarget.setTarget(new BetterPlayer(new StarPlayer(GameClient.getClientPlayerState())));
            String contractName = "";
            switch(randomContractType) {
                case MINING:
                    contractTarget = new MiningTarget();
                    ItemStack miningTargetStack = new ItemStack(Blocks.THRENS_ORE_RAW.getId());
                    int randomAmount = (random.nextInt(300 - 1) + 1) * 10;
                    miningTargetStack.setAmount(randomAmount);
                    contractTarget.setTarget(miningTargetStack);
                    contractName = "Mine x" + miningTargetStack.getAmount() + " " + miningTargetStack.getName();
                    break;
                case BOUNTY:
                    contractTarget = new PlayerTarget();
                    contractTarget.setTarget(new BetterPlayer(new StarPlayer(GameClient.getClientPlayerState())));
                    contractName = "Kill " + GameClient.getClientPlayerState().getName();
                    break;
                case PRODUCTION:
                    contractTarget = new ProductionTarget();
                    ItemStack productionTargetStatck = new ItemStack(Blocks.REACTOR_POWER.getId());
                    int randomProductionAmount = (random.nextInt(300 - 1) + 1) * 10;
                    productionTargetStatck.setAmount(randomProductionAmount);
                    contractTarget.setTarget(productionTargetStatck);
                    contractName = "Produce x" + randomProductionAmount + " " + productionTargetStatck.getName();
                    break;
                case CARGO_ESCORT:
                    contractTarget = new CargoTarget();
                    ItemStack cargoTargetStack = new ItemStack(Blocks.REACTOR_POWER.getId());
                    int randomCargoAmount = (random.nextInt(300 - 1) + 1) * 10;
                    cargoTargetStack.setAmount(randomCargoAmount);
                    contractTarget.setTarget(cargoTargetStack);
                    contractName = "Deliver cargo to (15.15.15)";
                    break;
            }



            Contract randomContract = new Contract(new BetterFaction(GameServer.getServerState().getFactionManager().getFaction(-2)), contractName, randomContractType, randomReward, contractTarget);
            contracts.add(randomContract);
        }

        /*
        Alliance alliance = new Alliance("Dual Monarchy", AllianceGovernmentType.CONFEDERATION);
        alliance.getMembers().add(new BetterFaction(GameClient.getClientState().getFaction()));
        alliance.setAllianceID("0");
        this.getFactionAlliances().put(alliance.getAllianceID(), alliance);

        WarParticipant attacker = new WarParticipant(GameClientState.instance.getFaction(), new WarGoal(GameClientState.instance.getFaction(), GameClientState.instance.getFactionManager().getFaction(-2), WarGoal.WarGoalType.DEFEAT));
        WarParticipant defender = new WarParticipant(GameClientState.instance.getFactionManager().getFaction(-2), new WarGoal(GameClientState.instance.getFactionManager().getFaction(-2), GameClientState.instance.getFaction(), WarGoal.WarGoalType.NONE));

        FactionWar holyWar = new FactionWar(attacker, defender);
        wars.add(holyWar);
         */
    }
}
