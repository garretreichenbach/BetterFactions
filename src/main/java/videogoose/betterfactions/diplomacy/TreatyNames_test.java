package videogoose.betterfactions.diplomacy;

/**
 * Pins how an agreement gets its name.
 *
 * <p>Three things are worth pinning. A name is player-facing text that another faction's client
 * renders in a list row, so the sanitiser is a trust boundary like any other name a player types. A
 * generated name has to come out readable no matter what the signatories have built - a faction with
 * no home base, a system nobody has named, a modded type with no templates - because the alternative
 * is a treaty list full of blanks. And a template must not name an agreement after something that
 * did not happen: a concession is named for the system that was conceded, and a faction never
 * concedes its capital, so a concession template has to stand aside rather than reach for a home
 * system.</p>
 *
 * <p>Run via {@code gradlew apiSelfChecks}, or {@code java -ea} on this class.</p>
 */
public final class TreatyNames_test {

    private TreatyNames_test() {
    }

    public static void main(String[] args) {
        tokensAreFilledIn();
        aConcessionIsNeverNamedAfterACapital();
        aMissingHomeFallsBackToTheFactionName();
        theSameTreatyKeepsTheSameName();
        aTypeWithNoTemplatesUsesItsDisplayName();
        sanitizeRefusesWhatARowCannotShow();
        everyVanillaTypeNamesItself();
        relationshipWarsFillEveryToken();
        System.out.println("TreatyNames_test: OK");
    }

    private static void relationshipWarsFillEveryToken() {
        TreatyType war = TreatyTypeRegistry.get(TreatyTypeRegistry.WAR);
        for (String[] set : new String[][]{TreatyNames.INDEPENDENCE_WAR, TreatyNames.SUBJUGATION_WAR}) {
            for (long seed = 0; seed < set.length; seed++) {
                String name = TreatyNames.defaultName(war, set, seed, tokens());
                assert !name.contains("{") && name.length() <= TreatyNames.MAX_LENGTH : name;
            }
        }
    }

    private static void tokensAreFilledIn() {
        TreatyType vassalage = TreatyTypeRegistry.get(TreatyTypeRegistry.VASSALAGE);
        String name = TreatyNames.defaultName(vassalage, 0, tokens().ceded("Berenike"));
        assert !name.contains("{") : "a token was left unfilled: " + name;
    }

    /**
     * The rule the whole filter exists for. A home system is not something anyone hands over, so a
     * template naming a concession may only run when a system really changed hands - and it must name
     * <em>that</em> system, never the loser's capital.
     */
    private static void aConcessionIsNeverNamedAfterACapital() {
        TreatyType nap = TreatyTypeRegistry.get(TreatyTypeRegistry.NON_AGGRESSION);

        //nothing was ceded: no template that claims a concession may be chosen, at any seed
        for (long seed = 0; seed < 32; seed++) {
            String name = TreatyNames.defaultName(nap, seed, tokens());
            assert !name.contains("Concession") : "named a concession with nothing conceded: " + name;
            assert !name.contains("{") : name;
        }

        //a system did change hands: the concession names it, and never the home systems
        boolean sawConcession = false;
        for (long seed = 0; seed < 32; seed++) {
            String name = TreatyNames.defaultName(nap, seed, tokens().ceded("Tarsis"));
            if (name.contains("Concession")) {
                sawConcession = true;
                assert name.contains("Tarsis") : name;
                assert !name.contains("Xanthe") && !name.contains("Berenike")
                        : "a concession was named after a capital: " + name;
            }
        }
        assert sawConcession : "a ceded system never produced a concession name";
    }

    private static void aMissingHomeFallsBackToTheFactionName() {
        TreatyType nap = TreatyTypeRegistry.get(TreatyTypeRegistry.NON_AGGRESSION);
        for (long seed = 0; seed < 32; seed++) {
            String name = TreatyNames.defaultName(nap, seed, new TreatyNames.Tokens()
                    .from("Kepler Reach").to("Corvid Union").homes(null, "  "));
            assert !name.contains("{") : name;
            //a faction with no home base still gets a name, built from what it does have
            assert name.contains("Kepler Reach") || name.contains("Corvid Union") : name;
        }
    }

    /**
     * The seed is the signing time, which never changes, so the name a treaty is given on the day it
     * is signed is the name it still has after a restart.
     */
    private static void theSameTreatyKeepsTheSameName() {
        TreatyType alliance = TreatyTypeRegistry.get(TreatyTypeRegistry.ALLIANCE);
        String first = TreatyNames.defaultName(alliance, 1234567L, tokens());
        String again = TreatyNames.defaultName(alliance, 1234567L, tokens());
        assert first.equals(again) : first + " vs " + again;
        //a negative seed is a real possibility once times are subtracted; it must not index out of range
        assert !TreatyNames.defaultName(alliance, -9999L, tokens()).isEmpty();
    }

    private static void aTypeWithNoTemplatesUsesItsDisplayName() {
        TreatyType federation = TreatyTypeRegistry.get(TreatyTypeRegistry.FEDERATION);
        assert federation.getNameTemplates().length == 0 : "a federation is never signed directly";
        assert federation.getDisplayName().equals(TreatyNames.defaultName(federation, 0, tokens()));
        //a modded type that registers no templates must not produce an empty row either
        assert "Agreement".equals(TreatyNames.defaultName(null, 0, tokens()));
        //nor must a caller that knows nothing at all about the signing
        assert !TreatyNames.defaultName(federation, 0, null).isEmpty();
    }

    private static void sanitizeRefusesWhatARowCannotShow() {
        assert TreatyNames.sanitize(null) == null;
        assert TreatyNames.sanitize("   ") == null : "blank means 'name it for me', not a blank name";
        assert "Treaty of Xanthe".equals(TreatyNames.sanitize("  Treaty of Xanthe  "));
        //a name is drawn as one line in one column, so it may not carry its own line breaks
        assert "a b".equals(TreatyNames.sanitize("a\nb")) : TreatyNames.sanitize("a\nb");
        assert "a b".equals(TreatyNames.sanitize("a\tb"));

        StringBuilder long_ = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            long_.append('x');
        }
        assert TreatyNames.sanitize(long_.toString()).length() == TreatyNames.MAX_LENGTH;
    }

    /**
     * A shipped type with a template that misnames its own tokens would produce "The {toHom} Tribute"
     * in front of a player, and nothing else in the build would notice.
     */
    private static void everyVanillaTypeNamesItself() {
        for (TreatyType t : TreatyTypeRegistry.all()) {
            for (long seed = 0; seed < 32; seed++) {
                for (String ceded : new String[]{null, "Tarsis"}) {
                    String name = TreatyNames.defaultName(t, seed, tokens().ceded(ceded));
                    assert !name.isEmpty() : t.getId();
                    assert !name.contains("{") && !name.contains("}")
                            : t.getId() + " has an unfilled or misspelled token: " + name;
                    assert name.length() <= TreatyNames.MAX_LENGTH : t.getId() + ": " + name;
                }
            }
        }
    }

    private static TreatyNames.Tokens tokens() {
        return new TreatyNames.Tokens()
                .from("Kepler Reach").to("Corvid Union")
                .homes("Xanthe", "Berenike");
    }
}
