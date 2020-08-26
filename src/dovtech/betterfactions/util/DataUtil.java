package dovtech.betterfactions.util;

import api.common.GameServer;
import api.element.block.Blocks;
import api.element.inventory.ItemStack;
import api.entity.StarPlayer;
import dovtech.betterfactions.contracts.Contract;
import dovtech.betterfactions.contracts.ContractData;
import dovtech.betterfactions.contracts.target.*;
import dovtech.betterfactions.faction.BetterFaction;
import dovtech.betterfactions.faction.FactionData;
import dovtech.betterfactions.player.BetterPlayer;
import dovtech.betterfactions.player.PlayerData;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.PlayerNotFountException;
import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

public class DataUtil {

    public static ArrayList<Contract> getContracts(StarPlayer internalPlayer) {
        ArrayList<Contract> playerContracts = new ArrayList<>();

        try {
            File playerDataFolder = new File("moddata/BetterFactions/player");
            if(!(playerDataFolder.exists())) {
                playerDataFolder.mkdir();
            }

            FileInputStream playerDataFile = new FileInputStream(playerDataFolder.getPath() + "/" + internalPlayer.getId() + ".smdat");
            ObjectInputStream playerDataInput = new ObjectInputStream(playerDataFile);
            PlayerData playerData = (PlayerData) playerDataInput.readObject();
            for(String uid : playerData.contracts) {
                playerContracts.add(getContractFromUUID(uid));
            }
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

            FileInputStream factionDataFile = new FileInputStream(factionDataFolder.getPath() + "/" + internalFaction.getIdFaction() + ".smdat");
            ObjectInputStream factionDataInput = new ObjectInputStream(factionDataFile);
            FactionData factionData = (FactionData) factionDataInput.readObject();
            faction.setFactionStats(factionData.factionStats);

        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return faction;
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

            for(File contractFile : Objects.requireNonNull(contractsFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".smdat");
                }
            }))) {
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

                Contract contract = new Contract(getBetterFaction(GameServer.getServerState().getFactionManager().getFaction(contractData.contractorID)), contractData.display, contractType, contractData.reward, contractTarget);
                contracts.add(contract);
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
            FileOutputStream contractDataFile = new FileOutputStream(contractsFolder.getPath() + "/" + contract.getUid() + ".smdat");
            ObjectOutputStream contractDataOutput = new ObjectOutputStream(contractDataFile);
            contractDataOutput.writeObject(contractData);
            contractDataFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
