package io.github.anonymous123_code.quilt_bisect;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.screen.api.client.ScreenEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuiltBisect implements ModInitializer, PreLaunchEntrypoint {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod name as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("Quilt Bisect");

	@Override
	public void onInitialize(ModContainer mod) {
		ScreenEvents.AFTER_INIT.register((screen, client, firstInit) -> {
            if (screen instanceof TitleScreen) {
                screen.getButtons().add(ButtonWidget.builder(Text.of("Hi"), buttonWidget -> GracefulTerminator.gracefullyTerminate(57)).build());
            }
        });
	}

	@Override
	public void onPreLaunch(ModContainer mod) {
		if (!"true".equals(System.getProperty("quiltBisect.active"))) {
			throw new RuntimeException("Failed to load loader plugin: Something went very long");
		}
		LOGGER.info("Successfully battled quilt loader demons, bisect is alive! Cursed stuff may happen, make sure to test the errors you found without bisect before reporting them. (Bisect can't include itself in search)");
	}
}
