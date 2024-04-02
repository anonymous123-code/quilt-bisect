package io.github.anonymous123_code.quilt_bisect.shared;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import org.quiltmc.loader.api.plugin.solver.ModLoadOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class ActiveBisectConfig extends AbstractBisectConfig {
	public static final ActiveBisectConfig INSTANCE = create(ActiveBisectConfig.class);
	public final HashMap<String, ModSet> modSets = new HashMap<>();
	public final ArrayList<Issue> issues = new ArrayList<>();
	public final HashMap<String, String> modIdToFile = new HashMap<>();
	public AutoTest bisectSettings = null;
	private ActiveBisectConfig() {
		super("active_bisect");
	}

	@Override
	public void setupReadGson(GsonBuilder builder) {
		builder.registerTypeAdapter(
				   ModSet.class,
				   (JsonDeserializer<ModSet>) (jsonElement, type, jsonDeserializationContext) -> {
					   if (jsonElement.getAsJsonObject().get("working").getAsBoolean()) {
						   return new Gson().fromJson(jsonElement, ModSet.Working.class);
					   } else {
						   return new Gson().fromJson(jsonElement, ModSet.Erroring.class);
					   }
				   }
			   )
			   .registerTypeAdapter(
				   Issue.class,
				   (JsonDeserializer<Issue>) (jsonElement, type, jsonDeserializationContext) -> {
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
				   }
			   );
	}

	public boolean isActive() {
		return bisectSettings != null;
	}

	public void updateLoadOptions(List<ModLoadOption> loadOptions) {
		for (ModLoadOption loadOption : loadOptions) {
			var fileName = loadOption.from()
									 .getFileName()
									 .toString(); // TODO Figure out how to get the hash. For now use the file name
			if (modIdToFile.containsKey(loadOption.id())) {
				if (!modIdToFile.get(loadOption.id()).equals(fileName)) {
					for (var modSet : modSets.values()) {
						if (modSet.modSet.contains(loadOption.id())) {
							modSet.invalidated = true;
						}
					}
					modIdToFile.put(loadOption.id(), fileName);
				}
			} else {
				modIdToFile.put(loadOption.id(), fileName);
			}
		}
	}

	public Optional<ModSet> getFirstInvalidatedModSet() {
		for (ModSet modSet : modSets.values()) {
			if (modSet.invalidated) {
				return Optional.of(modSet);
			}
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
			if (set.isWorkingOrFixed(this)) {
				continue;
			}
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
