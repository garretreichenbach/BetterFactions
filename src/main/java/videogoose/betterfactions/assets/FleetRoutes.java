package videogoose.betterfactions.assets;

import api.mod.ModSkeleton;
import api.mod.StarLoader;

import java.util.List;
import java.util.Map;

/**
 * Soft hook into Resources Reorganized's fleet scheduler: which of a faction's fleets are running a route,
 * and which of those are carrying a logistics order.
 *
 * <p>Reflection rather than a compile dependency, so this mod builds and runs without it. It calls that
 * mod's {@code FleetRoutesApi.routes(int)}, which answers in plain maps so no class is shared across the two
 * class loaders; the keys are documented there. If that ever fails, this answers nothing and logs once
 * rather than taking the asset answer down with it.</p>
 */
public final class FleetRoutes {

    private static final String MOD_ID = "videogoose.resourcesreorganized";
    private static final String API = "videogoose.resourcesreorganized.api.FleetRoutesApi";

    private static boolean warned;

    private FleetRoutes() {
    }

    public static boolean installed() {
        ModSkeleton rr = StarLoader.getModFromModId(MOD_ID);
        return rr != null && rr.isEnabled();
    }

    /**
     * Server: the routes of every fleet the faction owns, empty without the mod.
     */
    public static void addTo(FactionAssets a) {
        a.routes.clear();
        if (!installed()) {
            return;
        }
        try {
            Class<?> api = Class.forName(API, true, StarLoader.getModFromModId(MOD_ID).getClassLoader());
            List<?> routes = (List<?>) api.getMethod("routes", int.class).invoke(null, a.factionId);
            for (Object o : routes) {
                Map<?, ?> m = (Map<?, ?>) o;
                a.routes.add(new FactionAssets.Route((Long) m.get("fleetDbId"), (String) m.get("order"),
                        (String) m.get("status"), (Integer) m.get("stop"), (Integer) m.get("stops"),
                        (String) m.get("station"), (Boolean) m.get("paused"), (Long) m.get("etaMs")));
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            a.routes.clear();
            if (!warned) {
                warned = true;
                System.err.println("[BetterFactions] Resources Reorganized is installed but its fleet routes could not be read: " + e);
            }
        }
    }
}
