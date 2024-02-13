package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import io.github.anonymous123_code.quilt_bisect.shared.ModSet;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.loader.api.ModMetadata;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.gui.QuiltLoaderText;
import org.quiltmc.loader.api.plugin.QuiltPluginContext;
import org.quiltmc.loader.impl.fabric.metadata.ParseMetadataException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Bisect {
	public static void parentBisect(Optional<String> crashLog, Optional<String> crashLogPath) throws IOException, NoSuchAlgorithmException {
		var config_dir = QuiltLoader.getConfigDir().resolve("bisect");
		var modset_path = config_dir.resolve("modSet.txt");
		var sections_path = config_dir.resolve("sections.txt");
		List<String> modSet = readModSet(modset_path);
		List<Integer> sections = readSections(sections_path);


		String modSetHash = generateModSetHash(modSet);
		copyLatestLog(modSetHash);
		var active_bisect = ActiveBisectConfig.getInstance();
		active_bisect.processRun(modSet, sections, modSetHash, crashLog, crashLogPath);
		active_bisect.safe(false);
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
		activeBisect.safe(false);
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

	private static List<ModSet.Section> calculateModSet(ActiveBisectConfig activeBisect) {
		Optional<ModSet> firstInvalidated = activeBisect.getFirstInvalidatedModSet();
		if (firstInvalidated.isPresent()) {
			return List.of(firstInvalidated.get().getFullSection());
		} else {
			// find the smallest mod set where an unsolved issue is present -> this is the mod set we're trying to debug
			ModSet.Erroring smallestIssueModSet = activeBisect.findSmallestUnfixedModSet();
			var result = new ArrayList<ModSet.Section>();
			var sections = smallestIssueModSet.sections();
			for (int i = 0; i < sections.size(); i++) {
				ModSet.Section section = sections.get(i);

				if (section.size() != 1) {
					result.addAll(sections.subList(i + 1, sections.size()));
					result.addAll(bisect(section, activeBisect, result.toArray(new ModSet.Section[]{})));
					return result;
				}

				result.add(section);
			}
			var reproductionSet = new ArrayList<String>();
			for (ModSet.Section section : result) {
				reproductionSet.add(section.getListCopy().get(0));
			}
			activeBisect.issues.get(smallestIssueModSet.issueId).fix.reproductions.add(reproductionSet);
			// TODO: Verify reproduction: Calculate Fixes: And
			// TODO handle multiple issues and what to do when done
			activeBisect.bisectSettings = null;
			return new ArrayList<>();
		}
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

	private static void loadModSet(QuiltPluginContext context, List<ModSet.Section> mods, HashMap<String, Path> loadOptions) throws IOException {
		mods.sort(Comparator.comparingInt(ModSet.Section::start));

		var sectionIndices = new ArrayList<Integer>(mods.size());
		sectionIndices.add(0);
		var flatMapModIds = new ArrayList<String>();
		for (ModSet.Section modSection : mods) {
			sectionIndices.add(flatMapModIds.size() + modSection.size());
			flatMapModIds.addAll(modSection.getListCopy());
		}
		sectionIndices.remove(sectionIndices.size() - 1);

		loadModSet(context, flatMapModIds, sectionIndices, loadOptions);
	}

	/**
	 * @param mods MUST BE SORTED if more than one section exists
	 * @param sectionIndices must be empty or get(0) == 0
	 */
	public static void loadModSet(QuiltPluginContext context, List<String> mods, List<Integer> sectionIndices, HashMap<String, Path> loadOptions) throws IOException {
		var inBisectTreeNode = context.manager().getRootGuiNode().addChild(QuiltLoaderText.of("Quilt Bisect - Loaded"));
		var outOfBisectTreeNode = context.manager().getRootGuiNode().addChild(QuiltLoaderText.of(
			"Quilt Bisect - Only loaded via dependency"));
		for (Map.Entry<String, Path> modIdPathEntry : loadOptions.entrySet()) {
			if (mods.contains(modIdPathEntry.getKey())) {
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

		if (sectionIndices.size() == 1) {
			// Sorting is safe under these conditions
			mods.sort(null);
		} else {
			// TODO Check that mods is sorted
		}

		Files.writeString(ActiveBisectConfig.configDirectory.resolve("modSet.txt"), String.join("\n", mods));
		Files.writeString(
			ActiveBisectConfig.configDirectory.resolve("sections.txt"),
			sectionIndices.stream().map(Object::toString).collect(Collectors.joining("\n"))
		);
	}
}
