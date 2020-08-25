package dovtech.betterfactions.faction;

import org.schema.game.common.data.player.faction.FactionPermission;

public class FactionRank {

    private FactionPermission internalRank;

    public FactionRank(FactionPermission internalRank) {
        this.internalRank = internalRank;
    }

    public FactionPermission getInternalRank() {
        return internalRank;
    }
}
