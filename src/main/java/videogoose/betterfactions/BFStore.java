package videogoose.betterfactions;

import api.common.GameCommon;
import org.schema.schine.resource.tag.Tag;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * The server's diplomacy, one file per world. A tag file rather than the mod-persistence Gson store,
 * because every diplomacy type already round-trips through tags and is tested doing so.
 */
public final class BFStore {

	private BFStore() {
	}

	private static File file() {
		return new File("moddata/BetterFactions/diplomacy-" + GameCommon.getUniqueContextId() + ".tag");
	}

	public static void load(DiplomacyManager m) {
		File f = file();
		if (!f.exists()) {
			return;
		}
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
			m.fromTag(Tag.readFromZipped(in, false), true);
		} catch (Exception e) {
			//a world must still load if its diplomacy cannot: keep the bad file for a human to look at
			System.err.println("[BetterFactions] could not read " + f + ", starting with no diplomacy: " + e);
			f.renameTo(new File(f.getPath() + ".unreadable-" + System.currentTimeMillis()));
		}
	}

	public static void save(DiplomacyManager m) {
		File f = file();
		f.getParentFile().mkdirs();
		File tmp = new File(f.getPath() + ".tmp");
		try {
			try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
				m.toTag(false).writeToZipped(out, false);
			}
			Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			System.err.println("[BetterFactions] could not save diplomacy: " + e);
		}
	}
}
