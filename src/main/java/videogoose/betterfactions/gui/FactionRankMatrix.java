package videogoose.betterfactions.gui;

import videogoose.betterfactions.Perms;
import org.schema.game.client.controller.PlayerGameTextInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.common.data.player.faction.FactionRole;
import org.schema.game.common.data.player.faction.FactionRoles;
import org.schema.schine.common.InputChecker;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUICheckBoxTextPair;
import org.schema.schine.input.InputState;

import javax.vecmath.Vector4f;
import java.util.regex.Pattern;

/**
 * Who may do what, laid out as what it is: a grid of five ranks against the rights each one holds.
 */
public class FactionRankMatrix extends GUIAnchor {

    /**
     * The right's name column. Everything to the right of it is divided evenly between the ranks.
     */
    private static final int NAME_WIDTH = 236;
    private static final int MIN_COLUMN_WIDTH = 68;
    private static final int HEADER_HEIGHT = 44;
    private static final int SECTION_HEIGHT = 22;
    private static final int ROW_HEIGHT = 26;
    private static final Vector4f DIM = new Vector4f(0.55f, 0.58f, 0.60f, 1.0f);
    private static final Vector4f HEADING = new Vector4f(0.62f, 0.72f, 0.78f, 1.0f);
    private static final Vector4f YOURS = new Vector4f(0.45f, 0.86f, 0.91f, 1.0f);
    private static final Vector4f HELD = new Vector4f(0.55f, 0.58f, 0.60f, 0.85f);

    private final Faction faction;
    /**
     * The rewrite under construction. Nothing outside this object changes until the dialog is
     * accepted and {@link org.schema.game.common.data.player.faction.FactionManager#sendFactionRoles}
     * takes it.
     */
    private final FactionRoles edited = new FactionRoles();
    /**
     * Highest rank index this player may rewrite, {@link FactionRoles#ROLE_COUNT} for a server admin
     * who is bound by nothing, or -1 for somebody who may not edit at all. Mirrors the server's own
     * {@code rolesEditRank}.
     */
    private final int editRank;
    /**
     * Which rights this player can hand out, by the same rule the server applies: everything their own
     * rank holds, plus everything outside {@link FactionPermission#ADMIN_PERMISSIONS}, which confers
     * no authority and so is nobody's to withhold. Fixed on open, because the server weighs it against
     * the rank as it stands rather than as this screen leaves it.
     */
    private final long grantable;
    private final int[] memberCount = new int[FactionRoles.ROLE_COUNT];

    public FactionRankMatrix(InputState state, Faction faction) {
        super(state);
        this.faction = faction;
        if (faction != null) {
            edited.factionId = faction.getIdFaction();
            edited.apply(faction.getRoles());
            for (FactionPermission p : faction.getMembersUID().values()) {
                if (p.role >= 0 && p.role < FactionRoles.ROLE_COUNT) {
                    memberCount[p.role]++;
                }
            }
        }
        editRank = ownEditRank();
        grantable = editRank >= FactionRoles.ROLE_COUNT
                ? -1L
                : editRank < 0
                ? 0L
                : edited.getRoles()[editRank].role | ~FactionPermission.ADMIN_PERMISSIONS;
    }

    private int ownEditRank() {
        if (getState().getPlayer() != null && getState().getPlayer().isAdmin()) {
            return FactionRoles.ROLE_COUNT;
        }
        FactionPermission p = faction == null || getState().getPlayer() == null
                ? null
                : faction.getMembersUID().get(getState().getPlayer().getName());
        if (p == null || p.role < 0 || p.role >= FactionRoles.ROLE_COUNT) {
            return -1;
        }
        return p.role;
    }

    /**
     * @return the rewrite to send, which is the faction's own roles with whatever was changed here
     */
    public FactionRoles getRoles() {
        return edited;
    }

    @Override
    public GameClientState getState() {
        return (GameClientState) super.getState();
    }

    @Override
    public void onInit() {
        super.onInit();
        GUIElementList list = new GUIElementList(getState());

        if (faction == null) {
            add(list, section(Lng.str("No faction"), HEADING));
            list.setPos(1, 0, 0);
            attach(list);
            return;
        }

        add(list, header());
        group(list, Lng.str("People"),
                FactionPermission.PermType.INVITE_PERMISSION, FactionPermission.PermType.KICK_PERMISSION, FactionPermission.PermType.FACTION_EDIT_PERMISSION);
        group(list, Lng.str("Territory"),
                FactionPermission.PermType.MAY_CLAIM_SYSTEM, FactionPermission.PermType.HOMEBASE_PERMISSION,
                Perms.ASSETS_VIEW, FactionPermission.PermType.FOG_OF_WAR_SHARE);
        group(list, Lng.str("Diplomacy"),
                FactionPermission.PermType.RELATIONSHIPS_EDIT, Perms.TREATIES, Perms.FEDERATION_VOTE);
        group(list, Lng.str("Public face"),
                FactionPermission.PermType.NEWS_POST_PERMISSION);

        list.setPos(1, 0, 0);
        attach(list);
    }

    private void group(GUIElementList list, Object title, FactionPermission.PermType... rights) {
        add(list, section(title, HEADING));
        for (FactionPermission.PermType t : rights) {
            if (t.active) {
                add(list, row(t));
            }
        }
    }

    private void add(GUIElementList list, GUIAnchor e) {
        list.add(new GUIListElement(e, e, getState()));
    }

    private float columnWidth() {
        return Math.max(MIN_COLUMN_WIDTH, (rowWidth() - NAME_WIDTH) / FactionRoles.ROLE_COUNT);
    }

    private float rowWidth() {
        return Math.max(NAME_WIDTH + MIN_COLUMN_WIDTH * FactionRoles.ROLE_COUNT, getWidth() - 8);
    }

    /**
     * Column x for a rank, with the small inset every cell and caption shares so the grid reads as
     * columns rather than as five loose stacks.
     */
    private float columnX(int rank) {
        return NAME_WIDTH + rank * columnWidth() + 6;
    }

    /**
     * The rank names, each one the control that renames it, over what the rank costs to read: how many
     * people are in it, which one is yours, and which are out of your reach.
     */
    private GUIAnchor header() {
        GUIElement[] names = new GUIElement[FactionRoles.ROLE_COUNT];
        GUITextOverlay[] subs = new GUITextOverlay[FactionRoles.ROLE_COUNT];

        GUIAnchor h = new GUIAnchor(getState(), 100, HEADER_HEIGHT) {
            @Override
            public void draw() {
                setWidth(rowWidth());
                float cw = columnWidth();
                for (int i = 0; i < FactionRoles.ROLE_COUNT; i++) {
                    if (names[i] instanceof GUITextButton b) {
                        b.setWidth(cw - 10);
                    }
                    names[i].setPos(columnX(i), 4, 0);
                    subs[i].setPos(columnX(i) + 2, 26, 0);
                }
                super.draw();
            }
        };

        GUITextOverlay caption = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        caption.setTextSimple(Lng.str("LOWEST RANK FIRST"));
        caption.setColor(DIM);
        caption.setPos(8, 26, 0);
        h.attach(caption);

        for (int i = 0; i < FactionRoles.ROLE_COUNT; i++) {
            FactionRole role = edited.getRoles()[i];
            boolean mine = i == editRank;
            names[i] = i <= editRank ? renameButton(role) : rankLabel(role);
            subs[i] = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
            subs[i].setTextSimple(i > editRank
                    ? Lng.str("out of reach")
                    : mine
                    ? Lng.str("you, %s here", memberCount[i])
                    : Lng.str("%s here", memberCount[i]));
            subs[i].setColor(mine ? YOURS : DIM);
            h.attach(names[i]);
            h.attach(subs[i]);
        }
        return h;
    }

    /**
     * A rank you may not rewrite. Its name is text rather than a button, because
     * {@link FactionRoles#applyLimited} leaves the name of an out-of-reach rank alone too - a founder
     * who cannot be disarmed by a lieutenant cannot be renamed by one either.
     */
    private GUITextOverlay rankLabel(FactionRole role) {
        GUITextOverlay t = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium16(), getState());
        t.setTextSimple(new Object() {
            @Override
            public String toString() {
                return role.name;
            }
        });
        t.setColor(DIM);
        return t;
    }

    private GUITextButton renameButton(FactionRole role) {
        return new GUITextButton(getState(), 60, 20, GUITextButton.ColorPalette.OK, new Object() {
            @Override
            public String toString() {
                return role.name;
            }
        }, new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse()) {
                    rename(role);
                }
            }

            @Override
            public boolean isOccluded() {
                return !isActive();
            }
        });
    }

    private GUIAnchor section(Object title, Vector4f color) {
        GUIColoredRectangle rule = new GUIColoredRectangle(getState(), 10, 1,
                new Vector4f(color.x, color.y, color.z, 0.35f));
        GUIAnchor h = new GUIAnchor(getState(), 100, SECTION_HEIGHT) {
            @Override
            public void draw() {
                setWidth(rowWidth());
                rule.setWidth(getWidth() - 16);
                super.draw();
            }
        };
        GUITextOverlay t = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        t.setTextSimple(title);
        t.setColor(color);
        t.setPos(8, 4, 0);
        rule.setPos(8, 20, 0);
        h.attach(t);
        h.attach(rule);
        return h;
    }

    /**
     * One right against all five ranks.
     */
    private GUIAnchor row(FactionPermission.PermType right) {
        GUIElement[] cells = new GUIElement[FactionRoles.ROLE_COUNT];
        boolean mineToGive = (grantable & right.value) == right.value;

        GUIAnchor r = new GUIAnchor(getState(), 100, ROW_HEIGHT) {
            @Override
            public void draw() {
                setWidth(rowWidth());
                for (int i = 0; i < FactionRoles.ROLE_COUNT; i++) {
                    cells[i].setPos(columnX(i), 4, 0);
                }
                super.draw();
            }
        };

        GUITextOverlay n = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium16(), getState());
        n.setTextSimple(Perms.label(right));
        if (!mineToGive) {
            n.setColor(DIM);
        }
        n.setPos(8, 4, 0);
        r.attach(n);

        for (int i = 0; i < FactionRoles.ROLE_COUNT; i++) {
            cells[i] = mineToGive && settable(i, right)
                    ? checkBox(edited.getRoles()[i], right)
                    : mark(edited.getRoles()[i], right);
            r.attach(cells[i]);
        }
        return r;
    }

    /**
     * Whether this player may move this one cell at all, by the same two rules the server applies: a
     * rank above the editor's own is out of reach, and the top rank gives up no capability at all,
     * because the clamp only lets a right travel down and nothing above the founder could hand one
     * back. The founder's row therefore reads as fixed marks rather than boxes - except
     * kick-on-friendly-fire, which is a policy the faction sets on itself and not a right.
     *
     * @see FactionRoles#applyLimited
     */
    private boolean settable(int rank, FactionPermission.PermType right) {
        if (rank > editRank) {
            return false;
        }
        return rank != FactionRoles.INDEX_ADMIN_ROLE
                || (FactionPermission.ADMIN_PERMISSIONS & right.value) == 0;
    }

    private GUICheckBoxTextPair checkBox(FactionRole role, FactionPermission.PermType right) {
        return new GUICheckBoxTextPair(getState(), "", 0, 20) {
            @Override
            public boolean isActivated() {
                return role.hasPermission(right);
            }

            @Override
            public void activate() {
                role.setPermission(right, true);
            }

            @Override
            public void deactivate() {
                role.setPermission(right, false);
            }
        };
    }

    /**
     * A right this player can read but not set. Drawn as a dot rather than a greyed checkbox so that
     * it is obvious at a glance which cells are controls - a disabled checkbox looks like one you have
     * not clicked yet.
     */
    private GUIElement mark(FactionRole role, FactionPermission.PermType right) {
        GUIColoredRectangle dot = new GUIColoredRectangle(getState(), 8, 8, HELD) {
            @Override
            public void draw() {
                if (role.hasPermission(right)) {
                    super.draw();
                }
            }
        };
        dot.setPos(3, 4, 0);
        //a box the size of the checkbox it stands in for, so a column of mixed cells still lines up
        GUIAnchor cell = new GUIAnchor(getState(), 16, 16);
        cell.attach(dot);
        return cell;
    }

    private void rename(FactionRole role) {
        PlayerGameTextInput in = new PlayerGameTextInput("FACTION_RANK_RENAME", getState(), 16,
                Lng.str("Rename Rank"),
                Lng.str("What this rank is called. Every member of it is shown under this name."),
                role.name) {
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
                role.name = entry;
                return true;
            }
        };
        in.setInputChecker(new InputChecker() {
            @Override
            public boolean check(String entry, TextCallback callback) {
                if (entry.length() >= 3 && entry.length() <= 16
                        && Pattern.matches("[a-zA-Z0-9 _-]+", entry)) {
                    return true;
                }
                callback.onFailedTextCheck(Lng.str("Please only alphanumeric (and space, _, -) values \nand between 3 and 16 long!"));
                return false;
            }
        });
        in.activate();
    }
}
