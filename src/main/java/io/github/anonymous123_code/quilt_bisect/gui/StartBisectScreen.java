package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.util.SpruceUtil;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceCheckboxWidget;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextAreaWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.AutoTest;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import io.github.anonymous123_code.quilt_bisect.shared.Issue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public class StartBisectScreen extends CreateIssueScreen {
	private AutoJoinType autoJoinMode = AutoJoinType.None;
	private SpruceTextFieldWidget autoJoinNameWidget;
	private SpruceCheckboxWidget autoJoinDisableWorldSavingWidget;
	private SpruceCheckboxWidget autoJoinEnableAutoAcceptWidget;
	private SpruceTextAreaWidget autoJoinCommandInput;
	private int autoJoinAutoAcceptTime;

	protected StartBisectScreen(Text title, int rowCount, int columnCount, int cellHeight, int cellWidth, @Nullable Screen parent) {
		super(title, rowCount, columnCount, cellHeight, cellWidth, 10, 5, parent);
	}

	public StartBisectScreen(@Nullable Screen parent) {
		this(Text.translatable("gui.bisect.start.screen"), 7, 2, 20, 150, parent);
	}

	@Override
	protected void setupWidgets() {
		addIssueNameInput(0);
		addAutoJoinOption(1);
		addCommandInput(2, 3);
		addDisableWorldSavingOption(5);
		addAutoAcceptOption(6);
		addCancelContinueButtons(Text.translatable("gui.bisect.start"));
	}

	@Override
	protected void onDone() {
		ActiveBisectConfig.update();
		ActiveBisectConfig activeBisectConfig = ActiveBisectConfig.getInstance();
		activeBisectConfig.issues.add(new Issue.UserIssue(issueNameWidget.getText()));
		activeBisectConfig.bisectSettings = new AutoTest(autoJoinMode.convertToAutoTest(),
			autoJoinNameWidget.getText(),
			autoJoinCommandInput.getText(),
			autoJoinDisableWorldSavingWidget.getValue(),
			autoJoinAutoAcceptTime,
			autoJoinEnableAutoAcceptWidget.getValue()
		);
		saveAndQuit();
	}

	private void addAutoJoinOption(int row) {
		BisectUtils.Result result = BisectUtils.getAutoJoinData();
		String autoJoinName = result.autoJoinName();
		autoJoinMode = AutoJoinType.from(result.autoJoinMode());
		// Auto Join
		addDrawableChild(new SpruceButtonWidget(centerVertically(row, 0, 20),
			CELL_WIDTH,
			20,
			autoJoinMode.text,
			button -> {
				autoJoinMode = autoJoinMode.nextCycle(Screen.hasAltDown());
				button.setMessage(autoJoinMode.text);
			}
		));
		autoJoinNameWidget = new SpruceTextFieldWidget(centerVertically(row, 1, 20),
			CELL_WIDTH,
			20,
			Text.translatable("gui.bisect.start.auto_join.name")
		);
		autoJoinNameWidget.setText(autoJoinName);
		addDrawableChild(autoJoinNameWidget);
	}

	private void addDisableWorldSavingOption(int row) {
		autoJoinDisableWorldSavingWidget = new SpruceCheckboxWidget(centerVertically(row, 0, 20),
			CELL_WIDTH * 2 + HORIZONTAL_PADDING,
			20,
			Text.translatable("gui.bisect.start.disable_world_saving"),
			false
		);
		addDrawableChild(autoJoinDisableWorldSavingWidget);
	}

	private void addAutoAcceptOption(int row) {
		autoJoinEnableAutoAcceptWidget = new SpruceCheckboxWidget(centerVertically(row, 0, 20),
			CELL_WIDTH,
			20,
			Text.translatable("gui.bisect.start.auto_accept.enable"),
			false
		);
		addDrawableChild(autoJoinEnableAutoAcceptWidget);
		SpruceTextFieldWidget autoJoinAutoAcceptTimeWidget = new SpruceTextFieldWidget(centerVertically(row, 1, 20),
			CELL_WIDTH,
			20,
			Text.translatable("gui.bisect.start.auto_accept.time")
		);
		autoJoinAutoAcceptTime = 20 * 5;
		autoJoinAutoAcceptTimeWidget.setText(String.valueOf(autoJoinAutoAcceptTime));
		autoJoinAutoAcceptTimeWidget.setTextPredicate(SpruceTextFieldWidget.INTEGER_INPUT_PREDICATE);
		autoJoinAutoAcceptTimeWidget.setRenderTextProvider((displayedText, offset) -> {
			try {
				Integer.parseInt(autoJoinAutoAcceptTimeWidget.getText());
				return OrderedText.forward(displayedText, Style.EMPTY);
			} catch (NumberFormatException e) {
				return OrderedText.forward(displayedText, Style.EMPTY.withColor(Formatting.RED));
			}
		});
		autoJoinAutoAcceptTimeWidget.setChangedListener(input -> autoJoinAutoAcceptTime = SpruceUtil.parseIntFromString(input));
		autoJoinAutoAcceptTimeWidget.setTooltip(Text.translatable("gui.bisect.start.auto_accept.time.tooltip"));
		addDrawableChild(autoJoinAutoAcceptTimeWidget);
	}

	private void addCommandInput(int row, int rowCount) {
		addDrawableChild(new SpruceLabelWidget(centerVertically(row, 0, 10),
			Text.translatable("gui.bisect.start.command.label"),
			CELL_WIDTH * 2 + HORIZONTAL_PADDING
		));
		autoJoinCommandInput = new SpruceTextAreaWidget(Position.of(corner(row + 1, 0), 0, -VERTICAL_PADDING),
			CELL_WIDTH * 2 + HORIZONTAL_PADDING,
			(rowCount - 1) * (CELL_HEIGHT + VERTICAL_PADDING) - VERTICAL_PADDING,
			Text.translatable("gui.bisect.start.command.input")
		);
		addDrawableChild(autoJoinCommandInput);
	}

	@Override
	public void renderTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		guiGraphics.drawCenteredShadowedText(this.textRenderer, this.title, this.width / 2, 15, 0xffffff);
	}


	public enum AutoJoinType {
		None(Text.translatable("gui.bisect.start.auto_join.none")),
		World(Text.translatable("gui.bisect.start.auto_join.world")),
		Server(Text.translatable("gui.bisect.start.auto_join.server")),
		LasJoined(Text.translatable("gui.bisect.start.auto_join.last_joined")),
		Realm(Text.translatable("gui.bisect.start.auto_join.realm"));

		public final Text text;

		AutoJoinType(Text text) {
			this.text = text;
		}

		static AutoJoinType from(AutoTest.AutoJoinType other) {
			return switch (other) {
				case LastJoined -> LasJoined;
				case Realm -> Realm;
				case None -> None;
				case Server -> Server;
				case World -> World;
			};
		}

		AutoJoinType nextCycle(boolean activateHidden) {
			if (activateHidden) {
				return switch (this) {
					case None -> World;
					case World -> Server;
					case Server -> LasJoined;
					case LasJoined -> Realm;
					case Realm -> None;
				};
			} else {
				return switch (this) {
					case None -> World;
					case World -> Server;
					case Server -> LasJoined;
					case LasJoined, Realm -> None;
				};
			}
		}

		AutoTest.AutoJoinType convertToAutoTest() {
			return switch (this) {
				case World -> AutoTest.AutoJoinType.World;
				case Server -> AutoTest.AutoJoinType.Server;
				case None -> AutoTest.AutoJoinType.None;
				case Realm -> AutoTest.AutoJoinType.Realm;
				case LasJoined -> AutoTest.AutoJoinType.LastJoined;
			};
		}
	}
}
