package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
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

public class CreateIssueScreen extends FixedSizeGridScreen {
	private final Screen parent;
	protected SpruceTextFieldWidget issueNameWidget;

	public CreateIssueScreen(Text title, int rowCount, int columnCount, int rowHeight, int columnWidth, @Nullable Screen parent) {
		super(title, rowCount, columnCount, rowHeight, columnWidth, 10, 5);
		this.parent = parent;
	}

	public CreateIssueScreen(@Nullable Screen parent) {
		this(Text.translatable("gui.bisect.new_issue.screen"), 1, 2, 30, 150, parent);
	}

	@Override
	protected void init() {
		super.init();
		setupWidgets();
	}

	protected void setupWidgets() {
		addIssueNameInput(0);
		addCancelContinueButtons(Text.translatable("gui.bisect.continue"));
	}

	protected void addIssueNameInput(int row) {
		// Issue Name
		this.addDrawableChild(new SpruceLabelWidget(centerVertically(row, 0, 10),
			Text.translatable("gui.bisect.issue_name"),
			COLUMN_WIDTH,
			false
		));
		issueNameWidget = new SpruceTextFieldWidget(centerVertically(row, 1, 20),
			COLUMN_WIDTH,
			20,
			Text.translatable("gui.bisect.issue_name")
		);
		this.addDrawableChild(issueNameWidget);
	}

	protected void addCancelContinueButtons(Text continueText) {
		this.addDrawableChild(new SpruceButtonWidget(Position.of(this.width / 2 + 5, this.height - 38),
			ButtonWidget.DEFAULT_WIDTH,
			20,
			continueText,
			button -> this.onDone()
		));
		this.addDrawableChild(new SpruceButtonWidget(Position.of(this.width / 2 - ButtonWidget.DEFAULT_WIDTH - 5,
			this.height - 38
		),
			ButtonWidget.DEFAULT_WIDTH,
			20,
			CommonTexts.CANCEL,
			button -> this.onCancel()
		));
	}

	protected void onCancel() {
		closeScreen();
	}

	protected void onDone() {
		ActiveBisectConfig.update();
		ActiveBisectConfig activeBisectConfig = ActiveBisectConfig.getInstance();
		activeBisectConfig.issues.add(new Issue.UserIssue(issueNameWidget.getText()));
		saveAndQuit();
	}

	protected static void saveAndQuit() {
		ActiveBisectConfig activeBisectConfig = ActiveBisectConfig.getInstance();
		try {
			activeBisectConfig.safe(false);
			Files.writeString(ActiveBisectConfig.configDirectory.resolve("issue.txt"),
				Integer.toString(activeBisectConfig.issues.size() - 1)
			);
			GracefulTerminator.gracefullyTerminate(56);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void closeScreen() {
		this.client.setScreen(parent);
	}
}
