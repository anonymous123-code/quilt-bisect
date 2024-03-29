package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.plugin.gui.BisectPluginUi;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import io.github.anonymous123_code.quilt_bisect.shared.Issue;
import io.github.anonymous123_code.quilt_bisect.shared.ModSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.ModMetadata;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.gui.QuiltLoaderText;
import org.quiltmc.loader.api.plugin.QuiltPluginContext;
import org.quiltmc.loader.impl.fabric.metadata.ParseMetadataException;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Bisect {
	public static void parentBisect(@Nullable String crashLog, @Nullable String crashLogPath) throws IOException, NoSuchAlgorithmException {
		var config_dir = ActiveBisectConfig.configDirectory;
		var modset_path = config_dir.resolve("modSet.txt");
		var sections_path = config_dir.resolve("sections.txt");
		List<String> modSet = readModSet(modset_path);
		List<Integer> sections = readSections(sections_path);


		String modSetHash = generateModSetHash(modSet);
		copyLatestLog(modSetHash);
		var active_bisect = ActiveBisectConfig.getInstance();
		processRun(modSet, sections, modSetHash, crashLog, crashLogPath, active_bisect);
		active_bisect.safe(false);
	}


	public static void processRun(List<String> modIdSet, List<Integer> sections, String modSetHash, @Nullable String crashLog, @Nullable String crashLogPath, ActiveBisectConfig activeBisectConfig) {
		Optional<Integer> issue = getOrAddIssue(crashLog, activeBisectConfig);
		ModSet modSet = issue.isPresent() ? new ModSet.Erroring(new ArrayList<>(modIdSet), issue.get(),
			crashLogPath != null ? crashLogPath : "", new ArrayList<>(sections)
		) : new ModSet.Working(new ArrayList<>(modIdSet), new ArrayList<>(sections));
		activeBisectConfig.modSets.put(modSetHash, modSet);
	}


	public static Optional<Integer> getOrAddIssue(@Nullable String crashLog, ActiveBisectConfig activeBisectConfig) {
		var issuePath = ActiveBisectConfig.configDirectory.resolve("issue.txt");
		if (Files.exists(issuePath)) {
			String issueData;
			try {
				issueData = Files.readString(issuePath);
				Files.delete(issuePath);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return Optional.of(Integer.parseInt(issueData, 10));
		} else if (crashLog != null) {
			var stacktrace = removeStacktracePoison(BisectUtils.extractStackTrace(crashLog));
			for (int issueIndex = 0; issueIndex < activeBisectConfig.issues.size(); issueIndex++) {
				var issue = activeBisectConfig.issues.get(issueIndex);
				if (issue instanceof Issue.CrashIssue crashIssue) {
					for (var knownStacktrace : crashIssue.stacktraces) {
						String cleanKnownStacktrace = removeStacktracePoison(knownStacktrace);
						if (cleanKnownStacktrace.equals(stacktrace)) {
							return Optional.of(issueIndex);
						}
					}
				}
			}
			for (int issueIndex = 0; issueIndex < activeBisectConfig.issues.size(); issueIndex++) {
				var issue = activeBisectConfig.issues.get(issueIndex);
				if (issue instanceof Issue.CrashIssue crashIssue) {
					for (var knownStacktrace : crashIssue.stacktraces) {
						String cleanKnownStacktrace = removeStacktracePoison(knownStacktrace);
						if (cleanKnownStacktrace.split("\n")[0].equals(stacktrace.split("\n")[0])) {
							try {
								if (BisectPluginUi.openDialog(cleanKnownStacktrace, stacktrace)) {
									crashIssue.stacktraces.add(stacktrace);
									return Optional.of(issueIndex);
								}
							} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
									 IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						}
					}
				}
			}
			activeBisectConfig.issues.add(new Issue.CrashIssue(stacktrace));
			return Optional.of(activeBisectConfig.issues.size() - 1);
		} else {
			return Optional.empty();
		}
	}

	public static String removeStacktracePoison(String oldStacktrace) {
		return oldStacktrace
			.replaceAll(":\\d+\\)", ")") // Line numbers
			.replaceAll("\\.handler\\$[0-9a-z]{6}\\$", ".fuzzyMixinHandler\\$"); // Mixin
	}

	private static List<Integer> readSections(Path sectionsPath) throws IOException {
		if (Files.exists(sectionsPath)) {
			var resultUnprocessed = Files.readAllLines(sectionsPath);
			Files.delete(sectionsPath);
			var result = new ArrayList<Integer>(resultUnprocessed.size());
			for (String modSectionUnprocessed : resultUnprocessed) {
				result.add(Integer.parseInt(modSectionUnprocessed, 10));
			}
			return result;
		} else {
			throw new RuntimeException("In bisect, but run was unable to read sections file");
		}
	}

	private static void copyLatestLog(String modSetHash) throws IOException {
		var logFilePath = Path.of("logs", "latest.log");
		if (Files.isRegularFile(logFilePath)) {
			Files.createDirectories(Path.of("bisectLogStorage"));
			Files.copy(logFilePath, Path.of("bisectLogStorage", modSetHash + ".log"), REPLACE_EXISTING);
		}
	}

	private static String generateModSetHash(List<String> modSet) throws NoSuchAlgorithmException {
		return BisectUtils.hash256(String.join(",", modSet));
	}

	private static @NotNull List<String> readModSet(Path modSetPath) throws IOException {
		if (Files.exists(modSetPath)) {
			var result = Files.readAllLines(modSetPath);
			Files.delete(modSetPath);
			return result;
		} else {
			throw new RuntimeException("In bisect, but run was unable to read mod set file");
		}
	}

	public static void childBisect(QuiltPluginContext context) throws IOException, ParseMetadataException {
		var activeBisect = ActiveBisectConfig.getInstance();
		HashMap<String, Path> loadOptions = getModOptions();
		activeBisect.updateFiles(loadOptions);
		loadModSet(context, calculateModSet(activeBisect), loadOptions);
		if (activeBisect.bisectSettings != null && activeBisect.bisectSettings.disableWorldSaving()) {
			Path worldStorageLocation = Path.of("saves");
			Path backupStorageLocation = Path.of("bisectSavesBackup");

			if (Files.exists(backupStorageLocation)) {
				if (Files.isDirectory(backupStorageLocation)) {
					// For every dir in backup:
					try (Stream<Path> path = Files.list(backupStorageLocation)) {
						for (var file : path.toList()) {
							// Delete the corresponding world
							deleteDirectory(worldStorageLocation.resolve(file.getFileName()));
						}
					}
					copyDirectory(backupStorageLocation, worldStorageLocation);
				} else {
					throw new RuntimeException("save backup is not a directory");
				}
			}
			copyDirectory(worldStorageLocation, backupStorageLocation);
		}
		activeBisect.safe(false);
	}

	public static void deleteDirectory(Path dir) throws IOException {
		if (Files.exists(dir)) {
			try (Stream<Path> pathStream = Files.walk(dir)) {
				for (Path path : (Iterable<? extends Path>) pathStream.sorted(Comparator.reverseOrder())::iterator) {
					Files.deleteIfExists(path);
				}
			}
		}
	}

	public static void copyDirectory(Path from, Path to) throws IOException {
		try (Stream<Path> paths = Files.walk(from)) {
			for (Path source : (Iterable<? extends Path>) paths::iterator) {
				Path destination = to.resolve(from.relativize(source));
				if (!Files.exists(destination)) {
					Files.copy(source, destination);
				}
			}
		}
	}

	public static HashMap<String, Path> getModOptions() throws IOException, ParseMetadataException {
		try (var files = Files.list(Path.of("modsToBisect"))) {
			var result = new HashMap<String, Path>();
			for (Iterator<Path> it = files.iterator(); it.hasNext(); ) {
				Path file = it.next();
				if (!Files.isRegularFile(file)) {
					continue;
				}
				result.put(extractModId(file), file);
			}
			return result;
		}
	}

	@SuppressWarnings("RedundantThrows") // Thrown in method invoked via reflection
	private static String extractModId(Path it) throws IOException, ParseMetadataException {
		try (var zip = new ZipInputStream(Files.newInputStream(it))) {
			for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				// I dont want to impl the mod id extraction by myself, so I'll let Quilt Loader internals do their job
				if (entry.getName().equals("fabric.mod.json")) {
					return ((net.fabricmc.loader.api.metadata.ModMetadata) QuiltLoader.class.getClassLoader().loadClass(
						"org.quiltmc.loader.impl.fabric.metadata.FabricModMetadataReader").getMethod(
						"parseMetadata",
						InputStream.class
					).invoke(null, zip)).getId();
				} else if (entry.getName().equals("quilt.mod.json")) {
					return ((ModMetadata) QuiltLoader.class.getClassLoader()
														   .loadClass(
															   "org.quiltmc.loader.impl.metadata.qmj.ModMetadataReader")
														   .getMethod("read", InputStream.class)
														   .invoke(null, zip)).id();
				}
			}
			throw new RuntimeException("Unknown mod type: " + it.getFileName());
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
				 InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private static SectionList calculateModSet(ActiveBisectConfig activeBisect) {
		Optional<ModSet> firstInvalidated = activeBisect.getFirstInvalidatedModSet();
		if (firstInvalidated.isPresent()) {
			return SectionList.fromSections(List.of(firstInvalidated.get().getFullSection()));
		} else {
			// find the smallest mod set where an unsolved issue is present -> this is the mod set we're trying to debug
			var modSet = testFixes(activeBisect);
			if (modSet.isPresent()) {
				return modSet.get();
			}

			ModSet.Erroring smallestIssueModSet = activeBisect.findSmallestUnfixedModSet();
			while (smallestIssueModSet != null) {
				modSet = debugIssueModSet(smallestIssueModSet, activeBisect);
				if (modSet.isPresent()) {
					return modSet.get();
				}

				modSet = testFixes(activeBisect);
				if (modSet.isPresent()) {
					return modSet.get();
				}

				smallestIssueModSet = activeBisect.findSmallestUnfixedModSet();
			}

			var fixes = BisectUtils.calculateFixes(activeBisect.issues.stream().map(issue -> issue.fix).toList());
			modSet = testAllFixes(fixes, activeBisect);
			if (modSet.isPresent()) {
				return modSet.get();
			}

			Set<String> smallestFix = fixes.stream().min(Comparator.comparingInt(Set::size)).orElse(Set.of());
			ModSet largestModSet = activeBisect.modSets.values()
													   .stream()
													   .max(Comparator.comparingInt(modSet1 -> modSet1.modSet.size()))
													   .orElse(new ModSet.Working(
														   new ArrayList<>(),
														   new ArrayList<>()
													   ));

			activeBisect.bisectSettings = null;
			return SectionList.from(largestModSet.modSet.stream().filter(it -> !smallestFix.contains(it)).toList());
		}
	}

	private static Optional<SectionList> debugIssueModSet(ModSet.Erroring issueModSet, ActiveBisectConfig activeBisect) {
		var result = new ArrayList<ModSet.Section>();
		var sections = issueModSet.sections();
		for (int i = 0; i < sections.size(); i++) {
			ModSet.Section section = sections.get(i);
			if (section.size() != 1) {
				result.addAll(sections.subList(i + 1, sections.size()));
				result.addAll(bisect(section, activeBisect, result.toArray(new ModSet.Section[]{})));
				return Optional.of(SectionList.fromSections(result));
			}
			result.add(section);
		}

		var reproductionSet = new ArrayList<String>();
		for (ModSet.Section section : result) {
			reproductionSet.add(section.getListCopy().get(0));
		}
		reproductionSet.sort(null);

		activeBisect.issues.get(issueModSet.issueId).fix.reproductions.add(reproductionSet);
		return Optional.empty();
	}

	private static Optional<SectionList> testFixes(ActiveBisectConfig activeBisect) {
		for (int fixedIssueId : activeBisect.findFixedIssues()) {
			Issue testedIssue = activeBisect.issues.get(fixedIssueId);
			ArrayList<ArrayList<String>> reproductions = testedIssue.fix.reproductions;

			List<ModSet> issueModSets = activeBisect.modSets.values()
															.stream()
															.filter(it -> it instanceof ModSet.Erroring erroring && erroring.issueId == fixedIssueId)
															.toList();
			// All mod sets of the issue that are not contained by any other mod set with the issue
			List<ModSet> largestIssueModSets = issueModSets.stream().filter(it -> issueModSets.stream()
																							  .noneMatch(other -> other != it && other.modSet.containsAll(
																								  it.modSet))).toList();
			var fixes = BisectUtils.mergeReproductions(reproductions);
			System.out.println(Arrays.toString(fixes.toArray()));
			for (var largestIssueModSet : largestIssueModSets) {
				for (var fix : fixes) {
					var fixedMods = new ArrayList<>(largestIssueModSet.modSet.stream()
																			 .filter(it -> !fix.contains(it))
																			 .toList());
					if (activeBisect.getModSet(fixedMods).isEmpty()) {
						return Optional.of(SectionList.from(fixedMods));
					}
				}
			}
		}
		return Optional.empty();
	}

	private static Optional<SectionList> testAllFixes(Set<Set<String>> fixes, ActiveBisectConfig activeBisectConfig) {
		List<ModSet> largestModSets = activeBisectConfig.modSets.values()
																.stream()
																.filter(it -> activeBisectConfig.modSets.values()
																										.stream()
																										.noneMatch(other -> other != it && other.modSet.containsAll(
																											it.modSet)))
																.toList();
		for (ModSet largestModSet : largestModSets) {
			for (Set<String> fix : fixes) {
				var fixedMods = new ArrayList<>(largestModSet.modSet.stream().filter(it -> !fix.contains(it)).toList());
				if (activeBisectConfig.getModSet(fixedMods).isEmpty()) {
					return Optional.of(SectionList.from(fixedMods));
				}
			}
		}
		return Optional.empty();
	}

	private static ArrayList<ModSet.Section> bisect(ModSet.Section parent, ActiveBisectConfig activeBisect, ModSet.Section... givenModSetSections) {
		// If the size is 1, we found one culprit, and can return early
		if (parent.size() == 1) {
			return new ArrayList<>(List.of(parent));
		}
		// Divide the mod set into halves
		for (ModSet.Section[] half : parent.getPossibleArrayHalveCombinations()) {
			Optional<ModSet> half0ModSet = activeBisect.getModSet(half[0], givenModSetSections);
			Optional<ModSet> half1ModSet = activeBisect.getModSet(half[1], givenModSetSections);
			if (modSetExistsAndIsWorkingOrFixed(half0ModSet, activeBisect) && modSetExistsAndIsWorkingOrFixed(
				half1ModSet,
				activeBisect
			)) {
				// If both have been tested (without any issue), two mods in the parent section must be required for the incompatibility. In this case treat both halves as different sections and bisect them.
				if (half[0].size() > 1) {
					var modSetSection = new ModSet.Section[givenModSetSections.length + 1];
					System.arraycopy(givenModSetSections, 0, modSetSection, 1, givenModSetSections.length);
					modSetSection[0] = half[1];
					var result = bisect(half[0], activeBisect, modSetSection);
					result.add(half[1]);
					return result;
				} else {
					var modSetSection = new ModSet.Section[givenModSetSections.length + 1];
					System.arraycopy(givenModSetSections, 0, modSetSection, 1, givenModSetSections.length);
					modSetSection[0] = half[0];
					var result = bisect(half[1], activeBisect, modSetSection);
					result.add(half[0]);
					return result;
				}
			} else if (modSetExistsAndIsWorkingOrFixed(half0ModSet, activeBisect)) {
				// If the first half already has been tested, test the other half
				return new ArrayList<>(Collections.singletonList(half[1]));
			} else if (modSetExistsAndIsWorkingOrFixed(half1ModSet, activeBisect)) {
				// If the second half already has been tested, test the other half
				return new ArrayList<>(Collections.singletonList(half[0]));
			}
		}
		// If no halves have been tested, test the first one
		return new ArrayList<>(Collections.singletonList(parent.getPossibleArrayHalveCombinations()[0][0]));
	}

	private static boolean modSetExistsAndIsWorkingOrFixed(Optional<ModSet> modSet, ActiveBisectConfig activeBisect) {
		return modSet.isPresent() && modSet.get().isWorkingOrFixed(activeBisect);
	}

	/**
	 * @param mods MUST BE SORTED if more than one section exists
	 */
	public static void loadModSet(QuiltPluginContext context, SectionList mods, HashMap<String, Path> loadOptions) throws IOException {
		var inBisectTreeNode = context.manager().getRootGuiNode().addChild(QuiltLoaderText.of("Quilt Bisect - Loaded"));
		var outOfBisectTreeNode = context.manager().getRootGuiNode().addChild(QuiltLoaderText.of(
			"Quilt Bisect - Only loaded via dependency"));
		for (Map.Entry<String, Path> modIdPathEntry : loadOptions.entrySet()) {
			if (mods.modIds().contains(modIdPathEntry.getKey())) {
				context.addFileToScan(
					modIdPathEntry.getValue(),
					inBisectTreeNode.addChild(QuiltLoaderText.of(modIdPathEntry.getValue().getFileName().toString())),
					true
				);
			} else {
				context.addFileToScan(
					modIdPathEntry.getValue(),
					outOfBisectTreeNode.addChild(QuiltLoaderText.of(modIdPathEntry.getValue()
																				  .getFileName()
																				  .toString())),
					false
				);
			}
		}

		if (mods.sectionIndices().size() == 1) {
			// Sorting is safe under these conditions
			mods.modIds().sort(null);
		} else {
			// TODO Check that mods is sorted
		}

		Files.writeString(ActiveBisectConfig.configDirectory.resolve("modSet.txt"), String.join("\n", mods.modIds()));
		Files.writeString(
			ActiveBisectConfig.configDirectory.resolve("sections.txt"),
			mods.sectionIndices().stream().map(Object::toString).collect(Collectors.joining("\n"))
		);
	}
}
