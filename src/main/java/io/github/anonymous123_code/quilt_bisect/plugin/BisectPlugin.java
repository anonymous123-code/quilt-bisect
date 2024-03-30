package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.plugin.gui.BisectPluginUi;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import org.quiltmc.loader.api.LoaderValue;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.gui.QuiltLoaderText;
import org.quiltmc.loader.api.plugin.ModMetadataExt;
import org.quiltmc.loader.api.plugin.QuiltLoaderPlugin;
import org.quiltmc.loader.api.plugin.QuiltPluginContext;
import org.quiltmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.quiltmc.loader.api.plugin.solver.ModLoadOption;
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
	private final LogFileManager logFileManager = new LogFileManager(Path.of("crash-reports"));
	private final BisectPluginProcessManager processManager = new BisectPluginProcessManager();

	private QuiltPluginContext context;

	private void runParent() throws IOException {
		LOGGER.info("preparing to invoke great evils");
		logFileManager.scan();
		Optional<Integer> exitCode = processManager.fork(QuiltLoader.getLaunchArguments(false));
		while (exitCode.isPresent()) {
			if (exitCode.get() == 0) {
				System.exit(0);
			}

			ActiveBisectConfig.update();
			ActiveBisectConfig config = ActiveBisectConfig.getInstance();

			var crashLogFile = logFileManager.getNew();
			if (config.isActive()) {
				try {
					Bisect.parentBisect(crashLogFile.isEmpty() ? null : Files.readString(crashLogFile.get()), crashLogFile.map(file -> file.getFileName().toString()).orElse(null));
				} catch (IOException | NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				}
            } else {
				if (crashLogFile.isEmpty()) {
					System.exit(exitCode.get());
				} else {
					try {
						var crashLog = Files.readString(crashLogFile.get());
						var active_bisect = ActiveBisectConfig.getInstance();
						active_bisect.bisectSettings = BisectPluginUi.openDialog(exitCode.get(), crashLog);
						if (active_bisect.isActive()) {
							Bisect.parentBisect(crashLog, crashLogFile.get().getFileName().toString());
						} else {
							System.exit(exitCode.get());
						}
					} catch (UnsupportedLookAndFeelException | IOException | ClassNotFoundException |
                             InstantiationException | IllegalAccessException | NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }
			}
			exitCode = processManager.fork(QuiltLoader.getLaunchArguments(false));
		}
		System.exit(1);
	}

	@Override
	public void load(QuiltPluginContext context, Map<String, LoaderValue> previousData) {
		this.context = context;
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
	public void beforeSolve() {
		LOGGER.info("Discovering mods...");
		ArrayList<ModLoadOption> loadOptions = new ArrayList<>();
		List<ModLoadOption> allLoadOptions = new ArrayList<>();
		for (String modId : context.manager().getModIds()) {
			allLoadOptions.addAll(context.manager().getAllMods(modId));
			switch (modId) {
				case "quilt_bisect", "minecraft", "java", "quilt_loader", "" -> LOGGER.info("Skipping mod id " + modId);
				default -> {
					for (ModLoadOption loadOption : context.manager().getAllMods(modId)) {
						if (loadOption instanceof ProvidedModOption) continue;
						LOGGER.info("Found mod " + loadOption.id() + " from " + loadOption.from());
						loadOptions.add(loadOption);
					}
				}
			}
		}
		LOGGER.info("Discovering JiJ");
		// FIXME: Proper JiJ Analysis. Must set the dependency of the parent mod if it is made optional
		Map<ModLoadOption, ArrayList<ModLoadOption>> jij = new HashMap<>();
		for (ModLoadOption loadOption : loadOptions) {
			if (loadOption.isMandatory()) {
				jij.put(loadOption, new ArrayList<>());
			}
		}

		ActiveBisectConfig.update();
		var activeConfig = ActiveBisectConfig.getInstance();
		SectionList sections;
		if (activeConfig.isActive()) {
			LOGGER.info("Calculating mod set");
			try {
				sections = Bisect.childBisect(jij.keySet().stream().toList());
				ActiveBisectConfig.getInstance().safe(false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			var options = jij.keySet();
			var fixes = BisectUtils.calculateFixes(activeConfig.issues.stream().map(issue -> issue.fix).toList());
			Set<String> smallestFix = fixes.stream().min(Comparator.comparingInt(Set::size)).orElse(Set.of());
			// Safe because only one section exists
			sections = SectionList.from(options.stream().map(ModLoadOption::id).filter(it -> !smallestFix.contains(it)).toList());
		}

		if (sections.sectionIndices().size() == 1) {
			// Sorting is safe under these conditions
			sections.modIds().sort(null);
		} else {
			// TODO Check that mods is sorted
		}

		LOGGER.info("Modifying mods...");
		PluginGuiTreeNode node = context.manager().getRootGuiNode().addChild(QuiltLoaderText.of("Made Optional by Quilt Bisect - should appear somewhere else as well"));
		for (ModLoadOption loadOption : loadOptions) {
			if (!sections.modIds().contains(loadOption.id())) {
				context.ruleContext().removeOption(loadOption);
				context.manager().getGuiNode(loadOption).addChild(QuiltLoaderText.of("Made optional by Quilt Bisect, see below"));
				context.addModLoadOption(new WrappedModLoadOption(loadOption, false, ModMetadataExt.ModLoadType.IF_REQUIRED), node.addChild(QuiltLoaderText.of(loadOption.id())));
			}
		}

		try {
			Files.writeString(ActiveBisectConfig.configDirectory.resolve("modSet.txt"), String.join("\n", sections.modIds()));
			Files.writeString(
				ActiveBisectConfig.configDirectory.resolve("sections.txt"),
				sections.sectionIndices().stream().map(Object::toString).collect(Collectors.joining("\n"))
			);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void unload(Map<String, LoaderValue> data) {
		LOGGER.info("nooooo :(");
	}
}
