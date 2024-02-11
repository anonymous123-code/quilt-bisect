package io.github.anonymous123_code.quilt_bisect.mixin;

import io.github.anonymous123_code.quilt_bisect.QuiltBisect;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DownloadingTerrainScreen.class)
public class DownloadingTerrainScreenMixin extends Screen {
	protected DownloadingTerrainScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "closeScreen", at = @At("TAIL"))
	public void onReadyToPlay(CallbackInfo ci) {
		if (QuiltBisect.shouldExecuteCommands) {
			for (String message : ActiveBisectConfig.getInstance().bisectSettings.autoJoinCommands().split("\n")) {
				if (!message.isEmpty()) {
					assert client != null;
					assert client.player != null;
					client.inGameHud.getChatHud().addToMessageHistory(message);

					if (message.startsWith("/")) {
						client.player.networkHandler.sendChatCommand(message.substring(1));
					} else {
						client.player.networkHandler.sendChatMessage(message);
					}
				}
			}
			QuiltBisect.shouldExecuteCommands = false;
		}
	}
}
