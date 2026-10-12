package videogoose.betterfactions.emblem;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Pins the two things about an emblem that are not obvious from reading it: what the server refuses,
 * and that the same picture always gets the same name.
 *
 * <p>Both are load-bearing rather than tidy. The refusals are the trust boundary - this is the one
 * place in the game where one player hands another player's client a file - and the naming is what
 * makes the whole transport work: a hash that moved with the upload rather than with the image would
 * mean every client re-fetching every emblem on every change, and a hash that collided across
 * different images would mean a faction quietly wearing somebody else's.</p>
 *
 * <p>Run via {@code gradlew apiSelfChecks}, or {@code java -ea} on this class.</p>
 */
public final class FactionEmblem_test {

    private FactionEmblem_test() {
    }

    public static void main(String[] args) throws IOException {
        aRequestNamesAFileAndNothingElse();
        theSamePictureGetsTheSameName();
        whatIsStoredIsAlwaysOurOwnBytes();
        aFileThatIsNotAPngIsRefused();
        anImageTooBigToDecodeIsRefusedBeforeItIsDecoded();
        System.out.println("FactionEmblem_test: OK");
    }

    /**
     * The server pastes a request onto a directory and serves what comes out, so this check is the
     * whole of the boundary: anything that could climb out of that directory must not get through.
     */
    private static void aRequestNamesAFileAndNothingElse() {
        assert FactionEmblem.isPlainFileName("emblem_0123456789abcdef.png");
        assert !FactionEmblem.isPlainFileName("../server.properties");
        assert !FactionEmblem.isPlainFileName("..\\server.properties");
        assert !FactionEmblem.isPlainFileName("sub/dir.png");
        assert !FactionEmblem.isPlainFileName("C:\\keys.txt");
        assert !FactionEmblem.isPlainFileName("");
        assert !FactionEmblem.isPlainFileName(null);

        String name = FactionEmblem.requestName("0123456789abcdef");
        assert FactionEmblem.isRequest(name) : name;
        assert "0123456789abcdef".equals(FactionEmblem.hashOf(name)) : FactionEmblem.hashOf(name);
        assert !FactionEmblem.isRequest("schema.smskin");
    }

    /**
     * A client that already holds these bytes must never ask for them again - that is the only reason
     * an emblem can ride the faction sync at all. Two uploads of the same picture are one file.
     */
    private static void theSamePictureGetsTheSameName() throws IOException {
        File a = png(64, 64, Color.RED);
        File b = png(64, 64, Color.RED);
        File c = png(64, 64, Color.BLUE);
        try {
            String ha = FactionEmblem.store(java.nio.file.Files.readAllBytes(a.toPath()));
            String hb = FactionEmblem.store(java.nio.file.Files.readAllBytes(b.toPath()));
            String hc = FactionEmblem.store(java.nio.file.Files.readAllBytes(c.toPath()));
            assert ha.equals(hb) : ha + " / " + hb;
            assert !ha.equals(hc) : "two different pictures got one name";
            assert FactionEmblem.serverFile(ha).exists();
            cleanUp(ha, hc);
        } finally {
            a.delete();
            b.delete();
            c.delete();
        }
    }

    /**
     * Nothing of what arrived survives storing, which is what keeps a hostile PNG from being handed
     * verbatim to everyone who looks at the faction. The size is normalised in the same pass, so a
     * bigger upload does not buy a sharper logo.
     */
    private static void whatIsStoredIsAlwaysOurOwnBytes() throws IOException {
        File big = png(512, 200, Color.GREEN);
        try {
            String hash = FactionEmblem.store(java.nio.file.Files.readAllBytes(big.toPath()));
            File stored = FactionEmblem.serverFile(hash);
            BufferedImage out = ImageIO.read(stored);
            assert out != null : "stored emblem is not readable";
            assert out.getWidth() == FactionEmblem.SIZE : out.getWidth();
            assert out.getHeight() == FactionEmblem.SIZE : out.getHeight();
            assert stored.length() != big.length() : "the uploaded file was served back unchanged";
            cleanUp(hash);
        } finally {
            big.delete();
        }
    }

    private static void aFileThatIsNotAPngIsRefused() throws IOException {
        File f = File.createTempFile("emblem_test_", ".png");
        try {
            java.io.FileOutputStream o = new java.io.FileOutputStream(f);
            try {
                o.write("<html>not a png at all</html>".getBytes("UTF-8"));
            } finally {
                o.close();
            }
            assert refused(f) : "a file that is not an image was stored";
        } finally {
            f.delete();
        }
    }

    /**
     * The byte cap does not bound the work: a PNG compresses, so a small file can describe a raster
     * that costs gigabytes to decode. Refusing it has to happen on the header.
     */
    private static void anImageTooBigToDecodeIsRefusedBeforeItIsDecoded() throws IOException {
        File f = png(FactionEmblem.MAX_SOURCE_DIM * 2, 8, null);
        try {
            assert f.length() < FactionEmblem.MAX_UPLOAD_BYTES
                    : "this check proves nothing unless the file is small: " + f.length();
            assert refused(f) : "an image past the dimension limit was decoded and stored";
        } finally {
            f.delete();
        }
    }

    private static boolean refused(File f) {
        try {
            String hash = FactionEmblem.store(java.nio.file.Files.readAllBytes(f.toPath()));
            cleanUp(hash);
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * @param fill null leaves the image blank, which is how the oversized case stays small on disk
     */
    private static File png(int w, int h, Color fill) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        if (fill != null) {
            Graphics2D g = img.createGraphics();
            try {
                g.setColor(fill);
                g.fillRect(0, 0, w, h);
                g.setColor(Color.WHITE);
                g.fillRect(w / 4, h / 4, Math.max(1, w / 2), Math.max(1, h / 2));
            } finally {
                g.dispose();
            }
        }
        File f = File.createTempFile("emblem_test_", ".png");
        ImageIO.write(img, "png", f);
        return f;
    }

    private static void cleanUp(String... hashes) {
        for (String h : hashes) {
            FactionEmblem.serverFile(h).delete();
        }
    }
}
