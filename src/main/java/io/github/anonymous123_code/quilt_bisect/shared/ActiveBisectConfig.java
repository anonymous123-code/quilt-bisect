package io.github.anonymous123_code.quilt_bisect.shared;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

public class ActiveBisectConfig {
	public static final Path configDirectory = QuiltLoader.getConfigDir().resolve("bisect");
	private static ActiveBisectConfig INSTANCE = create();

	public static ActiveBisectConfig getInstance() {
		return INSTANCE;
	}

	private ActiveBisectConfig() {
		bisectActive = false;
		modSets = new HashMap<>();
		issues = new ArrayList<>();
	}

	private static ActiveBisectConfig create() {
		if (!Files.exists(configDirectory)) {
			try {
				Files.createDirectories(configDirectory);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to create directory '%s'", configDirectory), e);
			}
		}
		var config_file = configDirectory.resolve("active_bisect.json");
		if (Files.exists(config_file) && Files.isRegularFile(config_file)) {
			return serializeFromFile(config_file);
		} else {
			return new ActiveBisectConfig();
		}
	}

	public static void update() {
		INSTANCE = create();
	}

	public boolean isUpToDate() {
		return INSTANCE == this;
	}

	public boolean safe(boolean force)  {
		if (!force && !isUpToDate()) return false;
		var config_dir = QuiltLoader.getConfigDir().resolve("bisect");
		var config_file = config_dir.resolve("active_bisect.json");
		var gson = new GsonBuilder().setPrettyPrinting().create();
		try (var file = new FileOutputStream(config_file.toFile())) {
			file.write(gson.toJson(this).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private static ActiveBisectConfig serializeFromFile(Path configFile) {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ModSet.class, (JsonDeserializer<ModSet>) (jsonElement, type, jsonDeserializationContext) -> {
				if (jsonElement.getAsJsonObject().get("working").getAsBoolean()) {
					return new Gson().fromJson(jsonElement, WorkingModSet.class);
				} else {
					return new Gson().fromJson(jsonElement, ErroringModSet.class);
				}
			})
			.registerTypeAdapter(Issue.class, (JsonDeserializer<Issue>) (jsonElement, type, jsonDeserializationContext) -> {
				var name = jsonElement.getAsJsonObject().get("type").getAsString();
				switch (name) {
					case "crash" -> {
						return new Gson().fromJson(jsonElement, CrashIssue.class);
					}
					case "log" -> {
						return new Gson().fromJson(jsonElement, LogIssue.class);
					}
					case "user" -> {
						return new Gson().fromJson(jsonElement, UserIssue.class);
					}
					default -> {
						throw new JsonParseException("Invalid Type");
					}
				}
			})
			.create();
		try {
			return gson.fromJson(new FileReader(configFile.toFile()), ActiveBisectConfig.class);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}


	public boolean bisectActive = false;
	public HashMap<String, ModSet> modSets = new HashMap<>();
	public ArrayList<Issue> issues = new ArrayList<>();
	public HashMap<String, String> modIdToFile = new HashMap<>();


	public void processRun(HashMap<String, String> modSetToPath, String modSetHash, Optional<String> crashLog, Optional<String> crashLogPath) {
		updateFiles(modSetToPath);
		Optional<Integer> issue = getOrAddIssue(crashLog);
		ModSet modSet = issue.isPresent() ? new ErroringModSet(modSetToPath.keySet(), issue.get(), crashLogPath.orElse("")) : new WorkingModSet(modSetToPath.keySet());
		modSets.put(modSetHash, modSet);
	}

	private void updateFiles(HashMap<String, String> modSetToPath) {
		for (var entry : modSetToPath.entrySet()) {
			if (modIdToFile.containsKey(entry.getKey())) {
				if (!modIdToFile.get(entry.getKey()).equals(entry.getValue())) {
					for (var modSet: modSets.values()) {
						if (modSet.modSet.contains(entry.getKey())) modSet.invalidated = true;
					}
					modIdToFile.put(entry.getKey(), entry.getValue());
				}
			} else {
				modIdToFile.put(entry.getKey(), entry.getValue());
			}
		}
	}

	public Optional<Integer> getOrAddIssue(Optional<String> crashLog) {
		var issuePath = configDirectory.resolve("issue.txt");
		if (Files.exists(issuePath)) {
			String issueData;
			try {
				issueData = BisectUtils.readFile(issuePath.toFile());
				Files.delete(issuePath);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return Optional.of(Integer.parseInt(issueData, 10));
		} else if (crashLog.isPresent()){
			issues.add(new CrashIssue(BisectUtils.extractStackTrace(crashLog.get())));
			return Optional.of(issues.size() - 1);
		} else return Optional.empty();
	}

	public static abstract class ModSet {
		protected ModSet(boolean working, Set<String> modSet) {
			this.working = working;
			this.invalidated = false;
			this.modSet = modSet;
		}

		public boolean isWorking() {
			return working;
		}

		public final boolean working;
		public boolean invalidated;
		public final Set<String> modSet;
	}

	public static class WorkingModSet extends ModSet {
		public WorkingModSet(Set<String> modSet) {
			super(true, modSet);
		}
	}

	public static class ErroringModSet extends ModSet {
		public final int issueId;
		public final String crashLogPath;

		public ErroringModSet(Set<String> modSet, int issueId, String crashLogPath) {
			super(false, modSet);
			this.issueId = issueId;
			this.crashLogPath = crashLogPath;
		}
	}

	public static abstract class Issue {
		public String world;
		public String server;
		public IssueType type;

		private Issue(IssueType type, String server, String world) {
			if (!(server.isEmpty() || world.isEmpty())) throw new IllegalArgumentException("Server or World must be empty");
			this.type = type;
			this.server = server;
			this.world = world;
		}
	}

	public enum IssueType {
		@SerializedName("crash")
		CRASH(),
		@SerializedName("log")
		LOG(),
		@SerializedName("user")
		USER();
	}

	public static class CrashIssue extends Issue {
		public final String stacktrace;

		CrashIssue(String stacktrace, String server, String world) {
			super(IssueType.CRASH, server, world);
			this.stacktrace = stacktrace;
		}

		public CrashIssue(String stacktrace) {
			this(stacktrace, "", "");
		}

		public static CrashIssue createWithWorld(String stacktrace, String world) {
			return new CrashIssue(stacktrace, "", world);
		}
		public static CrashIssue createWithServer(String stacktrace, String server) {
			return new CrashIssue(stacktrace, server, "");
		}
		public static CrashIssue create(String stacktrace) {
			return new CrashIssue(stacktrace);
		}
	}

	public static class LogIssue extends Issue {
		public String name;
		public boolean regex = false;
		public String logger;
		public String message;
		public String level;

		LogIssue(String server, String world, String name, String logger, String message, String level, boolean regex) {
			super(IssueType.LOG, server, world);
			this.name = name;
			this.logger = logger;
			this.message = message;
			this.level = level;
			this.regex = regex;
		}
	}

	public static class UserIssue extends Issue {
		public String name;
		UserIssue(String name, String server, String world) {
			super(IssueType.USER, server, world);
			this.name = name;
		}
	}
}
