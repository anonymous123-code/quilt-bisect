package io.github.anonymous123_code.quilt_bisect.plugin;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.*;
import org.quiltmc.loader.api.plugin.ModMetadataExt;

import java.util.Collection;
import java.util.Map;

public class WrappedModMetadataExt implements ModMetadataExt {
	private final ModMetadataExt inner;
	private final ModLoadType loadType;

	public WrappedModMetadataExt(ModMetadataExt inner) {
		this.inner = inner;
		this.loadType = null;
	}

	public WrappedModMetadataExt(ModMetadataExt inner, ModLoadType loadType) {
		this.inner = inner;
		this.loadType = loadType;
	}

	@Override
	public boolean shouldQuiltDefineDependencies() {
		return inner.shouldQuiltDefineDependencies();
	}

	@Override
	public boolean shouldQuiltDefineProvides() {
		return inner.shouldQuiltDefineProvides();
	}

	@Override
	public ModLoadType loadType() {
		return this.loadType == null ? inner.loadType() : this.loadType;
	}

	@Nullable
	@Override
	public ModPlugin plugin() {
		return inner.plugin();
	}

	@Override
	public Map<String, Collection<ModEntrypoint>> getEntrypoints() {
		return inner.getEntrypoints();
	}

	@Override
	public Map<String, String> languageAdapters() {
		return inner.languageAdapters();
	}

	@SuppressWarnings("UnstableApiUsage")
	@Override
	public Collection<String> mixins(EnvType env) {
		return inner.mixins(env);
	}

	@SuppressWarnings("UnstableApiUsage")
	@Override
	public Collection<String> accessWideners() {
		return inner.accessWideners();
	}

	@SuppressWarnings("UnstableApiUsage")
	@Override
	public ModEnvironment environment() {
		return inner.environment();
	}

	@Override
	public String id() {
		return inner.id();
	}

	@Override
	public String group() {
		return inner.group();
	}

	@Override
	public Version version() {
		return inner.version();
	}

	@Override
	public String name() {
		return inner.name();
	}

	@Override
	public String description() {
		return inner.description();
	}

	@Override
	public Collection<ModLicense> licenses() {
		return inner.licenses();
	}

	@Override
	public Collection<ModContributor> contributors() {
		return inner.contributors();
	}

	@Override
	public @Nullable String getContactInfo(String key) {
		return inner.getContactInfo(key);
	}

	@Override
	public Map<String, String> contactInfo() {
		return inner.contactInfo();
	}

	@Override
	public Collection<ModDependency> depends() {
		return inner.depends();
	}

	@Override
	public Collection<ModDependency> breaks() {
		return inner.breaks();
	}

	@Override
	public Collection<? extends ProvidedMod> provides() {
		return inner.provides();
	}

	@Override
	public @Nullable String icon(int size) {
		return inner.icon(size);
	}

	@Override
	public boolean containsValue(String key) {
		return inner.containsValue(key);
	}

	@Override
	public @Nullable LoaderValue value(String key) {
		return inner.value(key);
	}

	@Override
	public Map<String, LoaderValue> values() {
		return inner.values();
	}
}
