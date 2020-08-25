package dovtech.betterfactions.gui.contracts;

import dovtech.betterfactions.BetterFactions;
import dovtech.betterfactions.contracts.Contract;
import org.hsqldb.lib.StringComparator;
import org.schema.common.util.CompareTools;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

public class ContractsScrollableList extends ScrollableTableList<Contract> implements GUIActiveInterface {

    private ContractListRow selectedRow;

    public ContractsScrollableList(InputState state, float var2, float var3, GUIElement guiElement) {
        super(state, var2, var3, guiElement);
    }

    @Override
    public void initColumns() {
        new StringComparator();

        this.addColumn("Task", 15.0F, new Comparator<Contract>() {
            public int compare(Contract o1, Contract o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        this.addColumn("Type", 7.0F, new Comparator<Contract>() {
            public int compare(Contract o1, Contract o2) {
                return o1.getContractType().compareTo(o2.getContractType());
            }
        });

        this.addColumn("Contractor", 7.0F, new Comparator<Contract>() {
            public int compare(Contract o1, Contract o2) {
                return o1.getContractor().getName().compareTo(o2.getContractor().getName());
            }
        });

        this.addColumn("Reward", 5.0F, new Comparator<Contract>() {
            public int compare(Contract o1, Contract o2) {
                return CompareTools.compare(o1.getReward(), o2.getReward());
            }
        });

        this.addTextFilter(new GUIListFilterText<Contract>() {
            public boolean isOk(String s, Contract contract) {
                return contract.getName().toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        this.addDropdownFilter(new GUIListFilterDropdown<Contract, Contract.ContractType>(Contract.ContractType.values()) {
            public boolean isOk(Contract.ContractType contractType, Contract contract) {
                switch(contractType) {
                    case ALL: return true;
                    case CARGO_ESCORT: return contract.getContractType().equals(Contract.ContractType.CARGO_ESCORT);
                    case PRODUCTION: return contract.getContractType().equals(Contract.ContractType.PRODUCTION);
                    case BOUNTY: return contract.getContractType().equals(Contract.ContractType.BOUNTY);
                    case MINING: return contract.getContractType().equals(Contract.ContractType.MINING);
                }
                return true;
            }

        }, new CreateGUIElementInterface<Contract.ContractType>() {
            @Override
            public GUIElement create(Contract.ContractType contractType) {
                GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(contractType.displayName);
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(contractType);
                anchor.attach(dropDown);
                return anchor;
            }

            @Override
            public GUIElement createNeutral() {
                return null;
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        this.activeSortColumnIndex = 0;
    }

    @Override
    protected Collection<Contract> getElementList() {
        return BetterFactions.getInstance().getContracts();
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<Contract> set) {
        if(guiElementList.size() == 0) {
            for(Contract contract : set) {
                GUITextOverlayTable nameTextElement;
                (nameTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(contract.getName());
                GUIClippedRow nameRowElement;
                (nameRowElement = new GUIClippedRow(this.getState())).attach(nameTextElement);

                GUITextOverlayTable contractTypeTextElement;
                (contractTypeTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(contract.getContractType().displayName);
                GUIClippedRow contractTypeRowElement;
                (contractTypeRowElement = new GUIClippedRow(this.getState())).attach(contractTypeTextElement);

                GUITextOverlayTable contractorTextElement;
                (contractorTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(String.valueOf(contract.getContractor().getName()));
                GUIClippedRow contractorRowElement;
                (contractorRowElement = new GUIClippedRow(this.getState())).attach(contractorTextElement);

                GUITextOverlayTable rewardTextElement;
                (rewardTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(String.valueOf(contract.getReward()));
                GUIClippedRow rewardRowElement;
                (rewardRowElement = new GUIClippedRow(this.getState())).attach(rewardTextElement);

                ContractListRow contractListRow;
                (contractListRow = new ContractListRow(this.getState(), contract, nameRowElement, contractTypeRowElement, contractorRowElement, rewardRowElement)).onInit();
                guiElementList.addWithoutUpdate(contractListRow);
            }
        } else {
            for(Contract contract : set) {
                if(!(set.contains(contract))) {
                    GUITextOverlayTable nameTextElement;
                    (nameTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(contract.getName());
                    GUIClippedRow nameRowElement;
                    (nameRowElement = new GUIClippedRow(this.getState())).attach(nameTextElement);

                    GUITextOverlayTable contractTypeTextElement;
                    (contractTypeTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(contract.getContractType().displayName);
                    GUIClippedRow contractTypeRowElement;
                    (contractTypeRowElement = new GUIClippedRow(this.getState())).attach(contractTypeTextElement);

                    GUITextOverlayTable contractorTextElement;
                    (contractorTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(String.valueOf(contract.getContractor().getName()));
                    GUIClippedRow contractorRowElement;
                    (contractorRowElement = new GUIClippedRow(this.getState())).attach(contractorTextElement);

                    GUITextOverlayTable rewardTextElement;
                    (rewardTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(String.valueOf(contract.getReward()));
                    GUIClippedRow rewardRowElement;
                    (rewardRowElement = new GUIClippedRow(this.getState())).attach(rewardTextElement);

                    ContractListRow contractListRow;
                    (contractListRow = new ContractListRow(this.getState(), contract, nameRowElement, contractTypeRowElement, contractorRowElement, rewardRowElement)).onInit();
                    guiElementList.addWithoutUpdate(contractListRow);
                }
            }
        }
        guiElementList.updateDim();
    }

    public class ContractListRow extends ScrollableTableList<Contract>.Row {
        private Contract contract;

        public ContractListRow(InputState inputState, Contract contract, GUIElement... guiElements) {
            super(inputState, contract, guiElements);
            this.highlightSelect = true;
            this.highlightSelectSimple = true;
            this.contract = contract;
        }

        @Override
        public void clickedOnRow() {
            super.clickedOnRow();
            selectedRow = this;
        }

        public Contract getContract() {
            return contract;
        }
    }
}
