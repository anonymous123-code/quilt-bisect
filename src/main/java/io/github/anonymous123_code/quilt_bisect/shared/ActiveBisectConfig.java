package io.github.anonymous123_code.quilt_bisect.shared;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ActiveBisectConfig {
	public static final Path configDirectory = QuiltLoader.getConfigDir().resolve("quilt_bisect");
	private static ActiveBisectConfig INSTANCE = create();

	public static ActiveBisectConfig getInstance() {
		return INSTANCE;
	}

	private ActiveBisectConfig() {}

	private static ActiveBisectConfig create() {
		if (!Files.exists(configDirectory)) {
			try {
				Files.createDirectories(configDirectory);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to create directory '%s'", configDirectory), e);
			}
		}
		var config_file = configDirectory.resolve("active_bisect.json");
		if (Files.exists(config_file) && Files.isRegularFile(config_file)) {
			return serializeFromFile(config_file);
		} else {
			return new ActiveBisectConfig();
		}
	}

	public static void update() {
		INSTANCE = create();
	}

	public boolean isUpToDate() {
		return INSTANCE == this;
	}

	public boolean isActive() {
		return bisectSettings != null;
	}

	public void safe(boolean force) throws IOException {
		if (!force && !isUpToDate()) return;
		var config_file = configDirectory.resolve("active_bisect.json");
		var gson = new GsonBuilder().setPrettyPrinting().create();
		Files.writeString(config_file, gson.toJson(this));
	}

	private static ActiveBisectConfig serializeFromFile(Path configFile) {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ModSet.class, (JsonDeserializer<ModSet>) (jsonElement, type, jsonDeserializationContext) -> {
				if (jsonElement.getAsJsonObject().get("working").getAsBoolean()) {
					return new Gson().fromJson(jsonElement, ModSet.Working.class);
				} else {
					return new Gson().fromJson(jsonElement, ModSet.Erroring.class);
				}
			})
			.registerTypeAdapter(Issue.class, (JsonDeserializer<Issue>) (jsonElement, type, jsonDeserializationContext) -> {
				var name = jsonElement.getAsJsonObject().get("type").getAsString();
				switch (name) {
					case "crash" -> {
						return new Gson().fromJson(jsonElement, Issue.CrashIssue.class);
					}
					case "log" -> {
						return new Gson().fromJson(jsonElement, Issue.LogIssue.class);
					}
					case "user" -> {
						return new Gson().fromJson(jsonElement, Issue.UserIssue.class);
					}
					default -> throw new JsonParseException("Invalid Type");
				}
			})
			.create();
		try {
			return gson.fromJson(Files.readString(configFile), ActiveBisectConfig.class);
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	public AutoTest bisectSettings = null;
	public final HashMap<String, ModSet> modSets = new HashMap<>();
	public final ArrayList<Issue> issues = new ArrayList<>();
	public final HashMap<String, String> modIdToFile = new HashMap<>();


	public void updateFiles(HashMap<String, Path> modSetToPath) {
		for (var entry : modSetToPath.entrySet()) {
			var fileName = entry.getValue().getFileName().toString();
			if (modIdToFile.containsKey(entry.getKey())) {
				if (!modIdToFile.get(entry.getKey()).equals(fileName)) {
					for (var modSet: modSets.values()) {
						if (modSet.modSet.contains(entry.getKey())) modSet.invalidated = true;
					}
					modIdToFile.put(entry.getKey(), fileName);
				}
			} else {
				modIdToFile.put(entry.getKey(), fileName);
			}
		}
	}

	public Optional<ModSet> getFirstInvalidatedModSet() {
		for (ModSet modSet : modSets.values()) {
			if (modSet.invalidated) return Optional.of(modSet);
		}
		return Optional.empty();
	}

	public Optional<ModSet> getModSet(ModSet.Section first, ModSet.Section... sections) {
        ArrayList<String> mods = new ArrayList<>(first.getListCopy());
		for (ModSet.Section section : sections) {
			mods.addAll(section.getListCopy());
		}
		return getModSet(mods);
	}
	public Optional<ModSet> getModSet(List<String> modIds) {
		Collections.sort(modIds);
		return modSets.values().stream().filter(it -> modIds.equals(it.modSet)).findAny();
	}

	public ModSet.Erroring findSmallestUnfixedModSet() {
		ModSet.Erroring smallest = null;
		int smallestSize = Integer.MAX_VALUE;
		for (var set : modSets.values()) {
			if (set.isWorkingOrFixed(this)) continue;
			if (set.modSet.size() < smallestSize) {
				smallest = (ModSet.Erroring) set;
				smallestSize = set.modSet.size();
			}
		}
		return smallest;
	}

	public List<Integer> findFixedIssues() {
		var result = new ArrayList<Integer>();
		ISSUES:
		for (int i = 0; i < issues.size(); i++) {
			for (ModSet modSet : modSets.values()) {
				if (modSet instanceof ModSet.Erroring erroring && erroring.issueId == i) {
					if (!modSet.isWorkingOrFixed(this)) {
						continue ISSUES;
					}
				}
			}
			result.add(i);
		}
		return result;
	}
}
