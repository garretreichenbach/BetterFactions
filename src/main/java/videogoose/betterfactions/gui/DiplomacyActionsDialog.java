package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.controller.PlayerOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUITextButton;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.Perms;
import videogoose.betterfactions.diplomacy.DiplomacyProposal;
import videogoose.betterfactions.diplomacy.FactionTreaty;
import videogoose.betterfactions.diplomacy.TreatyType;
import videogoose.betterfactions.diplomacy.TreatyTypeRegistry;

/**
 * What one faction can do to another on its own say-so: declare war, or walk out of a treaty. These
 * used to share a dropdown with proposals, but nobody is asked about them, so they live apart from the
 * Proposals dialog, where everything is something the other side agrees to.
 */
public class DiplomacyActionsDialog extends PlayerInput {

    private static final int WIDTH = 420;
    private static final int ROW = 30;

    private final GUIInputPanel panel;

    public DiplomacyActionsDialog(GameClientState state, Faction own, Faction other) {
        super(state);
        panel = new GUIInputPanel("BF_DIPLOMACY_ACTIONS", state, WIDTH, 360, this,
                Lng.str("Actions toward %s", other.getName()), "") {
            @Override
            public void onInit() {
                super.onInit();
                int y = 6;
                for (TreatyType t : DiplomacyProposeDialog.declarable(state, own, other)) {
                    if (t.isSanction()) {
                        //no goals to set, so no dialog: just whether you mean it
                        y = add(y, sanctionVerb(t), GUITextButton.ColorPalette.CANCEL,
                                () -> confirmSanction(state, own, other, t));
                        continue;
                    }
                    y = add(y, Lng.str("Declare %s", DiplomacyProposeDialog.displayName(own, other, t)),
                            GUITextButton.ColorPalette.CANCEL,
                            () -> DiplomacyProposeDialog.declare(state, own, other, t.getId()).activate());
                }
                for (FactionTreaty t : DiplomacyManager.client().getTreaties(own.getIdFaction(), other.getIdFaction()).getTreaties()) {
                    TreatyType type = t.getType();
                    if (type != null && type.isBreakable() && mayBreak(state, own, t)) {
                        y = add(y, type.isSanction() ? liftVerb(type) : Lng.str("Break %s", t.getDisplayName()),
                                type.isSanction() ? GUITextButton.ColorPalette.OK : GUITextButton.ColorPalette.NEUTRAL,
                                () -> confirmBreak(state, own, other, t));
                    }
                }
                if (y == 6) {
                    GUITextOverlay none = new GUITextOverlay(WIDTH - 40, 40, FontLibrary.FontSize.MEDIUM.getFont(), state);
                    none.setTextSimple(Lng.str("Nothing to declare or break with %s right now.", other.getName()));
                    none.setPos(6, y, 0);
                    getContent().attach(none);
                }
            }

            /** One button down the list; it closes this dialog and hands over to what it opens. */
            private int add(int y, String text, GUITextButton.ColorPalette palette, Runnable then) {
                GUITextButton b = new GUITextButton(state, WIDTH - 40, 24, palette, text, new GUICallback() {
                    @Override
                    public void callback(GUIElement callingGuiElement, MouseEvent event) {
                        if (event.pressedLeftMouse()) {
                            DiplomacyActionsDialog.this.deactivate();
                            then.run();
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return !DiplomacyActionsDialog.this.isActive();
                    }
                });
                b.setPos(6, y, 0);
                getContent().attach(b);
                return y + ROW;
            }
        };
        panel.setCancelButton(false);
    }

    /**
     * Whether the viewer's role lets them end this treaty. Mirrors the server's gate - hostility needs the
     * relations permission, anything else also takes the treaty permission - so it only hides what would
     * be refused; the server still decides.
     */
    public static boolean mayBreak(GameClientState state, Faction own, FactionTreaty t) {
        if (own == null) {
            return false;
        }
        FactionPermission p = own.getMembersUID().get(state.getPlayerName());
        if (p == null) {
            return false;
        }
        TreatyType type = t.getType();
        if (type != null && type.getKind() == TreatyType.Kind.DEMAND && t.to == own.getIdFaction()) {
            //the vassal or tributary is released, it does not walk out
            return false;
        }
        if (type != null && type.isSanction() && t.from != own.getIdFaction()) {
            //only the side that took it can lift it
            return false;
        }
        boolean hostility = type == null || type.getImplies() == FactionRelation.RType.ENEMY;
        return p.hasRelationshipPermission(own) || (!hostility && Perms.has(own, p, Perms.TREATIES));
    }

    /**
     * Asks before tearing up {@code t}, then does; refused outright while {@code own} is in debt. Lifting
     * your own embargo breaks nobody's word and costs nothing, so it is not asked about.
     */
    public static void confirmBreak(GameClientState state, Faction own, Faction other, FactionTreaty t) {
        if (DiplomacyProposeDialog.inDebt(own)) {
            state.getController().popupAlertTextMessage(Lng.str("%s is in debt (%s faction points) and cannot break a treaty until it is paid off.",
                    own.getName(), (int) own.factionPoints), 0);
            return;
        }
        if (t.getType() != null && t.getType().isSanction()) {
            DiplomacyManager.client().sendTreatyProposal(state.getPlayerName(), own.getIdFaction(),
                    t.other(own.getIdFaction()), t.typeId, "", DiplomacyProposal.Action.REVOKE);
            return;
        }
        String name = other != null ? other.getName() : "";
        int breakCost = videogoose.betterfactions.civics.Council.client().cost(own.getIdFaction(), videogoose.betterfactions.civics.Council.INTRIGUE, BFConfig.breakTreatyCost());
        new PlayerOkCancelInput("DIPLOMACY_BREAK", state, 440, 230, Lng.str("Break %s", t.getDisplayName()),
                breakCost > 0
                        ? Lng.str("Really end this agreement with %s?\nThey will be told, and it will cost you standing and %s faction points.",
                        name, breakCost)
                        : Lng.str("Really end this agreement with %s?\nThey will be told, and it will cost you standing.", name)) {
            @Override
            public void pressedOK() {
                DiplomacyManager.client().sendTreatyProposal(state.getPlayerName(), own.getIdFaction(),
                        t.other(own.getIdFaction()), t.typeId, "", DiplomacyProposal.Action.REVOKE);
                deactivate();
            }

            @Override
            public void onDeactivate() {
            }
        }.activate();
    }

    private static String sanctionVerb(TreatyType t) {
        return switch (t.getId()) {
            case TreatyTypeRegistry.EMBARGO -> Lng.str("Close Trade");
            case TreatyTypeRegistry.TARIFF -> Lng.str("Impose Tariffs");
            default -> Lng.str("Declare %s", t.getDisplayName());
        };
    }

    private static String liftVerb(TreatyType t) {
        return switch (t.getId()) {
            case TreatyTypeRegistry.EMBARGO -> Lng.str("Reopen Trade");
            case TreatyTypeRegistry.TARIFF -> Lng.str("Lift Tariffs");
            default -> Lng.str("Lift %s", t.getDisplayName());
        };
    }

    /** Takes a sanction against {@code other}, once asked. */
    private static void confirmSanction(GameClientState state, Faction own, Faction other, TreatyType t) {
        String what = switch (t.getId()) {
            case TreatyTypeRegistry.EMBARGO -> Lng.str("Close trade with %s?\nYour shops will refuse their members and anyone allied to them, theirs will refuse yours, no trade route will run between you, and any trade agreement ends. Factions that trade with them anyway will lose standing with you. They will take it badly. Only you can reopen it.",
                    other.getName());
            case TreatyTypeRegistry.TARIFF -> Lng.str("Put tariffs on %s?\nYour shops will charge their members %s%% more and pay them %s%% less. They will resent it. Only you can lift them, and they may ask you to as part of a deal.",
                    other.getName(), BFConfig.tariffPercent(), BFConfig.tariffPercent());
            default -> Lng.str("Declare %s on %s?", t.getDisplayName(), other.getName());
        };
        new PlayerOkCancelInput("DIPLOMACY_SANCTION", state, 440, 250, Lng.str("%s: %s", sanctionVerb(t), other.getName()), what) {
            @Override
            public void pressedOK() {
                DiplomacyManager.client().sendTreatyProposal(state.getPlayerName(), own.getIdFaction(), other.getIdFaction(),
                        t.getId(), "", DiplomacyProposal.Action.PROPOSE);
                deactivate();
            }

            @Override
            public void onDeactivate() {
            }
        }.activate();
    }

    @Override
    public GUIElement getInputPanel() {
        return panel;
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public void handleMouseEvent(MouseEvent e) {
    }
}
