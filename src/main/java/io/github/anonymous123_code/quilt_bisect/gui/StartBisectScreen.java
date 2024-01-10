package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
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

public class StartBisectScreen extends SpruceScreen {
	private final Screen parent;
	private SpruceTextFieldWidget widget;

	public StartBisectScreen(@Nullable Screen parent) {
		super(Text.translatable("gui.bisect.start.screen"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		this.addDrawableChild(new SpruceLabelWidget(Position.of(this.width/2-150, this.height/2-5), Text.translatable("gui.bisect.issue_name"), 150, false));
		widget = new SpruceTextFieldWidget(Position.of(this.width/2 + 5, this.height/2-10), 150, 20, Text.translatable("gui.bisect.issue_name"));
		this.addDrawableChild(widget);
		this.addDrawableChild(
			new SpruceButtonWidget(Position.of(this.width / 2 + 5, this.height - 38), ButtonWidget.DEFAULT_WIDTH, 20, Text.translatable("gui.bisect.start"), button -> this.onDone())
		);
		this.addDrawableChild(
			new SpruceButtonWidget(Position.of(this.width / 2 - ButtonWidget.DEFAULT_WIDTH - 5, this.height - 38), ButtonWidget.DEFAULT_WIDTH, 20, CommonTexts.CANCEL, button -> this.onCancel())
		);
	}

	private void onCancel() {
		closeScreen();
	}

	private void onDone() {
		ActiveBisectConfig.update();
		ActiveBisectConfig activeBisectConfig = ActiveBisectConfig.getInstance();
		activeBisectConfig.issues.add(new Issue.UserIssue(widget.getText(), "", ""));
		activeBisectConfig.bisectActive = true;
        try {
            activeBisectConfig.safe(false);
			Files.writeString(ActiveBisectConfig.configDirectory.resolve("issue.txt"), Integer.toString(activeBisectConfig.issues.size()-1));
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
