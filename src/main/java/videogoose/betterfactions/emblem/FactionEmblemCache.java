package videogoose.betterfactions.emblem;

import videogoose.betterfactions.DiplomacyManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.emblem.FactionEmblem;
import org.schema.game.common.updater.FileUtil;
import org.schema.schine.graphicsengine.core.Controller;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.resource.FileExt;

import java.io.File;
import java.io.IOException;

/**
 * The client's copy of every emblem it has been shown, on disk and on the GPU.
 *
 * <p>{@link #get} is the only place a texture is created, on the draw thread, because creating one
 * anywhere else is a crash rather than a bug you find later. {@link #received} only writes a file;
 * the handoff between them is that file appearing on disk.</p>
 *
 * <p>Keyed by content hash, so the cache survives a faction renaming itself, changing its emblem back
 * to one it used before, or two factions choosing the same image - and a hash already on disk is
 * never requested at all, which is what makes this cheap enough to consult from a draw call.</p>
 *
 * <p>Static because there is one client per process and one cache directory per server it connects
 * to. {@link #reset} is what a disconnect calls; without it a reconnect to a different server would
 * read the last server's images.</p>
 */
public final class FactionEmblemCache {

	private static final Object2ObjectOpenHashMap<String, Sprite> SPRITES = new Object2ObjectOpenHashMap<String, Sprite>();
	private static final ObjectOpenHashSet<String> REQUESTED = new ObjectOpenHashSet<String>();
	private static final ObjectOpenHashSet<String> FAILED = new ObjectOpenHashSet<String>();
	private static File dir;

	private FactionEmblemCache() {
	}

	/**
	 * This faction's emblem, or null while it is not here yet - which is most of the first frame a
	 * faction is shown, and forever for one that has no emblem. Callers draw nothing on null rather
	 * than waiting; the next frame either has it or has asked for it.
	 */
	public static Sprite get(GameClientState state, Faction f) {
		String hash = DiplomacyManager.client().emblemOf(f);
		if (!FactionEmblem.isPlainHash(hash)) {
			return null;
		}
		Sprite s = SPRITES.get(hash);
		if (s != null) {
			return s;
		}
		if (FAILED.contains(hash)) {
			return null;
		}
		File file = cacheFile(state, hash);
		if (!file.exists()) {
			request(state, hash);
			return null;
		}
		return load(hash, file);
	}

	/**
	 * An emblem the server sent. Written to the cache and nothing else: dropping it out of
	 * {@link #REQUESTED} is what lets the next {@link #get} turn it into a texture.
	 */
	public static void received(GameClientState state, String hash, byte[] bytes) {
		if (!FactionEmblem.isPlainHash(hash)) {
			return;
		}
		try {
			java.nio.file.Files.write(cacheFile(state, hash).toPath(), bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		synchronized (REQUESTED) {
			REQUESTED.remove(hash);
		}
	}

	/**
	 * Forgets everything, for a disconnect. The sprites go too: their handles belong to a GL context
	 * this client is about to leave.
	 */
	public static void reset() {
		SPRITES.clear();
		FAILED.clear();
		synchronized (REQUESTED) {
			REQUESTED.clear();
		}
		dir = null;
	}

	private static Sprite load(String hash, File file) {
		try {
			String name = "faction_emblem_" + hash;
			Controller.getResLoader().getImageLoader().loadImage(file.getAbsolutePath(), name, true);
			Sprite s = Controller.getResLoader().getSprite(name);
			if (s == null) {
				FAILED.add(hash);
				return null;
			}
			s.setPositionCenter(false);
			SPRITES.put(hash, s);
			return s;
		} catch (Exception e) {
			e.printStackTrace();
			FAILED.add(hash);
			return null;
		}
	}

	private static void request(GameClientState state, String hash) {
		synchronized (REQUESTED) {
			if (!REQUESTED.add(hash)) {
				return;
			}
		}
		DiplomacyManager.client().requestEmblem(hash);
	}

	/**
	 * Per server, because two servers can hand out different images and a hash is only unique within
	 * the one that minted it.
	 */
	private static File dir(GameClientState state) {
		if (dir == null) {
			String host = state.getController().getConnection().getHost();
			dir = new FileExt("." + File.separator + "client-faction-emblems" + File.separator
					+ host.replaceAll("[^A-Za-z0-9_.-]", "_") + File.separator);
			dir.mkdirs();
		}
		return dir;
	}

	private static File cacheFile(GameClientState state, String hash) {
		return new FileExt(dir(state), FactionEmblem.requestName(hash));
	}
}
