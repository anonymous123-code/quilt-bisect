package io.github.anonymous123_code.quilt_bisect.shared;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public abstract class Issue {
	public Fix fix = new Fix();
	public Type type;

	private Issue(Type type) {
		this.type = type;
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
		public final ArrayList<String> stacktraces;

		public CrashIssue(String stacktrace) {
			super(Type.CRASH);
			this.stacktraces = new ArrayList<>();
			this.stacktraces.add(stacktrace);
		}
	}

	public static class LogIssue extends Issue implements NamedIssue {
		public String name;
		public boolean regex;
		public String logger;
		public String message;
		public String level;

		LogIssue(String name, String logger, String message, String level, boolean regex) {
			super(Type.LOG);
			this.name = name;
			this.logger = logger;
			this.message = message;
			this.level = level;
			this.regex = regex;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public static class UserIssue extends Issue implements NamedIssue {
		public String name;
		public UserIssue(String name) {
			super(Type.USER);
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public interface NamedIssue {
		String getName();
	}

	public static class Fix {
		public final ArrayList<ArrayList<String>> reproductions = new ArrayList<>();
	}
}
