package videogoose.betterfactions;

import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * The mod's {@code *_test} self-checks, run inside the game by {@code /run_tests} (registered in
 * {@link BetterFactions#onRegisterTests}). {@code /run_tests @videogoose.betterfactions} runs these and the
 * mod's world tests ({@code tests/*.json}) together. Gradle's {@code test} runs the same checks outside
 * the game.
 *
 * <p>Each check is a plain {@code main} with {@code assert}s. The game runs without {@code -ea}, so
 * assertions are switched on for each check's class before it loads. A few checks touch state a live
 * server shares, and only run under Gradle ({@link #SERVER_UNSAFE}).</p>
 */
@RunWith(InGameChecks.Runner.class)
public final class InGameChecks {

	/** Check class to why it must not run in a live server. */
	static final Map<String, String> SERVER_UNSAFE = Map.of(
			"videogoose.betterfactions.diplomacy.FactionTreaty_test", "unregisters every mod's treaty types",
			"videogoose.betterfactions.emblem.FactionEmblem_test", "writes test images into the world's emblem store",
			"videogoose.betterfactions.diplomacy.DiplomacyAcceptance_test", "reads the NPC presets from the source tree");

	public static final class Runner extends ParentRunner<String> {

		private final List<String> checks;

		public Runner(Class<?> testClass) throws InitializationError {
			super(testClass);
			checks = discover();
		}

		@Override
		protected List<String> getChildren() {
			return checks;
		}

		@Override
		protected Description describeChild(String check) {
			return Description.createTestDescription(InGameChecks.class, check.substring(check.lastIndexOf('.') + 1));
		}

		@Override
		protected void runChild(String check, RunNotifier notifier) {
			Description description = describeChild(check);
			notifier.fireTestStarted(description);
			try {
				ClassLoader loader = InGameChecks.class.getClassLoader();
				//before the class initialises, which is when its assertion status is fixed
				loader.setClassAssertionStatus(check, true);
				Class.forName(check, true, loader).getMethod("main", String[].class).invoke(null, (Object) new String[0]);
			} catch (InvocationTargetException e) {
				notifier.fireTestFailure(new Failure(description, e.getCause()));
			} catch (Throwable e) {
				notifier.fireTestFailure(new Failure(description, e));
			} finally {
				notifier.fireTestFinished(description);
			}
		}
	}

	/**
	 * Every {@code *_test} class where this class was loaded from (the mod jar in game, a classes folder
	 * outside it), but those in {@link #SERVER_UNSAFE}.
	 */
	static List<String> discover() {
		List<String> entries = new ArrayList<>();
		File from = null;
		try {
			from = new File(InGameChecks.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (from.isDirectory()) {
				Path root = from.toPath();
				try (Stream<Path> files = Files.walk(root)) {
					files.forEach(p -> entries.add(root.relativize(p).toString().replace(File.separatorChar, '/')));
				}
			} else {
				try (JarFile jar = new JarFile(from)) {
					for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
						entries.add(e.nextElement().getName());
					}
				}
			}
		} catch (Exception e) {
			System.err.println("[BetterFactions] could not list self-checks in " + from + ": " + e);
		}
		List<String> found = new ArrayList<>();
		for (String name : entries) {
			if (name.endsWith("_test.class") && !name.contains("$")) {
				String check = name.substring(0, name.length() - ".class".length()).replace('/', '.');
				if (!SERVER_UNSAFE.containsKey(check)) {
					found.add(check);
				}
			}
		}
		Collections.sort(found);
		return found;
	}
}
