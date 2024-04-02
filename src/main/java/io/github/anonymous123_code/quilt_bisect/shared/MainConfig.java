package io.github.anonymous123_code.quilt_bisect.shared;

import com.google.gson.GsonBuilder;

public class MainConfig extends AbstractBisectConfig {
	public static final MainConfig INSTANCE = create(MainConfig.class);

	protected MainConfig() {
		super("main");
	}

	@Override
	public void setupReadGson(GsonBuilder builder) {

	}
}
