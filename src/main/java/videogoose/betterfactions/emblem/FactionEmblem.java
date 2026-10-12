package videogoose.betterfactions.emblem;

import org.schema.schine.resource.FileExt;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

/**
 * A faction's uploaded logo: where the bytes live, what counts as a valid one, and what a client asks
 * for to get a copy.
 *
 * <p>A faction is known by a sixteen-character hash of its emblem, which every client receives, and
 * a client fetches the image itself once. The hash rather than a filename because it doubles as the
 * cache key: a client that has these exact bytes on disk never asks for them again, two factions that
 * picked the same image cost one transfer between them, and re-uploading what you already had costs
 * nothing at all.</p>
 *
 * <p>Uploads are off unless a server owner turns {@code custom_emblems_enabled} on in the mod config,
 * having decided they are willing to moderate what their players draw. With it off the control is
 * absent from the settings tab <i>and</i> {@link #store} is never reached, because the client gate is
 * not the gate.</p>
 *
 * <p>Nothing of an uploaded file survives {@link #store}. The image is decoded, drawn into a fresh
 * {@value #SIZE}x{@value #SIZE} buffer and re-encoded, so what reaches another client is bytes this
 * server wrote - a hostile or malformed PNG cannot be passed along verbatim to be opened by everyone
 * who looks at the faction. That is also why the hash is taken after the re-encode rather than of
 * what arrived: the name of the image is the name of what we actually serve.</p>
 */
public final class FactionEmblem {

	/**
	 * What an emblem is stored and drawn at. Every upload is rescaled to this, so the stored files are
	 * all one size and a faction does not get a sharper logo than another by uploading a bigger file.
	 */
	public static final int SIZE = 128;

	/**
	 * The largest source image we will decode. A PNG is compressed, so sixty kilobytes of file can
	 * describe a 30000x30000 raster that costs gigabytes to decode - the byte cap alone does not bound
	 * the work. The header is read first and anything past this is refused before a pixel is touched.
	 */
	public static final int MAX_SOURCE_DIM = 2048;

	/**
	 * Hard cap on what a client may send. Generous for a logo and small enough that a slow connection
	 * is not held open by one.
	 */
	public static final int MAX_UPLOAD_BYTES = 128 * 1024;

	public static final String SERVER_DIR = "moddata" + File.separator + "BetterFactions" + File.separator + "emblems" + File.separator;

	private static final String PREFIX = "emblem_";
	private static final String SUFFIX = ".png";

	private FactionEmblem() {
	}

	/**
	 * What a client puts on the wire to ask for an emblem, and also what the file is called at both
	 * ends. One name everywhere: the request is the filename is the cache key, so there is no mapping
	 * to get wrong and no second place for a path to come from.
	 */
	public static String requestName(String hash) {
		return PREFIX + hash + SUFFIX;
	}

	public static boolean isRequest(String name) {
		return name != null && name.startsWith(PREFIX) && name.endsWith(SUFFIX);
	}

	/**
	 * The hash back out of a request name. Only ever called on a name {@link #isRequest} accepted.
	 */
	public static String hashOf(String requestName) {
		return requestName.substring(PREFIX.length(), requestName.length() - SUFFIX.length());
	}

	/**
	 * A file request names a file, never a path. The server pastes the request onto a directory and
	 * serves whatever comes out, so a request carrying a separator or a {@code ..} would read anything
	 * the server process can reach - its config, its database, its keys. Every request goes through
	 * here first.
	 */
	public static boolean isPlainFileName(String name) {
		if (name == null || name.isEmpty() || name.length() > 128) {
			return false;
		}
		if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.indexOf(':') >= 0) {
			return false;
		}
		return !name.contains("..");
	}

	public static File serverFile(String hash) {
		return new FileExt(SERVER_DIR + requestName(hash));
	}

	/**
	 * Whether a hash is one {@link #store} could have minted. A request names a hash, never a path,
	 * so anything else is refused before it gets near the file system.
	 */
	public static boolean isPlainHash(String hash) {
		return hash != null && hash.matches("[0-9a-f]{16}");
	}

	/**
	 * Validates an uploaded file, normalises it and writes it to the server's store.
	 *
	 * @return the hash the faction is then pointed at
	 * @throws IOException if the file is not a PNG we are willing to serve; the caller deletes it and
	 *                     tells the uploader, and nothing is stored
	 */
	public static String store(byte[] raw) throws IOException {
		if (raw == null || raw.length == 0) {
			throw new IOException("emblem upload is empty");
		}
		if (raw.length > MAX_UPLOAD_BYTES) {
			throw new IOException("emblem upload is " + raw.length + " bytes, the limit is " + MAX_UPLOAD_BYTES);
		}

		BufferedImage src = decodeBounded(raw);
		BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(src, 0, 0, SIZE, SIZE, null);
		} finally {
			g.dispose();
		}

		ByteArrayOutputStream encoded = new ByteArrayOutputStream(16 * 1024);
		if (!ImageIO.write(out, "png", encoded)) {
			throw new IOException("no PNG encoder available on this server");
		}
		byte[] bytes = encoded.toByteArray();
		String hash = hash(bytes);

		File dir = new FileExt(SERVER_DIR);
		dir.mkdirs();
		File dest = serverFile(hash);
		if (!dest.exists()) {
			FileOutputStream o = new FileOutputStream(dest);
			try {
				o.write(bytes);
			} finally {
				o.close();
			}
		}
		return hash;
	}

	/**
	 * Reads the header before the pixels, so an image too large to decode is refused rather than
	 * decoded and then measured.
	 */
	private static BufferedImage decodeBounded(byte[] raw) throws IOException {
		ImageInputStream in = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(raw));
		if (in == null) {
			throw new IOException("emblem upload is not readable as an image");
		}
		try {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (!readers.hasNext()) {
				throw new IOException("emblem upload is not an image");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(in, true, true);
				if (!"png".equalsIgnoreCase(reader.getFormatName())) {
					throw new IOException("emblem upload is not a PNG");
				}
				int w = reader.getWidth(0);
				int h = reader.getHeight(0);
				if (w <= 0 || h <= 0 || w > MAX_SOURCE_DIM || h > MAX_SOURCE_DIM) {
					throw new IOException("emblem upload is " + w + "x" + h + ", the limit is "
							+ MAX_SOURCE_DIM + "x" + MAX_SOURCE_DIM);
				}
				BufferedImage img = reader.read(0);
				if (img == null) {
					throw new IOException("emblem upload could not be decoded");
				}
				return img;
			} finally {
				reader.dispose();
			}
		} finally {
			in.close();
		}
	}

	private static String hash(byte[] bytes) throws IOException {
		try {
			byte[] d = MessageDigest.getInstance("SHA-256").digest(bytes);
			StringBuilder b = new StringBuilder(16);
			for (int i = 0; i < 8; i++) {
				b.append(Character.forDigit((d[i] >> 4) & 0xF, 16));
				b.append(Character.forDigit(d[i] & 0xF, 16));
			}
			return b.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}
}
