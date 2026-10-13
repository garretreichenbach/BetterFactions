package videogoose.betterfactions.repair;

import java.util.List;

/** Blueprint guesses from ship names, and the assignment tag round trip. Run with -ea. */
public final class FactionDesigns_test {

	public static void main(String[] args) {
		List<String> bps = List.of("Falcon", "Falcon Mk2", "Kestrel-Heavy", "Tug");
		assert "Falcon Mk2".equals(FactionDesigns.guess("Falcon Mk2_1700000", bps)) : "longest prefix wins";
		assert "Falcon".equals(FactionDesigns.guess("falcon_12", bps)) : "case and punctuation ignored";
		assert "Kestrel-Heavy".equals(FactionDesigns.guess("Kestrel Hvy 3", bps)) : "close start, no exact prefix";
		assert "Tug".equals(FactionDesigns.guess("Tug 2", bps)) : "short blueprint still matches as a prefix";
		assert FactionDesigns.guess("Tugboat", List.of("Tuxedo")) == null : "three letters in common is not a match";
		assert FactionDesigns.guess("Anything", List.of()) == null;

		FactionDesigns d = new FactionDesigns();
		d.assign(5, "ENTITY_SHIP_A", "Falcon");
		d.assign(5, "ENTITY_SHIP_B", "Tug");
		d.assign(7, "ENTITY_SHIP_C", "Tug");
		d.assign(5, "ENTITY_SHIP_B", "");
		d.moved(5, "ENTITY_SHIP_A", "ENTITY_SHIP_A2");
		FactionDesigns back = new FactionDesigns();
		back.fromTag(d.toTag());
		assert "Falcon".equals(back.assigned(5, "ENTITY_SHIP_A2")) && back.assigned(5, "ENTITY_SHIP_A") == null : "moved with the rebuild";
		assert back.assigned(5, "ENTITY_SHIP_B") == null : "cleared";
		assert "Tug".equals(back.assigned(7, "ENTITY_SHIP_C"));
		FactionDesigns.received(d.tagFor(5));
		assert FactionDesigns.client().size() == 1 && "Falcon".equals(FactionDesigns.client().get("ENTITY_SHIP_A2"));
		System.out.println("FactionDesigns_test: OK");
	}
}
