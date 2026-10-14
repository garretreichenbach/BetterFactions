package videogoose.betterfactions.civics;

import org.schema.game.common.controller.generator.names.NameConstruction.DefaultNameParts;
import org.schema.game.common.controller.generator.names.NameConstruction.NameGen;
import org.schema.game.common.controller.generator.names.NameConstruction.NamingTemplate;
import org.schema.schine.common.language.Lng;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Who the Council is, by the faction's civics: its name, its lore, what its seats are called and how its
 * councilors speak. Words only; what a seat asks for is its role's directives ({@link Directive}).
 *
 * <p>todo: tables in code, not data; move to {@code council.xml} when servers want their own.</p>
 */
public final class CouncilLore {

    private CouncilLore() {
    }

    public enum Flavour {
        COUNCIL, SHADOW, BOARD, JUNTA
    }

    /** The styles the faction's civics allow: each shaping civic's, in slot order, then the plain Council. */
    public static java.util.List<Flavour> available(FactionCivics c) {
        java.util.List<Flavour> out = new java.util.ArrayList<>();
        for (String id : c.civics) {
            Flavour f = CivicRegistry.RELIGIOUS.equals(id) ? Flavour.SHADOW
                    : CivicRegistry.CORPORATE_CHAINS.equals(id) ? Flavour.BOARD
                    : CivicRegistry.WARRIOR_CODE.equals(id) ? Flavour.JUNTA : null;
            if (f != null && !out.contains(f)) {
                out.add(f);
            }
        }
        out.add(Flavour.COUNCIL);
        return out;
    }

    /** The style the faction chose, while its civic is still held; otherwise the first shaping civic's. */
    public static Flavour of(FactionCivics c, String chosen) {
        java.util.List<Flavour> allowed = available(c);
        for (Flavour f : allowed) {
            if (f.name().equals(chosen)) {
                return f;
            }
        }
        return allowed.get(0);
    }

    /** Whether the chosen style still stands: blank (the default) always does. */
    public static boolean allowed(FactionCivics c, String chosen) {
        return chosen.isEmpty() || of(c, chosen).name().equals(chosen);
    }

    /** What shapes it, for the badge: the civic's name, or none. */
    public static String shapedBy(Flavour f) {
        return switch (f) {
            case SHADOW -> CivicRegistry.nameOf(CivicRegistry.RELIGIOUS);
            case BOARD -> CivicRegistry.nameOf(CivicRegistry.CORPORATE_CHAINS);
            case JUNTA -> CivicRegistry.nameOf(CivicRegistry.WARRIOR_CODE);
            case COUNCIL -> Lng.str("no civic");
        };
    }

    public static String title(Flavour f) {
        return switch (f) {
            case SHADOW -> Lng.str("The Dark Shadow Council");
            case BOARD -> Lng.str("The Board");
            case JUNTA -> Lng.str("Military Junta");
            case COUNCIL -> Lng.str("The Council");
        };
    }

    public static String lore(Flavour f) {
        return switch (f) {
            case SHADOW -> Lng.str("We do not meet the Shadow Council. We commune with it: psionic beings from beyond the veil of the world, who speak through our priests in trance.");
            case BOARD -> Lng.str("Directors with a seat for every major shareholder. They meet each quarter and expect returns.");
            case JUNTA -> Lng.str("Old captains who earned their seats in battle. They meet around a fire and speak plainly.");
            case COUNCIL -> Lng.str("Advisors from the great houses of our faction meet in the high chamber.");
        };
    }

    /** A seat's title, by archetype. */
    public static String seat(Flavour f, int archetype) {
        String[] t = switch (f) {
            case SHADOW -> new String[]{Lng.str("The Flame"), Lng.str("The Still Water"), Lng.str("The Whisper"), Lng.str("The Watcher"),
                    Lng.str("The Hollow One"), Lng.str("The Storm"), Lng.str("The Voice"),
                    Lng.str("The Wanderer"), Lng.str("The Keeper"), Lng.str("The Inquisitor")};
            case BOARD -> new String[]{Lng.str("Head of Security"), Lng.str("VP of Partnerships"), Lng.str("Acquisitions"), Lng.str("Compliance"),
                    Lng.str("Chief Financial Officer"), Lng.str("Head of Operations"), Lng.str("Chief Executive"),
                    Lng.str("VP of Expansion"), Lng.str("General Counsel"), Lng.str("Head of Litigation")};
            case JUNTA -> new String[]{Lng.str("Warlord"), Lng.str("Peace-Speaker"), Lng.str("Raidmaster"), Lng.str("Elder"),
                    Lng.str("Quartermaster"), Lng.str("Champion"), Lng.str("Oath-Binder"),
                    Lng.str("Pathfinder"), Lng.str("Lawspeaker"), Lng.str("Avenger")};
            case COUNCIL -> new String[]{Lng.str("Marshal"), Lng.str("Envoy"), Lng.str("Spymaster"), Lng.str("Steward"),
                    Lng.str("Treasurer"), Lng.str("Admiral"), Lng.str("Chancellor"),
                    Lng.str("Surveyor"), Lng.str("Archivist"), Lng.str("Inquisitor")};
        };
        return t[Math.floorMod(archetype, t.length)];
    }

    /** A seat's traits, by archetype: flavour only, for now. */
    public static String traits(Flavour f, int archetype) {
        String[] t = switch (f) {
            case SHADOW -> new String[]{Lng.str("Psionic, Wrathful"), Lng.str("Psionic, Serene"), Lng.str("Psionic, Inscrutable"), Lng.str("Psionic, Ancient"),
                    Lng.str("Psionic, Hungering"), Lng.str("Psionic, Voidspeaker"), Lng.str("Psionic, All-Knowing"),
                    Lng.str("Psionic, Restless"), Lng.str("Psionic, Patient"), Lng.str("Psionic, Zealous")};
            case BOARD -> new String[]{Lng.str("Aggressive"), Lng.str("Networker"), Lng.str("Ruthless"), Lng.str("Cautious"),
                    Lng.str("Miser"), Lng.str("Bold"), Lng.str("Shrewd"), Lng.str("Ambitious"), Lng.str("Meticulous"), Lng.str("Litigious")};
            case JUNTA -> new String[]{Lng.str("Warmonger"), Lng.str("Honourable"), Lng.str("Ruthless"), Lng.str("Ancient, Cautious"),
                    Lng.str("Miser"), Lng.str("Bold, Glory-seeker"), Lng.str("Honourable"), Lng.str("Restless"), Lng.str("Honourable, Stern"), Lng.str("Vengeful")};
            case COUNCIL -> new String[]{Lng.str("Warmonger, Impatient"), Lng.str("Idealist"), Lng.str("Paranoid, Ruthless"), Lng.str("Cautious"),
                    Lng.str("Miser"), Lng.str("Bold"), Lng.str("Shrewd"), Lng.str("Ambitious"), Lng.str("Meticulous"), Lng.str("Zealous")};
        };
        return t[Math.floorMod(archetype, t.length)];
    }

    /** What the councilor says of their agenda; {@code target} is a faction name, or blank. */
    public static String quote(Flavour f, Council.Objective o, String target) {
        return switch (f) {
            case SHADOW -> switch (o) {
                case DESTROY_SHIPS -> Lng.str("%s burns bright and wrong. Dim them.", target);
                case DECLARE_WAR -> Lng.str("We have dreamt of %s in flames. Make the dream true.", target);
                case SIGN_TREATY -> Lng.str("Let %s come to the water. Bind them in peace.", target);
                case BREAK_TREATY -> Lng.str("%s trades with the forges. Sever the thread.", target);
                case SANCTION -> Lng.str("Close your hand to %s. Let them hunger.", target);
                case KEEP_PEACE -> Lng.str("Be still. We are listening.");
                case STAY_SOLVENT -> Lng.str("Owe nothing. Debt is a door the void walks through.");
                case CLAIM_SYSTEMS -> Lng.str("The stars call to us. Answer them.");
                case KILL_PLAYERS -> Lng.str("The pilots of %s dream too loudly. Silence them.", target);
            };
            case BOARD -> switch (o) {
                case DESTROY_SHIPS -> Lng.str("%s is eating our margins. Remove them from the market.", target);
                case DECLARE_WAR -> Lng.str("A hostile takeover of %s is overdue.", target);
                case SIGN_TREATY -> Lng.str("A deal with %s opens their markets to us.", target);
                case BREAK_TREATY -> Lng.str("The %s contract no longer serves us. Terminate it.", target);
                case SANCTION -> Lng.str("Price %s out of our markets.", target);
                case KEEP_PEACE -> Lng.str("A quiet quarter. The shareholders are nervous.");
                case STAY_SOLVENT -> Lng.str("Close the quarter in the black.");
                case CLAIM_SYSTEMS -> Lng.str("Untapped markets. Plant our flag.");
                case KILL_PLAYERS -> Lng.str("%s's key personnel are a liability. For them.", target);
            };
            case JUNTA -> switch (o) {
                case DESTROY_SHIPS -> Lng.str("%s mocks us. Answer with fire.", target);
                case DECLARE_WAR -> Lng.str("Our blades rust. %s will do.", target);
                case SIGN_TREATY -> Lng.str("Even wolves rest. Swear an oath with %s.", target);
                case BREAK_TREATY -> Lng.str("%s pays us in promises. Break with them.", target);
                case SANCTION -> Lng.str("Starve %s of our trade before we starve them of air.", target);
                case KEEP_PEACE -> Lng.str("Let the young ones heal.");
                case STAY_SOLVENT -> Lng.str("An army in debt fights for someone else.");
                case CLAIM_SYSTEMS -> Lng.str("New ground for new halls.");
                case KILL_PLAYERS -> Lng.str("Bring me the heads of %s's captains.", target);
            };
            case COUNCIL -> switch (o) {
                case DESTROY_SHIPS -> Lng.str("%s grows bolder. Strike their ships.", target);
                case DECLARE_WAR -> Lng.str("%s will not stop until we make them.", target);
                case SIGN_TREATY -> Lng.str("Every war we avoid is a fleet we keep. Sign with %s.", target);
                case BREAK_TREATY -> Lng.str("%s trades with our enemies. Cut them off.", target);
                case SANCTION -> Lng.str("%s will not listen. Make them feel it in their markets.", target);
                case KEEP_PEACE -> Lng.str("Give the colonies time to breathe.");
                case STAY_SOLVENT -> Lng.str("Debt is a slow defeat.");
                case CLAIM_SYSTEMS -> Lng.str("Our borders are too narrow. Claim more systems.");
                case KILL_PLAYERS -> Lng.str("Ships are rebuilt. Pilots are not. Hunt those of %s.", target);
            };
        };
    }

    /**
     * A new councilor's name, from the engine's own name generator: one alien word for the Shadow
     * Council's beings, a Greco-Roman first and last name for everyone else.
     */
    public static String name(Flavour f) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return f == Flavour.SHADOW ? word(DefaultNameParts.cheesyScifi, r) : word(DefaultNameParts.grecoRoman, r) + " " + word(DefaultNameParts.grecoRoman, r);
    }

    /**
     * One generated word. The templates are the engine's star-naming ones, so a name that came out with a
     * star's append ("Core", "Nova", a Greek letter) is drawn again. The templates are shared with system
     * naming and break when cloned, so they are used as they are.
     */
    private static String word(NamingTemplate t, ThreadLocalRandom r) {
        for (int i = 0; i < 8; i++) {
            try {
                String n = NameGen.build(t, r);
                if (n != null && !n.isEmpty() && n.indexOf(' ') < 0 && !n.startsWith("#")) {
                    return n;
                }
            } catch (RuntimeException ignored) {
                //a template that cannot build: fall through to the plain names
            }
        }
        return FALLBACK[r.nextInt(FALLBACK.length)];
    }

    private static final String[] FALLBACK = {"Oranis", "Corr", "Vale", "Rho", "Mirren", "Quell", "Thaal", "Drest", "Ommun", "Saryn", "Kett", "Brask"};
}
