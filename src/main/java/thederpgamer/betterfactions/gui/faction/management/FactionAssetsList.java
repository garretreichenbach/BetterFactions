package thederpgamer.betterfactions.gui.faction.management;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/14/2021]
 */
public class FactionAssetsList
 {//public class FactionAssetsList extends ScrollableTableList<String> {

     /*
    private GUIAncor anchor;
    private FactionManagementTab managementTab;

    public FactionAssetsList(InputState inputState, GUIAncor anchor, FactionManagementTab managementTab) {
        super(inputState, 100, 100, anchor);
        this.anchor = anchor;
        this.managementTab = managementTab;
        anchor.attach(this);
    }

    public void redrawList() {
        clear();
        handleDirty();
    }

    @Override
    public void initColumns() {
        addColumn("Name", 15.0f, new Comparator<FactionEntityData>() {
            public int compare(FactionEntityData o1, FactionEntityData o2) {
                return o1.name.compareTo(o2.name);
            }
        });

        addColumn("Type", 8.5f, new Comparator<FactionEntityData>() {
            public int compare(FactionEntityData o1, FactionEntityData o2) {
                return o1.entityType.compareTo(o2.entityType);
            }
        });

        addColumn("Location", 10.0f, new Comparator<FactionEntityData>() {
            @Override
            public int compare(FactionEntityData o1, FactionEntityData o2) {
                Vector3i currentSector = GameClient.getClientPlayerState().getCurrentSector();
                Vector3i o1Sector = o1.getSector();
                Vector3i o2Sector = o2.getSector();
                float distance1 = Math.abs(Vector3fTools.distance(currentSector.x, currentSector.y, currentSector.z, o1Sector.x, o1Sector.y, o1Sector.z)) * (int) ServerConfig.SECTOR_SIZE.getCurrentState();
                float distance2 = Math.abs(Vector3fTools.distance(currentSector.x, currentSector.y, currentSector.z, o2Sector.x, o2Sector.y, o2Sector.z)) * (int) ServerConfig.SECTOR_SIZE.getCurrentState();
                return Float.compare(distance1, distance2);
            }
        });

        addColumn("Status", 8.0f, new Comparator<FactionEntityData>() {
            @Override
            public int compare(FactionEntityData o1, FactionEntityData o2) {
                return Float.compare(o1.getStatus(), o2.getStatus());
            }
        });

        addTextFilter(new GUIListFilterText<FactionEntityData>() {
            public boolean isOk(String s, FactionEntityData entityData) {
                return entityData.name.toLowerCase().contains(s.toLowerCase());
            }
        }, "NAME", ControllerElement.FilterRowStyle.LEFT);

        addDropdownFilter(new GUIListFilterDropdown<FactionEntityData, FactionEntityData.EntityType>(FactionEntityData.EntityType.values()) {
            @Override
            public boolean isOk(FactionEntityData.EntityType entityType, FactionEntityData entityData) {
                if(entityType == FactionEntityData.EntityType.ALL) return true;
                else return entityData.entityType.equals(entityType);
            }
        }, new CreateGUIElementInterface<FactionEntityData.EntityType>() {
            @Override
            public GUIElement create(FactionEntityData.EntityType entityType) {
                GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(entityType.name());
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(entityType);
                anchor.attach(dropDown);
                return anchor;
            }

            @Override
            public GUIElement createNeutral() {
                return null;
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        activeSortColumnIndex = 0;
    }

    @Override
    public ArrayList<FactionEntityData> getElementList() {
        ArrayList<FactionEntityData> entityDataList = new ArrayList<>();
        try {
            entityDataList.addAll(ClientCacheManager.factionAssets.values());
        } catch(Exception exception) {
            LogManager.logException("Encountered an exception while trying to fetch faction message inbox", exception);
        }
        return entityDataList;
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<FactionEntityData> set) {
        guiElementList.deleteObservers();
        guiElementList.addObserver(this);
        FactionMember playerFactionMember = FactionManagerOld.getPlayerFactionMember(GameClient.getClientPlayerState().getDisplayName());
        assert playerFactionMember != null;
        for(FactionEntityData entityData : set) {
            try {
                GUITextOverlayTable nameTextElement;
                (nameTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(entityData.name);
                GUIClippedRow nameRowElement;
                (nameRowElement = new GUIClippedRow(this.getState())).attach(nameTextElement);

                GUITextOverlayTable typeTextElement;
                (typeTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(WordUtils.capitalize(entityData.entityType.name().toLowerCase()));
                GUIClippedRow typeRowElement;
                (typeRowElement = new GUIClippedRow(this.getState())).attach(typeTextElement);

                GUITextOverlayTable locationTextElement;
                (locationTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(entityData.getSector().toString());
                GUIClippedRow locationRowElement;
                (locationRowElement = new GUIClippedRow(this.getState())).attach(locationTextElement);

                GUITextOverlayTable statusTextElement;
                (statusTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(entityData.getStatus() + "%");
                GUIClippedRow statusRowElement;
                (statusRowElement = new GUIClippedRow(this.getState())).attach(statusTextElement);

                FactionAssetsListRow row = new FactionAssetsListRow(getState(), entityData, nameRowElement, typeRowElement, locationRowElement, statusRowElement);
                if(playerFactionMember.hasPermission("manage.assets.[ANY]")) {
                    GUIAncor anchor = new GUIAncor(getState(), this.anchor.getWidth() - 28.0f, 28.0f);
                    anchor.attach(redrawButtonPane(entityData, playerFactionMember, anchor));
                    row.expanded = new GUIElementList(getState());
                    row.expanded.add(new GUIListElement(anchor, getState()));
                    row.expanded.attach(anchor);
                }
                row.onInit();
                guiElementList.addWithoutUpdate(row);
            } catch(Exception exception) {
                exception.printStackTrace();
            }
        }
        guiElementList.updateDim();
    }

    private GUIHorizontalButtonTablePane redrawButtonPane(final FactionEntityData entityData, final FactionMember playerFactionMember, GUIAncor anchor) {
        GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 0, 1, anchor);
        buttonPane.onInit();
        final FactionData factionData = playerFactionMember.getFactionData();
        int buttonIndex = 0;

        return buttonPane;
    }

    public class FactionAssetsListRow extends ScrollableTableList<FactionEntityData>.Row {

        public FactionAssetsListRow(InputState inputState, FactionEntityData entityData, GUIElement... guiElements) {
            super(inputState, entityData, guiElements);
            this.highlightSelect = true;
            this.highlightSelectSimple = true;
            this.setAllwaysOneSelected(true);
        }
    }

      */
}
