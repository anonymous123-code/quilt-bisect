package io.github.anonymous123_code.quilt_bisect.mixin;

import io.github.anonymous123_code.quilt_bisect.QuiltBisect;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import io.github.anonymous123_code.quilt_bisect.shared.AutoTest;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.resource.pack.ResourcePackManager;
import net.minecraft.server.WorldStem;
import net.minecraft.world.storage.WorldSaveStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Inject(method = "startIntegratedServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/storage/WorldSaveStorage$Session;backupLevelDataFile(Lnet/minecraft/registry/DynamicRegistryManager;Lnet/minecraft/world/SaveProperties;)V"))
	public void updateAutoJoinData(String worldId, WorldSaveStorage.Session session, ResourcePackManager resourcePackManager, WorldStem worldStem, boolean isNewWorld, CallbackInfo ci) {
        try {
            Files.writeString(ActiveBisectConfig.configDirectory.resolve("lastActiveJoin.txt"), AutoTest.AutoJoinType.World + "\n" + worldId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("TAIL"))
	public void disableAutoAcceptOnDisconnect(Screen screen, CallbackInfo ci) {
		if (ActiveBisectConfig.getInstance().isActive() && ActiveBisectConfig.getInstance().bisectSettings.autoJoinType() != AutoTest.AutoJoinType.None) { // Only disable auto accept if something was automatically joined
			QuiltBisect.joinedTimestamp = 0;
		}
	}
}
