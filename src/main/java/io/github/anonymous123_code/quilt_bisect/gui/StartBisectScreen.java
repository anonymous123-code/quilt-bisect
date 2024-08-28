package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.text.SpruceTextAreaWidget;
import io.github.anonymous123_code.quilt_bisect.shared_mod.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared_mod.AutoTest;
import io.github.anonymous123_code.quilt_bisect.shared_mod.BisectUtils;
import io.github.anonymous123_code.quilt_bisect.shared_mod.Issue;
import io.github.anonymous123_code.quilt_bisect.shared_mod.MainConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.ButtonWidget;
import net.minecraft.client.gui.widget.text.TextWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class StartBisectScreen extends CreateIssueScreen {
	public static final Predicate<String> INTEGER_INPUT_PREDICATE = input -> {
		if (input.isEmpty() || input.equals("-")) {
			return true;
		}
		try {
			Integer.parseInt(input);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	};
	private AutoJoinType autoJoinMode = AutoJoinType.None;
	private TextFieldWidget autoJoinNameWidget;
	private CheckboxWidget autoJoinDisableWorldSavingWidget;
	private CheckboxWidget autoJoinEnableAutoAcceptWidget;
	private SpruceTextAreaWidget autoJoinCommandInput;
	private int autoJoinAutoAcceptTime;

	protected StartBisectScreen(Text title, int rowCount, int columnCount, int cellHeight, int cellWidth, @Nullable Screen parent) {
		super(title, rowCount, columnCount, cellHeight, cellWidth, 10, 5, parent);
	}

	public StartBisectScreen(@Nullable Screen parent) {
		this(Text.translatable("gui.bisect.start.screen"), 7, 2, 20, 150, parent);
	}

	private static int parseIntFromString(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
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
		ActiveBisectConfig activeBisectConfig = ActiveBisectConfig.INSTANCE;
		activeBisectConfig.update();
		activeBisectConfig.issues.add(new Issue.UserIssue(issueNameWidget.getText()));
		activeBisectConfig.bisectSettings = new AutoTest(
			autoJoinMode.convertToAutoTest(),
			autoJoinNameWidget.getText(),
			autoJoinCommandInput.getText(),
			autoJoinDisableWorldSavingWidget.isChecked(),
			autoJoinAutoAcceptTime,
			autoJoinEnableAutoAcceptWidget.isChecked()
		);
		saveAndQuit();
	}

	private void addAutoJoinOption(int row) {
		BisectUtils.Result result = BisectUtils.getAutoJoinData();
		String autoJoinName = result.autoJoinName();
		autoJoinMode = AutoJoinType.from(result.autoJoinMode());
		// Auto Join
		addDrawableSelectableElement(ButtonWidget.builder(autoJoinMode.text, button -> {
			autoJoinMode = autoJoinMode.nextCycle(Screen.hasAltDown());
			button.setMessage(autoJoinMode.text);
		}).positionAndSize(cornerX(0), centerY(row, 20), CELL_WIDTH, 20).build());
		autoJoinNameWidget = new TextFieldWidget(
			textRenderer,
			cornerX(1),
			centerY(row, 20),
			CELL_WIDTH,
			20,
			Text.translatable("gui.bisect.start.auto_join.name")
		);
		autoJoinNameWidget.setText(autoJoinName);
		addDrawableSelectableElement(autoJoinNameWidget);
	}

	private void addDisableWorldSavingOption(int row) {
		autoJoinDisableWorldSavingWidget = CheckboxWidget.method_54787(Text.translatable(
				"gui.bisect.start.disable_world_saving"), client.textRenderer) // CheckBoxWidget.builder()
			.method_54789(cornerX(0), centerY(row, 20)) // .position()
			.method_54794(MainConfig.INSTANCE.defaultBisectSettings.disableWorldSaving()) // .selected()
			.method_54788(); //.build()
		addDrawableSelectableElement(autoJoinDisableWorldSavingWidget);
	}

	private void addAutoAcceptOption(int row) {
		autoJoinEnableAutoAcceptWidget = CheckboxWidget.method_54787(Text.translatable(
				"gui.bisect.start.auto_accept.enable"), client.textRenderer)
			.method_54789(cornerX(0), centerY(row, 20))
			.method_54794(MainConfig.INSTANCE.defaultBisectSettings.autoAccept())
			.method_54788();
		addDrawableSelectableElement(autoJoinEnableAutoAcceptWidget);
		TextFieldWidget autoJoinAutoAcceptTimeWidget = new TextFieldWidget(
			textRenderer,
			cornerX(1),
			centerY(row, 20),
			CELL_WIDTH,
			20,
			Text.translatable("gui.bisect.start.auto_accept.time")
		);
		autoJoinAutoAcceptTime = MainConfig.INSTANCE.defaultBisectSettings.autoAcceptTime();
		autoJoinAutoAcceptTimeWidget.setText(String.valueOf(autoJoinAutoAcceptTime));
		autoJoinAutoAcceptTimeWidget.setTextPredicate(INTEGER_INPUT_PREDICATE);
		autoJoinAutoAcceptTimeWidget.setRenderTextProvider((displayedText, offset) -> {
			try {
				Integer.parseInt(autoJoinAutoAcceptTimeWidget.getText());
				return OrderedText.forward(displayedText, Style.EMPTY);
			} catch (NumberFormatException e) {
				return OrderedText.forward(displayedText, Style.EMPTY.withColor(Formatting.RED));
			}
		});
		autoJoinAutoAcceptTimeWidget.setChangedListener(input -> autoJoinAutoAcceptTime = parseIntFromString(input));
		autoJoinAutoAcceptTimeWidget.setTooltip(Tooltip.create(Text.translatable(
			"gui.bisect.start.auto_accept.time.tooltip")));
		addDrawableSelectableElement(autoJoinAutoAcceptTimeWidget);
	}

	private void addCommandInput(int row, int rowCount) {
		addDrawable(new TextWidget(
			cornerX(0),
			centerY(row, 10),
			CELL_WIDTH * 2 + HORIZONTAL_PADDING,
			10,
			Text.translatable("gui.bisect.start.command.label"),
			textRenderer
		));
		autoJoinCommandInput = new SpruceTextAreaWidget(
			Position.of(cornerX(0), cornerY(row + 1) - VERTICAL_PADDING),
			CELL_WIDTH * 2 + HORIZONTAL_PADDING,
			(rowCount - 1) * (CELL_HEIGHT + VERTICAL_PADDING) - VERTICAL_PADDING,
			Text.translatable("gui.bisect.start.command.input")
		);
		autoJoinCommandInput.setText(MainConfig.INSTANCE.defaultBisectSettings.autoJoinCommands());
		addDrawableSelectableElement(autoJoinCommandInput);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredShadowedText(this.textRenderer, this.title, this.width / 2, 15, 0xffffff);
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
