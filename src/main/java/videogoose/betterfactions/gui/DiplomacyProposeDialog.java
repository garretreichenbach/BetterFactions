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
import videogoose.betterfactions.BFConfig;
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
     * A declaration picked from the Action list, or null for a deal - which, at war, is peace. A deal's
     * treaty is not picked here: it is whichever one is on the table in the columns, see {@link #deal()}.
     */
    private String typeId;
    private long termMs;
    /**
     * What the last query asked about, so a stale answer is not rendered against a new selection.
     */
    private Deal asked;
    /**
     * Everything on the table, treaties included. A pact both sides sign shows in both columns.
     */
    private final List<SettlementTerm> terms = new ObjectArrayList<SettlementTerm>();

    /**
     * What goes on the wire: the proposal's own treaty, and the terms riding on it. The wire carries
     * one treaty as the proposal itself, null meaning peace, so a deal needs one to stand on.
     */
    private record Deal(String typeId, List<SettlementTerm> terms) {
    }

    /** Width of the full-width rows, and of each of the two term columns under them. */
    private static final int FULL = 680;
    private static final int COL = 335;

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
            terms.add(SettlementTerm.signTreaty(primaryGiver(), renaming.typeId));
        }
        if (counterTo != null) {
            //start from what is on the table, so a counter can be "the same, minus that one term"
            TreatyType offered = counterTo.typeId != null ? TreatyTypeRegistry.get(counterTo.typeId) : null;
            if (offered != null && offered.needsAcceptance()) {
                //the offer bound the side it was put to, which is us
                terms.add(SettlementTerm.signTreaty(counterTo.b, offered.getId()));
            }
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

    /**
     * A faction in debt may answer offers but start nothing new; the server refuses the same, see
     * {@code DiplomacyManager.handleRelationShipOffers}.
     */
    public static boolean inDebt(Faction f) {
        return f != null && f.factionPoints < 0;
    }

    /** Why this dialog may not send, or null. A counter is an answer and peace is always open, so debt stops neither. */
    private String sendBlock() {
        if (counterTo == null && !(atWar() && typeId == null) && inDebt(from)) {
            return Lng.str("%s is in debt (%s faction points). You can answer offers, but cannot start new dealings until it is paid off.",
                    from.getName(), (int) from.factionPoints);
        }
        return deal() == null ? noAgreement() : null;
    }

    private String noAgreement() {
        return Lng.str("Nothing is on the table yet: add something to either column.");
    }

    /**
     * Who the proposal's own treaty binds when it is one-sided: the side it is put to. That holds for a
     * counter too, because the server turns a counter around before it goes back.
     */
    private int primaryGiver() {
        return to.getIdFaction();
    }

    /** A treaty both sides sign alike, which is why it shows in both columns. */
    private static boolean mutual(SettlementTerm t) {
        TreatyType type = t.kind == SettlementTerm.Kind.SIGN_TREATY ? TreatyTypeRegistry.get(t.typeId) : null;
        return type != null && type.isSymmetric();
    }

    /** Whether a term belongs in a column: the one its giver is on, or both for a mutual pact. */
    private boolean inColumn(SettlementTerm t, int giver) {
        return t.giver == giver || mutual(t);
    }

    /**
     * The table as it goes on the wire, or null when there is nothing on it. A declaration and a peace
     * carry everything as terms; a deal lifts its first treaty that can be the proposal's own out to be
     * it, and otherwise goes as a settlement of terms alone - which is how we sign a one-sided treaty.
     */
    private Deal deal() {
        if (typeId != null || atWar()) {
            return new Deal(typeId, List.copyOf(terms));
        }
        for (SettlementTerm t : terms) {
            if (t.kind == SettlementTerm.Kind.SIGN_TREATY && (mutual(t) || t.giver == primaryGiver())) {
                List<SettlementTerm> rest = new ObjectArrayList<SettlementTerm>(terms);
                rest.remove(t);
                return new Deal(t.typeId, List.copyOf(rest));
            }
        }
        return terms.isEmpty() ? null : new Deal(null, List.copyOf(terms));
    }

    private String describe(SettlementTerm t) {
        return mutual(t)
                ? Lng.str("Both sign %s", TreatyTypeRegistry.get(t.typeId).getDisplayName())
                : t.describe(this::nameOf, from.getIdFaction(), to.getIdFaction());
    }

    private static boolean sameType(String a, String b) {
        return Objects.equals(a, b);
    }

    private static GUIAnchor entry(GameClientState state, Object userPointer, String text) {
        return entry(state, userPointer, text, FULL - 20);
    }

    private static GUIAnchor entry(GameClientState state, Object userPointer, String text, int width) {
        GUIAnchor c = new GUIAnchor(state, width, 24);
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
    private String offerLabel(TreatyType t) {
        return t.getVerb() + ": " + displayName(t);
    }

    /**
     * The type's name, or what it amounts to between these two: a vassal's war on its lord is an
     * independence war.
     */
    private String displayName(TreatyType t) {
        return t.getImplies() == FactionRelation.RType.ENEMY
                && DiplomacyManager.client().isIndependenceWar(from.getIdFaction(), to.getIdFaction())
                ? Lng.str("Independence War") : t.getDisplayName();
    }

    /**
     * What can be declared on this faction right now. Agreements are not here: they go in the columns,
     * as something one side signs or, for a pact, both.
     */
    private List<TreatyType> proposable() {
        List<TreatyType> out = new ObjectArrayList<TreatyType>();
        if (atWar() || renaming != null || counterTo != null) {
            //at war the only thing on the table is how it ends; the server refuses anything else
            return out;
        }
        for (TreatyType t : TreatyTypeRegistry.all()) {
            if (t.getKind() != TreatyType.Kind.DECLARATION || TreatyTypeRegistry.FEDERATION.equals(t.getId())
                    || DiplomacyManager.client().hasTreaty(from.getIdFaction(), to.getIdFaction(), t.getId())) {
                //federation is synthesised from membership rather than signed, so there is nothing to offer
                continue;
            }
            if (t.getImplies() == FactionRelation.RType.ENEMY && (!mayDeclareWar()
                    || DiplomacyManager.client().getTreaties(from.getIdFaction(), to.getIdFaction()).getDerivedRelation() != null)) {
                //an alliance, pact, vassalage or federation has to be broken before a war can start
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
        asked = deal();
        if (asked != null) {
            DiplomacyManager.client().sendAcceptanceQuery(getState().getPlayerName(),
                    from.getIdFaction(), to.getIdFaction(), asked.typeId(), asked.terms());
        }
    }

    /**
     * Whether the last answer is about this pair and the table as it is right now: every client sees
     * every answer on the shared buffer.
     */
    private boolean answered(DiplomacyProposal r) {
        return asked != null && r != null && r.a == from.getIdFaction() && r.b == to.getIdFaction()
                && sameType(r.typeId, asked.typeId()) && r.terms.equals(asked.terms()) && asked.equals(deal());
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
        DiplomacyManager dm = DiplomacyManager.client();
        //treaties first: they are what a deal is mostly about
        for (TreatyType t : TreatyTypeRegistry.all()) {
            SettlementTerm sign = SettlementTerm.signTreaty(them, t.getId());
            //a mutual one is listed once, as given by them, and shown in both columns
            if (sign.problem(us, them) == null && t.needsAcceptance()
                    && (!dm.hasTreaty(us, them, t.getId()) || renaming != null && t.getId().equals(renaming.typeId))) {
                out.add(sign);
                if (!t.isSymmetric()) {
                    out.add(SettlementTerm.signTreaty(us, t.getId()));
                }
            }
        }
        for (int amount : new int[]{100, 500, 1000, 5000}) {
            out.add(SettlementTerm.points(them, amount));
            out.add(SettlementTerm.points(us, amount));
        }
        //territory: any system either side holds but its home; an NPC takes none by treaty
        for (Faction f : to.isNPC() ? new Faction[]{to} : new Faction[]{to, from}) {
            for (org.schema.common.util.linAlg.Vector3i sys : systemsOf(f)) {
                out.add(SettlementTerm.cedeSystem(f.getIdFaction(), sys));
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

    /**
     * The systems {@code f} could cede, home excluded. A player faction's claims ride its faction blob as
     * one sector per system; an NPC's come with the combat totals. Both are as of the last upkeep turn
     * or sweep, and the server checks ownership again at signing.
     */
    private static List<org.schema.common.util.linAlg.Vector3i> systemsOf(Faction f) {
        List<org.schema.common.util.linAlg.Vector3i> out = new ObjectArrayList<>();
        if (f.isNPC()) {
            out.addAll(DiplomacyManager.client().getCombatRatings().npcSystemListOf(f.getIdFaction()));
        } else {
            for (org.schema.common.util.linAlg.Vector3i sector : f.lastSystemSectors) {
                out.add(org.schema.game.common.data.world.VoidSystem.getPosFromSector(sector, new org.schema.common.util.linAlg.Vector3i()));
            }
        }
        out.remove(DiplomacyManager.homeSystem(f));
        return out;
    }

    private boolean declaringWar() {
        TreatyType t = selectedType();
        return t != null && t.getImplies() == FactionRelation.RType.ENEMY;
    }

    /**
     * The line under the dropdowns: what they would say if asked.
     */
    private String previewText() {
        TreatyType selected = selectedType();
        if (selected != null && selected.getImplies() == FactionRelation.RType.ENEMY) {
            DiplomacyProposal r = DiplomacyManager.client().getQueryResult();
            if (!answered(r)) {
                return Lng.str("Checking your casus belli...");
            }
            return r.accepted
                    ? Lng.str("Your casus belli holds. War takes effect the moment you confirm.")
                    : Lng.str("No casus belli: %s\nAn unjust war costs %s more faction points and marks you an aggressor.",
                    r.getMessage(), BFConfig.unjustWarCost());
        }
        if (selected != null && selected.getKind() == TreatyType.Kind.DECLARATION) {
            //nobody is being asked, so there is no answer to preview - saying "they would refuse"
            //about a war declaration is just wrong
            return Lng.str("%s is not offered. It takes effect the moment you confirm.",
                    displayName(selected));
        }
        if (deal() == null) {
            return noAgreement();
        }
        if (!to.isNPC()) {
            return Lng.str("%s decides for itself. Offer it and see.", to.getName());
        }
        DiplomacyProposal r = DiplomacyManager.client().getQueryResult();
        if (!answered(r)) {
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
        long expiresAt = termMs > 0 && !declaringWar() ? System.currentTimeMillis() + termMs : 0L;
        Deal d = deal();
        if (counterTo != null) {
            DiplomacyManager.client().sendCounterOffer(getState().getPlayerName(),
                    counterTo, d.typeId(), expiresAt, note, treatyName, d.terms());
        } else {
            DiplomacyManager.client().sendTreatyProposal(getState().getPlayerName(),
                    from.getIdFaction(), to.getIdFaction(), d.typeId(), note,
                    DiplomacyProposal.Action.PROPOSE, expiresAt, treatyName, d.terms());
        }
    }

    @Override
    public void callback(GUIElement callingGuiElement, MouseEvent event) {
        if (!event.pressedLeftMouse()) {
            return;
        }
        Object p = callingGuiElement.getUserPointer();
        if ("OK".equals(p) && sendBlock() != null) {
            panel.setErrorMessage(sendBlock(), 4000);
            return;
        }
        if ("OK".equals(p)) {
            //read before deactivating: the panel is torn down with the dialog
            String treatyName = panel.nameField.getText();
            String note = panel.messageField.getText();
            deactivate();
            send(treatyName, note);
        } else if ("WHITE_PEACE".equals(p)) {
            //status quo: the war ends and nothing changes hands, whatever was on the table
            String note = panel.messageField.getText();
            deactivate();
            typeId = null;
            terms.clear();
            send("", note);
        } else if ("CANCEL".equals(p) || "X".equals(p)) {
            cancel();
        } else if ("CLEAR_OFFER".equals(p) || "CLEAR_DEMAND".equals(p)) {
            int giver = "CLEAR_OFFER".equals(p) ? from.getIdFaction() : to.getIdFaction();
            terms.removeIf(t -> inColumn(t, giver));
            ask();
        } else if ("WAR_GOALS".equals(p)) {
            terms.clear();
            terms.addAll(DiplomacyManager.client().warGoals(from.getIdFaction(), to.getIdFaction()));
            ask();
        } else if (p instanceof SettlementTerm t && terms.remove(t)) {
            //a term in either column, clicked to take it off the table
            ask();
        }
    }

    /** Puts a picked term on the table; war goals are demands only, so nothing of ours rides a declaration. */
    private void addTerm(SettlementTerm t) {
        if (t != null && !terms.contains(t) && terms.size() < DiplomacyProposal.MAX_TERMS
                && !(declaringWar() && inColumn(t, from.getIdFaction()))) {
            terms.add(t);
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
        private GUIDropDownList offerPicker;
        private GUIDropDownList demandPicker;

        public ProposePanel(GameClientState state) {
            //the id carries the size: a window remembers what it was last resized to against its
            //own id, and that saved size beats whatever is passed here. Changing the size means
            //changing the id, or the panel keeps the geometry it had before the fields were added.
            super("DIPLOMACY_PROPOSE_PANEL_V5", state, 720, 720, DiplomacyProposeDialog.this,
                    counterTo != null
                            ? Lng.str("Counter-offer to %s", to.getName())
                            : Lng.str("Diplomacy with %s", to.getName()), "");
        }

        @Override
        public void draw() {
            //greyed rather than hidden, so the error on pressing it can say why
            if (getButtonOK() != null) {
                getButtonOK().setColorPalette(sendBlock() != null ? GUITextButton.ColorPalette.TRANSPARENT : GUITextButton.ColorPalette.OK);
            }
            super.draw();
        }

        @Override
        public void onInit() {
            super.onInit();

            List<TreatyType> types = proposable();
            List<GUIElement> typeEntries = new ObjectArrayList<GUIElement>();
            if (atWar() && mayDeclareWar()) {
                //white peace only makes sense while there is a war to end
                typeEntries.add(entry(getClientState(), "", Lng.str("Propose: Peace (end the war)")));
            } else if (!atWar()) {
                //what is signed is picked in the columns below
                typeEntries.add(entry(getClientState(), "", counterTo != null ? Lng.str("Counter-offer")
                        : renaming != null ? Lng.str("Rename") : Lng.str("Propose a deal")));
            }
            for (TreatyType t : types) {
                typeEntries.add(entry(getClientState(), t.getId(), offerLabel(t)));
            }
            if (typeEntries.isEmpty()) {
                //at war without the standing to end it
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

            GUITextOverlay preview = new GUITextOverlay(FULL, 60, FontLibrary.FontSize.MEDIUM.getFont(), getClientState());
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

            GUIDropDownList termList = new GUIDropDownList(getClientState(), FULL, 24, 140,
                    element -> termMs = ((Long) element.getContent().getUserPointer()).longValue(), termEntries) {
                @Override
                public void draw() {
                    //a war ends in peace, never on a timer
                    if (!declaringWar()) {
                        super.draw();
                    }
                }
            };
            termList.setPos(6, 112, 0);

            GUIDropDownList typeList = new GUIDropDownList(getClientState(), FULL, 24, 180,
                    element -> {
                        typeId = pointerToTypeId(element.getContent().getUserPointer());
                        if (declaringWar()) {
                            //war goals are demands only; concessions and pacts picked for a deal don't carry over
                            terms.removeIf(t -> inColumn(t, from.getIdFaction()));
                        }
                        refillTermPicker();
                        ask();
                    }, typeEntries);
            typeList.setPos(6, 46, 0);

            //not "Treaty": the list holds demands and declarations too, and calling those treaties
            //is what made a war declaration read as an agreement
            GUITextOverlay typeLabel = label(Lng.str("Action"), 26);
            GUITextOverlay termLabel = label(Lng.str("Term"), 92);
            GUITextOverlay nameLabel = label(Lng.str("Name"), 216);
            GUITextOverlay messageLabel = label(Lng.str("Message"), 272);
            //two sides of the table, below the message box's character counter: what we give, what we ask for
            GUIAnchor offerCol = column(6, true);
            GUIAnchor demandCol = column(6 + COL + 10, false);
            refillTermPicker();

            //attached bottom-up: a dropdown draws its expansion over whatever was attached before it,
            //so the one that opens downward over the other has to go last
            getContent().attach(typeLabel);
            getContent().attach(termLabel);
            getContent().attach(nameLabel);
            getContent().attach(messageLabel);
            getContent().attach(preview);
            getContent().attach(offerCol);
            getContent().attach(demandCol);
            getContent().attach(termList);
            getContent().attach(typeList);

            ask();
        }

        /**
         * One side of the table: a header naming who gives, a picker of what they could give, and what
         * they already do. Our side is hidden while declaring war, because war goals are demands only.
         */
        private GUIAnchor column(int x, boolean ours) {
            int giver = ours ? from.getIdFaction() : to.getIdFaction();
            GUIAnchor col = new GUIAnchor(getClientState(), COL, 240) {
                @Override
                public void draw() {
                    if (!(ours && declaringWar())) {
                        super.draw();
                    }
                }
            };
            col.setPos(x, 380, 0);

            GUITextOverlay header = new GUITextOverlay(COL, 16, FontLibrary.FontSize.MEDIUM.getFont(), getClientState());
            header.setTextSimple(new Object() {
                @Override
                public String toString() {
                    if (!ours && declaringWar()) {
                        return Lng.str("War goals (the starting point for peace)");
                    }
                    return Lng.str("%s gives", (ours ? from : to).getName());
                }
            });

            //one button per term, clicked to take it off the table; rebuilt whenever this side changes
            GUIAnchor list = new GUIAnchor(getClientState(), COL, 140) {
                private List<SettlementTerm> shown;

                @Override
                public void draw() {
                    List<SettlementTerm> mine = terms.stream().filter(t -> inColumn(t, giver)).toList();
                    if (!mine.equals(shown)) {
                        shown = mine;
                        for (Object c : List.copyOf(getChilds())) {
                            detach((org.schema.schine.graphicsengine.forms.AbstractSceneNode) c);
                        }
                        for (int i = 0; i < mine.size(); i++) {
                            SettlementTerm t = mine.get(i);
                            GUITextButton b = new GUITextButton(getClientState(), COL, 22,
                                    GUITextButton.ColorPalette.NEUTRAL,
                                    "x  " + describe(t),
                                    DiplomacyProposeDialog.this);
                            b.setUserPointer(t);
                            b.setPos(0, i * 24, 0);
                            attach(b);
                        }
                    }
                    super.draw();
                }
            };
            list.setPos(0, 84, 0);

            GUITextButton clear = new GUITextButton(getClientState(), 64, 24, GUITextButton.ColorPalette.CANCEL,
                    Lng.str("Clear"), DiplomacyProposeDialog.this);
            clear.setUserPointer(ours ? "CLEAR_OFFER" : "CLEAR_DEMAND");
            clear.setPos(0, 52, 0);

            //picking adds it, then the picker goes back to its prompt, so nothing reads as already chosen
            GUIDropDownList[] self = new GUIDropDownList[1];
            GUIDropDownList picker = new GUIDropDownList(getClientState(), COL, 24, 180, element -> {
                if (element.getContent().getUserPointer() instanceof SettlementTerm t) {
                    addTerm(t);
                    self[0].setSelectedElement(self[0].get(0));
                }
            });
            self[0] = picker;
            picker.setPos(0, 20, 0);

            col.attach(header);
            col.attach(list);
            col.attach(clear);
            if (!ours && atWar()) {
                GUITextButton goals = new GUITextButton(getClientState(), 64, 24, GUITextButton.ColorPalette.NEUTRAL,
                        Lng.str("Goals"), DiplomacyProposeDialog.this);
                goals.setUserPointer("WAR_GOALS");
                goals.setPos(70, 52, 0);
                col.attach(goals);
            }
            if (ours && atWar() && mayDeclareWar()) {
                GUITextButton whitePeace = new GUITextButton(getClientState(), 100, 24, GUITextButton.ColorPalette.OK,
                        Lng.str("White Peace"), DiplomacyProposeDialog.this);
                whitePeace.setUserPointer("WHITE_PEACE");
                whitePeace.setPos(70, 52, 0);
                col.attach(whitePeace);
            }
            //last, so its expansion draws over the list below it
            col.attach(picker);
            if (ours) {
                offerPicker = picker;
            } else {
                demandPicker = picker;
            }
            return col;
        }

        /** Refills both columns' pickers from what is on offer between the two right now. */
        private void refillTermPicker() {
            if (offerPicker == null || demandPicker == null) {
                return; //the type list fires before the pickers are built
            }
            for (GUIDropDownList picker : new GUIDropDownList[]{offerPicker, demandPicker}) {
                picker.clear();
                GUIElement prompt = entry(getClientState(), "", Lng.str("Add..."), COL - 20);
                picker.add(new GUIListElement(prompt, prompt, getClientState()));
            }
            for (SettlementTerm t : termChoices()) {
                for (GUIDropDownList picker : mutual(t) ? new GUIDropDownList[]{offerPicker, demandPicker}
                        : new GUIDropDownList[]{t.giver == from.getIdFaction() ? offerPicker : demandPicker}) {
                    GUIElement e = entry(getClientState(), t, describe(t), COL - 20);
                    picker.add(new GUIListElement(e, e, getClientState()));
                }
            }
            offerPicker.setSelectedElement(offerPicker.get(0));
            demandPicker.setSelectedElement(demandPicker.get(0));
        }

        /**
         * A text field in a box of its own. The bar takes its width and height from whatever element
         * it is told to depend on, so a fixed-size anchor is the only thing stopping a multi-line one
         * from growing to fill the whole dialog.
         */
        private GUIActivatableTextBar field(int y, int height, int chars, int lines, String hint) {
            GUIAnchor box = new GUIAnchor(getClientState(), FULL, height);
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
