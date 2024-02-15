package io.github.anonymous123_code.quilt_bisect.shared;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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

	public static Set<Set<String>> mergeReproductions(ArrayList<ArrayList<String>> reproductions) {
		// Reproduction takes the form (a ∧ b ∧ c)∨(a ∧ b ∧ d)∨(a ∧ e) (Disjunctions of conjunctions, notably without negations)
		// Assuming that that term is equivalent to the issue occurring, we can negate it to get the term for the issue not occurring
		// ¬((a ∧ b ∧ ...)∨...)=(¬a ∨ ¬b ∨ ...)∧... (Conjunctions of disjunctions of negations, all elements inside the disjunction are negated)
		Set<Set<String>> mergedFixes = new HashSet<>();
		// Should this ever become a performance issue, it's complexity might be reducible to log(len(reproductions)) using a divide and conquer algorithm
		for (var reproduction : reproductions) {
			if (mergedFixes.isEmpty()) {
				for (var mod : reproduction) {
					HashSet<String> fix = new HashSet<>();
					fix.add(mod);
					mergedFixes.add(fix);
				}
			} else {
				Set<Set<String>> newFixes = new HashSet<>();
				for (var mod : reproduction) {
					for (Set<String> fix : mergedFixes) {
						var newVariant = new HashSet<>(fix);
						newVariant.add(mod);
						newFixes.add(newVariant);
					}
				}
				mergedFixes = newFixes;
			}
		}
		return simplifyFixes(mergedFixes);
	}

	public static Set<Set<String>> mergeFixes(Set<Set<String>> first, Set<Set<String>> second) {
		Set<Set<String>> result = new HashSet<>();
		for (var fix1 : first) {
			for (var fix2 : second) {
				var mergedFix = new HashSet<>(fix1);
				mergedFix.addAll(fix2);
				result.add(mergedFix);
			}
		}
		return simplifyFixes(result);
	}

	private static Set<Set<String>> simplifyFixes(Set<Set<String>> fixes) {
		Set<Set<String>> simplifiedFixes = new HashSet<>();

		OUTER_FOR:
		for (var fix : fixes) {
			for (var other : fixes) {
				if (other != fix && fix.containsAll(other)) {
					continue OUTER_FOR;
				}
			}
			simplifiedFixes.add(fix);
		}

		return simplifiedFixes;
	}

	public record Result(String autoJoinName, AutoTest.AutoJoinType autoJoinMode) {}
}
