package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.plugin.gui.BisectPluginUi;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import org.quiltmc.loader.api.LoaderValue;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.plugin.QuiltLoaderPlugin;
import org.quiltmc.loader.api.plugin.QuiltPluginContext;
import org.quiltmc.loader.impl.fabric.metadata.ParseMetadataException;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class BisectPlugin implements QuiltLoaderPlugin {
	private static final PluginLogHandler LOGGER = PluginLogHandler.INSTANCE;
	private final LogFileManager logFileManager = new LogFileManager(Path.of("crash-reports"));
	private final BisectPluginProcessManager processManager = new BisectPluginProcessManager();

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
		if (!"true".equals(System.getProperty("quiltBisect.forked"))) {
            try {
                runParent();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
		}
        try {
            runChild(context);
        } catch (IOException | ParseMetadataException e) {
            throw new RuntimeException(e);
        }
    }

	private void runChild(QuiltPluginContext context) throws IOException, ParseMetadataException {
		Optional<ProcessHandle> parentProcess = ProcessHandle.current().parent();
		if (parentProcess.isEmpty()) {
			LOGGER.warn("Failed to get parent process: will keep running if a launcher terminates the game!");
		} else {
			processManager.parentExitWatchdog(parentProcess.get());
		}
		ActiveBisectConfig.update();
		var activeConfig = ActiveBisectConfig.getInstance();
		if (activeConfig.isActive()) {
			Bisect.childBisect(context);
		} else {
			var  options = Bisect.getModOptions();
			var fixes = BisectUtils.calculateFixes(activeConfig.issues.stream().map(issue -> issue.fix).toList());
			Set<String> smallestFix = fixes.stream().min(Comparator.comparingInt(Set::size)).orElse(Set.of());
			// Safe because only one section exists
			Bisect.loadModSet(context, SectionList.from(options.keySet().stream().filter(it -> !smallestFix.contains(it)).toList()), options);
		}
		System.setProperty("quiltBisect.active", "true");
	}

	@Override
	public void unload(Map<String, LoaderValue> data) {
		LOGGER.info("nooooo :(");
	}
}
