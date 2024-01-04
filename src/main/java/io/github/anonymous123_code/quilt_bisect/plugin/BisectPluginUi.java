package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;

import javax.swing.*;
import java.awt.*;

import static javax.swing.JOptionPane.CLOSED_OPTION;
import static javax.swing.JOptionPane.getRootFrame;

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

	public static boolean openDialog(int exitCode, String crashLog) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		init();

		var dialogInfo = new JPanel();
		dialogInfo.setMinimumSize(new Dimension(600, 400));
		dialogInfo.setPreferredSize(new Dimension(800, 600));
		dialogInfo.setLayout(new BorderLayout());
		var label = new JLabel("<html><p>Minecraft crashed with exit code " + exitCode + ". If you start" +
			" a bisect, Quilt Bisect will automatically restart Minecraft in order to find out which mod is at fault.</p></html>");
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		dialogInfo.add(label, BorderLayout.NORTH);

		var tabs = new JTabbedPane();
		tabs.add("Stacktrace", createStacktracePanel(crashLog));
		tabs.add("Full crash log", createTextArea(crashLog));
		dialogInfo.add(tabs, BorderLayout.CENTER);

		int result = showOptionDialog(dialogInfo, "Minecraft crashed. Start Bisect?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		return result == JOptionPane.OK_OPTION;
	}

	private static Component createStacktracePanel(String crashlog) {
		var panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JLabel label = new JLabel("The stacktrace will be used to discern different errors");
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(label, BorderLayout.NORTH);
		panel.add(createTextArea(BisectUtils.extractStackTrace(crashlog)), BorderLayout.CENTER);
		return panel;
	}

	private static Component createTextArea(String content) {
		var textarea = new JTextArea(content);
		textarea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, textarea.getFont().getSize()));
		textarea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		return new JScrollPane(textarea);
	}

	public static int showOptionDialog(Object message, String title, int optionType, int messageType)
		throws HeadlessException {
		JOptionPane pane = new JOptionPane(message, messageType,
			optionType, null,
			null, null);

		pane.setComponentOrientation(getRootFrame().getComponentOrientation());

		JDialog dialog = pane.createDialog(title);

		dialog.setResizable(true);
		pane.selectInitialValue();
		//noinspection deprecation
		dialog.show();
		dialog.dispose();

		Integer selectedValue = (Integer) pane.getValue();

		if (selectedValue == null)
			return CLOSED_OPTION;
		return selectedValue;
	}
}
