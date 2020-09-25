package dovtech.betterfactions.util;

import api.DebugFile;
import api.entity.StarPlayer;
import com.google.gson.Gson;
import dovtech.betterfactions.BetterFactions;
import dovtech.betterfactions.contracts.Contract;
import dovtech.betterfactions.player.PlayerData;
import java.io.*;
import java.util.ArrayList;

public class DataUtil {

    private static final BetterFactions instance = BetterFactions.getInstance();
    private static final boolean debug = BetterFactions.getInstance().debugMode;
    private static final File contractsFolder = new File("moddata/BetterFactions/contract");
    private static final File playerDataFolder = new File("moddata/BetterFactions/player");

    public static ArrayList<Contract> contracts = new ArrayList<>();
    public static ArrayList<PlayerData> players = new ArrayList<>();
    public static ArrayList<Contract> contractWriteBuffer = new ArrayList<>();
    public static ArrayList<PlayerData> playerDataWriteBuffer = new ArrayList<>();

    public static PlayerData getPlayerData(StarPlayer player) {
        for(PlayerData playerData : players) {
            if(playerData.getPlayerName().equals(player.getName())) return playerData;
        }
        PlayerData newPlayerData = new PlayerData(player);
        players.add(newPlayerData);
        playerDataWriteBuffer.add(newPlayerData);
        return newPlayerData;
    }

    public static Contract getUpdatedContract(Contract contract) {
        for(Contract c : contracts) {
            if(c.getUid().equals(contract.getUid())) return c;
        }
        return contract;
    }

    public static void readData() throws IOException {
        Gson gson = new Gson();
        if(contractsFolder.listFiles() != null) {
            for(File contractFile : contractsFolder.listFiles()) {
                BufferedReader br = new BufferedReader(new FileReader(contractFile));
                contracts.add(gson.fromJson(br, Contract.class));
                br.close();
            }
        } else {
            if(debug) DebugFile.log("[DEBUG]: Contracts folder is empty, not reading...", instance);
        }

        if(playerDataFolder.listFiles() != null) {
            for(File playerDataFile : playerDataFolder.listFiles()) {
                BufferedReader br = new BufferedReader(new FileReader(playerDataFile));
                players.add(gson.fromJson(br, PlayerData.class));
                br.close();
            }
        } else {
            if(debug) DebugFile.log("[DEBUG]: PlayerData folder is empty, not reading...", instance);
        }
    }

    public static void writeData() {
        Gson gson = new Gson();
        for(Contract contract : contractWriteBuffer) {
            try {
                File contractFile = new File(contractsFolder.getAbsolutePath() + "/" + contract.getUid() + ".smdat");
                if(contractFile.exists()) contractFile.delete();
                contractFile.createNewFile();
                BufferedWriter bw = new BufferedWriter(new FileWriter(contractFile));
                bw.write(gson.toJson(contract));
                bw.close();
                contractWriteBuffer.remove(contract);
                if (!contracts.contains(contract)) contracts.add(contract);
            } catch (IOException e) {
                e.printStackTrace();
                DebugFile.log("[ERROR]: Something went wrong while trying to write contract " + contract.getName() + " to file!");
                /*
                if(debug) {
                    StringBuilder builder = new StringBuilder();
                    builder.append(contract.getClaimants().get(0).getName());
                    for(int i = 1; i < contract.getClaimants().size(); i ++) builder.append(", ").append(contract.getClaimants().get(i).getName());
                    DebugFile.log("[DEBUG]: Error Info[contractWriteBuffer.size() = " + contractWriteBuffer.size() + ", contractUID = '" + contract.getUid() + "', contractClaimants = [" + builder.toString() + "]]");
                }
                 */
            }
        }

        for(PlayerData playerData : playerDataWriteBuffer) {
            try {
                File playerDataFile = new File(playerDataFolder.getAbsolutePath() + "/" + playerData.getPlayerName() + ".smdat");
                if(playerDataFile.exists()) playerDataFile.delete();
                playerDataFile.createNewFile();
                BufferedWriter bw = new BufferedWriter(new FileWriter(playerDataFile));
                bw.write(gson.toJson(playerData));
                bw.close();
                playerDataWriteBuffer.remove(playerData);
                if (!players.contains(playerData)) players.add(playerData);
            } catch (IOException e) {
                e.printStackTrace();
                DebugFile.log("[ERROR]: Something went wrong while trying to write player " + playerData.getPlayerName() + " to file!");
                /*
                if(debug) {
                    StringBuilder builder = new StringBuilder();
                    builder.append(playerData.getContracts().get(0).getUid());
                    for(int i = 1; i < playerData.getContracts().size(); i ++) builder.append(", ").append(playerData.getContracts().get(i).getUid()));
                    DebugFile.log("[DEBUG]: Error Info[playerDataWriteBuffer.size() = " + playerDataWriteBuffer.size() + ", player = " + playerData.getPlayerName() + ", playerContracts = [" + builder.toString() + "]]");
                }
                 */
            }
        }
    }

    /*
    public static ArrayList<Contract> getContracts(StarPlayer internalPlayer) {
        ArrayList<Contract> playerContracts = new ArrayList<>();
        try {
            File playerDataFolder = new File("moddata/BetterFactions/player");
            if(!(playerDataFolder.exists())) {
                playerDataFolder.mkdir();
            }

            File pData = new File(playerDataFolder.getPath() + "/" + internalPlayer.getName() + ".smdat");
            if(!(pData.exists())) {
                savePlayer(new BetterPlayer(internalPlayer));
            }

            FileInputStream playerDataFile = new FileInputStream(pData.getPath());
            ObjectInputStream playerDataInput = new ObjectInputStream(playerDataFile);
            PlayerData playerData = (PlayerData) playerDataInput.readObject();
            if(playerData.contracts.length > 0 && playerData.contracts[0] != null) {
                for(String uid : playerData.contracts) {
                    Contract contract = getContractFromUUID(uid);
                    playerContracts.add(contract);
                    if(BetterFactions.getInstance().debugMode && contract != null) {
                        DebugFile.log("[DEBUG]: Added Contract " + contract.getName() + " to player " + internalPlayer.getName(), BetterFactions.getInstance());
                    }
                }
            } else {
                DebugFile.log("[DEBUG]: Player " + internalPlayer.getName() + "doesn't have any contracts", BetterFactions.getInstance());
            }
            playerDataInput.close();
            playerDataFile.close();

        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return playerContracts;
    }

    public static BetterFaction getBetterFaction(Faction internalFaction) {
        BetterFaction faction = new BetterFaction(internalFaction);

        try {
            File factionDataFolder = new File("moddata/BetterFactions/faction");
            if(!(factionDataFolder.exists())) {
                factionDataFolder.mkdir();
            }

            File factionFile = new File(factionDataFolder.getPath() + "/" + faction.getID() + ".smdat");
            if(!(factionFile.exists())) {
                saveFaction(new BetterFaction(internalFaction));
            }

            FileInputStream factionDataFile = new FileInputStream(factionFile.getPath());
            ObjectInputStream factionDataInput = new ObjectInputStream(factionDataFile);
            FactionData factionData = (FactionData) factionDataInput.readObject();
            faction.setFactionStats(factionData.factionStats);

            factionDataInput.close();
            factionDataFile.close();

        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        if(BetterFactions.getInstance().debugMode) DebugFile.log("[DEBUG]: Converted Faction " + internalFaction.getName() + " to BetterFaction", BetterFactions.getInstance());
        return faction;
    }

    public static ArrayList<BetterFaction> getAllFactions() {
        ArrayList<BetterFaction> factions = new ArrayList<>();
        for(Faction f : GameServer.getServerState().getFactionManager().getFactionCollection()) {
            BetterFaction faction = getBetterFaction(f);
            factions.add(faction);
        }
        return factions;
    }

    public static Contract getContractFromUUID(String uid) {
        ArrayList<Contract> contracts = getAllContracts();
        for(Contract contract : contracts) {
            if(contract.getUid().equals(uid)) {
                return contract;
            }
        }
        return null;
    }

    public static ArrayList<Contract> getAllContracts() {
        ArrayList<Contract> contracts = new ArrayList<>();

        try {
            File contractsFolder = new File("moddata/BetterFactions/contracts");
            if(!(contractsFolder.exists())) {
                contractsFolder.mkdir();
            }

            for(File contractFile : Objects.requireNonNull(contractsFolder.listFiles())) {
                FileInputStream contractDataFile = new FileInputStream(contractFile.getPath());
                ObjectInputStream contractDataInput = new ObjectInputStream(contractDataFile);
                ContractData contractData = (ContractData) contractDataInput.readObject();
                Contract.ContractType contractType = Contract.ContractType.BOUNTY;
                ContractTarget[] contractTarget = null;
                switch(contractData.contractType) {
                    case 0:
                        contractType = Contract.ContractType.BOUNTY;
                        contractTarget = new PlayerTarget[1];
                        contractTarget[0] = new PlayerTarget();
                        contractTarget[0].setTarget(new BetterPlayer(new StarPlayer(GameServer.getServerState().getPlayerFromName((contractData.targetData[0])))));
                        break;
                    case 1:
                        contractType = Contract.ContractType.CARGO_ESCORT;
                        contractTarget = new CargoTarget[contractData.targetData.length];
                        for(int cT = 0; cT < contractData.targetData.length; cT ++) {
                            String[] cString = contractData.targetData[cT].split(" : ");
                            ItemStack cargoStack = new ItemStack(Blocks.fromId(Short.parseShort(cString[0])).getId());
                            cargoStack.setAmount(Integer.parseInt(cString[1]));
                            contractTarget[cT] = new CargoTarget();
                            contractTarget[cT].setLocation(contractData.location);
                            contractTarget[cT].setTarget(cargoStack);
                        }
                        break;
                    case 2:
                        contractType = Contract.ContractType.MINING;
                        contractTarget = new MiningTarget[contractData.targetData.length];
                        for(int mT = 0; mT < contractData.targetData.length; mT ++) {
                            String[] mString = contractData.targetData[mT].split(" : ");
                            ItemStack miningStack = new ItemStack(Blocks.fromId(Short.parseShort(mString[0])).getId());
                            miningStack.setAmount(Integer.parseInt(mString[1]));
                            contractTarget[mT] = new MiningTarget();
                            contractTarget[mT].setTarget(miningStack);
                        }
                        break;
                    case 3:
                        contractType = Contract.ContractType.PRODUCTION;
                        contractTarget = new ProductionTarget[contractData.targetData.length];
                        for(int pT = 0; pT < contractData.targetData.length; pT ++) {
                            String[] pString = contractData.targetData[pT].split(" : ");
                            ItemStack productionStack = new ItemStack(Blocks.fromId(Short.parseShort(pString[0])).getId());
                            productionStack.setAmount(Integer.parseInt(pString[1]));
                            contractTarget[pT] = new ProductionTarget();
                            contractTarget[pT].setTarget(productionStack);
                        }
                        break;
                }

                Contract contract = new Contract(getBetterFaction(GameServer.getServerState().getFactionManager().getFaction(contractData.contractorID)), contractData.display, contractType, contractData.reward, contractData.uid, contractTarget);
                ArrayList<BetterPlayer> claimants = new ArrayList<>();
                for(String playerName : contractData.claimants) {
                    claimants.add(new BetterPlayer(new StarPlayer(Server.getPlayer(playerName))));
                }
                contract.setClaimants(claimants);
                contracts.add(contract);

                contractDataInput.close();
                contractDataFile.close();
                if(BetterFactions.getInstance().debugMode) DebugFile.log("[DEBUG]: Found Contract " + contract.getName() + " and added it to list", BetterFactions.getInstance());
            }

        } catch(IOException | ClassNotFoundException | PlayerNotFountException e) {
            e.printStackTrace();
        }

        return contracts;
    }

    public static void saveContract(Contract contract) {
        File contractsFolder = new File("moddata/BetterFactions/contracts");
        if(!(contractsFolder.exists())) {
            contractsFolder.mkdir();
        }

        ContractData contractData = new ContractData(contract);

        try {
            File contractDataFile = new File(contractsFolder.getPath() + "/" + contract.getUid() + ".smdat");
            if(contractDataFile.exists()) {
                contractDataFile.delete();
            }

            FileOutputStream contractDataOutput = new FileOutputStream(contractDataFile.getPath());
            ObjectOutputStream contractDataOutputStream = new ObjectOutputStream(contractDataOutput);
            contractDataOutputStream.writeObject(contractData);
            contractDataOutputStream.close();
            contractDataOutput.close();

            if(BetterFactions.getInstance().debugMode) DebugFile.log("[DEBUG]: Successfully saved Contract " + contract.getName(), BetterFactions.getInstance());

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveFaction(BetterFaction faction) {
        File factionsFolder = new File("moddata/BetterFactions/faction");
        if(!(factionsFolder.exists())) {
            factionsFolder.mkdir();
        }

        FactionData factionData = new FactionData(faction);

        try {
            File factionDataFile = new File(factionsFolder.getPath() + "/" + faction.getID() + ".smdat");
            if(factionDataFile.exists()) {
                factionDataFile.delete();
            }

            FileOutputStream factionDataOutput = new FileOutputStream(factionDataFile.getPath());
            ObjectOutputStream factionDataOutputStream = new ObjectOutputStream(factionDataOutput);
            factionDataOutputStream.writeObject(factionData);
            factionDataOutputStream.close();
            factionDataOutput.close();
            if(BetterFactions.getInstance().debugMode) DebugFile.log("[DEBUG]: Successfully saved Faction " + faction.getName(), BetterFactions.getInstance());

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void savePlayer(BetterPlayer player) {
        File playerFolder = new File("moddata/BetterFactions/player");
        if(!(playerFolder.exists())) {
            playerFolder.mkdir();
        }
        PlayerData playerData = new PlayerData(player);

        try {
            File playerDataFile = new File(playerFolder.getPath() + "/" + player.getName() + ".smdat");
            if(playerDataFile.exists()) {
                playerDataFile.delete();
            }

            FileOutputStream playerDataOutput = new FileOutputStream(playerDataFile.getPath());
            ObjectOutputStream playerDataOutputStream = new ObjectOutputStream(playerDataOutput);
            playerDataOutputStream.writeObject(playerData);
            playerDataOutputStream.close();
            playerDataOutput.close();

            if(BetterFactions.getInstance().debugMode) DebugFile.log("[DEBUG]: Successfully saved Player " + player.getName(), BetterFactions.getInstance());

        } catch(IOException e) {
            e.printStackTrace();
        }
    }
     */
}
