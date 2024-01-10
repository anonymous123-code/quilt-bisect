package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import io.github.anonymous123_code.quilt_bisect.GracefulTerminator;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.Issue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.CommonTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class SelectIssueScreen extends SpruceScreen {
	private final Screen parent;
	private IssueList issueList;

	public SelectIssueScreen(@Nullable Screen parent) {
		super(Text.literal("Bisect: Select Issue"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		this.issueList = new IssueList(MinecraftClient.getInstance());
        ArrayList<Issue> issues = ActiveBisectConfig.getInstance().issues;
        for (int id = 0; id < issues.size(); id++) {
            Issue issue = issues.get(id);
			if (issue instanceof Issue.NamedIssue namedIssue) {
				this.issueList.addElement(namedIssue, id);
			}
        }
		this.addDrawableChild(this.issueList);
		this.addDrawableChild(
			new SpruceButtonWidget(Position.of(this.width / 2 + 5, this.height - 30), ButtonWidget.DEFAULT_WIDTH, 20, Text.translatable("gui.bisect.continue"), button -> this.onDone())
		);
		this.addDrawableChild(
			new SpruceButtonWidget(Position.of(this.width / 2 + 5, this.height - 30 - 25), ButtonWidget.DEFAULT_WIDTH, 20, Text.translatable("gui.bisect.new_issue"), button -> this.newIssue())
		);
		this.addDrawableChild(
			new SpruceButtonWidget(Position.of(this.width / 2 - ButtonWidget.DEFAULT_WIDTH - 5, this.height - 30), ButtonWidget.DEFAULT_WIDTH, 20, CommonTexts.CANCEL, button -> this.onCancel())
		);
	}

	private void newIssue() {
		client.setScreen(new CreateIssueScreen(this, false));
	}

	private void onCancel() {
		closeScreen();
	}

	private void onDone() {
        try {
            Files.writeString(ActiveBisectConfig.configDirectory.resolve("issue.txt"), Integer.toString(issueList.getSelectedOrNull().id));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GracefulTerminator.gracefullyTerminate(56);
	}
	@Override
	public void closeScreen() {
		this.client.setScreen(parent);
	}

	public class IssueList extends AlwaysSelectedEntryListWidget<IssueList.IssueEntry> {
		public IssueList(MinecraftClient minecraftClient) {
			super(minecraftClient, SelectIssueScreen.this.width, SelectIssueScreen.this.height, 32, SelectIssueScreen.this.height-64, 20);
		}

		public void addElement(Issue.NamedIssue issue, int id) {
			var entry = new IssueEntry(issue, id);
			this.addEntry(entry);
			if (this.getSelectedOrNull() == null) {
				this.setSelected(entry);
			}
		}

		public class IssueEntry extends AlwaysSelectedEntryListWidget.Entry<IssueEntry> {
			public final Issue.NamedIssue issue;
			public final int id;
			private long lastClickTime;

			private IssueEntry(Issue.NamedIssue issue, int id) {
				this.issue = issue;
                this.id = id;
            }

			@Override
			public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
				graphics.drawCenteredShadowedText(
					SelectIssueScreen.this.textRenderer, Text.of(issue.getName()), SelectIssueScreen.IssueList.this.width / 2, y + 1, 16777215
				);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				if (button == 0) {
					this.onPressed();
					if (Util.getMeasuringTimeMs() - this.lastClickTime < 250L) {
						SelectIssueScreen.this.onDone();
					}

					this.lastClickTime = Util.getMeasuringTimeMs();
					return true;
				} else {
					this.lastClickTime = Util.getMeasuringTimeMs();
					return false;
				}
			}

			void onPressed() {
				IssueList.this.setSelected(this);
			}

			@Override
			public Text getNarration() {
				return Text.translatable("narrator.select", this.issue.getName());
			}
		}
	}
}
