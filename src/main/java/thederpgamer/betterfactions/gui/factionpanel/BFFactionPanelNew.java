package thederpgamer.betterfactions.gui.factionpanel;

import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.faction.newfaction.FactionMemberScrollableListNew;
import org.schema.game.client.view.gui.faction.newfaction.FactionNewsScrollableListNew;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIScrollablePanel;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITextOverlayTable;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.diplomacy.FactionDiplomacyEntity;
import thederpgamer.betterfactions.manager.FactionDiplomacyManager;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class BFFactionPanelNew extends FactionPanelNew {

	public GUIMainWindow factionPanel;
	private GUIContentPane newsTab;
	private GUIContentPane membersTab;
	private GUIContentPane diplomacyTab;
	private GUIContentPane optionTab;
	private BFFactionScrollableListNew fList;
	private boolean init;
	private int fid;
	private boolean flagFactionTabRecreate;
	private FactionMemberScrollableListNew mList;
	private FactionNewsScrollableListNew nList;
	private GUIAncor topList;
	private GUIAncor bottomList;
	private GUIFactionsScrollableList factionDiploList;
	private GUIFactionDiploScrollableList gPlayer;
	private GUIFactionDiploScrollableList gFaction;
	private GUIAncor bottomHead;
	private GUIAncor topHead;
	private GUIAncor infoPanel;
	private GUIContentPane listTab;


	public BFFactionPanelNew(InputState inputState) {
		super(inputState);
	}

	@Override
	public void onInit() {
		factionPanel = new GUIMainWindow(getState(), 850, 550, "FactionPanelNew");
		factionPanel.onInit();

		factionPanel.setCloseCallback(new GUICallback() {
			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if(event.pressedLeftMouse()) getState().getWorldDrawer().getGuiDrawer().getPlayerPanel().deactivateAll();
			}

			@Override
			public boolean isOccluded() {
				return !getState().getController().getPlayerInputs().isEmpty();
			}
		});

		factionPanel.orientate(ORIENTATION_HORIZONTAL_MIDDLE | ORIENTATION_VERTICAL_MIDDLE);

		recreateTabs();

		this.fid = getOwnPlayer().getFactionId();

		init = true;
	}

	public void recreateTabs() {
		Object beforeTab = null;
		if(factionPanel.getSelectedTab() < factionPanel.getTabs().size()) beforeTab = factionPanel.getTabs().get(factionPanel.getSelectedTab()).getTabName();
		factionPanel.clearTabs();
		newsTab = factionPanel.addTab(Lng.str("NEWS"));
		diplomacyTab = factionPanel.addTab(Lng.str("DIPLOMACY"));

		createNewsPane();
		createDiplomacyPane();

		if(getOwnFaction() != null) {
			newsTab = factionPanel.addTab(Lng.str("FACTION NEWS"));
			membersTab = factionPanel.addTab(Lng.str("MEMBERS"));
			createNewsPane();
			createMembersPane();
		}
		listTab = factionPanel.addTab(Lng.str("LIST"));
		optionTab = factionPanel.addTab(Lng.str("OPTIONS"));

		createFactionListPane();
		createOptionPane();

		factionPanel.activeInterface = this;
		if (beforeTab != null) {
			for (int i = 0; i < factionPanel.getTabs().size(); i++) {
				if (factionPanel.getTabs().get(i).getTabName().equals(beforeTab)) {
					factionPanel.setSelectedTab(i);
					break;
				}
			}
		}
	}

	public void createNewsPane() {
		newsTab.setTextBoxHeightLast(28);
		GUIAncor content = newsTab.getContent(0);
		GUIFactionNewsScrollableList c = new GUIFactionNewsScrollableList(getState(), content);
		c.onInit();
		content.attach(c);
	}

	public void createDiplomacyPane() {
		diplomacyTab.setTextBoxHeightLast(270);
		diplomacyTab.addNewTextBox(28);
		infoPanel = diplomacyTab.getContent(0, 1);

		factionDiploList = new GUIFactionsScrollableList(getState(), diplomacyTab.getContent(0, 0), this);
		factionDiploList.onInit();
		diplomacyTab.getContent(0, 0).attach(factionDiploList);

		diplomacyTab.addDivider(290);
		diplomacyTab.setTextBoxHeightLast(1, 48);
		diplomacyTab.addNewTextBox(1, 190);
		diplomacyTab.addNewTextBox(1, 48);
		diplomacyTab.addNewTextBox(1, 200);

		topHead = diplomacyTab.getContent(1, 0);
		topList = diplomacyTab.getContent(1, 1);
		bottomHead = diplomacyTab.getContent(1, 2);
		bottomList = diplomacyTab.getContent(1, 3);
	}

	public void createMembersPane() {
		if(mList != null) mList.cleanUp();
		mList = new FactionMemberScrollableListNew(getState(), membersTab.getContent(0), getOwnFaction());
		mList.onInit();
		membersTab.getContent(0).attach(mList);
	}

	public void createFactionListPane() {
		if(fList != null) fList.cleanUp();
		fList = new BFFactionScrollableListNew(getState(), listTab.getContent(0));
		fList.onInit();
		listTab.getContent(0).attach(fList);
	}

	public void createOptionPane() {
		BFFactionOptionPersonalContent c0 = new BFFactionOptionPersonalContent(getState(), this);
		c0.onInit();
		optionTab.setContent(0, c0);
		optionTab.setTextBoxHeightLast(86);
		optionTab.addNewTextBox(10);
		BFFactionOptionFactionContent c1 = new BFFactionOptionFactionContent(getState(), this);
		c1.onInit();
		optionTab.setContent(1, c1);
	}


	@Override
	public void draw() {
		if(!init) onInit();
		if(flagFactionTabRecreate) {
			recreateTabs();
			flagFactionTabRecreate = false;
		}
		factionPanel.draw();
	}

	@Override
	public void update(Timer timer) {
		if(init) {
			if(this.fid != getOwnPlayer().getFactionId()) {
				if(getOwnPlayer().getFactionId() >= 0 || getOwnFaction() != null) {
					flagFactionTabRecreate = true;
					this.fid = getOwnPlayer().getFactionId();
				}
			}

		}
	}

	@Override
	public void cleanUp() {

	}

	@Override
	public GameClientState getState() {
		return ((GameClientState) super.getState());
	}

	@Override
	public float getHeight() {
		return factionPanel.getHeight();
	}

	@Override
	public float getWidth() {
		return factionPanel.getWidth();
	}

	@Override
	public boolean isActive() {
		return getState().getController().getPlayerInputs().isEmpty();
	}

	public PlayerState getOwnPlayer() {
		return BFFactionPanelNew.this.getState().getPlayer();
	}

	public Faction getOwnFaction() {
		return BFFactionPanelNew.this.getState().getFactionManager().getFaction(getOwnPlayer().getFactionId());
	}

	public void onSelectFaction(final Faction f) {
		if(gPlayer != null) gPlayer.cleanUp();
		if(gFaction != null) gFaction.cleanUp();
		bottomList.getChilds().clear();
		topList.getChilds().clear();

		gPlayer = new GUIFactionDiploScrollableList(getState(), getState().getPlayer().getDbId(), f, topList);
		gPlayer.onInit();
		topList.attach(gPlayer);

		gFaction = new GUIFactionDiploScrollableList(getState(), getState().getPlayer().getFactionId(), f, bottomList);
		gFaction.onInit();
		bottomList.attach(gFaction);

		GUIScrollablePanel topScroll = new GUIScrollablePanel(10, 10, topHead, getState());

		GUITextOverlayTable lTop = new GUITextOverlayTable(2, 2, getState());
		lTop.autoWrapOn = topHead;
		lTop.autoHeight = true;
		lTop.setTextSimple(new Object(){
			@Override
			public String toString() {
				FactionRelation.RType ownRelationTo = getOwnRelationTo(f);
				String rel = "";
				switch(ownRelationTo){
					case ENEMY:
						rel = Lng.str("They consider you an enemy");
						break;
					case FRIEND:
						rel = Lng.str("They consider you an ally");
						break;
					case NEUTRAL:
						rel = Lng.str("Neutral relation");
						break;
				}
				FactionDiplomacyEntity ent = FactionDiplomacyManager.getDiplomacy(fid).entities.get(getOwnPlayer().getDbId());
				int points = 0;
				int raw = 0;
				if(ent != null){
					points = ent.getPoints();
					raw = ent.getRawPoints();
				}

				return Lng.str("Personal Relation Status: %s (%s w/o status)", points, raw)+"\n"+rel;
			}

		});
		lTop.onInit();
		GUIScrollablePanel bottomScroll = new GUIScrollablePanel(10, 10, bottomHead, getState());

		GUITextOverlayTable lBottom = new GUITextOverlayTable(2, 2, getState());
		lBottom.autoHeight = true;
		lBottom.autoWrapOn = bottomHead;
		lBottom.setTextSimple(new Object(){
			@Override
			public String toString() {
				FactionRelation.RType ownRelationTo = getOwnRelationTo(f);

				String rel = "";
				switch(ownRelationTo){
					case ENEMY:
						rel = Lng.str("They consider your faction enemy");
						break;
					case FRIEND:
						rel = Lng.str("They consider your faction an ally");
						break;
					case NEUTRAL:
						rel = Lng.str("Neutral relation to your faction");
						break;
				}
				FactionDiplomacyEntity ent =FactionDiplomacyManager.getDiplomacy(fid).entities.get(getState().getPlayer().getFactionId());
				int points = 0;
				int raw = 0;
				if(ent != null){
					points = ent.getPoints();
					raw = ent.getRawPoints();
				}

				return Lng.str("Faction Relation Status: %s (%s w/o status)", points, raw)+"\n"+rel;
			}

		});
		lBottom.onInit();


		topScroll.setContent(lTop);
		bottomScroll.setContent(lBottom);

		topScroll.onInit();
		bottomScroll.onInit();

		topHead.getChilds().clear();
		bottomHead.getChilds().clear();

		topHead.attach(topScroll);
		bottomHead.attach(bottomScroll);
		putinInfoPanel(f);
	}

	public void putinInfoPanel(final Faction f) {
		GUIScrollablePanel sc = new GUIScrollablePanel(10, 10, infoPanel, getState());
		GUITextOverlayTable lBottom = new GUITextOverlayTable(2, 2, getState());
		lBottom.autoHeight = true;
		lBottom.autoWrapOn = infoPanel;
		lBottom.setTextSimple(new Object(){
			@Override
			public String toString() {
				return f.getName()+"\n"+f.getDescription();
			}

		});
		lBottom.onInit();


		sc.setContent(lBottom);

		sc.onInit();
		infoPanel.getChilds().clear();
		infoPanel.attach(sc);
	}

	public FactionRelation.RType getOwnRelationTo(Faction f){
		return getState().getFactionManager().getRelation(getState().getPlayerName(), getState().getPlayer().getFactionId(), f.getIdFaction());
	}
}
