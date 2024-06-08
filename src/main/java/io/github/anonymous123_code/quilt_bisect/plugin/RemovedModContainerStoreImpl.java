package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.shared.RemovedModContainerStore;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.plugin.solver.ModLoadOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemovedModContainerStoreImpl implements RemovedModContainerStore {
	// See ReflectiveRemovedModContainerStore
	private static RemovedModContainerStoreImpl INSTANCE = new RemovedModContainerStoreImpl();
	private final Map<String, List<ModContainer>> removedMods = new HashMap<>();
	private final Map<String, List<ModLoadOption>> removedOptions = new HashMap<>();
	private boolean readPhase = false;

	public static RemovedModContainerStoreImpl getInstance() {
		return INSTANCE;
	}


	@Override
	public void addRemovedMod(ModLoadOption mod) {
		if (readPhase) {
			throw new IllegalStateException("Must not remove mods after loading is finished");
		}
		if (!removedOptions.containsKey(mod.id())) {
			removedOptions.put(mod.id(), new ArrayList<>());
		}
		removedOptions.get(mod.id()).add(mod);
	}

	@Override
	public @Nullable List<ModContainer> getRemovedMods(String modId) {
		if (!readPhase) {
			throw new IllegalStateException("Loading isnt finished, cannot get removed options");
		}
		return removedMods.get(modId);
	}

	@Override
	public void finishLoading() {
		for (Map.Entry<String, List<ModLoadOption>> entry : removedOptions.entrySet()) {
			var list = new ArrayList<ModContainer>();
			for (ModLoadOption mod : entry.getValue()) {
				list.add(mod.convertToMod(mod.resourceRoot()));
			}
			removedMods.put(entry.getKey(), list);
		}
		readPhase = true;
	}
}
