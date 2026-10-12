package videogoose.betterfactions.diplomacy;

public abstract class OpinionModifier {

    public abstract String getName();

    public abstract boolean isStatic();

    public abstract int getValue();

    public int getFrequency() {
        return 0;
    }

}
