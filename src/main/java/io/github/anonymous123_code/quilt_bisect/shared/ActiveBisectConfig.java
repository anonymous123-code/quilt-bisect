package io.github.anonymous123_code.quilt_bisect.shared;

import com.google.gson.GsonBuilder;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public class ActiveBisectConfig {
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
		var config_dir = QuiltLoader.getConfigDir().resolve("bisect");
		if (!Files.exists(config_dir)) {
			try {
				Files.createDirectories(config_dir);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to create directory '%s'", config_dir), e);
			}
		}
		var config_file = config_dir.resolve("active_bisect.json");
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
		var gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			return gson.fromJson(new FileReader(configFile.toFile()), ActiveBisectConfig.class);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}



	public boolean bisectActive;
	public HashMap<String, ModSet> modSets;
	public ArrayList<Issue> issues;
	public HashMap<String, String> modIdToFile;
	public ActiveModset activeModset;

	public static abstract class ModSet {
		protected ModSet(boolean working, ArrayList<String> modSet) {
			this.working = working;
			this.invalidated = false;
			this.modSet = modSet;
		}

		public boolean isWorking() {
			return working;
		}

		public final boolean working;
		public boolean invalidated;
		public final ArrayList<String> modSet;
	}

	public static class WorkingModSet extends ModSet {
		public WorkingModSet(ArrayList<String> modSet) {
			super(true, modSet);
		}
	}

	public static class ErroringModSet extends ModSet {
		public final int issueId;
		public final boolean hasCrashLog;

		public ErroringModSet(ArrayList<String> modSet, int issueId, boolean hasCrashLog) {
			super(false, modSet);
			this.issueId = issueId;
			this.hasCrashLog = hasCrashLog;
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
		CRASH("crash"),
		LOG("log"),
		USER("user");

		public final String serializedName;

		IssueType(String serializedName) {
			this.serializedName = serializedName;
		}
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

	public static class ActiveModset {
		public String id;
		public ArrayList<String> activeMods;

		public ActiveModset(String id, ArrayList<String> activeMods) {
			this.id = id;
			this.activeMods = activeMods;
		}
	}
}
