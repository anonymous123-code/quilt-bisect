package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.shared.RemovedModContainerStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.LoaderValue;
import org.quiltmc.loader.api.ModContributor;
import org.quiltmc.loader.api.ModDependency;
import org.quiltmc.loader.api.ModLicense;
import org.quiltmc.loader.api.Version;
import org.quiltmc.loader.api.plugin.ModMetadataExt;

import java.util.Collection;
import java.util.Map;

// See ReflectiveRemovedModContainerStore
public class SmuggleModMetadataExt implements ModMetadataExt {
	private final ModMetadataExt delegate;
	private final RemovedModContainerStore removedModContainerStore;

	public SmuggleModMetadataExt(ModMetadataExt delegate, RemovedModContainerStore removedModContainerStore) {
		this.delegate = delegate;
		this.removedModContainerStore = removedModContainerStore;
	}

	public RemovedModContainerStore getRemovedModContainerStore() {
		// See ReflectiveRemovedModContainerStore
		return removedModContainerStore;
	}

	@Nullable
	@Override
	public ModPlugin plugin() {
		return this.delegate.plugin();
	}

	@Override
	public Map<String, Collection<ModEntrypoint>> getEntrypoints() {
		return this.delegate.getEntrypoints();
	}

	@Override
	public Map<String, String> languageAdapters() {
		return this.delegate.languageAdapters();
	}

	@Override
	public Collection<String> mixins(EnvType env) {
		return this.delegate.mixins(env);
	}

	@Override
	public Collection<String> accessWideners() {
		return this.delegate.accessWideners();
	}

	@Override
	public ModEnvironment environment() {
		return this.delegate.environment();
	}

	@Override
	public String id() {
		return this.delegate.id();
	}

	@Override
	public String group() {
		return this.delegate.group();
	}

	@Override
	public Version version() {
		return this.delegate.version();
	}

	@Override
	public String name() {
		return this.delegate.name();
	}

	@Override
	public String description() {
		return this.delegate.description();
	}

	@Override
	public Collection<ModLicense> licenses() {
		return this.delegate.licenses();
	}

	@Override
	public Collection<ModContributor> contributors() {
		return this.delegate.contributors();
	}

	@Override
	public @Nullable String getContactInfo(String key) {
		return this.delegate.getContactInfo(key);
	}

	@Override
	public Map<String, String> contactInfo() {
		return this.delegate.contactInfo();
	}

	@Override
	public Collection<ModDependency> depends() {
		return this.delegate.depends();
	}

	@Override
	public Collection<ModDependency> breaks() {
		return this.delegate.breaks();
	}

	@Override
	public @Nullable String icon(int size) {
		return this.delegate.icon(size);
	}

	@Override
	public boolean containsValue(String key) {
		return this.delegate.containsValue(key);
	}

	@Override
	public @Nullable LoaderValue value(String key) {
		return this.delegate.value(key);
	}

	@Override
	public Map<String, LoaderValue> values() {
		return this.delegate.values();
	}
}
