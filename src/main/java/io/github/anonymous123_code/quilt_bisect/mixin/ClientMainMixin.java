package io.github.anonymous123_code.quilt_bisect.mixin;

import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.AutoTest;
import io.github.anonymous123_code.quilt_bisect.shared.BisectUtils;
import net.minecraft.client.RunArgs;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Main.class)
public class ClientMainMixin {

	@ModifyArg(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;<init>(Lnet/minecraft/client/RunArgs;)V"))
	private static RunArgs useQuickPlayFeatureToAutoJoin(RunArgs args) {
		ActiveBisectConfig.update();
		ActiveBisectConfig activeBisectConfig = ActiveBisectConfig.getInstance();

		if (activeBisectConfig.isActive()) {
			String singleplayer = args.quickPlay.singleplayer();
			String multiplayer = args.quickPlay.multiplayer();
			String realm = args.quickPlay.realms();

			AutoTest.AutoJoinType autoJoinType = activeBisectConfig.bisectSettings.autoJoinType();
			String autoJoinName = activeBisectConfig.bisectSettings.autoJoinName();
			if (autoJoinType == AutoTest.AutoJoinType.LastJoined) {
				var result = BisectUtils.getAutoJoinData();
				autoJoinType = result.autoJoinMode();
				autoJoinName = result.autoJoinName();
			}

			switch (autoJoinType) {
				case Server -> multiplayer = autoJoinName;
				case World -> singleplayer = autoJoinName;
				case Realm -> realm = autoJoinName;
				case LastJoined -> throw new IllegalStateException();
			}

			return new RunArgs(
				args.network,
				args.windowSettings,
				args.directories,
				args.game,
				new RunArgs.QuickPlay(args.quickPlay.path(), singleplayer, multiplayer, realm)
			);
		}
		return args;
	}
}
