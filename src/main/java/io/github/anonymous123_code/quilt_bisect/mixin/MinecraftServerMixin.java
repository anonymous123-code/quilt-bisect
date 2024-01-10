package io.github.anonymous123_code.quilt_bisect.mixin;
/*
 * Licensed under the MIT license by comp500 (2023). See the ModVote-License file in the repository root.
 * Modifications:
 * - Adjusted package
 */
import io.github.anonymous123_code.quilt_bisect.GracefulTerminator;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	public void quiltbisect$onInit(CallbackInfo ci) {
		GracefulTerminator.addServer((MinecraftServer)(Object)this);
	}
}
