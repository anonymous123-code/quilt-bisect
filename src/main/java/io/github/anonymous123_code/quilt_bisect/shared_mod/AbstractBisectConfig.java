package io.github.anonymous123_code.quilt_bisect.shared_mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractBisectConfig {
	public static final Path configDirectory = QuiltLoader.getConfigDir().resolve("quilt_bisect");
	private transient final Path location;
	private transient final String name;

	protected AbstractBisectConfig(String name) {
		this.location = configDirectory.resolve(name + ".json");
		this.name = name;
	}

	protected static <T extends AbstractBisectConfig> T create(Class<T> thisClass) {
		if (!Files.exists(configDirectory)) {
			try {
				Files.createDirectories(configDirectory);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to create directory '%s'", configDirectory), e);
			}
		}
		try {
			var constr = thisClass.getDeclaredConstructor();
			constr.setAccessible(true);
			T inst = constr.newInstance();
			inst.update();
			return inst;
		} catch (InstantiationException | InvocationTargetException | NoSuchMethodException |
				 IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}


	public void safe() throws IOException {
		var gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
		Files.writeString(location, gson.toJson(this));
	}

	public void update() {
		if (Files.exists(this.location) && Files.isRegularFile(this.location)) {
			GsonBuilder gsonB = new GsonBuilder()
				.setPrettyPrinting()
				.setLenient()
				.registerTypeAdapter(this.getClass(), (InstanceCreator<?>) type -> this);
			setupReadGson(gsonB);
			Gson gson = gsonB.create();
			try {
				gson.fromJson(Files.readString(this.location), this.getClass());
			} catch (IOException e) {
				throw new RuntimeException("Unable to read " + this.name + " config", e);
			}
		}
	}

	public abstract void setupReadGson(GsonBuilder builder);
}
