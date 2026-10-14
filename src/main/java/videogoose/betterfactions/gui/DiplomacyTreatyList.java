package videogoose.betterfactions.gui;

import videogoose.betterfactions.DiplomacyManager;
import org.schema.common.util.StringTools;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import videogoose.betterfactions.diplomacy.FactionTreaty;
import videogoose.betterfactions.diplomacy.SettlementTerm;
import videogoose.betterfactions.diplomacy.TreatyType;
import videogoose.betterfactions.diplomacy.TreatyTypeRegistry;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import javax.vecmath.Vector4f;
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
    /**
     * Whose side the list is seen from: null for the viewer's own faction. Another faction's treaties
     * are shown read-only.
     */
    private Faction viewpoint;

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

    public void setViewpoint(Faction viewpoint) {
        this.viewpoint = viewpoint;
        flagDirty();
    }

    private GameClientState client() {
        return (GameClientState) getState();
    }

    private int ownFactionId() {
        return client().getPlayer().getFactionId();
    }

    /**
     * The faction on "our" side of the list: the viewpoint, else the viewer's own.
     */
    private int sideId() {
        return viewpoint != null ? viewpoint.getIdFaction() : ownFactionId();
    }

    /**
     * Whether the viewer's role lets them end an agreement of this type.
     */
    private boolean mayBreak(FactionTreaty t) {
        return DiplomacyActionsDialog.mayBreak(client(), client().getFaction(), t);
    }

    @Override
    public void initColumns() {
        //"Standing" rather than "Treaty": a war sits in this list too, and it is not an agreement
        addColumn(Lng.str("Standing"), 3.0f, (o1, o2) -> name(o1).compareToIgnoreCase(name(o2)));
        addFixedWidthColumn(Lng.str("Held For"), 120, (o1, o2) -> Long.compare(o2.signedAt, o1.signedAt));
        addFixedWidthColumn(Lng.str("Term"), 120, Comparator.comparingLong(o -> o.expiresAt));

        //what you can do here lives on the list's own button rows rather than in a box of their own,
        //which is what leaves the opinion list its height: proposals and actions, then inbox and history
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
                //everything here is offered and answered; what is done unasked is under ACTIONS
                return Lng.str("PROPOSALS");
            }
        }, GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, ControllerElement.FilterRowStyle.LEFT, ControllerElement.FilterPos.BOTTOM);

        addButton(new GUICallback() {
            @Override
            public boolean isOccluded() {
                return !isActive();
            }

            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse() && other != null && client().getFaction() != null) {
                    new DiplomacyActionsDialog(client(), client().getFaction(), other).activate();
                }
            }
        }, Lng.str("ACTIONS"), GUIHorizontalArea.HButtonType.BUTTON_RED_MEDIUM, ControllerElement.FilterRowStyle.RIGHT, ControllerElement.FilterPos.BOTTOM);

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
        }, GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, ControllerElement.FilterRowStyle.LEFT, ControllerElement.FilterPos.BOTTOM);

        addButton(new GUICallback() {
            @Override
            public boolean isOccluded() {
                return !isActive();
            }

            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse() && other != null && sideId() != 0) {
                    new DiplomacyHistoryDialog(client(), sideId(), other).activate();
                }
            }
        }, Lng.str("HISTORY"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, ControllerElement.FilterRowStyle.RIGHT, ControllerElement.FilterPos.BOTTOM);

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
        int own = sideId();
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
                    //whole hours only: a ticking seconds count is noise for something held for days
                    long held = Math.max(0, System.currentTimeMillis() - t.signedAt);
                    long hour = 3_600_000L;
                    return held < hour
                            ? Lng.str("Under an hour")
                            : StringTools.formatTimeFromMS(held - held % hour);
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
        boolean war = TreatyTypeRegistry.WAR.equals(t.typeId);
        //wars get room for a second line of clause above the progress bar, which sits under it
        int textH = war ? 46 : 30;
        int buttonsY = war ? textH + PROGRESS_HEIGHT : textH;
        GUIAnchor c = new GUIAnchor(getState(), 100, buttonsY + 29) {
            @Override
            public void draw() {
                setWidth(parent.getWidth() - 28);
                super.draw();
            }
        };
        GUIScrollablePanel scroll = new GUIScrollablePanel(80, textH, c, getState());
        GUITextOverlayTableInnerDescription clause = new GUITextOverlayTableInnerDescription(10, 10, getState());
        clause.autoWrapOn = c;
        clause.setTextSimple(new Object() {
            @Override
            public String toString() {
                String s = t.clause == null ? "" : t.clause.trim();
                String text = s.isEmpty()
                        ? Lng.str("%s. No clause attached.", typeName(t))
                        : Lng.str("%s. %s", typeName(t), s);
                if (!t.goals.isEmpty()) {
                    text += "\n" + Lng.str("War goals:") + "\n" + SettlementTerm.describeAll(t.goals, id -> {
                        Faction f = ((GameClientState) getState()).getFactionManager().getFaction(id);
                        return f != null ? f.getName() : String.valueOf(id);
                    }, t.from, t.to, "");
                }
                return text;
            }
        });
        clause.setPos(4, 2, 0);
        scroll.setContent(clause);

        //view battles only on a war; the others grey out while they do not apply
        GUIHorizontalButtonTablePane buttons = new GUIHorizontalButtonTablePane(getState(), war ? 3 : 2, 1, c);
        buttons.onInit();
        buttons.setPos(0, buttonsY, 0);
        buttons.addButton(0, 0, Lng.str("BREAK"), GUIHorizontalArea.HButtonType.BUTTON_RED_MEDIUM,
                press(() -> showBreak(t), () -> DiplomacyActionsDialog.confirmBreak(client(), client().getFaction(), other, t)),
                activeWhile(() -> showBreak(t)));
        buttons.addButton(1, 0, Lng.str("RENAME"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM,
                press(() -> showRename(t) && other != null && client().getFaction() != null,
                        //a rename is the same agreement offered again under a new name, so it is the
                        //propose dialog and the other side has to agree to it
                        () -> DiplomacyProposeDialog.rename(client(), client().getFaction(), other, t).activate()),
                activeWhile(() -> showRename(t)));
        if (war) {
            buttons.addButton(2, 0, Lng.str("VIEW BATTLES"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM,
                    press(() -> true, () -> new WarBattlesDialog(client(), t).activate()), null);
        }

        c.attach(scroll);
        if (war) {
            c.attach(warProgress(t, c, textH));
        }
        c.attach(buttons);
        return c;
    }

    private GUICallback press(java.util.function.BooleanSupplier allowed, Runnable onPress) {
        return new GUICallback() {
            @Override
            public boolean isOccluded() {
                return !isActive();
            }

            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse() && allowed.getAsBoolean()) {
                    onPress.run();
                }
            }
        };
    }

    private static GUIActivationCallback activeWhile(java.util.function.BooleanSupplier allowed) {
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

    private static final int PROGRESS_HEIGHT = 22;
    private static final Vector4f OURS = new Vector4f(0.31f, 0.50f, 0.35f, 1.0f);
    private static final Vector4f THEIRS = new Vector4f(0.50f, 0.31f, 0.31f, 1.0f);

    /**
     * Who is winning, ours from the left: the server's {@link FactionTreaty#progress} - the battles won
     * and ships destroyed since the war began, and the systems it is being fought over. Read from the
     * treaty in force each frame, since a progress update replaces the treaty object.
     */
    private GUIAnchor warProgress(FactionTreaty t, GUIAnchor c, int y) {
        GUITextOverlayTable label = new GUITextOverlayTable(10, 10, getState());
        GUIColoredRectangle ours = new GUIColoredRectangle(getState(), 1, 7, new Vector4f(OURS));
        GUIColoredRectangle theirs = new GUIColoredRectangle(getState(), 1, 7, new Vector4f(THEIRS));
        double[] balance = {0.5};
        GUIAnchor box = new GUIAnchor(getState(), 10, PROGRESS_HEIGHT) {
            @Override
            public void draw() {
                FactionTreaty live = DiplomacyManager.client().getTreaties(t.from, t.to).get(t.typeId);
                FactionTreaty war = live != null ? live : t;
                balance[0] = war.from == sideId() ? war.progress : 1 - war.progress;
                setWidth(Math.max(10, c.getWidth() - 16));
                float barX = 110, barW = Math.max(0, getWidth() - barX - 50);
                float split = (float) balance[0] * barW;
                ours.setPos(barX, 7, 0);
                ours.setWidth(Math.max(0, split - 1));
                theirs.setPos(barX + split + 1, 7, 0);
                theirs.setWidth(Math.max(0, barW - split - 1));
                super.draw();
            }
        };
        label.setTextSimple(new Object() {
            @Override
            public String toString() {
                return Lng.str("War Progress");
            }
        });
        label.setPos(0, 3, 0);
        GUITextOverlayTable percent = new GUITextOverlayTable(10, 10, getState()) {
            @Override
            public void draw() {
                setPos(box.getWidth() - 42, 3, 0);
                super.draw();
            }
        };
        percent.setTextSimple(new Object() {
            @Override
            public String toString() {
                return Math.round(balance[0] * 100) + "%";
            }
        });
        box.attach(label);
        box.attach(ours);
        box.attach(theirs);
        box.attach(percent);
        box.setPos(4, y, 0);
        return box;
    }

    private boolean showBreak(FactionTreaty t) {
        TreatyType type = t.getType();
        return viewpoint == null && type != null && type.isBreakable() && mayBreak(t);
    }

    /**
     * Whether a rename - the agreement offered again under a new name - is something the viewer can
     * send and the server will take. A war or a federation is not offered at all (a war keeps the name
     * it was declared with), nothing but peace is offered at war, and a one-sided agreement re-offered
     * from the bound side would turn it around.
     */
    private boolean showRename(FactionTreaty t) {
        TreatyType type = t.getType();
        Faction own = client().getFaction();
        if (viewpoint != null || type == null || own == null || !type.needsAcceptance() || !DiplomacyProposeDialog.mayPropose(client(), own)) {
            return false;
        }
        if (!type.isSymmetric() && t.from != own.getIdFaction()) {
            return false;
        }
        return other == null || client().getFactionManager().getRelation(own.getIdFaction(), other.getIdFaction()) != FactionRelation.RType.ENEMY;
    }

    private class TreatyRow extends ScrollableTableList.Row {
        public TreatyRow(InputState state, FactionTreaty t, GUIElement... elements) {
            super(state, t, elements);
            highlightSelect = true;
        }
    }
}
