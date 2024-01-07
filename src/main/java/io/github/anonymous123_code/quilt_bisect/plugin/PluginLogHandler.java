package io.github.anonymous123_code.quilt_bisect.plugin;
/*
 * Licensed under the MIT license by comp500 (2023). See the ModVote-License file in the repository root.
 * Modifications:
 * - Adjusted plugin name
 * - adjusted package
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract sealed class PluginLogHandler {
	public static final PluginLogHandler INSTANCE;

	public abstract void info(String msg);
	public abstract void warn(String msg);
	public abstract void warn(String msg, Throwable e);
	public abstract void error(String msg, Throwable e);
	public void shutdown() {}

	static {
		PluginLogHandler inst;
		try {
			inst = new Log4j();
		} catch (Throwable ex) {
			inst = new Basic();
		}
		INSTANCE = inst;
	}

	private static final class Log4j extends PluginLogHandler {
		private final Logger LOGGER = LogManager.getLogger("QuiltBisectPlugin");

		@Override
		public void info(String msg) {
			LOGGER.info(msg);
		}

		@Override
		public void warn(String msg) {
			LOGGER.warn(msg);
		}

		@Override
		public void warn(String msg, Throwable e) {
			LOGGER.warn(msg, e);
		}

		@Override
		public void error(String msg, Throwable e) {
			LOGGER.error(msg, e);
		}

		@Override
		public void shutdown() {
			LogManager.shutdown();
		}
	}

	private static final class Basic extends PluginLogHandler {
		@Override
		public void info(String msg) {
			System.out.println("[QuiltBisectPlugin/INFO] " + msg);
		}

		@Override
		public void warn(String msg) {
			System.out.println("[QuiltBisectPlugin/WARN] " + msg);
		}

		@Override
		public void warn(String msg, Throwable e) {
			System.out.println("[QuiltBisectPlugin/WARN] " + msg + ": " + e);
		}

		@Override
		public void error(String msg, Throwable e) {
			System.out.println("[QuiltBisectPlugin/ERROR] " + msg + ": " + e);
		}
	}
}
