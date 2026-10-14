package videogoose.betterfactions.civics;

import org.schema.common.util.linAlg.Vector3i;

/**
 * Pins the corporations' money rules: charters, the HQ's reach, the cut and bonus on a sale, the dividend
 * and its cap, and that a faction's corporations survive a save.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class Corporations_test {

    private Corporations_test() {
    }

    public static void main(String[] args) {
        assert Corporations.freeCharters(false) == 1 && Corporations.freeCharters(true) == 2;
        assert Corporations.extraCharters(3, false) == 2 && Corporations.extraCharters(3, true) == 1 && Corporations.extraCharters(0, true) == 0;

        //the HQ reaches three systems (16 sectors each) every way, negative coordinates included
        assert Corporations.near(new Vector3i(0, 0, 0), new Vector3i(63, 0, 0)) : "three systems over";
        assert !Corporations.near(new Vector3i(0, 0, 0), new Vector3i(64, 0, 0)) : "four is too far";
        assert Corporations.near(new Vector3i(-1, 0, 0), new Vector3i(-64, 0, 0)) && !Corporations.near(new Vector3i(-1, 0, 0), new Vector3i(-65, 0, 0)) : "system -1 to -4 is three";

        long[] e = Corporations.earn(1_000_000, 20, false);
        assert e[0] == 200_000 && e[1] == 0 : "a fifth of the sale, no bonus";
        e = Corporations.earn(1_000_000, 20, true);
        assert e[0] == 200_000 && e[1] == 20_000 : "near the HQ: ten percent more, on top";

        long[] d = Corporations.dividend(1_000_000, 40, 5_000_000, 10_000, 100, false);
        assert d[0] == 400_000 && d[1] == 40 : "40% of the turn at 10k credits a point";
        d = Corporations.dividend(1_000_000, 40, 5_000_000, 10_000, 100, true);
        assert d[0] == 400_000 && d[1] == 46 : "Corporate Chains: 15% more points for the same credits";
        d = Corporations.dividend(1_000_000, 40, 100_000, 10_000, 100, false);
        assert d[0] == 100_000 && d[1] == 10 : "never more than the treasury holds";
        d = Corporations.dividend(100_000_000, 100, 1_000_000_000, 10_000, 100, false);
        assert d[1] == 100 && d[0] == 1_000_000 : "capped, and only the capped points are paid for";
        d = Corporations.dividend(100_000_000, 100, 1_000_000_000, 10_000, -1, false);
        assert d[1] == 10_000 : "-1: no cap";
        assert Corporations.dividend(-5, 50, 10, 1, 100, false)[0] == 0;

        Corporations.State st = new Corporations.State();
        Corporations.Corp c = new Corporations.Corp();
        c.name = "Varon Logistics";
        c.type = Corporations.Type.LOGISTICS;
        c.npcCeo = "Tullia Varo";
        c.ceo = "Vael";
        c.treasury = 3_000_000_000L;
        c.rate = 40;
        c.holdings.add(new Corporations.Holding("ENTITY_SPACESTATION_Depot", "Depot", new Vector3i(2, 3, -4)));
        c.hq = new Corporations.Holding("ENTITY_SPACESTATION_Tower", "Tower", new Vector3i(5, 5, 5));
        c.hqSince = 77;
        c.revenue = 12;
        c.lastRevenue = 34;
        c.lastDividend = 56;
        c.description = "We move what others will not.";
        st.corps.add(c);
        st.corps.add(new Corporations.Corp());
        Corporations.State back = Corporations.State.fromTag(st.toTag());
        Corporations.Corp b = back.corps.get(0);
        assert back.corps.size() == 2 && back.corps.get(1).hq == null && back.corps.get(1).holdings.isEmpty();
        assert b.name.equals("Varon Logistics") && b.type == Corporations.Type.LOGISTICS && b.ceo.equals("Vael") && b.npcCeo.equals("Tullia Varo");
        assert b.treasury == 3_000_000_000L : "treasuries past what an int holds survive";
        assert b.description.equals("We move what others will not.") && back.corps.get(1).describe().equals(back.corps.get(1).type.description());
        assert b.rate == 40 && b.hqSince == 77 && b.revenue == 12 && b.lastRevenue == 34 && b.lastDividend == 56;
        assert b.hq.sector.equals(new Vector3i(5, 5, 5)) && b.holdings.get(0).name.equals("Depot");
        assert back.runs("Depot") == b && back.runs("ENTITY_SPACESTATION_Depot") == b : "a station is found with or without the prefix";
        assert back.runs("Tower") == null : "the HQ is not a station it runs unless given one";
        assert b.memberCeo() && b.ceoName().equals("Vael") && !b.hqActive(Corporations.HQ_SETTLE_MS) && b.hqActive(Corporations.HQ_SETTLE_MS + 77);
        //a planet is one holding, keyed by its core whichever plate is asked about
        int ico = org.schema.game.common.data.world.SimpleTransformableSendableObject.EntityType.PLANET_ICO.dbTypeId;
        int station = org.schema.game.common.data.world.SimpleTransformableSendableObject.EntityType.SPACE_STATION.dbTypeId;
        int ship = org.schema.game.common.data.world.SimpleTransformableSendableObject.EntityType.SHIP.dbTypeId;
        String core = videogoose.betterfactions.assets.FactionAssets.planetUid(ico, new Vector3i(4, -2, 9));
        assert core.equals("ENTITY_PLANETICO_CORE_4_-2_9") : core;
        assert Corporations.holdable(ico) && Corporations.holdable(station) && !Corporations.holdable(ship);
        Corporations.Corp planet = new Corporations.Corp();
        planet.holdings.add(new Corporations.Holding(core, "Nerva", new Vector3i(4, -2, 9)));
        assert planet.holding(videogoose.betterfactions.assets.FactionAssets.planetUid(ico, new Vector3i(4, -2, 9))) != null;
        assert Corporations.cleanDescription("  two\n  lines ").equals("two lines") && Corporations.cleanDescription("x".repeat(300)).length() == Corporations.MAX_DESCRIPTION;
        //franchises: fees by kind, traffic and discounts; traffic fades a tenth a day
        assert Corporations.decayed(1000, 5, 5) == 1000 && Corporations.decayed(1000, 5, 6) == 900 && Corporations.decayed(1000, 5, 7) == 810;
        assert Corporations.decayed(1000, 9, 5) == 1000 : "no fading backwards";
        long base = 100_000_000L, unit = 50_000_000L;
        assert Corporations.franchiseFee(base, false, 0, unit, false) == base : "a quiet outpost: the base";
        assert Corporations.franchiseFee(base, true, 0, unit, false) == 2 * base : "a Trade Station: twice";
        assert Corporations.franchiseFee(base, false, unit, unit, false) == 2 * base : "a unit of sales adds the base again";
        assert Corporations.franchiseFee(base, false, 100 * unit, unit, false) == 4 * base : "busy tops out at four times";
        assert Corporations.franchiseFee(base, false, 0, unit, true) == base * 3 / 4 : "Corporate Chains: a quarter off";
        assert Corporations.relationFee(1000, 0, 5000) == 1000 : "an indifferent owner: unchanged";
        assert Corporations.relationFee(1000, 5000, 5000) == 500 : "a friendly owner: half off";
        assert Corporations.relationFee(1000, -9000, 5000) == 1500 : "a hostile owner: half again, however hostile";
        assert Corporations.tradeStation("Trade Station") && !Corporations.tradeStation("Trade Outpost") && !Corporations.tradeStation(null);
        assert Corporations.systems(new Vector3i(0, 0, 0), new Vector3i(80, -17, 3)) == 5;
        Corporations.Offer offer = new Corporations.Offer();
        offer.shop = new Corporations.Holding("ENTITY_SHOP_Outpost", "Trade Outpost", new Vector3i(70, 0, 0));
        offer.fee = 1000;
        Corporations.Corp goods = new Corporations.Corp();
        goods.type = Corporations.Type.CONSUMER_GOODS;
        assert Corporations.feeFor(goods, offer, 0) == 1000 : "no HQ, no discount";
        goods.hq = new Corporations.Holding("ENTITY_SPACESTATION_Mall", "Mall", new Vector3i(0, 0, 0));
        goods.hqSince = 0;
        assert Corporations.feeFor(goods, offer, Corporations.HQ_SETTLE_MS) == 900 : "a Consumer Goods HQ within 5 systems: 10% off";
        assert Corporations.feeFor(goods, offer, Corporations.HQ_SETTLE_MS - 1) == 1000 : "not while it settles";
        goods.type = Corporations.Type.SHIPYARDS;
        assert Corporations.feeFor(goods, offer, Corporations.HQ_SETTLE_MS) == 1000 : "only Consumer Goods";
        goods.franchises.add(offer.shop);
        goods.holdings.add(new Corporations.Holding("A", "A", new Vector3i()));
        Corporations.Corp g2 = Corporations.Corp.fromTag(goods.toTag());
        assert g2.franchises.size() == 1 && g2.franchise("Outpost") != null && g2.holdingCount() == 2 : "franchises survive a save and share the limit";
        Corporations whole = new Corporations();
        Corporations.State ws = new Corporations.State();
        ws.corps.add(goods);
        whole.fromTag(new org.schema.schine.resource.tag.Tag(org.schema.schine.resource.tag.Tag.Type.STRUCT, null, new org.schema.schine.resource.tag.Tag[]{
                new org.schema.schine.resource.tag.Tag(org.schema.schine.resource.tag.Tag.Type.STRUCT, null, new org.schema.schine.resource.tag.Tag[]{
                        new org.schema.schine.resource.tag.Tag(org.schema.schine.resource.tag.Tag.Type.INT, null, 9), ws.toTag(), org.schema.schine.resource.tag.FinishTag.INST}),
                org.schema.schine.resource.tag.FinishTag.INST}));
        Corporations again = new Corporations();
        again.fromTag(whole.toTag());
        assert again.of(9) != null && again.of(9).corps.get(0).franchises.size() == 1 : "a whole save, traffic entry and all, reads back";
        System.out.println("Corporations_test: OK");
    }
}
