package videogoose.betterfactions.manager;

import api.mod.config.PersistentObjectUtil;
import api.utils.StarRunnable;
import org.schema.common.util.linAlg.Vector3i;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.data.diplomacy.claims.ClaimData;
import videogoose.betterfactions.data.diplomacy.claims.ClaimData.ClaimOrigin;
import videogoose.betterfactions.data.diplomacy.war.CasusBelli;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages territorial claims. Tracks which factions claim which systems,
 * detects contested claims, and generates BORDER_FRICTION casus belli.
 */
public class ClaimsManager {

    // Key: "x,y,z" system coords string → list of claims on that system
    private static final ConcurrentHashMap<String, List<ClaimData>> claimsBySystem = new ConcurrentHashMap<>();
    // Key: factionId → list of that faction's claims
    private static final ConcurrentHashMap<Integer, List<ClaimData>> claimsByFaction = new ConcurrentHashMap<>();
    private static boolean initialized;

    public static void initialize() {
        if (initialized) return;

        // Load persisted claims
        for (Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), ClaimData.class)) {
            if (obj instanceof ClaimData claim) {
                registerClaim(claim, false);
            }
        }

        // Periodic: decay unoccupied claims, detect contested claims
        new StarRunnable() {
            @Override
            public void run() {
                updateClaims();
            }
        }.runTimer(BetterFactions.getInstance(), 1200); // Every 60 seconds

        initialized = true;
    }

    private static String coordsKey(Vector3i coords) {
        return coords.x + "," + coords.y + "," + coords.z;
    }

    /**
     * Add a new claim for a faction on a system.
     */
    public static void addClaim(int factionId, Vector3i systemCoords, ClaimOrigin origin) {
        ClaimData claim = new ClaimData(factionId, systemCoords, origin);
        if (hasClaim(factionId, systemCoords)) return; // Already claimed
        registerClaim(claim, true);
        BetterFactions.getInstance().logInfo("Faction " + factionId + " claimed system " + systemCoords + " (" + origin.displayName + ")");
    }

    /**
     * Remove a faction's claim on a system.
     */
    public static void removeClaim(int factionId, Vector3i systemCoords) {
        String key = coordsKey(systemCoords);
        var systemClaims = claimsBySystem.get(key);
        if (systemClaims != null) {
            systemClaims.removeIf(c -> c.factionId == factionId);
            if (systemClaims.isEmpty()) claimsBySystem.remove(key);
        }
        var factionClaims = claimsByFaction.get(factionId);
        if (factionClaims != null) {
            factionClaims.removeIf(c -> c.systemCoords.equals(systemCoords));
        }
        // Remove from persistence
        for (Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), ClaimData.class)) {
            if (obj instanceof ClaimData claim && claim.factionId == factionId && claim.systemCoords.equals(systemCoords)) {
                PersistentObjectUtil.removeObject(BetterFactions.getInstance().getSkeleton(), obj);
                break;
            }
        }
        PersistentObjectUtil.save(BetterFactions.getInstance().getSkeleton());
    }

    /**
     * Check if a faction has a claim on a specific system.
     */
    public static boolean hasClaim(int factionId, Vector3i systemCoords) {
        var claims = claimsByFaction.get(factionId);
        if (claims == null) return false;
        return claims.stream().anyMatch(c -> c.systemCoords.equals(systemCoords));
    }

    /**
     * Get all claims for a faction.
     */
    public static List<ClaimData> getClaims(int factionId) {
        return claimsByFaction.getOrDefault(factionId, new ArrayList<>());
    }

    /**
     * Get all claims on a specific system.
     */
    public static List<ClaimData> getClaimsOnSystem(Vector3i systemCoords) {
        return claimsBySystem.getOrDefault(coordsKey(systemCoords), new ArrayList<>());
    }

    /**
     * Check if a system is contested (claimed by multiple factions).
     */
    public static boolean isContested(Vector3i systemCoords) {
        var claims = claimsBySystem.get(coordsKey(systemCoords));
        return claims != null && claims.size() > 1;
    }

    /**
     * Get all factions that contest claims with a given faction.
     */
    public static List<Integer> getContestingFactions(int factionId) {
        List<Integer> contesting = new ArrayList<>();
        var claims = claimsByFaction.get(factionId);
        if (claims == null) return contesting;

        for (ClaimData claim : claims) {
            var systemClaims = claimsBySystem.get(coordsKey(claim.systemCoords));
            if (systemClaims == null) continue;
            for (ClaimData other : systemClaims) {
                if (other.factionId != factionId && !contesting.contains(other.factionId)) {
                    contesting.add(other.factionId);
                }
            }
        }
        return contesting;
    }

    private static void registerClaim(ClaimData claim, boolean persist) {
        claimsBySystem.computeIfAbsent(coordsKey(claim.systemCoords), k -> new ArrayList<>()).add(claim);
        claimsByFaction.computeIfAbsent(claim.factionId, k -> new ArrayList<>()).add(claim);
        if (persist) {
            PersistentObjectUtil.addObject(BetterFactions.getInstance().getSkeleton(), claim);
            PersistentObjectUtil.save(BetterFactions.getInstance().getSkeleton());
        }
    }

    private static void updateClaims() {
        // Detect contested claims and update CONTESTED_CLAIMS status
        // The actual DiploStatusType.CONTESTED_CLAIMS modifier is applied by
        // FactionDiplomacyEntity.calculateStatus() checking ClaimsManager.getContestingFactions()

        // Decay fabricated claims that aren't occupied
        // (Claim strength decay is a future enhancement — for now claims persist indefinitely)
    }
}
