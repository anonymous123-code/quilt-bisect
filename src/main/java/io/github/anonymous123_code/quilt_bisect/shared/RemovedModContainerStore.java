package io.github.anonymous123_code.quilt_bisect.shared;

import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.plugin.solver.ModLoadOption;

import java.util.List;
// See ReflectiveRemovedModContainerStore
public interface RemovedModContainerStore {
	void addRemovedMod(ModLoadOption mod);

	@Nullable
	List<ModContainer> getRemovedMods(String modId);

	void finishLoading();
}
