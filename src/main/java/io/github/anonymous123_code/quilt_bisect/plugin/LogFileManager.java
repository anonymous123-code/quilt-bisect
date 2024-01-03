package io.github.anonymous123_code.quilt_bisect.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class LogFileManager {

	private final Path crashLogPath;
	ArrayList<File> knownCrashLogs = new ArrayList<>();

	public LogFileManager(Path crashLogPath) {
		if (!Files.exists(crashLogPath)) {
			try {
				Files.createDirectories(crashLogPath);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to create directory '%s'", crashLogPath), e);
			}
		}
		this.crashLogPath = crashLogPath;
	}

	public void scan() {
		Arrays.stream(Objects.requireNonNull(crashLogPath.toFile().listFiles())).filter(File::isFile).forEach(knownCrashLogs::add);
	}

	public Optional<File> getNew() {
		var newFileSet = Arrays.stream(Objects.requireNonNull(crashLogPath.toFile().listFiles())).filter(File::isFile).filter((item) -> knownCrashLogs.stream().noneMatch(file -> file.getName().equals(item.getName()))).collect(Collectors.toSet());
		knownCrashLogs.addAll(newFileSet);
		if (newFileSet.size() == 1) {
			return Optional.of(newFileSet.iterator().next());
		}
		return Optional.empty();
	}
}
