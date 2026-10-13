package videogoose.betterfactions.gui;

import videogoose.betterfactions.DiplomacyManager;

import org.schema.common.util.StringTools;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.diplomacy.DiplomacyProposal;
import videogoose.betterfactions.diplomacy.SettlementTerm;
import videogoose.betterfactions.diplomacy.TreatyType;
import videogoose.betterfactions.diplomacy.TreatyTypeRegistry;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.input.InputState;

import java.util.Observable;
import java.util.Observer;

/**
 * Offers waiting on an answer.
 *
 * <p>Used to render every proposal as one of three fixed captions - war declaration, alliance
 * proposal, peace offer - because that was all a relation byte could say. A proposal now names a
 * treaty type and a term, so the row says what is actually being offered, and a modded treaty type
 * shows up here under its own name with no change.</p>
 *
 * <p>Counter is the third button because without it {@code Action.COUNTER} is unreachable: the
 * protocol has supported answering with different terms since the proposal pipeline was unified,
 * and nothing in the UI could produce one.</p>
 */
public class ProposalInboxPanel extends GUIInputPanel implements Observer {

    private static final int ROW_HEIGHT = 124;
    /** Extra height per settlement term, one line each. */
    private static final int TERM_LINE = 16;
    /**
     * Narrowest a row is allowed to get, whatever the player does to the window.
     */
    private static final int MIN_ROW_WIDTH = 420;
    private static final int BUTTON_WIDTH = 80;
    /**
     * Kept clear on the right of a row for the vertical scrollbar, which appears as soon as there is
     * more than one offer waiting.
     */
    private static final int SCROLL_GUTTER = 18;

	private GUIScrollablePanel scrollPanel;
	private GUIElementList list;
	private boolean needsUpdate = true;

	public ProposalInboxPanel(InputState state, GUICallback guiCallback) {
        //the id carries the size: a window remembers what it was last resized to against its own
        //id, and that saved size beats whatever is passed here
        super("ProposalInboxPanel_V2", state, 620, 320, guiCallback, Lng.str("Incoming Offers"), "");

		DiplomacyManager.client().deleteObserver(this);
		DiplomacyManager.client().addObserver(this);
	}

    /**
     * How many offers are waiting, for the button that opens this.
     */
    public static int pendingCount(GameClientState state) {
        return DiplomacyManager.client().getOffersTo(state.getPlayer().getFactionId()).size();
    }

    /**
     * What an offer is called, whether or not it names a treaty type.
     */
    public static String titleOf(DiplomacyProposal offer) {
        //the name the proposer chose is the headline, because agreeing to the offer is agreeing to
        //the name as well - it is what both sides will call this afterwards
        if (offer.name != null && !offer.name.isEmpty()) {
            return offer.name;
        }
        //a deal's treaties carry their own names; the first one it signs heads it
        for (SettlementTerm t : offer.terms) {
            if (t.kind == SettlementTerm.Kind.SIGN_TREATY && !t.name.isEmpty()) {
                return t.name;
            }
        }
        return typeOf(offer);
    }

    /**
     * What kind of agreement is being offered, which the title stops saying once it carries a name.
     */
    public static String typeOf(DiplomacyProposal offer) {
        if (offer.against != 0) {
            return Lng.str("Call to Arms");
        }
        if (offer.typeId == null) {
            if (!DiplomacyManager.client().isSettlement(offer)) {
                return Lng.str("Peace Offer");
            }
            //a deal goes by the first treaty it signs
            String signed = DiplomacyManager.firstSigned(offer);
            TreatyType type = signed != null ? TreatyTypeRegistry.get(signed) : null;
            return type != null ? type.getDisplayName() : Lng.str("Settlement");
        }
        TreatyType type = TreatyTypeRegistry.get(offer.typeId);
        return type != null ? type.getDisplayName() : offer.typeId;
    }

	@Override
	public void draw() {
        super.draw();
		if (needsUpdate) {
			updateList();
			needsUpdate = false;
		}
	}

	@Override
	public void onInit() {
        super.onInit();

        //follows the content box rather than a fixed size: the panel is resizable, and a row drawn
        //at a size the window no longer is reads as a broken background
        scrollPanel = new GUIScrollablePanel(10, 10, getContent(), getState());
		list = new GUIElementList(getState());
		scrollPanel.setContent(list);
		getContent().attach(scrollPanel);
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		needsUpdate = true;
	}

	private void updateList() {
		list.clear();
        for (DiplomacyProposal f : DiplomacyManager.client().getOffersTo(((GameClientState) getState()).getPlayer().getFactionId())) {
            FactionOfferListElement e = new FactionOfferListElement(getState(), f);
            e.onInit();
			list.add(e);
		}
	}

	private class FactionOfferListElement extends GUIListElement {
        private final GUIAnchor row;
        private final DiplomacyProposal offer;
        /**
         * What the offer's text wraps against. Sized at draw time from whatever the buttons leave.
         */
        private GUIAnchor textColumn;
        /**
         * Accept, counter and decline, kept on the right edge as the window is resized. Null until
         * {@link #onInit()} has run, which is one frame after the row's background starts drawing.
         */
        private GUIElement[] buttons;

        public FactionOfferListElement(InputState state, DiplomacyProposal offer) {
			super(state);
            this.offer = offer;
            //no fill of its own: the panel behind it is already a readable surface, and a banded row
            //only earned its keep back when this was a table of one-line entries
            row = new GUIAnchor(getState(), MIN_ROW_WIDTH, rowHeight()) {
                @Override
                public void draw() {
                    layout(this);
                    super.draw();
                }
            };
            setContent(row);
            setSelectContent(row);
        }

        /**
         * Width follows the panel; height does not, because rows stack.
         */
        private void layout(GUIAnchor row) {
            row.setWidth(Math.max(MIN_ROW_WIDTH, scrollPanel.getWidth() - SCROLL_GUTTER));
            if (buttons == null) {
                return;
            }
            float x = row.getWidth() - BUTTON_WIDTH - 10;
            for (int i = 0; i < buttons.length; i++) {
                buttons[i].setPos(x, 10 + i * 26, 0);
            }
            textColumn.setWidth(Math.max(120, x - 14));
        }

        private int rowHeight() {
            return ROW_HEIGHT + TERM_LINE * offer.terms.size();
        }

        private GameClientState client() {
            return (GameClientState) getState();
		}

		@Override
		public void onInit() {
            super.onInit();

            Faction from = client().getFactionManager().getFaction(offer.a);
            Faction to = client().getFactionManager().getFaction(offer.b);
            if (from == null || to == null) {
                System.err.println("[GUI][FACTION] offer with an unknown party: " + offer.a + " / " + offer.b);
                return;
            }

            //an unattached anchor purely to wrap against: the text column is narrower than the row,
            //and wrapping on the row itself would run the clause under the buttons
            textColumn = new GUIAnchor(getState(), MIN_ROW_WIDTH - BUTTON_WIDTH - 24, rowHeight());
            GUITextOverlay info = new GUITextOverlay(MIN_ROW_WIDTH, rowHeight() - 8, getState());
            info.autoWrapOn = textColumn;
            info.setTextSimple(new Object() {
                @Override
                public String toString() {
                    StringBuilder b = new StringBuilder();
                    b.append(titleOf(offer).toUpperCase(java.util.Locale.ENGLISH)).append("\n");
                    b.append(Lng.str("From: [%s] %s  (%s)", from.getName(), offer.getInitiator(), typeOf(offer))).append("\n");
                    Faction enemy = offer.against != 0 ? client().getFactionManager().getFaction(offer.against) : null;
                    if (offer.against != 0) {
                        b.append(Lng.str("Against: %s", enemy != null ? enemy.getName() : String.valueOf(offer.against))).append("\n");
                    }
                    b.append(offer.against != 0
                            ? Lng.str("Answer within: %s", StringTools.formatTimeFromMS(
                            Math.max(0, offer.expiresAt - System.currentTimeMillis())))
                            : offer.expiresAt > 0
                            ? Lng.str("Term: %s", StringTools.formatTimeFromMS(
                            Math.max(0, offer.expiresAt - System.currentTimeMillis())))
                            //a deal's treaties each say their own term, among the terms below
                            : "");
                    if (offer.typeId == null && !DiplomacyManager.client().warGoals(offer.a, offer.b).isEmpty()) {
                        b.append("\n").append(offer.terms.equals(DiplomacyManager.client().warGoals(offer.a, offer.b))
                                ? Lng.str("Exactly the war goals.")
                                : Lng.str("Differs from the war goals."));
                    }
                    for (SettlementTerm t : offer.terms) {
                        b.append("\n- ").append(t.describe(id -> {
                            Faction f = client().getFactionManager().getFaction(id);
                            return f != null ? f.getName() : String.valueOf(id);
                        }, offer.a, offer.b));
                    }
                    String note = offer.getMessage() == null ? "" : offer.getMessage().trim();
                    if (!note.isEmpty()) {
                        b.append("\n").append(note);
                    }
                    return b.toString();
                }
            });
            info.setPos(6, 4, 0);

            GUITextButton accept = new GUITextButton(getState(), 80, 18, GUITextButton.ColorPalette.OK, Lng.str("Accept"),
                    new GUICallback() {
                        @Override
                        public void callback(GUIElement callingGuiElement, MouseEvent event) {
                            if (event.pressedLeftMouse()) {
                                DiplomacyManager.client().sendRelationshipAccept(client().getPlayerName(), offer, true);
                            }
                        }

                        @Override
                        public boolean isOccluded() {
                            return false;
                        }
                    });

            GUITextButton counter = new GUITextButton(getState(), 80, 18, GUITextButton.ColorPalette.NEUTRAL, Lng.str("Counter"),
                    new GUICallback() {
                        @Override
                        public void callback(GUIElement callingGuiElement, MouseEvent event) {
                            if (event.pressedLeftMouse()) {
                                //the answering side is the offer's target, so it proposes back the other way
                                new DiplomacyProposeDialog(client(), to, from, offer).activate();
                            }
                        }

                        @Override
                        public boolean isOccluded() {
                            return false;
                        }
                    });

            GUITextButton decline = new GUITextButton(getState(), 80, 18, GUITextButton.ColorPalette.CANCEL, Lng.str("Decline"),
                    new GUICallback() {
                        @Override
                        public void callback(GUIElement callingGuiElement, MouseEvent event) {
                            if (event.pressedLeftMouse()) {
                                DiplomacyManager.client().sendRelationshipAccept(client().getPlayerName(), offer, false);
                            }
                        }

                        @Override
                        public boolean isOccluded() {
                            return false;
                        }
                    });

            //a call to arms is answered yes or no; there is nothing to haggle over
            buttons = offer.against != 0 ? new GUIElement[]{accept, decline} : new GUIElement[]{accept, counter, decline};
            layout(row);

            row.attach(info);
            for (GUIElement button : buttons) {
                row.attach(button);
            }
		}
	}
}
