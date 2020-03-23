package net.dovtech.betterfactions.entity.station.module.mining;

import net.dovtech.betterfactions.entity.station.BetterStation;
import net.dovtech.betterfactions.entity.station.module.StationModule;

public class GasMining extends StationModule {

    public GasMining() {
        super("Gas Mining Module", 5, new int[] { 30000, 80000, 150000, 300000, 500000 }); //Test values, not final
    }

    @Override
    public void activate(BetterStation station) {

    }
}
