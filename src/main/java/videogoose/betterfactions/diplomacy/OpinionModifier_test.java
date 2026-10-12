package videogoose.betterfactions.diplomacy;

import videogoose.betterfactions.diplomacy.DiplomacyAction.DiplActionType;
import videogoose.betterfactions.diplomacy.DiplomacyEntity.DiplStatusType;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

/**
 * Pins the opinion ledger's tag format.
 *
 * <p>Two things are being held in place here.</p>
 *
 * <p>First, the modifiers used to drop the field tracking how long they had been applied - the tag
 * simply did not contain it. The only symptom is that a condition like "they have been peaceful for
 * 15 minutes" silently restarts its clock on every server boot, and the shipped NPC configs gate
 * their peace and alliance offers on exactly those conditions.</p>
 *
 * <p>Second, every diplomacy tag used to key its enums by {@code ordinal()}. That made declaration
 * order load-bearing: inserting a value anywhere but the end reassigned every value after it in
 * already-saved worlds. Since growing this vocabulary is the point of the diplomacy work, the tags
 * key by name, and an unrecognised name is skipped rather than resolved to whatever constant now
 * sits at that index. {@link #resolvesByNameNotPosition()} is the check that would fail if anyone
 * moved it back.</p>
 *
 * <p>Run via {@code gradlew apiSelfChecks}, or {@code java -ea} on this class.</p>
 */
public final class OpinionModifier_test {

    private OpinionModifier_test() {
    }

    public static void main(String[] args) {
        staticModifierRoundTrips();
        turnModifierRoundTrips();
        actionRoundTripsWithSubject();
        actionsWithDifferentSubjectsDoNotCollide();
        resolvesByNameNotPosition();
        legacyTagsStillLoad();
        System.out.println("OpinionModifier_test: OK");
    }

    private static void staticModifierRoundTrips() {
        OpinionStaticModifier m = new OpinionStaticModifier();
        m.type = DiplStatusType.NON_AGGRESSION;
        m.value = -42;
        m.elapsedTimeInactive = 1234;
        m.totalTimeApplied = 900_000;

        OpinionStaticModifier back = new OpinionStaticModifier();
        assert back.fromTag(m.toTag());

        assert back.type == m.type : back.type;
        assert back.value == m.value : back.value;
        assert back.elapsedTimeInactive == m.elapsedTimeInactive : back.elapsedTimeInactive;
        // the field that used to be dropped
        assert back.totalTimeApplied == m.totalTimeApplied : back.totalTimeApplied;
        assert back.getValue() == -42 : back.getValue();
        assert back.isStatic();
    }

    private static void turnModifierRoundTrips() {
        OpinionTurnModifier m = new OpinionTurnModifier();
        m.type = DiplActionType.ATTACK;
        m.pointsPerTurn = -7;
        m.totalElapsedTime = 55_000;
        m.elapsedTime = 3_000;

        OpinionTurnModifier back = new OpinionTurnModifier();
        assert back.fromTag(m.toTag());

        assert back.type == m.type : back.type;
        assert back.pointsPerTurn == m.pointsPerTurn : back.pointsPerTurn;
        assert back.totalElapsedTime == m.totalElapsedTime : back.totalElapsedTime;
        // the field that used to be dropped
        assert back.elapsedTime == m.elapsedTime : back.elapsedTime;
        assert !back.isStatic();
    }

    private static void actionRoundTripsWithSubject() {
        DiplomacyAction a = new DiplomacyAction();
        a.type = DiplActionType.ATTACK;
        a.counter = 3;
        a.timeDuration = 60_000;

        DiplomacyAction back = new DiplomacyAction();
        assert back.fromTag(a.toTag());
        assert back.type == DiplActionType.ATTACK : back.type;
        assert back.counter == 3 : back.counter;
        assert back.subject == null : back.subject;
        assert back.key().equals("ATTACK") : back.key();

        a.subject = "starmade:alliance";
        DiplomacyAction back2 = new DiplomacyAction();
        assert back2.fromTag(a.toTag());
        assert "starmade:alliance".equals(back2.subject) : back2.subject;
        assert back2.key().equals("ATTACK:starmade:alliance") : back2.key();
    }

    /**
     * The reason the action map is string-keyed: a generic treaty action has to be able to say
     * <em>which</em> treaty, or every treaty type shares one counter and one decay clock.
     */
    private static void actionsWithDifferentSubjectsDoNotCollide() {
        String a = DiplomacyAction.key(DiplActionType.ALLIANCE_CANCEL, "starmade:alliance");
        String b = DiplomacyAction.key(DiplActionType.ALLIANCE_CANCEL, "starmade:non_aggression");
        String plain = DiplomacyAction.key(DiplActionType.ALLIANCE_CANCEL, null);
        assert !a.equals(b) : a;
        assert !a.equals(plain) : a;
        assert !b.equals(plain) : b;
    }

    /**
     * Resolution must follow the written name, not the position it happened to occupy. Written with
     * a name that is deliberately not the first constant, so a positional reader would disagree.
     */
    private static void resolvesByNameNotPosition() {
        DiplStatusType picked = DiplStatusType.NON_AGGRESSION;
        assert picked.ordinal() != 0 : "pick a constant that is not first, or this proves nothing";

        OpinionStaticModifier m = new OpinionStaticModifier();
        m.type = picked;
        m.value = 5;
        OpinionStaticModifier back = new OpinionStaticModifier();
        assert back.fromTag(m.toTag());
        assert back.type == picked : back.type;

        // a name from a vocabulary this build does not have: skipped, not mis-resolved
        OpinionStaticModifier unknown = new OpinionStaticModifier();
        assert !unknown.fromTag(statusTagNamed("SOME_MODDED_STATUS")) : unknown.type;
        assert unknown.type == null : unknown.type;
    }

    /**
     * A v0/v1 tag has no trailing time field; it must load with that field at zero, not blow up.
     */
    private static void legacyTagsStillLoad() {
        Tag legacyStatic = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) 0),
                new Tag(Tag.Type.INT, null, 11),
                new Tag(Tag.Type.LONG, null, 22L),
                new Tag(Tag.Type.BYTE, null, (byte) DiplStatusType.IN_WAR.ordinal()),
                FinishTag.INST,
        });
        OpinionStaticModifier s = new OpinionStaticModifier();
        assert s.fromTag(legacyStatic);
        assert s.type == DiplStatusType.IN_WAR : s.type;
        assert s.value == 11 : s.value;
        assert s.totalTimeApplied == 0 : s.totalTimeApplied;

        Tag legacyTurn = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) 0),
                new Tag(Tag.Type.INT, null, -3),
                new Tag(Tag.Type.LONG, null, 44L),
                new Tag(Tag.Type.BYTE, null, (byte) DiplActionType.MINING.ordinal()),
                FinishTag.INST,
        });
        OpinionTurnModifier t = new OpinionTurnModifier();
        assert t.fromTag(legacyTurn);
        assert t.type == DiplActionType.MINING : t.type;
        assert t.pointsPerTurn == -3 : t.pointsPerTurn;
        assert t.elapsedTime == 0 : t.elapsedTime;
    }

    private static Tag statusTagNamed(String name) {
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.BYTE, null, (byte) 2),
                new Tag(Tag.Type.INT, null, 1),
                new Tag(Tag.Type.LONG, null, 0L),
                new Tag(Tag.Type.STRING, null, name),
                new Tag(Tag.Type.LONG, null, 0L),
                FinishTag.INST,
        });
    }
}
