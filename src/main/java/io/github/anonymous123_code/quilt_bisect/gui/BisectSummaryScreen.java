package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.SpruceSeparatorWidget;
import io.github.anonymous123_code.quilt_bisect.gui.widget.ModWidget;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import io.github.anonymous123_code.quilt_bisect.shared.Issue;
import io.github.anonymous123_code.quilt_bisect.shared.ReflectiveRemovedContainerStoreImpl;
import io.github.anonymous123_code.quilt_bisect.shared.RemovedModContainerStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.text.CommonTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.QuiltLoader;

import java.util.Set;

public class BisectSummaryScreen extends SpruceScreen {
	public final IconStore iconStore = new IconStore();
	private final ModWidget.ClickBehaviorHolder behaviorHolder;
	public RemovedModContainerStore removedModContainerStore;

	public BisectSummaryScreen() {
		super(Text.of("Bisect summary"));
		this.behaviorHolder = new ModWidget.ClickBehaviorHolder(ModWidget.ClickBehavior.OpenPath);
	}


	@Override
	protected void init() {
		removedModContainerStore = ReflectiveRemovedContainerStoreImpl.obtainPluginInstance();
		removedModContainerStore.finishLoading();
		super.init();
		var oldBisect = ActiveBisectConfig.INSTANCE;
		int x = this.width / 2 - 155;
		int y = 40;
		var cycleButton = new SpruceButtonWidget(
			Position.of(this.width / 2 + 5, y),
			150,
			20,
			behaviorHolder.inner.text(),
			button -> {
				behaviorHolder.inner = behaviorHolder.inner.next();
				button.setMessage(behaviorHolder.inner.text());
			}
		);
		addDrawableChild(new SpruceLabelWidget(
			Position.of(this.width / 2 - 155, y + 3),
			Text.of("On mod click").setStyle(Style.EMPTY.withColor(Formatting.WHITE)).get(0),
			150,
			true
		));
		y += cycleButton.getHeight();
		addSelectableChild(cycleButton);
		int crashIndex = 1;
		for (Issue issue : oldBisect.issues) {
			if (issue instanceof Issue.CrashIssue) {
				y = draw_issue(
					x,
					y,
					310,
					Text.of("Crash " + crashIndex),
					Text.of(((Issue.CrashIssue) issue).stacktraces.get(0).split("\n", 2)[0]),
					issue
				);
				crashIndex++;
			} else if (issue instanceof Issue.NamedIssue logIssue) {
				y = draw_issue(x, y, 310, Text.of(logIssue.getName()), null, issue);
			}
		}
		this.addSelectableChild(new SpruceButtonWidget(
			Position.of(this.width / 2 - 100, this.height - 38),
			200,
			20,
			CommonTexts.DONE,
			button -> this.closeScreen()
		));
	}

	@Override
	public void closeScreen() {
		super.closeScreen();
		this.iconStore.close();
	}

	@Override
	public void renderTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		guiGraphics.drawCenteredShadowedText(this.textRenderer, this.title, this.width / 2, 15, 0xffffff);
	}

	int draw_issue(int x, int y, int maxWidth, Text header, @Nullable Text subtitle, Issue issue) {
		y += 5;
		var headerLabel = new SpruceSeparatorWidget(Position.of(x, y), maxWidth, header);
		this.addDrawableChild(headerLabel);
		y += headerLabel.getHeight();
		if (subtitle != null) {
			var subtitleLabel = new SpruceLabelWidget(Position.of(x, y), subtitle, maxWidth);
			this.addDrawableChild(subtitleLabel);
			y += subtitleLabel.getHeight();
		}
		var fixesLabel = new SpruceLabelWidget(Position.of(x, y), Text.of("Fixes:"), maxWidth);
		y += fixesLabel.getHeight();
		this.addDrawableChild(fixesLabel);
		for (Set<String> fix : BisectUtils.mergeReproductions(issue.fix.reproductions)) {
			y = draw_fix(x, y, maxWidth, fix);
		}
		return y;
	}

	int draw_fix(int x, int y, int maxWidth, Set<String> fix) {
		var fixStart = new SpruceLabelWidget(Position.of(x, y), Text.of("- Remove "), maxWidth);
		x += fixStart.getWidth();
		maxWidth -= fixStart.getWidth();
		this.addDrawableChild(fixStart);

		int currentX = x;
		int currentWidth = 0;

		String[] array = fix.toArray(new String[0]);
		for (int i = 0; i < array.length; i++) {
			String modId = array[i];
			var modContainer = QuiltLoader.getModContainer(modId)
										  .orElseGet(() -> removedModContainerStore.getRemovedMods(modId).get(0));
			var widget = new ModWidget(currentX, y, behaviorHolder, modContainer, iconStore);
			if (currentWidth + widget.getWidth() > maxWidth) {
				currentWidth = 0;
				currentX = x + 10;
				y += ModWidget.height();
				widget.setX(currentX);
				widget.setY(y);
			}
			currentWidth += widget.getWidth();
			currentX += widget.getWidth() + 2;
			if (i < array.length - 2) {
				var commaLabel = new SpruceLabelWidget(Position.of(currentX, y), Text.of(", "), maxWidth);
				currentX += commaLabel.getWidth();
				this.addDrawableChild(commaLabel);
			} else if (i == array.length - 2) {
				var andLabel = new SpruceLabelWidget(Position.of(currentX, y), Text.of(" and "), maxWidth);
				currentX += andLabel.getWidth();
				this.addDrawableChild(andLabel);
			}
			this.addSelectableChild(widget);
		}
		y += ModWidget.height();
		return y;
	}
}
