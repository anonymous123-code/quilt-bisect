package io.github.anonymous123_code.quilt_bisect.shared;

import java.io.*;
import java.util.regex.Pattern;

public class BisectUtils {

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
}
