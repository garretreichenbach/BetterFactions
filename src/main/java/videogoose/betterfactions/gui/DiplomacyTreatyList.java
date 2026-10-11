package videogoose.betterfactions.gui;

import videogoose.betterfactions.Perms;
import videogoose.betterfactions.DiplomacyManager;
import org.schema.common.util.StringTools;
import org.schema.game.client.controller.PlayerOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import videogoose.betterfactions.diplomacy.DiplomacyProposal;
import videogoose.betterfactions.diplomacy.FactionTreaty;
import videogoose.betterfactions.diplomacy.TreatyType;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.GUITextButton.ColorPalette;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

/**
 * The agreements in force between the viewer's faction and whichever one is selected.
 *
 * <p>Nothing rendered treaties before this: they have been syncing to the client since the storage
 * phase, and the only way to find out you held one was that turrets stopped shooting.</p>
 *
 * <p>Expanding a row shows the clause the proposer attached and offers to walk away from it. Break
 * is deliberately behind the expand rather than sitting in the row: the list is read far more often
 * than it is acted on, and a one-click way to end an alliance next to the mouse's resting position
 * is a way to end an alliance by accident.</p>
 */
public class DiplomacyTreatyList extends ScrollableTableList<FactionTreaty> {

    private final GUIElement parent;
    private Faction other;

    public DiplomacyTreatyList(InputState state, GUIElement p) {
        super(state, 100, 100, p);
        parent = p;
        ((GameClientState) state).getFactionManager().addObserver(this);
    }

    private static String name(FactionTreaty t) {
        return t.getDisplayName();
    }

    /**
     * The kind of agreement, which the row no longer says on its own once it carries a name.
     */
    private static String typeName(FactionTreaty t) {
        TreatyType type = t.getType();
        return type != null ? type.getDisplayName() : t.typeId;
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        ((GameClientState) getState()).getFactionManager().deleteObserver(this);
    }

    /**
     * Points the list at another faction; null clears it.
     */
    public void setOther(Faction other) {
        this.other = other;
        flagDirty();
    }

    private GameClientState client() {
        return (GameClientState) getState();
    }

    private int ownFactionId() {
        return client().getPlayer().getFactionId();
    }

    /**
     * Whether the viewer's role lets them end an agreement of this type.
     */
    private boolean mayBreak(FactionTreaty t) {
        Faction own = client().getFaction();
        if (own == null) {
            return false;
        }
        FactionPermission p = own.getMembersUID().get(client().getPlayerName());
        if (p == null) {
            return false;
        }
        //mirrors the server's gate: hostility needs the relations permission, everything else also
        //accepts the treaty permission. The server is still the gate; this only hides a button that
        //would be refused.
        TreatyType type = t.getType();
        boolean hostility = type == null || type.getImplies() == org.schema.game.common.data.player.faction.FactionRelation.RType.ENEMY;
        return p.hasRelationshipPermission(own) || (!hostility && Perms.has(own, p, Perms.TREATIES));
    }

    @Override
    public void initColumns() {
        //"Standing" rather than "Treaty": a war sits in this list too, and it is not an agreement
        addColumn(Lng.str("Standing"), 3.0f, new Comparator<FactionTreaty>() {
            @Override
            public int compare(FactionTreaty o1, FactionTreaty o2) {
                return name(o1).compareToIgnoreCase(name(o2));
            }
        });
        addFixedWidthColumn(Lng.str("Held For"), 120, new Comparator<FactionTreaty>() {
            @Override
            public int compare(FactionTreaty o1, FactionTreaty o2) {
                return Long.compare(o2.signedAt, o1.signedAt);
            }
        });
        addFixedWidthColumn(Lng.str("Term"), 120, new Comparator<FactionTreaty>() {
            @Override
            public int compare(FactionTreaty o1, FactionTreaty o2) {
                return Long.compare(o1.expiresAt, o2.expiresAt);
            }
        });

        //the two things you can actually do on this screen live on the list's own button row rather
        //than in a box of their own, which is what leaves the opinion list its height
        addButton(new GUICallback() {
            @Override
            public boolean isOccluded() {
                return !isActive();
            }

            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (!event.pressedLeftMouse() || other == null) {
                    return;
                }
                Faction own = client().getFaction();
                if (!DiplomacyProposeDialog.mayPropose(client(), own)) {
                    return;
                }
                new DiplomacyProposeDialog(client(), own, other).activate();
            }
        }, new Object() {
            @Override
            public String toString() {
                return Lng.str("ACTIONS");
            }
        }, GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, ControllerElement.FilterRowStyle.LEFT, ControllerElement.FilterPos.BOTTOM);

        addButton(new GUICallback() {
            @Override
            public boolean isOccluded() {
                return !isActive();
            }

            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse()) {
                    new ProposalInboxDialog(client()).activate();
                }
            }
        }, new Object() {
            @Override
            public String toString() {
                //the count is the point: the inbox used to be two clicks deep in OPTIONS with nothing
                //anywhere saying an offer was waiting
                return Lng.str("INBOX (%s)", ProposalInboxPanel.pendingCount(client()));
            }
        }, GUIHorizontalArea.HButtonType.BUTTON_GREY_MEDIUM, ControllerElement.FilterRowStyle.RIGHT, ControllerElement.FilterPos.BOTTOM);

        activeSortColumnIndex = 0;
    }

    /**
     * A span to the hour. How long a pact has stood is read in days; a figure that ticks every
     * second only says the screen is alive. Also used for how long a federation vote has left.
     */
    public static String roughly(long ms) {
        long hours = Math.max(0, ms) / (60L * 60L * 1000L);
        if (hours < 1) {
            return Lng.str("under an hour");
        }
        if (hours < 48) {
            return hours == 1 ? Lng.str("1 hour") : Lng.str("%s hours", hours);
        }
        return Lng.str("%s days", hours / 24);
    }

    @Override
    protected Collection<FactionTreaty> getElementList() {
        int own = ownFactionId();
        if (other == null || own == 0) {
            return Collections.emptyList();
        }
        return DiplomacyManager.client().getTreaties(own, other.getIdFaction()).getTreaties();
    }

    @Override
    public void updateListEntries(GUIElementList mainList, Set<FactionTreaty> collection) {
        mainList.deleteObservers();
        mainList.addObserver(this);
        for (FactionTreaty t : collection) {
            GUITextOverlayTable nameText = new GUITextOverlayTable(10, 10, getState());
            GUITextOverlayTable heldText = new GUITextOverlayTable(10, 10, getState());
            GUITextOverlayTable termText = new GUITextOverlayTable(10, 10, getState());

            nameText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return name(t);
                }
            });
            heldText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return StringTools.formatTimeFromMS(Math.max(0, System.currentTimeMillis() - t.signedAt));
                }
            });
            termText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    if (t.expiresAt == FactionTreaty.NO_EXPIRY) {
                        return Lng.str("No term");
                    }
                    return StringTools.formatTimeFromMS(Math.max(0, t.expiresAt - System.currentTimeMillis()));
                }
            });

            nameText.getPos().y = 5;
            heldText.getPos().y = 5;
            termText.getPos().y = 5;

            GUIClippedRow nameRow = new GUIClippedRow(getState());
            nameRow.attach(nameText);
            GUIClippedRow heldRow = new GUIClippedRow(getState());
            heldRow.attach(heldText);
            GUIClippedRow termRow = new GUIClippedRow(getState());
            termRow.attach(termText);

            TreatyRow r = new TreatyRow(getState(), t, nameRow, heldRow, termRow);
            GUIAnchor expandedContent = expandedFor(t);
            r.expanded = new GUIElementList(getState());
            r.expanded.add(new GUIListElement(expandedContent, expandedContent, getState()));
            r.onInit();
            mainList.addWithoutUpdate(r);
        }
        mainList.updateDim();
    }

    /**
     * The clause the proposer wrote, plus the way out.
     */
    private GUIAnchor expandedFor(FactionTreaty t) {
        GUIAnchor c = new GUIAnchor(getState(), 100, 59) {
            @Override
            public void draw() {
                setWidth(parent.getWidth());
                super.draw();
            }
        };
        GUIScrollablePanel scroll = new GUIScrollablePanel(80, 30, c, getState());
        GUITextOverlayTableInnerDescription clause = new GUITextOverlayTableInnerDescription(10, 10, getState());
        clause.autoWrapOn = c;
        clause.setTextSimple(new Object() {
            @Override
            public String toString() {
                String s = t.clause == null ? "" : t.clause.trim();
                return s.isEmpty()
                        ? Lng.str("%s. No clause attached.", typeName(t))
                        : Lng.str("%s. %s", typeName(t), s);
            }
        });
        clause.setPos(4, 2, 0);
        scroll.setContent(clause);

        GUITextButton breakButton = new GUITextButton(getState(), 120, 24, ColorPalette.CANCEL, Lng.str("BREAK"),
                new GUICallback() {
                    @Override
                    public boolean isOccluded() {
                        return !isActive();
                    }

                    @Override
                    public void callback(GUIElement callingGuiElement, MouseEvent event) {
                        if (!event.pressedLeftMouse()) {
                            return;
                        }
                        PlayerOkCancelInput confirm = new PlayerOkCancelInput("DIPLOMACY_BREAK", getState(), 440, 230,
                                Lng.str("Break %s", name(t)),
                                Lng.str("Really end this agreement with %s?\nThey will be told, and it will cost you standing.",
                                        other != null ? other.getName() : "")) {
                            @Override
                            public void pressedOK() {
                                DiplomacyManager.client().sendTreatyProposal(client().getPlayerName(),
                                        ownFactionId(), t.other(ownFactionId()), t.typeId, "",
                                        DiplomacyProposal.Action.REVOKE);
                                deactivate();
                            }

                            @Override
                            public void onDeactivate() {
                            }
                        };
                        confirm.activate();
                    }
                }) {
            @Override
            public void draw() {
                TreatyType type = t.getType();
                if (type != null && type.isBreakable() && mayBreak(t)) {
                    super.draw();
                }
            }
        };

        GUITextButton renameButton = new GUITextButton(getState(), 120, 24, ColorPalette.OK, Lng.str("RENAME"),
                new GUICallback() {
                    @Override
                    public boolean isOccluded() {
                        return !isActive();
                    }

                    @Override
                    public void callback(GUIElement callingGuiElement, MouseEvent event) {
                        if (!event.pressedLeftMouse() || other == null) {
                            return;
                        }
                        Faction own = client().getFaction();
                        if (own == null) {
                            return;
                        }
                        //a rename is the same agreement offered again under a new name, so it is the
                        //propose dialog and the other side has to agree to it
                        DiplomacyProposeDialog.rename(client(), own, other, t).activate();
                    }
                }) {
            @Override
            public void draw() {
                if (DiplomacyProposeDialog.mayPropose(client(), client().getFaction())) {
                    super.draw();
                }
            }
        };

        c.attach(scroll);
        c.attach(breakButton);
        c.attach(renameButton);
        breakButton.setPos(4, 30, 0);
        renameButton.setPos(130, 30, 0);
        return c;
    }

    private class TreatyRow extends ScrollableTableList.Row {
        public TreatyRow(InputState state, FactionTreaty t, GUIElement... elements) {
            super(state, t, elements);
            highlightSelect = true;
        }
    }
}
