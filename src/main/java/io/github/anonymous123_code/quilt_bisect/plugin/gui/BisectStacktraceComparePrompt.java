package io.github.anonymous123_code.quilt_bisect.plugin.gui;

import io.github.anonymous123_code.quilt_bisect.plugin.Diff;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class BisectStacktraceComparePrompt extends JOptionPane {
	private final String title;

	public BisectStacktraceComparePrompt(String title, String baseContent, String newContent) {
		super("", JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION, null, null, null);
		this.title = title;

		this.setMessage(buildMessage(baseContent, newContent));


		this.setComponentOrientation(getRootFrame().getComponentOrientation());
	}

	private JPanel buildMessage(String baseContent, String newContent) {
		var dialogInfo = new JPanel();
		dialogInfo.setMinimumSize(new Dimension(600, 400));
		dialogInfo.setPreferredSize(new Dimension(800, 600));
		dialogInfo.setLayout(new BorderLayout());
		var label = new JLabel(
			"<html><p>A stacktrace similar to a known issue appeared. Treat the stacktrace as the same issue?</p></html>");
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		dialogInfo.add(label, BorderLayout.NORTH);
		DefaultStyledDocument baseDoc = new DefaultStyledDocument();
		DefaultStyledDocument newDoc = new DefaultStyledDocument();
		StyleContext styleContext = new StyleContext();

		Style addition = styleContext.addStyle("Addition", null);
		StyleConstants.setBackground(addition, new Color(0, 255, 0, 64));

		Style deletion = styleContext.addStyle("Deletion", null);
		StyleConstants.setBackground(deletion, new Color(255, 0, 0, 64));

		Style edit = styleContext.addStyle("Change", null);
		StyleConstants.setBackground(edit, new Color(0, 128, 255, 64));

		String commonString = Diff.calculateLongestCommonSequence(baseContent, newContent);
		StringBuilder baseRemoval = new StringBuilder();
		StringBuilder newAddition = new StringBuilder();
		int baseLength = baseContent.codePointCount(0, baseContent.length());
		int newLength = newContent.codePointCount(0, newContent.length());
		int commonLength = commonString.codePointCount(0, commonString.length());
		int baseIndex = 0;
		int newIndex = 0;
		for (int commonIndex = 0; commonIndex < commonLength; commonIndex++) {
			int commonPoint = commonString.codePointAt(commonIndex);
			while (baseIndex < baseLength && baseContent.codePointAt(baseIndex) != commonPoint) {
				baseRemoval.appendCodePoint(baseContent.codePointAt(baseIndex));
				baseIndex++;
			}
			while (newIndex < newLength && newContent.codePointAt(newIndex) != commonPoint) {
				newAddition.appendCodePoint(newContent.codePointAt(newIndex));
				newIndex++;
			}
			try {
				if (!baseRemoval.isEmpty() && !newAddition.isEmpty()) {
					baseDoc.insertString(baseDoc.getLength(), baseRemoval.toString(), edit);
					newDoc.insertString(newDoc.getLength(), newAddition.toString(), edit);
				} else {
					baseDoc.insertString(baseDoc.getLength(), baseRemoval.toString(), deletion);
					newDoc.insertString(newDoc.getLength(), newAddition.toString(), addition);
				}
				baseRemoval = new StringBuilder();
				newAddition = new StringBuilder();
				baseIndex++;
				newIndex++;
				baseDoc.insertString(baseDoc.getLength(), Character.toString(commonPoint), null);
				newDoc.insertString(newDoc.getLength(), Character.toString(commonPoint), null);
			} catch (BadLocationException ignored) {
			} // Should never happen
		}
		JTextPane baseTextPane = new JTextPane();
		baseTextPane.setEditable(false);
		baseTextPane.setDocument(baseDoc);
		JTextPane newTextPane = new JTextPane();
		newTextPane.setEditable(false);
		newTextPane.setDocument(newDoc);
		dialogInfo.add(BorderLayout.WEST, baseTextPane);
		dialogInfo.add(BorderLayout.EAST, newTextPane);
		return dialogInfo;
	}

	public boolean prompt() {
		JDialog dialog = this.createDialog(title);

		dialog.setResizable(true);
		this.selectInitialValue();

		//noinspection deprecation
		dialog.show();
		dialog.dispose();

		Integer selectedValue = (Integer) this.getValue();


		if (selectedValue == null) {
			return false;
		}
		return selectedValue == JOptionPane.YES_OPTION;
	}

}
