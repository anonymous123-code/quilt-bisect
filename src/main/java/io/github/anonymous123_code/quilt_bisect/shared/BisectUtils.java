package io.github.anonymous123_code.quilt_bisect.shared;

import org.jetbrains.annotations.NotNull;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class BisectUtils {

	public static final Pattern CLIENT_MOD_TABLE_REGEX = Pattern.compile("Quilt Mods: *\\n[^\\n]*\\n[ \\t]*[|\\-:]*\\n((?:[^\\n]*\\n)*[^\\n]*)\\n.*\\n[ \\t]*Mod Table Version");
	public static final Pattern QUILT_MOD_TABLE_REGEX = Pattern.compile("-- Mods --\\n.*\\n.*\\n[ \\t]*[|\\-:]*\\n((?:.*\\n)*.*)\\n[^\\n]*\\n[ \\t]*Mod Table Version");
	public static final Pattern MOD_ID_AND_FILE_NAME_REGEX = Pattern.compile("\\|[^|]*\\|[^|]*\\| *([a-zA-Z0-9_-]+) *\\|(?:.*\\|)*(?:[^|/]*/)*([^|/\\\\ ]+) *\\|[^|]*\\|$");

	public static String extractStackTrace(String crashLog) {
		Pattern r = Pattern.compile("Description:.*\n\n((?:.+\n)*)\n\nA detailed walkthrough of the error, its code path and all known details is as follows");
		var matcher = r.matcher(crashLog);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			throw new RuntimeException("Failed to extract stacktrace from crash log");
		}
	}

	public static String readFile(File file) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			return reader.lines().collect(StringBuilder::new, (stringBuilder, string) -> stringBuilder.append("\n").append(string), StringBuilder::append).toString();
		}
	}

	public static void parentBisect(Optional<String> crashLog, Optional<String> crashLogPath) throws IOException, NoSuchAlgorithmException {
		var config_dir = QuiltLoader.getConfigDir().resolve("bisect");
		var modset_path = config_dir.resolve("modSet.txt");
		HashMap<String, String> modSet;
		if (Files.exists(modset_path)) {
			modSet = readModSet(modset_path);
			Files.delete(modset_path);
		} else if (crashLog.isPresent()) {
			modSet = extractModSet(crashLog.get());
		} else throw new RuntimeException("In bisect, but neither mod set file nor crash log present");
		String modSetHash = generateModSetHash(modSet);
		copyLatestLog(modSetHash);
		var active_bisect = ActiveBisectConfig.getInstance();
		active_bisect.bisectActive = true;
		active_bisect.processRun(modSet, modSetHash, crashLog, crashLogPath);
		active_bisect.safe(false);
	}

	private static void copyLatestLog(String modSetHash) throws IOException {
		var logFilePath = Path.of("logs", "latest.log");
		if (Files.isRegularFile(logFilePath)) {
			Files.createDirectories(Path.of("bisectLogStorage"));
			Files.copy(logFilePath, Path.of("bisectLogStorage", modSetHash + ".log"), REPLACE_EXISTING);
		}
	}

	private static String generateModSetHash(HashMap<String, String> modSet) throws NoSuchAlgorithmException {
		return hash256(modSet.entrySet().stream().map((entry) -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
	}

	private static @NotNull HashMap<String, String> extractModSet(String crashLog) {
		var clientModTableMatcher = CLIENT_MOD_TABLE_REGEX.matcher(crashLog);
		String table_lines;
		if (clientModTableMatcher.find()) {
			table_lines = clientModTableMatcher.group(1);
		} else {
			var quiltModTableMatcher = QUILT_MOD_TABLE_REGEX.matcher(crashLog);
			if (quiltModTableMatcher.find()) {
				table_lines = quiltModTableMatcher.group(1);
			} else {
				throw new RuntimeException("Unable to extract mod table from crash log");
			}
		}
		return table_lines.lines().collect(HashMap::new, (map, line) -> {
			var matcher = MOD_ID_AND_FILE_NAME_REGEX.matcher(line.trim());
			if (matcher.matches()) {
				map.put(matcher.group(1), matcher.group(2));
			}
		}, HashMap::putAll);
	}

	private static @NotNull HashMap<String, String> readModSet(Path modSetPath) throws IOException {
		return readFile(modSetPath.toFile()).lines().collect(HashMap::new, (map, line) -> {
			var pair = line.split(":", 2);
			map.put(pair[0], pair[1]);
		}, HashMap::putAll);
	}

	public static String hash256(String data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(data.getBytes());
		return bytesToHex(md.digest());
	}

	public static String bytesToHex(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
		return result.toString();
	}
}
