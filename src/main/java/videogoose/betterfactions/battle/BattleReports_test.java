package videogoose.betterfactions.battle;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.List;

/** The battle report archive: who sees what, the cap, saving it, and the short news post. Run with -ea. */
public final class BattleReports_test {

	public static void main(String[] args) {
		BattleReports a = new BattleReports();
		for (int i = 0; i < BattleReports.KEPT + 5; i++) {
			int other = i % 2 == 0 ? 20 : 30;
			a.add(new BattleReports.Report(i, new Vector3i(i, 0, 0), "v" + i, "v" + i + "\nbody", 60_000, i % 3 == 0, 10, i % 2 == 0 ? 20 : Integer.MIN_VALUE,
					List.of(new BattleReports.Line(10, "Us", 1, false), new BattleReports.Line(other, "Them", 2, true)),
					List.of(new BattleReports.Unit("Ark", 10, (byte) 5, 1200, 0.5f, 40, false), new BattleReports.Unit("Raider", other, (byte) 5, 300, 0, 9, true)), "Battle " + i));
		}
		List<BattleReports.Report> ours = a.of(10, 1000);
		assert ours.size() == BattleReports.KEPT : "the oldest past the cap are dropped: " + ours.size();
		assert ours.get(0).time() == BattleReports.KEPT + 4 : "newest first";
		List<BattleReports.Report> twenty = a.of(20, 1000);
		assert twenty.stream().allMatch(r -> r.lostByOthers(20) == 1) : "a faction only sees battles it was in";
		assert a.of(10, 7).size() == 7 : "capped per answer";
		assert a.of(99, 10).isEmpty() : "nobody else's";

		BattleReports.Report r = ours.get(0);
		assert r.lostBy(10) == 1 && r.lostByOthers(10) == 2 && r.opponents(10).equals("Them");

		BattleReports back = new BattleReports();
		back.fromTag(a.toTag());
		List<BattleReports.Report> again = back.of(10, 1000);
		assert again.size() == ours.size() && again.get(0).time() == ours.get(0).time() && again.get(0).body().equals(ours.get(0).body()) : "survives a save";
		assert again.get(0).sides().get(1).withdrew() && again.get(0).winner() == 10;
		BattleReports.Report withNeutral = new BattleReports.Report(1, new Vector3i(), "v", "v\nb", 1, false, 10, Integer.MIN_VALUE,
				List.of(new BattleReports.Line(10, "Us", 1, false), new BattleReports.Line(0, "Unaffiliated", 4, false)), List.of(), "B");
		assert BattleReports.listFrom(BattleReports.toTag(List.of(withNeutral), true)).get(0).lostByOthers(10) == 0
				: "a report saved with unaffiliated as a side reads back without it";
		assert again.get(0).name().equals("Battle " + (BattleReports.KEPT + 4)) : "the name survives a save";
		assert again.get(0).units().size() == 2 && again.get(0).units().get(1).lost() && again.get(0).units().get(0).mass() == 1200
				: "units survive a save";
		assert BattleReports.listFrom(a.tagFor(10)).get(0).units().isEmpty() : "the list sent to a client leaves units out";
		Tag ask = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.LONG, null, r.time()), new Tag(Tag.Type.VECTOR3i, null, new Vector3i(r.pos())), FinishTag.INST});
		assert BattleReports.unitsFrom(a.detailFor(10, ask).getStruct()[1]).size() == 2 : "and are sent on request";
		assert a.detailFor(99, ask) == null : "only to a side";
		assert again.get(0).favoured() == 20 && again.get(1).favoured() == Integer.MIN_VALUE : "the edge survives a save";

		//a report saved before "came off better" ends at the winner: drop the favoured tag and read it back
		Tag[] e = BattleReports.toTag(List.of(ours.get(0)), true).getStruct()[0].getStruct();
		Tag old = new Tag(Tag.Type.STRUCT, null, new Tag[]{
				new Tag(Tag.Type.STRUCT, null, new Tag[]{e[0], e[1], e[2], e[3], e[4], e[5], e[6], e[7], FinishTag.INST}), FinishTag.INST});
		BattleReports.Report r0 = BattleReports.listFrom(old).get(0);
		assert r0.name().startsWith("Battle at") : "an old report is named after its sector";
		assert r0.winner() == 10 && r0.favoured() == Integer.MIN_VALUE && r0.sides().size() == 2 : "an old report reads as no edge";

		assert BattleReports.mayAnswer("p", 10_000) && !BattleReports.mayAnswer("p", 10_500) && BattleReports.mayAnswer("q", 10_500)
				: "one answer a second per player";

		BattleTracker.Battle b = new BattleTracker.Battle(1, new Vector3i(), 0);
		b.side(10).lost = 3;
		b.side(20).lost = 1;
		String news = BattleTracker.newsBody(b, "F10 held the field.\nlong body\nLasted 3 min.", id -> "F" + id);
		assert news.startsWith("F10 held the field.\nShips lost: ") && news.contains("F10 3") && news.contains("F20 1")
				&& news.endsWith(".") : news;
		assert news.split("\n").length == 2 : "two short lines, not the whole report";
		b.side(10).survivors = true;
		b.side(20).survivors = true;
		news = BattleTracker.newsBody(b, BattleTracker.summarize(b, id -> "F" + id, 60_000), id -> "F" + id);
		assert news.startsWith("Inconclusive; F20 came off better (lost 1 ship to F10's 3).\nShips lost: ") && news.split("\n").length == 2
				: "the edge fits the news post's one verdict line: " + news;
		//momentum: a fresh win is worth a full step, a loss as much the other way, coming off better half
		BattleReports m = new BattleReports();
		long day = BattleReports.MOMENTUM_MS;
		m.add(new BattleReports.Report(0, new Vector3i(), "v", "b", 1, false, 10, Integer.MIN_VALUE,
				List.of(new BattleReports.Line(10, "Us", 0, false), new BattleReports.Line(20, "Them", 2, false)), List.of(), "Old"));
		m.add(new BattleReports.Report(day, new Vector3i(), "v", "b", 1, false, Integer.MIN_VALUE, 30,
				List.of(new BattleReports.Line(30, "Edge", 1, false), new BattleReports.Line(20, "Them", 3, false)), List.of(), "Edge"));
		var mo = m.momentum(day);
		assert mo.get(10) == 0 : "a battle a full window old counts for nothing";
		assert Math.abs(mo.get(30) - BattleReports.MOMENTUM_PER_WIN / 2) < 1e-9 && Math.abs(mo.get(20) + BattleReports.MOMENTUM_PER_WIN / 2) < 1e-9 : mo;
		assert Math.abs(m.momentum(day + day / 2).get(30) - BattleReports.MOMENTUM_PER_WIN / 4) < 1e-9 : "fades linearly";
		for (int i = 0; i < 20; i++) {
			m.add(new BattleReports.Report(day, new Vector3i(), "v", "b", 1, false, 30, Integer.MIN_VALUE,
					List.of(new BattleReports.Line(30, "Edge", 0, false), new BattleReports.Line(20, "Them", 1, false)), List.of(), "Win"));
		}
		mo = m.momentum(day);
		assert mo.get(30) == BattleReports.MOMENTUM_CAP && mo.get(20) == -BattleReports.MOMENTUM_CAP : "capped both ways: " + mo;
		m.add(new BattleReports.Report(day, new Vector3i(), "v", "b", 1, false, Integer.MIN_VALUE, Integer.MIN_VALUE,
				List.of(new BattleReports.Line(40, "A", 0, false), new BattleReports.Line(50, "B", 0, false)), List.of(), "Draw"));
		assert !m.momentum(day).containsKey(40) : "an inconclusive battle moves nobody";
		//weariness is per opponent: 20 lost to 30 often enough to want out, and has no quarrel with 10
		assert m.weariness(20, 30, day) == 1 && m.weariness(30, 20, day) == 0 : "the winner is not weary";
		assert m.weariness(20, 10, day) == 0 : "losing to one enemy is no reason to make peace with another";
		BattleReports w = new BattleReports();
		w.add(new BattleReports.Report(0, new Vector3i(), "v", "b", 1, false, 30, Integer.MIN_VALUE,
				List.of(new BattleReports.Line(30, "Edge", 0, false), new BattleReports.Line(20, "Them", 1, false)), List.of(), "Loss"));
		assert Math.abs(w.weariness(20, 30, 0) - BattleReports.MOMENTUM_PER_WIN / BattleReports.MOMENTUM_CAP) < 1e-9;
		w.add(new BattleReports.Report(0, new Vector3i(), "v", "b", 1, false, 20, Integer.MIN_VALUE,
				List.of(new BattleReports.Line(30, "Edge", 1, false), new BattleReports.Line(20, "Them", 0, false)), List.of(), "Revenge"));
		assert w.weariness(20, 30, 0) == 0 : "a win back makes up for a loss";
		w.add(new BattleReports.Report(0, new Vector3i(), "v", "b", 1, false, 40, Integer.MIN_VALUE,
				List.of(new BattleReports.Line(40, "Third", 0, false), new BattleReports.Line(30, "Edge", 1, false), new BattleReports.Line(20, "Them", 1, false)), List.of(), "Third"));
		assert w.weariness(20, 30, 0) == 0 : "a third side beating both is not 30 beating 20";
		System.out.println("BattleReports_test: OK");
	}
}
