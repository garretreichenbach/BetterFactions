package videogoose.betterfactions.civics;

import java.util.ArrayList;
import java.util.List;

/**
 * Pins the contract rules: refunds, NPC bounties growing, kills fading from memory, who may take what,
 * and that the board survives a save.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class Contracts_test {

    private Contracts_test() {
    }

    public static void main(String[] args) {
        assert Contracts.refund(1_000_000, 5) == 950_000 && Contracts.refund(1_000_000, 0) == 1_000_000;
        assert Contracts.npcBounty(5_000_000, 1) == 5_000_000 && Contracts.npcBounty(5_000_000, 3) == 15_000_000 && Contracts.npcBounty(10, 0) == 10;

        List<Long> kills = new ArrayList<>(List.of(0L, Contracts.AGGRESSION_MEMORY_MS / 2, Contracts.AGGRESSION_MEMORY_MS));
        assert Contracts.remembered(kills, Contracts.AGGRESSION_MEMORY_MS) == 3 : "all within memory";
        assert Contracts.remembered(kills, Contracts.AGGRESSION_MEMORY_MS + 1) == 2 && kills.size() == 2 : "the oldest fades and is dropped";

        Contracts.Contract bounty = new Contracts.Contract();
        bounty.type = Contracts.Type.BOUNTY;
        bounty.posterFaction = 5;
        bounty.posterCorp = 77;
        assert Contracts.mayTake(bounty, Corporations.Type.MERCENARIES, 9, 3) : "mercenaries of another faction";
        assert !Contracts.mayTake(bounty, Corporations.Type.LOGISTICS, 9, 3) : "not the wrong line of business";
        assert !Contracts.mayTake(bounty, Corporations.Type.MERCENARIES, 5, 3) : "not the poster's own faction";
        assert !Contracts.mayTake(bounty, Corporations.Type.MERCENARIES, 3, 3) : "not a bounty on one of their own";
        for (Contracts.Type t : Contracts.Type.values()) {
            assert t.takenBy() != null && !t.title().isEmpty();
        }
        assert Contracts.Type.ESCORT.takenBy() == Corporations.Type.MERCENARIES && Contracts.Type.HAUL.takenBy() == Corporations.Type.LOGISTICS;
        bounty.takerFaction = 9;
        bounty.takerCorp = 12;
        bounty.takerName = "Varon Security";
        assert bounty.taken() && !bounty.npc() && !Contracts.mayTake(bounty, Corporations.Type.MERCENARIES, 11, 3) : "one taker";

        bounty.id = 42;
        bounty.posterName = "Varon Logistics";
        bounty.target = "Vael";
        bounty.reward = 3_000_000_000L;
        bounty.posted = 10;
        bounty.deadline = 20;
        Contracts.Contract back = Contracts.Contract.fromTag(bounty.toTag());
        assert back.id == 42 && back.type == Contracts.Type.BOUNTY && back.posterFaction == 5 && back.posterCorp == 77
                && back.posterName.equals("Varon Logistics") && back.target.equals("Vael") && back.reward == 3_000_000_000L
                && back.posted == 10 && back.deadline == 20 && back.takerFaction == 9 && back.takerCorp == 12 && back.takerName.equals("Varon Security");

        Contracts board = new Contracts();
        board.received(new org.schema.schine.resource.tag.Tag(org.schema.schine.resource.tag.Tag.Type.STRUCT, null,
                new org.schema.schine.resource.tag.Tag[]{bounty.toTag(), org.schema.schine.resource.tag.FinishTag.INST}));
        Contracts again = new Contracts();
        again.fromTag(board.toTag());
        assert again.all().size() == 1 && again.all().get(0).target.equals("Vael") : "the board survives a save";

        assert Corporations.Corp.fromTag(new Corporations.Corp().toTag()).id != 0 : "an old corporation is given an id";
        Corporations.Corp named = new Corporations.Corp();
        named.id = 1234;
        assert Corporations.Corp.fromTag(named.toTag()).id == 1234 : "and keeps it";

        assert videogoose.betterfactions.gui.CorporationsContent.parseMoney("5m") == 5_000_000 && videogoose.betterfactions.gui.CorporationsContent.parseMoney("2.5B") == 2_500_000_000L
                && videogoose.betterfactions.gui.CorporationsContent.parseMoney("1,200k") == 1_200_000 && videogoose.betterfactions.gui.CorporationsContent.parseMoney("lots") == -1;
        //goods: collateral, who keeps it, and a goods contract surviving a save
        assert Contracts.collateral(250, 4_000) == 1_000_000 && Contracts.collateral(-1, 5) == 0;
        Contracts.Contract haul = new Contracts.Contract();
        haul.type = Contracts.Type.HAUL;
        haul.item = 5;
        haul.count = 1000;
        haul.pickedUp = 600;
        haul.delivered = 400;
        assert haul.goods() && Contracts.collateralToPoster(haul) : "goods loaded and not delivered: the poster is owed";
        haul.delivered = 600;
        assert !Contracts.collateralToPoster(haul) : "all loaded goods arrived: the taker has it back";
        haul.pickedUp = 0;
        haul.delivered = 0;
        assert !Contracts.collateralToPoster(haul) : "nothing loaded, nothing owed";
        haul.destination = new Corporations.Holding("ENTITY_SPACESTATION_Depot", "Depot", new org.schema.common.util.linAlg.Vector3i(1, 2, 3));
        haul.origin = new Corporations.Holding("ENTITY_SPACESTATION_Mine", "Mine", new org.schema.common.util.linAlg.Vector3i(4, 5, 6));
        haul.pickedUp = 300;
        haul.delivered = 120;
        haul.collateral = 9_000_000;
        Contracts.Contract h2 = Contracts.Contract.fromTag(haul.toTag());
        assert h2.type == Contracts.Type.HAUL && h2.item == 5 && h2.count == 1000 && h2.delivered == 120 && h2.pickedUp == 300 && h2.collateral == 9_000_000
                && h2.destination.name.equals("Depot") && h2.origin.sector.equals(new org.schema.common.util.linAlg.Vector3i(4, 5, 6));
        assert Contracts.Contract.fromTag(bounty.toTag()).destination == null && !Contracts.Contract.fromTag(bounty.toTag()).goods() : "a bounty carries no goods";
        System.out.println("Contracts_test: OK");
    }
}
