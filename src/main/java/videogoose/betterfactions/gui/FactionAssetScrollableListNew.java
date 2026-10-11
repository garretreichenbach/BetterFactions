package videogoose.betterfactions.gui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.Perms;
import videogoose.betterfactions.assets.FactionAssets;
import videogoose.betterfactions.assets.FleetRoutes;

import javax.vecmath.Vector4f;
import java.util.*;

/**
 * Everything the faction holds that is not a ship: where each thing is, and whether it stands in
 * territory the faction has actually claimed.
 *
 * <p>Nothing has ever shown a faction this. A station the faction owns exists nowhere on a client until
 * somebody flies to it, so the list comes from the server's entity table and is a snapshot with an age on
 * it - see {@link FactionAssets}. The age is on the screen rather than hidden, because a list that looks
 * live and is not is worse than one that admits it.</p>
 *
 * <p>The edge strip answers one question and only one: is this inside our own borders. A holding sitting
 * in somebody else's space, or in nobody's, is the thing worth picking out of a long list - it is the one
 * paying no upkeep and getting no protection.</p>
 *
 * <p>Claimed systems the answer lists nothing in get a row of their own, so a claim anchored on something
 * that is not a station still appears, and so the tab is not blank while the first answer is in flight -
 * claimed systems already ride the faction blob.</p>
 */
public class FactionAssetScrollableListNew extends ScrollableTableList<FactionAssetScrollableListNew.Holding> {

    /**
     * Green, matching the overview's territory colour - claimed space is claimed space on both screens.
     */
    private static final Vector4f TERRITORY = new Vector4f(0.03f, 0.76f, 0.33f, 1.0f);
    /**
     * The home station: the one holding whose loss ends the faction.
     */
    private static final Vector4f HOME = new Vector4f(0.90f, 0.70f, 0.13f, 1.0f);
    /**
     * Standing outside any system the faction has claimed.
     */
    private static final Vector4f OUTSIDE = new Vector4f(0.44f, 0.48f, 0.52f, 1.0f);

    private final GUIElement parent;
    private final Faction faction;
    /**
     * The {@code queriedAt} of the answer this list was last built from, so a reply arriving after the
     * list has already drawn rebuilds it. Without this the first look at the tab stays as it was:
     * nothing else marks the list dirty when a packet lands.
     */
    private long builtFrom = -1;

    public FactionAssetScrollableListNew(InputState state, GUIElement p, Faction faction) {
        super(state, 100, 100, p);
        parent = p;
        this.faction = faction;
        //fleets are the one thing here that changes without the server being asked anything: they are
        //added, disbanded and given orders locally, and none of that touches the asset answer this list
        //otherwise rebuilds on
        ((GameClientState) state).getFleetManager().addObserver(this);
    }

    @Override
    public void cleanUp() {
        client().getFleetManager().deleteObserver(this);
        super.cleanUp();
    }

    /**
     * One row. Three things end up in this list and they are all answers to "what have we got": a
     * station or shop, a fleet, or a system we have claimed that neither is standing in.
     */
    public static final class Holding {
        /**
         * A station or shop, or null when this row is a fleet or a bare claim.
         */
        public final FactionAssets.Asset asset;
        /**
         * A fleet, or null when this row is a station or a bare claim.
         */
        public final Fleet fleet;
        public final Vector3i system;
        public final boolean home;
        public final boolean claimed;

        private Holding(FactionAssets.Asset asset, Fleet fleet, Vector3i system, boolean home, boolean claimed) {
            this.asset = asset;
            this.fleet = fleet;
            this.system = system;
            this.home = home;
            this.claimed = claimed;
        }

        static Holding of(FactionAssets.Asset asset, Vector3i system, boolean home, boolean claimed) {
            return new Holding(asset, null, system, home, claimed);
        }

        static Holding of(Fleet fleet, Vector3i system, boolean claimed) {
            return new Holding(null, fleet, system, false, claimed);
        }

        static Holding claim(Vector3i system) {
            return new Holding(null, null, system, false, true);
        }

        boolean isFleet() {
            return fleet != null;
        }

        boolean isBareClaim() {
            return asset == null && fleet == null;
        }

        /**
         * Two holdings are the same row when they would draw the same.
         *
         * <p>This is not housekeeping. {@code ScrollableTableList.handleDirty} rebuilds by diffing the
         * new element list against the rows it already has, keyed on {@code equals} - so a row whose
         * element does not equal itself is destroyed and built again on every refresh, taking the
         * selection and the highlight with it. A {@code Holding} is minted fresh by
         * {@link #getElementList}, so on identity that is <i>every</i> refresh, and the fleet manager
         * notifies its observers whenever any fleet anywhere reports a mission or changes sector. The
         * list was therefore rebuilding itself under the player several times a second.</p>
         *
         * <p>Compared on what the row shows rather than on what it points at: a fleet's own fields are
         * read live so they stay current without churn, while an asset is a snapshot and has to be
         * compared field by field or a rename would never appear. Anything that would change a cell
         * counts as a different row, which is exactly when replacing one is worth the selection.</p>
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Holding)) {
                return false;
            }
            Holding h = (Holding) o;
            if (home != h.home || claimed != h.claimed || !system.equals(h.system)) {
                return false;
            }
            if ((fleet == null) != (h.fleet == null) || (asset == null) != (h.asset == null)) {
                return false;
            }
            if (fleet != null) {
                return fleet.dbid == h.fleet.dbid && fleet.getMembers().size() == h.fleet.getMembers().size();
            }
            if (asset == null) {
                return true;
            }
            return asset.type == h.asset.type
                    && Objects.equals(asset.uid, h.asset.uid)
                    && Objects.equals(asset.name, h.asset.name)
                    && Objects.equals(asset.sector, h.asset.sector);
        }

        @Override
        public int hashCode() {
            int r = system.hashCode();
            if (fleet != null) {
                return 31 * r + Long.hashCode(fleet.dbid);
            }
            if (asset != null) {
                return 31 * r + (asset.uid == null ? 0 : asset.uid.hashCode());
            }
            return r;
        }
    }

    private GameClientState client() {
        return (GameClientState) getState();
    }

    /**
     * Whether this member's rank may read the holdings. Claimed systems are shown either way - they are
     * on the galaxy map in the faction's own colour for the whole server to see, so hiding them here
     * would protect nothing and only make the tab look broken.
     */
    private boolean mayView() {
        return Perms.mine(Perms.ASSETS_VIEW);
    }

    /**
     * The centre sector of a system. There is a helper for sector-to-system and none for the way back,
     * so this is the same arithmetic the admin claim command does.
     */
    private static Vector3i centreSector(Vector3i system) {
        return new Vector3i(
                system.x * VoidSystem.SYSTEM_SIZE + VoidSystem.SYSTEM_SIZE_HALF,
                system.y * VoidSystem.SYSTEM_SIZE + VoidSystem.SYSTEM_SIZE_HALF,
                system.z * VoidSystem.SYSTEM_SIZE + VoidSystem.SYSTEM_SIZE_HALF);
    }

    /**
     * The name of a system, if this client has the galaxy data for it. It usually does not for somewhere
     * nobody has been, which is why the coordinates are always shown too.
     */
    private String systemName(Vector3i system) {
        VoidSystem s = client().getController().getClientChannel().getGalaxyManagerClient()
                .getSystemOnClient(centreSector(system));
        if (s == null || s.getName() == null || s.getName().isEmpty()) {
            return system.toStringPure();
        }
        return Lng.str("%s %s", s.getName(), system.toStringPure());
    }

    private static SimpleTransformableSendableObject.EntityType typeOf(FactionAssets.Asset a) {
        if (a == null || a.type < 0 || a.type >= SimpleTransformableSendableObject.EntityType.byDbId.length || SimpleTransformableSendableObject.EntityType.byDbId[a.type] == null) {
            return null;
        }
        return SimpleTransformableSendableObject.EntityType.byDbId[a.type];
    }

    private static String kind(Holding h) {
        if (h.isBareClaim()) {
            return Lng.str("Claim only");
        }
        if (h.isFleet()) {
            FactionAssets.Route r = routeOf(h);
            if (r == null) {
                return Lng.str("Fleet (%s)", h.fleet.getMembers().size());
            }
            return r.isDelivery() ? Lng.str("Delivery (%s)", h.fleet.getMembers().size())
                    : Lng.str("On route (%s)", h.fleet.getMembers().size());
        }
        if (h.home) {
            return Lng.str("Homebase");
        }
        SimpleTransformableSendableObject.EntityType t = typeOf(h.asset);
        return t != null ? t.getName() : Lng.str("Holding");
    }

    /**
     * The Resources Reorganized route a fleet row is running, read off the latest answer so it moves on
     * without the row being rebuilt. Null for anything else, or without that mod.
     */
    private static FactionAssets.Route routeOf(Holding h) {
        FactionAssets a = DiplomacyManager.client().getFactionAssets();
        return h.isFleet() && a != null ? a.route(h.fleet.dbid) : null;
    }

    /**
     * One line on where a route has got to, ahead of the fleet's own description.
     */
    private static String routeLine(FactionAssets.Route r) {
        String where = r.stop > 0 ? Lng.str("stop %s/%s, %s", r.stop, r.stops, r.station) : Lng.str("between schedules");
        long eta = r.etaLeft();
        String status = r.status + (r.paused ? Lng.str(" (paused)") : "")
                + (eta <= 0 ? "" : eta < 60000 ? Lng.str(", arriving in under a minute")
                : Lng.str(", about %s min out", (eta + 30000) / 60000));
        return r.isDelivery() ? Lng.str("Delivering %s - %s: %s", r.order, where, status)
                : Lng.str("On route, %s: %s", where, status);
    }

    private static String name(Holding h) {
        if (h.isBareClaim()) {
            return Lng.str("Claimed, nothing listed");
        }
        if (h.isFleet()) {
            return h.fleet.getName();
        }
        String n = h.asset.name == null ? "" : h.asset.name.trim();
        return n.isEmpty() ? h.asset.uid : n;
    }

    /**
     * Where a row actually sits. A fleet is wherever its flagship is; an empty one has no flagship and
     * is not in this list at all.
     */
    private static Vector3i sectorOf(Holding h) {
        if (h.isFleet()) {
            return h.fleet.getFlagShip().getSector();
        }
        return h.asset.sector;
    }

    /**
     * The system a row is in, read live for a fleet so a row does not keep claiming a fleet is where it
     * was when the list was last built. The strip down its edge still says what was true at that point;
     * it catches up on the next rebuild, which a fleet being added or ordered anywhere already triggers.
     */
    private static Vector3i systemOf(Holding h) {
        if (h.isFleet() && h.fleet.getFlagShip() != null) {
            return VoidSystem.getContainingSystem(h.fleet.getFlagShip().getSector(), new Vector3i());
        }
        return h.system;
    }

    @Override
    public void initColumns() {
        addColumn(Lng.str("Holding"), 3.0f, (o1, o2) -> name(o1).compareToIgnoreCase(name(o2)));
        addFixedWidthColumn(Lng.str("Kind"), 110, (o1, o2) -> kind(o1).compareToIgnoreCase(kind(o2)));
        addColumn(Lng.str("System"), 2.0f, (o1, o2) -> {
            //ours first, then by position, so the ones standing outside our own space group together
            if (o1.claimed != o2.claimed) {
                return o1.claimed ? -1 : 1;
            }
            int c = o1.system.compareTo(o2.system);
            return c != 0 ? c : name(o1).compareToIgnoreCase(name(o2));
        });

        addButton(new GUICallback() {
                      @Override
                      public boolean isOccluded() {
                          return !isActive();
                      }

                      @Override
                      public void callback(GUIElement callingGuiElement, MouseEvent event) {
                          if (event.pressedLeftMouse()) {
                              DiplomacyManager.client().requestFactionAssets(true);
                          }
                      }
                  }, new Object() {
                      @Override
                      public String toString() {
                          return asOf();
                      }
                  }, GUIHorizontalArea.HButtonType.BUTTON_GREY_MEDIUM, ControllerElement.FilterRowStyle.FULL,
                ControllerElement.FilterPos.BOTTOM);

        activeSortColumnIndex = 2;
    }

    /**
     * The button caption doubles as the staleness readout: there is one thing to press and one thing to
     * know, and both are "how old is this".
     */
    private String asOf() {
        if (!mayView()) {
            return Lng.str("RANK CANNOT SEE HOLDINGS");
        }
        FactionAssets a = DiplomacyManager.client().getFactionAssets();
        if (a == null) {
            return DiplomacyManager.client().isFactionAssetsPending()
                    ? Lng.str("ASKING...") : Lng.str("REFRESH");
        }
        return Lng.str("REFRESH");
    }

    @Override
    public void draw() {
        //keeps routes moving while the tab is open; throttled to one ask per 30s in the channel
        DiplomacyManager.client().requestFactionAssets(false);
        FactionAssets a = DiplomacyManager.client().getFactionAssets();
        long stamp = a == null ? -1 : a.queriedAt;
        if (stamp != builtFrom) {
            builtFrom = stamp;
            flagDirty();
        }
        super.draw();
    }

    @Override
    protected Collection<Holding> getElementList() {
        if (faction == null) {
            return Collections.emptyList();
        }
        //the one place that knows the list is being looked at. Rate-limited in the channel, and cached
        //again on the server, so a redraw storm cannot turn into a query storm
        DiplomacyManager.client().requestFactionAssets(false);

        Set<Vector3i> claimed = new ObjectOpenHashSet<Vector3i>();
        for (Vector3i sectorPos : faction.lastSystemSectors) {
            claimed.add(VoidSystem.getContainingSystem(sectorPos, new Vector3i()));
        }

        List<Holding> out = new ObjectArrayList<Holding>();
        Set<Vector3i> listed = new ObjectOpenHashSet<Vector3i>();
        String homeUid = faction.getHomebaseUID();
        FactionAssets a = DiplomacyManager.client().getFactionAssets();
        if (a != null) {
            for (FactionAssets.Asset asset : a.assets) {
                Vector3i system = VoidSystem.getContainingSystem(asset.sector, new Vector3i());
                listed.add(system);
                out.add(Holding.of(asset, system, !homeUid.isEmpty() && homeUid.equals(asset.uid),
                        claimed.contains(system)));
            }
        }
        //fleets need no query and no new right: the client is already sent the ones this player may
        //command, and the per-fleet access level decides that - the same rule the fleet menu uses, so
        //the two screens cannot disagree about who may see what
        for (Fleet f : client().getFleetManager().getAvailableFleetsClient()) {
            if (f.isEmpty() || f.getFlagShip() == null) {
                continue;
            }
            Vector3i system = VoidSystem.getContainingSystem(f.getFlagShip().getSector(), new Vector3i());
            listed.add(system);
            out.add(Holding.of(f, system, claimed.contains(system)));
        }
        for (Vector3i system : claimed) {
            if (!listed.contains(system)) {
                out.add(Holding.claim(system));
            }
        }
        return out;
    }

    @Override
    public void updateListEntries(GUIElementList mainList, Set<Holding> collection) {
        mainList.deleteObservers();
        mainList.addObserver(this);
        for (Holding h : collection) {
            GUITextOverlayTable nameText = new GUITextOverlayTable(10, 10, getState());
            GUITextOverlayTable kindText = new GUITextOverlayTable(10, 10, getState());
            GUITextOverlayTable systemText = new GUITextOverlayTable(10, 10, getState());

            nameText.setTextSimple(name(h));
            kindText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return kind(h);
                }
            });
            systemText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return systemName(systemOf(h));
                }
            });

            nameText.getPos().y = 5;
            kindText.getPos().y = 5;
            systemText.getPos().y = 5;

            GUIClippedRow nameRow = new GUIClippedRow(getState());
            nameRow.attach(nameText);
            GUIClippedRow kindRow = new GUIClippedRow(getState());
            kindRow.attach(kindText);
            GUIClippedRow systemRow = new GUIClippedRow(getState());
            systemRow.attach(systemText);

            HoldingRow r = new HoldingRow(getState(), h, nameRow, kindRow, systemRow);
            GUIAnchor expanded = expandedFor(h);
            r.expanded = new GUIElementList(getState());
            r.expanded.add(new GUIListElement(expanded, expanded, getState()));
            r.onInit();
            mainList.addWithoutUpdate(r);
        }
        mainList.updateDim();
    }

    /**
     * Where it is, and a way to go there. What it makes, holds and is short of needs the station loaded
     * to answer, which is a feature of its own rather than a line here.
     */
    private GUIAnchor expandedFor(Holding h) {
        //a route adds a line above the fleet's own description
        boolean routed = h.isFleet() && FleetRoutes.installed();
        GUIAnchor c = new GUIAnchor(getState(), 100, routed ? 76 : 56) {
            @Override
            public void draw() {
                setWidth(parent.getWidth());
                super.draw();
            }
        };
        GUITextOverlayTableInnerDescription where = new GUITextOverlayTableInnerDescription(10, 10, getState());
        where.autoWrapOn = c;
        where.setTextSimple(new Object() {
            @Override
            public String toString() {
                if (h.isBareClaim()) {
                    return Lng.str("System %s is claimed, and nothing of ours is listed standing in it. "
                            + "It costs faction points every period either way.", systemName(h.system));
                }
                if (h.isFleet()) {
                    FactionAssets.Route r = routeOf(h);
                    return (r != null ? routeLine(r) + "\n" : "") + Lng.str("%s ships under %s, %s, in sector %s of %s. Open to %s. Composition "
                                    + "and orders are the fleet menu's.",
                            h.fleet.getMembers().size(), h.fleet.getOwner(), h.fleet.missionString,
                            h.fleet.getFlagShip().getSector().toStringPure(), systemName(systemOf(h)),
                            h.fleet.getFactionAccessString());
                }
                return h.claimed
                        ? Lng.str("Sector %s, in our own system %s. What it makes and what it is short of "
                                + "needs the station loaded; that is not built yet.",
                        h.asset.sector.toStringPure(), systemName(h.system))
                        : Lng.str("Sector %s, in %s, which we have not claimed - no upkeep paid on it and "
                                + "no homebase protection over it.",
                        h.asset.sector.toStringPure(), systemName(h.system));
            }
        });
        where.setPos(4, 2, 0);

        GUITextButton navButton = new GUITextButton(getState(), 90, 24, GUITextButton.ColorPalette.OK,
                Lng.str("NAV TO"), new GUICallback() {
            @Override
            public boolean isOccluded() {
                return !isActive();
            }

            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse()) {
                    client().getController().getClientGameData().setWaypoint(target(h));
                }
            }
        });
        navButton.setPos(4, routed ? 48 : 28, 0);

        c.attach(where);
        c.attach(navButton);
        return c;
    }

    /**
     * Where "go there" means. A holding has a sector of its own; a bare claim only has a system, so the
     * waypoint is its centre.
     */
    private static Vector3i target(Holding h) {
        return h.isBareClaim() ? centreSector(h.system) : new Vector3i(sectorOf(h));
    }

    private class HoldingRow extends ScrollableTableList.Row {
        private final Holding holding;

        public HoldingRow(InputState state, Holding h, GUIElement... elements) {
            super(state, h, elements);
            holding = h;
            highlightSelect = true;
        }

        /**
         * Inside our borders, outside them, or the one that cannot be replaced.
         */
        @Override
        protected Vector4f getAccentColor() {
            if (holding.home) {
                return HOME;
            }
            return holding.claimed ? TERRITORY : OUTSIDE;
        }
    }
}
