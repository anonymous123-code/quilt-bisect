package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceNamedTextFieldWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import io.github.anonymous123_code.quilt_bisect.GracefulTerminator;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.Issue;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.CommonTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;

public class CreateIssueScreen extends SpruceScreen {
	private static final int MIDDLE_HALF_PADDING = 10 / 2;
	private static final int COLUMN_WIDTH = 150;
	private static final int LINE_COUNT = 2;
	private static final int LINE_HEIGHT = 30;
	private final Screen parent;
	private final boolean start;
	private AutoJoinType autoJoinMode = AutoJoinType.None;
	private SpruceTextFieldWidget issueNameWidget;
	private SpruceNamedTextFieldWidget autoJoinNameWidget;

	public CreateIssueScreen(@Nullable Screen parent, boolean start) {
		super(Text.translatable(start ? "gui.bisect.start.screen" : "gui.bisect.new_issue.screen"));
		this.parent = parent;
		this.start = start;
	}

	@Override
	protected void init() {
		super.init();
		addOptions();
		addCancelContinueButtons();
	}

	private void addOptions() {
		// Issue Name
		this.addDrawableChild(new SpruceLabelWidget(getPosition(0, 10, true), Text.translatable("gui.bisect.issue_name"), COLUMN_WIDTH, false));
		issueNameWidget = new SpruceTextFieldWidget(getPosition(0, 20, false), COLUMN_WIDTH, 20, Text.translatable("gui.bisect.issue_name"));
		this.addDrawableChild(issueNameWidget);

		// Auto Join
		this.addDrawableChild(new SpruceButtonWidget(getPosition(1, 20, true), COLUMN_WIDTH, 20, autoJoinMode.text, button -> {
			autoJoinMode = autoJoinMode.nextCycle();
			button.setMessage(autoJoinMode.text);
		}));
		autoJoinNameWidget = new SpruceNamedTextFieldWidget(new SpruceTextFieldWidget(getPosition(1, 20, false), COLUMN_WIDTH, 20, Text.translatable("gui.bisect.new_issue.auto_join.name")));
		this.addDrawableChild(autoJoinNameWidget);
	}

	private void addCancelContinueButtons() {
		this.addDrawableChild(
			new SpruceButtonWidget(Position.of(this.width / 2 + 5, this.height - 38), ButtonWidget.DEFAULT_WIDTH, 20, Text.translatable(this.start ? "gui.bisect.start" : "gui.bisect.continue"), button -> this.onDone())
		);
		this.addDrawableChild(
			new SpruceButtonWidget(Position.of(this.width / 2 - ButtonWidget.DEFAULT_WIDTH - 5, this.height - 38), ButtonWidget.DEFAULT_WIDTH, 20, CommonTexts.CANCEL, button -> this.onCancel())
		);
	}

	private Position getPosition(int line, int height, boolean left) {
		return Position.of(left ? this.width / 2 - COLUMN_WIDTH - MIDDLE_HALF_PADDING : this.width / 2 + MIDDLE_HALF_PADDING, this.height / 2 - (LINE_HEIGHT * LINE_COUNT / 2) + (LINE_HEIGHT * line) + (LINE_HEIGHT - height) / 2);
	}

	private void onCancel() {
		closeScreen();
	}

	private void onDone() {
		ActiveBisectConfig.update();
		ActiveBisectConfig activeBisectConfig = ActiveBisectConfig.getInstance();
		String server = autoJoinMode == AutoJoinType.Server ? autoJoinNameWidget.getText() : "";
		String world = autoJoinMode == AutoJoinType.World ? autoJoinNameWidget.getText() : "";
		activeBisectConfig.issues.add(new Issue.UserIssue(issueNameWidget.getText(), server, world));
		activeBisectConfig.bisectActive = true;
		try {
			activeBisectConfig.safe(false);
			Files.writeString(ActiveBisectConfig.configDirectory.resolve("issue.txt"), Integer.toString(activeBisectConfig.issues.size() - 1));
			GracefulTerminator.gracefullyTerminate(56);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void closeScreen() {
		this.client.setScreen(parent);
	}

	public enum AutoJoinType {
		None(Text.translatable("gui.bisect.new_issue.auto_join.none")),
		World(Text.translatable("gui.bisect.new_issue.auto_join.world")),
		Server(Text.translatable("gui.bisect.new_issue.auto_join.server"));
		public final Text text;

        AutoJoinType(Text text) {
            this.text = text;
        }

		AutoJoinType nextCycle() {
			return switch (this) {
				case None -> World;
				case World -> Server;
				case Server -> None;
			};
		}
    }
}
