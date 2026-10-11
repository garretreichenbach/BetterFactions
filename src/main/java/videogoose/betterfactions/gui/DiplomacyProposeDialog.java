package videogoose.betterfactions.gui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.input.KeyEventInterface;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.Perms;
import videogoose.betterfactions.diplomacy.*;

import java.util.List;
import java.util.Objects;

/**
 * Picks a treaty and a term, says whether it would be accepted, and puts it on the table.
 *
 * <p>Replaces {@code FactionRelationEditPanel}, which was a {@code switch} over the three-value
 * relation with six buttons at hardcoded pixel offsets. The list of things you can propose now comes
 * from {@link TreatyTypeRegistry}, so a treaty type registered by a mod appears here with no GUI
 * change at all.</p>
 *
 * <h2>Why the preview is a round trip</h2>
 * An NPC faction's acceptance rules live in its preset, and the preset never leaves the server -
 * {@code NPCFaction.getConfig()} is null on a client. Evaluating {@code wouldAccept} here would
 * silently answer from the default settings and confidently give the wrong answer, which is worse
 * than not answering, so the question goes to the server and the reply is rendered when it lands.
 */
public class DiplomacyProposeDialog extends PlayerInput {

    /**
     * The name and the note are read when OK is pressed and nothing reacts to them as they are typed,
     * so one shared do-nothing callback serves every field in the dialog.
     */
    private static final TextCallback TEXT = new TextCallback() {
        @Override
        public String[] getCommandPrefixes() {
            return null;
        }

        @Override
        public String handleAutoComplete(String s, TextCallback callback, String prefix) {
            return null;
        }

        @Override
        public void onFailedTextCheck(String msg) {
        }

        @Override
        public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {
        }

        @Override
        public void newLine() {
        }
    };

    /**
     * Terms a treaty can be offered for, in ms; 0 means it does not lapse.
     */
    private static final long[] TERMS = {
            0L,
            1000L * 60L * 60L,
            1000L * 60L * 60L * 24L,
            1000L * 60L * 60L * 24L * 7L,
            1000L * 60L * 60L * 24L * 30L,
    };

    private final Faction from;
    private final Faction to;
    private final ProposePanel panel;
    /**
     * The offer being answered, when this dialog is a counter rather than a fresh proposal. A counter
     * is both halves at once - it settles what it answers and asks for something else - so the only
     * thing that differs here is which send it ends in.
     */
    private final DiplomacyProposal counterTo;
    /**
     * The agreement being renamed, when that is what this dialog is for. A rename is a renegotiation:
     * the same type on the same term with a different name, which the other side has to agree to -
     * because a name both signatories can see is only worth having if neither can change it alone.
     * Signing a type already in force replaces it, so nothing else is needed to make it work.
     */
    private final FactionTreaty renaming;

    /**
     * Null means white peace: an agreement to stop, rather than an agreement to do something.
     */
    private String typeId;
    private long termMs;
    /**
     * What the last query asked about, so a stale answer is not rendered against a new selection.
     */
    private String askedFor;
    private List<SettlementTerm> askedTerms = List.of();
    private boolean asked;
    /**
     * What changes hands on signing, on top of the action. Empty is a plain offer - or white peace.
     */
    private final List<SettlementTerm> terms = new ObjectArrayList<SettlementTerm>();
    /**
     * The term the picker is on, waiting for Add.
     */
    private SettlementTerm picked;

    public DiplomacyProposeDialog(GameClientState state, Faction from, Faction to) {
        this(state, from, to, null);
    }

    /**
     * @param counterTo the offer being answered with different terms, or null for a fresh proposal
     */
    public DiplomacyProposeDialog(GameClientState state, Faction from, Faction to, DiplomacyProposal counterTo) {
        this(state, from, to, counterTo, null);
    }

    private DiplomacyProposeDialog(GameClientState state, Faction from, Faction to,
                                   DiplomacyProposal counterTo, FactionTreaty renaming) {
        super(state);
        this.from = from;
        this.to = to;
        this.counterTo = counterTo;
        this.renaming = renaming;
        if (renaming != null) {
            typeId = renaming.typeId;
        }
        if (counterTo != null) {
            //start from what is on the table, so a counter can be "the same, minus that one term"
            terms.addAll(counterTo.terms);
        } else {
            //peace is offered first when at war, and it starts from what the war was declared for:
            //sending it unchanged is surrender, editing it is haggling
            terms.addAll(DiplomacyManager.client().warGoals(from.getIdFaction(), to.getIdFaction()));
        }
        panel = new ProposePanel(state);
    }

    /**
     * Renames an agreement already in force, by offering it again under a new name.
     */
    public static DiplomacyProposeDialog rename(GameClientState state, Faction from, Faction to,
                                                FactionTreaty treaty) {
        return new DiplomacyProposeDialog(state, from, to, null, treaty);
    }

    /**
     * Whether the viewer's role lets them offer this at all; the server checks the same thing.
     */
    public static boolean mayPropose(GameClientState state, Faction own) {
        if (own == null) {
            return false;
        }
        FactionPermission p = own.getMembersUID().get(state.getPlayerName());
        return p != null && (p.hasRelationshipPermission(own) || Perms.has(own, p, Perms.TREATIES));
    }

    private static boolean sameType(String a, String b) {
        return Objects.equals(a, b);
    }

    private static GUIAnchor entry(GameClientState state, Object userPointer, String text) {
        GUIAnchor c = new GUIAnchor(state, 380, 24);
        c.setUserPointer(userPointer);
        GUITextOverlay t = new GUITextOverlay(10, 10, state);
        t.setTextSimple(text);
        t.setUserPointer(userPointer);
        t.setPos(4, 6, 0);
        c.attach(t);
        return c;
    }

    private static String termName(long ms) {
        if (ms == 0) {
            return Lng.str("No term (until broken)");
        }
        long hours = ms / (1000L * 60L * 60L);
        if (hours < 24) {
            return Lng.str("%s hours", hours);
        }
        return Lng.str("%s days", hours / 24);
    }

    private static String pointerToTypeId(Object pointer) {
        String s = pointer == null ? "" : pointer.toString();
        return s.isEmpty() ? null : s;
    }

    private boolean mayDeclareWar() {
        FactionPermission p = from.getMembersUID().get(getState().getPlayerName());
        return p != null && p.hasRelationshipPermission(from);
    }

    /**
     * How an entry reads in the dropdown: the verb first, so nothing in the list claims to be a
     * treaty when it is not one. You propose an alliance, you demand vassalage, and you declare war -
     * the engine stores all three the same way, but the player is not doing the same thing.
     */
    private static String offerLabel(TreatyType t) {
        return t.getVerb() + ": " + t.getDisplayName();
    }

    /**
     * What can be put to this faction right now, agreements first and declarations last.
     */
    private List<TreatyType> proposable() {
        List<TreatyType> out = new ObjectArrayList<TreatyType>();
        for (TreatyType t : TreatyTypeRegistry.all()) {
            if (TreatyTypeRegistry.FEDERATION.equals(t.getId())) {
                //synthesised from membership rather than signed, so there is nothing to offer
                continue;
            }
            if (DiplomacyManager.client().hasTreaty(from.getIdFaction(), to.getIdFaction(), t.getId())
                    && !(renaming != null && t.getId().equals(renaming.typeId))) {
                //the one already in force is exactly what a rename re-offers
                continue;
            }
            if (t.getImplies() == FactionRelation.RType.ENEMY && !mayDeclareWar()) {
                continue;
            }
            out.add(t);
        }
        //agreements between equals, then what is demanded of somebody, then what is simply declared
        out.sort((o1, o2) -> {
            int byKind = o1.getKind().ordinal() - o2.getKind().ordinal();
            return byKind != 0 ? byKind : o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
        });
        return out;
    }

    private TreatyType selectedType() {
        return typeId == null ? null : TreatyTypeRegistry.get(typeId);
    }

    private boolean atWar() {
        return getState().getFactionManager().getRelation(from.getIdFaction(), to.getIdFaction()) == FactionRelation.RType.ENEMY;
    }

    private void ask() {
        askedFor = typeId;
        askedTerms = List.copyOf(terms);
        asked = true;
        DiplomacyManager.client().sendAcceptanceQuery(getState().getPlayerName(),
                from.getIdFaction(), to.getIdFaction(), typeId, askedTerms);
    }

    private String nameOf(int factionId) {
        Faction f = getState().getFactionManager().getFaction(factionId);
        return f != null ? f.getName() : String.valueOf(factionId);
    }

    /**
     * Every term on offer from here, as picker entries.
     *
     * <p>ponytail: faction points come in preset amounts rather than a typed number; a number field is
     * the upgrade if the presets turn out too coarse.</p>
     */
    private List<SettlementTerm> termChoices() {
        int us = from.getIdFaction();
        int them = to.getIdFaction();
        List<SettlementTerm> out = new ObjectArrayList<SettlementTerm>();
        for (int amount : new int[]{100, 500, 1000, 5000}) {
            out.add(SettlementTerm.points(them, amount));
            out.add(SettlementTerm.points(us, amount));
        }
        DiplomacyManager dm = DiplomacyManager.client();
        for (TreatyType t : TreatyTypeRegistry.all()) {
            SettlementTerm sign = SettlementTerm.signTreaty(them, t.getId());
            if (sign.problem(us, them) == null && t.needsAcceptance() && !dm.hasTreaty(us, them, t.getId())) {
                out.add(sign);
                if (!t.isSymmetric()) {
                    out.add(SettlementTerm.signTreaty(us, t.getId()));
                }
            }
        }
        //what either side holds - with each other, or with anyone else - can be given up
        for (Faction other : getState().getFactionManager().getFactionCollection()) {
            int o = other.getIdFaction();
            for (int giver : new int[]{them, us}) {
                if (o == giver) {
                    continue;
                }
                for (FactionTreaty held : dm.getTreaties(giver, o).getTreaties()) {
                    SettlementTerm drop = SettlementTerm.breakTreaty(giver, held.typeId, o == us || o == them ? 0 : o);
                    if (drop.problem(us, them) == null && !out.contains(drop)) {
                        out.add(drop);
                    }
                }
            }
        }
        return out;
    }

    private String termsText() {
        if (terms.isEmpty()) {
            return Lng.str("No terms.");
        }
        StringBuilder b = new StringBuilder();
        for (SettlementTerm t : terms) {
            if (b.length() > 0) {
                b.append("\n");
            }
            b.append("- ").append(t.describe(this::nameOf, from.getIdFaction(), to.getIdFaction()));
        }
        return b.toString();
    }

    /**
     * The line under the dropdowns: what they would say if asked.
     */
    private String previewText() {
        TreatyType selected = selectedType();
        if (selected != null && selected.getImplies() == FactionRelation.RType.ENEMY && !terms.isEmpty()) {
            DiplomacyProposal r = DiplomacyManager.client().getQueryResult();
            boolean matches = asked && r != null && r.a == from.getIdFaction() && r.b == to.getIdFaction()
                    && sameType(r.typeId, askedFor) && sameType(typeId, askedFor)
                    && r.terms.equals(askedTerms) && terms.equals(askedTerms);
            if (!matches) {
                return Lng.str("Checking your casus belli...");
            }
            return r.accepted
                    ? Lng.str("Your casus belli holds. War takes effect the moment you confirm.")
                    : Lng.str("No casus belli: %s", r.getMessage());
        }
        if (selected != null && selected.getKind() == TreatyType.Kind.DECLARATION) {
            //nobody is being asked, so there is no answer to preview - saying "they would refuse"
            //about a war declaration is just wrong
            return Lng.str("%s is not offered. It takes effect the moment you confirm.",
                    selected.getDisplayName());
        }
        if (!to.isNPC()) {
            return Lng.str("%s decides for itself. Offer it and see.", to.getName());
        }
        DiplomacyProposal r = DiplomacyManager.client().getQueryResult();
        //every client sees every answer on the shared buffer, so an answer only counts when it is
        //about this pair and the selection that is on screen right now
        boolean matches = asked && r != null
                && r.a == from.getIdFaction() && r.b == to.getIdFaction()
                && sameType(r.typeId, askedFor) && sameType(typeId, askedFor)
                && r.terms.equals(askedTerms) && terms.equals(askedTerms);
        if (!matches) {
            return Lng.str("Asking...");
        }
        if (r.accepted) {
            return Lng.str("They would accept.");
        }
        String why = r.getMessage();
        return why == null || why.isEmpty()
                ? Lng.str("They would refuse.")
                : Lng.str("They would refuse: %s", why);
    }

    /**
     * Puts the offer on the table. Both text fields are optional: an empty name is filled in by the
     * server from the treaty type, the two factions and their home systems - which the client cannot
     * work out for itself, because it does not know what the galaxy calls a system it has never been to.
     */
    private void send(String treatyName, String note) {
        long expiresAt = termMs > 0 ? System.currentTimeMillis() + termMs : 0L;
        if (counterTo != null) {
            DiplomacyManager.client().sendCounterOffer(getState().getPlayerName(),
                    counterTo, typeId, expiresAt, note, treatyName, terms);
        } else {
            DiplomacyManager.client().sendTreatyProposal(getState().getPlayerName(),
                    from.getIdFaction(), to.getIdFaction(), typeId, note,
                    DiplomacyProposal.Action.PROPOSE, expiresAt, treatyName, terms);
        }
    }

    @Override
    public void callback(GUIElement callingGuiElement, MouseEvent event) {
        if (!event.pressedLeftMouse()) {
            return;
        }
        Object p = callingGuiElement.getUserPointer();
        if ("OK".equals(p)) {
            //read before deactivating: the panel is torn down with the dialog
            String treatyName = panel.nameField.getText();
            String note = panel.messageField.getText();
            deactivate();
            send(treatyName, note);
        } else if ("CANCEL".equals(p) || "X".equals(p)) {
            cancel();
        } else if ("ADD_TERM".equals(p)) {
            if (picked != null && !terms.contains(picked) && terms.size() < DiplomacyProposal.MAX_TERMS) {
                terms.add(picked);
                ask();
            }
        } else if ("CLEAR_TERMS".equals(p)) {
            terms.clear();
            ask();
        } else if ("WAR_GOALS".equals(p)) {
            terms.clear();
            terms.addAll(DiplomacyManager.client().warGoals(from.getIdFaction(), to.getIdFaction()));
            ask();
        }
    }

    @Override
    public void handleKeyEvent(KeyEventInterface e) {
        super.handleKeyEvent(e);
    }

    @Override
    public GUIElement getInputPanel() {
        return panel;
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public boolean isOccluded() {
        return false;
    }

    @Override
    public void handleMouseEvent(MouseEvent e) {
    }

    @Override
    public GameClientState getState() {
        return super.getState();
    }

    private class ProposePanel extends GUIInputPanel {

        private GUIActivatableTextBar nameField;
        private GUIActivatableTextBar messageField;

        public ProposePanel(GameClientState state) {
            //the id carries the size: a window remembers what it was last resized to against its
            //own id, and that saved size beats whatever is passed here. Changing the size means
            //changing the id, or the panel keeps the geometry it had before the fields were added.
            super("DIPLOMACY_PROPOSE_PANEL_V4", state, 480, 720, DiplomacyProposeDialog.this,
                    counterTo != null
                            ? Lng.str("Counter-offer to %s", to.getName())
                            : Lng.str("Diplomacy with %s", to.getName()), "");
        }

        @Override
        public void onInit() {
            super.onInit();

            List<TreatyType> types = proposable();
            List<GUIElement> typeEntries = new ObjectArrayList<GUIElement>();
            if (atWar()) {
                //white peace only makes sense while there is a war to end
                typeEntries.add(entry(getClientState(), "", Lng.str("Propose: Peace (end the war)")));
            }
            for (TreatyType t : types) {
                GUIElement e = entry(getClientState(), t.getId(), offerLabel(t));
                //a rename re-offers the treaty being renamed, so it is the one selected
                typeEntries.add(renaming != null && t.getId().equals(renaming.typeId) ? 0 : typeEntries.size(), e);
            }
            if (typeEntries.isEmpty()) {
                //nothing left to offer: every type is either held already or out of reach
                typeEntries.add(entry(getClientState(), "", Lng.str("Nothing left to propose")));
            }
            //user pointer "" stands for white peace, because a null pointer would be indistinguishable
            //from an entry that was never given one
            typeId = pointerToTypeId(typeEntries.getFirst().getUserPointer());

            //attached first, so an expanded dropdown draws over them rather than under
            nameField = field(236, 24, TreatyNames.MAX_LENGTH, 1,
                    Lng.str("Named for you if left blank"));
            if (renaming != null) {
                nameField.setText(renaming.getDisplayName());
            }
            messageField = field(292, 56, 140, 5,
                    Lng.str("A note to send with it (optional)"));

            GUITextOverlay preview = new GUITextOverlay(440, 60, FontLibrary.FontSize.MEDIUM.getFont(), getClientState());
            preview.autoWrapOn = getContent();
            preview.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return previewText();
                }
            });
            preview.setPos(6, 150, 0);

            List<GUIElement> termEntries = new ObjectArrayList<GUIElement>();
            for (long ms : TERMS) {
                termEntries.add(entry(getClientState(), Long.valueOf(ms), termName(ms)));
            }
            termMs = 0L;

            GUIDropDownList termList = new GUIDropDownList(getClientState(), 440, 24, 140,
                    new DropDownCallback() {
                        @Override
                        public void onSelectionChanged(GUIListElement element) {
                            termMs = ((Long) element.getContent().getUserPointer()).longValue();
                        }
                    }, termEntries);
            termList.setPos(6, 112, 0);

            GUIDropDownList typeList = new GUIDropDownList(getClientState(), 440, 24, 180,
                    new DropDownCallback() {
                        @Override
                        public void onSelectionChanged(GUIListElement element) {
                            typeId = pointerToTypeId(element.getContent().getUserPointer());
                            ask();
                        }
                    }, typeEntries);
            typeList.setPos(6, 46, 0);

            //not "Treaty": the list holds demands and declarations too, and calling those treaties
            //is what made a war declaration read as an agreement
            GUITextOverlay typeLabel = label(Lng.str("Action"), 26);
            GUITextOverlay termLabel = label(Lng.str("Term"), 92);
            GUITextOverlay nameLabel = label(Lng.str("Name"), 216);
            GUITextOverlay messageLabel = label(Lng.str("Message"), 272);
            //below the message box's character counter, which draws under the box rather than inside it
            GUITextOverlay termsLabel = label("", 380);
            termsLabel.setTextSimple(new Object() {
                @Override
                public String toString() {
                    TreatyType t = selectedType();
                    return t != null && t.getImplies() == FactionRelation.RType.ENEMY
                            ? Lng.str("War goals (the starting point for peace)")
                            : Lng.str("Terms (all or nothing, at signing)");
                }
            });

            GUITextOverlay termsList = new GUITextOverlay(440, 140, FontLibrary.FontSize.MEDIUM.getFont(), getClientState());
            termsList.autoWrapOn = getContent();
            termsList.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return termsText();
                }
            });
            termsList.setPos(6, 464, 0);

            List<GUIElement> termEntries2 = new ObjectArrayList<GUIElement>();
            for (SettlementTerm t : termChoices()) {
                termEntries2.add(entry(getClientState(), t, t.describe(DiplomacyProposeDialog.this::nameOf,
                        from.getIdFaction(), to.getIdFaction())));
            }
            picked = termEntries2.isEmpty() ? null : (SettlementTerm) termEntries2.get(0).getUserPointer();
            GUIDropDownList termPicker = new GUIDropDownList(getClientState(), 440, 24, 180,
                    element -> picked = (SettlementTerm) element.getContent().getUserPointer(), termEntries2);
            termPicker.setPos(6, 400, 0);

            GUITextButton add = new GUITextButton(getClientState(), 64, 24, GUITextButton.ColorPalette.OK,
                    Lng.str("Add"), DiplomacyProposeDialog.this);
            add.setUserPointer("ADD_TERM");
            add.setPos(6, 432, 0);
            GUITextButton clear = new GUITextButton(getClientState(), 64, 24, GUITextButton.ColorPalette.CANCEL,
                    Lng.str("Clear"), DiplomacyProposeDialog.this);
            clear.setUserPointer("CLEAR_TERMS");
            clear.setPos(76, 432, 0);
            GUITextButton goals = new GUITextButton(getClientState(), 64, 24, GUITextButton.ColorPalette.NEUTRAL,
                    Lng.str("Goals"), DiplomacyProposeDialog.this);
            goals.setUserPointer("WAR_GOALS");
            goals.setPos(146, 432, 0);

            //attached bottom-up: a dropdown draws its expansion over whatever was attached before it,
            //so the one that opens downward over the other has to go last
            getContent().attach(typeLabel);
            getContent().attach(termLabel);
            getContent().attach(nameLabel);
            getContent().attach(messageLabel);
            getContent().attach(preview);
            getContent().attach(termsLabel);
            getContent().attach(termsList);
            getContent().attach(add);
            getContent().attach(clear);
            if (atWar()) {
                getContent().attach(goals);
            }
            getContent().attach(termPicker);
            getContent().attach(termList);
            getContent().attach(typeList);

            ask();
        }

        /**
         * A text field in a box of its own. The bar takes its width and height from whatever element
         * it is told to depend on, so a fixed-size anchor is the only thing stopping a multi-line one
         * from growing to fill the whole dialog.
         */
        private GUIActivatableTextBar field(int y, int height, int chars, int lines, String hint) {
            GUIAnchor box = new GUIAnchor(getClientState(), 440, height);
            box.setPos(6, y, 0);
            GUIActivatableTextBar bar = new GUIActivatableTextBar(getClientState(),
                    FontLibrary.FontSize.MEDIUM, chars, lines, hint, box, TEXT,
                    t -> t);
            bar.onInit();
            box.attach(bar);
            getContent().attach(box);
            return bar;
        }

        private GUITextOverlay label(String text, int y) {
            GUITextOverlay t = new GUITextOverlay(200, 16, FontLibrary.FontSize.MEDIUM.getFont(), getClientState());
            t.setTextSimple(text);
            t.setPos(6, y, 0);
            return t;
        }

        private GameClientState getClientState() {
            return (GameClientState) super.getState();
        }
    }
}
