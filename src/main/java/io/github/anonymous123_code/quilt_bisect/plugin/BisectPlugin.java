package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import org.quiltmc.loader.api.LoaderValue;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.plugin.QuiltLoaderPlugin;
import org.quiltmc.loader.api.plugin.QuiltPluginContext;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;


public class BisectPlugin implements QuiltLoaderPlugin {
	private static final PluginLogHandler LOGGER = PluginLogHandler.INSTANCE;
	private final LogFileManager logFileManager = new LogFileManager(Path.of("crash-reports"));
	private final BisectPluginProcessManager processManager = new BisectPluginProcessManager();

	private void runParent() {
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
			if (config.bisectActive) {
				try {
					BisectUtils.parentBisect(crashLogFile.isEmpty() ? Optional.empty() : Optional.of(BisectUtils.readFile(crashLogFile.get())), crashLogFile.map(File::getName));
				} catch (IOException | NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				}
            } else {
				if (crashLogFile.isEmpty()) {
					System.exit(exitCode.get());
				} else {
					try {
						var crashLog = BisectUtils.readFile(crashLogFile.get());
						if (BisectPluginUi.openDialog(exitCode.get(), crashLog)) {
							BisectUtils.parentBisect(Optional.of(crashLog), Optional.of(crashLogFile.get().getName()));
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
			runParent();
			return;
		}
		runChild(context);
	}

	private void runChild(QuiltPluginContext context) {
		Optional<ProcessHandle> parentProcess = ProcessHandle.current().parent();
		if (parentProcess.isEmpty()) {
			LOGGER.warn("Failed to get parent process: will keep running if a launcher terminates the game!");
		} else {
			processManager.parentExitWatchdog(parentProcess.get());
		}

		LOGGER.info("hehe :3");
		System.setProperty("quiltBisect.active", "true");
	}

	@Override
	public void unload(Map<String, LoaderValue> data) {
		LOGGER.info("nooooo :(");
	}
}
