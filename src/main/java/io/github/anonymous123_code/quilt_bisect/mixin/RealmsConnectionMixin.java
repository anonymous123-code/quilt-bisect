package io.github.anonymous123_code.quilt_bisect.mixin;

import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.AutoTest;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.realms.RealmsConnection;
import net.minecraft.client.realms.dto.RealmsServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;

@Mixin(RealmsConnection.class)
public class RealmsConnectionMixin {

	@Inject(method = "connect", at = @At("HEAD"))
	public void updateAutoJoinData(RealmsServer server, ServerAddress address, CallbackInfo ci) {
		try {
			Files.writeString(ActiveBisectConfig.configDirectory.resolve("lastActiveJoin.txt"), AutoTest.AutoJoinType.Realm + "\n" + server.id);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
