package io.github.anonymous123_code.quilt_bisect.shared;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

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

	@NotNull
	public static Result getAutoJoinData() {
		String autoJoinName;
		AutoTest.AutoJoinType autoJoinMode = AutoTest.AutoJoinType.None;
		if (Files.exists(ActiveBisectConfig.configDirectory.resolve("lastActiveJoin.txt"))) {
			String[] s;
			try {
				s = Files.readString(ActiveBisectConfig.configDirectory.resolve("lastActiveJoin.txt")).split("\n", 2);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			autoJoinMode = AutoTest.AutoJoinType.from(s[0]);
			autoJoinName = autoJoinMode != AutoTest.AutoJoinType.None ? s[1] : "";
		} else {
			autoJoinName = "";
		}
		return new Result(autoJoinName, autoJoinMode);
	}

	public record Result(String autoJoinName, AutoTest.AutoJoinType autoJoinMode) {}
}
