package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextAreaWidget;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.Issue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.text.CommonTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;

import static java.lang.String.join;

public class BisectSummaryScreen extends SpruceScreen {
	public BisectSummaryScreen() {
		super(Text.of("Bisect summary"));
	}


	@Override
	protected void init() {
		super.init();
		var oldBisect = ActiveBisectConfig.getInstance();
		StringBuilder sb = new StringBuilder();
		sb.append("List of Reproduction cases for Issues:\n");
		int crashIndex = 1;
		for (Issue issue : oldBisect.issues) {
			if (issue instanceof Issue.CrashIssue) {
				sb.append("  - Crash ").append(crashIndex).append(":").append("\n");
			} else if (issue instanceof Issue.LogIssue logIssue) {
				sb.append("  - ").append(logIssue.name).append("\n");
			} else if (issue instanceof Issue.UserIssue userIssue) {
				sb.append("  - ").append(userIssue.name).append("\n");
			}
			for (ArrayList<String> reproduction : issue.fix.reproductions) {
				sb.append("    - ").append(join(", ", reproduction)).append("\n");
			}
		}

		var widget = new SpruceTextAreaWidget(Position.of(this.width/2-155, 40), 310, this.height - 96, this.title);
		widget.setText(sb.toString());
		widget.setEditable(false);
		addDrawableChild(widget);
		this.addDrawableChild(
			new SpruceButtonWidget(Position.of(this.width / 2 - 100, this.height - 38), 200, 20, CommonTexts.DONE, button -> this.closeScreen())
		);
	}

	@Override
	public void renderTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		guiGraphics.drawCenteredShadowedText(this.textRenderer, this.title, this.width / 2, 15, 0xffffff);
	}
}
