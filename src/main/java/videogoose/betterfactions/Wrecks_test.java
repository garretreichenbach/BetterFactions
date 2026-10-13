package videogoose.betterfactions;

import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.List;

/**
 * Pins the wreck bookkeeping: a reboot clears a wreck, knocked-off turrets are handed back once per
 * root, and wrecks survive a tag round trip.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class Wrecks_test {

    private Wrecks_test() {
    }

    public static void main(String[] args) {
        Wrecks w = new Wrecks();
        w.fromTag(tagOf("ENTITY_SHIP_a", "ENTITY_SHIP_b"));
        assert w.isWreck("ENTITY_SHIP_a") && w.isWreck("ENTITY_SHIP_b");
        w.onRebooted("ENTITY_SHIP_a");
        assert !w.isWreck("ENTITY_SHIP_a") : "rebooted is a ship again";

        Wrecks back = new Wrecks();
        back.fromTag(w.toTag());
        assert back.isWreck("ENTITY_SHIP_b") && !back.isWreck("ENTITY_SHIP_a");

        w.onTurretKnockedOff("t1", "root");
        w.onTurretKnockedOff("t2", "root");
        w.onTurretKnockedOff("t3", "other");
        assert w.takeKnockedOff("root").size() == 2;
        assert w.takeKnockedOff("root").isEmpty() : "handed back once";
        assert w.takeKnockedOff("other").equals(List.of("t3"));
        System.out.println("Wrecks_test: OK");
    }

    private static Tag tagOf(String... uids) {
        Tag[] t = new Tag[uids.length + 1];
        for (int i = 0; i < uids.length; i++) {
            t[i] = new Tag(Tag.Type.STRING, null, uids[i]);
        }
        t[uids.length] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }
}
