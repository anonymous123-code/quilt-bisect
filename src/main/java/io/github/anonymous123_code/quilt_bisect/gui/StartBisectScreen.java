package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.AutoTest;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import io.github.anonymous123_code.quilt_bisect.shared.Issue;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class StartBisectScreen extends CreateIssueScreen{
	private AutoJoinType autoJoinMode = AutoJoinType.None;
	private SpruceTextFieldWidget autoJoinNameWidget;
	protected StartBisectScreen(Text title, int rowCount, int columnCount, int rowHeight, int columnWidth, @Nullable Screen parent) {
		super(title, rowCount, columnCount, rowHeight, columnWidth, parent);
	}

	public StartBisectScreen(@Nullable Screen parent) {
		this(Text.translatable("gui.bisect.start.screen"), 2, 2, 30, 150, parent);
	}

	@Override
	protected void addCancelContinueButtons(Text continueText) {
		super.addCancelContinueButtons(Text.translatable("gui.bisect.start"));
	}

	@Override
	protected void setupWidgets() {
		super.setupWidgets();
		addAutoJoinOption(1);
	}

	@Override
	protected void onDone() {
		ActiveBisectConfig.update();
		ActiveBisectConfig activeBisectConfig = ActiveBisectConfig.getInstance();
		activeBisectConfig.issues.add(new Issue.UserIssue(issueNameWidget.getText()));
		activeBisectConfig.bisectSettings = new AutoTest(
			autoJoinMode.convertToAutoTest(),
			autoJoinNameWidget.getText(),
			"",
			false,
			0,
			false
		);
		saveAndQuit();
	}

	private void addAutoJoinOption(int row) {
		BisectUtils.Result result = BisectUtils.getAutoJoinData();
		String autoJoinName = result.autoJoinName();
		this.autoJoinMode = AutoJoinType.from(result.autoJoinMode());
		// Auto Join
		this.addDrawableChild(new SpruceButtonWidget(centerVertically(row, 0, 20),
			COLUMN_WIDTH,
			20,
			autoJoinMode.text,
			button -> {
				autoJoinMode = autoJoinMode.nextCycle(Screen.hasAltDown());
				button.setMessage(autoJoinMode.text);
			}
		));
		autoJoinNameWidget = new SpruceTextFieldWidget(centerVertically(row, 1, 20),
			COLUMN_WIDTH,
			20,
			Text.translatable("gui.bisect.new_issue.auto_join.name")
		);
		autoJoinNameWidget.setText(autoJoinName);
		this.addDrawableChild(autoJoinNameWidget);
	}


	public enum AutoJoinType {
		None(Text.translatable("gui.bisect.new_issue.auto_join.none")),
		World(Text.translatable("gui.bisect.new_issue.auto_join.world")),
		Server(Text.translatable("gui.bisect.new_issue.auto_join.server")),
		LasJoined(Text.translatable("gui.bisect.new_issue.auto_join.last_joined")),
		Realm(Text.translatable("gui.bisect.new_issue.auto_join.realm"));

		public final Text text;

		AutoJoinType(Text text) {
			this.text = text;
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

		static AutoJoinType from(AutoTest.AutoJoinType other) {
			return switch (other) {
				case LastJoined -> LasJoined;
				case Realm -> Realm;
				case None -> None;
				case Server -> Server;
				case World -> World;
			};
		}
	}
}
