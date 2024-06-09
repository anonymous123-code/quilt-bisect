package io.github.anonymous123_code.quilt_bisect.gui;

import io.github.anonymous123_code.quilt_bisect.GracefulTerminator;
import io.github.anonymous123_code.quilt_bisect.shared.AbstractBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.Issue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.ButtonWidget;
import net.minecraft.client.gui.widget.text.TextWidget;
import net.minecraft.text.CommonTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;

public class CreateIssueScreen extends FixedSizeGridScreen {
	private final Screen parent;
	protected TextFieldWidget issueNameWidget;

	public CreateIssueScreen(Text title, int rowCount, int columnCount, int cellHeight, int cellWidth, int horizontalPadding, int verticalPadding, @Nullable Screen parent) {
		super(title, rowCount, columnCount, cellHeight, cellWidth, horizontalPadding, verticalPadding);
		this.parent = parent;
	}

	public CreateIssueScreen(@Nullable Screen parent) {
		this(Text.translatable("gui.bisect.new_issue.screen"), 1, 2, 20, 150, 10, 10, parent);
	}

	protected static void saveAndQuit() {
		ActiveBisectConfig activeBisectConfig = ActiveBisectConfig.INSTANCE;
		try {
			activeBisectConfig.safe();
			Files.writeString(
				AbstractBisectConfig.configDirectory.resolve("issue.txt"),
				Integer.toString(activeBisectConfig.issues.size() - 1)
			);
			GracefulTerminator.gracefullyTerminate(56);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
		addDrawable(new TextWidget(
			cornerX(0),
			centerY(row, 10),
			CELL_WIDTH,
			10,
			Text.translatable("gui.bisect.issue_name"),
			textRenderer
		));
		issueNameWidget = new TextFieldWidget(
			textRenderer,
			cornerX(1),
			centerY(row, 20),
			CELL_WIDTH,
			20,
			Text.translatable("gui.bisect.issue_name")
		);
		addDrawableSelectableElement(issueNameWidget);
	}

	protected void addCancelContinueButtons(Text continueText) {
		addDrawableSelectableElement(ButtonWidget.builder(continueText, button -> onDone())
								.positionAndSize(width / 2 + 5, height - 28, ButtonWidget.DEFAULT_WIDTH, 20)
								.build());
		addDrawableSelectableElement(ButtonWidget.builder(CommonTexts.CANCEL, button -> onCancel())
								.positionAndSize(
									width / 2 - ButtonWidget.DEFAULT_WIDTH - 5,
									height - 28,
									ButtonWidget.DEFAULT_WIDTH,
									20
								)
								.build());
	}

	protected void onCancel() {
		closeScreen();
	}

	protected void onDone() {
		ActiveBisectConfig activeBisectConfig = ActiveBisectConfig.INSTANCE;
		activeBisectConfig.update();
		activeBisectConfig.issues.add(new Issue.UserIssue(issueNameWidget.getText()));
		saveAndQuit();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredShadowedText(this.textRenderer, this.title, this.width / 2, 15, 0xffffff);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.renderBackgroundTexture(graphics);
	}

	@Override
	public void closeScreen() {
		client.setScreen(parent);
	}
}
