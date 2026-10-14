package videogoose.betterfactions.civics;

import org.schema.schine.resource.tag.Tag;

/**
 * Pins the Council's rules that need no server: which agendas clash, how many seats a faction earns,
 * how progress reads, and that a Council survives a save.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class Council_test {

    private Council_test() {
    }

    private static Council.Agenda agenda(Council.Objective o, int target) {
        Council.Agenda a = new Council.Agenda();
        a.objective = o;
        a.target = target;
        return a;
    }

    public static void main(String[] args) {
        Council.Agenda war = agenda(Council.Objective.DECLARE_WAR, 7);
        Council.Agenda sign = agenda(Council.Objective.SIGN_TREATY, 7);
        Council.Agenda signOther = agenda(Council.Objective.SIGN_TREATY, 8);
        Council.Agenda peace = agenda(Council.Objective.KEEP_PEACE, 0);
        Council.Agenda destroy = agenda(Council.Objective.DESTROY_SHIPS, 8);
        assert Council.clash(war, sign) && Council.clash(sign, war) : "war and peace with one faction";
        assert !Council.clash(war, signOther) : "different factions do not clash";
        assert Council.clash(peace, war) && Council.clash(agenda(Council.Objective.BREAK_TREATY, 3), peace) : "any act of war breaks the peace";
        assert !Council.clash(peace, destroy) : "fighting a war already declared keeps the peace";
        assert Council.clash(destroy, signOther);
        assert !Council.clash(war, null);

        assert Council.aiSeats(FactionCivics.STARTING_SLOTS) == 3;
        assert Council.aiSeats(FactionCivics.STARTING_SLOTS + 1) == 4;
        assert Council.aiSeats(FactionCivics.STARTING_SLOTS + 9) == 5 : "members fill the rest";

        Council.Agenda timed = agenda(Council.Objective.STAY_SOLVENT, 0);
        timed.count = 3;
        timed.issued = 0;
        assert timed.progress(Council.DAY_MS * 2 + 5) == 2 && timed.progress(Council.DAY_MS * 9) == 3 : "whole days held, capped";
        destroy.count = 3;
        destroy.done = 5;
        assert destroy.progress(0) == 3;
        destroy.penalty = 250;
        assert destroy.refuseCost() == 100 : "refusing costs two fifths of failing";

        Council.State st = new Council.State();
        Council.Seat ai = new Council.Seat();
        ai.archetype = 0;
        ai.name = "Vex Oranis";
        ai.favour = 37;
        ai.agenda = war;
        war.backedBy = "vael";
        Council.Seat held = new Council.Seat();
        held.archetype = 3;
        held.member = "Vael";
        held.name = "Vael";
        held.options.add(sign);
        st.seats.add(ai);
        st.seats.add(held);
        st.fulfilled = 4;
        st.name = "The Inner Circle";
        Tag tag = st.toTag();
        Council.State back = Council.State.fromTag(tag);
        assert back.fulfilled == 4 && back.seats.size() == 2;
        assert back.name.equals("The Inner Circle") : "a renamed Council keeps its name";
        assert back.pool.isEmpty() && back.poolDay == -1 : "no pool drawn yet";
        st.pool.addAll(Council.drawPool(0));
        st.poolDay = 3;
        Council.State drawn = Council.State.fromTag(st.toTag());
        assert drawn.pool.size() == Council.POOL_SIZE && drawn.poolDay == 3 : "the day's pool survives a save";
        for (Council.Seat c : drawn.pool) {
            assert !c.held() && c.favour >= 35 && c.favour <= 70 && c.archetype >= 0 && c.archetype < Council.POOLS.length;
        }
        assert back.seats.get(0).favour == 37 && back.seats.get(0).agenda.objective == Council.Objective.DECLARE_WAR
                && back.seats.get(0).agenda.backedBy.equals("vael") : "an AI seat survives a save";
        assert back.seats.get(1).held() && back.seats.get(1).agenda == null && back.seats.get(1).options.size() == 1 : "a member seat and its options survive";
        assert back.heldBy("VAEL") == back.seats.get(1) : "seats are found by name, any case";
        assert Council.clashWith(st, ai) == null && Council.clashWith(back, back.seats.get(0)) == null;
        held.agenda = sign;
        assert Council.clashWith(st, ai) == held : "a member's agenda can clash too";
        //relations decide what a councilor asks: nudges, not orders
        java.util.function.Predicate<String> none = t -> false;
        String embargo = videogoose.betterfactions.diplomacy.TreatyTypeRegistry.EMBARGO;
        assert Council.targetType(Council.Objective.DECLARE_WAR, -300, false, none, true) == null : "poor relations are no cause for war";
        assert Council.targetType(Council.Objective.DECLARE_WAR, -600, false, none, true) != null : "very bad ones are";
        assert Council.targetType(Council.Objective.DECLARE_WAR, -600, false, none, false) == null : "not while a treaty binds us";
        assert embargo.equals(Council.targetType(Council.Objective.SANCTION, -300, false, none, true)) : "poor relations: an embargo first";
        assert Council.targetType(Council.Objective.SANCTION, -300, false, embargo::equals, true)
                .equals(videogoose.betterfactions.diplomacy.TreatyTypeRegistry.TARIFF) : "then a tariff";
        assert Council.targetType(Council.Objective.SANCTION, -100, false, none, true) == null : "merely cool relations: no sanction";
        assert Council.targetType(Council.Objective.SIGN_TREATY, -50, false, none, true) == null : "no treaties with those who dislike us";
        assert Council.targetType(Council.Objective.SIGN_TREATY, 100, false, embargo::equals, true) == null : "nor while we embargo them";
        assert Council.targetType(Council.Objective.SIGN_TREATY, 100, false, none, true) != null;
        assert Council.targetType(Council.Objective.DESTROY_SHIPS, 500, false, none, true) == null : "ships only of those we are at war with";
        assert Council.weight(Council.Objective.DECLARE_WAR, -1500) > Council.weight(Council.Objective.DECLARE_WAR, -550) : "the worse, the likelier";
        assert Council.pick(java.util.List.of(1, 5, 1), 0) == 0 && Council.pick(java.util.List.of(1, 5, 1), 3) == 1 && Council.pick(java.util.List.of(1, 5, 1), 6) == 2;

        //perks: a devoted councilor or a member's seat gives its archetype's perk
        Council council = new Council();
        Council.State perks = new Council.State();
        for (int i = 0; i < 5; i++) {
            Council.Seat s = new Council.Seat();
            s.archetype = i;
            perks.seats.add(s);
        }
        perks.seats.get(Council.WAR).favour = 80;
        perks.seats.get(Council.TREASURY).member = "Vael";
        Council.Seat added = new Council.Seat();
        added.archetype = 5;
        added.member = "Doran";
        perks.seats.add(added);
        council.fromTag(new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.INT, null, 9), perks.toTag(), org.schema.schine.resource.tag.FinishTag.INST}),
                org.schema.schine.resource.tag.FinishTag.INST}));
        assert council.cost(9, Council.WAR, 400) == 300 : "devoted: war costs a quarter less";
        assert council.cost(9, Council.PEACE, 400) == 400 : "a content councilor gives nothing";
        assert council.cost(8, Council.WAR, 400) == 400 : "another faction's Council is not ours";
        assert council.perk(9, Council.GLORY) && council.scale(9, Council.GLORY, 100f) == 90f : "a member's seat always gives its perk";
        assert council.extraSeats(9, FactionCivics.STARTING_SLOTS) == 3 : "six seats, three earned";
        FactionCivics civics = new FactionCivics();
        civics.institutions.add(CivicRegistry.COUNCIL);
        civics.slotsEarned = FactionCivics.STARTING_SLOTS + 2;
        assert council.institutionUpkeep(9, civics, 10) == (10 + 10) * 0.85f : "one institution, one extra seat, less the treasury's 15%";
        System.out.println("Council_test: OK");
    }
}
