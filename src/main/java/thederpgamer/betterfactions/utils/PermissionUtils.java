package thederpgamer.betterfactions.utils;

import java.util.ArrayList;

/**
 * PermissionUtils
 * <Description>
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
            "manage.members.ranks", "manage.members.kick", "manage.members.invite", "manage.fp", "manage.info"
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
