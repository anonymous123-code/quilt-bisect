package io.github.anonymous123_code.quilt_bisect.mixin;

import io.github.anonymous123_code.quilt_bisect.QuiltBisect;
import io.github.anonymous123_code.quilt_bisect.gui.BisectSummaryScreen;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.AutoTest;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
	@Shadow
	@Final
	private boolean doBackgroundFade;

	@Inject(method = "init", at = @At("HEAD"))
	public void init(CallbackInfo ci) {
		var conf = ActiveBisectConfig.getInstance();
		if (this.doBackgroundFade && !conf.isActive() && !conf.issues.isEmpty()) {
			MinecraftClient.getInstance().setScreen(new BisectSummaryScreen());
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		var conf = ActiveBisectConfig.getInstance();
		if (conf.isActive() && conf.bisectSettings.autoAccept() && conf.bisectSettings.autoJoinType() == AutoTest.AutoJoinType.None && QuiltBisect.firstTimeInAutoJoinScope) {
			QuiltBisect.joinedTimestamp = System.currentTimeMillis();
			QuiltBisect.firstTimeInAutoJoinScope = false;
		}
	}
}
