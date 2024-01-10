package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.CommonTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class SelectIssueScreen extends SpruceScreen {
	private final Screen parent;

	public SelectIssueScreen(@Nullable Screen parent) {
		super(Text.literal("Bisect: Select Issue"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		this.addDrawableChild(
			new SpruceButtonWidget(Position.of(this.width / 2 + 5, this.height - 38), ButtonWidget.DEFAULT_WIDTH, 20, CommonTexts.DONE, button -> this.onDone())
		);
		this.addDrawableChild(
			new SpruceButtonWidget(Position.of(this.width / 2 - ButtonWidget.DEFAULT_WIDTH - 5, this.height - 38), ButtonWidget.DEFAULT_WIDTH, 20, CommonTexts.CANCEL, button -> this.onCancel())
		);
	}

	private void onCancel() {
		closeScreen();
	}

	private void onDone() {
		closeScreen();
	}
	@Override
	public void closeScreen() {
		this.client.setScreen(parent);
	}
}
