package io.github.anonymous123_code.quilt_bisect;

import io.github.anonymous123_code.quilt_bisect.gui.SelectIssueScreen;
import io.github.anonymous123_code.quilt_bisect.gui.StartBisectScreen;
import io.github.anonymous123_code.quilt_bisect.shared.ActiveBisectConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;
import org.quiltmc.qsl.screen.api.client.ScreenEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuiltBisect implements ModInitializer, PreLaunchEntrypoint, ClientTickEvents.Start {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod name as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("Quilt Bisect");
	public static boolean firstJoin = true;
	public static long joinedTimestamp = -1;

	@Override
	public void onInitialize(ModContainer mod) {
		var bisectConfig = ActiveBisectConfig.getInstance();
		if (bisectConfig.isActive()) {
			ScreenEvents.AFTER_INIT.register((screen, client, firstInit) -> {
				screen.getButtons().add(ButtonWidget.builder(Text.of("No Issue"), buttonWidget -> markAsWorkingAndShutdown()).position(screen.width / 2 - ButtonWidget.DEFAULT_WIDTH - 5, 0).build());
				screen.getButtons().add(ButtonWidget.builder(Text.of("Manual Issue"), buttonWidget -> screen.getClient().setScreen(new SelectIssueScreen(screen))).position(screen.width / 2 + 5, 0).build());
			});
			ClientTickEvents.START.register(this);
		} else {
			ScreenEvents.AFTER_INIT.register((screen, client, firstInit) -> screen.getButtons().add(ButtonWidget.builder(Text.translatable("gui.bisect.start"), (w) -> screen.getClient().setScreen(new StartBisectScreen(screen))).position(screen.width / 2 - ButtonWidget.DEFAULT_WIDTH / 2, 0).build()));
		}
	}

	@Override
	public void onPreLaunch(ModContainer mod) {
		if (!"true".equals(System.getProperty("quiltBisect.active"))) {
			throw new RuntimeException("Failed to load loader plugin: Something went very long");
		}
		LOGGER.info("Successfully battled quilt loader demons, bisect is alive! Cursed stuff may happen, make sure to test the errors you found without bisect before reporting them. (Bisect can't include itself in search)");
	}

	public static void markAsWorkingAndShutdown() {
		GracefulTerminator.gracefullyTerminate(56);
	}

	@Override
	public void startClientTick(MinecraftClient client) {
		if (joinedTimestamp > 0) {
			long currentTime = System.currentTimeMillis();
			long deltaTime = currentTime - joinedTimestamp;
			long deltaTimeTicks = deltaTime / 50; // 50ms per tick
			if (deltaTimeTicks > ActiveBisectConfig.getInstance().bisectSettings.autoAcceptTime()) {
				IntegratedServer server = client.getServer();
				if (server == null || server.getTicks() > ActiveBisectConfig.getInstance().bisectSettings.autoAcceptTime()) { // If a server exists, both the server must have ticked for the autoAcceptTime, and the client must have been in the world for long enough
					markAsWorkingAndShutdown();
				}
			}
		}
	}
}
