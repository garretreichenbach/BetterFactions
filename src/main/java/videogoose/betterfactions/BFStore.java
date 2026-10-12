package videogoose.betterfactions;

import api.common.GameCommon;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.battle.BattleTracker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

/**
 * The server's diplomacy and battles, a file each per world. Tag files rather than the mod-persistence
 * Gson store, because every diplomacy type already round-trips through tags and is tested doing so.
 */
public final class BFStore {

	private BFStore() {
	}

	private static File file(String name) {
		return new File("moddata/BetterFactions/" + name + "-" + GameCommon.getUniqueContextId() + ".tag");
	}

	public static void load(DiplomacyManager m) {
		read("diplomacy", t -> m.fromTag(t, true));
	}

	public static void save(DiplomacyManager m) {
		write("diplomacy", m.toTag(false));
	}

	public static void load(BattleTracker b) {
		read("battles", b::fromTag);
	}

	public static void save(BattleTracker b) {
		write("battles", b.toTag());
	}

	private static void read(String name, Consumer<Tag> into) {
		File f = file(name);
		if (!f.exists()) {
			return;
		}
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
			into.accept(Tag.readFrom(in, false, false));
		} catch (Exception e) {
			//a world must still load if this file cannot: keep the bad file for a human to look at
			System.err.println("[BetterFactions] could not read " + f + ", starting without it: " + e);
			f.renameTo(new File(f.getPath() + ".unreadable-" + System.currentTimeMillis()));
		}
	}

	private static void write(String name, Tag tag) {
		File f = file(name);
		f.getParentFile().mkdirs();
		File tmp = new File(f.getPath() + ".tmp");
		try {
			try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
				//not writeToZipped: in this engine build it never writes to the stream it is given
				tag.writeTo(out, false);
			}
			Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			System.err.println("[BetterFactions] could not save " + name + ": " + e);
		}
	}
}
