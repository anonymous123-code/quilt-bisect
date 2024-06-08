package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.plugin.gui.BisectPluginUi;
import io.github.anonymous123_code.quilt_bisect.shared.AbstractBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import io.github.anonymous123_code.quilt_bisect.shared.MainConfig;
import io.github.anonymous123_code.quilt_bisect.shared.StacktraceExtractionFailException;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.*;
import org.quiltmc.loader.api.gui.QuiltLoaderText;
import org.quiltmc.loader.api.plugin.ModMetadataExt;
import org.quiltmc.loader.api.plugin.QuiltLoaderPlugin;
import org.quiltmc.loader.api.plugin.QuiltPluginContext;
import org.quiltmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.quiltmc.loader.api.plugin.solver.ModLoadOption;
import org.quiltmc.loader.api.plugin.solver.ModSolveResult;
import org.quiltmc.loader.impl.plugin.quilt.ProvidedModOption;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;


public class BisectPlugin implements QuiltLoaderPlugin {
	private static final PluginLogHandler LOGGER = PluginLogHandler.INSTANCE;
	private final LogFileManager logFileManager = new LogFileManager(Path.of(MainConfig.INSTANCE.crashReportsDirectory));
	private final BisectPluginProcessManager processManager = new BisectPluginProcessManager();

	private QuiltPluginContext context;

	private static SectionList calculateSectionList(ArrayList<ModLoadOption> modSet) {
		SectionList sections;
		var activeConfig = ActiveBisectConfig.INSTANCE;
		activeConfig.update();
		if (activeConfig.isActive()) {
			try {
				sections = Bisect.childBisect(modSet);
				ActiveBisectConfig.INSTANCE.safe();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			var fixes = BisectUtils.calculateFixes(activeConfig.issues.stream().map(issue -> issue.fix).toList());
			Set<String> smallestFix = fixes.stream().min(Comparator.comparingInt(Set::size)).orElse(Set.of());
			// Safe because only one section exists
			sections = SectionList.from(modSet.stream()
											  .map(ModLoadOption::id)
											  .filter(it -> !smallestFix.contains(it))
											  .toList());
		}

		if (sections.sectionIndices().size() == 1) {
			// Sorting is safe under these conditions
			sections.modIds().sort(null);
		} else {
			// TODO Check that mods is sorted
		}

		return sections;
	}

	private void runParent() throws IOException {
		LOGGER.info("preparing to invoke great evils");
		logFileManager.scan();
		Optional<Integer> exitCode = processManager.fork(QuiltLoader.getLaunchArguments(false));
		while (exitCode.isPresent()) {
			if (exitCode.get() == 0) {
				System.exit(0);
			}

			ActiveBisectConfig config = ActiveBisectConfig.INSTANCE;
			config.update();

			var crashLogFile = logFileManager.getNew();
			if (config.isActive()) {
				try {
					Bisect.parentBisect(
						crashLogFile.isEmpty() ? null : Files.readString(crashLogFile.get()),
						crashLogFile.map(file -> file.getFileName().toString()).orElse(null)
					);
				} catch (IOException | NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				} catch (InvalidCommunicationFileStateException e) {
					context.reportError(QuiltLoaderText.of(e.titleText())).appendDescription(QuiltLoaderText.of(
						"This is likely caused by an error in bisect itself or by another loader plugin."));
					context.haltLoading();
				} catch (StacktraceExtractionFailException e) {
					var error = context.reportError(QuiltLoaderText.of(
										   "Bisect: Failed to extract stack trace from crash log."))
									   .appendDescription(QuiltLoaderText.of(
										   "Please open an issue with the log attached"));
					//noinspection OptionalGetWithoutIsPresent
					error.addCopyFileToClipboardButton(
						QuiltLoaderText.of("Copy crash log file"),
						crashLogFile.get()
					);
					error.addOpenLinkButton(
						QuiltLoaderText.of("Create new issue"),
						"https://github.com/anonymous123-code/quilt-bisect/issues/new"
					);
					context.haltLoading();
				}
			} else {
				if (crashLogFile.isEmpty()) {
					System.exit(exitCode.get());
				} else {
					try {
						var crashLog = Files.readString(crashLogFile.get());
						var active_bisect = ActiveBisectConfig.INSTANCE;
						active_bisect.bisectSettings = BisectPluginUi.openDialog(exitCode.get(), crashLog);
						if (active_bisect.isActive()) {
							Bisect.parentBisect(crashLog, crashLogFile.get().getFileName().toString());
						} else {
							System.exit(exitCode.get());
						}
					} catch (UnsupportedLookAndFeelException | IOException | ClassNotFoundException |
							 InstantiationException | IllegalAccessException | NoSuchAlgorithmException e) {
						throw new RuntimeException(e);
					} catch (InvalidCommunicationFileStateException e) {
						context.reportError(QuiltLoaderText.of(e.titleText())).appendDescription(QuiltLoaderText.of(
							"This is likely caused by an error in bisect itself or by another loader plugin."));
						context.haltLoading();
					} catch (StacktraceExtractionFailException e) {
						var error = context.reportError(QuiltLoaderText.of(
											   "Bisect: Failed to extract stack trace from crash log."))
										   .appendDescription(QuiltLoaderText.of(
											   "Please open an issue with the log attached"));
						error.addCopyFileToClipboardButton(
							QuiltLoaderText.of("Copy crash log file"),
							crashLogFile.get()
						);
						error.addOpenLinkButton(
							QuiltLoaderText.of("Create new issue"),
							"https://github.com/anonymous123-code/quilt-bisect/issues/new"
						);
						context.haltLoading();
					}
				}
			}
			exitCode = processManager.fork(QuiltLoader.getLaunchArguments(false));
		}
		System.exit(1);
	}

	private void runChild() {
		Optional<ProcessHandle> parentProcess = ProcessHandle.current().parent();
		if (parentProcess.isEmpty()) {
			LOGGER.warn("Failed to get parent process: will keep running if a launcher terminates the game!");
		} else {
			processManager.parentExitWatchdog(parentProcess.get());
		}
		System.setProperty("quiltBisect.active", "true");
	}

	@Override
	public void load(QuiltPluginContext context, Map<String, LoaderValue> previousData) {
		this.context = context;
		try {
			MainConfig.INSTANCE.safe();
		} catch (IOException e) {
			throw new RuntimeException("Unable to write main config", e);
		}
		if (!"true".equals(System.getProperty("quiltBisect.forked"))) {
			try {
				runParent();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return;
		}
		runChild();
	}

	@Override
	public void beforeSolve() {
		smuggleRemovedModContainerStore(context);
		LOGGER.info("Discovering mods...");
		ArrayList<ModLoadOption> loadOptions = new ArrayList<>();
		Map<PluginGuiTreeNode, ModLoadOption> treeNodeModLoadOptionMap = new HashMap<>();
		for (String modId : context.manager().getModIds()) {
			for (ModLoadOption loadOption : context.manager().getAllMods(modId)) {
				treeNodeModLoadOptionMap.put(context.manager().getGuiNode(loadOption), loadOption);
			}
			if (!MainConfig.INSTANCE.bisectSkipMods.contains(modId)) {
				for (ModLoadOption loadOption : context.manager().getAllMods(modId)) {
					if (loadOption instanceof ProvidedModOption) {
						continue;
					}
					loadOptions.add(loadOption);
				}
			} else {
				LOGGER.info("Skipping mod id " + modId);
			}
		}
		LOGGER.info("Analyzing JiJ");
		Map<ModLoadOption, ArrayList<ModLoadOption>> jij = new HashMap<>();
		ArrayList<ModLoadOption> modSet = new ArrayList<>();
		for (ModLoadOption loadOption : loadOptions) {
			jij.putIfAbsent(loadOption, new ArrayList<>());
			if (!loadOption.isMandatory()) { // Assume the mod was JiJed
				ModLoadOption parent = this.findParentLoadOption(loadOption, treeNodeModLoadOptionMap);
				if (parent != null) {
					jij.putIfAbsent(parent, new ArrayList<>());
					jij.get(parent).add(loadOption);
					continue;
				}
				LOGGER.warn("Containing mod not found for mod " + loadOption + ", assuming its a top-level mod.");
			}
			modSet.add(loadOption);
		}

		LOGGER.info("Calculating mod set");
		SectionList sections = calculateSectionList(modSet);

		LOGGER.info("Modifying mods...");
		PluginGuiTreeNode node = context.manager().getRootGuiNode().addChild(QuiltLoaderText.of(
			"Made Optional by Quilt Bisect - should appear somewhere else as well"));
		for (ModLoadOption loadOption : modSet) {
			if (!sections.modIds().contains(loadOption.id())) {
				makeOptional(jij, loadOption, node);
			}
		}

		try {
			Files.writeString(
				AbstractBisectConfig.configDirectory.resolve("modSet.txt"),
				String.join("\n", sections.modIds())
			);
			Files.writeString(
				AbstractBisectConfig.configDirectory.resolve("sections.txt"),
				sections.sectionIndices().stream().map(Object::toString).collect(Collectors.joining("\n"))
			);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write mod set comms files", e);
		}
	}

	private void smuggleRemovedModContainerStore(QuiltPluginContext context) {
		// See ReflectiveRemovedModContainerStore
		var javaLoadOption = context.manager().getAllMods("java").iterator().next();
		var javaClassLoader = javaLoadOption.getClass().getClassLoader();
		try {
			var internalModOptionBaseClass = javaClassLoader.loadClass("org.quiltmc.loader.impl.plugin.base.InternalModOptionBase");
			if (internalModOptionBaseClass.isAssignableFrom(javaLoadOption.getClass())) {
				var field = internalModOptionBaseClass.getDeclaredField("metadata");
				field.setAccessible(true);
				field.set(javaLoadOption, new SmuggleModMetadataExt(javaLoadOption.metadata(), RemovedModContainerStoreImpl.getInstance()));
			} else {
				throw new RuntimeException("Java load option class doesnt seem to be a InternalModLoadOptionBase");
			}
		} catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void finish(ModSolveResult result) {
		LOGGER.error("LoaderPlugin.finish was called. This is not expected. It is likely that Quilt Bisect is out of date with quilt loader");
	}

	public @Nullable ModLoadOption findParentLoadOption(ModLoadOption loadOption, Map<PluginGuiTreeNode, ModLoadOption> treeNodeModLoadOptionMap) {
		var parent = context.manager().getGuiNode(loadOption).parent();
		while (parent != null) {
			var parentLoadOption = treeNodeModLoadOptionMap.get(parent);
			if (parentLoadOption != null) {
				return parentLoadOption;
			} else {
				parent = parent.parent();
			}
		}
		return null;
	}

	private void makeOptional(Map<ModLoadOption, ArrayList<ModLoadOption>> jij, ModLoadOption loadOption, PluginGuiTreeNode node) {
		var modNode = node.addChild(QuiltLoaderText.of(loadOption.id()));
		ArrayList<ModDependencyIdentifier> dependedIds = new ArrayList<>();
		for (ModLoadOption nestedLoadOption : jij.get(loadOption)) {
			if (nestedLoadOption.metadata().loadType() != ModMetadataExt.ModLoadType.IF_REQUIRED) {
				dependedIds.add(ModDependencyIdentifier.of(nestedLoadOption.group(), nestedLoadOption.id()));
				makeOptional(jij, nestedLoadOption, modNode);
			}
		}
		RemovedModContainerStoreImpl.getInstance().addRemovedMod(loadOption);
		context.ruleContext().removeOption(loadOption);
		context.manager()
			   .getGuiNode(loadOption)
			   .addChild(QuiltLoaderText.of("Made optional by Quilt Bisect, see below"));
		var newLoadOption = new WrappedModLoadOption(loadOption, false, ModMetadataExt.ModLoadType.IF_REQUIRED);
		for (ModDependencyIdentifier id : dependedIds) {
			newLoadOption.metadata().depends().add(new ModDependency.Only() { // TODO Custom class?
				@Override
				public boolean shouldIgnore() {
					return false;
				}

				@Override
				public ModDependencyIdentifier id() {
					return id;
				}

				@Override
				public VersionRange versionRange() {
					return VersionRange.ANY;
				}

				@Override
				public String reason() {
					return "Quilt Bisect: Automatically generated because a mod with that data was embedded";
				}

				@Override
				public @Nullable ModDependency unless() {
					return null;
				}

				@Override
				public boolean optional() {
					return false;
				}
			});
		}
		context.addModLoadOption(newLoadOption, modNode);
	}

	@Override
	public @Nullable Boolean isHigherPriorityThan(Path path, List<ModLoadOption> thisOptions, String otherPluginId, List<ModLoadOption> otherOptions) {
		return true;
	}

	@Override
	public void unload(Map<String, LoaderValue> data) {
		LOGGER.warn("Quilt Bisect unloaded... Never tested this case, should just work tm");
	}
}
