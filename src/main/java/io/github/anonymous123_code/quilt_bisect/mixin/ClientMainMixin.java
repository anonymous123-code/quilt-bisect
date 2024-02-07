package io.github.anonymous123_code.quilt_bisect.mixin;

import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
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
			switch (activeBisectConfig.bisectSettings.autoJoinType()) {
				case Server -> multiplayer = activeBisectConfig.bisectSettings.autoJoinName();
				case World -> singleplayer = activeBisectConfig.bisectSettings.autoJoinName();
				case Realm -> realm = activeBisectConfig.bisectSettings.autoJoinName();
				case LastJoined -> throw new RuntimeException("TODO"); // TODO
			}
			return new RunArgs(args.network, args.windowSettings, args.directories, args.game, new RunArgs.QuickPlay(args.quickPlay.path(), singleplayer, multiplayer, realm));
		}
		return args;
	}
}
