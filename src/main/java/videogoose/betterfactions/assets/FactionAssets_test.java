package videogoose.betterfactions.assets;

import org.schema.common.util.linAlg.Vector3i;

import java.io.*;

/**
 * Pins what an asset answer carries across the wire.
 *
 * <p>Worth a check rather than an eyeball: the list is variable-length inside a variable-length buffer, so
 * a serializer that writes one field more or fewer than it reads does not fail here - it desynchronises
 * every message that follows it in the same packet. The empty case matters for the same reason, and is the
 * one a faction that holds nothing actually sends.</p>
 *
 * <p>Run via {@code gradlew apiSelfChecks}, or {@code java -ea} on this class.</p>
 */
public final class FactionAssets_test {

    private FactionAssets_test() {
    }

    public static void main(String[] args) throws IOException {
        aFullAnswerSurvivesTheWire();
        anEmptyAnswerSurvivesTheWire();
        theStreamIsLeftExactlyWhereTheAnswerEnds();
        anUnknownEntityTypeIsNotAnError();
        System.out.println("FactionAssets_test: OK");
    }

    private static FactionAssets sample() {
        FactionAssets a = new FactionAssets(7, 1234567890123L);
        a.assets.add(new FactionAssets.Asset("ENTITY_SPACESTATION_Tarsis Yard", "Tarsis Yard",
                (byte) 2, new Vector3i(8, 3, -2)));
        a.assets.add(new FactionAssets.Asset("ENTITY_SHOP_Ring Depot", "", (byte) 1,
                new Vector3i(-100, 0, 4000)));
        a.routes.add(new FactionAssets.Route(42L, "40 Iron: Tarsis Yard -> Ring Depot", "Travelling to Ring Depot",
                2, 2, "Ring Depot", true, 90000L));
        return a;
    }

    private static byte[] write(FactionAssets a) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        a.serialize(new DataOutputStream(bytes));
        return bytes.toByteArray();
    }

    private static FactionAssets read(byte[] b) throws IOException {
        FactionAssets back = new FactionAssets();
        back.deserialize(new DataInputStream(new ByteArrayInputStream(b)));
        return back;
    }

    private static void aFullAnswerSurvivesTheWire() throws IOException {
        FactionAssets a = sample();
        a.truncated = true;
        FactionAssets back = read(write(a));

        assert back.factionId == 7 : back.factionId;
        assert back.queriedAt == 1234567890123L : back.queriedAt;
        assert back.truncated : "the truncation flag was dropped, so a clipped list would read as a whole one";
        assert back.assets.size() == 2 : back.assets.size();
        FactionAssets.Route r = back.route(42L);
        assert r != null && r.isDelivery() && r.paused && r.stop == 2 && r.stops == 2 && r.etaMs == 90000L
                && "Ring Depot".equals(r.station) && "Travelling to Ring Depot".equals(r.status) : "a route did not survive the wire";
        assert back.route(43L) == null;

        FactionAssets.Asset first = back.assets.get(0);
        assert "ENTITY_SPACESTATION_Tarsis Yard".equals(first.uid) : first.uid;
        assert "Tarsis Yard".equals(first.name) : first.name;
        assert first.type == 2 : first.type;
        assert first.sector.equals(new Vector3i(8, 3, -2)) : first.sector;

        FactionAssets.Asset second = back.assets.get(1);
        assert "".equals(second.name) : "an unnamed holding came back as something else: " + second.name;
        assert second.sector.equals(new Vector3i(-100, 0, 4000)) : second.sector;
    }

    private static void anEmptyAnswerSurvivesTheWire() throws IOException {
        FactionAssets back = read(write(new FactionAssets(3, 99L)));
        assert back.factionId == 3 : back.factionId;
        assert back.assets.isEmpty() : back.assets.size();
        assert !back.truncated : "an empty answer claimed it had been truncated";
    }

    /**
     * The one that catches a field written and not read. An answer rides a buffer with other messages
     * behind it, so a serializer that is off by one field corrupts them rather than failing here.
     */
    private static void theStreamIsLeftExactlyWhereTheAnswerEnds() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        sample().serialize(out);
        out.writeInt(0x5EA1ED);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        new FactionAssets().deserialize(in);
        assert in.readInt() == 0x5EA1ED : "the answer read past its own end, so whatever followed it is gone";
        assert in.read() == -1 : "the answer left bytes of its own behind";
    }

    /**
     * Entity types are the database's own ids, so a holding of a type this build has no entry for is a
     * row to render plainly, not a crash. The list asks for the type by index into a fixed array.
     */
    private static void anUnknownEntityTypeIsNotAnError() throws IOException {
        FactionAssets a = new FactionAssets(1, 1L);
        a.assets.add(new FactionAssets.Asset("ENTITY_FUTURE_Thing", "Thing", (byte) 120, new Vector3i()));
        FactionAssets back = read(write(a));
        assert back.assets.get(0).type == 120 : back.assets.get(0).type;
    }
}
