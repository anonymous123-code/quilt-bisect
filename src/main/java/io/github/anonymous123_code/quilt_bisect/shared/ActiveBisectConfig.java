package io.github.anonymous123_code.quilt_bisect.shared;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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

	public void safe(boolean force) throws IOException {
		if (!force && !isUpToDate()) return;
		var config_dir = QuiltLoader.getConfigDir().resolve("bisect");
		var config_file = config_dir.resolve("active_bisect.json");
		var gson = new GsonBuilder().setPrettyPrinting().create();
		Files.writeString(config_file, gson.toJson(this));
	}

	private static ActiveBisectConfig serializeFromFile(Path configFile) {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ModSet.class, (JsonDeserializer<ModSet>) (jsonElement, type, jsonDeserializationContext) -> {
				if (jsonElement.getAsJsonObject().get("working").getAsBoolean()) {
					return new Gson().fromJson(jsonElement, ModSet.WorkingModSet.class);
				} else {
					return new Gson().fromJson(jsonElement, ModSet.ErroringModSet.class);
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
					default -> throw new JsonParseException("Invalid Type");
				}
			})
			.create();
		try {
			return gson.fromJson(Files.readString(configFile), ActiveBisectConfig.class);
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


	public boolean bisectActive = false;
	public HashMap<String, ModSet> modSets = new HashMap<>();
	public ArrayList<Issue> issues = new ArrayList<>();
	public HashMap<String, String> modIdToFile = new HashMap<>();


	public void processRun(List<String> modIdSet, String modSetHash, Optional<String> crashLog, Optional<String> crashLogPath) {
		Optional<Integer> issue = getOrAddIssue(crashLog);
		ModSet modSet = issue.isPresent() ? new ModSet.ErroringModSet(new ArrayList<>(modIdSet), issue.get(), crashLogPath.orElse("")) : new ModSet.WorkingModSet(new ArrayList<>(modIdSet));
		modSets.put(modSetHash, modSet);
	}

	public void updateFiles(HashMap<String, Path> modSetToPath) {
		for (var entry : modSetToPath.entrySet()) {
			var fileName = entry.getValue().getFileName().toString();
			if (modIdToFile.containsKey(entry.getKey())) {
				if (!modIdToFile.get(entry.getKey()).equals(fileName)) {
					for (var modSet: modSets.values()) {
						if (modSet.modSet.contains(entry.getKey())) modSet.invalidated = true;
					}
					modIdToFile.put(entry.getKey(), fileName);
				}
			} else {
				modIdToFile.put(entry.getKey(), fileName);
			}
		}
	}

	public Optional<Integer> getOrAddIssue(Optional<String> crashLog) {
		var issuePath = configDirectory.resolve("issue.txt");
		if (Files.exists(issuePath)) {
			String issueData;
			try {
				issueData = Files.readString(issuePath);
				Files.delete(issuePath);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return Optional.of(Integer.parseInt(issueData, 10));
		} else if (crashLog.isPresent()){
			var stacktrace = removeLikelyMixinPoison(BisectUtils.extractStackTrace(crashLog.get()));
			for (int issueIndex = 0; issueIndex < issues.size(); issueIndex++) {
				var issue = issues.get(issueIndex);
				if (issue instanceof CrashIssue && removeLikelyMixinPoison(((CrashIssue) issue).stacktrace).equals(stacktrace)) {
					return Optional.of(issueIndex);
				}
			}
			issues.add(new CrashIssue(stacktrace));
			return Optional.of(issues.size() - 1);
		} else return Optional.empty();
	}

	public String removeLikelyMixinPoison(String oldStacktrace) {
		return oldStacktrace.replaceAll("\\.handler\\$[0-9a-z]{6}\\$", ".fuzzyMixinHandler$");
	}

	public Optional<ModSet> getFirstInvalidatedModSet() {
		for (ModSet modSet : modSets.values()) {
			if (modSet.invalidated) return Optional.of(modSet);
		}
		return Optional.empty();
	}

	public Optional<ModSet> getModSet(ModSet.ModSetSection first, ModSet.ModSetSection... modSetSections) {
        ArrayList<String> mods = new ArrayList<>(first.getArrayListCopy());
		for (ModSet.ModSetSection modSetSection : modSetSections) {
			mods.addAll(modSetSection.getArrayListCopy());
		}
		Collections.sort(mods);
		return modSets.values().stream().filter(it -> mods.equals(it.modSet)).findAny();
	}

	public ModSet findSmallestUnfixedModSet() {
		ModSet smallest = null;
		int smallestSize = Integer.MAX_VALUE;
		for (var set : modSets.values()) {
			if (set.working) continue;
			if (set.modSet.size() < smallestSize) {
				smallest = set;
				smallestSize = set.modSet.size();
			}
		}
		return smallest;
	}

	public static abstract class Issue {
		public String world;
		public String server;
		public boolean fixed;
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
		USER()
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
		public boolean regex;
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
