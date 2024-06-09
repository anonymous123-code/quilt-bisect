package io.github.anonymous123_code.quilt_bisect.mixin;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInvoker {
	@SuppressWarnings("UnusedReturnValue")
	@Invoker("addDrawableSelectableElement")
	<T extends Element & Drawable & Selectable> T invokeAddDrawableSelectableElement(T drawableElement);
}
