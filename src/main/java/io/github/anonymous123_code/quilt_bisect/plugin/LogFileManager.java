package io.github.anonymous123_code.quilt_bisect.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class LogFileManager {

	private final Path crashLogPath;
	ArrayList<Path> knownCrashLogs = new ArrayList<>();

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

	public void scan() throws IOException {
		try (var files = Files.list(crashLogPath)) {
			files.filter(Files::isRegularFile).forEach(knownCrashLogs::add);
		}
	}

	public Optional<Path> getNew() throws IOException {
		try (var files = Files.list(crashLogPath)) {
			var newFileSet = files.filter(Files::isRegularFile).filter((item) -> knownCrashLogs.stream().noneMatch(file -> file.getFileName().equals(item.getFileName()))).collect(Collectors.toSet());
			knownCrashLogs.addAll(newFileSet);
			if (newFileSet.size() == 1) {
				return Optional.of(newFileSet.iterator().next());
			}
			return Optional.empty();
		}
	}
}
