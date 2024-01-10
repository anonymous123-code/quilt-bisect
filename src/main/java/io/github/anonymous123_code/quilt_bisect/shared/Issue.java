package io.github.anonymous123_code.quilt_bisect.shared;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public abstract class Issue {
	public String world;
	public String server;
	public Fix fix = new Fix();
	public Type type;

	private Issue(Type type, String server, String world) {
		if (!(server.isEmpty() || world.isEmpty())) throw new IllegalArgumentException("Server or World must be empty");
		this.type = type;
		this.server = server;
		this.world = world;
	}

	public enum Type {
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
			super(Type.CRASH, server, world);
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
			super(Type.LOG, server, world);
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
			super(Type.USER, server, world);
			this.name = name;
		}
	}

	public static class Fix {
		public final ArrayList<ArrayList<String>> reproductions = new ArrayList<>();
	}
}