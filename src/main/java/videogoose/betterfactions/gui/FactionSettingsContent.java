package videogoose.betterfactions.gui;

import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.diplomacy.FactionStrength;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.civics.CivicRegistry;
import videogoose.betterfactions.civics.CivicType;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.FactionCivics;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.PlayerTextAreaInput;
import org.schema.game.client.controller.manager.ingame.faction.FactionDialogNew;
import org.schema.game.client.controller.manager.ingame.faction.FactionPersonalEnemyDialog;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.faction.FactionColorPickerDialog;
import org.schema.game.client.view.mainmenu.FileChooserDialog;
import org.schema.game.client.view.mainmenu.gui.FileChooserStats;
import videogoose.betterfactions.emblem.FactionEmblem;
import org.schema.game.server.data.ServerConfig;
import org.schema.schine.resource.FileExt;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRole;
import org.schema.schine.common.InputChecker;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUICheckBoxTextPair;
import org.schema.schine.input.InputState;

import javax.swing.filechooser.FileFilter;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * What the faction is, how it behaves while nobody is looking, and the two things that cannot be
 * taken back.
 *
 * <p>Replaces the two button grids on the old OPTIONS tab, which held thirteen identical blue
 * rectangles: membership actions, three inbox queues carrying their counts in their captions,
 * identity editing, one destructive reset and three unexplained checkboxes. Nothing about a row told
 * you which kind it was. The queues and the membership actions moved to the tabs they belong to -
 * the overview and the member list - and what is left is sorted by what a row actually is.</p>
 *
 * <p>The second line on every row is the point of the rework. {@code "Consider neutrals enemy: "} is
 * a label with a trailing colon and no consequence attached; saying that our turrets will open fire
 * on anyone we have no agreement with is the same toggle a player can weigh before flipping it.</p>
 *
 * <p>Governance holds the rank ladder, which is the one part of it that has a model to bind to. The
 * rest of the section - government type, succession, and which decisions get put to a vote - waits on
 * that model rather than being drawn as rows that say "soon"; a row whose only content is that you
 * cannot use it yet is the same filler as a row that says a field is locked.</p>
 *
 * <p>Ranks are also reachable from the member list, and that is deliberate rather than duplication:
 * who may do what is governance, but the member list is where you are standing when you want it.</p>
 */
public class FactionSettingsContent extends GUIAnchor {

    private static final int ROW_HEIGHT = 40;
    private static final int SECTION_HEIGHT = 26;
    /**
     * Room kept clear on the right of every row for its control, so a long description runs out of
     * space before it runs into a button.
     */
    private static final int CONTROL_GUTTER = 200;
    private static final Vector4f DIM = new Vector4f(0.55f, 0.58f, 0.60f, 1.0f);
    private static final Vector4f HEADING = new Vector4f(0.62f, 0.72f, 0.78f, 1.0f);
    private static final Vector4f DANGER = new Vector4f(0.94f, 0.52f, 0.49f, 1.0f);

    private final FactionPanelTabs panel;

    public FactionSettingsContent(InputState state, FactionPanelTabs panel) {
        super(state);
        this.panel = panel;
        //so every control on the page goes inert while a dialog is open, the way the old button
        //panes did through their activeInterface
        activationInterface = panel;
    }

    public PlayerState getOwnPlayer() {
        return getState().getPlayer();
    }

    public Faction getOwnFaction() {
        return getState().getFaction();
    }

    @Override
    public GameClientState getState() {
        return (GameClientState) super.getState();
    }

    @Override
    public void onInit() {
        super.onInit();
        GUIElementList list = new GUIElementList(getState());

        if (getOwnFaction() == null) {
            add(list, section(Lng.str("No faction"), HEADING));
            add(list, row(Lng.str("Create a faction"),
                    Lng.str("You become its founder, and the only member until you invite somebody."),
                    button(Lng.str("CREATE"), GUITextButton.ColorPalette.OK, null,
                            () -> {
                                if (getState().getController().getPlayerInputs().isEmpty()) {
                                    new FactionDialogNew(getState(), Lng.str("Create Faction")).activate();
                                }
                            })));
            attachScrolled(list);
            return;
        }

        add(list, section(Lng.str("Identity"), HEADING));
        add(list, row(Lng.str("Description"),
                Lng.str("Shown to every other faction on the diplomacy screen."),
                button(Lng.str("EDIT"), GUITextButton.ColorPalette.OK,
                        () -> getOwnPlayer().getFactionController().hasDescriptionAndNewsPostPermission(),
                        this::editDescription)));
        add(list, row(Lng.str("Map colour"),
                Lng.str("Colors your systems on the galaxy map."),
                swatched(button(Lng.str("CHANGE"), GUITextButton.ColorPalette.OK, null,
                        () -> new FactionColorPickerDialog(getState()).activate()))));
        if (BFConfig.customEmblemsEnabled()) {
            add(list, row(Lng.str("Emblem"),
                    Lng.str("A PNG up to %s kb, shown beside your name wherever another faction is reading about you.",
                            FactionEmblem.MAX_UPLOAD_BYTES / 1024),
                    button(Lng.str("CHANGE"), GUITextButton.ColorPalette.OK,
                            () -> getOwnPlayer().getFactionController().hasRelationshipPermission(),
                            this::chooseEmblem)));
            add(list, row(Lng.str("Remove emblem"),
                    Lng.str("Go back to having none. An administrator can also remove it, and your faction is told when one does."),
                    button(Lng.str("REMOVE"), GUITextButton.ColorPalette.HOSTILE,
                            () -> getOwnPlayer().getFactionController().hasRelationshipPermission()
                                    && !DiplomacyManager.client().emblemOf(getOwnFaction()).isEmpty(),
                            () -> DiplomacyManager.client().requestEmblemClear())));
        }

        add(list, section(Lng.str("Governance"), HEADING));
        add(list, row(Lng.str("Ranks & permissions"),
                new Object() {
                    @Override
                    public String toString() {
                        return ladder();
                    }
                },
                button(Lng.str("EDIT RANKS"), GUITextButton.ColorPalette.OK,
                        () -> getOwnPlayer().getFactionController().hasPermissionEditPermission(),
                        () -> new FactionRankDialog(getState(), getOwnFaction()).activate())));

        add(list, section(Lng.str("Civics"), HEADING));
        int own = getOwnFaction().getIdFaction();
        //every slot there can be, so one earned while the panel is open shows without a rebuild
        int slots = Math.max(Civics.client().of(own).slotsEarned, FactionCivics.STARTING_SLOTS + BFConfig.civicSlotMilestones().length);
        for (int i = 0; i < slots; i++) {
            int slot = i;
            add(list, row(new Object() {
                        @Override
                        public String toString() {
                            FactionCivics c = Civics.client().of(own);
                            return slot < c.civics.size() ? CivicRegistry.nameOf(c.civics.get(slot))
                                    : slot >= c.slotsEarned ? Lng.str("Slot %s: locked", slot + 1) : Lng.str("Slot %s: empty", slot + 1);
                        }
                    },
                    new Object() {
                        @Override
                        public String toString() {
                            FactionCivics c = Civics.client().of(own);
                            if (slot >= c.slotsEarned) {
                                int[] m = BFConfig.civicSlotMilestones();
                                int at = slot - FactionCivics.STARTING_SLOTS;
                                return at < m.length ? Lng.str("Opens when the faction reaches strength %s (now %s). Once open, it stays open.",
                                        m[at], Math.round(FactionStrength.of(getOwnFaction()))) : Lng.str("No longer earned on this server.");
                            }
                            if (slot >= c.civics.size()) {
                                return Lng.str("A defining trait of the faction, chosen by those who may edit ranks.");
                            }
                            CivicType t = CivicRegistry.get(c.civics.get(slot));
                            return t != null ? t.getDescription() : Lng.str("The mod that added this civic is gone.");
                        }
                    },
                    button(Lng.str("CHOOSE"), GUITextButton.ColorPalette.OK,
                            //slots fill in order, so only the next empty one is offered
                            () -> getOwnPlayer().getFactionController().hasPermissionEditPermission()
                                    && slot <= Civics.client().of(own).civics.size() && slot < Civics.client().of(own).slotsEarned,
                            () -> CivicChooser.open(getState(), slot, false))));
        }

        add(list, section(Lng.str("Fleet doctrine"), HEADING));
        add(list, doctrines(own));

        add(list, section(Lng.str("Standing orders"), HEADING));
        add(list, row(Lng.str("Treat neutrals as enemies"),
                Lng.str("Our turrets open fire on anyone we have no agreement with."),
                toggle(() -> getOwnFaction() != null && getOwnFaction().isAttackNeutral(),
                        on -> getOwnFaction().clientRequestAttackNeutral(getOwnPlayer(), on))));
        add(list, row(Lng.str("Open to join"),
                Lng.str("Anyone may join without an invitation. Your ranks still decide what they can do."),
                toggle(() -> getOwnFaction() != null && getOwnFaction().isOpenToJoin(),
                        on -> getOwnFaction().clientRequestOpenFaction(getOwnPlayer(), on))));
        add(list, row(Lng.str("Individual enemies"),
                new Object() {
                    @Override
                    public String toString() {
                        Faction own = getOwnFaction();
                        int n = own == null ? 0 : own.getPersonalEnemies().size();
                        return n == 0
                                ? Lng.str("Nobody is marked as an enemy personally.")
                                : Lng.str("%s marked as enemies personally, whatever faction they are in.", n);
                    }
                },
                button(Lng.str("MANAGE"), GUITextButton.ColorPalette.OK,
                        () -> getOwnPlayer().getFactionController().hasRelationshipPermission(),
                        () -> new FactionPersonalEnemyDialog(getState(), getOwnFaction()).activate())));

        add(list, section(Lng.str("Cannot be undone"), DANGER));
        add(list, row(Lng.str("Reset homebase"),
                Lng.str("The faction keeps its points and its treaties, but loses homebase protection "
                        + "until a new home is claimed."),
                button(Lng.str("RESET"), GUITextButton.ColorPalette.HOSTILE,
                        () -> getOwnPlayer().getFactionController().hasHomebaseEditPermission()
                                && getOwnPlayer().getFactionController().hasHomebase(),
                        this::resetHomebase)));
        add(list, row(Lng.str("Leave faction"),
                Lng.str("You go immediately. If you are the last member, the faction disbands with you."),
                button(Lng.str("LEAVE"), GUITextButton.ColorPalette.HOSTILE, null, this::leaveFaction)));

        attachScrolled(list);
    }

    /**
     * The page scrolls rather than clipping. It is a list of settings that only ever grows - the
     * governance section alone is three rows short of what it will be - and the panel is resizable, so
     * the bottom of it is exactly where an unscrolled list loses the two rows that cannot be undone.
     */
    private void attachScrolled(GUIElementList list) {
        list.setPos(1, 0, 0);
        GUIScrollablePanel sc = new GUIScrollablePanel(10, 10, this, getState());
        sc.setContent(list);
        sc.onInit();
        attach(sc);
    }

    private void add(GUIElementList list, GUIAnchor e) {
        list.add(new GUIListElement(e, e, getState()));
    }

    /**
     * One setting: what it is called, what it does, and the control that changes it.
     */
    private GUIAnchor row(Object name, Object description, GUIElement control) {
        GUIAnchor r = new GUIAnchor(getState(), 100, ROW_HEIGHT) {
            @Override
            public void draw() {
                setWidth(Math.max(CONTROL_GUTTER, FactionSettingsContent.this.getWidth() - 8));
                if (control != null) {
                    control.setPos(getWidth() - control.getWidth() - 10, 8, 0);
                }
                super.draw();
            }
        };
        GUITextOverlay n = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium16(), getState());
        n.setTextSimple(name);
        n.setPos(8, 4, 0);

        GUITextOverlay d = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        d.setTextSimple(description);
        d.setColor(DIM);
        d.setPos(8, 22, 0);

        r.attach(n);
        r.attach(d);
        if (control != null) {
            r.attach(control);
        }
        return r;
    }

    private static final int DOCTRINE_CARD_HEIGHT = 96;

    /**
     * The three doctrines side by side, the current one marked. Few enough to show at once, so they are
     * picked here rather than in the civic picker. Switching costs what a civic swap does, and asks first.
     */
    private GUIAnchor doctrines(int own) {
        List<GUIAnchor> cards = new ArrayList<>();
        for (CivicType t : CivicRegistry.all()) {
            if (t.isDoctrine()) {
                cards.add(doctrineCard(own, t));
            }
        }
        GUITextOverlay note = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        note.setTextSimple(new Object() {
            @Override
            public String toString() {
                return Civics.client().of(own).doctrine == null
                        ? Lng.str("How the faction fights: one doctrine at a time. The first is free; click one to choose it.")
                        : Lng.str("One at a time. Switching costs %s faction points and shares the civic swap cooldown.", videogoose.betterfactions.civics.Council.ownCost(videogoose.betterfactions.civics.Council.REFORM, BFConfig.civicSwapCost()));
            }
        });
        note.setColor(DIM);
        note.setPos(8, 4, 0);
        GUIAnchor row = new GUIAnchor(getState(), 100, DOCTRINE_CARD_HEIGHT + 34) {
            @Override
            public void draw() {
                setWidth(Math.max(CONTROL_GUTTER, FactionSettingsContent.this.getWidth() - 8));
                int cw = ((int) getWidth() - 16 - 10 * (cards.size() - 1)) / Math.max(1, cards.size());
                for (int i = 0; i < cards.size(); i++) {
                    cards.get(i).setWidth(cw);
                    cards.get(i).setPos(8 + i * (cw + 10), 26, 0);
                }
                super.draw();
            }
        };
        row.attach(note);
        for (GUIAnchor c : cards) {
            row.attach(c);
        }
        return row;
    }

    private GUIAnchor doctrineCard(int own, CivicType t) {
        String id = t.getId();
        BooleanSupplier current = () -> id.equals(Civics.client().of(own).doctrine);
        GUIColoredRectangle bg = new GUIColoredRectangle(getState(), 10, DOCTRINE_CARD_HEIGHT, new Vector4f(InstitutionsContent.CARD));
        GUIAnchor wrap = new GUIAnchor(getState(), 10, DOCTRINE_CARD_HEIGHT);
        GUITextOverlay name = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium16(), getState());
        name.setTextSimple(t.getDisplayName());
        name.setPos(10, 8, 0);
        GUITextOverlay tag = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        tag.setTextSimple(new Object() {
            @Override
            public String toString() {
                return current.getAsBoolean() ? Lng.str("CURRENT")
                        : Civics.client().of(own).doctrine == null ? Lng.str("FREE") : Lng.str("SWITCH: %s FP", videogoose.betterfactions.civics.Council.ownCost(videogoose.betterfactions.civics.Council.REFORM, BFConfig.civicSwapCost()));
            }
        });
        GUITextOverlay desc = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        desc.setTextSimple(t.getDescription());
        desc.setColor(DIM);
        desc.autoWrapOn = wrap;
        desc.setPos(10, 30, 0);
        //an empty transparent button over the card is what makes all of it clickable
        GUITextButton hit = button(getState(), 10, "", GUITextButton.ColorPalette.TRANSPARENT,
                () -> getOwnPlayer().getFactionController().hasPermissionEditPermission() && !current.getAsBoolean(),
                () -> chooseDoctrine(own, t));
        hit.setHeight(DOCTRINE_CARD_HEIGHT);
        GUIAnchor card = new GUIAnchor(getState(), 10, DOCTRINE_CARD_HEIGHT) {
            @Override
            public void draw() {
                bg.setWidth(getWidth());
                bg.getColor().set(current.getAsBoolean() ? InstitutionsContent.CARD_HELD : InstitutionsContent.CARD);
                wrap.setWidth(getWidth() - 20);
                hit.setWidth(getWidth());
                tag.setColor(current.getAsBoolean() ? InstitutionsContent.BLUE : DIM);
                tag.setPos(getWidth() - 10 - tag.getMaxLineWidth(), 10, 0);
                super.draw();
            }
        };
        card.attach(bg);
        card.attach(name);
        card.attach(tag);
        card.attach(desc);
        card.attach(hit);
        return card;
    }

    private void chooseDoctrine(int own, CivicType t) {
        FactionCivics c = Civics.client().of(own);
        if (c.doctrine == null) {
            Civics.requestPick(FactionCivics.DOCTRINE, t.getId(), (short) 0);
            return;
        }
        long now = System.currentTimeMillis();
        long cooldownMs = BFConfig.civicSwapCooldownDays() * 24L * 60L * 60L * 1000L;
        long days = c.cooldownDays(now, cooldownMs);
        if (days > 0 && BFConfig.civicRushCost() <= 0) {
            getState().getController().popupAlertTextMessage(Lng.str("You can switch again in %s days.", days), 0);
            return;
        }
        int cost = c.cost(FactionCivics.DOCTRINE, now, videogoose.betterfactions.civics.Council.ownCost(videogoose.betterfactions.civics.Council.REFORM, BFConfig.civicSwapCost()), BFConfig.civicRushCost(), cooldownMs);
        new PlayerGameOkCancelInput("CONFIRM", getState(), Lng.str("Switch to %s?", t.getDisplayName()),
                Lng.str("Replaces %s for %s faction points. The next civic or doctrine swap waits %s days.",
                        CivicRegistry.nameOf(c.doctrine), cost, BFConfig.civicSwapCooldownDays())) {
            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void onDeactivate() {
            }

            @Override
            public void pressedOK() {
                deactivate();
                Civics.requestPick(FactionCivics.DOCTRINE, t.getId(), (short) 0);
            }
        }.activate();
    }

    /**
     * The rank ladder, top down, as the description of the row that opens it. A member whose rank may
     * not edit ranks still gets to read what the ladder is - only the button goes.
     */
    private String ladder() {
        Faction own = getOwnFaction();
        if (own == null) {
            return "";
        }
        FactionRole[] roles = own.getRoles().getRoles();
        StringBuilder b = new StringBuilder();
        //a long ladder ran off the row; the top four say what it is
        if (roles.length > 4) {
            return Lng.str("%s, %s, %s, %s and %s more - and what each of them may do.", roles[roles.length - 1].name,
                    roles[roles.length - 2].name, roles[roles.length - 3].name, roles[roles.length - 4].name, roles.length - 4);
        }
        for (int i = roles.length - 1; i >= 0; i--) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(roles[i].name);
        }
        return Lng.str("%s - and what each of them may do.", b.toString());
    }

    /**
     * A control with the faction's colour beside it. The picker is a dialog, so without this the row
     * says a colour exists and then makes you open something to find out which one.
     */
    private GUIAnchor swatched(GUIElement control) {
        GUIColoredRectangle swatch = new GUIColoredRectangle(getState(), 18, 18, new Vector4f()) {
            @Override
            public void draw() {
                Faction own = getOwnFaction();
                if (own != null) {
                    Vector3f c = own.getColor();
                    getColor().set(c.x, c.y, c.z, 1.0f);
                }
                super.draw();
            }
        };
        swatch.setPos(0, 2, 0);
        control.setPos(26, 0, 0);
        GUIAnchor box = new GUIAnchor(getState(), 26 + control.getWidth(), 22);
        box.attach(swatch);
        box.attach(control);
        return box;
    }

    /**
     * Picks a PNG and sends it. Nothing here is a permission check or a validity check - the server
     * refuses a file it does not like and says so, and would refuse a replayed packet the same way with
     * this dialog never having existed. The size is checked because it is the one refusal we can save a
     * player the wait for.
     */
    private void chooseEmblem() {
        new FileChooserDialog(getState(), new FileChooserStats(getState(), new FileExt("./"), "", new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".png");
            }

            @Override
            public String getDescription() {
                return ".png (Faction Emblem)";
            }
        }) {
            @Override
            public void onSelectedFile(File dir, String name) {
                File file = new FileExt(dir, name);
                if (!file.exists() || file.isDirectory()) {
                    return;
                }
                if (file.length() > FactionEmblem.MAX_UPLOAD_BYTES) {
                    getState().getController().popupAlertTextMessage(
                            Lng.str("That image is %s kb.\nThe limit is %s kb.",
                                    file.length() / 1024, FactionEmblem.MAX_UPLOAD_BYTES / 1024), 0);
                    return;
                }
                try {
                    DiplomacyManager.client().uploadEmblemFile(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    getState().getController().popupAlertTextMessage(
                            Lng.str("Could not send the emblem:\n%s", e.getClass().getSimpleName()), 0);
                }
            }
        }).activate();
    }

    private GUIAnchor section(Object title, Vector4f color) {
        GUIColoredRectangle rule = new GUIColoredRectangle(getState(), 10, 1,
                new Vector4f(color.x, color.y, color.z, 0.35f));
        GUIAnchor h = new GUIAnchor(getState(), 100, SECTION_HEIGHT) {
            @Override
            public void draw() {
                setWidth(Math.max(CONTROL_GUTTER, FactionSettingsContent.this.getWidth() - 8));
                rule.setWidth(getWidth() - 16);
                super.draw();
            }
        };
        GUITextOverlay t = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        t.setTextSimple(title);
        t.setColor(color);
        t.setPos(8, 8, 0);
        rule.setPos(8, 24, 0);
        h.attach(t);
        h.attach(rule);
        return h;
    }

    /**
     * A bare checkbox, right-aligned by its row. {@link GUICheckBoxTextPair} normally carries its own
     * label at a fixed offset; here the row already said what the setting is, twice.
     */
    private GUICheckBoxTextPair toggle(BooleanSupplier on, Consumer<Boolean> set) {
        return new GUICheckBoxTextPair(getState(), "", 0, 24) {
            @Override
            public boolean isActivated() {
                return on.getAsBoolean();
            }

            @Override
            public void activate() {
                if (FactionSettingsContent.this.isActive() && getOwnFaction() != null) {
                    set.accept(Boolean.TRUE);
                }
            }

            @Override
            public void deactivate() {
                if (FactionSettingsContent.this.isActive() && getOwnFaction() != null) {
                    set.accept(Boolean.FALSE);
                }
            }
        };
    }

    /**
     * @param allowed when the viewer's rank may use this at all, or null when anyone may. A control
     *                that is not allowed is not drawn, which is also what makes it unclickable - the
     *                member list's promote and kick buttons have always worked this way.
     */
    private GUITextButton button(String text, GUITextButton.ColorPalette palette, BooleanSupplier allowed,
                                 Runnable onPress) {
        return button(getState(), 88, text, palette, allowed, onPress);
    }

    static GUITextButton button(InputState state, int width, Object text, GUITextButton.ColorPalette palette,
                                BooleanSupplier allowed, Runnable onPress) {
        return new GUITextButton(state, width, 22, palette, text, new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse() && (allowed == null || allowed.getAsBoolean())) {
                    onPress.run();
                }
            }

            @Override
            public boolean isOccluded() {
                return allowed != null && !allowed.getAsBoolean();
            }
        }) {
            @Override
            public void draw() {
                if (allowed == null || allowed.getAsBoolean()) {
                    super.draw();
                }
            }
        };
    }

    private void editDescription() {
        Faction own = getOwnFaction();
        PlayerTextAreaInput t = new PlayerTextAreaInput("FACTION_SETTINGS_DESC", getState(), 140, 5,
                Lng.str("Edit Faction Description"), "",
                own != null ? own.getDescription() : "") {
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
                return getState().getPlayer().getFactionController().editDescriptionClient(entry);
            }
        };
        t.activate();
        t.setInputChecker(new InputChecker() {
            @Override
            public boolean check(String entry, TextCallback callback) {
                return true;
            }
        });
    }

    private void resetHomebase() {
        PlayerGameOkCancelInput check = new PlayerGameOkCancelInput("CONFIRM", getState(),
                Lng.str("Confirm reset Homebase"),
                Lng.str("The faction keeps its points and its treaties, but loses homebase\n"
                        + "protection until a new home is claimed. Continue?")) {
            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void onDeactivate() {
            }

            @Override
            public void pressedOK() {
                deactivate();
                getState().getFactionManager().sendClientHomeBaseChange(getState().getPlayer().getName(),
                        getState().getPlayer().getFactionId(), "");
            }
        };
        check.activate();
    }

    private void leaveFaction() {
        PlayerGameOkCancelInput confirm = new PlayerGameOkCancelInput("CONFIRM", getState(),
                Lng.str("Confirm"),
                Lng.str("Do you really want to leave this faction?\n"
                        + "You'll be unable to access any ship/structure that belongs to this\n"
                        + "faction except for your ships that use the Personal Permission Rank.\n\n"
                        + "If you are the last member, the faction will also automatically disband!")) {
            @Override
            public void onDeactivate() {
            }

            @Override
            public void pressedOK() {
                getState().getPlayer().getFactionController().leaveFaction();
                deactivate();
            }
        };
        confirm.activate();
    }
}
