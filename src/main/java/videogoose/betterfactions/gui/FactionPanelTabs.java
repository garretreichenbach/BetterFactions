package videogoose.betterfactions.gui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.util.StringTools;
import org.schema.game.client.controller.PlayerCreateFactionNewsInputNew;
import org.schema.game.client.controller.PlayerFactionPointDialogNew;
import org.schema.game.client.controller.PlayerGameDropDownInput;
import org.schema.game.client.controller.PlayerOkCancelInput;
import org.schema.game.client.controller.PlayerTextInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.faction.newfaction.*;
import org.schema.game.client.view.gui.npc.GUINPCFactionNewsScrollableList;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.TextCallback;
import org.schema.schine.input.InputState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITabbedContent;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITextOverlayTable;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.civics.CivicRegistry;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.FactionCivics;
import org.schema.game.common.data.element.ElementKeyMap;
import videogoose.betterfactions.Perms;
import videogoose.betterfactions.diplomacy.DiplomacyEntity;
import videogoose.betterfactions.diplomacy.FactionStrength;
import videogoose.betterfactions.diplomacy.Federation;
import videogoose.betterfactions.diplomacy.Reputation;
import videogoose.betterfactions.diplomacy.FederationCommand;
import videogoose.betterfactions.diplomacy.TreatyTypeRegistry;
import videogoose.betterfactions.emblem.FactionEmblemCache;
import videogoose.betterfactions.emblem.GUIFactionEmblem;

import javax.vecmath.Vector4f;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The faction panel's tabs, rebuilt by the mod whenever the engine rebuilds the panel: an overview,
 * holdings, members, diplomacy, federation and settings.
 *
 * <p>The engine fires the panel hook after adding its own tabs and before restoring the selected tab
 * by name, so clearing them here and adding these keeps the player on the tab they were on - as long
 * as a tab keeps its name, which is why the names are constants.</p>
 */
public class FactionPanelTabs implements GUIActiveInterface {

	/**
	 * One per panel, so what was selected survives the panel rebuilding its tabs.
	 */
	private static final Map<FactionPanelNew, FactionPanelTabs> TABS = new WeakHashMap<>();

	private final FactionPanelNew panel;

	private FactionPanelTabs(FactionPanelNew panel) {
		this.panel = panel;
	}

	public static void addTabs(FactionPanelNew panel, GUIMainWindow window) {
		FactionPanelTabs t = TABS.computeIfAbsent(panel, FactionPanelTabs::new);
		t.factionPanel = window;
		t.build();
	}

	private void build() {
		factionPanel.clearTabs();
		overviewTab = factionPanel.addTab(TAB_OVERVIEW);
		createOverviewPane();
		if (getOwnFaction() != null) {
			assetsTab = factionPanel.addTab(TAB_ASSETS);
			createAssetsPane();
			governanceTab = factionPanel.addTab(Lng.str("GOVERNANCE"));
			createGovernancePane();
		}
		diplomacyTab = factionPanel.addTab(TAB_DIPLOMACY);
		createFactionDiplomacyPane();
		federationTab = factionPanel.addTab(TAB_FEDERATION);
		createFederationPane();
		settingsTab = factionPanel.addTab(TAB_SETTINGS);
		createSettingsPane();
		factionPanel.activeInterface = this;
		promptCivics();
	}

	/**
	 * Once a session: a faction with no civics yet (new, or from before civics) is asked to pick them,
	 * then its fleet doctrine; one with civics but no doctrine is asked for the doctrine.
	 */
	private static boolean civicsPrompted;

	private void promptCivics() {
		Faction own = getOwnFaction();
		//version 0: the join sync has not arrived, so we cannot tell yet
		if (civicsPrompted || own == null || Civics.client().version() == 0
				|| !getOwnPlayer().getFactionController().hasPermissionEditPermission()) {
			return;
		}
		//the doctrine is not prompted for: its three cards sit in Faction Settings, under the civics
		if (Civics.client().of(own.getIdFaction()).civics.isEmpty()) {
			civicsPrompted = true;
			CivicChooser.open(getState(), 0, true);
		}
	}

	public GameClientState getState() {
		return (GameClientState) panel.getState();
	}

	@Override
	public boolean isActive() {
		return getState().getController().getPlayerInputs().isEmpty();
	}

	public GUIMainWindow factionPanel;
    private GUIContentPane overviewTab;
	private GUIContentPane governanceTab;
	private GUITabbedContent governanceSub;
	private GUITabbedContent settingsSub;
	/** The MEMBERS sub-tab of SETTINGS. */
	private GUIContentPane membersTab;
	/** Governance's card column; the summary beside it takes the rest. */
	private static final int GOVERNANCE_CARDS_WIDTH = 640;
    private GUIContentPane assetsTab;
    private FactionAssetScrollableListNew aList;
    private FactionDesignsList dList;
    private GUITabbedContent assetsSub;
    private BattleReportList rList;
    private GUITabbedContent newsSub;
	private GUIContentPane diplomacyTab;
    private GUIContentPane settingsTab;
	private FactionScrollableListNew fList;
	private boolean init;
	private int fid;
	private FactionMemberScrollableListNew mList;
	private FactionNewsScrollableListNew nList;
    /**
     * Held rather than built and forgotten: the list registers itself as an observer on the news
     * store, and the tab is rebuilt whenever the player's faction changes.
     */
    private GUINPCFactionNewsScrollableList gList;
    /**
     * Which log the overview is showing. Lives here rather than on the tab because switching it
     * rebuilds the tabs, and this has to survive that.
     */
    private boolean overviewGalaxy;
    /**
     * Hoisted because the panel restores the selected tab by matching the name string after a
     * rebuild: a literal repeated in two places silently resets the selection when one of them
     * changes.
     */
    private static final String TAB_DIPLOMACY = "DIPLOMACY";
    private static final String TAB_OVERVIEW = "OVERVIEW";
    private static final String TAB_ASSETS = "ASSETS";
    private static final String TAB_FEDERATION = "FEDERATION";
    private static final String TAB_SETTINGS = "SETTINGS";
    /**
     * Left column of the overview, in px. The same width the diplomacy tab gives its list, so the
     * two line up when you switch between them.
     */
    private static final int OVERVIEW_IDENTITY_WIDTH = 380;
    /**
     * Name, size, power and points: four lines and a bar, none of which wrap.
     */
    private static final int OVERVIEW_VITALS_HEIGHT = 172;
    /**
     * One row of buttons above a list, on the overview and the member tab alike.
     */
    private static final int BAR_HEIGHT = 30;
    private static final int POWER_BAR_HEIGHT = 6;
    /**
     * The power bar's three segments, and the swatches in the key beneath it. One set of constants
     * because the whole point of the key is that a colour means the same thing in both places.
     */
    private static final Vector4f TERRITORY_COLOR = new Vector4f(0.03f, 0.76f, 0.33f, 1.0f);
    private static final Vector4f PEOPLE_COLOR = new Vector4f(0.16f, 0.58f, 0.75f, 1.0f);
    private static final Vector4f INCOME_COLOR = new Vector4f(0.44f, 0.48f, 0.52f, 1.0f);
    private static final Vector4f MILITARY_COLOR = new Vector4f(0.85f, 0.42f, 0.30f, 1.0f);
    private static final Vector4f DIM = new Vector4f(0.55f, 0.58f, 0.60f, 1.0f);
    /**
     * Gap between the power bar's segments, so a term worth almost nothing still reads as a sliver
     * of its own rather than merging into its neighbour.
     */
    private static final int POWER_BAR_GAP = 2;
    private static final int CHIP_HEIGHT = 19;
    /**
     * Every chip the same width, so the row does not reflow as counts come and go. Three of them
     * plus the scope switch is what fits the right column at the window's default 960px - check
     * that again before adding a fourth.
     */
    private static final int CHIP_WIDTH = 104;
    private static final int SCOPE_WIDTH = 78;
    private static final int NEWS_WRAP_CHARS = 56;
    /**
     * Left column, in px: wide enough for name, relation and the treaty summary together.
     */
    private static final int DIPLOMACY_LIST_WIDTH = 480;
    /** Relation and opinion, then the power comparison: two lines. */
    private static final int DIPLOMACY_HEADER_HEIGHT = 56;
    /** About eight rows before the list scrolls; the selected faction's identity gets the rest of the column. */
    private static final int DIPLOMACY_FACTION_LIST_HEIGHT = 240;
    /** The emblem at the top of the description box, above the government. */
    private static final int DESCRIPTION_EMBLEM_SIZE = 128;
    /** The description's share of its column; the modifiers box under it takes the rest. */
    private static final float DESCRIPTION_FRACTION = 0.5f;
    /**
     * The same box under the diplomacy list. The engine takes a fraction of the whole column, not
     * of what is left, so the list and vitals above it have to come out of this share or the column
     * runs off the window and nothing scrolls.
     */
    private static final float DIPLOMACY_DESCRIPTION_FRACTION = 0.25f;
    /**
     * Name, founding, chair and the charter text.
     */
    private static final int FEDERATION_CARD_HEIGHT = 96;
    /**
     * Five governance rows.
     */
    private static final int FEDERATION_GOVERNANCE_HEIGHT = 204;
    /**
     * Wider than the diplomacy list: the member list carries power and two row buttons.
     */
    private static final int FEDERATION_LEFT_WIDTH = 360;
    /**
     * Treaty list, as a share of the right column, so it keeps its proportion when resized.
     */
    private static final float DIPLOMACY_TREATY_FRACTION = 0.34f;
    private static final int DIPLOMACY_TREATY_HEIGHT = 190;
	private GUIContentPane listTab;
    /**
     * Line break inside the header's assembled text.
     */
    private static final String BREAK = "\n";
    private DiplomacyFactionList diplomacyFactionList;
    private DiplomacyTreatyList treatyList;
    private DiplomacyOpinionList opinionList;
    private GUIAnchor diplomacyHeader;
    private GUIAnchor diplomacyVitals;
    private GUIAnchor diplomacyAbout;
    private GUIAnchor diplomacyModifiers;
    private GUIContentPane federationTab;
    private FederationList federationList;
    private FederationMemberList federationMemberList;
    /**
     * Whether the federation tab was built in its member layout, so joining or leaving can be noticed.
     */
    private boolean builtInFederation;
    /**
     * Survives a tab rebuild: the bloc clicked on the browse screen.
     */
    private Federation selectedFederation;
    /**
     * Survives a tab rebuild, so changing faction does not drop what you were looking at.
     */
    private Faction selectedDiplomacyFaction;






    public void createFactionDiplomacyPane() {
        if (diplomacyFactionList != null) {
            diplomacyFactionList.cleanUp();
        }
        if (treatyList != null) {
            treatyList.cleanUp();
        }
        if (opinionList != null) {
            opinionList.cleanUp();
        }

        diplomacyTab.addDivider(DIPLOMACY_LIST_WIDTH);

        //the list, then the selected faction under it: what they are, then what they say about themselves
        diplomacyTab.setTextBoxHeightLast(0, DIPLOMACY_FACTION_LIST_HEIGHT);
        diplomacyTab.addNewTextBox(0, OVERVIEW_VITALS_HEIGHT);
        diplomacyTab.addNewTextBox(0, 10);
        diplomacyTab.setTextBoxHeightFractionLast(0, DIPLOMACY_DESCRIPTION_FRACTION);
        diplomacyTab.addNewTextBox(0, 10);
        diplomacyVitals = diplomacyTab.getContent(0, 1);
        diplomacyAbout = diplomacyTab.getContent(0, 2);
        diplomacyModifiers = diplomacyTab.getContent(0, 3);

        diplomacyFactionList = new DiplomacyFactionList(getState(), diplomacyTab.getContent(0, 0), this);
        diplomacyFactionList.onInit();
        diplomacyTab.getContent(0, 0).attach(diplomacyFactionList);

        diplomacyTab.setTextBoxHeightLast(1, DIPLOMACY_HEADER_HEIGHT);
        diplomacyTab.addNewTextBox(1, DIPLOMACY_TREATY_HEIGHT);
        diplomacyTab.setTextBoxHeightFractionLast(1, DIPLOMACY_TREATY_FRACTION);
        //the last box in a div stretches to fill what is left, so the opinion breakdown gets whatever
        //the header and the treaty list do not need
        diplomacyTab.addNewTextBox(1, DIPLOMACY_TREATY_HEIGHT);

        diplomacyHeader = diplomacyTab.getContent(1, 0);

        treatyList = new DiplomacyTreatyList(getState(), diplomacyTab.getContent(1, 1));
        treatyList.onInit();
        diplomacyTab.getContent(1, 1).attach(treatyList);

        opinionList = new DiplomacyOpinionList(getState(), diplomacyTab.getContent(1, 2));
        opinionList.onInit();
        diplomacyTab.getContent(1, 2).attach(opinionList);

        if (selectedDiplomacyFaction != null) {
            onSelectFaction(selectedDiplomacyFaction);
        } else {
            fillDiplomacyHeader(null);
            fillDiplomacyIdentity(null);
        }
    }

    /**
     * Brings the diplomacy tab up on a particular faction, for the shortcut on the faction list.
     */
    public void openDiplomacyFor(Faction f) {
        for (int i = 0; i < factionPanel.getTabs().size(); i++) {
            if (TAB_DIPLOMACY.equals(factionPanel.getTabs().get(i).getTabName())) {
                factionPanel.setSelectedTab(i);
				break;
			}
		}
		onSelectFaction(f);
	}


	/**
	 * Institutions first, then a sub-tab for each institution the faction holds. Adopting or dissolving
	 * one rebuilds the tabs, so the sub-tabs follow what is held.
	 */
	private void createGovernancePane() {
		int wasSelected = governanceSub == null ? 0 : governanceSub.getSelectedTab();
		GUITabbedContent sub = governanceSub = new GUITabbedContent(getState(), governanceTab.getContent(0));
		sub.activationInterface = this;
		sub.onInit();
		governanceTab.getContent(0).attach(sub);

		int own = getOwnFaction().getIdFaction();
		List<String> held = new ArrayList<>(Civics.client().of(own).institutions);
		String senateName = Civics.client().of(own).senateName();
		GUIContentPane institutions = sub.addTab(Lng.str("INSTITUTIONS"));
		InstitutionsContent ic = new InstitutionsContent(getState(), this);
		cardsBeside(institutions, ic, ic::summary);
		if (held.contains(CivicRegistry.SENATE)) {
			GUIContentPane senate = sub.addTab(senateName.toUpperCase(Locale.ROOT));
			SenateContent sc = new SenateContent(getState(), this);
			cardsBeside(senate, sc, sc::summary, Lng.str("%s RULES", senateName.toUpperCase(Locale.ROOT)),
					() -> getOwnPlayer().getFactionController().hasPermissionEditPermission(),
					() -> new SenateRulesDialog(getState(), getOwnFaction()).activate());
		}
		if (held.contains(CivicRegistry.COUNCIL)) {
			//the ring and its detail need the full width, so no summary beside it
			GUIContentPane council = sub.addTab(Lng.str("COUNCIL"));
			CouncilContent cc = new CouncilContent(getState(), this);
			cc.onInit();
			council.setContent(0, 0, cc);
		}
		sub.setSelectedTab(Math.min(wasSelected, sub.getTabs().size() - 1));

		institutions.getContent(0, 0).attach(new GUIAnchor(getState(), 0, 0) {
			@Override
			public void draw() {
				Faction f = getOwnFaction();
				//a renamed Senate rebuilds too, so its tab carries the new name
				if (f != null && (!Civics.client().of(f.getIdFaction()).institutions.equals(held)
						|| !Civics.client().of(f.getIdFaction()).senateName().equals(senateName))) {
					requestRebuild();
				}
			}
		});
	}

	/** Cards on the left, a summary that keeps itself current on the right. */
	private void cardsBeside(GUIContentPane pane, GUIAnchor cards, Supplier<String> summary) {
		cardsBeside(pane, cards, summary, null, null, null);
	}

	/** @param action a button above the summary, beside what it changes, shown while allowed; null for none */
	private void cardsBeside(GUIContentPane pane, GUIAnchor cards, Supplier<String> summary,
							 Object action, BooleanSupplier allowed, Runnable onPress) {
		pane.addDivider(GOVERNANCE_CARDS_WIDTH);
		cards.onInit();
		pane.setContent(0, 0, cards);
		int textBox = 0;
		if (action != null) {
			pane.setTextBoxHeightLast(1, BAR_HEIGHT);
			pane.addNewTextBox(1, 10);
			GUIHorizontalButtonTablePane buttons = new GUIHorizontalButtonTablePane(getState(), 1, 1, pane.getContent(1, 0));
			buttons.onInit();
			buttons.addButton(0, 0, action, GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse() && allowed.getAsBoolean()) {
						onPress.run();
					}
				}

				@Override
				public boolean isOccluded() {
					return !allowed.getAsBoolean();
				}
			}, activeWhile(allowed));
			pane.getContent(1, 0).attach(buttons);
			textBox = 1;
		}
		wrappedText(pane.getContent(1, textBox), new Object() {
			@Override
			public String toString() {
				return summary.get();
			}
		});
	}

	public void createMembersPane() {
		if(mList != null) {
			mList.cleanUp();
		}
        membersTab.setTextBoxHeightLast(BAR_HEIGHT);
        membersTab.addNewTextBox(10);
        fillMemberBar(membersTab.getContent(0));

        mList = new FactionMemberScrollableListNew(getState(), membersTab.getContent(1), getOwnFaction());
		mList.onInit();

        membersTab.getContent(1).attach(mList);
    }

    /**
     * Inviting and ranks sit above the member list rather than in the options grid, because both are
     * about the people in the list beside them.
     *
     * <p>Ranks is a shortcut - the ladder is defined under governance, with the government type that
     * names its rungs - but this is where you are standing when you want it, the same way the faction
     * list keeps a shortcut into the diplomacy tab.</p>
     *
     * <p>Invites this faction has sent live here too. They are waiting on somebody else rather than
     * on you, which is why they are not on the overview's strip, and this is the list they change.</p>
     */
    private void fillMemberBar(GUIAnchor box) {
        GUIHorizontalButtonTablePane buttons = new GUIHorizontalButtonTablePane(getState(), 3, 1, box);
        buttons.onInit();
        buttons.addButton(0, 0, Lng.str("INVITE PLAYER"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM,
                gated(() -> getOwnPlayer().getFactionController().hasInvitePermission(), () -> getState().getGlobalGameControlManager()
                        .getIngameControlManager().getPlayerGameControlManager().getFactionControlManager().openInvitePlayerDialog()),
                activeWhile(() -> getOwnPlayer().getFactionController().hasInvitePermission()));
        buttons.addButton(1, 0, Lng.str("RANKS"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM,
                gated(() -> getOwnPlayer().getFactionController().hasPermissionEditPermission(),
                        () -> new FactionRankDialog(getState(), getOwnFaction()).activate()),
                activeWhile(() -> getOwnPlayer().getFactionController().hasPermissionEditPermission()));
        buttons.addButton(2, 0, new Object() {
                    @Override
                    public String toString() {
                        return Lng.str("INVITES SENT (%s)", invitesOut());
                    }
                }, GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM,
                gated(() -> invitesOut() > 0 && getOwnPlayer().getFactionController().hasInvitePermission(),
                        () -> new FactionOutgoingInvitesPlayerInputNew(getState()).activate()),
                activeWhile(() -> invitesOut() > 0 && getOwnPlayer().getFactionController().hasInvitePermission()));
        box.attach(buttons);
    }

    /** A table-pane button that greys out while the viewer may not use it. */
    private static GUIActivationCallback activeWhile(BooleanSupplier allowed) {
        return new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState state) {
                return true;
            }

            @Override
            public boolean isActive(InputState state) {
                return allowed.getAsBoolean();
            }
        };
    }

    /** A press that does nothing while greyed out. */
    private GUICallback gated(BooleanSupplier allowed, Runnable onPress) {
        return press(() -> {
            if (allowed.getAsBoolean()) {
                onPress.run();
            }
        });
    }

    /**
     * What the power figure is made of. Worth showing beside it: the number is on an arbitrary scale,
     * so on its own it says nothing a player can act on.
     */
    private static String powerBreakdown(Faction f) {
        long momentum = Math.round(FactionStrength.momentum(f) * 100);
        return Lng.str("(%s systems, %s active, %s/period, %s combat%s)",
                FactionStrength.claimedSystems(f),
                StringTools.formatPointZero(FactionStrength.activeMembers(f)),
                StringTools.formatPointZero(FactionStrength.pointSurplus(f)),
                Math.round(FactionStrength.combatRating(f)),
                momentum == 0 ? "" : Lng.str(", %s from recent battles", (momentum > 0 ? "+" : "") + momentum + "%"));
    }


    /**
     * What the faction is and how it behaves, sorted by what each row actually is.
     *
     * <p>The old OPTIONS tab was two grids of identical blue buttons holding four different kinds of
     * thing at once. Its membership actions are on the member list now and its inbox queues are on
     * the overview, both beside what they change; what remains is settings, and this is one list of
     * them rather than a grid.</p>
     */
    private void createSettingsPane() {
        int wasSelected = settingsSub == null ? 0 : settingsSub.getSelectedTab();
        GUITabbedContent sub = settingsSub = new GUITabbedContent(getState(), settingsTab.getContent(0));
        sub.activationInterface = this;
        sub.onInit();
        settingsTab.getContent(0).attach(sub);
        if (getOwnFaction() != null) {
            membersTab = sub.addTab(Lng.str("MEMBERS"));
            createMembersPane();
        }
        GUIContentPane settings = sub.addTab(Lng.str("FACTION"));
        FactionSettingsContent c = new FactionSettingsContent(getState(), this);
        c.onInit();
        settings.setContent(0, c);
        sub.setSelectedTab(Math.min(wasSelected, sub.getTabs().size() - 1));
	}

	public PlayerState getOwnPlayer() {
		return getState().getPlayer();
	}

	public Faction getOwnFaction() {
		return getState().getFactionManager().getFaction(getOwnPlayer().getFactionId());
	}






	public FactionRelation.RType getOwnRelationTo(Faction f) {
		return getState().getFactionManager().getRelation(getState().getPlayerName(), getState().getPlayer().getFactionId(), f.getIdFaction());
    }

    /**
     * The faction whose relations the diplomacy list is showing, or null for our own.
     */
    private Faction viewing() {
        return diplomacyFactionList == null ? null : diplomacyFactionList.viewing();
    }

    /**
     * The list switched whose relations it shows: the detail panel follows. A faction cannot have
     * relations with itself, so selecting the new viewpoint clears the selection.
     */
    public void onViewpointChanged() {
        Faction v = viewing();
        Faction f = selectedDiplomacyFaction;
        onSelectFaction(v != null && f != null && v.getIdFaction() == f.getIdFaction() ? null : f);
    }

    public void onSelectFaction(Faction f) {
        selectedDiplomacyFaction = f;
        Faction v = viewing();
        if (treatyList != null) {
            treatyList.setViewpoint(v);
            treatyList.setOther(f);
        }
        if (opinionList != null) {
            opinionList.setViewpoint(v);
            opinionList.setToFaction(f);
        }
        fillDiplomacyHeader(f);
        fillDiplomacyIdentity(f);
    }

    /**
     * Identity, what is waiting on an answer, and the event feed - the three things the panel gets
     * opened to check.
     *
     * <p>It replaces the NEWS and FACTION LOG tabs, which were the same feed at two scopes with the
     * faction's own identity bolted onto the header of one of them. The scope is a switch above the
     * list now rather than a tab of its own.</p>
     */
    private void createOverviewPane() {
        if (gList != null) {
            gList.cleanUp();
            gList = null;
        }
        if (nList != null) {
            nList.cleanUp();
            nList = null;
        }
        if (rList != null) {
            rList.cleanUp();
            rList = null;
        }

        Faction own = getOwnFaction();
        if (own == null) {
            //nothing to be identified as, and no log of our own to switch to: the galaxy feed is
            //the whole tab, which is exactly what the NEWS tab used to be
            overviewTab.setTextBoxHeightLast(28);
            attachGalaxyFeed(overviewTab.getContent(0));
            return;
        }

        overviewTab.addDivider(OVERVIEW_IDENTITY_WIDTH);

        overviewTab.setTextBoxHeightLast(0, OVERVIEW_VITALS_HEIGHT);
        overviewTab.addNewTextBox(0, 10);
        overviewTab.setTextBoxHeightFractionLast(0, DESCRIPTION_FRACTION);
        overviewTab.addNewTextBox(0, 10);
        fillVitals(overviewTab.getContent(0, 0), own);
        fillDescription(overviewTab.getContent(0, 1), own);
        fillModifiers(overviewTab.getContent(0, 2), own);

        //the feed and the battles it reports on, as two sub-tabs of the news column. The panel rebuilds
        //on a faction or scope change, so carry the selected sub-tab over from the old one
        int wasSelected = newsSub == null ? 0 : newsSub.getSelectedTab();
        GUITabbedContent sub = newsSub = new GUITabbedContent(getState(), overviewTab.getContent(1, 0));
        sub.activationInterface = this;
        sub.onInit();
        overviewTab.getContent(1, 0).attach(sub);

        GUIContentPane news = sub.addTab(Lng.str("NEWS"));
        news.setTextBoxHeightLast(BAR_HEIGHT);
        news.addNewTextBox(10);
        fillFeedBar(news.getContent(0));
        if (overviewGalaxy) {
            attachGalaxyFeed(news.getContent(1));
        } else {
            nList = new FactionNewsScrollableListNew(getState(), news.getContent(1), own);
            nList.onInit();
            news.getContent(1).attach(nList);
        }

        GUIContentPane reports = sub.addTab(Lng.str("BATTLE REPORTS"));
        rList = new BattleReportList(getState(), reports.getContent(0));
        rList.onInit();
        reports.getContent(0).attach(rList);
        sub.setSelectedTab(wasSelected);
    }

    /**
     * What the faction owns. What a station makes, holds and is short of needs that station loaded,
     * so it is not here - see {@link FactionAssetScrollableListNew}.
     */
    private void createAssetsPane() {
        if (aList != null) {
            aList.cleanUp();
            aList = null;
        }
        if (dList != null) {
            dList.cleanUp();
            dList = null;
        }
        Faction own = getOwnFaction();
        if (own == null) {
            return;
        }

        //holdings, and the blueprint each ship is repaired to, as two sub-tabs
        int wasSelected = assetsSub == null ? 0 : assetsSub.getSelectedTab();
        GUITabbedContent sub = assetsSub = new GUITabbedContent(getState(), assetsTab.getContent(0));
        sub.activationInterface = this;
        sub.onInit();
        assetsTab.getContent(0).attach(sub);

        GUIContentPane holdings = sub.addTab(Lng.str("HOLDINGS"));
        aList = new FactionAssetScrollableListNew(getState(), holdings.getContent(0), own);
        aList.onInit();
        holdings.getContent(0).attach(aList);

        GUIContentPane designs = sub.addTab(Lng.str("DESIGNS"));
        dList = new FactionDesignsList(getState(), designs.getContent(0));
        dList.onInit();
        designs.getContent(0).attach(dList);
        sub.setSelectedTab(wasSelected);
    }

    private void attachGalaxyFeed(GUIAnchor content) {
        gList = new GUINPCFactionNewsScrollableList(getState(), content, panel);
        gList.onInit();
        content.attach(gList);
    }

    /**
     * The faction's name, followed by its federation's name in brackets when it has one.
     */
    private static String nameWithFederation(Faction f) {
        Federation fed = DiplomacyManager.client().getFederationOf(f.getIdFaction());
        return fed == null ? f.getName() : f.getName() + " [" + fed.name + "]";
    }

    /**
     * The part of the card that does not wrap: who we are, how big, and what that size is made of.
     */
    private void fillVitals(GUIAnchor box, Faction f) {
        GUITextOverlay name = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium20(), getState());
        name.setTextSimple(new Object() {
            @Override
            public String toString() {
                return nameWithFederation(f);
            }
        });
        name.setPos(4, 4, 0);

        GUITextOverlay size = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        size.setTextSimple(new Object() {
            @Override
            public String toString() {
                return Lng.str("%s members, %s systems, %s", f.getMembersUID().size(), FactionStrength.claimedSystems(f),
                        f.getHomebaseUID().isEmpty()
                                ? Lng.str("no home")
                                : Lng.str("home %s", f.getHomeSector().toStringPure()));
            }
        });
        size.setColor(0.76f, 0.76f, 0.76f, 1.0f);
        size.setPos(4, 30, 0);

        GUITextOverlay power = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium17(), getState());
        power.setTextSimple(new Object() {
            @Override
            public String toString() {
                return Lng.str("Power %s", Math.round(FactionStrength.of(f)));
            }
        });
        power.setPos(4, 52, 0);

        GUITextOverlay points = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        points.setTextSimple(new Object() {
            @Override
            public String toString() {
                return Lng.str("Faction points %s", (int) f.factionPoints);
            }
        });
        points.setPos(4, 148, 0);

        //green or red rather than parenthesised, because whether a faction is gaining points or
        //bleeding them is the part of that row anybody actually reads
        GUITextOverlay surplus = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState()) {
            @Override
            public void draw() {
                if (FactionStrength.pointSurplus(f) < 0) {
                    setColor(0.90f, 0.25f, 0.21f, 1.0f);
                } else {
                    setColor(0.22f, 0.71f, 0.00f, 1.0f);
                }
                super.draw();
            }
        };
        surplus.setTextSimple(new Object() {
            @Override
            public String toString() {
                return Lng.str("%s per period", StringTools.formatPointZero(FactionStrength.pointSurplus(f)));
            }
        });
        surplus.setPos(150, 148, 0);

        //the only thing the old options grid held that has nowhere else to go: the breakdown of
        //where the points come from and what territory costs. It belongs beside the number it explains
        GUITextButton pointsDetail = new GUITextButton(getState(), 62, 18, GUITextButton.ColorPalette.OK,
                Lng.str("DETAIL"), press(() -> new PlayerFactionPointDialogNew(getState()).activate()));
        pointsDetail.setPos(OVERVIEW_IDENTITY_WIDTH - 78, 148, 0);

        box.attach(name);
        if (f == getOwnFaction()) {
            box.attach(pointsDetail);
        }
        box.attach(size);
        box.attach(power);
        box.attach(powerBar(box, f));
        key(box, 86, TERRITORY_COLOR, new Object() {
            @Override
            public String toString() {
                return Lng.str("%s systems claimed", FactionStrength.claimedSystems(f));
            }
        });
        key(box, 102, PEOPLE_COLOR, new Object() {
            @Override
            public String toString() {
                return Lng.str("%s active members", StringTools.formatPointZero(FactionStrength.activeMembers(f)));
            }
        });
        key(box, 118, INCOME_COLOR, new Object() {
            @Override
            public String toString() {
                return Lng.str("%s surplus per period", StringTools.formatPointZero(FactionStrength.pointSurplus(f)));
            }
        });
        key(box, 134, MILITARY_COLOR, new Object() {
            @Override
            public String toString() {
                return Lng.str("%s combat rating (ships and stations)", Math.round(FactionStrength.combatRating(f)));
            }
        });
        box.attach(points);
        box.attach(surplus);
    }

    /**
     * One line of the power key: the bar's colour, then what that segment is made of. Without it the
     * bar is three anonymous stripes and the numbers below it are three anonymous numbers.
     */
    private void key(GUIAnchor box, int y, Vector4f color, Object text) {
        GUIColoredRectangle swatch = new GUIColoredRectangle(getState(), 7, 7, color);
        swatch.setPos(5, y + 4, 0);
        GUITextOverlay t = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        t.setTextSimple(text);
        t.setColor(0.76f, 0.76f, 0.76f, 1.0f);
        t.setPos(18, y, 0);
        box.attach(swatch);
        box.attach(t);
    }


    /**
     * The four terms of the power number, to scale.
     *
     * <p>Worth drawing because the number itself is on an arbitrary scale - 340 means nothing on its
     * own - while the shares of it are the part a player can do something about.</p>
     */
    private GUIAnchor powerBar(GUIAnchor box, Faction f) {
        GUIColoredRectangle territory = segment(TERRITORY_COLOR);
        GUIColoredRectangle people = segment(PEOPLE_COLOR);
        GUIColoredRectangle income = segment(INCOME_COLOR);
        GUIColoredRectangle military = segment(MILITARY_COLOR);

        GUIAnchor bar = new GUIAnchor(getState(), 10, POWER_BAR_HEIGHT) {
            @Override
            public void draw() {
                setWidth(Math.max(0, box.getWidth() - 8));
                double t = FactionStrength.territoryTerm(f);
                double p = FactionStrength.peopleTerm(f);
                double i = FactionStrength.incomeTerm(f);
                double m = FactionStrength.militaryTerm(f);
                double total = t + p + i + m;
                //a faction worth nothing at all still gets a bar, so the row does not blink out
                float x = lay(territory, 0, total > 0 ? (float) (t / total) : 1.0f);
                x = lay(people, x, total > 0 ? (float) (p / total) : 0.0f);
                x = lay(income, x, total > 0 ? (float) (i / total) : 0.0f);
                lay(military, x, total > 0 ? (float) (m / total) : 0.0f);
                super.draw();
            }

            private float lay(GUIColoredRectangle r, float x, float share) {
                float w = share * super.getWidth();
                r.setWidth(Math.max(0, w - POWER_BAR_GAP));
                r.setPos(x, 0, 0);
                return x + w;
            }
        };
        bar.setPos(4, 74, 0);
        bar.attach(territory);
        bar.attach(people);
        bar.attach(income);
        bar.attach(military);
        return bar;
    }

    private GUIColoredRectangle segment(Vector4f color) {
        return new GUIColoredRectangle(getState(), 1, POWER_BAR_HEIGHT, new Vector4f(color));
    }

    /**
     * The one part of the card that is prose, so it takes whatever room is left and scrolls when
     * there is more of it than that.
     */
    /**
     * The government in bold, then the description as wrapped prose. Two overlays because one
     * cannot mix fonts; they scroll together.
     */
    private void fillDescription(GUIAnchor box, Faction f) {
        GUITextOverlay government = new GUITextOverlay(10, 10, FontLibrary.getBlenderProHeavy30(), getState());
        government.setTextSimple(new Object() {
            @Override
            public String toString() {
                String g = Civics.client().of(f.getIdFaction()).government();
                return g == null ? "" : g;
            }
        });

        GUITextOverlayTable t = new GUITextOverlayTable(2, 2, getState());
        t.autoHeight = true;
        t.autoWrapOn = box;
        t.setTextSimple(new Object() {
            @Override
            public String toString() {
                String d = f.getDescription();
                return d == null || d.trim().isEmpty() ? Lng.str("No description.") : d.trim();
            }
        });
        t.onInit();

        //centred above the text, scrolling with it; no room is kept for it until the image is here
        GUIFactionEmblem emblem = new GUIFactionEmblem(getState(), DESCRIPTION_EMBLEM_SIZE, () -> f);

        GUIAnchor content = new GUIAnchor(getState(), 10, 10) {
            @Override
            public void draw() {
                int top = 2;
                if (FactionEmblemCache.get((GameClientState) getState(), f) != null) {
                    emblem.setPos((box.getWidth() - DESCRIPTION_EMBLEM_SIZE) / 2, top, 0);
                    top += DESCRIPTION_EMBLEM_SIZE + 6;
                }
                government.setPos(2, top, 0);
                if (Civics.client().of(f.getIdFaction()).government() != null) {
                    top += 28;
                }
                t.setPos(0, top, 0);
                setWidth(box.getWidth());
                setHeight(top + t.getHeight());
                super.draw();
            }
        };
        content.attach(emblem);
        content.attach(government);
        content.attach(t);
        GUIScrollablePanel sc = new GUIScrollablePanel(10, 10, box, getState());
        sc.setContent(content);
        sc.onInit();
        box.attach(sc);
    }

    /**
     * Prose filling a content box: wrapped to its width and scrolled when there is more of it than
     * the box is tall.
     */
    private void wrappedText(GUIAnchor box, Object text) {
        GUIScrollablePanel sc = new GUIScrollablePanel(10, 10, box, getState());
        GUITextOverlayTable t = new GUITextOverlayTable(2, 2, getState());
        t.autoHeight = true;
        t.autoWrapOn = box;
        t.setTextSimple(text);
        t.onInit();
        sc.setContent(t);
        sc.onInit();
        box.attach(sc);
    }



    /**
     * The bloc screen. A member sees their bloc and its members on the left, its rules and open votes
     * on the right. A faction in no bloc sees every bloc on the server instead - which is where an
     * invitation is answered and a new one founded - with the one clicked beside it.
     *
     * <p>Its own tab rather than a corner of the diplomacy one because a federation is a different
     * subject. Diplomacy is a list of pairs and what each pair agreed; a bloc has members, a chair,
     * a charter and decisions in front of it, and none of those fit in a row about two factions.</p>
     */
    public void createFederationPane() {
        if (federationList != null) {
            federationList.cleanUp();
            federationList = null;
        }
        if (federationMemberList != null) {
            federationMemberList.cleanUp();
        }

        builtInFederation = ownFederation() != null;
        federationTab.addDivider(FEDERATION_LEFT_WIDTH);

        if (builtInFederation) {
            federationTab.setTextBoxHeightLast(0, FEDERATION_CARD_HEIGHT);
            federationTab.addNewTextBox(0, BAR_HEIGHT);
            federationTab.setTextBoxHeightLast(0, BAR_HEIGHT);
            federationTab.addNewTextBox(0, 10);
            wrappedText(federationTab.getContent(0, 0), new Object() {
                @Override
                public String toString() {
                    return federationProse();
                }
            });
            fillMembershipBar(federationTab.getContent(0, 1));
            attachFederationMembers(federationTab.getContent(0, 2));

            federationTab.setTextBoxHeightLast(1, FEDERATION_GOVERNANCE_HEIGHT);
            federationTab.addNewTextBox(1, BAR_HEIGHT);
            federationTab.setTextBoxHeightLast(1, BAR_HEIGHT);
            federationTab.addNewTextBox(1, 10);
            FederationGovernance governance = new FederationGovernance(getState(), this::shownFederation);
            governance.onInit();
            federationTab.setContent(1, 0, governance);
            fillDecisionBar(federationTab.getContent(1, 1));
            FederationVoteList votes = new FederationVoteList(getState(), this::shownFederation);
            votes.onInit();
            federationTab.setContent(1, 2, votes);
        } else {
            federationList = new FederationList(getState(), federationTab.getContent(0, 0), this);
            federationList.onInit();
            federationTab.getContent(0, 0).attach(federationList);

            federationTab.setTextBoxHeightLast(1, FEDERATION_CARD_HEIGHT);
            federationTab.addNewTextBox(1, BAR_HEIGHT);
            federationTab.setTextBoxHeightLast(1, BAR_HEIGHT);
            federationTab.addNewTextBox(1, 10);
            wrappedText(federationTab.getContent(1, 0), new Object() {
                @Override
                public String toString() {
                    return federationProse();
                }
            });
            fillFoundingBar(federationTab.getContent(1, 1));
            attachFederationMembers(federationTab.getContent(1, 2));
        }

        //the two layouts are different panes, so joining or leaving rebuilds the tab rather than
        //leaving a member looking at the browse screen
        federationTab.getContent(0, 0).attach(new GUIAnchor(getState(), 0, 0) {
            @Override
            public void draw() {
                if ((ownFederation() != null) != builtInFederation) {
                    requestRebuild();
                }
            }
        });
    }

    private void attachFederationMembers(GUIAnchor box) {
        federationMemberList = new FederationMemberList(getState(), box, this::shownFederation);
        federationMemberList.onInit();
        box.attach(federationMemberList);
    }

    private Federation ownFederation() {
        return DiplomacyManager.client().getFederationOf(getOwnPlayer().getFactionId());
    }

    /**
     * Which bloc the screen is about: your own, else the one clicked, else nothing.
     */
    private Federation shownFederation() {
        Federation own = ownFederation();
        if (own != null) {
            return own;
        }
        if (selectedFederation != null) {
            Federation live = DiplomacyManager.client().getFederation(selectedFederation.id);
            if (live != null) {
                return live;
            }
            selectedFederation = null;
        }
        return null;
    }

    public void onSelectFederation(Federation f) {
        selectedFederation = f;
    }

    private String federationProse() {
        Federation f = shownFederation();
        if (f == null) {
            return Lng.str("Your faction is in no federation.") + BREAK + BREAK
                    + Lng.str("A bloc of factions rather than an agreement between two. Members are allied to "
                    + "each other for as long as they are in it, and leaving is always free.");
        }
        StringBuilder b = new StringBuilder();
        b.append(f.name).append(BREAK);
        Faction chair = getState().getFactionManager().getFaction(f.leader);
        if (f.founded > 0) {
            b.append(Lng.str("Founded %s", new java.text.SimpleDateFormat("MMM d, yyyy").format(new java.util.Date(f.founded))))
                    .append("  ");
        }
        b.append(Lng.str("%s members, chaired by %s", f.members.size(),
                chair != null ? chair.getName() : String.valueOf(f.leader)));
        if (f.rotationDays() > 0) {
            b.append("  ").append(Lng.str("(passes on in %s)", DiplomacyTreatyList.roughly(
                    f.chairSince + f.rotationDays() * Federation.DAY_MS - System.currentTimeMillis())));
        }
        b.append(BREAK);
        if (f.charter != null && !f.charter.trim().isEmpty()) {
            b.append(f.charter.trim());
        }
        return b.toString();
    }

    private boolean leadsShown() {
        Federation f = shownFederation();
        return f != null && f.leader == getOwnPlayer().getFactionId() && maySpeak();
    }

    /**
     * Whether this player's rank speaks for their faction in a bloc. Not a gate - the server checks
     * the same thing - just the difference between drawing a button and not.
     */
    private boolean maySpeak() {
        return Perms.mine(Perms.FEDERATION_VOTE);
    }

    /**
     * The chair may act on this alone, and with the charter flag set any member may put it to a vote.
     */
    private boolean mayRaise(long voteFlag) {
        Federation f = ownFederation();
        return f != null && maySpeak() && (f.leader == getOwnPlayer().getFactionId() || f.hasCharterFlag(voteFlag));
    }

    /**
     * Founding a bloc is the one thing to do on the browse screen; answering an invite is on the row
     * of the bloc that sent it.
     */
    private void fillFoundingBar(GUIAnchor box) {
        buttonRow(box, new BarButton(Lng.str("FOUND"), () -> maySpeak() && getOwnFaction() != null,
                () -> federationText(Lng.str("Name the federation"), "",
                        name -> send(new FederationCommand(FederationCommand.Kind.CREATE, 0).text(name)))));
    }

    /**
     * Who is in the bloc: asking somebody in, and walking out.
     */
    private void fillMembershipBar(GUIAnchor box) {
        buttonRow(box,
                new BarButton(voteOr(Federation.CHARTER_VOTE_ADMISSION, Lng.str("VOTE TO ADMIT"), Lng.str("INVITE")),
                        () -> mayRaise(Federation.CHARTER_VOTE_ADMISSION),
                        () -> pickFaction(Lng.str("Faction to invite"), FederationCommand.Kind.INVITE,
                                f -> DiplomacyManager.client().getFederationOf(f.getIdFaction()) == null)),
                new BarButton(Lng.str("LEAVE"), this::maySpeak,
                        () -> confirm(getState(), Lng.str("Leave %s", shownFederationName()),
                                Lng.str("Really leave the federation?\nYour alliance with every member ends."),
                                () -> sendToShown(new FederationCommand(FederationCommand.Kind.LEAVE, 0)))));
    }

    /**
     * What the bloc does as one: war, its name and charter, and winding it up.
     */
    private void fillDecisionBar(GUIAnchor box) {
        //peace and lift only while there is a war or an embargo to end; when that changes the bar is
        //rebuilt, since a table pane keeps a column for a hidden button
        boolean war = anyAtWar();
        boolean embargo = anyEmbargo();
        box.attach(new GUIAnchor(getState(), 0, 0) {
            @Override
            public void draw() {
                if (anyAtWar() != war || anyEmbargo() != embargo) {
                    requestRebuild();
                }
            }
        });
        java.util.List<BarButton> row = new java.util.ArrayList<>();
        row.add(new BarButton(voteOr(Federation.CHARTER_VOTE_WAR, Lng.str("VOTE ON WAR"), Lng.str("DECLARE WAR")),
                        () -> mayRaise(Federation.CHARTER_VOTE_WAR),
                        () -> pickFaction(Lng.str("Faction to go to war with"), FederationCommand.Kind.DECLARE_WAR,
                                f -> atWarWith(f) < ownFederation().members.size())));
        if (war) {
            row.add(new BarButton(voteOr(Federation.CHARTER_VOTE_WAR, Lng.str("VOTE ON PEACE"), Lng.str("MAKE PEACE")),
                    () -> mayRaise(Federation.CHARTER_VOTE_WAR),
                    () -> pickFaction(Lng.str("Faction to offer peace"), FederationCommand.Kind.MAKE_PEACE,
                            f -> atWarWith(f) > 0)));
        }
        row.add(new BarButton(voteOr(Federation.CHARTER_VOTE_WAR, Lng.str("VOTE ON EMBARGO"), Lng.str("EMBARGO")),
                        () -> mayRaise(Federation.CHARTER_VOTE_WAR),
                        () -> pickFaction(Lng.str("Faction to close trade with"), FederationCommand.Kind.EMBARGO,
                                f -> embargoing(f) < ownFederation().members.size())));
        if (embargo) {
            row.add(new BarButton(voteOr(Federation.CHARTER_VOTE_WAR, Lng.str("VOTE TO LIFT"), Lng.str("LIFT EMBARGO")),
                    () -> mayRaise(Federation.CHARTER_VOTE_WAR),
                    () -> pickFaction(Lng.str("Faction to reopen trade with"), FederationCommand.Kind.LIFT_EMBARGO,
                            f -> embargoing(f) > 0)));
        }
        row.add(new BarButton(Lng.str("CHARTER"), () -> mayRaise(Federation.CHARTER_VOTE_CHARTER), this::editCharter));
        row.add(new BarButton(Lng.str("RENAME"), this::leadsShown,
                        () -> federationText(Lng.str("Rename the federation"), shownFederationName(),
                                name -> sendToShown(new FederationCommand(FederationCommand.Kind.RENAME, 0).text(name)))));
        row.add(new BarButton(Lng.str("DISSOLVE"), this::leadsShown,
                        () -> confirm(getState(), Lng.str("Dissolve %s", shownFederationName()),
                                Lng.str("Really dissolve the federation?\nEvery member's alliance with the others ends."),
                                () -> sendToShown(new FederationCommand(FederationCommand.Kind.DISSOLVE, 0)))));
        buttonRow(box, row.toArray(new BarButton[0]));
    }

    private record BarButton(Object label, BooleanSupplier allowed, Runnable onPress) {
    }

    /** A bar of buttons sharing its width, each greyed out while it may not be used. */
    private void buttonRow(GUIAnchor box, BarButton... row) {
        GUIHorizontalButtonTablePane buttons = new GUIHorizontalButtonTablePane(getState(), row.length, 1, box);
        buttons.onInit();
        for (int i = 0; i < row.length; i++) {
            BarButton b = row[i];
            buttons.addButton(i, 0, b.label(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM,
                    gated(b.allowed(), b.onPress()), activeWhile(b.allowed()));
        }
        box.attach(buttons);
    }

    /**
     * A caption that says whether pressing it decides or asks, read per frame since the charter can
     * change under it.
     */
    private Object voteOr(long voteFlag, String byVote, String alone) {
        return new Object() {
            @Override
            public String toString() {
                Federation f = ownFederation();
                return f != null && f.hasCharterFlag(voteFlag) ? byVote : alone;
            }
        };
    }

    private String shownFederationName() {
        Federation f = shownFederation();
        return f != null ? f.name : "";
    }

    /**
     * How many members of your own bloc are at war with {@code f}.
     */
    private int atWarWith(Faction f) {
        Federation own = ownFederation();
        int n = 0;
        if (own != null) {
            for (int i = 0; i < own.members.size(); i++) {
                if (DiplomacyManager.client().hasTreaty(own.members.getInt(i), f.getIdFaction(), TreatyTypeRegistry.WAR)) {
                    n++;
                }
            }
        }
        return n;
    }

    /** How many members hold an embargo of their own on {@code f}. */
    private int embargoing(Faction f) {
        Federation own = ownFederation();
        int n = 0;
        if (own != null) {
            for (int i = 0; i < own.members.size(); i++) {
                int m = own.members.getInt(i);
                videogoose.betterfactions.diplomacy.FactionTreaty e = DiplomacyManager.client().getTreaties(m, f.getIdFaction()).get(TreatyTypeRegistry.EMBARGO);
                if (e != null && e.from == m) {
                    n++;
                }
            }
        }
        return n;
    }

    private boolean anyEmbargo() {
        for (Faction f : getState().getFactionManager().getFactionCollection()) {
            if (embargoing(f) > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean anyAtWar() {
        for (Faction f : getState().getFactionManager().getFactionCollection()) {
            if (atWarWith(f) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * A dropdown of the factions the command can sensibly name: never a member, never one the
     * filter rules out. The server checks again; this only keeps the obvious refusals off the list.
     */
    private void pickFaction(String title, FederationCommand.Kind kind, java.util.function.Predicate<Faction> fits) {
        Federation own = ownFederation();
        java.util.List<Faction> factions = new ObjectArrayList<>(getState().getFactionManager().getFactionCollection());
        factions.sort(java.util.Comparator.comparing(Faction::getName, String.CASE_INSENSITIVE_ORDER));
        java.util.List<GUIElement> items = new ObjectArrayList<>();
        for (Faction f : factions) {
            if (!DiplomacyFactionList.isParty(f) || own == null || own.isMember(f.getIdFaction()) || !fits.test(f)) {
                continue;
            }
            GUITextOverlay name = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium16(), getState());
            name.setTextSimple(nameWithFederation(f));
            name.setPos(5, 4, 0);
            GUIAnchor a = new GUIAnchor(getState(), 400, 24);
            a.attach(name);
            a.setUserPointer(f.getIdFaction());
            items.add(a);
        }
        if (items.isEmpty()) {
            getState().getController().popupAlertTextMessage(Lng.str("No faction to choose."), 0);
            return;
        }
        new PlayerGameDropDownInput("FEDERATION_FACTION", getState(), 440, 300, title, 24, "", items) {
            @Override
            public void onDeactivate() {
            }

            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void pressedOK(GUIListElement current) {
                if (current != null && current.getContent() != null) {
                    sendToShown(new FederationCommand(kind, 0).target((Integer) current.getContent().getUserPointer()));
                }
                deactivate();
            }
        }.activate();
    }

    /**
     * Asks before an act that cannot be taken back. Shared with the member list's expel and chair
     * buttons.
     */
    static void confirm(GameClientState state, String title, String question, Runnable onOk) {
        new PlayerOkCancelInput("BF_FEDERATION_CONFIRM", state, 440, 200, title, question) {
            @Override
            public void pressedOK() {
                onOk.run();
                deactivate();
            }

            @Override
            public void onDeactivate() {
            }
        }.activate();
    }

    private void editCharter() {
        Federation f = shownFederation();
        if (f == null) {
            return;
        }
        federationText(Lng.str("What the federation is for"), f.charter, text ->
                sendToShown(new FederationCommand(FederationCommand.Kind.SET_CHARTER, 0)
                        .text(text).value(f.charterFlags)));
    }

    private void send(FederationCommand c) {
        DiplomacyManager.client().sendFederationCommand(c);
    }

    private void sendToShown(FederationCommand c) {
        Federation f = shownFederation();
        if (f != null) {
            c.federationId = f.id;
            send(c);
        }
    }

    private void federationText(String title, String initial, java.util.function.Consumer<String> onInput) {
        PlayerTextInput t = new PlayerTextInput("FEDERATION_TEXT", getState(), 100, title, "",
                initial == null ? "" : initial) {
            @Override
            public String[] getCommandPrefixes() {
                return null;
            }

            @Override
            public String handleAutoComplete(String s, TextCallback callback, String prefix)
                    throws PrefixNotFoundException {
                return null;
            }

            @Override
            public void onFailedTextCheck(String msg) {
            }

            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void onDeactivate() {
            }

            @Override
            public boolean onInput(String entry) {
                onInput.accept(entry == null ? "" : entry);
                return true;
            }
        };
        t.activate();
    }

    /**
     * Asks the engine's panel to rebuild its tabs before it next draws, which fires the hook that
     * builds these again. The engine keeps the flag private and checks it in its own draw - the only
     * point where swapping tabs out cannot pull them from under something iterating them.
     */
    private void requestRebuild() {
        try {
            java.lang.reflect.Field flag = FactionPanelNew.class.getDeclaredField("flagFactionTabRecreate");
            flag.setAccessible(true);
            flag.setBoolean(panel, true);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    /**
     * The engine's news dialog builds its body as a five-line text bar but never sets a wrap width,
     * so everything runs off the right edge on one line. The bar is private, hence the reflection.
     */
    private void openNewsPost() {
        PlayerCreateFactionNewsInputNew input = new PlayerCreateFactionNewsInputNew(getState());
        try {
            java.lang.reflect.Field body = GUICreateNewsPostPanel.class.getDeclaredField("messageTextBar");
            body.setAccessible(true);
            //characters per line, not pixels: roughly what fits the 500px dialog at MEDIUM
            ((GUIActivatableTextBar) body.get(input.getInputPanel())).setLineWrap(NEWS_WRAP_CHARS);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        input.activate();
    }

    /**
     * One row above the feed: which log you are reading on the left, and on the right only the
     * things actually waiting for an answer.
     *
     * <p>An empty queue is not drawn at all, which is the whole reason these moved off the options
     * grid. There they were permanent buttons carrying their own counts in the caption, so "nothing
     * to do" and "three people want to join" looked exactly alike.</p>
     */
    private void fillFeedBar(GUIAnchor box) {
        ObjectArrayList<Chip> chips = new ObjectArrayList<Chip>();

        chips.add(chip(CHIP_WIDTH, new Object() {
            @Override
            public String toString() {
                return Lng.str("INVITES (%s)", invitesIn());
            }
        }, () -> invitesIn() > 0, () -> new FactionIncomingInvitesPlayerInputNew(getState()).activate()));

        chips.add(chip(CHIP_WIDTH, new Object() {
            @Override
            public String toString() {
                return Lng.str("OFFERS (%s)", offersIn());
            }
                }, () -> offersIn() > 0 && getOwnPlayer().getFactionController().hasRelationshipPermission(),
                () -> new ProposalInboxDialog(getState()).activate()));

        //invites this faction has sent are waiting on somebody else, so they are not on this bar.
        //They belong beside the member list, which is the thing they change.
        chips.add(chip(CHIP_WIDTH, Lng.str("POST NEWS"),
                () -> !overviewGalaxy && getOwnPlayer().getFactionController().hasDescriptionAndNewsPostPermission(),
                this::openNewsPost));

        GUITextButton ours = new GUITextButton(getState(), SCOPE_WIDTH, CHIP_HEIGHT,
                overviewGalaxy ? GUITextButton.ColorPalette.TRANSPARENT : GUITextButton.ColorPalette.OK, Lng.str("OUR LOG"), scopeTo(false));
        GUITextButton galaxy = new GUITextButton(getState(), SCOPE_WIDTH, CHIP_HEIGHT,
                overviewGalaxy ? GUITextButton.ColorPalette.OK : GUITextButton.ColorPalette.TRANSPARENT, Lng.str("GALAXY"), scopeTo(true));
        ours.setPos(2, 2, 0);
        galaxy.setPos(2 + SCOPE_WIDTH + 2, 2, 0);

        actionBar(box, new GUIElement[]{ours, galaxy}, chips);
    }

    /**
     * A row above a list: fixed buttons packed from the left, and on the right only the chips with
     * something to report. Laid out at draw time rather than once, because which chips are present
     * changes between frames and the box's width follows the window.
     */
    private GUIAnchor actionBar(GUIAnchor box, GUIElement[] left,
                                ObjectArrayList<Chip> chips) {
        GUIAnchor bar = new GUIAnchor(getState(), 10, BAR_HEIGHT) {
            @Override
            public void draw() {
                setWidth(box.getWidth());
                float x = 4;
                for (GUIElement e : left) {
                    e.setPos(x, 4, 0);
                    x += e.getWidth() + 4;
                }
                x = super.getWidth() - 4;
                for (int i = chips.size() - 1; i >= 0; i--) {
                    Chip c = chips.get(i);
                    if (!c.shown().getAsBoolean()) {
                        continue;
                    }
                    x -= c.button().getWidth();
                    c.button().setPos(x, 4, 0);
                    x -= 4;
                }
                super.draw();
            }
        };
        for (GUIElement e : left) {
            bar.attach(e);
        }
        for (Chip c : chips) {
            bar.attach(c.button());
        }
        box.attach(bar);
        return bar;
    }

    private GUICallback press(Runnable onPress) {
        return new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse()) {
                    onPress.run();
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        };
    }

    private int invitesIn() {
        return getOwnPlayer().getFactionController().getInvitesIncoming().size();
    }

    private int invitesOut() {
        return getOwnPlayer().getFactionController().getInvitesOutgoing().size();
    }

    private int offersIn() {
        return DiplomacyManager.client().getOffersTo(getOwnPlayer().getFactionId()).size();
    }

    /**
     * A button on the overview bar, with the reason it might not be there at all. Not drawing it is
     * also what makes it unclickable - the same way row buttons elsewhere in this panel hide
     * themselves from a viewer who may not use them.
     */
    private record Chip(GUITextButton button, BooleanSupplier shown) {
    }

    private Chip chip(int width, Object text, BooleanSupplier shown, Runnable onPress) {
        GUITextButton b = new GUITextButton(getState(), width, CHIP_HEIGHT, GUITextButton.ColorPalette.OK, text,
                new GUICallback() {
                    @Override
                    public void callback(GUIElement callingGuiElement, MouseEvent event) {
                        if (event.pressedLeftMouse()) {
                            onPress.run();
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return !shown.getAsBoolean();
                    }
                }) {
            @Override
            public void draw() {
                if (shown.getAsBoolean()) {
                    super.draw();
                }
            }
        };
        return new Chip(b, shown);
    }

    /**
     * Switching feed rebuilds the tabs, which is the path a faction change already takes and which
     * keeps the selected tab by name. Cheaper than teaching a content pane to swap one list for
     * another, and it happens on a click rather than per frame.
     */
    private GUICallback scopeTo(boolean galaxy) {
        return new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse() && overviewGalaxy != galaxy) {
                    overviewGalaxy = galaxy;
                    requestRebuild();
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        };
    }

    /**
     * Who they are, what they think of you, and how they came to think it - the one part of the tab
     * that is prose rather than a table.
     */
    private void fillDiplomacyHeader(Faction f) {
        if (diplomacyHeader == null) {
            return;
        }
        diplomacyHeader.getChilds().clear();
        Faction viewing = viewing();
        if (f == null && viewing == null) {
            return;
        }
        GUIScrollablePanel sc = new GUIScrollablePanel(10, 10, diplomacyHeader, getState());
        GUITextOverlayTable t = new GUITextOverlayTable(2, 2, getState());
        t.autoHeight = true;
		t.autoWrapOn = diplomacyHeader;
        t.setTextSimple(new Object() {
            @Override
            public String toString() {
                StringBuilder b = new StringBuilder();
                if (viewing != null) {
                    b.append(Lng.str("Showing %s's relations. Right-click a faction to go back to yours.", viewing.getName()));
                    if (f == null) {
                        return b.toString();
                    }
                    b.append(BREAK);
                    switch (getState().getFactionManager().getRelation(viewing.getIdFaction(), f.getIdFaction())) {
                        case ENEMY:
                            b.append(Lng.str("At war with %s.", viewing.getName()));
                            break;
                        case FRIEND:
                            b.append(Lng.str("Allied to %s.", viewing.getName()));
                            break;
                        default:
                            b.append(Lng.str("Neutral toward %s.", viewing.getName()));
                            break;
                    }
                    DiplomacyEntity e = DiplomacyManager.client().diplomacyOf(f).entities.get((long) viewing.getIdFaction());
                    b.append("  ").append(Lng.str("Opinion of %s: %s", viewing.getName(), e != null ? e.getPoints() : 0)).append(BREAK);
                    b.append(Lng.str("Power: %s vs %s's %s   %s", Math.round(FactionStrength.of(f)), viewing.getName(),
                            Math.round(FactionStrength.of(viewing)), powerBreakdown(f)));
                } else {
                    switch (getOwnRelationTo(f)) {
                        case ENEMY:
                            b.append(Lng.str("At war with you."));
                            break;
                        case FRIEND:
                            b.append(Lng.str("Allied to you."));
                            break;
                        default:
                            b.append(Lng.str("Neutral toward you."));
                            break;
                    }
                    b.append("  ").append(opinionLine(f)).append(BREAK);
                    b.append(powerLine(f));
                }
                String rep = reputationLine(f);
                if (rep != null) {
                    b.append(BREAK).append(rep);
                }
                return b.toString();
            }
        });
        t.onInit();
        sc.setContent(t);
        sc.onInit();
        diplomacyHeader.attach(sc);
    }

    /**
     * Who the selected faction is, in the left column under the list: the same vitals and prose the
     * overview shows for our own faction, so a neighbour reads the way we read ourselves.
     */
    private void fillDiplomacyIdentity(Faction f) {
        if (diplomacyVitals == null) {
            return;
        }
        diplomacyVitals.getChilds().clear();
        diplomacyAbout.getChilds().clear();
        diplomacyModifiers.getChilds().clear();
        if (f == null) {
            wrappedText(diplomacyAbout, Lng.str("Select a faction to see who they are."));
            return;
        }
        fillVitals(diplomacyVitals, f);
        fillDescription(diplomacyAbout, f);
        fillModifiers(diplomacyModifiers, f);
    }

    /** What the faction's civics, doctrine and institutions do, kept current as they change. */
    private void fillModifiers(GUIAnchor box, Faction f) {
        wrappedText(box, new Object() {
            @Override
            public String toString() {
                FactionCivics c = Civics.client().of(f.getIdFaction());
                String m = c.modifiers(Civics.isResource(c.resource) ? ElementKeyMap.getInfo(c.resource).getName() : null);
                return Lng.str("MODIFIERS") + BREAK + (m.isEmpty() ? Lng.str("None.") : m);
            }
        });
    }

    /**
     * How big they are, and how that compares with you - which is the question the number exists to
     * answer, so showing it without the comparison would make a reader do arithmetic.
     */
    private String powerLine(Faction f) {
        long theirs = Math.round(FactionStrength.of(f));
        Faction own = getOwnFaction();
        String breakdown = powerBreakdown(f);
        if (own == null || own.getIdFaction() == f.getIdFaction()) {
            return Lng.str("Power: %s   %s", theirs, breakdown);
        }
        return Lng.str("Power: %s vs your %s   %s", theirs, Math.round(FactionStrength.of(own)), breakdown);
    }

    /**
     * What the rest of the galaxy has heard about them, or null for a clean record.
     */
    private static String reputationLine(Faction f) {
        Reputation r = DiplomacyManager.client().getReputation();
        double infamy = r.infamyOf(f.getIdFaction());
        double aggression = r.aggressionOf(f.getIdFaction());
        if (infamy < 0.05 && aggression < 0.05) {
            return null;
        }
        return Lng.str("Reputation: %s broken treaties, %s fights started (fading)",
                String.format(java.util.Locale.ENGLISH, "%.1f", infamy), String.format(java.util.Locale.ENGLISH, "%.1f", aggression));
    }

    /**
     * Both opinion scores on one line; the breakdown below says where they came from.
     */
    private String opinionLine(Faction f) {
        DiplomacyEntity faction = DiplomacyManager.client().diplomacyOf(f).entities.get(getOwnPlayer().getFactionId());
        DiplomacyEntity personal = DiplomacyManager.client().diplomacyOf(f).entities.get(getOwnPlayer().getDbId());
        int factionPoints = faction != null ? faction.getPoints() : 0;
        int personalPoints = personal != null ? personal.getPoints() : 0;
        if (getOwnPlayer().getFactionId() == 0) {
            return Lng.str("Opinion of you: %s", personalPoints);
        }
        return Lng.str("Opinion of your faction: %s / of you: %s", factionPoints, personalPoints);
	}
}
