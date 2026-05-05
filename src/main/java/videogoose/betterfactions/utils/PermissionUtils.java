package videogoose.betterfactions.utils;

import org.schema.game.common.data.player.faction.FactionPermission;
import videogoose.betterfactions.data.persistent.faction.FactionRank;
import videogoose.betterfactions.mixin.BetterMemberAccessor;

import java.util.ArrayList;

/**
 * Utility class for permission checking and management.
 *
 * @author TheDerpGamer
 * @since 04/15/2021
 */
public class PermissionUtils {

    private static final String[] permissionDatabase = {
            "chat.channel.[CHANNEL_TAG]",
            "entity.ship.[ENTITY_TAG].activate.[ELEMENT_TAG]", "entity.station.[ENTITY_TAG].activate.[ELEMENT_TAG]",
            "entity.ship.[ENTITY_TAG].edit", "entity.station.[ENTITY_TAG].edit",
            "entity.ship.[ENTITY_TAG].dock", "entity.ship.[ENTITY_TAG].undock", "entity.ship.[ENTITY_TAG].pilot",
            "manage.members.ranks", "manage.members.kick", "manage.members.invite", "manage.fp", "manage.info",
            "diplomacy.ally", "diplomacy.alliance", "diplomacy.war", "diplomacy.nap", "diplomacy.demand", "diplomacy.claims",
            "federation.create", "federation.invite", "federation.manage",
            "trade.offer", "trade.cancel"
    };

    public static ArrayList<String> getSubPermissions(String permission) {
        ArrayList<String> subPermissions = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        for(String s : permission.split("\\.")) if(s.contains("%") || s.contains("\"")) values.add(s.replace("\"", ""));
        permission = permission.replace("\"", "");
        for(String perm : permissionDatabase) {
            perm = convertTags(perm, values.toArray(new String[0]));
            if(permission.equals("*") || perm.contains(permission.substring(0, permission.indexOf('*')))) {
                subPermissions.add(perm);
            }
        }
        return subPermissions;
    }

    /**
     * Check if a faction member has the given permission via their custom rank.
     * Falls back to true for founders (role index 4).
     */
    public static boolean hasPermission(FactionPermission member, String... permissions) {
        if (member == null) return false;
        // Founders always have all permissions
        if (member.getRoleIndex() >= 4) return true;
        FactionRank rank = ((BetterMemberAccessor) member).getCustomRank();
        if (rank == null) return false;
        for (String perm : permissions) {
            boolean found = false;
            for (String rankPerm : rank.getPermissions()) {
                if (rankPerm.equals("*") || rankPerm.equals(perm)) {
                    found = true;
                    break;
                }
                // Wildcard matching: "diplomacy.*" matches "diplomacy.war"
                if (rankPerm.endsWith("*") && perm.startsWith(rankPerm.substring(0, rankPerm.length() - 1))) {
                    found = true;
                    break;
                }
                // [ANY] matching: "diplomacy.[ANY]" matches "diplomacy.war"
                if (rankPerm.contains("[ANY]")) {
                    String prefix = rankPerm.substring(0, rankPerm.indexOf("[ANY]"));
                    if (perm.startsWith(prefix)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public static String convertTags(String permission, String... values) {
        StringBuilder builder = new StringBuilder();
        String[] split = permission.split("\\.");
        int i = 0;
        for(String s : split) {
            if(s.contains("TAG") && i < values.length) {
                builder.append(values[i]);
                i ++;
            } else builder.append(s);
        }
        return builder.toString();
    }
}
