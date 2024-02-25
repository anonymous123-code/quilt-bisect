package io.github.anonymous123_code.quilt_bisect.plugin.gui;

import io.github.anonymous123_code.quilt_bisect.shared.AutoTest;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParseException;

public class BisectCrashPrompt extends JOptionPane {
	private final JTextField autoJoinServerName = new JTextField();
	private final JTextField autoJoinWorldName = new JTextField();
	private final JTextField autoJoinRealmName = new JTextField();
	private final JRadioButton autoJoinNone = new JRadioButton("No auto join");
	private final JRadioButton autoJoinLast = new JRadioButton("Auto join last joined");
	private final JRadioButton autoJoinWorld = new JRadioButton("Auto join world: ");
	private final JRadioButton autoJoinServer = new JRadioButton("Auto join server: ");
	private final JRadioButton autoJoinRealm = new JRadioButton("Auto join realm: ");
	private final JCheckBox disableWorldSaving = new JCheckBox("Disable world saving", false);
	private final JCheckBox autoAcceptCheckBox = new JCheckBox("Automatically mark as working after ");
	private final JTextArea commandInput = new JTextArea();
	private final JTextField autoAcceptInput;
	private final String title;

	public BisectCrashPrompt(String title, int exitCode, String crashLog) {
		super("", JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, null, null);
		this.title = title;

		NumberFormat format = NumberFormat.getInstance();
		NumberFormatter formatter = new NumberFormatter(format);
		formatter.setValueClass(Integer.class);
		formatter.setMinimum(0);
		formatter.setMaximum(Integer.MAX_VALUE);
		formatter.setAllowsInvalid(false);
		// If you want the value to be committed on each keystroke instead of focus lost
		formatter.setCommitsOnValidEdit(true);
		autoAcceptInput = new JFormattedTextField(formatter);

		this.setMessage(buildMessage(exitCode, crashLog));


		this.setComponentOrientation(getRootFrame().getComponentOrientation());
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
		textarea.setEditable(false);
		textarea.setRows(5);
		return new JScrollPane(textarea);
	}

	public @Nullable AutoTest prompt() {
		JDialog dialog = this.createDialog(title);

		dialog.setResizable(true);
		this.selectInitialValue();

		//noinspection deprecation
		dialog.show();
		dialog.dispose();

		Integer selectedValue = (Integer) this.getValue();

		AutoTest.AutoJoinType type;
		String name = "";
		if (autoJoinServer.isSelected()) {
			type = AutoTest.AutoJoinType.Server;
			name = autoJoinServerName.getText();
		} else if (autoJoinWorld.isSelected()) {
			type = AutoTest.AutoJoinType.World;
			name = autoJoinWorldName.getText();
		} else if (autoJoinRealm.isSelected()) {
			type = AutoTest.AutoJoinType.Realm;
			name = autoJoinRealmName.getText();
		} else if (autoJoinLast.isSelected()) {
			type = AutoTest.AutoJoinType.LastJoined;
		} else {
			type = AutoTest.AutoJoinType.None;
		}

		if (selectedValue == null)
			return null;
        try {
            return selectedValue == JOptionPane.OK_OPTION ? new AutoTest(type, name, commandInput.getText(), disableWorldSaving.isSelected(), NumberFormat.getInstance().parse(autoAcceptInput.getText()).intValue(), autoAcceptCheckBox.isSelected()) : null;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

	private JPanel buildMessage(int exitCode, String crashLog) {
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

		BisectUtils.Result result = BisectUtils.getAutoJoinData();

		dialogInfo.add(this.createAutoJoinPanel(new AutoTest(
			result.autoJoinMode(),
			result.autoJoinName(), "", false, 20 * 5, false)), BorderLayout.SOUTH);
		return dialogInfo;
	}

	private Component createAutoJoinPanel(AutoTest defaultSettings) {

		var buttonGroup = new ButtonGroup();
		buttonGroup.add(autoJoinNone);
		autoJoinNone.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonGroup.add(autoJoinWorld);
		autoJoinWorld.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonGroup.add(autoJoinServer);
		autoJoinServer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonGroup.add(autoJoinRealm);
		autoJoinRealm.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonGroup.add(autoJoinLast);
		autoJoinLast.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		if (defaultSettings.autoJoinType() == AutoTest.AutoJoinType.Server) {
			autoJoinServer.setSelected(true);
			autoJoinServerName.setText(defaultSettings.autoJoinName());
		} else if (defaultSettings.autoJoinType() == AutoTest.AutoJoinType.World) {
			autoJoinWorld.setSelected(true);
			autoJoinWorldName.setText(defaultSettings.autoJoinName());
		} else if (defaultSettings.autoJoinType() == AutoTest.AutoJoinType.Realm) {
			autoJoinRealm.setSelected(true);
			autoJoinRealmName.setText(defaultSettings.autoJoinName());
		} else if (defaultSettings.autoJoinType() == AutoTest.AutoJoinType.LastJoined) {
			autoJoinLast.setSelected(true);
		} else {
			autoJoinNone.setSelected(true);
		}

		updateTextFields(null);
		autoJoinServer.addActionListener(this::updateTextFields);
		autoJoinNone.addActionListener(this::updateTextFields);
		autoJoinWorld.addActionListener(this::updateTextFields);
		autoJoinRealm.addActionListener(this::updateTextFields);
		autoJoinLast.addActionListener(this::updateTextFields);

		var panel = new JPanel(new GridBagLayout());

		var lineConstraints = new GridBagConstraints();
		lineConstraints.gridx = 0;
		lineConstraints.gridwidth = 2;
		lineConstraints.fill = GridBagConstraints.HORIZONTAL;


		var firstColumnConstraints = new GridBagConstraints();
		firstColumnConstraints.gridx = 0;
		firstColumnConstraints.weightx = 0;
		firstColumnConstraints.anchor = GridBagConstraints.LINE_START;


		var secondColumnConstraints = new GridBagConstraints();
		secondColumnConstraints.gridx = 1;
		secondColumnConstraints.weightx = 1;
		secondColumnConstraints.anchor = GridBagConstraints.LINE_START;
		secondColumnConstraints.fill = GridBagConstraints.HORIZONTAL;

		panel.add(new JSeparator(), lineConstraints);

		panel.add(autoJoinServer, firstColumnConstraints);
		panel.add(autoJoinServerName, secondColumnConstraints);

		panel.add(autoJoinWorld, firstColumnConstraints);
		panel.add(autoJoinWorldName, secondColumnConstraints);

		panel.add(autoJoinRealm, firstColumnConstraints);
		panel.add(autoJoinRealmName, secondColumnConstraints);

		panel.add(autoJoinLast, firstColumnConstraints);
		panel.add(new JPanel(), secondColumnConstraints);

		// This must be last or the constraints must be improved
		panel.add(autoJoinNone, firstColumnConstraints);
		panel.add(new JPanel(), secondColumnConstraints);

		JLabel label = new JLabel("Chat messages/commands to execute after join: ");
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(label, lineConstraints);

		commandInput.setRows(5);
		commandInput.setText(defaultSettings.autoJoinCommands());
		commandInput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, commandInput.getFont().getSize()));
		commandInput.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(commandInput, lineConstraints);

		panel.add(new JSeparator(), lineConstraints);

		autoAcceptCheckBox.setSelected(defaultSettings.autoAccept());
		autoAcceptCheckBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(autoAcceptCheckBox, firstColumnConstraints);

		var autoAcceptPanel = new Panel(new FlowLayout(FlowLayout.LEFT));
		autoAcceptInput.setText(String.valueOf(defaultSettings.autoAcceptTime()));
		autoAcceptInput.setMinimumSize(new Dimension(autoAcceptInput.getFont().getSize() * 20, 0)); // TODO:rough scaling -> is there a better version?
		autoAcceptPanel.add(autoAcceptInput, BorderLayout.CENTER);
		autoAcceptPanel.add(new JLabel("(1/20 seconds minimum wait, if singleplayer also minimum ticks count)"), BorderLayout.LINE_END);
		panel.add(autoAcceptPanel, secondColumnConstraints);


		disableWorldSaving.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		disableWorldSaving.setSelected(defaultSettings.disableWorldSaving());
		panel.add(disableWorldSaving, firstColumnConstraints);
		panel.add(new JPanel(), secondColumnConstraints);

		return panel;
	}

	private void updateTextFields(ActionEvent ignored) {
		autoJoinServerName.setEnabled(autoJoinServer.isSelected());
		autoJoinWorldName.setEnabled(autoJoinWorld.isSelected());
		autoJoinRealmName.setEnabled(autoJoinRealm.isSelected());
		commandInput.setEnabled(!autoJoinNone.isSelected());
	}
}
