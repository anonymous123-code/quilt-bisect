package io.github.anonymous123_code.quilt_bisect.plugin;

/*
 * Licensed under the MIT license by comp500 (2023). See the ModVote-License file in the ropository root.
 * Modifications:
 * - Extracted from plugin into extra class
 * - Fix compat with dev env
 * -
 * - adjusted package
 */

import net.fabricmc.api.EnvType;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.minecraft.MinecraftQuiltLoader;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BisectProcessManager {

	private static final PluginLogHandler LOGGER = PluginLogHandler.INSTANCE;
	private static volatile Process process = null;
	private static Thread shutdownHook = null;

	void parentExitWatchdog(ProcessHandle handle) {
		Thread t = new Thread(() -> {
			while (true) {
				if (!handle.isAlive()) {
					System.exit(1);
				}
				try {
					handle.onExit().get(1, TimeUnit.SECONDS);
				} catch (InterruptedException | ExecutionException ex) {
					break;
				} catch (TimeoutException ignored) {}
			}
		}, "ModVotePlugin-ParentExitWatchdog");
		t.setDaemon(true);
		t.start();
	}


	private static void cleanupProcess() {
		Process p = process; // Could be run concurrently!
		if (p != null) {
			p.destroy();
			try {
				p.onExit().get(1, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException("Failed to terminate forked process", e);
			} catch (TimeoutException ignored) {}
			p.destroyForcibly();
			process = null;
		}
	}

	private static void setupShutdownHook() {
		if (shutdownHook == null) {
			// Note: this isn't run when the JVM forcibly terminates (i.e. from a "kill" button in a launcher)
			//       The parent exit watchdog is a workaround for this issue (though it only works if the child process isn't hung)
			shutdownHook = new Thread(BisectProcessManager::cleanupProcess);
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
	}

	static Optional<Integer> fork(String[] mainArgs) {
		cleanupProcess();
		setupShutdownHook();

		LOGGER.info("VERY NORMAL NOTHING TO SEE HERE DEFINITELY NOT CREATING A NEW PROCESS");
		List<String> args = new ArrayList<>();

		String javaExec = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
		args.add(javaExec);
		args.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
		args.add("-DquiltBisect.forked=true");
		if (System.getProperty("quiltBisect.forkarg") != null) {
			args.add(System.getProperty("quiltBisect.forkarg"));
		}
		args.add("-cp");
		// Dev envs have a biiig class path, so they stick it in an argfile
		try {
			Path tempFile = Files.createTempFile("quiltBisect", ".txt");
			Files.writeString(tempFile, System.getProperty("java.class.path"));
			args.add("@" + tempFile);
		} catch (IOException ex) {
			LOGGER.warn("Failed to create argfile, passing classpath directly");
			args.add(System.getProperty("java.class.path"));
		}
		if (QuiltLoader.isDevelopmentEnvironment()) {
			args.add("net.fabricmc.devlaunchinjector.Main");
		} else if (MinecraftQuiltLoader.getEnvironmentType() == EnvType.CLIENT) {
			args.add("org.quiltmc.loader.impl.launch.knot.KnotClient");
		} else {
			args.add("org.quiltmc.loader.impl.launch.server.QuiltServerLauncher");
		}
		args.addAll(Arrays.asList(mainArgs));

		LOGGER.shutdown();
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.inheritIO();
		try {
			process = pb.start();
		} catch (IOException e) {
			throw new RuntimeException("Failed to fork self", e);
		}
		try {
			if (process == null) {
				return Optional.empty();
			}
			int exitCode = process.waitFor();
			return Optional.of(exitCode);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted when waiting for process", e);
		}
	}


}
