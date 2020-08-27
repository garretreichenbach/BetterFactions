package dovtech.betterfactions.gui.contracts;

import dovtech.betterfactions.contracts.Contract;
import dovtech.betterfactions.player.BetterPlayer;
import org.hsqldb.lib.StringComparator;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

public class ContractClaimantsScrollableList extends ScrollableTableList<BetterPlayer> implements GUIActiveInterface {

    private Contract contract;

    public ContractClaimantsScrollableList(InputState state, float var2, float var3, GUIElement guiElement, Contract contract) {
        super(state, var2, var3, guiElement);
        this.contract = contract;
    }

    @Override
    public void initColumns() {
        new StringComparator();

        this.addColumn("Name", 20.0F, new Comparator<BetterPlayer>() {
            public int compare(BetterPlayer o1, BetterPlayer o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        this.addColumn("Faction", 25.0F, new Comparator<BetterPlayer>() {
            public int compare(BetterPlayer o1, BetterPlayer o2) {
                return o1.getFaction().getName().compareTo(o2.getFaction().getName());
            }
        });

        this.addTextFilter(new GUIListFilterText<BetterPlayer>() {
            public boolean isOk(String s, BetterPlayer player) {
                return player.getName().toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.FULL);

        this.activeSortColumnIndex = 0;
    }

    @Override
    protected Collection<BetterPlayer> getElementList() {
        return contract.getClaimants();
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<BetterPlayer> set) {
        for(BetterPlayer player : set) {
            GUITextOverlayTable nameTextElement;
            (nameTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(player.getName());
            GUIClippedRow nameRowElement;
            (nameRowElement = new GUIClippedRow(this.getState())).attach(nameTextElement);

            GUITextOverlayTable factionTextElement;
            (factionTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(player.getFaction().getName());
            GUIClippedRow factionRowElement;
            (factionRowElement = new GUIClippedRow(this.getState())).attach(factionTextElement);

            ClaimantListRow claimantListRow;
            (claimantListRow = new ClaimantListRow(this.getState(), player, nameRowElement, factionRowElement)).onInit();

            claimantListRow.onInit();
            guiElementList.addWithoutUpdate(claimantListRow);
        }
        guiElementList.updateDim();
    }

    public class ClaimantListRow extends ScrollableTableList<BetterPlayer>.Row {

        public ClaimantListRow(InputState inputState, BetterPlayer player, GUIElement... guiElements) {
            super(inputState, player, guiElements);
            this.highlightSelect = true;
            this.highlightSelectSimple = true;
            this.setAllwaysOneSelected(true);
        }
    }
}