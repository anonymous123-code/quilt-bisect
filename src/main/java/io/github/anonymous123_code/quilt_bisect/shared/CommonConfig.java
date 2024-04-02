package io.github.anonymous123_code.quilt_bisect.shared;

import org.quiltmc.loader.api.QuiltLoader;

import java.nio.file.Path;

public class CommonConfig {

	public static final Path configDirectory = QuiltLoader.getConfigDir().resolve("quilt_bisect");
}
