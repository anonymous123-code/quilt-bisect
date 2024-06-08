package io.github.anonymous123_code.quilt_bisect.shared;

import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.plugin.solver.ModLoadOption;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ReflectiveRemovedContainerStoreImpl implements RemovedModContainerStore {
	public final Object inner;
	public final Method addRemovedModMethod;
	public final Method getRemovedModsMethod;
	public final Method finishLoadingMethod;

	public ReflectiveRemovedContainerStoreImpl(Object inner) {
		this.inner = inner;
		var innerClass = inner.getClass();
		try {
			this.addRemovedModMethod = innerClass.getMethod("addRemovedMod", ModLoadOption.class);
			this.getRemovedModsMethod = innerClass.getMethod("getRemovedMods", String.class);
			this.finishLoadingMethod = innerClass.getMethod("finishLoading");
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static RemovedModContainerStore obtainPluginInstance() {
		// Because static fields are per class loader, we need to move a proper RemovedModContainer store instace to this class loader.
		// In order to do that, the the ModLoadOption with the id "java" has it's metadata replaced with a SmuggleModMetadataExt instance in BisectPlugin.smuggleRemovedModContainerStore()
		// Then we can retrieve the new container, which carries over the metadata, and thus the RemovedModContainer store.
		// However, that is still not of the same class as if we would load it on this class loader. In order to circumvent that we wrap all methods with reflection.
		try {
			var javaMetadata = (QuiltLoader.getModContainer("java").orElseThrow().metadata());
			var javaClass = javaMetadata.getClass();
			return new ReflectiveRemovedContainerStoreImpl(javaClass.getMethod("getRemovedModContainerStore")
																	.invoke(javaMetadata));
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addRemovedMod(ModLoadOption mod) {
		try {
			addRemovedModMethod.invoke(inner, mod);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public @Nullable List<ModContainer> getRemovedMods(String modId) {
		try {
			return (List<ModContainer>) getRemovedModsMethod.invoke(inner, modId);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void finishLoading() {
		try {
			finishLoadingMethod.invoke(inner);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
