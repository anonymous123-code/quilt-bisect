package io.github.anonymous123_code.quilt_bisect.gui;
/*
Heavily inspired by mod menu code (https://github.com/TerraformersMC/ModMenu), which is under the MIT license
 */


import com.mojang.blaze3d.texture.NativeImage;
import io.github.anonymous123_code.quilt_bisect.QuiltBisect;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.Closeable;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.github.anonymous123_code.quilt_bisect.QuiltBisect.LOGGER;

public class IconStore implements Closeable {
	private final Map<Path, NativeImageBackedTexture> modIconCache = new HashMap<>();

	public NativeImageBackedTexture createIcon(ModContainer iconSource, String iconPath) {
		try {
			Path path = iconSource.getPath(iconPath);
			NativeImageBackedTexture cachedIcon = getCachedModIcon(path);
			if (cachedIcon != null) {
				return cachedIcon;
			}
			try (InputStream inputStream = Files.newInputStream(path)) {
				NativeImage image = NativeImage.read(Objects.requireNonNull(inputStream));
				Validate.validState(image.getHeight() == image.getWidth(), "Must be square icon");
				NativeImageBackedTexture tex = new NativeImageBackedTexture(image);
				cacheModIcon(path, tex);
				return tex;
			}

		} catch (IllegalStateException e) {
			if (e.getMessage().equals("Must be square icon")) {
				LOGGER.error(
					"Mod icon must be a square for icon source {}: {}",
					iconSource.metadata().id(),
					iconPath,
					e
				);
			}

			return null;
		} catch (Throwable t) {
			if (!iconPath.equals("assets/" + iconSource.metadata().id() + "/icon.png")) {
				LOGGER.error("Invalid mod icon for icon source {}: {}", iconSource.metadata().id(), iconPath, t);
			}
			return null;
		}
	}

	public @NotNull NativeImageBackedTexture getIcon(ModContainer container, int i) {
		String iconSourceId = container.metadata().id();
		String iconPath = container.metadata().icon(i);
		if (iconPath == null) {
			iconPath = "assets/" + container.metadata().id() + "/icon.png";
		}
		if ("minecraft".equals(container.metadata().id())) {
			iconSourceId = QuiltBisect.MOD_ID;
			iconPath = "assets/" + QuiltBisect.MOD_ID + "/minecraft_icon.png";
		} else if ("java".equals(container.metadata().id())) {
			iconSourceId = QuiltBisect.MOD_ID;
			iconPath = "assets/" + QuiltBisect.MOD_ID + "/java_icon.png";
		}
		ModContainer iconSource = QuiltLoader.getModContainer(iconSourceId).orElse(container);
		NativeImageBackedTexture icon = this.createIcon(iconSource, iconPath);
		if (icon == null) {
			LOGGER.warn("Warning! Mod {} has a broken icon, loading default icon", container.metadata().id());
			return this.createIcon(
				QuiltLoader.getModContainer(QuiltBisect.MOD_ID)
						   .orElseThrow(() -> new RuntimeException("Cannot get ModContainer for Fabric mod with id " + QuiltBisect.MOD_ID)),
				"assets/" + QuiltBisect.MOD_ID + "/unknown_icon.png"
			);
		}
		return icon;
	}

	@Override
	public void close() {
		for (NativeImageBackedTexture tex : modIconCache.values()) {
			tex.close();
		}
	}

	NativeImageBackedTexture getCachedModIcon(Path path) {
		return modIconCache.get(path);
	}

	void cacheModIcon(Path path, NativeImageBackedTexture tex) {
		modIconCache.put(path, tex);
	}
}
