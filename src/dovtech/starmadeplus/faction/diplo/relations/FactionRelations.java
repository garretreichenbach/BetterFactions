package dovtech.starmadeplus.faction.diplo.relations;

import org.schema.game.common.data.player.faction.Faction;
import java.util.ArrayList;

public class FactionRelations {

    private Faction from;
    private Faction to;
    private ArrayList<RelationModifier> modifiers;
    private int opinion;

    public FactionRelations(Faction from, Faction to) {
        this.from = from;
        this.to = to;
        this.opinion = 0;
        modifiers = new ArrayList<>();
    }

    public Faction getFrom() {
        return from;
    }

    public Faction getTo() {
        return to;
    }

    public int getOpinion() {
        this.opinion = 0;
        for(RelationModifier modifier : modifiers) {
            this.opinion = this.opinion + modifier.relationScore;
        }
        return this.opinion;
    }

    public RelationDisplay getDisplay() {
        int currentOpinion = getOpinion();
        if(currentOpinion <= -300) {
            return RelationDisplay.HATED;
        } else if(currentOpinion <= -150) {
            return RelationDisplay.HOSTILE;
        } else if(currentOpinion <= -70) {
            return RelationDisplay.POOR;
        } else if(currentOpinion <= -30) {
            return RelationDisplay.WARY;
        } else if(currentOpinion <= 30) {
            return RelationDisplay.NEUTRAL;
        } else if(currentOpinion <= 70) {
            return RelationDisplay.COOL;
        } else if(currentOpinion <= 150) {
            return RelationDisplay.FRIENDLY;
        } else if(currentOpinion <= 300) {
            return RelationDisplay.VERY_FRIENDLY;
        } else {
            return RelationDisplay.EXCELLENT;
        }
    }

    public enum RelationModifier {
        //Positive
        MEMBER_OF_PACT(150),
        ALLIED(130),
        FOUGHT_TOGETHER(50),
        TRADE_PARTNERS(30),
        DEFENDED_COALITION(70),
        SAME_ALLY(50),
        SAME_ENEMY(30),
        PEACEKEEPER(85),

        //Negative
        RECENTLY_AT_WAR(-30),
        THREATENING(-50),
        AGGRESSIVE(-40),
        ALLIED_WITH_ENEMY(-50),
        TRADING_WITH_ENEMY(-30),
        AT_WAR(-150),
        CLAIMED_OUR_TERRITORY(-65),
        CONTROLS_OUR_TERRITORY(-100);

        public int relationScore;

        RelationModifier(int relationScore) {
            this.relationScore = relationScore;
        }
    }

    public enum RelationDisplay {
        EXCELLENT(8),
        VERY_FRIENDLY(7),
        FRIENDLY(6),
        COOL(5),
        NEUTRAL(4),
        WARY(3),
        POOR(2),
        HOSTILE(1),
        HATED(0);

        public int spriteID;

        RelationDisplay(int spriteID) {
            this.spriteID = spriteID;
        }
    }
}
