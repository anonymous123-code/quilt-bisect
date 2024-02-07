package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.shared.AutoTest;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class BisectPluginUi {
	static {
		// Set MacOS specific system props
		System.setProperty("apple.awt.application.appearance", "system");
		System.setProperty("apple.awt.application.name", "Quilt Loader");
	}

	static void init() throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (GraphicsEnvironment.isHeadless()) {
			throw new HeadlessException();
		}
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	}

	public static @Nullable AutoTest openDialog(int exitCode, String crashLog) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		init();
        return new BisectCrashPrompt("Minecraft crashed. Start bisect?", exitCode, crashLog).prompt();
	}
}
